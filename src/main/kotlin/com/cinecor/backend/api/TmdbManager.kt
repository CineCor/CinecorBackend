package com.cinecor.backend.api

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.Billboard
import com.cinecor.backend.model.Movie
import com.cinecor.backend.parser.JsoupManager
import com.cinecor.backend.utils.DateUtils
import com.vivekpanyam.iris.Bitmap
import com.vivekpanyam.iris.Color
import com.vivekpanyam.iris.Palette
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import info.movito.themoviedbapi.model.core.MovieResultsPage
import java.io.IOException
import java.net.URL
import java.time.ZonedDateTime
import javax.imageio.ImageIO

object TmdbManager {

    private const val TMDB_LANGUAGE = "es-ES"
    private val tmdbApi = TmdbApi(System.getenv("TMDB_API_KEY"))

    fun fillMoviesData(billboardData: Billboard, remoteMovies: List<Movie>) {
        val localMovies = billboardData.movies
        localMovies.forEach { movie ->
            if (!fillDataWithExistingMovies(movie, localMovies, remoteMovies)) {
                JsoupManager.fillBasicDataWithOriginalSource(movie)

                fillDataWithApi(movie)

                if (!movie.images.containsKey(Movie.Images.POSTER.name)) {
                    JsoupManager.fillPosterImage(movie)
                }

                fillColors(movie)
            }
        }

        billboardData.sessions.forEach { session ->
            localMovies.find { it.id == session.movieId }?.let {
                session.movieTitle = it.title
                session.movieImages = it.images
            }
        }
    }

    private fun fillDataWithExistingMovies(movie: Movie, localMovies: List<Movie>, remoteMovies: List<Movie>): Boolean {
        remoteMovies.find { it.id == movie.id && it.overview.isNotBlank() }?.let {
            movie.copy(it)
            return true
        }
        localMovies.find { it.id == movie.id && it.overview.isNotBlank() }?.let {
            movie.copy(it)
            return true
        }
        return false
    }

    private fun fillDataWithApi(movie: Movie): Boolean {
        var movieResults = MovieResultsPage()

        if (movie.releaseDate != null) {
            movieResults = searchMovie(movie.title, ZonedDateTime.from(DateUtils.DATE_FORMAT_ISO.parse(movie.releaseDate)).year)
        }

        if (movieResults.totalResults == 0) movieResults = searchMovie(movie.title, NOW.year)
        if (movieResults.totalResults == 0) movieResults = searchMovie(movie.title, NOW.year - 1)
        if (movieResults.totalResults == 0) movieResults = searchMovie(movie.title, 0)
        if (movieResults.totalResults != 0) {
            val movieApi = if (movieResults.results[0].overview.isBlank() && movieResults.totalResults > 1) {
                movieResults.results[1]
            } else {
                movieResults.results[0]
            }

            tmdbApi.movies.getMovie(movieApi.id, TMDB_LANGUAGE, TmdbMovies.MovieMethod.videos)?.let {
                movie.copy(it)
                return true
            }
        }
        return false
    }

    private fun searchMovie(title: String, year: Int) = tmdbApi.search.searchMovie(title, year, TMDB_LANGUAGE, true, 0)

    private fun fillColors(movie: Movie) {
        if (movie.images.isEmpty()) return
        val url = movie.images.getOrDefault(Movie.Images.BACKDROP_THUMBNAIL.name, movie.images[Movie.Images.POSTER.name])
        getMovieColorsFromImageUrl(url)?.let { movie.colors = it }
    }

    private fun getMovieColorsFromImageUrl(url: String?): HashMap<String, String>? {
        try {
            val palette = Palette.Builder(Bitmap(ImageIO.read(URL(url)))).generate()
            palette?.let {
                var swatch = palette.vibrantSwatch
                if (swatch == null) swatch = palette.mutedSwatch
                if (swatch == null) swatch = palette.dominantSwatch

                return hashMapOf(
                        Pair(Movie.Colors.MAIN.name, swatch.rgb.formattedColor()),
                        Pair(Movie.Colors.TITLE.name, swatch.titleTextColor.formattedColor()),
                        Pair(Movie.Colors.BODY.name, swatch.bodyTextColor.formattedColor())
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun Int.formattedColor() = String.format("#%02x%02x%02x%02x", Color.red(this), Color.green(this), Color.blue(this), Color.alpha(this))
}
