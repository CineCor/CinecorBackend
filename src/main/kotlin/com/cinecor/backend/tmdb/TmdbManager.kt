package com.cinecor.backend.tmdb

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.model.dto.CinemaDto
import com.cinecor.backend.model.dto.BillboardDto
import com.cinecor.backend.model.Movie
import com.cinecor.backend.parser.JsoupManager
import com.vivekpanyam.iris.Bitmap
import com.vivekpanyam.iris.Color
import com.vivekpanyam.iris.Palette
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO

object TmdbManager {

    private const val TMDB_LANGUAGE = "es-ES"
    private val tmdbApi = TmdbApi(System.getenv("TMDB_API_KEY"))

    fun fillMoviesData(billboardData: BillboardDto) {
        billboardData.movies.forEach { movie ->
            if (!fillDataWithExistingMovie(movie, billboardData.movies)) {
                if (!fillDataWithExternalApi(movie) || movie.overview.isBlank()) {
                    JsoupManager.fillDataWithOriginalWeb(movie)
                }

                if (!movie.images.containsKey(Movie.Images.POSTER.name)) {
                    JsoupManager.fillPosterImage(movie)
                }

                fillColors(movie)
            }
            fillBillboardMovie(billboardData.billboard, movie)
        }
    }

    private fun fillDataWithExistingMovie(originalMovie: Movie, movies: List<Movie>): Boolean {
        movies.forEach { movie ->
            if (movie.id == originalMovie.id && movie.overview.isNotBlank()) {
                originalMovie.copy(movie)
                return true
            }
        }
        return false
    }

    private fun fillDataWithExternalApi(movie: Movie): Boolean {
        var movieResults = searchMovie(movie.title, NOW.year)
        if (movieResults.totalResults == 0) movieResults = searchMovie(movie.title, NOW.year - 1)
        if (movieResults.totalResults == 0) movieResults = searchMovie(movie.title, 0)
        if (movieResults.totalResults != 0) {
            var movieDb = movieResults.results[0]
            if (movieDb.overview.isBlank() && movieResults.totalResults > 1) {
                movieDb = movieResults.results[1]
            }

            val movieApi = tmdbApi.movies.getMovie(movieDb.id, TMDB_LANGUAGE, TmdbMovies.MovieMethod.videos)
            movieApi?.let {
                movie.copy(it)
                return true
            }
        }
        return false
    }

    private fun fillColors(movie: Movie) {
        if (movie.images.isEmpty()) return
        val url = movie.images.getOrDefault(Movie.Images.BACKDROP_THUMBNAIL.name, movie.images[Movie.Images.POSTER.name])
        getMovieColorsFromUrl(url)?.let { movie.colors = it }
    }

    private fun getMovieColorsFromUrl(url: String?): HashMap<String, String>? {
        try {
            val palette = Palette.Builder(Bitmap(ImageIO.read(URL(url)))).generate()
            palette?.let {
                var swatch = palette.vibrantSwatch
                if (swatch == null) swatch = palette.mutedSwatch
                if (swatch == null) swatch = palette.dominantSwatch

                return hashMapOf(
                        Pair(Movie.Colors.MAIN.name, swatch.rgb.formatedColor()),
                        Pair(Movie.Colors.TITLE.name, swatch.titleTextColor.formatedColor()),
                        Pair(Movie.Colors.BODY.name, swatch.bodyTextColor.formatedColor())
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun fillBillboardMovie(billboard: List<CinemaDto>, movie: Movie) {
        billboard.forEach {
            it.movies.filter { it.id == movie.id }.forEach {
                it.title = movie.title
                it.images = movie.images
                it.colors = movie.colors
            }
        }
    }

    private fun searchMovie(title: String, year: Int) = tmdbApi.search.searchMovie(title, year, TMDB_LANGUAGE, true, 0)

    private fun Int.formatedColor() = String.format("#%02x%02x%02x%02x", Color.red(this), Color.green(this), Color.blue(this), Color.alpha(this))
}
