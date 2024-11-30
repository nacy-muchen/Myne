package com.starry.myne.ui.screens.note

import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewModelScope
import coil.compose.rememberImagePainter
import com.starry.myne.R
import com.starry.myne.database.note.NoteEntry
import com.starry.myne.network.ImageGenerator
import com.starry.myne.network.ImageGenerator.generateImageFromText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import java.io.IOException

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
    var title by remember { mutableStateOf(TextFieldValue(initialText)) }
    var entries by remember { mutableStateOf(mutableListOf<NoteEntry>()) }
    val snackBarHostState = remember { SnackbarHostState() }
    var showBackgroundPicker by remember { mutableStateOf(false) } // 控制背景选择栏的显示
   // var accessToken by remember { mutableStateOf<String?>(null) } // Define accessToken here


    val backgroundOptions = listOf(
        R.drawable.p1,
        R.drawable.p2,
        R.drawable.p3,
        R.drawable.p4,
        R.drawable.p5,
        R.drawable.p6
    )
    var selectedBackground by remember { mutableStateOf(backgroundOptions.first()) }
    var showDialogForIndex by remember { mutableStateOf(-1) }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf<String?>(null) } // 用于存储选中的文本
    var taskId by remember { mutableStateOf<Long?>(null) } // 存储生成图片的任务 ID
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }

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


    fun checkImageStatus(taskId: Long, accessToken: String,index: Int) {
        viewModel.viewModelScope.launch {
            var retries = 0
            var statusResult: String? = null
            val maxRetries = 3 // 最大重试次数
            val retryDelay = 4000L // 每次重试的延迟（单位：毫秒）

            while (retries < maxRetries) {
                // 查询图像生成状态
                statusResult = withContext(Dispatchers.IO) {
                    try {
                        // 假设 ImageGenerator.queryImageStatus 返回的是一个 JSON 字符串
                        ImageGenerator.queryImageStatus(accessToken, taskId)
                    } catch (e: Exception) {
                        Log.e("ImageGeneration", "Error while checking image status", e)
                        null
                    }
                }

                // 打印状态结果
                Log.d("ImageGeneration", "Status Result: $statusResult")

                if (!statusResult.isNullOrEmpty()) {
                    try {
                        // 解析 JSON 字符串为 Map<String, Any>
                        val secureImageUrl = statusResult.replace("http://", "https://")

                        generatedImageUrl = secureImageUrl
                        // 打印解析后的状态
                        Log.d("ImageGeneration", "Generated Image URL: $generatedImageUrl")

                        val updatedEntries = entries.toMutableList()
                        updatedEntries[index] = updatedEntries[index].copy(imageUrl = generatedImageUrl)
                        entries = updatedEntries // 更新 entries 的引用

                        snackBarHostState.showSnackbar("Image generated successfully!")

                    } catch (e: Exception) {
                        Log.e("ImageGeneration", "Error parsing status result", e)
                        snackBarHostState.showSnackbar("Failed to parse image generation status.")
                    }
                } else {
                    Log.d("ImageGeneration", "Failed to generate image.")
                    snackBarHostState.showSnackbar("Image generation failed.")
                }

                // 增加延迟后再重试
                delay(retryDelay)
                retries++
            }

            // 如果超过最大重试次数仍未成功
            isGeneratingImage = false
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

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        entries.forEachIndexed { index, entry ->
                            var showActions by remember { mutableStateOf(false) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                showActions = true // 长按显示删除按钮
                                            },
                                            onTap = {
                                                // 点击切换删除按钮显示状态
                                                showActions = false
                                            }
                                        )
                                    }
                            ) {
                                Text(
                                    text = entry.text,
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 8.dp)
                                )

                                if (showActions) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp) // 按钮之间留空
                                    ) {
                                        Button(
                                            onClick = {
                                                showDialogForIndex = index // 点击删除按钮显示确认对话框
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Delete")
                                        }

                                        Button(
                                            onClick = {
                                                isGeneratingImage = true
                                                viewModel.viewModelScope.launch {
                                                    val accessToken = "24.486e9625345c5215cc306a6157c365d0.2592000.1735440634.282335-116408284" // 使用实际的 accessToken
                                                    val resolution = "512*512"
                                                    val taskIdResponse = withContext(Dispatchers.IO) {
                                                            // 网络请求应该放在 IO 线程池中执行
                                                        try {
                                                            // 调用生成图像的 API
                                                            ImageGenerator.generateImageFromText(accessToken, entry.text, resolution)
                                                        } catch (e: Exception) {
                                                            Log.e("GenerateImage", "Error generating image", e)
                                                            null
                                                        }
                                                    }

                                                    if (taskIdResponse != null) {
                                                        taskId = taskIdResponse
                                                        checkImageStatus(taskIdResponse, accessToken,index)

                                                        Log.d("GenerateImage", "Generated Image URL: $generatedImageUrl")

//                                                        val updatedEntries = entries.toMutableList()
//                                                        updatedEntries[index] = updatedEntries[index].copy(imageUrl = generatedImageUrl)
//                                                        entries = updatedEntries
                                                        Log.d("ImageURL", "check ImageURL: $entry")
                                                        Log.d("ImageURL", "check ImageURL: $generatedImageUrl")


                                                    } else {
                                                        snackBarHostState.showSnackbar("Failed to generate image.")

                                                    }
                                                    isGeneratingImage = false
                                                }
                                        },
                                            enabled = !isGeneratingImage,
                                            modifier = Modifier.fillMaxWidth() // 按钮占据整行
                                        ) {
                                            if (isGeneratingImage) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                            } else {
                                                Text("Generate Image")
                                            }
                                        }


                                    }
                                }
                                entry.imageUrl?.let { generatedImageUrl ->
                                    Image(
                                        painter = rememberImagePainter(generatedImageUrl),
                                        contentDescription = "Generated Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .padding(8.dp)
                                    )
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
        }

    )
    }