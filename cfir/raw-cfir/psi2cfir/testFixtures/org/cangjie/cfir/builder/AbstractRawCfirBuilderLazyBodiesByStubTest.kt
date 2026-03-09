package org.cangjie.cfir.builder

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileElementType
import java.io.File

/**
 * 对齐 Kotlin 的 stub-backed lazy bodies 测试类别。
 */
abstract class AbstractRawCfirBuilderLazyBodiesByStubTest : AbstractRawCfirBuilderLazyBodiesTestCase() {
    override fun doRawCfirTest(filePath: String) {
        val ignoreTreeAccess = isDirectiveDefined(File(filePath).readText(), "// IGNORE_TREE_ACCESS:")
        var treeAccessFound = false
        try {
            super.doRawCfirTest(filePath)
        } catch (e: Throwable) {
            if (!ignoreTreeAccess || e.message?.startsWith("Access to tree elements not allowed for") != true) {
                throw e
            }
            treeAccessFound = true
        }
        assertEquals("The tree access is not detected. 'IGNORE_TREE_ACCESS' have to be dropped", ignoreTreeAccess, treeAccessFound)
    }

    override fun createFileForLazyMode(filePath: String): CjFile {
        val originalFile = super.createFileForLazyMode(filePath)
        val originalProvider = originalFile.viewProvider
        val virtualFile = originalProvider.virtualFile
        check(virtualFile.fileType == CangJieFileType.INSTANCE) {
            "Expected CangJie file type, got ${virtualFile.fileType.name}: ${File(filePath).name}"
        }

        val updatedProvider = object : SingleRootFileViewProvider(
            originalProvider.manager,
            virtualFile,
            originalProvider.isEventSystemEnabled,
            originalProvider.fileType,
        ) {
            override fun isPhysical(): Boolean = true
        }

        val fileWithStub = object : CjFile(updatedProvider, false) {
            private val fakeStub: CangJieFileStub?
                get() = stubTree?.root as? CangJieFileStub

            override fun getStub(): CangJieFileStub? = fakeStub
        }
        updatedProvider.forceCachedPsi(fileWithStub)

        check(fileWithStub.viewProvider.isPhysical) { "Stub mode file must be physical: ${File(filePath).name}" }
        checkNotNull(fileWithStub.stub) { "Stub for the file must not be null: ${File(filePath).name}" }

        updatedProvider.manager.setAssertOnFileLoadingFilter(
            { vf -> vf != virtualFile },
            testRootDisposable,
        )
        return fileWithStub
    }

    private fun isDirectiveDefined(text: String, directive: String): Boolean {
        return text.lineSequence().any { it.trim() == directive }
    }
}
