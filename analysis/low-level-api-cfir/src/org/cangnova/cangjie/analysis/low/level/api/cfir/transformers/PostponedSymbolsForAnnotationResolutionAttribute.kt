

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isLocalForLazyResolutionPurposes
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol

/**
 * 存储注解延迟解析符号集合的声明数据键。
 */
private object PostponedSymbolsForAnnotationResolutionKey : CfirDeclarationDataKey()

/**
 * 记录当前 callable 在注解参数阶段之前必须先解析的外部符号集合。
 *
 * 隐式类型阶段可能遇到不属于当前上下文的注解调用，即注解的 containing declaration symbol 不在当前解析上下文中。
 * 这些注解不能在当前位置直接解析，原因包括：隐式类型阶段早于注解参数阶段，直接解析会破坏阶段契约；同一个外部注解实例可能被
 * 原声明和当前使用点共享，并发修改会产生未定义行为；使用点上下文也可能无法看见注解参数中引用的声明。
     *
 * @return 在解析当前声明前必须推进到注解参数阶段的 [CfirBasedSymbol] 集合。
 *
 * @see LLCfirImplicitBodyTargetResolver
 * @see LLCfirAnnotationArgumentsTargetResolver
 */
internal var CfirCallableDeclaration.postponedSymbolsForAnnotationResolution: Collection<CfirBasedSymbol<*>>?
        by CfirDeclarationDataRegistry.data(PostponedSymbolsForAnnotationResolutionKey)

/**
 * 判断当前符号是否不能作为普通注解拥有者按需解析。
 *
 * 局部 callable 的注解不会以未完整解析状态泄漏出 body，因此不应按普通非局部声明路径处理。
 *
 * @return 如果该符号应跳过按需注解解析则返回 `true`。
 */
internal fun CfirBasedSymbol<*>.cannotResolveAnnotationsOnDemand(): Boolean {
    return this is CfirCallableSymbol<*> && isLocalForLazyResolutionPurposes
}

/**
 * 对可能持有延迟注解解析符号集合的 callable 声明执行 [action]。
 *
 * @see postponedSymbolsForAnnotationResolution
 */
internal fun CfirDeclaration.forEachDeclarationWhichCanHavePostponedSymbols(action: (CfirCallableDeclaration) -> Unit) {
    when (this) {
        is CfirCallableDeclaration -> action(this)
        else -> {}
    }
}

/**
 * 返回写入 [postponedSymbolsForAnnotationResolution] 集合时应使用的符号。
 *
 * @see postponedSymbolsForAnnotationResolution
 */
internal fun CfirBasedSymbol<*>.unwrapSymbolToPostpone(): CfirBasedSymbol<*> = when (this) {
    is CfirValueParameterSymbol -> cfir.containingDeclarationSymbol
    else -> this
}

/**
 * 返回可按需解析注解的展开后符号；如果符号不能按需解析则返回 `null`。
 *
 * @see unwrapSymbolToPostpone
 * @see cannotResolveAnnotationsOnDemand
 */
internal fun CfirBasedSymbol<*>.symbolToPostponeIfCanBeResolvedOnDemand(): CfirBasedSymbol<*>? {
    return unwrapSymbolToPostpone().takeUnless { it.cannotResolveAnnotationsOnDemand() }
}
