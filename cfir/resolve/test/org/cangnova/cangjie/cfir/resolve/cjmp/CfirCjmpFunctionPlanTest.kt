package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirParameterDefaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameterCopy
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.mpp.AbstractCjmpMatcher
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCandidateTraversal
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchEvaluator
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchEvent
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchPlan
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchResult
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 真实 CFIR 头部进入纯求值器，验证默认来源跨失败候选延续且没有写回源声明。 */
class CfirCjmpFunctionPlanTest {
    private val session = CallResolutionTestFixtures.newTestSession()

    @Test
    fun transferredDefaultSurvivesLaterNameFailureAndAffectsNextCandidate() {
        val specific = function("specific", listOf("first", "second"), listOf(false, false))
        val first = function("commonFirst", listOf("first", "different"), listOf(true, false))
        val second = function("commonSecond", listOf("first", "second"), listOf(true, false))
        val sourceDefault = first.valueParameters[0].defaultValue
        val sourceHelper = first.valueParameters[0].defaultValueMetadata!!.function!!
        val plan = plan(specific, listOf(candidate(first), candidate(second)))
        assertEquals(CjmpMatchResult.Matched, plan.candidates[0].eligibility)
        assertEquals(CjmpMatchResult.Matched, plan.candidates[0].signatureCompatibility)
        assertEquals(listOf(true, false), plan.candidates[0].parameters.map { it.namesMatch })
        assertNull(specific.valueParameters[0].defaultValue)

        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan))
        assertEquals(false, aggregate.matched[specific.symbol])
        val transferred = aggregate.defaults.getValue(specific.symbol)[0]
        assertTrue(transferred.hasInitial)
        assertSame(sourceHelper.symbol, transferred.desugaredDefaultSource)
        val events = aggregate.events.map { it as CjmpMatchEvent.ParameterMismatch<*, *> }
        assertEquals(listOf(CjmpMismatchKind.PARAMETER_NAMES, CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES),
            events.map { it.mismatch.kind })
        assertEquals(listOf(1, 0), events.map { it.mismatch.parameterIndex })
        assertTrue(aggregate.acceptedBindings.isEmpty())
        assertNull(specific.valueParameters[0].defaultValue, "The evaluator must not clone expressions into CFIR")
        assertSame(sourceDefault, first.valueParameters[0].defaultValue)
    }

    @Test
    fun aLaterSuccessfulCandidateKeepsTheEarlierDefaultAndParameterDiagnostic() {
        val specific = function("specific", listOf("first", "second"), listOf(false, false))
        val rejected = function("rejected", listOf("first", "different"), listOf(true, false))
        val accepted = function("accepted", listOf("first", "second"), listOf(false, false))
        val unvisited = function("unvisited", listOf("first", "second"), listOf(false, false))
        val sourceHelper = rejected.valueParameters[0].defaultValueMetadata!!.function!!
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(candidate(rejected), candidate(accepted), candidate(unvisited)))))

        assertEquals(true, aggregate.matched[specific.symbol])
        assertEquals(listOf(rejected.symbol, accepted.symbol), aggregate.visits.map { it.common })
        assertSame(specific.symbol, aggregate.implementations[accepted.symbol])
        assertSame(sourceHelper.symbol, aggregate.defaults.getValue(specific.symbol)[0].desugaredDefaultSource)
        val event = aggregate.events.single() as CjmpMatchEvent.ParameterMismatch<*, *>
        assertSame(rejected.symbol, event.common)
        assertEquals(CjmpMismatchKind.PARAMETER_NAMES, event.mismatch.kind)
        assertEquals(1, event.mismatch.parameterIndex)
        assertNull(specific.valueParameters[0].defaultValue)
    }

    @Test
    fun aLaterDefaultConflictKeepsTheTransferredPrefixForTheNextSuccessfulCandidate() {
        val specific = function("specific", listOf("first", "second"), listOf(false, true))
        val rejected = function("rejected", listOf("first", "second"), listOf(true, true))
        val accepted = function("accepted", listOf("first", "second"), listOf(false, false))
        val transferredHelper = rejected.valueParameters[0].defaultValueMetadata!!.function!!
        val originalHelper = specific.valueParameters[1].defaultValueMetadata!!.function!!
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(candidate(rejected), candidate(accepted)))))

        assertEquals(true, aggregate.matched[specific.symbol])
        assertEquals(listOf(rejected.symbol, accepted.symbol), aggregate.visits.map { it.common })
        assertSame(specific.symbol, aggregate.implementations[accepted.symbol])
        val defaults = aggregate.defaults.getValue(specific.symbol)
        assertSame(transferredHelper.symbol, defaults[0].desugaredDefaultSource)
        assertSame(originalHelper.symbol, defaults[1].desugaredDefaultSource)
        val event = aggregate.events.single() as CjmpMatchEvent.ParameterMismatch<*, *>
        assertEquals(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, event.mismatch.kind)
        assertEquals(1, event.mismatch.parameterIndex)
        assertNull(specific.valueParameters[0].defaultValue)
        assertSame(originalHelper, specific.valueParameters[1].defaultValueMetadata!!.function)
    }

    @Test
    fun preparedCandidateLeavesDefaultConflictAndParameterNamesForTheOrderedProgram() {
        val specific = function("specific", listOf("first", "second"), listOf(true, false))
        val common = function("common", listOf("first", "different"), listOf(true, false))
        val specificHelper = specific.valueParameters[0].defaultValueMetadata!!.function!!
        val commonHelper = common.valueParameters[0].defaultValueMetadata!!.function!!
        val context = CfirCjmpMatchingContext(session)
        val prepared = AbstractCjmpMatcher.prepareCandidate(specific.symbol, common.symbol, context)

        assertEquals(CjmpMatchResult.Matched, prepared.eligibility)
        assertEquals(CjmpMatchResult.Matched, prepared.signatureCompatibility)
        assertSame(specificHelper, specific.valueParameters[0].defaultValueMetadata!!.function)
        assertSame(commonHelper, common.valueParameters[0].defaultValueMetadata!!.function)
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(candidate(common)))))
        val event = aggregate.events.single() as CjmpMatchEvent.ParameterMismatch<*, *>
        assertEquals(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, event.mismatch.kind)
        assertEquals(0, event.mismatch.parameterIndex)
        assertSame(specificHelper.symbol, aggregate.defaults.getValue(specific.symbol)[0].desugaredDefaultSource)
        assertTrue(aggregate.implementations.isEmpty())
    }

    @Test
    fun alpha65ResolverPlanDoesNotApplyTheLaterBothSideDefaultConflictRule() {
        val alpha65Session = CallResolutionTestFixtures.newTestSession(
            languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_65),
        )
        val specific = function(alpha65Session, "specific", listOf("value"), listOf(true))
        val common = function(alpha65Session, "common", listOf("value"), listOf(true))
        val plan = CfirCjmpResolver.buildCallableMatchPlan(
            specific,
            cfirCjmpDefaultStates(specific),
            listOf(candidate(common)),
            alpha65Session,
            CjmpCandidateTraversal.FIRST_SUCCESS,
            emptyMap(),
        )

        assertFalse(plan.checkParameterDefaultValueConflict)
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan))
        assertEquals(true, aggregate.matched[specific.symbol])
        assertTrue(aggregate.events.isEmpty())
        assertSame(specific.symbol, aggregate.implementations[common.symbol])
    }

    @Test
    fun alpha66ResolverPlanAppliesTheBothSideDefaultConflictRule() {
        val alpha66Session = CallResolutionTestFixtures.newTestSession(
            languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_66),
        )
        val specific = function(alpha66Session, "specific", listOf("value"), listOf(true))
        val common = function(alpha66Session, "common", listOf("value"), listOf(true))
        val plan = CfirCjmpResolver.buildCallableMatchPlan(
            specific,
            cfirCjmpDefaultStates(specific),
            listOf(candidate(common)),
            alpha66Session,
            CjmpCandidateTraversal.FIRST_SUCCESS,
            emptyMap(),
        )

        assertTrue(plan.checkParameterDefaultValueConflict)
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan))
        assertEquals(false, aggregate.matched[specific.symbol])
        val event = aggregate.events.single() as CjmpMatchEvent.ParameterMismatch<*, *>
        assertEquals(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, event.mismatch.kind)
        assertEquals(0, event.mismatch.parameterIndex)
        assertTrue(aggregate.implementations.isEmpty())
    }

    @Test
    fun incompatibleSignatureDoesNotExecuteNamesOrTransferDefaults() {
        val specific = function("specific", listOf("first", "second"), listOf(false, false))
        val common = function("common", listOf("first", "different"), listOf(true, false))
        common.replaceReturnTypeRef(ConePrimitiveType.BOOLEAN.toCfirResolvedTypeRef(null))
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(candidate(common)))))

        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), aggregate.visits.single().result)
        assertTrue(aggregate.events.isEmpty())
        assertTrue(aggregate.defaults.getValue(specific.symbol).none { it.hasInitial || it.desugaredDefaultSource != null })
        assertTrue(aggregate.implementations.isEmpty())
    }

    @Test
    fun afterParameterFailureKeepsEarlierDefaultAndOrderedDiagnosticData() {
        val specific = function("specific", listOf("value"), listOf(false))
        val common = function("common", listOf("value"), listOf(true))
        val checks = CfirCjmpCallableCandidate(
            common = common,
            commonDefaults = cfirCjmpDefaultStates(common),
            beforeSignatureDiagnostics = listOf("before signature"),
            afterParameterDiagnostics = listOf("after parameters"),
            afterParameters = CjmpMatchResult.Mismatched(CjmpMismatchKind.PARAMETER_SHAPE),
            beforeBindingDiagnostics = listOf("must not execute"),
        )
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(checks))))
        assertEquals(false, aggregate.matched[specific.symbol])
        assertSame(common.valueParameters[0].defaultValueMetadata!!.function!!.symbol,
            aggregate.defaults.getValue(specific.symbol)[0].desugaredDefaultSource)
        assertEquals(listOf("before signature", "after parameters"),
            aggregate.events.map { (it as CjmpMatchEvent.DiagnosticData<*, *>).diagnostic })
        assertTrue(aggregate.implementations.isEmpty())
    }

    @Test
    fun initialFlagWithoutDesugaredSourceDoesNotTransferLibraryMarker() {
        val specific = function("specific", listOf("value"), listOf(false))
        val common = function("libraryCommon", listOf("value"), listOf(true))
        common.valueParameters[0].replaceDefaultValue(buildLiteralExpression {
            coneTypeOrNull = ConePrimitiveType.INT64
            kind = CfirLiteralKind.UNIT
            value = null
        })
        common.valueParameters[0].defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = true, function = null)
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(candidate(common)))))
        assertEquals(true, aggregate.matched[specific.symbol])
        assertFalse(aggregate.defaults.getValue(specific.symbol)[0].hasInitial)
        assertNull(aggregate.defaults.getValue(specific.symbol)[0].desugaredDefaultSource)
        assertNotNull(common.valueParameters[0].defaultValue, "A library marker alone is not a transfer source")
    }

    @Test
    fun planKeepsItsSignatureSnapshotWhenLaterBodyInferenceChangesCfir() {
        val specific = function("specific", emptyList(), emptyList())
        val common = function("common", emptyList(), emptyList())
        val plan = plan(specific, listOf(candidate(common)))
        specific.replaceReturnTypeRef(ConePrimitiveType.BOOLEAN.toCfirResolvedTypeRef(null))
        val aggregate = CjmpMatchEvaluator.evaluate(listOf(plan))
        assertEquals(true, aggregate.matched[specific.symbol])
        assertSame(specific.symbol, aggregate.implementations[common.symbol])
    }

    private fun candidate(common: CfirNamedFunction): CfirCjmpCallableCandidate<String> = CfirCjmpCallableCandidate(
        common = common,
        commonDefaults = cfirCjmpDefaultStates(common),
        beforeSignatureDiagnostics = emptyList(),
        afterParameterDiagnostics = emptyList(),
        afterParameters = CjmpMatchResult.Matched,
        beforeBindingDiagnostics = emptyList(),
    )

    private fun plan(
        specific: CfirNamedFunction,
        candidates: List<CfirCjmpCallableCandidate<String>>,
    ): CjmpMatchPlan.Declaration<CfirBasedSymbol<*>, CfirTypeParameterSymbol, String> =
        CfirCjmpResolver.buildCallableMatchPlan(
            specific, cfirCjmpDefaultStates(specific), candidates, session, CjmpCandidateTraversal.FIRST_SUCCESS, emptyMap(),
        )

    /** 使用真实 helper 声明记录来源；参数符号与默认表达式都不是 desugar 函数。 */
    private fun function(name: String, parameterNames: List<String>, defaults: List<Boolean>): CfirNamedFunction {
        return function(session, name, parameterNames, defaults)
    }

    private fun function(
        session: CallResolutionTestFixtures.TestSession,
        name: String,
        parameterNames: List<String>,
        defaults: List<Boolean>,
    ): CfirNamedFunction {
        val function = CallResolutionTestFixtures.buildFunctionSymbol(
            session,
            name,
            returnType = ConePrimitiveType.INT64,
            parameterTypes = parameterNames.map { ConePrimitiveType.INT64 },
            parameterNames = parameterNames,
            parameterDefaults = defaults,
        ).cfir
        function.replaceValueParameters(function.valueParameters.map { parameter ->
            buildValueParameterCopy(parameter) {
                symbol = CfirValueParameterSymbol(parameter.symbol.callableId)
                isNamed = true
            }
        })
        function.valueParameters.forEachIndexed { index, parameter ->
            if (defaults[index]) {
                val helper = CallResolutionTestFixtures.buildFunctionSymbol(
                    session, "${name}Default$index",
                    returnType = ConePrimitiveType.INT64,
                    parameterTypes = parameterNames.take(index).map { ConePrimitiveType.INT64 },
                    parameterNames = parameterNames.take(index),
                ).cfir
                parameter.defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = true, function = helper)
            }
        }
        return function
    }
}
