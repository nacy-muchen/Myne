/**
 * Copyright (c) [2022 - Present] Stɑrry Shivɑm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.starry.myne.ui.screens.reader.main.composables

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.starry.myne.ui.screens.note.NoteViewModel
import com.starry.myne.R
import com.starry.myne.epub.BookTextMapper
import com.starry.myne.epub.models.EpubChapter
import com.starry.myne.helpers.toToast
import com.starry.myne.ui.common.MyneSelectionContainer
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderScreenState
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderViewModel
import com.starry.myne.ui.theme.pacificoFont


@Composable
fun ChaptersContent(
    state: ReaderScreenState,
    lazyListState: LazyListState,
    onToggleReaderMenu: () -> Unit,
    viewModel: ReaderViewModel,
    navController: NavController,
    noteViewModel: NoteViewModel
) {
    val viewModel:NoteViewModel = hiltViewModel()
    var showDialog by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("") }
    var thoughts by remember { mutableStateOf("") }
    val snackBarHostState = remember { SnackbarHostState() }
    var showSnackbar by remember { mutableStateOf(false) }
    var showNoteSelectionDialog by remember { mutableStateOf(false) }
    val notes by noteViewModel.allNotes.observeAsState(emptyList())
    var selectedNoteId by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf("") }


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState
    ) {
        items(
            count = state.chapters.size,
            key = { index -> state.chapters[index].chapterId }
        ) { index ->
            val chapter = state.chapters[index]
            ChapterLazyItemItem(
                viewModel = viewModel,
                chapter = chapter,
                state = state,
                onClick = onToggleReaderMenu,
                onAddToNoteRequested = { text ->
                    selectedText = text
                    showDialog = true
                }
            )
        }
    }

    // AlertDialog 部分
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose A Note") },
            text = {
                Column {
                    Box(modifier = Modifier.heightIn(min = 56.dp, max = 150.dp)) {
                        Text("Selected Text: $selectedText", modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                        )
                    }
                    TextField(
                        value = thoughts,
                        onValueChange = { thoughts = it },
                        label = { Text("Your Thought")},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState()) // 添加滚动功能
                            .heightIn(min = 56.dp, max = 150.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(notes) { note ->
                            Button(
                                onClick = {
                                    selectedNoteId = if (selectedNoteId == note.id.toString()) {
                                        "" // 如果当前按钮已经选中，则取消
                                    } else {
                                        note.id.toString() // 否则选择当前按钮
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedNoteId == note.id.toString()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = if (selectedNoteId == note.id.toString()) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(note.title) // 显示笔记标题
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (selectedNoteId.isEmpty()) {
                        // 如果没有选择笔记，弹出提示
                        showSnackbar = true
                        snackbarMessage = "Please select a note before saving." // 错误提示

                    } else {
                        // 保存选中的文本和感想到笔记
                        noteViewModel.addEntryToExistingNote(selectedNoteId.toLong(), selectedText, thoughts)
                        showDialog = false // 关闭对话框
                        thoughts = "" // 清空感想字段
                        selectedNoteId = "" // 重置选中状态

                        showSnackbar = true
                        snackbarMessage = "Saved"
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false // 取消操作
                    selectedNoteId = "" // 重置选中状态
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 显示 Snackbar 提示
    if (showSnackbar) {
        LaunchedEffect(showSnackbar) {
            snackBarHostState.showSnackbar(snackbarMessage) // 显示提示
            showSnackbar = false // 重置Snackbar状态
        }
    }
    // 显示 Snackbar
    SnackbarHost(hostState = snackBarHostState)
}

@Composable
private fun ChapterLazyItemItem(
    viewModel: NoteViewModel,
    chapter: EpubChapter,
    state: ReaderScreenState,
    onClick: () -> Unit,
    onAddToNoteRequested: (String) -> Unit
) {
    val context = LocalContext.current
    val paragraphs = remember { chunkText(chapter.body) }

    val targetFontSize = remember(state.fontSize) {
        (state.fontSize / 5) * 0.9f
    }

    val fontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = tween(durationMillis = 300),
        label = "fontSize"
    )
    val titleFontSize by animateFloatAsState(
        targetValue = targetFontSize * 1.35f,
        animationSpec = tween(durationMillis = 300),
        label = "titleFontSize"
    )

    MyneSelectionContainer(
        onCopyRequested = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                context.getString(R.string.copied).toToast(context)
            }
        },
        onShareRequested = {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, it)
                type = "text/plain"
            }
            try {
                context.startActivity(Intent.createChooser(intent, null))
            } catch (e: ActivityNotFoundException) {
                context.getString(R.string.no_app_to_handle_content).toToast(context)
            }
        },
        onWebSearchRequested = {
            val intent = Intent().apply {
                action = Intent.ACTION_WEB_SEARCH
                putExtra(SearchManager.QUERY, it)
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                context.getString(R.string.no_app_to_handle_content).toToast(context)
            }
        },
        onTranslateRequested = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=$it")
            )
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                context.getString(R.string.no_app_to_handle_content).toToast(context)
            }
        },
        onDictionaryRequested = {
            val dictionaryIntent = Intent()
            val browserIntent = Intent()

            dictionaryIntent.type = "text/plain"
            dictionaryIntent.action = Intent.ACTION_PROCESS_TEXT
            dictionaryIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, it.trim())
            dictionaryIntent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)

            browserIntent.action = Intent.ACTION_VIEW
            val text = it.trim().replace(" ", "+")
            browserIntent.data = Uri.parse("https://www.onelook.com/?w=$text")

            var dictionaryFailure = false
            try {
                context.startActivity(Intent.createChooser(dictionaryIntent, null))
            } catch (e: ActivityNotFoundException) {
                dictionaryFailure = true
            }

            if (dictionaryFailure) {
                try {
                    context.startActivity(Intent.createChooser(browserIntent, null))
                } catch (e: ActivityNotFoundException) {
                    context.getString(R.string.no_app_to_handle_content).toToast(context)
                }
            }
        },
        onAddToNoteRequested = { text ->
            onAddToNoteRequested(text) // 传递选中的文本
        }


    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    // We're using awaitEachGesture instead of Modifier.clickable or
                    // Modifier.detectTapGestures so that we can propagate the click
                    // event to the parent, which in this case is SelectionContainer.
                    // Without this, the click event would be consumed before reaching
                    // SelectionContainer, preventing the copy/paste popup from being
                    // dismissed until the user copies the selected text.
                    awaitEachGesture {
                        val down: PointerInputChange = awaitFirstDown()
                        val up: PointerInputChange? = waitForUpOrCancellation()
                        // only trigger the click if the pointer hasn't moved up or down
                        // i.e only on tap gesture
                        if (up != null && down.id == up.id) {
                            onClick()
                        }
                    }
                }
        ) {
            Text(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp),
                text = chapter.title,
                fontSize = titleFontSize.sp,
                lineHeight = 1.3f.em,
                fontFamily = pacificoFont,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val accumulatedText = remember { StringBuilder() }
            paragraphs.forEachIndexed { index, para ->
                val imgEntry = BookTextMapper.ImgEntry.fromXMLString(para)

                if (imgEntry == null) {
                    // Accumulate text until an image is found
                    accumulatedText.append(para).append("\n\n")
                } else {
                    // If image is found, display the accumulated text before the image
                    if (accumulatedText.isNotEmpty()) {
                        Text(
                            text = accumulatedText.toString().trimEnd(),
                            fontSize = fontSize.sp,
                            lineHeight = 1.3.em,
                            fontFamily = state.fontFamily.fontFamily,
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                        )
                        accumulatedText.clear()
                    }
                    // Image Handling
                    val image = state.images.find { it.absPath == imgEntry.path }
                    image?.let {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(image.image)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp)
                        )
                    }
                }

                // Display any remaining accumulated text after the last paragraph
                if (index == paragraphs.lastIndex && accumulatedText.isNotEmpty()) {
                    Text(
                        text = accumulatedText.toString().trimEnd(),
                        fontSize = fontSize.sp,
                        lineHeight = 1.3.em,
                        fontFamily = state.fontFamily.fontFamily,
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                    )
                    accumulatedText.clear()
                }
            }
        }

        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        )
    }
}

// Helper function to chunk text into paragraphs
private fun chunkText(text: String): List<String> {
    return text.splitToSequence("\n\n")
        .filter { it.isNotBlank() }
        .toList()
}