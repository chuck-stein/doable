package io.chuckstein.doable.common

import io.chuckstein.doable.database.Task
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

fun Task.isCompletedAsOf(date: LocalDate) = dateCompleted != null && dateCompleted <= date

fun Task.isOlderAsOf(date: LocalDate) = isCompletedAsOf(date.previousDay())

fun Task.isDueThisWeekAsOf(date: LocalDate) = deadline != null && deadline in date ..< date.plus(7, DAY)

fun Task.isOverdueAsOf(date: LocalDate) = deadline != null && deadline < date && !isCompletedAsOf(date)

fun Task.wasOverdueButCompletedOn(date: LocalDate) = deadline != null && deadline < date && dateCompleted == date

fun taskUrgencyComparator(date: LocalDate) = compareByDescending(nullsLast(), Task::dateCompleted)
    .thenBy(nullsLast()) { it.deadline?.takeUnlessOverAWeekFrom(date) }
    .thenByDescending { it.priority }
    .thenBy(nullsLast(), Task::deadline)

private fun LocalDate.takeUnlessOverAWeekFrom(date: LocalDate) = takeUnless { it >= date.plus(7, DAY) }