@file:Suppress("UNUSED_VALUE")

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

class GetAllNotesUseCaseTest {

    @MockK
    private lateinit var repository: NotesRepository

    private lateinit var useCase: GetAllNotesUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = GetAllNotesUseCase(repository)
    }

    @Test
    fun `GIVEN repository has notes WHEN invoke is called THEN emits list of notes`() = runTest {
        val expectedNotes = listOf(
            createNewNote(id = 1, title = "First Note"),
            createNewNote(id = 2, title = "Second Note"),
            createNewNote(id = 3, title = "Third Note")
        )
        every { repository.getAllNotes() } returns flowOf(expectedNotes)

        val result = useCase()

        result.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).isEqualTo(expectedNotes)
            assertThat(emittedNotes).hasSize(3)
            awaitComplete()
        }
        verify(exactly = 1) { repository.getAllNotes() }
    }

    @Test
    fun `GIVEN repository has no notes WHEN invoke is called THEN emits empty list`() = runTest {
        val expectedNotes = emptyList<Note>()
        every { repository.getAllNotes() } returns flowOf(expectedNotes)

        val result = useCase()

        result.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).isEmpty()
            awaitComplete()
        }
        verify(exactly = 1) { repository.getAllNotes() }
    }

    @Test
    fun `GIVEN repository emits multiple updates WHEN invoke is called THEN all emissions are forwarded`() = runTest {
        val firstBatch = listOf(createNewNote(id = 1, title = "Note A"))
        val secondBatch = listOf(
            createNewNote(id = 1, title = "Note A"),
            createNewNote(id = 2, title = "Note B")
        )
        every { repository.getAllNotes() } returns flowOf(firstBatch, secondBatch)

        val result = useCase()

        result.test {
            assertThat(awaitItem()).isEqualTo(firstBatch)
            assertThat(awaitItem()).isEqualTo(secondBatch)
            awaitComplete()
        }
        verify(exactly = 1) { repository.getAllNotes() }
    }

    @Test
    fun `GIVEN repository has pinned and unpinned notes WHEN invoke is called THEN emits all notes without filtering`() = runTest {
        val expectedNotes = listOf(
            createNewNote(id = 1, title = "Pinned Note", isPinned = true),
            createNewNote(id = 2, title = "Regular Note", isPinned = false)
        )
        every { repository.getAllNotes() } returns flowOf(expectedNotes)

        val result = useCase()

        result.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(2)
            assertThat(emittedNotes.first().isPinned).isTrue()
            assertThat(emittedNotes.last().isPinned).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN repository has notes with mixed content WHEN invoke is called THEN emits notes with correct content`() = runTest {
        val expectedNotes = listOf(
            createNewNote(
                id = 1,
                title = "Note with text",
                content = listOf(ContentItem.Text("Hello world"))
            ),
            createNewNote(
                id = 2,
                title = "Note with image",
                content = listOf(ContentItem.Image("https://example.com/image.png"))
            )
        )
        every { repository.getAllNotes() } returns flowOf(expectedNotes)

        val result = useCase()

        result.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(2)
            assertThat(emittedNotes[0].content.first()).isInstanceOf(ContentItem.Text::class.java)
            assertThat(emittedNotes[1].content.first()).isInstanceOf(ContentItem.Image::class.java)
            awaitComplete()
        }
    }
}