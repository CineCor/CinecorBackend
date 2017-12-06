package com.cinecor.backend.model

data class Session(val id: String,
                   val cinemaId: String,
                   val movieId: String,
                   var hours: HashMap<String, List<String>>) {

    enum class HoursType { NORMAL, VOSE, THREEDIM }
}
