package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
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
    /**
     * CFIR 符号到来源 PSI 的缓存。
     */
    private val sourcePsiCache = linkedMapOf<CfirBasedSymbol<*>, PsiElement?>()

    /**
     * PSI 元素到可恢复 CFIR 符号集合的缓存。
     */
    private val psiSymbolsCache = linkedMapOf<PsiElement, List<CfirBasedSymbol<*>>>()

    /**
     * CFIR 符号到其所在仓颉文件的缓存。
     */
    private val containingFileCache = linkedMapOf<CfirBasedSymbol<*>, CjFile?>()

    /**
     * PSI 元素到调用解析结果的缓存。
     */
    private val callInfoCache = linkedMapOf<PsiElement, CaCallInfo?>()

    /**
     * 元素级诊断查询结果缓存。
     */
    private val diagnosticsCache = linkedMapOf<CaCfirDiagnosticsQueryKey, List<CjPsiDiagnostic>>()

    /**
     * 文件级诊断查询结果缓存。
     */
    private val fileDiagnosticsCache = linkedMapOf<CaCfirFileDiagnosticsQueryKey, Collection<CjPsiDiagnostic>>()

    /**
     * 文件声明作用域缓存。
     */
    private val fileDeclaredScopeCache = linkedMapOf<CjFile, CfirContainingNamesAwareScope>()

    /**
     * 包作用域缓存。
     */
    private val packageScopeCache = linkedMapOf<FqName, CfirContainingNamesAwareScope?>()

    /**
     * 类声明成员作用域缓存。
     */
    private val declaredMemberScopeCache = linkedMapOf<ClassId, CfirContainingNamesAwareScope?>()

    /**
     * 类型成员作用域缓存。
     */
    private val memberScopeCache = linkedMapOf<ClassId, CfirTypeScope?>()

    /**
     * Cone 类型到类型作用域的缓存。
     */
    private val typeScopeCache = linkedMapOf<ConeCangJieType, CfirTypeScope?>()

    /**
     * 包可见性查询缓存。
     */
    private val packageVisibilityCache = linkedMapOf<FqName, Boolean>()

    /**
     * ClassId 到 CFIR class-like 符号的缓存。
     */
    private val classLikeSymbolCache = linkedMapOf<ClassId, CfirClassLikeSymbol<*>?>()

    /**
     * PSI 文件到 CFIR 文件符号的缓存。
     */
    private val fileSymbolCache = linkedMapOf<CjFile, CfirFileSymbol?>()

    /**
     * 表达式类型查询缓存。
     */
    private val expressionTypeCache = linkedMapOf<CjExpression, ConeCangJieType?>()

    /**
     * 可调用声明返回类型查询缓存。
     */
    private val declarationReturnTypeCache = linkedMapOf<CjCallableDeclaration, ConeCangJieType?>()

    /**
     * 值参数类型查询缓存。
     */
    private val valueParameterTypeCache = linkedMapOf<CjParameter, ConeCangJieType?>()

    /**
     * CFIR callable 符号返回类型查询缓存。
     */
    private val callableReturnTypeCache = linkedMapOf<CfirCallableSymbol<*>, ConeCangJieType?>()

    /**
     * class-like 符号默认类型查询缓存。
     */
    private val classLikeDefaultTypeCache = linkedMapOf<CfirClassLikeSymbol<*>, ConeCangJieType?>()

    /**
     * Cone 类型到其 class-like 符号的缓存。
     */
    private val typeClassLikeSymbolCache = linkedMapOf<ConeCangJieType, CfirClassLikeSymbol<*>?>()

    /**
     * class-like 符号直接超类型查询缓存。
     */
    private val classLikeSuperTypesCache = linkedMapOf<CfirClassLikeSymbol<*>, List<ConeCangJieType>>()

    /**
     * 获取或计算 CFIR 符号的来源 PSI。
     */
    fun getOrCreateSourcePsi(
        symbol: CfirBasedSymbol<*>,
        create: () -> PsiElement?,
    ): PsiElement? = getOrCreateCachedValue(sourcePsiCache, symbol, create)

    /**
     * 获取或计算 CFIR 符号所在的仓颉 PSI 文件。
     */
    fun getOrCreateContainingFile(
        symbol: CfirBasedSymbol<*>,
        create: () -> CjFile?,
    ): CjFile? = getOrCreateCachedValue(containingFileCache, symbol, create)

    /**
     * 获取或计算 PSI 元素对应的 CFIR 符号集合。
     */
    fun getOrCreatePsiSymbols(
        psi: PsiElement,
        create: () -> List<CfirBasedSymbol<*>>,
    ): List<CfirBasedSymbol<*>> = getOrCreateCachedValue(psiSymbolsCache, psi, create)

    /**
     * 获取或计算 PSI 元素对应的调用解析信息。
     */
    fun getOrCreateCallInfo(
        element: PsiElement,
        create: () -> CaCallInfo?,
    ): CaCallInfo? = getOrCreateCachedValue(callInfoCache, element, create)

    /**
     * 获取或计算元素级诊断列表。
     */
    fun getOrCreateDiagnostics(
        element: PsiElement,
        filter: DiagnosticCheckerFilter,
        create: () -> List<CjPsiDiagnostic>,
    ): List<CjPsiDiagnostic> = getOrCreateCachedValue(
        diagnosticsCache,
        CaCfirDiagnosticsQueryKey(element, filter),
        create,
    )

    /**
     * 获取或计算文件级诊断集合。
     */
    fun getOrCreateFileDiagnostics(
        file: CjFile,
        filter: DiagnosticCheckerFilter,
        create: () -> Collection<CjPsiDiagnostic>,
    ): Collection<CjPsiDiagnostic> = getOrCreateCachedValue(
        fileDiagnosticsCache,
        CaCfirFileDiagnosticsQueryKey(file, filter),
        create,
    )

    /**
     * 获取或计算文件声明作用域。
     */
    fun getOrCreateFileDeclaredScope(
        file: CjFile,
        create: () -> CfirContainingNamesAwareScope,
    ): CfirContainingNamesAwareScope = getOrCreateCachedValue(fileDeclaredScopeCache, file, create)

    /**
     * 获取或计算包作用域。
     */
    fun getOrCreatePackageScope(
        packageFqName: FqName,
        create: () -> CfirContainingNamesAwareScope?,
    ): CfirContainingNamesAwareScope? = getOrCreateCachedValue(packageScopeCache, packageFqName, create)

    /**
     * 获取或计算类声明成员作用域。
     */
    fun getOrCreateDeclaredMemberScope(
        classId: ClassId,
        create: () -> CfirContainingNamesAwareScope?,
    ): CfirContainingNamesAwareScope? = getOrCreateCachedValue(declaredMemberScopeCache, classId, create)

    /**
     * 获取或计算类型成员作用域。
     */
    fun getOrCreateMemberScope(
        classId: ClassId,
        create: () -> CfirTypeScope?,
    ): CfirTypeScope? = getOrCreateCachedValue(memberScopeCache, classId, create)

    /**
     * 获取或计算 Cone 类型对应的类型作用域。
     */
    fun getOrCreateTypeScope(
        type: ConeCangJieType,
        create: () -> CfirTypeScope?,
    ): CfirTypeScope? = getOrCreateCachedValue(typeScopeCache, type, create)

    /**
     * 获取或计算包可见性。
     */
    fun getOrCreatePackageVisibility(
        packageFqName: FqName,
        create: () -> Boolean,
    ): Boolean = getOrCreateCachedValue(packageVisibilityCache, packageFqName, create)

    /**
     * 获取或计算 ClassId 对应的 CFIR class-like 符号。
     */
    fun getOrCreateClassLikeSymbol(
        classId: ClassId,
        create: () -> CfirClassLikeSymbol<*>?,
    ): CfirClassLikeSymbol<*>? = getOrCreateCachedValue(classLikeSymbolCache, classId, create)

    /**
     * 获取或计算仓颉 PSI 文件对应的 CFIR 文件符号。
     */
    fun getOrCreateFileSymbol(
        file: CjFile,
        create: () -> CfirFileSymbol?,
    ): CfirFileSymbol? = getOrCreateCachedValue(fileSymbolCache, file, create)

    /**
     * 获取或计算表达式的 CFIR 类型。
     */
    fun getOrCreateExpressionType(
        expression: CjExpression,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(expressionTypeCache, expression, create)

    /**
     * 获取或计算可调用声明的返回类型。
     */
    fun getOrCreateDeclarationReturnType(
        declaration: CjCallableDeclaration,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(declarationReturnTypeCache, declaration, create)

    /**
     * 获取或计算值参数的 CFIR 类型。
     */
    fun getOrCreateValueParameterType(
        parameter: CjParameter,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(valueParameterTypeCache, parameter, create)

    /**
     * 获取或计算 callable 符号的返回类型。
     */
    fun getOrCreateCallableReturnType(
        symbol: CfirCallableSymbol<*>,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(callableReturnTypeCache, symbol, create)

    /**
     * 获取或计算 class-like 符号的默认类型。
     */
    fun getOrCreateClassLikeDefaultType(
        symbol: CfirClassLikeSymbol<*>,
        create: () -> ConeCangJieType?,
    ): ConeCangJieType? = getOrCreateCachedValue(classLikeDefaultTypeCache, symbol, create)

    /**
     * 获取或计算 Cone 类型对应的 class-like 符号。
     */
    fun getOrCreateTypeClassLikeSymbol(
        type: ConeCangJieType,
        create: () -> CfirClassLikeSymbol<*>?,
    ): CfirClassLikeSymbol<*>? = getOrCreateCachedValue(typeClassLikeSymbolCache, type, create)

    /**
     * 获取或计算 class-like 符号的直接超类型列表。
     */
    fun getOrCreateClassLikeSuperTypes(
        symbol: CfirClassLikeSymbol<*>,
        create: () -> List<ConeCangJieType>,
    ): List<ConeCangJieType> = getOrCreateCachedValue(classLikeSuperTypesCache, symbol, create)

    /**
     * 在线程安全区间内按键读取缓存，缺失时计算并回填。
     */
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
