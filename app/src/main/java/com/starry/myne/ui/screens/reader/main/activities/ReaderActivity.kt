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

package com.starry.myne.ui.screens.reader.main.activities

import android.content.ContentResolver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.starry.myne.ui.screens.note.NoteViewModel
import com.starry.myne.R
import com.starry.myne.helpers.Constants
import com.starry.myne.helpers.toToast
import com.starry.myne.ui.screens.note.NoteEditScreen
import com.starry.myne.ui.screens.reader.main.composables.ChaptersContent
import com.starry.myne.ui.screens.reader.main.composables.ReaderScreen
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderViewModel
import com.starry.myne.ui.screens.settings.viewmodels.SettingsViewModel
import com.starry.myne.ui.theme.MyneTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.FileInputStream


@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {

    private lateinit var settingsViewModel: SettingsViewModel
    private val viewModel: ReaderViewModel by viewModels()
    private val noteViewModel: NoteViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupWindowInsets() // Setup fullscreen mode.

        // Initialize settings view model.
        settingsViewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        // Set UI contents.
        setContent {
            MyneTheme(settingsViewModel = settingsViewModel) {
                val navController = rememberNavController() // 添加 NavController
                val lazyListState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()
                // Handle intent and load epub book.
                val intentData = remember {
                    handleIntent(intent = intent,
                        viewModel = viewModel,
                        contentResolver = contentResolver,
                        scrollToPosition = { index, offset ->
                            coroutineScope.launch {
                                lazyListState.scrollToItem(index, offset)
                            }
                        },
                        onError = {
                            getString(R.string.error).toToast(this)
                            finish()
                        })
                }


                // 设置 NavHost
                NavHost(navController = navController, startDestination = "reader_screen") {
                    composable("reader_screen") {
                        ReaderScreen(
                            viewModel = viewModel,
                            onScrollToChapter = { lazyListState.scrollToItem(it) },
                            chaptersContent = {
                                LaunchedEffect(lazyListState) {
                                    snapshotFlow {
                                        lazyListState.firstVisibleItemScrollOffset
                                    }.collect { visibleChapterOffset ->
                                        val visibleChapterIdx = lazyListState.firstVisibleItemIndex
                                        viewModel.setVisibleChapterIndex(visibleChapterIdx)
                                        viewModel.setChapterScrollPercent(
                                            calculateChapterPercentage(lazyListState)
                                        )
                                        if (!intentData.isExternalFile) {
                                            viewModel.updateReaderProgress(
                                                libraryItemId = intentData.libraryItemId!!,
                                                chapterIndex = visibleChapterIdx,
                                                chapterOffset = visibleChapterOffset
                                            )
                                        }
                                    }
                                }

                                // 渲染章节内容
                                val state = viewModel.state.collectAsState().value
                                ChaptersContent(
                                    state = state,
                                    lazyListState = lazyListState,
                                    onToggleReaderMenu = { viewModel.toggleReaderMenu() },
                                    viewModel = viewModel,
                                    navController = navController,
                                    noteViewModel = noteViewModel
                                )

                                LaunchedEffect(state.showReaderMenu) {
                                    toggleSystemBars(state.showReaderMenu)
                                }
                            }
                        )
                    }

                    // 新的 Note 编辑页面
                    composable(
                        "note_edit/{selectedText}/{thoughts}",
                        arguments = listOf(
                            navArgument("selectedText") { type = NavType.StringType },
                            navArgument("thoughts") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val selectedText = backStackEntry.arguments?.getString("selectedText") ?: ""
                        val thoughts = backStackEntry.arguments?.getString("thoughts") ?: ""

                        NoteEditScreen(
                            navController = navController,
                             initialThoughts = thoughts
                        )
                    }
                }
            }
        }
    }

    private fun setupWindowInsets() {
        // Fullscreen mode that ignores any cutout, notch etc.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.displayCutout())

        // Set layout in display cutout mode for Android P and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Keep screen on.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun toggleSystemBars(show: Boolean) {
        when (show) {
            true -> showSystemBars()
            false -> hideSystemBars()
        }
    }

    private fun showSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
        controller.show(WindowInsetsCompat.Type.displayCutout())
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.displayCutout())
    }
}

