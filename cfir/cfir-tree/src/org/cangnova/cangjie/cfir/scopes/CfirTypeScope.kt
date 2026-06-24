package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 类型成员 scope 的基类。
 *
 * 类型 scope 除了普通按名查询，还需要提供 override 链的直接父成员查询能力，
 * 以支持 override 检查、fake override 构造和成员继承分析。
 */
abstract class CfirTypeScope : CfirContainingNamesAwareScope() {
    /**
     * 处理给定函数直接 override 的父函数及其 base scope。
     *
     * 若当前 scope 与 [functionSymbol] 的声明来源 scope 一致，该方法会提供所有直接被 override 的成员。
     * 每个返回的父成员都携带自己的 base scope，调用方可继续在该 base scope 中查询更上层 override。
     *
     * 当前契约与 Kotlin FIR 保持一致：在 substitution 或 intersection 场景中，同一个父符号可能以不同
     * base scope 出现多次，调用方需要自行去重或保留 scope 差异。
     */
    abstract fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction

    /**
     * 处理给定属性直接 override 的父属性及其 base scope。
     *
     * 语义与 [processDirectOverriddenFunctionsWithBaseScope] 相同，只是目标符号为属性。
     */
    abstract fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction

    /**
     * 在新 session / scope session 下替换当前类型 scope。
     */
    abstract override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession
    ): CfirTypeScope?

    /**
     * 空类型 scope。
     *
     * 该 scope 不暴露任何 callable、classifier 或 override 关系。
     */
    object Empty : CfirTypeScope() {
        /**
         * 空 scope 没有直接 override 函数。
         */
        override fun processDirectOverriddenFunctionsWithBaseScope(
            functionSymbol: CfirNamedFunctionSymbol,
            processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT

        /**
         * 空 scope 没有直接 override 属性。
         */
        override fun processDirectOverriddenPropertiesWithBaseScope(
            propertySymbol: CfirPropertySymbol,
            processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT

        /**
         * 空 scope 不包含 callable 名称。
         */
        override fun getCallableNames(): Set<Name> = emptySet()

        /**
         * 空 scope 不包含 classifier 名称。
         */
        override fun getClassifierNames(): Set<Name> = emptySet()

        /**
         * 返回调试用名称。
         */
        override fun toString(): String {
            return "Empty scope"
        }


        /**
         * 空 scope 不能迁移 session，直接返回 `null`。
         */
        override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? {
            return null
        }
    }
}
