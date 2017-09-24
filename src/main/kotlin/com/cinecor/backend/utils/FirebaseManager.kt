package com.cinecor.backend.utils

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.BillboardData
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseCredentials
import com.google.firebase.database.FirebaseDatabase
import java.time.format.DateTimeFormatter

class FirebaseManager {

    private val firebaseDatabase: FirebaseDatabase

    init {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredential(FirebaseCredentials.fromCertificate(System.getenv("FIREBASE_KEY").byteInputStream()))
                .setDatabaseAuthVariableOverride(mapOf(Pair("uid", System.getenv("FIREBASE_UID"))))
                .setDatabaseUrl(System.getenv("FIREBASE_DB"))
                .build())

        firebaseDatabase = FirebaseDatabase.getInstance()
    }

    fun uploadBillboard(billboardData: BillboardData) {
        val data = mapOf(
                Pair("billboardData", billboardData),
                Pair("last_update", DateTimeFormatter.ISO_INSTANT.format(NOW))
        )

        firebaseDatabase.getReference("v2").setValue(data) { databaseError, _ ->
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
