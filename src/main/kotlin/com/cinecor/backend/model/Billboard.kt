package com.cinecor.backend.model

data class Billboard(var cinemas: List<Cinema>,
                     var movies: List<Movie>,
                     var sessions: List<Session>)
