@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolverFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderMerger
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderFactory
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderMerger
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneAnnotationsResolverFactory
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneDeclarationProviderMerger
import org.cangnova.cangjie.analysis.api.standalone.base.packages.CangJieStandalonePackageProviderFactory
import org.cangnova.cangjie.analysis.api.standalone.base.packages.CangJieStandalonePackageProviderMerger
import org.cangnova.cangjie.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.cangnova.cangjie.analysis.test.framework.services.configuration.libraryIndexingConfiguration
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * Analysis API 基础测试服务注册器。
 *
 * 对齐 Kotlin `AnalysisApiBaseTestServiceRegistrar` 的 project-model 服务职责：
 * 在测试模块结构安装完成后，为 low-level session 注册 declaration/package/annotation provider。
 */
object CaAnalysisApiBaseTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerPlatformSettings(testServices)
    }

    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        project.apply {
            registerService(
                CangJieAnnotationsResolverFactory::class.java,
                CangJieStandaloneAnnotationsResolverFactory::class.java,
            )
            registerService(
                CangJieDeclarationProviderFactory::class.java,
                CangJieStandaloneDeclarationProviderFactory(project),
            )
            registerService(
                CangJieDeclarationProviderMerger::class.java,
                CangJieStandaloneDeclarationProviderMerger::class.java,
            )
            registerService(
                CangJiePackageProviderFactory::class.java,
                CangJieStandalonePackageProviderFactory(project),
            )
            registerService(
                CangJiePackageProviderMerger::class.java,
                CangJieStandalonePackageProviderMerger::class.java,
            )
        }
    }

    private fun MockProject.registerPlatformSettings(testServices: TestServices) {
        val deserializedDeclarationsOrigin = when (testServices.libraryIndexingConfiguration?.binaryLibraryIndexingMode) {
            AnalysisApiBinaryLibraryIndexingMode.INDEX_STUBS -> CaDeserializedDeclarationsOrigin.STUBS
            AnalysisApiBinaryLibraryIndexingMode.NO_INDEXING,
            null,
            -> CaDeserializedDeclarationsOrigin.BINARIES
        }

        val settings = object : CaPlatformSettings {
            override val deserializedDeclarationsOrigin: CaDeserializedDeclarationsOrigin = deserializedDeclarationsOrigin
        }

        registerService(CaPlatformSettings::class.java, settings)
    }
}
