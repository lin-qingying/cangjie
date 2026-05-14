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

internal object CjoCompiledStubsTestEngine {
    const val INCONSISTENT_TREE: String = "INCONSISTENT_TREE"

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

    fun render(fileStub: CangJieFileStubImpl): String = buildString {
        renderStub(fileStub, indent = "")
    }.trimEnd()

    private fun StringBuilder.renderStub(stub: StubElement<*>, indent: String) {
        append(indent)
        appendLine(stub.toString())
        stub.childrenStubs.forEach { child ->
            renderStub(child, "$indent  ")
        }
    }
}
