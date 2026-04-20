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
    fun start()

    @CaPlatformInterface
    companion object {
        val areStatisticsEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Registry.`is`("kotlin.analysis.statistics", false)
        }

        fun getInstance(project: Project): CaStatisticsService? =
            if (areStatisticsEnabled) project.serviceOrNull() else null
    }
}
