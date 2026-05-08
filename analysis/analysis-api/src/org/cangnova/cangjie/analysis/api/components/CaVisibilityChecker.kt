package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.psi.CjExpression

/**
 * use-site 可见性判定协议。
 *
 * 这里按 Kotlin `KaVisibilityChecker` 的形状组织：
 * 1. 对外暴露 use-site file / receiver / position 三元组；
 * 2. 可复用的 use-site checker 单独抽成对象；
 * 3. `isVisibleInClass` 与 `isPublicApi` 继续落在同一组件上。
 */
interface CaVisibilityChecker : CaLifetimeOwner {
    /**
     * 检查 [candidateSymbol] 在给定 use-site 上是否可见。
     *
     * 相比直接逐次调用，更推荐复用 [createUseSiteVisibilityChecker] 返回的 checker。
     */
    @CaExperimentalApi
    @Deprecated(
        "Use `createUseSiteVisibilityChecker` instead.",
        replaceWith = ReplaceWith("createUseSiteVisibilityChecker(useSiteFile, receiverExpression, position).isVisible(candidateSymbol)"),
    )
    fun isVisible(
        candidateSymbol: CaDeclarationSymbol,
        useSiteFile: CaFileSymbol,
        receiverExpression: CjExpression? = null,
        position: PsiElement,
    ): Boolean = withValidityAssertion {
        createUseSiteVisibilityChecker(useSiteFile, receiverExpression, position).isVisible(candidateSymbol)
    }

    /**
     * 为给定 use-site 构造可复用的可见性检查器。
     */
    @CaExperimentalApi
    fun createUseSiteVisibilityChecker(
        useSiteFile: CaFileSymbol,
        receiverExpression: CjExpression? = null,
        position: PsiElement,
    ): CaUseSiteVisibilityChecker

    /**
     * 检查 callable 在给定类上下文中是否可见。
     */
    @CaExperimentalApi
    fun CaCallableSymbol.isVisibleInClass(classSymbol: CaClassSymbol): Boolean

    /**
     * 判断声明是否属于对外公开 API。
     */
    fun isPublicApi(symbol: CaDeclarationSymbol): Boolean
}

/**
 * 针对固定 use-site 复用的可见性检查器。
 */
@CaExperimentalApi
interface CaUseSiteVisibilityChecker : CaLifetimeOwner {
    fun isVisible(candidateSymbol: CaDeclarationSymbol): Boolean
}

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 */
@CaExperimentalApi
context(session: CaSession)
fun createUseSiteVisibilityChecker(
    useSiteFile: CaFileSymbol,
    receiverExpression: CjExpression? = null,
    position: PsiElement,
): CaUseSiteVisibilityChecker {
    return with(session) {
        createUseSiteVisibilityChecker(
            useSiteFile = useSiteFile,
            receiverExpression = receiverExpression,
            position = position,
        )
    }
}

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 */
@CaExperimentalApi
context(session: CaSession)
fun CaCallableSymbol.isVisibleInClass(classSymbol: CaClassSymbol): Boolean {
    return with(session) {
        isVisibleInClass(classSymbol)
    }
}

/**
 * Auto-generated bridge. DO NOT EDIT MANUALLY!
 */
context(session: CaSession)
fun isPublicApi(symbol: CaDeclarationSymbol): Boolean {
    return with(session) {
        isPublicApi(symbol)
    }
}
