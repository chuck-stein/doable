package io.chuckstein.doable.common

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format.Padding
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

fun currentDateTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun today() = Clock.System.todayIn(TimeZone.currentSystemDefault())
fun yesterday() = today().previousDay()

fun LocalDate.nextDay() = plus(1, DAY)
fun LocalDate.previousDay() = minus(1, DAY)

fun LocalDate.previousDays(numDays: Int) = List(numDays) { index ->
    this.minus(numDays, DAY).plus(index, DAY)
}

fun LocalDate.previousDaysInclusive(numDays: Int) = List(numDays) { index ->
    this.minus(numDays, DAY).plus(index + 1, DAY)
}

fun LocalDate.weeksUntil(other: LocalDate): Int = daysUntil(other) / 7
fun LocalDate.monthsUntil(other: LocalDate): Int = daysUntil(other) / AVG_DAYS_IN_MONTH

fun avgNumDaysBetweenDates(dates: List<LocalDate>) = dates
    .sorted()
    .zipWithNext { date1, date2 -> date1.daysUntil(date2) }
    .average()


operator fun OpenEndRange<LocalDate>.iterator() = generateSequence(start) { it.nextDay() }
    .takeWhile { it < endExclusive }
    .iterator()

fun OpenEndRange<LocalDate>.asList() = iterator().asSequence().toList()

// TODO: i18n
val shortDateFormatter = LocalDate.Format {
    monthNumber(Padding.NONE)
    chars("/")
    dayOfMonth(Padding.NONE)
    chars("/")
    year()
}

const val AVG_DAYS_IN_MONTH = 30
