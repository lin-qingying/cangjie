/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.asResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.throwUnexpectedCfirElementError
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkDeprecationProviderIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.expressionGuard
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkAnalysisReadiness
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.isLocal
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirEmptyArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.extensions.withGeneratedDeclarationsSymbolProviderDisabled
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.resolve.transformers.plugin.CompilerRequiredAnnotationsComputationSession
import org.cangnova.cangjie.cfir.resolve.transformers.plugin.CfirCompilerRequiredAnnotationsResolveTransformer
import org.cangnova.cangjie.cfir.symbols.impl.CfirRegularClassSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.util.PrivateForInline

internal object LLCfirCompilerAnnotationsLazyResolver : LLCfirLazyResolver(CfirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
    override fun createTargetResolver(
        target: LLCfirResolveTarget,
    ): LLCfirTargetResolver = LLCfirCompilerRequiredAnnotationsTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        when (target) {
            is CfirClassLikeDeclaration -> checkDeprecationProviderIsResolved(target, target.deprecationsProvider)
            is CfirCallableDeclaration -> checkDeprecationProviderIsResolved(target, target.deprecationsProvider)
        }
    }
}

/**
 * This special session is necessary to avoid non-local classes from being modified by CLI transformers.
 *
 * As it is called far later than from [CfirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] phase,
 * it is safe to explicitly request resolution for required classes till the phase.
 *
 * @see LLCfirCompilerRequiredAnnotationsTargetResolver.LLCfirCompilerRequiredAnnotationsComputationSession
 * @see org.cangnova.cangjie.cfir.resolve.transformers.plugin.runCompilerRequiredAnnotationsResolvePhaseForLocalClass
 */
internal class LLCompilerRequiredAnnotationsComputationSessionLocalClassesAware : CompilerRequiredAnnotationsComputationSession() {
    override fun resolveAnnotationSymbol(symbol: CfirRegularClassSymbol, scopeSession: ScopeSession) {
        if (symbol.isLocal) {
            super.resolveAnnotationSymbol(symbol, scopeSession)
        } else {
            symbol.lazyResolveToPhase(CfirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        }
    }
}

/**
 * This resolver is responsible for [COMPILER_REQUIRED_ANNOTATIONS][CfirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] phase.
 *
 * This resolver:
 * - Transforms compiler required annotations of declarations.
 * - Calculates [DeprecationsProvider].
 *
 * @see CfirCompilerRequiredAnnotationsResolveTransformer
 * @see CfirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS
 */
private class LLCfirCompilerRequiredAnnotationsTargetResolver(
    target: LLCfirResolveTarget,
    computationSession: LLCfirCompilerRequiredAnnotationsComputationSession? = null,
) : LLCfirTargetResolver(target, CfirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
    inner class LLCfirCompilerRequiredAnnotationsComputationSession : CompilerRequiredAnnotationsComputationSession() {
        override fun resolveAnnotationSymbol(symbol: CfirRegularClassSymbol, scopeSession: ScopeSession) {
            val regularClass = symbol.fir
            if (checkAnalysisReadiness(regularClass, containingDeclarations, resolverPhase)) return

            symbol.lazyResolveToPhase(resolverPhase.previous)
            val designation = regularClass.collectDesignation().asResolveTarget()
            val resolver = LLCfirCompilerRequiredAnnotationsTargetResolver(
                designation,
                this,
            )

            resolver.resolveDesignation()
        }

        override val useCacheForImportScope: Boolean get() = true

        /**
         * In the Analysis API we still need to transform non-source declarations like `componentN` functions
         */
        override val treatNonSourceDeclarationsAsResolved: Boolean get() = false

        /**
         * Annotation arguments should be calculated even if they are not from
         * [org.cangnova.cangjie.cfir.declarations.CfirAnnotationsPlatformSpecificSupportComponent.requiredAnnotationsWithArguments]
         * as compiler plugins still may access unresolved arguments for some computations (like to get a class literal)
         */
        override fun annotationResolved(annotation: CfirAnnotationCall) {
            CfirLazyBodiesCalculator.calculateAnnotation(annotation, resolveTargetSession)
        }
    }

    private val transformer = CfirCompilerRequiredAnnotationsResolveTransformer(
        resolveTargetSession,
        resolveTargetScopeSession,
        computationSession ?: LLCfirCompilerRequiredAnnotationsComputationSession(),
    )

    @OptIn(PrivateForInline::class)
    private val llCfirComputationSession: LLCfirCompilerRequiredAnnotationsComputationSession
        get() = transformer.annotationTransformer.computationSession as LLCfirCompilerRequiredAnnotationsComputationSession

    /**
     * It is a valid scenario as meta-annotations might have a cycle.
     *
     * Simple example: [Target].
     * The annotation marks itself.
     *
     * Usually such situations can be detected by [CompilerRequiredAnnotationsComputationSession.annotationResolutionWasAlreadyStarted],
     * but in multithreaded scenarios it is not always possible.
     * ```kotlin
     * @Two
     * annotation class One
     *
     * @Three
     * annotation class Two
     *
     * @One
     * annotation class Three
     * ```
     * in this case if two or three classes start resolution at the same time, there will be no any thread that is able
     * to visit all classes with the same [CompilerRequiredAnnotationsComputationSession].
     */
    override fun handleCycleInResolution(target: CfirElementWithResolveState) {}

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(firFile: CfirFile, action: () -> Unit) {
        transformer.annotationTransformer.withFileAndFileScopes(firFile, action)
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: CfirRegularClass, action: () -> Unit) {
        transformer.annotationTransformer.withClass(firClass, action)
    }

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        val alreadyResolved = target is CfirAnnotationContainer && llCfirComputationSession.annotationsAreResolved(target) ||
                target is CfirClassLikeDeclaration && llCfirComputationSession.annotationResolutionWasAlreadyStarted(target)

        return alreadyResolved
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirProperty -> resolve(target, CompilerAnnotationsStateKeepers.PROPERTY)
            is CfirFunction -> resolve(target, CompilerAnnotationsStateKeepers.FUNCTION)
            is CfirCallableDeclaration -> resolve(target, CompilerAnnotationsStateKeepers.CALLABLE_DECLARATION)
            is CfirClassLikeDeclaration -> resolve(target, CompilerAnnotationsStateKeepers.CLASS_LIKE_DECLARATION)
            is CfirCodeFragment -> {}
            is CfirFile, is CfirAnonymousInitializer, is CfirDanglingModifierList -> {
                resolve(target, CompilerAnnotationsStateKeepers.ANNOTATION_CONTAINER)
            }

            else -> throwUnexpectedCfirElementError(target)
        }
    }

    private fun <T : CfirDeclaration> resolve(target: T, keeper: StateKeeper<T, Unit>) {
        resolveWithKeeper(target, Unit, keeper) {
            // N.B. We disable generated declarations provider to avoid infinite resolve problems (see KT-67483)
            @OptIn(CfirSymbolProviderInternals::class)
            transformer.session.withGeneratedDeclarationsSymbolProviderDisabled {
                rawResolve(target)
            }
        }
    }

    private fun rawResolve(target: CfirDeclaration) {
        val annotationTransformer = transformer.annotationTransformer
        when (target) {
            is CfirFile -> annotationTransformer.resolveFile(target) {}
            is CfirRegularClass -> annotationTransformer.resolveClass(target) {}
            else -> target.transformSingle(annotationTransformer, null)
        }
    }
}

