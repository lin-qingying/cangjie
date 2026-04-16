package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.psi.CjCallableDeclaration

/**
 * callable 签名协议。
 *
 * 该层只暴露结构化语义签名，不再缓存源码文本快照。
 * 需要源码级文本时，应由 renderer 直接读取 PSI 或 source snapshot。
 */
interface CaSignatureProvider : CaLifetimeOwner {
    val CjCallableDeclaration.signature: CaSignature

    val CaCallableSymbol.signature: CaSignature?
}
