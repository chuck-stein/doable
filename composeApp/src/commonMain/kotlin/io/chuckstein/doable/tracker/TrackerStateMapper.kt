package io.chuckstein.doable.tracker

import doable.composeapp.generated.resources.Res
import doable.composeapp.generated.resources.deadline_label
import doable.composeapp.generated.resources.delete_habit_cd
import doable.composeapp.generated.resources.delete_task_cd
import doable.composeapp.generated.resources.due_friday
import doable.composeapp.generated.resources.due_monday
import doable.composeapp.generated.resources.due_saturday
import doable.composeapp.generated.resources.due_sunday
import doable.composeapp.generated.resources.due_thursday
import doable.composeapp.generated.resources.due_today
import doable.composeapp.generated.resources.due_tomorrow
import doable.composeapp.generated.resources.due_tuesday
import doable.composeapp.generated.resources.due_wednesday
import doable.composeapp.generated.resources.habit_trend_down_cd
import doable.composeapp.generated.resources.habit_trend_neutral_cd
import doable.composeapp.generated.resources.habit_trend_up_cd
import doable.composeapp.generated.resources.hide_habit_cd
import doable.composeapp.generated.resources.hide_older_tasks
import doable.composeapp.generated.resources.hide_task_cd
import doable.composeapp.generated.resources.hide_untracked_habits
import doable.composeapp.generated.resources.high_priority
import doable.composeapp.generated.resources.low_priority
import doable.composeapp.generated.resources.medium_priority
import doable.composeapp.generated.resources.no_deadline
import doable.composeapp.generated.resources.resume_tracking_habit_cd
import doable.composeapp.generated.resources.show_older_tasks
import doable.composeapp.generated.resources.show_untracked_habits
import doable.composeapp.generated.resources.stop_tracking_habit_cd
import io.chuckstein.doable.common.IconState
import io.chuckstein.doable.common.Icons
import io.chuckstein.doable.common.TextModel
import io.chuckstein.doable.common.isCompletedAsOf
import io.chuckstein.doable.common.isOlderAsOf
import io.chuckstein.doable.common.nextDay
import io.chuckstein.doable.common.previousDay
import io.chuckstein.doable.common.toTextModel
import io.chuckstein.doable.common.today
import io.chuckstein.doable.common.tomorrow
import io.chuckstein.doable.database.Task
import io.chuckstein.doable.tracker.CheckableItemMetadataState.HabitMetadataState
import io.chuckstein.doable.tracker.TrackerEvent.ClearHabitIdToFocus
import io.chuckstein.doable.tracker.TrackerEvent.ClearTaskIdToFocus
import io.chuckstein.doable.tracker.TrackerEvent.DeleteHabit
import io.chuckstein.doable.tracker.TrackerEvent.DeleteHabitAndMoveFocus
import io.chuckstein.doable.tracker.TrackerEvent.DeleteTask
import io.chuckstein.doable.tracker.TrackerEvent.DeleteTaskAndMoveFocus
import io.chuckstein.doable.tracker.TrackerEvent.HideHabitFromJournal
import io.chuckstein.doable.tracker.TrackerEvent.HideTaskFromJournal
import io.chuckstein.doable.tracker.TrackerEvent.InsertHabitAfter
import io.chuckstein.doable.tracker.TrackerEvent.InsertTaskAfter
import io.chuckstein.doable.tracker.TrackerEvent.SaveCurrentHabitName
import io.chuckstein.doable.tracker.TrackerEvent.SaveCurrentTaskName
import io.chuckstein.doable.tracker.TrackerEvent.ToggleEditingTask
import io.chuckstein.doable.tracker.TrackerEvent.ToggleHabitPerformed
import io.chuckstein.doable.tracker.TrackerEvent.ToggleTaskCompleted
import io.chuckstein.doable.tracker.TrackerEvent.ToggleTrackingHabit
import io.chuckstein.doable.tracker.TrackerEvent.UpdateHabitName
import io.chuckstein.doable.tracker.TrackerEvent.UpdateTaskName
import io.telereso.kmp.core.icons.resources.AddCircleOutline
import io.telereso.kmp.core.icons.resources.Close
import io.telereso.kmp.core.icons.resources.KeyboardDoubleArrowDown
import io.telereso.kmp.core.icons.resources.KeyboardDoubleArrowUp
import io.telereso.kmp.core.icons.resources.RemoveCircleOutline
import io.telereso.kmp.core.icons.resources.TrendingDown
import io.telereso.kmp.core.icons.resources.TrendingFlat
import io.telereso.kmp.core.icons.resources.TrendingUp
import io.telereso.kmp.core.icons.resources.Visibility
import io.telereso.kmp.core.icons.resources.VisibilityOff
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.plus

