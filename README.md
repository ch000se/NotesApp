# Notes

A modern Android note-taking application built with Jetpack Compose and Clean Architecture.

![Android](https://img.shields.io/badge/Android-24+-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024-purple.svg)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20Architecture-orange.svg)

## Features

- **Create & Edit Notes** - Rich content with text and images
- **Image Support** - Add photos from gallery with automatic storage management
- **Pin Notes** - Keep important notes at the top (long press to pin/unpin)
- **Search** - Real-time search through note titles and content
- **Modern UI** - Material Design 3 with custom brown color palette
- **Offline First** - All data stored locally with Room database

## Screenshots

<!-- Add your screenshots here -->
| Notes List | Create Note | Edit Note |
|------------|-------------|-----------|
| Screenshot | Screenshot  | Screenshot |

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Kotlin** | 2.0 | Programming Language |
| **Jetpack Compose** | BOM 2024 | Modern UI Toolkit |
| **Material 3** | Latest | Design System |
| **Room** | 2.6+ | Local Database |
| **Hilt** | 2.51+ | Dependency Injection |
| **Navigation 3** | 1.0.0 | Type-safe Navigation |
| **Coil** | 3.0+ | Image Loading |
| **Kotlin Coroutines** | Latest | Asynchronous Programming |
| **Kotlin Serialization** | 1.6+ | Data Serialization |

## Architecture

The app follows **Clean Architecture** principles with clear separation of concerns:

```
com.example.notes/
├── domain/           # Business logic layer
│   ├── Note.kt                    # Note entity
│   ├── ContentItem.kt             # Sealed interface (Text/Image)
│   ├── NotesRepository.kt         # Repository interface
│   └── usecases/
│       ├── AddNoteUseCase.kt
│       ├── EditNoteUseCase.kt
│       ├── DeleteNoteUseCase.kt
│       ├── GetNoteUseCase.kt
│       ├── GetAllNotesUseCase.kt
│       ├── SearchNotesUseCase.kt
│       └── SwitchPinnedStatusUseCase.kt
│
├── data/             # Data layer
│   ├── NotesDatabase.kt           # Room database
│   ├── NotesDao.kt                # Data Access Object
│   ├── NotesRepositoryImpl.kt     # Repository implementation
│   ├── models/
│   │   ├── NoteDbModel.kt
│   │   ├── ContentItemDbModel.kt
│   │   └── NoteWithContentDbModel.kt
│   ├── Mapper.kt                  # Domain <-> Data mapping
│   └── ImageFileManager.kt        # Image storage management
│
├── presentation/     # UI layer
│   ├── screens/
│   │   ├── notes/                 # Main notes list
│   │   │   ├── NotesScreen.kt
│   │   │   ├── NotesViewModel.kt
│   │   │   ├── NotesState.kt
│   │   │   └── NotesCommand.kt
│   │   ├── creation/              # Create note
│   │   │   ├── CreateNoteScreen.kt
│   │   │   └── CreateNoteViewModel.kt
│   │   └── editing/               # Edit note
│   │       ├── EditNoteScreen.kt
│   │       └── EditNoteViewModel.kt
│   ├── components/
│   │   ├── Content.kt
│   │   ├── TextContent.kt
│   │   ├── ImageContent.kt
│   │   └── ImageGroup.kt
│   ├── navigation/
│   │   └── NavGraph.kt            # Navigation 3 setup
│   ├── ui/theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   └── utils/
│       └── DateFormatter.kt
│
├── di/               # Dependency Injection
│   └── AppModule.kt               # Hilt module
│
└── MyApp.kt          # Application class
```

## Design Patterns

- **MVVM** - ViewModel manages UI state with StateFlow
- **MVI** - Unidirectional data flow with Commands/Events
- **Repository Pattern** - Abstraction over data sources
- **Use Cases** - Single responsibility business logic
- **Dependency Injection** - Hilt for loose coupling

## Database Schema

```sql
-- Notes table
CREATE TABLE notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    updatedAt INTEGER NOT NULL,
    isPinned INTEGER NOT NULL DEFAULT 0
);

-- Content items table (One-to-Many relationship)
CREATE TABLE content_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    noteId INTEGER NOT NULL,
    type TEXT NOT NULL,        -- "text" or "image"
    text TEXT,
    imageUrl TEXT,
    FOREIGN KEY (noteId) REFERENCES notes(id) ON DELETE CASCADE
);
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34+

### Installation

1. Clone the repository
```bash
git clone https://github.com/yourusername/notes.git
```

2. Open in Android Studio

3. Sync Gradle files

4. Run on emulator or device (API 24+)

### Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Run lint
./gradlew lint
```

## Project Configuration

| Property | Value |
|----------|-------|
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 35 |
| Java Version | 11 |
| Kotlin Version | 2.0 |

## State Management

The app uses a combination of **StateFlow** and **Channel** for state management:

```kotlin
// ViewModel
class NotesViewModel : ViewModel() {
    private val _state = MutableStateFlow(NotesState())
    val state: StateFlow<NotesState> = _state.asStateFlow()

    private val _event = Channel<NotesEvent>()
    val event = _event.receiveAsFlow()

    fun processCommand(command: NotesCommand) {
        when (command) {
            is NotesCommand.Search -> searchNotes(command.query)
            // ...
        }
    }
}

// Screen
@Composable
fun NotesScreen(viewModel: NotesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ...
}
```

## Navigation

Using **Jetpack Navigation 3** with type-safe routes:

```kotlin
@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Notes : Screen

    @Serializable
    data object CreateNote : Screen

    @Serializable
    data class EditNote(val noteId: Int) : Screen
}
```

## Localization

The app supports multiple languages:
- English (default)
- Ukrainian (uk)

To add a new language, create `values-{language_code}/strings.xml`.

## Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Coil](https://coil-kt.github.io/coil/)
- [Room](https://developer.android.com/training/data-storage/room)
- [Hilt](https://dagger.dev/hilt/)

---

Made with Kotlin and Jetpack Compose