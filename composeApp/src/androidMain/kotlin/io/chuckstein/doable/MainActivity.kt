package io.chuckstein.doable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.chuckstein.doable.common.IconState
import io.chuckstein.doable.common.Icons
import io.chuckstein.doable.common.toTextModel
import io.chuckstein.doable.theme.DoableTheme
import io.chuckstein.doable.tracker.CheckableItemState
import io.chuckstein.doable.tracker.HabitsTabState
import io.chuckstein.doable.tracker.JournalTabState
import io.chuckstein.doable.tracker.TasksTabState
import io.chuckstein.doable.tracker.TrackerDayState
import io.chuckstein.doable.tracker.TrackerEvent.SavePendingChanges
import io.chuckstein.doable.tracker.TrackerEvent.ToggleHabitPerformed
import io.chuckstein.doable.tracker.TrackerEvent.ToggleTaskCompleted
import io.chuckstein.doable.tracker.TrackerScreen
import io.chuckstein.doable.tracker.TrackerUiState
import io.chuckstein.doable.tracker.TrackerViewModel
import io.telereso.kmp.core.icons.resources.Close
import io.telereso.kmp.core.icons.resources.RemoveCircleOutline
import io.telereso.kmp.core.icons.resources.VisibilityOff
import kotlinx.datetime.LocalDate
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val trackerViewModel: TrackerViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            DoableTheme {
                val uiState by trackerViewModel.uiState.collectAsStateWithLifecycle()
                TrackerScreen(uiState) { event ->
                    trackerViewModel.processEvent(event)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        trackerViewModel.processEvent(SavePendingChanges)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    TrackerScreen(mockTrackerUiState)
}

val mockTrackerUiState: TrackerUiState = TrackerUiState(
    isLoading = false,
    header = "Saturday, January 25th".toTextModel(),
    days = listOf(
        TrackerDayState(
            date = LocalDate(2025, 1, 25),
            isLoading = false,
            tasksTab = TasksTabState(
                tasks = listOf(
                    CheckableItemState(
                        id = 1,
                        checked = true,
                        name = "Grocery Shopping".toTextModel(),
                        endIcon = IconState(Icons.Close, null),
                        toggleCheckedEvent = ToggleTaskCompleted(1)
                    ),
                    CheckableItemState(
                        id = 2,
                        checked = true,
                        name = "Book Doctor Appointment".toTextModel(),
                        endIcon = IconState(Icons.Close, null),
                        toggleCheckedEvent = ToggleTaskCompleted(2)
                    ),
                    CheckableItemState(
                        id = 3,
                        checked = false,
                        name = "Pay Bills".toTextModel(),
                        endIcon = IconState(Icons.Close, null),
                        toggleCheckedEvent = ToggleTaskCompleted(3)
                    )
                )
            ),
            journalTab = JournalTabState(
                note = """
                    Today was a productive day! I went grocery shopping and stocked up on eggs since I finally managed
                    to find some for a reasonable price. I also booked my doctor appointment for next week. I was on
                    hold for a long time which was frustrating, and I managed to decompress afterwards by curling up
                    on the couch with a jazz record and my cat by my side, to read some of my book. Oh man, is Children
                    of Dune getting crazy. What the hell is happening to Leto? Anwyay, didn't feel like meditating today
                    but at least I managed to check some things off my to-do list.
                """.trimIndent().toTextModel(),
                isStarred = true,
                journalTasks = listOf(
                    CheckableItemState(
                        id = 1,
                        checked = true,
                        name = "Grocery Shopping".toTextModel(),
                        endIcon = null,
                        toggleCheckedEvent = ToggleTaskCompleted(1)
                    ),
                    CheckableItemState(
                        id = 2,
                        checked = true,
                        name = "Book Doctor Appointment".toTextModel(),
                        endIcon = null,
                        toggleCheckedEvent = ToggleTaskCompleted(2)
                    ),
                ),
                journalHabits = listOf(
                    CheckableItemState(
                        id = 1,
                        checked = true,
                        name = "Read for 30 minutes".toTextModel(),
                        endIcon = null,
                        toggleCheckedEvent = ToggleHabitPerformed(1)
                    ),
                    CheckableItemState(
                        id = 2,
                        checked = false,
                        name = "Meditate".toTextModel(),
                        endIcon = IconState(Icons.VisibilityOff, null),
                        toggleCheckedEvent = ToggleHabitPerformed(2)
                    ),
                )
            ),
            habitsTab = HabitsTabState(
                trackedHabits = listOf(
                    CheckableItemState(
                        id = 1,
                        checked = true,
                        name = "Read for 30 minutes".toTextModel(),
                        endIcon = IconState(Icons.RemoveCircleOutline, null),
                        toggleCheckedEvent = ToggleHabitPerformed(1)
                    ),
                    CheckableItemState(
                        id = 2,
                        checked = false,
                        name = "Meditate".toTextModel(),
                        endIcon = IconState(Icons.RemoveCircleOutline, null),
                        toggleCheckedEvent = ToggleHabitPerformed(2)
                    ),
                    CheckableItemState(
                        id = 3,
                        checked = false,
                        name = "New Habit Added Today".toTextModel(),
                        endIcon = IconState(Icons.Close, null),
                        toggleCheckedEvent = ToggleHabitPerformed(3)
                    ),
                ),
                showAddHabitButton = true,
                toggleViewUntrackedHabitsButtonState = null
            )
        )
    )
)