package com.cinecor.backend.utils

import com.cinecor.backend.Main
import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie
import com.vivekpanyam.iris.Bitmap
import com.vivekpanyam.iris.Color
import com.vivekpanyam.iris.Palette
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO

object Tmdb {

    private const val TMDB_LANGUAGE = "es-ES"
    private val tmdbApi = TmdbApi(System.getenv("TMDB_API_KEY"))

    fun fillMovieData(cinemas: List<Cinema>) {
        cinemas.forEach { cinema ->
            cinema.movies.forEach { movie ->
                if (!fillDataWithExistingMovie(cinemas, movie)) {
                    if (!fillDataWithExternalApi(movie) || movie.overview.isBlank()) {
                        Parser.fillDataWithOriginalWeb(movie)
                    }

                    if (!movie.images.containsKey(Movie.Images.POSTER.name)) {
                        Parser.fillPosterImage(movie)
                    }

                    fillColors(movie)
                }
            }
        }
    }

    private fun fillDataWithExistingMovie(cinemas: List<Cinema>, originalMovie: Movie): Boolean {
        cinemas.forEach { cinema ->
            cinema.movies.forEach { movie ->
                if (movie.id == originalMovie.id && movie.overview.isNotBlank()) {
                    originalMovie.copy(movie)
                    return true
                }
            }
        }
        return false
    }

    private fun fillDataWithExternalApi(movie: Movie): Boolean {
        var movieResults = searchMovie(movie.title, Main.NOW.year)
        if (movieResults.totalResults == 0) movieResults = searchMovie(movie.title, Main.NOW.year - 1)
        if (movieResults.totalResults == 0) movieResults = searchMovie(movie.title, 0)
        if (movieResults.totalResults != 0) {
            var movieDb = movieResults.results[0]
            if (movieDb.overview.isBlank() && movieResults.totalResults > 1) {
                movieDb = movieResults.results[1]
            }

            val movieApi = tmdbApi.movies.getMovie(movieDb.id, TMDB_LANGUAGE, TmdbMovies.MovieMethod.videos)
            movieApi.let {
                movie.copy(it)
                return true
            }
        }
        return false
    }

    private fun fillColors(movie: Movie) {
        if (movie.images.isEmpty()) return

        val url = movie.images.getOrDefault(Movie.Images.BACKDROP.name, movie.images[Movie.Images.POSTER.name])
        url?.let {
            val colors = getMovieColorsFromUrl(it)
            colors?.let { movie.colors = it }
        }
    }

    private fun getMovieColorsFromUrl(url: String): HashMap<String, String>? {
        try {
            val palette = Palette.Builder(Bitmap(ImageIO.read(URL(url)))).generate()
            palette?.let {
                var swatch = palette.vibrantSwatch
                if (swatch == null) swatch = palette.mutedSwatch
                if (swatch == null) swatch = palette.dominantSwatch

                return hashMapOf(
                        Pair(Movie.Colors.MAIN.name, formatColor(swatch.rgb)),
                        Pair(Movie.Colors.TITLE.name, formatColor(swatch.titleTextColor)),
                        Pair(Movie.Colors.BODY.name, formatColor(swatch.bodyTextColor))
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun searchMovie(title: String, year: Int) = tmdbApi.search.searchMovie(title, year, TMDB_LANGUAGE, true, 0)
    private fun formatColor(color: Int): String = String.format("#%02x%02x%02x%02x", Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color))
}
