package com.shieldcore.security.core.utils

import java.io.File
import java.util.*

object FileUtils {

    /**
     * Traverses directories to find junk files.
     */
    fun findJunkFiles(root: File, extensions: Set<String>): List<File> {
        val junkFiles = mutableListOf<File>()
        val stack = Stack<File>()
        stack.push(root)

        while (stack.isNotEmpty()) {
            val current = stack.pop()
            val files = current.listFiles() ?: continue
            for (file in files) {
                if (file.isDirectory) {
                    stack.push(file)
                } else if (extensions.contains(file.extension.lowercase())) {
                    junkFiles.add(file)
                }
            }
        }
        return junkFiles
    }

    fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0
        if (!file.isDirectory) return file.length()
        
        var size: Long = 0
        val files = file.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return size
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun deleteFileOrDirectory(file: File): Boolean {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteFileOrDirectory(child)
                }
            }
        }
        return file.delete()
    }
}
