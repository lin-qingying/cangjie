package org.cangnova.cangjie.analysis.api.platform.statistics

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * 以平台全局 [GlobalOpenTelemetry] 实例作为 Analysis API 统计后端。
 *
 * 对齐 Kotlin `KotlinGlobalOpenTelemetryProvider`。采用该实现的前提是平台把
 * OpenTelemetry SDK 初始化为全局实例；若平台没有初始化，[GlobalOpenTelemetry.get]
 * 会退化为 noop 实现，统计调用不产生上报，也不会抛错。
 */
@CaPlatformInterface
class CangJieGlobalOpenTelemetryProvider : CangJieOpenTelemetryProvider {
    /**
     * 平台初始化后的全局 OpenTelemetry 实例。
     */
    override val openTelemetry: OpenTelemetry
        get() = GlobalOpenTelemetry.get()
}
