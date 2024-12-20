package com.starry.myne.ui.screens.note

import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import coil.compose.rememberImagePainter
import com.starry.myne.R
import com.starry.myne.database.note.NoteEntry
import com.starry.myne.network.ImageGenerator
import com.starry.myne.network.SummaryGenerator
import com.starry.myne.ui.screens.reader.main.composables.ReaderFontChooserDialog
import com.starry.myne.ui.screens.reader.main.viewmodel.ReaderFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.ui.text.TextStyle
/**
 * Composable function to edit an existing note or create a new one.
 *
 * @param navController A controller for navigating between screens.
 * @param noteId The ID of the note to be edited. If null, a new note will be created.
 * @param initialText The initial text to display in the text field.
 */
@Composable
fun NoteEditScreen(
    navController: NavController, // Navigation controller to manage navigation actions
    noteId: Long? = null, // Optional note ID for editing an existing note
    initialText: String = "", // Initial text for the note title
    initialThoughts: String = "" // Initial thoughts or content for the note
) {

    val viewModel: NoteViewModel = hiltViewModel() // ViewModel to manage the note data
    val notes by viewModel.allNotes.observeAsState(emptyList()) // Observing all notes from the ViewModel
    var title by remember { mutableStateOf(TextFieldValue(initialText)) } // State for the title of the note
    var entries by remember { mutableStateOf(mutableListOf<NoteEntry>()) } // State to hold the list of note entries
    val snackBarHostState = remember { SnackbarHostState() } // State to manage Snackbar messages
    var showBackgroundPicker by remember { mutableStateOf(false) } // Controls the visibility of the background picker
    val showFontDialog = remember { mutableStateOf(false) }
    var selectedFont by remember { mutableStateOf<ReaderFont>(ReaderFont.System) }
    var fontSize by remember { mutableStateOf(16) }

    // List of available background images
    val backgroundOptions = listOf(
        R.drawable.p1,
        R.drawable.p2,
        R.drawable.p3,
        R.drawable.p4,
        R.drawable.p5,
        R.drawable.p6
    )

    val context = LocalContext.current
    var selectedBackground by remember { mutableStateOf(backgroundOptions.first()) } // State for the selected background image
    var showDialogForIndex by remember { mutableStateOf(-1) } // State to keep track of dialog visibility for selected index
    var isGeneratingImage by remember { mutableStateOf(false) } // State to track image generation status
    var taskId by remember { mutableStateOf<Long?>(null) } // State to store the task ID for image generation
    var generatedImageUrl by remember { mutableStateOf<String?>(null) } // State to store the generated image URL
    var generatedSummary by remember { mutableStateOf<String?>(null) } // State to store the generated summary
    val isGenerate = remember { mutableStateOf(false) }
    val maxSummaryLen = 300 // Maximum length of the generated summary
    val accessToken =
        "24.486e9625345c5215cc306a6157c365d0.2592000.1735440634.282335-116408284" // accessToken to use
    val isLoading = remember { mutableStateOf(false) } // State to manage loading status

    // If editing an existing note, load its content
    if (noteId != null) {
        // Load the existing note
        val existingNote = notes.find { it.id == noteId }
        existingNote?.let {
            title = TextFieldValue(it.title)// Set the note title in the TextField
            entries = Json.decodeFromString<List<NoteEntry>>(it.entriesJson)
                .toMutableList()// Load the note entries
            selectedBackground = it.background// Set the selected background for the note
            selectedFont = ReaderFont.getFontById(it.font) // 初始化字体
            fontSize = it.fontSize // 初始化字号
        }
    }

    // Function to check the status of the image generation process
    fun checkImageStatus(taskId: Long, accessToken: String, index: Int) {
        viewModel.viewModelScope.launch {
            var statusResult: String? = null
            val retryDelay = 15000L // // Delay for each retry in milliseconds

            // Query image generation status
            statusResult = withContext(Dispatchers.IO) {
                try {
                    delay(retryDelay) // Wait before querying
                    ImageGenerator.queryImageStatus(accessToken, taskId)// Query image status
                } catch (e: Exception) {
                    Log.e("ImageGeneration", "Error while checking image status", e)
                    null
                }
            }

            // Log the status result
            Log.d("ImageGeneration", "Status Result: $statusResult")

            if (!statusResult.isNullOrEmpty()) {
                try {
                    // Parse the image URL from the response
                    val secureImageUrl = statusResult.replace("http://", "https://")

                    generatedImageUrl = secureImageUrl// Store the secure image URL
                    Log.d("ImageGeneration", "Generated Image URL: $generatedImageUrl")

                    // Update the note entries with the generated image URL
                    val updatedEntries = entries.toMutableList()
                    updatedEntries[index] = updatedEntries[index].copy(imageUrl = generatedImageUrl)
                    entries = updatedEntries // Update the entries state

                    snackBarHostState.showSnackbar("Image generated successfully!")

                } catch (e: Exception) {
                    Log.e("ImageGeneration", "Error parsing status result", e)
                    snackBarHostState.showSnackbar("Failed to parse image generation status.")
                }
            } else {
                Log.d("ImageGeneration", "Failed to generate image.")
                snackBarHostState.showSnackbar("Image generation failed.")
            }

            // Reset image generation status
            isGeneratingImage = false
        }
    }


    // Function to generate a summary for the note
    fun generateSummary() {
        // Indicate that we are loading
        isLoading.value = true
        val content =
            entries.joinToString(" ") { it.text } // Combine all entry texts into one content string        // Call the API to generate a summary
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            SummaryGenerator.generateSummary("Please Summary these text as my reading notes.$content") { summary ->
                // Once the summary is generated, update the state
                isLoading.value = false
                generatedSummary = summary
                if (summary == null) {
                    Log.d("Summary Failed", "Failed to generate summary.")
                } else {
                    Log.d("Summary Successfully", "$generatedSummary")
                }
            }
        }
    }

    // Scaffold to layout the screen, including AppBar, Body content, and SnackBar
    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },// Display a Snackbar to show messages
        topBar = {
            TopAppBar(
                title = { Text("Edit Note") },// Title for the top bar
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        // Navigation icon to go back to the previous screen
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                // buttons to select background and preview
                actions = {
                    // Button to toggle the background picker visibility
                    IconButton(onClick = { showBackgroundPicker = !showBackgroundPicker }) {
                        Icon(
                            painterResource(id = R.drawable.ic_background_picker),
                            contentDescription = "Toggle Background Picker"
                        )
                    }

                    // Button to navigate to the note preview screen
                    IconButton(onClick = {
                        navController.navigate("note_preview/${noteId ?: 0}")
                    }) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview")
                    }

                    IconButton(onClick = { showFontDialog.value = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_reader_font),
                            contentDescription = "Change Font"
                        )
                    }
                }
            )
        },

        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)// Background color for the content area
            ) {
                ReaderFontChooserDialog(
                    showFontDialog = showFontDialog,
                    fontFamily = selectedFont,
                    onFontFamilyChanged = { newFont ->
                        selectedFont = newFont
                    }
                )

                // Display selected background image
                Image(
                    painter = painterResource(id = selectedBackground),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()// Image takes up the whole screen size
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)// Padding for content inside the column
                        .padding(16.dp),
                ) {
                    // Show background picker when the flag is true
                    if (showBackgroundPicker) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between items
                        ) {
                            items(backgroundOptions) { background ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp) // Set the size of each background option
                                        .clickable {
                                            selectedBackground = background
                                        }// Set selected background when clicked
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Display each background option as an image
                                    Image(
                                        painter = painterResource(id = background),
                                        contentDescription = "Background Option",// Descriptive text for accessibility
                                        modifier = Modifier.fillMaxSize()// Image fills the whole box
                                    )
                                }
                            }
                        }
                    }

                    // Text field for the note's title
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        textStyle = TextStyle(
                            fontSize = fontSize.sp,
                            fontFamily = selectedFont.fontFamily
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)// Allow this column to take up remaining space
                            .verticalScroll(rememberScrollState())// Make the column scrollable
                    ) {
                        // Iterate through the entries and display each entry
                        entries.forEachIndexed { index, entry ->
                            var showActions by remember { mutableStateOf(false) } // State to show or hide actions for each entry
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                showActions =
                                                    true // Show delete actions on long press
                                            },
                                            onTap = {
                                                // Hide actions on normal tap
                                                showActions = false
                                            }
                                        )
                                    }
                            ) {
                                // Display the text of each entry
                                Text(
                                    text = entry.text,
                                    style = TextStyle(
                                        fontSize = fontSize.sp,
                                        fontFamily = selectedFont.fontFamily
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 8.dp)
                                )

                                // Show actions (like delete) if long pressed
                                if (showActions) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp) // 按钮之间留空
                                    ) {
                                        Button(
                                            onClick = {
                                                showDialogForIndex =
                                                    index // Show confirmation dialog for deletion
                                            },
                                            modifier = Modifier.fillMaxWidth()// Button takes up full width
                                        ) {
                                            Text("Delete")
                                        }

                                        // Button to generate image from the entry text
                                        Button(
                                            onClick = {
                                                isGeneratingImage = true // Set generating flag
                                                viewModel.viewModelScope.launch {
                                                    val resolution =
                                                        "512*512"// Define resolution for image
                                                    val taskIdResponse =
                                                        withContext(Dispatchers.IO) {
                                                            try {
                                                                // Generate image based on entry text
                                                                ImageGenerator.generateImageFromText(
                                                                    accessToken,
                                                                    entry.text,
                                                                    resolution
                                                                )
                                                            } catch (e: Exception) {
                                                                withContext(Dispatchers.Main) {
                                                                    Toast.makeText(context, "Too many words!", LENGTH_SHORT)
                                                                }
                                                                isGeneratingImage = false
                                                                null
                                                            }
                                                        }

                                                    if (taskIdResponse != null) {
                                                        taskId = taskIdResponse// Store the task ID
                                                        checkImageStatus(
                                                            taskIdResponse,
                                                            accessToken,
                                                            index
                                                        )// Check image generation status

                                                        Log.d(
                                                            "GenerateImage",
                                                            "Generated Image URL: $generatedImageUrl"
                                                        )
//
                                                        Log.d("ImageURL", "check ImageURL: $entry")
                                                        Log.d(
                                                            "ImageURL",
                                                            "check ImageURL: $generatedImageUrl"
                                                        )


                                                    } else {
                                                        // Show error snackbar if image generation fails
                                                        snackBarHostState.showSnackbar("Too many words!")
                                                        isGeneratingImage = false

                                                    }
                                                }
                                            },
                                            enabled = !isGeneratingImage,// Disable the button if image is being generated
                                            modifier = Modifier.fillMaxWidth()  // Button takes up full width
                                        ) {
                                            // Show progress indicator when image is being generated
                                            if (isGeneratingImage) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(
                                                        16.dp
                                                    )
                                                )
                                            } else {
                                                Text("Generate Image")// Button text when not generating
                                            }
                                        }

                                    }
                                }
                                entry.imageUrl?.let { generatedImageUrl ->
                                    // If the entry has an image URL, display the generated image
                                    Image(
                                        painter = rememberImagePainter(generatedImageUrl), // Load the image using the generated URL
                                        contentDescription = "Generated Image",// Descriptive text for accessibility
                                        modifier = Modifier
                                            .fillMaxWidth()// Image takes the full width of the screen
                                            .height(200.dp)// Set the height of the image
                                            .padding(8.dp)// Add padding around the image
                                    )
                                }

                                TextField(
                                    value = entry.thoughts,// Bind the thoughts field of the entry to the TextField
                                    onValueChange = { newThought -> // When the user modifies the text
                                        val updatedEntries =
                                            entries.toMutableList()// Create a mutable copy of the entries list
                                        updatedEntries[index] =
                                            entry.copy(thoughts = newThought)// Update the current entry with the new thoughts
                                        entries =
                                            updatedEntries// Set the updated list as the new entries
                                    },
                                    label = { Text("Thought ${index + 1}") },
                                    textStyle = TextStyle(
                                        fontSize = fontSize.sp, // 动态设置字号
                                        fontFamily = selectedFont.fontFamily // 动态设置字体
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Divider(color = Color.Gray, thickness = 1.dp)

                            // If the user has long-pressed on an entry, show the confirmation dialog to delete it
                            if (showDialogForIndex == index) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showDialogForIndex = -1
                                    }, // Close the dialog when the user taps outside
                                    title = { Text("Delete Entry") },
                                    text = { Text("Are you sure you want to delete this entry?") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            // If confirmed, delete the entry
                                            val updatedEntries = entries.toMutableList()
                                            updatedEntries.removeAt(index)
                                            entries = updatedEntries// Update the entries list
                                            showDialogForIndex = -1 // Close the confirmation dialog
                                        }) {
                                            Text("Yes")
                                        }
                                    },
                                    dismissButton = {
                                        // If canceled, just close the dialog
                                        TextButton(onClick = { showDialogForIndex = -1 }) {
                                            Text("No")
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        // Show a loading spinner if the content is still being generated
                        if (isLoading.value) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }

                        Button(
                            onClick = {
                                isGenerate.value = true
                                generateSummary()  // Trigger the summary generation function
                            },
                            modifier = Modifier.fillMaxWidth() // Make the button fill the width of the screen
                        ) {
                            Text("Generate Summary")// Button text
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        val summary =
                            viewModel.getSummary(noteId!!)?.collectAsState(initial = null)?.value
                        if (summary != null && !isGenerate.value) {
                            Text(
                                text = "Summary:\n$summary",
                                style = TextStyle(
                                    fontSize = fontSize.sp,
                                    fontFamily = selectedFont.fontFamily
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            generatedSummary?.let {
                                Text(
                                    text = "Summary:\n$it",
                                    style = TextStyle(
                                        fontSize = fontSize.sp,
                                        fontFamily = selectedFont.fontFamily
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                        }


                        Button(
                            onClick = {
                                // Save the note when the user clicks "Save"
                                viewModel.viewModelScope.launch {
                                    val note =
                                        notes.find { it.id == noteId }// Find the existing note by its ID
                                    if (note != null) {
                                        // Create a new updated note with the modified fields
                                        val updatedNote = note.copy(
                                            title = title.text, // Update the title
                                            entriesJson = Json.encodeToString(entries),  // Serialize the updated entries to JSON
                                            background = selectedBackground,
                                            font = selectedFont.id, // 保存字体 ID
                                            fontSize = fontSize // 保存字号
                                        )
                                        val updatedSummary = generatedSummary

                                        if (updatedSummary != null) {
                                            viewModel.updateSummary(noteId, updatedSummary)
                                        }

                                        // Update the note in the ViewModel
                                        viewModel.updateNote(updatedNote)
                                        snackBarHostState.showSnackbar("Saved")// Show a Snackbar indicating the note is saved
                                    }
                                    navController.popBackStack()// Navigate back after saving
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save")
                        }


                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = { if (fontSize > 12) fontSize -= 2 }) {
                            Text("-")
                        }
                        Text(
                            text = "$fontSize sp",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 16.sp
                        )
                        FilledTonalButton(onClick = { if (fontSize < 32) fontSize += 2 }) {
                            Text("+")
                        }
                    }
                }

            }
        }

    )
}