object ReaderConstants {
    const val EXTRA_LIBRARY_ITEM_ID = "reader_book_id"
    const val EXTRA_CHAPTER_IDX = "reader_chapter_index"
    const val DEFAULT_NONE = -100000

}

/**
 * Data class to hold intent information for ReaderActivity.
 *
 * @param libraryItemId Library item id.
 * @param chapterIndex Chapter index.
 * @param isExternalFile Is book opened from external file.
 */
data class IntentData(
    val libraryItemId: Int?, val chapterIndex: Int?, val isExternalFile: Boolean
)

/**
 * Handle intent and load epub book from given id or external file.
 *
 * @param intent Intent to handle.
 * @param viewModel ReaderViewModel to load book.
 * @param contentResolver ContentResolver to open input stream.
 * @param scrollToPosition Function to scroll to specific position.
 * @param onError Function to handle error.
 *
 * @return IntentData object containing book id, chapter index and isExternalBook.
 */
fun handleIntent(
    intent: Intent,
    viewModel: ReaderViewModel,
    contentResolver: ContentResolver,
    scrollToPosition: (index: Int, offset: Int) -> Unit,
    onError: () -> Unit
): IntentData {
    val libraryItemId = intent.extras?.getInt(
        ReaderConstants.EXTRA_LIBRARY_ITEM_ID, ReaderConstants.DEFAULT_NONE
    )
    val chapterIndex = intent.extras?.getInt(
        ReaderConstants.EXTRA_CHAPTER_IDX, ReaderConstants.DEFAULT_NONE
    )
    val isExternalFile = intent.type == Constants.EPUB_MIME_TYPE

    // Internal book
    if (libraryItemId != null && libraryItemId != ReaderConstants.DEFAULT_NONE) {
        // Load epub book from library.
        viewModel.loadEpubBook(libraryItemId = libraryItemId, onLoaded = {
            // if there is saved progress for this book, then scroll to
            // last page at exact position were used had left.
            if (it.hasProgressSaved && chapterIndex == ReaderConstants.DEFAULT_NONE) {
                scrollToPosition(it.lastChapterIndex, it.lastChapterOffset)
            }
        })
        // if user clicked on specific chapter, then scroll to
        // that chapter directly.
        if (chapterIndex != null && chapterIndex != ReaderConstants.DEFAULT_NONE) {
            scrollToPosition(chapterIndex, 0)
        }


    } else if (isExternalFile) {
        // External book from file.
        intent.data?.let {
            contentResolver.openInputStream(it)?.let { ips ->
                viewModel.loadEpubBookExternal(ips as FileInputStream)
            }
        }
    } else {
        onError() // Invalid intent.
    }

    return IntentData(libraryItemId, chapterIndex, isExternalFile)
}

/**
 * Calculate the scroll percentage for the first visible item in a LazyColumn.
 *
 * @param lazyListState The LazyListState of the LazyColumn
 * @return The scroll percentage for the first visible item, or -1 if no item is visible
 */
fun calculateChapterPercentage(lazyListState: LazyListState): Float {
    val firstVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return -1f
    val listHeight =
        lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset

    // Calculate the scroll percentage for the first visible item
    val itemTop = firstVisibleItem.offset.toFloat()
    val itemBottom = itemTop + firstVisibleItem.size.toFloat()

    return if (itemTop >= listHeight || itemBottom <= 0f) {
        1f // Item is completely scrolled out of view
    } else {
        // Calculate the visible portion of the item
        val visiblePortion = if (itemTop < 0f) {
            itemBottom
        } else {
            listHeight - itemTop
        }
        // Calculate the scroll percentage based on the visible portion
        ((1f - visiblePortion / firstVisibleItem.size.toFloat())).coerceIn(0f, 1f)
    }
}


