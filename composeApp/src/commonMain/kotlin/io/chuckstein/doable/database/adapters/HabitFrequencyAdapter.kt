package io.chuckstein.doable.database.adapters

import app.cash.sqldelight.ColumnAdapter
import io.chuckstein.doable.tracker.HabitFrequency

val habitFrequencyAdapter = object : ColumnAdapter<HabitFrequency, String> {

    override fun decode(databaseValue: String): HabitFrequency {
        return HabitFrequency.entries.find { it.serializedName == databaseValue } ?: HabitFrequency.None
    }

    override fun encode(value: HabitFrequency) = value.serializedName
}