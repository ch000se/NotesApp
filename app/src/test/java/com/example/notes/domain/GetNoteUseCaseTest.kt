package com.example.notes.domain

import com.example.notes.stubs.createNewNote
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GetNoteUseCaseTest {

    @MockK
    private lateinit var repository: NotesRepository

    private lateinit var useCase: GetNoteUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = GetNoteUseCase(repository)
    }

    @Test
    fun `GIVEN valid note id WHEN invoke is called THEN returns correct note`() = runTest {
        val noteId = 1
        val expectedNote = createNewNote(id = noteId, title = "My Note")
        coEvery { repository.getNote(noteId) } returns expectedNote

        val result = useCase(noteId)

        assertThat(result).isEqualTo(expectedNote)
        assertThat(result.id).isEqualTo(noteId)
        coVerify(exactly = 1) { repository.getNote(noteId) }
    }

    @Test
    fun `GIVEN note with text content WHEN invoke is called THEN returns note with text content`() = runTest {
        val noteId = 42
        val expectedContent = listOf(ContentItem.Text("Some text content"))
        val expectedNote = createNewNote(id = noteId, title = "Text Note", content = expectedContent)
        coEvery { repository.getNote(noteId) } returns expectedNote

        val result = useCase(noteId)

        assertThat(result.content).hasSize(1)
        assertThat(result.content.first()).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result.content.first() as ContentItem.Text).content).isEqualTo("Some text content")
        coVerify(exactly = 1) { repository.getNote(noteId) }
    }

    @Test
    fun `GIVEN note with image content WHEN invoke is called THEN returns note with image content`() = runTest {
        val noteId = 7
        val expectedContent = listOf(ContentItem.Image("https://example.com/photo.jpg"))
        val expectedNote = createNewNote(id = noteId, title = "Image Note", content = expectedContent)
        coEvery { repository.getNote(noteId) } returns expectedNote

        val result = useCase(noteId)

        assertThat(result.content).hasSize(1)
        assertThat(result.content.first()).isInstanceOf(ContentItem.Image::class.java)
        assertThat((result.content.first() as ContentItem.Image).url).isEqualTo("https://example.com/photo.jpg")
        coVerify(exactly = 1) { repository.getNote(noteId) }
    }

    @Test
    fun `GIVEN pinned note WHEN invoke is called THEN returns note with pinned status true`() = runTest {
        val noteId = 5
        val expectedNote = createNewNote(id = noteId, title = "Pinned Note", isPinned = true)
        coEvery { repository.getNote(noteId) } returns expectedNote

        val result = useCase(noteId)

        assertThat(result.isPinned).isTrue()
        coVerify(exactly = 1) { repository.getNote(noteId) }
    }

    @Test
    fun `GIVEN note with empty content WHEN invoke is called THEN returns note with empty content list`() = runTest {
        val noteId = 3
        val expectedNote = createNewNote(id = noteId, title = "Empty Note", content = emptyList())
        coEvery { repository.getNote(noteId) } returns expectedNote

        val result = useCase(noteId)

        assertThat(result.content).isEmpty()
        coVerify(exactly = 1) { repository.getNote(noteId) }
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke is called with invalid id THEN exception propagates`() = runTest {
        val invalidNoteId = -1
        coEvery { repository.getNote(invalidNoteId) } throws NoSuchElementException("Note not found")

        val result = runCatching { useCase(invalidNoteId) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NoSuchElementException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Note not found")
        coVerify(exactly = 1) { repository.getNote(invalidNoteId) }
    }

    @Test
    fun `GIVEN note with mixed content items WHEN invoke is called THEN returns note preserving content order`() = runTest {
        val noteId = 10
        val expectedContent = listOf(
            ContentItem.Text("First paragraph"),
            ContentItem.Image("https://example.com/img1.png"),
            ContentItem.Text("Second paragraph"),
            ContentItem.Image("https://example.com/img2.png")
        )
        val expectedNote = createNewNote(id = noteId, title = "Mixed Content Note", content = expectedContent)
        coEvery { repository.getNote(noteId) } returns expectedNote

        val result = useCase(noteId)

        assertThat(result.content).hasSize(4)
        assertThat(result.content[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat(result.content[1]).isInstanceOf(ContentItem.Image::class.java)
        assertThat(result.content[2]).isInstanceOf(ContentItem.Text::class.java)
        assertThat(result.content[3]).isInstanceOf(ContentItem.Image::class.java)
        coVerify(exactly = 1) { repository.getNote(noteId) }
    }
}