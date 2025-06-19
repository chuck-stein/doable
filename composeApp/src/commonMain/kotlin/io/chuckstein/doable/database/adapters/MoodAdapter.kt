package io.chuckstein.doable.database.adapters

import app.cash.sqldelight.ColumnAdapter
import io.chuckstein.doable.tracker.Mood

val moodAdapter = object : ColumnAdapter<Mood, Long> {

    override fun decode(databaseValue: Long): Mood {
        return Mood.entries.find { it.positivityScore == databaseValue } ?: Mood.Neutral
    }

    override fun encode(value: Mood) = value.positivityScore
}