package com.example.lkmagneticrobot.util

import java.io.File
import java.util.*

object FileGet {
    fun listFileSortByModifyTime(path: String): MutableList<File> {
        val list: MutableList<File> = getFiles(path, mutableListOf())
        if (list != null && list.size > 0) {
            list.sortWith(Comparator { file, newFile ->
                if (file?.lastModified()!! < newFile?.lastModified()!!) {
                    -1
                } else if (file?.lastModified() == newFile?.lastModified()) {
                    0
                } else {
                    1
                }
            })
        }
        return list
    }

    fun getFiles(realpath: String, files: MutableList<File>): MutableList<File> {
        val realFile = File(realpath)
        if (realFile.isDirectory) {
            val subfiles = realFile.listFiles()
            for (file in subfiles) {
                if (file.isDirectory) {
                    getFiles(file.absolutePath, files)
                } else {
                    files.add(file)
                }
            }
        }
        return files
    }
}