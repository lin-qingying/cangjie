package org.cangnova.cangjie.analysis.api.cfir.statistics

import io.opentelemetry.api.GlobalOpenTelemetry
import org.cangnova.cangjie.analysis.api.platform.statistics.CangJieGlobalOpenTelemetryProvider
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * [CangJieGlobalOpenTelemetryProvider] 的取值测试。
 *
 * 平台可能没有初始化 OpenTelemetry SDK；此时全局实例退化为 noop，
 * provider 必须原样返回它，不能抛错。
 *
 * [GlobalOpenTelemetry.get] 的首次调用与后续调用返回的不是同一个引用：首次调用在没有找到
 * SDK 自动配置类时返回 noop 实例本身，同时把包装该实例的 `ObfuscatedOpenTelemetry` 写入全局字段；
 * 之后所有调用都直接返回全局字段中的包装实例。两者行为等价，因此引用断言前先预热一次。
 */
class CangJieGlobalOpenTelemetryProviderTest {
    /**
     * provider 必须返回平台全局 OpenTelemetry 实例，且多次取值保持稳定。
     */
    @Test
    fun returnsGlobalOpenTelemetryInstance() {
        val warmUp = GlobalOpenTelemetry.get()
        assertNotNull(warmUp, "预热读取全局实例不应返回 null。")

        val provider = CangJieGlobalOpenTelemetryProvider()

        val first = provider.openTelemetry
        assertSame(first, GlobalOpenTelemetry.get(), "provider 必须返回平台全局 OpenTelemetry 实例。")
        assertSame(first, provider.openTelemetry, "provider 多次取值必须返回同一个全局实例。")

        val meter = first.getMeter("cangjie.analysis.test")
        assertNotNull(meter.counterBuilder("test.counter").build(), "全局实例必须可用于创建 meter 与 counter。")
    }

    /**
     * 未初始化 SDK 的平台上，全局实例至少是 noop：取 meter、建 counter 都不抛错。
     */
    @Test
    fun worksWithoutConfiguredSdk() {
        val meter = CangJieGlobalOpenTelemetryProvider().openTelemetry.getMeter("cangjie.analysis.test")

        assertNotNull(meter.counterBuilder("test.counter").build(), "全局实例必须可用于创建 meter 与 counter。")
    }
}
