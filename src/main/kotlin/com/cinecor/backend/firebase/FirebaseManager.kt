package com.cinecor.backend.firebase

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.Billboard
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import java.time.format.DateTimeFormatter


class FirebaseManager {

    private val firestoreDb: Firestore

    init {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(System.getenv("FIREBASE_KEY").byteInputStream()))
                .setDatabaseAuthVariableOverride(mapOf(Pair("uid", System.getenv("FIREBASE_UID"))))
                .build())

        firestoreDb = FirestoreClient.getFirestore()
    }

    fun uploadBillboard(billboardData: Billboard) {
        val batch = firestoreDb.batch()
        val cinemas = firestoreDb.collection("cinemas")
        val movies = firestoreDb.collection("movies")
        val sessions = firestoreDb.collection("sessions")

        sessions.whereLessThan("id", DateTimeFormatter.ofPattern("YYYYMMdd").format(NOW).plus("00000")).get().get().documents.forEach {
            batch.delete(sessions.document(it.id))
        }

        billboardData.cinemas.forEach {
            batch.set(cinemas.document(it.id), it, SetOptions.mergeFields("id", "name"))
        }

        billboardData.movies.forEach {
            batch.set(movies.document(it.id), it)
        }

        billboardData.sessions.forEach {
            batch.set(sessions.document(it.id), it)
        }

        batch.commit().get()

        println("Data saved successfully.")
        System.exit(0)
    }
}
