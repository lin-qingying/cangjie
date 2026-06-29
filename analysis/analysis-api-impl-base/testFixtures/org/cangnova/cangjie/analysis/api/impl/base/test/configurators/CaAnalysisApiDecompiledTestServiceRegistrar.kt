package org.cangnova.cangjie.analysis.api.impl.base.test.configurators

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.psi.FileTypeFileViewProviders
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.decompiled.psi.CangJieDecompiledFileViewProviderFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.test.services.TestServices

/**
 * `.cjo` decompiled PSI 测试宿主注册器。
 *
 * 该注册器对齐 Kotlin `AnalysisApiBaseTestServiceRegistrar` 中 decompiled 相关职责：
 * 注册 `.cjo` file view provider、`CjoFileDecompilers` 扩展以及 builtins/decompiled 服务。
 */
object CaAnalysisApiDecompiledTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * decompiled Analysis API 插件描述文件路径。
     *
     * 该 XML 由 `PluginStructureProvider` 读取，用于把 decompiled PSI 与 builtins 相关服务注册到 mock 宿主。
     */
    private const val DECOMPILED_PLUGIN_XML = "META-INF/analysis-api/cangjie-analysis-decompiled.xml"

    /**
     * 注册 decompiled 测试需要的应用级服务。
     *
     * 该覆盖方法保留 testServices 入口以匹配测试框架扩展点，并委托到无 testServices 的共享注册实现。
     */
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        registerApplicationServices(application)
    }

    /**
     * 注册 decompiled 测试需要的项目级服务。
     *
     * 该覆盖方法保留 testServices 入口以匹配测试框架扩展点，并委托到无 testServices 的共享注册实现。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        registerProjectServices(project)
    }

    /**
     * 在 mock application 中注册 `.cjo` 反编译相关服务。
     *
     * 该方法既安装插件 XML 中声明的应用级服务，也为内置文件类型绑定 decompiled file view provider，
     * 使测试中的 `.cjo` 或 builtins 文件可以走真实的 PSI view provider 路径。
     */
    fun registerApplicationServices(application: MockApplication) {
        PluginStructureProvider.registerApplicationServices(application, DECOMPILED_PLUGIN_XML)
        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(
            CangJieBuiltInFileType,
            CangJieDecompiledFileViewProviderFactory(),
        )
    }

    /**
     * 在 mock project 中注册 `.cjo` 反编译相关服务。
     *
     * 该方法加载插件 XML 中的项目级服务，补齐 decompiled PSI、stub 与项目结构测试所需的宿主能力。
     */
    fun registerProjectServices(project: MockProject) {
        PluginStructureProvider.registerProjectServices(project, DECOMPILED_PLUGIN_XML)
    }
}