class TrackerStateMapper {

    fun mapToUiState(domainState: TrackerDomainState) = TrackerUiState(
        header = domainState.focusedDay.toHeaderText(),
        previousDayButtonEnabled = domainState.focusedDay.previousDay() in domainState.trackedDays,
        nextDayButtonEnabled = domainState.focusedDay.nextDay() in domainState.trackedDays,
        days = domainState.trackedDays.mapIndexed { index, trackerDate ->
            val dayDetails = domainState.dayDetailsMap[trackerDate]
            val habitsAndTasksAreCheckable = index > domainState.trackedDays.lastIndex - 7
            val habitTrackingIsToggleable = index == domainState.trackedDays.lastIndex
            if (dayDetails == null) {
                TrackerDayState(
                    date = trackerDate,
                    isLoading = true,
                )
            } else {
                TrackerDayState(
                    date = trackerDate,
                    tasksTab = dayDetails.createTasksTab(
                        trackerDate,
                        domainState.tasks.filter { it.dateCreated <= trackerDate },
                        domainState.taskIdToFocus,
                        domainState.taskEditingState,
                        habitsAndTasksAreCheckable
                    ),
                    journalTab = dayDetails.createJournalTab(
                        trackerDate,
                        domainState.tasks,
                        domainState.taskEditingState,
                        habitsAndTasksAreCheckable
                    ),
                    habitsTab = dayDetails.createHabitsTab(
                        trackerDate,
                        domainState.habitIdToFocus,
                        habitsAndTasksAreCheckable,
                        habitTrackingIsToggleable
                    ),
                    errorMessage = dayDetails.error?.message?.toTextModel(),
                    isLoading = false
                )
            }
        },
        showDatePicker = domainState.isSelectingDate,
        isLoading = domainState.isLoading,
        errorMessage = domainState.error?.message?.toTextModel()
    )

