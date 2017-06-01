package com.cinecor.backend.utils

import com.cinecor.backend.Main
import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie
import com.vivekpanyam.iris.Bitmap
import com.vivekpanyam.iris.Color
import com.vivekpanyam.iris.Palette
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO

object Tmdb {

    private val TMDB_LANGUAGE = "es-ES"
    private val tmdbApi = TmdbApi(System.getenv("TMDB_API_KEY"))

    fun fillMovieData(cinemas: List<Cinema>) {
        cinemas.forEach { cinema ->
            cinema.movies?.forEach { movie ->
                if (!fillDataWithExistingMovie(cinemas, movie)) {
                    if (!fillDataWithExternalApi(movie) || StringUtils.isEmpty(movie.overview)) {
                        Parser.fillDataWithOriginalWeb(movie)
                    }

                    fillColors(movie)
                }
            }
        }
    }

    private fun fillDataWithExistingMovie(cinemas: List<Cinema>, originalMovie: Movie): Boolean {
        cinemas.forEach { cinema ->
            cinema.movies?.forEach { movie ->
                if (movie.id == originalMovie.id && !movie.overview.isNullOrBlank()) {
                    originalMovie.copy(movie)
                    return true
                }
            }
        }
        return false
    }

    private fun fillDataWithExternalApi(movie: Movie): Boolean {
        var movieResults = tmdbApi.search.searchMovie(movie.title, Main.NOW.year, TMDB_LANGUAGE, true, 0)
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
                    movie.images?.put(Movie.Images.POSTER.name, "http://image.tmdb.org/t/p/w780" + movieApi.posterPath)
                    movie.images?.put(Movie.Images.POSTER_THUMBNAIL.name, "http://image.tmdb.org/t/p/w92" + movieApi.posterPath)
                }
                if (!StringUtils.isEmpty(movieApi.backdropPath)) {
                    movie.images?.put(Movie.Images.BACKDROP.name, "http://image.tmdb.org/t/p/w780" + movieApi.backdropPath)
                    movie.images?.put(Movie.Images.BACKDROP_THUMBNAIL.name, "http://image.tmdb.org/t/p/w300" + movieApi.backdropPath)
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
        movie.colors = getMovieColorsFromUrl(url)
    }

    private fun getMovieColorsFromUrl(url: String?): HashMap<String, String>? {
        try {
            val palette = Palette.Builder(Bitmap(ImageIO.read(URL(url)))).generate()
            if (palette != null) {
                var swatch: Palette.Swatch? = palette.vibrantSwatch
                if (swatch == null) swatch = palette.mutedSwatch
                if (swatch == null) return null

                val colors = HashMap<String, String>()
                colors.put(Movie.Colors.MAIN.name, formatColor(swatch.rgb))
                colors.put(Movie.Colors.TITLE.name, formatColor(swatch.titleTextColor))
                return colors
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun formatColor(color: Int): String = String.format("#%02x%02x%02x", Color.red(color), Color.green(color), Color.blue(color))
}
