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

class SwitchPinnedStatusUseCaseTest {

    @MockK
    private lateinit var repository: NotesRepository

    private lateinit var useCase: SwitchPinnedStatusUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = SwitchPinnedStatusUseCase(repository)
    }

    @Test
    fun `GIVEN valid note id WHEN invoke is called THEN delegates to repository`() = runTest {
        val noteId = 1
        coJustRun { repository.switchPinnedStatus(noteId) }

        useCase(noteId)

        coVerify(exactly = 1) { repository.switchPinnedStatus(noteId) }
    }

    @Test
    fun `GIVEN note id of first note WHEN invoke is called THEN correct id is passed to repository`() = runTest {
        val noteId = 1
        coJustRun { repository.switchPinnedStatus(noteId) }

        useCase(noteId)

        coVerify(exactly = 1) { repository.switchPinnedStatus(noteId) }
    }

    @Test
    fun `GIVEN note id of last note WHEN invoke is called THEN correct id is passed to repository`() = runTest {
        val noteId = Int.MAX_VALUE
        coJustRun { repository.switchPinnedStatus(noteId) }

        useCase(noteId)

        coVerify(exactly = 1) { repository.switchPinnedStatus(noteId) }
    }

    @Test
    fun `GIVEN valid note id WHEN invoke is called twice THEN repository switchPinnedStatus is called twice`() = runTest {
        val noteId = 5
        coJustRun { repository.switchPinnedStatus(noteId) }

        useCase(noteId)
        useCase(noteId)

        coVerify(exactly = 2) { repository.switchPinnedStatus(noteId) }
    }

    @Test
    fun `GIVEN different note ids WHEN invoke is called for each THEN repository receives each id separately`() = runTest {
        val firstNoteId = 10
        val secondNoteId = 20
        coJustRun { repository.switchPinnedStatus(firstNoteId) }
        coJustRun { repository.switchPinnedStatus(secondNoteId) }

        useCase(firstNoteId)
        useCase(secondNoteId)

        coVerify(exactly = 1) { repository.switchPinnedStatus(firstNoteId) }
        coVerify(exactly = 1) { repository.switchPinnedStatus(secondNoteId) }
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke is called THEN exception propagates`() = runTest {
        val noteId = 3
        coEvery { repository.switchPinnedStatus(noteId) } throws RuntimeException("Switch failed")

        val result = runCatching { useCase(noteId) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Switch failed")
        coVerify(exactly = 1) { repository.switchPinnedStatus(noteId) }
    }

    @Test
    fun `GIVEN repository throws NoSuchElementException WHEN invoke is called with non-existent id THEN exception propagates`() = runTest {
        val nonExistentId = -99
        coEvery { repository.switchPinnedStatus(nonExistentId) } throws NoSuchElementException("Note not found")

        val result = runCatching { useCase(nonExistentId) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NoSuchElementException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Note not found")
    }

    @Test
    fun `GIVEN note id of zero WHEN invoke is called THEN delegates to repository with id zero`() = runTest {
        val noteId = 0
        coJustRun { repository.switchPinnedStatus(noteId) }

        useCase(noteId)

        coVerify(exactly = 1) { repository.switchPinnedStatus(noteId) }
    }
}