

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isLocalForLazyResolutionPurposes
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirReceiverParameterSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirValueParameterSymbol

private object PostponedSymbolsForAnnotationResolutionKey : CfirDeclarationDataKey()

/**
 * During [implicit type][org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] phase we can
 * meet [CfirAnnotationCall][org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall]'s which do not belong to us
 * (their [containingDeclarationSymbol][org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall.containingDeclarationSymbol] is not in our context).
 * Such annotations can't be resolved in-place due to:
 * * Contract violation
 * ([implicit type][org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] phase is less than [annotation arguments][org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.ANNOTATION_ARGUMENTS] phase)
 *
 * * Concurrent modification issues.
 * The same instance of a foreign annotation is shared at least between two declarations – the original declaration and this call site,
 * so simultaneous modification of the annotation can lead to undefined behavior.
 *
 * * Wrong context on the call site.
 * It is possible that the annotation can use arguments which are not visible from the call site.
 *
 * @return The collection of [CfirBasedSymbol]s which have to be resolved on
 * [annotation arguments][org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.ANNOTATION_ARGUMENTS] phase before [this] declaration.
 *
 * @see LLCfirImplicitBodyTargetResolver
 * @see LLCfirAnnotationArgumentsTargetResolver
 */
internal var CfirCallableDeclaration.postponedSymbolsForAnnotationResolution: Collection<CfirBasedSymbol<*>>?
        by CfirDeclarationDataRegistry.data(PostponedSymbolsForAnnotationResolutionKey)

/**
 * Some symbols shouldn't be processed as a regular annotation owner and should be just skipped.
 * Example:
 * ```kotlin
 * fun foo() {
 *   class Local {
 *     fun localMemberWithoutType() = localMember()
 *     fun localMember(): @Anno Int = 0
 *   }
 * }
 * ```
 * Here `localMember` is the owner of `Anno`, but we shouldn't process it as a usual non-local declaration, because
 * this annotation cannot be leaked out of the body in not fully resolved state.
 *
 * @return true if this symbol shouldn't be processed as the owner of an annotation call
 */
internal fun CfirBasedSymbol<*>.cannotResolveAnnotationsOnDemand(): Boolean {
    return this is CfirCallableSymbol<*> && isLocalForLazyResolutionPurposes
}

/**
 * Invoke [action] on each callable declaration that can have postponed symbols
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
 * @return a symbol which should be used as a member of [postponedSymbolsForAnnotationResolution] collection
 *
 * @see postponedSymbolsForAnnotationResolution
 */
internal fun CfirBasedSymbol<*>.unwrapSymbolToPostpone(): CfirBasedSymbol<*> = when (this) {
    is CfirValueParameterSymbol -> containingDeclarationSymbol
    is CfirReceiverParameterSymbol -> containingDeclarationSymbol
    else -> this
}

/**
 * @return an [unwrapped][unwrapSymbolToPostpone] symbol which [can][cannotResolveAnnotationsOnDemand] be resolved on demand
 *
 * @see unwrapSymbolToPostpone
 * @see cannotResolveAnnotationsOnDemand
 */
internal fun CfirBasedSymbol<*>.symbolToPostponeIfCanBeResolvedOnDemand(): CfirBasedSymbol<*>? {
    return unwrapSymbolToPostpone().takeUnless { it.cannotResolveAnnotationsOnDemand() }
}
