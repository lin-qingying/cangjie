package org.cangnova.cangjie.analysis.api.platform.packages

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaComposableProviderMerger
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent

@CaPlatformInterface
interface CangJiePackageProviderFactory : CaPlatformComponent {
    fun createPackageProvider(searchScope: GlobalSearchScope): CangJiePackageProvider

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJiePackageProviderFactory = project.service()
    }
}

@CaPlatformInterface
interface CangJiePackageProviderMerger : CaComposableProviderMerger<CangJiePackageProvider>, CaPlatformComponent {
    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJiePackageProviderMerger = project.service()
    }
}

@CaPlatformInterface
fun Project.createPackageProvider(searchScope: GlobalSearchScope): CangJiePackageProvider =
    CangJiePackageProviderFactory.getInstance(this).createPackageProvider(searchScope)

@CaPlatformInterface
fun Project.mergePackageProviders(packageProviders: List<CangJiePackageProvider>): CangJiePackageProvider =
    CangJiePackageProviderMerger.getInstance(this).merge(packageProviders)
