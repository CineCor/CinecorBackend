package com.cinecor.backend.utils

import com.cinecor.backend.Main
import com.cinecor.backend.model.Cinema
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseCredentials
import com.google.firebase.database.FirebaseDatabase
import java.time.format.DateTimeFormatter

class FirebaseManager {

    private var firebaseDatabase: FirebaseDatabase

    init {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredential(FirebaseCredentials.fromCertificate(System.getenv("FIREBASE_KEY").byteInputStream()))
                .setDatabaseAuthVariableOverride(hashMapOf<String, Any>(Pair("uid", System.getenv("FIREBASE_UID"))))
                .setDatabaseUrl(System.getenv("FIREBASE_DB"))
                .build())

        firebaseDatabase = FirebaseDatabase.getInstance()
    }

    fun uploadCinemas(cinemas: List<Cinema>) {
        val data = hashMapOf<String, Any>(
                Pair("cinemas", cinemas),
                Pair("last_update", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Main.NOW))
        )

        firebaseDatabase.reference.setValue(data) { databaseError, _ ->
            if (databaseError != null) {
                println("Data could not be saved " + databaseError.message)
                System.exit(1)
            } else {
                println("Data saved successfully.")
                System.exit(0)
            }
        }
    }
}
