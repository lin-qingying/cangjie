/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLResolutionFacadeService
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * Returns [LLResolutionFacade] which corresponds to containing module
 */
fun CaModule.getResolutionFacade(project: Project): LLResolutionFacade =
    LLResolutionFacadeService.getInstance(project).getResolutionFacade(this)

/**
 * Resolves this [CjDeclaration] to a [CfirBasedSymbol].
 *
 * The underlying [CfirDeclaration][org.cangnova.cangjie.cfir.declarations.CfirDeclaration] will be resolved at least to [phase].
 */
fun CjDeclaration.resolveToCfirSymbol(
    resolutionFacade: LLResolutionFacade,
    phase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR,
): CfirBasedSymbol<*> {
    return resolutionFacade.resolveToCfirSymbol(this, phase)
}

/**
 * Resolves this [CjDeclaration] to a [CfirBasedSymbol]. If the resulting [CfirBasedSymbol] is not subtype of [S],
 * [InvalidCfirElementTypeException] is thrown.
 *
 * The underlying [CfirDeclaration][org.cangnova.cangjie.cfir.declarations.CfirDeclaration] will be resolved at least to [phase].
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified S : CfirBasedSymbol<*>> CjDeclaration.resolveToCfirSymbolOfType(
    resolutionFacade: LLResolutionFacade,
    phase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR,
): @kotlin.internal.NoInfer S {
    val symbol = resolveToCfirSymbol(resolutionFacade, phase)
    if (symbol !is S) {
        throwUnexpectedCfirElementError(symbol, this, S::class)
    }
    return symbol
}

/**
 * Resolves this [CjDeclaration] to a [CfirBasedSymbol]. If the resulting [CfirBasedSymbol] is not subtype of [S], returns `null`.
 *
 * The underlying [CfirDeclaration][org.cangnova.cangjie.cfir.declarations.CfirDeclaration] will be resolved at least to [phase].
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified S : CfirBasedSymbol<*>> CjDeclaration.resolveToCfirSymbolOfTypeSafe(
    resolutionFacade: LLResolutionFacade,
    phase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR,
): @kotlin.internal.NoInfer S? {
    return resolveToCfirSymbol(resolutionFacade, phase) as? S
}

/**
 * Returns a list of Diagnostics compiler finds for given [CjElement]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun CjElement.getDiagnostics(resolutionFacade: LLResolutionFacade, filter: DiagnosticCheckerFilter): Collection<CjPsiDiagnostic> =
    resolutionFacade.getDiagnostics(this, filter)

/**
 * Returns a list of Diagnostics compiler finds for given [CjFile]
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
fun CjFile.collectDiagnosticsForFile(
    resolutionFacade: LLResolutionFacade,
    filter: DiagnosticCheckerFilter
): Collection<CjPsiDiagnostic> =
    resolutionFacade.collectDiagnosticsForFile(this, filter)

/**
 * Build [CfirElement] node in its final resolved state for a requested element.
 *
 * Note: that it isn't always [BODY_RESOLVE][CfirResolvePhase.BODY_RESOLVE]
 * as not all declarations have types/bodies/etc. to resolve.
 *
 * This operation could be time-consuming because it creates
 * [FileStructureElement][org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructureElement]
 * and may resolve non-local declarations into [BODY_RESOLVE][CfirResolvePhase.BODY_RESOLVE] phase.
 *
 * Please use [getOrBuildCfirFile] to get [CfirFile] in undefined phase.
 *
 * @return associated [CfirElement] in final resolved state if it exists.
 *
 * @see getOrBuildCfirFile
 * @see LLResolutionFacade.getOrBuildCfirFor
 */
fun CjElement.getOrBuildCfir(resolutionFacade: LLResolutionFacade): CfirElement? =
    resolutionFacade.getOrBuildCfirFor(this)

/**
 * Get a [CfirElement] which was created by [CjElement], but only if it is subtype of [E], `null` otherwise
 * Returned [CfirElement] is guaranteed to be resolved to [CfirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : CfirElement> CjElement.getOrBuildCfirSafe(resolutionFacade: LLResolutionFacade) =
    getOrBuildCfir(resolutionFacade) as? E

/**
 * Get a [CfirElement] which was created by [CjElement], but only if it is subtype of [E], throws [InvalidCfirElementTypeException] otherwise
 * Returned [CfirElement] is guaranteed to be resolved to [CfirResolvePhase.BODY_RESOLVE] phase
 * This operation could be performance affective because it create FIleStructureElement and resolve non-local declaration into BODY phase
 */
inline fun <reified E : CfirElement> CjElement.getOrBuildCfirOfType(resolutionFacade: LLResolutionFacade): E {
    val fir = getOrBuildCfir(resolutionFacade)
    if (fir is E) return fir
    throwUnexpectedCfirElementError(fir, this, E::class)
}

/**
 * Get a [CfirFile] which was created by [CjElement]
 * Returned [CfirFile] can be resolved to any phase from [CfirResolvePhase.RAW_CFIR] to [CfirResolvePhase.BODY_RESOLVE]
 */
fun CjFile.getOrBuildCfirFile(resolutionFacade: LLResolutionFacade): CfirFile =
    resolutionFacade.getOrBuildCfirFile(this)
