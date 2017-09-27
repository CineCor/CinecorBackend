package com.cinecor.backend.model

import com.google.firebase.database.Exclude
import info.movito.themoviedbapi.model.MovieDb

data class Movie(val id: String,
                 var title: String,
                 var is3d: Boolean,
                 var isVose: Boolean,
                 @Exclude var url: String,
                 var images: HashMap<String, String> = HashMap(),
                 var colors: HashMap<String, String> = HashMap(),
                 var overview: String = "",
                 var imdb: String? = null,
                 var rating: Float? = null,
                 var duration: Int? = null,
                 var trailer: String? = null,
                 var releaseDate: String? = null,
                 var genres: List<String>? = null,
                 var rawDescription: String? = null) {

    enum class Images {POSTER, POSTER_THUMBNAIL, BACKDROP, BACKDROP_THUMBNAIL }
    enum class Colors {MAIN, TITLE, BODY }

    fun copy(movie: Movie) {
        movie.title.let { this.title = it }
        movie.url.let { this.url = it }
        movie.images.let { this.images = it }
        movie.colors.let { this.colors = it }
        movie.overview.let { this.overview = it }
        movie.imdb?.let { this.imdb = it }
        movie.rating?.let { this.rating = it }
        movie.duration?.let { this.duration = it }
        movie.trailer?.let { this.trailer = it }
        movie.releaseDate?.let { this.releaseDate = it }
        movie.genres?.let { this.genres = it }
        movie.rawDescription?.let { this.rawDescription = it }
    }

    fun copy(movieDb: MovieDb) {
        movieDb.title?.let { this.title = it }
        movieDb.overview?.let { this.overview = it }
        movieDb.imdbID?.let { this.imdb = "http://www.imdb.com/title/" + it }
        movieDb.voteAverage.let { this.rating = it }
        movieDb.runtime.let { this.duration = it }
        movieDb.releaseDate?.let { this.releaseDate = it }
        movieDb.genres?.let { this.genres = it.map { it.name } }
        movieDb.videos?.let { videos ->
            if (videos.isNotEmpty()) {
                videos.find { it.type == "Trailer" && it.site == "YouTube" }?.key.let {
                    this.trailer = "https://www.youtube.com/watch?v=" + it
                }
            }
        }
        movieDb.posterPath?.let { path ->
            if (path.isNotBlank()) {
                this.images.put(Movie.Images.POSTER.name, "http://image.tmdb.org/t/p/w780" + path)
                this.images.put(Movie.Images.POSTER_THUMBNAIL.name, "http://image.tmdb.org/t/p/w92" + path)
            }
        }
        movieDb.backdropPath?.let { path ->
            if (path.isNotBlank()) {
                this.images.put(Movie.Images.BACKDROP.name, "http://image.tmdb.org/t/p/w780" + path)
                this.images.put(Movie.Images.BACKDROP_THUMBNAIL.name, "http://image.tmdb.org/t/p/w300" + path)
            }
        }
    }
}
