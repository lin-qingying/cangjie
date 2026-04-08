package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
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
    private val diagnostics: DiagnosticBuckets,
    private val scopeProvider: CaCfirScopeSnapshotProvider,
    private val visibleSymbolProvider: CaCfirVisibleSymbolProvider,
    private val sourceNavigationProvider: CaCfirSourceNavigationProvider,
) : CaCfirResolutionFacade {
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

    override fun getCallableReturnType(symbol: CfirCallableSymbol<*>): ConeCangJieType? =
        symbol.resolvedReturnTypeRef.coneType

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
