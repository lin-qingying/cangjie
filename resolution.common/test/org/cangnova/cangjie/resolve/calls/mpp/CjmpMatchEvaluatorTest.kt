package org.cangnova.cangjie.resolve.calls.mpp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 依据官方 v1.1.3 CheckCJMP.cpp 的候选动作顺序验证纯求值，不依赖 CFIR 或 reporter。
 *
 * TrySetSpecificImpl 为 898–912，逐参数默认值动作及后续绑定为 936–990，property 为
 * 993–1013，variable/pattern 为 1050–1113。默认转移与多候选组合属于源码控制流回归，
 * 不冒称它们都已有合法的多 common CJO 端到端探针。完整证据见 P1-CANDIDATE-EVENTS.md。
 */
class CjmpMatchEvaluatorTest {
    private data class Declaration(val name: String) : CjmpDeclarationSymbolMarker
    private data class TypeParameter(val name: String) : CjmpTypeParameterSymbolMarker

    @Test
    fun `shared matching result rejects consumer mutations at every collection boundary`() {
        val common = Declaration("common")
        val specific = Declaration("specific")
        val defaultSource = Declaration("default source")
        val mapping = linkedMapOf(TypeParameter("T") to TypeParameter("U"), TypeParameter("V") to TypeParameter("W"))
        val plan = declaration(
            specific,
            candidate(specific, common).copy(
                parameters = listOf(parameter(0, defaultSource)),
                beforeSignatureDiagnostics = listOf("diagnostic"),
                binding = CjmpBindingRule.Callable(request(specific, common, mapping = mapping)),
            ),
            defaults = listOf(noDefault()),
        )
        val seed = accepted(Declaration("seed specific"), Declaration("seed common")).copy(typeParameterMapping = mapping)
        val result = evaluate(plan, seeds = listOf(seed))
        val expectedMapping = mapping.toMap()
        mapping.clear()

        result.acceptedBindings.forEach { assertEquals(expectedMapping, it.typeParameterMapping) }
        val mutations = listOf<() -> Unit>(
            { (result.matched as MutableMap).clear() },
            { (result.implementations as MutableMap).clear() },
            { (result.doNotExport as MutableSet).clear() },
            { (result.defaults as MutableMap).clear() },
            { (result.defaults.getValue(specific) as MutableList).clear() },
            { (result.acceptedBindings as MutableList).clear() },
            { (result.visits as MutableList).clear() },
            { (result.events as MutableList).clear() },
        ) + result.acceptedBindings.map { binding -> { (binding.typeParameterMapping as MutableMap).clear() } }
        mutations.forEach { mutation -> assertThrows(UnsupportedOperationException::class.java, mutation) }
        assertEquals(specific, result.implementations[common])
        assertEquals(listOf(defaultFrom(defaultSource)), result.defaults[specific])
    }

    @Test
    fun `facts factory detaches root plans seeds and nested candidate collections before evaluation`() {
        val specific = Declaration("specific")
        val common = Declaration("common")
        val seededSpecific = Declaration("seeded specific")
        val seededCommon = Declaration("seeded common")
        val defaultSource = Declaration("default source")
        val commonType = TypeParameter("T")
        val specificType = TypeParameter("U")
        val mapping = linkedMapOf(commonType to specificType)
        val seedMapping = linkedMapOf(commonType to TypeParameter("seed U"))
        val parameters = mutableListOf(parameter(0, defaultSource))
        val beforeSignature = mutableListOf("before signature")
        val afterParameters = mutableListOf("after parameters")
        val beforeBinding = mutableListOf("before binding")
        val candidates = mutableListOf(
            candidate(specific, common).copy(
                beforeSignatureDiagnostics = beforeSignature,
                parameters = parameters,
                afterParameterDiagnostics = afterParameters,
                beforeBindingDiagnostics = beforeBinding,
                binding = CjmpBindingRule.Callable(request(specific, common, mapping = mapping)),
            ),
        )
        val defaults = mutableListOf(noDefault())
        val source = CjmpMatchPlan.Declaration(
            specific = specific,
            initialDefaults = defaults,
            candidates = candidates,
            traversal = CjmpCandidateTraversal.FIRST_SUCCESS,
            checkParameterDefaultValueConflict = true,
        )

        val plans = mutableListOf(source)
        val seeds = mutableListOf(accepted(seededSpecific, seededCommon).copy(typeParameterMapping = seedMapping))
        val facts = CjmpMatchFacts.create(plans, seeds)
        val expected = CjmpMatchEvaluator.evaluate(plans, seeds)
        plans.clear()
        seeds.clear()
        candidates.clear()
        defaults[0] = defaultFrom(Declaration("changed default source"))
        parameters.clear()
        beforeSignature += "changed signature diagnostic"
        afterParameters.clear()
        beforeBinding.clear()
        mapping.clear()
        seedMapping.clear()

        val result = CjmpMatchEvaluator.evaluateFacts(listOf(facts))
        assertEquals(expected, result)
        assertEquals(listOf(defaultFrom(defaultSource)), result.defaults[specific])
        val frozen = facts.plans.single() as CjmpMatchPlan.Declaration<Declaration, TypeParameter, String>
        val frozenCandidate = frozen.candidates.single()
        val mutations = listOf<() -> Unit>(
            { (facts.plans as MutableList).clear() },
            { (facts.initialBindings as MutableList).clear() },
            { (facts.initialBindings.single().typeParameterMapping as MutableMap).clear() },
            { (frozen.initialDefaults as MutableList).clear() },
            { (frozen.candidates as MutableList).clear() },
            { (frozenCandidate.parameters as MutableList).clear() },
            { (frozenCandidate.beforeSignatureDiagnostics as MutableList).clear() },
            { (frozenCandidate.afterParameterDiagnostics as MutableList).clear() },
            { (frozenCandidate.beforeBindingDiagnostics as MutableList).clear() },
            { ((frozenCandidate.binding as CjmpBindingRule.Callable).request.typeParameterMapping as MutableMap).clear() },
        )
        mutations.forEach { mutation -> assertThrows(UnsupportedOperationException::class.java, mutation) }
        assertEquals(expected, CjmpMatchEvaluator.evaluateFacts(listOf(facts)))
    }

