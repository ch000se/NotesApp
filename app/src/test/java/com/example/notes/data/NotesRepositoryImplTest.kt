package com.example.notes.data

import app.cash.turbine.test
import com.example.notes.domain.ContentItem
import com.example.notes.stubs.createNewNote
import com.example.notes.stubs.createNoteWithContentDbModel
import com.google.common.truth.Truth.assertThat
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class NotesRepositoryImplTest {

    @MockK
    private lateinit var notesDao: NotesDao

    @MockK
    private lateinit var imageFileManager: ImageFileManager

    private lateinit var repository: NotesRepositoryImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        repository = NotesRepositoryImpl(notesDao, imageFileManager)
    }

    // region addNote

    @Test
    fun `GIVEN text-only content WHEN addNote is called THEN delegates to DAO with correct NoteDbModel`() = runTest {
        val expectedTitle = "Shopping List"
        val expectedUpdatedAt = 1_700_000_000_000L
        val expectedIsPinned = false
        val content = listOf(ContentItem.Text("Buy milk"))
        val capturedNoteDbModel = slot<NoteDbModel>()
        coJustRun {
            notesDao.addNoteWithContent(capture(capturedNoteDbModel), any())
        }

        repository.addNote(
            title = expectedTitle,
            content = content,
            isPinned = expectedIsPinned,
            updatedAt = expectedUpdatedAt
        )

        assertThat(capturedNoteDbModel.captured.id).isEqualTo(0)
        assertThat(capturedNoteDbModel.captured.title).isEqualTo(expectedTitle)
        assertThat(capturedNoteDbModel.captured.updatedAt).isEqualTo(expectedUpdatedAt)
        assertThat(capturedNoteDbModel.captured.isPinned).isEqualTo(expectedIsPinned)
        coVerify(exactly = 1) { notesDao.addNoteWithContent(any(), any()) }
    }

    @Test
    fun `GIVEN isPinned true WHEN addNote is called THEN DAO receives NoteDbModel with isPinned true`() = runTest {
        val capturedNoteDbModel = slot<NoteDbModel>()
        coJustRun { notesDao.addNoteWithContent(capture(capturedNoteDbModel), any()) }

        repository.addNote(
            title = "Pinned Note",
            content = emptyList(),
            isPinned = true,
            updatedAt = 1000L
        )

        assertThat(capturedNoteDbModel.captured.isPinned).isTrue()
    }

    @Test
    fun `GIVEN text-only content WHEN addNote is called THEN DAO receives content unchanged`() = runTest {
        val content = listOf(
            ContentItem.Text("First paragraph"),
            ContentItem.Text("Second paragraph")
        )
        val capturedContent = slot<List<ContentItem>>()
        coJustRun { notesDao.addNoteWithContent(any(), capture(capturedContent)) }

        repository.addNote(
            title = "Text Note",
            content = content,
            isPinned = false,
            updatedAt = 2000L
        )

        assertThat(capturedContent.captured).hasSize(2)
        assertThat(capturedContent.captured[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((capturedContent.captured[0] as ContentItem.Text).content).isEqualTo("First paragraph")
        assertThat(capturedContent.captured[1]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((capturedContent.captured[1] as ContentItem.Text).content).isEqualTo("Second paragraph")
    }

    @Test
    fun `GIVEN content with internal image WHEN addNote is called THEN internal image url is not changed`() = runTest {
        val internalUrl = "/data/user/0/com.example.notes/files/IMG_abc.jpg"
        val content = listOf(ContentItem.Image(internalUrl))
        val capturedContent = slot<List<ContentItem>>()
        every { imageFileManager.isInternal(internalUrl) } returns true
        coJustRun { notesDao.addNoteWithContent(any(), capture(capturedContent)) }

        repository.addNote(
            title = "Internal Image Note",
            content = content,
            isPinned = false,
            updatedAt = 3000L
        )

        assertThat(capturedContent.captured).hasSize(1)
        val resultImage = capturedContent.captured[0] as ContentItem.Image
        assertThat(resultImage.url).isEqualTo(internalUrl)
        coVerify(exactly = 0) { imageFileManager.copyImageToInternalStorage(any()) }
    }

    @Test
    fun `GIVEN content with external image WHEN addNote is called THEN external image is copied to internal storage`() = runTest {
        val externalUrl = "content://media/external/images/media/123"
        val expectedInternalUrl = "/data/user/0/com.example.notes/files/IMG_xyz.jpg"
        val content = listOf(ContentItem.Image(externalUrl))
        val capturedContent = slot<List<ContentItem>>()
        every { imageFileManager.isInternal(externalUrl) } returns false
        coEvery { imageFileManager.copyImageToInternalStorage(externalUrl) } returns expectedInternalUrl
        coJustRun { notesDao.addNoteWithContent(any(), capture(capturedContent)) }

        repository.addNote(
            title = "External Image Note",
            content = content,
            isPinned = false,
            updatedAt = 4000L
        )

        assertThat(capturedContent.captured).hasSize(1)
        val resultImage = capturedContent.captured[0] as ContentItem.Image
        assertThat(resultImage.url).isEqualTo(expectedInternalUrl)
        coVerify(exactly = 1) { imageFileManager.copyImageToInternalStorage(externalUrl) }
    }

    @Test
    fun `GIVEN content with mixed internal and external images WHEN addNote is called THEN only external images are copied`() = runTest {
        val internalUrl = "/data/user/0/com.example.notes/files/IMG_existing.jpg"
        val externalUrl = "content://media/external/images/media/456"
        val expectedCopiedUrl = "/data/user/0/com.example.notes/files/IMG_new.jpg"
        val content = listOf(
            ContentItem.Image(internalUrl),
            ContentItem.Image(externalUrl)
        )
        val capturedContent = slot<List<ContentItem>>()
        every { imageFileManager.isInternal(internalUrl) } returns true
        every { imageFileManager.isInternal(externalUrl) } returns false
        coEvery { imageFileManager.copyImageToInternalStorage(externalUrl) } returns expectedCopiedUrl
        coJustRun { notesDao.addNoteWithContent(any(), capture(capturedContent)) }

        repository.addNote(
            title = "Mixed Images Note",
            content = content,
            isPinned = false,
            updatedAt = 5000L
        )

        assertThat(capturedContent.captured).hasSize(2)
        assertThat((capturedContent.captured[0] as ContentItem.Image).url).isEqualTo(internalUrl)
        assertThat((capturedContent.captured[1] as ContentItem.Image).url).isEqualTo(expectedCopiedUrl)
        coVerify(exactly = 0) { imageFileManager.copyImageToInternalStorage(internalUrl) }
        coVerify(exactly = 1) { imageFileManager.copyImageToInternalStorage(externalUrl) }
    }

    @Test
    fun `GIVEN mixed text and external image content WHEN addNote is called THEN text items are unchanged and image is copied`() = runTest {
        val externalUrl = "content://media/external/images/media/789"
        val expectedInternalUrl = "/data/user/0/com.example.notes/files/IMG_copied.jpg"
        val content = listOf(
            ContentItem.Text("Opening paragraph"),
            ContentItem.Image(externalUrl),
            ContentItem.Text("Closing paragraph")
        )
        val capturedContent = slot<List<ContentItem>>()
        every { imageFileManager.isInternal(externalUrl) } returns false
        coEvery { imageFileManager.copyImageToInternalStorage(externalUrl) } returns expectedInternalUrl
        coJustRun { notesDao.addNoteWithContent(any(), capture(capturedContent)) }

        repository.addNote(
            title = "Mixed Content Note",
            content = content,
            isPinned = false,
            updatedAt = 6000L
        )

        assertThat(capturedContent.captured).hasSize(3)
        assertThat(capturedContent.captured[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((capturedContent.captured[0] as ContentItem.Text).content).isEqualTo("Opening paragraph")
        assertThat(capturedContent.captured[1]).isInstanceOf(ContentItem.Image::class.java)
        assertThat((capturedContent.captured[1] as ContentItem.Image).url).isEqualTo(expectedInternalUrl)
        assertThat(capturedContent.captured[2]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((capturedContent.captured[2] as ContentItem.Text).content).isEqualTo("Closing paragraph")
    }

    @Test
    fun `GIVEN empty content WHEN addNote is called THEN DAO is called once with empty content`() = runTest {
        val capturedContent = slot<List<ContentItem>>()
        coJustRun { notesDao.addNoteWithContent(any(), capture(capturedContent)) }

        repository.addNote(
            title = "Empty Note",
            content = emptyList(),
            isPinned = false,
            updatedAt = 7000L
        )

        assertThat(capturedContent.captured).isEmpty()
        coVerify(exactly = 1) { notesDao.addNoteWithContent(any(), any()) }
    }

    @Test
    fun `GIVEN addNote is called THEN NoteDbModel id is always zero for auto-generation`() = runTest {
        val capturedNoteDbModel = slot<NoteDbModel>()
        coJustRun { notesDao.addNoteWithContent(capture(capturedNoteDbModel), any()) }

        repository.addNote(
            title = "Auto ID Note",
            content = emptyList(),
            isPinned = false,
            updatedAt = 8000L
        )

        assertThat(capturedNoteDbModel.captured.id).isEqualTo(0)
    }

    // endregion

    // region deleteNote

    @Test
    fun `GIVEN note with no images WHEN deleteNote is called THEN DAO deleteNote is called and no image deletions occur`() = runTest {
        val noteId = 1
        val noteWithContent = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.TEXT, content = "Some text", order = 0)
            )
        )
        coEvery { notesDao.getNote(noteId) } returns noteWithContent
        coJustRun { notesDao.deleteNote(noteId) }

        repository.deleteNote(noteId)

        coVerify(exactly = 1) { notesDao.deleteNote(noteId) }
        coVerify(exactly = 0) { imageFileManager.deleteImage(any()) }
    }

    @Test
    fun `GIVEN note with one image WHEN deleteNote is called THEN image is deleted from filesystem`() = runTest {
        val noteId = 2
        val imageUrl = "/data/user/0/com.example.notes/files/IMG_to_delete.jpg"
        val noteWithContent = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = imageUrl, order = 0)
            )
        )
        coEvery { notesDao.getNote(noteId) } returns noteWithContent
        coJustRun { notesDao.deleteNote(noteId) }
        coJustRun { imageFileManager.deleteImage(imageUrl) }

        repository.deleteNote(noteId)

        coVerify(exactly = 1) { notesDao.deleteNote(noteId) }
        coVerify(exactly = 1) { imageFileManager.deleteImage(imageUrl) }
    }

    @Test
    fun `GIVEN note with multiple images WHEN deleteNote is called THEN all images are deleted from filesystem`() = runTest {
        val noteId = 3
        val imageUrl1 = "/data/user/0/com.example.notes/files/IMG_first.jpg"
        val imageUrl2 = "/data/user/0/com.example.notes/files/IMG_second.jpg"
        val noteWithContent = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = imageUrl1, order = 0),
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = imageUrl2, order = 1)
            )
        )
        coEvery { notesDao.getNote(noteId) } returns noteWithContent
        coJustRun { notesDao.deleteNote(noteId) }
        coJustRun { imageFileManager.deleteImage(any()) }

        repository.deleteNote(noteId)

        coVerify(exactly = 1) { notesDao.deleteNote(noteId) }
        coVerify(exactly = 1) { imageFileManager.deleteImage(imageUrl1) }
        coVerify(exactly = 1) { imageFileManager.deleteImage(imageUrl2) }
    }

    @Test
    fun `GIVEN note with mixed content WHEN deleteNote is called THEN only images are deleted not text content`() = runTest {
        val noteId = 4
        val imageUrl = "/data/user/0/com.example.notes/files/IMG_mixed.jpg"
        val noteWithContent = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.TEXT, content = "Some text", order = 0),
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = imageUrl, order = 1),
                ContentItemDbModel(noteId = noteId, contentType = ContentType.TEXT, content = "More text", order = 2)
            )
        )
        coEvery { notesDao.getNote(noteId) } returns noteWithContent
        coJustRun { notesDao.deleteNote(noteId) }
        coJustRun { imageFileManager.deleteImage(imageUrl) }

        repository.deleteNote(noteId)

        coVerify(exactly = 1) { imageFileManager.deleteImage(imageUrl) }
        coVerify(exactly = 0) { imageFileManager.deleteImage("Some text") }
        coVerify(exactly = 0) { imageFileManager.deleteImage("More text") }
    }

    @Test
    fun `GIVEN deleteNote is called THEN note is fetched before deletion`() = runTest {
        val noteId = 5
        val noteWithContent = createNoteWithContentDbModel(id = noteId, content = emptyList())
        coEvery { notesDao.getNote(noteId) } returns noteWithContent
        coJustRun { notesDao.deleteNote(noteId) }

        repository.deleteNote(noteId)

        coVerify(exactly = 1) { notesDao.getNote(noteId) }
        coVerify(exactly = 1) { notesDao.deleteNote(noteId) }
    }

    // endregion

    // region editNote

    @Test
    fun `GIVEN note with text-only content WHEN editNote is called THEN DAO updateNote is called with correct model`() = runTest {
        val noteId = 10
        val oldNote = createNoteWithContentDbModel(id = noteId, content = emptyList())
        val updatedNote = createNewNote(
            id = noteId,
            title = "Updated Title",
            content = listOf(ContentItem.Text("Updated text")),
            updatedAt = 9000L,
            isPinned = false
        )
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(any(), any()) }

        repository.editNote(updatedNote)

        coVerify(exactly = 1) { notesDao.updateNote(any(), any()) }
    }

    @Test
    fun `GIVEN editNote is called THEN NoteDbModel passed to DAO contains correct fields from the updated note`() = runTest {
        val noteId = 11
        val expectedTitle = "Edited Title"
        val expectedUpdatedAt = 10_000L
        val expectedIsPinned = true
        val oldNote = createNoteWithContentDbModel(id = noteId, content = emptyList())
        val updatedNote = createNewNote(
            id = noteId,
            title = expectedTitle,
            content = emptyList(),
            updatedAt = expectedUpdatedAt,
            isPinned = expectedIsPinned
        )
        val capturedNoteDbModel = slot<NoteDbModel>()
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(capture(capturedNoteDbModel), any()) }

        repository.editNote(updatedNote)

        assertThat(capturedNoteDbModel.captured.id).isEqualTo(noteId)
        assertThat(capturedNoteDbModel.captured.title).isEqualTo(expectedTitle)
        assertThat(capturedNoteDbModel.captured.updatedAt).isEqualTo(expectedUpdatedAt)
        assertThat(capturedNoteDbModel.captured.isPinned).isEqualTo(expectedIsPinned)
    }

    @Test
    fun `GIVEN old note has image that is removed in new version WHEN editNote is called THEN removed image is deleted`() = runTest {
        val noteId = 12
        val removedImageUrl = "/data/user/0/com.example.notes/files/IMG_old.jpg"
        val oldNote = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = removedImageUrl, order = 0)
            )
        )
        val updatedNote = createNewNote(
            id = noteId,
            title = "Note",
            content = listOf(ContentItem.Text("Only text now")),
            updatedAt = 11_000L,
            isPinned = false
        )
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(any(), any()) }
        coJustRun { imageFileManager.deleteImage(removedImageUrl) }

        repository.editNote(updatedNote)

        coVerify(exactly = 1) { imageFileManager.deleteImage(removedImageUrl) }
    }

    @Test
    fun `GIVEN old note has image that is retained in new version WHEN editNote is called THEN retained image is not deleted`() = runTest {
        val noteId = 13
        val retainedImageUrl = "/data/user/0/com.example.notes/files/IMG_kept.jpg"
        val oldNote = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = retainedImageUrl, order = 0)
            )
        )
        val updatedNote = createNewNote(
            id = noteId,
            title = "Note",
            content = listOf(ContentItem.Image(retainedImageUrl)),
            updatedAt = 12_000L,
            isPinned = false
        )
        every { imageFileManager.isInternal(retainedImageUrl) } returns true
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(any(), any()) }

        repository.editNote(updatedNote)

        coVerify(exactly = 0) { imageFileManager.deleteImage(retainedImageUrl) }
    }

    @Test
    fun `GIVEN old note has two images and new note removes one WHEN editNote is called THEN only removed image is deleted`() = runTest {
        val noteId = 14
        val removedUrl = "/data/user/0/com.example.notes/files/IMG_removed.jpg"
        val keptUrl = "/data/user/0/com.example.notes/files/IMG_kept.jpg"
        val oldNote = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = removedUrl, order = 0),
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = keptUrl, order = 1)
            )
        )
        val updatedNote = createNewNote(
            id = noteId,
            title = "Note",
            content = listOf(ContentItem.Image(keptUrl)),
            updatedAt = 13_000L,
            isPinned = false
        )
        every { imageFileManager.isInternal(keptUrl) } returns true
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(any(), any()) }
        coJustRun { imageFileManager.deleteImage(removedUrl) }

        repository.editNote(updatedNote)

        coVerify(exactly = 1) { imageFileManager.deleteImage(removedUrl) }
        coVerify(exactly = 0) { imageFileManager.deleteImage(keptUrl) }
    }

    @Test
    fun `GIVEN new note content has external image WHEN editNote is called THEN external image is copied to internal storage`() = runTest {
        val noteId = 15
        val externalUrl = "content://media/external/images/media/999"
        val expectedInternalUrl = "/data/user/0/com.example.notes/files/IMG_new_edit.jpg"
        val oldNote = createNoteWithContentDbModel(id = noteId, content = emptyList())
        val updatedNote = createNewNote(
            id = noteId,
            title = "Note with external",
            content = listOf(ContentItem.Image(externalUrl)),
            updatedAt = 14_000L,
            isPinned = false
        )
        val capturedContent = slot<List<ContentItemDbModel>>()
        every { imageFileManager.isInternal(externalUrl) } returns false
        coEvery { imageFileManager.copyImageToInternalStorage(externalUrl) } returns expectedInternalUrl
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(any(), capture(capturedContent)) }

        repository.editNote(updatedNote)

        coVerify(exactly = 1) { imageFileManager.copyImageToInternalStorage(externalUrl) }
        assertThat(capturedContent.captured).hasSize(1)
        assertThat(capturedContent.captured[0].content).isEqualTo(expectedInternalUrl)
        assertThat(capturedContent.captured[0].contentType).isEqualTo(ContentType.IMAGE)
    }

    @Test
    fun `GIVEN new note content has internal image WHEN editNote is called THEN internal image is not copied again`() = runTest {
        val noteId = 16
        val internalUrl = "/data/user/0/com.example.notes/files/IMG_already_internal.jpg"
        val oldNote = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = internalUrl, order = 0)
            )
        )
        val updatedNote = createNewNote(
            id = noteId,
            title = "Note",
            content = listOf(ContentItem.Image(internalUrl)),
            updatedAt = 15_000L,
            isPinned = false
        )
        every { imageFileManager.isInternal(internalUrl) } returns true
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(any(), any()) }

        repository.editNote(updatedNote)

        coVerify(exactly = 0) { imageFileManager.copyImageToInternalStorage(any()) }
    }

    @Test
    fun `GIVEN old note has no images WHEN editNote is called with no images THEN no image operations are performed`() = runTest {
        val noteId = 17
        val oldNote = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.TEXT, content = "Old text", order = 0)
            )
        )
        val updatedNote = createNewNote(
            id = noteId,
            title = "Note",
            content = listOf(ContentItem.Text("New text")),
            updatedAt = 16_000L,
            isPinned = false
        )
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(any(), any()) }

        repository.editNote(updatedNote)

        coVerify(exactly = 0) { imageFileManager.deleteImage(any()) }
        coVerify(exactly = 0) { imageFileManager.copyImageToInternalStorage(any()) }
    }

    @Test
    fun `GIVEN editNote is called THEN content db models passed to DAO use the note id as noteId`() = runTest {
        val noteId = 18
        val expectedNoteId = noteId
        val oldNote = createNoteWithContentDbModel(id = noteId, content = emptyList())
        val updatedNote = createNewNote(
            id = noteId,
            title = "Note",
            content = listOf(ContentItem.Text("Line one"), ContentItem.Text("Line two")),
            updatedAt = 17_000L,
            isPinned = false
        )
        val capturedContent = slot<List<ContentItemDbModel>>()
        coEvery { notesDao.getNote(noteId) } returns oldNote
        coJustRun { notesDao.updateNote(any(), capture(capturedContent)) }

        repository.editNote(updatedNote)

        assertThat(capturedContent.captured.all { it.noteId == expectedNoteId }).isTrue()
    }

    // endregion

    // region getAllNotes

    @Test
    fun `GIVEN DAO has notes WHEN getAllNotes is called THEN emits mapped list of notes`() = runTest {
        val expectedNotes = listOf(
            createNoteWithContentDbModel(id = 1, title = "First"),
            createNoteWithContentDbModel(id = 2, title = "Second")
        )
        every { notesDao.getAllNotes() } returns flowOf(expectedNotes)

        val result = repository.getAllNotes()

        result.test {
            val emitted = awaitItem()
            assertThat(emitted).hasSize(2)
            assertThat(emitted[0].id).isEqualTo(1)
            assertThat(emitted[0].title).isEqualTo("First")
            assertThat(emitted[1].id).isEqualTo(2)
            assertThat(emitted[1].title).isEqualTo("Second")
            awaitComplete()
        }
        verify(exactly = 1) { notesDao.getAllNotes() }
    }

    @Test
    fun `GIVEN DAO has no notes WHEN getAllNotes is called THEN emits empty list`() = runTest {
        every { notesDao.getAllNotes() } returns flowOf(emptyList())

        val result = repository.getAllNotes()

        result.test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN DAO emits multiple updates WHEN getAllNotes is called THEN all emissions are forwarded as domain notes`() = runTest {
        val firstBatch = listOf(createNoteWithContentDbModel(id = 1, title = "Note A"))
        val secondBatch = listOf(
            createNoteWithContentDbModel(id = 1, title = "Note A"),
            createNoteWithContentDbModel(id = 2, title = "Note B")
        )
        every { notesDao.getAllNotes() } returns flowOf(firstBatch, secondBatch)

        val result = repository.getAllNotes()

        result.test {
            assertThat(awaitItem()).hasSize(1)
            assertThat(awaitItem()).hasSize(2)
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN DAO note has image content WHEN getAllNotes is called THEN emitted note contains ContentItem Image`() = runTest {
        val imageUrl = "/data/user/0/com.example.notes/files/IMG_list.jpg"
        val dbNote = createNoteWithContentDbModel(
            id = 1,
            title = "Image Note",
            content = listOf(
                ContentItemDbModel(noteId = 1, contentType = ContentType.IMAGE, content = imageUrl, order = 0)
            )
        )
        every { notesDao.getAllNotes() } returns flowOf(listOf(dbNote))

        val result = repository.getAllNotes()

        result.test {
            val notes = awaitItem()
            assertThat(notes).hasSize(1)
            assertThat(notes[0].content[0]).isInstanceOf(ContentItem.Image::class.java)
            assertThat((notes[0].content[0] as ContentItem.Image).url).isEqualTo(imageUrl)
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN DAO note is pinned WHEN getAllNotes is called THEN emitted note has isPinned true`() = runTest {
        val dbNote = createNoteWithContentDbModel(id = 1, title = "Pinned", isPinned = true)
        every { notesDao.getAllNotes() } returns flowOf(listOf(dbNote))

        val result = repository.getAllNotes()

        result.test {
            val notes = awaitItem()
            assertThat(notes[0].isPinned).isTrue()
            awaitComplete()
        }
    }

    // endregion

    // region getNote

    @Test
    fun `GIVEN valid note id WHEN getNote is called THEN returns mapped domain note`() = runTest {
        val noteId = 20
        val expectedTitle = "My Note"
        val expectedUpdatedAt = 99_000L
        val dbNote = createNoteWithContentDbModel(
            id = noteId,
            title = expectedTitle,
            updatedAt = expectedUpdatedAt,
            isPinned = false
        )
        coEvery { notesDao.getNote(noteId) } returns dbNote

        val result = repository.getNote(noteId)

        assertThat(result.id).isEqualTo(noteId)
        assertThat(result.title).isEqualTo(expectedTitle)
        assertThat(result.updatedAt).isEqualTo(expectedUpdatedAt)
        assertThat(result.isPinned).isFalse()
        coVerify(exactly = 1) { notesDao.getNote(noteId) }
    }

    @Test
    fun `GIVEN note with text content in DAO WHEN getNote is called THEN returned note contains ContentItem Text`() = runTest {
        val noteId = 21
        val expectedTextContent = "Note body text"
        val dbNote = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.TEXT, content = expectedTextContent, order = 0)
            )
        )
        coEvery { notesDao.getNote(noteId) } returns dbNote

        val result = repository.getNote(noteId)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result.content[0] as ContentItem.Text).content).isEqualTo(expectedTextContent)
    }

    @Test
    fun `GIVEN note with image content in DAO WHEN getNote is called THEN returned note contains ContentItem Image`() = runTest {
        val noteId = 22
        val imageUrl = "/data/user/0/com.example.notes/files/IMG_single.jpg"
        val dbNote = createNoteWithContentDbModel(
            id = noteId,
            content = listOf(
                ContentItemDbModel(noteId = noteId, contentType = ContentType.IMAGE, content = imageUrl, order = 0)
            )
        )
        coEvery { notesDao.getNote(noteId) } returns dbNote

        val result = repository.getNote(noteId)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0]).isInstanceOf(ContentItem.Image::class.java)
        assertThat((result.content[0] as ContentItem.Image).url).isEqualTo(imageUrl)
    }

    @Test
    fun `GIVEN pinned note in DAO WHEN getNote is called THEN returned note has isPinned true`() = runTest {
        val noteId = 23
        val dbNote = createNoteWithContentDbModel(id = noteId, isPinned = true)
        coEvery { notesDao.getNote(noteId) } returns dbNote

        val result = repository.getNote(noteId)

        assertThat(result.isPinned).isTrue()
    }

    @Test
    fun `GIVEN DAO throws exception WHEN getNote is called THEN exception propagates`() = runTest {
        val noteId = 999
        coEvery { notesDao.getNote(noteId) } throws NoSuchElementException("Note not found")

        val result = runCatching { repository.getNote(noteId) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NoSuchElementException::class.java)
    }

    // endregion

    // region searchNotes

    @Test
    fun `GIVEN query matching notes WHEN searchNotes is called THEN emits mapped matching notes`() = runTest {
        val query = "meeting"
        val matchingNotes = listOf(
            createNoteWithContentDbModel(id = 1, title = "Team meeting"),
            createNoteWithContentDbModel(id = 2, title = "Project meeting notes")
        )
        every { notesDao.searchNotes(query) } returns flowOf(matchingNotes)

        val result = repository.searchNotes(query)

        result.test {
            val emitted = awaitItem()
            assertThat(emitted).hasSize(2)
            assertThat(emitted[0].title).isEqualTo("Team meeting")
            assertThat(emitted[1].title).isEqualTo("Project meeting notes")
            awaitComplete()
        }
        verify(exactly = 1) { notesDao.searchNotes(query) }
    }

    @Test
    fun `GIVEN query matching no notes WHEN searchNotes is called THEN emits empty list`() = runTest {
        val query = "nonexistent"
        every { notesDao.searchNotes(query) } returns flowOf(emptyList())

        val result = repository.searchNotes(query)

        result.test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
        verify(exactly = 1) { notesDao.searchNotes(query) }
    }

    @Test
    fun `GIVEN empty query WHEN searchNotes is called THEN delegates empty string to DAO`() = runTest {
        val query = ""
        val allNotes = listOf(
            createNoteWithContentDbModel(id = 1, title = "Any Note")
        )
        every { notesDao.searchNotes(query) } returns flowOf(allNotes)

        val result = repository.searchNotes(query)

        result.test {
            assertThat(awaitItem()).hasSize(1)
            awaitComplete()
        }
        verify(exactly = 1) { notesDao.searchNotes(query) }
    }

    @Test
    fun `GIVEN DAO emits multiple search result updates WHEN searchNotes is called THEN all updates are forwarded`() = runTest {
        val query = "task"
        val firstResult = listOf(createNoteWithContentDbModel(id = 1, title = "Task one"))
        val secondResult = listOf(
            createNoteWithContentDbModel(id = 1, title = "Task one"),
            createNoteWithContentDbModel(id = 2, title = "Task two")
        )
        every { notesDao.searchNotes(query) } returns flowOf(firstResult, secondResult)

        val result = repository.searchNotes(query)

        result.test {
            assertThat(awaitItem()).hasSize(1)
            assertThat(awaitItem()).hasSize(2)
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN search result note has content WHEN searchNotes is called THEN emitted note content is correctly mapped`() = runTest {
        val query = "image"
        val imageUrl = "/data/user/0/com.example.notes/files/IMG_search.jpg"
        val dbNote = createNoteWithContentDbModel(
            id = 1,
            title = "Note with image",
            content = listOf(
                ContentItemDbModel(noteId = 1, contentType = ContentType.IMAGE, content = imageUrl, order = 0)
            )
        )
        every { notesDao.searchNotes(query) } returns flowOf(listOf(dbNote))

        val result = repository.searchNotes(query)

        result.test {
            val notes = awaitItem()
            assertThat(notes[0].content[0]).isInstanceOf(ContentItem.Image::class.java)
            assertThat((notes[0].content[0] as ContentItem.Image).url).isEqualTo(imageUrl)
            awaitComplete()
        }
    }

    // endregion

    // region switchPinnedStatus

    @Test
    fun `GIVEN valid note id WHEN switchPinnedStatus is called THEN delegates to DAO`() = runTest {
        val noteId = 30
        coJustRun { notesDao.switchPinnedStatus(noteId) }

        repository.switchPinnedStatus(noteId)

        coVerify(exactly = 1) { notesDao.switchPinnedStatus(noteId) }
    }

    @Test
    fun `GIVEN note id of zero WHEN switchPinnedStatus is called THEN delegates zero to DAO`() = runTest {
        val noteId = 0
        coJustRun { notesDao.switchPinnedStatus(noteId) }

        repository.switchPinnedStatus(noteId)

        coVerify(exactly = 1) { notesDao.switchPinnedStatus(noteId) }
    }

    @Test
    fun `GIVEN switchPinnedStatus is called twice WHEN DAO is called THEN DAO receives exactly two calls`() = runTest {
        val noteId = 31
        coJustRun { notesDao.switchPinnedStatus(noteId) }

        repository.switchPinnedStatus(noteId)
        repository.switchPinnedStatus(noteId)

        coVerify(exactly = 2) { notesDao.switchPinnedStatus(noteId) }
    }

    @Test
    fun `GIVEN DAO throws exception WHEN switchPinnedStatus is called THEN exception propagates`() = runTest {
        val noteId = 32
        coEvery { notesDao.switchPinnedStatus(noteId) } throws RuntimeException("Switch failed")

        val result = runCatching { repository.switchPinnedStatus(noteId) }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(RuntimeException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Switch failed")
    }

    @Test
    fun `GIVEN multiple different note ids WHEN switchPinnedStatus is called for each THEN each call is delegated to DAO`() = runTest {
        val noteIds = listOf(1, 5, 10, 100)
        noteIds.forEach { id -> coJustRun { notesDao.switchPinnedStatus(id) } }

        noteIds.forEach { id -> repository.switchPinnedStatus(id) }

        noteIds.forEach { id ->
            coVerify(exactly = 1) { notesDao.switchPinnedStatus(id) }
        }
    }

    // endregion

    // region processForStorage (tested indirectly via addNote and editNote)

    @Test
    fun `GIVEN content with multiple external images WHEN addNote is called THEN all external images are copied`() = runTest {
        val externalUrl1 = "content://media/external/images/media/1"
        val externalUrl2 = "content://media/external/images/media/2"
        val internalUrl1 = "/data/user/0/com.example.notes/files/IMG_copy1.jpg"
        val internalUrl2 = "/data/user/0/com.example.notes/files/IMG_copy2.jpg"
        val content = listOf(
            ContentItem.Image(externalUrl1),
            ContentItem.Image(externalUrl2)
        )
        val capturedContent = slot<List<ContentItem>>()
        every { imageFileManager.isInternal(externalUrl1) } returns false
        every { imageFileManager.isInternal(externalUrl2) } returns false
        coEvery { imageFileManager.copyImageToInternalStorage(externalUrl1) } returns internalUrl1
        coEvery { imageFileManager.copyImageToInternalStorage(externalUrl2) } returns internalUrl2
        coJustRun { notesDao.addNoteWithContent(any(), capture(capturedContent)) }

        repository.addNote(
            title = "Multi Image Note",
            content = content,
            isPinned = false,
            updatedAt = 20_000L
        )

        coVerify(exactly = 1) { imageFileManager.copyImageToInternalStorage(externalUrl1) }
        coVerify(exactly = 1) { imageFileManager.copyImageToInternalStorage(externalUrl2) }
        assertThat((capturedContent.captured[0] as ContentItem.Image).url).isEqualTo(internalUrl1)
        assertThat((capturedContent.captured[1] as ContentItem.Image).url).isEqualTo(internalUrl2)
    }

    @Test
    fun `GIVEN content with only internal images WHEN addNote is called THEN copyImageToInternalStorage is never called`() = runTest {
        val internalUrl1 = "/data/user/0/com.example.notes/files/IMG_a.jpg"
        val internalUrl2 = "/data/user/0/com.example.notes/files/IMG_b.jpg"
        val content = listOf(
            ContentItem.Image(internalUrl1),
            ContentItem.Image(internalUrl2)
        )
        every { imageFileManager.isInternal(internalUrl1) } returns true
        every { imageFileManager.isInternal(internalUrl2) } returns true
        coJustRun { notesDao.addNoteWithContent(any(), any()) }

        repository.addNote(
            title = "All Internal Note",
            content = content,
            isPinned = false,
            updatedAt = 21_000L
        )

        coVerify(exactly = 0) { imageFileManager.copyImageToInternalStorage(any()) }
    }

    @Test
    fun `GIVEN isInternal check determines url type WHEN addNote processes content THEN isInternal is called for each image`() = runTest {
        val url1 = "content://media/external/images/1"
        val url2 = "content://media/external/images/2"
        val internalPath1 = "/data/user/0/com.example.notes/files/IMG_1.jpg"
        val internalPath2 = "/data/user/0/com.example.notes/files/IMG_2.jpg"
        val content = listOf(
            ContentItem.Image(url1),
            ContentItem.Image(url2)
        )
        every { imageFileManager.isInternal(url1) } returns false
        every { imageFileManager.isInternal(url2) } returns false
        coEvery { imageFileManager.copyImageToInternalStorage(url1) } returns internalPath1
        coEvery { imageFileManager.copyImageToInternalStorage(url2) } returns internalPath2
        coJustRun { notesDao.addNoteWithContent(any(), any()) }

        repository.addNote(
            title = "isInternal Check Note",
            content = content,
            isPinned = false,
            updatedAt = 22_000L
        )

        verify(exactly = 1) { imageFileManager.isInternal(url1) }
        verify(exactly = 1) { imageFileManager.isInternal(url2) }
    }

    @Test
    fun `GIVEN content has only text items WHEN addNote processes content THEN isInternal is never called`() = runTest {
        val content = listOf(
            ContentItem.Text("No images here"),
            ContentItem.Text("Still no images")
        )
        coJustRun { notesDao.addNoteWithContent(any(), any()) }

        repository.addNote(
            title = "Text Only Note",
            content = content,
            isPinned = false,
            updatedAt = 23_000L
        )

        verify(exactly = 0) { imageFileManager.isInternal(any()) }
        coVerify(exactly = 0) { imageFileManager.copyImageToInternalStorage(any()) }
    }

    // endregion
}
