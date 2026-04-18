/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.CjFakeSourceElementKind
import org.cangnova.cangjie.CjFakeSourceElementKind.*
import org.cangnova.cangjie.CjPsiSourceElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOfType
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.declarations.isLazyResolvable
import org.cangnova.cangjie.cfir.getContainingClassLookupTag
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.impl.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingClassOrObject

internal object LLContainingClassCalculator {
    /**
     * Returns a containing class symbol for the given symbol, computing it solely from the source information
     * and information inside CFIR nodes.
     */
    fun getContainingClassSymbol(symbol: CfirBasedSymbol<*>): CfirClassSymbol<*>? {
        if (!symbol.origin.isLazyResolvable) {
            // Handle only source or source-based declarations for now as below we use the PSI tree
            return null
        }

        if (symbol is CfirAnonymousInitializerSymbol) {
            // For anonymous initializers, the containing class symbol is right there, no need in PSI traversal
            return symbol.containingDeclarationSymbol as? CfirClassSymbol<*>
        }

        if (!canHaveContainingClassSymbol(symbol)) {
            return null
        }

        val containingClassLookupTag = when (symbol) {
            is CfirCallableSymbol<*> -> symbol.containingClassLookupTag()
            is CfirClassLikeSymbol<*> -> symbol.getContainingClassLookupTag()
            is CfirDanglingModifierSymbol -> symbol.containingClassLookupTag()
            else -> null
        }

        // For members of local classes lookup tag should be used to avoid a phase
        // contract violation
        if (containingClassLookupTag is ConeClassLikeLookupTagWithFixedSymbol) {
            return containingClassLookupTag.symbol as? CfirClassSymbol<*>
        }

        val source = symbol.source as? CjPsiSourceElement ?: return null
        when (val kind = source.kind) {
            is CjFakeSourceElementKind -> {
                if (symbol is CfirBackingFieldSymbol) {
                    if (kind == DefaultAccessor) {
                        return computeContainingClass(symbol, (source.psi as? CjDeclaration)?.containingClassOrObject)
                    }
                }

                if (symbol is CfirConstructorSymbol && kind == ImplicitConstructor) {
                    return computeContainingClass(symbol, source.psi)
                }

                if (symbol is CfirPropertyAccessorSymbol) {
                    if (kind == DefaultAccessor) {
                        val containingProperty = source.psi
                        return if (containingProperty is CjProperty || containingProperty is CjParameter) {
                            computeContainingClass(symbol, (containingProperty as CjDeclaration).containingClassOrObject)
                        } else {
                            null
                        }
                    }

                    if (kind == DelegatedPropertyAccessor) {
                        val containingProperty = source.psi as? CjProperty
                        return computeContainingClass(symbol, containingProperty?.containingClassOrObject)
                    }

                    if (kind == PropertyFromParameter) {
                        val containingParameter = source.psi as? CjParameter
                        return computeContainingClass(symbol, containingParameter?.containingClassOrObject)
                    }
                }

                if (symbol is CfirPropertySymbol && kind == PropertyFromParameter) {
                    val containingParameter = source.psi as? CjParameter
                    return computeContainingClass(symbol, containingParameter?.containingClassOrObject)
                }

                if (kind == EnumGeneratedDeclaration) {
                    return computeContainingClass(symbol, source.psi)
                }

                if (symbol is CfirDanglingModifierSymbol && kind == DanglingModifierList) {
                    val modifierList = source.psi as? CjModifierList
                    val body = modifierList?.parent as? CjClassBody
                    return computeContainingClass(symbol, body?.parent)
                }
            }
            else -> {
                if (symbol is CfirClassLikeSymbol<*>) {
                    val selfClass = source.psi as? CjClassOrObject
                    return computeContainingClass(symbol, selfClass?.containingClassOrObject)
                }

                if (symbol is CfirCallableSymbol<*>) {
                    return when (val selfCallable = source.psi) {
                        is CjCallableDeclaration, is CjEnumEntry -> {
                            computeContainingClass(symbol, selfCallable.containingClassOrObject)
                        }
                        is CjPropertyAccessor -> {
                            val containingProperty = selfCallable.property
                            computeContainingClass(symbol, containingProperty.containingClassOrObject)
                        }
                        else -> null
                    }
                }
            }
        }

        return null
    }

    private fun canHaveContainingClassSymbol(symbol: CfirBasedSymbol<*>): Boolean = when (symbol) {
        is CfirValueParameterSymbol, is CfirAnonymousFunctionSymbol -> false
        is CfirRegularPropertySymbol -> true
        is CfirNamedFunctionSymbol -> symbol.rawStatus.visibility != Visibilities.Local
        is CfirClassLikeSymbol -> symbol.classId.isNestedClass
        is CfirCallableSymbol, is CfirDanglingModifierSymbol -> true
        else -> false
    }

    private fun computeContainingClass(symbol: CfirBasedSymbol<*>, psi: PsiElement?): CfirClassSymbol<*>? {
        if (psi !is CjClassOrObject) {
            return null
        }

        val module = symbol.llCfirModuleData.ktModule
        val resolutionFacade = module.getResolutionFacade(module.project)
        return psi.resolveToCfirSymbolOfType<CfirClassSymbol<*>>(resolutionFacade)
    }
}