private object CompilerAnnotationsStateKeepers {
    val PROPERTY: StateKeeper<CfirProperty, Unit> = stateKeeper { builder, property, context ->
        builder.entity(property, CALLABLE_DECLARATION, context)
        builder.entity(property.getter, CALLABLE_DECLARATION, context)
        builder.entity(property.setter, CALLABLE_DECLARATION, context)
        builder.entity(property.backingField, CALLABLE_DECLARATION, context)
    }

    val FUNCTION: StateKeeper<CfirFunction, Unit> = stateKeeper { builder, function, context ->
        builder.entity(function, CALLABLE_DECLARATION, context)
        builder.entityList(function.valueParameters, CALLABLE_DECLARATION, context)
    }

    val CALLABLE_DECLARATION: StateKeeper<CfirCallableDeclaration, Unit> = stateKeeper { builder, declaration, context ->
        builder.add(CfirCallableDeclaration::deprecationsProvider, CfirCallableDeclaration::replaceDeprecationsProvider)

        builder.entity(declaration, ANNOTATION_CONTAINER, context)
    }

    val CLASS_LIKE_DECLARATION: StateKeeper<CfirClassLikeDeclaration, Unit> = stateKeeper { builder, declaration, context ->
        builder.add(CfirClassLikeDeclaration::deprecationsProvider, CfirClassLikeDeclaration::replaceDeprecationsProvider)

        builder.entity(declaration, ANNOTATION_CONTAINER, context)
    }

    val ANNOTATION_CONTAINER: StateKeeper<CfirAnnotationContainer, Unit> = stateKeeper { builder, container, context ->
        // For containers where the annotations might be rotated
        builder.add(CfirAnnotationContainer::annotations, CfirAnnotationContainer::replaceAnnotations)

        builder.entityList(container.annotations, ANNOTATION, context)
    }

    private val ANNOTATION: StateKeeper<CfirAnnotation, Unit> = stateKeeper { builder, annotation, context ->
        if (annotation is CfirAnnotationCall) {
            builder.entity(annotation, ANNOTATION_CALL, context)
        }
    }

    private val ANNOTATION_CALL: StateKeeper<CfirAnnotationCall, Unit> = stateKeeper { builder, annotationCall, _ ->
        builder.add(CfirAnnotationCall::annotationResolvePhase, CfirAnnotationCall::replaceAnnotationResolvePhase)
        builder.add(CfirAnnotationCall::annotationTypeRef, CfirAnnotationCall::replaceAnnotationTypeRef)
        builder.add(CfirAnnotationCall::argumentMapping, CfirAnnotationCall::replaceArgumentMapping)

        if (annotationCall.argumentList !is CfirEmptyArgumentList) {
            builder.add(CfirAnnotationCall::argumentList, CfirAnnotationCall::replaceArgumentList) { oldList ->
                if (oldList.arguments.all { it is CfirLazyExpression }) {
                    oldList
                } else {
                    buildArgumentList {
                        source = oldList.source
                        for (argument in oldList.arguments) {
                            arguments.add(expressionGuard(argument))
                        }
                    }
                }
            }
        }
    }
}
