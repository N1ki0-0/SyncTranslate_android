package com.example.synctranslate.AI.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AssetCopier {

    fun copyAssetFolder(context: Context, assetPath: String, targetDir: File) {
        val assetManager = context.assets
        val files = assetManager.list(assetPath) ?: return

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        for (filename in files) {
            val inStream = assetManager.open("$assetPath/$filename")
            val outFile = File(targetDir, filename)
            val outStream = FileOutputStream(outFile)

            val buffer = ByteArray(1024)
            var read: Int
            while (inStream.read(buffer).also { read = it } != -1) {
                outStream.write(buffer, 0, read)
            }

            inStream.close()
            outStream.flush()
            outStream.close()
        }
    }
}