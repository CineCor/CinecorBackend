package com.cinecor.backend.model

data class Billboard(val billboard: List<CinemaDto>,
                     val cinemas: List<Cinema>,
                     val movies: List<Movie>)
