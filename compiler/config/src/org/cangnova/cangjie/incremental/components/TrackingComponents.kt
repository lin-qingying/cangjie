package org.cangnova.cangjie.incremental.components

import java.io.File
import java.io.Serializable

/**
 * 源码位置。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.incremental.components.Position`
 */
data class Position(
    val line: Int,
    val column: Int,
) : Serializable {
    companion object {
        /** 未知位置常量。 */
        val NO_POSITION: Position = Position(-1, -1)
    }
}

/**
 * 名称查找范围类型。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.incremental.components.ScopeKind`
 */
enum class ScopeKind {
    PACKAGE,
    CLASSIFIER,
}

/**
 * 名称查找记录项。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.incremental.components.LookupInfo`
 */
data class LookupInfo(
    val filePath: String,
    val position: Position,
    val scopeFqName: String,
    val scopeKind: ScopeKind,
    val name: String,
) : Serializable

/**
 * 名称查找跟踪器。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.incremental.components.LookupTracker`
 */
interface LookupTracker {
    /** 是否要求记录精确位置（测试常用）。 */
    val requiresPosition: Boolean

    /** 记录一次名称查找。 */
    fun record(
        filePath: String,
        position: Position,
        scopeFqName: String,
        scopeKind: ScopeKind,
        name: String,
    )

    /** 清空跟踪状态。 */
    fun clear()

    object DO_NOTHING : LookupTracker {
        override val requiresPosition: Boolean
            get() = false

        override fun record(
            filePath: String,
            position: Position,
            scopeFqName: String,
            scopeKind: ScopeKind,
            name: String,
        ) = Unit

        override fun clear() = Unit
    }
}

/**
 * `when` 枚举使用跟踪器。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.incremental.components.EnumMatchTracker`
 */
interface EnumMatchTracker {
    /** 记录一次枚举在 `when` 表达式中的使用。 */
    fun report(whenExpressionFilePath: String, enumClassFqName: String)

    object DoNothing : EnumMatchTracker {
        override fun report(whenExpressionFilePath: String, enumClassFqName: String) = Unit
    }
}

/**
 * import 跟踪器。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.incremental.components.ImportTracker`
 */
interface ImportTracker {
    /** 记录一次 import 使用。 */
    fun report(filePath: String, importedFqName: String)

    object DoNothing : ImportTracker {
        override fun report(filePath: String, importedFqName: String) = Unit
    }
}

/**
 * 编译增量文件映射跟踪器。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.incremental.components.ICFileMappingTracker`
 */
interface ICFileMappingTracker {
    /** 记录源码文件与输出文件映射。 */
    fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File)

    /** 记录被编译器插件引用的源码。 */
    fun recordSourceReferencedByCompilerPlugin(sourceFile: File)

    /** 记录由插件生成的输出文件。 */
    fun recordOutputFileGeneratedForPlugin(outputFile: File)

    object DoNothing : ICFileMappingTracker {
        override fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File) = Unit
        override fun recordSourceReferencedByCompilerPlugin(sourceFile: File) = Unit
        override fun recordOutputFileGeneratedForPlugin(outputFile: File) = Unit
    }
}

