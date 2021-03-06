package com.cinecor.backend.parser

import com.cinecor.backend.Main
import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.Billboard
import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie
import com.cinecor.backend.model.Session
import com.cinecor.backend.utils.DateUtils
import com.google.common.base.CharMatcher
import org.jsoup.Jsoup
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object JsoupManager {

    private const val PARSE_TIMEOUT = 60000
    private const val PARSE_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"

    fun parseBillboard(): Billboard? = try {
        val billboard = Billboard()
        for (i in 0..6) parseWeb(billboard, NOW.plusDays(i.toLong()))
        billboard
    } catch (e: Exception) {
        e.printStackTrace()
        System.exit(0)
        null
    }

    private fun parseWeb(billboard: Billboard, date: ZonedDateTime): Billboard {
        val formattedDate = DateUtils.DATE_FORMAT_FULL.format(date)
        val parseUrl = "${System.getenv("PARSE_URL")}index.php?dcar=$formattedDate"
        val document = Jsoup.connect(parseUrl)
                .userAgent(PARSE_USER_AGENT)
                .timeout(PARSE_TIMEOUT)
                .get()

        val cinemasElements = document.select("div#bloqueportadaa")
        if (cinemasElements.isNotEmpty()) {
            cinemasElements.forEach { cinemaElement ->
                val cinemaId = cinemaElement.select("a").first().attr("abs:href").split("&id=").dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                val cinemaName = cinemaElement.select("h1 a").text()

                val moviesElements = cinemaElement.select("div.pildora")
                if (moviesElements.isNotEmpty()) {
                    moviesElements.forEach { movieElement ->
                        val movieLink = movieElement.select("a")
                        if (movieLink.isNotEmpty()) {
                            val movieId = movieLink.first().attr("abs:href").split("&id=").dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                            val title = movieLink.first().text()
                            val rawHours = movieElement.select("h5").text()
                            val hours = getFormattedDatesFromHoursText(date, rawHours)
                            val is3d = rawHours.contains("3D")
                            val isVose = rawHours.contains("V.O.S.E")
                            val isJunior = rawHours.contains("Junior")
                            val url = movieLink.first().attr("abs:href")

                            billboard.sessions.add(date, cinemaId, movieId, is3d, isVose, isJunior, hours)
                            billboard.movies.add(movieId, title, url)
                        }
                    }

                    if (moviesElements.select("a").isNotEmpty()) {
                        billboard.cinemas.add(cinemaId, cinemaName)
                    }
                } else {
                    System.err.println("Empty movies for $cinemaName on $formattedDate")
                }
            }
        } else {
            System.err.println("Empty cinemas on $formattedDate")
        }
        return billboard
    }

    fun Movie.fillBasicDataWithOriginalSource() {
        println("\t Fetching `$title` from original source...")

        try {
            val document = Jsoup.connect(originalUrl)
                    .userAgent(PARSE_USER_AGENT)
                    .timeout(PARSE_TIMEOUT)
                    .get()

            val movieDetails = document.select("div#sobrepelicula")

            movieDetails.select("img")?.first()?.absUrl("src")?.let { imagePoster = it }

            movieDetails.select("h5")?.let { descriptions ->
                for ((index, description) in descriptions.withIndex()) {
                    if (index == 0) {
                        raw = description.text()
                        getFieldFromRawHtml(description.html(), "Año")?.let { year = it.toInt() }
                        getFieldFromRawHtml(description.html(), "Título original")?.let { originalTitle = it }
                    } else if (index == 1) {
                        overview = description.text()
                        break
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("\t\t ERROR Fetching from `$originalUrl`: $e")
        }
    }

    private fun getFieldFromRawHtml(html: String, field: String): String? =
            html.split("<br>")
                    .find { it.contains(field) }
                    ?.split("</b>")?.get(1)
                    ?.clean()

    private fun getFormattedDatesFromHoursText(date: ZonedDateTime, hours: String) =
            hours.split("-".toRegex())
                    .dropLastWhile { it.isEmpty() }.toTypedArray()
                    .map { CharMatcher.digit().retainFrom(it) }
                    .filter { it.length >= 4 }
                    .map { it.substring(0, 4) }
                    .map { LocalTime.parse(it, DateUtils.DATE_FORMAT_HOUR) }
                    .map { date.plusDays(if (it.hour < 5) 1 else 0).withHour(it.hour).withMinute(it.minute) }
                    .filter { it.isAfter(Main.NOW) }
                    .map { DateTimeFormatter.ISO_INSTANT.format(it) }

    private fun MutableSet<Cinema>.add(cinemaId: String, cinemaName: String) =
            add(Cinema(cinemaId, cinemaName))

    private fun MutableSet<Movie>.add(movieId: String, title: String, url: String) =
            add(Movie(movieId, title, url))

    private fun MutableSet<Session>.add(time: ZonedDateTime, cinemaId: String, movieId: String, is3d: Boolean, isVose: Boolean, isJunior: Boolean, hours: List<String>) {
        if (hours.isEmpty()) return

        val sessionTime = DateUtils.DATE_FORMAT_FULL_SIMPLE.format(time)
        val sessionId = sessionTime.plus(cinemaId).plus(movieId)
        val sessionType = when {
            is3d -> Session.Type.THREEDIM
            isVose -> Session.Type.VOSE
            isJunior -> Session.Type.JUNIOR
            else -> Session.Type.NORMAL
        }

        val foundSession = find { it.id == sessionId }
        if (foundSession != null) {
            foundSession.hours.put(sessionType.name, hours)
        } else {
            add(Session(sessionId, cinemaId, movieId, sessionTime, hashMapOf(Pair(sessionType.name, hours))))
        }
    }

    private fun String.clean() = Regex("[^A-Za-z0-9 ]").replace(this.trim(), "")
}
