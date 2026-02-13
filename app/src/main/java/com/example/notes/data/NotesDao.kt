package com.example.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.notes.domain.ContentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {

    @Transaction
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteWithContentDbModel>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNote(noteId: Int): NoteWithContentDbModel

    @Transaction
    @Query(
        """
            SELECT DISTINCT notes. * FROM notes JOIN content 
            ON notes.id = content.noteId 
            WHERE title LIKE '%' || :query || '%' 
            OR content LIKE '%' || :query || '%' 
            ORDER BY updatedAt DESC
            """
    )
    fun searchNotes(query: String): Flow<List<NoteWithContentDbModel>>

    @Transaction
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Int)

    @Query("UPDATE notes SET isPinned = NOT isPinned WHERE id = :noteId")
    suspend fun switchPinnedStatus(noteId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNote(noteDbModel: NoteDbModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNoteContent(content: List<ContentItemDbModel>)

    @Query("DELETE FROM content WHERE noteId = :noteId")
    suspend fun deleteNoteContent(noteId: Int)

    @Transaction
    suspend fun addNoteWithContent(
        noteDbModel: NoteDbModel,
        content: List<ContentItem>
    ) {
        val noteId = addNote(noteDbModel)
        val contentItems = content.toContentItemDbModels(noteId.toInt())
        addNoteContent(contentItems)
    }

    @Transaction
    suspend fun updateNote(
        noteDbModel: NoteDbModel,
        content: List<ContentItemDbModel>
    ) {
        addNote(noteDbModel)
        deleteNoteContent(noteDbModel.id)
        addNoteContent(content)
    }
}