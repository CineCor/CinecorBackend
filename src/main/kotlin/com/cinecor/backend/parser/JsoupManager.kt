package com.cinecor.backend.parser

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.Billboard
import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie
import com.cinecor.backend.model.Session
import com.google.common.base.CharMatcher
import org.jsoup.Jsoup
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object JsoupManager {

    private const val PARSE_TIMEOUT = 60000
    private const val PARSE_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"

    fun parseBillboard(): Billboard? {
        val cinemas = ArrayList<Cinema>()
        val movies = ArrayList<Movie>()
        val sessions = ArrayList<Session>()

        try {
            val document = Jsoup.connect(System.getenv("PARSE_URL"))
                    .userAgent(PARSE_USER_AGENT)
                    .timeout(PARSE_TIMEOUT)
                    .get()

            val cinemasElements = document.select("div#bloqueportadaa")
            if (cinemasElements.isNotEmpty()) {
                cinemasElements.forEach { cinemaElement ->
                    val cinemaId = cinemaElement.select("a").first().attr("abs:href").split("&id=").dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    val cinemaName = cinemaElement.select("h1 a").text()

                    val moviesElements = cinemaElement.select("div.pildora")
                    moviesElements.forEach { movieElement ->
                        val movieLink = movieElement.select("a")
                        if (movieLink.isNotEmpty()) {
                            val movieId = movieLink.first().attr("abs:href").split("&id=").dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                            val title = movieLink.first().text()
                            val hours = getHoursDateFromText(movieElement.select("h5").text())
                            val is3d = movieElement.select("h5").text().contains("3D")
                            val isVose = movieElement.select("h5").text().contains("V.O.S.E")
                            val url = movieLink.first().attr("abs:href")

                            if (hours.isNotEmpty()) sessions.add(NOW, cinemaId, movieId, is3d, isVose, hours)
                            movies.add(movieId, title, url)
                        }
                    }

                    if (moviesElements.select("a").isNotEmpty()) {
                        cinemas.add(cinemaId, cinemaName)
                    }
                }

                return Billboard(cinemas, movies, sessions)
            } else {
                println("Empty cinemas")
                System.exit(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(0)
        }
        return null
    }

    private fun getHoursDateFromText(text: String): List<String> {
        return text
                .split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                .map { CharMatcher.digit().retainFrom(it) }
                .filter { it.length >= 4 }
                .map { it.substring(0, 4) }
                .map { LocalTime.parse(it, DateTimeFormatter.ofPattern("HHmm")) }
                .map { NOW.plusDays(if (it.hour < 8) 1 else 0).withHour(it.hour).withMinute(it.minute) }
                .filter { it.isAfter(NOW) }
                .map { DateTimeFormatter.ISO_INSTANT.format(it) }
    }

    fun fillDataWithOriginalWeb(movie: Movie) {
        println("Fetching `" + movie.title + "` from original source...")

        try {
            val document = Jsoup.connect(movie.url)
                    .userAgent(PARSE_USER_AGENT)
                    .timeout(PARSE_TIMEOUT)
                    .get()

            val movieDetails = document.select("div#sobrepelicula h5")
            if (!movieDetails.isEmpty()) {
                movieDetails.indices.forEach { i ->
                    if (i == 0) {
                        movie.rawDescription = movieDetails[i].text()
                    }
                    if (i == 1) {
                        movie.overview = movieDetails[i].text()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fillPosterImage(movie: Movie) {
        var posterBaseUrl = System.getenv("PARSE_URL") + "gestor/ficheros/imagen${movie.id}.jpeg"

        try {
            URL(posterBaseUrl).readText()
        } catch (e: IOException) {
            if (e is FileNotFoundException) posterBaseUrl = posterBaseUrl.replace("jpeg", "jpg")
        }

        movie.images.put(Movie.Images.POSTER.name, posterBaseUrl)
    }
}

private fun ArrayList<Cinema>.add(cinemaId: String, cinemaName: String) {
    add(Cinema(cinemaId, cinemaName))
}

private fun ArrayList<Movie>.add(movieId: String, title: String, url: String) {
    add(Movie(movieId, title, url))
}

private fun ArrayList<Session>.add(time: ZonedDateTime, cinemaId: String, movieId: String, is3d: Boolean, isVose: Boolean, hours: List<String>) {
    val sessionId = DateTimeFormatter.ofPattern("YYYYMMdd").format(time).plus(cinemaId).plus(movieId)
    val key = when {
        is3d -> Session.HoursType.THREEDIM.toString()
        isVose -> Session.HoursType.VOSE.toString()
        else -> Session.HoursType.NORMAL.toString()
    }

    val foundSession = find { it.id == sessionId }
    if (foundSession != null) {
        foundSession.hours.put(key, hours)
    } else {
        add(Session(sessionId, cinemaId, movieId, hashMapOf(Pair(key, hours))))
    }
}
