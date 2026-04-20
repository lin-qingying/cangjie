package org.cangnova.cangjie.analysis.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjParameter

/**
 * CFIR 会话内的类型查询服务。
 *
 * 表达式类型、声明返回类型、值参数类型、class-like 默认类型、
 * 超类型与类型关系统一由这一层向 low-level 发起查询并缓存。
 */
internal class CaCfirSessionTypeQueryService(
    private val resolutionFacade: LLResolutionFacade,
    private val cacheStore: CaCfirSessionCacheStore,
) {
    fun queryExpressionType(expression: CjExpression): ConeCangJieType? =
        cacheStore.getOrCreateExpressionType(expression) {
            resolutionFacade.getExpressionType(expression)
        }

    fun queryDeclarationReturnType(declaration: CjCallableDeclaration): ConeCangJieType? =
        cacheStore.getOrCreateDeclarationReturnType(declaration) {
            resolutionFacade.getDeclarationReturnType(declaration)
        }

    fun queryValueParameterType(parameter: CjParameter): ConeCangJieType? =
        cacheStore.getOrCreateValueParameterType(parameter) {
            resolutionFacade.getValueParameterType(parameter)
        }

    fun queryCallableReturnType(symbol: CfirCallableSymbol<*>): ConeCangJieType? =
        cacheStore.getOrCreateCallableReturnType(symbol) {
            resolutionFacade.getCallableReturnType(symbol)
        }

    fun queryClassLikeDefaultType(symbol: CfirClassLikeSymbol<*>): ConeCangJieType? =
        cacheStore.getOrCreateClassLikeDefaultType(symbol) {
            resolutionFacade.getClassLikeDefaultType(symbol)
        }

    fun queryTypeClassLikeSymbol(type: ConeCangJieType): CfirClassLikeSymbol<*>? =
        cacheStore.getOrCreateTypeClassLikeSymbol(type) {
            resolutionFacade.getTypeClassLikeSymbol(type)
        }

    fun queryClassLikeSuperTypes(symbol: CfirClassLikeSymbol<*>): List<ConeCangJieType> =
        cacheStore.getOrCreateClassLikeSuperTypes(symbol) {
            resolutionFacade.getClassLikeSuperTypes(symbol)
        }

    fun isSubTypeOf(
        subType: ConeCangJieType,
        superType: ConeCangJieType,
    ): Boolean = resolutionFacade.isSubTypeOf(subType, superType)

    fun areTypesEqual(
        left: ConeCangJieType,
        right: ConeCangJieType,
    ): Boolean = resolutionFacade.areTypesEqual(left, right)
}
