package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

abstract class AbstractCallCandidate<P : AbstractConeResolutionAtom> : AbstractCandidate() {
    /**
     * Contains mapping of arguments to value and context parameters (in case of explicit context arguments).
     */
    abstract val argumentMapping: LinkedHashMap<P, CfirValueParameter>
    abstract val argumentMappingInitialized: Boolean
    abstract val dispatchReceiver: AbstractConeResolutionAtom?
    abstract val chosenExtensionReceiver: AbstractConeResolutionAtom?
    abstract val explicitReceiverKind: ExplicitReceiverKind
    abstract val contextArguments: List<AbstractConeResolutionAtom>?
    abstract val callInfo: AbstractCallInfo
    abstract val diagnostics: List<ResolutionDiagnostic>
    abstract val errors: List<ConstraintSystemError>
    abstract val system: ConstraintSystem
    abstract val usedOuterCs: Boolean

    private var _cangjieVariadicRegularCallDiagnostics: List<ResolutionDiagnostic>? = null

    /**
     * 仓颉变参调用必须先尝试普通调用；只有普通调用不匹配时，官方编译器才会
     * 抑制普通调用诊断并继续尝试变参解糖。若变参尝试最终失败，需要回放这里保存
     * 的普通调用诊断，而不是报告解糖后数组字面量上的派生错误。
     */
    val cangjieVariadicRegularCallDiagnostics: List<ResolutionDiagnostic>
        get() = _cangjieVariadicRegularCallDiagnostics ?: emptyList()

    fun initializeCangjieVariadicRegularCallDiagnostics(diagnostics: List<ResolutionDiagnostic>) {
        require(_cangjieVariadicRegularCallDiagnostics == null) {
            "Cangjie variadic regular-call diagnostics already initialized"
        }
        _cangjieVariadicRegularCallDiagnostics = diagnostics
    }
}
