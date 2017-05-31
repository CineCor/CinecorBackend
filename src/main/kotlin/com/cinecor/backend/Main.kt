package com.cinecor.backend

import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie
import com.google.common.base.CharMatcher
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseCredentials
import com.google.firebase.database.FirebaseDatabase
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


object Main {

    private val PARSE_TIMEOUT = 60000
    private val PARSE_USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
    private val TMDB_LANGUAGE = "es-ES"
    private val NOW = ZonedDateTime.now(ZoneId.of("Europe/Madrid")).withSecond(0).withNano(0)

    private val tmdbApi = TmdbApi(System.getenv("TMDB_API_KEY"))
    private var firebaseDatabase: FirebaseDatabase? = null
    private val cinemas = ArrayList<Cinema>()

    @JvmStatic fun main(args: Array<String>) {
        initializeFirebase()
        parseData()
        fillMovieData()
        uploadToFirebase()
    }

    private fun initializeFirebase() {
        println("Initializing Firebase...")

        try {
            FirebaseApp.initializeApp(FirebaseOptions.Builder()
                    .setCredential(FirebaseCredentials.fromCertificate(System.getenv("FIREBASE_KEY").byteInputStream()))
                    .setDatabaseAuthVariableOverride(hashMapOf<String, Any>(Pair("uid", System.getenv("FIREBASE_UID"))))
                    .setDatabaseUrl(System.getenv("FIREBASE_DB"))
                    .build())

            firebaseDatabase = FirebaseDatabase.getInstance()
        } catch (e: IOException) {
            println("ERROR: invalid service account credentials: " + e.message)
            System.exit(1)
        }
    }

