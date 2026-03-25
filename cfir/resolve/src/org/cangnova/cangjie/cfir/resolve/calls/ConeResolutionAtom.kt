package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.semantics.AbstractConeResolutionAtom
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.resolve.calls.model.PostponedCallableReferenceMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.type.model.CangJieTypeMarker


sealed class ConeResolutionAtom : AbstractConeResolutionAtom(){
    abstract override val expression: CfirExpression

    companion object {
        /**
         * 根据表达式类型创建对应的 atom。
         *
         * 对齐 K2 `createRawAtom`：在完整实现中应根据表达式类型
         * （lambda、可调用引用、带候选的调用等）选择不同的 atom 子类。
         * 当前仓颉版本简化为叶子 atom。
         */
        fun createRawAtom(expression: CfirExpression): ConeResolutionAtom {
            return ConeSimpleLeafResolutionAtom(expression)
        }
    }
}

class ConeSimpleLeafResolutionAtom(
    override val expression: CfirExpression,
    @Suppress("UNUSED_PARAMETER") allowUnresolvedExpression: Boolean = true,
) : ConeResolutionAtom()

class ConeAtomWithCandidate(
    override val expression: CfirExpression,
    val candidate: Candidate,
) : ConeResolutionAtom()

sealed class ConePostponedResolvedAtom : ConeResolutionAtom(), PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<ConeCangJieType>
    abstract override val outputType: ConeCangJieType?
    abstract override val expectedType: ConeCangJieType?
    override var analyzed: Boolean = false
}

class ConeResolutionAtomWithPostponedChild(
    override val expression: CfirExpression,
    val fallbackSubAtom: ConeResolutionAtom? = null,
) : ConeResolutionAtom() {
    var subAtom: ConeResolutionAtom? = null
        private set

    fun setPostponedSubAtom(atom: ConePostponedResolvedAtom) {
        require(subAtom == null) { "subAtom already initialized" }
        subAtom = atom
    }

    fun useFallbackSubAtom() {
        subAtom = fallbackSubAtom
    }
}

object ConeResolutionAtomFactory {
    fun create(expression: CfirExpression): ConeResolutionAtom {
        return ConeSimpleLeafResolutionAtom(expression)
    }

    fun createWithCandidate(expression: CfirExpression, candidate:  Candidate): ConeResolutionAtom {
        return ConeAtomWithCandidate(expression, candidate)
    }
}


//  ------------- References -------------

//  ------------- Lambdas -------------

// A lambda or a callable reference.
// We separate this kind of atom because for them, we might fix earlier type variables contained inside the parameter
// type of the relevant function expected type.
sealed class ConeFunctionTypeRelatedPostponedResolvedAtom : ConePostponedResolvedAtom()

// ─────────────────── 已解析的 lambda atom ───────────────────

/**
 * 已解析的 lambda atom。
 *
 * 持有 lambda 的参数类型、返回类型、返回语句以及可选的返回类型变量。
 * 对齐 K2 `ConeResolvedLambdaAtom`。
 *
 * 仓颉暂无 `CfirAnonymousFunction`，使用 [CfirDeclaration] 替代。
 */
class ConeResolvedLambdaAtom(
    override val expression: CfirExpression,
    val anonymousFunction: CfirDeclaration,
    expectedType: ConeCangJieType?,
    val parameterTypes: List<ConeCangJieType>,
    val returnType: ConeCangJieType,
    typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = null,
) : ConeFunctionTypeRelatedPostponedResolvedAtom() {
    override val inputTypes: Collection<ConeCangJieType> get() = parameterTypes
    override val outputType: ConeCangJieType get() = returnType
    override var expectedType: ConeCangJieType? = expectedType
        private set

    var typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = typeVariableForLambdaReturnType
        private set

    var returnStatements: Collection<ConeResolutionAtom> = emptyList()
        internal set

    fun replaceExpectedType(expectedType: ConeCangJieType, newReturnType: ConeCangJieType? = null) {
        this.expectedType = expectedType
    }

    fun replaceTypeVariableForLambdaReturnType(variable: ConeTypeVariableForLambdaReturnType) {
        typeVariableForLambdaReturnType = variable
    }
}

