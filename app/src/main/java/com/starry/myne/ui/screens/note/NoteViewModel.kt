package com.starry.myne.ui.screens.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.starry.myne.database.note.Note
import com.starry.myne.database.note.NoteDAO
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
    suspend fun addNote(title:String, text: String, thoughts: String):Long {
        var newId:Long=0
        val newNote = Note(title= title,text = text, thoughts = thoughts)
        newId = noteDao.insert(newNote) // 插入并获取生成的 ID
        return newId
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
    fun addTextToExistingNote(note: Note, newText: String) {
        viewModelScope.launch {
            val updatedNote = note.copy(text = note.text + "\n" + newText)
            noteDao.update(updatedNote)
        }
    }

    /**
     * Adds thoughts and text to an existing note by its ID.
     *
     * @param noteId The ID of the note to which the content will be added.
     * @param text The new text to update the note's main content.
     * @param thoughts The user's thoughts to be appended to the existing thoughts.
     */
    fun addThoughtToExistingNote(noteId: Long, text: String, thoughts: String) {
        // 这里需要实现将选中的文本和感想保存到指定笔记的逻辑
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId)
            note?.let {
                // 获取当前笔记内容，并更新
                val updatedNote: Note = it.copy(
                    text = text,
                    thoughts = note.thoughts + "\n" + thoughts
                ) // 根据需求更新其他字段
                // 保存到数据库或更新操作
                noteDao.update(updatedNote)
            }
        }
    }
}