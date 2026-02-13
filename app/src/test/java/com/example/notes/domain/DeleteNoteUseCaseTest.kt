package com.example.notes.domain

import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DeleteNoteUseCaseTest {

    @MockK
    private lateinit var repository: NotesRepository

    private lateinit var useCase: DeleteNoteUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = DeleteNoteUseCase(repository)
    }

    @Test
    fun `GIVEN valid note id WHEN invoke is called THEN delegates deletion to repository`() = runTest {
        val noteId = 1
        coJustRun { repository.deleteNote(noteId) }

        useCase(noteId)

        coVerify(exactly = 1) { repository.deleteNote(noteId) }
    }

    @Test
    fun `GIVEN note id of zero WHEN invoke is called THEN delegates to repository with id zero`() = runTest {
        val noteId = 0
        coJustRun { repository.deleteNote(noteId) }

        useCase(noteId)

        coVerify(exactly = 1) { repository.deleteNote(noteId) }
    }

    @Test
    fun `GIVEN large note id WHEN invoke is called THEN delegates correct id to repository`() = runTest {
        val noteId = Int.MAX_VALUE
        coJustRun { repository.deleteNote(noteId) }

        useCase(noteId)

        coVerify(exactly = 1) { repository.deleteNote(noteId) }
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke is called THEN exception propagates`() = runTest {
        val noteId = 99
        coEvery { repository.deleteNote(noteId) } throws RuntimeException("Deletion failed")

        val result = runCatching { useCase(noteId) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Deletion failed")
        coVerify(exactly = 1) { repository.deleteNote(noteId) }
    }

    @Test
    fun `GIVEN multiple note ids WHEN invoke is called for each THEN each deletion is delegated to repository`() = runTest {
        val noteIds = listOf(1, 2, 3, 4, 5)
        noteIds.forEach { id -> coJustRun { repository.deleteNote(id) } }

        noteIds.forEach { id -> useCase(id) }

        noteIds.forEach { id ->
            coVerify(exactly = 1) { repository.deleteNote(id) }
        }
    }

    @Test
    fun `GIVEN same note id WHEN invoke is called twice THEN repository deleteNote is called twice`() = runTest {
        val noteId = 7
        coJustRun { repository.deleteNote(noteId) }

        useCase(noteId)
        useCase(noteId)

        coVerify(exactly = 2) { repository.deleteNote(noteId) }
    }

    @Test
    fun `GIVEN repository throws NoSuchElementException WHEN invoke is called with non-existent id THEN exception propagates`() = runTest {
        val nonExistentId = -1
        coEvery { repository.deleteNote(nonExistentId) } throws NoSuchElementException("Note with id $nonExistentId does not exist")

        val result = runCatching { useCase(nonExistentId) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NoSuchElementException::class.java)
    }
}