package org.cangnova.cangjie.resolve.calls.mpp

import java.util.Collections

/**
 * 官方 MergeCJMPNominalsExceptExtension / MergeCJMPExtensions / MergeCommonIntoSpecific 的纯求值。
 * 只修改一次调用内的成员清单、逻辑归属和实现关系；不写入 CFIR，也不解析函数体。
 */
object CjmpContainerMergeEvaluator {
    /** 保持输入遍历顺序及每个 identifier 首个带角色声明，不把 nominal 当作 callable 的 first-fit。 */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker> evaluateNominals(
        containers: List<CjmpMergeContainer<D, T>>,
    ): CjmpContainerMergeResult<D, T> {
        require(containers.none { it.kind == CjmpContainerKind.EXTEND })
        val evaluation = Evaluation(containers)
        val firstByName = linkedMapOf<String, CjmpMergeContainer<D, T>>()
        for (container in containers) {
            val first = firstByName[container.name]
            if (first == null) {
                if (container.isCommon || container.isSpecific) firstByName[container.name] = container
                continue
            }
            // 官方只在此排除 generic/non-generic；N 与 M 均非零时仍合并，由后续泛型检查报告数量差异。
            if (container.hasGeneric != first.hasGeneric) continue
            when {
                container.isSpecific && first.isCommon -> evaluation.merge(first.symbol, container.symbol)
                container.isCommon && first.isSpecific -> evaluation.merge(container.symbol, first.symbol)
            }
        }
        return evaluation.result()
    }

    /**
     * 保留 extend 的输入顺序，以及前端显式给出的 common 集合顺序。
     * 官方输入是 unordered_set，common 组是指针 set；这里不假定它们按文件名排序。
     */
    fun <D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, K : Any> evaluateExtensions(
        inputs: List<CjmpExtendMergeInput<D, T, K>>,
        commonOrder: List<D>,
    ): CjmpContainerMergeResult<D, T> {
        require(inputs.all { it.container.kind == CjmpContainerKind.EXTEND })
        val expectedCommon = inputs.filter { it.key != null && it.container.isCommon }.map { it.container.symbol }.toSet()
        require(commonOrder.size == expectedCommon.size && commonOrder.toSet() == expectedCommon) {
            "The common extend order must cover every eligible common exactly once"
        }
        val ranks = commonOrder.withIndex().associate { it.value to it.index }
        val evaluation = Evaluation(inputs.map { it.container })
        val specificByKey = linkedMapOf<K, D>()
        val commonByKey = linkedMapOf<K, MutableList<D>>()
        for ((container, key) in inputs) {
            if (key == null || !container.isCommon && !container.isSpecific) continue
            if (container.isCommon) {
                specificByKey[key]?.let { evaluation.merge(container.symbol, it) }
                commonByKey.getOrPut(key) { mutableListOf() }.add(container.symbol)
                continue
            }
            val firstSpecific = specificByKey[key]
            if (firstSpecific != null) {
                evaluation.duplicateExtend(firstSpecific, container.symbol)
                continue
            }
            for (common in commonByKey[key].orEmpty().sortedBy { ranks.getValue(it) }) {
                evaluation.merge(common, container.symbol)
            }
            specificByKey[key] = container.symbol
        }
        return evaluation.result()
    }

