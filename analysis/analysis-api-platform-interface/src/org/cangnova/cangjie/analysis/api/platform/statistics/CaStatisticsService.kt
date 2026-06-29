package org.cangnova.cangjie.analysis.api.platform.statistics

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaEngineService

/**
 * 收集并上报 Analysis API 内部统计信息。
 *
 * 对齐 Kotlin `KaStatisticsService` 的主契约与启用方式。
 */
@CaPlatformInterface
interface CaStatisticsService : CaEngineService {
    /**
     * 启动统计收集和上报逻辑。
     */
    fun start()

    @CaPlatformInterface
    companion object {
        /**
         * 是否通过 Registry 启用 Analysis API 统计。
         */
        val areStatisticsEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Registry.`is`("kotlin.analysis.statistics", false)
        }

        /**
         * 在统计启用时获取项目级统计服务。
         */
        fun getInstance(project: Project): CaStatisticsService? =
            if (areStatisticsEnabled) project.serviceOrNull() else null
    }
}
