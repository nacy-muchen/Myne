package com.starry.myne.database.note

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import com.starry.myne.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Represents a Note entity in the database.
 *
 * @property id The unique identifier for the note, auto-generated by the database.
 * @property title The title of the note.
 * @property text The selected text content that the user has saved.
 * @property thoughts The user's personal thoughts or annotations associated with the note.
 */
@Entity(tableName = "notes")
@Serializable
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "entries") val entriesJson: String,
    @ColumnInfo(name = "background") val background: Int = R.drawable.p1){
    // 将 JSON 转换为 NoteEntry 列表
    val entries: List<NoteEntry>
        get() = try {
            Json.decodeFromString(entriesJson)
        } catch (e: Exception) {
            emptyList() // 解析失败返回空列表
        }
    // 返回一个新对象，并更新 entriesJson
    fun withUpdatedEntries(newEntries: List<NoteEntry>): Note {
        return this.copy(entriesJson = Json.encodeToString(newEntries))
    }
}



