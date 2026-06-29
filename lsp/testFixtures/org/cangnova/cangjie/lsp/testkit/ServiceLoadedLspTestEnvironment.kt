package org.cangnova.cangjie.lsp.testkit

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.lsp.CangjieLspEnvironment

/**
 * 为 LSP 集成测试创建环境，并按插件 XML 的真实加载路径注册服务。
 */
object ServiceLoadedLspTestEnvironment {
    /**
     * 创建带可选插件 XML 服务注册的 LSP 测试环境。
     *
     * 该方法先构造核心环境，再将指定插件描述中的 application/project 服务注册到 mock 容器，
     * 用于复现 IDE 插件加载路径下的 LSP 集成测试环境。
     */
    fun create(
        mode: CangJieCoreEnvironmentMode = CangJieCoreEnvironmentMode.UnitTest,
        pluginXmlPaths: List<String> = emptyList(),
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
}
