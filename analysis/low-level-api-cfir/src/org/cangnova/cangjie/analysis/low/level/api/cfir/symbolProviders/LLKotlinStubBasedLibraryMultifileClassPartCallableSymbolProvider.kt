/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.JvmAndBuiltinsDeserializedContainerSourceProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.caches.firCachesFactory
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirPropertySymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.JvmStandardClassIds
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty

/**
 * Issue: [KT-68484](https://youtrack.jetbrains.com/issue/KT-68484).
 *
 * This class provides fallback symbols for top-level callables from synthetic multifile class part
 * ([MULTIFILE_CLASS_PART][org.cangnova.cangjie.load.kotlin.header.KotlinClassHeader.Kind.MULTIFILE_CLASS_PART]).
 *
 * @see org.cangnova.cangjie.analysis.decompiler.stub.file.ClsClassFinder.isKotlinInternalCompiledFile
 * @see addCallableIfNeeded
 **/
internal class LLKotlinStubBasedLibraryMultifileClassPartCallableSymbolProvider(val session: CfirSession) {
    private val fallbackFunctionCache = session.firCachesFactory.createCache(::loadFunction)
    private val fallbackPropertyCache = session.firCachesFactory.createCache(::loadProperty)

    /**
     * This fallback is required for multifile part classes which are not present in indices
     * but might be requested as in some cases we still build stubs for them.
     */
    fun addCallableIfNeeded(
        callableCandidates: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: CjCallableDeclaration,
    ) {
        val fileName = callableDeclaration.containingCjFile.virtualFile?.nameWithoutExtension ?: return
        if (!fileName.endsWith("Cj") || JvmStandardClassIds.MULTIFILE_PART_NAME_DELIMITER !in fileName) {
            return
        }

        val callableId = CallableId(packageFqName, shortName)
        val symbol = when (callableDeclaration) {
            is CjNamedFunction -> fallbackFunctionCache.getValue(callableDeclaration, callableId)
            is CjProperty -> fallbackPropertyCache.getValue(callableDeclaration, callableId)
            else -> null
        }

        symbol?.let(callableCandidates::add)
    }

    private fun loadFunction(function: CjNamedFunction, callableId: CallableId): CfirNamedFunctionSymbol? {
        return LLKotlinStubBasedLibrarySymbolProvider.loadFunction(
            function = function,
            callableId = callableId,
            functionOrigin = CfirDeclarationOrigin.Library,
            deserializedContainerSourceProvider = JvmAndBuiltinsDeserializedContainerSourceProvider,
            session = session,
        )
    }

    private fun loadProperty(property: CjProperty, callableId: CallableId): CfirPropertySymbol? {
        return LLKotlinStubBasedLibrarySymbolProvider.loadProperty(
            property = property,
            callableId = callableId,
            propertyOrigin = CfirDeclarationOrigin.Library,
            deserializedContainerSourceProvider = JvmAndBuiltinsDeserializedContainerSourceProvider,
            session = session,
        )
    }
}
