/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjFakeSourceElementKind.*
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOfType
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.declarations.isLazyResolvable
import org.cangnova.cangjie.cfir.resolve.toClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement

internal object LLContainingClassCalculator {
    /**
     * Returns a containing class symbol for the given symbol, computing it solely from the source information
     * and information inside CFIR nodes.
     */
    fun getContainingClassSymbol(symbol: CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? {
        if (!symbol.origin.isLazyResolvable) {
            // Handle only source or source-based declarations for now as below we use the PSI tree
            return null
        }

        if (!canHaveContainingClassSymbol(symbol)) {
            return null
        }

        if (symbol is CfirCallableSymbol<*>) {
            val containingClassLookupTag = symbol.containingClassLookupTag()
            if (containingClassLookupTag != null) {
                return containingClassLookupTag.toClassSymbol(symbol.cfir.moduleData.session) as? CfirClassLikeSymbol<*>
            }
        }

        val source = symbol.cfir.source as? CjPsiSourceElement ?: return null
        when (val kind = source.kind) {
            is CjFakeSourceElementKind -> {
                if (symbol is CfirConstructorSymbol && kind == ImplicitConstructor) {
                    return computeContainingClass(symbol, source.psi as? CjTypeStatement)
                }

                if (symbol is CfirPropertySymbol && kind == PropertyFromParameter) {
                    val containingParameter = source.psi as? CjParameter
                    return computeContainingClass(symbol, containingParameter?.containingTypeStatement)
                }
            }
            else -> if (symbol is CfirCallableSymbol<*>) {
                return when (val selfCallable = source.psi) {
                    is CjCallableDeclaration -> computeContainingClass(symbol, selfCallable.containingTypeStatement)
                    is CjPropertyAccessor -> computeContainingClass(symbol, selfCallable.property.containingTypeStatement)
                    else -> null
                }
            }
        }

        return null
    }

    private fun canHaveContainingClassSymbol(symbol: CfirBasedSymbol<*>): Boolean = when (symbol) {
        is CfirValueParameterSymbol, is CfirAnonymousFunctionSymbol -> false
        is CfirPropertySymbol -> true
        is CfirNamedFunctionSymbol -> symbol.rawStatus.visibility != Visibilities.Local
        is CfirCallableSymbol -> true
        else -> false
    }

    private fun computeContainingClass(symbol: CfirBasedSymbol<*>, psi: CjTypeStatement?): CfirClassLikeSymbol<*>? {
        if (psi == null) {
            return null
        }

        val module = symbol.llCfirModuleData.caModule
        val resolutionFacade = module.getResolutionFacade(module.project)
        return psi.resolveToCfirSymbolOfType<CfirClassLikeSymbol<*>>(resolutionFacade)
    }
}
