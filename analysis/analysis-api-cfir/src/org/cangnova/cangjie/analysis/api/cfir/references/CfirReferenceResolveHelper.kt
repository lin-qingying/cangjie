package org.cangnova.cangjie.analysis.api.cfir.references

import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.buildSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeUnmatchedTypeArgumentsError
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.resolveImportBinding
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.abbreviatedTypeOrSelf
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjImportGroup
import org.cangnova.cangjie.psi.CjImportPathOwner
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.psiUtil.isImportDirectiveExpression

/**
 * simple-name reference 到 Analysis API symbol 的解析桥。
 *
 * 对齐 Kotlin `FirReferenceResolveHelper.resolveSimpleNameReference` 的职责：
 * PSI 侧只决定要解析哪个表达式，CFIR 侧根据 resolved/candidate/error reference
 * 统一转换为公开 symbol。仓颉没有 Kotlin 的 safe-call、synthetic Java property、
 * resolved qualifier 等语义，因此这些分支不出现。
 */
internal object CfirReferenceResolveHelper {
    /**
     * 解析一个简单名引用并返回公开 Analysis API 符号集合。
     */
    fun resolveSimpleNameReference(
        ref: CaCfirSimpleNameReference,
        analysisSession: CaCfirSession,
    ): Collection<org.cangnova.cangjie.analysis.api.symbols.CaSymbol> {
        val expression = ref.expression
        val symbolBuilder = analysisSession.cfirSymbolBuilder

        // 组织限定符不是包名，没有对应的声明符号；不得进入普通名称解析。
        val pathOwner = expression.getStrictParentOfType<CjImportPathOwner>()
        if (pathOwner?.organizationReference === expression) return emptyList()

        if (expression.isImportDirectiveExpression()) {
            return getSymbolsByImportDirective(expression, analysisSession, symbolBuilder)
        }

        val adjustedResolutionExpression = adjustResolutionExpression(expression)
        val cfir = adjustedResolutionExpression.getOrBuildCfir(analysisSession.resolutionFacade)

        if (cfir is CfirQualifierPart) {
            return getSymbolsByUserTypeQualifierPart(expression, analysisSession, symbolBuilder)
        }

        return when (cfir) {
            is CfirResolvedTypeRef -> listOfNotNull(cfir.toTargetSymbol(analysisSession, symbolBuilder))
            is CfirTypeParameter -> listOf(symbolBuilder.buildSymbol(cfir.symbol))
            is CfirResolvable -> getSymbolsByResolvable(cfir, symbolBuilder)
            is CfirResolvedNamedReference -> cfir.toCaTargetSymbols(symbolBuilder)
            is CfirErrorNamedReference -> cfir.toCaTargetSymbols(symbolBuilder)
            else -> emptyList()
        }
    }

    /**
     * 调用表达式中的 callee 简单名需要提升到整个 call 表达式参与 CFIR 查询。
     */
    private fun adjustResolutionExpression(expression: CjSimpleNameExpression): org.cangnova.cangjie.psi.CjElement {
        val parentAsCall = expression.parent as? CjCallExpression
        return parentAsCall ?: expression
    }

    /**
     * 解析用户类型 qualifier 中可作为目标的简单名。
     */
    private fun getSymbolsByUserTypeQualifierPart(
        expression: CjSimpleNameExpression,
        analysisSession: CaCfirSession,
        symbolBuilder: CaSymbolByCfirBuilder,
    ): Collection<org.cangnova.cangjie.analysis.api.symbols.CaSymbol> {
        val userType = expression.parent as? CjUserType ?: return emptyList()
        if (expression.isPartOfUserTypeRefQualifier()) return emptyList()

        val typeReference = userType.parent as? CjTypeReference ?: return emptyList()
        val resolvedTypeRef = typeReference.getOrBuildCfir(analysisSession.resolutionFacade) as? CfirResolvedTypeRef
            ?: return emptyList()

        return listOfNotNull(resolvedTypeRef.toTargetSymbol(analysisSession, symbolBuilder))
    }

    /**
     * 判断简单名是否只是用户类型限定路径中的中间片段。
     */
    private fun CjSimpleNameExpression.isPartOfUserTypeRefQualifier(): Boolean {
        var currentParent = parent
        while (currentParent is CjUserType) {
            if (currentParent.referenceExpression == null) break
            if (currentParent.referenceExpression !== this) return true
            currentParent = currentParent.parent
        }
        return false
    }

