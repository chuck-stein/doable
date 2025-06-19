package io.chuckstein.doable.tracker

import androidx.compose.ui.graphics.lerp
import doable.composeapp.generated.resources.Res
import doable.composeapp.generated.resources.amazing
import doable.composeapp.generated.resources.bad
import doable.composeapp.generated.resources.failed_to_load_error
import doable.composeapp.generated.resources.failed_to_save_error
import doable.composeapp.generated.resources.good
import doable.composeapp.generated.resources.neutral
import doable.composeapp.generated.resources.terrible
import io.chuckstein.doable.common.ColorModel
import io.chuckstein.doable.common.today
import io.chuckstein.doable.database.Habit
import io.chuckstein.doable.database.JournalEntry
import io.chuckstein.doable.database.Task
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import kotlin.math.pow

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
                habitsCalculated = false,
                mood = null
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
    val lastPerformed: LocalDate?,
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

enum class Mood(val positivityScore: Long, val stringRes: StringResource) {
    Terrible(1, Res.string.terrible),
    Bad(2, Res.string.bad),
    Neutral(3, Res.string.neutral),
    Good(4, Res.string.good),
    Amazing(5, Res.string.amazing);

    private val colorBias = 0.5

    private val positivityPercent by lazy {
        val minPositivityScore = entries.minOf { it.positivityScore }
        val maxPositivityScore = entries.maxOf { it.positivityScore }
        val positivityScoreRange = maxPositivityScore - minPositivityScore
        (positivityScore - minPositivityScore).toDouble() / positivityScoreRange.toDouble()
    }

    private val colorLerpAmount by lazy {
        positivityPercent.pow(colorBias).toFloat()
    }

    val color = ColorModel.FromTheme { lerp(error, primary, colorLerpAmount) }
    val containerColor = ColorModel.FromTheme { lerp(errorContainer, primaryContainer, colorLerpAmount) }
}

data class TaskEditingState(
    val taskId: Long,
    val isEditingPriority: Boolean = false,
    val isEditingDeadline: Boolean = false
)
