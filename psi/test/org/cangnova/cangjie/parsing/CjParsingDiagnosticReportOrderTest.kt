package org.cangnova.cangjie.parsing

import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertNotNull as present
import kotlin.test.assertNotSame as distinct
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/**
 * 原生诊断去重必须保留 producer 的首报，不能以错误节点的父子遍历顺序选择消息。
 * 官方 cjc 1.1.3 的实际证据：
 * .workbuddy/tmp/cjmp_probe113/review-20261108/diagnostic-report-order/cjc-results.json。
 * class C{@M[]z} 唯一发布 parse_expected_decl；UTF16 [12,13)，原生 [1:13,1:14)。
 * 后报的非法宏输入错误通过 errorBefore 包住先报错误，不能替换该范围的首报。
 */
class CjParsingDiagnosticReportOrderTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun sdkPsiPublishesTheFirstReportInsideTheLaterWrappingError() {
        val file = sourceFile()
        val input = actualLexicalInput(file)

        assertLaterErrorWrapsTheFirstReport(file)
        assertOfficialFirstReport(file.collectParsingDiagnostics(), input, "SDK PSI")
        // 再次遍历已经展开的真实 PSI，不能让首报身份依赖收集次数。
        assertOfficialFirstReport(file.collectParsingDiagnostics(), input, "SDK PSI 再次收集")
        identical(input, actualLexicalInput(file))
    }

    @Test
    fun compilationPsiPublishesTheFirstReportFromItsIndependentTargetTree() {
        val source = sourceFile()
        val input = actualLexicalInput(source)
        val product = CangJieParser.parseCompilationFile(
            source, CjmpParsingContext.Compilation(false, false, true),
        )

        identical(source, product.originFile)
        distinct(source, product.file)
        distinct(source.node, product.file.node)
        equal(SOURCE, product.file.text)
        identical(input, product.actualInputSeed.lexicalInput)
        identical(product.actualInputSeed, product.file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        identical(input, actualLexicalInput(product.file))
        product.requireConsistentInput()

        // 目标期望直接取官方常量；不能用原 PSI 的诊断作为目标期望。
        assertLaterErrorWrapsTheFirstReport(product.file)
        assertOfficialFirstReport(product.diagnostics, input, "编译目标 PSI 解析结果")
        assertOfficialFirstReport(product.file.collectParsingDiagnostics(), input, "编译目标 PSI 再次收集")
    }

    @Test
    fun bufferedLightTreePublishesTheFirstReportWithTheActualPsiLexicalInput() {
        val source = sourceFile()
        val input = actualLexicalInput(source)
        // SDK builder 独立解析同一冻结输入；不另造文本快照，也不借 PSI 诊断构造期望。
        val builder = PsiBuilderImpl(
            null, null, CangJieParserDefinition(), CangJieLexer(), null, SOURCE, null, null,
        )
        val parsed = CangJieLightParser.parseBuffered(builder, lexicalInput = input)

        identical(input, parsed.file.lexicalInput)
        identical(input.source, parsed.file.sourceContext.snapshot)
        equal(CangJieLightDiagnosticOwnership.BUFFERED, parsed.file.diagnosticOwnership)
        parsed.file.requireConsistentInput()
        assertOfficialFirstReport(parsed.diagnostics, input, "buffered LightTree")
    }

    /** 官方 const-parameter.cj 只发布入队最早的 const 声明错误，倒序物化不能授予后报优先级。 */
    @Test
    fun deferredParameterMessagesKeepTheFirstProducerReportInAllSdkRoutes() {
        val text = "func f(const value: Int64): Unit {}\n"
        val source = CjPsiFactory(project, markGenerated = false).createFile(text)
        val input = actualLexicalInput(source)
        val errors = PsiTreeUtil.findChildrenOfType(source, PsiErrorElement::class.java)
        val first = errors.single { it.errorDescription == CONST_FIRST_MESSAGE }
        val laterMessage = org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.parameter.no.modifier", "const")
        val later = errors.single { it.errorDescription == laterMessage }
        truth(PsiTreeUtil.isAncestor(first, later, true), "队列倒序物化的原 AST 嵌套必须保留")

        val target = CangJieParser.parseCompilationFile(source, CjmpParsingContext.Compilation(false, false, true))
        val builder = PsiBuilderImpl(null, null, CangJieParserDefinition(), CangJieLexer(), null, text, null, null)
        val light = CangJieLightParser.parseBuffered(builder, lexicalInput = input)
        for ((route, diagnostics) in listOf(
            "SDK PSI" to source.collectParsingDiagnostics(),
            "编译目标 PSI" to target.diagnostics,
            "LightTree" to light.diagnostics,
        )) {
            equal(listOf(CONST_FIRST_MESSAGE), diagnostics.map { it.message }, route)
            val diagnostic = diagnostics.single()
            equal(7, diagnostic.startOffset, route)
            equal(12, diagnostic.endOffset, route)
            equal(ParsingDiagnostic.Category.PARSE, diagnostic.category, route)
            val range = present(diagnostic.mainRange)
            identical(input.source, range.source, route)
            equal(1 to 8, range.begin, route)
            equal(1 to 13, range.end, route)
            equal(emptyList(), diagnostic.nativeHints, route)
        }
    }

    private fun sourceFile(): CjFile = CjPsiFactory(project, markGenerated = false).createFile(SOURCE)

    /** 仅展开真实文件根，词法输入必须来自本次 SDK PSI parser 发布的 seed。 */
    private fun actualLexicalInput(file: CjFile): CjLexicalSourceInput {
        file.node.firstChildNode
        val seed = present(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        val input = present(seed.lexicalInput)
        identical(input.source, present(seed.sourceContext).snapshot)
        equal(file.text, input.source.text)
        return input
    }

    /** 此回归必须实际产生内外两个错误，而不是手工制造报告顺序或丢弃后报节点。 */
    private fun assertLaterErrorWrapsTheFirstReport(file: CjFile) {
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        val first = errors.single { it.errorDescription == FIRST_MESSAGE }
        val later = errors.single { it.errorDescription == LATER_MESSAGE }
        truth(PsiTreeUtil.isAncestor(later, first, true), "后报错误应包住先报错误，触发树遍历逆序")
    }

    /** 所有入口分别断言官方 published message、原始偏移及原生范围，不经 collector 构造期望。 */
    private fun assertOfficialFirstReport(
        diagnostics: List<ParsingDiagnostic>,
        input: CjLexicalSourceInput,
        route: String,
    ) {
        equal(listOf(FIRST_MESSAGE), diagnostics.map { it.message }, "$route：唯一发布 parse_expected_decl")
        val diagnostic = diagnostics.single()
        equal(12, diagnostic.startOffset, "$route：UTF16 起点")
        equal(13, diagnostic.endOffset, "$route：UTF16 终点")
        equal(ParsingDiagnostic.Category.PARSE, diagnostic.category, route)
        val range = present(diagnostic.mainRange, "$route：完成的原生主范围")
        identical(input.source, range.source, "$route：真实冻结来源")
        equal(1 to 13, range.begin, "$route：原生起点")
        equal(1 to 14, range.end, "$route：原生终点")
        // mainRange 对应官方 MainHint；nativeHints 对应 OtherHints，本例官方明确为空。
        equal(emptyList(), diagnostic.nativeHints, "$route：没有原生附加提示")
    }

    private companion object {
        const val SOURCE = "class C{@M[]z}\n"
        const val FIRST_MESSAGE = "expected declaration, found 'z'"
        const val LATER_MESSAGE = "illegal macro expansion input arguments without parentheses"
        const val CONST_FIRST_MESSAGE = "unexpected const variable declaration in paramlist"
    }
}
