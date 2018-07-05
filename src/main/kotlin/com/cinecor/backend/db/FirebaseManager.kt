package com.cinecor.backend.db

import com.cinecor.backend.model.Billboard
import com.cinecor.backend.model.Movie
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.cloud.StorageClient
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO


object FirebaseManager {

    private const val COLLECTION_CINEMAS = "cinemas"
    private const val COLLECTION_MOVIES = "movies"
    private const val COLLECTION_SESSIONS = "sessions"

    private val FIREBASE_KEY = System.getenv("FIREBASE_KEY").byteInputStream()
    private val FIREBASE_UID = mapOf(Pair("uid", System.getenv("FIREBASE_UID")))

    fun init() {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(FIREBASE_KEY))
                .setDatabaseAuthVariableOverride(FIREBASE_UID)
                .setStorageBucket("project-8715522583919149180.appspot.com")
                .build())
    }

    fun uploadBillboard(billboardData: Billboard) {
        val firestoreDb = FirestoreClient.getFirestore()

        val cinemas = firestoreDb.collection(COLLECTION_CINEMAS)
        val movies = firestoreDb.collection(COLLECTION_MOVIES)
        val sessions = firestoreDb.collection(COLLECTION_SESSIONS)

        val batch = firestoreDb.batch()

        billboardData.cinemas.forEach { cinema ->
            batch.set(cinemas.document(cinema.id), cinema, SetOptions.mergeFields("id", "name"))
        }

        billboardData.movies.forEach { movie ->
            batch.set(movies.document(movie.id), movie)
        }

        billboardData.sessions.map { it.date }.forEach { date ->
            billboardData.sessions.filter { it.date == date }.forEach { session ->
                batch.set(sessions.document(date).collection(session.cinemaId).document(session.movieId), session)
            }
        }

        batch.commit().get()

        println("### Data saved successfully.")
        System.exit(0)
    }

    fun uploadImage(bufferedImage: BufferedImage? = null, imageUrl: String, imagePath: String): String {
        val firebaseBucket = StorageClient.getInstance().bucket()
        firebaseBucket.get(imagePath)?.run { return mediaLink }

        val bucketBufferedImage = bufferedImage ?: ImageIO.read(URL(imageUrl))

        val imageOutputStream = ByteArrayOutputStream()
        ImageIO.write(bucketBufferedImage, "jpg", imageOutputStream)
        val blob = firebaseBucket.create(imagePath, imageOutputStream.toByteArray(), "image/jpg")
        imageOutputStream.close()

        return blob.mediaLink
    }

    fun getRemoteMovies() =
            FirestoreClient.getFirestore().collection(COLLECTION_MOVIES).get().get().documents.map { it.toObject(Movie::class.java) }

    fun clearData() {
        val firestoreDb = FirestoreClient.getFirestore()
        val firebaseBucket = StorageClient.getInstance().bucket()

        val batch = firestoreDb.batch()
        firestoreDb.collection(COLLECTION_CINEMAS).get().get().documents.forEach { batch.delete(it.reference) }
        firestoreDb.collection(COLLECTION_MOVIES).get().get().documents.forEach { batch.delete(it.reference) }
        firestoreDb.collection(COLLECTION_SESSIONS).get().get().documents.forEach { batch.delete(it.reference) }
        batch.commit().get()

        firebaseBucket.list().iterateAll().filter { it.blobId.name.contains("movies") }.forEach { it.delete() }
    }
}
