package io.chuckstein.doable.tracker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import io.chuckstein.doable.common.ColorModel
import io.chuckstein.doable.common.IconState
import io.chuckstein.doable.common.TextModel
import io.chuckstein.doable.tracker.TrackerEvent.ChangeFocusedDay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

data class TrackerUiState(
    val header: TextModel = TextModel.empty,
    val days: List<TrackerDayState> = emptyList(),
    val initialFocusedDayIndex: Int = 0,
    val previousDayButtonEnabled: Boolean = false,
    val nextDayButtonEnabled: Boolean = false,
    val showDatePicker: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: TextModel? = null
) {
    private val trackedDates = days.map { it.date }
    val trackedDatesAsUtcMillis = trackedDates.map { it.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }
    val trackedYears = trackedDates.map { it.year }.toSet()
}

data class TrackerDayState(
    val date: LocalDate,
    val tasksTab: TasksTabState = TasksTabState(),
    val journalTab: JournalTabState = JournalTabState(),
    val habitsTab: HabitsTabState = HabitsTabState(),
    val isLoading: Boolean = true,
    val errorMessage: TextModel? = null
) {
    val onFocusEvent = ChangeFocusedDay(date)
    val dateUtcMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}

data class TasksTabState(
    val tasks: List<CheckableItemState> = emptyList(),
    val toggleViewOlderTasksButtonState: IconButtonState? = null,
    val olderTasks: List<CheckableItemState> = emptyList()
)

data class JournalTabState(
    val note: TextModel = TextModel.empty,
    val isStarred: Boolean = false,
    val selectedMoodIndex: Int? = null,
    val journalTasks: List<CheckableItemState> = emptyList(),
    val journalHabits: List<CheckableItemState> = emptyList(),
)

data class HabitsTabState(
    val trackedHabits: List<CheckableItemState> = emptyList(),
    val showAddHabitButton: Boolean = true,
    val toggleViewUntrackedHabitsButtonState: IconButtonState? = null,
    val untrackedHabits: List<CheckableItemState> = emptyList(),
) {
    val showNoHabitsTrackedMessage = trackedHabits.isEmpty() && !showAddHabitButton
}

data class IconButtonState(
    val icon: IconState,
    val text: TextModel
)

data class CheckableItemState(
    val id: Long,
    val checked: Boolean = false,
    val name: TextModel = TextModel.empty,
    val infoText: TextModel? = null,
    val infoTextColor: ColorModel = ColorModel.FromTheme { tertiary },
    val metadata: CheckableItemMetadataState = CheckableItemMetadataState.Empty,
    val endIcon: IconState? = null,
    val optionsState: CheckableItemOptionsState = CheckableItemOptionsState.Empty,
    val active: Boolean = true,
    val autoFocus: Boolean = false,
    val toggleEditingEvent: TrackerEvent? = null,
    val updateNameEvent: ((String) -> TrackerEvent)? = null,
    val toggleCheckedEvent: TrackerEvent? = null,
    val endIconClickEvent: TrackerEvent? = null,
    val loseFocusEvent: TrackerEvent? = null,
    val autoFocusDoneEvent: TrackerEvent? = null,
    val nextActionEvent: TrackerEvent? = null,
    val backspaceWhenEmptyEvent: TrackerEvent? = null,
) {
    val checkboxEnabled = toggleCheckedEvent != null
    val textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
    val textAlpha = if (checked || !active) 0.5f else 1f
}

sealed interface CheckableItemMetadataState {

    data object Empty : CheckableItemMetadataState

    data class TaskMetadataState(
        val tagsState: TagsState = TagsState(),
        val priorityIcon: IconState? = null,
    ) : CheckableItemMetadataState

    data class HabitMetadataState(
        val tagsState: TagsState = TagsState(),
        val frequency: TextModel = TextModel.empty,
        val trendIcon: IconState? = null
    ) : CheckableItemMetadataState
}

sealed interface CheckableItemOptionsState {

    val optionsShouldStayFocused: Boolean
        get() = false

    data object Empty : CheckableItemOptionsState

    data class TaskOptionsState(
        val taskId: Long,
        val priorityLabel: TextModel,
        val showPriorityDropdown: Boolean,
        val deadline: LocalDate?,
        val deadlineLabel: TextModel,
        val showDeadlineDatePicker: Boolean,
        val deadlineDatePickerTitle: TextModel
    ) : CheckableItemOptionsState {
        override val optionsShouldStayFocused = showPriorityDropdown || showDeadlineDatePicker
        val deadlineUtcMillis = deadline?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
    }

    data object HabitOptionsState : CheckableItemOptionsState
}

data class TagsState(
    val tagColors: List<Color> = emptyList(),
) {
    init {
        require(tagColors.size <= MAX_NUM_TAGS) {
            "tasks and habits cannot have more than $MAX_NUM_TAGS tags"
        }
    }
    companion object {
        const val MAX_NUM_TAGS = 4
    }
}
