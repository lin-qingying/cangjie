@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.transformers.CfirImportResolveTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusResolver
import org.cangnova.cangjie.cfir.resolve.transformers.runSupertypeResolvePhaseForNonLocalClassLikeDeclaration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 直接构造的 CJO SOURCE fixture 通过正式状态解析器发布 header 产品。
 * 先由正规 SUPER_TYPES 完成原 Source class/alias 产品，再解析状态；不进入正文或惰性初值。
 */
internal fun resolveCjoSourceFixtureStatuses(
    session: CfirSession,
    declarations: List<CfirDeclaration>,
    useSiteFile: CfirFile? = null,
) {
    val scopeSession = ScopeSession()
    completeCjoSourceFixtureClassLikeHeaders(session, declarations, scopeSession, useSiteFile)
    val resolver = CfirStatusResolver(session, scopeSession)
    val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    val visitor = object : CfirDefaultVisitorVoid() {
        private var containingClass: CfirClassLikeDeclaration? = null
        private var containingProperty: CfirProperty? = null

        override fun visitExpression(expression: CfirExpression) = Unit

        override fun visitElement(element: CfirElement) {
            if (!seen.add(element)) return
            when (element) {
                is CfirValueParameter -> element.replaceStatus(element.status.resolvedForStatuslessDeclaration())
                is CfirMemberDeclaration -> {
                    val isLocal = when (element) {
                        is CfirCallableDeclaration -> element.isLocal
                        // 仓颉 ClassId 仅代表顶层类型声明，类型声明不继承 Kotlin 的局部类型位。
                        else -> false
                    }
                    element.replaceStatus(resolver.resolveStatus(element, containingClass, containingProperty, isLocal))
                }
            }
            val previousClass = containingClass
            val previousProperty = containingProperty
            if (element is CfirClassLikeDeclaration) containingClass = element
            if (element is CfirProperty) containingProperty = element
            try {
                element.acceptSourceChildren(this, null)
                // 默认辅助函数的物理 outer 与其宿主参数分开；沿既有元数据边保持同一词法 owner。
                (element as? CfirValueParameter)?.defaultValueMetadata?.function?.accept(this)
            } finally {
                containingClass = previousClass
                containingProperty = previousProperty
            }
        }
    }
    declarations.forEach { it.accept(visitor) }
}

/**
 * 只完成真实拥有的 class-like header，不沿引用认领正文，不写入手工 superclass/RHS 产品。
 * 已完成产品复用原 symbol；高阶段缺产品是 fixture 前置错误，不能降低其 phase 后重放。
 */
internal fun completeCjoSourceFixtureClassLikeHeaders(
    session: CfirSession,
    declarations: List<CfirDeclaration>,
    scopeSession: ScopeSession = ScopeSession(),
    useSiteFile: CfirFile? = null,
) {
    // 正常IMPORTS入口为这个原文件产生完成绑定；已有高phase不等于store中已有产品。
    useSiteFile?.let { file ->
        val diagnostics = CfirDiagnosticCollector()
        CfirImportResolveTransformer(session, diagnostics).recordImportBindingsForFile(file)
        check(!diagnostics.hasErrors) { "Source fixture import resolution failed: ${diagnostics.diagnostics}" }
    }
    val headers = mutableListOf<Pair<CfirClassLikeDeclaration, List<CfirDeclaration>>>()
    val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    val containers = mutableListOf<CfirDeclaration>()
    val visitor = object : CfirDefaultVisitorVoid() {
        override fun visitExpression(expression: CfirExpression) = Unit

        override fun visitElement(element: CfirElement) {
            if (!seen.add(element)) return
            if (element is CfirClassLikeDeclaration && element.origin.fromSource) {
                check(element.moduleData.session === session) { "Source header belongs to another fixture session" }
                headers += element to containers.toList()
            }
            val declaration = element as? CfirDeclaration
            if (declaration != null) containers += declaration
            try {
                element.acceptSourceChildren(this, null)
                (element as? CfirValueParameter)?.defaultValueMetadata?.function?.accept(this)
            } finally {
                if (declaration != null) check(containers.removeAt(containers.lastIndex) === declaration)
            }
        }
    }
    declarations.forEach { it.accept(visitor) }
    if (headers.isEmpty()) return
    val provider = checkNotNull(session.directSupertypeProviderOrNull) { "Source fixture requires its original supertype provider" }
    for ((declaration, _) in headers) {
        if (provider.getCompletedSupertypeProduct(declaration.symbol) == null) {
            check(declaration.resolvePhase <= CfirResolvePhase.SUPER_TYPES) {
                "Source fixture header ${declaration.symbol.classId} at ${declaration.resolvePhase} has no completed SUPER_TYPES product; " +
                    "construct its class/alias header before SUPER_TYPES instead of lowering an existing declaration phase"
            }
        }
    }
    for ((declaration, containingDeclarations) in headers) {
        val symbol = declaration.symbol
        val source = declaration.source
        val phase = declaration.resolvePhase
        if (provider.getCompletedSupertypeProduct(symbol) == null) {
            if (declaration is CfirClass && symbol.classId != StdlibClassIds.Object && symbol.classId != StdlibClassIds.Any &&
                declaration.superTypeRefs.none { ref -> (ref.coneTypeOrNull as? ConeClassLikeType)?.isInterface == false }
            ) {
                val objectSymbol = checkNotNull(session.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.Object)) {
                    "Source fixture ${symbol.classId} requires the real std.core.Object provider; " +
                        "configure the library session CjoManager and CANGJIE_STDLIB_MODULE before SUPER_TYPES"
                }
                check(objectSymbol is CfirClassSymbol && objectSymbol.isBound) { "std.core.Object must be its bound class header" }
            }
            val resolved = declaration.runSupertypeResolvePhaseForNonLocalClassLikeDeclaration(
                session, scopeSession, useSiteFile, containingDeclarations,
            )
            check(resolved === declaration)
        }
        check(declaration.symbol === symbol && symbol.cfir === declaration && declaration.source === source)
        check(declaration.resolvePhase >= phase) { "Fixture header completion cannot regress its original phase" }
        val product = checkNotNull(provider.getCompletedSupertypeProduct(symbol)) { "SUPER_TYPES did not publish the original fixture header: $symbol" }
        check(product.ownerSymbol === symbol)
        if (declaration is CfirTypeAlias) {
            check(product.superTypeRefs.size == 1 && product.superTypeRefs.single() is CfirResolvedTypeRef)
            check(product.superTypeRefs.single().coneTypeOrNull == declaration.expandedTypeRef.coneTypeOrNull)
        } else {
            check(product.superTypeRefs == declaration.superTypeRefs)
        }
    }
}
