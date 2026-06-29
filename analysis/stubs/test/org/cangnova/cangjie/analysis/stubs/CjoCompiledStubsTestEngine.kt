package org.cangnova.cangjie.analysis.stubs

import com.intellij.psi.PsiFileFactory
import com.intellij.psi.stubs.StubElement
import com.intellij.util.indexing.FileContentImpl
import org.cangnova.cangjie.analysis.api.util.requireIsInstance
import org.cangnova.cangjie.analysis.decompiled.psi.CjoFileDecompilers
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * compiled `.cjo` stub 测试共用引擎。
 *
 * 该对象负责从二进制直接计算 file stub、校验反编译 PSI stub 与二进制 stub 一致，并渲染 stub 树。
 */
internal object CjoCompiledStubsTestEngine {
    /**
     * golden 中用于标记已知临时 stub 差异的固定文本。
     */
    const val INCONSISTENT_TREE: String = "INCONSISTENT_TREE"

    /**
     * 从 compiled PSI 文件对应的 `.cjo` 二进制直接计算文件 stub。
     */
    fun compute(file: CjFile): CangJieFileStubImpl {
        requireIsInstance<CjDecompiledFile>(file)
        val virtualFile = file.viewProvider.virtualFile
        val decompiler = requireNotNull(CjoFileDecompilers.getInstance().find(virtualFile, CjoFileDecompilers.Full::class.java)) {
            "A `.cjo` decompiler is expected to be registered for ${virtualFile.path}"
        }
        val fileStub = decompiler
            .getStubBuilder()
            .buildFileStub(FileContentImpl.createByFile(virtualFile, file.project))
        requireNotNull(fileStub) { "A `.cjo` file stub is expected for ${virtualFile.path}" }
        requireIsInstance<CangJieFileStubImpl>(fileStub)
        return fileStub
    }

    /**
     * 校验反编译 PSI 计算出的 stub 树与二进制直接计算出的 stub 树一致。
     */
    fun validate(file: CjFile, fileStub: CangJieFileStubImpl) {
        requireIsInstance<CjDecompiledFile>(file)
        val decompiledStub = try {
            file.calcStubTree().root
        } catch (e: Throwable) {
            val sourceStubDump = runCatching {
                val sourceFile = PsiFileFactory.getInstance(file.project).createFileFromText(
                    "${file.name}.decompiled.cj",
                    CangJieFileType.INSTANCE,
                    file.text.orEmpty(),
                ) as CjFile
                render(sourceFile.calcStubTree().root as CangJieFileStubImpl)
            }.getOrElse { sourceError ->
                "<failed to compute source stub tree: ${sourceError::class.qualifiedName}: ${sourceError.message}>"
            }
            throw AssertionError(
                e.message.orEmpty() + "\n\nBinary stub tree:\n" + render(fileStub) + "\n\nSource text stub tree:\n" + sourceStubDump,
                e,
            )
        }
        requireIsInstance<CangJieFileStubImpl>(decompiledStub)
        assertEquals(
            render(fileStub),
            render(decompiledStub),
            "The stub tree computed from decompiled text must match the stub tree computed from `.cjo` binary data. Use $INCONSISTENT_TREE only for tracked temporary divergences.",
        )
    }

    /**
     * 将文件 stub 树渲染为稳定的缩进文本。
     */
    fun render(fileStub: CangJieFileStubImpl): String = buildString {
        renderStub(fileStub, indent = "")
    }.trimEnd()

    /**
     * 递归渲染单个 stub 节点及其子节点。
     */
    private fun StringBuilder.renderStub(stub: StubElement<*>, indent: String) {
        append(indent)
        appendLine(stub.toString())
        stub.childrenStubs.forEach { child ->
            renderStub(child, "$indent  ")
        }
    }
}