    private class Evaluation<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
        snapshots: List<CjmpMergeContainer<D, T>>,
    ) {
        private val containers = snapshots.associateBy { it.symbol }
        private val memberFacts = linkedMapOf<D, CjmpMergeMember<D>>()
        private val sourceOwners = linkedMapOf<D, D>()
        private val members = linkedMapOf<D, List<D>>()
        private val logicalOwners = linkedMapOf<D, D>()
        private val dependencies = linkedMapOf<D, CjmpInitializationDependencies<D>>()
        private val dependencyMergeSteps = mutableListOf<CjmpInitializationDependencyMergeStep<D>>()
        private val bindings = linkedMapOf<D, CjmpAcceptedBinding<D, T>>()
        private val doNotExport = linkedSetOf<D>()
        private val commonNonExhaustive = linkedSetOf<D>()
        private val merges = mutableListOf<CjmpContainerMerge<D, T>>()
        private val events = mutableListOf<CjmpContainerMergeEvent<D>>()

        init {
            require(containers.size == snapshots.size) { "A container must occur once in the input" }
            for (container in snapshots) {
                if (container.doNotExport) doNotExport += container.symbol
                members[container.symbol] = container.members.map { it.symbol }
                for (member in container.members) {
                    require(memberFacts.put(member.symbol, member) == null) { "A member must have one physical owner" }
                    sourceOwners[member.symbol] = container.symbol
                    logicalOwners[member.symbol] = container.symbol
                    dependencies[member.symbol] = member.dependencies
                    if (member.doNotExport) doNotExport += member.symbol
                }
            }
        }

        fun duplicateExtend(first: D, duplicate: D) {
            events += CjmpContainerMergeEvent.DuplicateSpecificExtend(first, duplicate)
        }

        fun merge(commonSymbol: D, specificSymbol: D) {
            val common = containers.getValue(commonSymbol)
            val specific = containers.getValue(specificSymbol)
            require(common.isCommon && specific.isSpecific && commonSymbol != specificSymbol)
            if (common.kind != specific.kind) {
                events += CjmpContainerMergeEvent.DifferentKind(commonSymbol, specificSymbol)
                return
            }
            if (common.kind == CjmpContainerKind.ENUM) {
                if (common.isNonExhaustive) {
                    commonNonExhaustive += specificSymbol
                } else if (specific.isNonExhaustive) {
                    events += CjmpContainerMergeEvent.NonExhaustiveMismatch(commonSymbol, specificSymbol)
                }
            }

            val commonMembers = members.getValue(commonSymbol)
            val specificMembers = members.getValue(specificSymbol)
            val merged = commonMembers.toMutableList()
            val retained = mutableListOf<D>()
            val commonVariables = linkedMapOf<String, Int>()
            val ordinaryMemberParameters = linkedSetOf<String>()
            for ((index, symbol) in commonMembers.withIndex()) {
                val member = memberFacts.getValue(symbol)
                logicalOwners[symbol] = specificSymbol
                doNotExport -= symbol
                if (!member.isInstanceVariable) continue
                if (member.isCommon) {
                    commonVariables.putIfAbsent(member.name, index)
                } else if (member.isMemberParameter) {
                    ordinaryMemberParameters += member.name
                }
            }

            for (symbol in specificMembers) {
                val member = memberFacts.getValue(symbol)
                if (member.isInstanceVariable) {
                    if (member.isSpecific) {
                        val commonIndex = commonVariables[member.name]
                        if (commonIndex != null) {
                            // 必须绑定当前 slot 中的节点再交换；重复 specific 字段会形成实际的指针链。
                            val displaced = merged[commonIndex]
                            bind(displaced, symbol, typeParameterMapping(sourceOwners.getValue(displaced), specificSymbol))
                            merged[commonIndex] = symbol
                            retained += displaced
                            continue
                        }
                        events += CjmpContainerMergeEvent.UnmatchedSpecificField(symbol)
                    } else if (member.isMemberParameter && member.name in ordinaryMemberParameters) {
                        // 未标记成员参数只去重，没有实现关系；原 specific 节点仍保留在 common 清单中。
                        retained += symbol
                        continue
                    }
                }
                merged += symbol
            }

            members[specificSymbol] = merged.toList()
            members[commonSymbol] = retained.toList()
            val implementationsBeforeMerge = bindings.mapValues { (_, binding) -> binding.specific }
            for (symbol in merged) {
                // 官方 UpdateVarDependencies 每次合并只替换一条已存在 implementation 边，不求传递闭包。
                dependencies[symbol] = dependencies.getValue(symbol).mapDeclarations { bindings[it]?.specific ?: it }
                val member = memberFacts.getValue(symbol)
                if (member.isInstanceVariable && member.isCommon && !member.hasCommonDefault && symbol !in bindings) {
                    events += CjmpContainerMergeEvent.UnmatchedCommonField(symbol)
                }
            }
            dependencyMergeSteps += CjmpInitializationDependencyMergeStep(
                members = merged.toList(),
                implementations = implementationsBeforeMerge.toMap(),
            )
            doNotExport += commonSymbol
            val mapping = typeParameterMapping(commonSymbol, specificSymbol)
            bind(commonSymbol, specificSymbol, mapping)
            merges += CjmpContainerMerge(commonSymbol, specificSymbol, mapping, commonMembers.toList())
        }

        private fun typeParameterMapping(common: D, specific: D): Map<T, T> {
            val commonParameters = containers.getValue(common).typeParameters
            val specificParameters = containers.getValue(specific).typeParameters
            // 元数不等的 nominal 仍然配对，但不能把共同前缀伪造成兼容的位置映射。
            if (commonParameters.size != specificParameters.size) return emptyMap()
            return commonParameters.zip(specificParameters).toMap()
        }

        private fun bind(common: D, specific: D, mapping: Map<T, T>) {
            bindings[common] = CjmpAcceptedBinding(specific, common, mapping.toMap(), suppressesCommonExport = false)
        }

        fun result(): CjmpContainerMergeResult<D, T> = CjmpContainerMergeResult(
            // 合并成员图、seed 与依赖边作为一个结果发布，所有集合层级都禁止后续写入。
            members = Collections.unmodifiableMap(members.mapValues { (_, value) -> Collections.unmodifiableList(value.toList()) }),
            memberFacts = Collections.unmodifiableMap(memberFacts.toMap()),
            logicalOwners = Collections.unmodifiableMap(logicalOwners.toMap()),
            dependencies = Collections.unmodifiableMap(dependencies.toMap()),
            initialBindings = Collections.unmodifiableList(bindings.map { (common, binding) ->
                binding.copy(
                    typeParameterMapping = Collections.unmodifiableMap(binding.typeParameterMapping.toMap()),
                    suppressesCommonExport = common in doNotExport,
                )
            }),
            doNotExport = Collections.unmodifiableSet(doNotExport.toSet()),
            commonNonExhaustive = Collections.unmodifiableSet(commonNonExhaustive.toSet()),
            merges = Collections.unmodifiableList(merges.map { merge ->
                merge.copy(
                    typeParameterMapping = Collections.unmodifiableMap(merge.typeParameterMapping.toMap()),
                    commonMembers = Collections.unmodifiableList(merge.commonMembers.toList()),
                )
            }),
            events = Collections.unmodifiableList(events.toList()),
            dependencyMergeSteps = Collections.unmodifiableList(dependencyMergeSteps.map { step ->
                step.copy(
                    members = Collections.unmodifiableList(step.members.toList()),
                    implementations = Collections.unmodifiableMap(step.implementations.toMap()),
                )
            }),
        )
    }
}
