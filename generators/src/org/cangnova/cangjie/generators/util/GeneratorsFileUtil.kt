/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.cangnova.cangjie.generators.util

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import java.io.File
import java.io.IOException
import kotlin.io.path.*

/**
 * 生成器文件 I/O 工具。
 */
object GeneratorsFileUtil {
    /**
     * 当前进程是否运行在 TeamCity 构建环境中。
     */
    private val isTeamCityBuild: Boolean =
        System.getProperty("teamcity", "false").toBoolean() || System.getenv("TEAMCITY_VERSION") != null

    /**
     * 写入到生成文件头部的块注释。
     */
    val GENERATED_MESSAGE = """
    /*
     * 本文件由生成器自动生成
     * 请勿手动修改
     */
     """.trimIndent()

    /**
     * 单行自动生成提示的前缀，用于识别历史生成文件。
     */
    const val GENERATED_MESSAGE_PREFIX = "// 本文件由生成器自动生成。参见 "
    /**
     * 单行自动生成提示的后缀，用于识别历史生成文件。
     */
    const val GENERATED_MESSAGE_SUFFIX = "// 请勿手动修改。"

    /**
     * 仅在文件内容变化时写入目标文件。
     *
     * 内容比较会忽略行分隔符差异；在 TeamCity 上默认拒绝真实写入并报告需要重新生成。
     */
    @OptIn(ExperimentalPathApi::class)
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun writeFileIfContentChanged(file: File, newText: String, logNotChanged: Boolean = true, forbidGenerationOnTeamcity: Boolean = true): Boolean {
        val parentFile = file.parentFile
        if (!parentFile.exists()) {
            if (forbidGenerationOnTeamcity) {
                if (failOnTeamCity("Create dir `${parentFile.path}`")) return false
            }
            if (parentFile.mkdirs()) {
                println("Directory created: " + parentFile.absolutePath)
            } else if (!parentFile.exists()) {
                throw IllegalStateException("Cannot create directory: $parentFile")
            }
        }
        if (!isFileContentChangedIgnoringLineSeparators(file, newText)) {
            if (logNotChanged) {
                println("Not changed: " + file.absolutePath)
            }
            return false
        }
        if (forbidGenerationOnTeamcity) {
            if (failOnTeamCity("Write file `${file.toPath()}`")) return false
        }
        val useTempFile = !SystemInfo.isWindows
        val targetFile = file.toPath()
        val tempFile =
            if (useTempFile) createTempDirectory(targetFile.name) / "${targetFile.name}.tmp" else targetFile
        tempFile.writeText(newText, Charsets.UTF_8)
        if (useTempFile) {
            tempFile.moveTo(targetFile, overwrite = true)
        }
        println("File written: ${targetFile.toAbsolutePath()}")
        return true
    }

    /**
     * 在 TeamCity 上将生成动作转为 build problem。
     */
    private fun failOnTeamCity(message: String): Boolean {
        if (!isTeamCityBuild) return false

        fun String.escapeForTC(): String = StringBuilder(length).apply {
            for (char in this@escapeForTC) {
                append(
                    when (char) {
                        '|' -> "||"
                        '\'' -> "|'"
                        '\n' -> "|n"
                        '\r' -> "|r"
                        '[' -> "|["
                        ']' -> "|]"
                        else -> char
                    }
                )
            }
        }.toString()

        val fullMessage = "[Re-generation needed!] $message\n" +
                "Run correspondent (check the log above) Gradle task locally and commit changes."

        println("##teamcity[buildProblem description='${fullMessage.escapeForTC()}']")
        return true
    }

    /**
     * 判断文件内容与给定文本是否不同，比较时忽略行分隔符差异。
     */
    fun isFileContentChangedIgnoringLineSeparators(file: File, content: String): Boolean {
        val currentContent: String = try {
            StringUtil.convertLineSeparators(file.readText(Charsets.UTF_8))
        } catch (ignored: Throwable) {
            return true
        }
        return StringUtil.convertLineSeparators(content) != currentContent
    }

    /**
     * 收集指定目录下带自动生成标记的历史生成文件。
     */
    fun collectPreviouslyGeneratedFiles(generationPath: File): List<File> {
        return generationPath.walkTopDown().filter {
            it.isFile && it.readText().let { GENERATED_MESSAGE_PREFIX in it && GENERATED_MESSAGE_SUFFIX in it }
        }.toList()
    }

    /**
     * 删除上次生成存在但本次没有重新生成的多余文件。
     */
    fun removeExtraFilesFromPreviousGeneration(previouslyGeneratedFiles: List<File>, generatedFiles: List<File>) {
        val generatedFilesPath = generatedFiles.mapTo(mutableSetOf()) { it.absolutePath }

        for (file in previouslyGeneratedFiles) {
            if (file.absolutePath !in generatedFilesPath) {
                if (failOnTeamCity("File delete `${file.absolutePath}`")) continue
                println("Deleted: ${file.absolutePath}")
                file.delete()
            }
        }
    }
}
