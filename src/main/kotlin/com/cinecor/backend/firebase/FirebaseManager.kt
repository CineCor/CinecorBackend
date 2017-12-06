package com.cinecor.backend.firebase

import com.cinecor.backend.model.Billboard
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.cloud.firestore.WriteBatch
import com.google.api.core.ApiFuture





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

        billboardData.cinemas.forEach {
            batch.set(firestoreDb.collection("cinemas").document(it.id), it)
        }

        billboardData.movies.forEach {
            batch.set(firestoreDb.collection("movies").document(it.id), it)
        }

        billboardData.sessions.forEach {
            batch.set(firestoreDb.collection("sessions").document(it.id), it)
        }

        batch.commit().get()

        println("Data saved successfully.")
        System.exit(0)
    }
}
