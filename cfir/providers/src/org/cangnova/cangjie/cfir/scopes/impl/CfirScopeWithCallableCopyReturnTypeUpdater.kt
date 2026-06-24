package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.scopes.CfirDelegatingTypeScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * 包装会创建 callable copy 的 scope，并在派发 callable 前补算返回类型。
 *
 * 部分 callable copy 的返回类型因为隐式 body resolve 逻辑尚未计算；
 * 该 wrapper 在查询结果流出 scope 前触发 [CallableCopyTypeCalculator]。
 */
class CfirScopeWithCallableCopyReturnTypeUpdater(
    /**
     * 被包装的类型 scope。
     */
    private val delegate: CfirTypeScope,
    /**
     * callable copy 返回类型计算器。
     */
    private val callableCopyTypeCalculator: CallableCopyTypeCalculator
) : CfirDelegatingTypeScope(delegate) {
    /**
     * 处理函数前补算返回类型。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        delegate.processFunctionsByName(name) {
            updateReturnType(it.cfir)
            processor(it)
        }
    }

    /**
     * 处理属性前补算返回类型。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        delegate.processPropertiesByName(name) {
            updateReturnType(it.cfir)
            processor(it)
        }
    }

    /**
     * 处理直接覆盖函数前补算返回类型。
     */
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return delegate.processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { symbol, scope ->
            updateReturnType(symbol.cfir)
            processor(symbol, scope)
        }
    }

    /**
     * 处理直接覆盖属性前补算返回类型。
     */
    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return delegate.processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { symbol, scope ->
            updateReturnType(symbol.cfir)
            processor(symbol, scope)
        }
    }

    /**
     * 若 [declaration] 支持延迟返回类型计算，则触发计算。
     */
    private fun updateReturnType(declaration: CfirCallableDeclaration) {
        if (declaration.canHaveDeferredReturnTypeCalculation) {
            callableCopyTypeCalculator.computeReturnType(declaration)
        }
    }

    /**
     * 返回委托 scope 的调试文本。
     */
    override fun toString(): String {
        return delegate.toString()
    }

    /**
     * 替换委托 scope 的 session 后重建 wrapper。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession
    ): CfirScopeWithCallableCopyReturnTypeUpdater? {
        return delegate.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
            CfirScopeWithCallableCopyReturnTypeUpdater(delegate, callableCopyTypeCalculator)
        }
    }
}
