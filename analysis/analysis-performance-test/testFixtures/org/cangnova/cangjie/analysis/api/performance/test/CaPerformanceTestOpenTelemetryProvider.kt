@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.api.performance.test

import io.opentelemetry.api.OpenTelemetry
import org.cangnova.cangjie.analysis.api.platform.statistics.CangJieOpenTelemetryProvider

/**
 * 把统计后端指向 SDK 实例的测试宿主实现。
 *
 * 角色对齐 IDE 描述符中的 `CangJieGlobalOpenTelemetryProvider`，差别只在于实例来源：
 * IDE 取平台全局 OpenTelemetry，测试取 [CaPerformanceTestTelemetry] 里显式构建的 SDK，
 * 从而避免测试之间通过全局单例互相影响。
 */
class CaPerformanceTestOpenTelemetryProvider : CangJieOpenTelemetryProvider {
    /**
     * SDK 实例。
     */
    override val openTelemetry: OpenTelemetry
        get() = CaPerformanceTestTelemetry.openTelemetry
}