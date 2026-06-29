/*
 * Copyright 2010-2025 JetBrains s.r.o. and CangJie Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.decompiled.psi.file

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.util.indexing.FileContentImpl
import org.cangnova.cangjie.analysis.api.util.requireIsInstance
import org.cangnova.cangjie.analysis.decompiled.psi.CjoFileDecompilers
import org.cangnova.cangjie.analysis.decompiled.psi.CangJieDecompiledFileViewProvider
import org.cangnova.cangjie.analysis.decompiled.psi.text.buildDecompiledText
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.cangnova.cangjie.utils.concurrent.block.LockedClearableLazyValue

/**
 * 从 `.cjo` compiled stub 暴露出的仓颉反编译 PSI 文件。
 *
 * 文件文本不来自真实源文件，而是由 compiled stub 重新渲染；该类负责把 IntelliJ PSI 文件协议
 * 与 [CangJieDecompiledFileViewProvider]、[CompiledStubBuilder] 串联起来。
 */
abstract class CjDecompiledFile(
    /**
     * 当前反编译文件所属的 view provider，用于访问原始虚拟文件和清理文本缓存。
     */
    private val provider: CangJieDecompiledFileViewProvider,
) : CjFile(provider, true) {
    /**
     * 返回用于反编译 PSI 的自定义 stub builder。
     *
     * 反编译文件没有源码 AST，需要直接从 `.cjo` compiled stub 构造 PSI stub 树。
     */
    @OptIn(CjImplementationDetail::class)
    override val customStubBuilder: StubBuilder?
        get() = CompiledStubBuilder

    /**
     * 懒加载的反编译文本。
     *
     * 第一次访问时读取或构建 compiled stub，再通过 decompiled text builder 渲染成可展示源码。
     */
    private val decompiledText = LockedClearableLazyValue(Any()) {
        val stub = CompiledStubBuilder.readOrBuildCompiledStub(this)
        buildDecompiledText(stub)
    }

    /**
     * 返回由 compiled stub 渲染出的反编译源码文本。
     */
    override fun getText(): String? {
        return decompiledText.get()
    }

    /**
     * 返回反编译文本长度。
     *
     * 该长度必须与 [getText] 返回值保持一致，避免 document 与 PSI 文本范围失配。
     */
    override fun getTextLength(): Int {
        // Decompiled PSI 的可见文本来自 compiled stub 渲染结果，
        // 不能再沿用底层 stub/AST 的空长度，否则会和 document 文本失配。
        return getText().orEmpty().length
    }

    /**
     * 返回反编译文本的字符数组形式。
     */
    override fun textToCharArray(): CharArray {
        return getText().orEmpty().toCharArray()
    }

    /**
     * 在底层内容刷新时清理 view provider 和本文件持有的反编译文本缓存。
     */
    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        decompiledText.drop()
    }
}

/**
 * 从 `.cjo` 文件读取 compiled stub，并把它转换为当前反编译 PSI 文件可用的 stub 树。
 */
private object CompiledStubBuilder : StubBuilder {
    /**
     * 为 IntelliJ stub infrastructure 构建当前反编译文件的 stub 根。
     *
     * compiled stub 需要深拷贝后再绑定到当前 PSI 文件，避免多个 PSI 文件共享同一批可变 stub 实例。
     */
    override fun buildStubTree(file: PsiFile): CangJieFileStubImpl {
        requireIsInstance<CjDecompiledFile>(file)
        val stub = readOrBuildCompiledStub(file)

        // A copy is required because stubs are stateful and mutable, so they cannot be shared as they are
        @OptIn(CjImplementationDetail::class)
        val clonedStub = stub.deepCopy()
        clonedStub.psi = file
        return clonedStub
    }

    /**
     * 读取或构建指定反编译文件的 compiled stub 根。
     *
     * 如果注册的 `.cjo` decompiler 无法生成仓颉文件 stub，则返回带错误文本的 invalid file stub，
     * 让编辑器展示明确失败信息而不是空文件。
     */
    fun readOrBuildCompiledStub(file: CjDecompiledFile): CangJieFileStubImpl {
        val virtualFile = file.viewProvider.virtualFile
        val project = file.project

        val decompiler = checkNotNull(CjoFileDecompilers.getInstance().find(virtualFile, CjoFileDecompilers.Full::class.java)) {
            "CangJie .cjo decompiler is not registered for ${virtualFile.path}"
        }
        val fileStub = decompiler
            .getStubBuilder()
            .buildFileStub(FileContentImpl.createByFile(virtualFile, project)) as? CangJieFileStubImpl
        return if (fileStub != null) {
            fileStub
        } else {
            val text = """
                // Could not decompile the file: CangJie file stub is not found
                // Please report an issue: https://kotl.in/issue
            """.trimIndent()

            CangJieFileStubImpl.forInvalid(text)
        }
    }

    /**
     * 反编译 stub 构建不跳过任何子节点处理。
     */
    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean = false
}
