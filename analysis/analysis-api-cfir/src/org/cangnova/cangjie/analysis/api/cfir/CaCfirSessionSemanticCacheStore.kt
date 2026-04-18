package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallInfoSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.DiagnosticCheckerFilter
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
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter

/**
 * 会话内 low-level 查询结果缓存。
 *
 * 这里缓存的是直接来自 low-level facade 的稳定查询结果，
 * 包括诊断、作用域、符号回查、源码导航与类型查询快照。
 */
internal class CaCfirSessionSemanticCacheStore {
    private val sourcePsiCache = linkedMapOf<CfirBasedSymbol<*>, PsiElement?>()
    private val psiSymbolsCache = linkedMapOf<PsiElement, List<CfirBasedSymbol<*>>>()
    private val containingFileCache = linkedMapOf<CfirBasedSymbol<*>, CjFile?>()
    private val callInfoCache = linkedMapOf<PsiElement, CaCfirCallInfoSnapshot?>()
    private val diagnosticsCache = linkedMapOf<CaCfirDiagnosticsQueryKey, List<CjPsiDiagnostic>>()
    private val fileDiagnosticsCache = linkedMapOf<CaCfirFileDiagnosticsQueryKey, Collection<CjPsiDiagnostic>>()
    private val fileDeclaredScopeCache = linkedMapOf<CjFile, CfirContainingNamesAwareScope>()
    private val packageScopeCache = linkedMapOf<FqName, CfirContainingNamesAwareScope?>()
    private val declaredMemberScopeCache = linkedMapOf<ClassId, CfirContainingNamesAwareScope?>()
    private val memberScopeCache = linkedMapOf<ClassId, CfirTypeScope?>()
    private val typeScopeCache = linkedMapOf<ConeCangJieType, CfirTypeScope?>()
    private val packageVisibilityCache = linkedMapOf<FqName, Boolean>()
    private val classLikeSymbolCache = linkedMapOf<ClassId, CfirClassLikeSymbol<*>?>()
    private val fileSymbolCache = linkedMapOf<CjFile, CfirFileSymbol?>()
    private val expressionTypeCache = linkedMapOf<CjExpression, ConeCangJieType?>()
    private val declarationReturnTypeCache = linkedMapOf<CjCallableDeclaration, ConeCangJieType?>()
    private val valueParameterTypeCache = linkedMapOf<CjParameter, ConeCangJieType?>()
    private val callableReturnTypeCache = linkedMapOf<CfirCallableSymbol<*>, ConeCangJieType?>()
    private val classLikeDefaultTypeCache = linkedMapOf<CfirClassLikeSymbol<*>, ConeCangJieType?>()
    private val typeClassLikeSymbolCache = linkedMapOf<ConeCangJieType, CfirClassLikeSymbol<*>?>()
    private val classLikeSuperTypesCache = linkedMapOf<CfirClassLikeSymbol<*>, List<ConeCangJieType>>()

    fun getOrCreateSourcePsi(
        symbol: CfirBasedSymbol<*>,
        create: () -> PsiElement?,
    ): PsiElement? = getOrCreateCachedValue(sourcePsiCache, symbol, create)

    fun getOrCreateContainingFile(
        symbol: CfirBasedSymbol<*>,
        create: () -> CjFile?,
    ): CjFile? = getOrCreateCachedValue(containingFileCache, symbol, create)

    fun getOrCreatePsiSymbols(
        psi: PsiElement,
        create: () -> List<CfirBasedSymbol<*>>,
    ): List<CfirBasedSymbol<*>> = getOrCreateCachedValue(psiSymbolsCache, psi, create)

    fun getOrCreateCallInfo(
        element: PsiElement,
        create: () -> CaCfirCallInfoSnapshot?,
    ): CaCfirCallInfoSnapshot? = getOrCreateCachedValue(callInfoCache, element, create)

    fun getOrCreateDiagnostics(
        element: PsiElement,
        filter: DiagnosticCheckerFilter,
        create: () -> List<CjPsiDiagnostic>,
    ): List<CjPsiDiagnostic> = getOrCreateCachedValue(
        diagnosticsCache,
        CaCfirDiagnosticsQueryKey(element, filter),
        create,
    )

    fun getOrCreateFileDiagnostics(
        file: CjFile,
        filter: DiagnosticCheckerFilter,
        create: () -> Collection<CjPsiDiagnostic>,
    ): Collection<CjPsiDiagnostic> = getOrCreateCachedValue(
        fileDiagnosticsCache,
        CaCfirFileDiagnosticsQueryKey(file, filter),
        create,
    )

    fun getOrCreateFileDeclaredScope(
        file: CjFile,
        create: () -> CfirContainingNamesAwareScope,
    ): CfirContainingNamesAwareScope = getOrCreateCachedValue(fileDeclaredScopeCache, file, create)

    fun getOrCreatePackageScope(
        packageFqName: FqName,
        create: () -> CfirContainingNamesAwareScope?,
    ): CfirContainingNamesAwareScope? = getOrCreateCachedValue(packageScopeCache, packageFqName, create)

    fun getOrCreateDeclaredMemberScope(
        classId: ClassId,
        create: () -> CfirContainingNamesAwareScope?,
    ): CfirContainingNamesAwareScope? = getOrCreateCachedValue(declaredMemberScopeCache, classId, create)

    fun getOrCreateMemberScope(
        classId: ClassId,
        create: () -> CfirTypeScope?,
    ): CfirTypeScope? = getOrCreateCachedValue(memberScopeCache, classId, create)

    fun getOrCreateTypeScope(
        type: ConeCangJieType,
        create: () -> CfirTypeScope?,
    ): CfirTypeScope? = getOrCreateCachedValue(typeScopeCache, type, create)

    fun getOrCreatePackageVisibility(
        packageFqName: FqName,
        create: () -> Boolean,
    ): Boolean = getOrCreateCachedValue(packageVisibilityCache, packageFqName, create)

    fun getOrCreateClassLikeSymbol(
        classId: ClassId,
        create: () -> CfirClassLikeSymbol<*>?,
    ): CfirClassLikeSymbol<*>? = getOrCreateCachedValue(classLikeSymbolCache, classId, create)

    fun getOrCreateFileSymbol(
        file: CjFile,
        create: () -> CfirFileSymbol?,
    ): CfirFileSymbol? = getOrCreateCachedValue(fileSymbolCache, file, create)

    fun getOrCreateExpressionType(
        expression: CjExpression,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(expressionTypeCache, expression, create)

    fun getOrCreateDeclarationReturnType(
        declaration: CjCallableDeclaration,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(declarationReturnTypeCache, declaration, create)

    fun getOrCreateValueParameterType(
        parameter: CjParameter,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(valueParameterTypeCache, parameter, create)

    fun getOrCreateCallableReturnType(
        symbol: CfirCallableSymbol<*>,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(callableReturnTypeCache, symbol, create)

    fun getOrCreateClassLikeDefaultType(
        symbol: CfirClassLikeSymbol<*>,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(classLikeDefaultTypeCache, symbol, create)

    fun getOrCreateTypeClassLikeSymbol(
        type: ConeCangJieType,
        create: () -> CfirClassLikeSymbol<*>?,
    ): CfirClassLikeSymbol<*>? = getOrCreateCachedValue(typeClassLikeSymbolCache, type, create)

    fun getOrCreateClassLikeSuperTypes(
        symbol: CfirClassLikeSymbol<*>,
        create: () -> List<ConeCangJieType>,
    ): List<ConeCangJieType> = getOrCreateCachedValue(classLikeSuperTypesCache, symbol, create)

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
