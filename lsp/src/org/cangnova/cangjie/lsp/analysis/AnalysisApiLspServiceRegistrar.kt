package org.cangnova.cangjie.lsp.analysis

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieAnnotationsResolverFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProviderMerger
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.api.platform.restrictedAnalysis.CaRestrictedAnalysisService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderFactory
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderMerger
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneAnnotationsResolverFactory
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneDeclarationProviderFactory
import org.cangnova.cangjie.analysis.api.standalone.base.declarations.CangJieStandaloneDeclarationProviderMerger
import org.cangnova.cangjie.analysis.api.standalone.base.packages.CangJieStandalonePackageProviderFactory
import org.cangnova.cangjie.analysis.api.standalone.base.packages.CangJieStandalonePackageProviderMerger
import org.cangnova.cangjie.lsp.CangjieLspEnvironment

/**
 * LSP 运行时对 Analysis API 的统一装配入口。
 *
 * 这里负责两件事：
 * 1. 把 Analysis API/CFIR/references 模块的 XML service 描述注册进当前 headless IntelliJ 容器；
 * 2. 为 LSP 文档快照注册平台侧 project structure / modification / scope refiner 服务。
 *
 * 这样 LSP 与 Analysis API 的耦合集中在一处，不会散落到 server、document store 和具体能力实现里。
 */
internal object AnalysisApiLspServiceRegistrar {
    private val analysisPluginXmls = listOf(
        "META-INF/analysis-api/cangjie-analysis-api-cfir.xml",
        "META-INF/analysis-api/cangjie-cj-references.xml",
    )

    @OptIn(CaPlatformInterface::class)
    fun register(environment: CangjieLspEnvironment) {
        val application = environment.coreEnvironment.applicationEnvironment.application as? MockApplication
            ?: error("LSP Analysis API 集成要求使用 MockApplication 容器")
        val project = environment.project as? MockProject
            ?: error("LSP Analysis API 集成要求使用 MockProject 容器")

        analysisPluginXmls.forEach { pluginXmlPath ->
            PluginStructureProvider.registerApplicationServices(application, pluginXmlPath)
            PluginStructureProvider.registerProjectServices(project, pluginXmlPath)
        }

        /*
         * LSP 运行时要保留自己的 project structure / modification 服务实现，
         * 但 low-level session 仍然需要 standalone 平台提供 declaration/package/annotation provider。
         * 这里按 Kotlin `AnalysisApiBaseTestServiceRegistrar` 的 owner 边界手工补齐，不整包覆盖 standalone XML。
         */
        project.registerService(
            CangJieAnnotationsResolverFactory::class.java,
            CangJieStandaloneAnnotationsResolverFactory::class.java,
        )
        project.registerService(
            CangJieDeclarationProviderFactory::class.java,
            CangJieStandaloneDeclarationProviderFactory(project),
        )
        project.registerService(
            CangJieDeclarationProviderMerger::class.java,
            CangJieStandaloneDeclarationProviderMerger::class.java,
        )
        project.registerService(
            CangJiePackageProviderFactory::class.java,
            CangJieStandalonePackageProviderFactory(project),
        )
        project.registerService(
            CangJiePackageProviderMerger::class.java,
            CangJieStandalonePackageProviderMerger::class.java,
        )

        project.registerService(
            AnalysisApiLspProjectStructureState::class.java,
            AnalysisApiLspProjectStructureState::class.java,
        )
        project.registerService(
            CaAnalysisPermissionChecker::class.java,
            AnalysisApiLspPermissionChecker::class.java,
        )
        project.registerService(
            CangJieProjectStructureProvider::class.java,
            AnalysisApiLspProjectStructureProvider::class.java,
        )
        project.registerService(
            CaModuleProvider::class.java,
            AnalysisApiLspModuleProvider::class.java,
        )
        project.registerService(
            CaModificationTracker::class.java,
            AnalysisApiLspModificationTracker::class.java,
        )
        project.registerService(
            CaSessionInvalidationService::class.java,
            AnalysisApiLspSessionInvalidationService::class.java,
        )
        project.registerService(
            CaRestrictedAnalysisService::class.java,
            AnalysisApiLspRestrictedAnalysisService::class.java,
        )
        project.registerService(
            CaPlatformSettings::class.java,
            AnalysisApiLspPlatformSettings::class.java,
        )
    }
}
