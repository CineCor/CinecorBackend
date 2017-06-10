package com.cinecor.backend

import com.cinecor.backend.utils.FirebaseManager
import com.cinecor.backend.utils.Parser
import com.cinecor.backend.utils.Tmdb
import java.time.ZoneId
import java.time.ZonedDateTime


object Main {

    val NOW: ZonedDateTime = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).withSecond(0).withNano(0)

    @JvmStatic fun main(args: Array<String>) {
        println("Initializing Firebase...")
        val firebaseManager = FirebaseManager()

        println("Parsing Data...")
        val cinemas = Parser.getCinemas()

        cinemas?.let {
            println("Filling Movie Data...")
            Tmdb.fillMovieData(it)

            println("Writing to Firebase...")
            firebaseManager.uploadCinemas(it)
        }
    }
}
