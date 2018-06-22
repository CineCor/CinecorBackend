package com.cinecor.backend.utils

import java.time.format.DateTimeFormatter

class DateUtils {

    companion object {
        val DATE_FORMAT_FULL_SIMPLE = DateTimeFormatter.ofPattern("YYYYMMdd")
        val DATE_FORMAT_FULL = DateTimeFormatter.ofPattern("YYYY-MM-dd")
        val DATE_FORMAT_HOUR = DateTimeFormatter.ofPattern("HHmm")
    }
}
