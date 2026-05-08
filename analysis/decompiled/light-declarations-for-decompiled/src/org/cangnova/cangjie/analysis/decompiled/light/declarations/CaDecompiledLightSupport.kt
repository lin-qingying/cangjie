@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.decompiled.light.declarations

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDecompiledFile

/**
 * Decompiled declaration-view 消费侧的统一辅助入口。
 *
 * 它只负责：
 * 1. 从 `library / builtins` 模块收集 decompiled files；
 * 2. 为 symbol / declaration-view 层提供 `package -> decompiled file` 的稳定查找。
 */
class CaDecompiledLightSupport(
    private val project: Project,
) {
    private val binaryIndex: CaDecompiledBinaryIndex
        get() = CaDecompiledBinaryIndex.getInstance(project)

    private val psiProvider: CaDecompiledPsiProvider
        get() = CaDecompiledPsiProvider.getInstance(project)

    fun getDecompiledFiles(module: CaModule): List<CjDecompiledFile> {
        return when (module) {
            is CaLibraryModule -> binaryIndex.getBinaryFiles(module).mapNotNull(psiProvider::getDecompiledFile)
            is CaBuiltinsModule -> binaryIndex.getBinaryFiles(module).mapNotNull(psiProvider::getDecompiledFile)
            else -> emptyList()
        }
    }

    /**
     * 统一按 decompiled binary 归属反查 package 所在模块。
     *
     * 这里的顺序必须和 decompiled facade 的其它消费方保持一致：
     * 1. 先尊重显式传入的 preferred module；
     * 2. 再固定 builtins -> libraries；
     * 3. 只接受能够稳定命中 binary index 的模块。
     */
    fun findContainingModule(packageFqName: FqName, preferredModule: CaModule? = null): CaModule? {
        findInModule(preferredModule, packageFqName)?.let { return preferredModule }

        psiProvider.findBuiltinsDecompiledFile(packageFqName)?.let { decompiledFile ->
            val module = CangJieProjectStructureProvider.getModule(
                project,
                decompiledFile,
                useSiteModule = null,
            )
            if (module !== preferredModule) {
                return module
            }
        }

        val projectStructure = CaModuleProvider.getInstance(project)
        projectStructure.allModules.filterIsInstance<CaLibraryModule>().forEach { module ->
            if (module === preferredModule) return@forEach
            findInModule(module, packageFqName)?.let { return module }
        }
        return null
    }

    fun findContainingFile(packageFqName: FqName, preferredModule: CaModule? = null): CjDecompiledFile? {
        findInModule(preferredModule, packageFqName)?.let { return it }
        val owningModule = findContainingModule(packageFqName, preferredModule) ?: return null
        return findInModule(owningModule, packageFqName)
    }

    fun hasPackage(packageFqName: FqName): Boolean {
        return findContainingModule(packageFqName) != null
    }

    private fun findInModule(module: CaModule?, packageFqName: FqName): CjDecompiledFile? = when (module) {
        is CaLibraryModule -> psiProvider.findDecompiledFile(module, packageFqName)
        is CaBuiltinsModule -> psiProvider.findDecompiledFile(module, packageFqName)
        else -> null
    }
}
