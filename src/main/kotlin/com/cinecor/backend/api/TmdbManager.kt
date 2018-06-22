package com.cinecor.backend.api

import com.cinecor.backend.Main.NOW
import com.cinecor.backend.db.FirebaseManager
import com.cinecor.backend.model.Billboard
import com.cinecor.backend.model.Movie
import com.cinecor.backend.parser.JsoupManager.fillBasicDataWithOriginalSource
import com.vivekpanyam.iris.Bitmap
import com.vivekpanyam.iris.Color
import com.vivekpanyam.iris.Palette
import info.movito.themoviedbapi.TmdbApi
import info.movito.themoviedbapi.TmdbMovies
import info.movito.themoviedbapi.model.MovieDb
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

object TmdbManager {

    private const val TMDB_LANGUAGE = "es-ES"
    private val tmdbApi = TmdbApi(System.getenv("TMDB_API_KEY"))

    fun fillMoviesData(billboardData: Billboard, remoteMovies: List<Movie>) {
        billboardData.movies.forEach { movie ->
            if (!movie.fillDataWithExistingMovies(billboardData.movies, remoteMovies)) {
                movie.fillBasicDataWithOriginalSource()
                movie.fillDataWithExternalApi()
                movie.fillColorsAndUploadImage()
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

    private fun Movie.fillDataWithExternalApi() {
        println("\t\t Filling data with external API...")

        val foundMovie = searchMovie(originalTitle, year)
                ?: searchMovie(title, year)
                ?: searchMovie(originalTitle, NOW.year)
                ?: searchMovie(title, NOW.year)
                ?: searchMovie(originalTitle, 0)
                ?: searchMovie(title, 0)

        foundMovie?.fetch()?.let { fetchedMovie ->
            copy(fetchedMovie)
        } ?: run {
            System.err.println("\t\t ERROR Filling data from `$originalUrl`")
        }
    }

    private fun Movie.fillColorsAndUploadImage() {
        if (imagePoster.isEmpty()) return

        var bufferedImage: BufferedImage? = null
        try {
            bufferedImage = ImageIO.read(URL(imagePoster))
            Palette.Builder(Bitmap(bufferedImage)).generate()?.let { palette ->
                val swatch = palette.vibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
                colors = Movie.Colors(
                        swatch.rgb.formattedColor(),
                        swatch.titleTextColor.formattedColor(),
                        swatch.bodyTextColor.formattedColor()
                )
            }
        } catch (e: Exception) {
            System.err.println("\t\t ERROR Uploading: ${this}")
            e.printStackTrace()
        } finally {
            imagePoster = FirebaseManager.uploadImage(bufferedImage, imagePoster, "movies/$id/poster.jpg")
            imageBackdrop = imageBackdrop?.let { FirebaseManager.uploadImage(imageUrl = it, imagePath = "movies/$id/backdrop.jpg") }
        }
    }

    @Throws(IndexOutOfBoundsException::class)
    private fun searchMovie(title: String?, year: Int?): Int? {
        if (title.isNullOrBlank() || year == null) return null
        return tmdbApi.search.searchMovie(title, year, TMDB_LANGUAGE, true, 0)?.results?.firstOrNull()?.id
    }

    private fun Int.fetch(): MovieDb? =
            tmdbApi.movies.getMovie(this, TMDB_LANGUAGE, TmdbMovies.MovieMethod.videos, TmdbMovies.MovieMethod.credits)

    private fun Int.formattedColor() = String.format("#%02x%02x%02x%02x", Color.red(this), Color.green(this), Color.blue(this), Color.alpha(this))

    private fun Movie.copy(movie: MovieDb) {
        if (!movie.title.isNullOrBlank()) this.title = movie.title
        if (!movie.imdbID.isNullOrBlank()) this.imdbId = movie.imdbID
        if (!movie.overview.isNullOrBlank()) this.overview = movie.overview
        if (!movie.releaseDate.isNullOrBlank()) this.releaseDate = movie.releaseDate
        if (!movie.posterPath.isNullOrBlank()) this.imagePoster = "https://image.tmdb.org/t/p/original${movie.posterPath}"
        if (!movie.backdropPath.isNullOrBlank()) this.imageBackdrop = "https://image.tmdb.org/t/p/original${movie.backdropPath}"
        if (movie.runtime != 0) this.duration = movie.runtime
        if (movie.voteAverage != 0.0f) this.rating = "%.2f".format(movie.voteAverage)
        movie.genres?.take(4)?.map { it.name }?.let { this.genres = it }
        movie.credits?.crew?.firstOrNull { it.job == "Director" }?.name?.let { this.director = it }
        movie.videos?.find { it.type == "Trailer" && it.site == "YouTube" }?.key?.let { this.trailer = "https://www.youtube.com/watch?v=$it" }
    }
}
