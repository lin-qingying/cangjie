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

abstract class CjDecompiledFile(private val provider: CangJieDecompiledFileViewProvider) : CjFile(provider, true) {
    @OptIn(CjImplementationDetail::class)
    override val customStubBuilder: StubBuilder?
        get() = CompiledStubBuilder

    private val decompiledText = LockedClearableLazyValue(Any()) {
        val stub = CompiledStubBuilder.readOrBuildCompiledStub(this)
        buildDecompiledText(stub)
    }

    override fun getText(): String? {
        return decompiledText.get()
    }

    override fun getTextLength(): Int {
        // Decompiled PSI 的可见文本来自 compiled stub 渲染结果，
        // 不能再沿用底层 stub/AST 的空长度，否则会和 document 文本失配。
        return getText().orEmpty().length
    }

    override fun textToCharArray(): CharArray {
        return getText().orEmpty().toCharArray()
    }

    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        decompiledText.drop()
    }
}

private object CompiledStubBuilder : StubBuilder {
    override fun buildStubTree(file: PsiFile): CangJieFileStubImpl {
        requireIsInstance<CjDecompiledFile>(file)
        val stub = readOrBuildCompiledStub(file)

        // A copy is required because stubs are stateful and mutable, so they cannot be shared as they are
        @OptIn(CjImplementationDetail::class)
        val clonedStub = stub.deepCopy()
        clonedStub.psi = file
        return clonedStub
    }

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

    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean = false
}
