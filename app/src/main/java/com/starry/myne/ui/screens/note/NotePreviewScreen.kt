package com.starry.myne.ui.screens.note

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberImagePainter


@Composable
fun NotePreviewScreen(
    navController: NavController,
    noteId: Long? = null
) {
    // Get the ViewModel using Hilt
    val viewModel: NoteViewModel = hiltViewModel()
    // Observe all notes from the ViewModel
    val notes by viewModel.allNotes.observeAsState(emptyList())

    // Find the note with the corresponding noteId
    val note = notes.find { it.id == noteId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview Note") },
                navigationIcon = {
                    // Back button to pop the navigation stack
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            // If the note exists, display it
            note?.let {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues) // Apply padding from Scaffold
                        .background(Color.White)
                ) {
                    // Set the background image for the note
                    Image(
                        painter = painterResource(id = it.background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Create a vertical scrollable column to hold note content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Display the note's title
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.h4,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Display each entry in the note
                        it.entries.forEach { entry ->
                            // Display entry text
                            Text(
                                text = entry.text,
                                style = MaterialTheme.typography.body1,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Display entry thoughts with a different color
                            Text(
                                text = entry.thoughts,
                                style = MaterialTheme.typography.body1,
                                color = Color(0xFF9C27B0),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // If the entry contains an image URL, display the image
                            entry.imageUrl?.let { imageUrl ->
                                Image(
                                    painter = rememberImagePainter(imageUrl),
                                    contentDescription = "Generated Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(bottom = 16.dp)
                                )
                            }

                        }
                        it.summary?.let { summary ->
                            Text(
                                text = "Summary:\n$summary",
                                style = MaterialTheme.typography.body1,
                                color = Color.Blue,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }
            } ?: run {
                // If no note is found, display a message indicating so
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("No Note Found", style = MaterialTheme.typography.body1)
                }
            }
        }
    )
}
