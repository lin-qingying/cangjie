package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

/**
 * 调用解析候选的跨模块抽象。
 *
 * @param P 调用实参或上下文参数在候选中的原子表示。
 */
abstract class AbstractCallCandidate<P : AbstractConeResolutionAtom> : AbstractCandidate() {
    /**
     * 实参到值参数或上下文参数的映射。
     *
     * 当调用含显式 context 参数时，该映射同时覆盖普通值参数和上下文参数。
     */
    abstract val argumentMapping: LinkedHashMap<P, CfirValueParameter>

    /** 参数映射是否已经完成初始化。 */
    abstract val argumentMappingInitialized: Boolean

    /** 当前候选选择的 dispatch receiver 原子。 */
    abstract val dispatchReceiver: AbstractConeResolutionAtom?

    /** 当前候选选择的 extension receiver 原子。 */
    abstract val chosenExtensionReceiver: AbstractConeResolutionAtom?

    /** 显式接收者在调用语法中的分类。 */
    abstract val explicitReceiverKind: ExplicitReceiverKind

    /** 显式传入的上下文参数原子列表。 */
    abstract val contextArguments: List<AbstractConeResolutionAtom>?

    /** 当前候选所属调用的抽象调用信息。 */
    abstract val callInfo: AbstractCallInfo

    /** 候选构建与检查阶段收集到的结构化诊断。 */
    abstract val diagnostics: List<ResolutionDiagnostic>

    /** 约束系统产生的底层错误列表。 */
    abstract val errors: List<ConstraintSystemError>

    /** 当前候选使用的类型约束系统。 */
    abstract val system: ConstraintSystem

    /**
     * 声明类型参数到当前候选 fresh type variable/已知 owner 实参的替换器。
     *
     * 该只读 seam 供 resolve 之后的语义检查消费最终实例化结果；checker 必须先应用此替换，
     * 再应用 [system] 的最终替换器，不能重新推导一套候选类型实参。
     */
    abstract val typeParameterSubstitutorOrNull: ConeSubstitutor?

    /** 当前候选是否复用了外层调用的约束系统。 */
    abstract val usedOuterCs: Boolean

    /** 仓颉变参普通调用尝试阶段保留的诊断。 */
    private var _cangjieVariadicRegularCallDiagnostics: List<ResolutionDiagnostic>? = null

    /**
     * 仓颉变参调用必须先尝试普通调用；只有普通调用不匹配时，官方编译器才会
     * 抑制普通调用诊断并继续尝试变参解糖。若变参尝试最终失败，需要回放这里保存
     * 的普通调用诊断，而不是报告解糖后数组字面量上的派生错误。
     */
    val cangjieVariadicRegularCallDiagnostics: List<ResolutionDiagnostic>
        get() = _cangjieVariadicRegularCallDiagnostics ?: emptyList()

    /**
     * 初始化仓颉变参普通调用阶段保留的诊断。
     *
     * 该数据只能初始化一次，避免后续变参解糖阶段覆盖真正需要回放的普通调用失败信息。
     */
    fun initializeCangjieVariadicRegularCallDiagnostics(diagnostics: List<ResolutionDiagnostic>) {
        require(_cangjieVariadicRegularCallDiagnostics == null) {
            "Cangjie variadic regular-call diagnostics already initialized"
        }
        _cangjieVariadicRegularCallDiagnostics = diagnostics
    }
}
