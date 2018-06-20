package com.cinecor.backend.model

data class Billboard(
        var cinemas: MutableSet<Cinema> = mutableSetOf(),
        var movies: MutableSet<Movie> = mutableSetOf(),
        var sessions: MutableSet<Session> = mutableSetOf()
)
