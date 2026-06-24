package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.NoMutableState
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.typeApproximator
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintIncorporator
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintInjector
import org.cangnova.cangjie.resolve.calls.inference.components.LegacyVariableReadinessCalculator
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver
import org.cangnova.cangjie.resolve.calls.inference.components.ResultTypeResolver
import org.cangnova.cangjie.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.cangnova.cangjie.resolve.calls.inference.components.VariableFixationFinder
import org.cangnova.cangjie.resolve.calls.inference.components.VariableReadinessCalculator
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl

/**
 * 调用类型推断需要的会话级组件集合。
 *
 * 该组件统一创建约束注入、约束归并、变量固定、延迟实参分析和结果类型解析器，
 * 保证一次调用解析内的推断流程共享同一套类型上下文和语言版本设置。
 */
@NoMutableState
class InferenceComponents(override val session: CfirSession) : CfirSessionComponent, SessionHolder {
    /**
     * 当前会话的推断类型上下文。
     */
    private val typeContext: ConeInferenceContext = session.typeContext
    /**
     * 当前会话的类型近似器。
     */
    private val approximator = session.typeApproximator

    /**
     * 判断约束类型是否可直接推断的基础 oracle。
     */
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle =
        TrivialConstraintTypeInferenceOracle.create(typeContext)
    /**
     * 将新约束并入约束系统并传播派生约束的组件。
     */
    private val incorporator =
        ConstraintIncorporator(
            approximator,
            trivialConstraintTypeInferenceOracle,
            ConeConstraintSystemUtilContext,
            session.languageVersionSettings,
            session.inferenceLogger,
        )
    /**
     * 向约束系统注入 subtype/equality 等约束的组件。
     */
    private val injector = ConstraintInjector(
        incorporator,
        approximator,
        session.languageVersionSettings,
        session.inferenceLogger,
    )
    /**
     * 根据约束系统状态解析候选调用的最终结果类型。
     */
    val resultTypeResolver: ResultTypeResolver =
        ResultTypeResolver(approximator, trivialConstraintTypeInferenceOracle, session.languageVersionSettings)
    /**
     * 选择下一批可固定类型变量的组件。
     */
    val variableFixationFinder: VariableFixationFinder = run {
        val variableReadinessCalculatorBuilder =
            ::VariableReadinessCalculator.takeIf {
                session.languageVersionSettings.supportsFeature(LanguageFeature.LexicographicVariableReadinessCalculation)
            }
                ?: ::LegacyVariableReadinessCalculator

        VariableFixationFinder(
            session.languageVersionSettings,
            variableReadinessCalculatorBuilder(
                trivialConstraintTypeInferenceOracle,
                session.languageVersionSettings,
                session.inferenceLogger,
            ),
        )
    }
    /**
     * 为 lambda、callable reference 等延迟实参解析输入类型的组件。
     */
    val postponedArgumentInputTypesResolver: PostponedArgumentInputTypesResolver =
        PostponedArgumentInputTypesResolver(
            resultTypeResolver, variableFixationFinder, ConeConstraintSystemUtilContext
        )

    /**
     * 约束系统工厂，供调用候选构造和测试夹具复用。
     */
    val constraintSystemFactory: ConstraintSystemFactory = ConstraintSystemFactory()

    /**
     * 创建新的调用约束系统实例。
     */
    fun createConstraintSystem(): ConstraintSystemImpl {
        return ConstraintSystemImpl(
            injector, typeContext,
            session.languageVersionSettings,
        )
    }

    /**
     * 暴露给外部组件的约束系统工厂包装。
     */
    inner class ConstraintSystemFactory {
        /**
         * 委托 [InferenceComponents] 创建新的约束系统。
         */
        fun createConstraintSystem(): ConstraintSystemImpl {
            return this@InferenceComponents.createConstraintSystem()
        }
    }
}

/**
 * 当前会话的类型推断组件集合。
 */
val CfirSession.inferenceComponents: InferenceComponents by CfirSession.sessionComponentAccessor()
