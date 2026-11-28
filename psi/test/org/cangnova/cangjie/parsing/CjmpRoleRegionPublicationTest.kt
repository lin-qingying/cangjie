package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.impl.source.tree.LazyParseableElement
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.cjmpRoleEventsFor
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.awt.EventQueue
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertFalse as assertFalseValue
import kotlin.test.assertNotEquals as assertDifferentValue
import kotlin.test.assertNotNull as assertPresent
import kotlin.test.assertNotSame as assertDistinct
import kotlin.test.assertNull as assertAbsent
import kotlin.test.assertSame as assertIdentical
import kotlin.test.assertTrue as assertTrueValue

/**
 * 真实 PSI 的惰性角色区域发布契约：仅用 parent-slot 身份替换 Unknown，不预展开正文。
 * 主线程已考证官方 v1.1.3：函数先检查修饰符，普通变量先解析初始化式；检查顺序不按偏移排序。
 * public 函数与 unsafe 局部函数沿用既有合法形状，非角色修饰符仍产生 Check 事件。
 * 独立子根的局部序号可以重复；采集必须保留事件对象及原根身份，旧快照保持不可变。
 */
class CjmpRoleRegionPublicationTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun collectingAnUnloadedFileDoesNotLoadItsAstOrPublishFacts() {
        val file = createRoleFile("unloadedRoleRegion", SINGLE_BODY)
        assertFalseValue(file.isContentsLoaded)
        assertAbsent(file.parsedRoleSnapshot)

