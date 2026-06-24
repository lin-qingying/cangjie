package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.semantics.AbstractConeResolutionAtom
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.resolve.calls.model.PostponedCallableReferenceMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.type.model.CangJieTypeMarker

/**
 * 调用完成阶段使用的 CFIR resolution atom 基类。
 *
 * atom 是约束系统和 CFIR 表达式之间的最小桥接单位：普通表达式作为叶子，
 * 已选中候选的调用携带候选对象，lambda / callable reference 等上下文依赖表达式则作为 postponed atom。
 */
sealed class ConeResolutionAtom : AbstractConeResolutionAtom() {
    /** 当前 atom 对应的原始 CFIR 表达式。 */
    abstract override val expression: CfirExpression

    /**
     * 从 CFIR 表达式构造 resolution atom 的统一入口。
     */
    companion object {
        /**
         * 对可空表达式创建 atom；空表达式保持为空。
         */
        @JvmName("createRawAtomNullable")
        fun createRawAtom(expression: CfirExpression?): ConeResolutionAtom? {
            return expression?.let(::createRawAtom)
        }

        /**
         * 根据表达式形态创建最合适的 atom。
         *
         * lambda 和函数引用参数会被延迟；block 使用最后一个表达式作为子 atom；
         * 已解析候选的可解析表达式会携带候选，其余表达式作为简单叶子。
         */
        fun createRawAtom(expression: CfirExpression): ConeResolutionAtom {
            return when (expression) {
                is CfirAnonymousFunctionExpression -> ConeResolutionAtomWithPostponedChild(expression)
                is CfirBlock -> {
                    val childExpression = expression.statements.lastOrNull() as? CfirExpression
                    ConeResolutionAtomWithSingleChild(
                        expression = expression,
                        subAtom = childExpression?.let { createRawAtom(it) },
                    )
                }
                is CfirNamedAccessExpression -> when {
                    expression.shouldBeResolvedAsFunctionReferenceArgument() ->
                        ConeResolutionAtomWithPostponedChild(
                            expression = expression,
                            fallbackSubAtom = createRawAtomForResolvable(expression),
                        )
                    else -> createRawAtomForResolvable(expression)
                }
                is CfirResolvable -> createRawAtomForResolvable(expression)
                else -> ConeSimpleLeafResolutionAtom(expression)
            }
        }

        /**
         * 判断命名访问是否是因函数引用参数歧义而需要 postponed 处理的表达式。
         */
        private fun CfirNamedAccessExpression.shouldBeResolvedAsFunctionReferenceArgument(): Boolean {
            val diagnostic = (calleeReference as? CfirErrorNamedReference)?.diagnostic as? ConeAmbiguityError
                ?: return false
            return diagnostic.candidates.isNotEmpty() &&
                diagnostic.candidates.all { candidate ->
                    candidate.symbol.takeIf { it.isBound }?.cfir is CfirFunction
                }
        }

        /**
         * 为 `CfirResolvable` 创建携带候选或简单叶子的 atom。
         */
        private fun createRawAtomForResolvable(expression: CfirResolvable): ConeResolutionAtom {
            val candidate = (expression.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate
            val cfirExpression = expression as? CfirExpression
                ?: error("Resolvable argument is expected to be an expression: ${expression::class}")
            return if (candidate != null) {
                ConeAtomWithCandidate(cfirExpression, candidate)
            } else {
                ConeSimpleLeafResolutionAtom(cfirExpression)
            }
        }
    }
}

/**
 * 不需要候选或 postponed 分析的叶子表达式 atom。
 */
class ConeSimpleLeafResolutionAtom(
    /** 叶子 atom 对应的 CFIR 表达式。 */
    override val expression: CfirExpression,
    /** 保留与 Kotlin 推断模型兼容的参数位，当前 CFIR 实现不读取该值。 */
    @Suppress("UNUSED_PARAMETER") allowUnresolvedExpression: Boolean = true,
) : ConeResolutionAtom()

/**
 * 已经选择出调用候选的表达式 atom。
 */
class ConeAtomWithCandidate(
    /** 携带候选的调用表达式。 */
    override val expression: CfirExpression,
    /** tower resolve 选出的候选，约束系统完成会围绕它推进。 */
    val candidate: Candidate,
) : ConeResolutionAtom()

/**
 * 包含一个已经确定子 atom 的复合表达式 atom。
 *
 * 典型场景是 block 表达式，block 自身作为容器，最后一个表达式作为返回值约束来源。
 */
class ConeResolutionAtomWithSingleChild(
    /** 复合表达式本身。 */
    override val expression: CfirExpression,
    /** 参与约束传播的子 atom；没有表达式结果时可以为空。 */
    val subAtom: ConeResolutionAtom?,
) : ConeResolutionAtom()

/**
 * 已经解析成 postponed 形态、等待约束系统触发分析的 atom 基类。
 */
sealed class ConePostponedResolvedAtom : ConeResolutionAtom(), PostponedResolvedAtomMarker {
    /** postponed atom 在分析前可提供给约束系统的输入类型集合。 */
    abstract override val inputTypes: Collection<ConeCangJieType>
    /** postponed atom 分析后产出的类型；无法产出具体类型时为空。 */
    abstract override val outputType: ConeCangJieType?
    /** 外层上下文下传给 postponed atom 的期望类型。 */
    abstract override val expectedType: ConeCangJieType?
    /** 标记该 postponed atom 是否已经被约束完成器分析。 */
    final override var analyzed: Boolean = false
}

/**
 * 子 atom 需要在约束系统推进过程中再确定的表达式 atom。
 */
class ConeResolutionAtomWithPostponedChild(
    /** 延迟子表达式所属的外层 CFIR 表达式。 */
    override val expression: CfirExpression,
    /** 无法形成 postponed 子 atom 时使用的回退子 atom。 */
    val fallbackSubAtom: ConeResolutionAtom? = null,
) : ConeResolutionAtom() {
    /** 实际绑定的 postponed 子 atom 或回退子 atom。 */
    var subAtom: ConeResolutionAtom? = null
        internal set

    /**
     * 绑定一个 postponed 子 atom。
     */
    fun setPostponedSubAtom(atom: ConePostponedResolvedAtom) {
        require(subAtom == null) { "subAtom already initialized" }
        subAtom = atom
    }

    /**
     * 使用创建时保存的回退子 atom。
     */
    fun useFallbackSubAtom() {
        subAtom = fallbackSubAtom
    }

    /**
     * 创建一个共享同一表达式与回退 atom、但尚未绑定子 atom 的副本。
     */
    fun makeFreshCopy(): ConeResolutionAtomWithPostponedChild =
        ConeResolutionAtomWithPostponedChild(expression, fallbackSubAtom)
}

/**
 * resolution atom 的外部工厂对象。
 */
object ConeResolutionAtomFactory {
    /**
     * 从表达式创建默认 atom。
     */
    fun create(expression: CfirExpression): ConeResolutionAtom = ConeResolutionAtom.createRawAtom(expression)

