package com.starry.myne.database.note

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NoteEntryConverter {

    private val json = Json { encodeDefaults = true }

    @TypeConverter
    fun fromEntries(entries: List<NoteEntry>): String {
        return json.encodeToString(entries) // serialization to JSON string
    }

    @TypeConverter
    fun toEntries(jsonString: String): List<NoteEntry> {
        return json.decodeFromString(jsonString) // deserialization to List<NoteEntry>
    }
}