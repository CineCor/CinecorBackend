package com.cinecor.backend.model

data class MovieDto(val id: Int,
                    var title: String,
                    var hours: List<String>,
                    var images: Map<String, String> = HashMap(),
                    var colors: Map<String, String> = HashMap())
