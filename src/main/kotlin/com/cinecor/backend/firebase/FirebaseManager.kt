package com.cinecor.backend.firebase

import com.cinecor.backend.model.Billboard
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient


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
        deleteCollection(firestoreDb.collection("sessions"), 100)

        val batch = firestoreDb.batch()

        billboardData.cinemas.forEach {
            batch.set(firestoreDb.collection("cinemas").document(it.id), it, SetOptions.mergeFields("id", "name"))
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

    private fun deleteCollection(collection: CollectionReference, batchSize: Int) {
        try {
            // retrieve a small batch of documents to avoid out-of-memory errors
            val future = collection.limit(batchSize).get()
            var deleted = 0
            // future.get() blocks on document retrieval
            val documents = future.get().documents
            for (document in documents) {
                document.reference.delete()
                ++deleted
            }
            if (deleted >= batchSize) {
                // retrieve and delete another batch
                deleteCollection(collection, batchSize)
            }
        } catch (e: Exception) {
            System.err.println("Error deleting collection : " + e.message)
        }
    }
}
