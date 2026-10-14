package io.github.deveshb1.peptideessentials.ui.nav

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.AppRepository
import io.github.deveshb1.peptideessentials.repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/** A tiny back stack of string routes, see [Route]. */
class Navigator(private val read: () -> List<String>, private val write: (List<String>) -> Unit) {
    val stack: List<String> get() = read()
    fun push(route: String) = write(read() + route)
    fun pop() = write(read().dropLast(1))

    /** Replaces the current screen, e.g. after saving a new item. */
    fun replace(route: String) = write(read().dropLast(1) + route)
}

/** Repository writes plus snackbar feedback, scoped to the current composition. */
class Actions(
    val repository: AppRepository,
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
) {
    fun run(block: suspend AppRepository.() -> Unit) {
        scope.launch { repository.block() }
    }

    fun message(text: String) {
        scope.launch { snackbar.showSnackbar(text) }
    }

    /** Shows [text] with an undo action and runs [onUndo] if it's tapped. */
    fun undoable(text: String, undoLabel: String, onUndo: suspend AppRepository.() -> Unit) {
        scope.launch {
            val result = snackbar.showSnackbar(text, actionLabel = undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) repository.onUndo()
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator not provided") }
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("Snackbar not provided") }
val LocalAppData = compositionLocalOf { AppData() }
val LocalNow = compositionLocalOf<ZonedDateTime> { ZonedDateTime.now() }

@Composable
fun rememberActions(): Actions {
    val repository = LocalContext.current.repository
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    return remember(repository, scope, snackbar) { Actions(repository, scope, snackbar) }
}
