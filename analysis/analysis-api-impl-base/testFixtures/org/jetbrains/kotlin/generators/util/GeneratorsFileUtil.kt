/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.generators.util

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import java.io.File
import java.io.IOException
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.div
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.toPath
import kotlin.io.path.writeText
import kotlin.io.path.createTempDirectory

/**
 * 测试源码生成过程中的文件系统工具。
 *
 * 该对象集中处理生成文件头、内容差异判断、TeamCity 写入保护以及过期生成文件清理，
 * 保证各个测试生成器共享一致的文件更新语义。
 */
object GeneratorsFileUtil {
    /**
     * 当前进程是否运行在 TeamCity 环境中。
     */
    private val isTeamCityBuild: Boolean =
        System.getProperty("teamcity", "false").toBoolean() || System.getenv("TEAMCITY_VERSION") != null

    /**
     * 多行生成文件头文本。
     */
    val GENERATED_MESSAGE = """
    /*
     * This file was generated automatically
     * DO NOT MODIFY IT MANUALLY
     */
     """.trimIndent()

    /**
     * 单行生成文件头的起始标记。
     */
    const val GENERATED_MESSAGE_PREFIX = "// This file was generated automatically. See "

    /**
     * 单行生成文件头的禁止手改标记。
     */
    const val GENERATED_MESSAGE_SUFFIX = "// DO NOT MODIFY IT MANUALLY."

    /**
     * 仅当目标文件内容发生变化时写入新文本。
     *
     * @param file 目标生成文件。
     * @param newText 新生成的完整文件内容。
     * @param logNotChanged 内容未变化时是否输出日志。
     * @param forbidGenerationOnTeamcity 是否在 TeamCity 上阻止真实写入并改为报告构建问题。
     * @return 文件内容是否发生变化并触发写入。
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
     * 在 TeamCity 环境中把需要重新生成的动作报告为构建问题。
     *
     * @return 返回 true 表示当前运行在 TeamCity 且调用方应跳过真实文件操作。
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
     * 比较目标文件与新内容是否不同，比较前统一换行符。
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
     * 收集指定目录下此前由生成器写出的文件。
     */
    fun collectPreviouslyGeneratedFiles(generationPath: File): List<File> {
        return generationPath.walkTopDown().filter {
            it.isFile && it.readText().let { text -> GENERATED_MESSAGE_PREFIX in text && GENERATED_MESSAGE_SUFFIX in text }
        }.toList()
    }

    /**
     * 删除本轮生成结果中已经不存在的历史生成文件。
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
