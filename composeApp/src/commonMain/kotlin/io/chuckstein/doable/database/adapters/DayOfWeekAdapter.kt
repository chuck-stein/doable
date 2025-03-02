package io.chuckstein.doable.database.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.DayOfWeek

val dayOfWeekAdapter = object : ColumnAdapter<DayOfWeek, String> {

    override fun decode(databaseValue: String): DayOfWeek {
        return DbDayOfWeek.entries.first { it.serializedName == databaseValue }.kotlinDayOfWeek
    }

    override fun encode(value: DayOfWeek) = DbDayOfWeek.entries.first { it.kotlinDayOfWeek == value }.serializedName
}

private enum class DbDayOfWeek(val serializedName: String, val kotlinDayOfWeek: DayOfWeek) {
    Monday("MONDAY", DayOfWeek.MONDAY),
    Tuesday("TUESDAY", DayOfWeek.TUESDAY),
    Wednesday("WEDNESDAY", DayOfWeek.WEDNESDAY),
    Thursday("THURSDAY", DayOfWeek.THURSDAY),
    Friday("FRIDAY", DayOfWeek.FRIDAY),
    Saturday("SATURDAY", DayOfWeek.SATURDAY),
    Sunday("SUNDAY", DayOfWeek.SUNDAY)
}