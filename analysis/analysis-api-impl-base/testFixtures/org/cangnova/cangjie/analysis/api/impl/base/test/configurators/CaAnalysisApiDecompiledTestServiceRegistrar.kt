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
    private const val DECOMPILED_PLUGIN_XML = "META-INF/analysis-api/cangjie-analysis-decompiled.xml"

    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        registerApplicationServices(application)
    }

    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        registerProjectServices(project)
    }

    fun registerApplicationServices(application: MockApplication) {
        PluginStructureProvider.registerApplicationServices(application, DECOMPILED_PLUGIN_XML)
        FileTypeFileViewProviders.INSTANCE.addExplicitExtension(
            CangJieBuiltInFileType,
            CangJieDecompiledFileViewProviderFactory(),
        )
    }

    fun registerProjectServices(project: MockProject) {
        PluginStructureProvider.registerProjectServices(project, DECOMPILED_PLUGIN_XML)
    }
}
