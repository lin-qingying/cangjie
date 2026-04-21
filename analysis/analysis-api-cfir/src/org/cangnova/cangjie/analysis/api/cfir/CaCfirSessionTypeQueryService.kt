package org.cangnova.cangjie.analysis.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOfTypeSafe
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.toClassLikeSymbol
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * CFIR 会话内的类型查询服务。
 *
 * 类型查询统一落在 session 层完成：
 * - PSI 入口先恢复 CFIR 表达式/声明/符号
 * - 类型关系统一走 `AbstractTypeChecker`
 * - class-like 默认类型与超类型直接复用 CFIR symbol 事实
 */
internal class CaCfirSessionTypeQueryService(
    private val resolutionFacade: LLResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    private val useSiteSession get() = resolutionFacade.useSiteCfirSession

    fun queryExpressionType(expression: CjExpression): ConeCangJieType? =
        cacheStore.getOrCreateExpressionType(expression) {
            (expression.getOrBuildCfir(resolutionFacade) as? CfirExpression)?.coneTypeOrNull
        }

    fun queryDeclarationReturnType(declaration: CjCallableDeclaration): ConeCangJieType? =
        cacheStore.getOrCreateDeclarationReturnType(declaration) {
            declaration.resolveToCfirSymbolOfTypeSafe<CfirCallableSymbol<*>>(resolutionFacade)?.resolvedReturnType
        }

    fun queryValueParameterType(parameter: CjParameter): ConeCangJieType? =
        cacheStore.getOrCreateValueParameterType(parameter) {
            parameter.resolveToCfirSymbolOfTypeSafe<CfirValueParameterSymbol>(resolutionFacade)?.resolvedReturnType
        }

    fun queryCallableReturnType(symbol: CfirCallableSymbol<*>): ConeCangJieType? =
        cacheStore.getOrCreateCallableReturnType(symbol) {
            symbol.resolvedReturnType
        }

    fun queryClassLikeDefaultType(symbol: CfirClassLikeSymbol<*>): ConeCangJieType? =
        cacheStore.getOrCreateClassLikeDefaultType(symbol) {
            symbol.takeIf { it.isBound }?.constructType()
        }

    fun queryTypeClassLikeSymbol(type: ConeCangJieType): CfirClassLikeSymbol<*>? =
        cacheStore.getOrCreateTypeClassLikeSymbol(type) {
            type.toClassLikeSymbol(useSiteSession)
        }

    fun queryClassLikeSuperTypes(symbol: CfirClassLikeSymbol<*>): List<ConeCangJieType> =
        cacheStore.getOrCreateClassLikeSuperTypes(symbol) {
            symbol.resolvedSuperTypeRefs.map { superTypeRef -> superTypeRef.coneType }
        }

    fun isSubTypeOf(
        subType: ConeCangJieType,
        superType: ConeCangJieType,
    ): Boolean = AbstractTypeChecker.isSubtypeOf(useSiteSession.typeContext, subType, superType)

    fun areTypesEqual(
        left: ConeCangJieType,
        right: ConeCangJieType,
    ): Boolean = AbstractTypeChecker.equalTypes(useSiteSession.typeContext, left, right)
}
