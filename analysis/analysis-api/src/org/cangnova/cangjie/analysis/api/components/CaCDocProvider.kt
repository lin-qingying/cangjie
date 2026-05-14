package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaNonPublicApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponentImplementationDetail
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjNonPublicApi

/**
 * 定位声明与符号对应 CDoc 的会话组件。
 *
 * 对齐 Kotlin `KaKDocProvider` 的结构化文档入口。
 */
@CaNonPublicApi
@CaSessionComponentImplementationDetail
@SubclassOptInRequired(CaSessionComponentImplementationDetail::class)
interface CaCDocProvider : CaSessionComponent {
    /**
     * 为当前声明恢复结构化 CDoc。
     */
    @CaNonPublicApi
    @CjNonPublicApi
    fun CjDeclaration.findCDoc(): CDocCommentDescriptor?

    /**
     * 为当前声明符号恢复结构化 CDoc。
     *
     * 恢复顺序与 Kotlin `KaKDocProvider.findKDoc()` 对齐：
     * 1. 当前 PSI navigation element
     * 2. callable 的 [CaCallableSymbol.allOverriddenSymbols]
     * 3. `getExpectsForActual()`
     */
    @CaNonPublicApi
    @CjNonPublicApi
    fun CaDeclarationSymbol.findCDoc(): CDocCommentDescriptor?
}

/**
 * 顶层桥接:在当前 [CaSession] 上下文中查找声明对应的 CDoc。
 */
@CaNonPublicApi
@CjNonPublicApi
context(session: CaSession)
fun CjDeclaration.findCDoc(): CDocCommentDescriptor? {
    return with(session) {
        findCDoc()
    }
}

/**
 * 顶层桥接:在当前 [CaSession] 上下文中查找声明 symbol 对应的 CDoc。
 */
@CaNonPublicApi
@CjNonPublicApi
context(session: CaSession)
fun CaDeclarationSymbol.findCDoc(): CDocCommentDescriptor? {
    return with(session) {
        findCDoc()
    }
}
