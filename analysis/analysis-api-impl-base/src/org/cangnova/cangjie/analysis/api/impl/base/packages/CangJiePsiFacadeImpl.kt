@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.impl.base.packages

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CangJiePsiFacade
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.packgae.AbstractCangJiePackage
import org.cangnova.cangjie.psi.packgae.CangJiePackage

/** 包 PSI 平台入口：源码和二进制文件统一经模块自己的 declaration provider 查询。 */
class CangJiePsiFacadeImpl(private val project: Project) : CangJiePsiFacade() {
    override fun findPackage(fqName: FqName, searchScope: GlobalSearchScope, organizationName: Name?): CangJiePackage? {
        if (packageFiles(fqName, searchScope, organizationName).isEmpty()) return null
        return PlatformPackage(PsiManager.getInstance(project), fqName, organizationName, searchScope, this)
    }

    override fun findPackage(fqName: FqName, searchScope: GlobalSearchScope): CangJiePackage? =
        findPackage(fqName, searchScope, null)

    override fun findPackage(fqName: FqName): CangJiePackage? = findPackage(fqName, GlobalSearchScope.allScope(project), null)
    override fun findPackage(fqName: String): CangJiePackage? = findPackage(FqName(fqName))

    /** 包由自身和子包的实际文件共同建立，目录布局不参与包名推断。 */
    private fun packageFiles(fqName: FqName, scope: GlobalSearchScope, organization: Name?): List<CjFile> {
        val factory = CangJieDeclarationProviderFactory.getInstance(project)
        return CaModuleProvider.getInstance(project).allModules.flatMap { module ->
            val provider = factory.createDeclarationProvider(scope.intersectWith(module.contentScope), module)
            provider.getPackageFiles(fqName, organization, includeSubpackages = true)
        }.distinct()
    }

    override fun processPackageDirectories(
        psiPackage: CangJiePackage,
        scope: GlobalSearchScope,
        consumer: Processor<in PsiDirectory>,
        includeLibrarySources: Boolean,
    ): Boolean {
        val packageScope = (psiPackage as PlatformPackage).searchScope.intersectWith(scope)
        val name = FqName(psiPackage.qualifiedName)
        val directories = linkedSetOf<PsiDirectory>()
        for (file in packageFiles(name, packageScope, psiPackage.organizationName)) {
            file.containingDirectory?.let(directories::add)
        }
        return directories.all { consumer.process(it) }
    }

    /** 真正的包目录容器；多根包保持聚合身份，导航不任意选择首个文件。 */
    private class PlatformPackage(
        manager: PsiManager,
        private val fqName: FqName,
        override val organizationName: Name?,
        val searchScope: GlobalSearchScope,
        private val packageFacade: CangJiePsiFacadeImpl,
    ) : AbstractCangJiePackage(manager, fqName.asString()) {
        override fun getAllDirectories(scope: GlobalSearchScope): MutableCollection<PsiDirectory> {
            val directories = linkedSetOf<PsiDirectory>()
            packageFacade.processPackageDirectories(this, scope, Processor { directories += it; true }, scope.isForceSearchingInLibrarySources)
            return directories
        }

        override fun findPackage(qName: String): AbstractCangJiePackage? =
            packageFacade.findPackage(FqName(qName), searchScope, organizationName) as? AbstractCangJiePackage

        override fun isValid(): Boolean = !project.isDisposed &&
            packageFacade.packageFiles(fqName, searchScope, organizationName).isNotEmpty()

        override fun navigate(requestFocus: Boolean) {
            ProjectView.getInstance(project).selectPsiElement(this, requestFocus)
        }

        override fun equals(other: Any?): Boolean = other is PlatformPackage &&
            project == other.project && fqName == other.fqName && organizationName == other.organizationName && searchScope == other.searchScope

        override fun hashCode(): Int = 31 * (31 * fqName.hashCode() + (organizationName?.hashCode() ?: 0)) + searchScope.hashCode()
    }
}
