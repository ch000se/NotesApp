package com.example.notes.presentation.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation.NavGraph
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.notes.presentation.screens.creation.CreateNoteScreen
import com.example.notes.presentation.screens.editing.EditNoteScreen
import com.example.notes.presentation.screens.notes.NotesScreen
import kotlinx.serialization.Serializable


// Jetpack Navigation 3

@Composable
fun NavGraph() {
    val backStack = rememberNavBackStack(Screen.Notes)

    NavDisplay(
        backStack = backStack,
        onBack = {
            backStack.removeLastOrNull()
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Screen.Notes> {
                NotesScreen(
                    onNoteClick = { note ->
                        backStack.addIfNotOnTop(Screen.EditNote(note.id))
                    },
                    onAddNoteClick = {
                        backStack.addIfNotOnTop(Screen.CreateNote)
                    }
                )
            }

            entry<Screen.CreateNote> {
                CreateNoteScreen(
                    onFinished = {
                        backStack.removeLastOrNull()
                    }
                )
            }

            entry<Screen.EditNote> { editNote ->
                EditNoteScreen(
                    noteId = editNote.noteId,
                    onFinished = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    )
}

fun <T : NavKey> MutableList<T>.addIfNotOnTop(screen: T) {
    if (lastOrNull() != screen) {
        add(screen)
    }
}

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Notes : Screen

    @Serializable
    data object CreateNote : Screen

    @Serializable
    data class EditNote(val noteId: Int) : Screen
}


/*

     || NAVIGATION2 WITH TYPE SAFETY ||

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Notes
    ) {
        composable<Screen.Notes> {
            NotesScreen(
                onNoteClick = { note ->
                    navController.navigate(Screen.EditNote(note.id))
                },
                onAddNoteClick = {
                    navController.navigate(Screen.CreateNote)
                }
            )
        }

        composable<Screen.CreateNote> {
            CreateNoteScreen(
                onFinished = {
                    navController.navigateUp()
                }
            )
        }

        composable<Screen.EditNote> { backStackEntry ->
            val editNote = backStackEntry.toRoute<Screen.EditNote>()

            EditNoteScreen(
                noteId = editNote.noteId,
                onFinished = {
                    navController.navigateUp()
                }
            )
        }
    }
}

@Serializable
sealed interface Screen {
    @Serializable
    data object Notes : Screen

    @Serializable
    data object CreateNote : Screen

    @Serializable
    data class EditNote(val noteId: Int) : Screen
}*/


/*

   || NAVIGATION2 WITHOUT TYPE SAFETY ||

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Notes.route
    ) {
        composable(Screen.Notes.route) {
            NotesScreen(
                onNoteClick = {
                    navController.navigate(Screen.EditNote.createRoute(it.id))
                },
                onAddNoteClick = {
                    navController.navigate(Screen.CreateNote.route)
                }
            )
        }
        composable(Screen.CreateNote.route) {
            CreateNoteScreen(
                onFinished = {
                    navController.navigateUp()
                }
            )
        }
        composable(Screen.EditNote.route) {
            val noteId = Screen.EditNote.getNoteId(it.arguments)

            EditNoteScreen(
                noteId = noteId,
                onFinished = {
                    navController.navigateUp()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    data object Notes : Screen("notes")
    data object CreateNote : Screen("create_note")
    data object EditNote : Screen("edit_note/{noteId}") { // Bundle("noteId" - "5")
        fun createRoute(noteId: Int): String { // edit_note/5
            return "edit_note/$noteId"
        }

        fun getNoteId(arguments: Bundle?): Int {
            return arguments?.getString("noteId")?.toInt() ?: 0
        }
    }
}*/
