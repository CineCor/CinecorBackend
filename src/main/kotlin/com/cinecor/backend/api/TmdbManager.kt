package com.cinecor.backend.api

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.Billboard
import com.cinecor.backend.model.Movie
import com.cinecor.backend.parser.JsoupManager.fillBasicDataWithOriginalSource
import com.vivekpanyam.iris.Bitmap
import com.vivekpanyam.iris.Color
import com.vivekpanyam.iris.Palette
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import info.movito.themoviedbapi.model.MovieDb
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO

object TmdbManager {

    private const val TMDB_LANGUAGE = "es-ES"
    private val tmdbApi = TmdbApi(System.getenv("TMDB_API_KEY"))

    fun fillMoviesData(billboardData: Billboard, remoteMovies: List<Movie>) {
        billboardData.movies.forEach { movie ->
            if (!movie.fillDataWithExistingMovies(billboardData.movies, remoteMovies)) {
                movie.fillBasicDataWithOriginalSource()
                movie.fillColors()
                movie.fillDataWithExternalApi() // TODO Fill colors should be done at the end, make GCS url public to download
            }
        }

        billboardData.sessions.forEach { session ->
            billboardData.movies.find { it.id == session.movieId }?.let {
                session.movieTitle = it.title
                session.movieImage = it.imageBackdrop ?: it.imagePoster
            }
        }
    }

    private fun Movie.fillDataWithExistingMovies(localMovies: MutableSet<Movie>, remoteMovies: List<Movie>): Boolean {
        remoteMovies.plus(localMovies)
                .filter { it.overview.isNotBlank() }
                .find { it.id == id }?.let {
                    copy(it)
                    return true
                }
        return false
    }

    private fun Movie.fillColors() {
        if (imagePoster.isEmpty()) return

        try {
            val palette = Palette.Builder(Bitmap(ImageIO.read(URL(imagePoster)))).generate()
            palette?.let {
                var swatch = palette.vibrantSwatch
                if (swatch == null) swatch = palette.mutedSwatch
                if (swatch == null) swatch = palette.dominantSwatch

                colors = hashMapOf(
                        Pair(Movie.Colors.MAIN.name, swatch.rgb.formattedColor()),
                        Pair(Movie.Colors.TITLE.name, swatch.titleTextColor.formattedColor()),
                        Pair(Movie.Colors.BODY.name, swatch.bodyTextColor.formattedColor())
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun Movie.fillDataWithExternalApi() {
        println("\t\t Filling data with external API...")

        val movieYear = year?.let { it } ?: NOW.year
        val movieTitle = originalTitle?.let { it } ?: title

        try {
            // TODO Search with different combinations of title/years
            val foundMovie = searchMovie(title, NOW.year) ?: searchMovie(movieTitle, movieYear)
            foundMovie?.let { copy(fetchMovie(it)) }
        } catch (e: Exception) {
            println("\t\t ERROR Filling data from `$originalUrl`: $e")
        }
    }

    @Throws(IndexOutOfBoundsException::class)
    private fun searchMovie(title: String, year: Int?): Int? =
            tmdbApi.search.searchMovie(title, year, TMDB_LANGUAGE, true, 0)?.results?.get(0)?.id

    private fun fetchMovie(movieId: Int): MovieDb =
            tmdbApi.movies.getMovie(movieId, TMDB_LANGUAGE, TmdbMovies.MovieMethod.videos)

    private fun Int.formattedColor() = String.format("#%02x%02x%02x%02x", Color.red(this), Color.green(this), Color.blue(this), Color.alpha(this))
}
