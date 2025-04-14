package io.chuckstein.doable.database.adapters

import app.cash.sqldelight.ColumnAdapter
import io.chuckstein.doable.tracker.TaskPriority

val taskPriorityAdapter = object : ColumnAdapter<TaskPriority, String> {

    override fun decode(databaseValue: String): TaskPriority {
        return TaskPriority.entries.find { it.serializedName == databaseValue } ?: TaskPriority.Medium
    }

    override fun encode(value: TaskPriority) = value.serializedName
}