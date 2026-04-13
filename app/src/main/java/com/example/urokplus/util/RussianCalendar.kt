package com.example.urokplus.util

import java.time.LocalDate

private val fixedHolidays = setOf(
    "01-01", "01-02", "01-03", "01-04", "01-05", "01-06", "01-07", "01-08",
    "02-23",
    "03-08",
    "05-01",
    "05-09",
    "06-12",
    "11-04"
)

fun isRussianWeekendOrHoliday(date: LocalDate): Boolean {
    if (date.dayOfWeek.value >= 6) return true
    val key = "%02d-%02d".format(date.monthValue, date.dayOfMonth)
    return key in fixedHolidays
}

fun russianNoLessonsReason(date: LocalDate): String? {
    return when {
        date.dayOfWeek.value >= 6 -> "Выходной день — уроков нет"
        isRussianWeekendOrHoliday(date) -> "Государственный праздник РФ — уроков нет"
        else -> null
    }
}
