package com.cinecor.backend.firebase.model

data class CinemaDto(val id: String,
                     var name: String,
                     var movies: List<MovieDto>) : Comparable<CinemaDto> {

    override fun compareTo(other: CinemaDto): Int
            = compareValuesBy(this, other, { !it.name.contains("EL TABLERO") }, { !it.name.contains("GUADALQUIVIR") }, { it.id })

}
