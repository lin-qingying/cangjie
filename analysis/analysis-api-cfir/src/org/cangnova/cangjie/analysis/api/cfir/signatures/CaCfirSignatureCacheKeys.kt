package org.cangnova.cangjie.analysis.api.cfir.signatures

import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 已替换签名缓存键。
 *
 * 相同原始签名与相同映射在同一 session 内必须复用同一个 use-site 签名对象。
 */
internal data class CaCfirSubstitutedSignatureCacheKey(
    val signature: CaSignature<*>,
    val mappings: List<Pair<CaTypeParameterSymbol, CaType>>,
)
