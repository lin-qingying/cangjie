package org.cangnova.cangjie.lsp.testkit

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.lsp.CangjieLspEnvironment

/**
 * 为 LSP 集成测试创建环境，并按插件 XML 的真实加载路径注册服务。
 */
object ServiceLoadedLspTestEnvironment {
    fun create(
        mode: CangJieCoreEnvironmentMode = CangJieCoreEnvironmentMode.UnitTest,
        pluginXmlPaths: List<String> = listOf(ANALYSIS_API_IMPL_BASE_PLUGIN_XML),
    ): CangjieLspEnvironment {
        val environment = CangjieLspEnvironment.create(mode)
        val application = environment.coreEnvironment.applicationEnvironment.application as? MockApplication
            ?: error("LSP test environment requires MockApplication")
        val project = environment.project as? MockProject
            ?: error("LSP test environment requires MockProject")

        pluginXmlPaths.forEach { pluginXmlPath ->
            PluginStructureProvider.registerApplicationServices(application, pluginXmlPath)
            PluginStructureProvider.registerProjectServices(project, pluginXmlPath)
        }

        return environment
    }

    const val ANALYSIS_API_IMPL_BASE_PLUGIN_XML: String = "META-INF/analysis-api/cangjie-analysis-api-impl-base.xml"
}
