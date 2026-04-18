package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPublicSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirCompletionSymbolKey
import org.cangnova.cangjie.analysis.api.cfir.signatures.CaCfirSubstitutedSignatureCacheKey
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallInfoSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.DiagnosticCheckerFilter
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
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter

/**
 * `analysis-api-cfir` 的统一 session 缓存编排层。
 *
 * 该类不再直接维护全部缓存实现，而是只负责把：
 * 1. 派生公开语义缓存；
 * 2. low-level 查询结果缓存；
 * 统一组合成 session 内部协议。
 */
internal class CaCfirSessionCacheStore(
    private val derivedCacheStore: CaCfirSessionDerivedCacheStore = CaCfirSessionDerivedCacheStore(),
    private val semanticCacheStore: CaCfirSessionSemanticCacheStore = CaCfirSessionSemanticCacheStore(),
) {
    fun <S : CaSymbol> getOrCreatePublicSymbol(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> S,
    ): S = derivedCacheStore.getOrCreatePublicSymbol(key, create)

    fun getOrCreateTopLevelSymbolQuery(
        packageFqName: FqName,
        name: Name,
        create: () -> CaCfirTopLevelPublicSymbolQueryValue,
    ): CaCfirTopLevelPublicSymbolQueryValue =
        derivedCacheStore.getOrCreateTopLevelSymbolQuery(packageFqName, name, create)

    fun getOrCreateDocumentation(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> String?,
    ): String? = derivedCacheStore.getOrCreateDocumentation(key, create)

    fun getOrCreateDeclarationAnnotations(
        owner: PsiElement,
        create: () -> List<CaAnnotation>,
    ): List<CaAnnotation> = derivedCacheStore.getOrCreateDeclarationAnnotations(owner, create)

    fun getOrCreateDeclarationSignature(
        declaration: CjCallableDeclaration,
        create: () -> CaSignature<CaCallableSymbol>?,
    ): CaSignature<CaCallableSymbol>? = derivedCacheStore.getOrCreateDeclarationSignature(declaration, create)

    fun getOrCreateCallableSignature(
        key: CaCfirCallableSymbolCacheKey,
        create: () -> CaSignature<CaCallableSymbol>,
    ): CaSignature<CaCallableSymbol> = derivedCacheStore.getOrCreateCallableSignature(key, create)

    fun getOrCreateSubstitutedSignature(
        key: CaCfirSubstitutedSignatureCacheKey,
        create: () -> CaSignature<CaCallableSymbol>,
    ): CaSignature<CaCallableSymbol> = derivedCacheStore.getOrCreateSubstitutedSignature(key, create)

    fun getOrCreateSubstitutor(
        key: CaCfirSubstitutorCacheKey,
        create: () -> CaSubstitutor,
    ): CaSubstitutor = derivedCacheStore.getOrCreateSubstitutor(key, create)

    fun getOrCreateDefaultImports(create: () -> CaDefaultImports): CaDefaultImports =
        derivedCacheStore.getOrCreateDefaultImports(create)

    fun getOrCreateDataFlowInfo(
        expression: CjExpression,
        create: () -> CaDataFlowInfo,
    ): CaDataFlowInfo = derivedCacheStore.getOrCreateDataFlowInfo(expression, create)

    fun getOrCreateCompletionDecision(
        symbolKey: CaCfirCompletionSymbolKey,
        position: PsiElement,
        create: () -> CaCompletionCandidateDecision,
    ): CaCompletionCandidateDecision =
        derivedCacheStore.getOrCreateCompletionDecision(symbolKey, position, create)

    fun getOrCreateReferenceShorteningPlan(
        file: CjFile,
        create: () -> CaReferenceShorteningPlan,
    ): CaReferenceShorteningPlan = derivedCacheStore.getOrCreateReferenceShorteningPlan(file, create)

    fun getOrCreateImportOptimizationPlan(
        file: CjFile,
        create: () -> CaImportOptimizationPlan,
    ): CaImportOptimizationPlan = derivedCacheStore.getOrCreateImportOptimizationPlan(file, create)

    fun getOrCreateInteropInfo(
        element: PsiElement,
        create: () -> CaInteropInfo?,
    ): CaInteropInfo? = derivedCacheStore.getOrCreateInteropInfo(element, create)

    fun getOrCreateSymbolInteropInfo(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> CaInteropInfo?,
    ): CaInteropInfo? = derivedCacheStore.getOrCreateSymbolInteropInfo(key, create)

    fun getOrCreateSourcePsi(
        symbol: CfirBasedSymbol<*>,
        create: () -> PsiElement?,
    ): PsiElement? = semanticCacheStore.getOrCreateSourcePsi(symbol, create)

    fun getOrCreateContainingFile(
        symbol: CfirBasedSymbol<*>,
        create: () -> CjFile?,
    ): CjFile? = semanticCacheStore.getOrCreateContainingFile(symbol, create)

    fun getOrCreatePsiSymbols(
        psi: PsiElement,
        create: () -> List<CfirBasedSymbol<*>>,
    ): List<CfirBasedSymbol<*>> = semanticCacheStore.getOrCreatePsiSymbols(psi, create)

    fun getOrCreateCallInfo(
        element: PsiElement,
        create: () -> CaCfirCallInfoSnapshot?,
    ): CaCfirCallInfoSnapshot? = semanticCacheStore.getOrCreateCallInfo(element, create)

    fun getOrCreateDiagnostics(
        element: PsiElement,
        filter: DiagnosticCheckerFilter,
        create: () -> List<CjPsiDiagnostic>,
    ): List<CjPsiDiagnostic> = semanticCacheStore.getOrCreateDiagnostics(element, filter, create)

    fun getOrCreateFileDiagnostics(
        file: CjFile,
        filter: DiagnosticCheckerFilter,
        create: () -> Collection<CjPsiDiagnostic>,
    ): Collection<CjPsiDiagnostic> = semanticCacheStore.getOrCreateFileDiagnostics(file, filter, create)

    fun getOrCreateFileDeclaredScope(
        file: CjFile,
        create: () -> CfirContainingNamesAwareScope,
    ): CfirContainingNamesAwareScope = semanticCacheStore.getOrCreateFileDeclaredScope(file, create)

    fun getOrCreatePackageScope(
        packageFqName: FqName,
        create: () -> CfirContainingNamesAwareScope?,
    ): CfirContainingNamesAwareScope? = semanticCacheStore.getOrCreatePackageScope(packageFqName, create)

    fun getOrCreateDeclaredMemberScope(
        classId: ClassId,
        create: () -> CfirContainingNamesAwareScope?,
    ): CfirContainingNamesAwareScope? = semanticCacheStore.getOrCreateDeclaredMemberScope(classId, create)

    fun getOrCreateMemberScope(
        classId: ClassId,
        create: () -> CfirTypeScope?,
    ): CfirTypeScope? = semanticCacheStore.getOrCreateMemberScope(classId, create)

    fun getOrCreateTypeScope(
        type: ConeCangJieType,
        create: () -> CfirTypeScope?,
    ): CfirTypeScope? = semanticCacheStore.getOrCreateTypeScope(type, create)

    fun getOrCreatePackageVisibility(
        packageFqName: FqName,
        create: () -> Boolean,
    ): Boolean = semanticCacheStore.getOrCreatePackageVisibility(packageFqName, create)

    fun getOrCreateClassLikeSymbol(
        classId: ClassId,
        create: () -> CfirClassLikeSymbol<*>?,
    ): CfirClassLikeSymbol<*>? = semanticCacheStore.getOrCreateClassLikeSymbol(classId, create)

    fun getOrCreateFileSymbol(
        file: CjFile,
        create: () -> CfirFileSymbol?,
    ): CfirFileSymbol? = semanticCacheStore.getOrCreateFileSymbol(file, create)

    fun getOrCreateExpressionType(
        expression: CjExpression,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = semanticCacheStore.getOrCreateExpressionType(expression, create)

    fun getOrCreateDeclarationReturnType(
        declaration: CjCallableDeclaration,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = semanticCacheStore.getOrCreateDeclarationReturnType(declaration, create)

    fun getOrCreateValueParameterType(
        parameter: CjParameter,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = semanticCacheStore.getOrCreateValueParameterType(parameter, create)

    fun getOrCreateCallableReturnType(
        symbol: CfirCallableSymbol<*>,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = semanticCacheStore.getOrCreateCallableReturnType(symbol, create)

    fun getOrCreateClassLikeDefaultType(
        symbol: CfirClassLikeSymbol<*>,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = semanticCacheStore.getOrCreateClassLikeDefaultType(symbol, create)

    fun getOrCreateTypeClassLikeSymbol(
        type: ConeCangJieType,
        create: () -> CfirClassLikeSymbol<*>?,
    ): CfirClassLikeSymbol<*>? = semanticCacheStore.getOrCreateTypeClassLikeSymbol(type, create)

    fun getOrCreateClassLikeSuperTypes(
        symbol: CfirClassLikeSymbol<*>,
        create: () -> List<ConeCangJieType>,
    ): List<ConeCangJieType> = semanticCacheStore.getOrCreateClassLikeSuperTypes(symbol, create)
}
