package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirTypeParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.psi.CjTypeParameter

/**
 * 对位 Kotlin KaFirTypeParameterSymbolBase：共享上界、指针和公开身份契约。
 * 仓颉继续映射已有 resolvedBounds，不引入 Kotlin 的 nullable Any 上界过滤。
 */
internal sealed class CaCfirTypeParameterSymbolBase<P : PsiElement> :
    CaTypeParameterSymbol, CaCfirPsiSymbol<P, CfirTypeParameterSymbol> {
    override val upperBounds: List<CaType>
        get() = withValidityAssertion { cfirSymbol.resolvedBounds.map(builder.typeBuilder::buildType) }

    /** 源码使用正式 PSI pointer；二进制参数从实际 owner 列表计算身份下标，不另存可漂移的索引。 */
    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        if (this is CaCfirCjBasedSymbol<*, *>) {
            psiBasedSymbolPointerOfTypeIfSource<CaTypeParameterSymbol> { source ->
                (source as? CjTypeParameter)?.symbol
            }?.let { return@withValidityAssertion it }
        }

        val containingDeclaration = cfirSymbol.containingDeclarationSymbol
        val owner = checkNotNull(builder.buildSymbol(containingDeclaration)) {
            "A binary type parameter must have a pointer-restorable owner."
        }
        CaCfirTypeParameterSymbolPointer(owner.createPointer(), name, index)
    }

    /** 仓颉稳定缓存键与 pointer 使用同一个真实 owner 下标，保留参数声明/引用的身份。 */
    internal val index: Int
        get() {
            val parameters = checkNotNull((cfirSymbol.containingDeclarationSymbol.cfir as? CfirTypeParameterRefsOwner)?.typeParameters) {
                "A type parameter must belong to an actual type-parameter owner."
            }
            return parameters.indexOfFirst { it.symbol === cfirSymbol }.also {
                check(it >= 0) { "The containing declaration must retain its actual type parameter." }
            }
        }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)

    override fun hashCode(): Int = psiOrSymbolHashCode()
}
