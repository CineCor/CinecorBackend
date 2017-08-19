package com.cinecor.backend

import com.cinecor.backend.utils.FirebaseManager
import com.cinecor.backend.utils.Parser
import com.cinecor.backend.utils.TmdbManager
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
        val billboard = Parser.parseBillboard()

        billboard?.let {
            println("Filling Movies Data...")
            TmdbManager.fillMoviesData(it.movies)

            println("Writing to Firebase...")
            firebaseManager.uploadBillboard(it)
        }
    }
}
