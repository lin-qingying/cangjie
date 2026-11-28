@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirTransformerBasedResolveProcessor
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.cfir.session.phaseResolverRegistry
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.frontend.CfirCompilerLazyDeclarationResolverWithPhaseChecking
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 已有真实 header/status/super 产品的构造 fixture 补足实际缺失的 BODY 工作。
 * 原注册 processor 遍历同一拥有树并产生 class/file CFG；既有 phase 数值不充当运行证据。
 * 不重放 TYPES、不降阶段，也不在 producer 内启动解析。
 */
internal fun completeCjoSourceFixtureBodies(file: CfirFile) {
    val session = file.moduleData.session
    val sourceProvider = session.cfirProvider as CfirProviderImpl
    check(sourceProvider.getAllFiles().any { it === file })
    val declarations = mutableListOf<CfirDeclaration>()
    val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    file.accept(object : CfirDefaultVisitorVoid() {
        override fun visitExpression(expression: CfirExpression) = Unit
        override fun visitElement(element: CfirElement) {
            if (!seen.add(element)) return
            if (element is CfirDeclaration && element.origin.fromSource) declarations += element
            element.acceptSourceChildren(this, null)
        }
    })
    val identities = declarations.map { Triple(it, it.symbol, it.source) }
    val roots = file.declarations.filterIsInstance<CfirClassLikeDeclaration>()
    check(roots.isNotEmpty() && roots.all { it.resolvePhase < CfirResolvePhase.BODY_RESOLVE })
    check(declarations.filterIsInstance<CfirMemberDeclaration>().all { it.status is CfirResolvedDeclarationStatus })
    val symbols = session.symbolProvider
    val dependencies = session.dependenciesSymbolProvider
    val registry = session.phaseResolverRegistry
    val processor = checkNotNull(registry.getProcessor(CfirResolvePhase.BODY_RESOLVE)) as CfirTransformerBasedResolveProcessor
    check(processor.session === session && processor.phase == CfirResolvePhase.BODY_RESOLVE)
    check(processor.scopeSession === checkNotNull(registry.getProcessor(CfirResolvePhase.STATUS)).scopeSession)
    processor.beforePhase()
    try {
        processor.processFile(file)
    } finally {
        processor.afterPhase()
    }
    check(session.symbolProvider === symbols && session.dependenciesSymbolProvider === dependencies)
    check(identities.all { (declaration, symbol, source) ->
        declaration.symbol === symbol && symbol.cfir === declaration && declaration.source === source
    })
    check(roots.all { it.resolvePhase == CfirResolvePhase.BODY_RESOLVE })
    check(roots.filterIsInstance<CfirClass>().all { it.controlFlowGraphReference != null })
    check(file.controlFlowGraphReference != null)
    val checker = session.lazyDeclarationResolver as CfirCompilerLazyDeclarationResolverWithPhaseChecking
    check(checker.getContractViolationExceptions().isEmpty())
}

/** 负例仅移除所测 classifier，其余真实依赖、包和 callable 身份保持原 provider 产品。 */
internal class CjoSourceFixtureDependenciesWithoutClassifier(
    private val delegate: CfirSymbolProvider,
    private val absentClassId: ClassId,
) : CfirSymbolProvider(delegate.session) {
    override val symbolNamesProvider = delegate.symbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        if (classId == absentClassId) null else delegate.getClassLikeSymbolByClassId(classId)

    override fun getClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> =
        if (classId == absentClassId) emptyList() else delegate.getClassLikeSymbolsByClassId(classId)

    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) = delegate.getTopLevelCallableSymbolsTo(destination, packageFqName, name)

    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) = delegate.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)

    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) = delegate.getTopLevelPropertySymbolsTo(destination, packageFqName, name)

    override fun hasPackage(fqName: FqName): Boolean = delegate.hasPackage(fqName)

    override fun getCjmpLoadDiagnostics(packageFqName: FqName) = delegate.getCjmpLoadDiagnostics(packageFqName)

    override fun getCjmpLoadDiagnostics(packageFqName: FqName, consumerSession: org.cangnova.cangjie.cfir.session.CfirSession) =
        delegate.getCjmpLoadDiagnostics(packageFqName, consumerSession)
}
