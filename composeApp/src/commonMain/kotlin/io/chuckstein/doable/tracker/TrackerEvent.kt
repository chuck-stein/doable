package io.chuckstein.doable.tracker

import kotlinx.datetime.LocalDate

sealed interface TrackerEvent {

    data object InitializeTracker : TrackerEvent
    data class ChangeFocusedDay(val date: LocalDate) : TrackerEvent
    data object ToggleSelectingDate : TrackerEvent
    data object SavePendingChanges : TrackerEvent

    data object AddTask : TrackerEvent
    data class InsertTaskAfter(val otherTaskId: Long) : TrackerEvent
    data class DeleteTask(val id: Long) : TrackerEvent
    data class DeleteTaskAndMoveFocus(val id: Long) : TrackerEvent
    data class ToggleTaskCompleted(val id: Long) : TrackerEvent
    data class UpdateTaskName(val id: Long, val name: String) : TrackerEvent
    data class SaveCurrentTaskName(val id: Long) : TrackerEvent
    data object ClearTaskIdToFocus : TrackerEvent
    data class UpdateTaskPriority(val id: Long, val priority: TaskPriority) : TrackerEvent
    data class UpdateTaskDeadline(val id: Long, val deadline: LocalDate) : TrackerEvent
    data class ViewTaskDetails(val id: Long) : TrackerEvent

    data class UpdateJournalNote(val note: String) : TrackerEvent
    data object ToggleJournalEntryStarred : TrackerEvent
    data class HideTaskFromJournal(val id: Long): TrackerEvent
    data class HideHabitFromJournal(val id: Long): TrackerEvent

    data object AddTrackedHabit : TrackerEvent
    data class InsertHabitAfter(val otherHabitId: Long): TrackerEvent
    data class DeleteHabit(val id: Long) : TrackerEvent
    data class DeleteHabitAndMoveFocus(val id: Long) : TrackerEvent
    data class ToggleHabitPerformed(val id: Long) : TrackerEvent
    data class ToggleBuildingHabit(val id: Long) : TrackerEvent
    data class ToggleTrackingHabit(val id: Long) : TrackerEvent
    data class UpdateHabitName(val id: Long, val name: String) : TrackerEvent
    data class SaveCurrentHabitName(val id: Long) : TrackerEvent
    data object ClearHabitIdToFocus : TrackerEvent
    data class ViewHabitDetails(val id: Long) : TrackerEvent
    data object ToggleViewingUntrackedHabits : TrackerEvent
}