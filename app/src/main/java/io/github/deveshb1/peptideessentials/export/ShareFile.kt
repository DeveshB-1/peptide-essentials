package io.github.deveshb1.peptideessentials.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes files into the cache export folder and hands them to the share sheet. */
object ShareFile {
    fun exportDir(context: Context): File = File(context.cacheDir, "exports").apply { mkdirs() }

    /** Clears earlier exports so shared files don't pile up in the cache. */
    fun newFile(context: Context, name: String): File {
        val dir = exportDir(context)
        dir.listFiles()?.forEach { it.delete() }
        return File(dir, name)
    }

    /** @throws android.content.ActivityNotFoundException if nothing can receive the file. */
    fun share(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(file.name, uri)
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
