package io.chuckstein.doable.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring.DampingRatioNoBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import doable.composeapp.generated.resources.Res
import doable.composeapp.generated.resources.action_clear
import doable.composeapp.generated.resources.action_confirm
import doable.composeapp.generated.resources.action_go_to_today
import doable.composeapp.generated.resources.add_habit
import doable.composeapp.generated.resources.add_task
import doable.composeapp.generated.resources.habits
import doable.composeapp.generated.resources.high_priority
import doable.composeapp.generated.resources.journal
import doable.composeapp.generated.resources.low_priority
import doable.composeapp.generated.resources.medium_priority
import doable.composeapp.generated.resources.next_day_cd
import doable.composeapp.generated.resources.no_deadline
import doable.composeapp.generated.resources.no_tracked_habits_message
import doable.composeapp.generated.resources.previous_day_cd
import doable.composeapp.generated.resources.select_journal_entry
import doable.composeapp.generated.resources.tasks
import doable.composeapp.generated.resources.what_did_you_do_today
import io.chuckstein.doable.common.DatePickerTitleWithCancelButton
import io.chuckstein.doable.common.DoableIcon
import io.chuckstein.doable.common.DoableIconButton
import io.chuckstein.doable.common.EmptyIconButton
import io.chuckstein.doable.common.Error
import io.chuckstein.doable.common.IconState
import io.chuckstein.doable.common.IconTextButton
import io.chuckstein.doable.common.Icons
import io.chuckstein.doable.common.LoadingIndicator
import io.chuckstein.doable.common.TextModel
import io.chuckstein.doable.common.isEmpty
import io.chuckstein.doable.common.isKeyboardVisible
import io.chuckstein.doable.common.resolve
import io.chuckstein.doable.common.resolveText
import io.chuckstein.doable.common.toTextModel
import io.chuckstein.doable.tracker.CheckableItemMetadataState.HabitMetadataState
import io.chuckstein.doable.tracker.CheckableItemMetadataState.TaskMetadataState
import io.chuckstein.doable.tracker.CheckableItemOptionsState.HabitOptionsState
import io.chuckstein.doable.tracker.CheckableItemOptionsState.TaskOptionsState
import io.chuckstein.doable.tracker.TrackerEvent.AddTask
import io.chuckstein.doable.tracker.TrackerEvent.AddTrackedHabit
import io.chuckstein.doable.tracker.TrackerEvent.ToggleEditingTaskDeadline
import io.chuckstein.doable.tracker.TrackerEvent.ToggleEditingTaskPriority
import io.chuckstein.doable.tracker.TrackerEvent.ToggleSelectingDate
import io.chuckstein.doable.tracker.TrackerEvent.ToggleViewingOlderTasks
import io.chuckstein.doable.tracker.TrackerEvent.ToggleViewingUntrackedHabits
import io.chuckstein.doable.tracker.TrackerEvent.UpdateJournalNote
import io.chuckstein.doable.tracker.TrackerEvent.UpdateTaskDeadline
import io.chuckstein.doable.tracker.TrackerEvent.UpdateTaskPriority
import io.telereso.kmp.core.icons.resources.Add
import io.telereso.kmp.core.icons.resources.ChevronLeft
import io.telereso.kmp.core.icons.resources.ChevronRight
import io.telereso.kmp.core.icons.resources.KeyboardDoubleArrowDown
import io.telereso.kmp.core.icons.resources.KeyboardDoubleArrowUp
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TrackerScreen(state: TrackerUiState = TrackerUiState(), onEvent: (TrackerEvent) -> Unit = {}) {
    Surface {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            if (state.errorMessage != null) {
                Error(state.errorMessage)
            } else if (state.isLoading) {
                LoadingIndicator()
            } else {
                val pagerState = rememberPagerState(initialPage = state.days.lastIndex) { state.days.size }
                val focusedDay = state.days[pagerState.currentPage]

                LaunchedEffect(focusedDay.onFocusEvent) {
                    focusedDay.onFocusEvent.let(onEvent)
                }

                if (state.showDatePicker) {
                    TrackerDatePicker(state, pagerState, focusedDay, onEvent)
                }

                DateNavigationBar(state, pagerState, onEvent)
                HorizontalPager(pagerState, Modifier.weight(1f)) { index ->
                    DayTrackerCard(state.days[index], onEvent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackerDatePicker(
    trackerState: TrackerUiState,
    pagerState: PagerState,
    focusedDay: TrackerDayState,
    onEvent: (TrackerEvent) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = focusedDay.dateUtcMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in trackerState.trackedDatesAsUtcMillis
            override fun isSelectableYear(year: Int) = year in trackerState.trackedYears
        },
        yearRange = trackerState.trackedYears.min()..trackerState.trackedYears.max()
    )
    DatePickerDialog(
        onDismissRequest = { onEvent(ToggleSelectingDate) },
        confirmButton = {
            Button(
                onClick = {
                    val newFocusedDayIndex = trackerState.trackedDatesAsUtcMillis.indexOf(datePickerState.selectedDateMillis)
                    if (newFocusedDayIndex in 0 ..< pagerState.pageCount) {
                        pagerState.requestScrollToPage(newFocusedDayIndex)
                    }
                    onEvent(ToggleSelectingDate)
                }
            ) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    pagerState.requestScrollToPage(pagerState.pageCount - 1)
                    onEvent(ToggleSelectingDate)
                }
            ) {
                Text(stringResource(Res.string.action_go_to_today))
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = {
                DatePickerTitleWithCancelButton(
                    title = Res.string.select_journal_entry.toTextModel(),
                    onCancelClick = { onEvent(ToggleSelectingDate) }
                )
            }
        )
    }
}

@Composable
private fun DateNavigationBar(
    state: TrackerUiState,
    pagerState: PagerState,
    onEvent: (TrackerEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pageChangeAnimation = spring<Float>(DampingRatioNoBouncy, StiffnessLow)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        DoableIconButton(
            icon = Icons.ChevronLeft,
            contentDescription = stringResource(Res.string.previous_day_cd),
            enabled = state.previousDayButtonEnabled,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = pageChangeAnimation)
                }
            }
        )
        // TODO: change this to a horizontal slide animation, direction depending on whether we're going to the previous or next day
        Crossfade(state.header.resolveText(), Modifier.weight(1f)) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onEvent(ToggleSelectingDate) }
            )
        }
        DoableIconButton(
            icon = Icons.ChevronRight,
            contentDescription = stringResource(Res.string.next_day_cd),
            enabled = state.nextDayButtonEnabled,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = pageChangeAnimation)
                }
            }
        )
    }
}

