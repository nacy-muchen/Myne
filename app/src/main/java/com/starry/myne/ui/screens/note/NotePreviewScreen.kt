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
    val viewModel: NoteViewModel = hiltViewModel()
    val notes by viewModel.allNotes.observeAsState(emptyList())

    // 查找对应笔记
    val note = notes.find { it.id == noteId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview Note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            note?.let {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues) // 使用 paddingValues
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(id = it.background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.h4,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )


                        it.entries.forEach { entry ->
                            Text(
                                text = entry.text,
                                style = MaterialTheme.typography.body1,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = entry.thoughts,
                                style = MaterialTheme.typography.body1,
                                color = Color(0xFF9C27B0),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

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

                            entry.summary?.let { it1 ->
                                Text(
                                    text = "Summary:\n$it1",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Blue,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }


                        }
                    }
                }
            } ?: run {
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
