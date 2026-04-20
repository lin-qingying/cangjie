/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty

/**
 * A [CfirSymbolProvider] which provides symbols from CangJie sources via [CangJieDeclarationProvider].
 *
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined.LLCombinedCangJieSymbolProvider
 */
@OptIn(CaPlatformInterface::class)
internal abstract class LLCangJieSymbolProvider(session: CfirSession) :
    CfirSymbolProvider(session),
    LLKnownClassDeclarationSymbolProvider<CjClassLikeDeclaration>,
    LLPsiAwareSymbolProvider {
    abstract val declarationProvider: CangJieDeclarationProvider

    abstract val packageProvider: CangJiePackageProvider

    /**
     * Maps the [CfirCallableSymbol]s with the given [callableId] for known [callables] to [destination].
     *
     * As the [callables] are already known, this function is optimized to avoid declaration provider accesses. However, the given callable
     * declarations have to be coherent with the union of [CangJieDeclarationProvider.getTopLevelFunctions] and
     * [CangJieDeclarationProvider.getTopLevelProperties]. In other words, the callables must be chosen such that the resulting
     * [CfirCallableSymbol]s are the same as the result of [getTopLevelCallableSymbolsTo] without known declarations.
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<CjCallableDeclaration>,
    )

    /**
     * Maps the [CfirNamedFunctionSymbol]s with the given [callableId] for known [functions] to [destination].
     *
     * As the [functions] are already known, this function is optimized to avoid declaration provider accesses. However, the given function
     * declarations have to be coherent with [CangJieDeclarationProvider.getTopLevelFunctions]. In other words, the functions must be chosen
     * such that the resulting [CfirNamedFunctionSymbol]s are the same as the result of [getTopLevelFunctionSymbolsTo] without known
     * declarations.
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<CjNamedFunction>,
    )

    /**
     * Maps the [CfirPropertySymbol]s with the given [callableId] for known [properties] to [destination].
     *
     * As the [properties] are already known, this function is optimized to avoid declaration provider accesses. However, the given property
     * declarations have to be coherent with [CangJieDeclarationProvider.getTopLevelProperties]. In other words, the properties must be
     * chosen such that the resulting [CfirPropertySymbol]s are the same as the result of [getTopLevelPropertySymbolsTo] without known
     * declarations.
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<CjProperty>,
    )

    override fun toString(): String {
        return "${this::class.simpleName} for $session"
    }
}
