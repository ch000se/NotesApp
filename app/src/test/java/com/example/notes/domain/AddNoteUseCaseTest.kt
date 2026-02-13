package com.example.notes.domain

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

class AddNoteUseCaseTest {

    @MockK
    private lateinit var repository: NotesRepository

    private lateinit var useCase: AddNoteUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = AddNoteUseCase(repository)
    }

    @Test
    fun `GIVEN title and content WHEN invoke is called THEN delegates to repository with isPinned false`() = runTest {
        val title = "New Note"
        val content = listOf(ContentItem.Text("Note body"))
        val isPinnedSlot = slot<Boolean>()
        coJustRun {
            repository.addNote(
                title = title,
                content = content,
                isPinned = capture(isPinnedSlot),
                updatedAt = any()
            )
        }

        useCase(title, content)

        assertThat(isPinnedSlot.captured).isFalse()
        coVerify(exactly = 1) {
            repository.addNote(
                title = title,
                content = content,
                isPinned = false,
                updatedAt = any()
            )
        }
    }

    @Test
    fun `GIVEN title and content WHEN invoke is called THEN passes current timestamp to repository`() = runTest {
        val title = "Timestamped Note"
        val content = emptyList<ContentItem>()
        val capturedTimestamp = slot<Long>()
        val beforeCall = System.currentTimeMillis()
        coJustRun {
            repository.addNote(
                title = any(),
                content = any(),
                isPinned = any(),
                updatedAt = capture(capturedTimestamp)
            )
        }

        useCase(title, content)

        val afterCall = System.currentTimeMillis()
        assertThat(capturedTimestamp.captured).isAtLeast(beforeCall)
        assertThat(capturedTimestamp.captured).isAtMost(afterCall)
    }

    @Test
    fun `GIVEN empty title and empty content WHEN invoke is called THEN still delegates to repository`() = runTest {
        val title = ""
        val content = emptyList<ContentItem>()
        coJustRun {
            repository.addNote(
                title = title,
                content = content,
                isPinned = false,
                updatedAt = any()
            )
        }

        useCase(title, content)

        coVerify(exactly = 1) {
            repository.addNote(
                title = title,
                content = content,
                isPinned = false,
                updatedAt = any()
            )
        }
    }

    @Test
    fun `GIVEN content with only images WHEN invoke is called THEN passes image content to repository`() = runTest {
        val title = "Image Note"
        val content = listOf(
            ContentItem.Image("https://example.com/photo1.jpg"),
            ContentItem.Image("https://example.com/photo2.jpg")
        )
        val capturedContent = slot<List<ContentItem>>()
        coJustRun {
            repository.addNote(
                title = any(),
                content = capture(capturedContent),
                isPinned = any(),
                updatedAt = any()
            )
        }

        useCase(title, content)

        assertThat(capturedContent.captured).hasSize(2)
        assertThat(capturedContent.captured.all { it is ContentItem.Image }).isTrue()
        coVerify(exactly = 1) {
            repository.addNote(
                title = title,
                content = content,
                isPinned = false,
                updatedAt = any()
            )
        }
    }

    @Test
    fun `GIVEN content with mixed items WHEN invoke is called THEN passes all content items to repository`() = runTest {
        val title = "Mixed Note"
        val content = listOf(
            ContentItem.Text("Opening paragraph"),
            ContentItem.Image("https://example.com/image.png"),
            ContentItem.Text("Closing paragraph")
        )
        val capturedContent = slot<List<ContentItem>>()
        coJustRun {
            repository.addNote(
                title = any(),
                content = capture(capturedContent),
                isPinned = any(),
                updatedAt = any()
            )
        }

        useCase(title, content)

        assertThat(capturedContent.captured).isEqualTo(content)
        assertThat(capturedContent.captured).hasSize(3)
    }

    @Test
    fun `GIVEN repository throws exception WHEN invoke is called THEN exception propagates`() = runTest {
        val title = "Failing Note"
        val content = listOf(ContentItem.Text("Content"))
        coEvery {
            repository.addNote(
                title = any(),
                content = any(),
                isPinned = any(),
                updatedAt = any()
            )
        } throws RuntimeException("Database error")

        val result = runCatching { useCase(title, content) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Database error")
    }

    @Test
    fun `GIVEN note is added WHEN invoke is called multiple times THEN repository is called for each invocation`() = runTest {
        val titles = listOf("First Note", "Second Note", "Third Note")
        val content = listOf(ContentItem.Text("Content"))
        coJustRun {
            repository.addNote(
                title = any(),
                content = any(),
                isPinned = any(),
                updatedAt = any()
            )
        }

        titles.forEach { title -> useCase(title, content) }

        coVerify(exactly = 3) {
            repository.addNote(
                title = any(),
                content = any(),
                isPinned = false,
                updatedAt = any()
            )
        }
    }
}