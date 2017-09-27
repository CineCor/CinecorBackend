package com.cinecor.backend.model.dto

import com.cinecor.backend.model.Cinema
import com.cinecor.backend.model.Movie

data class BillboardDto(val billboard: List<CinemaDto>,
                        val cinemas: List<Cinema>,
                        val movies: List<Movie>)
