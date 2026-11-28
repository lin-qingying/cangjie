package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.session.declaredCjmpDefaultParameterState
import org.cangnova.cangjie.cfir.session.cjmpDefaultParameterState
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled

/**
 * 单个调用候选的不可变默认参数视图。
 *
 * 声明自身的 HAS_INITIAL 是即时事实；specific 的转移默认前缀来自 owning callable 的有序 CJMP 聚合。
 * 聚合只用于函数和 enum constructor 声明，在第一个需要转移事实的形参被查询时读取，并在该候选的
 * 参数映射期间复用同一份状态。property/variable 的函数值调用参数由候选按函数类型合成，不参与默认值转移。
 */
internal class CandidateDefaultParameterStates private constructor(
    private val parameters: List<CfirValueParameter>,
    private val declaredDefaults: List<Boolean>,
    private val effectiveDefaults: Lazy<List<Boolean>>,
) {
    /** 返回该候选形参是否可省略；非候选形参表示调用映射器违反了声明归属不变量。 */
    fun hasDefaultValue(parameter: CfirValueParameter): Boolean {
        val index = parameters.indexOfFirst { it === parameter }
        check(index >= 0) { "A default-state query must use a parameter owned by this candidate" }
        if (declaredDefaults[index]) return true
        return effectiveDefaults.value[index]
    }

    companion object {
        fun forCandidate(candidate: Candidate, parameters: List<CfirValueParameter>): CandidateDefaultParameterStates {
            val parameterSnapshot = parameters.toList()
            val declaredDefaults = parameterSnapshot.map { it.declaredCjmpDefaultParameterState().hasInitial }
            val owner = parameterSnapshot.firstOrNull()?.let { parameter ->
                parameter.containingDeclarationSymbol.cfir as? CfirCallableDeclaration
                    ?: error("A mapped value parameter must belong to a bound callable")
            }
            val candidateDeclaration = candidate.symbol.takeIf { it.isBound }?.cfir
            // 继承/替换覆盖可能用包装声明暴露原声明参数，归一化双方身份后再校验所有权。
            val candidateParameterOwner = when (candidateDeclaration) {
                is CfirFunction -> candidateDeclaration.unwrapFakeOverridesOrDelegated().unwrapSubstitutionOverrides()
                is CfirEnumConstructor -> candidateDeclaration.unwrapFakeOverridesOrDelegated().unwrapSubstitutionOverrides()
                else -> null
            }
            if (candidateParameterOwner != null && owner != null) {
                check(candidateParameterOwner === owner.unwrapFakeOverridesOrDelegated().unwrapSubstitutionOverrides()) {
                    "Callable parameters must belong to the candidate's original declaration"
                }
            }
            if (owner != null) {
                check(parameterSnapshot.all { parameter -> parameter.containingDeclarationSymbol === owner.symbol }) {
                    "Mapped value parameters must share one owning declaration"
                }
                when (owner) {
                    is CfirFunction, is CfirEnumConstructor -> {
                        // 覆盖视图可复用同一 owner 的参数对象，不要求它们再次出现在包装声明的参数列表中。
                    }
                    is CfirProperty, is CfirVariable -> {
                        // callable-value 的实参映射参数由 Candidate 按函数类型合成，不承载声明默认值。
                        check(parameterSnapshot.all { parameter ->
                            parameter.isLocal && parameter.origin == CfirDeclarationOrigin.Synthetic.Error
                        }) {
                            "Callable-value parameters must be synthetic local parameters"
                        }
                    }
                    else -> error("A value-parameter candidate must belong to a callable declaration")
                }
            }

            val specificCallable = owner?.takeIf { it is CfirFunction || it is CfirEnumConstructor }?.takeIf {
                !it.isLocal &&
                        it.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) &&
                        it.moduleData.session.isCjmpSpecificCompilationEnabled()
            }
            val effectiveDefaults = lazy(LazyThreadSafetyMode.NONE) {
                if (specificCallable == null) return@lazy declaredDefaults
                parameterSnapshot.mapIndexed { index, parameter ->
                    declaredDefaults[index] || parameter.cjmpDefaultParameterState().hasInitial
                }
            }

            return CandidateDefaultParameterStates(parameterSnapshot, declaredDefaults, effectiveDefaults)
        }
    }
}

/** 在一次调用候选的参数映射/arity 计算中共享同一默认参数状态视图。 */
internal fun Candidate.defaultParameterStates(
    parameters: List<CfirValueParameter> = declaredParametersForMapping(),
): CandidateDefaultParameterStates = CandidateDefaultParameterStates.forCandidate(this, parameters)
