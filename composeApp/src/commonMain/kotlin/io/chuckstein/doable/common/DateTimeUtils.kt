package io.chuckstein.doable.common

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

fun today() = Clock.System.todayIn(TimeZone.currentSystemDefault())
fun yesterday() = today().previousDay()

fun LocalDate.nextDay() = plus(1, DAY)
fun LocalDate.previousDay() = minus(1, DAY)

fun LocalDate.previousDays(numDays: Int) = List(numDays) { index ->
    this.minus(numDays, DAY).plus(index, DAY)
}
