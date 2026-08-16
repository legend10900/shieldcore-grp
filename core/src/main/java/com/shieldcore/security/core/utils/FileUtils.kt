package com.shieldcore.security.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.util.*

object FileUtils {

    /**
     * Traverses directories to find junk files with non-blocking coroutine yielding.
     */
    suspend fun findJunkFilesAsync(
        root: File,
        extensions: Set<String>,
        folderNames: Set<String> = emptySet(),
        maxDepth: Int = 8
    ): List<File> = withContext(Dispatchers.IO) {
        val junkFiles = mutableListOf<File>()
        if (!root.exists() || !root.canRead()) return@withContext junkFiles

        data class DirItem(val file: File, val depth: Int)
        val stack = ArrayDeque<DirItem>()
        stack.push(DirItem(root, 0))

        var scannedCount = 0

        while (stack.isNotEmpty()) {
            val (current, depth) = stack.pop()
            if (depth > maxDepth) continue

            val files = current.listFiles() ?: continue
            for (file in files) {
                scannedCount++
                if (scannedCount % 25 == 0) {
                    yield()
                }

                if (file.isDirectory) {
                    if (folderNames.contains(file.name.lowercase())) {
                        junkFiles.add(file)
                    } else if (!file.name.startsWith(".")) {
                        stack.push(DirItem(file, depth + 1))
                    }
                } else {
                    val ext = file.extension.lowercase()
                    if (extensions.contains(ext) || file.name.endsWith(".tmp") || file.name.endsWith(".log") || file.name.endsWith(".temp") || file.name.endsWith(".old") || file.name.endsWith(".bak")) {
                        junkFiles.add(file)
                    }
                }
            }
        }
        junkFiles
    }

    /**
     * Traverses directories to find junk files synchronously (legacy/compatibility).
     */
    fun findJunkFiles(root: File, extensions: Set<String>): List<File> {
        val junkFiles = mutableListOf<File>()
        if (!root.exists()) return junkFiles
        val stack = Stack<File>()
        stack.push(root)

        while (stack.isNotEmpty()) {
            val current = stack.pop()
            val files = current.listFiles() ?: continue
            for (file in files) {
                if (file.isDirectory) {
                    if (!file.name.startsWith(".")) {
                        stack.push(file)
                    }
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
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", size / Math.pow(1024.0, index.toDouble()), units[index])
    }

    fun deleteFileOrDirectory(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteFileOrDirectory(child)
                }
            }
        }
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun deleteAndCalculateFreedBytes(file: File): Long {
        if (!file.exists()) return 0L
        val size = if (file.isDirectory) getFolderSize(file) else file.length()
        return if (deleteFileOrDirectory(file)) size else 0L
    }
}