    @Test
    fun `occupied common reports occupancy before checking missing implementation`() {
        val common = Declaration("common")
        val previous = Declaration("previous")
        val specific = Declaration("specific")
        val seed = accepted(previous, common)
        val result = evaluate(
            declaration(specific, candidate(specific, common, requiresImplementation = true)),
            seeds = listOf(seed),
        )

        assertEquals(listOf(occupied(specific, common, previous)), result.events)
        assertEquals(listOf(seed), result.acceptedBindings)
        assertEquals(mapOf(common to previous), result.implementations)
        assertEquals(mapOf(specific to false), result.matched)
        assertEquals(listOf(visit(specific, common, mismatch(CjmpMismatchKind.SECOND_BINDING))), result.visits)
    }

    @Test
    fun `root preparation order does not change canonical first fit defaults or candidate visits`() {
        val firstCommon = Declaration("first common")
        val secondCommon = Declaration("second common")
        val defaultSource = Declaration("first common default")
        val first = Declaration("first specific")
        val second = Declaration("second specific")
        val plans = listOf(
            declaration(
                first,
                candidate(first, firstCommon).copy(parameters = listOf(parameter(0, defaultSource))),
                candidate(first, secondCommon).copy(beforeBindingDiagnostics = listOf("unvisited first tail")),
                defaults = listOf(noDefault()),
            ),
            declaration(
                second,
                candidate(second, firstCommon).copy(parameters = listOf(parameter(0, defaultSource))),
                candidate(second, secondCommon).copy(parameters = listOf(parameter(0))),
                defaults = listOf(noDefault()),
            ),
        )
        val preparedForward = plans.map { CjmpMatchFacts.create(listOf(it)) }
        val preparedBackward = plans.asReversed().associate { it.specific to CjmpMatchFacts.create(listOf(it)) }
        val canonicalFacts = plans.map { preparedBackward.getValue(it.specific) }

        val result = CjmpMatchEvaluator.evaluateFacts(canonicalFacts)
        assertEquals(CjmpMatchEvaluator.evaluate(plans), result)
        assertEquals(CjmpMatchEvaluator.evaluateFacts(preparedForward), result)
        assertEquals(listOf(occupied(second, firstCommon, first)), result.events)
        assertEquals(
            listOf(
                visit(first, firstCommon),
                visit(second, firstCommon, mismatch(CjmpMismatchKind.SECOND_BINDING)),
                visit(second, secondCommon),
            ),
            result.visits,
        )
        assertEquals(listOf(accepted(first, firstCommon), accepted(second, secondCommon)), result.acceptedBindings)
        assertEquals(mapOf(firstCommon to first, secondCommon to second), result.implementations)
        assertEquals(
            mapOf(first to listOf(defaultFrom(defaultSource)), second to listOf(defaultFrom(defaultSource))),
            result.defaults,
        )
        assertEquals(plans, canonicalFacts.flatMap { it.plans })
        assertEquals(result, CjmpMatchEvaluator.evaluateFacts(canonicalFacts))
    }

    @Test
    fun `facts factory preserves conflicting merge seeds for evaluation to reject`() {
        val common = Declaration("seed common")
        val first = Declaration("first seed specific")
        val second = Declaration("second seed specific")
        val seeds = listOf(accepted(first, common), accepted(second, common))
        val plan = declaration(second, candidate(second, common))

        val facts = CjmpMatchFacts.create(listOf(plan), seeds)

        assertEquals(seeds, facts.initialBindings)
        assertEquals(listOf(plan), facts.plans)
        assertThrows(IllegalArgumentException::class.java) { CjmpMatchEvaluator.evaluateFacts(listOf(facts)) }
    }

    @Test
    fun `ordered specifics retry the next common after the first candidate is occupied`() {
        val first = Declaration("first specific")
        val second = Declaration("second specific")
        val firstCommon = Declaration("first common")
        val secondCommon = Declaration("second common")
        val firstCommonDefault = Declaration("first common default")
        val result = CjmpMatchEvaluator.evaluate(
            listOf(
                declaration(
                    first,
                    candidate(first, firstCommon).copy(parameters = listOf(parameter(0, firstCommonDefault))),
                    candidate(first, secondCommon).copy(parameters = listOf(parameter(0))),
                    defaults = listOf(noDefault()),
                ),
                declaration(
                    second,
                    candidate(second, firstCommon).copy(parameters = listOf(parameter(0, firstCommonDefault))),
                    candidate(second, secondCommon).copy(parameters = listOf(parameter(0))),
                    defaults = listOf(noDefault()),
                ),
            ),
        )

        assertEquals(
            listOf(
                visit(first, firstCommon),
                visit(second, firstCommon, mismatch(CjmpMismatchKind.SECOND_BINDING)),
                visit(second, secondCommon),
            ),
            result.visits,
        )
        assertEquals(listOf(occupied(second, firstCommon, first)), result.events)
        assertEquals(
            listOf(accepted(first, firstCommon), accepted(second, secondCommon)),
            result.acceptedBindings,
        )
        assertEquals(mapOf(firstCommon to first, secondCommon to second), result.implementations)
        assertEquals(
            mapOf(first to listOf(defaultFrom(firstCommonDefault)), second to listOf(defaultFrom(firstCommonDefault))),
            result.defaults,
        )
        assertEquals(mapOf(first to true, second to true), result.matched)
    }

