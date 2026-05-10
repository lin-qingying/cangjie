/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
    fun findMultifileClassParts(file: VirtualFile, classId: ClassId, partNames: List<String>): List<VirtualFile> {
        if (!file.isValidAndExists() || !CjoBinaryFileReader.isCjoBinaryFile(file)) {
            return emptyList()
        }

        // `.cjo` 以单个 package binary 承载 decompiled multifile facade 信息；
        // part names 只用于 facade/part 元数据，不映射为额外的 binary files。
        return emptyList()
    }

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

    enum class MultifileClassPartKindStrategy {
        INTERNAL,
        NON_INTERNAL,
        FROM_STACK,
    }

    fun <T> allowMultifileClassPart(action: () -> T): T {
        val old = treatMultifileClassPartAsInternal.get()
        return try {
            treatMultifileClassPartAsInternal.set(false)
            action()
        } finally {
            treatMultifileClassPartAsInternal.set(old)
        }
    }

    fun isMultifileClassPartFile(file: VirtualFile, fileContent: ByteArray? = null): Boolean {
        if (!file.isValidAndExists(fileContent) || !CjoBinaryFileReader.isCjoBinaryFile(file)) {
            return false
        }

        // `.cjo` 没有额外的 part binary；multifile part 信息只存在于同一个 package binary 的头部。
        return false
    }

    private val treatMultifileClassPartAsInternal = ThreadLocal.withInitial { true }

    private fun VirtualFile.isValidAndExists(fileContent: ByteArray? = null): Boolean =
        isValid && (fileContent == null || fileContent.isNotEmpty()) && exists()
}
