package org.cangnova.cangjie.analysis.api.impl.base.platform.packages

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.impl.base.platform.CaIdeScopeCangJieFileCollector
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieCompositePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieEmptyPackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderFactory
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderMerger
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * IDE 平台的主包 provider 工厂。
 *
 * 组织方式参考 Kotlin `KotlinStandalonePackageProviderFactory`：
 * 包可见性从作用域内文件集合集中推导，而不是从调用方零散拼接。
 */
@OptIn(CaPlatformInterface::class)
class CaIdePackageProviderFactory(
    private val project: Project,
) : CangJiePackageProviderFactory {
    override fun createPackageProvider(searchScope: GlobalSearchScope): CangJiePackageProvider {
        val files = CaIdeScopeCangJieFileCollector(project).collect(searchScope)
        if (files.isEmpty()) return CangJieEmptyPackageProvider

        return CaIdeScopePackageProvider(files)
    }
}

/**
 * IDE 平台的包 provider merger。
 */
@OptIn(CaPlatformInterface::class)
class CaIdePackageProviderMerger : CangJiePackageProviderMerger {
    override fun merge(providers: List<CangJiePackageProvider>): CangJiePackageProvider {
        return CangJieCompositePackageProvider.create(providers)
    }
}

/**
 * 当前作用域下的包视图。
 *
 * 只接受由作用域内 `CjFile` 推导出的包事实，
 * 并把父包链显式补齐，保证 `doesPackageExist` 与 `getSubpackageNames` 的判断一致。
 */
@OptIn(CaPlatformInterface::class)
private class CaIdeScopePackageProvider(
    files: List<CjFile>,
) : CangJiePackageProvider {
    private val kotlinPackageToSubpackages: Map<FqName, Set<Name>> = run {
        val packages = linkedMapOf<FqName, MutableSet<Name>>()
        for (file in files) {
            var currentPackage = FqName.ROOT
            for (subpackage in file.packageFqName.pathSegments()) {
                packages.getOrPut(currentPackage, ::linkedSetOf).add(subpackage)
                currentPackage = currentPackage.child(subpackage)
            }
            packages.computeIfAbsent(currentPackage) { linkedSetOf() }
        }
        packages
    }

    override fun doesPackageExist(packageFqName: FqName): Boolean {
        return packageFqName.isRoot || packageFqName in kotlinPackageToSubpackages
    }

    override fun getSubpackageNames(packageFqName: FqName): Set<Name> {
        return kotlinPackageToSubpackages[packageFqName].orEmpty()
    }
}
