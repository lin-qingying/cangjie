package org.cangnova.cangjie.analysis.decompiled

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiManager
import com.intellij.psi.StubBuilder
import com.intellij.psi.PsiFile
import com.intellij.lang.ASTNode
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledTextRenderer
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaDecompiledBinarySupport
import org.cangnova.cangjie.analysis.decompiled.psi.StandaloneDecompiledModuleData
import org.cangnova.cangjie.analysis.decompiled.stubs.CaDecompiledPackageViewBuilder
import org.cangnova.cangjie.analysis.decompiled.stubs.CaDecompiledPackageView
import org.cangnova.cangjie.lang.declarations.CangJieFileViewProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDecompiledFile
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import java.util.concurrent.ConcurrentHashMap

/**
 * 对外 `.cjo` binary index facade。
 *
 * 根模块只负责装配公开服务，
 * 不再承载仓库扫描、反序列化与 PSI 构造等实现细节。
 */
class CaDecompiledBinaryIndexImpl(
    private val project: Project,
) : CaDecompiledBinaryIndex {
    private val support: CaDecompiledBinarySupport
        get() = project.getService(CaDecompiledBinarySupport::class.java)

    override fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile> {
        return support.getBinaryFiles(module)
    }

    override fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile> {
        return support.getBinaryFiles(module)
    }

    override fun readPackageFqName(binaryFile: VirtualFile): FqName? {
        return support.readPackageFqName(binaryFile)
    }

    override fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile? {
        return support.findBinaryFile(module, packageFqName)
    }

    override fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile? {
        return support.findBinaryFile(module, packageFqName)
    }

    override fun findBuiltinsBinaryFile(packageFqName: FqName): VirtualFile? {
        return support.findBuiltinsBinaryFile(packageFqName)
    }

    override fun findOwningModule(binaryFile: VirtualFile): CaModule? {
        return support.findOwningModule(binaryFile)
    }
}

/**
 * 对外 Decompiled PSI facade。
 */
class CaDecompiledPsiProviderImpl(
    private val project: Project,
) : CaDecompiledPsiProvider {
    private val psiManager: PsiManager = PsiManager.getInstance(project)
    @Volatile
    private var knownModificationCount: Long = Long.MIN_VALUE
    private val files = ConcurrentHashMap<String, CjDecompiledFile?>()

    private val binaryIndex: CaDecompiledBinaryIndex
        get() = project.getService(CaDecompiledBinaryIndex::class.java)

    private val binarySupport: CaDecompiledBinarySupport
        get() = project.getService(CaDecompiledBinarySupport::class.java)

    override fun getDecompiledFile(binaryFile: VirtualFile): CjDecompiledFile? {
        refreshIfNeeded()
        files[binaryFile.url]?.let { return it }
        val decompiledFile = createDecompiledFile(binaryFile) ?: return null
        val previous = files.putIfAbsent(binaryFile.url, decompiledFile)
        return previous ?: decompiledFile
    }

    override fun findDecompiledFile(module: CaLibraryModule, packageFqName: FqName): CjDecompiledFile? {
        val binaryFile = binaryIndex.findBinaryFile(module, packageFqName) ?: return null
        return getDecompiledFile(binaryFile)
    }

    override fun findDecompiledFile(module: CaBuiltinsModule, packageFqName: FqName): CjDecompiledFile? {
        val binaryFile = binaryIndex.findBinaryFile(module, packageFqName) ?: return null
        return getDecompiledFile(binaryFile)
    }

    override fun findBuiltinsDecompiledFile(packageFqName: FqName): CjDecompiledFile? {
        val binaryFile = binaryIndex.findBuiltinsBinaryFile(packageFqName) ?: return null
        return getDecompiledFile(binaryFile)
    }

    private fun createDecompiledFile(binaryFile: VirtualFile): CjDecompiledFile? {
        val loadedPackage = binarySupport.loadPackageData(binaryFile) ?: return null
        val packageView = CaDecompiledPackageViewBuilder.buildPackageView(loadedPackage, StandaloneDecompiledModuleData)
        val stubBuilder = DecompiledStubBuilder(packageView)
        val textProvider = { _: CangJieFileViewProvider -> packageView.renderedText }
        lateinit var decompiledFile: CjDecompiledFile
        val viewProvider = CangJieFileViewProvider(
            manager = psiManager,
            file = binaryFile,
            physical = false,
            factory = { decompiledFile },
            textProvider = textProvider,
        )
        decompiledFile = CjDecompiledFile(viewProvider, stubBuilder) { textProvider(viewProvider).toString() }

        return ApplicationManager.getApplication().runWriteAction<CjDecompiledFile> {
            viewProvider.forceCachedPsi(decompiledFile)
            decompiledFile
        }
    }

    private fun refreshIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (knownModificationCount == modificationCount) return
        files.clear()
        knownModificationCount = modificationCount
    }
}

private class DecompiledStubBuilder(
    private val packageView: CaDecompiledPackageView,
) : StubBuilder {
    override fun buildStubTree(file: PsiFile): CangJieFileStubImpl {
        require(file is CjDecompiledFile) { "Expected CjDecompiledFile, got ${file::class.simpleName}" }
        val originalChildren = packageView.fileStub.childrenStubs.map { child ->
            "${child::class.simpleName}:${child.stubType}"
        }
        val copiedStub = packageView.fileStub.deepCopy()
        copiedStub.psi = file
        file.recordCompiledStubDebug(
            originalChildren = originalChildren,
            copiedChildren = copiedStub.childrenStubs.map { child -> "${child::class.simpleName}:${child.stubType}" },
            declarationKinds = packageView.declarationDebug,
        )
        return copiedStub
    }

    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean = false
}

/**
 * 对外 Decompiled 文本渲染 facade。
 */
class CaDecompiledTextRendererImpl(
    private val project: Project,
) : CaDecompiledTextRenderer {
    private val binarySupport: CaDecompiledBinarySupport
        get() = project.getService(CaDecompiledBinarySupport::class.java)

    override fun render(binaryFile: VirtualFile): String? {
        val loadedPackage = binarySupport.loadPackageData(binaryFile) ?: return null
        return CaDecompiledPackageViewBuilder.buildPackageView(loadedPackage, StandaloneDecompiledModuleData).renderedText
    }

    override fun render(file: CjDecompiledFile): String {
        return render(file.viewProvider.virtualFile) ?: file.text
    }
}
