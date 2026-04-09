package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 默认的 CFIR low-level 解析 facade 实现。
 *
 * facade 只负责组合：
 * 1. 当前 use-site 的 session 与模块闭包；
 * 2. source navigation；
 * 3. 可见符号查询；
 * 4. scope snapshot；
 * 5. 语义查询索引；
 * 6. 诊断快照。
 *
 * 底层 visitor、调用快照归一化、诊断分桶等细节都下沉到独立提供器中。
 */
internal class CaCfirResolutionFacadeImpl internal constructor(
    override val useSiteModule: CaModule,
    override val useSiteFirSession: CfirSession,
    override val allModules: Set<CaModule>,
    override val cfirFiles: List<CfirFile>,
    diagnosticsProvider: () -> DiagnosticBuckets,
    private val scopeProvider: CaCfirScopeSnapshotProvider,
    private val visibleSymbolProvider: CaCfirVisibleSymbolProvider,
    private val sourceNavigationProvider: CaCfirSourceNavigationProvider,
) : CaCfirResolutionFacade {
    /**
     * diagnostics 按需初始化，避免 static use-site 在只读链路中被无关的 checker 初始化拖入。
     */
    private val diagnostics: DiagnosticBuckets by lazy(LazyThreadSafetyMode.NONE, diagnosticsProvider)
    /**
     * low-level 类型关系由 analysis 自己持有的关系引擎负责。
     */
    private val typeRelations: CaCfirTypeRelations by lazy(LazyThreadSafetyMode.NONE) {
        CaCfirTypeRelations(::getDirectSuperTypes)
    }

    private val cfirFilesByPsi: Map<CjFile, CfirFile> = cfirFiles.mapNotNull { cfirFile ->
        val psiFile = cfirFile.source?.psi as? CjFile ?: return@mapNotNull null
        psiFile to cfirFile
    }.toMap()

    /**
     * PSI -> CFIR 的稳定语义查询入口。
     */
    private val semanticQueries: CaCfirSemanticQueryProvider by lazy(LazyThreadSafetyMode.NONE) {
        CaCfirSemanticQueryProvider(cfirFiles)
    }

    override fun getCfirFile(file: CjFile): CfirFile? = cfirFilesByPsi[file]

    override fun getFileSymbol(file: CjFile): CfirFileSymbol? =
        getCfirFile(file)?.symbol

    override fun getFileScope(file: CjFile): CaCfirScopeSnapshot =
        scopeProvider.getFileScope(file)

    override fun getPackageScope(packageFqName: FqName): CaCfirScopeSnapshot? =
        scopeProvider.getPackageScope(packageFqName)

    override fun getDeclaredMemberScope(classId: ClassId): CaCfirScopeSnapshot? =
        scopeProvider.getDeclaredMemberScope(classId)

    override fun getMemberScope(classId: ClassId): CaCfirScopeSnapshot? =
        scopeProvider.getMemberScope(classId)

    override fun getTypeScope(type: ConeCangJieType): CaCfirScopeSnapshot? =
        scopeProvider.getTypeScope(type)

    override fun hasPackage(packageFqName: FqName): Boolean =
        visibleSymbolProvider.hasPackage(packageFqName)

    override fun getClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>? =
        visibleSymbolProvider.getClassLikeSymbolByClassId(classId)

    override fun getTopLevelClassLikeSymbols(packageFqName: FqName, name: Name): List<CfirClassLikeSymbol<*>> =
        visibleSymbolProvider.getTopLevelClassifierSymbols(packageFqName, name)

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> =
        visibleSymbolProvider.getTopLevelCallableSymbols(packageFqName, name)

    override fun getTopLevelSymbols(packageFqName: FqName, name: Name): CaCfirTopLevelSymbolQueryResult =
        visibleSymbolProvider.getTopLevelSymbols(packageFqName, name)

    override fun findSourcePsi(symbol: CfirSymbol<*>): PsiElement? =
        sourceNavigationProvider.findPsi(symbol)

    override fun getDeclarationSymbols(psi: PsiElement): List<CfirSymbol<*>> =
        semanticQueries.getDeclarationSymbols(psi)

    override fun getContainingFile(symbol: CfirSymbol<*>): CjFile? =
        sourceNavigationProvider.getContainingFile(symbol)

    override fun getExpressionType(expression: CjExpression): ConeCangJieType? =
        semanticQueries.getExpressionType(expression)

    override fun getDeclarationReturnType(declaration: CjCallableDeclaration): ConeCangJieType? =
        semanticQueries.getDeclarationReturnType(declaration)

    override fun getValueParameterType(parameter: CjParameter): ConeCangJieType? =
        semanticQueries.getValueParameterType(parameter)

    override fun getClassDefaultType(declaration: CjClassLikeDeclaration): ConeCangJieType? =
        semanticQueries.getClassDefaultType(declaration)

    override fun getCallableReturnType(symbol: CfirCallableSymbol<*>): ConeCangJieType? {
        val directType = symbol.resolvedReturnTypeRef.coneType
        if (directType !is ConeErrorType) {
            return directType
        }

        val sourceDeclaration = sourceNavigationProvider.findPsi(symbol) as? CjCallableDeclaration
            ?: return directType
        return semanticQueries.getDeclarationReturnType(sourceDeclaration) ?: directType
    }

    override fun getClassLikeDefaultType(symbol: CfirClassLikeSymbol<*>): ConeCangJieType? =
        symbol.constructType()

    override fun getTypeClassLikeSymbol(type: ConeCangJieType): CfirClassLikeSymbol<*>? {
        val classId = type.classIdOrPrimitiveClassId ?: return null
        return visibleSymbolProvider.getClassLikeSymbolByClassId(classId)
    }

    override fun getClassLikeSuperTypes(symbol: CfirClassLikeSymbol<*>): List<ConeCangJieType> {
        if (!symbol.isBound) return emptyList()
        symbol.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)

        val declarationType = symbol.constructType()
        val providerTypes = useSiteFirSession.typeAwareSupertypeProviderOrNull
            ?.getDirectSupertypes(declarationType)
            ?.takeIf(List<ConeCangJieType>::isNotEmpty)
        if (providerTypes != null) return providerTypes

        return symbol.cfir.superTypeRefs
            .mapNotNull { typeRef -> (typeRef as? CfirResolvedTypeRef)?.coneType }
    }

    override fun getDirectlyOverriddenCallableSymbols(symbol: CfirCallableSymbol<*>): List<CfirCallableSymbol<*>> {
        val extendOverrides = getDirectlyOverriddenExtendCallableSymbols(symbol)
        if (extendOverrides.isNotEmpty()) {
            return extendOverrides
        }

        val ownerClassId = symbol.overrideOwnerClassId(useSiteFirSession) ?: return emptyList()
        val memberTypeScope = scopeProvider.getMemberTypeScope(ownerClassId) ?: return emptyList()

        val directOverrides: List<CfirCallableSymbol<*>> = when (symbol) {
            is CfirFunctionSymbol<*> -> memberTypeScope.collectStableDirectOverriddenFunctions(symbol)

            is CfirPropertySymbol -> buildList {
                memberTypeScope.processDirectOverriddenPropertiesWithBaseScope(symbol) { overridden, _ ->
                    add(overridden)
                    ProcessorAction.NEXT
                }
            }

            else -> emptyList()
        }

        return directOverrides.distinctBy { overridden ->
            overridden.callableId.toString()
        }
    }

    override fun isSubTypeOf(
        subType: ConeCangJieType,
        superType: ConeCangJieType,
    ): Boolean {
        return typeRelations.isSubTypeOf(subType, superType)
    }

    override fun areTypesEqual(
        left: ConeCangJieType,
        right: ConeCangJieType,
    ): Boolean {
        return typeRelations.areTypesEqual(left, right)
    }

    override fun resolveReference(reference: CjReferenceExpression): Collection<CfirSymbol<*>> =
        semanticQueries.resolveReference(reference)

    override fun getCallInfo(element: PsiElement): CaCfirCallInfoSnapshot? =
        semanticQueries.getCallInfo(element)

    override fun getDiagnostics(element: PsiElement, filter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        return diagnostics.forFilter(filter)
            .filter { diagnostic ->
                diagnostic.psiElement == element || diagnostic.psiElement.isAncestorOf(element)
            }
    }

    override fun collectDiagnosticsForFile(file: CjFile, filter: DiagnosticCheckerFilter): Collection<CjPsiDiagnostic> {
        return diagnostics.forFilter(filter)
            .filter { it.psiFile == file }
    }

    /**
     * 统一取得类型关系引擎使用的 direct supertypes。
     */
    private fun getDirectSuperTypes(type: ConeCangJieType): List<ConeCangJieType> {
        val providerTypes = useSiteFirSession.typeAwareSupertypeProviderOrNull
            ?.getDirectSupertypes(type)
            ?.takeIf(List<ConeCangJieType>::isNotEmpty)
        if (providerTypes != null) return providerTypes

        if (type is ConeTypeParameterType) {
            return type.lookupTag.symbol.resolvedBounds.map(CfirResolvedTypeRef::coneType)
        }

        val classId = type.classIdOrPrimitiveClassId ?: return emptyList()
        val symbol = visibleSymbolProvider.getClassLikeSymbolByClassId(classId) ?: return emptyList()
        return getClassLikeSuperTypes(symbol)
    }

    /**
     * extend 成员不属于 class 声明体本身，因此不能直接复用 class member scope 的 parentScopes。
     *
     * 这里按 extend 自己声明的 `superTypeRefs` 恢复直接覆写候选，
     * 让 extend 成员与普通 class member 一样，都能进入统一的 overrides 主链。
     */
    private fun getDirectlyOverriddenExtendCallableSymbols(symbol: CfirCallableSymbol<*>): List<CfirCallableSymbol<*>> {
        val extendProvider = useSiteFirSession.extendProviderOrNull ?: return emptyList()
        val ownerExtend = extendProvider.getContainingExtend(symbol)
            ?.takeIf(extendProvider::isExtendAccessible)
            ?: return emptyList()

        val directSuperScopes = ownerExtend.superTypeRefs.mapNotNull { superTypeRef ->
            val coneType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
            val classId = coneType.classIdOrPrimitiveClassId ?: return@mapNotNull null
            scopeProvider.getMemberTypeScope(classId)
        }
        if (directSuperScopes.isEmpty()) {
            return emptyList()
        }

        val directOverrides: List<CfirCallableSymbol<*>> = when (symbol) {
            is CfirFunctionSymbol<*> -> directSuperScopes.flatMap { scope ->
                scope.collectStableDirectOverriddenFunctionsByName(symbol.name)
            }

            is CfirPropertySymbol -> buildList {
                directSuperScopes.forEach { scope ->
                    scope.processPropertiesByName(symbol.name) { overridden ->
                        add(overridden)
                    }
                }
            }

            else -> emptyList()
        }

        return directOverrides.distinctBy { overridden ->
            overridden.callableId.toString()
        }
    }
}

