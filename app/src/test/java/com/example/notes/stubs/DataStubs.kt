package com.example.notes.stubs

import com.example.notes.data.ContentItemDbModel
import com.example.notes.data.ContentType
import com.example.notes.data.NoteDbModel
import com.example.notes.data.NoteWithContentDbModel

fun createNoteWithContentDbModel(
    id: Int = 1,
    title: String = "Title",
    updatedAt: Long = 1000L,
    isPinned: Boolean = false,
    content: List<ContentItemDbModel> = emptyList()
): NoteWithContentDbModel {
    return NoteWithContentDbModel(
        noteDbModel = NoteDbModel(
            id = id,
            title = title,
            updatedAt = updatedAt,
            isPinned = isPinned
        ),
        content = content
    )
}

fun createTextContentDbModel(
    noteId: Int,
    content: String,
    order: Int
): ContentItemDbModel {
    return ContentItemDbModel(
        noteId = noteId,
        contentType = ContentType.TEXT,
        content = content,
        order = order
    )
}

fun createImageContentDbModel(
    noteId: Int,
    url: String,
    order: Int
): ContentItemDbModel {
    return ContentItemDbModel(
        noteId = noteId,
        contentType = ContentType.IMAGE,
        content = url,
        order = order
    )
}
