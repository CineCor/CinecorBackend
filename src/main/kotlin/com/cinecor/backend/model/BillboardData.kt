package com.cinecor.backend.model

import com.cinecor.backend.firebase.model.CinemaDto

data class BillboardData(val billboard: List<CinemaDto>,
                         val cinemas: List<Cinema>,
                         val movies: List<Movie>)
