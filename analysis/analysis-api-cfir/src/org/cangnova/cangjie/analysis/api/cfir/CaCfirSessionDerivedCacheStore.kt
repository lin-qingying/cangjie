package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCallableSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirCompletionSymbolKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirPublicSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSubstitutedSignatureCacheKey
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirTypeSubstitutorCacheKey
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.substitution.CaSubstitutedSignature
import org.cangnova.cangjie.analysis.api.substitution.CaTypeSubstitutor
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile

/**
 * 会话内派生语义缓存。
 *
 * 这里缓存的是“由 low-level 结果进一步映射或加工得到”的公开语义对象，
 * 例如公开符号、签名、替换器、导入规划、数据流与互操作信息。
 */
internal class CaCfirSessionDerivedCacheStore {
    private val publicSymbolCache = linkedMapOf<CaCfirPublicSymbolCacheKey, CaSymbol>()
    private val topLevelSymbolQueryCache = linkedMapOf<CaCfirTopLevelPublicSymbolQueryKey, CaCfirTopLevelPublicSymbolQueryValue>()
    private val documentationCache = linkedMapOf<CaCfirPublicSymbolCacheKey, String?>()
    private val declarationAnnotationsCache = linkedMapOf<PsiElement, List<CaAnnotation>>()
    private val declarationSignatureCache = linkedMapOf<CjCallableDeclaration, CaSignature>()
    private val callableSignatureCache = linkedMapOf<CaCfirCallableSymbolCacheKey, CaSignature?>()
    private val substitutedSignatureCache = linkedMapOf<CaCfirSubstitutedSignatureCacheKey, CaSubstitutedSignature>()
    private val typeSubstitutorCache = linkedMapOf<CaCfirTypeSubstitutorCacheKey, CaTypeSubstitutor>()
    private val dataFlowInfoCache = linkedMapOf<CjExpression, CaDataFlowInfo>()
    private val completionDecisionCache = linkedMapOf<CaCfirCompletionDecisionKey, CaCompletionCandidateDecision>()
    private val referenceShorteningPlanCache = linkedMapOf<CjFile, CaReferenceShorteningPlan>()
    private val importOptimizationPlanCache = linkedMapOf<CjFile, CaImportOptimizationPlan>()
    private val interopInfoCache = linkedMapOf<PsiElement, CaInteropInfo?>()
    private val symbolInteropInfoCache = linkedMapOf<CaCfirPublicSymbolCacheKey, CaInteropInfo?>()
    private var defaultImportsCache: CaDefaultImports? = null

    fun <S : CaSymbol> getOrCreatePublicSymbol(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> S,
    ): S {
        synchronized(publicSymbolCache) {
            val cached = publicSymbolCache[key]
            if (cached != null) {
                @Suppress("UNCHECKED_CAST")
                return cached as S
            }

            return create().also { publicSymbolCache[key] = it }
        }
    }

    fun getOrCreateTopLevelSymbolQuery(
        packageFqName: FqName,
        name: Name,
        create: () -> CaCfirTopLevelPublicSymbolQueryValue,
    ): CaCfirTopLevelPublicSymbolQueryValue = getOrCreateCachedValue(
        topLevelSymbolQueryCache,
        CaCfirTopLevelPublicSymbolQueryKey(packageFqName, name),
        create,
    )

    fun getOrCreateDocumentation(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> String?,
    ): String? = getOrCreateCachedValue(documentationCache, key, create)

    fun getOrCreateDeclarationAnnotations(
        owner: PsiElement,
        create: () -> List<CaAnnotation>,
    ): List<CaAnnotation> = getOrCreateCachedValue(declarationAnnotationsCache, owner, create)

    fun getOrCreateDeclarationSignature(
        declaration: CjCallableDeclaration,
        create: () -> CaSignature,
    ): CaSignature = getOrCreateCachedValue(declarationSignatureCache, declaration, create)

    fun getOrCreateCallableSignature(
        key: CaCfirCallableSymbolCacheKey,
        create: () -> CaSignature?,
    ): CaSignature? = getOrCreateCachedValue(callableSignatureCache, key, create)

    fun getOrCreateSubstitutedSignature(
        key: CaCfirSubstitutedSignatureCacheKey,
        create: () -> CaSubstitutedSignature,
    ): CaSubstitutedSignature = getOrCreateCachedValue(substitutedSignatureCache, key, create)

    fun getOrCreateTypeSubstitutor(
        key: CaCfirTypeSubstitutorCacheKey,
        create: () -> CaTypeSubstitutor,
    ): CaTypeSubstitutor = getOrCreateCachedValue(typeSubstitutorCache, key, create)

    fun getOrCreateDefaultImports(create: () -> CaDefaultImports): CaDefaultImports {
        synchronized(this) {
            val cached = defaultImportsCache
            if (cached != null) return cached
            return create().also { defaultImportsCache = it }
        }
    }

    fun getOrCreateDataFlowInfo(
        expression: CjExpression,
        create: () -> CaDataFlowInfo,
    ): CaDataFlowInfo = getOrCreateCachedValue(dataFlowInfoCache, expression, create)

    fun getOrCreateCompletionDecision(
        symbolKey: CaCfirCompletionSymbolKey,
        position: PsiElement,
        create: () -> CaCompletionCandidateDecision,
    ): CaCompletionCandidateDecision = getOrCreateCachedValue(
        completionDecisionCache,
        CaCfirCompletionDecisionKey(symbolKey, position),
        create,
    )

    fun getOrCreateReferenceShorteningPlan(
        file: CjFile,
        create: () -> CaReferenceShorteningPlan,
    ): CaReferenceShorteningPlan = getOrCreateCachedValue(referenceShorteningPlanCache, file, create)

    fun getOrCreateImportOptimizationPlan(
        file: CjFile,
        create: () -> CaImportOptimizationPlan,
    ): CaImportOptimizationPlan = getOrCreateCachedValue(importOptimizationPlanCache, file, create)

    fun getOrCreateInteropInfo(
        element: PsiElement,
        create: () -> CaInteropInfo?,
    ): CaInteropInfo? = getOrCreateCachedValue(interopInfoCache, element, create)

    fun getOrCreateSymbolInteropInfo(
        key: CaCfirPublicSymbolCacheKey,
        create: () -> CaInteropInfo?,
    ): CaInteropInfo? = getOrCreateCachedValue(symbolInteropInfoCache, key, create)

    private fun <K, V> getOrCreateCachedValue(
        cache: LinkedHashMap<K, V>,
        key: K,
        create: () -> V,
    ): V {
        synchronized(cache) {
            if (cache.containsKey(key)) {
                @Suppress("UNCHECKED_CAST")
                return cache[key] as V
            }

            return create().also { cache[key] = it }
        }
    }
}
