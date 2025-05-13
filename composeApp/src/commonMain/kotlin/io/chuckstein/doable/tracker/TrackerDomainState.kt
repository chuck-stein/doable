package io.chuckstein.doable.tracker

import doable.composeapp.generated.resources.Res
import doable.composeapp.generated.resources.failed_to_load_error
import doable.composeapp.generated.resources.failed_to_save_error
import io.chuckstein.doable.common.today
import io.chuckstein.doable.database.Habit
import io.chuckstein.doable.database.JournalEntry
import io.chuckstein.doable.database.Task
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

data class TrackerDomainState(
    val trackedDays: List<LocalDate> = emptyList(),
    val focusedDay: LocalDate = today(),
    val dayDetailsMap: Map<LocalDate, DayDetails> = emptyMap(),
    val tasks: List<Task> = emptyList(),
    val taskIdToFocus: Long? = null,
    val habitIdToFocus: Long? = null,
    val taskEditingState: TaskEditingState? = null,
    val pendingChanges: Set<PendingChange> = emptySet(),
    val isSelectingDate: Boolean = false,
    val isLoading: Boolean = true,
    val error: TrackerError? = null
) {
    val focusedDayDetails = dayDetailsMap[focusedDay] ?: DayDetails.error
    val latestTrackedDay = trackedDays.lastOrNull() ?: today()
}

sealed interface TrackerError {
    val message: StringResource

    data object FailedToSave : TrackerError {
        override val message: StringResource = Res.string.failed_to_save_error
    }
    data object FailedToLoad : TrackerError {
        override val message: StringResource = Res.string.failed_to_load_error
    }
}

data class DayDetails(
    val journalEntry: JournalEntry,
    val journalTaskIds: List<Long> = emptyList(),
    val journalHabitIds: List<Long> = emptyList(),
    val trackedHabits: List<TrackedHabit> = emptyList(),
    val untrackedHabits: List<Habit> = emptyList(),
    val viewingUntrackedHabits: Boolean = false,
    val viewingOlderTasks: Boolean = false,
    val error: TrackerError? = null
) {
    fun journalTaskIds(allTasks: List<Task>) = journalTaskIds.mapNotNull { id -> allTasks.find { it.id == id } }
    fun journalHabitIds() = journalHabitIds.mapNotNull { id -> trackedHabits.find { it.id == id } }

    companion object {
        val error = DayDetails(
            journalEntry = JournalEntry(
                date = LocalDate.fromEpochDays(0),
                note = "",
                isStarred = false,
                habitsCalculated = false
            ),
            error = TrackerError.FailedToLoad,
        )
    }
}

sealed interface PendingChange {
    data class Habit(val id: Long) : PendingChange
    data class Task(val id: Long) : PendingChange
}

data class TrackedHabit(
    val id: Long,
    val name: String,
    val frequency: HabitFrequency,
    val trend: HabitTrend,
    val wasBuilding: Boolean,
    val wasPerformed: Boolean,
    val isNew: Boolean,
)

enum class HabitFrequency(val serializedName: String) {
    Daily("DAILY"), Weekly("WEEKLY"), Monthly("MONTHLY"), None("NONE")
}

enum class HabitTrend(val serializedName: String) {
    Up("UP"), Down("DOWN"), Neutral("NEUTRAL"), None("NONE")
}

enum class TaskPriority(val serializedName: String) {
    Low("LOW"), Medium("MEDIUM"), High("HIGH")
}

data class TaskEditingState(
    val taskId: Long,
    val isEditingPriority: Boolean = false,
    val isEditingDeadline: Boolean = false
)
