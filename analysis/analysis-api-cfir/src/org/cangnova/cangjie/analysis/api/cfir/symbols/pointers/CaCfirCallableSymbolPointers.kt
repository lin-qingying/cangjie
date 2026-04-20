package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendMemberCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreCallablePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.restoreExtendMemberCallablePublicSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol

/**
 * 具备稳定 callable 标识的专用 pointer。
 *
 * 仓颉当前公开 callable 恢复入口已经由 `CallableId + kind` 和
 * `extendId + callableName + kind` 两组稳定身份覆盖，这里直接使用它们构建 pointer。
 */
internal class CaCfirCallableSymbolPointer<S : CaCallableSymbol>(
    private val cacheKey: CaCfirCallableSymbolCacheKey,
    private val symbolType: Class<S>,
) : CaCfirSymbolPointerBase<S>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): S? =
        symbolType.castOrNull(
            restoreSession(session)?.restoreCallablePublicSymbol(
                callableId = cacheKey.callableId,
                kind = cacheKey.kind,
            )
        )
}

internal class CaCfirExtendMemberCallableSymbolPointer<S : CaCallableSymbol>(
    private val cacheKey: CaCfirExtendMemberCallableSymbolCacheKey,
    private val symbolType: Class<S>,
) : CaCfirSymbolPointerBase<S>() {
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): S? =
        symbolType.castOrNull(
            restoreSession(session)?.restoreExtendMemberCallablePublicSymbol(
                extendIdentity = cacheKey.extendIdentity,
                callableName = cacheKey.callableName,
                kind = cacheKey.kind,
            )
        )
}
