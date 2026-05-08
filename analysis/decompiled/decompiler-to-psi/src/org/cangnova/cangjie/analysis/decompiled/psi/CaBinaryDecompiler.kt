package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaCjoBinaryFileReader
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaLoadedCjoPackage
import org.cangnova.cangjie.analysis.decompiled.stubs.CaCjoDeclarationLoader
import org.cangnova.cangjie.analysis.decompiled.stubs.CaDecompiledTextRendering
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieCompiledFileErrors
import java.io.File

/**
 * IDE `BinaryFileDecompiler` 入口。
 */
class CaCjoBinaryFileDecompiler : BinaryFileDecompiler {
    override fun decompile(file: VirtualFile): CharSequence {
        return CaStandaloneBinaryTextRenderer.render(file) ?: ""
    }
}

/**
 * 独立的 `.cjo` 文本反编译入口。
 */
object CaStandaloneBinaryTextRenderer {
    fun render(binaryFile: VirtualFile): String? {
        if (!CaCjoBinaryFileReader.isCjoBinaryFile(binaryFile)) return null
        val loadedPackage = loadStandalonePackage(binaryFile) ?: return null
        if (!loadedPackage.isVersionSupported) {
            return CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR
        }
        val declarations = CaCjoDeclarationLoader.loadDeclarations(loadedPackage, StandaloneDecompiledModuleData)
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
     * `.cjo` 单文件反编译时尽量恢复真实仓库根。
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

object StandaloneDecompiledSession : CfirSession(Kind.Library) {
    init {
        register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
    }

    override fun toString(): String = "CaStandaloneDecompiledSession"
}

object StandaloneDecompiledModuleData : CfirModuleData() {
    override val name: Name = Name.identifier("analysis-decompiled-standalone")
    override val dependencies: List<CfirModuleData> = emptyList()
    override val refinementDependencies: List<CfirModuleData> = emptyList()
    override val allRefinementDependencies: List<CfirModuleData> = emptyList()
    override val platform: CfirPlatform = CfirPlatform.DEFAULT
    override val isCommon: Boolean = true
    override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
    override val stableModuleName: String = "analysis-decompiled-standalone"
    override val session: CfirSession
        get() = StandaloneDecompiledSession

    init {
        bindSession(StandaloneDecompiledSession)
    }
}
