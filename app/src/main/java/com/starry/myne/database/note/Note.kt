package com.starry.myne.database.note

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "text") val text: String,   // 选中的文字内容
    @ColumnInfo(name = "thoughts") val thoughts: String // 用户的感想或注释
)

