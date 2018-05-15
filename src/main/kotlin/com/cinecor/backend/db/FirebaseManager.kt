package com.cinecor.backend.db

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.Billboard
import com.cinecor.backend.model.Movie
import com.cinecor.backend.utils.DateUtils
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient


class FirebaseManager {

    private val firestoreDb: Firestore

    private val COLLECTION_CINEMAS = "cinemas"
    private val COLLECTION_MOVIES = "movies"
    private val COLLECTION_SESSIONS = "sessions"

    init {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(System.getenv("FIREBASE_KEY").byteInputStream()))
                .setDatabaseAuthVariableOverride(mapOf(Pair("uid", System.getenv("FIREBASE_UID"))))
                .build())

        firestoreDb = FirestoreClient.getFirestore()
    }

    fun uploadBillboard(billboardData: Billboard) {
        val batch = firestoreDb.batch()

        val cinemas = firestoreDb.collection(COLLECTION_CINEMAS)
        val movies = firestoreDb.collection(COLLECTION_MOVIES)
        val sessions = firestoreDb.collection(COLLECTION_SESSIONS)

        billboardData.cinemas.forEach {
            batch.set(cinemas.document(it.id), it, SetOptions.mergeFields("id", "name"))
        }

        billboardData.movies.forEach {
            batch.set(movies.document(it.id), it)
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

    fun getRemoteMovies(): List<Movie> =
        firestoreDb.collection(COLLECTION_MOVIES).get().get().documents.map { it.toObject(Movie::class.java) }
}
