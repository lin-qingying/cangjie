/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.throwUnexpectedCfirElementError
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.NonLocalAnnotationVisitor
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnnotationsAreResolved
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirEmptyArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.impl.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.isError
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.transformers.plugin.CfirAnnotationArgumentsTransformer
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.visitors.transformSingle

internal object LLCfirAnnotationArgumentsLazyResolver : LLCfirLazyResolver(CfirResolvePhase.ANNOTATION_ARGUMENTS) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirAnnotationArgumentsTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        if (target is CfirAnnotationContainer) {
            checkAnnotationsAreResolved(target)
        }

        when (target) {
            is CfirCallableDeclaration -> checkAnnotationsAreResolved(target, target.returnTypeRef)
            is CfirReceiverParameter -> checkAnnotationsAreResolved(target, target.typeRef)
            is CfirTypeParameter -> {
                for (bound in target.bounds) {
                    checkAnnotationsAreResolved(target, bound)
                }
            }

            is CfirRegularClass -> {
                for (typeRef in target.superTypeRefs) {
                    checkAnnotationsAreResolved(target, typeRef)
                }
            }

            is CfirTypeAlias -> checkAnnotationsAreResolved(target, target.expandedTypeRef)
        }
    }
}

/**
 * This resolver is responsible for [ANNOTATION_ARGUMENTS][CfirResolvePhase.ANNOTATION_ARGUMENTS] phase.
 *
 * This resolver:
 * - Transforms unresolved annotation arguments into resolved ones.
 *   It includes both regular and type annotations.
 *
 * Before the transformation, the resolver [recreates][AnnotationArgumentsStateKeepers] all unresolved argument lists
 * to prevent corrupted states due to [PCE][com.intellij.openapi.progress.ProcessCanceledException].
 *
 * @see postponedSymbolsForAnnotationResolution
 * @see AnnotationArgumentsStateKeepers
 * @see CfirAnnotationArgumentsTransformer
 * @see CfirResolvePhase.ANNOTATION_ARGUMENTS
 */
private class LLCfirAnnotationArgumentsTargetResolver(resolveTarget: LLCfirResolveTarget) : LLCfirAbstractBodyTargetResolver(
    resolveTarget,
    CfirResolvePhase.ANNOTATION_ARGUMENTS,
) {
    /**
     * All foreign annotations have to be resolved before by [postponedSymbolsForAnnotationResolution] or [resolveDependencies]
     * so there is no sense to override
     * [transformForeignAnnotationCall][org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirAbstractBodyResolveTransformerDispatcher.transformForeignAnnotationCall]
     *
     * We can add additional [checkAnnotationCallIsResolved][org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnnotationCallIsResolved],
     * but this also doesn't make sense
     * because we anyway will check all annotations during [LLCfirAnnotationArgumentsLazyResolver.phaseSpecificCheckIsResolved]
     *
     * @see postponedSymbolsForAnnotationResolution
     * @see org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirAbstractBodyResolveTransformerDispatcher.transformForeignAnnotationCall
     */
    override val transformer = CfirAnnotationArgumentsTransformer(
        resolveTargetSession,
        resolveTargetScopeSession,
        resolverPhase,
        returnTypeCalculator = createReturnTypeCalculator(),
    )

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        if (target !is CfirDeclaration) return false

        var processed = false
        var symbolsToResolve: Collection<CfirBasedSymbol<*>>? = null
        withReadLock(target) {
            processed = true
            symbolsToResolve = buildList {
                target.forEachDeclarationWhichCanHavePostponedSymbols {
                    addAll(it.postponedSymbolsForAnnotationResolution.orEmpty())
                }

                addSymbolsFromForeignAnnotations(target)
            }
        }

        // some other thread already resolved this element to this or upper phase
        if (!processed) return true
        symbolsToResolve?.forEach { it.lazyResolveToPhase(resolverPhase) }

        return false
    }

    private fun MutableList<CfirBasedSymbol<*>>.addSymbolsFromForeignAnnotations(target: CfirDeclaration) {
        // It is fine to just visit the declaration recursively as copy declarations don't have a body
        target.accept(ForeignAnnotationsCollector, ForeignAnnotationsContext(this, target.symbol))
    }

    private class ForeignAnnotationsContext(val collection: MutableCollection<CfirBasedSymbol<*>>, val currentSymbol: CfirBasedSymbol<*>)
    private object ForeignAnnotationsCollector : NonLocalAnnotationVisitor<ForeignAnnotationsContext>() {
        override fun processAnnotation(annotation: CfirAnnotation, data: ForeignAnnotationsContext) {
            if (annotation !is CfirAnnotationCall) return
            val symbolToPostpone = annotation.containingDeclarationSymbol.symbolToPostponeIfCanBeResolvedOnDemand() ?: return
            if (symbolToPostpone != data.currentSymbol) {
                data.collection += symbolToPostpone
            }
        }
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        // There is no sense to resolve such declarations as they do not have their own annotations
        if (target is CfirCallableDeclaration && target.canHaveDeferredReturnTypeCalculation) return

        resolveWithKeeper(
            target,
            target.llCfirSession,
            AnnotationArgumentsStateKeepers.DECLARATION,
        ) {
            transformAnnotations(target)
        }

        if (target is CfirDeclaration) {
            /**
             * All symbols from [postponedSymbolsForAnnotationResolution] already processed during [doResolveWithoutLock],
             * so we have to clean up the attribute
             */
            target.forEachDeclarationWhichCanHavePostponedSymbols {
                it.postponedSymbolsForAnnotationResolution = null
            }
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: CfirRegularClass, action: () -> Unit) {
        transformer.declarationsTransformer.forRegularClassBody(firClass) {
            action()
            firClass
        }
    }

    private fun transformAnnotations(target: CfirElementWithResolveState) {
        when (target) {
            is CfirRegularClass -> {
                val declarationTransformer = transformer.declarationsTransformer
                declarationTransformer.context.withClassHeader(target) {
                    target.transformAnnotations(declarationTransformer, ResolutionMode.ContextIndependent)
                    target.transformTypeParameters(declarationTransformer, ResolutionMode.ContextIndependent)
                    target.transformSuperTypeRefs(declarationTransformer, ResolutionMode.ContextIndependent)
                }
            }

            is CfirFile -> transformer.declarationsTransformer.withFile(target) {
                target.transformAnnotations(transformer.declarationsTransformer, ResolutionMode.ContextIndependent)
            }

            is CfirCallableDeclaration, is CfirAnonymousInitializer, is CfirDanglingModifierList, is CfirTypeAlias -> {
                target.transformSingle(transformer, ResolutionMode.ContextIndependent)
            }

            is CfirCodeFragment -> {}
            else -> throwUnexpectedCfirElementError(target)
        }
    }
}

