package org.cangjie.analysis.api.cfir.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangjie.analysis.api.impl.base.projectStructure.PluginStructureProvider
import org.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangjie.test.services.TestServices

/**
 * CFIR 服务注册器（对齐 Kotlin 的 AnalysisApiFirTestServiceRegistrar）。
 *
 * 从 XML 描述文件加载并注册 CFIR 实现的 Analysis API 服务：
 * - 包含 impl-base 的基础服务（权限、生命周期等）
 * - 包含 CFIR 特有的服务（CaCfirSessionProvider 等）
 */
object AnalysisApiCFirServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    private const val PLUGIN_RELATIVE_PATH = "/META-INF/analysis-api/cangjie-analysis-api-cfir.xml"

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        PluginStructureProvider.registerApplicationServices(application, PLUGIN_RELATIVE_PATH)
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        PluginStructureProvider.registerProjectServices(project, PLUGIN_RELATIVE_PATH)
    }
}
