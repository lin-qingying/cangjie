@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.packages

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieCompositePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieEmptyPackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderFactory
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderMerger
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneSourceFileCollector
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Standalone 平台的包 provider 工厂。
 *
 * 对齐 Kotlin `KotlinStandalonePackageProviderFactory` 的框架职责：
 * 工厂同时合并：
 * 1. source-like 文件的包事实；
 * 2. binary library roots 直接可读的包事实。
 */
class CangJieStandalonePackageProviderFactory(
    project: Project,
) : CangJiePackageProviderFactory {
    /**
     * 提供模块结构和 decompiled binary index 的 standalone project。
     */
    private val project = project

    /**
     * 用于从 source roots 和 library roots 收集包事实的文件收集器。
     */
    private val fileCollector = CangJieStandaloneSourceFileCollector(project)

    /**
     * 为指定搜索作用域创建 package provider。
     */
    override fun createPackageProvider(searchScope: GlobalSearchScope): CangJiePackageProvider {
        val packageNames = buildSet {
            fileCollector.collect(searchScope).mapTo(this) { it.packageFqName }
            addAll(collectLibraryPackageNames(searchScope))
        }
        if (packageNames.isEmpty()) return CangJieEmptyPackageProvider

        return CangJieStandalonePackageProvider(packageNames)
    }

    /**
     * 对齐 Kotlin standalone package provider 对 binary libraries 的处理：
     * package existence 只需要 package facts，必须直接从 `.cjo` binary header 读取，
     * 不能退回到 decompiled PSI / source-file collector。
     */
    private fun collectLibraryPackageNames(searchScope: GlobalSearchScope): Set<FqName> {
        val binaryIndex = CaDecompiledBinaryIndex.getInstance(project)
        return buildSet {
            CaModuleProvider.getInstance(project).allModules
                .filterIsInstance<CaLibraryModule>()
                .forEach { libraryModule ->
                    fileCollector.collectFromRoots(libraryModule.binaryRoots, searchScope)
                        .mapTo(this) { it.packageFqName }

                    binaryIndex.getBinaryFiles(libraryModule)
                        .asSequence()
                        .filter(searchScope::contains)
                        .mapNotNull(binaryIndex::readPackageFqName)
                        .forEach(::add)
                }
        }
    }
}

/**
 * Standalone 平台的包 provider 合并器。
 */
class CangJieStandalonePackageProviderMerger : CangJiePackageProviderMerger {
    /**
     * 将多个 package provider 合并为组合 provider。
     */
    override fun merge(providers: List<CangJiePackageProvider>): CangJiePackageProvider {
        return CangJieCompositePackageProvider.create(providers)
    }
}

/**
 * 基于已知包名集合的 standalone package provider。
 */
private class CangJieStandalonePackageProvider(
    packageNames: Set<FqName>,
) : CangJiePackageProvider {
    /**
     * 包到直接子包名的映射。
     */
    private val packageToSubpackages: Map<FqName, Set<Name>> = buildPackageToSubpackages(packageNames)

    /**
     * 判断指定包是否存在。
     */
    override fun doesPackageExist(packageFqName: FqName): Boolean {
        return packageFqName.isRoot || packageFqName in packageToSubpackages
    }

    /**
     * 返回指定包的直接子包短名集合。
     */
    override fun getSubpackageNames(packageFqName: FqName): Set<Name> {
        return packageToSubpackages[packageFqName].orEmpty()
    }

    /**
     * 从完整包名集合构建父包到直接子包的索引。
     */
    private fun buildPackageToSubpackages(packageNames: Set<FqName>): Map<FqName, Set<Name>> {
        val packages = linkedMapOf<FqName, MutableSet<Name>>()
        for (packageName in packageNames) {
            var currentPackage = FqName.ROOT
            for (subpackage in packageName.pathSegments()) {
                packages.getOrPut(currentPackage, ::linkedSetOf).add(subpackage)
                currentPackage = currentPackage.child(subpackage)
            }
            packages.computeIfAbsent(currentPackage) { linkedSetOf() }
        }
        return packages
    }
}
