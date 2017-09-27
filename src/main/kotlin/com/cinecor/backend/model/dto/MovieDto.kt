package com.cinecor.backend.model.dto

data class MovieDto(val id: String,
                    var title: String,
                    var hours: List<String>,
                    var is3d: Boolean,
                    var isVose: Boolean,
                    var url: String,
                    var images: HashMap<String, String> = HashMap(),
                    var colors: HashMap<String, String> = HashMap())
