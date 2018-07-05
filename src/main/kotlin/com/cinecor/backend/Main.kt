package com.cinecor.backend

import com.cinecor.backend.api.TmdbManager
import com.cinecor.backend.db.FirebaseManager
import com.cinecor.backend.parser.JsoupManager
import java.time.ZoneId
import java.time.ZonedDateTime


object Main {

    @JvmStatic
    val NOW: ZonedDateTime = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).withSecond(0).withNano(0)

    @JvmStatic
    fun main(args: Array<String>) {
        println("### Initializing Firebase")
        FirebaseManager.init()

        if (NOW.dayOfMonth == 1) {
            println("### Clearing old data")
            FirebaseManager.clearData()
        }

        println("### Parsing movies")
        val billboardData = JsoupManager.parseBillboard()

        billboardData?.let {
            println("### Filling movies data")
            TmdbManager.fillMoviesData(billboardData, FirebaseManager.getRemoteMovies())

            println("### Writing to Firebase")
            FirebaseManager.uploadBillboard(billboardData)
        }
    }
}
