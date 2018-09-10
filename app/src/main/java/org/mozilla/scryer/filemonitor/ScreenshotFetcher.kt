package org.mozilla.scryer.filemonitor

import android.content.Context
import android.provider.MediaStore
import org.mozilla.scryer.persistence.CollectionModel
import org.mozilla.scryer.persistence.ScreenshotModel
import java.io.File

class ScreenshotFetcher {

    fun fetchScreenshots(context: Context): List<ScreenshotModel> {
        val folders = getFolders(context)
        val screenshots = mutableListOf<ScreenshotModel>()
        folders.forEach { folderPath ->
            screenshots.addAll(fetchScreenshots(folderPath))
        }
        return screenshots
    }

    private fun getFolders(context: Context): List<String> {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val columns = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
        val selection = "${MediaStore.Images.ImageColumns.BUCKET_ID} IS NOT NULL) GROUP BY (${MediaStore.Images.ImageColumns.BUCKET_ID}"
        val results = mutableListOf<String>()

        val cursor = context.contentResolver.query(uri, columns, selection, null, null)
        cursor.use {
            while (cursor.moveToNext()) {
                val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)).trim()
                if (path.contains("screenshot", true)) {
                    val folder = File(path).parent?.trimEnd(File.separatorChar) ?: continue
                    results.add(folder)
                }
            }
        }

        return results
    }

    private fun fetchScreenshots(dirPath: String): List<ScreenshotModel> {
        val results = mutableListOf<ScreenshotModel>()

        File(dirPath).listFiles()?.let { files ->
            for (file in files) {
                val model = ScreenshotModel(file.absolutePath, file.lastModified(), CollectionModel.UNCATEGORIZED)
                results.add(model)
            }
        }
        return results
    }
}