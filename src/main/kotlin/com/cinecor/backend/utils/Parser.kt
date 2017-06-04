package com.cinecor.backend.utils

import com.cinecor.backend.Main
import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie
import com.google.common.base.CharMatcher
import org.jsoup.Jsoup
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

object Parser {

    private val PARSE_TIMEOUT = 60000
    private val PARSE_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"

    fun getCinemas(): List<Cinema>? {
        try {
            val cinemas = ArrayList<Cinema>()
            val document = Jsoup.connect(System.getenv("PARSE_URL"))
                    .userAgent(PARSE_USER_AGENT)
                    .timeout(PARSE_TIMEOUT)
                    .get()

            val cinemasElements = document.select("div#bloqueportadaa")
            if (!cinemasElements.isEmpty()) {
                cinemasElements.forEach { cinemaElement ->
                    val cinema = Cinema()
                    cinema.name = cinemaElement.select("h1 a").text()
                    cinema.id = Integer.parseInt(cinemaElement.select("a").first().attr("abs:href").split("&id=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])

                    val movies = ArrayList<Movie>()
                    val moviesElements = cinemaElement.select("div.pildora")
                    moviesElements.forEach { movieElement ->
                        val movieId = Integer.parseInt(movieElement.select("a").first().attr("abs:href").split("&id=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])

                        val movie = Movie()
                        movie.hours = getHoursDateFromText(movieElement.select("h5").text())
                        movie.title = movieElement.select("a").first().text()
                        movie.images = object : HashMap<String, String>() {
                            init {
                                put(Movie.Images.POSTER.name, System.getenv("PARSE_URL") + "gestor/ficheros/imagen$movieId.jpeg")
                            }
                        }
                        movie.url = movieElement.select("a").first().attr("abs:href")
                        movie.id = movieId

                        movies.add(movie)
                    }
                    cinema.movies = movies
                    cinemas.add(cinema)
                }

                cinemas.sortByDescending { it.id }

                return cinemas
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

    fun fillDataWithOriginalWeb(movie: Movie) {
        println("Fetching Movie Data from Original Web...")

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

    private fun getHoursDateFromText(text: String): List<String> {
        return text
                .split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                .map { CharMatcher.digit().retainFrom(it) }
                .filter { it.length >= 4 }
                .map { LocalTime.parse(it.substring(0, 4), DateTimeFormatter.ofPattern("HHmm")) }
                .map { Main.NOW.withHour(it.hour).withMinute(it.minute) }
                .map { DateTimeFormatter.ISO_INSTANT.format(it) }
    }
}
