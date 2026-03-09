package org.cangjie.cfir.declarations

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.expressions.CfirStatement
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.types.CfirTypeRef

/**
 * 声明基接口。所有 CFIR 声明节点的公共接口。
 *
 * 每个声明都有：
 * - 一个唯一的符号标识
 * - 声明来源信息
 * - 注解列表
 * - 所属模块信息
 * - 当前解析阶段
 */
sealed interface CfirDeclaration : CfirElement, CfirStatement {
    val symbol: CfirSymbol<*>
    val origin: CfirDeclarationOrigin
    val annotations: List<CfirAnnotation>
    val moduleData: CfirModuleData
    var resolvePhase: CfirResolvePhase
    val attributes: CfirDeclarationAttributes
}

/**
 * 注解（当前简化表示）。
 */
class CfirAnnotation(
    val typeRef: CfirTypeRef,
    val arguments: List<CfirElement> = emptyList(),
)

/**
 * 成员声明（类/函数/属性等可以拥有修饰符和泛型参数的声明）。
 */
sealed interface CfirMemberDeclaration : CfirDeclaration {
    val status: CfirDeclarationStatus
    val typeParameters: List<CfirTypeParameter>
}

/**
 * 可调用声明（函数/属性/变量等可以有返回类型的声明）。
 */
sealed interface CfirCallableDeclaration : CfirMemberDeclaration {
    var returnTypeRef: CfirTypeRef
}

/**
 * 类类型声明基接口（class/interface/struct/enum）。
 */
sealed interface CfirClassLikeDeclaration : CfirMemberDeclaration {
    val superTypeRefs: List<CfirTypeRef>
    val declarations: List<CfirDeclaration>
}
