package com.cinecor.backend.utils

import com.cinecor.backend.Main
import com.google.common.base.CharMatcher
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DateUtils {

    val DATE_FORMAT_SIMPLE = DateTimeFormatter.ofPattern("YYYYMMdd")
    val DATE_FORMAT_HOUR = DateTimeFormatter.ofPattern("HHmm")

    fun getFormattedDatesFromHoursText(hours: String): List<String> {
        return hours
                .split("-".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
                .map { CharMatcher.digit().retainFrom(it) }
                .filter { it.length >= 4 }
                .map { it.substring(0, 4) }
                .map { LocalTime.parse(it, DATE_FORMAT_HOUR) }
                .map { Main.NOW.plusDays(if (it.hour < 8) 1 else 0).withHour(it.hour).withMinute(it.minute) }
                .filter { it.isAfter(Main.NOW) }
                .map { DateTimeFormatter.ISO_INSTANT.format(it) }
    }

    fun getFormattedYearFromRawText(rawText: String): Int? {
        if (!rawText.contains("Año")) return null
        return rawText
                .split("<br>")
                .find { it.contains("Año") }!!
                .split("</b>")[1]
                .trim()
                .toInt()
    }
}
