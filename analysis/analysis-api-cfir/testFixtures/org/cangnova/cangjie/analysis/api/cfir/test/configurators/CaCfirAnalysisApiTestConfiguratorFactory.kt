package org.cangnova.cangjie.analysis.api.cfir.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiBaseTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiIdeModeTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieModuleDependentsProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureFactory
import org.cangnova.cangjie.analysis.test.framework.services.DependencyKindModuleStructureTransformer
import org.cangnova.cangjie.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.cangnova.cangjie.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.cangnova.cangjie.analysis.test.framework.services.libraries.configureLibraryCompilationSupport
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.analysis.test.services.CaTestModificationTracker
import org.cangnova.cangjie.analysis.test.services.CaTestModuleDependentsProvider
import org.cangnova.cangjie.analysis.test.services.CaTestModuleProvider
import org.cangnova.cangjie.analysis.test.services.CaTestPlatformState
import org.cangnova.cangjie.analysis.test.services.CaTestProjectStructureProvider
import org.cangnova.cangjie.analysis.test.services.CaTestSessionInvalidationService
import org.cangnova.cangjie.test.services.TestServices

/**
 * CFIR Analysis API IDE-mode test configurator factory.
 *
 * 对齐 Kotlin `AnalysisApiFirTestConfiguratorFactory`：
 * 本工厂只负责 IDE mode；standalone mode 的 factory 归 standalone 模块持有。
 */
object CaCfirAnalysisApiTestConfiguratorFactory : AnalysisApiTestConfiguratorFactory() {
    override fun createConfigurator(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfigurator {
        requireSupported(data)
        return CaCfirConfiguredAnalysisApiTestConfigurator(
            moduleKind = data.moduleKind,
            serviceRegistrars = listOf(
                CaAnalysisApiBaseTestServiceRegistrar,
                CaAnalysisApiDecompiledTestServiceRegistrar,
                CaCfirConfiguredAnalysisApiTestConfigurator.CaCfirAnalysisApiServiceRegistrar(),
                CaAnalysisApiIdeModeTestServiceRegistrar,
            ),
            analyseInDependentSession = data.analysisSessionMode == AnalysisSessionMode.Dependent,
        )
    }

    override fun supportMode(data: AnalysisApiTestConfiguratorFactoryData): Boolean {
        if (data.frontend != FrontendKind.Cfir) return false
        if (data.analysisApiMode != AnalysisApiMode.Ide) return false

        return when (data.moduleKind) {
            TestModuleKind.Source,
            TestModuleKind.CodeFragment -> true

            TestModuleKind.LibraryBinary,
            TestModuleKind.LibraryBinaryDecompiled,
            TestModuleKind.LibrarySource -> data.analysisSessionMode == AnalysisSessionMode.Normal

            TestModuleKind.ScriptSource,
            TestModuleKind.NotUnderContentRoot,
            TestModuleKind.NotUnderContentRootWithDependencies -> false
        }
    }
}

/**
 * CFIR Analysis API configured test configurator.
 *
 * 这里先收敛成“可组合宿主”：
 * owner 模块各自给出 registrar 列表，
 * 避免在 CFIR 模块里继续混入 standalone mode 的所有权。
 */
open class CaCfirConfiguredAnalysisApiTestConfigurator(
    private val moduleKind: TestModuleKind,
    final override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>,
    final override val analyseInDependentSession: Boolean,
) : AnalysisApiTestConfigurator() {
    @OptIn(TestInfrastructureInternals::class)
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        when (moduleKind) {
            TestModuleKind.Source,
            TestModuleKind.CodeFragment,
            -> {
                builder.useAdditionalService {
                    AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.INDEX_STUBS)
                }
                builder.useModuleStructureTransformers({ DependencyKindModuleStructureTransformer })
                builder.configureLibraryCompilationSupport()
            }

            TestModuleKind.LibraryBinaryDecompiled -> {
                builder.useAdditionalService {
                    AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.INDEX_STUBS)
                }
                builder.configureLibraryCompilationSupport()
            }

            else -> Unit
        }
    }

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure {
        return CjTestModuleStructureFactory.createProjectStructureByTestStructure(
            testModuleStructure = moduleStructure,
            testServices = testServices,
            project = project,
        )
    }

    /**
     * CFIR Analysis API 测试宿主的服务注册器。
     */
    class CaCfirAnalysisApiServiceRegistrar : org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar() {
        override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
            PluginStructureProvider.registerApplicationServices(application, ANALYSIS_API_PLUGIN_XML)
            PluginStructureProvider.registerApplicationServices(application, CJ_REFERENCES_PLUGIN_XML)
        }

        override fun registerProjectServices(project: MockProject, testServices: TestServices) {
            PluginStructureProvider.registerProjectServices(project, ANALYSIS_API_PLUGIN_XML)
            PluginStructureProvider.registerProjectServices(project, CJ_REFERENCES_PLUGIN_XML)

            project.registerService(CaTestPlatformState::class.java, CaTestPlatformState::class.java)
            project.registerService(CangJieProjectStructureProvider::class.java, CaTestProjectStructureProvider::class.java)
            project.registerService(CaModuleProvider::class.java, CaTestModuleProvider::class.java)
            project.registerService(CangJieModuleDependentsProvider::class.java, CaTestModuleDependentsProvider::class.java)
            project.registerService(CaModificationTracker::class.java, CaTestModificationTracker::class.java)
            project.registerService(CaSessionInvalidationService::class.java, CaTestSessionInvalidationService::class.java)
        }

        override fun toString(): String = "CaCfirAnalysisApiServiceRegistrar"

        private companion object {
            private const val ANALYSIS_API_PLUGIN_XML = "META-INF/analysis-api/cangjie-analysis-api-cfir.xml"
            private const val CJ_REFERENCES_PLUGIN_XML = "META-INF/analysis-api/cangjie-cj-references.xml"
        }
    }
}