    /**
     * 对齐 Kotlin `getSymbolsByResolvedImport` 的 owner：import reference 的解析也留在 reference helper 内。
     *
     * 仓颉没有 Kotlin 的 `FirResolvedImport` + `FirExplicitSimpleImportingScope` 组合入口，
     * 这里改为复用仓颉现有的 import 绑定解析模型：按导入项计算当前 simple-name
     * 在完整导入路径中的选中 FQ 名，再把它解析成 package / class-like / callable 符号。
     */
    private fun getSymbolsByImportDirective(
        expression: CjSimpleNameExpression,
        analysisSession: CaCfirSession,
        symbolBuilder: CaSymbolByCfirBuilder,
    ): Collection<org.cangnova.cangjie.analysis.api.symbols.CaSymbol> {
        val owner = expression.getStrictParentOfType<CjImportPathOwner>() ?: return emptyList()
        val selectedFqName = owner.selectedFqNameFor(expression) ?: return emptyList()
        val bindingTargets = resolveImportTargets(
            analysisSession = analysisSession,
            owner = owner,
            expression = expression,
            selectedFqName = selectedFqName,
        )

        return buildList {
            bindingTargets.forEach { target ->
                when (target) {
                    is CfirResolvedImportTarget.Package -> {
                        symbolBuilder.createPackageSymbolIfOneExists(target.fqName, target.organizationName)?.let(::add)
                    }

                    is CfirResolvedImportTarget.ClassLike -> {
                        add(symbolBuilder.buildSymbol(target.symbol))
                    }

                    is CfirResolvedImportTarget.Callable -> {
                        target.symbols.mapTo(this) { callable -> symbolBuilder.buildSymbol(callable) }
                    }
                }
            }
        }
    }

    /**
     * 解析 import 项在当前 simple-name 位置选择出的 CFIR import targets。
     */
    private fun resolveImportTargets(
        analysisSession: CaCfirSession,
        owner: CjImportPathOwner,
        expression: CjSimpleNameExpression,
        selectedFqName: FqName,
    ): List<CfirResolvedImportTarget> {
        // 前缀及路径中间段只能表示包。用 all-under 的绑定查询禁止把它们解析为成员。
        val isPackagePath = owner !is CjImportItem || owner.isAllUnder ||
            collectSimpleNames(owner.importedReference).lastOrNull() !== expression
        val importDirective = buildImport {
            importedFqName = selectedFqName
            organizationName = when (owner) {
                is CjImportItem -> owner.organizationName
                is CjImportGroup -> owner.organizationName
                else -> error("Unexpected import path owner: ${owner::class}")
            }
            isAllUnder = isPackagePath
        }
        return analysisSession.cfirSession.resolveImportBinding(
            importDirective,
            CfirLookupOrigin.EXPLICIT_IMPORT,
            allowPackagePrefix = isPackagePath,
        ).targets
    }

    /**
     * 计算 import 路径中当前 simple-name 对应的 FQ name。
     */
    private fun CjImportPathOwner.selectedFqNameFor(expression: CjSimpleNameExpression): FqName? {
        val segments = collectSimpleNames(importedReference)
        val currentIndex = segments.indexOf(expression)
        if (currentIndex < 0) return null
        val localPath = FqName.fromSegments(segments.take(currentIndex + 1).map { it.referencedName })
        val group = (this as? CjImportItem)?.importGroup ?: return localPath
        // 裸花括号分组不添加路径；已出现但损坏的前缀不能被当成空前缀。
        val prefix = group.localFqName
        if (prefix == null && group.importedReference != null) return null
        return if (prefix == null) localPath else prefix.child(localPath)
    }

    /**
     * 收集 import reference 表达式中的简单名片段。
     */
    private fun collectSimpleNames(expression: org.cangnova.cangjie.psi.CjExpression?): List<CjSimpleNameExpression> {
        return when (expression) {
            is CjDotQualifiedExpression -> buildList {
                addAll(collectSimpleNames(expression.receiverExpression))
                addAll(collectSimpleNames(expression.selectorExpression))
            }

            is CjSimpleNameExpression -> listOf(expression)
            else -> emptyList()
        }
    }

    /**
     * 从可解析 CFIR 元素的 callee reference 中提取目标符号。
     */
    private fun getSymbolsByResolvable(
        cfir: CfirResolvable,
        symbolBuilder: CaSymbolByCfirBuilder,
    ): Collection<org.cangnova.cangjie.analysis.api.symbols.CaSymbol> {
        return cfir.calleeReference.toCaTargetSymbols(symbolBuilder)
    }

    /**
     * 从已解析类型引用恢复目标符号。
     */
    private fun CfirResolvedTypeRef.toTargetSymbol(
        analysisSession: CaCfirSession,
        symbolBuilder: CaSymbolByCfirBuilder,
    ): org.cangnova.cangjie.analysis.api.symbols.CaSymbol? {
        return coneType.toTargetSymbol(analysisSession, symbolBuilder) ?: run {
            val diagnostic = (this as? CfirErrorTypeRef)?.diagnostic as? ConeUnmatchedTypeArgumentsError
            diagnostic?.symbol?.buildSymbol(symbolBuilder)
        }
    }

    /**
     * 从 Cone 类型恢复引用解析目标符号。
     */
    private fun ConeCangJieType.toTargetSymbol(
        analysisSession: CaCfirSession,
        symbolBuilder: CaSymbolByCfirBuilder,
    ): org.cangnova.cangjie.analysis.api.symbols.CaSymbol? {
        val targetType = abbreviatedTypeOrSelf
        val resolvedSymbol = when (targetType) {
            is ConeTypeAliasType -> targetType.classId.toSymbol(analysisSession.cfirSession)
            is ConeLookupTagBasedType -> targetType.lookupTag.toSymbol(analysisSession.cfirSession)
            else -> null
        }
        val symbol = resolvedSymbol ?: run {
            val diagnostic = (this as? ConeErrorType)?.diagnostic
            (diagnostic as? ConeUnmatchedTypeArgumentsError)?.symbol
        }
        return symbol?.buildSymbol(symbolBuilder)
    }

}
