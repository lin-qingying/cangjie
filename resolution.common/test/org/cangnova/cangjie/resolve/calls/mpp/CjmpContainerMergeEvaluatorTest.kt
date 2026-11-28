package org.cangnova.cangjie.resolve.calls.mpp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 官方 v1.1.3 CheckCJMP.cpp 的容器合并回归：173–293、309–335、387–455。
 *
 * 预期来自原始成员移动、slot 交换、逐次合并和单边 dependency 替换，不用最终签名去重，
 * 不把 nominal/extend 合并伪装成 callable first-fit。符号仅表示声明身份。
 */
class CjmpContainerMergeEvaluatorTest {
    private data class Symbol(val id: String) : CjmpDeclarationSymbolMarker
    private data class TypeParameter(val id: String) : CjmpTypeParameterSymbolMarker

    @Test
    fun `container merge preserves uncollected dependencies and common initialization rejects them`() {
        val commonField = member("common part field", instanceVariable = true, fromCommonPart = true)
        val specificField = member("specific part field", instanceVariable = true)
        val common = container("common", common = true, members = listOf(commonField))
        val specific = container("specific", specific = true, members = listOf(specificField))

        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(listOf(commonField.symbol, specificField.symbol), graph.members[specific.symbol])
        assertEquals(specific.symbol, graph.logicalOwners[commonField.symbol])
        for (field in listOf(commonField, specificField)) {
            assertSame(CjmpInitializationDependencies.NotCollected, graph.dependencies.getValue(field.symbol))
            assertSame(CjmpInitializationDependencies.NotCollected, graph.memberFacts.getValue(field.symbol).dependencies)
        }
        assertThrows(IllegalStateException::class.java) {
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(specific.symbol, graph, emptyMap())
        }
        assertEquals(listOf(seed(specific.symbol, common.symbol, suppressesExport = true)), graph.initialBindings)
    }

    @Test
    fun `late initialization facts replay the exact merge-time dependency replacements`() {
        val commonX = member("common x", "x", instanceVariable = true, common = true, commonDefault = true)
        val commonY = member("common y", "y", instanceVariable = true, common = true, fromCommonPart = true)
        val specificX = member("specific x", "x", instanceVariable = true, specific = true)
        val common = container("common", common = true, members = listOf(commonX, commonY))
        val specific = container("specific", specific = true, members = listOf(specificX))
        val structuralGraph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        val initializedGraph = structuralGraph.withInitializationDependencies(
            mapOf(commonY.symbol to collected(commonX.symbol)),
        )

        assertSame(CjmpInitializationDependencies.NotCollected, structuralGraph.dependencies.getValue(commonY.symbol))
        assertSame(CjmpInitializationDependencies.NotCollected, structuralGraph.memberFacts.getValue(commonY.symbol).dependencies)
        assertEquals(collected(specificX.symbol), initializedGraph.dependencies.getValue(commonY.symbol))
        assertEquals(
            listOf(specificX.symbol, commonY.symbol),
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(
                specific.symbol, initializedGraph, emptyMap(),
            ).map { it.symbol },
        )
    }

    @Test
    fun `collected empty common dependencies permit uncollected specific fields and non variable members`() {
        val sourceDependencies = mutableListOf<Symbol>()
        val emptyDependencies = CjmpInitializationDependencies.Collected(sourceDependencies)
        val commonField = member(
            "common part field", instanceVariable = true, dependencies = emptyDependencies, fromCommonPart = true,
        )
        val commonProperty = member("common property", fromCommonPart = true)
        val late = member("late specific field", instanceVariable = true, sourceOrder = 20)
        val early = member("early specific field", instanceVariable = true, sourceOrder = 10)
        val common = container("common", common = true, members = listOf(commonField, commonProperty))
        val specific = container("specific", specific = true, members = listOf(late, early))
        sourceDependencies += Symbol("added after collection")

        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(collected(), graph.dependencies.getValue(commonField.symbol))
        for (field in listOf(commonProperty, late, early)) {
            assertSame(CjmpInitializationDependencies.NotCollected, graph.dependencies.getValue(field.symbol))
        }
        assertEquals(
            listOf(
                CjmpVariableWithPosition(commonField.symbol, 0),
                CjmpVariableWithPosition(early.symbol, 2),
                CjmpVariableWithPosition(late.symbol, 1),
            ),
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(specific.symbol, graph, emptyMap()),
        )
        assertThrows(UnsupportedOperationException::class.java) { (emptyDependencies.declarations as MutableList).clear() }
    }

