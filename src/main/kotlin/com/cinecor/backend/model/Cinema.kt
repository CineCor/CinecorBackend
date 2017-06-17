package com.cinecor.backend.model

data class Cinema(var id: Int? = 0,
                  var name: String? = null,
                  var image: String? = null,
                  var address: String? = null,
                  var rooms: String? = null,
                  var phone: String? = null,
                  var web: String? = null,
                  var movies: List<Movie>? = ArrayList()) : Comparable<Cinema> {

    override fun compareTo(other: Cinema): Int
            = compareValuesBy(this, other, { it.name?.contains("EL TABLERO") }, { it.name?.contains("GUADALQUIVIR") }, { it.id })
}

