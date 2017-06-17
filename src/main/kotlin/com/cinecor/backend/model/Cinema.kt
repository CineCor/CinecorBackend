package com.cinecor.backend.model

data class Cinema(var id: Int = 0,
                  var name: String = "",
                  var image: String = "",
                  var address: String = "",
                  var rooms: String = "",
                  var phone: String = "",
                  var web: String = "",
                  var movies: List<Movie> = ArrayList()) : Comparable<Cinema> {

    override fun compareTo(other: Cinema): Int
            = compareValuesBy(this, other, { !it.name.contains("EL TABLERO") }, { !it.name.contains("GUADALQUIVIR") }, { it.id })
}
