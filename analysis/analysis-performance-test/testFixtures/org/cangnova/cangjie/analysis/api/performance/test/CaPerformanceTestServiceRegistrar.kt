@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.performance.test

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.util.registry.Registry
import org.cangnova.cangjie.analysis.api.platform.statistics.CangJieOpenTelemetryProvider
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.PluginStructureProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * 性能测试宿主的服务注册器：把统计链路需要、但 CFIR 描述符不提供的部分补齐。
 *
 * 1. 加载 `cangjie-analysis-api-platform-interface.xml`：其中的 `<registryKey>` 声明
 *    （`cangjie.analysis.statistics`）由宿主从描述符登记，因此这里拿得到真实 key；
 * 2. 把统计开关置为 `true`：等价于用户在 IDE 里修改该 registry 值；
 * 3. 注册 SDK 支撑的 [CangJieOpenTelemetryProvider]：IDE 描述符里注册的是
 *    `CangJieGlobalOpenTelemetryProvider`，测试改为指向内存 reader。
 *
 * 不在这里创建 [org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService]：
 * 它由 `cangjie-low-level-api-cfir.xml` 声明，已随 `cangjie-analysis-api-cfir.xml` 一并登记。
 */
class CaPerformanceTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 登记统计开关并打开统计。
     */
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        PluginStructureProvider.registerApplicationServices(application, PLATFORM_INTERFACE_PLUGIN_XML)
        Registry.get(STATISTICS_REGISTRY_KEY).setValue("true")
    }

    /**
     * 注册 SDK 支撑的 OpenTelemetry provider。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerService(CangJieOpenTelemetryProvider::class.java, CaPerformanceTestOpenTelemetryProvider())
    }

    /**
     * 稳定的注册器名称，便于失败日志识别当前测试宿主。
     */
    override fun toString(): String = "CaPerformanceTestServiceRegistrar"

    private companion object {
        /**
         * 声明统计开关的描述符。
         */
        private const val PLATFORM_INTERFACE_PLUGIN_XML = "META-INF/analysis-api/cangjie-analysis-api-platform-interface.xml"

        /**
         * 统计开关 registry key，与生产描述符一致。
         */
        private const val STATISTICS_REGISTRY_KEY = "cangjie.analysis.statistics"
    }
}