        repeat(3) {
            assertAbsent(file.collectParsedRoleSnapshot())
            assertAbsent(file.parsedRoleSnapshot)
            assertFalseValue(file.isContentsLoaded, "未发布的文件根不能由 collect 触发建树")
        }
    }

    @Test
    fun collectingUnexpandedBodiesKeepsTheirActualUnknownSlots() {
        val file = loadRoleFile("unexpandedRoleRegions", TWO_BODIES)
        val functions = file.declarations.filterIsInstance<CjNamedFunction>()
        assertValueEquals(2, functions.size)
        val first = bodyOf(functions[0])
        val second = bodyOf(functions[1])
        val initial = assertPresent(file.parsedRoleSnapshot)
        val firstSlot = unknownSlot(initial, first)
        val secondSlot = unknownSlot(initial, second)
        val expected = listOf(checkOf(functions[0]), firstSlot, checkOf(functions[1]), secondSlot)

        assertDifferentValue(firstSlot.id, secondSlot.id)
        assertValueEquals(firstSlot.id.rootIdentity, secondSlot.id.rootIdentity)
        assertEvents(initial, expected)
        repeat(3) {
            assertFalseValue(first.isParsed)
            assertFalseValue(second.isParsed)
            val collected = collect(file)
            assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, collected.completeness)
            assertEvents(collected, expected)
            assertContextPreserved(initial, collected)
            assertFalseValue(first.isParsed, "collect 不得为获取区域事件预展开第一个正文")
            assertFalseValue(second.isParsed, "collect 不得为获取区域事件预展开第二个正文")
        }
        assertEvents(initial, expected)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, initial.completeness)
    }

    @Test
    fun interleavedExpansionReplacesOnlyTheBoundSlotAtEachDepth() {
        val file = loadRoleFile("nestedRoleRegions", TWO_BODIES)
        val functions = file.declarations.filterIsInstance<CjNamedFunction>()
        val firstCheck = checkOf(functions[0])
        val secondCheck = checkOf(functions[1])
        val firstBody = bodyOf(functions[0])
        val secondBody = bodyOf(functions[1])
        val initial = assertPresent(file.parsedRoleSnapshot)
        val firstSlot = unknownSlot(initial, firstBody)
        val secondSlot = unknownSlot(initial, secondBody)
        val originalEvents = initial.events.toList()

        // 先展开源码中靠后的兄弟，不能把新子根放进第一个 Unknown。
        expand(secondBody)
        assertValueEquals(secondSlot.id, assertPresent(secondBody.parserSeedOrNull()).roleRegionSlot)
        val secondNested = localFunction(secondBody)
        val secondNestedCheck = checkOf(secondNested)
        val secondNestedBody = bodyOf(secondNested)
        val afterSecond = collect(file)
        val secondNestedSlot = unknownSlot(afterSecond, secondNestedBody)
        assertEvents(afterSecond, listOf(firstCheck, firstSlot, secondCheck, secondNestedCheck, secondNestedSlot))
        assertFalseValue(firstBody.isParsed)
        assertFalseValue(secondNestedBody.isParsed)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, afterSecond.completeness)
        assertDifferentValue(secondSlot.id.rootIdentity, secondNestedCheck.id.rootIdentity)

        expand(firstBody)
        assertValueEquals(firstSlot.id, assertPresent(firstBody.parserSeedOrNull()).roleRegionSlot)
        val firstNested = localFunction(firstBody)
        val firstNestedCheck = checkOf(firstNested)
        val firstNestedBody = bodyOf(firstNested)
        val afterFirst = collect(file)
        val firstNestedSlot = unknownSlot(afterFirst, firstNestedBody)
        val firstLevelEvents = listOf(firstCheck, firstNestedCheck, firstNestedSlot,
            secondCheck, secondNestedCheck, secondNestedSlot)
        assertEvents(afterFirst, firstLevelEvents)
        assertDifferentValue(firstNestedCheck.id.rootIdentity, secondNestedCheck.id.rootIdentity)
        assertValueEquals(0L, firstNestedCheck.id.sequence)
        assertValueEquals(0L, secondNestedCheck.id.sequence)
        assertFalseValue(firstNestedBody.isParsed)
        assertFalseValue(secondNestedBody.isParsed)

        expand(firstNestedBody)
        assertValueEquals(firstNestedSlot.id, assertPresent(firstNestedBody.parserSeedOrNull()).roleRegionSlot)
        val leaf = localFunction(firstNestedBody)
        val leafCheck = checkOf(leaf)
        val leafBody = bodyOf(leaf)
        val afterLeafDeclaration = collect(file)
        val leafSlot = unknownSlot(afterLeafDeclaration, leafBody)
        assertEvents(afterLeafDeclaration, listOf(firstCheck, firstNestedCheck, leafCheck, leafSlot,
            secondCheck, secondNestedCheck, secondNestedSlot))
        assertDifferentValue(firstNestedCheck.id.rootIdentity, leafCheck.id.rootIdentity)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, afterLeafDeclaration.completeness)
        assertFalseValue(leafBody.isParsed, "嵌套 Unknown 尚未展开时不能发布 COMPLETE")
        assertFalseValue(secondNestedBody.isParsed)

        expand(leafBody)
        val stillPartial = collect(file)
        assertEvents(stillPartial, listOf(firstCheck, firstNestedCheck, leafCheck,
            secondCheck, secondNestedCheck, secondNestedSlot))
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, stillPartial.completeness)
        assertFalseValue(secondNestedBody.isParsed)
        expand(secondNestedBody)
        val complete = collect(file)
        assertComplete(complete)
        assertEvents(complete, listOf(firstCheck, firstNestedCheck, leafCheck, secondCheck, secondNestedCheck))
        assertContextPreserved(initial, complete)
        assertDistinct(initial, complete)
        assertEvents(initial, originalEvents)
        assertEvents(afterFirst, firstLevelEvents)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, initial.completeness)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, afterFirst.completeness)
    }

    @Test
    fun expandedRootsWithMissingOrForeignSlotsLeaveTheParentUnknown() {
        for (missingSlot in listOf(true, false)) {
            val file = loadRoleFile("unboundRegion$missingSlot", SINGLE_BODY)
            val foreignFile = loadRoleFile("foreignRegion$missingSlot", SINGLE_BODY)
            val body = bodyOf(file.declarations.filterIsInstance<CjNamedFunction>().single())
            val foreignBody = bodyOf(foreignFile.declarations.filterIsInstance<CjNamedFunction>().single())
            val initial = assertPresent(file.parsedRoleSnapshot)
            val parentSlot = unknownSlot(initial, body)
            val foreignInitial = assertPresent(foreignFile.parsedRoleSnapshot)
            val foreignSlot = unknownSlot(foreignInitial, foreignBody)
            assertValueEquals(parentSlot.declarationOffset, foreignSlot.declarationOffset)
            assertValueEquals(parentSlot.id.sequence, foreignSlot.id.sequence)
            assertDifferentValue(parentSlot.id.rootIdentity, foreignSlot.id.rootIdentity)

            // 仅破坏真实 chameleon 的绑定输入，文本、语境和语法均不变。
            val seed = assertPresent(body.parserSeedOrNull())
            val replacementSlot = if (missingSlot) null else foreignSlot.id
            body.putCopyableUserData(PARSER_SEED_KEY, seed.copy(roleRegionSlot = replacementSlot))
            expand(body)
            assertValueEquals(replacementSlot, assertPresent(body.parserSeedOrNull()).roleRegionSlot)
            expand(bodyOf(localFunction(body)))

            val collected = collect(file)
            assertEvents(collected, initial.events)
            assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, collected.completeness)
            assertIdentical(parentSlot, collected.events.single { it.id == parentSlot.id })
            assertContextPreserved(initial, collected)
            assertIdentical(foreignInitial, foreignFile.parsedRoleSnapshot)
            assertFalseValue(foreignBody.isParsed)
        }
    }

    @Test
    fun repeatedCollectionKeepsEventIdentityAndInitializerCheckOrder() {
        val source = "public let callback = { => unsafe func nested(): Unit {} }"
        val file = loadRoleFile("initializerRoleRegion", source)
        val variable = file.declarations.filterIsInstance<CjPatternVariable>().single()
        val variableCheck = checkOf(variable)
        val lambda = assertPresent(variable.initializer) as CjLambdaExpression
        val initial = assertPresent(file.parsedRoleSnapshot)
        val lambdaSlot = unknownSlot(initial, lambda)
        assertEvents(initial, listOf(lambdaSlot, variableCheck))
        assertFalseValue(lambda.isParsed)

        expand(lambda)
        assertValueEquals(lambdaSlot.id, assertPresent(lambda.parserSeedOrNull()).roleRegionSlot)
        val nested = localFunction(assertPresent(lambda.bodyExpression))
        val nestedCheck = checkOf(nested)
        val nestedBody = bodyOf(nested)
        val partial = collect(file)
        val nestedSlot = unknownSlot(partial, nestedBody)
        val partialEvents = listOf(nestedCheck, nestedSlot, variableCheck)
        assertEvents(partial, partialEvents)
        assertDifferentValue(variableCheck.id.rootIdentity, nestedCheck.id.rootIdentity)
        assertTrueValue(source.indexOf("let callback") < source.indexOf("func nested"))

        // ParseVarDecl 先处理初始化式；不允许按偏移把变量检查排到局部函数之前。
        repeat(4) {
            val collected = collect(file)
            assertEvents(collected, partialEvents)
            assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, collected.completeness)
            assertFalseValue(nestedBody.isParsed)
        }
        expand(nestedBody)
        val complete = collect(file)
        val expected = listOf(nestedCheck, variableCheck)
        assertComplete(complete)
        assertEvents(complete, expected)
        assertContextPreserved(initial, complete)
        assertDistinct(initial, complete)
        repeat(4) {
            val repeated = collect(file)
            assertValueEquals(complete, repeated)
            assertEvents(repeated, expected)
            assertComplete(repeated)
        }
        assertEvents(initial, listOf(lambdaSlot, variableCheck))
        assertEvents(partial, partialEvents)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, initial.completeness)
    }

    @Test
    fun expandingAFileCopyPublishesOnlyItsOwnRegions() {
        val original = loadRoleFile("copiedRoleRegion", SINGLE_BODY)
        val originalBody = bodyOf(original.declarations.filterIsInstance<CjNamedFunction>().single())
        val initial = assertPresent(original.parsedRoleSnapshot)
        val originalSlot = unknownSlot(initial, originalBody)
        val copy = original.copy() as CjFile
        assertIdentical(original, copy.originalFile, "来源证明来自 SDK 的实际 copy 关联")
        val copiedFunction = copy.declarations.filterIsInstance<CjNamedFunction>().single()
        val copiedBody = bodyOf(copiedFunction)
        val copiedInitial = assertPresent(copy.parsedRoleSnapshot)
        assertDistinct(original.node, copy.node)
        assertDistinct(originalBody, copiedBody)
        assertValueEquals(initial, copiedInitial)
        assertValueEquals(originalSlot.id, unknownSlot(copiedInitial, copiedBody).id)
        assertFalseValue(originalBody.isParsed)
        assertFalseValue(copiedBody.isParsed)

        expand(copiedBody)
        val parentRegion = assertPresent(copy.parsedRoleRegion)
        val expectedSeed = parentRegion.slots.getValue(originalSlot.id)
        val expandedRegion = assertPresent(copiedBody.findChildByType(CjTokens.LBRACE)
            ?.getCopyableUserData(PARSED_ROLE_REGION_KEY))
        assertIdentical(expectedSeed, expandedRegion.entrySeed, "PSI 副本展开必须保留实际收到的父入口")
        assertValueEquals(expectedSeed.sourceContext, expandedRegion.sourceContext,
            "virtualFile 的缺失不能改变真实 PSI 副本继承的 parser 输入身份")
        val copiedNested = localFunction(copiedBody)
        val copiedNestedCheck = checkOf(copiedNested)
        expand(bodyOf(copiedNested))
        val copiedComplete = collect(copy)
        assertComplete(copiedComplete)
        assertEvents(copiedComplete, listOf(checkOf(copiedFunction), copiedNestedCheck))
        assertContextPreserved(initial, copiedComplete)
        assertIdentical(initial, original.parsedRoleSnapshot)
        assertFalseValue(originalBody.isParsed, "副本发布不得展开或更新原文件")
        val originalPartial = collect(original)
        assertEvents(originalPartial, initial.events)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, originalPartial.completeness)
        assertFalseValue(originalBody.isParsed)

        expand(originalBody)
        val originalFunction = original.declarations.filterIsInstance<CjNamedFunction>().single()
        val originalNested = localFunction(originalBody)
        val originalNestedCheck = checkOf(originalNested)
        expand(bodyOf(originalNested))
        val originalComplete = collect(original)
        assertComplete(originalComplete)
        assertEvents(originalComplete, listOf(checkOf(originalFunction), originalNestedCheck))
        assertDifferentValue(copiedNestedCheck.id.rootIdentity, originalNestedCheck.id.rootIdentity)
        assertIdentical(copiedComplete, copy.parsedRoleSnapshot)
        assertEvents(initial, copiedInitial.events)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, copiedInitial.completeness)
    }

    @Test
    fun copiedInputRemainsIndependentWhenTheOriginalAstIsReplaced() {
        val original = loadRoleFile("copyFrozenRoleInput", SINGLE_BODY)
        val copy = original.copy() as CjFile
        val initial = assertPresent(copy.parsedRoleSnapshot)
        val oldText = original.text
        val replacement = "public func replacement(): Unit {}"
        val diff = BlockSupportImpl.reparse(original, original.node, TextRange(0, oldText.length),
            replacement, EmptyProgressIndicator(), oldText)
        EventQueue.invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction { diff.log.doActualPsiChange(original) }
            }, "替换原文件 AST", null)
        }
        assertValueEquals(replacement, original.text)
        assertValueEquals(SINGLE_BODY, copy.text)
        val function = copy.declarations.filterIsInstance<CjNamedFunction>().single()
        val body = bodyOf(function)
        expand(body)
        val nested = localFunction(body)
        expand(bodyOf(nested))
        val complete = collect(copy)
        assertComplete(complete)
        assertEvents(complete, listOf(checkOf(function), checkOf(nested)))
        assertContextPreserved(initial, complete)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, initial.completeness)
        assertValueEquals(replacement, original.text)
    }

    @Test
    fun filesWithTheSameNameTextAndOffsetsNeverShareRegionRoots() {
        val first = loadRoleFile("sameRoleRegion", SINGLE_BODY)
        val second = loadRoleFile("sameRoleRegion", SINGLE_BODY)
        val firstFunction = first.declarations.filterIsInstance<CjNamedFunction>().single()
        val secondFunction = second.declarations.filterIsInstance<CjNamedFunction>().single()
        val firstBody = bodyOf(firstFunction)
        val secondBody = bodyOf(secondFunction)
        val firstInitial = assertPresent(first.parsedRoleSnapshot)
        val secondInitial = assertPresent(second.parsedRoleSnapshot)
        val firstSlot = unknownSlot(firstInitial, firstBody)
        val secondSlot = unknownSlot(secondInitial, secondBody)
        assertDistinct(first, second)
        assertValueEquals(first.name, second.name)
        assertValueEquals(firstBody.startOffset, secondBody.startOffset)
        assertValueEquals(firstSlot.declarationOffset, secondSlot.declarationOffset)
        assertValueEquals(firstSlot.id.sequence, secondSlot.id.sequence)
        assertDifferentValue(firstSlot.id.rootIdentity, secondSlot.id.rootIdentity)

        expand(secondBody)
        val secondNested = localFunction(secondBody)
        val secondNestedCheck = checkOf(secondNested)
        expand(bodyOf(secondNested))
        val secondComplete = collect(second)
        assertComplete(secondComplete)
        assertEvents(secondComplete, listOf(checkOf(secondFunction), secondNestedCheck))
        assertIdentical(firstInitial, first.parsedRoleSnapshot)
        val firstPartial = collect(first)
        assertEvents(firstPartial, firstInitial.events)
        assertFalseValue(firstBody.isParsed)
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, firstPartial.completeness)

        expand(firstBody)
        val firstNested = localFunction(firstBody)
        val firstNestedCheck = checkOf(firstNested)
        expand(bodyOf(firstNested))
        val firstComplete = collect(first)
        assertComplete(firstComplete)
        assertEvents(firstComplete, listOf(checkOf(firstFunction), firstNestedCheck))
        assertDifferentValue(firstNestedCheck.id.rootIdentity, secondNestedCheck.id.rootIdentity)
        assertValueEquals(firstNestedCheck.id.sequence, secondNestedCheck.id.sequence)
        assertIdentical(secondComplete, second.parsedRoleSnapshot)
        assertEvents(firstInitial, listOf(checkOf(firstFunction), firstSlot))
        assertEvents(secondInitial, listOf(checkOf(secondFunction), secondSlot))
    }

    @Test
    fun stubConstructionPublishesTheSnapshotAfterTheVisitorExpandsBodies() {
        val file = loadRoleFile("stubRoleRegions", SINGLE_BODY)
        val function = file.declarations.filterIsInstance<CjNamedFunction>().single()
        val body = bodyOf(function)
        val initial = assertPresent(file.parsedRoleSnapshot)
        val slot = unknownSlot(initial, body)
        val functionCheck = checkOf(function)
        assertFalseValue(body.isParsed)
        assertEvents(initial, listOf(functionCheck, slot))

        val stub = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        assertTrueValue(body.isParsed, "真实 stub visitor 已访问正文，最终快照不能保留此前的 Unknown")
        val nested = localFunction(body)
        assertTrueValue(bodyOf(nested).isParsed, "断言前不由测试展开 visitor 应当访问的嵌套正文")
        val published = assertPresent(file.parsedRoleSnapshot)
        val persisted = assertPresent(stub.cjmpRoleSnapshot)
        val expected = listOf(functionCheck, checkOf(nested))
        assertComplete(published)
        assertComplete(persisted)
        assertEvents(published, expected)
        assertEvents(persisted, expected)
        assertContextPreserved(initial, published)
        assertContextPreserved(published, persisted)
        assertDistinct(initial, published)
        assertEvents(initial, listOf(functionCheck, slot))
        assertValueEquals(CjmpRoleInputCompleteness.PARTIAL, initial.completeness)
        assertTrueValue(persisted.stubBindings.isNotEmpty())
        val functionStub = stub.childrenStubs.filterIsInstance<CangJieNamedFunctionStub>().single()
        val bound = assertPresent(stub.cjmpRoleEventsFor(functionStub))
        assertValueEquals(listOf(functionCheck), bound)
        assertIdentical(functionCheck, bound.single())
        assertValueEquals(emptyMap(), published.stubBindings)
        assertEvents(collect(file), expected)
    }

    /** 必须先发布语境再读取 firstChild，避免测试意外使用 SyntaxOnly 初次解析。 */
    private fun createRoleFile(name: String, source: String): CjFile {
        val file = createPsiFile(name, source) as CjFile
        assertFalseValue(file.isContentsLoaded)
        file.cjmpParsingContext = CjmpParsingContext.Compilation(true, true, true)
        assertFalseValue(file.isContentsLoaded)
        return file
    }

    private fun loadRoleFile(name: String, source: String): CjFile {
        val file = createRoleFile(name, source)
        assertPresent(file.firstChild)
        assertTrueValue(file.isContentsLoaded)
        assertValueEquals(file.cjmpParsingContext, assertPresent(file.parsedRoleSnapshot).parsingContext)
        return file
    }

    private fun collect(file: CjFile): CjmpFileRoleSyntaxSnapshot {
        val snapshot = assertPresent(file.collectParsedRoleSnapshot())
        assertIdentical(snapshot, file.parsedRoleSnapshot, "返回值必须是文件本次发布的不可变快照")
        return snapshot
    }

    private fun bodyOf(function: CjNamedFunction): CjBlockExpression = assertPresent(function.bodyBlockExpression)

    /** 仅在测试已显式展开当前正文之后读取其直接局部函数，不深搜或展开其他区域。 */
    private fun localFunction(body: CjBlockExpression): CjNamedFunction {
        assertTrueValue(body.isParsed)
        return body.statements.filterIsInstance<CjNamedFunction>().single()
    }

    private fun expand(region: LazyParseableElement) {
        assertFalseValue(region.isParsed)
        assertPresent(region.firstChildNode)
        assertTrueValue(region.isParsed)
    }

    /** 期望取自真实声明关键字上的原事件，不以偏移或名字在文件快照中重新绑定。 */
    private fun checkOf(declaration: CjDeclaration): CjmpParsedRoleEvent {
        val event = generateSequence(declaration.node.firstChildNode) { it.treeNext }
            .mapNotNull { it.getCopyableUserData(CJMP_DECLARATION_ROLE_EVENTS_KEY) }
            .flatten().single()
        assertValueEquals(CjmpRoleEventKind.Check(CjmpRoleModifiers(false, false, false)), event.kind)
        return event
    }

    private fun unknownSlot(snapshot: CjmpFileRoleSyntaxSnapshot, region: ASTNode): CjmpParsedRoleEvent {
        val id = assertPresent(assertPresent(region.parserSeedOrNull()).roleRegionSlot)
        val event = snapshot.events.single { it.id == id }
        assertValueEquals(CjmpRoleEventKind.Unknown, event.kind)
        return event
    }

    private fun assertEvents(snapshot: CjmpFileRoleSyntaxSnapshot, expected: List<CjmpParsedRoleEvent>) {
        assertValueEquals(expected, snapshot.events)
        assertValueEquals(expected.map { it.id }, snapshot.events.map { it.id })
        assertValueEquals(snapshot.events.size, snapshot.events.map { it.id }.toSet().size)
        expected.zip(snapshot.events).forEach { (original, published) ->
            assertIdentical(original, published, "采集不得重建相同 ID 的事件对象")
        }
    }

    private fun assertContextPreserved(before: CjmpFileRoleSyntaxSnapshot, after: CjmpFileRoleSyntaxSnapshot) {
        assertValueEquals(before.parsingContext, after.parsingContext)
        assertValueEquals(before.entryFileRole, after.entryFileRole)
        assertValueEquals(assertPresent(before.sourceFingerprint), after.sourceFingerprint)
    }

    private fun assertComplete(snapshot: CjmpFileRoleSyntaxSnapshot) {
        assertValueEquals(CjmpRoleInputCompleteness.COMPLETE, snapshot.completeness)
        assertFalseValue(snapshot.events.any { it.kind == CjmpRoleEventKind.Unknown })
    }

    companion object {
        private val SINGLE_BODY = """
            public func host(): Unit {
                unsafe func nested(): Unit {}
            }
        """.trimIndent()

        private val TWO_BODIES = """
            public func first(): Unit {
                unsafe func nested(): Unit {
                    unsafe func leaf(): Unit {}
                }
            }
            public func second(): Unit {
                unsafe func nested(): Unit {}
            }
        """.trimIndent()
    }
}
