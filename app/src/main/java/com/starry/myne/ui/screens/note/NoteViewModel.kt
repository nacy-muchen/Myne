package com.starry.myne.ui.screens.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.starry.myne.database.note.Note
import com.starry.myne.database.note.NoteDAO
import com.starry.myne.database.note.NoteEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel class for managing note data and providing functionality to interact with the database.
 *
 * This class uses Room database to persist notes and provides methods to add, update, delete, and retrieve notes.
 * It also includes functionality for adding selected text and thoughts to existing notes.
 *
 * @property noteDao The Data Access Object (DAO) used to interact with the notes database.
 */
@HiltViewModel
class NoteViewModel @Inject constructor(private val noteDao: NoteDAO) : ViewModel() {
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes().asLiveData()

    /**
     * Adds a new note to the database.
     *
     * @param title The title of the new note.
     * @param text The main content of the note (e.g., selected text).
     * @param thoughts The user's thoughts or comments related to the note.
     * @return The ID of the newly created note.
     */
    suspend fun addNote(title: String): Long {
        val newNote = Note(
            title = title,
            entriesJson = Gson().toJson(emptyList<NoteEntry>()) // 初始化空 entries 列表
        )
        return noteDao.insert(newNote)
    }

    /**
     * Deletes the specified note from the database.
     *
     * @param note The note to be deleted.
     */
    suspend fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.delete(note)
        }
    }

    /**
     * Retrieves all notes from the database.
     * (This function doesn't return any value as `allNotes` is already observed.)
     *
     * @param note The note to be retrieved (not used in current implementation).
     */
    fun getAllNotes(note: Note) {
        viewModelScope.launch {
            noteDao.getAllNotes()
        }
    }

    /**
     * Updates the specified note with new data.
     *
     * @param note The note to be updated with new values.
     */
    suspend fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.update(note)
        }
    }

    /**
     * Checks if there are any existing notes.
     *
     * @return True if there are notes, false otherwise.
     */
    fun hasNotes(): Boolean {
        return allNotes.value?.isNotEmpty() == true
    }

    /**
     * Adds new text to an existing note.
     *
     * @param note The note to which the new text will be appended.
     * @param newText The additional text to be added to the note's content.
     */
    fun addEntryToExistingNote(noteId: Long, newText: String, newThought: String) {
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId)
            note?.let {
                // 获取当前的 entries 列表并添加新条目
                val updatedEntries = it.entries.toMutableList()
                updatedEntries.add(NoteEntry(newText, newThought))

                // 更新 Note 对象并保存到数据库
                val updatedNote = it.withUpdatedEntries(updatedEntries)
                noteDao.update(updatedNote)
            }
        }
    }
}