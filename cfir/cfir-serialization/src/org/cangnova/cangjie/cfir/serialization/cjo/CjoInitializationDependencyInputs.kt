package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 字段自己的初始化检查/正式库恢复先完成，再冻结完整依赖 inventory。
 * 未收集与已收集空集合分开保存，依赖不会因此取得本包 definition/body 的拥有权。
 */
internal class CjoInitializationDependencyInputs private constructor(
    val declarations: List<CfirDeclaration>,
    private val originalStates: Map<CfirDeclaration, CjmpInitializationDependencies<CfirBasedSymbol<*>>>,
) {
    fun validateUnchanged() {
        originalStates.forEach { (declaration, state) ->
            check(declaration.initializationDependencies === state) { "Initialization dependencies changed after export preparation" }
            if (state is CjmpInitializationDependencies.Collected) state.declarations.forEach { target ->
                check(target.isBound && target.cfir.symbol === target) { "Initialization dependency binding changed after export preparation" }
            }
        }
    }

    companion object {
        fun capture(inputs: Collection<CfirDeclaration>): CjoInitializationDependencyInputs {
            val states = IdentityHashMap<CfirDeclaration, CjmpInitializationDependencies<CfirBasedSymbol<*>>>()
            val ordered = ArrayList<CfirDeclaration>()
            fun visit(declaration: CfirDeclaration) {
                if (states.containsKey(declaration)) return
                if (declaration is CfirVariable && declaration.cjoDeclarationOrigin == null) {
                    check(declaration.initializer !is org.cangnova.cangjie.cfir.expressions.CfirLazyExpression) {
                        "Source export requires materialized expression regions"
                    }
                }
                if (declaration is CfirFieldVariable && declaration.cjoDeclarationOrigin == null &&
                    declaration.symbol.callableId.classId != null && !declaration.status.isStatic &&
                    declaration.initializer != null) {
                    check(declaration.initializationDependencies !is CjmpInitializationDependencies.NotCollected) {
                        "Common-part instance field dependencies must be collected before CJO publication"
                    }
                }
                val state = declaration.initializationDependencies
                states[declaration] = state
                ordered += declaration
                if (state is CjmpInitializationDependencies.Collected) state.declarations.forEach { target ->
                    check(target.isBound && target.cfir.symbol === target) { "Initialization dependency is not canonically bound" }
                    visit(target.cfir)
                }
            }
            inputs.forEach(::visit)
            return CjoInitializationDependencyInputs(Collections.unmodifiableList(ordered.toList()),
                Collections.unmodifiableMap(IdentityHashMap(states))).also { it.validateUnchanged() }
        }
    }
}

