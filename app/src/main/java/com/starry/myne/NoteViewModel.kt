package com.starry.myne

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.starry.myne.database.note.Note
import com.starry.myne.database.note.NoteDAO
import kotlinx.coroutines.launch
import javax.inject.Inject

class NoteViewModel @Inject constructor(private val noteDao: NoteDAO) : ViewModel() {
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes().asLiveData()
    // Add note function to save a note to the database
    fun addNote(text: String, thoughts: String) {
        viewModelScope.launch {
            noteDao.insert(Note(text = text, thoughts = thoughts))
        }
    }
}