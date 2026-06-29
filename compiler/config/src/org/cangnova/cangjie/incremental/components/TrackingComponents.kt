package org.cangnova.cangjie.incremental.components

import java.io.File
import java.io.Serializable

/**
 * 源码位置。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.incremental.components.Position`
 */
data class Position(
    /**
     * 1-based 源码行号；未知位置使用 `-1`。
     */
    val line: Int,
    /**
     * 1-based 源码列号；未知位置使用 `-1`。
     */
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
    /**
     * 发生名称查找的源文件路径。
     */
    val filePath: String,
    /**
     * 名称查找在源文件中的位置。
     */
    val position: Position,
    /**
     * 被查找名称所在作用域的全限定名。
     */
    val scopeFqName: String,
    /**
     * 被查找作用域的种类。
     */
    val scopeKind: ScopeKind,
    /**
     * 实际查找的短名称。
     */
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

    /**
     * 空实现名称查找跟踪器，用于不启用增量依赖记录的编译流程。
     */
    object DO_NOTHING : LookupTracker {
        /**
         * 空实现不需要记录精确源码位置。
         */
        override val requiresPosition: Boolean
            get() = false

        /**
         * 忽略一次名称查找记录。
         */
        override fun record(
            filePath: String,
            position: Position,
            scopeFqName: String,
            scopeKind: ScopeKind,
            name: String,
        ) = Unit

        /**
         * 空实现没有可清理状态。
         */
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

    /**
     * 空实现枚举匹配跟踪器。
     */
    object DoNothing : EnumMatchTracker {
        /**
         * 忽略一次 `when` 枚举使用记录。
         */
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

    /**
     * 空实现 import 跟踪器。
     */
    object DoNothing : ImportTracker {
        /**
         * 忽略一次 import 使用记录。
         */
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

    /**
     * 空实现文件映射跟踪器。
     */
    object DoNothing : ICFileMappingTracker {
        /**
         * 忽略源码文件到输出文件的映射记录。
         */
        override fun recordSourceFilesToOutputFileMapping(sourceFiles: Collection<File>, outputFile: File) = Unit

        /**
         * 忽略编译器插件引用源码的记录。
         */
        override fun recordSourceReferencedByCompilerPlugin(sourceFile: File) = Unit

        /**
         * 忽略插件生成输出文件的记录。
         */
        override fun recordOutputFileGeneratedForPlugin(outputFile: File) = Unit
    }
}
