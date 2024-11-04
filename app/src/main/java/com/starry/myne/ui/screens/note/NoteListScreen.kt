package com.starry.myne.ui.screens.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.starry.myne.R
import com.starry.myne.database.note.Note
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox

/**
 * Composable function to display the list of notes with options to delete or edit.
 *
 * @param navController A controller for navigating between screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(navController: NavController) {
    val view = LocalView.current
    val context = LocalContext.current
    val viewModel: NoteViewModel = hiltViewModel()

    val notes = viewModel.allNotes.observeAsState(listOf()).value
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val dialogState = remember { mutableStateOf(false) }
    val titleInput = remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { dialogState.value = true },
                modifier = Modifier
                    .offset(y = (-48).dp)
                    .padding(bottom = 16.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Notes")
            }
        }
    ) { paddingValues ->
        if (dialogState.value) {
            AlertDialog(
                onDismissRequest = { dialogState.value = false },
                title = { Text("Add Notes") },
                text = {
                    TextField(
                        value = titleInput.value,
                        onValueChange = { titleInput.value = it },
                        label = { Text("Title") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (titleInput.value.isNotEmpty()) {
                            val title = titleInput.value
                            coroutineScope.launch {
                                viewModel.addNote(title = title, text = "", thoughts = "")
                                snackBarHostState.showSnackbar("new notes added")
                            }
                            dialogState.value = false
                            titleInput.value = ""
                        }
                    }) {
                        Text("confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        dialogState.value = false
                        titleInput.value = "" // 重置输入框
                    }) {
                        Text("cancel")
                    }
                }
            )
        }
        NoteListContent(
            notes = notes,
            snackBarHostState = snackBarHostState,
            lazyListState = lazyListState,
            paddingValues = paddingValues,
            onDeleteClick = { note ->
                coroutineScope.launch {
                    viewModel.deleteNote(note)
                    snackBarHostState.showSnackbar("notes deleted")
                }
            },
            onNoteClick = { note ->
                navController.navigate("note_edit/${note.id}")
            }
        )
    }
}

/**
 * Composable function to display the list of notes.
 *
 * @param notes List of notes to display.
 * @param snackBarHostState Host state for the Snackbar.
 * @param onDeleteClick Callback when a note is deleted.
 * @param onNoteClick Callback when a note is clicked for editing.
 */
@Composable
private fun NoteListContent(
    notes: List<Note>,
    snackBarHostState: SnackbarHostState,
    lazyListState: LazyListState,
    paddingValues: PaddingValues,
    onDeleteClick: (Note) -> Unit,
    onNoteClick: (Note) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No Note Here", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                items(
                    count = notes.size,
                    key = { i -> notes[i].id }
                ) { i ->
                    val note: Note = notes[i]
                    SwipeableNoteItem(
                        note = note,
                        onDeleteClick = { onDeleteClick(note) },
                        onDetailClick = {
                            onNoteClick(note)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))

                }
            }
        }
    }
}

/**
 * Composable function to display a single note item with swipeable delete functionality.
 *
 * @param note The note to be displayed.
 * @param onDeleteClick Callback when the note is swiped to delete.
 * @param onDetailClick Callback when the note is clicked to edit.
 */
@Composable
private fun SwipeableNoteItem(
    note: Note,
    onDeleteClick: () -> Unit,
    onDetailClick: () -> Unit
) {
    val deleteAction = SwipeAction(
        icon = { painterResource(id = R.drawable.ic_delete) },
        background = MaterialTheme.colorScheme.error,
        onSwipe = { onDeleteClick() }
    )

    SwipeableActionsBox(
        startActions = listOf(deleteAction),
        swipeThreshold = 80.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        NoteCard(note = note, onDetailClick = onDetailClick)
    }
}

/**
 * Composable function to display a card view for a note.
 *
 * @param note The note to be displayed.
 * @param onDetailClick Callback when the note is clicked to view details.
 */
@Composable
fun NoteListItem(note: Note, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(note.text) },
        supportingContent = { Text(note.thoughts) },
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(8.dp)
    )
    Divider()
}

/**
 * Composable function to display a card view for a note.
 *
 * @param note The note to be displayed.
 * @param onDetailClick Callback when the note is clicked to view details.
 */
@Composable
private fun NoteCard(
    note: Note,
    onDetailClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onDetailClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (note.title.isEmpty()) "No Title" else note.title.take(1).uppercase(),
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifEmpty { "No Title" },
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.thoughts.ifEmpty { "No Content" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
fun NoteCardPreview() {
    NoteCard(
        Note(
            title = "c1",
            text = "",
            thoughts = ""
        )
    ) { }
}
