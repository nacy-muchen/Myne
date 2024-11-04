package com.starry.myne.ui.screens.note

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


@Composable
fun NoteEditScreen(
    navController: NavController,
    noteId: Long? = null,
    initialText: String = "",
    initialThoughts: String = ""
) {
    //var title by remember { mutableStateOf(TextFieldValue(initialText)) }
    //var thoughts by remember { mutableStateOf(TextFieldValue(initialThoughts)) }
    val viewModel: NoteViewModel = hiltViewModel()
    //val context = LocalContext.current
    val notes by viewModel.allNotes.observeAsState(emptyList())
    val showDialog = remember { mutableStateOf(false) }
    val showNoNotesDialog = remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(TextFieldValue(initialText)) }
    var newThought by remember { mutableStateOf(TextFieldValue(initialThoughts)) }
    var entries by remember { mutableStateOf(mutableListOf<Pair<String, String>>()) }
    val snackBarHostState = remember { SnackbarHostState() }
    // 如果是编辑已存在的笔记，加载其内容
    if (noteId != null) {
        // Load the existing note
        val existingNote = notes.find { it.id == noteId }
        existingNote?.let {
            title = TextFieldValue(it.title) // 假设这里 title 是标题
            newThought = TextFieldValue(it.thoughts) // 根据你的设计，这里可以处理
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
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text("Selected Text:", style = MaterialTheme.typography.h6)
                val existingNote = notes.find { it.id == noteId }
                if (existingNote != null) {
                    Text(existingNote.text, color = MaterialTheme.colors.onBackground)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Your thought:", style = MaterialTheme.typography.h6)
                TextField(
                    value = newThought,
                    onValueChange = { newThought = it },
                    label = { Text("Thought") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            val note = notes.find { it.id == noteId }
                            if (note != null) {
                                viewModel.updateNote(
                                    note.copy(title = title.text, thoughts = newThought.text)
                                )
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

//            // 显示笔记选择对话框
//            if (showDialog.value) {
//                AlertDialog(
//                    onDismissRequest = { showDialog.value = false },
//                    title = { Text("Choose a note") },
//                    text = {
//                        Column {
//                            notes.forEach { note ->
//                                Text(
//                                    text = note.text,
//                                    modifier = Modifier
//                                        .clickable {
//                                            viewModel.addTextToExistingNote(note, selectedText)
//                                            showDialog.value = false
//                                            navController.popBackStack()
//                                        }
//                                        .padding(8.dp)
//                                )
//                                Divider()
//                            }
//                        }
//                    },
//                    confirmButton = {
//                        TextButton(onClick = { showDialog.value = false }) {
//                            Text("Cancel")
//                        }
//                    }
//                )
//            }
//            // 没有笔记时的提示对话框
//            if (showNoNotesDialog.value) {
//                AlertDialog(
//                    onDismissRequest = { showNoNotesDialog.value = false },
//                    title = { Text("Tip") },
//                    text = { Text("No node, please add") },
//                    confirmButton = {
//                        TextButton(onClick = { showNoNotesDialog.value = false }) {
//                            Text("confirm")
//                        }
//                    }
//                )
//            }
        }
    )
}