@Composable
private fun DayTrackerCard(state: TrackerDayState, onEvent: (TrackerEvent) -> Unit) {
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardIsVisible = isKeyboardVisible()

    LaunchedEffect(pagerState.currentPage) {
        focusManager.clearFocus()
    }

    Card(
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (keyboardIsVisible) 0.dp else 16.dp,
            bottomEnd = if (keyboardIsVisible) 0.dp else 16.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = if (keyboardIsVisible) 0.dp else 8.dp)
    ) {
        Column(
            Modifier.fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(Res.string.tasks)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(Res.string.journal)) }
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    text = { Text(stringResource(Res.string.habits)) }
                )
            }
            HorizontalPager(pagerState, modifier = Modifier.fillMaxSize()) { index ->
                if (state.errorMessage != null) {
                    Error(state.errorMessage)
                } else if (state.isLoading) {
                    LoadingIndicator()
                } else {
                    when (index) {
                        0 -> TasksTab(state.tasksTab, onEvent)
                        1 -> JournalTab(state.journalTab, onEvent)
                        2 -> HabitsTab(state.habitsTab, onEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksTab(state: TasksTabState, onEvent: (TrackerEvent) -> Unit) {
    val listState = rememberLazyListState()
    var cachedTaskIds by remember { mutableStateOf(state.tasks.map { it.id }.toSet()) }
    val taskIds = state.tasks.map { it.id }

    LaunchedEffect(taskIds) {
        val newTaskId = taskIds.minus(cachedTaskIds).firstOrNull()
        val visibleTaskIds = listState.layoutInfo.visibleItemsInfo.map { it.key }
        if (newTaskId != null && newTaskId !in visibleTaskIds) {
            // ensure newly added tasks are always visible
            listState.animateScrollToItem(state.tasks.indexOfFirst { it.id == newTaskId })
        }
        cachedTaskIds = taskIds.toSet()
    }

    LazyColumn(Modifier.fillMaxSize(), listState) {
        items(state.tasks, key = { it.id }) { task ->
            CheckableItem(task, Modifier.animateItem(), onEvent)
        }
        item {
            PseudoCheckableItem(
                leftSlot = { Icon(painterResource(Icons.Add), contentDescription = null) },
                text = Res.string.add_task.toTextModel(),
                modifier = Modifier.clickable { onEvent(AddTask) }
            )
        }
        state.toggleViewOlderTasksButtonState?.let { buttonState ->
            item {
                IconTextButton(buttonState, Modifier.padding(top = checkableItemListButtonTopPadding)) {
                    onEvent(ToggleViewingOlderTasks)
                }
            }
        }
        items(state.olderTasks, key = { it.id }) { task ->
            // TODO: when one first appears (due to older tasks becoming toggled), scroll to it... see similar comment below about untracked habits
            CheckableItem(task, Modifier.animateItem(), onEvent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalTab(state: JournalTabState, onEvent: (TrackerEvent) -> Unit) {
    val listState = rememberLazyListState()
    val lastListIndex = state.journalTasks.size + state.journalHabits.size

    LazyColumn(Modifier.fillMaxSize(), listState) {
        items(state.journalTasks, key = { it.id }) { task ->
            CheckableItem(task, Modifier.animateItem(), onEvent)
        }
        items(state.journalHabits, key = { it.id }) { habit ->
            CheckableItem(habit, Modifier.animateItem(), onEvent)
        }
        // TODO: add MagicBar Composable
        item {
            val textStyle = LocalTextStyle.current
            var cachedTextModel by remember { mutableStateOf(state.note) }
            val cachedText = cachedTextModel.resolveText()
            val currentText = state.note.resolveText()
            LaunchedEffect(currentText, lastListIndex) {
                val newTextAppended = currentText.length > cachedText.length && currentText.indexOf(cachedText) == 0
                if (newTextAppended && listState.canScrollForward) {
                    listState.scrollToItem(lastListIndex, scrollOffset = Int.MAX_VALUE)
                }
                cachedTextModel = state.note
            }

            val interactionSource = remember { MutableInteractionSource() }
            BasicTextField(
                value = currentText,
                onValueChange = { onEvent(UpdateJournalNote(it)) },
                textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences),
                interactionSource = interactionSource,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 14.dp)
                    .fillMaxWidth(),
            ) { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = currentText,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = false,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
                    placeholder = {
                        Text(stringResource(Res.string.what_did_you_do_today), style = textStyle)
                    }
                )
            }
        }
    }
}

@Composable
private fun HabitsTab(state: HabitsTabState, onEvent: (TrackerEvent) -> Unit) {
    val listState = rememberLazyListState()
    var cachedTrackedHabitIds by remember { mutableStateOf(state.trackedHabits.map { it.id }.toSet()) }
    val trackedHabitIds = state.trackedHabits.map { it.id }

    LaunchedEffect(trackedHabitIds) {
        val newHabitId = trackedHabitIds.minus(cachedTrackedHabitIds).firstOrNull()
        val visibleHabitIds = listState.layoutInfo.visibleItemsInfo.map { it.key }
        if (newHabitId != null && newHabitId !in visibleHabitIds) {
            // ensure newly added habits are always visible
            listState.animateScrollToItem(state.trackedHabits.indexOfFirst { it.id == newHabitId })
        }
        cachedTrackedHabitIds = trackedHabitIds.toSet()
    }

    LazyColumn(Modifier.fillMaxSize(), listState) {
        items(state.trackedHabits, key = { it.id }) { habit ->
            CheckableItem(habit, Modifier.animateItem(), onEvent)
        }
        if (state.trackedHabits.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.no_tracked_habits_message),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        if (state.showAddHabitButton) {
            item {
                PseudoCheckableItem(
                    leftSlot = { Icon(painterResource(Icons.Add), contentDescription = null) },
                    text = Res.string.add_habit.toTextModel(),
                    modifier = Modifier.clickable { onEvent(AddTrackedHabit) }
                )
            }
        }
        state.toggleViewUntrackedHabitsButtonState?.let { buttonState ->
            item {
                IconTextButton(buttonState,  Modifier.padding(top = checkableItemListButtonTopPadding)) {
                    onEvent(ToggleViewingUntrackedHabits)
                }
            }
        }
        items(state.untrackedHabits, key = { it.id }) { habit ->
            // TODO: when one first appears (due to habits becoming toggled), scroll to it... could maybe do this with a pending one-off-event structure in the domain state when handling ToggleViewingUntrackedHabits, but also might be simpler if there's a way to keep it in the view layer
            CheckableItem(habit, Modifier.animateItem(), onEvent)
        }
    }
}

@Composable
private fun CheckableItem(state: CheckableItemState, modifier: Modifier = Modifier, onEvent: (TrackerEvent) -> Unit) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var itemIsFocused by remember { mutableStateOf(false) }
    val backgroundColor = if (itemIsFocused) focusedItemColor() else Color.Transparent
    var keyboardIsVisible by remember { mutableStateOf(false) }
    val keyboardIsNowVisible = isKeyboardVisible()

    if (itemIsFocused && !state.optionsState.optionsShouldStayFocused && keyboardIsVisible && !keyboardIsNowVisible) {
        LaunchedEffect(Unit) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(keyboardIsNowVisible) {
        keyboardIsVisible = keyboardIsNowVisible
    }

    if (state.autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            state.autoFocusDoneEvent?.let(onEvent)
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = checkableItemVerticalPadding)
            .background(color = backgroundColor)
            .onFocusChanged { newState: FocusState ->
                val itemIsNowFocused = newState.isFocused || newState.hasFocus
                if (itemIsFocused && !itemIsNowFocused) {
                    state.loseFocusEvent?.let(onEvent)
                    state.toggleEditingEvent?.let(onEvent)
                } else if (!itemIsFocused && itemIsNowFocused) {
                    state.toggleEditingEvent?.let(onEvent)
                }
                itemIsFocused = itemIsNowFocused
            },
    ) {
        AnimatedVisibility(
            visible = state.optionsState != CheckableItemOptionsState.Empty,
            enter = expandIn(expandFrom = Alignment.BottomCenter, initialSize = { IntSize(it.width, 0) }),
            exit = shrinkOut(shrinkTowards = Alignment.BottomCenter, targetSize = { IntSize(it.width, 0) })
        ) {
            var latestOptionsState by remember { mutableStateOf(state.optionsState) }
            if (state.optionsState != CheckableItemOptionsState.Empty) {
                latestOptionsState = state.optionsState
            }
            CheckableItemOptions(latestOptionsState, onEvent)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = state.checked,
                onCheckedChange = { state.toggleCheckedEvent?.let(onEvent) },
                enabled = state.checkboxEnabled
            )
            Column(Modifier.weight(1f)) {
                BasicTextField(
                    value = state.name.resolveText(),
                    onValueChange = { state.updateNameEvent?.invoke(it)?.let(onEvent) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = state.textAlpha),
                        textDecoration = state.textDecoration
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = if (state.nextActionEvent != null) ImeAction.Next else ImeAction.Unspecified
                    ),
                    keyboardActions = KeyboardActions(onNext = { state.nextActionEvent?.let(onEvent) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (state.name.isEmpty() && keyEvent.key == Key.Backspace) {
                                state.backspaceWhenEmptyEvent?.let(onEvent)
                                true
                            } else {
                                false
                            }
                        }
                )
                state.infoText?.let {
                    Text(
                        text = it.resolveText(),
                        style = MaterialTheme.typography.labelLarge,
                        color = state.infoTextColor.resolve().copy(alpha = state.textAlpha)
                    )
                }
            }
            CheckableItemMetadata(state.metadata)
            Crossfade(state.endIcon) { icon ->
                if (icon == null) {
                    EmptyIconButton()
                } else {
                    DoableIconButton(icon, onClick = { state.endIconClickEvent?.let(onEvent) })
                }
            }
        }
    }
}

@Composable
private fun CheckableItemMetadata(state: CheckableItemMetadataState) {
    when (state) {
        is CheckableItemMetadataState.Empty -> {}
        is TaskMetadataState -> MetadataIcon(state.priorityIcon)
        is HabitMetadataState -> MetadataIcon(state.trendIcon)
    }
}

@Composable
private fun MetadataIcon(iconState: IconState?) {
    Crossfade(iconState) { state ->
        state?.let {
            DoableIcon(it, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun CheckableItemOptions(state: CheckableItemOptionsState, onEvent: (TrackerEvent) -> Unit) {
    when (state) {
        is CheckableItemOptionsState.Empty -> {}
        is TaskOptionsState -> TaskOptionsBar(state, onEvent)
        is HabitOptionsState -> {} // TODO
    }
}

@Composable
private fun TaskOptionsBar(state: TaskOptionsState, onEvent: (TrackerEvent) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = focusedItemColor()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                TextButton(onClick = { onEvent(ToggleEditingTaskPriority) }) {
                    Text(text = state.priorityLabel.resolveText())
                }
                TaskPriorityDropdown(state, onEvent)
            }
            Box {
                TextButton(onClick = { onEvent(ToggleEditingTaskDeadline) }) {
                    Text(text = state.deadlineLabel.resolveText())
                }
                if (state.showDeadlineDatePicker) {
                    TaskDeadlineDatePicker(state, onEvent)
                }
            }
        }
    }
}

@Composable
private fun TaskPriorityDropdown(
    state: TaskOptionsState,
    onEvent: (TrackerEvent) -> Unit
) {
    DropdownMenu(
        expanded = state.showPriorityDropdown,
        onDismissRequest = { onEvent(ToggleEditingTaskPriority) }
    ) {
        listOf(
            Triple(TaskPriority.High, Res.string.high_priority, Icons.KeyboardDoubleArrowUp),
            Triple(TaskPriority.Medium, Res.string.medium_priority, null),
            Triple(TaskPriority.Low, Res.string.low_priority, Icons.KeyboardDoubleArrowDown)
        ).forEach { (priority, label, icon) ->

            DropdownMenuItem(
                text = { Text(stringResource(label)) },
                trailingIcon = icon?.let {
                    { Icon(painterResource(it), contentDescription = null) }
                },
                onClick = {
                    onEvent(UpdateTaskPriority(state.taskId, priority))
                    onEvent(ToggleEditingTaskPriority)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDeadlineDatePicker(
    state: TaskOptionsState,
    onEvent: (TrackerEvent) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.deadlineUtcMillis,
    )
    DatePickerDialog(
        onDismissRequest = { onEvent(ToggleEditingTaskDeadline) },
        confirmButton = {
            Button(
                onClick = {
                    val selectedDateInstant = datePickerState.selectedDateMillis?.let { Instant.fromEpochMilliseconds(it) }
                    val selectedDate = selectedDateInstant?.toLocalDateTime(TimeZone.UTC)?.date
                    onEvent(UpdateTaskDeadline(state.taskId, selectedDate))
                    onEvent(ToggleEditingTaskDeadline)
                }
            ) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onEvent(UpdateTaskDeadline(state.taskId, deadline = null))
                    onEvent(ToggleEditingTaskDeadline)
                }
            ) {
                Text(stringResource(Res.string.action_clear))
            }
        }
    ) {
        val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
            title = {
                DatePickerTitleWithCancelButton(
                    title = state.deadlineDatePickerTitle,
                    onCancelClick = { onEvent(ToggleEditingTaskDeadline) }
                )
            },
            headline = {
                Box(Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)) {
                    if (datePickerState.selectedDateMillis == null) {
                        Text(stringResource(Res.string.no_deadline))
                    } else {
                        DatePickerDefaults.DatePickerHeadline(
                            selectedDateMillis = datePickerState.selectedDateMillis,
                            displayMode = datePickerState.displayMode,
                            dateFormatter = dateFormatter,
                        )
                    }
                }
            }
        )
    }
}

/**
 * An item that lives in a list of [CheckableItem]s, but is not checkable itself.
 * It will be laid out similarly to a [CheckableItem] so that the list looks uniform.
 *
 * @param leftSlot the content that will be in place of the checkbox
 * @param text some non-editable text that will have the same appearance of the editable text of a [CheckableItem]
 */
@Composable
private fun PseudoCheckableItem(leftSlot: @Composable () -> Unit, text: TextModel, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = checkableItemVerticalPadding),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Checkbox(checked = false, onCheckedChange = {}, enabled = false, modifier = Modifier.alpha(0f))
            leftSlot()
        }
        BasicTextField(
            value = text.resolveText(),
            onValueChange = {},
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            enabled = false,
            modifier = Modifier.weight(1f)
        )
        EmptyIconButton()
    }
}

@Composable
private fun focusedItemColor() = MaterialTheme.colorScheme.surfaceContainerHigh

private val checkableItemVerticalPadding = 4.dp
private val checkableItemListButtonTopPadding = 24.dp