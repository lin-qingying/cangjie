package org.cangnova.cangjie.analysis.decompiled

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledTextRenderer
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaDecompiledBinarySupport
import org.cangnova.cangjie.analysis.decompiled.psi.CaDecompiledPsiSupport
import org.cangnova.cangjie.analysis.decompiled.stubs.CaDecompiledPackageViewService
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDecompiledFile

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

    override fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile? {
        return support.findBinaryFile(module, packageFqName)
    }

    override fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile? {
        return support.findBinaryFile(module, packageFqName)
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
    private val binaryIndex: CaDecompiledBinaryIndex
        get() = project.getService(CaDecompiledBinaryIndex::class.java)

    private val psiSupport: CaDecompiledPsiSupport
        get() = project.getService(CaDecompiledPsiSupport::class.java)

    override fun getDecompiledFile(binaryFile: VirtualFile): CjDecompiledFile? {
        return psiSupport.getDecompiledFile(binaryFile)
    }

    override fun findDecompiledFile(module: CaLibraryModule, packageFqName: FqName): CjDecompiledFile? {
        val binaryFile = binaryIndex.findBinaryFile(module, packageFqName) ?: return null
        return psiSupport.getDecompiledFile(binaryFile)
    }

    override fun findDecompiledFile(module: CaBuiltinsModule, packageFqName: FqName): CjDecompiledFile? {
        val binaryFile = binaryIndex.findBinaryFile(module, packageFqName) ?: return null
        return psiSupport.getDecompiledFile(binaryFile)
    }
}

/**
 * 对外 Decompiled 文本渲染 facade。
 */
class CaDecompiledTextRendererImpl(
    private val project: Project,
) : CaDecompiledTextRenderer {
    private val packageViews: CaDecompiledPackageViewService
        get() = project.getService(CaDecompiledPackageViewService::class.java)

    override fun render(binaryFile: VirtualFile): String? {
        return packageViews.getPackageView(binaryFile)?.renderedText
    }

    override fun render(file: CjDecompiledFile): String {
        return packageViews.getPackageView(file.viewProvider.virtualFile)?.renderedText ?: file.text
    }
}
