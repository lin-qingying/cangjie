package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

internal enum class ConstructorFilter {
    OnlyNested,
    Both,
    ;

    fun accepts(@Suppress("UNUSED_PARAMETER") declaration: CfirClassLikeDeclaration): Boolean {
        // Cangjie currently has no language-level inner-class constructor split, so every
        // constructible classifier is treated as nested for tower lookup purposes.
        return true
    }
}

internal fun CfirScope.processFunctionsAndConstructorsByName(
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    constructorFilter: ConstructorFilter,
    processor: (CfirFunctionSymbol<*>) -> Unit,
) {
    processConstructorsByName(callInfo, bodyResolveComponents, constructorFilter, processor)
    processFunctionsByName(callInfo.name, processor)
}

private fun CfirScope.processConstructorsByName(
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    constructorFilter: ConstructorFilter,
    processor: (CfirFunctionSymbol<*>) -> Unit,
) {
    val seen = linkedSetOf<CfirClassLikeSymbol<*>>()
    processClassifiersByName(callInfo.name) { classifier ->
        val matchedSymbol = classifier

        if (!seen.add(matchedSymbol)) return@processClassifiersByName

        matchedSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        if (matchedSymbol is CfirTypeAliasSymbol) {
            matchedSymbol.cfir.scopeProvider
                .getTypealiasConstructorScope(
                    matchedSymbol.cfir,
                    bodyResolveComponents.session,
                    bodyResolveComponents.scopeSession,
                )
                .processDeclaredConstructors(processor)
            return@processClassifiersByName
        }

        val declaration = matchedSymbol.cfir as? CfirClassLikeDeclaration ?: return@processClassifiersByName
        if (declaration is CfirEnum) return@processClassifiersByName
        if (!constructorFilter.accepts(declaration)) return@processClassifiersByName

        declaration.declarations
            .asSequence()
            .filterIsInstance<CfirConstructor>()
            .map { it.symbol }
            .forEach(processor)
    }
}
