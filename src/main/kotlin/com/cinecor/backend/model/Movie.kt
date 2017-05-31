package com.cinecor.backend.model

data class Movie(
        var id: Int? = 0,
        var colors: HashMap<String, String>? = null,
        var images: HashMap<String, String>? = null,
        var hours: List<String>? = null,
        var genres: List<String>? = null,
        var rawDescription: String? = null,
        var imdb: String? = null,
        var trailer: String? = null,
        var duration: Int? = 0,
        var releaseDate: String? = null,
        var overview: String? = null,
        var director: String? = null,
        var url: String? = null,
        var rating: Float? = 0.0f,
        var title: String? = null) {

    enum class Images {POSTER, POSTER_THUMBNAIL, BACKDROP, BACKDROP_THUMBNAIL }
    enum class Colors {MAIN, TITLE }
}
