package com.cinecor.backend

import com.cinecor.backend.firebase.FirebaseManager
import com.cinecor.backend.parser.JsoupManager
import com.cinecor.backend.tmdb.TmdbManager
import java.time.ZoneId
import java.time.ZonedDateTime


object Main {

    @JvmStatic
    val NOW: ZonedDateTime = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).withSecond(0).withNano(0)

    @JvmStatic
    fun main(args: Array<String>) {
        println("Initializing Firebase...")
        val firebaseManager = FirebaseManager()

        println("Parsing Data...")
        val billboardData = JsoupManager.parseBillboard()

        billboardData?.let {
            println("Filling Movies Data...")
            TmdbManager.fillMoviesData(billboardData)

            println("Writing to Firebase...")
            firebaseManager.uploadBillboard(billboardData)
        }
    }
}
