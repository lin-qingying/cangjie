package org.cangnova.cangjie.analysis.test.framework.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaTargetPlatform
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.permissions.CaAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaContentScopeRefiner
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.api.standalone.platform.CaStandaloneAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.test.services.CaTestIdeAnalysisPermissionChecker
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureFactory
import org.cangnova.cangjie.analysis.test.services.CaTestContentScopeRefiner
import org.cangnova.cangjie.analysis.test.services.CaTestModificationTracker
import org.cangnova.cangjie.analysis.test.services.CaTestModuleProvider
import org.cangnova.cangjie.analysis.test.services.CaTestPlatformState
import org.cangnova.cangjie.analysis.test.services.CaTestProjectStructureProvider
import org.cangnova.cangjie.analysis.test.services.CaTestSessionInvalidationService
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import java.nio.file.Path

/**
 * CFIR Analysis API 测试 configurator 工厂。
 *
 * 该工厂把前端、宿主模式、session 模式和模块种类折叠成具体 configurator。
 * 当前只支持 CFIR 前端，但保留了完整矩阵入口，便于 generated tests 统一展开。
 */
object CaCfirAnalysisApiTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        requireSupported(data)
        return CaCfirConfiguredAnalysisApiTestConfigurator(
            analysisApiMode = data.analysisApiMode,
            analyseInDependentSession = data.analysisSessionMode == AnalysisSessionMode.Dependent,
        )
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean {
        if (data.frontend != FrontendKind.Cfir) return false
        if (data.analysisApiMode == AnalysisApiMode.LspCompatible &&
            data.analysisSessionMode == AnalysisSessionMode.Dependent
        ) {
            return false
        }

        return when (data.moduleKind) {
            TestModuleKind.Source,
            TestModuleKind.ScriptSource,
            TestModuleKind.CodeFragment,
            TestModuleKind.NotUnderContentRoot -> true

            TestModuleKind.LibraryBinary,
            TestModuleKind.LibrarySource -> data.analysisSessionMode == AnalysisSessionMode.Normal
        }
    }
}

/**
 * 参数化的 CFIR Analysis API 测试 configurator。
 *
 * 三类宿主模式共用同一套测试 project-structure 协议，只在权限检查和目标平台标识上区分宿主行为。
 */
open class CaCfirConfiguredAnalysisApiTestConfigurator(
    private val analysisApiMode: AnalysisApiMode,
    final override val analyseInDependentSession: Boolean,
) : AnalysisApiTestConfigurator() {
    private val targetPlatform: CaTargetPlatform = when (analysisApiMode) {
        AnalysisApiMode.Ide -> CaTargetPlatform.IDE
        AnalysisApiMode.Standalone -> CaTargetPlatform.STANDALONE
        AnalysisApiMode.LspCompatible -> CaTargetPlatform.LSP
    }

    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(CaCfirAnalysisApiServiceRegistrar(analysisApiMode))

    override fun createModules(
        testDataPath: Path,
        testServices: TestServices,
        project: Project,
        additionalDirectives: List<DirectivesContainer>,
    ): CjTestModuleStructure {
        return CjTestModuleStructureFactory.createFromTestDataFile(
            testDataPath = testDataPath,
            testServices = testServices,
            project = project,
            targetPlatform = targetPlatform,
            additionalDirectives = additionalDirectives,
        )
    }

    /**
     * CFIR Analysis API 测试宿主的服务注册器。
     *
     * 三种宿主模式共用同一套测试 project-structure 状态源，
     * 只有权限模型在 IDE 与非 IDE 宿主之间存在差异。
     */
    private class CaCfirAnalysisApiServiceRegistrar(
        private val analysisApiMode: AnalysisApiMode,
    ) : AnalysisApiTestServiceRegistrar() {
        override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
            PluginStructureProvider.registerApplicationServices(application, ANALYSIS_API_PLUGIN_XML)
        }

        override fun registerProjectServices(project: MockProject, testServices: TestServices) {
            PluginStructureProvider.registerProjectServices(project, ANALYSIS_API_PLUGIN_XML)
            PluginStructureProvider.registerProjectServices(project, CJ_REFERENCES_PLUGIN_XML)

            project.registerService(CaTestPlatformState::class.java, CaTestPlatformState::class.java)
            when (analysisApiMode) {
                AnalysisApiMode.Ide -> {
                    // IDE 生产态权限规则保持不变；这里只为测试宿主显式放宽分析入口。
                    project.registerService(CaAnalysisPermissionChecker::class.java, CaTestIdeAnalysisPermissionChecker::class.java)
                }

                else -> {
                    project.registerService(CaAnalysisPermissionChecker::class.java, CaStandaloneAnalysisPermissionChecker::class.java)
                }
            }
            project.registerService(CaProjectStructureProvider::class.java, CaTestProjectStructureProvider::class.java)
            project.registerService(CaModuleProvider::class.java, CaTestModuleProvider::class.java)
            project.registerService(CaContentScopeRefiner::class.java, CaTestContentScopeRefiner::class.java)
            project.registerService(CaModificationTracker::class.java, CaTestModificationTracker::class.java)
            project.registerService(CaSessionInvalidationService::class.java, CaTestSessionInvalidationService::class.java)
        }

        override fun toString(): String = "CaCfirAnalysisApiServiceRegistrar(mode=${analysisApiMode.suffix})"

        private companion object {
            private const val ANALYSIS_API_PLUGIN_XML = "META-INF/analysis-api/cangjie-analysis-api-cfir.xml"
            private const val CJ_REFERENCES_PLUGIN_XML = "META-INF/analysis-api/cangjie-cj-references.xml"
        }
    }
}
