package com.example.notes.stubs

import com.example.notes.domain.ContentItem
import com.example.notes.domain.Note

fun createNewNote(
    id: Int = 1,
    title: String = "Title",
    content: List<ContentItem> = emptyList(),
    updatedAt: Long = System.currentTimeMillis(),
    isPinned: Boolean = false
) = Note(
    id = id,
    title = title,
    content = content,
    updatedAt = updatedAt,
    isPinned = isPinned
)
