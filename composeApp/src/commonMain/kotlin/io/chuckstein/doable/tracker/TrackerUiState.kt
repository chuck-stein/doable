package io.chuckstein.doable.tracker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import doable.composeapp.generated.resources.Res
import doable.composeapp.generated.resources.show_older_tasks
import doable.composeapp.generated.resources.show_untracked_habits
import io.chuckstein.doable.common.IconState
import io.chuckstein.doable.common.Icons
import io.chuckstein.doable.common.TextModel
import io.chuckstein.doable.common.toTextModel
import io.telereso.kmp.core.icons.resources.Visibility

data class TrackerUiState(
    val header: TextModel = TextModel.empty,
    val days: List<DayTrackerState> = emptyList(),
    val previousDayButtonEnabled: Boolean = false,
    val nextDayButtonEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: TextModel? = null
)

data class DayTrackerState(
    val tasksTab: TasksTabState = TasksTabState(),
    val journalTab: JournalTabState = JournalTabState(),
    val habitsTab: HabitsTabState = HabitsTabState(),
    val onFocusEvent: TrackerEvent? = null,
    val isLoading: Boolean = true,
    val errorMessage: TextModel? = null
)

data class TasksTabState(
    val tasks: List<CheckableItemState> = emptyList(),
    val bottomButtonText: TextModel = Res.string.show_older_tasks.toTextModel()
)

data class JournalTabState(
    val note: TextModel = TextModel.empty,
    val isStarred: Boolean = false,
    val journalTasks: List<CheckableItemState> = emptyList(),
    val journalHabits: List<CheckableItemState> = emptyList(),
)

data class HabitsTabState(
    val trackedHabits: List<CheckableItemState> = emptyList(),
    val showAddHabitButton: Boolean = true,
    val toggleViewUntrackedHabitsButtonState: ToggleViewUntrackedHabitsButtonState? = null,
    val untrackedHabits: List<CheckableItemState> = emptyList(),
)

data class ToggleViewUntrackedHabitsButtonState(
    val icon: IconState = IconState(Icons.Visibility, contentDescription = null),
    val text: TextModel = Res.string.show_untracked_habits.toTextModel(),
)

data class CheckableItemState(
    val id: Long,
    val checked: Boolean = false,
    val name: TextModel = TextModel.empty,
    val metadata: CheckableItemMetadataState = CheckableItemMetadataState.Empty,
    val endIcon: IconState? = null,
    val active: Boolean = true,
    val autoFocus: Boolean = false,
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

    data class TaskMetadataState(
        val tagsState: TagsState = TagsState(),
        val deadline: TextModel = TextModel.empty,
        val priorityIcon: IconState? = null,
        val showViewDetailsIcon: Boolean = true,
    ) : CheckableItemMetadataState

    data class HabitMetadataState(
        val tagsState: TagsState = TagsState(),
        val frequency: TextModel = TextModel.empty,
        val trendIcon: IconState? = null
    ) : CheckableItemMetadataState

    data object Empty : CheckableItemMetadataState
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
