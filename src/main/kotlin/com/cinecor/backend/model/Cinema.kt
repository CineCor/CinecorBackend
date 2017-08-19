package com.cinecor.backend.model

data class Cinema(val id: Int,
                  var name: String,
                  var movies: List<Int>,
                  var address: String? = null,
                  var image: String? = null,
                  var rooms: String? = null,
                  var phone: String? = null,
                  var web: String? = null) : Comparable<Cinema> {

    override fun compareTo(other: Cinema): Int
            = compareValuesBy(this, other, { !it.name.contains("EL TABLERO") }, { !it.name.contains("GUADALQUIVIR") }, { it.id })
}
