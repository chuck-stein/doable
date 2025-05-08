package io.chuckstein.doable.common

import io.chuckstein.doable.database.Task
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

fun Task.isCompletedAsOf(date: LocalDate) = dateCompleted != null && dateCompleted <= date

fun Task.isOlderAsOf(date: LocalDate) = isCompletedAsOf(date.previousDay())

val taskUrgencyComparator = compareByDescending(nullsLast(), Task::dateCompleted)
    .thenBy(nullsLast()) { it.deadline?.takeUnlessOverAWeekFromNow() }
    .thenByDescending { it.priority }
    .thenBy(nullsLast(), Task::deadline)

private fun LocalDate.takeUnlessOverAWeekFromNow() = takeUnless { it >= today().plus(7, DAY) }