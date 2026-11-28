/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.messages

import java.io.Serializable

/**
 * 编译器消息关联的源码位置接口。
 */
interface CompilerMessageSourceLocation : Serializable {
    /**
     * 源文件路径。
     */
    val path: String
    /**
     * 起始行号；未知时为 -1。
     */
    val line: Int
    /**
     * 起始列号；未知时为 -1。
     */
    val column: Int
    /**
     * 结束行号；未提供范围时为 -1。
     */
    val lineEnd: Int get() = -1
    /**
     * 结束列号；未提供范围时为 -1。
     */
    val columnEnd: Int get() = -1
    /**
     * 起始位置所在行的文本内容。
     */
    val lineContent: String? // related to the (start) line/column only, used to show start position in the console output
}

/** 同一条编译器消息关联的辅助源码位置。 */
data class CompilerMessageRelatedLocation(
    /** 辅助位置的源码范围。 */
    val location: CompilerMessageSourceLocation,
    /** 辅助位置的显示文字；null 表示只标记范围。 */
    val message: String?,
) : Serializable

/** 支持将主位置与同一条诊断的辅助位置一并传给消息收集器。 */
interface CompilerMessageLocationWithRelatedLocations : CompilerMessageSourceLocation {
    /** 与主位置关联的辅助源码位置。 */
    val relatedLocations: List<CompilerMessageRelatedLocation>
    /** 主提示和无位置 note 分别传递，不能拼进主消息或辅助源码 label。 */
    val mainHint: String? get() = null
    val notes: List<String> get() = emptyList()
    val nativeDiagnosticKind: String? get() = null
    val diagnosticCategory: ParsingDiagnostic.Category? get() = null
}

/**
 * 只包含单点位置的编译器消息位置。
 */
@ConsistentCopyVisibility
data class CompilerMessageLocation private constructor(
    /**
     * 源文件路径。
     */
    override val path: String,
    /**
     * 起始行号。
     */
    override val line: Int,
    /**
     * 起始列号。
     */
    override val column: Int,
    /**
     * 起始行文本内容。
     */
    override val lineContent: String?
) : CompilerMessageSourceLocation {
    /**
     * 渲染为 `path (line:column)` 形式的调试文本。
     */
    override fun toString(): String =
        path + (if (line != -1 || column != -1) " ($line:$column)" else "")

    companion object {
        /**
         * 基于路径创建不带行列信息的位置。
         */
        @JvmStatic
        fun create(path: String?): CompilerMessageLocation? =
            create(path, -1, -1, null)

        /**
         * 基于路径、行列和行文本创建位置；路径为空时返回 null。
         */
        @JvmStatic
        fun create(path: String?, line: Int, column: Int, lineContent: String?): CompilerMessageLocation? =
            if (path == null) null else CompilerMessageLocation(path, line, column, lineContent)

        /**
         * 序列化兼容版本号。
         */
        @Suppress("unused")
        private val serialVersionUID: Long = 8228357578L
    }
}

/**
 * 包含起止范围的编译器消息位置。
 */
@ConsistentCopyVisibility
data class CompilerMessageLocationWithRange private constructor(
    /**
     * 源文件路径。
     */
    override val path: String,
    /**
     * 起始行号。
     */
    override val line: Int,
    /**
     * 起始列号。
     */
    override val column: Int,
    /**
     * 结束行号。
     */
    override val lineEnd: Int,
    /**
     * 结束列号。
     */
    override val columnEnd: Int,
    /**
     * 起始行文本内容。
     */
    override val lineContent: String?,
    override val relatedLocations: List<CompilerMessageRelatedLocation>,
    override val mainHint: String?,
    override val notes: List<String>,
    override val nativeDiagnosticKind: String?,
    override val diagnosticCategory: ParsingDiagnostic.Category?,
) : CompilerMessageLocationWithRelatedLocations {
    /**
     * 渲染为 `path (line:column)` 形式的调试文本。
     */
    override fun toString(): String =
        path + (if (line != -1 || column != -1) " ($line:$column)" else "")

    companion object {
        /**
         * 基于路径和可选结束范围创建消息位置；路径为空时返回 null。
         */
        @JvmStatic
        fun create(
            path: String?,
            lineStart: Int,
            columnStart: Int,
            lineEnd: Int?,
            columnEnd: Int?,
            lineContent: String?
        ): CompilerMessageLocationWithRange? =
            if (path == null) null else CompilerMessageLocationWithRange(
                path,
                lineStart,
                columnStart,
                lineEnd ?: -1,
                columnEnd ?: -1,
                lineContent,
                emptyList(),
                null, emptyList(), null, null,
            )

        /** 创建包含主源码范围及关联辅助范围的消息位置。 */
        @JvmStatic
        fun createWithRelatedLocations(
            path: String?,
            lineStart: Int,
            columnStart: Int,
            lineEnd: Int?,
            columnEnd: Int?,
            lineContent: String?,
            relatedLocations: List<CompilerMessageRelatedLocation>,
            mainHint: String? = null,
            notes: List<String> = emptyList(),
            nativeDiagnosticKind: String? = null,
            diagnosticCategory: ParsingDiagnostic.Category? = null,
        ): CompilerMessageLocationWithRange? =
            if (path == null) null else CompilerMessageLocationWithRange(
                path,
                lineStart,
                columnStart,
                lineEnd ?: -1,
                columnEnd ?: -1,
                lineContent,
                java.util.Collections.unmodifiableList(relatedLocations.toList()),
                mainHint,
                java.util.Collections.unmodifiableList(notes.toList()),
                nativeDiagnosticKind,
                diagnosticCategory,
            )

        /**
         * 序列化兼容版本号。
         */
        @Suppress("unused")
        private val serialVersionUID: Long = 8228357578L
    }
}
