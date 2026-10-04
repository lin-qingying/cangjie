package org.cangnova.cangjie.analysis.api.cfir.references

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic

/**
 * 从 CFIR 引用恢复公开 Analysis API 符号集合。
 *
 * 对齐 Kotlin `FirReferenceUtils.getCandidateSymbols` 与
 * `FirReferenceResolveHelper.toTargetSymbol`：
 * - 已解析引用的 `resolvedSymbol` 是导航目标；
 * - 错误命名引用只暴露诊断携带的真实候选，隐藏候选不暴露；
 * - 带候选的引用（`CfirNamedReferenceWithCandidate`，含 `CfirErrorReferenceWithCandidate`）
 *   持有的是解析过程中的候选而不是解析结果，Kotlin 侧对此返回 `emptyList()`；
 * - 错误占位符号（`CfirErrorCallableSymbol`，如 `CfirErrorNamedValueSymbol`）没有对应的公开符号形态，
 *   不能作为导航目标，对齐 Kotlin `FirConflictsHelpers.isCollectable` 的同一条排除规则。
 */
internal fun CfirReference.toCaTargetSymbols(symbolBuilder: CaSymbolByCfirBuilder): List<CaSymbol> = when (this) {
    is CfirResolvedNamedReference -> listOfNotNull(resolvedSymbol.toCaTargetSymbolOrNull(symbolBuilder))
    is CfirThisReference -> listOfNotNull(boundSymbol?.toCaTargetSymbolOrNull(symbolBuilder))
    is CfirErrorNamedReference -> diagnostic.getCaCandidateSymbols().mapNotNull { it.toCaTargetSymbolOrNull(symbolBuilder) }
    else -> emptyList()
}

/**
 * 错误命名引用携带的真实候选符号。
 *
 * 对齐 Kotlin `FirUtils.ConeDiagnostic.getCandidateSymbols`：隐藏候选不可导航，
 * 重复诊断继续向原始诊断取候选。
 */
internal fun ConeDiagnostic.getCaCandidateSymbols(): List<CfirBasedSymbol<*>> = when (this) {
    is ConeHiddenCandidateError -> emptyList()
    is ConeDiagnosticWithCandidates -> candidateSymbols.toList()
    is ConeUnreportedDuplicateDiagnostic -> original.getCaCandidateSymbols()
    else -> emptyList()
}

/** 错误占位符号不产出公开符号，导航目标需要真实声明。 */
internal fun CfirBasedSymbol<*>.toCaTargetSymbolOrNull(symbolBuilder: CaSymbolByCfirBuilder): CaSymbol? =
    if (this is CfirErrorCallableSymbol<*>) null else symbolBuilder.buildSymbol(this)
