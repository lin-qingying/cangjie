package org.cangnova.cangjie.analysis.api.performance.test

import com.intellij.diagnostic.LoadingState
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.registry.RegistryKeyDescriptor

/**
 * 在测试宿主中打开 Analysis API 统计开关。
 *
 * 运行时开关来自 IntelliJ registry：`cangjie.analysis.statistics`（默认 false）控制
 * `CaStatisticsService.areStatisticsEnabled`，`cangjie.analysis.lowMemoryCacheCleanup`（默认 true）
 * 控制强制缓存清理器。mock 宿主不解析插件描述符里的 `<registryKey>`，因此这里显式登记这两个 key，
 * 并把统计开关置为 true，让测试走与 IDE 开启统计后完全相同的接线。
 *
 * `CaStatisticsService.areStatisticsEnabled` 是 JVM 级 lazy 读取，必须早于任何统计读取调用 [enable]；
 * [AbstractCaPerformanceTest] 在类加载与宿主注册两个时机都会调用它。
 */
object CaPerformanceStatisticsBootstrap {
    /**
     * 统计开关 registry key。
     */
    const val STATISTICS_KEY: String = "cangjie.analysis.statistics"

    /**
     * 低内存缓存清理开关 registry key。
     */
    const val LOW_MEMORY_CACHE_CLEANUP_KEY: String = "cangjie.analysis.lowMemoryCacheCleanup"

    /**
     * 登记这些 key 的虚拟插件 id，仅用于日志与 key 归属信息。
     */
    private const val PLUGIN_ID: String = "cangjie.analysis.performance-test"

    @Volatile
    private var enabled: Boolean = false

    /**
     * 幂等地开启统计：登记 registry key 并把统计开关置为 true。
     */
    @Synchronized
    fun enable() {
        if (enabled) return

        // Registry.getInstance() 在组件尚未加载时只记录状态错误；测试宿主把状态推到 COMPONENTS_LOADED 以避免噪声日志。
        if (!LoadingState.COMPONENTS_LOADED.isOccurred) {
            LoadingState.setCurrentState(LoadingState.COMPONENTS_LOADED)
        }
        Registry.mutateContributedKeys { contributed -> contributed + contributedKeys() }
        Registry.get(STATISTICS_KEY).setValue("true")
        enabled = true
    }

    /**
     * 与生产描述符保持一致的 key 声明（默认值、restartRequired）。
     */
    private fun contributedKeys(): Map<String, RegistryKeyDescriptor> = mapOf(
        // RegistryKeyDescriptor(name, description, defaultValue, restartRequired, overrides, pluginId)
        STATISTICS_KEY to RegistryKeyDescriptor(
            STATISTICS_KEY,
            "Enables Cangjie Analysis API statistics collection and reporting to OpenTelemetry.",
            "false",
            true,
            false,
            PLUGIN_ID,
        ),
        LOW_MEMORY_CACHE_CLEANUP_KEY to RegistryKeyDescriptor(
            LOW_MEMORY_CACHE_CLEANUP_KEY,
            "Perform cache clean-up when running out of RAM",
            "true",
            true,
            false,
            PLUGIN_ID,
        ),
    )
}