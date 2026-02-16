package com.example.notes.data

import com.example.notes.domain.ContentItem
import com.example.notes.domain.Note
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MapperTest {

    // region Note.toDbModel()

    @Test
    fun `GIVEN note with all fields WHEN toDbModel is called THEN returns NoteDbModel with same field values`() {
        val expectedId = 7
        val expectedTitle = "Shopping List"
        val expectedUpdatedAt = 1_700_000_000_000L
        val expectedIsPinned = false
        val note = Note(
            id = expectedId,
            title = expectedTitle,
            content = emptyList(),
            updatedAt = expectedUpdatedAt,
            isPinned = expectedIsPinned
        )

        val result = note.toDbModel()

        assertThat(result.id).isEqualTo(expectedId)
        assertThat(result.title).isEqualTo(expectedTitle)
        assertThat(result.updatedAt).isEqualTo(expectedUpdatedAt)
        assertThat(result.isPinned).isEqualTo(expectedIsPinned)
    }

    @Test
    fun `GIVEN pinned note WHEN toDbModel is called THEN isPinned is true in NoteDbModel`() {
        val note = Note(
            id = 1,
            title = "Pinned",
            content = emptyList(),
            updatedAt = 1000L,
            isPinned = true
        )

        val result = note.toDbModel()

        assertThat(result.isPinned).isTrue()
    }

    @Test
    fun `GIVEN unpinned note WHEN toDbModel is called THEN isPinned is false in NoteDbModel`() {
        val note = Note(
            id = 2,
            title = "Not Pinned",
            content = emptyList(),
            updatedAt = 2000L,
            isPinned = false
        )

        val result = note.toDbModel()

        assertThat(result.isPinned).isFalse()
    }

    @Test
    fun `GIVEN note with empty title WHEN toDbModel is called THEN NoteDbModel title is empty string`() {
        val note = Note(
            id = 3,
            title = "",
            content = emptyList(),
            updatedAt = 3000L,
            isPinned = false
        )

        val result = note.toDbModel()

        assertThat(result.title).isEmpty()
    }

    @Test
    fun `GIVEN note with special characters in title WHEN toDbModel is called THEN title is preserved exactly`() {
        val expectedTitle = "Note #1 — with émojis 🎉 & <symbols>"
        val note = Note(
            id = 4,
            title = expectedTitle,
            content = emptyList(),
            updatedAt = 4000L,
            isPinned = false
        )

        val result = note.toDbModel()

        assertThat(result.title).isEqualTo(expectedTitle)
    }

    @Test
    fun `GIVEN note with zero id WHEN toDbModel is called THEN NoteDbModel id is zero`() {
        val note = Note(
            id = 0,
            title = "Zero ID",
            content = emptyList(),
            updatedAt = 5000L,
            isPinned = false
        )

        val result = note.toDbModel()

        assertThat(result.id).isEqualTo(0)
    }

    @Test
    fun `GIVEN note with content items WHEN toDbModel is called THEN content is not included in NoteDbModel`() {
        val note = Note(
            id = 5,
            title = "Has Content",
            content = listOf(
                ContentItem.Text("Hello"),
                ContentItem.Image("https://example.com/img.png")
            ),
            updatedAt = 6000L,
            isPinned = false
        )

        val result = note.toDbModel()

        assertThat(result).isInstanceOf(NoteDbModel::class.java)
        assertThat(result.id).isEqualTo(5)
    }

    // endregion

    // region List<ContentItem>.toContentItemDbModels(noteId)

    @Test
    fun `GIVEN empty content list WHEN toContentItemDbModels is called THEN returns empty list`() {
        val contentItems = emptyList<ContentItem>()

        val result = contentItems.toContentItemDbModels(noteId = 1)

        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN single Text item WHEN toContentItemDbModels is called THEN returns ContentItemDbModel with TEXT type`() {
        val expectedContent = "Hello World"
        val expectedNoteId = 10
        val contentItems = listOf(ContentItem.Text(expectedContent))

        val result = contentItems.toContentItemDbModels(noteId = expectedNoteId)

        assertThat(result).hasSize(1)
        assertThat(result[0].noteId).isEqualTo(expectedNoteId)
        assertThat(result[0].contentType).isEqualTo(ContentType.TEXT)
        assertThat(result[0].content).isEqualTo(expectedContent)
        assertThat(result[0].order).isEqualTo(0)
    }

    @Test
    fun `GIVEN single Image item WHEN toContentItemDbModels is called THEN returns ContentItemDbModel with IMAGE type`() {
        val expectedUrl = "https://example.com/photo.jpg"
        val expectedNoteId = 20
        val contentItems = listOf(ContentItem.Image(expectedUrl))

        val result = contentItems.toContentItemDbModels(noteId = expectedNoteId)

        assertThat(result).hasSize(1)
        assertThat(result[0].noteId).isEqualTo(expectedNoteId)
        assertThat(result[0].contentType).isEqualTo(ContentType.IMAGE)
        assertThat(result[0].content).isEqualTo(expectedUrl)
        assertThat(result[0].order).isEqualTo(0)
    }

    @Test
    fun `GIVEN multiple content items WHEN toContentItemDbModels is called THEN order values reflect original list indices`() {
        val contentItems = listOf(
            ContentItem.Text("First"),
            ContentItem.Image("https://example.com/img.png"),
            ContentItem.Text("Third")
        )

        val result = contentItems.toContentItemDbModels(noteId = 5)

        assertThat(result[0].order).isEqualTo(0)
        assertThat(result[1].order).isEqualTo(1)
        assertThat(result[2].order).isEqualTo(2)
    }

    @Test
    fun `GIVEN mixed content list WHEN toContentItemDbModels is called THEN each item has correct content type`() {
        val contentItems = listOf(
            ContentItem.Text("Paragraph"),
            ContentItem.Image("https://example.com/img.png"),
            ContentItem.Text("Another paragraph"),
            ContentItem.Image("https://example.com/img2.png")
        )

        val result = contentItems.toContentItemDbModels(noteId = 3)

        assertThat(result[0].contentType).isEqualTo(ContentType.TEXT)
        assertThat(result[1].contentType).isEqualTo(ContentType.IMAGE)
        assertThat(result[2].contentType).isEqualTo(ContentType.TEXT)
        assertThat(result[3].contentType).isEqualTo(ContentType.IMAGE)
    }

    @Test
    fun `GIVEN content list WHEN toContentItemDbModels is called THEN all items share the provided noteId`() {
        val expectedNoteId = 42
        val contentItems = listOf(
            ContentItem.Text("Text"),
            ContentItem.Image("https://example.com/img.png"),
            ContentItem.Text("More text")
        )

        val result = contentItems.toContentItemDbModels(noteId = expectedNoteId)

        assertThat(result.all { it.noteId == expectedNoteId }).isTrue()
    }

    @Test
    fun `GIVEN content list WHEN toContentItemDbModels is called THEN output size equals input size`() {
        val contentItems = listOf(
            ContentItem.Text("A"),
            ContentItem.Text("B"),
            ContentItem.Image("https://example.com/c.png")
        )

        val result = contentItems.toContentItemDbModels(noteId = 1)

        assertThat(result).hasSize(contentItems.size)
    }

    @Test
    fun `GIVEN Text item with blank content WHEN toContentItemDbModels is called THEN content field is preserved as blank`() {
        val contentItems = listOf(ContentItem.Text("   "))

        val result = contentItems.toContentItemDbModels(noteId = 1)

        assertThat(result[0].content).isEqualTo("   ")
        assertThat(result[0].contentType).isEqualTo(ContentType.TEXT)
    }

    @Test
    fun `GIVEN Text item with special characters WHEN toContentItemDbModels is called THEN content is preserved exactly`() {
        val specialContent = "Line1\nLine2\tTabbed & <escaped> — em dash"
        val contentItems = listOf(ContentItem.Text(specialContent))

        val result = contentItems.toContentItemDbModels(noteId = 1)

        assertThat(result[0].content).isEqualTo(specialContent)
    }

    @Test
    fun `GIVEN Image item with complex url WHEN toContentItemDbModels is called THEN url is stored in content field`() {
        val complexUrl = "https://example.com/path/to/image.png?size=large&v=2#anchor"
        val contentItems = listOf(ContentItem.Image(complexUrl))

        val result = contentItems.toContentItemDbModels(noteId = 1)

        assertThat(result[0].content).isEqualTo(complexUrl)
        assertThat(result[0].contentType).isEqualTo(ContentType.IMAGE)
    }

    @Test
    fun `GIVEN only Image items WHEN toContentItemDbModels is called THEN all results have IMAGE content type`() {
        val contentItems = listOf(
            ContentItem.Image("https://example.com/a.png"),
            ContentItem.Image("https://example.com/b.png"),
            ContentItem.Image("https://example.com/c.png")
        )

        val result = contentItems.toContentItemDbModels(noteId = 1)

        assertThat(result.all { it.contentType == ContentType.IMAGE }).isTrue()
    }

    @Test
    fun `GIVEN only Text items WHEN toContentItemDbModels is called THEN all results have TEXT content type`() {
        val contentItems = listOf(
            ContentItem.Text("Alpha"),
            ContentItem.Text("Beta"),
            ContentItem.Text("Gamma")
        )

        val result = contentItems.toContentItemDbModels(noteId = 1)

        assertThat(result.all { it.contentType == ContentType.TEXT }).isTrue()
    }

    // endregion

    // region List<ContentItemDbModel>.toContentItems()

    @Test
    fun `GIVEN empty ContentItemDbModel list WHEN toContentItems is called THEN returns empty list`() {
        val dbModels = emptyList<ContentItemDbModel>()

        val result = dbModels.toContentItems()

        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN single TEXT ContentItemDbModel WHEN toContentItems is called THEN returns ContentItem Text with correct content`() {
        val expectedContent = "Hello from DB"
        val dbModels = listOf(
            ContentItemDbModel(
                noteId = 1,
                contentType = ContentType.TEXT,
                content = expectedContent,
                order = 0
            )
        )

        val result = dbModels.toContentItems()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result[0] as ContentItem.Text).content).isEqualTo(expectedContent)
    }

    @Test
    fun `GIVEN single IMAGE ContentItemDbModel WHEN toContentItems is called THEN returns ContentItem Image with correct url`() {
        val expectedUrl = "https://example.com/stored-image.jpg"
        val dbModels = listOf(
            ContentItemDbModel(
                noteId = 1,
                contentType = ContentType.IMAGE,
                content = expectedUrl,
                order = 0
            )
        )

        val result = dbModels.toContentItems()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isInstanceOf(ContentItem.Image::class.java)
        assertThat((result[0] as ContentItem.Image).url).isEqualTo(expectedUrl)
    }

    @Test
    fun `GIVEN mixed ContentItemDbModel list WHEN toContentItems is called THEN each item maps to the correct ContentItem subtype`() {
        val dbModels = listOf(
            ContentItemDbModel(noteId = 1, contentType = ContentType.TEXT, content = "First", order = 0),
            ContentItemDbModel(noteId = 1, contentType = ContentType.IMAGE, content = "https://example.com/img.png", order = 1),
            ContentItemDbModel(noteId = 1, contentType = ContentType.TEXT, content = "Third", order = 2)
        )

        val result = dbModels.toContentItems()

        assertThat(result).hasSize(3)
        assertThat(result[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat(result[1]).isInstanceOf(ContentItem.Image::class.java)
        assertThat(result[2]).isInstanceOf(ContentItem.Text::class.java)
    }

    @Test
    fun `GIVEN ContentItemDbModel list WHEN toContentItems is called THEN output preserves input list order`() {
        val dbModels = listOf(
            ContentItemDbModel(noteId = 1, contentType = ContentType.TEXT, content = "Alpha", order = 0),
            ContentItemDbModel(noteId = 1, contentType = ContentType.IMAGE, content = "https://example.com/beta.png", order = 1),
            ContentItemDbModel(noteId = 1, contentType = ContentType.TEXT, content = "Gamma", order = 2),
            ContentItemDbModel(noteId = 1, contentType = ContentType.IMAGE, content = "https://example.com/delta.png", order = 3)
        )

        val result = dbModels.toContentItems()

        assertThat((result[0] as ContentItem.Text).content).isEqualTo("Alpha")
        assertThat((result[1] as ContentItem.Image).url).isEqualTo("https://example.com/beta.png")
        assertThat((result[2] as ContentItem.Text).content).isEqualTo("Gamma")
        assertThat((result[3] as ContentItem.Image).url).isEqualTo("https://example.com/delta.png")
    }

    @Test
    fun `GIVEN ContentItemDbModel with blank text content WHEN toContentItems is called THEN blank content is preserved`() {
        val dbModels = listOf(
            ContentItemDbModel(noteId = 1, contentType = ContentType.TEXT, content = "   ", order = 0)
        )

        val result = dbModels.toContentItems()

        assertThat((result[0] as ContentItem.Text).content).isEqualTo("   ")
    }

    @Test
    fun `GIVEN ContentItemDbModel list WHEN toContentItems is called THEN output size equals input size`() {
        val dbModels = listOf(
            ContentItemDbModel(noteId = 1, contentType = ContentType.TEXT, content = "One", order = 0),
            ContentItemDbModel(noteId = 1, contentType = ContentType.TEXT, content = "Two", order = 1),
            ContentItemDbModel(noteId = 1, contentType = ContentType.IMAGE, content = "https://example.com/three.png", order = 2)
        )

        val result = dbModels.toContentItems()

        assertThat(result).hasSize(dbModels.size)
    }

    // endregion

    // region NoteWithContentDbModel.toEntity()

    @Test
    fun `GIVEN NoteWithContentDbModel with all fields WHEN toEntity is called THEN Note has same field values`() {
        val expectedId = 99
        val expectedTitle = "Full Note"
        val expectedUpdatedAt = 9_999_999L
        val expectedIsPinned = true
        val noteDbModel = NoteDbModel(
            id = expectedId,
            title = expectedTitle,
            updatedAt = expectedUpdatedAt,
            isPinned = expectedIsPinned
        )
        val noteWithContent = NoteWithContentDbModel(
            noteDbModel = noteDbModel,
            content = emptyList()
        )

        val result = noteWithContent.toEntity()

        assertThat(result.id).isEqualTo(expectedId)
        assertThat(result.title).isEqualTo(expectedTitle)
        assertThat(result.updatedAt).isEqualTo(expectedUpdatedAt)
        assertThat(result.isPinned).isEqualTo(expectedIsPinned)
    }

    @Test
    fun `GIVEN NoteWithContentDbModel with empty content WHEN toEntity is called THEN Note has empty content list`() {
        val noteDbModel = NoteDbModel(id = 1, title = "Empty", updatedAt = 1000L, isPinned = false)
        val noteWithContent = NoteWithContentDbModel(noteDbModel = noteDbModel, content = emptyList())

        val result = noteWithContent.toEntity()

        assertThat(result.content).isEmpty()
    }

    @Test
    fun `GIVEN NoteWithContentDbModel with Text content WHEN toEntity is called THEN Note content contains ContentItem Text`() {
        val expectedText = "Body of the note"
        val noteDbModel = NoteDbModel(id = 2, title = "Text Note", updatedAt = 2000L, isPinned = false)
        val noteWithContent = NoteWithContentDbModel(
            noteDbModel = noteDbModel,
            content = listOf(
                ContentItemDbModel(noteId = 2, contentType = ContentType.TEXT, content = expectedText, order = 0)
            )
        )

        val result = noteWithContent.toEntity()

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result.content[0] as ContentItem.Text).content).isEqualTo(expectedText)
    }

    @Test
    fun `GIVEN NoteWithContentDbModel with Image content WHEN toEntity is called THEN Note content contains ContentItem Image`() {
        val expectedUrl = "https://example.com/cover.png"
        val noteDbModel = NoteDbModel(id = 3, title = "Image Note", updatedAt = 3000L, isPinned = false)
        val noteWithContent = NoteWithContentDbModel(
            noteDbModel = noteDbModel,
            content = listOf(
                ContentItemDbModel(noteId = 3, contentType = ContentType.IMAGE, content = expectedUrl, order = 0)
            )
        )

        val result = noteWithContent.toEntity()

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0]).isInstanceOf(ContentItem.Image::class.java)
        assertThat((result.content[0] as ContentItem.Image).url).isEqualTo(expectedUrl)
    }

    @Test
    fun `GIVEN NoteWithContentDbModel with mixed content WHEN toEntity is called THEN Note content order is preserved`() {
        val noteDbModel = NoteDbModel(id = 4, title = "Mixed", updatedAt = 4000L, isPinned = false)
        val noteWithContent = NoteWithContentDbModel(
            noteDbModel = noteDbModel,
            content = listOf(
                ContentItemDbModel(noteId = 4, contentType = ContentType.TEXT, content = "Intro", order = 0),
                ContentItemDbModel(noteId = 4, contentType = ContentType.IMAGE, content = "https://example.com/img.png", order = 1),
                ContentItemDbModel(noteId = 4, contentType = ContentType.TEXT, content = "Outro", order = 2)
            )
        )

        val result = noteWithContent.toEntity()

        assertThat(result.content).hasSize(3)
        assertThat(result.content[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result.content[0] as ContentItem.Text).content).isEqualTo("Intro")
        assertThat(result.content[1]).isInstanceOf(ContentItem.Image::class.java)
        assertThat((result.content[1] as ContentItem.Image).url).isEqualTo("https://example.com/img.png")
        assertThat(result.content[2]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result.content[2] as ContentItem.Text).content).isEqualTo("Outro")
    }

    @Test
    fun `GIVEN NoteWithContentDbModel with isPinned true WHEN toEntity is called THEN Note isPinned is true`() {
        val noteDbModel = NoteDbModel(id = 5, title = "Pinned", updatedAt = 5000L, isPinned = true)
        val noteWithContent = NoteWithContentDbModel(noteDbModel = noteDbModel, content = emptyList())

        val result = noteWithContent.toEntity()

        assertThat(result.isPinned).isTrue()
    }

    @Test
    fun `GIVEN NoteWithContentDbModel with isPinned false WHEN toEntity is called THEN Note isPinned is false`() {
        val noteDbModel = NoteDbModel(id = 6, title = "Not Pinned", updatedAt = 6000L, isPinned = false)
        val noteWithContent = NoteWithContentDbModel(noteDbModel = noteDbModel, content = emptyList())

        val result = noteWithContent.toEntity()

        assertThat(result.isPinned).isFalse()
    }

    @Test
    fun `GIVEN NoteWithContentDbModel with special characters in title WHEN toEntity is called THEN title is preserved exactly`() {
        val expectedTitle = "Títlé wîth ünïcödé & <html> — em dash"
        val noteDbModel = NoteDbModel(id = 7, title = expectedTitle, updatedAt = 7000L, isPinned = false)
        val noteWithContent = NoteWithContentDbModel(noteDbModel = noteDbModel, content = emptyList())

        val result = noteWithContent.toEntity()

        assertThat(result.title).isEqualTo(expectedTitle)
    }

    // endregion

    // region List<NoteWithContentDbModel>.toEntityList()

    @Test
    fun `GIVEN empty NoteWithContentDbModel list WHEN toEntityList is called THEN returns empty list`() {
        val dbModels = emptyList<NoteWithContentDbModel>()

        val result = dbModels.toEntityList()

        assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN single NoteWithContentDbModel WHEN toEntityList is called THEN returns list with one Note`() {
        val noteDbModel = NoteDbModel(id = 1, title = "Solo", updatedAt = 1000L, isPinned = false)
        val dbModels = listOf(
            NoteWithContentDbModel(noteDbModel = noteDbModel, content = emptyList())
        )

        val result = dbModels.toEntityList()

        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo(1)
        assertThat(result[0].title).isEqualTo("Solo")
    }

    @Test
    fun `GIVEN multiple NoteWithContentDbModels WHEN toEntityList is called THEN returns Note list of same size`() {
        val dbModels = listOf(
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 1, title = "Note 1", updatedAt = 1000L, isPinned = false),
                content = emptyList()
            ),
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 2, title = "Note 2", updatedAt = 2000L, isPinned = true),
                content = emptyList()
            ),
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 3, title = "Note 3", updatedAt = 3000L, isPinned = false),
                content = emptyList()
            )
        )

        val result = dbModels.toEntityList()

        assertThat(result).hasSize(3)
    }

    @Test
    fun `GIVEN multiple NoteWithContentDbModels WHEN toEntityList is called THEN list order is preserved`() {
        val dbModels = listOf(
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 10, title = "First", updatedAt = 1000L, isPinned = false),
                content = emptyList()
            ),
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 20, title = "Second", updatedAt = 2000L, isPinned = false),
                content = emptyList()
            ),
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 30, title = "Third", updatedAt = 3000L, isPinned = false),
                content = emptyList()
            )
        )

        val result = dbModels.toEntityList()

        assertThat(result[0].id).isEqualTo(10)
        assertThat(result[1].id).isEqualTo(20)
        assertThat(result[2].id).isEqualTo(30)
    }

    @Test
    fun `GIVEN NoteWithContentDbModels with mixed pinned flags WHEN toEntityList is called THEN each Note isPinned flag is correct`() {
        val dbModels = listOf(
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 1, title = "Pinned", updatedAt = 1000L, isPinned = true),
                content = emptyList()
            ),
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 2, title = "Not Pinned", updatedAt = 2000L, isPinned = false),
                content = emptyList()
            )
        )

        val result = dbModels.toEntityList()

        assertThat(result[0].isPinned).isTrue()
        assertThat(result[1].isPinned).isFalse()
    }

    @Test
    fun `GIVEN NoteWithContentDbModels each with content WHEN toEntityList is called THEN each Note has its own content items`() {
        val dbModels = listOf(
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 1, title = "Note A", updatedAt = 1000L, isPinned = false),
                content = listOf(
                    ContentItemDbModel(noteId = 1, contentType = ContentType.TEXT, content = "Content A", order = 0)
                )
            ),
            NoteWithContentDbModel(
                noteDbModel = NoteDbModel(id = 2, title = "Note B", updatedAt = 2000L, isPinned = false),
                content = listOf(
                    ContentItemDbModel(noteId = 2, contentType = ContentType.IMAGE, content = "https://example.com/b.png", order = 0),
                    ContentItemDbModel(noteId = 2, contentType = ContentType.TEXT, content = "Caption B", order = 1)
                )
            )
        )

        val result = dbModels.toEntityList()

        assertThat(result[0].content).hasSize(1)
        assertThat(result[0].content[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result[0].content[0] as ContentItem.Text).content).isEqualTo("Content A")
        assertThat(result[1].content).hasSize(2)
        assertThat(result[1].content[0]).isInstanceOf(ContentItem.Image::class.java)
        assertThat(result[1].content[1]).isInstanceOf(ContentItem.Text::class.java)
    }

    // endregion

    // region Round-trip conversions

    @Test
    fun `GIVEN Note WHEN converted to NoteDbModel and back via NoteWithContentDbModel THEN all fields are preserved`() {
        val originalNote = Note(
            id = 55,
            title = "Round Trip",
            content = listOf(
                ContentItem.Text("First paragraph"),
                ContentItem.Image("https://example.com/img.png"),
                ContentItem.Text("Last paragraph")
            ),
            updatedAt = 12_345_678L,
            isPinned = true
        )

        val noteDbModel = originalNote.toDbModel()
        val contentDbModels = originalNote.content.toContentItemDbModels(noteId = originalNote.id)
        val noteWithContent = NoteWithContentDbModel(noteDbModel = noteDbModel, content = contentDbModels)
        val result = noteWithContent.toEntity()

        assertThat(result.id).isEqualTo(originalNote.id)
        assertThat(result.title).isEqualTo(originalNote.title)
        assertThat(result.updatedAt).isEqualTo(originalNote.updatedAt)
        assertThat(result.isPinned).isEqualTo(originalNote.isPinned)
        assertThat(result.content).hasSize(originalNote.content.size)
    }

    @Test
    fun `GIVEN content items WHEN converted to db models and back THEN all items are preserved with correct types and values`() {
        val originalContent = listOf(
            ContentItem.Text("Opening"),
            ContentItem.Image("https://example.com/middle.png"),
            ContentItem.Text("Closing")
        )

        val dbModels = originalContent.toContentItemDbModels(noteId = 1)
        val result = dbModels.toContentItems()

        assertThat(result).hasSize(originalContent.size)
        assertThat(result[0]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result[0] as ContentItem.Text).content).isEqualTo("Opening")
        assertThat(result[1]).isInstanceOf(ContentItem.Image::class.java)
        assertThat((result[1] as ContentItem.Image).url).isEqualTo("https://example.com/middle.png")
        assertThat(result[2]).isInstanceOf(ContentItem.Text::class.java)
        assertThat((result[2] as ContentItem.Text).content).isEqualTo("Closing")
    }

    @Test
    fun `GIVEN empty content list WHEN converted to db models and back THEN result is still empty`() {
        val originalContent = emptyList<ContentItem>()

        val dbModels = originalContent.toContentItemDbModels(noteId = 1)
        val result = dbModels.toContentItems()

        assertThat(result).isEmpty()
    }

    // endregion
}