    /**
     * 为已知候选的表达式显式创建候选 atom。
     */
    fun createWithCandidate(expression: CfirExpression, candidate: Candidate): ConeResolutionAtom {
        return ConeAtomWithCandidate(expression, candidate)
    }
}

/**
 * 与函数类型上下文直接相关的 postponed atom 基类。
 */
sealed class ConeFunctionTypeRelatedPostponedResolvedAtom : ConePostponedResolvedAtom()

/**
 * 已经获得期望函数类型信息的 lambda postponed atom。
 */
class ConeResolvedLambdaAtom(
    /** lambda 表达式节点。 */
    override val expression: CfirExpression,
    /** lambda 对应的匿名函数声明。 */
    val anonymousFunction: CfirAnonymousFunction,
    /** 外层调用下传的期望函数类型。 */
    expectedType: ConeCangJieType?,
    /** 期望函数类型中的形参类型列表。 */
    val parameterTypes: List<ConeCangJieType>,
    /** 期望函数类型中的返回类型，后续可被返回类型变量替换。 */
    returnType: ConeCangJieType,
    /** builder/factory 推断场景中代表 lambda 返回值的类型变量。 */
    typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = null,
) : ConeFunctionTypeRelatedPostponedResolvedAtom() {
    /** lambda 输入类型，即形参类型。 */
    override val inputTypes: Collection<ConeCangJieType>
        get() = parameterTypes

    /** lambda 当前输出类型。 */
    override var outputType: ConeCangJieType = returnType
        private set

    /** lambda 当前期望类型，可在 callable/lambda 推断过程中修订。 */
    override var expectedType: ConeCangJieType? = expectedType
        private set

    /** 当前绑定的 lambda 返回类型变量。 */
    var typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = typeVariableForLambdaReturnType
        private set

    /** lambda body 解析后收集到的返回表达式 atom。 */
    var returnStatements: Collection<ConeResolutionAtom> = emptyList()
        internal set

    /** lambda 当前返回类型视图。 */
    val returnType: ConeCangJieType
        get() = outputType

    /**
     * 替换 lambda 期望类型，并可同步替换输出返回类型。
     */
    fun replaceExpectedType(expectedType: ConeCangJieType, newReturnType: ConeCangJieType? = null) {
        this.expectedType = expectedType
        if (newReturnType != null) {
            outputType = newReturnType
        }
    }

    /**
     * 记录用于 lambda 返回值推断的类型变量。
     */
    fun replaceTypeVariableForLambdaReturnType(variable: ConeTypeVariableForLambdaReturnType) {
        typeVariableForLambdaReturnType = variable
    }
}

/**
 * 期望类型可被约束系统重新修订的 postponed atom 基类。
 */
sealed class ConePostponedAtomWithRevisableExpectedType :
    ConeFunctionTypeRelatedPostponedResolvedAtom(),
    PostponedAtomWithRevisableExpectedType

/**
 * 期望类型本身仍包含类型变量的 lambda atom。
 */
class ConeLambdaWithTypeVariableAsExpectedTypeAtom(
    /** lambda 表达式节点。 */
    override val expression: CfirExpression,
    /** lambda 对应的匿名函数声明。 */
    val anonymousFunction: CfirAnonymousFunction,
    /** 含类型变量的期望函数类型。 */
    override val expectedType: ConeCangJieType,
    /** 提供该期望类型的外层调用候选。 */
    val candidateOfOuterCall: Candidate,
    /** 当 lambda 作为 return 表达式时对应的匿名函数声明。 */
    val anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
) : ConePostponedAtomWithRevisableExpectedType(), LambdaWithTypeVariableAsExpectedTypeMarker {
    /** 约束系统修订后的期望类型。 */
    override var revisedExpectedType: CangJieTypeMarker? = null
        private set

    /** 从 lambda 声明处获得的参数类型，用于辅助重新分析 lambda body。 */
    override var parameterTypesFromDeclaration: List<CangJieTypeMarker?>? = null
        private set

    /** 该 atom 自身不直接提供输入类型，输入由修订后的函数类型驱动。 */
    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    /** 该 atom 不直接产出输出类型。 */
    override val outputType: ConeCangJieType? = null

    /** 修订后真正用于分析的 lambda atom。 */
    var subAtom: ConeResolvedLambdaAtom? = null
        internal set

    /**
     * 保存约束系统修订出的期望类型。
     */
    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        revisedExpectedType = expectedType
    }

