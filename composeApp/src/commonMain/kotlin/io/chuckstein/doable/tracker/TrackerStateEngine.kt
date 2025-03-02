package io.chuckstein.doable.tracker

import co.touchlab.kermit.Logger
import io.chuckstein.doable.common.avgNumDaysBetweenDates
import io.chuckstein.doable.common.nextDay
import io.chuckstein.doable.common.previousDay
import io.chuckstein.doable.common.previousDays
import io.chuckstein.doable.common.today
import io.chuckstein.doable.common.yesterday
import io.chuckstein.doable.database.DataSourceException
import io.chuckstein.doable.database.DoableDataSource
import io.chuckstein.doable.database.Habit
import io.chuckstein.doable.database.HabitStatus
import io.chuckstein.doable.database.Task
import io.chuckstein.doable.tracker.TrackerError.FailedToLoad
import io.chuckstein.doable.tracker.TrackerError.FailedToSave
import io.chuckstein.doable.tracker.TrackerEvent.AddTask
import io.chuckstein.doable.tracker.TrackerEvent.AddTrackedHabit
import io.chuckstein.doable.tracker.TrackerEvent.ChangeFocusedDay
import io.chuckstein.doable.tracker.TrackerEvent.ClearHabitIdToFocus
import io.chuckstein.doable.tracker.TrackerEvent.ClearTaskIdToFocus
import io.chuckstein.doable.tracker.TrackerEvent.DeleteHabit
import io.chuckstein.doable.tracker.TrackerEvent.DeleteHabitAndMoveFocus
import io.chuckstein.doable.tracker.TrackerEvent.DeleteTask
import io.chuckstein.doable.tracker.TrackerEvent.DeleteTaskAndMoveFocus
import io.chuckstein.doable.tracker.TrackerEvent.HideHabitFromJournal
import io.chuckstein.doable.tracker.TrackerEvent.HideTaskFromJournal
import io.chuckstein.doable.tracker.TrackerEvent.InitializeTracker
import io.chuckstein.doable.tracker.TrackerEvent.InsertHabitAfter
import io.chuckstein.doable.tracker.TrackerEvent.InsertTaskAfter
import io.chuckstein.doable.tracker.TrackerEvent.SaveCurrentHabitName
import io.chuckstein.doable.tracker.TrackerEvent.SaveCurrentTaskName
import io.chuckstein.doable.tracker.TrackerEvent.SavePendingChanges
import io.chuckstein.doable.tracker.TrackerEvent.ToggleBuildingHabit
import io.chuckstein.doable.tracker.TrackerEvent.ToggleHabitPerformed
import io.chuckstein.doable.tracker.TrackerEvent.ToggleJournalEntryStarred
import io.chuckstein.doable.tracker.TrackerEvent.ToggleTaskCompleted
import io.chuckstein.doable.tracker.TrackerEvent.ToggleTrackingHabit
import io.chuckstein.doable.tracker.TrackerEvent.ToggleViewingUntrackedHabits
import io.chuckstein.doable.tracker.TrackerEvent.UpdateHabitName
import io.chuckstein.doable.tracker.TrackerEvent.UpdateJournalNote
import io.chuckstein.doable.tracker.TrackerEvent.UpdateTaskDeadline
import io.chuckstein.doable.tracker.TrackerEvent.UpdateTaskName
import io.chuckstein.doable.tracker.TrackerEvent.UpdateTaskPriority
import io.chuckstein.doable.tracker.TrackerEvent.ViewHabitDetails
import io.chuckstein.doable.tracker.TrackerEvent.ViewTaskDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit.Companion.DAY
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.milliseconds

