

package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.name.ClassId

/**
 * 当前 `.cjo` 二进制是 package 级容器，而不是 JVM `.class` 那种“facade + multifile part + synthetic/internal class”
 * 的拆分产物，因此：
 * - `allowMultifileClassPart` 只保留 Kotlin 对位的 thread-local 入口形状；
 * - `.cjo` 文件不会被当成 internal compiled file 过滤；
 * - multifile 信息存在于单个 package binary 的头部 `allFiles` 中，而不是额外 sibling binary parts 中。
 *
 * 这里不把这些规则上推到 PSI / decompiler 入口层，避免再次打破 Kotlin 的低层 owner 边界。
 */
object CjoClassFinder {
    /**
     * 查找 Kotlin 对位接口中的 multifile part 二进制文件。
     *
     * 仓颉 `.cjo` 以 package binary 承载所有 facade 与 part 元数据，不生成独立 sibling part 文件；
     * 因此该方法保留调用形状和基本文件有效性检查，但稳定返回空列表。
     */
    fun findMultifileClassParts(file: VirtualFile, classId: ClassId, partNames: List<String>): List<VirtualFile> {
        if (!file.isValidAndExists() || !CjoBinaryFileReader.isCjoBinaryFile(file)) {
            return emptyList()
        }

        // `.cjo` 以单个 package binary 承载 decompiled multifile facade 信息；
        // part names 只用于 facade/part 元数据，不映射为额外的 binary files。
        return emptyList()
    }

    /**
     * 判断文件是否属于不应暴露给反编译 PSI 的 internal compiled file。
     *
     * Kotlin `.class` 需要区分 facade、part 与 synthetic/internal class；仓颉 `.cjo` 没有这一层拆分，
     * 只要是有效 `.cjo` 就代表可展示的 package binary，因此当前实现不会把它标记为 internal。
     */
    @JvmOverloads
    fun isCangJieInternalCompiledFile(
        file: VirtualFile,
        fileContent: ByteArray? = null,
        multifileClassPartKindStrategy: MultifileClassPartKindStrategy = MultifileClassPartKindStrategy.FROM_STACK,
    ): Boolean {
        if (!file.isValidAndExists(fileContent) || !CjoBinaryFileReader.isCjoBinaryFile(file)) {
            return false
        }

        // `.cjo` 本身就是可反编译展示的 package binary，不存在 Kotlin `.class` 那类
        // “应当隐藏的 synthetic/internal compiled file” 分流。
        return false
    }

    /**
     * 控制 multifile part 判定语义的策略枚举。
     *
     * 该枚举保留 Kotlin decompiler 的调用契约，使上层调用方可以沿用相同入口；
     * 对仓颉 `.cjo` 来说，具体策略不会改变“无独立 part binary”的事实。
     */
    enum class MultifileClassPartKindStrategy {
        /** 将 multifile part 按 internal compiled file 处理。 */
        INTERNAL,

        /** 明确允许 multifile part 作为可见二进制参与处理。 */
        NON_INTERNAL,

        /** 由 [allowMultifileClassPart] 设置的线程局部上下文决定处理方式。 */
        FROM_STACK,
    }

    /**
     * 在当前线程内临时允许 multifile part 作为非 internal 文件处理。
     *
     * 这是为保留 Kotlin 对位 API 的线程局部控制点；仓颉 `.cjo` 当前没有实际 part 文件，
     * 但保留该入口可以让调用方不需要感知底层二进制格式差异。
     */
    fun <T> allowMultifileClassPart(action: () -> T): T {
        val old = treatMultifileClassPartAsInternal.get()
        return try {
            treatMultifileClassPartAsInternal.set(false)
            action()
        } finally {
            treatMultifileClassPartAsInternal.set(old)
        }
    }

    /**
     * 判断文件是否表示独立的 multifile part binary。
     *
     * 仓颉 multifile 信息内嵌在 package binary 头部而非拆成物理文件；
     * 因此有效 `.cjo` 也不会被该方法识别为独立 part。
     */
    fun isMultifileClassPartFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        if (!file.isValidAndExists(fileContent) || !CjoBinaryFileReader.isCjoBinaryFile(file)) {
            return false
        }

        // `.cjo` 没有额外的 part binary；multifile part 信息只存在于同一个 package binary 的头部。
        return false
    }

    /**
     * 当前线程的 multifile part internal 处理标记。
     *
     * 默认沿用 Kotlin decompiler 的保守语义；只有 [allowMultifileClassPart] 包裹的调用段会临时关闭。
     */
    private val treatMultifileClassPartAsInternal = ThreadLocal.withInitial { true }

    /**
     * 校验虚拟文件仍然有效、仍存在，并且传入的预读内容不是空内容。
     *
     * 反编译入口通常来自索引或虚拟文件系统事件，该检查用于在真正读取 `.cjo` 前过滤已失效文件。
     */
    private fun VirtualFile.isValidAndExists(fileContent: ByteArray? = null): Boolean =
        isValid && (fileContent == null || fileContent.isNotEmpty()) && exists()
}
