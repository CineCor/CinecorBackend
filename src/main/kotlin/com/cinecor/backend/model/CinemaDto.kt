package com.cinecor.backend.model

data class CinemaDto(val id: Int,
                     var name: String,
                     var movies: List<MovieDto>)
