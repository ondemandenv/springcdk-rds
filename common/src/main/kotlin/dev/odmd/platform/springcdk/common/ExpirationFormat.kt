package dev.odmd.platform.springcdk.common

import java.time.YearMonth
import java.time.format.DateTimeFormatter


object ExpirationFormat {
    fun twoDigitMonthAndYear(year: Int, month: Int) =
        YearMonth.of(year, month).twoDigitMonthYearString()

    val twoDigitMonthYearFormatter = DateTimeFormatter.ofPattern("MMyy")
}

fun YearMonth.twoDigitMonthYearString() =
    format(ExpirationFormat.twoDigitMonthYearFormatter)
