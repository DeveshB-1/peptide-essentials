package io.github.deveshb1.doselog.export

import android.content.Context
import android.net.Uri
import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.JsonCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** JSON backup and restore through Storage Access Framework URIs chosen by the user. */
object Backup {
    suspend fun write(context: Context, uri: Uri, data: AppData) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
        stream.use { it.write(JsonCodec.encode(data).encodeToByteArray()) }
    }

    /** @throws Exception if the file can't be read or isn't a Dose Log backup. */
    suspend fun read(context: Context, uri: Uri): AppData = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        val text = stream.use { it.readBytes().decodeToString() }
        JsonCodec.decode(text)
    }
}