private fun CfirTypeScope.collectStableDirectOverriddenFunctions(
    symbol: CfirFunctionSymbol<*>,
): List<CfirFunctionSymbol<*>> {
    val candidatesByBaseScope = linkedMapOf<CfirTypeScope, MutableList<CfirFunctionSymbol<*>>>()
    processDirectOverriddenFunctionsWithBaseScope(symbol) { overridden, baseScope ->
        candidatesByBaseScope.getOrPut(baseScope) { mutableListOf() }.add(overridden)
        ProcessorAction.NEXT
    }

    return candidatesByBaseScope.flatMapTo(linkedSetOf()) { (baseScope, candidates) ->
        baseScope.filterMostSpecificFunctions(candidates)
    }.toList()
}

private fun CfirTypeScope.collectStableDirectOverriddenFunctionsByName(
    name: Name,
): List<CfirFunctionSymbol<*>> {
    val candidates = buildList {
        processFunctionsByName(name) { symbol ->
            add(symbol)
        }
    }
    return filterMostSpecificFunctions(candidates)
}

/**
 * `CfirTypeScope` 的 direct-override 查询保证较弱，某个直接父 scope 可能同时返回：
 * - 父类自身声明
 * - 该父类继续继承来的更上层声明
 *
 * Analysis API 公开 `directlyOverriddenSymbols` 时必须把这类“同一父 scope 内被更具体声明覆盖”的候选裁掉，
 * 否则会把 transitive override 错误暴露成 direct override。
 */
