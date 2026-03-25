package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom.Companion.createRawAtom
import org.cangnova.cangjie.cfir.semantics.AbstractCallInfo
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

enum class ImplicitInvokeMode {
    None,
    Regular,
    ReceiverAsArgument,
}

open class CallInfo(
    override val callSite: CfirElement,
    val callKind: CallKind,
    override val name: Name,
    override val explicitReceiver: CfirExpression?,
    override val arguments: List<CfirExpression>,
    val isUsedAsGetClassReceiver: Boolean,
    val typeArguments: List<CfirTypeRef>,
    val session: CfirSession,
    val containingFile: CfirFile,
    val containingDeclarations: List<CfirDeclaration>,
    val candidateForCommonInvokeReceiver: Candidate? = null,
    val resolutionMode: ResolutionMode,
    val origin: CfirFunctionCallOrigin = CfirFunctionCallOrigin.Regular,
    val implicitInvokeMode: ImplicitInvokeMode = ImplicitInvokeMode.None,
    val containingCandidateForCollectionLiteral: Candidate? = null,
) : AbstractCallInfo() {
    val isCollectionLiteralCall: Boolean
        get() = containingCandidateForCollectionLiteral != null

    override val isImplicitInvoke: Boolean
        get() = implicitInvokeMode != ImplicitInvokeMode.None

    val argumentAtoms: List<ConeResolutionAtom> = arguments.map { createRawAtom(it) }

    fun replaceWithVariableAccess(): CallInfo =
        copy(callKind = CallKind.VariableAccess, typeArguments = emptyList())

    fun replaceExplicitReceiver(explicitReceiver: CfirExpression?): CallInfo =
        copy(explicitReceiver = explicitReceiver)

    fun withReceiverAsArgument(receiverExpression: CfirExpression): CallInfo =
        copy(
            arguments = listOf(receiverExpression) + arguments,
            implicitInvokeMode = ImplicitInvokeMode.ReceiverAsArgument,
        )

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
