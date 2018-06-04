package com.cinecor.backend.db

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.Billboard
import com.cinecor.backend.model.Movie
import com.cinecor.backend.utils.DateUtils
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.cloud.storage.Bucket
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.cloud.StorageClient
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO


class FirebaseManager {

    companion object {
        private const val COLLECTION_CINEMAS = "cinemas"
        private const val COLLECTION_MOVIES = "movies"
        private const val COLLECTION_SESSIONS = "sessions"
    }

    private val FIREBASE_KEY = System.getenv("FIREBASE_KEY").byteInputStream()
    private val FIREBASE_UID = mapOf(Pair("uid", System.getenv("FIREBASE_UID")))

    private val firestoreDb: Firestore
    private var bucket: Bucket

    init {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(FIREBASE_KEY))
                .setDatabaseAuthVariableOverride(FIREBASE_UID)
                .setStorageBucket("project-8715522583919149180.appspot.com")
                .build())

        firestoreDb = FirestoreClient.getFirestore()
        bucket = StorageClient.getInstance().bucket()
    }

    fun uploadBillboard(billboardData: Billboard) {
        val batch = firestoreDb.batch()

        val cinemas = firestoreDb.collection(COLLECTION_CINEMAS)
        val movies = firestoreDb.collection(COLLECTION_MOVIES)
        val sessions = firestoreDb.collection(COLLECTION_SESSIONS)

        billboardData.cinemas.forEach {
            batch.set(cinemas.document(it.id), it, SetOptions.mergeFields("id", "name"))
        }

        billboardData.movies.forEach { movie ->
            movie.imagePoster = uploadImage("${movie.id}/poster", movie.imagePoster)
            if (!movie.imageBackdrop.isNullOrBlank())
                movie.imageBackdrop = uploadImage("${movie.id}/backdrop", movie.imageBackdrop!!)

            batch.set(movies.document(movie.id), movie)
        }

        val earlierSessionId = DateUtils.DATE_FORMAT_FULL_SIMPLE.format(NOW).plus("00000")
        sessions.whereLessThan("id", earlierSessionId).get().get().documents.forEach {
            batch.delete(sessions.document(it.id))
        }

        billboardData.sessions.forEach {
            batch.set(sessions.document(it.id), it)
        }

        batch.commit().get()

        println("### Data saved successfully.")
        System.exit(0)
    }

    private fun uploadImage(imageId: String, imageUrl: String): String {
        val outputStream = ByteArrayOutputStream()
        val bufferedImage = ImageIO.read(URL(imageUrl))
        ImageIO.write(bufferedImage, "jpg", outputStream)
        return bucket.create("movies/$imageId.jpg", outputStream.toByteArray(), "image/jpg").mediaLink
    }

    fun getRemoteMovies(): List<Movie> =
            firestoreDb.collection(COLLECTION_MOVIES).get().get().documents.map { it.toObject(Movie::class.java) }
}
