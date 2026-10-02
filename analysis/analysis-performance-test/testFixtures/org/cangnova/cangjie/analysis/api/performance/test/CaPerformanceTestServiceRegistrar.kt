@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.performance.test

import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import org.cangnova.cangjie.analysis.api.platform.statistics.CangJieOpenTelemetryProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsService
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * 性能测试宿主的服务注册器：补齐统计链路在 mock 宿主中缺失的部分。
 *
 * 1. [CaPerformanceStatisticsBootstrap.enable]：打开统计开关；
 * 2. 注册 SDK 支撑的 `CangJieOpenTelemetryProvider`，否则 `LLStatisticsService.getInstance` 恒为 null；
 * 3. 兜底注册 `LLStatisticsService`：正常由 `cangjie-low-level-api-cfir.xml` 注册，
 *    这里只在描述符未生效时补上，避免统计域缺失。
 */
class CaPerformanceTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    /**
     * 统计开关是 JVM 级状态，必须在读取它的第一个服务被创建之前打开。
     */
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        CaPerformanceStatisticsBootstrap.enable()
    }

    /**
     * 注册统计后端，并确保统计服务存在。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.registerService(CangJieOpenTelemetryProvider::class.java, CaPerformanceTestOpenTelemetryProvider())
        if (project.getService(LLStatisticsService::class.java) == null) {
            project.registerService(LLStatisticsService::class.java, LLStatisticsService(project))
        }
    }

    /**
     * 稳定的注册器名称，便于失败日志识别当前测试宿主。
     */
    override fun toString(): String = "CaPerformanceTestServiceRegistrar"
}