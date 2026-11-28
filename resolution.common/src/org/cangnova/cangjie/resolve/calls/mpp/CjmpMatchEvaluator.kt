package org.cangnova.cangjie.resolve.calls.mpp

import java.util.Collections

/**
 * 对完整的有序声明族进行纯求值，不解析类型、不写 CFIR，也不直接报告诊断。
 *
 * 官方 CheckCJMP.cpp 的 first-fit、默认值转移和部分绑定通过一次调用内的局部状态表达。
 * 候选失败不撤销此前动作；返回的快照由默认参数/实际化视图与 checker 共同消费。
 */
object CjmpMatchEvaluator {
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic> evaluate(
        plans: List<CjmpMatchPlan<D, T, Diagnostic>>,
        initialBindings: List<CjmpAcceptedBinding<D, T>> = emptyList(),
    ): CjmpMatchAggregate<D, T, Diagnostic> {
        val frozenPlans = Collections.unmodifiableList(plans.map { it.frozenCopy() })
        val frozenBindings = Collections.unmodifiableList(initialBindings.map { binding ->
            binding.copy(typeParameterMapping = Collections.unmodifiableMap(LinkedHashMap(binding.typeParameterMapping)))
        })
        return Evaluation<D, T, Diagnostic>().run(frozenPlans, frozenBindings)
    }

    /**
     * 按调用方给定的规范顺序重放多个阶段根的冻结输入。
     *
     * facts 的顺序决定共享 common 占用、默认值转移和最终实现；本方法不排序，也不修改根属性。
     */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic> evaluateFacts(
        facts: List<CjmpMatchFacts<D, T, Diagnostic>>,
    ): CjmpMatchAggregate<D, T, Diagnostic> = evaluate(
        plans = facts.flatMap { it.plans },
        initialBindings = facts.flatMap { it.initialBindings },
    )

    /** 每次调用独占的可变执行状态，绝不作为 session 或声明属性发布。 */
    private class Evaluation<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Diagnostic> {
        private val evaluated = hashSetOf<D>()
        private val matched = linkedMapOf<D, Boolean>()
        private val implementations = linkedMapOf<D, D>()
        private val doNotExport = linkedSetOf<D>()
        private val defaults = linkedMapOf<D, MutableList<CjmpParameterDefaultState<D>>>()
        private val acceptedBindings = mutableListOf<CjmpAcceptedBinding<D, T>>()
        private val visits = mutableListOf<CjmpCandidateVisit<D>>()
        private val events = mutableListOf<CjmpMatchEvent<D, Diagnostic>>()

        fun run(
            plans: List<CjmpMatchPlan<D, T, Diagnostic>>,
            initialBindings: List<CjmpAcceptedBinding<D, T>>,
        ): CjmpMatchAggregate<D, T, Diagnostic> {
            for (binding in initialBindings) {
                require(binding.common !in implementations) { "The merge seed contains duplicate common bindings" }
                implementations[binding.common] = binding.specific
                if (binding.suppressesCommonExport) doNotExport += binding.common
                acceptedBindings += binding.copy(typeParameterMapping = Collections.unmodifiableMap(binding.typeParameterMapping.toMap()))
            }
            plans.forEach(::evaluatePlan)
            return CjmpMatchAggregate(
                plans = Collections.unmodifiableList(plans.toList()),
                initialBindings = Collections.unmodifiableList(initialBindings.toList()),
                matched = Collections.unmodifiableMap(matched.toMap()),
                implementations = Collections.unmodifiableMap(implementations.toMap()),
                doNotExport = Collections.unmodifiableSet(doNotExport.toSet()),
                defaults = Collections.unmodifiableMap(defaults.mapValues { (_, values) -> Collections.unmodifiableList(values.toList()) }),
                acceptedBindings = Collections.unmodifiableList(acceptedBindings.toList()),
                visits = Collections.unmodifiableList(visits.toList()),
                events = Collections.unmodifiableList(events.toList()),
            )
        }

        private fun evaluatePlan(plan: CjmpMatchPlan<D, T, Diagnostic>): Boolean {
            require(evaluated.add(plan.specific)) { "A specific declaration must occur once in the ordered input" }
            val result = when (plan) {
                is CjmpMatchPlan.Declaration -> evaluateDeclaration(plan)
                is CjmpMatchPlan.TuplePattern -> evaluatePattern(plan)
                is CjmpMatchPlan.NonTuplePattern -> false
            }
            matched[plan.specific] = result
            return result
        }

        private fun evaluatePattern(plan: CjmpMatchPlan.TuplePattern<D, T, Diagnostic>): Boolean {
            var matchedAll = true
            for (element in plan.elements) {
                when (element) {
                    is CjmpPatternMatchElement.Variable -> {
                        if (!evaluatePlan(element.plan)) matchedAll = false
                    }
                    is CjmpPatternMatchElement.NonVariable -> return false
                }
            }
            return matchedAll
        }

        private fun evaluateDeclaration(plan: CjmpMatchPlan.Declaration<D, T, Diagnostic>): Boolean {
            val parameterDefaults = plan.initialDefaults.toMutableList()
            defaults[plan.specific] = parameterDefaults
            var anyMatched = false
            for (candidate in plan.candidates) {
                val result = evaluateCandidate(
                    plan.specific,
                    parameterDefaults,
                    candidate,
                    plan.checkParameterDefaultValueConflict,
                )
                visits += CjmpCandidateVisit(plan.specific, candidate.common, result)
                if (result == CjmpMatchResult.Matched) {
                    anyMatched = true
                    if (plan.traversal == CjmpCandidateTraversal.FIRST_SUCCESS) break
                }
            }
            return anyMatched
        }

