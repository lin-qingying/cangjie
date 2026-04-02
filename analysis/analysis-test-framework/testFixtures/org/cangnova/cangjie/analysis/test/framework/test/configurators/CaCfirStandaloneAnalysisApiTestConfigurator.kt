package org.cangnova.cangjie.analysis.test.framework.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructureFactory
import org.cangnova.cangjie.analysis.test.services.CaTestProjectStructureProvider
import org.cangnova.cangjie.test.services.TestServices
import java.nio.file.Path

/**
 * CFIR Analysis API 测试配置器。
 *
 * 当前仓库只有一套 CFIR 前端实现，因此该配置器先对齐 Kotlin 的
 * `Standalone + FIR` 路径，负责把 `analysis-api`、`analysis-api-impl-base`
 * 与 `analysis-api-cfir` 的服务链路完整挂到 `MockProject` 中。
 */
object CaCfirStandaloneAnalysisApiTestConfigurator : AnalysisApiTestConfigurator() {
    private const val ANALYSIS_API_PLUGIN_XML = "META-INF/analysis-api/cangjie-analysis-api-cfir.xml"

    override val analyseInDependentSession: Boolean = false

    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(CaCfirAnalysisApiServiceRegistrar)

    override fun createModules(
        testDataPath: Path,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure {
        return CjTestModuleStructureFactory.createFromTestDataFile(testDataPath, testServices, project)
    }

    /**
     * CFIR Analysis API 测试服务注册器。
     *
     * 这里直接复用模块 XML 描述，把与生产模块一致的 service wiring 带进测试环境，
     * 避免测试环境和真实模块装配出现两套分叉逻辑。
     */
    private object CaCfirAnalysisApiServiceRegistrar : AnalysisApiTestServiceRegistrar() {
        override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
            PluginStructureProvider.registerApplicationServices(application, ANALYSIS_API_PLUGIN_XML)
        }

        override fun registerProjectServices(project: MockProject, testServices: TestServices) {
            PluginStructureProvider.registerProjectServices(project, ANALYSIS_API_PLUGIN_XML)
            project.registerService(CaProjectStructureProvider::class.java, CaTestProjectStructureProvider::class.java)
        }
    }
}
