package io.chuckstein.doable.database

import co.touchlab.kermit.Logger
import io.chuckstein.doable.database.adapters.dayOfWeekAdapter
import io.chuckstein.doable.database.adapters.habitFrequencyAdapter
import io.chuckstein.doable.database.adapters.habitTrendAdapter
import io.chuckstein.doable.database.adapters.localDateAdapter
import io.chuckstein.doable.database.adapters.moodAdapter
import io.chuckstein.doable.database.adapters.taskPriorityAdapter
import io.chuckstein.doable.tracker.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class DoableDataSource(databaseDriverFactory: DatabaseDriverFactory) {

    private val database = DoableDatabase(
        driver = databaseDriverFactory.createDriver(),
        JournalEntryAdapter = JournalEntry.Adapter(dateAdapter = localDateAdapter, moodAdapter = moodAdapter),
        TaskAdapter = Task.Adapter(
            dateCreatedAdapter = localDateAdapter,
            dateCompletedAdapter = localDateAdapter,
            deadlineAdapter = localDateAdapter,
            priorityAdapter = taskPriorityAdapter,
        ),
        HabitPerformedAdapter = HabitPerformed.Adapter(
            dateAdapter = localDateAdapter,
            dayOfWeekAdapter = dayOfWeekAdapter
        ),
        HabitStatusAdapter = HabitStatus.Adapter(
            dateAdapter = localDateAdapter,
            frequencyAdapter = habitFrequencyAdapter,
            trendAdapter = habitTrendAdapter
        )
    )

    private val queries = database.doableDatabaseQueries

    suspend fun insertJournalEntry(date: LocalDate) = runQuery("insert journal entry for $date") {
        queries.insertJournalEntry(date)
    }

    suspend fun insertJournalEntries(dates: List<LocalDate>) = runQuery("insert journal entries for $dates") {
        database.transaction {
            dates.forEach {
                queries.insertJournalEntry(it)
            }
        }
    }

    suspend fun updateJournalEntry(journalEntry: JournalEntry) = runQuery("update journal entry - $journalEntry") {
        queries.updateJournalEntry(
            journalEntry.note, journalEntry.mood, journalEntry.isStarred,
            journalEntry.habitsCalculated, journalEntry.date
        )
    }

    suspend fun selectJournalEntryForDate(date: LocalDate): JournalEntry? = runQuery("select journal entry for $date") {
        queries.selectJournalEntryForDate(date).executeAsOneOrNull()
    }

    suspend fun selectFirstJournalEntry(): JournalEntry? = runQuery("select first journal entry") {
        queries.selectFirstJournalEntry().executeAsOneOrNull()
    }

    suspend fun selectLatestJournalEntry(): JournalEntry? = runQuery("select latest journal entry") {
        queries.selectLatestJournalEntry().executeAsOneOrNull()
    }

    suspend fun selectJournalDatesWithoutHabitStatuses(): List<LocalDate> =
        runQuery("select journal dates without habit statuses") {
            queries.selectJournalDatesWithoutHabitStatuses().executeAsList()
        }

    suspend fun insertTask(
        name: String,
        dateCreated: LocalDate,
        priority: TaskPriority = TaskPriority.Medium,
        deadline: LocalDate? = null
    ): Task = runQuery("insert task - $name") {
        database.transactionWithResult {
            queries.insertTask(name, dateCreated, priority, deadline)
            val newTaskId = queries.selectLastInsertRowId().executeAsOne()
            queries.selectTask(newTaskId).executeAsOne()
        }
    }

    suspend fun updateTask(task: Task) = runQuery("update task $task") {
        queries.updateTask(task.name, task.dateCompleted, task.priority, task.deadline, task.id)
    }

    suspend fun deleteTask(id: Long) = runQuery("delete task $id") {
        queries.deleteTask(id)
    }

    suspend fun selectAllTasks(): List<Task> = runQuery("select all tasks") {
        queries.selectAllTasks().executeAsList()
    }

    suspend fun insertHabit(name: String): Habit = runQuery("insert habit - $name") {
        database.transactionWithResult {
            queries.insertHabit(name)
            val newHabitId = queries.selectLastInsertRowId().executeAsOne()
            queries.selectHabit(newHabitId).executeAsOne()
        }
    }

    suspend fun updateHabitName(name: String, id: Long) = runQuery("update habit $id name to $name") {
        queries.updateHabitName(name, id)
    }

    suspend fun updateHabitIsTracked(isTracked: Boolean, id: Long) = runQuery("update habit $id is tracked to $isTracked") {
        queries.updateHabitIsTracked(isTracked, id)
    }

    suspend fun deleteHabit(id: Long) = runQuery("delete habit $id") {
        queries.deleteHabit(id)
    }

    suspend fun selectAllHabits(): List<Habit> = runQuery("select all habits") {
        queries.selectAllHabits().executeAsList()
    }

    suspend fun insertHabitPerformed(
        habitId: Long,
        date: LocalDate
    ) = runQuery("insert habit $habitId performed on $date") {
        queries.insertHabitPerformed(habitId, date, date.dayOfWeek)
    }

    suspend fun deleteHabitPerformed(
        habitId: Long,
        date: LocalDate
    ) = runQuery("delete habit $habitId performed on $date") {
        queries.deleteHabitPerformed(habitId, date)
    }

    suspend fun selectAllHabitIdsPerformedOnDate(
        date: LocalDate
    ): List<Long> = runQuery("select all habit ids performed on $date") {
        queries.selectAllHabitIdsPerformedOnDate(date).executeAsList()
    }

    suspend fun selectNumTimesHabitPerformedDuringDates(
        habitId: Long,
        dates: List<LocalDate>
    ) = runQuery("select num times habit $habitId performed during dates $dates") {
        queries.selectTimesHabitPerformedDuringDates(habitId, dates).executeAsList().size
    }

    suspend fun selectMostRecentDatesHabitPerformed(
        habitId: Long,
        numDates: Long,
        referenceDate: LocalDate
    ) = runQuery("select most recent $numDates dates habit $habitId performed as of $referenceDate") {
        queries.selectMostRecentDatesHabitPerformed(habitId, referenceDate, numDates).executeAsList()
    }

    suspend fun insertHabitStatusesForDate(
        date: LocalDate,
        habitStatuses: List<HabitStatus>
    ) = runQuery("insert habit statuses for $date: $habitStatuses") {
        require(habitStatuses.all { it.date == date }) { "All habit statuses must have the given date $date" }
        database.transaction {
            habitStatuses.forEach { habitStatus ->
                queries.insertOrReplaceHabitStatus(
                    habitStatus.habitId, date, habitStatus.frequency, habitStatus.trend, habitStatus.wasBuilding
                )
            }
            queries.setJournalEntryHabitsCalculatedTrue(date)
        }
    }

    suspend fun insertOrReplaceHabitStatuses(
        habitStatuses: List<HabitStatus>
    ) = runQuery("insert or replace habit statuses: $habitStatuses") {
        database.transaction {
            habitStatuses.forEach { habitStatus ->
                queries.insertOrReplaceHabitStatus(
                    habitStatus.habitId, habitStatus.date, habitStatus.frequency, habitStatus.trend, habitStatus.wasBuilding
                )
            }
        }
    }

    suspend fun selectAllHabitStatusesForDate(
        date: LocalDate
    ): List<HabitStatusDetails> = runQuery("select all habit statuses for $date") {
        queries.habitStatusDetails(date).executeAsList()
    }

    suspend fun selectAllHabitStatuses(): List<HabitStatus> = runQuery("select all habit statuses") {
        queries.selectAllHabitStatuses().executeAsList()
    }

    suspend fun doesAnyHabitStatusExistForHabit(
        habitId: Long
    ) = runQuery("does any habit status exist for habit $habitId") {
        queries.doesAnyHabitStatusExistForHabit(habitId).executeAsOne()
    }

    suspend fun deleteHabitStatus(
        habitId: Long,
        date: LocalDate
    ) = runQuery("delete habit status for habit $habitId on $date") {
        queries.deleteHabitStatus(habitId, date)
    }

    /**
     * Wraps any database queries to ensure they run on an IO thread and can only throw [DataSourceException].
     */
    private suspend fun <T> runQuery(description: String, query: () -> T): T = withContext(Dispatchers.IO) {
        Logger.i("Running database query $description")
        runCatching { query() }.getOrElse { exception ->
            Logger.e(exception) { "Failed to run database query: $description" }
            throw DataSourceException(exception.message, exception)
        }
    }
}

/**
 * Wraps any exception that occurs while querying [DoableDataSource], so that this is the only type you need to catch.
 */
class DataSourceException(message: String?, cause: Throwable?) : RuntimeException(message, cause)
