/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.throwUnexpectedCfirElementError
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirDeclarationModificationService
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkInitializerIsResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkReturnTypeRefIsResolved
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.getExplicitBackingField
import org.cangnova.cangjie.cfir.declarations.utils.isConst
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirImplicitAwareBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.util.setMultimapOf
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirSymbolEntry
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

internal object LLCfirImplicitTypesLazyResolver : LLCfirLazyResolver(CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirImplicitBodyTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        if (target !is CfirCallableDeclaration) return
        checkReturnTypeRefIsResolved(target)

        if (target is CfirProperty && target.isConst) {
            checkInitializerIsResolved(target)
        }
    }
}

internal class LLImplicitBodyResolveComputationSession : ImplicitBodyResolveComputationSession() {
    /**
     * The symbol on which foreign annotations will be postponed
     *
     * @see withAnchorForForeignAnnotations
     * @see postponeForeignAnnotationResolution
     */
    private var anchorForForeignAnnotations: CfirCallableSymbol<*>? = null

    inline fun <T> withAnchorForForeignAnnotations(symbol: CfirCallableSymbol<*>, action: () -> T): T {
        val previousSymbol = anchorForForeignAnnotations
        return try {
            anchorForForeignAnnotations = symbol
            action()
        } finally {
            anchorForForeignAnnotations = previousSymbol
        }
    }

    override fun <D : CfirCallableDeclaration> executeTransformation(symbol: CfirCallableSymbol<*>, transformation: () -> D): D {
        // Do not store local declarations as we can postpone only non-local callables
        return if (symbol.cannotResolveAnnotationsOnDemand()) {
            transformation()
        } else {
            withAnchorForForeignAnnotations(symbol, transformation)
        }
    }

    private val postponedSymbols = setMultimapOf<CfirCallableSymbol<*>, CfirBasedSymbol<*>>()

    /**
     * Postpone the resolution request to [symbol] until [annotation arguments][CfirResolvePhase.ANNOTATION_ARGUMENTS] phase
     * of the declaration which is used this foreign annotation.
     *
     * @see postponedSymbols
     */
    fun postponeForeignAnnotationResolution(symbol: CfirBasedSymbol<*>) {
        // We should unwrap local symbols to avoid recursion
        // We cannot resolve them on demand, so we shouldn't postpone them
        val symbolToPostpone = symbol.symbolToPostponeIfCanBeResolvedOnDemand() ?: return
        val currentSymbol = anchorForForeignAnnotations ?: errorWithAttachment("Unexpected state: the current symbol have to be here") {
            withCfirSymbolEntry("symbol to postpone", symbolToPostpone)
        }

        // There is no sense to postpone itself as it will lead to recursion
        if (currentSymbol == symbolToPostpone) return

        postponedSymbols.put(currentSymbol, symbolToPostpone)
    }

    /**
     * @return all symbols postponed with [postponeForeignAnnotationResolution] for the [target] element
     *
     * @see postponeForeignAnnotationResolution
     */
    fun postponedSymbols(target: CfirCallableDeclaration): Collection<CfirBasedSymbol<*>> {
        return postponedSymbols[target.symbol]
    }

    private var cycledSymbol: CfirCallableSymbol<*>? = null

    /**
     * Push [symbol] with a recursion return type to be able to report it later
     *
     * @param symbol is a symbol with the recursion error in the return type
     *
     * @see popCycledSymbolIfExists
     * @see LLCfirImplicitBodyTargetResolver.handleCycleInResolution
     */
    fun pushCycledSymbol(symbol: CfirCallableSymbol<*>) {
        requireWithAttachment(cycledSymbol == null, { "Nested recursion is not allowed" })
        cycledSymbol = symbol
    }

    /**
     * Pop [CfirCallableSymbol] with a recursion return type if it was [pushed][pushCycledSymbol]
     *
     * @see pushCycledSymbol
     * @see org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.LLCfirReturnTypeCalculatorWithJump.resolveDeclaration
     */
    fun popCycledSymbolIfExists(): CfirCallableSymbol<*>? = cycledSymbol?.also { cycledSymbol = null }
}

