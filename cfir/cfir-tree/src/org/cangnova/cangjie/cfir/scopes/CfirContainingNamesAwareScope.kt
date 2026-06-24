package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * 可枚举名称集合的 scope。
 *
 * 该抽象用于支持 completion、索引预过滤和批量成员查询：调用方可以先读取 callable / classifier 名称集合，
 * 再决定是否发起按名处理。
 */
abstract class CfirContainingNamesAwareScope : CfirScope() {
    /**
     * 当前 scope 可能包含的 callable 名称集合。
     */
    abstract fun getCallableNames(): Set<Name>

    /**
     * 当前 scope 可能包含的 classifier 名称集合。
     */
    abstract fun getClassifierNames(): Set<Name>

    /**
     * 当前 scope 是否确定没有 static 成员。
     */
    open val hasDefinitelyNoStaticMembers: Boolean get() = false

    /**
     * 在新 session / scope session 下替换当前 scope。
     */
    abstract override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirContainingNamesAwareScope?
}

/**
 * 委托型类型 scope 基类。
 *
 * 默认把大部分查询转发给 [delegate]，用于减少包装 scope 的样板代码。
 * 继承者应至少覆写部分行为，否则可直接使用 [delegate]。
 *
 * [toString] 不默认委托，因为包装 scope 通常与 [delegate] 具有不同的语义身份。
 */
abstract class CfirDelegatingTypeScope(private val delegate: CfirTypeScope) : CfirTypeScope() {
    /**
     * 委托返回 callable 名称集合。
     */
    override fun getCallableNames(): Set<Name> = delegate.getCallableNames()

    /**
     * 委托返回 classifier 名称集合。
     */
    override fun getClassifierNames(): Set<Name> = delegate.getClassifierNames()

    /**
     * 委托执行名称预过滤。
     */
    override fun mayContainName(name: Name): Boolean = delegate.mayContainName(name)

    /**
     * 委托返回 scope owner 查找名。
     */
    override val scopeOwnerLookupNames: List<String> get() = delegate.scopeOwnerLookupNames

    /**
     * 委托处理带替换器的 classifier 查询。
     */
    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    }

    /**
     * 委托处理函数查询。
     */
    override fun processFunctionsByName(
        name: Name,
        processor: (CfirNamedFunctionSymbol) -> Unit,
    ) {
        delegate.processFunctionsByName(name, processor)
    }

    /**
     * 委托处理属性查询。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        delegate.processPropertiesByName(name, processor)
    }

    /**
     * 委托处理构造器查询。
     */
    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    /**
     * 委托处理函数 override 链查询。
     */
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        return delegate.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor)
    }

    /**
     * 委托处理属性 override 链查询。
     */
    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        return delegate.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor)
    }

    /**
     * 在新 session / scope session 下替换当前委托 scope。
     */
    abstract override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirDelegatingTypeScope?
}
