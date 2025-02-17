package io.chuckstein.doable.database.adapters

import app.cash.sqldelight.ColumnAdapter
import io.chuckstein.doable.tracker.HabitTrend

val habitTrendAdapter = object : ColumnAdapter<HabitTrend, String> {

    override fun decode(databaseValue: String): HabitTrend {
        return HabitTrend.entries.find { it.serializedName == databaseValue } ?: HabitTrend.None
    }

    override fun encode(value: HabitTrend) = value.serializedName
}