    // TODO: i18n
    private fun LocalDate.toHeaderText(): TextModel {
        val formatter = LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_FULL)
            chars(", ")
            monthName(MonthNames.ENGLISH_FULL)
            chars(" ")
            dayOfMonth(Padding.NONE)
            when (dayOfMonth) {
                1, 21, 31 -> chars("st")
                2, 22 -> chars("nd")
                3, 23 -> chars("rd")
                else -> chars("th")
            }
            if (year != today().year) {
                chars(", ")
                year()
            }
        }
        return format(formatter).toTextModel()
    }

    private fun DayDetails.createTasksTab(
        date: LocalDate,
        tasks: List<Task>,
        taskIdToFocus: Long?,
        taskEditingState: TaskEditingState?,
        tasksAreCheckable: Boolean
    ) = TasksTabState(
        tasks = tasks
            .filterNot { it.isOlderAsOf(date) }
            .map { task ->
                task.toCheckableItemState(
                    date = date,
                    endIcon = IconState(Icons.Close, Res.string.delete_task_cd.toTextModel())
                        .takeUnless { task.isCompletedAsOf(date) },
                    taskIdToFocus = taskIdToFocus,
                    taskEditingState = taskEditingState,
                    checkable = tasksAreCheckable
                )
            },
        toggleViewOlderTasksButtonState = createToggleViewOlderTasksButton(date, tasks),
        olderTasks = tasks
            .filter { viewingOlderTasks && it.isOlderAsOf(date) }
            .map { task ->
                task.toCheckableItemState(
                    date = date,
                    endIcon = null,
                    taskIdToFocus = taskIdToFocus,
                    taskEditingState = taskEditingState,
                    checkable = false,
                    taskOptionsEnabled = false
                )
            },
    )

    private fun DayDetails.createJournalTab(
        date: LocalDate,
        allTasks: List<Task>,
        taskEditingState: TaskEditingState?,
        habitsAndTasksAreCheckable: Boolean
    ) = JournalTabState(
        note = journalEntry.note.toTextModel(),
        isStarred = journalEntry.isStarred,
        journalTasks = journalTaskIds(allTasks).map { task ->
            task.toCheckableItemState(
                date = date,
                endIcon = IconState(Icons.VisibilityOff, Res.string.hide_task_cd.toTextModel())
                    .takeIf { task.dateCompleted != date },
                endIconClickEvent = HideTaskFromJournal(task.id)
                    .takeIf { task.dateCompleted != date },
                taskEditingState = taskEditingState,
                checkable = habitsAndTasksAreCheckable
            )
        },
        journalHabits = journalHabitIds().map { habit ->
            habit.toCheckableItemState(
                endIcon = IconState(Icons.VisibilityOff, Res.string.hide_habit_cd.toTextModel())
                    .takeIf { !habit.wasPerformed },
                endIconClickEvent = HideHabitFromJournal(habit.id)
                    .takeIf { !habit.wasPerformed },
                checkable = habitsAndTasksAreCheckable
            )
        }
    )

    private fun DayDetails.createHabitsTab(
        date: LocalDate,
        habitIdToFocus: Long?,
        habitsAreCheckable: Boolean,
        habitTrackingIsToggleable: Boolean
    ) = HabitsTabState(
        trackedHabits = trackedHabits.map { habit ->
            habit.toCheckableItemState(
                endIcon = if (habit.isNew) {
                    IconState(Icons.Close, Res.string.delete_habit_cd.toTextModel())
                } else {
                    IconState(
                        icon = Icons.RemoveCircleOutline,
                        contentDescription = Res.string.stop_tracking_habit_cd.toTextModel(),
                        enabled = !habit.wasPerformed
                    ).takeIf { habitTrackingIsToggleable }
                },
                endIconClickEvent = if (habit.isNew) {
                    DeleteHabit(habit.id)
                } else {
                    ToggleTrackingHabit(habit.id)
                },
                backspaceWhenEmptyEvent = DeleteHabitAndMoveFocus(habit.id).takeIf { habit.isNew },
                habitIdToFocus = habitIdToFocus,
                checkable = habitsAreCheckable
            )
        },
        showAddHabitButton = date == today(),
        toggleViewUntrackedHabitsButtonState = createToggleViewUntrackedHabitsButton(),
        untrackedHabits = if (viewingUntrackedHabits) {
            untrackedHabits.map { habit ->
                CheckableItemState(
                    id = habit.id,
                    checked = false,
                    name = habit.name.toTextModel(),
                    metadata = CheckableItemMetadataState.Empty,  // TODO: implement metadata for both tasks and habits
                    endIcon = IconState(Icons.AddCircleOutline, Res.string.resume_tracking_habit_cd.toTextModel())
                        .takeIf { habitTrackingIsToggleable },
                    active = false,
                    updateNameEvent = { UpdateHabitName(habit.id, it) },
                    toggleCheckedEvent = null,
                    endIconClickEvent = ToggleTrackingHabit(habit.id),
                    loseFocusEvent = SaveCurrentHabitName(habit.id),
                )
            }
        } else {
            emptyList()
        }
    )

    private fun DayDetails.createToggleViewOlderTasksButton(date: LocalDate, tasks: List<Task>) = IconButtonState(
        icon = createVisibilityToggleIcon(isVisible = viewingOlderTasks),
        text = if (viewingOlderTasks) {
            Res.string.hide_older_tasks.toTextModel()
        } else {
            Res.string.show_older_tasks.toTextModel()
        }
    ).takeIf { tasks.any { it.isOlderAsOf(date) } }

    private fun DayDetails.createToggleViewUntrackedHabitsButton() = IconButtonState(
        icon = createVisibilityToggleIcon(isVisible = viewingUntrackedHabits),
        text = if (viewingUntrackedHabits) {
            Res.string.hide_untracked_habits.toTextModel()
        } else {
            Res.string.show_untracked_habits.toTextModel()
        }
    ).takeIf { untrackedHabits.isNotEmpty() }

    private fun Task.toCheckableItemState(
        date: LocalDate,
        endIcon: IconState?,
        endIconClickEvent: TrackerEvent? = DeleteTask(id),
        taskIdToFocus: Long? = null,
        taskEditingState: TaskEditingState?,
        checkable: Boolean,
        taskOptionsEnabled: Boolean = true
    ) = CheckableItemState(
        id = id,
        checked = isCompletedAsOf(date),
        name = name.toTextModel(),
        infoText = createTaskInfoText(),
        metadata = CheckableItemMetadataState.TaskMetadataState(
            priorityIcon = when (priority) {
                TaskPriority.Low -> IconState(Icons.KeyboardDoubleArrowDown, contentDescription = Res.string.low_priority.toTextModel())
                TaskPriority.Medium -> null
                TaskPriority.High -> IconState(Icons.KeyboardDoubleArrowUp, contentDescription = Res.string.high_priority.toTextModel())
            }
        ),
        endIcon = endIcon,
        optionsState = CheckableItemOptionsState.TaskOptionsState(
            taskId = id,
            priorityLabel = when (priority) {
                TaskPriority.Low -> Res.string.low_priority.toTextModel()
                TaskPriority.Medium -> Res.string.medium_priority.toTextModel()
                TaskPriority.High -> Res.string.high_priority.toTextModel()
            },
            showPriorityDropdown = taskEditingState?.isEditingPriority == true,
            deadline = deadline,
            deadlineLabel = createDeadlineLabel(),
            showDeadlineDatePicker = taskEditingState?.isEditingDeadline == true,
            deadlineDatePickerTitle = name.toTextModel(),
        ).takeIf { taskOptionsEnabled && id == taskEditingState?.taskId } ?: CheckableItemOptionsState.Empty,
        autoFocus = id == taskIdToFocus,
        toggleEditingEvent = ToggleEditingTask(id),
        updateNameEvent = { UpdateTaskName(id, it) },
        toggleCheckedEvent = ToggleTaskCompleted(id).takeIf { checkable },
        endIconClickEvent = endIconClickEvent,
        loseFocusEvent = SaveCurrentTaskName(id),
        autoFocusDoneEvent = ClearTaskIdToFocus,
        nextActionEvent = InsertTaskAfter(id),
        backspaceWhenEmptyEvent = DeleteTaskAndMoveFocus(id)
    )

    private fun TrackedHabit.toCheckableItemState(
        endIcon: IconState?,
        endIconClickEvent: TrackerEvent?,
        habitIdToFocus: Long? = null,
        backspaceWhenEmptyEvent: TrackerEvent? = null, // TODO: do I need this for tasks too?
        checkable: Boolean
    ) = CheckableItemState(
        id = id,
        checked = wasPerformed,
        name = name.toTextModel(),
        metadata = HabitMetadataState( // TODO: implement remaining metadata for habits
            trendIcon = when (trend) {
                HabitTrend.Up -> IconState(Icons.TrendingUp, contentDescription = Res.string.habit_trend_up_cd.toTextModel())
                HabitTrend.Down -> IconState(Icons.TrendingDown, contentDescription = Res.string.habit_trend_down_cd.toTextModel())
                HabitTrend.Neutral -> IconState(Icons.TrendingFlat, contentDescription = Res.string.habit_trend_neutral_cd.toTextModel())
                HabitTrend.None -> null
            },
        ),
        endIcon = endIcon,
        autoFocus = id == habitIdToFocus,
        updateNameEvent = { UpdateHabitName(id, it) },
        toggleCheckedEvent = ToggleHabitPerformed(id).takeIf { checkable },
        endIconClickEvent = endIconClickEvent,
        loseFocusEvent = SaveCurrentHabitName(id),
        autoFocusDoneEvent = ClearHabitIdToFocus,
        nextActionEvent = InsertHabitAfter(id),
        backspaceWhenEmptyEvent = backspaceWhenEmptyEvent
    )

    private fun Task.createTaskInfoText(): TextModel? = when {
        deadline == today() -> Res.string.due_today.toTextModel()
        deadline == tomorrow() -> Res.string.due_tomorrow.toTextModel()
        deadline?.dayOfWeek == DayOfWeek.MONDAY && deadline.isThisWeek() -> Res.string.due_monday.toTextModel()
        deadline?.dayOfWeek == DayOfWeek.TUESDAY && deadline.isThisWeek() -> Res.string.due_tuesday.toTextModel()
        deadline?.dayOfWeek == DayOfWeek.WEDNESDAY && deadline.isThisWeek() -> Res.string.due_wednesday.toTextModel()
        deadline?.dayOfWeek == DayOfWeek.THURSDAY && deadline.isThisWeek() -> Res.string.due_thursday.toTextModel()
        deadline?.dayOfWeek == DayOfWeek.FRIDAY && deadline.isThisWeek() -> Res.string.due_friday.toTextModel()
        deadline?.dayOfWeek == DayOfWeek.SATURDAY && deadline.isThisWeek() -> Res.string.due_saturday.toTextModel()
        deadline?.dayOfWeek == DayOfWeek.SUNDAY && deadline.isThisWeek() -> Res.string.due_sunday.toTextModel()
        else -> null
    }

    private fun LocalDate.isThisWeek() = this in today() ..< today().plus(7, DAY)

    // TODO: i18n
    private fun Task.createDeadlineLabel(): TextModel {
        val deadlineFormatter = LocalDate.Format {
            monthNumber(Padding.NONE)
            chars("/")
            dayOfMonth(Padding.NONE)
            chars("/")
            year()
        }
        return if (deadline == null) {
            Res.string.no_deadline.toTextModel()
        } else {
            TextModel.Resource(Res.string.deadline_label, deadline.format(deadlineFormatter))
        }
    }

    private fun createVisibilityToggleIcon(isVisible: Boolean) = if (isVisible) {
        IconState(Icons.VisibilityOff, contentDescription = null)
    } else {
        IconState(Icons.Visibility, contentDescription = null)
    }
}