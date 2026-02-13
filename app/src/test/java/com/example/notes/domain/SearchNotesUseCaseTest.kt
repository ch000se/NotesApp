package com.example.notes.domain

import app.cash.turbine.test
import com.example.notes.stubs.createNewNote
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SearchNotesUseCaseTest {

    @MockK
    private lateinit var repository: NotesRepository

    private lateinit var useCase: SearchNotesUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = SearchNotesUseCase(repository)
    }

    @Test
    fun `GIVEN query matches notes WHEN invoke is called THEN emits matching notes`() = runTest {
        val query = "shopping"
        val expectedNotes = listOf(
            createNewNote(id = 1, title = "Shopping list"),
            createNewNote(id = 2, title = "Shopping for party")
        )
        every { repository.searchNotes(query) } returns flowOf(expectedNotes)

        val result = useCase(query)

        result.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).isEqualTo(expectedNotes)
            assertThat(emittedNotes).hasSize(2)
            awaitComplete()
        }
        verify(exactly = 1) { repository.searchNotes(query) }
    }

    @Test
    fun `GIVEN query matches no notes WHEN invoke is called THEN emits empty list`() = runTest {
        val query = "nonexistent_term_xyz"
        every { repository.searchNotes(query) } returns flowOf(emptyList())

        val result = useCase(query)

        result.test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
        verify(exactly = 1) { repository.searchNotes(query) }
    }

    @Test
    fun `GIVEN empty query string WHEN invoke is called THEN delegates empty query to repository`() = runTest {
        val query = ""
        val allNotes = listOf(
            createNewNote(id = 1, title = "First Note"),
            createNewNote(id = 2, title = "Second Note")
        )
        every { repository.searchNotes(query) } returns flowOf(allNotes)

        val result = useCase(query)

        result.test {
            assertThat(awaitItem()).isEqualTo(allNotes)
            awaitComplete()
        }
        verify(exactly = 1) { repository.searchNotes(query) }
    }

    @Test
    fun `GIVEN query with single character WHEN invoke is called THEN delegates query to repository`() = runTest {
        val query = "a"
        val matchingNotes = listOf(createNewNote(id = 3, title = "Android tips"))
        every { repository.searchNotes(query) } returns flowOf(matchingNotes)

        val result = useCase(query)

        result.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(1)
            awaitComplete()
        }
        verify(exactly = 1) { repository.searchNotes(query) }
    }

    @Test
    fun `GIVEN repository emits multiple updates for query WHEN invoke is called THEN all emissions are forwarded`() = runTest {
        val query = "kotlin"
        val firstResult = listOf(createNewNote(id = 1, title = "Kotlin basics"))
        val secondResult = listOf(
            createNewNote(id = 1, title = "Kotlin basics"),
            createNewNote(id = 2, title = "Kotlin coroutines")
        )
        every { repository.searchNotes(query) } returns flowOf(firstResult, secondResult)

        val result = useCase(query)

        result.test {
            assertThat(awaitItem()).isEqualTo(firstResult)
            assertThat(awaitItem()).isEqualTo(secondResult)
            awaitComplete()
        }
        verify(exactly = 1) { repository.searchNotes(query) }
    }

    @Test
    fun `GIVEN query with spaces WHEN invoke is called THEN passes exact query string to repository`() = runTest {
        val query = "my important note"
        val expectedNotes = listOf(createNewNote(id = 5, title = "My important note"))
        every { repository.searchNotes(query) } returns flowOf(expectedNotes)

        val result = useCase(query)

        result.test {
            assertThat(awaitItem()).isEqualTo(expectedNotes)
            awaitComplete()
        }
        verify(exactly = 1) { repository.searchNotes(query) }
    }

    @Test
    fun `GIVEN different queries WHEN invoke is called for each THEN repository is called with each distinct query`() = runTest {
        val firstQuery = "work"
        val secondQuery = "personal"
        val workNotes = listOf(createNewNote(id = 1, title = "Work tasks"))
        val personalNotes = listOf(createNewNote(id = 2, title = "Personal goals"))
        every { repository.searchNotes(firstQuery) } returns flowOf(workNotes)
        every { repository.searchNotes(secondQuery) } returns flowOf(personalNotes)

        useCase(firstQuery).test {
            assertThat(awaitItem()).isEqualTo(workNotes)
            awaitComplete()
        }
        useCase(secondQuery).test {
            assertThat(awaitItem()).isEqualTo(personalNotes)
            awaitComplete()
        }

        verify(exactly = 1) { repository.searchNotes(firstQuery) }
        verify(exactly = 1) { repository.searchNotes(secondQuery) }
    }

    @Test
    fun `GIVEN query matches pinned note WHEN invoke is called THEN emits pinned note in results`() = runTest {
        val query = "important"
        val expectedNotes = listOf(
            createNewNote(id = 1, title = "Important meeting", isPinned = true),
            createNewNote(id = 2, title = "Important task", isPinned = false)
        )
        every { repository.searchNotes(query) } returns flowOf(expectedNotes)

        val result = useCase(query)

        result.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(2)
            assertThat(emittedNotes.first().isPinned).isTrue()
            assertThat(emittedNotes.last().isPinned).isFalse()
            awaitComplete()
        }
    }
}