@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildCodeFragment
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignmentCopy
import org.cangnova.cangjie.cfir.expressions.builder.buildAugmentedAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.dfa.CfirLocalVariableAssignmentAnalyzer
import org.cangnova.cangjie.cfir.resolve.dfa.DfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.dfa.createDfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCodeFragmentSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.IdentityHashMap

/**
 * mini 的静态赋值事件身份测试：只经公开赋值门面、词法作用域和来源协议观察。
 * 构造的是内部 typed CFIR，不是 cjc/CJO 或源程序运行测试；不证明 tuple 完成值 transfer。
 * N14276740 的 tuple 槽位合同与 K3c7a5041 的 assignedLater/scope 合同见本轮 facts。
 */
class CfirAssignmentOccurrenceLifecycleTest {
    @Test
    fun `one report fills both repeated tuple paths without allowing an incompatible smart cast`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        // 合法类型：var x: Int64；(x, x) = (1, 2)。两叶是两个事件。
        val assignment = fixture.assignment(
            fixture.tuple(fixture.access(fixture.x), fixture.access(fixture.x)),
            fixture.tuple(fixture.int(1), fixture.int(2)),
        )
        fixture.analyze(listOf(before, assignment)) { state ->
            state.mini.enterFunction(before)
            state.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            state.analyzer.recordAssignment(assignment)
            state.assertStable(fixture.x, ConePrimitiveType.INT64)
            state.assertUnstable(fixture.x, ConePrimitiveType.BOOLEAN)
            assertTrue(state.mini.isUnstableInCurrentScope(fixture.x, null, fixture.session))
            state.mini.exitFunction()
        }
    }

    @Test
    fun `nested repeated targets use full paths and the nonliteral rhs leaf types`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        val value = fixture.variable("value", fixture.nestedValue(), mutable = false)
        val assignment = fixture.assignment(fixture.nestedTargets(), fixture.access(value))
        assertFalse(assignment.rValue is CfirTupleLiteral)
        fixture.analyze(listOf(value, before, assignment)) { state ->
            state.mini.enterFunction(before)
            state.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            state.assertUnstable(fixture.y, ConePrimitiveType.BOOLEAN)
            state.analyzer.recordAssignment(assignment)
            state.assertStable(fixture.x, ConePrimitiveType.INT64)
            state.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            state.assertUnstable(fixture.x, ConePrimitiveType.BOOLEAN)
            state.assertUnstable(fixture.y, ConePrimitiveType.INT64)
            val source = sourceFacts(state.mini)
            assertTrue(source.elements.any { it === assignment })
            assertEquals(setOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN), source.types.toSet())
            state.mini.exitFunction()
        }
    }

    @Test
    fun `reverse tuple reports update only the exact owner visible after the earlier write`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        val between = fixture.observer("between")
        val earlier = fixture.assignment(fixture.nestedTargets(), fixture.nestedValue())
        val later = fixture.assignment(fixture.nestedTargets(), fixture.nestedValue())
        fixture.analyze(listOf(before, earlier, between, later)) { state ->
            state.mini.enterFunction(between)
            state.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            state.assertUnstable(fixture.y, ConePrimitiveType.BOOLEAN)
            state.analyzer.recordAssignment(later)
            state.assertStable(fixture.x, ConePrimitiveType.INT64)
            state.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            state.mini.exitFunction()

            state.mini.enterFunction(before)
            state.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            state.assertUnstable(fixture.y, ConePrimitiveType.BOOLEAN)
            state.analyzer.recordAssignment(earlier)
            state.assertStable(fixture.x, ConePrimitiveType.INT64)
            state.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            state.mini.exitFunction()
        }
    }

    @Test
    fun `repeating a single assignment report does not fill another event with the same source`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        val between = fixture.observer("between")
        val earlier = fixture.assignment(fixture.access(fixture.x), fixture.int(1))
        // 故意保留同一 source 和 lhs/rhs；只有 operation owner 身份不同。
        val later = buildAssignmentCopy(earlier) {}
        assertNotSame(earlier, later)
        assertSame(earlier.source, later.source)
        fixture.analyze(listOf(before, earlier, between, later)) { state ->
            state.mini.enterFunction(between)
            state.analyzer.recordAssignment(later, fixture.x)
            state.assertStable(fixture.x, ConePrimitiveType.INT64)
            state.mini.exitFunction()
            state.mini.enterFunction(before)
            state.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            state.analyzer.recordAssignment(later)
            state.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            state.analyzer.recordAssignment(earlier)
            state.assertStable(fixture.x, ConePrimitiveType.INT64)
            state.mini.exitFunction()
        }
    }

    @Test
    fun `snapshot reports reach active scopes and cached forks but leave original and peer untouched`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        val between = fixture.observer("between")
        val earlier = fixture.assignment(fixture.nestedTargets(), fixture.nestedValue())
        val later = fixture.assignment(fixture.nestedTargets(), fixture.nestedValue())
        fixture.analyze(listOf(before, earlier, between, later)) { original ->
            original.mini.enterFunction(before)
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            val copy = fixture.state(original.context.createSnapshot(IdentityMapper, original.analyzer.currentSmartCastPosition).context)
            val peer = fixture.state(original.context.createSnapshot(IdentityMapper, original.analyzer.currentSmartCastPosition).context)
            assertNotSame(original.mini, copy.mini)
            copy.analyzer.recordAssignment(later)
            copy.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            copy.analyzer.recordAssignment(earlier)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            peer.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            assertTrue(sourceFacts(original.mini).types.isEmpty())
            assertTrue(sourceFacts(peer.mini).types.isEmpty())

            // 离开复制时的 scope 后重新进入另一个 cached Fork，仍须看到同一写记录的类型。
            copy.mini.exitFunction()
            copy.mini.enterFunction(between)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            copy.mini.exitFunction()
            peer.mini.exitFunction()
            peer.mini.enterFunction(between)
            peer.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            peer.mini.exitFunction()
            original.mini.exitFunction()
        }
    }

    @Test
    fun `snapshot reports also update assignedInside and the outer prohibition scope`() {
        val fixture = Fixture()
        val assignment = fixture.assignment(fixture.nestedTargets(), fixture.nestedValue())
        val writer = fixture.observer("writer", listOf(assignment))
        val after = fixture.observer("after")
        fixture.analyze(listOf(writer, after)) { original ->
            assertEquals(setOf(fixture.x.symbol, fixture.y.symbol), original.mini.enterFunction(writer))
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            val copy = fixture.state(original.context.createSnapshot(IdentityMapper, original.analyzer.currentSmartCastPosition).context)
            copy.analyzer.recordAssignment(assignment)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            copy.mini.exitFunction()
            // writer.assignedInside 已传播到外层 scope；必须与刚更新的索引共享写记录。
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.mini.enterFunction(after)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            copy.mini.exitFunction()
            copy.mini.enterFunction(writer)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.mini.exitFunction()
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            original.mini.exitFunction()
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            assertTrue(sourceFacts(original.mini).types.isEmpty())
        }
    }

    @Test
    fun `mapped snapshot preserves exact owners leaf sources and subsequent explicit rebind`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        val assignment = fixture.assignment(fixture.nestedTargets(), fixture.nestedValue())
        val mapped = buildAssignmentCopy(assignment) {}
        val replacement = buildAssignmentCopy(mapped) {}
        // foreign 保留同一 source，但既不在 mapper 域内，也不是有效树中的已登记写事件。
        val foreign = buildAssignmentCopy(assignment) {}
        assertNotSame(foreign, assignment)
        assertNotSame(foreign, mapped)
        assertSame(foreign.source, assignment.source)
        fixture.analyze(listOf(before, assignment)) { original ->
            original.mini.enterFunction(before)
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            val mapper = OwnerMapper(assignment to mapped)
            val copy = fixture.state(original.context.createSnapshot(mapper, original.analyzer.currentSmartCastPosition).context)
            val pending = sourceFacts(copy.mini)
            assertTrue(pending.elements.any { it === mapped })
            assertFalse(pending.elements.any { it === assignment })
            assertTrue(pending.types.isEmpty())
            copy.analyzer.recordAssignment(foreign)
            copy.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            copy.analyzer.rebindAssignment(mapped, replacement)
            copy.analyzer.recordAssignment(mapped)
            copy.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            copy.analyzer.recordAssignment(replacement)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            val completed = sourceFacts(copy.mini)
            assertTrue(completed.elements.any { it === replacement })
            assertFalse(completed.elements.any { it === assignment || it === mapped })
            assertEquals(setOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN), completed.types.toSet())
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            assertTrue(sourceFacts(original.mini).types.isEmpty())
            copy.mini.exitFunction()
            copy.mini.enterFunction(before)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(fixture.y, ConePrimitiveType.BOOLEAN)
            copy.mini.exitFunction()
            original.mini.exitFunction()
        }
    }

    @Test
    fun `second mapped snapshot reports source owners into only the effective T2 scopes and forks`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        val between = fixture.observer("between")
        val earlier = fixture.assignment(fixture.access(fixture.x), fixture.int(1))
        val later = buildAssignmentCopy(earlier) {}
        val t1Variable = fixture.variable("x", fixture.int(0))
        val t2Variable = fixture.variable("x", fixture.int(0))
        val t1Before = fixture.observer("before")
        val t2Before = fixture.observer("before")
        val t1Between = fixture.observer("between")
        val t2Between = fixture.observer("between")
        val t1Earlier = buildAssignmentCopy(earlier) { lValue = fixture.access(t1Variable) }
        val t1Later = buildAssignmentCopy(later) { lValue = fixture.access(t1Variable) }
        val t2Earlier = buildAssignmentCopy(t1Earlier) { lValue = fixture.access(t2Variable) }
        val t2Later = buildAssignmentCopy(t1Later) { lValue = fixture.access(t2Variable) }
        val sourceToT1 = OwnerMapper(
            fixture.x to t1Variable, before to t1Before, between to t1Between,
            earlier to t1Earlier, later to t1Later,
        )
        val t1ToT2 = OwnerMapper(
            t1Variable to t2Variable, t1Before to t2Before, t1Between to t2Between,
            t1Earlier to t2Earlier, t1Later to t2Later,
        )
        fixture.analyze(listOf(before, earlier, between, later), sourceToT1) { t1 ->
            t1.mini.enterFunction(t1Before)
            t1.assertUnstable(t1Variable, ConePrimitiveType.INT64)
            val t2 = fixture.state(t1.context.createSnapshot(t1ToT2, t1.analyzer.currentSmartCastPosition).context)
            t2.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            t2.assertUnstable(t2Variable, ConePrimitiveType.INT64)
            // mini 从 Source 构建了 T1 索引，快照将其投到 T2；原操作仍作为公开报告输入。
            t2.analyzer.recordAssignment(later, t2Variable)
            t2.assertUnstable(t2Variable, ConePrimitiveType.INT64)
            t2.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            t2.mini.exitFunction()
            t2.mini.enterFunction(t2Between)
            t2.assertStable(t2Variable, ConePrimitiveType.INT64)
            t2.assertStable(fixture.x, ConePrimitiveType.INT64)
            t2.mini.exitFunction()
            // 原 Source observer 必须进入相同 T2 fork；before 的未知 A 可识别查不到键的假稳定。
            t2.mini.enterFunction(between)
            t2.assertStable(fixture.x, ConePrimitiveType.INT64)
            t2.assertStable(t2Variable, ConePrimitiveType.INT64)
            t2.mini.exitFunction()
            t2.mini.enterFunction(before)
            t2.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            t2.assertUnstable(t2Variable, ConePrimitiveType.INT64)
            t2.mini.exitFunction()
            t2.mini.enterFunction(t2Before)
            t2.assertUnstable(t2Variable, ConePrimitiveType.INT64)
            t2.analyzer.recordAssignment(later, t2Variable)
            t2.assertUnstable(t2Variable, ConePrimitiveType.INT64)
            t2.analyzer.recordAssignment(later, fixture.x)
            t2.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            t2.assertUnstable(t2Variable, ConePrimitiveType.INT64)
            // A 首次使用 Source 声明报告，必须实际填入 T2 记录，再与有效声明报告相互核验。
            t2.analyzer.recordAssignment(earlier, fixture.x)
            t2.assertStable(fixture.x, ConePrimitiveType.INT64)
            t2.assertStable(t2Variable, ConePrimitiveType.INT64)
            t2.analyzer.recordAssignment(earlier, t2Variable)
            t2.assertStable(t2Variable, ConePrimitiveType.INT64)
            t2.assertStable(fixture.x, ConePrimitiveType.INT64)
            val source = sourceFacts(t2.mini)
            assertTrue(source.elements.any { it === t2Earlier })
            assertTrue(source.elements.any { it === t2Later })
            assertTrue(source.elements.any { it === t2Variable })
            assertFalse(source.elements.any {
                it === earlier || it === later || it === t1Earlier || it === t1Later ||
                    it === t1Variable || it === t1Before || it === t1Between
            })
            assertEquals(setOf(ConePrimitiveType.INT64), source.types.toSet())
            t1.assertUnstable(t1Variable, ConePrimitiveType.INT64)
            t1.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            assertTrue(sourceFacts(t1.mini).types.isEmpty())
            t1.mini.exitFunction()
            t1.mini.enterFunction(t1Between)
            t1.assertUnstable(t1Variable, ConePrimitiveType.INT64)
            t1.mini.exitFunction()
            t2.mini.exitFunction()
            t2.mini.enterFunction(before)
            t2.assertStable(fixture.x, ConePrimitiveType.INT64)
            t2.assertStable(t2Variable, ConePrimitiveType.INT64)
            t2.mini.exitFunction()
        }
    }

    @Test
    fun `raw augmented owner needs explicit rebind before the resolved report can fill it`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        val raw = buildAugmentedAssignment {
            source = binarySource("augmented")
            operation = Name.identifier("plus")
            leftArgument = fixture.access(fixture.x)
            rightArgument = fixture.int(1)
        }
        // 仅重现 resolver 的真实内部 a = a.plus(b) DAG，不声称该形态是仓颉源语法。
        val operator = CallResolutionTestFixtures.buildFunctionSymbol(
            fixture.session, "plus", ConePrimitiveType.INT64, listOf(ConePrimitiveType.INT64),
        )
        val resolved = buildAssignment {
            source = raw.source
            lValue = raw.leftArgument
            rValue = buildFunctionCall {
                source = raw.source
                explicitReceiver = raw.leftArgument
                calleeReference = buildResolvedNamedReference { name = raw.operation; resolvedSymbol = operator }
                argumentList = buildArgumentList { arguments += raw.rightArgument }
                origin = CfirFunctionCallOrigin.Operator
                coneTypeOrNull = ConePrimitiveType.INT64
            }
            augmentedOperation = raw.operation
            coneTypeOrNull = ConePrimitiveType.UNIT
        }
        val other = fixture.assignment(fixture.access(fixture.x), fixture.int(2))
        fixture.analyze(listOf(before, raw, other)) { state ->
            state.mini.enterFunction(before)
            state.analyzer.recordAssignment(resolved)
            assertTrue(sourceFacts(state.mini).types.isEmpty())
            state.analyzer.rebindAssignment(raw, resolved)
            state.analyzer.recordAssignment(resolved)
            state.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            val rebound = sourceFacts(state.mini)
            assertTrue(rebound.elements.any { it === resolved })
            assertFalse(rebound.elements.any { it === raw })
            assertEquals(setOf(ConePrimitiveType.INT64), rebound.types.toSet())
            state.analyzer.recordAssignment(resolved)
            state.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            state.analyzer.recordAssignment(other)
            state.assertStable(fixture.x, ConePrimitiveType.INT64)
            state.mini.exitFunction()
        }
    }

    @Test
    fun `default mini first nonidentity snapshot maps source reports queries and scopes without polluting old trees`() {
        val fixture = Fixture()
        val before = fixture.observer("before")
        val between = fixture.observer("between")
        val earlier = fixture.assignment(fixture.access(fixture.x), fixture.int(1))
        val later = buildAssignmentCopy(earlier) {}
        val effectiveVariable = fixture.variable("x", fixture.int(0))
        val effectiveBefore = fixture.observer("before")
        val effectiveBetween = fixture.observer("between")
        val effectiveEarlier = buildAssignmentCopy(earlier) { lValue = fixture.access(effectiveVariable) }
        val effectiveLater = buildAssignmentCopy(later) { lValue = fixture.access(effectiveVariable) }
        val firstMapper = OwnerMapper(
            fixture.x to effectiveVariable, before to effectiveBefore, between to effectiveBetween,
            earlier to effectiveEarlier, later to effectiveLater,
        )
        // 明确从默认 P=null 分支开始：首次 snapshot 必须安装 firMapper，不能依赖 previousMapper。
        fixture.analyze(listOf(before, earlier, between, later), projectionMapper = null) { original ->
            original.mini.enterFunction(before)
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            val copy = fixture.state(original.context.createSnapshot(firstMapper, original.analyzer.currentSmartCastPosition).context)
            val peer = fixture.state(original.context.createSnapshot(IdentityMapper, original.analyzer.currentSmartCastPosition).context)
            copy.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            copy.assertUnstable(effectiveVariable, ConePrimitiveType.INT64)
            val pending = sourceFacts(copy.mini)
            assertTrue(pending.elements.any { it === effectiveEarlier })
            assertTrue(pending.elements.any { it === effectiveLater })
            assertTrue(pending.types.isEmpty())

            copy.analyzer.recordAssignment(later, fixture.x)
            copy.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            copy.assertUnstable(effectiveVariable, ConePrimitiveType.INT64)
            copy.mini.exitFunction()
            copy.mini.enterFunction(effectiveBetween)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(effectiveVariable, ConePrimitiveType.INT64)
            copy.mini.exitFunction()
            copy.mini.enterFunction(before)
            copy.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            copy.assertUnstable(effectiveVariable, ConePrimitiveType.INT64)
            copy.analyzer.recordAssignment(earlier, fixture.x)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(effectiveVariable, ConePrimitiveType.INT64)
            copy.assertUnstable(effectiveVariable, ConePrimitiveType.BOOLEAN)
            val completed = sourceFacts(copy.mini)
            assertTrue(completed.elements.any { it === effectiveVariable })
            assertTrue(completed.elements.any { it === effectiveEarlier })
            assertTrue(completed.elements.any { it === effectiveLater })
            assertFalse(completed.elements.any {
                it === fixture.x || it === earlier || it === later || it === before || it === between
            })
            assertEquals(setOf(ConePrimitiveType.INT64), completed.types.toSet())
            copy.mini.exitFunction()
            copy.mini.enterFunction(effectiveBefore)
            copy.assertStable(fixture.x, ConePrimitiveType.INT64)
            copy.assertStable(effectiveVariable, ConePrimitiveType.INT64)
            copy.mini.exitFunction()

            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            peer.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            assertTrue(sourceFacts(original.mini).types.isEmpty())
            assertTrue(sourceFacts(peer.mini).types.isEmpty())
            original.mini.exitFunction()
            original.mini.enterFunction(between)
            original.assertUnstable(fixture.x, ConePrimitiveType.INT64)
            original.mini.exitFunction()
            peer.mini.exitFunction()
        }
    }

    /** 与已有 ContextTest 一样使用正式 session/typed CFIR；所有 Flow 由 analyzer 建立。 */
    private class Fixture {
        val session = CallResolutionTestFixtures.newTestSession().also {
            it.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
        }
        val x = variable("x", int(0))
        val y = variable("y", bool(false))

        fun int(value: Long) = buildLiteralExpression {
            source = binarySource("integer $value")
            kind = CfirLiteralKind.INT
            this.value = value
            coneTypeOrNull = ConePrimitiveType.INT64
        }

        fun bool(value: Boolean) = buildLiteralExpression {
            source = binarySource("boolean $value")
            kind = CfirLiteralKind.BOOLEAN
            this.value = value
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
        }

        fun variable(identifier: String, initializer: CfirExpression, mutable: Boolean = true) = buildPatternBindingVariable {
            source = binarySource(identifier)
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = true
            name = Name.identifier(identifier)
            symbol = CfirPatternBindingSymbol(CallableId(name))
            returnTypeRef = buildResolvedTypeRef { coneType = checkNotNull(initializer.coneTypeOrNull) }
            isVar = mutable
            this.initializer = initializer
        }

        fun access(variable: CfirPatternBindingVariable) = buildNamedAccessExpression {
            source = binarySource("access ${variable.name}")
            calleeReference = buildResolvedNamedReference { name = variable.name; resolvedSymbol = variable.symbol }
            coneTypeOrNull = (variable.returnTypeRef as org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef).coneType
        }

        fun tuple(vararg children: CfirExpression) = buildTupleLiteral {
            source = binarySource("tuple")
            elements += children
            coneTypeOrNull = ConeTupleType(children.map { checkNotNull(it.coneTypeOrNull) })
        }

        fun nestedTargets() = tuple(tuple(access(x), access(y)), tuple(access(y), access(x)))
        fun nestedValue() = tuple(tuple(int(1), bool(true)), tuple(bool(false), int(2)))

        fun assignment(left: CfirExpression, right: CfirExpression): CfirAssignment = buildAssignment {
            source = binarySource("assignment")
            lValue = left
            rValue = right
            coneTypeOrNull = ConePrimitiveType.UNIT
        }

        /** 局部函数的 assignedLater/assignedInside 来自真实 CFIR 正文，不手造 prohibition。 */
        fun observer(identifier: String, statements: List<CfirStatement> = emptyList()): CfirNamedFunction = buildNamedFunction {
            source = binarySource(identifier)
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            status = CfirDeclarationStatusImpl()
            isLocal = true
            isMut = false
            name = Name.identifier(identifier)
            symbol = CfirNamedFunctionSymbol(CallableId(name))
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.UNIT }
            body = buildBlock {
                this.statements += statements
                coneTypeOrNull = ConePrimitiveType.UNIT
            }
        }

        fun state(context: CfirDataFlowAnalyzerContext): State {
            val bodyContext = BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, context)
            val holder = object : SessionHolder { override val session = this@Fixture.session }
            return State(context, CfirDataFlowAnalyzer(holder, bodyContext), session)
        }

        fun analyze(
            statements: List<CfirStatement>,
            projectionMapper: SnapshotCfirMapper? = null,
            test: (State) -> Unit,
        ) {
            val fragment = buildCodeFragment {
                source = binarySource("fragment")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirCodeFragmentSymbol()
                block = buildBlock {
                    source = binarySource("block")
                    this.statements += listOf(x, y) + statements
                    coneTypeOrNull = ConePrimitiveType.UNIT
                }
            }
            val file = buildFile {
                source = binarySource("assignmentOccurrence.cj")
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                symbol = CfirFileSymbol()
                name = "assignmentOccurrence.cj"
                packageDirective = buildPackageDirective { packageFqName = FqName.ROOT; isMacroPackage = false }
                declarations += fragment
            }
            val context = CfirDataFlowAnalyzerContext(variableAssignmentAnalyzer = CfirLocalVariableAssignmentAnalyzer(projectionMapper))
            val bodyContext = BodyResolveContext(ReturnTypeCalculatorForFullBodyResolve.Default, context)
            val analyzer = CfirDataFlowAnalyzer(object : SessionHolder { override val session = this@Fixture.session }, bodyContext)
            bodyContext.withFile(file) {
                bodyContext.withContainer(fragment) {
                    analyzer.enterCodeFragment(fragment)
                    analyzer.enterBlock(fragment.block)
                    test(State(context, analyzer, session))
                    analyzer.exitBlock(fragment.block)
                    analyzer.exitCodeFragment(fragment)
                }
            }
        }
    }

    /** scope 只推进 mini 的公开词法生命周期；record/rebind 始终调用真实 analyzer 门面。 */
    private class State(
        val context: CfirDataFlowAnalyzerContext,
        val analyzer: CfirDataFlowAnalyzer,
        private val session: CallResolutionTestFixtures.TestSession,
    ) {
        val mini: CfirLocalVariableAssignmentAnalyzer get() = context.variableAssignmentAnalyzer

        fun assertStable(variable: CfirPatternBindingVariable, type: ConeCangJieType) {
            assertFalse(mini.isUnstableInCurrentScope(variable, setOf(type), session), "${variable.name} must preserve $type")
        }

        fun assertUnstable(variable: CfirPatternBindingVariable, type: ConeCangJieType) {
            assertTrue(mini.isUnstableInCurrentScope(variable, setOf(type), session), "${variable.name} must not be treated as preserving $type")
        }
    }

    /** 回调只收集 mini 主动交出的载荷，不递归 AST，也不读取 Assignment 私表。 */
    private fun sourceFacts(mini: CfirLocalVariableAssignmentAnalyzer): SourceFacts {
        val elements = mutableListOf<CfirElement>()
        val types = mutableListOf<ConeCangJieType>()
        val visitor: DfaSourceVisitor = createDfaSourceVisitor(
            onElement = { elements += it },
            onType = { types += it },
            canVisitGraph = { false },
        )
        mini.acceptSourceChildren(visitor)
        return SourceFacts(elements, types)
    }

    private data class SourceFacts(val elements: List<CfirElement>, val types: List<ConeCangJieType>)

    /**
     * 映射域只包含 mappings 登记的对象身份；声明同时登记其 symbol。
     * 域内 Source owner 是合法报告输入，映射后的有效 owner 也可直接报告。
     * 域外对象保持自身身份；同 source 的 foreigncopy 不因此取得已登记写事件的身份。
     */
    private class OwnerMapper(vararg mappings: Pair<CfirElement, CfirElement>) : SnapshotCfirMapper {
        private val elements = IdentityHashMap<CfirElement, CfirElement>().apply {
            mappings.forEach { (original, replacement) -> put(original, replacement) }
        }
        private val symbols = IdentityHashMap<CfirBasedSymbol<*>, CfirBasedSymbol<*>>().apply {
            mappings.forEach { (original, replacement) ->
                if (original is CfirDeclaration && replacement is CfirDeclaration) {
                    put(original.symbol, replacement.symbol)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T =
            if (symbols.containsKey(symbol)) symbols.getValue(symbol) as T else symbol

        @Suppress("UNCHECKED_CAST")
        override fun <T : CfirElement> mapElement(element: T): T =
            if (elements.containsKey(element)) elements.getValue(element) as T else element
    }

    private object IdentityMapper : SnapshotCfirMapper {
        override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol
        override fun <T : CfirElement> mapElement(element: T): T = element
    }

    companion object {
        private fun binarySource(identity: String) = object : CjBinarySourceElement(identity, null, identity) {}
    }
}