private fun CfirTypeScope.filterMostSpecificFunctions(
    candidates: List<CfirFunctionSymbol<*>>,
): List<CfirFunctionSymbol<*>> {
    if (candidates.size < 2) {
        return candidates.distinctBy { candidate -> candidate.overrideRelationKey() }
    }

    return candidates.filter { candidate ->
        candidates.none { other ->
            other !== candidate && overridesTransitively(other, candidate)
        }
    }.distinctBy { candidate -> candidate.overrideRelationKey() }
}

private fun CfirTypeScope.overridesTransitively(
    possibleOverride: CfirFunctionSymbol<*>,
    expectedBase: CfirFunctionSymbol<*>,
): Boolean {
    val expectedKey = expectedBase.overrideRelationKey()
    val queue = ArrayDeque<CfirFunctionSymbol<*>>()
    val visited = linkedSetOf<String>()
    queue += possibleOverride

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (!visited.add(current.overrideRelationKey())) continue

        var matched = false
        processDirectOverriddenFunctionsWithBaseScope(current) { overridden, _ ->
            if (overridden.overrideRelationKey() == expectedKey) {
                matched = true
                ProcessorAction.STOP
            } else {
                queue += overridden
                ProcessorAction.NEXT
            }
        }
        if (matched) {
            return true
        }
    }

    return false
}

private fun CfirCallableSymbol<*>.overrideRelationKey(): String {
    return callableId.toString()
}

private fun CfirCallableSymbol<*>.overrideOwnerClassId(session: CfirSession): ClassId? {
    /**
     * override 鏌ヨ涓嶈嚜宸遍噸鏂板彂鏄巓wner 鎺ㄥ瑙勫垯锛屽繀椤诲鐢?provider 宸茬粡缁熶竴鐨勫０鏄庡厓鏁版嵁褰掑睘銆?     *
     * 杩欐牱鍗充娇绗﹀彿鏉ヨ嚜 substitution override锛屼篃浼氬洖鍒板師濮嬪０鏄庣殑 owner class锛?     * 閬垮厤 low-level relation provider 鑷繁鍐嶅仛涓€濂楅€€鍖栫殑 unwrap / fallback 閫昏緫銆?     */
    return session.symbolProvider.getContainingClassId(this)
        ?: callableId.classId
}

private fun PsiElement?.isAncestorOf(element: PsiElement): Boolean {
    if (this == null) return false
    var current: PsiElement? = element
    while (current != null) {
        if (current == this) return true
        current = current.parent
    }
    return false
}
