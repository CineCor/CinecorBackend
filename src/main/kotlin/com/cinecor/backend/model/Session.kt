package com.cinecor.backend.model

data class Session(val id: String,
                   val cinemaId: String,
                   val movieId: String,
                   val hours: List<String>,
                   var is3D: Boolean,
                   var isVose: Boolean)
