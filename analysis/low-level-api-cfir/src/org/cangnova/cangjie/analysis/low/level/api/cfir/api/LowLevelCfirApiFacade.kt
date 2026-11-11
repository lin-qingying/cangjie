/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLResolutionFacadeService
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.CfirResolution
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.UnresolvedReason
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.valueOrNull
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
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
 * Resolves this [CjDeclaration] to a [CfirBasedSymbol], **without throwing on missing symbols**.
 *
 * 符号恢复是部分函数：库声明可能尚未进入索引、反编译 PSI 可能失效、声明可能没有全局身份。
 * 该入口把「取不到」表达为 [CfirResolution.Unresolved]，让上层按 [UnresolvedReason] 分类降级。
 *
 * 新代码应优先使用本入口；[resolveToCfirSymbol] 仅作为仍要求非空符号的旧调用点的兼容层。
 *
 * The underlying [CfirDeclaration][org.cangnova.cangjie.cfir.declarations.CfirDeclaration] will be resolved at least to [phase].
 */
fun CjDeclaration.resolveToCfirSymbolOrUnresolved(
    resolutionFacade: LLResolutionFacade,
    phase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR,
): CfirResolution<CfirBasedSymbol<*>> {
    return resolutionFacade.resolveToCfirSymbolOrUnresolved(this, phase)
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
 * Resolves this [CjDeclaration] to a [CfirBasedSymbol].
 *
 * 与 [`as? S`] 的语义一致：**符号不可恢复**与**类型不匹配**都返回 `null`。
 * 之前该入口名为 Safe，实际第一步就走抛异常的非空路径——名不副实；
 * 现在它建立在 [resolveToCfirSymbolOrUnresolved] 之上，名字承诺才成立。
 *
 * The underlying [CfirDeclaration][org.cangnova.cangjie.cfir.declarations.CfirDeclaration] will be resolved at least to [phase].
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified S : CfirBasedSymbol<*>> CjDeclaration.resolveToCfirSymbolOfTypeSafe(
    resolutionFacade: LLResolutionFacade,
    phase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR,
): @kotlin.internal.NoInfer S? {
    return resolveToCfirSymbolOrUnresolved(resolutionFacade, phase).valueOrNull as? S
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
