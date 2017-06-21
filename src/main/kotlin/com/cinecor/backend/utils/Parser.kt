package com.cinecor.backend.utils

import com.cinecor.backend.Main
import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie
import com.google.common.base.CharMatcher
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Parser {

    private const val PARSE_TIMEOUT = 60000
    private const val PARSE_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"

    fun getCinemas(): List<Cinema>? {
        val cinemas = parseWeb()
        val sortedCinemas = cinemas?.sorted()
        return sortedCinemas
    }

    private fun parseWeb(): ArrayList<Cinema>? {
        try {
            val cinemas = ArrayList<Cinema>()
            val document = Jsoup.connect(System.getenv("PARSE_URL"))
                    .userAgent(PARSE_USER_AGENT)
                    .timeout(PARSE_TIMEOUT)
                    .get()

            val cinemasElements = document.select("div#bloqueportadaa")
            if (cinemasElements.isNotEmpty()) {
                cinemasElements.forEach { cinemaElement ->
                    val cinema = Cinema()
                    cinema.name = cinemaElement.select("h1 a").text()
                    cinema.id = Integer.parseInt(cinemaElement.select("a").first().attr("abs:href").split("&id=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])

                    val movies = ArrayList<Movie>()
                    val moviesElements = cinemaElement.select("div.pildora")
                    moviesElements.forEach { movieElement ->
                        val movieLink = movieElement.select("a")
                        if (movieLink.isNotEmpty()) {
                            val movie = Movie()

                            movie.id = Integer.parseInt(movieLink.first().attr("abs:href").split("&id=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                            movie.hours = getHoursDateFromText(movieElement.select("h5").text())
                            movie.title = movieLink.first().text()
                            movie.images = getPosterImage(movie.id)
                            movie.url = movieLink.first().attr("abs:href")

                            movies.add(movie)
                        }
                    }
                    if (movies.isNotEmpty()) {
                        cinema.movies = movies
                        cinemas.add(cinema)
                    }
                }

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

    private fun getPosterImage(movieId: Int): HashMap<String, String> {
        var posterUrl = System.getenv("PARSE_URL") + "gestor/ficheros/imagen$movieId.jpeg"

        val http = URL(posterUrl).openConnection() as HttpURLConnection
        if (http.getResponseCode() != HttpURLConnection.HTTP_OK) {
            posterUrl = System.getenv("PARSE_URL") + "gestor/ficheros/imagen$movieId.jpg"
        }

        return hashMapOf(Pair(Movie.Images.POSTER.name, posterUrl))
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
                .map { Main.NOW.plusDays(if (it.hour < 8) 1 else 0).withHour(it.hour).withMinute(it.minute) }
                .map { DateTimeFormatter.ISO_INSTANT.format(it) }
    }
}
