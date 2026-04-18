/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnnotationTypeIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkReturnTypeRefIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkTypeRefIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.transformers.CfirTypeResolveTransformer
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.util.PrivateForInline
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

internal object LLCfirTypeLazyResolver : LLCfirLazyResolver(CfirResolvePhase.TYPES) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirTypeTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        if (target is CfirAnnotationContainer) {
            checkAnnotationTypeIsResolved(target)
        }

        when (target) {
            is CfirCallableDeclaration -> checkReturnTypeRefIsResolved(target, acceptImplicitTypeRef = true)
            is CfirReceiverParameter -> checkTypeRefIsResolved(target.typeRef, "receiver type reference", target)
            is CfirTypeParameter -> {
                for (bound in target.bounds) {
                    checkTypeRefIsResolved(bound, "type parameter bound", target)
                }
            }
        }
    }
}

/**
 * This resolver is responsible for [TYPES][CfirResolvePhase.TYPES] phase.
 *
 * This resolver:
 * - Transform explicitly written types in declaration headers.
 *
 * Special rules:
 * - Cfirst resolves outer classes to this phase.
 *
 * @see CfirTypeResolveTransformer
 * @see CfirResolvePhase.TYPES
 */
private class LLCfirTypeTargetResolver(target: LLCfirResolveTarget) : LLCfirTargetResolver(target, CfirResolvePhase.TYPES) {
    private val transformer = CfirTypeResolveTransformer(resolveTargetSession, resolveTargetScopeSession)

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(firFile: CfirFile, action: () -> Unit) {
        transformer.withFileScope(firFile, action)
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: CfirRegularClass, action: () -> Unit) {
        firClass.lazyResolveToPhase(resolverPhase.previous)
        transformer.withClassDeclarationCleanup(firClass) {
            performCustomResolveUnderLock(firClass) {
                resolveClassTypes(firClass)
            }

            transformer.withClassScopes(
                firClass,
                action = action,
            )
        }
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirFunction -> resolve(target, TypeStateKeepers.FUNCTION)
            is CfirProperty -> resolve(target, TypeStateKeepers.PROPERTY)
            is CfirCallableDeclaration,
            is CfirDanglingModifierList,
            is CfirFile,
            is CfirTypeAlias,
            is CfirRegularClass,
            is CfirAnonymousInitializer,
                -> rawResolve(target)

            is CfirCodeFragment -> {}
            else -> errorWithAttachment("Unknown declaration ${target::class.simpleName}") {
                withCfirEntry("declaration", target)
            }
        }
    }

    private fun <T : CfirElementWithResolveState> resolve(target: T, keeper: StateKeeper<T, Unit>) {
        resolveWithKeeper(target, Unit, keeper) {
            rawResolve(target)
        }
    }

    private fun rawResolve(target: CfirElementWithResolveState) {
        when (target) {
            is CfirField if (target.origin == CfirDeclarationOrigin.Synthetic.DelegateField) -> {
                // delegated field should be resolved in the same context as super types
                resolveOutsideClassBody(target, transformer::transformDelegateField)
            }

            is CfirDanglingModifierList, is CfirCallableDeclaration, is CfirTypeAlias, is CfirAnonymousInitializer -> {
                target.accept(transformer, null)
            }

            is CfirFile -> transformer.withFileScope(target) { target.transformAnnotations(transformer, null) }
            is CfirRegularClass -> transformer.withClassDeclarationCleanup(target) { resolveClassTypes(target) }
            else -> errorWithAttachment("Unknown declaration ${target::class.simpleName}") {
                withCfirEntry("declaration", target)
            }
        }
    }

    private inline fun <T : CfirElementWithResolveState> resolveOutsideClassBody(
        target: T,
        crossinline actionOutsideClassBody: (T) -> Unit,
    ) {
        val scopesBeforeContainingClass = transformer.scopesBefore
            ?: errorWithCfirSpecificEntries("The containing class scope is not found", fir = target)

        val staticScopesBeforeContainingClass = transformer.staticScopesBefore
            ?: errorWithCfirSpecificEntries("The containing class static scope is not found", fir = target)

        @OptIn(PrivateForInline::class)
        transformer.withScopeCleanup {
            val clazz = transformer.classDeclarationsStack.last()
            if (!transformer.removeOuterTypeParameterScope(clazz)) {
                transformer.scopes = scopesBeforeContainingClass
            } else {
                transformer.scopes = staticScopesBeforeContainingClass
                transformer.addTypeParametersScope(clazz)
            }

            actionOutsideClassBody(target)
        }

        target.accept(transformer, null)
    }

    private fun resolveClassTypes(firClass: CfirRegularClass) {
        transformer.transformClassTypeParameters(firClass, null)
        transformer.withScopeCleanup {
            firClass.transformAnnotations(transformer, null)
        }

        transformer.withClassScopes(firClass) {}
    }
}

private object TypeStateKeepers {
    val FUNCTION: StateKeeper<CfirFunction, Unit> = stateKeeper { builder, function, context ->
        builder.add(CALLABLE_DECLARATION, context)
        builder.entityList(function.valueParameters, CALLABLE_DECLARATION, context)
    }

    val PROPERTY: StateKeeper<CfirProperty, Unit> = stateKeeper { builder, property, context ->
        builder.add(CALLABLE_DECLARATION, context)
        builder.entity(property.getter, FUNCTION, context)
        builder.entity(property.setter, FUNCTION, context)
        builder.entity(property.backingField, CALLABLE_DECLARATION, context)
    }

    private val CALLABLE_DECLARATION: StateKeeper<CfirCallableDeclaration, Unit> = stateKeeper { builder, _, _ ->
        builder.add(CfirCallableDeclaration::returnTypeRef, CfirCallableDeclaration::replaceReturnTypeRef)
    }
}