/**
 * This resolver is responsible for [IMPLICIT_TYPES_BODY_RESOLVE][CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] phase.
 *
 * This resolver:
 * - Transforms [CfirImplicitTypeRef] into [CfirResolvedTypeRef][org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef].
 *
 * Before the transformation, the resolver [recreates][BodyStateKeepers] all bodies
 * to prevent corrupted states due to [PCE][com.intellij.openapi.progress.ProcessCanceledException].
 *
 * @see postponedSymbolsForAnnotationResolution
 * @see BodyStateKeepers
 * @see CfirImplicitAwareBodyResolveTransformer
 * @see CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
 */
internal class LLCfirImplicitBodyTargetResolver(
    target: LLCfirResolveTarget,
    llImplicitBodyResolveComputationSessionParameter: LLImplicitBodyResolveComputationSession? = null,
) : LLCfirAbstractBodyTargetResolver(
    target,
    CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    llImplicitBodyResolveComputationSession = llImplicitBodyResolveComputationSessionParameter ?: LLImplicitBodyResolveComputationSession(),
) {
    override val transformer = object : CfirImplicitAwareBodyResolveTransformer(
        resolveTargetSession,
        implicitBodyResolveComputationSession = llImplicitBodyResolveComputationSession,
        phase = resolverPhase,
        implicitTypeOnly = true,
        scopeSession = resolveTargetScopeSession,
        returnTypeCalculator = createReturnTypeCalculator(),
    ) {
        override val preserveCFGForClasses: Boolean get() = false
        override val buildCfgForScripts: Boolean get() = false
        override val buildCfgForFiles: Boolean get() = false
        override fun transformForeignAnnotationCall(symbol: CfirBasedSymbol<*>, annotationCall: CfirAnnotationCall): CfirAnnotationCall {
            llImplicitBodyResolveComputationSession.postponeForeignAnnotationResolution(symbol)
            return annotationCall
        }
    }

    /**
     * @see org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.LLCfirReturnTypeCalculatorWithJump.resolveDeclaration
     */
    override fun handleCycleInResolution(target: CfirElementWithResolveState) {
        requireWithAttachment(target is CfirCallableDeclaration, { "Resolution cycle is supposed to be only for callable declaration" }) {
            withCfirEntry("target", target)
        }

        llImplicitBodyResolveComputationSession.pushCycledSymbol(target.symbol)
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirCallableDeclaration if target.canHaveDeferredReturnTypeCalculation -> {
                val typeCalculator = transformer.returnTypeCalculator.callableCopyTypeCalculator
                typeCalculator.computeReturnType(target)

                val explicitBackingField = (target as? CfirProperty)?.getExplicitBackingField()
                if (explicitBackingField != null) {
                    typeCalculator.computeReturnType(explicitBackingField)
                }
            }

            is CfirFunction -> {
                if (target.returnTypeRef is CfirImplicitTypeRef) {
                    resolve(target, BodyStateKeepers.FUNCTION)
                }
            }

            is CfirProperty -> {
                if (target.shouldBeResolvedOnImplicitTypePhase) {
                    resolve(target, BodyStateKeepers.PROPERTY)
                }
            }

            is CfirField -> {
                if (target.returnTypeRef is CfirImplicitTypeRef) {
                    resolve(target, BodyStateKeepers.FIELD)
                }
            }

            is CfirRegularClass, is CfirTypeAlias, is CfirFile, is CfirCodeFragment, is CfirAnonymousInitializer, is CfirDanglingModifierList, is CfirEnumEntry -> {
                // No implicit bodies here
            }

            else -> throwUnexpectedCfirElementError(target)
        }

        target.forEachDeclarationWhichCanHavePostponedSymbols(::publishPostponedSymbols)
    }

    private fun publishPostponedSymbols(target: CfirCallableDeclaration) {
        val postponedSymbols = llImplicitBodyResolveComputationSession.postponedSymbols(target)
        if (postponedSymbols.isNotEmpty()) {
            target.postponedSymbolsForAnnotationResolution = postponedSymbols
        }
    }

    override fun rawResolve(target: CfirElementWithResolveState) {
        super.rawResolve(target)
        LLCfirDeclarationModificationService.bodyResolved(target, resolverPhase)
    }
}

/**
 * Whether the property has something to resolve on the [CfirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE] phase.
 */
internal val CfirProperty.shouldBeResolvedOnImplicitTypePhase: Boolean
    get() = isConst || returnTypeRef is CfirImplicitTypeRef || backingField?.returnTypeRef is CfirImplicitTypeRef
