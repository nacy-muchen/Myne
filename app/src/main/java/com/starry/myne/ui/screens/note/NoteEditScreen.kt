package com.starry.myne.ui.screens.note

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.starry.myne.R
import com.starry.myne.database.note.NoteEntry
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Composable function to edit an existing note or create a new one.
 *
 * @param navController A controller for navigating between screens.
 * @param noteId The ID of the note to be edited. If null, a new note will be created.
 * @param initialText The initial text to display in the text field.
 * @param initialThoughts The initial thoughts to display in the thoughts field.
 */
@Composable
fun NoteEditScreen(
    navController: NavController,
    noteId: Long? = null,
    initialText: String = "",
    initialThoughts: String = ""
) {

    val viewModel: NoteViewModel = hiltViewModel()
    val notes by viewModel.allNotes.observeAsState(emptyList())
    val showDialog = remember { mutableStateOf(false) }
    val showNoNotesDialog = remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(TextFieldValue(initialText)) }
    var newThought by remember { mutableStateOf(TextFieldValue(initialThoughts)) }
    var entries by remember { mutableStateOf(mutableListOf<NoteEntry>()) }
    val snackBarHostState = remember { SnackbarHostState() }
    var showBackgroundPicker by remember { mutableStateOf(false) } // 控制背景选择栏的显示


    val backgroundOptions = listOf(
        R.drawable.p1,
        R.drawable.p2,
        R.drawable.p3,
        R.drawable.p4
    )
    var selectedBackground by remember { mutableStateOf(backgroundOptions.first()) }

    // 如果是编辑已存在的笔记，加载其内容
    if (noteId != null) {
        // Load the existing note
        val existingNote = notes.find { it.id == noteId }
        existingNote?.let {
            title = TextFieldValue(it.title) // 假设这里 title 是标题
            entries = Json.decodeFromString<List<NoteEntry>>(it.entriesJson).toMutableList()
            selectedBackground = it.background
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showBackgroundPicker = !showBackgroundPicker }) {
                        Icon( painterResource(id = R.drawable.ic_background_picker), contentDescription = "Toggle Background Picker")
                    }
                    IconButton(onClick = {
                        navController.navigate("note_preview/${noteId ?: 0}")
                    }) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview")
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(id = selectedBackground),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    if (showBackgroundPicker) {
                        // 背景选择器
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(backgroundOptions) { background ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clickable { selectedBackground = background }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = background),
                                        contentDescription = "Background Option",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    var showDialogForIndex by remember { mutableStateOf(-1) }

                    entries.forEachIndexed { index, entry ->
                        var showDeleteButton by remember { mutableStateOf(false) } // 控制删除按钮是否显示

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            showDeleteButton = true // 长按显示删除按钮
                                        },
                                        onTap = {
                                            // 点击切换删除按钮显示状态
                                            showDeleteButton = !showDeleteButton
                                        }
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.text,
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )

                                if (showDeleteButton) {
                                    Button(
                                        onClick = {
                                            showDialogForIndex = index // 点击删除按钮显示确认对话框
                                        },
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text("Delete")
                                    }
                                }
                            }

                            TextField(
                                value = entry.thoughts,
                                onValueChange = { newThought ->
                                    val updatedEntries = entries.toMutableList()
                                    updatedEntries[index] = entry.copy(thoughts = newThought)
                                    entries = updatedEntries
                                },
                                label = { Text("Thought ${index + 1}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Divider(color = Color.Gray, thickness = 1.dp)

                        if (showDialogForIndex == index) {
                            AlertDialog(
                                onDismissRequest = { showDialogForIndex = -1 }, // 点击对话框外部关闭对话框
                                title = { Text("Delete Entry") },
                                text = { Text("Are you sure you want to delete this entry?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        // 删除 entry
                                        val updatedEntries = entries.toMutableList()
                                        updatedEntries.removeAt(index)
                                        entries = updatedEntries
                                        showDeleteButton = false // 隐藏删除按钮
                                        showDialogForIndex = -1 // 关闭确认对话框
                                    }) {
                                        Text("Yes")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDialogForIndex = -1 }) {
                                        Text("No")
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.viewModelScope.launch {
                                val note = notes.find { it.id == noteId }
                                if (note != null) {
                                    val updatedNote = note.copy(
                                        title = title.text,
                                        entriesJson = Json.encodeToString(entries), // 将更新后的 entries 保存为 JSON
                                        background = selectedBackground
                                    )
                                    viewModel.updateNote(updatedNote)
                                    snackBarHostState.showSnackbar("Saved")
                                }
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save")
                    }
                }

            }
        }
    )
}

