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

@NoMutableState
class InferenceComponents(override val session: CfirSession) : CfirSessionComponent, SessionHolder {
    private val typeContext: ConeInferenceContext = session.typeContext
    private val approximator = session.typeApproximator

    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle =
        TrivialConstraintTypeInferenceOracle.create(typeContext)
    private val incorporator =
        ConstraintIncorporator(
            approximator,
            trivialConstraintTypeInferenceOracle,
            ConeConstraintSystemUtilContext,
            session.languageVersionSettings,
            session.inferenceLogger,
        )
    private val injector = ConstraintInjector(
        incorporator,
        approximator,
        session.languageVersionSettings,
        session.inferenceLogger,
    )
    val resultTypeResolver: ResultTypeResolver =
        ResultTypeResolver(approximator, trivialConstraintTypeInferenceOracle, session.languageVersionSettings)
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
    val postponedArgumentInputTypesResolver: PostponedArgumentInputTypesResolver =
        PostponedArgumentInputTypesResolver(
            resultTypeResolver, variableFixationFinder, ConeConstraintSystemUtilContext
        )

    val constraintSystemFactory: ConstraintSystemFactory = ConstraintSystemFactory()

    fun createConstraintSystem(): ConstraintSystemImpl {
        return ConstraintSystemImpl(
            injector, typeContext,
            session.languageVersionSettings,
        )
    }

    inner class ConstraintSystemFactory {
        fun createConstraintSystem(): ConstraintSystemImpl {
            return this@InferenceComponents.createConstraintSystem()
        }
    }
}

val CfirSession.inferenceComponents: InferenceComponents by CfirSession.sessionComponentAccessor()
