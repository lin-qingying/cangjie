@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.standalone.base.packages

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieCompositePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieEmptyPackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderFactory
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderMerger
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneSourceFileCollector
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * Standalone 平台的包 provider 工厂。
 *
 * 对齐 Kotlin `KotlinStandalonePackageProviderFactory` 的框架职责：
 * 工厂基于 standalone 可见源码文件构造包存在性与子包查询视图。
 */
class CangJieStandalonePackageProviderFactory(
    project: Project,
) : CangJiePackageProviderFactory {
    private val fileCollector = CangJieStandaloneSourceFileCollector(project)

    override fun createPackageProvider(searchScope: GlobalSearchScope): CangJiePackageProvider {
        val files = fileCollector.collect(searchScope)
        if (files.isEmpty()) return CangJieEmptyPackageProvider

        return CangJieStandalonePackageProvider(files)
    }
}

/**
 * Standalone 平台的包 provider 合并器。
 */
class CangJieStandalonePackageProviderMerger : CangJiePackageProviderMerger {
    override fun merge(providers: List<CangJiePackageProvider>): CangJiePackageProvider {
        return CangJieCompositePackageProvider.create(providers)
    }
}

private class CangJieStandalonePackageProvider(
    files: List<CjFile>,
) : CangJiePackageProvider {
    private val packageToSubpackages: Map<FqName, Set<Name>> = buildPackageToSubpackages(files)

    override fun doesPackageExist(packageFqName: FqName): Boolean {
        return packageFqName.isRoot || packageFqName in packageToSubpackages
    }

    override fun getSubpackageNames(packageFqName: FqName): Set<Name> {
        return packageToSubpackages[packageFqName].orEmpty()
    }

    private fun buildPackageToSubpackages(files: List<CjFile>): Map<FqName, Set<Name>> {
        val packages = linkedMapOf<FqName, MutableSet<Name>>()
        for (file in files) {
            var currentPackage = FqName.ROOT
            for (subpackage in file.packageFqName.pathSegments()) {
                packages.getOrPut(currentPackage, ::linkedSetOf).add(subpackage)
                currentPackage = currentPackage.child(subpackage)
            }
            packages.computeIfAbsent(currentPackage) { linkedSetOf() }
        }
        return packages
    }
}
