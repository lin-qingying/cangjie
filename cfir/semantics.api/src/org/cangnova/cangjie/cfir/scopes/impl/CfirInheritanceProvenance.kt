package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name

/**
 * 继承函数在 use-site scope 中的稳定来源身份。
 *
 * [directInputMember] 标识进入继承图前的原始函数声明，[inheritanceOwnerType] 标识该声明
 * 沿泛型父类型路径形成的 owner 实例。二者共同区分 `I<X>.foo` 与 `I<Y>.foo`，同时让
 * 菱形继承中重复到达的同一个 `I<T>.foo` 保持同一身份。
 */
data class CfirFunctionInheritanceIdentity(
    /** substitution override 链最初的函数声明。 */
    val directInputMember: CfirNamedFunctionSymbol,

    /** 进入当前实例化之前的泛型 owner/interface 实例。 */
    val inheritanceOwnerType: ConeCangJieType?,
)

/**
 * use-site scope 产生的函数继承结果。
 *
 * @property member 已完成当前 owner 替换、供 resolver 消费的最终函数。
 * @property identity 直接输入成员与泛型父类型实例组成的稳定来源身份。
 * @property baseScope 处理 [member] 覆盖链的 base scope。
 */
data class CfirFunctionInheritanceProvenance(
    val member: CfirNamedFunctionSymbol,
    val identity: CfirFunctionInheritanceIdentity,
    val baseScope: CfirTypeScope,
    /** 该函数进入当前 effective member graph 时的 extend/interface 来源。 */
    val lookupProvenance: CfirCallableLookupProvenance,
)

/**
 * 能够保留函数继承来源的 type scope。
 *
 * 普通成员查询只暴露最终函数集合；需要判断“实例化前独立、实例化后冲突”的消费者通过
 * 本接口读取 intersection 输入，不得自行重走父类型图。
 */
interface CfirFunctionInheritanceScope {
    /** 按名称处理最终可见函数及其继承来源。 */
    fun processFunctionsByNameWithProvenance(
        name: Name,
        processor: (CfirFunctionInheritanceProvenance) -> Unit,
    )

    /**
     * 按名称处理归并前的直接继承输入及其来源。
     *
     * 普通成员解析必须消费 [processFunctionsByNameWithProvenance] 的 effective 结果；
     * 继承一致性检查则需要在同一 scope 的成员归并前观察各个父类型输入，避免把不同
     * 返回类型的接口函数错误压成一个代表成员。
     */
    fun processUnmergedInheritedFunctionsByNameWithProvenance(
        name: Name,
        processor: (CfirFunctionInheritanceProvenance) -> Unit,
    )

    /** 处理函数的直接覆盖输入及其继承来源。 */
    fun processDirectOverriddenFunctionsWithProvenance(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirFunctionInheritanceProvenance) -> ProcessorAction,
    ): ProcessorAction
}

/**
 * 能够保留属性继承原始输入的 type scope。
 *
 * 普通属性解析消费过滤后的 effective candidates；继承类型一致性检查则需要观察
 * 多个父类型的未归并属性，避免子类型属性被错误地当作已经兼容的唯一结果。
 */
interface CfirPropertyInheritanceScope {
    /** 按名称处理归并前的直接继承属性及其来源。 */
    fun processUnmergedInheritedPropertiesByNameWithProvenance(
        name: Name,
        processor: (CfirPropertyInheritanceProvenance) -> Unit,
    )
}

/** 属性继承输入与其 base scope、extend 接口来源不可分离。 */
data class CfirPropertyInheritanceProvenance(
    val member: CfirPropertySymbol,
    val baseScope: CfirTypeScope,
    val lookupProvenance: CfirCallableLookupProvenance,
)
