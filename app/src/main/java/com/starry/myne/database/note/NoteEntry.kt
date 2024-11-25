package com.starry.myne.database.note

import kotlinx.serialization.Serializable

@Serializable
data class NoteEntry(
    val text: String,   // 选中的文字内容
    val thoughts: String // 用户的感想或注释
)
