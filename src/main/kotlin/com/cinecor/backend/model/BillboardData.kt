package com.cinecor.backend.model

data class BillboardData(val billboard: List<CinemaDto>,
                         val cinemas: List<Cinema>,
                         val movies: List<Movie>)
