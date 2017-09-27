package com.cinecor.backend.parser

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.firebase.model.CinemaDto
import com.cinecor.backend.firebase.model.MovieDto
import com.cinecor.backend.model.BillboardData
import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie
import com.google.common.base.CharMatcher
import org.jsoup.Jsoup
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object JsoupManager {

    private const val PARSE_TIMEOUT = 60000
    private const val PARSE_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"

    fun parseBillboard(): BillboardData? {
        val cinemas = ArrayList<CinemaDto>()
        val movies = ArrayList<MovieDto>()

        try {
            val document = Jsoup.connect(System.getenv("PARSE_URL"))
                    .userAgent(PARSE_USER_AGENT)
                    .timeout(PARSE_TIMEOUT)
                    .get()

            val cinemasElements = document.select("div#bloqueportadaa")
            if (cinemasElements.isNotEmpty()) {
                cinemasElements.forEach { cinemaElement ->
                    val moviesElements = cinemaElement.select("div.pildora")
                    moviesElements.forEach { movieElement ->
                        val movieLink = movieElement.select("a")
                        if (movieLink.isNotEmpty()) {
                            val id = Integer.parseInt(movieLink.first().attr("abs:href").split("&id=").dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                            val title = movieLink.first().text()
                            val hours = getHoursDateFromText(movieElement.select("h5").text())
                            val is3d = movieElement.select("h5").text().contains("3D")
                            val isVose = movieElement.select("h5").text().contains("V.O.S.E")
                            val url = movieLink.first().attr("abs:href")

                            movies.add(MovieDto(id, title, hours, is3d, isVose, url))
                        }
                    }

                    if (movies.isNotEmpty()) {
                        val id = Integer.parseInt(cinemaElement.select("a").first().attr("abs:href").split("&id=").dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                        val name = cinemaElement.select("h1 a").text()

                        cinemas.add(CinemaDto(id, name, movies))
                    }
                }

                return getBillBoard(cinemas.sorted(), movies)
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

    private fun getBillBoard(cinemasDto: List<CinemaDto>, moviesDto: List<MovieDto>): BillboardData {
        val cinemas = HashSet<Cinema>()
        val movies = HashSet<Movie>()

        cinemasDto.forEach { cinemas.add(Cinema(it.id, it.name)) }
        moviesDto.forEach { movies.add(Movie(it.id, it.title, it.is3d, it.isVose, it.url)) }

        return BillboardData(cinemasDto, cinemas.toList(), movies.toList())
    }

    private fun getHoursDateFromText(text: String): List<String> {
        return text
                .split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                .map { CharMatcher.digit().retainFrom(it) }
                .filter { it.length >= 4 }
                .map { LocalTime.parse(it.substring(0, 4), DateTimeFormatter.ofPattern("HHmm")) }
                .map { NOW.plusDays(if (it.hour < 8) 1 else 0).withHour(it.hour).withMinute(it.minute) }
                .map { DateTimeFormatter.ISO_INSTANT.format(it) }
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
