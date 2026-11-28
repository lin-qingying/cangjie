package org.cangnova.cangjie.cfir.resolve.calls

import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.constants.explicitNumericLiteralType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 操作数输入与目标必须闭合；metadata 中的旧 VM/error 也不能充当 proper 类型。 */
internal fun ConeCangJieType.isClosedOperatorInputType(): Boolean {
    var closed = true
    forEachTypeIncludingMetadata {
        if (it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType ||
            it is ConeIdealLiteralType || it is ConePrimitiveType && it.kind.isIdeal
        ) closed = false
    }
    return closed
}

/**
 * operator 的目标只参与当前 scalar 返回变量的推断；闭合 source 输入只接受与其兼容的目标。
 * 前后两个正式消费点使用同一规则；不固定 VM，不收窄 proper 输入或声明上界，
 * 不把外层 primitive 目标注入 Container<T>/函数值的内部类型参数。
 */
internal fun Candidate.canUseOperatorExpectedReturnType(
    returnType: ConeCangJieType,
    expectedType: ConeCangJieType,
    session: CfirSession,
): Boolean {
    if (isSourceCompletionWithdrawn || implicitTypeComputationScope?.graph?.isCandidateWithdrawn(this) == true ||
        callInfo.hasExplicitTypeArguments || !argumentMappingInitialized || !expectedType.isClosedOperatorInputType()
    ) return false
    val variableType = returnType.fullyExpandedType(session) as? ConeTypeVariableType
        ?: return false
    val variable = freshVariables.singleOrNull { it.typeConstructor === variableType.typeConstructor }
        ?: return false
    var ownedSignature = true
    returnType.forEachTypeIncludingMetadata {
        if (it is ConeTypeVariableType && it.typeConstructor !== variable.typeConstructor ||
            it is ConeStubType || it is ConeErrorType || it is ConeIdealLiteralType ||
            it is ConePrimitiveType && it.kind.isIdeal
        ) ownedSignature = false
    }
    if (!ownedSignature) return false
    // 只读输入闭包按实际身份访问；撤权检查先于 child 参数、lambda 返回值及 postponed payload。
    val pendingInputs = ArrayDeque<ConeResolutionAtom>()
    val visitedAtoms = Collections.newSetFromMap(IdentityHashMap<ConeResolutionAtom, Boolean>())
    val visitedCandidates = Collections.newSetFromMap(IdentityHashMap<Candidate, Boolean>())
    fun queueActiveCandidate(child: Candidate): Boolean {
        if (child.isSourceCompletionWithdrawn ||
            child.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(child) == true
        ) return false
        if (!visitedCandidates.add(child)) return true
        child.dispatchReceiver?.let(pendingInputs::addLast)
        child.givenExtensionReceiver?.let(pendingInputs::addLast)
        child.chosenExtensionReceiver?.let(pendingInputs::addLast)
        child.contextArguments?.let(pendingInputs::addAll)
        pendingInputs.addAll(child.arguments)
        pendingInputs.addAll(child.postponedAtoms)
        pendingInputs.addAll(child.postponedPCLACalls)
        return true
    }
    if (!queueActiveCandidate(this)) return false
    while (pendingInputs.isNotEmpty()) {
        val atom = pendingInputs.removeLast()
        if (!visitedAtoms.add(atom)) continue
        when (atom) {
            is ConeAtomWithCandidate -> if (!queueActiveCandidate(atom.candidate)) return false
            is ConeResolvedLambdaAtom -> {
                if (!queueActiveCandidate(atom.candidateOfOuterCall)) return false
                if (atom.analyzed) pendingInputs.addAll(atom.returnStatements)
            }
            is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                if (!queueActiveCandidate(atom.candidateOfOuterCall)) return false
                atom.subAtom?.let(pendingInputs::addLast)
            }
            is ConeResolvedCallableReferenceAtom -> {
                if (!queueActiveCandidate(atom.candidateOfOuterCall)) return false
                (atom.resultingReference as? CfirNamedReferenceWithCandidate)?.candidate?.let {
                    if (!queueActiveCandidate(it)) return false
                }
            }
            is ConeSimpleNameForContextSensitiveResolution -> {
                if (!queueActiveCandidate(atom.containingCallCandidate)) return false
            }
            is ConeResolutionAtomWithSingleChild -> atom.subAtom?.let(pendingInputs::addLast)
            is ConeResolutionAtomWithPostponedChild -> atom.subAtom?.let(pendingInputs::addLast)
            is ConeContextSensitiveAlternativeForQualifierAtom, is ConeSimpleLeafResolutionAtom -> Unit
        }
        val actualOwner = ((atom.expression as? CfirResolvable)?.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate
        if (actualOwner != null && !queueActiveCandidate(actualOwner)) return false
    }
    val source = withConstraintSystemForCompletionResults { system ->
        if (isSourceCompletionWithdrawn || implicitTypeComputationScope?.graph?.isCandidateWithdrawn(this) == true) null
        else system.captureStorage()
    } ?: return false
    if (source.allTypeVariables[variable.typeConstructor] !== variable) return false
    val rows = source.notFixedTypeVariables[variable.typeConstructor]?.constraints
        ?: return false
    if (rows.any { row ->
            (row.kind == ConstraintKind.LOWER || row.kind == ConstraintKind.EQUALITY) &&
                row.position.from is ArgumentConstraintPosition<*> && row.type.asCone().isClosedOperatorInputType() &&
                !AbstractTypeChecker.isSubtypeOf(session.typeContext, row.type.asCone(), expectedType)
        }
    ) return false
    if (rows.any { row ->
            row.kind == ConstraintKind.UPPER && row.position.from is DeclaredUpperBoundConstraintPosition<*> &&
                row.type.asCone().isClosedOperatorInputType() &&
                !AbstractTypeChecker.isSubtypeOf(session.typeContext, expectedType, row.type.asCone())
        }
    ) return false
    if (rows.any { row ->
            row.kind == ConstraintKind.LOWER && row.position.from is ArgumentConstraintPosition<*> &&
                row.type.asCone().isIdealType &&
                !AbstractTypeChecker.isSubtypeOf(session.typeContext, row.type.asCone(), expectedType)
        }
    ) return false
    return argumentMapping.none { (atom, parameter) ->
        val parameterType = parameter.returnTypeRef.coneTypeOrNull?.let(substitutor::substituteOrSelf)
            ?: return@none false
        var contributes = false
        parameterType.forEachTypeIncludingMetadata {
            if (it is ConeTypeVariableType && it.typeConstructor === variable.typeConstructor) contributes = true
        }
        if (!contributes) return@none false
        val argumentType = atom.expression.operatorInputTypeForReturnInference() ?: return@none false
        (argumentType.isClosedOperatorInputType() || argumentType.isIdealType) &&
            !AbstractTypeChecker.isSubtypeOf(session.typeContext, argumentType, expectedType)
    }
}

/** 与现有 candidate-local literal 协议一致；显式后缀和非 literal proper 值保持实际类型。 */
private fun CfirExpression.operatorInputTypeForReturnInference(): ConeCangJieType? {
    var value = this
    while (value is CfirWrappedExpression) value = value.expression
    val type = value.coneTypeOrNull ?: return null
    if (value is CfirLiteralExpression && type is ConePrimitiveType && type.kind.isNumeric) {
        return when (value.kind) {
            CfirLiteralKind.INT -> value.explicitNumericLiteralType() ?: ConePrimitiveType.IDEAL_INT
            CfirLiteralKind.FLOAT -> value.explicitNumericLiteralType() ?: ConePrimitiveType.IDEAL_FLOAT
            else -> type
        }
    }
    return type
}
