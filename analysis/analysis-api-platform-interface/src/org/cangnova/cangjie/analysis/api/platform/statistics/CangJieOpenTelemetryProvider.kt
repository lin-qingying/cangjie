package org.cangnova.cangjie.analysis.api.platform.statistics

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import io.opentelemetry.api.OpenTelemetry
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaOptionalPlatformComponent

/**
 * 提供平台初始化后的 OpenTelemetry 实例。
 *
 * 对齐 Kotlin `KotlinOpenTelemetryProvider`，但落在仓颉平台接口命名空间下。
 */
@CaPlatformInterface
interface CangJieOpenTelemetryProvider : CaOptionalPlatformComponent {
    val openTelemetry: OpenTelemetry

    @CaPlatformInterface
    companion object {
        fun getInstance(project: Project): CangJieOpenTelemetryProvider? = project.serviceOrNull()
    }
}
