package org.cangnova.cangjie.cfir.builder

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileElementType
import java.io.File

/**
 * 对齐 Kotlin 的 stub-backed lazy bodies 测试类别。
 */
abstract class AbstractRawCfirBuilderLazyBodiesByStubTest : AbstractRawCfirBuilderLazyBodiesTestCase() {
    /**
     * 执行 stub-backed lazy bodies golden 测试。
     */
    override fun doRawCfirTest(filePath: String) {
        val resolvedFilePath = resolveTestDataPath(filePath).path
        val sourceText = File(resolvedFilePath).readText()
        val fallbackToAst = isDirectiveDefined(sourceText, "// STUB_FALLBACK_TO_AST:")
        if (fallbackToAst) {
            val file = createPsiFile(File(resolvedFilePath).nameWithoutExtension, sourceText) as CjFile
            val cfirFile = file.toCfirFile(bodyBuildingMode = BodyBuildingMode.LAZY_BODIES)
            val dump = dumpCfirFile(cfirFile)
            val expected = File(resolvedFilePath.replace(".cj", ".lazyBodies.txt"))
            assertEqualsToFile(expected, dump)
            return
        }

        val ignoreTreeAccess = isDirectiveDefined(sourceText, "// IGNORE_TREE_ACCESS:")
        var treeAccessFound = false
        try {
            runOnEdt { super.doRawCfirTest(resolvedFilePath) }
        } catch (e: Throwable) {
            if (!ignoreTreeAccess || e.message?.startsWith("Access to tree elements not allowed for") != true) {
                throw e
            }
            treeAccessFound = true
        }
        assertEquals("The tree access is not detected. 'IGNORE_TREE_ACCESS' have to be dropped", ignoreTreeAccess, treeAccessFound)
    }

    /**
     * 创建带文件 stub 的测试文件。
     */
    override fun createFileForLazyMode(filePath: String): CjFile {
        val originalFile = super.createFileForLazyMode(filePath)
        val sourceText = File(filePath).readText()
        val allowAstFallback = isDirectiveDefined(sourceText, "// STUB_FALLBACK_TO_AST:")
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
        val stub = try {
            fileWithStub.stub
        } catch (t: Throwable) {
            if (allowAstFallback) {
                return originalFile
            }
            throw t
        }
        if (stub == null && allowAstFallback) {
            return originalFile
        }
        checkNotNull(stub) { "Stub for the file must not be null: ${File(filePath).name}" }

        updatedProvider.manager.setAssertOnFileLoadingFilter(
            { vf -> vf != virtualFile },
            testRootDisposable,
        )
        return fileWithStub
    }

    /**
     * 判断测试文件中是否声明了指定指令。
     */
    private fun isDirectiveDefined(text: String, directive: String): Boolean {
        return text.lineSequence().any { line ->
            val normalized = line.trimStart('\uFEFF').trim()
            normalized == directive || normalized.contains(directive)
        }
    }

    /**
     * 在 EDT 上同步执行测试动作并传播异常。
     */
    private fun runOnEdt(action: () -> Unit) {
        var error: Throwable? = null
        ApplicationManager.getApplication().invokeAndWait {
            try {
                action()
            } catch (t: Throwable) {
                error = t
            }
        }
        error?.let { throw it }
    }
}
