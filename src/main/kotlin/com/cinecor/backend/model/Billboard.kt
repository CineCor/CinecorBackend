package com.cinecor.backend.model

data class Billboard(val billboard: List<CinemaDto>,
                     val cinemas: Set<Cinema>,
                     val movies: Set<Movie>)
