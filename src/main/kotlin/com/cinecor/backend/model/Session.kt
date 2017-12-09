package com.cinecor.backend.model

data class Session(val id: String = "",
                   val cinemaId: String = "",
                   val movieId: String = "",
                   val date: String = "",
                   var hours: HashMap<String, List<String>> = HashMap(),
                   var movieTitle: String = "",
                   var movieImages: HashMap<String, String> = HashMap()) {

    enum class HoursType { NORMAL, VOSE, THREEDIM }
}
