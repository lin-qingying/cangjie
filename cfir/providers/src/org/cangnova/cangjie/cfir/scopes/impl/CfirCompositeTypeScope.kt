package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 组合多个类型作用域，用于类型参数多上界和交叉类型的成员解析。
 */
class CfirCompositeTypeScope(
    /**
     * 被组合的类型 scope 列表。
     */
    private val scopes: List<CfirTypeScope>,
    /**
     * 类型等价判断使用的 use-site session。
     */
    private val session: CfirSession,
) : CfirTypeScope() {

    /**
     * 在所有子 scope 中处理直接覆盖函数。
     */
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        for (scope in scopes) {
            if (scope.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor) == ProcessorAction.STOP) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }

    /**
     * 在所有子 scope 中处理直接覆盖属性。
     */
    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        for (scope in scopes) {
            if (scope.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor) == ProcessorAction.STOP) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }

    /**
     * 返回 callable 名称并集。
     */
    override fun getCallableNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getCallableNames()) }
    }

    /**
     * 返回 classifier 名称并集。
     */
    override fun getClassifierNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getClassifierNames()) }
    }

    /**
     * 在所有子 scope 中处理 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        scopes.forEach { it.processClassifiersByName(name, processor) }
    }

    /**
     * 在所有子 scope 中处理函数。
     *
     * 按实例化后的函数名与参数类型归并；类型替换后参数签名不同的实例必须全部保留，
     * 由调用点的实参或目标函数类型继续消歧。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        val mergedBySignature = linkedMapOf<String, CfirNamedFunctionSymbol>()
        scopes.forEach { scope ->
            scope.processFunctionsByName(name) { symbol ->
                val signature = symbol.overrideSignatureKey()
                val previous = mergedBySignature[signature]
                if (previous == null || previous.shouldBeReplacedBy(symbol)) {
                    mergedBySignature[signature] = symbol
                }
            }
        }
        mergedBySignature.values.forEach(processor)
    }

    /**
     * 在所有子 scope 中处理属性，并按实例化后的属性类型合并等价候选。
     *
     * 同名不同类型属性保持为多个候选，以便调用解析形成歧义；同类型属性代表同一个
     * 上界成员契约，只向解析层暴露一次。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        val merged = mutableListOf<CfirPropertySymbol>()
        scopes.forEach { scope ->
            scope.processPropertiesByName(name) { symbol ->
                val equivalentIndex = merged.indexOfFirst { it.hasEquivalentInstantiatedType(symbol) }
                if (equivalentIndex < 0) {
                    merged += symbol
                } else if (merged[equivalentIndex].shouldBeReplacedBy(symbol)) {
                    merged[equivalentIndex] = symbol
                }
            }
        }
        merged.forEach(processor)
    }

    /**
     * 在所有子 scope 中处理 callable。
     *
     * callable 入口必须组合函数、属性两个专用入口的结果，否则同一组合 scope 会因调用种类
     * 不同而看到不同候选集合，尤其会丢失 function/property 同名形成的歧义。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        processFunctionsByName(name, processor)
        processPropertiesByName(name, processor)

        // 字段等其它 callable 没有独立的 scope API，继续从子 scope 透传；函数和属性必须排除，
        // 防止绕过上面的统一合并规则再次进入候选集合。
        val remainingCallables = linkedSetOf<CfirCallableSymbol<*>>()
        scopes.forEach { scope ->
            scope.processCallablesByName(name) { symbol ->
                if (symbol !is CfirNamedFunctionSymbol && symbol !is CfirPropertySymbol) {
                    remainingCallables += symbol
                }
            }
        }
        remainingCallables.forEach(processor)
    }

    /**
     * 替换所有子 scope 的 session 后重建组合 scope。
     */
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? {
        val replacedScopes = scopes.mapNotNull { it.withReplacedSessionOrNull(newSession, newScopeSession) }
        return if (replacedScopes.size == scopes.size) {
            CfirCompositeTypeScope(replacedScopes, newSession)
        } else {
            null
        }
    }

    /**
     * 判断当前函数是否应被候选函数替换。
     */
    private fun CfirNamedFunctionSymbol.shouldBeReplacedBy(candidate: CfirNamedFunctionSymbol): Boolean {
        if (!isBound || !candidate.isBound) return false
        val currentFunction = cfir
        val candidateFunction = candidate.cfir
        return currentFunction.status.isAbstract && !candidateFunction.status.isAbstract
    }

    /**
     * 判断属性候选是否应由 concrete 实现替换。
     */
    private fun CfirPropertySymbol.shouldBeReplacedBy(candidate: CfirPropertySymbol): Boolean {
        if (!isBound || !candidate.isBound) return false
        return cfir.status.isAbstract && !candidate.cfir.status.isAbstract
    }

    /**
     * 判断两个属性在 owner 类型替换后是否具有等价类型。
     */
    private fun CfirPropertySymbol.hasEquivalentInstantiatedType(candidate: CfirPropertySymbol): Boolean {
        if (this == candidate) return true
        if (!isBound || !candidate.isBound) return false
        val ownType = cfir.returnTypeRef.coneTypeOrNull ?: return false
        val candidateType = candidate.cfir.returnTypeRef.coneTypeOrNull ?: return false
        return ownType.isEquivalentTo(candidateType)
    }

    /**
     * 使用当前 use-site 类型上下文判断语义类型等价。
     */
    private fun ConeCangJieType.isEquivalentTo(candidate: ConeCangJieType): Boolean {
        if (this == candidate) return true
        return AbstractTypeChecker.equalTypes(session.typeContext, this, candidate)
    }
}
