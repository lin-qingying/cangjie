package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.psi.CjCallableDeclaration

/**
 * PSI 声明到公开 use-site 签名的恢复入口。
 *
 * Kotlin Analysis API 的主路径是 `callableSymbol.asSignature()`。
 * 仓颉这里额外保留一条 PSI 侧入口，只负责把 `CjCallableDeclaration`
 * 恢复成对应的公开 callable symbol，再委托给统一的 `asSignature()` 流程。
 */
interface CaSignatureProvider : CaLifetimeOwner {
    fun CjCallableDeclaration.asSignature(): CaSignature<CaCallableSymbol>?
}

context(session: CaSession)
fun CjCallableDeclaration.asSignature(): CaSignature<CaCallableSymbol>? {
    return with(session) {
        asSignature()
    }
}
