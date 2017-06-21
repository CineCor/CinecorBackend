package com.cinecor.backend.model

import com.google.firebase.database.Exclude
import info.movito.themoviedbapi.model.MovieDb

data class Movie(val id: Int,
                 var title: String,
                 var hours: List<String>,
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
    enum class Colors {MAIN, TITLE }

    fun copy(movie: Movie) {
        movie.title.let { this.title = it }
        movie.url.let { this.url = it }
        movie.images.let { this.images = it }
        movie.colors.let { this.colors = it }
        movie.overview.let { this.overview = it }
        movie.imdb.let { this.imdb = it }
        movie.rating.let { this.rating = it }
        movie.duration.let { this.duration = it }
        movie.trailer.let { this.trailer = it }
        movie.releaseDate.let { this.releaseDate = it }
        movie.genres.let { this.genres = it }
        movie.rawDescription.let { this.rawDescription = it }
    }

    fun copy(movieApi: MovieDb) {
        movieApi.title?.let { this.title = it }
        movieApi.overview?.let { this.overview = it }
        movieApi.imdbID?.let { this.imdb = "http://www.imdb.com/title/" + it }
        movieApi.voteAverage.let { this.rating = it }
        movieApi.runtime.let { this.duration = it }
        movieApi.releaseDate?.let { this.releaseDate = it }
        movieApi.genres?.let { this.genres = it.map { it.name } }
        movieApi.videos?.let { videos ->
            if (videos.isNotEmpty()) {
                videos.find { it.type == "Trailer" && it.site == "YouTube" }?.key.let {
                    this.trailer = "https://www.youtube.com/watch?v=" + it
                }
            }
        }
        movieApi.posterPath?.let { path ->
            if (path.isNotBlank()) {
                this.images.put(Movie.Images.POSTER.name, "http://image.tmdb.org/t/p/w780" + path)
                this.images.put(Movie.Images.POSTER_THUMBNAIL.name, "http://image.tmdb.org/t/p/w92" + path)
            }
        }
        movieApi.backdropPath?.let { path ->
            if (path.isNotBlank()) {
                this.images.put(Movie.Images.BACKDROP.name, "http://image.tmdb.org/t/p/w780" + path)
                this.images.put(Movie.Images.BACKDROP_THUMBNAIL.name, "http://image.tmdb.org/t/p/w300" + path)
            }
        }
    }
}
