package com.cinecor.backend.model

import com.google.cloud.firestore.annotation.Exclude

data class Movie(
        val id: String = "",
        var title: String = "",
        @get:Exclude var originalUrl: String = "",
        var imagePoster: String = "",
        var imageBackdrop: String? = null,
        var colors: Colors = Colors(),
        var overview: String = "",
        var imdbId: String? = null,
        var director: String? = null,
        var rating: String? = null,
        var duration: Int? = null,
        var trailer: String? = null,
        var releaseDate: String? = null,
        var genres: List<String>? = null,
        var raw: String? = null,
        @get:Exclude var year: Int? = null,
        @get:Exclude var originalTitle: String? = null
) {

    fun copy(movie: Movie) {
        movie.title.let { this.title = it }
        movie.originalUrl.let { this.originalUrl = it }
        movie.imagePoster.let { this.imagePoster = it }
        movie.imageBackdrop.let { this.imageBackdrop = it }
        movie.colors.let { this.colors = it }
        movie.overview.let { this.overview = it }
        movie.imdbId?.let { this.imdbId = it }
        movie.director?.let { this.director = it }
        movie.rating?.let { this.rating = it }
        movie.duration?.let { this.duration = it }
        movie.trailer?.let { this.trailer = it }
        movie.releaseDate?.let { this.releaseDate = it }
        movie.genres?.let { this.genres = it }
        movie.raw?.let { this.raw = it }
    }

    class Colors(
            val main: String = "",
            val titleText: String = "",
            val bodyText: String = ""
    )
}
