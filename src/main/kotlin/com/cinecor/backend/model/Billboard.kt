package com.cinecor.backend.model

data class Billboard(var cinemas: MutableSet<Cinema>,
                     var movies: MutableSet<Movie>,
                     var sessions: MutableSet<Session>)
