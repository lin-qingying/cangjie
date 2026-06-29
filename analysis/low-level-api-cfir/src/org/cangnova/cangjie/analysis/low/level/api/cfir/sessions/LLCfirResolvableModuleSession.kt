

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * 具备源码或库源码 lazy resolve 能力的模块 session 基类。
 *
 * 可解析 session 持有 [LLCfirModuleResolveComponents]，并通过其中的 scope session provider 获取作用域会话。
 */
abstract class LLCfirResolvableModuleSession(
    caModule: CaModule,
    builtinTypes: CfirBuiltinTypes
) : LLCfirModuleSession(caModule, builtinTypes, Kind.Source) {
    /**
     * 当前可解析模块 session 的解析组件集合。
     */
    internal abstract val moduleComponents: LLCfirModuleResolveComponents

    /**
     * 返回模块解析组件维护的 scope session。
     */
    final override fun getScopeSession(): ScopeSession {
        return moduleComponents.scopeSessionProvider.getScopeSession()
    }
}

/**
 * 从带解析状态的 CFIR 元素取得其所属的可解析 low-level session。
 */
internal val CfirElementWithResolveState.llCfirResolvableSession: LLCfirResolvableModuleSession?
    get() = llCfirSession as? LLCfirResolvableModuleSession

/**
 * 从 CFIR symbol 取得其声明所属的可解析 low-level session。
 */
internal val CfirBasedSymbol<*>.llCfirResolvableSession: LLCfirResolvableModuleSession?
    get() = cfir.llCfirResolvableSession