    private fun parseData() {
        println("Parsing Data...")

        try {
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
            } else {
                println("Empty cinemas")
                System.exit(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(0)
        }

    }

    private fun fillMovieData() {
        println("Filling Movie Data...")

        cinemas.forEach { cinema ->
            cinema.movies!!.forEach { movie ->
                if (!fillDataWithExistingMovie(movie)) {
                    if (!fillDataWithExternalApi(movie) || StringUtils.isEmpty(movie.overview)) {
                        fillDataWithOriginalWeb(movie)
                    }

                    fillColors(movie)
                }
            }

            println("Cinema: " + cinema)
        }
    }

    private fun getHoursDateFromText(text: String): List<String> {
        return text
                .split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                .map { CharMatcher.digit().retainFrom(it) }
                .filter { it.length >= 4 }
                .map { LocalTime.parse(it.substring(0, 4), DateTimeFormatter.ofPattern("HHmm")) }
                .map { NOW.withHour(it.hour).withMinute(it.minute) }
                .map { DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(it) }
    }

    private fun fillDataWithExistingMovie(originalMovie: Movie): Boolean {
        cinemas.forEach { cinema ->
            cinema.movies!!.forEach { movie ->
                if (movie.id == originalMovie.id && !movie.overview.isNullOrBlank()) {
                    originalMovie.colors = movie.colors
                    originalMovie.images = movie.images
                    originalMovie.genres = movie.genres
                    originalMovie.rawDescription = movie.rawDescription
                    originalMovie.imdb = movie.imdb
                    originalMovie.trailer = movie.trailer
                    originalMovie.duration = movie.duration
                    originalMovie.releaseDate = movie.releaseDate
                    originalMovie.overview = movie.overview
                    originalMovie.director = movie.director
                    originalMovie.url = movie.url
                    originalMovie.rating = movie.rating
                    originalMovie.title = movie.title
                    return true
                }
            }
        }
        return false
    }

    private fun fillDataWithExternalApi(movie: Movie): Boolean {
        var movieResults = tmdbApi.search.searchMovie(movie.title, NOW.year, TMDB_LANGUAGE, true, 0)
        if (movieResults.totalResults == 0) {
            movieResults = tmdbApi.search.searchMovie(movie.title, null, TMDB_LANGUAGE, true, 0)
        }
        if (movieResults.totalResults != 0) {
            val movieApi = tmdbApi.movies.getMovie(movieResults.results[0].id, TMDB_LANGUAGE, TmdbMovies.MovieMethod.videos)
            if (movieApi != null) {
                if (!StringUtils.isEmpty(movieApi.title)) {
                    movie.title = movieApi.title
                }
                if (!StringUtils.isEmpty(movieApi.imdbID)) {
                    movie.imdb = "http://www.imdb.com/title/" + movieApi.imdbID
                }
                if (movieApi.voteAverage != 0.0f) {
                    movie.rating = movieApi.voteAverage
                }
                if (!StringUtils.isEmpty(movieApi.overview)) {
                    movie.overview = movieApi.overview
                }
                if (!StringUtils.isEmpty(movieApi.releaseDate)) {
                    movie.releaseDate = movieApi.releaseDate
                }
                if (movieApi.runtime != 0) {
                    movie.duration = movieApi.runtime
                }
                if (movieApi.genres != null && !movieApi.genres.isEmpty()) {
                    movie.genres = movieApi.genres.map { it.name }
                }
                if (movieApi.videos != null && !movieApi.videos.isEmpty()) {
                    val video = movieApi.videos[0]
                    if ("Trailer" == video.type && "YouTube" == video.site) {
                        movie.trailer = "https://www.youtube.com/watch?v=" + video.key
                    }
                }
                if (!StringUtils.isEmpty(movieApi.posterPath)) {
                    movie.images!!.put(Movie.Images.POSTER.name, "http://image.tmdb.org/t/p/w780" + movieApi.posterPath)
                    movie.images!!.put(Movie.Images.POSTER_THUMBNAIL.name, "http://image.tmdb.org/t/p/w92" + movieApi.posterPath)
                }
                if (!StringUtils.isEmpty(movieApi.backdropPath)) {
                    movie.images!!.put(Movie.Images.BACKDROP.name, "http://image.tmdb.org/t/p/w780" + movieApi.backdropPath)
                    movie.images!!.put(Movie.Images.BACKDROP_THUMBNAIL.name, "http://image.tmdb.org/t/p/w300" + movieApi.backdropPath)
                }
                return true
            }
        }
        return false
    }

    private fun fillColors(movie: Movie) {
        val images = movie.images
        if (images == null || images.isEmpty()) return

        val url = if (images.containsKey(Movie.Images.BACKDROP.name)) images[Movie.Images.BACKDROP.name] else images[Movie.Images.POSTER.name]
        movie.colors = Utils.getMovieColorsFromUrl(url)
    }

    private fun fillDataWithOriginalWeb(movie: Movie) {
        println("Fetching Movie Data from Original Web...")

        try {
            val document = Jsoup.connect(movie.url)
                    .userAgent(PARSE_USER_AGENT)
                    .timeout(PARSE_TIMEOUT)
                    .get()

            val movieDetails = document.select("div#sobrepelicula h5")
            if (!movieDetails.isEmpty()) {
                movieDetails.indices.forEach { i ->
                    val detail = movieDetails[i].text()
                    if (i == 0) {
                        movie.rawDescription = detail
                    }
                    if (i == 1) {
                        movie.overview = detail
                    }
                }
            } else {
                println("Empty movie details")
                System.exit(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            System.exit(0)
        }

    }

    private fun uploadToFirebase() {
        println("Writing to Firebase...")

        val timeStamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(NOW)
        val data = HashMap<String, Any>()
        data.put("cinemas", cinemas)
        data.put("timestamp", timeStamp)

        firebaseDatabase!!.reference.setValue(data) { databaseError, databaseReference ->
            if (databaseError != null) {
                println("Data could not be saved " + databaseError.message)
                System.exit(1)
            } else {
                println("Data saved successfully.")
                System.exit(0)
            }
        }
    }
}