    @Test
    fun `initialization order uses replaced dependencies and preserves intervening ordinary specific fields`() {
        // cjc 1.1.3 instance-dependency-order：before=7、x=8、hidden=11、y=15、after=15。
        val commonX = member(
            "common x", "x", instanceVariable = true, common = true, commonDefault = true,
            dependencies = collected(), fromCommonPart = true,
        )
        val hidden = member("hidden", instanceVariable = true, dependencies = collected(commonX.symbol), fromCommonPart = true)
        val y = member("y", instanceVariable = true, dependencies = collected(hidden.symbol), fromCommonPart = true)
        val before = member("before", instanceVariable = true, sourceOrder = 0)
        val specificX = member("specific x", "x", instanceVariable = true, specific = true, sourceOrder = 1)
        val after = member("after", instanceVariable = true, sourceOrder = 2)
        val common = container("common", common = true, members = listOf(commonX, hidden, y))
        val specific = container("specific", specific = true, members = listOf(before, specificX, after))
        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(collected(specificX.symbol), graph.dependencies[hidden.symbol])
        val order = CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(specific.symbol, graph, emptyMap())
        assertEquals(
            listOf(
                CjmpVariableWithPosition(before.symbol, 3),
                CjmpVariableWithPosition(specificX.symbol, 0),
                CjmpVariableWithPosition(hidden.symbol, 1),
                CjmpVariableWithPosition(y.symbol, 2),
                CjmpVariableWithPosition(after.symbol, 4),
            ),
            order,
        )
        assertThrows(UnsupportedOperationException::class.java) { (order as MutableList).clear() }
        assertEquals(listOf(commonX.symbol), graph.members[common.symbol], "初始化排序不能移除保存的旧字段")
        assertEquals(listOf(specificX.symbol, hidden.symbol, y.symbol, before.symbol, after.symbol), graph.members[specific.symbol])
    }

    @Test
    fun `initialization origin is independent of COMMON and ordinary dependencies do not advance the specific sequence`() {
        val ordinarySpecific = member("ordinary specific", instanceVariable = true, sourceOrder = 10)
        val ordinaryCommon = member(
            "ordinary common", instanceVariable = true, dependencies = collected(ordinarySpecific.symbol), fromCommonPart = true,
            sourceOrder = 100,
        )
        val laterCommon = member("later common", instanceVariable = true, dependencies = collected(), fromCommonPart = true, sourceOrder = 0)
        val notFromCommon = member("source COMMON", instanceVariable = true, common = true, commonDefault = true, sourceOrder = 20)
        val common = container("common", common = true, members = listOf(ordinaryCommon, laterCommon, notFromCommon))
        val specific = container("specific", specific = true, members = listOf(ordinarySpecific))
        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(
            listOf(ordinaryCommon.symbol, laterCommon.symbol, ordinarySpecific.symbol, notFromCommon.symbol),
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(specific.symbol, graph, emptyMap()).map { it.symbol },
        )
    }

    @Test
    fun `initialization order sorts source variables and counts only instance variable slots`() {
        val late = member("late", instanceVariable = true, sourceOrder = 30)
        val callable = member("callable", specific = true)
        val early = member("early", instanceVariable = true, sourceOrder = 10)
        val staticField = member("static field")
        val owner = container("owner", members = listOf(late, callable, early, staticField))
        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(owner))

