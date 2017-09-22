package com.cinecor.backend.model

data class Cinema(val id: Int,
                  var name: String,
                  var movies: List<MovieDto>,
                  var address: String? = null,
                  var image: String? = null,
                  var rooms: String? = null,
                  var phone: String? = null,
                  var web: String? = null)
