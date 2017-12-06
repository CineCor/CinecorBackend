package com.cinecor.backend.firebase

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.dto.BillboardDto
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseCredentials
import com.google.firebase.database.FirebaseDatabase
import java.time.format.DateTimeFormatter

class FirebaseManager {

    private val firebaseDatabase: FirebaseDatabase

    init {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(System.getenv("FIREBASE_KEY").byteInputStream()))
                .setDatabaseAuthVariableOverride(mapOf(Pair("uid", System.getenv("FIREBASE_UID"))))
                .setDatabaseUrl(System.getenv("FIREBASE_DB"))
                .build())

        firebaseDatabase = FirebaseDatabase.getInstance()
    }

    fun uploadBillboard(billboardData: BillboardDto) {
        val data = getDataFromBillboard(billboardData)
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

    private fun getDataFromBillboard(billboardData: BillboardDto): Any {
        return mapOf(
                Pair("billboard", billboardData.billboard),
                Pair("cinemas", billboardData.cinemas.associate { it.id to it }),
                Pair("movies", billboardData.movies.associate { it.id to it }),
                Pair("last_update", DateTimeFormatter.ISO_INSTANT.format(NOW))
        )
    }
}
