package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.psi.CjDeclaration

/**
 * 从 light declaration 恢复文档文本。
 *
 * 该入口不引入新的文档后端，而是复用现有 Analysis API 主线：
 * 1. 先通过 `origin.sourceElement` 恢复真实声明 PSI；
 * 2. 再走 `CaDocProvider` 的 symbol/documentation 协议。
 *
 * 这样 source-backed、library source-backed 与 decompiled light declaration
 * 都共享同一条恢复链路；若当前 light declaration 不存在真实声明 PSI，
 * 或者对应声明本身没有文档，则返回 `null`。
 */
fun CaSession.documentation(lightDeclaration: CaLightDeclaration): String? {
    val declaration = lightDeclaration.origin.sourceElement as? CjDeclaration ?: return null
    return with(this) {
        declaration.symbol.documentation()
    }
}