        assertEquals(
            listOf(CjmpVariableWithPosition(early.symbol, 1), CjmpVariableWithPosition(late.symbol, 0)),
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(owner.symbol, graph, emptyMap()),
        )
    }

    @Test
    fun `repeated common dependencies initialize the specific field once and preserve the stored dependency lists`() {
        val specificField = member("specific field", instanceVariable = true, specific = true, sourceOrder = 1)
        val first = member(
            "first common", instanceVariable = true, fromCommonPart = true,
            dependencies = collected(specificField.symbol, specificField.symbol),
        )
        val second = member("second common", instanceVariable = true, fromCommonPart = true, dependencies = collected(specificField.symbol))
        val owner = container("owner", members = listOf(first, second, specificField))
        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(owner))

        assertEquals(
            listOf(specificField.symbol, first.symbol, second.symbol),
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(owner.symbol, graph, emptyMap()).map { it.symbol },
        )
        assertEquals(collected(specificField.symbol, specificField.symbol), graph.dependencies[first.symbol])
    }

    @Test
    fun `initialization order requires role facts for every dependency identity`() {
        val common = member("common", instanceVariable = true, fromCommonPart = true, dependencies = collected(Symbol("unresolved")))
        val owner = container("owner", members = listOf(common))
        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(owner))

        assertThrows(NoSuchElementException::class.java) {
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(owner.symbol, graph, emptyMap())
        }
    }

    @Test
    fun `external dependencies use explicit role facts and cannot replace roles of members in the merged graph`() {
        // 官方 RecordInstanceVariableUsage 没有 current owner 过滤； imported 普通实例字段也有真实依赖身份。
        val external = Symbol("imported field")
        val common = member("common", instanceVariable = true, fromCommonPart = true, dependencies = collected(external))
        val specific = member("specific", instanceVariable = true, specific = true, sourceOrder = 10)
        val owner = container("owner", members = listOf(common, specific))
        val graph = CjmpContainerMergeEvaluator.evaluateNominals(listOf(owner))

        assertEquals(
            listOf(CjmpVariableWithPosition(common.symbol, 0), CjmpVariableWithPosition(specific.symbol, 1)),
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(owner.symbol, graph, mapOf(external to false)),
        )
        assertThrows(IllegalArgumentException::class.java) {
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(
                owner.symbol, graph, mapOf(external to false, specific.symbol to false),
            )
        }
        assertThrows(IllegalStateException::class.java) {
            // 官方 CJC_ASSERT：真实 SPECIFIC 依赖必须可在剩余的 specific 字段序列中找到。
            CjmpInitializationOrderEvaluator.getVarsInitializationOrderWithPositions(owner.symbol, graph, mapOf(external to true))
        }
    }

    @Test
    fun `shared member graph preserves detached dependencies and rejects consumer mutations`() {
        val commonField = member("common field", "value", instanceVariable = true, common = true)
        val specificField = member("specific field", "value", instanceVariable = true, specific = true)
        val sourceDependencies = mutableListOf(commonField.symbol, Symbol("external field"))
        val dependent = member("dependent", dependencies = CjmpInitializationDependencies.Collected(sourceDependencies))
        sourceDependencies.clear()
        val common = container(
            "common", common = true, members = listOf(commonField, dependent),
            typeParameters = listOf(TypeParameter("T"), TypeParameter("V")),
        )
        val specific = container(
            "specific", specific = true, members = listOf(specificField),
            typeParameters = listOf(TypeParameter("U"), TypeParameter("W")),
        )
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))
        assertEquals(collected(specificField.symbol, Symbol("external field")), result.dependencies[dependent.symbol])
        assertEquals(collected(commonField.symbol, Symbol("external field")), result.memberFacts.getValue(dependent.symbol).dependencies)

        val mutations = listOf<() -> Unit>(
            { (result.members as MutableMap).clear() },
            { (result.memberFacts as MutableMap).clear() },
            { (result.logicalOwners as MutableMap).clear() },
            { (result.dependencies as MutableMap).clear() },
            { (result.initialBindings as MutableList).clear() },
            { (result.doNotExport as MutableSet).clear() },
            { (result.commonNonExhaustive as MutableSet).clear() },
            { (result.merges as MutableList).clear() },
            { (result.events as MutableList).clear() },
        ) + result.members.values.map { members -> { (members as MutableList).clear() } } +
                result.dependencies.values.filterIsInstance<CjmpInitializationDependencies.Collected<Symbol>>()
                    .map { dependencies -> { (dependencies.declarations as MutableList).clear() } } +
                result.initialBindings.map { binding -> { (binding.typeParameterMapping as MutableMap).clear() } } +
                result.merges.flatMap { merge ->
                    listOf<() -> Unit>(
                        { (merge.typeParameterMapping as MutableMap).clear() },
                        { (merge.commonMembers as MutableList).clear() },
                    )
                }
        (mutations + result.memberFacts.values.map { it.dependencies }
            .filterIsInstance<CjmpInitializationDependencies.Collected<Symbol>>()
            .map { dependencies -> { (dependencies.declarations as MutableList).clear() } })
            .forEach { mutation -> assertThrows(UnsupportedOperationException::class.java, mutation) }
        assertEquals(listOf(specificField.symbol, dependent.symbol), result.members[specific.symbol])
        assertEquals(specificField.symbol, result.initialBindings.single { it.common == commonField.symbol }.specific)
    }

    @Test
    fun `ordinary members and same named unmarked fields remain distinct`() {
        val commonFunction = member("common function", "same", doNotExport = true)
        val commonField = member("ordinary common field", "field", instanceVariable = true)
        val specificFunction = member("specific function", "same")
        val specificField = member("ordinary specific field", "field", instanceVariable = true)
        val common = container("common", common = true, members = listOf(commonFunction, commonField))
        val specific = container("specific", specific = true, members = listOf(specificFunction, specificField))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(
            listOf(commonFunction.symbol, commonField.symbol, specificFunction.symbol, specificField.symbol),
            result.members[specific.symbol],
        )
        assertEquals(emptyList<Symbol>(), result.members[common.symbol])
        assertEquals(setOf(common.symbol), result.doNotExport)
        assertEquals(specific.symbol, result.logicalOwners[commonFunction.symbol])
        assertEquals(listOf(seed(specific.symbol, common.symbol, suppressesExport = true)), result.initialBindings)
        assertTrue(result.events.isEmpty())
        assertEquals(listOf(commonFunction, commonField), common.members, "合并不得改变原始输入")
        assertEquals(result, CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific)))
    }

    @Test
    fun `COMMON instance field is replaced by name and its old node is retained`() {
        val commonField = member("common field", "value", instanceVariable = true, common = true)
        val specificField = member("specific field", "value", instanceVariable = true, specific = true)
        val commonType = TypeParameter("T")
        val specificType = TypeParameter("U")
        val common = container("common", common = true, members = listOf(commonField), typeParameters = listOf(commonType))
        val specific = container("specific", specific = true, members = listOf(specificField), typeParameters = listOf(specificType))
        val mapping = mapOf(commonType to specificType)
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(listOf(specificField.symbol), result.members[specific.symbol])
        assertEquals(listOf(commonField.symbol), result.members[common.symbol])
        assertEquals(specific.symbol, result.logicalOwners[commonField.symbol])
        assertEquals(
            listOf(seed(specificField.symbol, commonField.symbol, mapping), seed(specific.symbol, common.symbol, mapping, true)),
            result.initialBindings,
        )
        assertFalse(commonField.symbol in result.doNotExport, "字段交换不等于把旧字段设置为不导出")
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `unmarked member parameters are deduplicated without creating a field implementation`() {
        val commonParameter = member("common member parameter", "value", instanceVariable = true, memberParameter = true)
        val specificParameter = member("specific member parameter", "value", instanceVariable = true, memberParameter = true)
        val common = container("common", common = true, members = listOf(commonParameter))
        val specific = container("specific", specific = true, members = listOf(specificParameter))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(listOf(commonParameter.symbol), result.members[specific.symbol])
        assertEquals(listOf(specificParameter.symbol), result.members[common.symbol])
        assertEquals(specific.symbol, result.logicalOwners[commonParameter.symbol])
        assertEquals(specific.symbol, result.logicalOwners[specificParameter.symbol])
        assertEquals(listOf(seed(specific.symbol, common.symbol, suppressesExport = true)), result.initialBindings)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `duplicate specific fields swap the current slot and form an implementation pointer chain`() {
        val original = member("common field", "value", instanceVariable = true, common = true)
        val first = member("first specific field", "value", instanceVariable = true, specific = true)
        val second = member("second specific field", "value", instanceVariable = true, specific = true)
        val common = container("common", common = true, members = listOf(original))
        val specific = container("specific", specific = true, members = listOf(first, second))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(listOf(second.symbol), result.members[specific.symbol])
        assertEquals(listOf(original.symbol, first.symbol), result.members[common.symbol])
        assertEquals(
            listOf(seed(first.symbol, original.symbol), seed(second.symbol, first.symbol), seed(specific.symbol, common.symbol, suppressesExport = true)),
            result.initialBindings,
        )
        assertTrue(result.events.isEmpty(), "slot 交换不调用 TrySetSpecificImpl 的占用诊断")
    }

    @Test
    fun `only unmatched COMMON instance fields without default require an implementation`() {
        val required = member("required common field", "required", instanceVariable = true, common = true)
        val defaulted = member("defaulted common field", "defaulted", instanceVariable = true, common = true, commonDefault = true)
        val ordinary = member("ordinary field", "ordinary", instanceVariable = true)
        val staticOrCallable = member("common non-instance declaration", "other", common = true)
        val missing = member("unmatched specific field", "missing", instanceVariable = true, specific = true)
        val common = container("common", common = true, members = listOf(required, defaulted, ordinary, staticOrCallable))
        val specific = container("specific", specific = true, members = listOf(missing))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(
            listOf<CjmpContainerMergeEvent<Symbol>>(
                CjmpContainerMergeEvent.UnmatchedSpecificField(missing.symbol),
                CjmpContainerMergeEvent.UnmatchedCommonField(required.symbol),
            ),
            result.events,
        )
        assertEquals(listOf(required.symbol, defaulted.symbol, ordinary.symbol, staticOrCallable.symbol, missing.symbol), result.members[specific.symbol])
    }

    @Test
    fun `nominal lookup retains the first role-bearing declaration and ignores ordinary namesakes`() {
        val ordinaryMember = member("ordinary member")
        val firstMember = member("first common member")
        val secondMember = member("second common member")
        val ordinary = container("ordinary nominal", members = listOf(ordinaryMember))
        val first = container("first common", common = true, members = listOf(firstMember))
        val later = container("later common", common = true, members = listOf(secondMember))
        val specific = container("specific", specific = true)
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(ordinary, first, later, specific))

        assertEquals(listOf(first.symbol to specific.symbol), result.merges.map { it.common to it.specific })
        assertEquals(listOf(firstMember.symbol), result.members[specific.symbol])
        assertEquals(listOf(secondMember.symbol), result.members[later.symbol])
        assertEquals(listOf(ordinaryMember.symbol), result.members[ordinary.symbol])
        assertEquals(later.symbol, result.logicalOwners[secondMember.symbol])
    }

    @Test
    fun `repeated nominal merge moves only the members still retained by the first common`() {
        val field = member("common field", "value", instanceVariable = true, common = true)
        val function = member("common function")
        val firstField = member("first specific field", "value", instanceVariable = true, specific = true)
        val secondField = member("second specific field", "value", instanceVariable = true, specific = true)
        val common = container("common", common = true, members = listOf(field, function))
        val first = container("first specific", specific = true, members = listOf(firstField))
        val second = container("second specific", specific = true, members = listOf(secondField))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, first, second))

        assertEquals(listOf(firstField.symbol, function.symbol), result.members[first.symbol])
        assertEquals(listOf(secondField.symbol), result.members[second.symbol])
        assertEquals(listOf(field.symbol), result.members[common.symbol])
        assertEquals(listOf(listOf(field.symbol, function.symbol), listOf(field.symbol)), result.merges.map { it.commonMembers })
        assertEquals(first.symbol, result.logicalOwners[function.symbol])
        assertEquals(second.symbol, result.logicalOwners[field.symbol])
        assertEquals(second.symbol, result.initialBindings.single { it.common == common.symbol }.specific)
        assertEquals(secondField.symbol, result.initialBindings.single { it.common == field.symbol }.specific)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `specific-first nominal processing prepends each later common in traversal order`() {
        val own = member("specific member")
        val firstMember = member("first common member")
        val lastMember = member("last common member")
        val specific = container("specific", specific = true, members = listOf(own))
        val first = container("first common", common = true, members = listOf(firstMember))
        val last = container("last common", common = true, members = listOf(lastMember))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(specific, first, last))

        assertEquals(listOf(first.symbol, last.symbol), result.merges.map { it.common })
        assertEquals(listOf(lastMember.symbol, firstMember.symbol, own.symbol), result.members[specific.symbol])
    }

    @Test
    fun `same-named nominal declarations follow package traversal order across common parents`() {
        val firstMember = member("first parent member")
        val secondMember = member("second parent member")
        val firstCommon = container("first parent common", name = "Box", common = true, members = listOf(firstMember))
        val secondCommon = container("second parent common", name = "Box", common = true, members = listOf(secondMember))
        val localCommon = container("local common", name = "Box", common = true)
        val specific = container("specific", name = "Box", specific = true)

        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(firstCommon, secondCommon, localCommon, specific))

        assertEquals(listOf(firstCommon.symbol), result.merges.map { it.common })
        assertEquals(listOf(firstMember.symbol), result.members[specific.symbol])
        assertEquals(listOf(firstCommon.symbol to specific.symbol), result.initialBindings.map { it.common to it.specific })
    }

    @Test
    fun `source common nominal without a parent identity participates in package matching`() {
        val commonMember = member("local common member")
        val sourceCommon = container("source common", name = "Local", common = true, members = listOf(commonMember))
        val specific = container("specific", name = "Local", specific = true)

        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(sourceCommon, specific))

        assertEquals(listOf(sourceCommon.symbol), result.merges.map { it.common })
        assertEquals(listOf(commonMember.symbol), result.members[specific.symbol])
    }

    @Test
    fun `generic existence mismatch skips merge but different nonzero parameter counts do not`() {
        val commonType = TypeParameter("T")
        val firstSpecificType = TypeParameter("U")
        val secondSpecificType = TypeParameter("V")
        val transferred = member("common member")
        val common = container("common", common = true, typeParameters = listOf(commonType), members = listOf(transferred))
        val nonGeneric = container("non generic specific", specific = true)
        val twoParameters = container("two parameter specific", specific = true, typeParameters = listOf(firstSpecificType, secondSpecificType))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, nonGeneric, twoParameters))

        assertEquals(emptyList<Symbol>(), result.members[nonGeneric.symbol])
        assertEquals(listOf(transferred.symbol), result.members[twoParameters.symbol])
        assertEquals(emptyMap<TypeParameter, TypeParameter>(), result.merges.single().typeParameterMapping)
        assertEquals(twoParameters.symbol, result.merges.single().specific)
        assertTrue(result.events.isEmpty(), "泛型数量诊断属于后续检查，不得改成合并失败")
    }

    @Test
    fun `different nominal kinds report a mismatch without moving members`() {
        val commonMember = member("class member")
        val specificMember = member("struct member")
        val common = container("common", common = true, kind = CjmpContainerKind.CLASS, members = listOf(commonMember))
        val specific = container("specific", specific = true, kind = CjmpContainerKind.STRUCT, members = listOf(specificMember))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(listOf<CjmpContainerMergeEvent<Symbol>>(CjmpContainerMergeEvent.DifferentKind(common.symbol, specific.symbol)), result.events)
        assertEquals(listOf(commonMember.symbol), result.members[common.symbol])
        assertEquals(listOf(specificMember.symbol), result.members[specific.symbol])
        assertTrue(result.merges.isEmpty())
        assertTrue(result.initialBindings.isEmpty())
        assertTrue(result.doNotExport.isEmpty())
    }

    @Test
    fun `extension merging uses explicit common order and reports later specific duplicates`() {
        val firstMember = member("common one member")
        val secondMember = member("common two member")
        val laterMember = member("late common member")
        val own = member("specific member")
        val duplicateMember = member("duplicate member")
        val first = container("common one", common = true, kind = CjmpContainerKind.EXTEND, members = listOf(firstMember))
        val second = container("common two", common = true, kind = CjmpContainerKind.EXTEND, members = listOf(secondMember))
        val specific = container("specific", specific = true, kind = CjmpContainerKind.EXTEND, members = listOf(own))
        val duplicate = container("duplicate specific", specific = true, kind = CjmpContainerKind.EXTEND, members = listOf(duplicateMember))
        val later = container("late common", common = true, kind = CjmpContainerKind.EXTEND, members = listOf(laterMember))
        val result = CjmpContainerMergeEvaluator.evaluateExtensions(
            listOf(second, first, specific, duplicate, later).map { CjmpExtendMergeInput(it, "same key") },
            commonOrder = listOf(first.symbol, second.symbol, later.symbol),
        )

        assertEquals(listOf(first.symbol, second.symbol, later.symbol), result.merges.map { it.common })
        assertEquals(listOf(laterMember.symbol, secondMember.symbol, firstMember.symbol, own.symbol), result.members[specific.symbol])
        assertEquals(listOf(duplicateMember.symbol), result.members[duplicate.symbol])
        assertEquals(
            listOf<CjmpContainerMergeEvent<Symbol>>(CjmpContainerMergeEvent.DuplicateSpecificExtend(specific.symbol, duplicate.symbol)),
            result.events,
        )
        assertFalse(result.initialBindings.any { it.specific == duplicate.symbol })
    }

    @Test
    fun `invalid extension keys and unmarked extensions do not participate in merge groups`() {
        val invalid = container("invalid common", common = true, kind = CjmpContainerKind.EXTEND, members = listOf(member("invalid member")))
        val ordinary = container("ordinary extend", kind = CjmpContainerKind.EXTEND, members = listOf(member("ordinary member")))
        val common = container("valid common", common = true, kind = CjmpContainerKind.EXTEND, members = listOf(member("valid member")))
        val specific = container("specific", specific = true, kind = CjmpContainerKind.EXTEND)
        val result = CjmpContainerMergeEvaluator.evaluateExtensions(
            listOf(
                CjmpExtendMergeInput<Symbol, TypeParameter, String>(invalid, null),
                CjmpExtendMergeInput(ordinary, "same key"),
                CjmpExtendMergeInput(common, "same key"),
                CjmpExtendMergeInput(specific, "same key"),
            ),
            commonOrder = listOf(common.symbol),
        )

        assertEquals(listOf(common.symbol), result.merges.map { it.common })
        assertEquals(invalid.members.map { it.symbol }, result.members[invalid.symbol])
        assertEquals(ordinary.members.map { it.symbol }, result.members[ordinary.symbol])
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun `common enum ellipsis propagates while specific-only ellipsis diagnoses without cancelling merge`() {
        val openCommon = container("open common", name = "Open", kind = CjmpContainerKind.ENUM, common = true, nonExhaustive = true)
        val closedSpecific = container("closed specific", name = "Open", kind = CjmpContainerKind.ENUM, specific = true)
        val closedCommon = container("closed common", name = "Closed", kind = CjmpContainerKind.ENUM, common = true)
        val openSpecific = container("open specific", name = "Closed", kind = CjmpContainerKind.ENUM, specific = true, nonExhaustive = true)
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(openCommon, closedSpecific, closedCommon, openSpecific))

        assertEquals(setOf(closedSpecific.symbol), result.commonNonExhaustive)
        assertEquals(
            listOf<CjmpContainerMergeEvent<Symbol>>(CjmpContainerMergeEvent.NonExhaustiveMismatch(closedCommon.symbol, openSpecific.symbol)),
            result.events,
        )
        assertEquals(2, result.merges.size)
        assertEquals(setOf(openCommon.symbol, closedCommon.symbol), result.doNotExport)
        assertFalse(closedSpecific.isNonExhaustive, "派生 COMMON_NON_EXHAUSTIVE 不改源码 ellipsis 快照")
    }

    @Test
    fun `dependency replacement follows one implementation edge rather than its transitive closure`() {
        val commonField = member("common field", "value", instanceVariable = true, common = true)
        val first = member("first specific field", "value", instanceVariable = true, specific = true)
        val external = Symbol("external dependency")
        val second = member("second specific field", "value", instanceVariable = true, specific = true,
            dependencies = collected(commonField.symbol, first.symbol, external))
        val dependent = member("dependent member", dependencies = collected(commonField.symbol, first.symbol, external))
        val common = container("common", common = true, members = listOf(commonField, dependent))
        val specific = container("specific", specific = true, members = listOf(first, second))
        val result = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))

        assertEquals(collected(first.symbol, second.symbol, external), result.dependencies[dependent.symbol])
        assertEquals(collected(first.symbol, second.symbol, external), result.dependencies[second.symbol])
        assertEquals(collected(commonField.symbol, first.symbol, external), dependent.dependencies)
    }

    @Test
    fun `actual container merge seeds satisfy the member variable evaluator contract`() {
        val commonType = TypeParameter("T")
        val specificType = TypeParameter("U")
        val commonField = member("common field", "value", instanceVariable = true, common = true)
        val specificField = member("specific field", "value", instanceVariable = true, specific = true)
        val common = container("common", common = true, members = listOf(commonField), typeParameters = listOf(commonType))
        val specific = container("specific", specific = true, members = listOf(specificField), typeParameters = listOf(specificType))
        val merge = CjmpContainerMergeEvaluator.evaluateNominals(listOf(common, specific))
        val request = CjmpBindingRequest(specificField.symbol, commonField.symbol, mapOf(commonType to specificType), false)
        val plan = CjmpMatchPlan.Declaration<Symbol, TypeParameter, String>(
            specific = specificField.symbol,
            initialDefaults = emptyList(),
            candidates = listOf(
                CjmpCandidatePlan(
                    common = commonField.symbol,
                    eligibility = CjmpMatchResult.Matched,
                    beforeSignatureDiagnostics = emptyList(),
                    signatureCompatibility = CjmpMatchResult.Matched,
                    parameters = emptyList(),
                    afterParameterDiagnostics = emptyList(),
                    afterParameters = CjmpMatchResult.Matched,
                    beforeBindingDiagnostics = emptyList(),
                    binding = CjmpBindingRule.MergedMemberVariable(request),
                ),
            ),
            traversal = CjmpCandidateTraversal.FIRST_SUCCESS,
            checkParameterDefaultValueConflict = true,
        )
        val result = CjmpMatchEvaluator.evaluate(listOf(plan), merge.initialBindings)

        assertEquals(mapOf(specificField.symbol to true), result.matched)
        assertEquals(merge.initialBindings, result.acceptedBindings, "字段检查必须复用真实 seed，不新增绑定")
        assertEquals(specificField.symbol, result.implementations[commonField.symbol])
        assertEquals(specific.symbol, result.implementations[common.symbol])
        assertEquals(setOf(common.symbol), result.doNotExport)
        assertTrue(result.events.isEmpty())
    }

    private fun collected(vararg declarations: Symbol): CjmpInitializationDependencies.Collected<Symbol> =
        CjmpInitializationDependencies.Collected(declarations.asList())

    private fun member(
        id: String,
        name: String = id,
        instanceVariable: Boolean = false,
        memberParameter: Boolean = false,
        common: Boolean = false,
        specific: Boolean = false,
        commonDefault: Boolean = false,
        doNotExport: Boolean = false,
        dependencies: CjmpInitializationDependencies<Symbol> = CjmpInitializationDependencies.NotCollected,
        fromCommonPart: Boolean = false,
        sourceOrder: Int = 0,
    ): CjmpMergeMember<Symbol> = CjmpMergeMember(
        Symbol(id), name, instanceVariable, memberParameter, common, specific, commonDefault, doNotExport, dependencies,
        fromCommonPart, sourceOrder,
    )

    private fun container(
        id: String,
        name: String = "Owner",
        kind: CjmpContainerKind = CjmpContainerKind.CLASS,
        common: Boolean = false,
        specific: Boolean = false,
        typeParameters: List<TypeParameter> = emptyList(),
        nonExhaustive: Boolean = false,
        members: List<CjmpMergeMember<Symbol>> = emptyList(),
    ): CjmpMergeContainer<Symbol, TypeParameter> = CjmpMergeContainer(
        symbol = Symbol(id),
        name = name,
        kind = kind,
        isCommon = common,
        isSpecific = specific,
        hasGeneric = typeParameters.isNotEmpty(),
        typeParameters = typeParameters,
        isNonExhaustive = nonExhaustive,
        doNotExport = false,
        members = members,
    )

    private fun seed(
        specific: Symbol,
        common: Symbol,
        mapping: Map<TypeParameter, TypeParameter> = emptyMap(),
        suppressesExport: Boolean = false,
    ): CjmpAcceptedBinding<Symbol, TypeParameter> = CjmpAcceptedBinding(specific, common, mapping, suppressesExport)
}
