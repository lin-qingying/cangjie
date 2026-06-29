

package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.TargetPlatform
import org.cangnova.cangjie.platform.isCommon

/**
 * 从带解析状态的 CFIR 元素取得低阶 CFIR module data。
 */
val CfirElementWithResolveState.llCfirModuleData: LLCfirModuleData
    get() = moduleData as LLCfirModuleData

/**
 * 从 CFIR session 取得低阶 CFIR module data。
 */
val CfirSession.llCfirModuleData: LLCfirModuleData
    get() = moduleData as LLCfirModuleData

/**
 * 低阶 CFIR session 绑定的 module data。
 */
val LLCfirSession.moduleData: LLCfirModuleData
    get() = llCfirModuleData

/**
 * 从 CFIR symbol 的实际声明反查低阶 CFIR module data。
 */
val CfirBasedSymbol<*>.llCfirModuleData: LLCfirModuleData
    get() = cfir.llCfirModuleData

/**
 * The [CfirModuleData] for CFIR elements managed by the Analysis API. In Analysis API mode, all CFIR elements must have [LLCfirModuleData].
 */
open class LLCfirModuleData internal constructor(val caModule: CaModule) : CfirModuleData() {
    constructor(session: LLCfirSession) : this(session.caModule) {
        bindSession(session)
    }

    /**
     * CFIR 层使用的模块名称。
     *
     * 名称来自 analysis API module 描述，并包裹为特殊名称以避免和源码声明名称混淆。
     */
    override val name: Name get() = Name.special("<${caModule.moduleDescription}>")

    /**
     * 当前模块的直接常规依赖。
     */
    override val dependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        caModule.directRegularDependencies.map(::LLCfirModuleData)
    }

    /**
     * 当前模块的直接 dependsOn refinement 依赖。
     */
    override val refinementDependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        caModule.directDependsOnDependencies.map(::LLCfirModuleData)
    }

    /**
     * 当前模块的传递 dependsOn refinement 依赖。
     */
    override val allRefinementDependencies: List<CfirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        caModule.transitiveDependsOnDependencies.map(::LLCfirModuleData)
    }

    /**
     * analysis API module 暴露的目标平台。
     */
    override val targetPlatform: TargetPlatform
        get() = caModule.targetPlatform

    /**
     * CFIR 层使用的平台表示。
     */
    override val platform: CfirPlatform
        get() = targetPlatform.toCfirPlatform()

    /**
     * 当前模块是否为 common 平台模块。
     */
    override val isCommon: Boolean
        get() = targetPlatform.isCommon()

    /**
     * 与该 module data 绑定的低阶 CFIR session。
     *
     * 优先复用已绑定 session；没有绑定时通过工程 session cache 按模块获取可解析 session。
     */
    override val session: LLCfirSession
        get() = boundSession?.let { it as LLCfirSession }
            ?: LLCfirSessionCache.getInstance(caModule.project).getSession(caModule, preferBinary = true)

    /**
     * analysis API module 提供的稳定模块名。
     */
    override val stableModuleName: String?
        get() = caModule.stableModuleName

    /**
     * Library sources have [CfirSession.Kind.Source] kind, but should be treated as a binary dependency since we don't expect
     * redeclararions there.
     */
    override val areRedeclarationsEquivalent: Boolean
        get() = super.areRedeclarationsEquivalent || caModule is CaLibrarySourceModule

    /**
     * 按底层 [caModule] 身份比较 module data。
     */
    override fun equals(other: Any?): Boolean = this === other || other is LLCfirModuleData && caModule == other.caModule

    /**
     * 使用底层 [caModule] 的哈希值，与 [equals] 保持一致。
     */
    override fun hashCode(): Int = caModule.hashCode()
}

/**
 * 当前仓颉前端尚未按后端细分 CFIR 平台实现。
 *
 * 因此 `cjnative` 与 `cjvm` 在 frontend / analysis 层都会先落到同一套默认 CFIR 平台，
 * 只保留高层 `targetPlatform` 身份，等待未来真正的后端分流接入。
 */
private fun TargetPlatform.toCfirPlatform(): CfirPlatform = CfirPlatform.DEFAULT
