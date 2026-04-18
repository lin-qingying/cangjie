package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.cfir.signatures.CaCfirSubstitutedSignatureCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPublicSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.completionDecisionKey
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirSubstitutorCacheKey
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile

/**
 * 会话级缓存访问入口。
 *
 * 这层只负责把组件侧需要的缓存语义映射到 `cacheStorage`，
 * 不承载解析、类型推导、作用域查询等职责。
 */
internal fun <S : CaSymbol> CaCfirSession.getOrCreatePublicSymbol(
    key: CaCfirPublicSymbolCacheKey,
    create: () -> S,
): S = cacheStorage.getOrCreatePublicSymbol(key, create)

internal fun CaCfirSession.getOrCreateTopLevelSymbolQuery(
    packageFqName: FqName,
    name: Name,
    create: () -> CaCfirTopLevelPublicSymbolQueryValue,
): CaCfirTopLevelPublicSymbolQueryValue =
    cacheStorage.getOrCreateTopLevelSymbolQuery(packageFqName, name, create)

internal fun CaCfirSession.getOrCreateDocumentation(
    key: CaCfirPublicSymbolCacheKey,
    create: () -> String?,
): String? = cacheStorage.getOrCreateDocumentation(key, create)

internal fun CaCfirSession.getOrCreateDeclarationAnnotations(
    owner: PsiElement,
    create: () -> List<CaAnnotation>,
): List<CaAnnotation> = cacheStorage.getOrCreateDeclarationAnnotations(owner, create)

internal fun CaCfirSession.getOrCreateDeclarationSignature(
    declaration: CjCallableDeclaration,
    create: () -> CaSignature<CaCallableSymbol>?,
): CaSignature<CaCallableSymbol>? = cacheStorage.getOrCreateDeclarationSignature(declaration, create)

internal fun CaCfirSession.getOrCreateCallableSignature(
    key: CaCfirCallableSymbolCacheKey,
    create: () -> CaSignature<CaCallableSymbol>,
): CaSignature<CaCallableSymbol> = cacheStorage.getOrCreateCallableSignature(key, create)

internal fun CaCfirSession.getOrCreateSubstitutedSignature(
    key: CaCfirSubstitutedSignatureCacheKey,
    create: () -> CaSignature<CaCallableSymbol>,
): CaSignature<CaCallableSymbol> = cacheStorage.getOrCreateSubstitutedSignature(key, create)

internal fun CaCfirSession.getOrCreateSubstitutor(
    key: CaCfirSubstitutorCacheKey,
    create: () -> CaSubstitutor,
): CaSubstitutor = cacheStorage.getOrCreateSubstitutor(key, create)

internal fun CaCfirSession.getOrCreateDefaultImports(
    create: () -> CaDefaultImports,
): CaDefaultImports = cacheStorage.getOrCreateDefaultImports(create)

internal fun CaCfirSession.getOrCreateDataFlowInfo(
    expression: CjExpression,
    create: () -> CaDataFlowInfo,
): CaDataFlowInfo = cacheStorage.getOrCreateDataFlowInfo(expression, create)

internal fun CaCfirSession.getOrCreateCompletionDecision(
    symbol: CaSymbol,
    position: PsiElement,
    create: () -> CaCompletionCandidateDecision,
): CaCompletionCandidateDecision = cacheStorage.getOrCreateCompletionDecision(
    symbolKey = symbol.completionDecisionKey(),
    position = position,
    create = create,
)

internal fun CaCfirSession.getOrCreateReferenceShorteningPlan(
    file: CjFile,
    create: () -> CaReferenceShorteningPlan,
): CaReferenceShorteningPlan = cacheStorage.getOrCreateReferenceShorteningPlan(file, create)

internal fun CaCfirSession.getOrCreateImportOptimizationPlan(
    file: CjFile,
    create: () -> CaImportOptimizationPlan,
): CaImportOptimizationPlan = cacheStorage.getOrCreateImportOptimizationPlan(file, create)

internal fun CaCfirSession.getOrCreateInteropInfo(
    element: PsiElement,
    create: () -> CaInteropInfo?,
): CaInteropInfo? = cacheStorage.getOrCreateInteropInfo(element, create)

internal fun CaCfirSession.getOrCreateSymbolInteropInfo(
    key: CaCfirPublicSymbolCacheKey,
    create: () -> CaInteropInfo?,
): CaInteropInfo? = cacheStorage.getOrCreateSymbolInteropInfo(key, create)