    @Test
    fun `unoccupied common with missing implementation does not acquire a binding`() {
        val common = Declaration("common")
        val specific = Declaration("specific")
        val result = evaluate(declaration(specific, candidate(specific, common, requiresImplementation = true)))

        assertEquals(
            listOf<CjmpMatchEvent<Declaration, String>>(CjmpMatchEvent.MissingImplementation(specific, common)),
            result.events,
        )
        assertTrue(result.implementations.isEmpty())
        assertTrue(result.acceptedBindings.isEmpty())
        assertEquals(listOf(visit(specific, common, mismatch(CjmpMismatchKind.MISSING_BODY))), result.visits)
    }

    @Test
    fun `default transfer survives a later parameter name failure`() {
        val specific = Declaration("specific")
        val common = Declaration("common")
        val defaultSource = Declaration("common parameter zero")
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, common).copy(
                    parameters = listOf(parameter(0, defaultSource), parameter(1, namesMatch = false)),
                    afterParameterDiagnostics = listOf("unreachable after parameters"),
                    beforeBindingDiagnostics = listOf("unreachable before binding"),
                ),
                defaults = listOf(noDefault(), noDefault()),
            ),
        )

        assertEquals(listOf(defaultFrom(defaultSource), noDefault()), result.defaults[specific])
        assertEquals(listOf(parameterEvent(specific, common, 1, CjmpMismatchKind.PARAMETER_NAMES)), result.events)
        assertEquals(mapOf(specific to false), result.matched)
        assertTrue(result.acceptedBindings.isEmpty())
    }

    @Test
    fun `parameter default conflict is evaluated only when the declaration plan enables it`() {
        val specific = Declaration("specific")
        val common = Declaration("common")
        val specificDefault = Declaration("specific default")
        val commonDefault = Declaration("common default")
        val candidate = candidate(specific, common).copy(parameters = listOf(parameter(0, commonDefault)))
        val initialDefaults = listOf(defaultFrom(specificDefault))

        val historicalPlan = declaration(specific, candidate, defaults = initialDefaults)
            .copy(checkParameterDefaultValueConflict = false)
        val historicalFacts = CjmpMatchFacts.create(listOf(historicalPlan))
        val historical = CjmpMatchEvaluator.evaluateFacts(listOf(historicalFacts))
        val current = evaluate(declaration(specific, candidate, defaults = initialDefaults))

        assertEquals(listOf(visit(specific, common)), historical.visits)
        assertEquals(mapOf(specific to true), historical.matched)
        assertEquals(listOf(defaultFrom(specificDefault)), historical.defaults[specific])
        assertTrue(historical.events.isEmpty())
        assertFalse(
            (historicalFacts.plans.single() as CjmpMatchPlan.Declaration<Declaration, TypeParameter, String>)
                .checkParameterDefaultValueConflict,
        )

        assertEquals(
            listOf(visit(
                specific,
                common,
                CjmpMatchResult.ParameterMismatched(
                    CjmpParameterMismatch(CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES, 0),
                ),
            )),
            current.visits,
        )
        assertEquals(mapOf(specific to false), current.matched)
        assertEquals(
            listOf(parameterEvent(specific, common, 0, CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES)),
            current.events,
        )
    }

    @Test
    fun `default transferred before occupied failure changes the next candidate`() {
        val specific = Declaration("specific")
        val firstCommon = Declaration("occupied common")
        val nextCommon = Declaration("next common")
        val previous = Declaration("previous specific")
        val firstDefault = Declaration("first default")
        val nextDefault = Declaration("next default")
        val seed = accepted(previous, firstCommon)
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, firstCommon).copy(parameters = listOf(parameter(0, firstDefault))),
                candidate(specific, nextCommon).copy(parameters = listOf(parameter(0, nextDefault))),
                defaults = listOf(noDefault()),
            ),
            seeds = listOf(seed),
        )

        assertEquals(
            listOf(
                occupied(specific, firstCommon, previous),
                parameterEvent(specific, nextCommon, 0, CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES),
            ),
            result.events,
        )
        assertEquals(listOf(defaultFrom(firstDefault)), result.defaults[specific])
        assertEquals(listOf(seed), result.acceptedBindings)
        assertEquals(mapOf(firstCommon to previous), result.implementations)
        assertEquals(listOf(firstCommon, nextCommon), result.visits.map { it.common })
        assertFalse(result.matched.getValue(specific))
    }

    @Test
    fun `initial flag alone does not manufacture a transferable default source`() {
        val specific = Declaration("specific")
        val common = Declaration("common")
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, common).copy(
                    parameters = listOf(CjmpParameterMatch(0, true, CjmpParameterDefaultState(true, null))),
                ),
                defaults = listOf(noDefault()),
            ),
        )

        assertEquals(listOf(noDefault()), result.defaults[specific])
        assertTrue(result.matched.getValue(specific))
        assertEquals(listOf(accepted(specific, common)), result.acceptedBindings)
    }

    @Test
    fun `serialized default marker without helper remains readable through the specific declaration`() {
        val specific = Declaration("specific")
        val common = Declaration("common")
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, common).copy(
                    parameters = listOf(
                        CjmpParameterMatch(0, true, CjmpParameterDefaultState(true, null, canBeReadThrough = true)),
                    ),
                ),
                defaults = listOf(noDefault()),
            ),
        )

        assertEquals(listOf(CjmpParameterDefaultState(true, null, canBeReadThrough = true)), result.defaults[specific])
        assertTrue(result.matched.getValue(specific))
    }

    @Test
    fun `default transfer uses desugared source and enables the initial flag`() {
        val specific = Declaration("specific")
        val common = Declaration("common")
        val source = Declaration("desugared common default")
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, common).copy(
                    parameters = listOf(CjmpParameterMatch(0, true, CjmpParameterDefaultState(false, source))),
                ),
                defaults = listOf(noDefault()),
            ),
        )

        assertEquals(listOf(defaultFrom(source)), result.defaults[specific])
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `first success keeps prior events and does not execute the remaining candidates`() {
        val specific = Declaration("specific")
        val rejected = Declaration("rejected common")
        val successfulCommon = Declaration("accepted common")
        val untouched = Declaration("unvisited common")
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, rejected).copy(
                    beforeSignatureDiagnostics = listOf("generic check"),
                    signatureCompatibility = mismatch(CjmpMismatchKind.PARAMETER_TYPES),
                    afterParameterDiagnostics = listOf("unreachable rejected suffix"),
                ),
                candidate(specific, successfulCommon).copy(
                    beforeSignatureDiagnostics = listOf("generic check"),
                    afterParameterDiagnostics = listOf("constructor check"),
                    beforeBindingDiagnostics = listOf("generic check"),
                ),
                candidate(specific, untouched, requiresImplementation = true).copy(
                    beforeSignatureDiagnostics = listOf("unreachable tail"),
                ),
            ),
        )

        assertEquals(
            listOf(
                diagnostic(specific, rejected, "generic check"),
                diagnostic(specific, successfulCommon, "generic check"),
                diagnostic(specific, successfulCommon, "constructor check"),
                diagnostic(specific, successfulCommon, "generic check"),
            ),
            result.events,
        )
        assertEquals(listOf(rejected, successfulCommon), result.visits.map { it.common })
        assertEquals(listOf(accepted(specific, successfulCommon)), result.acceptedBindings)
        assertEquals(mapOf(successfulCommon to specific), result.implementations)
        assertEquals(setOf(successfulCommon), result.doNotExport)
    }

    @Test
    fun `constructor shape rejection retains defaults but never reaches binding`() {
        val specific = Declaration("specific constructor")
        val common = Declaration("common constructor")
        val source = Declaration("common default")
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, common).copy(
                    parameters = listOf(parameter(0, source)),
                    afterParameterDiagnostics = listOf("constructor shape mismatch"),
                    afterParameters = mismatch(CjmpMismatchKind.CALLABLE_KIND),
                    beforeBindingDiagnostics = listOf("unreachable generic check"),
                ),
                defaults = listOf(noDefault()),
            ),
        )

        assertEquals(listOf(defaultFrom(source)), result.defaults[specific])
        assertEquals(listOf(diagnostic(specific, common, "constructor shape mismatch")), result.events)
        assertTrue(result.acceptedBindings.isEmpty())
    }

    @Test
    fun `facts preserve property child symbols while getter failure stops setter evaluation`() {
        val specific = Declaration("specific property")
        val common = Declaration("common property")
        val specificGetter = Declaration("specific getter")
        val commonGetter = Declaration("common getter")
        val previousGetter = Declaration("previous getter")
        val specificSetter = Declaration("specific setter")
        val commonSetter = Declaration("common setter")
        val seed = accepted(previousGetter, commonGetter)
        val plan = declaration(
            specific,
            candidate(specific, common).copy(
                beforeBindingDiagnostics = listOf("property type mismatch"),
                binding = CjmpBindingRule.Property(
                    request(specific, common),
                    request(specificGetter, commonGetter, requiresImplementation = true),
                    request(specificSetter, commonSetter, requiresImplementation = true),
                ),
            ),
        )
        val facts = CjmpMatchFacts.create(listOf(plan))
        val getterOwnerFacts = CjmpMatchFacts.create<Declaration, TypeParameter, String>(emptyList(), listOf(seed))
        val frozenPlan = facts.plans.single() as CjmpMatchPlan.Declaration<Declaration, TypeParameter, String>
        val frozenBinding = frozenPlan.candidates.single().binding as CjmpBindingRule.Property
        assertSame(specific, frozenBinding.property.specific)
        assertSame(common, frozenBinding.property.common)
        assertSame(specificGetter, frozenBinding.getter?.specific)
        assertSame(commonGetter, frozenBinding.getter?.common)
        assertSame(specificSetter, frozenBinding.setter?.specific)
        assertSame(commonSetter, frozenBinding.setter?.common)

        val result = CjmpMatchEvaluator.evaluateFacts(listOf(getterOwnerFacts, facts))

        assertEquals(
            listOf(diagnostic(specific, common, "property type mismatch"), occupied(specificGetter, commonGetter, previousGetter)),
            result.events,
        )
        assertEquals(listOf(seed, accepted(specific, common)), result.acceptedBindings)
        assertEquals(mapOf(commonGetter to previousGetter, common to specific), result.implementations)
        assertFalse(result.matched.getValue(specific))
        assertFalse(commonSetter in result.implementations)
        assertEquals(listOf(visit(specific, common, mismatch(CjmpMismatchKind.SECOND_BINDING))), result.visits)
        assertEquals(listOf(plan), facts.plans)
    }

    @Test
    fun `static and global variable assignments overwrite without second binding events`() {
        val common = Declaration("common variable")
        val first = Declaration("first static variable")
        val second = Declaration("second global variable")
        val result = CjmpMatchEvaluator.evaluate(
            listOf(
                declaration(
                    first,
                    candidate(first, common).copy(
                        binding = CjmpBindingRule.VariableAssignment(request(first, common), CjmpVariableAssignmentKind.STATIC_OR_GLOBAL),
                    ),
                ),
                declaration(
                    second,
                    candidate(second, common).copy(
                        beforeBindingDiagnostics = listOf("variable type mismatch", "var let mismatch"),
                        binding = CjmpBindingRule.VariableAssignment(request(second, common), CjmpVariableAssignmentKind.STATIC_OR_GLOBAL),
                    ),
                ),
            ),
        )

        assertEquals(mapOf(common to second), result.implementations)
        assertEquals(setOf(common), result.doNotExport)
        assertEquals(listOf(accepted(first, common), accepted(second, common)), result.acceptedBindings)
        assertEquals(mapOf(first to true, second to true), result.matched)
        assertEquals(
            listOf(diagnostic(second, common, "variable type mismatch"), diagnostic(second, common, "var let mismatch")),
            result.events,
        )
    }

    @Test
    fun `static mismatch assigns an implementation without suppressing common export`() {
        val specific = Declaration("instance specific with static mismatch")
        val common = Declaration("static common")
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, common).copy(
                    binding = CjmpBindingRule.VariableAssignment(request(specific, common), CjmpVariableAssignmentKind.STATIC_MISMATCH),
                ),
            ),
        )

        assertEquals(mapOf(common to specific), result.implementations)
        assertEquals(listOf(accepted(specific, common, suppressesCommonExport = false)), result.acceptedBindings)
        assertTrue(result.doNotExport.isEmpty())
        assertTrue(result.matched.getValue(specific))
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `later static mismatch does not clear a prior export suppression`() {
        val common = Declaration("common variable")
        val first = Declaration("static specific")
        val second = Declaration("instance specific")
        val result = CjmpMatchEvaluator.evaluate(
            listOf(
                declaration(
                    first,
                    candidate(first, common).copy(
                        binding = CjmpBindingRule.VariableAssignment(request(first, common), CjmpVariableAssignmentKind.STATIC_OR_GLOBAL),
                    ),
                ),
                declaration(
                    second,
                    candidate(second, common).copy(
                        binding = CjmpBindingRule.VariableAssignment(request(second, common), CjmpVariableAssignmentKind.STATIC_MISMATCH),
                    ),
                ),
            ),
        )

        assertEquals(mapOf(common to second), result.implementations)
        assertEquals(setOf(common), result.doNotExport)
        assertEquals(
            listOf(accepted(first, common), accepted(second, common, suppressesCommonExport = false)),
            result.acceptedBindings,
        )
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `facts factory defers instance variable seed requirement until ordered evaluation`() {
        val specific = Declaration("specific instance variable")
        val common = Declaration("common instance variable")
        val plan = declaration(
            specific,
            candidate(specific, common).copy(binding = CjmpBindingRule.MergedMemberVariable(request(specific, common))),
        )
        val seed = accepted(specific, common, suppressesCommonExport = false)
        val memberFacts = CjmpMatchFacts.create(listOf(plan))
        val nominalFacts = CjmpMatchFacts.create<Declaration, TypeParameter, String>(emptyList(), listOf(seed))
        val result = CjmpMatchEvaluator.evaluateFacts(listOf(memberFacts, nominalFacts))

        assertEquals(listOf(seed), result.acceptedBindings)
        assertTrue(result.doNotExport.isEmpty())
        assertTrue(result.matched.getValue(specific))
        assertTrue(result.events.isEmpty())
        assertThrows(IllegalStateException::class.java) { CjmpMatchEvaluator.evaluateFacts(listOf(memberFacts)) }
        val differentNominalFacts = CjmpMatchFacts.create<Declaration, TypeParameter, String>(
            emptyList(), listOf(accepted(Declaration("different specific"), common)),
        )
        assertThrows(IllegalStateException::class.java) {
            CjmpMatchEvaluator.evaluateFacts(listOf(memberFacts, differentNominalFacts))
        }
        assertEquals(result, CjmpMatchEvaluator.evaluateFacts(listOf(memberFacts, nominalFacts)))
    }

    @Test
    fun `a failed pattern variable does not roll back the prefix or skip later variables`() {
        val pattern = Declaration("tuple pattern")
        val first = Declaration("first binding")
        val missing = Declaration("unmatched binding")
        val last = Declaration("last binding")
        val firstCommon = Declaration("first common")
        val missingCommon = Declaration("wrong kind common")
        val lastCommon = Declaration("last common")
        val plan = CjmpMatchPlan.TuplePattern(
            pattern,
            listOf(
                CjmpPatternMatchElement.Variable(declaration(first, candidate(first, firstCommon))),
                CjmpPatternMatchElement.Variable(
                    declaration(missing, candidate(missing, missingCommon).copy(eligibility = mismatch(CjmpMismatchKind.CALLABLE_KIND))),
                ),
                CjmpPatternMatchElement.Variable(declaration(last, candidate(last, lastCommon))),
            ),
        )
        val result = CjmpMatchEvaluator.evaluate(listOf(plan))

        assertEquals(mapOf(first to true, missing to false, last to true, pattern to false), result.matched)
        assertEquals(listOf(accepted(first, firstCommon), accepted(last, lastCommon)), result.acceptedBindings)
        assertEquals(listOf(first, missing, last), result.visits.map { it.specific })
        assertEquals(mapOf(firstCommon to first, lastCommon to last), result.implementations)
    }

    @Test
    fun `facts preserve the tuple tail while non variable elements stop evaluation without reverting the prefix`() {
        val pattern = Declaration("tuple pattern")
        val first = Declaration("first binding")
        val unvisited = Declaration("after wildcard")
        val firstCommon = Declaration("first common")
        val laterCommon = Declaration("later common")
        val unvisitedPlan = declaration(
            unvisited, candidate(unvisited, laterCommon).copy(beforeBindingDiagnostics = listOf("unreachable")),
        )
        val elements = mutableListOf<CjmpPatternMatchElement<Declaration, TypeParameter, String>>(
            CjmpPatternMatchElement.Variable(declaration(first, candidate(first, firstCommon))),
            CjmpPatternMatchElement.NonVariable(),
            CjmpPatternMatchElement.Variable(unvisitedPlan),
        )
        val facts = CjmpMatchFacts.create(listOf(CjmpMatchPlan.TuplePattern(pattern, elements)))
        elements.clear()

        val frozenPlan = facts.plans.single() as CjmpMatchPlan.TuplePattern<Declaration, TypeParameter, String>
        assertTrue(frozenPlan.elements[1] is CjmpPatternMatchElement.NonVariable)
        val frozenTail = frozenPlan.elements[2] as CjmpPatternMatchElement.Variable
        assertEquals(unvisitedPlan, frozenTail.plan)
        assertSame(unvisited, frozenTail.plan.specific)
        assertThrows(UnsupportedOperationException::class.java) { (frozenPlan.elements as MutableList).clear() }

        val result = CjmpMatchEvaluator.evaluateFacts(listOf(facts))

        assertEquals(mapOf(first to true, pattern to false), result.matched)
        assertEquals(listOf(accepted(first, firstCommon)), result.acceptedBindings)
        assertEquals(listOf(visit(first, firstCommon)), result.visits)
        assertFalse(unvisited in result.defaults)
        assertFalse(laterCommon in result.implementations)
        assertTrue(result.events.isEmpty())
        assertEquals(unvisitedPlan, (frozenPlan.elements[2] as CjmpPatternMatchElement.Variable).plan)
    }

    @Test
    fun `non tuple pattern fails while an empty tuple succeeds without visiting bindings`() {
        val nonTuple = Declaration("non tuple pattern")
        val emptyTuple = Declaration("empty tuple pattern")
        val result = CjmpMatchEvaluator.evaluate(
            listOf<CjmpMatchPlan<Declaration, TypeParameter, String>>(
                CjmpMatchPlan.NonTuplePattern(nonTuple),
                CjmpMatchPlan.TuplePattern(emptyTuple, emptyList()),
            ),
        )

        assertEquals(mapOf(nonTuple to false, emptyTuple to true), result.matched)
        assertTrue(result.visits.isEmpty())
        assertTrue(result.acceptedBindings.isEmpty())
        assertTrue(result.implementations.isEmpty())
        assertTrue(result.doNotExport.isEmpty())
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `all candidates policy preserves order and continues after success`() {
        val specific = Declaration("specific")
        val commons = listOf(Declaration("parent two"), Declaration("parent one"), Declaration("parent three"))
        val plan = declaration(specific, *commons.map { candidate(specific, it) }.toTypedArray())
            .copy(traversal = CjmpCandidateTraversal.ALL_CANDIDATES)
        val result = evaluate(plan)

        assertEquals(commons.map { visit(specific, it) }, result.visits)
        assertEquals(commons.map { accepted(specific, it) }, result.acceptedBindings)
        assertEquals(commons.associateWith { specific }, result.implementations)
    }

    @Test
    fun `merged extend validation preserves every actual common owner mapping without binding again`() {
        val specific = Declaration("specific extend")
        val firstCommon = Declaration("first common extend")
        val secondCommon = Declaration("second common extend")
        val firstType = TypeParameter("first common T")
        val secondType = TypeParameter("second common T")
        val specificType = TypeParameter("specific U")
        val firstMapping = mapOf(firstType to specificType)
        val secondMapping = mapOf(secondType to specificType)
        val firstContainer = container(firstCommon, CjmpContainerKind.EXTEND, common = true, typeParameters = listOf(firstType))
        val secondContainer = container(secondCommon, CjmpContainerKind.EXTEND, common = true, typeParameters = listOf(secondType))
        val specificContainer = container(specific, CjmpContainerKind.EXTEND, specific = true, typeParameters = listOf(specificType))
        val merge = CjmpContainerMergeEvaluator.evaluateExtensions(
            listOf(secondContainer, firstContainer, specificContainer).map { CjmpExtendMergeInput(it, "same key") },
            commonOrder = listOf(firstCommon, secondCommon),
        )
        val plan = declaration(
            specific,
            candidate(specific, firstCommon).copy(
                binding = CjmpBindingRule.MergedContainer(request(specific, firstCommon, mapping = firstMapping)),
            ),
            candidate(specific, secondCommon).copy(
                binding = CjmpBindingRule.MergedContainer(request(specific, secondCommon, mapping = secondMapping)),
            ),
        ).copy(traversal = CjmpCandidateTraversal.ALL_CANDIDATES)

        val facts = CjmpMatchFacts.create(listOf(plan), merge.initialBindings)
        val result = CjmpMatchEvaluator.evaluateFacts(listOf(facts))

        assertEquals(listOf(visit(specific, firstCommon), visit(specific, secondCommon)), result.visits)
        assertEquals(
            listOf(
                accepted(specific, firstCommon).copy(typeParameterMapping = firstMapping),
                accepted(specific, secondCommon).copy(typeParameterMapping = secondMapping),
            ),
            result.acceptedBindings,
        )
        assertEquals(merge.initialBindings, result.initialBindings)
        assertEquals(merge.initialBindings, result.acceptedBindings)
        assertEquals(mapOf(firstCommon to specific, secondCommon to specific), result.implementations)
        assertEquals(mapOf(specific to true), result.matched)
        assertEquals(setOf(firstCommon, secondCommon), result.doNotExport)
        assertTrue(result.events.isEmpty())
        assertThrows(IllegalStateException::class.java) { CjmpMatchEvaluator.evaluate(listOf(plan)) }
    }

    @Test
    fun `merged nominal validation requires the exact actual container seed without binding again`() {
        val specific = Declaration("specific class")
        val common = Declaration("common class")
        val other = Declaration("other specific class")
        val commonType = TypeParameter("common T")
        val specificType = TypeParameter("specific U")
        val mapping = mapOf(commonType to specificType)
        val merge = CjmpContainerMergeEvaluator.evaluateNominals(
            listOf(
                container(common, common = true, typeParameters = listOf(commonType)),
                container(specific, specific = true, typeParameters = listOf(specificType)),
            ),
        )
        val plan = declaration(
            specific,
            candidate(specific, common).copy(binding = CjmpBindingRule.MergedContainer(request(specific, common, mapping = mapping))),
        )
        val result = evaluate(plan, seeds = merge.initialBindings)

        assertEquals(mapOf(specific to true), result.matched)
        assertEquals(merge.initialBindings, result.acceptedBindings)
        assertEquals(mapOf(common to specific), result.implementations)
        assertEquals(setOf(common), result.doNotExport)
        assertTrue(result.events.isEmpty())
        assertThrows(IllegalStateException::class.java) { evaluate(plan) }
        val wrongOwnerPlan = declaration(
            other,
            candidate(other, common).copy(binding = CjmpBindingRule.MergedContainer(request(other, common, mapping = mapping))),
        )
        assertThrows(IllegalStateException::class.java) { evaluate(wrongOwnerPlan, seeds = merge.initialBindings) }
        assertEquals(result, evaluate(plan, seeds = merge.initialBindings))
    }

    @Test
    fun `evaluation does not retain occupancy defaults or diagnostics between calls`() {
        val specific = Declaration("specific")
        val common = Declaration("common")
        val previous = Declaration("previous")
        val source = Declaration("default source")
        val plan = declaration(
            specific,
            candidate(specific, common).copy(parameters = listOf(parameter(0, source))),
            defaults = listOf(noDefault()),
        )
        val occupiedResult = evaluate(plan, seeds = listOf(accepted(previous, common)))
        val fresh = evaluate(plan)
        val repeated = evaluate(plan)

        assertFalse(occupiedResult.matched.getValue(specific))
        assertTrue(fresh.matched.getValue(specific))
        assertTrue(fresh.events.isEmpty())
        assertEquals(listOf(defaultFrom(source)), fresh.defaults[specific])
        assertEquals(listOf(noDefault()), plan.initialDefaults)
        assertEquals(fresh, repeated)
        assertEquals(mapOf(common to previous), occupiedResult.implementations)
    }

    @Test
    fun `published maps and default lists are detached from mutable input collections`() {
        val specific = Declaration("specific")
        val common = Declaration("common")
        val seededSpecific = Declaration("seeded specific")
        val seededCommon = Declaration("seeded common")
        val commonType = TypeParameter("common T")
        val specificType = TypeParameter("specific U")
        val seedType = TypeParameter("seed V")
        val mapping = linkedMapOf(commonType to specificType)
        val seedMapping = linkedMapOf(commonType to seedType)
        val initialDefaults = mutableListOf(noDefault())
        val source = Declaration("common default")
        val result = evaluate(
            declaration(
                specific,
                candidate(specific, common).copy(
                    parameters = listOf(parameter(0, source)),
                    binding = CjmpBindingRule.Callable(request(specific, common, mapping = mapping)),
                ),
                defaults = initialDefaults,
            ),
            seeds = listOf(CjmpAcceptedBinding(seededSpecific, seededCommon, seedMapping, suppressesCommonExport = false)),
        )

        mapping.clear()
        mapping[commonType] = TypeParameter("changed U")
        seedMapping.clear()
        initialDefaults[0] = defaultFrom(Declaration("changed source"))

        assertEquals(mapOf(commonType to seedType), result.acceptedBindings[0].typeParameterMapping)
        assertEquals(mapOf(commonType to specificType), result.acceptedBindings[1].typeParameterMapping)
        assertEquals(
            listOf(CjmpAcceptedBinding(seededSpecific, seededCommon, mapOf(commonType to seedType), suppressesCommonExport = false)),
            result.initialBindings,
        )
        assertEquals(mapOf(seededCommon to seededSpecific, common to specific), result.implementations)
        assertEquals(setOf(common), result.doNotExport)
        assertEquals(listOf(defaultFrom(source)), result.defaults[specific])
        assertThrows(UnsupportedOperationException::class.java) { (result.initialBindings as MutableList).clear() }
        assertThrows(UnsupportedOperationException::class.java) {
            (result.initialBindings.single().typeParameterMapping as MutableMap).clear()
        }
    }

    private fun container(
        symbol: Declaration,
        kind: CjmpContainerKind = CjmpContainerKind.CLASS,
        common: Boolean = false,
        specific: Boolean = false,
        typeParameters: List<TypeParameter> = emptyList(),
    ): CjmpMergeContainer<Declaration, TypeParameter> = CjmpMergeContainer(
        symbol = symbol,
        name = "Owner",
        kind = kind,
        isCommon = common,
        isSpecific = specific,
        hasGeneric = typeParameters.isNotEmpty(),
        typeParameters = typeParameters,
        isNonExhaustive = false,
        doNotExport = false,
        members = emptyList(),
    )

    private fun declaration(
        specific: Declaration,
        vararg candidates: CjmpCandidatePlan<Declaration, TypeParameter, String>,
        defaults: List<CjmpParameterDefaultState<Declaration>> = emptyList(),
        checkParameterDefaultValueConflict: Boolean = true,
    ): CjmpMatchPlan.Declaration<Declaration, TypeParameter, String> = CjmpMatchPlan.Declaration(
        specific,
        defaults,
        candidates.toList(),
        CjmpCandidateTraversal.FIRST_SUCCESS,
        checkParameterDefaultValueConflict,
    )

    private fun candidate(
        specific: Declaration,
        common: Declaration,
        requiresImplementation: Boolean = false,
    ): CjmpCandidatePlan<Declaration, TypeParameter, String> = CjmpCandidatePlan(
        common = common,
        eligibility = CjmpMatchResult.Matched,
        beforeSignatureDiagnostics = emptyList(),
        signatureCompatibility = CjmpMatchResult.Matched,
        parameters = emptyList(),
        afterParameterDiagnostics = emptyList(),
        afterParameters = CjmpMatchResult.Matched,
        beforeBindingDiagnostics = emptyList(),
        binding = CjmpBindingRule.Callable(request(specific, common, requiresImplementation)),
    )

    private fun request(
        specific: Declaration,
        common: Declaration,
        requiresImplementation: Boolean = false,
        mapping: Map<TypeParameter, TypeParameter> = emptyMap(),
    ): CjmpBindingRequest<Declaration, TypeParameter> = CjmpBindingRequest(specific, common, mapping, requiresImplementation)

    private fun accepted(
        specific: Declaration,
        common: Declaration,
        suppressesCommonExport: Boolean = true,
    ): CjmpAcceptedBinding<Declaration, TypeParameter> =
        CjmpAcceptedBinding(specific, common, emptyMap(), suppressesCommonExport)

    private fun parameter(index: Int, source: Declaration? = null, namesMatch: Boolean = true): CjmpParameterMatch<Declaration> =
        CjmpParameterMatch(index, namesMatch, CjmpParameterDefaultState(source != null, source))

    private fun noDefault(): CjmpParameterDefaultState<Declaration> = CjmpParameterDefaultState(false, null)

    private fun defaultFrom(source: Declaration): CjmpParameterDefaultState<Declaration> = CjmpParameterDefaultState(true, source)

    private fun mismatch(kind: CjmpMismatchKind): CjmpMatchResult = CjmpMatchResult.Mismatched(kind)

    private fun visit(specific: Declaration, common: Declaration, result: CjmpMatchResult = CjmpMatchResult.Matched): CjmpCandidateVisit<Declaration> =
        CjmpCandidateVisit(specific, common, result)

    private fun diagnostic(specific: Declaration, common: Declaration, value: String): CjmpMatchEvent<Declaration, String> =
        CjmpMatchEvent.DiagnosticData(specific, common, value)

    private fun occupied(specific: Declaration, common: Declaration, previous: Declaration): CjmpMatchEvent<Declaration, String> =
        CjmpMatchEvent.Occupied(specific, common, previous)

    private fun parameterEvent(specific: Declaration, common: Declaration, index: Int, kind: CjmpMismatchKind): CjmpMatchEvent<Declaration, String> =
        CjmpMatchEvent.ParameterMismatch(specific, common, CjmpParameterMismatch(kind, index))

    private fun evaluate(
        plan: CjmpMatchPlan.Declaration<Declaration, TypeParameter, String>,
        seeds: List<CjmpAcceptedBinding<Declaration, TypeParameter>> = emptyList(),
    ): CjmpMatchAggregate<Declaration, TypeParameter, String> = CjmpMatchEvaluator.evaluate(listOf(plan), seeds)
}
