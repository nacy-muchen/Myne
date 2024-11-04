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

@HiltViewModel
class NoteViewModel @Inject constructor(private val noteDao: NoteDAO) : ViewModel() {
    val allNotes: LiveData<List<Note>> = noteDao.getAllNotes().asLiveData()
    // Add note function to save a note to the database
    suspend fun addNote(title:String, text: String, thoughts: String):Long {
        var newId:Long=0
        val newNote = Note(title= title,text = text, thoughts = thoughts)
        newId = noteDao.insert(newNote) // 插入并获取生成的 ID
        return newId
    }

    suspend fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.delete(note)
        }
    }

    fun getAllNotes(note: Note) {
        viewModelScope.launch {
            noteDao.getAllNotes()
        }
    }

    suspend fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.update(note)
        }
    }
    fun hasNotes(): Boolean {
        return allNotes.value?.isNotEmpty() == true
    }

    // 增加一个方法以便向指定笔记添加内容
    fun addTextToExistingNote(note: Note, newText: String) {
        viewModelScope.launch {
            val updatedNote = note.copy(text = note.text + "\n" + newText)
            noteDao.update(updatedNote)
        }
    }

    fun addThoughtToExistingNote(noteId: Long, thoughts: String) {
        // 这里需要实现将选中的文本和感想保存到指定笔记的逻辑
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId)
            note?.let {
                // 获取当前笔记内容，并更新
                val updatedNote: Note = it.copy(
                    thoughts = thoughts
                ) // 根据需求更新其他字段
                // 保存到数据库或更新操作
                noteDao.update(updatedNote)
            }
        }
    }
}