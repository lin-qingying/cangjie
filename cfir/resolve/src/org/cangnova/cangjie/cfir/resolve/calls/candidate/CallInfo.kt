package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom.Companion.createRawAtom
import org.cangnova.cangjie.cfir.semantics.AbstractCallKind
import org.cangnova.cangjie.cfir.semantics.AbstractCallInfo
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

/**
 * 隐式 `invoke` 调用的展开模式。
 */
enum class ImplicitInvokeMode {
    /**
     * 当前调用不是隐式 `invoke` 展开。
     */
    None,
    /**
     * 常规隐式 `invoke`，接收者仍作为显式接收者保留。
     */
    Regular,
    /**
     * 接收者被提升为第一个实参参与候选解析。
     */
    ReceiverAsArgument,
}

/**
 * 调用解析阶段传递给候选收集与约束系统的完整调用上下文。
 *
 * 该对象聚合调用站点、接收者、实参、类型实参、所在文件和解析模式，
 * 使 tower resolve、候选检查和完成阶段都使用同一份结构化调用信息。
 */
open class CallInfo(
    /**
     * 触发调用解析的 CFIR 节点。
     */
    override val callSite: CfirElement,
    /**
     * 调用形态，决定解析命名函数、构造器、变量访问还是特殊调用。
     */
    val callKind: CallKind,
    /**
     * 源码调用名或合成调用名。
     */
    override val name: Name,
    /**
     * 源码显式写出的接收者表达式。
     */
    override val explicitReceiver: CfirExpression?,
    /**
     * 按源码顺序排列的实参表达式。
     */
    override val arguments: List<CfirExpression>,
    /**
     * 当前调用是否作为 `getClass` 接收者使用。
     */
    val isUsedAsGetClassReceiver: Boolean,
    /**
     * 源码显式提供的类型实参。
     */
    override val typeArguments: List<CfirTypeRef>,
    /**
     * 当前调用解析所属的 CFIR 会话。
     */
    val session: CfirSession,
    /**
     * 调用所在文件，用于导入、可见性和诊断定位。
     */
    val containingFile: CfirFile,
    /**
     * 调用外层声明链，由外部解析器按当前位置传入。
     */
    val containingDeclarations: List<CfirDeclaration>,
    /**
     * 普通隐式 `invoke` 的接收者候选。
     */
    val candidateForCommonInvokeReceiver: Candidate? = null,
    /**
     * 当前调用的解析模式，控制期望类型、局部解析和诊断策略。
     */
    val resolutionMode: ResolutionMode,
    /**
     * 调用来源，用于区分普通调用和编译器合成调用。
     */
    override val origin: CfirFunctionCallOrigin = CfirFunctionCallOrigin.Regular,
    /**
     * 隐式 `invoke` 的展开模式。
     */
    val implicitInvokeMode: ImplicitInvokeMode = ImplicitInvokeMode.None,
    /**
     * 集合字面量外层候选；非空时表示当前调用是集合字面量解析的内部调用。
     */
    val containingCandidateForCollectionLiteral: Candidate? = null,
) : AbstractCallInfo() {
    /**
     * 当前调用是否由集合字面量候选触发。
     */
    val isCollectionLiteralCall: Boolean
        get() = containingCandidateForCollectionLiteral != null

    /**
     * 供 diagnostics/checkers 等上层模块使用的稳定调用分类。
     *
     * 具体 resolve 阶段序列仍由本模块的 `CallKind` 决定；这里仅暴露语义分类。
     */
    override val semanticCallKind: AbstractCallKind
        get() = when (callKind) {
            CallKind.Function -> AbstractCallKind.Function
            CallKind.DelegatingConstructorCall -> AbstractCallKind.DelegatingConstructorCall
            CallKind.NamedValueAccess -> AbstractCallKind.NamedValueAccess
            CallKind.EnumConstructorCall -> AbstractCallKind.EnumConstructorCall
        }

    /**
     * 当前调用是否为隐式 `invoke` 展开。
     */
    override val isImplicitInvoke: Boolean
        get() = implicitInvokeMode != ImplicitInvokeMode.None

    /**
     * 源码是否显式提供类型实参。
     */
    override val hasExplicitTypeArguments: Boolean
        get() = typeArguments.isNotEmpty()

    /**
     * 实参表达式对应的原始解析原子。
     */
    val argumentAtoms: List<ConeResolutionAtom> = arguments.map { createRawAtom(it) }

    /**
     * 将当前调用上下文改写为命名值访问。
     */
    fun replaceWithVariableAccess(): CallInfo =
        copy(callKind = CallKind.NamedValueAccess)

    /**
     * 使用新的显式接收者派生调用上下文。
     */
    fun replaceExplicitReceiver(explicitReceiver: CfirExpression?): CallInfo =
        copy(explicitReceiver = explicitReceiver)

    /**
     * 将接收者表达式插入实参首位，用于接收者转实参的隐式 `invoke` 解析。
     */
    fun withReceiverAsArgument(receiverExpression: CfirExpression): CallInfo =
        copy(
            arguments = listOf(receiverExpression) + arguments,
            implicitInvokeMode = ImplicitInvokeMode.ReceiverAsArgument,
        )

    /**
     * 在保留固定调用上下文的前提下派生新的调用信息。
     *
     * 文件、会话、外层声明和调用站点不可被该方法改写，避免候选阶段丢失原始语义边界。
     */
    open fun copy(
        callKind: CallKind = this.callKind,
        typeArguments: List<CfirTypeRef> = this.typeArguments,
        arguments: List<CfirExpression> = this.arguments,
        explicitReceiver: CfirExpression? = this.explicitReceiver,
        name: Name = this.name,
        implicitInvokeMode: ImplicitInvokeMode = this.implicitInvokeMode,
        candidateForCommonInvokeReceiver: Candidate? = this.candidateForCommonInvokeReceiver,
        containingCandidateForCollectionLiteral: Candidate? = this.containingCandidateForCollectionLiteral,
    ): CallInfo = CallInfo(
        callSite = callSite,
        callKind = callKind,
        name = name,
        explicitReceiver = explicitReceiver,
        arguments = arguments,
        isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
        typeArguments = typeArguments,
        session = session,
        containingFile = containingFile,
        containingDeclarations = containingDeclarations,
        candidateForCommonInvokeReceiver = candidateForCommonInvokeReceiver,
        resolutionMode = resolutionMode,
        origin = origin,
        implicitInvokeMode = implicitInvokeMode,
        containingCandidateForCollectionLiteral = containingCandidateForCollectionLiteral,
    )
}
