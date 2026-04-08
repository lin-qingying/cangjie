package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.StubBuilder
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaCjoBinaryFileReader
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaLoadedCjoPackage
import org.cangnova.cangjie.analysis.decompiled.stubs.CaCjoDeclarationLoader
import org.cangnova.cangjie.analysis.decompiled.stubs.CaDecompiledPackageViewService
import org.cangnova.cangjie.analysis.decompiled.stubs.CaDecompiledTextRendering
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.lang.declarations.CangJieFileViewProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDecompiledFile
import org.cangnova.cangjie.psi.stubs.CangJieCompiledFileErrors
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Decompiled PSI 组装底座。
 *
 * 它负责把同一个 binary package view 投影成稳定的 `CjDecompiledFile`，
 * 从而保证文本、stub 与 PSI 三个视图共用同一份二进制事实源。
 */
class CaDecompiledPsiSupport(
    private val project: Project,
) {
    @Volatile
    private var knownModificationCount: Long = Long.MIN_VALUE

    private val files = ConcurrentHashMap<String, CjDecompiledFile?>()

    private val packageViews: CaDecompiledPackageViewService
        get() = project.getService(CaDecompiledPackageViewService::class.java)

    fun getDecompiledFile(binaryFile: VirtualFile): CjDecompiledFile? {
        refreshIfNeeded()
        if (!CaCjoBinaryFileReader.isCjoBinaryFile(binaryFile)) return null
        return files.computeIfAbsent(binaryFile.url) {
            createDecompiledFile(binaryFile)
        }
    }

    private fun createDecompiledFile(binaryFile: VirtualFile): CjDecompiledFile? {
        val psiManager = PsiManager.getInstance(project)
        val stubBuilder = CaCompiledStubBuilder(packageViews)
        val textProvider = { provider: CangJieFileViewProvider ->
            packageViews.getPackageView(provider.virtualFile)?.renderedText.orEmpty()
        }
        val viewProvider = CangJieFileViewProvider(
            manager = psiManager,
            file = binaryFile,
            physical = false,
            factory = { provider -> CjDecompiledFile(provider, stubBuilder) { textProvider(provider).toString() } },
            textProvider = textProvider,
        )
        return viewProvider.getPsi(CangJieLanguage) as? CjDecompiledFile
    }

    private fun refreshIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (knownModificationCount == modificationCount) return

        files.clear()
        knownModificationCount = modificationCount
    }
}

private class CaCompiledStubBuilder(
    private val packageViews: CaDecompiledPackageViewService,
) : StubBuilder {
    override fun buildStubTree(file: PsiFile): CangJieFileStubImpl {
        require(file is CjDecompiledFile) { "Expected CjDecompiledFile, got ${file::class.simpleName}" }
        val packageView = packageViews.getPackageView(file.viewProvider.virtualFile)
            ?: return CangJieFileStubImpl.forInvalid("// Missing decompiled package view for ${file.name}")
        val copiedStub = packageView.fileStub.deepCopy()
        copiedStub.psi = file
        return copiedStub
    }

    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean = false
}

/**
 * IDE `BinaryFileDecompiler` 入口。
 *
 * 该入口不依赖 `Project`，因此直接在当前文件所在目录构建临时 `.cjo` 仓库。
 */
class CaCjoBinaryFileDecompiler : BinaryFileDecompiler {
    override fun decompile(file: VirtualFile): CharSequence {
        return CaStandaloneBinaryTextRenderer.render(file) ?: ""
    }
}

object CaStandaloneBinaryTextRenderer {
    fun render(binaryFile: VirtualFile): String? {
        if (!CaCjoBinaryFileReader.isCjoBinaryFile(binaryFile)) return null
        val loadedPackage = loadStandalonePackage(binaryFile) ?: return null
        if (!loadedPackage.isVersionSupported) {
            return CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR
        }
        val declarations = CaCjoDeclarationLoader.loadDeclarations(loadedPackage)
        return CaDecompiledTextRendering.renderPackageText(loadedPackage, declarations)
    }

    private fun loadStandalonePackage(binaryFile: VirtualFile): CaLoadedCjoPackage? {
        val packageFqName = CaCjoBinaryFileReader.readPackageFqName(binaryFile) ?: return null

        val root = deriveRepositoryRoot(binaryFile, packageFqName)
        val cjoManager = CjoManager(
            CjoSearchPath { key ->
                when (key) {
                    "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> root.absolutePath
                    else -> null
                }
            },
        )
        val packageName = packageFqName.asString()
        val header = cjoManager.loadPackageHeader(packageName) ?: return null
        val pkg = cjoManager.loadPackage(packageName) ?: return null
        return CaLoadedCjoPackage(
            owningModule = null,
            binaryFile = binaryFile,
            packageFqName = packageFqName,
            pkg = pkg,
            header = header,
            searchRoots = listOf(root),
            isVersionSupported = CaCjoBinaryFileReader.isSupportedVersion(pkg),
        )
    }

    /**
     * `.cjo` 单文件反编译时需要尽量恢复真实仓库根，
     * 否则带 import 的包很容易因为只看到了当前子目录而失去同库可见性。
     *
     * 当前 CangJie SDK 布局是：
     * - `std.cjo` 直接位于根目录
     * - `std.core.cjo` / `std.collection.cjo` 位于 `std/`
     *
     * 因而对非根包优先回退到父目录的父目录；若结构不满足，再安全退回当前父目录。
     */
    private fun deriveRepositoryRoot(binaryFile: VirtualFile, packageFqName: FqName): File {
        val file = File(binaryFile.path)
        val normalizedPath = file.absolutePath.replace('\\', '/')
        val suffix = CjoConstants.packageNameToPath(packageFqName.asString()).replace('\\', '/')
        return if (normalizedPath.endsWith("/$suffix")) {
            File(normalizedPath.removeSuffix("/$suffix"))
        } else {
            val parent = File(binaryFile.parent.path)
            if (packageFqName.isRoot) parent else parent.parentFile ?: parent
        }
    }
}