        private fun evaluateCandidate(
            specific: D,
            parameterDefaults: MutableList<CjmpParameterDefaultState<D>>,
            candidate: CjmpCandidatePlan<D, T, Diagnostic>,
            checkParameterDefaultValueConflict: Boolean,
        ): CjmpMatchResult {
            if (candidate.eligibility != CjmpMatchResult.Matched) return candidate.eligibility
            emitDiagnostics(specific, candidate.common, candidate.beforeSignatureDiagnostics)
            val signature = requireNotNull(candidate.signatureCompatibility) { "An eligible candidate must have signature facts" }
            if (signature != CjmpMatchResult.Matched) return signature

            require(candidate.parameters.size == parameterDefaults.size) {
                "A compatible signature must provide facts for every parameter"
            }
            for ((index, parameter) in candidate.parameters.withIndex()) {
                require(parameter.index == index) { "Parameter facts must remain in declaration order" }
                if (!parameter.namesMatch) {
                    return parameterMismatch(specific, candidate.common, index, CjmpMismatchKind.PARAMETER_NAMES)
                }
                val current = parameterDefaults[index]
                val common = parameter.commonDefault
                if (checkParameterDefaultValueConflict && current.hasInitial && common.hasInitial) {
                    return parameterMismatch(
                        specific, candidate.common, index, CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES,
                    )
                }
                if (common.canBeReadThrough && current.desugaredDefaultSource == null) {
                    parameterDefaults[index] = CjmpParameterDefaultState(
                        hasInitial = true,
                        desugaredDefaultSource = common.desugaredDefaultSource,
                        canBeReadThrough = true,
                    )
                }
            }

            emitDiagnostics(specific, candidate.common, candidate.afterParameterDiagnostics)
            if (candidate.afterParameters != CjmpMatchResult.Matched) return candidate.afterParameters
            emitDiagnostics(specific, candidate.common, candidate.beforeBindingDiagnostics)
            return when (val binding = candidate.binding) {
                is CjmpBindingRule.Callable -> {
                    requireRootBinding(specific, candidate.common, binding.request)
                    tryBind(binding.request)
                }
                is CjmpBindingRule.Property -> {
                    requireRootBinding(specific, candidate.common, binding.property)
                    bindProperty(binding)
                }
                is CjmpBindingRule.VariableAssignment -> {
                    requireRootBinding(specific, candidate.common, binding.request)
                    accept(binding.request, binding.kind == CjmpVariableAssignmentKind.STATIC_OR_GLOBAL)
                    CjmpMatchResult.Matched
                }
                is CjmpBindingRule.MergedMemberVariable -> {
                    requireRootBinding(specific, candidate.common, binding.request)
                    check(implementations[binding.request.common] == binding.request.specific) {
                        "An instance variable must have its binding established by nominal merge"
                    }
                    CjmpMatchResult.Matched
                }
                is CjmpBindingRule.MergedContainer -> {
                    requireRootBinding(specific, candidate.common, binding.request)
                    check(implementations[binding.request.common] == binding.request.specific) {
                        "A nominal or extend counterpart must have its binding established by container merge"
                    }
                    CjmpMatchResult.Matched
                }
            }
        }

        private fun requireRootBinding(specific: D, common: D, request: CjmpBindingRequest<D, T>) {
            require(request.specific == specific && request.common == common) {
                "The root binding must belong to the current candidate"
            }
        }

        /** property 本体、getter、setter 顺序绑定；某一步失败不会撤销已接受的前缀。 */
        private fun bindProperty(binding: CjmpBindingRule.Property<D, T>): CjmpMatchResult {
            val propertyResult = tryBind(binding.property)
            if (propertyResult != CjmpMatchResult.Matched) return propertyResult
            binding.getter?.let { getter ->
                val getterResult = tryBind(getter)
                if (getterResult != CjmpMatchResult.Matched) return getterResult
            }
            binding.setter?.let { setter -> return tryBind(setter) }
            return CjmpMatchResult.Matched
        }

        /** 官方 TrySetSpecificImpl：占用检查严格早于实现体要求。 */
        private fun tryBind(request: CjmpBindingRequest<D, T>): CjmpMatchResult {
            val previous = implementations[request.common]
            if (previous != null) {
                events += CjmpMatchEvent.Occupied(request.specific, request.common, previous)
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.SECOND_BINDING)
            }
            if (request.requiresImplementation) {
                events += CjmpMatchEvent.MissingImplementation(request.specific, request.common)
                return CjmpMatchResult.Mismatched(CjmpMismatchKind.MISSING_BODY)
            }
            accept(request, suppressesCommonExport = true)
            return CjmpMatchResult.Matched
        }

        private fun accept(request: CjmpBindingRequest<D, T>, suppressesCommonExport: Boolean) {
            implementations[request.common] = request.specific
            if (suppressesCommonExport) doNotExport += request.common
            acceptedBindings += CjmpAcceptedBinding(
                request.specific, request.common, Collections.unmodifiableMap(request.typeParameterMapping.toMap()), suppressesCommonExport,
            )
        }

        private fun parameterMismatch(specific: D, common: D, index: Int, kind: CjmpMismatchKind): CjmpMatchResult {
            val mismatch = CjmpParameterMismatch(kind, index)
            events += CjmpMatchEvent.ParameterMismatch(specific, common, mismatch)
            return CjmpMatchResult.ParameterMismatched(mismatch)
        }

        private fun emitDiagnostics(specific: D, common: D, diagnostics: List<Diagnostic>) {
            for (diagnostic in diagnostics) {
                events += CjmpMatchEvent.DiagnosticData(specific, common, diagnostic)
            }
        }
    }
}