// ─────────────────── 可修订期望类型的延迟 atom 基类 ───────────────────

/**
 * 密封基类：期望类型可在推断过程中被修订的延迟 atom。
 *
 * 对齐 K2 `ConePostponedAtomWithRevisableExpectedType`。
 */
sealed class ConePostponedAtomWithRevisableExpectedType :
    ConeFunctionTypeRelatedPostponedResolvedAtom(),
    PostponedAtomWithRevisableExpectedType

// ─────────────────── 期望类型为类型变量的 lambda ───────────────────

/**
 * 期望类型为类型变量的 lambda atom。
 *
 * 初始状态下期望类型包含类型变量，推断过程中通过 [reviseExpectedType] 修订为
 * 具体的函数类型后，通过 [transformToResolvedLambda] 转换为 [ConeResolvedLambdaAtom]。
 *
 * 对齐 K2 `ConeLambdaWithTypeVariableAsExpectedTypeAtom`。
 */
class ConeLambdaWithTypeVariableAsExpectedTypeAtom(
    override val expression: CfirExpression,
    expectedType: ConeCangJieType,
    val candidateOfOuterCall: Candidate? = null,
) : ConePostponedAtomWithRevisableExpectedType(), LambdaWithTypeVariableAsExpectedTypeMarker {

    override var revisedExpectedType: CangJieTypeMarker? = null
        private set
    override var parameterTypesFromDeclaration: List<CangJieTypeMarker?>? = null
        private set

    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    override val outputType: ConeCangJieType? = null
    override val expectedType: ConeCangJieType = expectedType

    var subAtom: ConeResolvedLambdaAtom? = null
        private set

    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        revisedExpectedType = expectedType
    }

    override fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?) {
        parameterTypesFromDeclaration = types
    }

    /**
     * 将此 atom 转换为已解析的 [ConeResolvedLambdaAtom]。
     *
     * @param csBuilder 约束系统构建器
     * @param context 解析上下文
     * @param expectedType 修订后的函数期望类型
     */
    fun transformToResolvedLambda(
        csBuilder: ConstraintSystemImpl,
        context: ResolutionContext,
        expectedType: ConeCangJieType,
    ) {
        // 初版桩实现：仓颉的 lambda 转换逻辑待完善
        // 完整实现需要：提取参数类型 → 创建 ConeResolvedLambdaAtom → 注册到约束系统
        analyzed = true
    }
}

// ─────────────────── 可调用引用 atom ───────────────────


// ─────────────────── 上下文敏感解析 atom ───────────────────

/**
 * 上下文敏感名称解析 atom。
 *
 * 用于在 IDE 模式下，当一个简单名称在不同上下文中有多个候选时，
 * 延迟到推断出期望类型后再选择正确的候选。
 *
 * 对齐 K2 `ConeSimpleNameForContextSensitiveResolution`。
 */
class ConeSimpleNameForContextSensitiveResolution(
    override val expression: CfirExpression,
    val containingCallCandidate: Candidate,
    val fallbackSubAtom: ConeResolutionAtom? = null,
) : ConePostponedResolvedAtom() {
    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    override val outputType: ConeCangJieType? = null
    override val expectedType: ConeCangJieType? = null
}

/**
 * 限定符上下文敏感替代 atom。
 *
 * 当限定符（qualifier）在类型检查时需要考虑上下文信息，
 * 用于 IDE 模式下的增量分析。
 *
 * 对齐 K2 `ConeContextSensitiveAlternativeForQualifierAtom`。
 */
class ConeContextSensitiveAlternativeForQualifierAtom(
    override val expression: CfirExpression,
) : ConePostponedResolvedAtom() {
    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    override val outputType: ConeCangJieType? = null
    override val expectedType: ConeCangJieType? = null
}
