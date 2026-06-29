package org.cangnova.cangjie.analysis.api.platform.packages

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaComposableProviderMerger
import org.cangnova.cangjie.analysis.api.platform.CaPlatformComponent

/**
 * 包 provider 平台工厂。
 */
@CaPlatformInterface
interface CangJiePackageProviderFactory : CaPlatformComponent {
    /**
     * 为指定搜索范围创建包 provider。
     */
    fun createPackageProvider(searchScope: GlobalSearchScope): CangJiePackageProvider

    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级包 provider 工厂服务。
         */
        fun getInstance(project: Project): CangJiePackageProviderFactory = project.service()
    }
}

/**
 * 包 provider 合并器。
 */
@CaPlatformInterface
interface CangJiePackageProviderMerger : CaComposableProviderMerger<CangJiePackageProvider>, CaPlatformComponent {
    @CaPlatformInterface
    companion object {
        /**
         * 获取项目级包 provider 合并器服务。
         */
        fun getInstance(project: Project): CangJiePackageProviderMerger = project.service()
    }
}

/**
 * 使用项目平台注册的工厂创建包 provider。
 */
@CaPlatformInterface
fun Project.createPackageProvider(searchScope: GlobalSearchScope): CangJiePackageProvider =
    CangJiePackageProviderFactory.getInstance(this).createPackageProvider(searchScope)

/**
 * 使用项目平台注册的合并器合并包 provider。
 */
@CaPlatformInterface
fun Project.mergePackageProviders(packageProviders: List<CangJiePackageProvider>): CangJiePackageProvider =
    CangJiePackageProviderMerger.getInstance(this).merge(packageProviders)