internal object AnnotationArgumentsStateKeepers {
    private val ANNOTATION: StateKeeper<CfirAnnotation, CfirSession> = stateKeeper { builder, _, session ->
        builder.add(ANNOTATION_BASE, session)
        builder.add(CfirAnnotation::argumentMapping, CfirAnnotation::replaceArgumentMapping)
        builder.add(CfirAnnotation::typeArguments, CfirAnnotation::replaceTypeArguments) { typeArguments ->
            // To avoid modification of the original list
            if (typeArguments.isEmpty()) typeArguments else ArrayList(typeArguments)
        }
    }

    private val ANNOTATION_BASE: StateKeeper<CfirAnnotation, CfirSession> = stateKeeper { builder, annotation, session ->
        if (annotation is CfirAnnotationCall) {
            builder.entity(annotation, ANNOTATION_CALL, session)
        }
    }

    private val ANNOTATION_CALL: StateKeeper<CfirAnnotationCall, CfirSession> = stateKeeper { builder, _, _ ->
        builder.add(CfirAnnotationCall::calleeReference, CfirAnnotationCall::replaceCalleeReference)
        builder.add(CfirAnnotationCall::argumentList, CfirAnnotationCall::replaceArgumentList)
    }

    val DECLARATION: StateKeeper<CfirElementWithResolveState, CfirSession> = stateKeeper { builder, target, session ->
        val annotationCalls = hashSetOf<CfirAnnotationCall>()

        val visitor = object : NonLocalAnnotationVisitor<Unit>() {
            override fun processAnnotation(annotation: CfirAnnotation, data: Unit) {
                builder.entity(annotation, ANNOTATION, session)

                if (annotation is CfirAnnotationCall) {
                    annotationCalls += annotation
                }
            }
        }

        target.accept(visitor, Unit)

        // Argument calculation has to be done after the state keeper finished to properly handle possible exception from the argument calculation
        builder.postProcess {
            for (annotationCall in annotationCalls) {
                val oldList = annotationCall.argumentList
                if (oldList is CfirResolvedArgumentList || oldList is CfirEmptyArgumentList) continue

                val newArguments = CfirLazyBodiesCalculator.createArgumentsForAnnotation(annotationCall, session).arguments
                val newList = buildArgumentList {
                    source = oldList.source
                    for ((index, argument) in oldList.arguments.withIndex()) {
                        val replacement = when {
                            argument is CfirPropertyAccessExpression && argument.calleeReference.let { it.isError() || it is CfirResolvedNamedReference } -> argument
                            else -> newArguments[index]
                        }

                        arguments.add(replacement)
                    }
                }

                annotationCall.replaceArgumentList(newList)
            }
        }
    }
}