class TrackerStateEngine(
    private val trackerStateMapper: TrackerStateMapper,
    private val dataSource: DoableDataSource
) {

    private val domainStateFlow = MutableStateFlow(TrackerDomainState())
    private val currentDomainState: TrackerDomainState
        get() = domainStateFlow.value

    fun collectUiState(scope: CoroutineScope) =
        domainStateFlow.map { trackerStateMapper.mapToUiState(it) }
            .onEach { Logger.d { "ui state: $it" } }
            .stateIn(scope, SharingStarted.Eagerly, TrackerUiState())

    fun processEvent(event: TrackerEvent, scope: CoroutineScope) {
        when (event) {
            is InitializeTracker -> initializeTracker(scope)
            is ChangeFocusedDay -> changeFocusedDay(event.date, scope)
            is SavePendingChanges -> scope.launch { savePendingChanges() }
            is UpdateJournalNote -> updateJournalNote(event.note)
            is ToggleJournalEntryStarred -> TODO()
            is HideTaskFromJournal -> hideTaskFromJournal(event.id)
            is HideHabitFromJournal -> hideHabitFromJournal(event.id)

            is AddTask -> addTask(scope)
            is InsertTaskAfter -> insertTaskAfter(event.otherTaskId, scope)
            is DeleteTask -> deleteTask(event.id, scope)
            is DeleteTaskAndMoveFocus -> deleteTaskAndMoveFocus(event.id, scope)
            is ToggleTaskCompleted -> toggleTaskCompleted(event.id, scope)
            is UpdateTaskName -> updateTaskName(event.id, event.name)
            is SaveCurrentTaskName -> scope.launch { saveTask(event.id) }
            is ClearTaskIdToFocus -> domainStateFlow.update { it.copy(taskIdToFocus = null) }
            is UpdateTaskPriority -> TODO()
            is UpdateTaskDeadline -> TODO()
            is ViewTaskDetails -> TODO()

            is AddTrackedHabit -> addTrackedHabit(scope)
            is InsertHabitAfter -> insertTrackedHabitAfter(event.otherHabitId, scope)
            is DeleteHabit -> deleteHabit(event.id, scope)
            is DeleteHabitAndMoveFocus -> deleteHabitAndMoveFocus(event.id, scope)
            is ToggleHabitPerformed -> toggleHabitPerformed(event.id, scope)
            is UpdateHabitName -> updateHabitName(event.id, event.name)
            is SaveCurrentHabitName -> scope.launch { saveHabitName(event.id) }
            is ClearHabitIdToFocus -> domainStateFlow.update { it.copy(habitIdToFocus = null) }
            is ToggleTrackingHabit -> toggleTrackingHabit(event.id, scope)
            is ToggleBuildingHabit -> TODO()
            is ViewHabitDetails -> TODO()
            is ToggleViewingUntrackedHabits -> toggleViewingUntrackedHabits()
        }
    }

    private fun initializeTracker(scope: CoroutineScope) {
        scope.launch { observeJournalNote() }
        scope.launch {
            domainStateFlow.updateByReadingDataOrDefault(
                fallback = { TrackerDomainState(error = FailedToLoad, isLoading = false) }
            ) {
                createMissingJournalEntries()
                dataSource.selectJournalDatesWithoutHabitStatuses().filter { it != today() }.sorted().forEach { date ->
                    createHabitStatuses(date)
                }
                val firstJournalEntry = checkNotNull(dataSource.selectFirstJournalEntry())
                val numDaysTracked = firstJournalEntry.date.daysUntil(today()) + 1
                TrackerDomainState(
                    isLoading = false,
                    error = null,
                    tasks = dataSource.selectAllTasks().sortedBy { it.dateCompleted != null },
                    focusedDay = today(),
                    trackedDays = today().previousDays(numDaysTracked - 1) + today(),
                ).run {
                    // build an intermediate state then use it to build the dayDetailsMap
                    copy(
                        dayDetailsMap = if (firstJournalEntry.date < today()) {
                            mapOf(
                                today() to getDayDetails(today()),
                                yesterday() to getDayDetails(yesterday())
                            )
                        } else {
                            mapOf(today() to getDayDetails(today()))
                        }
                    )
                }
            }
        }
    }

    private suspend fun createMissingJournalEntries() {
        val latestJournalEntry = dataSource.selectLatestJournalEntry()
        when {
            latestJournalEntry == null -> dataSource.insertJournalEntry(today())
            latestJournalEntry.date != today() -> dataSource.insertJournalEntries(
                dates = List(size = latestJournalEntry.date.daysUntil(today())) { index ->
                    latestJournalEntry.date.plus(index + 1, DAY)
                }
            )
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeJournalNote() {
        val focusedDayFlow = domainStateFlow.map { it.focusedDay }.distinctUntilChanged()
        val journalNoteFlow = domainStateFlow.map { it.focusedDayDetails.journalEntry.note }.distinctUntilChanged()

        focusedDayFlow.collectLatest {
            journalNoteFlow
                .drop(2)
                // TODO: split the flow here and add a .sample(2.seconds).collectLatest { generateMagicBarSuggestionsFromNote() }
                .debounce(500.milliseconds)
                .collectLatest { saveCurrentJournalEntry() }
        }
    }

    private suspend fun createHabitStatuses(date: LocalDate) {
        val habitStatuses = dataSource.selectAllHabits().filter { it.currentlyTracking }.map { habit ->
            HabitStatus(
                habitId = habit.id,
                date = date,
                frequency = when {
                    date.habitPerformed5OfPast7Days(habit.id) -> HabitFrequency.Daily
                    date.habitPerformed3OfPast4Weeks(habit.id) -> HabitFrequency.Weekly
                    date.habitPerformedBothOfPast2Months(habit.id) -> HabitFrequency.Monthly
                    else -> HabitFrequency.None
                },
                trend = HabitTrend.None, // TODO: implement habit trends
                wasBuilding = false // TODO: implement habit building
            )
        }
        dataSource.insertHabitStatusesForDate(date, habitStatuses)
    }

    private suspend fun LocalDate.numDaysHabitPerformedInPastWeek(habitId: Long) =
        dataSource.selectNumTimesHabitPerformedDuringDates(habitId, previousDays(7))

    private suspend fun LocalDate.habitPerformed5OfPast7Days(habitId: Long) =
        numDaysHabitPerformedInPastWeek(habitId) >= 5

    private suspend fun LocalDate.habitPerformed3OfPast4Weeks(habitId: Long) = listOf(
        numDaysHabitPerformedInPastWeek(habitId),
        minus(7, DAY).numDaysHabitPerformedInPastWeek(habitId),
        minus(14, DAY).numDaysHabitPerformedInPastWeek(habitId),
        minus(21, DAY).numDaysHabitPerformedInPastWeek(habitId),
    ).filter { numDaysPerformedInWeek ->
        numDaysPerformedInWeek >= 1
    }.size >= 3

    private suspend fun LocalDate.habitPerformedBothOfPast2Months(habitId: Long) =
        dataSource.selectNumTimesHabitPerformedDuringDates(habitId, previousDays(30)) >= 1
                && dataSource.selectNumTimesHabitPerformedDuringDates(habitId, minus(30, DAY).previousDays(30)) >= 1

    private suspend fun saveCurrentJournalEntry() {
        Logger.d { "saving journal entry: ${currentDomainState.focusedDayDetails.journalEntry.note}" }
        saveData {
            dataSource.updateJournalEntry(currentDomainState.focusedDayDetails.journalEntry)
        }
    }

    private fun updateJournalNote(note: String) {
        domainStateFlow.update { state ->
            state.updateFocusedDayDetails { copy(journalEntry = journalEntry.copy(note = note)) }
        }
    }

    private fun hideTaskFromJournal(id: Long) {
        domainStateFlow.update {
            it.updateFocusedDayDetails {
                copy(journalTaskIds = journalTaskIds - id)
            }
        }
    }

    private fun hideHabitFromJournal(id: Long) {
        domainStateFlow.update {
            it.updateFocusedDayDetails {
                copy(journalHabitIds = journalHabitIds - id)
            }
        }
    }

    private suspend fun savePendingChanges(dayToSaveFrom: DayDetails = currentDomainState.focusedDayDetails) {
        coroutineScope {
            val pendingChanges = currentDomainState.pendingChanges
            pendingChanges.forEach {
                when (it) {
                    is PendingChange.Habit -> saveHabitName(it.id, dayToSaveFrom)
                    is PendingChange.Task -> saveTask(it.id)
                }
            }
        }
    }

    private fun changeFocusedDay(date: LocalDate, scope: CoroutineScope) {
        val previousFocusedDay = currentDomainState.focusedDayDetails
        domainStateFlow.update { state ->
            state.copy(focusedDay = date)
        }
        scope.launch {
            savePendingChanges(previousFocusedDay)
            domainStateFlow.updateByReadingDataOrDefault(
                fallback = {
                    if (date in dayDetailsMap) this else copy(
                        dayDetailsMap = dayDetailsMap + (date to DayDetails.error)
                    )
                }
            ) {
                copy(dayDetailsMap = dayDetailsMap + (date to getDayDetails(date)))
            }
            launch {
                if (date.previousDay() in currentDomainState.trackedDays) {
                    domainStateFlow.updateByReadingDataOrDefault(fallback = { this }) {
                        copy(dayDetailsMap = dayDetailsMap + (date.previousDay() to getDayDetails(date.previousDay())))
                    }
                }
            }
            launch {
                if (date.nextDay() in currentDomainState.trackedDays) {
                    domainStateFlow.updateByReadingDataOrDefault(fallback = { this }) {
                        copy(dayDetailsMap = dayDetailsMap + (date.nextDay() to getDayDetails(date.nextDay())))
                    }
                }
            }
        }
    }

    private suspend fun TrackerDomainState.getDayDetails(
        date: LocalDate
    ) = readDataOrDefault(fallback = { DayDetails.error }) {
        val journalEntry = dataSource.selectJournalEntryForDate(date) ?: return@readDataOrDefault DayDetails.error
        val habitsPerformed = dataSource.selectAllHabitIdsPerformedOnDate(date)
        val habits = dataSource.selectAllHabits()
        val dateForHabitStatuses = if (date == today()) yesterday() else date
        val habitStatuses = dataSource.selectAllHabitStatusesForDate(dateForHabitStatuses).associateBy { it.habitId }
        val trackedHabits = if (date == today()) {
            habits.filter { it.currentlyTracking }.map { habit ->
                TrackedHabit(
                    id = habit.id,
                    name = habit.name,
                    frequency = habitStatuses[habit.id]?.frequency ?: HabitFrequency.None,
                    wasBuilding = habit.currentlyBuilding,
                    wasPerformed = habit.id in habitsPerformed,
                    isNew = !dataSource.doesAnyHabitStatusExistForHabit(habit.id)
                )
            }
        } else {
            habitStatuses.values.map { habitStatus ->
                TrackedHabit(
                    id = habitStatus.habitId,
                    name = habitStatus.name,
                    frequency = habitStatus.frequency,
                    wasBuilding = habitStatus.wasBuilding,
                    wasPerformed = habitStatus.habitId in habitsPerformed,
                    isNew = false // TODO: need to disallow inserting habits while viewing past days, otherwise this could be true (and we would need to be able to retroactively create HabitStatuses, which would then have cascading effects on the HabitStatuses of past days for things like frequency)
                )
            }
        }

        return DayDetails(
            journalEntry = journalEntry,
            journalTaskIds = tasks
                .filter { it.dateCompleted == date || it.isSuggested(date, tasks) }
                .sortedByDescending { it.dateCompleted == date }
                .map { it.id },
            journalHabitIds = trackedHabits
                .filter { it.wasPerformed || it.isSuggested(date) }
                .sortedByDescending { it.wasPerformed }
                .map { it.id },
            untrackedHabits = if (date == today()) {
                habits.filterNot { it.currentlyTracking }
            } else {
                habits.filterNot { it.id in habitStatuses } // TODO: update if we need to allow inserting habits on past days
            },
            trackedHabits = trackedHabits
        )
    }

    // very naive suggestion algo for now -- just pick the oldest task that's not completed
    // TODO: have a more thoughtful approach to task suggestions
    private fun Task.isSuggested(date: LocalDate, allTasks: List<Task>) = date == today() && allTasks
        .filter { it.dateCompleted == null }
        .minByOrNull { it.dateCreated }?.id == id

    private suspend fun TrackedHabit.isSuggested(date: LocalDate): Boolean {
        if (date != today()) return false

        val shortTermDayOfWeekPropensity = calculateRecentDayOfWeekPropensity(SHORT_TERM_HABIT_PROPENSITY_NUM_DAYS, date)
        if (shortTermDayOfWeekPropensity == 1.0) return true

        val mediumTermDayOfWeekPropensity = calculateRecentDayOfWeekPropensity(MEDIUM_TERM_HABIT_PROPENSITY_NUM_DAYS, date)
        if (mediumTermDayOfWeekPropensity >= 0.5) return true // TODO: maybe strictly greater than, if this ends up giving too many irrelevant suggestions?

        val otherDaysOfWeekMediumTermPropensity = DayOfWeek.entries
            .filterNot { it == date.dayOfWeek }
            .map { calculateRecentDayOfWeekPropensity(MEDIUM_TERM_HABIT_PROPENSITY_NUM_DAYS, date, dayOfWeek = it) }
        val isOnlyDayOfWeekPerformed = mediumTermDayOfWeekPropensity > 0.3 && otherDaysOfWeekMediumTermPropensity.all { it == 0.0 }
        if (isOnlyDayOfWeekPerformed) return true

        val recentDatesHabitPerformed = dataSource.selectMostRecentDatesHabitPerformed(id, numDates = 3)
        val numDaysSinceHabitPerformed = recentDatesHabitPerformed.firstOrNull()?.daysUntil(date) ?: Int.MAX_VALUE
        return when (frequency) {
            HabitFrequency.Daily -> numDaysSinceHabitPerformed >= 1
            HabitFrequency.Weekly -> numDaysSinceHabitPerformed >= 7
            HabitFrequency.Monthly -> numDaysSinceHabitPerformed >= 30
            // TODO: consider upper limit for how long ago it was recently performed before we stop suggesting this one (otherwise remains until performed again)
            // TODO: if the standard deviation from avg is high % of avg (not enough of a clear pattern), then don't suggest this habit
            HabitFrequency.None -> recentDatesHabitPerformed.size >= 3 && numDaysSinceHabitPerformed >= avgNumDaysBetweenDates(recentDatesHabitPerformed)
        }
    }

    private suspend fun TrackedHabit.calculateRecentDayOfWeekPropensity(
        numDays: Int,
        date: LocalDate,
        dayOfWeek: DayOfWeek = date.dayOfWeek
    ): Double {
        val mediumTermReferenceDates = date.previousDays(numDays).filter { it.dayOfWeek == dayOfWeek }
        val mediumTermNumTimesPerformed = dataSource.selectNumTimesHabitPerformedDuringDates(id, mediumTermReferenceDates)
        return mediumTermNumTimesPerformed / mediumTermReferenceDates.size.toDouble()
    }

    private fun addTask(scope: CoroutineScope, position: Int? = null) {
        scope.launch {
            saveData {
                dataSource.insertTask(name = "", dateCreated = currentDomainState.focusedDay)
            }?.let { newTask ->
                domainStateFlow.update {
                    it.copy(
                        tasks = if (position != null) {
                            it.tasks.take(position) + newTask + it.tasks.drop(position)
                        } else {
                            it.tasks + newTask
                        },
                        taskIdToFocus = newTask.id,
                    )
                }
            }
        }
    }

    private fun insertTaskAfter(otherTaskId: Long, scope: CoroutineScope) {
        addTask(scope, position = currentDomainState.tasks.indexOfFirst { it.id == otherTaskId } + 1)
    }

    private fun deleteTask(id: Long, scope: CoroutineScope) {
        domainStateFlow.update { state ->
            state.copy(tasks = state.tasks.filterNot { it.id == id })
        }
        scope.launch {
            saveData {
                dataSource.deleteTask(id)
            }
        }
    }

    private fun deleteTaskAndMoveFocus(id: Long, scope: CoroutineScope) {
        val deletedTaskIndex = currentDomainState.tasks.indexOfFirst { it.id == id }
        deleteTask(id, scope)
        domainStateFlow.update { state ->
            state.copy(taskIdToFocus = state.tasks.getOrNull(deletedTaskIndex - 1)?.id)
        }
    }

    private fun toggleTaskCompleted(id: Long, scope: CoroutineScope) {
        domainStateFlow.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == id) {
                        task.copy(dateCompleted = if (task.dateCompleted == null) state.focusedDay else null)
                    } else {
                        task
                    }
                }
            ).updateFocusedDayDetails {
                copy(journalTaskIds = if (id !in journalTaskIds) journalTaskIds + id else journalTaskIds)
            }
        }
        scope.launch {
            saveTask(id)
        }
    }

    private fun updateTaskName(id: Long, name: String) {
        domainStateFlow.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == id) {
                        task.copy(name = name)
                    } else {
                        task
                    }
                },
                pendingChanges = state.pendingChanges + PendingChange.Task(id)
            )
        }
    }

    private suspend fun saveTask(id: Long) {
        saveData {
            currentDomainState.tasks.find { it.id == id }?.let { task ->
                dataSource.updateTask(task)
            }
            domainStateFlow.update { it.copy(pendingChanges = it.pendingChanges - PendingChange.Task(id)) }
        }
    }

    private fun addTrackedHabit(scope: CoroutineScope, position: Int? = null) {
        scope.launch {
            saveData {
                dataSource.insertHabit(name = "")
            }?.let { newHabit ->
                domainStateFlow.update { state ->
                    state
                        .copy(habitIdToFocus = newHabit.id)
                        .updateFocusedDayDetails {
                            val newTrackedHabit = TrackedHabit(
                                id = newHabit.id,
                                name = newHabit.name,
                                frequency = HabitFrequency.None,
                                wasBuilding = false,
                                wasPerformed = false,
                                isNew = true
                            )
                            copy(
                                trackedHabits = if (position != null) {
                                    trackedHabits.take(position) + newTrackedHabit + trackedHabits.drop(position)
                                } else {
                                    trackedHabits + newTrackedHabit
                                }
                            )
                        }
                }
            }
        }
    }

    private fun insertTrackedHabitAfter(otherHabitId: Long, scope: CoroutineScope) {
        addTrackedHabit(
            scope = scope,
            position = currentDomainState.focusedDayDetails.trackedHabits.indexOfFirst { it.id == otherHabitId } + 1
        )
    }

    private fun deleteHabit(id: Long, scope: CoroutineScope) {
        domainStateFlow.update { state ->
            state.updateFocusedDayDetails {
                copy(
                    trackedHabits = trackedHabits.filterNot { it.id == id },
                    untrackedHabits = untrackedHabits.filterNot { it.id == id }
                )
            }
        }
        scope.launch {
            saveData {
                dataSource.deleteHabit(id)
            }
        }
    }

    private fun deleteHabitAndMoveFocus(id: Long, scope: CoroutineScope) {
        val isTrackedHabit = currentDomainState.focusedDayDetails.trackedHabits.any { it.id == id }
        val deletedHabitIndex = if (isTrackedHabit) {
            currentDomainState.focusedDayDetails.trackedHabits.indexOfFirst { it.id == id }
        } else {
            currentDomainState.focusedDayDetails.untrackedHabits.indexOfFirst { it.id == id }
        }

        deleteHabit(id, scope)
        domainStateFlow.update { state ->
            state.copy(
                habitIdToFocus = if (isTrackedHabit) {
                    state.focusedDayDetails.trackedHabits.getOrNull(deletedHabitIndex - 1)?.id
                } else {
                    state.focusedDayDetails.untrackedHabits.getOrNull(deletedHabitIndex - 1)?.id
                }
            )
        }
    }

    private fun toggleHabitPerformed(id: Long, scope: CoroutineScope) {
        val habitIsNowPerformed =
            currentDomainState.focusedDayDetails.trackedHabits.find { it.id == id }?.wasPerformed?.not() ?: false

        domainStateFlow.update { state ->
            state.updateFocusedDayDetails {
                copy(
                    trackedHabits = trackedHabits.map { habit ->
                        if (habit.id == id) {
                            habit.copy(wasPerformed = habitIsNowPerformed)
                        } else {
                            habit
                        }
                    },
                    journalHabitIds = if (id !in journalHabitIds) journalHabitIds + id else journalHabitIds
                )
            }
        }
        scope.launch {
            saveData {
                if (habitIsNowPerformed) {
                    dataSource.insertHabitPerformed(id, currentDomainState.focusedDay)
                } else {
                    dataSource.deleteHabitPerformed(id, currentDomainState.focusedDay)
                }
            }
        }
    }

    private fun updateHabitName(id: Long, name: String) {
        domainStateFlow.update { state ->
            state
                .copy(pendingChanges = state.pendingChanges + PendingChange.Habit(id))
                .updateFocusedDayDetails {
                    copy(
                        trackedHabits = trackedHabits.map { habit ->
                            if (habit.id == id) {
                                habit.copy(name = name)
                            } else {
                                habit
                            }
                        },
                        untrackedHabits = untrackedHabits.map { habit ->
                            if (habit.id == id) {
                                habit.copy(name = name)
                            } else {
                                habit
                            }
                        }
                    )
                }
        }
    }

    private suspend fun saveHabitName(id: Long, dayToSaveFrom: DayDetails = currentDomainState.focusedDayDetails) {
        saveData {
            dayToSaveFrom.trackedHabits.find { it.id == id }?.let { habit ->
                dataSource.updateHabitName(habit.name, habit.id)
            }
            dayToSaveFrom.untrackedHabits.find { it.id == id }?.let { habit ->
                dataSource.updateHabitName(habit.name, habit.id)
            }
            domainStateFlow.update { it.copy(pendingChanges = it.pendingChanges - PendingChange.Habit(id)) }
        }
    }

    private fun toggleTrackingHabit(id: Long, scope: CoroutineScope) {
        domainStateFlow.update { state ->
            val isNowTracked = state.focusedDayDetails.untrackedHabits.any { it.id == id }
            val updatedHabit = if (isNowTracked) {
                state.focusedDayDetails.untrackedHabits.find { it.id == id }?.copy(currentlyTracking = true)
                    ?: return@update state
            } else {
                val previouslyTrackedHabit = state.focusedDayDetails.trackedHabits.find { it.id == id }
                    ?: return@update state
                Habit(id, previouslyTrackedHabit.name, currentlyBuilding = false, currentlyTracking = false)
            }
            state.updateFocusedDayDetails {
                copy(
                    trackedHabits = if (isNowTracked) {
                        trackedHabits + TrackedHabit(
                            id = id,
                            name = updatedHabit.name,
                            frequency = HabitFrequency.None, // TODO: find a way to preserve previous habit frequency and trend
                            wasBuilding = false,
                            wasPerformed = false,
                            isNew = false
                        )
                    } else {
                        trackedHabits.filterNot { it.id == id }
                    },
                    untrackedHabits = if (isNowTracked) {
                        untrackedHabits.filterNot { it.id == id }
                    } else {
                        untrackedHabits + updatedHabit
                    }
                )
            }
        }
        scope.launch {
            saveData {
                dataSource.updateHabitIsTracked(
                    isTracked = currentDomainState.focusedDayDetails.trackedHabits.any { it.id == id },
                    id = id
                )
            }
        }
    }

    private fun toggleViewingUntrackedHabits() {
        domainStateFlow.update {
            it.updateFocusedDayDetails { copy(viewingUntrackedHabits = !viewingUntrackedHabits) }
        }
    }

    private fun TrackerDomainState.updateFocusedDayDetails(update: DayDetails.() -> DayDetails) = copy(
        dayDetailsMap = dayDetailsMap + (focusedDay to focusedDayDetails.update())
    )

    private inline fun <T> saveData(saveBlock: () -> T): T? {
        return try {
            saveBlock()
        } catch (e: DataSourceException) {
            // TODO: maybe change to a DayDetails-level error instead of a top-level error?
            domainStateFlow.update { it.copy(error = FailedToSave, isLoading = false) }
            null
        }
    }

    private inline fun MutableStateFlow<TrackerDomainState>.updateByReadingDataOrDefault(
        fallback: TrackerDomainState.() -> TrackerDomainState,
        update: TrackerDomainState.() -> TrackerDomainState
    ) {
        update { state ->
            try {
                update.invoke(state)
            } catch (e: DataSourceException) {
                fallback.invoke(state)
            }
        }
    }

    private inline fun <T> readDataOrDefault(fallback: () -> T, block: () -> T): T {
        return try {
            block()
        } catch (e: DataSourceException) {
            fallback()
        }
    }
}

private const val SHORT_TERM_HABIT_PROPENSITY_NUM_DAYS = 14
private const val MEDIUM_TERM_HABIT_PROPENSITY_NUM_DAYS = 42