package com.example.notes.domain

import com.example.notes.stubs.createNewNote
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class EditNoteUseCaseTest {

    @MockK
    private lateinit var repository: NotesRepository

    private lateinit var useCase: EditNoteUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = EditNoteUseCase(repository)
    }

    @Test
    fun `GIVEN a note WHEN invoke is called THEN delegates updated note to repository`() = runTest {
        val originalNote = createNewNote(id = 1, title = "Original Title", updatedAt = 1000L)
        val capturedNote = slot<Note>()
        coJustRun { repository.editNote(capture(capturedNote)) }

        useCase(originalNote)

        assertThat(capturedNote.captured.id).isEqualTo(originalNote.id)
        assertThat(capturedNote.captured.title).isEqualTo(originalNote.title)
        assertThat(capturedNote.captured.content).isEqualTo(originalNote.content)
        assertThat(capturedNote.captured.isPinned).isEqualTo(originalNote.isPinned)
        coVerify(exactly = 1) { repository.editNote(any()) }
    }

    @Test
    fun `GIVEN a note with old timestamp WHEN invoke is called THEN repository receives note with updated timestamp`() = runTest {
        val staleTimestamp = 1_000L
        val originalNote = createNewNote(id = 1, title = "Stale Note", updatedAt = staleTimestamp)
        val capturedNote = slot<Note>()
        val beforeCall = System.currentTimeMillis()
        coJustRun { repository.editNote(capture(capturedNote)) }

        useCase(originalNote)

        val afterCall = System.currentTimeMillis()
        assertThat(capturedNote.captured.updatedAt).isGreaterThan(staleTimestamp)
        assertThat(capturedNote.captured.updatedAt).isAtLeast(beforeCall)
        assertThat(capturedNote.captured.updatedAt).isAtMost(afterCall)
    }

    @Test
    fun `GIVEN a note WHEN invoke is called THEN note id is preserved in updated note`() = runTest {
        val expectedId = 99
        val originalNote = createNewNote(id = expectedId, title = "ID Check Note")
        val capturedNote = slot<Note>()
        coJustRun { repository.editNote(capture(capturedNote)) }

        useCase(originalNote)

        assertThat(capturedNote.captured.id).isEqualTo(expectedId)
    }

    @Test
    fun `GIVEN a note with updated title WHEN invoke is called THEN repository receives note with new title`() = runTest {
        val expectedTitle = "Updated Title"
        val originalNote = createNewNote(id = 1, title = expectedTitle)
        val capturedNote = slot<Note>()
        coJustRun { repository.editNote(capture(capturedNote)) }

        useCase(originalNote)

        assertThat(capturedNote.captured.title).isEqualTo(expectedTitle)
    }

    @Test
    fun `GIVEN a note with text content WHEN invoke is called THEN repository receives note with same content`() = runTest {
        val expectedContent = listOf(
            ContentItem.Text("Updated paragraph"),
            ContentItem.Image("https://example.com/updated.png")
        )
        val originalNote = createNewNote(id = 1, title = "Content Note", content = expectedContent)
        val capturedNote = slot<Note>()
        coJustRun { repository.editNote(capture(capturedNote)) }

        useCase(originalNote)

        assertThat(capturedNote.captured.content).isEqualTo(expectedContent)
        assertThat(capturedNote.captured.content).hasSize(2)
    }

    @Test
    fun `GIVEN a pinned note WHEN invoke is called THEN pinned status is preserved`() = runTest {
        val originalNote = createNewNote(id = 1, title = "Pinned Note", isPinned = true)
        val capturedNote = slot<Note>()
        coJustRun { repository.editNote(capture(capturedNote)) }

        useCase(originalNote)

        assertThat(capturedNote.captured.isPinned).isTrue()
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke is called THEN exception propagates`() = runTest {
        val originalNote = createNewNote(id = 1, title = "Note that fails")
        coEvery { repository.editNote(any()) } throws IllegalStateException("Edit failed")

        val result = runCatching { useCase(originalNote) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Edit failed")
    }

    @Test
    fun `GIVEN original note has specific timestamp WHEN invoke is called THEN original note object is not mutated`() = runTest {
        val originalTimestamp = 5000L
        val originalNote = createNewNote(id = 1, title = "Immutability Check", updatedAt = originalTimestamp)
        coJustRun { repository.editNote(any()) }

        useCase(originalNote)

        assertThat(originalNote.updatedAt).isEqualTo(originalTimestamp)
    }
}