    /**
     * 保存从 lambda 声明显式参数类型中抽取到的参数类型列表。
     */
    override fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?) {
        parameterTypesFromDeclaration = types
    }
}

/**
 * callable reference 的 postponed atom。
 */
class ConeResolvedCallableReferenceAtom(
    /** callable reference 表达式。 */
    override val expression: CfirExpression,
    /** callable reference 的上下文期望类型。 */
    override val expectedType: ConeCangJieType?,
) : ConePostponedAtomWithRevisableExpectedType(), PostponedCallableReferenceMarker {
    /** callable reference 不直接提供输入类型。 */
    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    /** callable reference 的结果类型在单独字段中记录。 */
    override val outputType: ConeCangJieType? = null
    /** callable reference 解析完成后得到的函数类型。 */
    var resultingTypeForCallableReference: ConeCangJieType? = null
        internal set
    /** 标记该 callable reference 是否因候选歧义而推迟解析。 */
    var isPostponedBecauseOfAmbiguity: Boolean = false
        internal set

    /** 约束系统修订后的期望类型。 */
    override var revisedExpectedType: CangJieTypeMarker? = null
        private set

    /**
     * 保存 callable reference 的修订期望类型。
     */
    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        revisedExpectedType = expectedType
    }
}

/**
 * 需要从外层期望类型中恢复含义的简单名 atom。
 */
class ConeSimpleNameForContextSensitiveResolution(
    /** 简单名表达式。 */
    override val expression: CfirExpression,
    /** 外层上下文提供的期望类型。 */
    override val expectedType: ConeCangJieType,
    /** 包含该简单名的外层调用候选。 */
    val containingCallCandidate: Candidate,
    /** 上下文敏感解析失败时可继续使用的回退 atom。 */
    val fallbackSubAtom: ConeResolutionAtom,
) : ConePostponedResolvedAtom() {
    /** 简单名上下文敏感解析只依赖期望类型作为输入。 */
    override val inputTypes: Collection<ConeCangJieType> = listOf(expectedType)
    /** 简单名自身不在该层直接产出类型。 */
    override val outputType: ConeCangJieType? = null
}

/**
 * qualifier 上下文敏感解析的候选替代 atom。
 */
class ConeContextSensitiveAlternativeForQualifierAtom(
    /** qualifier 表达式。 */
    override val expression: CfirExpression,
) : ConePostponedResolvedAtom() {
    /** qualifier 替代项不直接提供输入类型。 */
    override val inputTypes: Collection<ConeCangJieType> = emptyList()
    /** qualifier 替代项不直接产出输出类型。 */
    override val outputType: ConeCangJieType? = null
    /** qualifier 替代项没有独立期望类型。 */
    override val expectedType: ConeCangJieType? = null
}
