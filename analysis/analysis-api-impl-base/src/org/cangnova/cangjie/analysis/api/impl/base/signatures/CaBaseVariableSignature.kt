package org.cangnova.cangjie.analysis.api.impl.base.signatures

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin `KaBaseVariableSignature` 的变量签名公共基座。
 *
 * 当前仓颉还没有 Kotlin `@ParameterName` 那条完整语义链，
 * 因此这里先严格保留“变量签名名默认等于底层 symbol 名”这一主线。
 * 后续若仓颉官方语义引入等价能力，应继续在这一层扩展，而不是回灌到 CFIR 叶子类。
 */
@OptIn(CaImplementationDetail::class)
abstract class CaBaseVariableSignature<out S : CaVariableSymbol> : CaVariableSignature<S> {
    override val name: Name
        get() = withValidityAssertion { symbol.name }

    override val type
        get() = withValidityAssertion { returnType }

    override val annotations: List<CaAnnotation>
        get() = withValidityAssertion {
            (symbol as? CaAnnotatedSymbol)?.annotations?.toList().orEmpty()
        }
}
