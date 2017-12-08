package com.cinecor.backend.model

data class Session(val id: String,
                   val cinemaId: String,
                   val movieId: String,
                   var hours: HashMap<String, List<String>>,
                   var movieTitle: String = "",
                   var movieImages: HashMap<String, String> = HashMap()) {

    enum class HoursType { NORMAL, VOSE, THREEDIM }
}
