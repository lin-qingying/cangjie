package org.cangnova.cangjie.analysis.api.platform

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Analysis API 的平台设置。
 *
 * 该接口承载 IDE、Standalone、LSP 三类宿主对分析引擎行为的统一开关，
 * 避免把平台策略直接写死在 Analysis API 实现层。
 */
interface CaPlatformSettings {
    /**
     * 控制库声明从何处反序列化到 Analysis API / low-level API。
     *
     * 与 Kotlin `KotlinPlatformSettings.deserializedDeclarationsOrigin` 同构：
     * - `BINARIES` 直接从二进制/KLIB/metadata 读取声明；
     * - `STUBS` 依赖平台预索引的 stub 声明。
     */
    val deserializedDeclarationsOrigin: CaDeserializedDeclarationsOrigin

    /**
     * 是否允许把库模块作为 use-site 模块直接进入分析。
     *
     * IDE 与 LSP 往往会更严格，Standalone 则可以根据调用方需要放宽。
     */
    val allowUseSiteLibraryModuleAnalysis: Boolean
        get() = true

    companion object {
        fun getInstance(project: Project): CaPlatformSettings = project.service()
    }
}

/**
 * 平台控制库声明来源的主开关。
 */
enum class CaDeserializedDeclarationsOrigin {
    /**
     * 直接从 `.class` / KLIB / metadata 反序列化。
     */
    BINARIES,

    /**
     * 依赖 IDE/LSP 预先建立的 stub 索引。
     */
    STUBS,
}
