package com.cinecor.backend.model

import com.cinecor.backend.db.FirebaseManager
import com.google.cloud.firestore.annotation.Exclude
import info.movito.themoviedbapi.model.MovieDb

data class Movie(
        val id: String = "",
        var title: String = "",
        @get:Exclude var originalUrl: String = "",
        var imagePoster: String = "",
        var imageBackdrop: String? = null,
        var colors: HashMap<String, String> = HashMap(),
        var overview: String = "",
        var imdbId: String? = null,
        var rating: String? = null,
        var duration: Int? = null,
        var trailer: String? = null,
        var releaseDate: String? = null,
        var genres: List<String>? = null,
        var raw: String? = null,
        @get:Exclude var year: Int? = null,
        @get:Exclude var originalTitle: String? = null
) {

    enum class Colors { MAIN, TITLE, BODY }

    fun uploadPosterImageUrl(imageUrl: String) {
        imagePoster = FirebaseManager.uploadImage(imageUrl, "movies/$id/poster")
    }

    fun uploadBackdropImageUrl(imageUrl: String) {
        imageBackdrop = FirebaseManager.uploadImage(imageUrl, "movies/$id/backdrop")
    }

    fun copy(movie: Movie) {
        movie.title.let { this.title = it }
        movie.originalUrl.let { this.originalUrl = it }
        movie.imagePoster.let { this.imagePoster = it }
        movie.imageBackdrop.let { this.imageBackdrop = it }
        movie.colors.let { this.colors = it }
        movie.overview.let { this.overview = it }
        movie.imdbId?.let { this.imdbId = it }
        movie.rating?.let { this.rating = it }
        movie.duration?.let { this.duration = it }
        movie.trailer?.let { this.trailer = it }
        movie.releaseDate?.let { this.releaseDate = it }
        movie.genres?.let { this.genres = it }
        movie.raw?.let { this.raw = it }
    }

    fun copy(movieDb: MovieDb) {
        movieDb.title?.let { this.title = it }
        movieDb.overview?.let { this.overview = it }
        movieDb.imdbID?.let { this.imdbId = it }
        movieDb.voteAverage.let { this.rating = "%.2f".format(it) }
        movieDb.runtime.let { this.duration = it }
        movieDb.releaseDate?.let { this.releaseDate = it }
        movieDb.genres?.let { this.genres = it.map { it.name } }
        movieDb.videos?.find { it.type == "Trailer" && it.site == "YouTube" }?.key.let {
            this.trailer = "https://www.youtube.com/watch?v=$it"
        }
        if (movieDb.posterPath.isNullOrBlank()) {
            uploadPosterImageUrl(imagePoster)
        } else {
            uploadPosterImageUrl("http://image.tmdb.org/t/p/original${movieDb.posterPath}")
        }
        if (!movieDb.backdropPath.isNullOrBlank()) {
            uploadBackdropImageUrl("http://image.tmdb.org/t/p/original${movieDb.backdropPath}")
        }
    }
}
