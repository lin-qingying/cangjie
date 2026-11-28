package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

/**
 * SDK 1.1.3 的 static 修饰符族完整 parser 期望，不经过 CJMP rulesOnly 过滤。
 * 前两个原始输入、主范围、消息及 related hints 来自
 * psi/build/cjmp-verification/20261113-cjmp-static-modifier-authority/cjc-results.json。
 * 其余输入由同一 SDK 验证，记录在 20261113-cjmp-static-modifier-test-writer/cjc-parser-summary.json。
 * CFIR 夹具只承载语义诊断，不能将这些 Parse 错误改名后塞入 CFIR 诊断表。
 * v1.0.5 没有显式 abstract 诊断且作用域表没有 abstract；这里仅覆盖 v1.1.0 起的规则。
 */
class StaticModifierDiagnosticsParsingTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    /** static 仅是 related hint；三个冲突各锚完整当前 token，static+abstract 不生成冲突。 */
    @Test
    fun staticCannotBeOpenAbstractOverrideReportsAllParserErrors() {
        val text = """
            // RUN_PIPELINE_TILL: FRONTEND
            open class Parent {
                public open func canOverride(): Unit {}
            }

            class Child <: Parent {
                public static override func canOverride(): Unit {}
            }

            class StaticStatus {
                public static open func openFn(): Unit {}
                public static operator func +(value: Int64): Int64 { value }
                redef func badRedef(): Unit {}
            }

            abstract class AbstractStaticStatus {
                static abstract func absFn(): Unit
            }
        """.trimIndent()
        assertBoth(text, listOf(
            conflict(text, 7, "override", 28),
            conflict(text, 11, "open", 24),
            conflict(text, 12, "operator", 28),
            explicitlyAbstract(text, 17),
            illegalAbstract(text, 17, 16),
        ))
    }

    /** call 不是可重载运算符；保留原名，独立断言完整 call，而非参数数量诊断。 */
    @Test
    fun staticIncompatibleModifiersRichAlsoReportsInvalidOperatorName() {
        val text = """
            // RUN_PIPELINE_TILL: FRONTEND
            open class Parent {
                public open func canOverride(): Unit {}
            }

            class Child <: Parent {
                public static override func canOverride(): Unit {}
            }

            class StaticStatus {
                public static open func openFn(): Unit {}
                public static operator func call(): Unit {}
                redef func badRedef(): Unit {}
            }

            abstract class AbstractStaticStatus {
                static abstract func absFn(): Unit
            }
        """.trimIndent()
        assertBoth(text, listOf(
            conflict(text, 7, "override", 28),
            conflict(text, 11, "open", 24),
            conflict(text, 12, "operator", 28),
            // 官方 parse_invalid_overloaded_operator 的消息保留末尾空格。
            diagnostic(text, 12, 33, 37, "cannot overload operator 'call' "),
            explicitlyAbstract(text, 17),
            illegalAbstract(text, 17, 16),
        ))
    }

    /** 原 memberStatus 源码经 SDK 1.1.3 确认四条 Parse 错误，mut 成员不增加 Parse 诊断。 */
    @Test
    fun memberStatusCheckersRichReportsCompleteStaticModifierFamily() {
        val text = """
            // RUN_PIPELINE_TILL: FRONTEND
            open class Parent {
                public open func canOverride(): Unit {}
            }

            interface MutCarrier {
                mut func denied(): Unit
            }

            struct Counter {
                mut func allowed(): Unit {}
            }

            class StaticStatus {
                public static open func openFn(): Unit {}
            }

            class StaticOverride <: Parent {
                public static override func canOverride(): Unit {}
            }

            abstract class AbstractStaticStatus {
                static abstract func absFn(): Unit
            }
        """.trimIndent()
        assertBoth(text, listOf(
            conflict(text, 15, "open", 24),
            conflict(text, 19, "override", 28),
            explicitlyAbstract(text, 23),
            illegalAbstract(text, 23, 22),
        ))
    }

    /** 普通 abstract class 中没有 static 也必须报告完整两错误，不能将作用域错误当成 static 冲突。 */
    @Test
    fun ordinaryAbstractClassReportsBothExplicitAbstractErrors() {
        val text = """
            abstract class Owner {
                abstract func missing(): Unit
            }
        """.trimIndent()
        assertBoth(text, listOf(
            diagnostic(text, 2, 5, 34,
                "only common/specific or Native FFI mirror abstract classes can have explicitly abstract function"),
            diagnostic(text, 2, 5, 13, "unexpected modifier 'abstract' on function declaration in class body",
                listOf(
                    hint(text, 2, 5, 9, "on function declaration"),
                    hint(text, 1, 10, 15, "in class"),
                )),
        ))
    }

    /** 合法 common abstract 容器中的 static abstract 无体函数没有 Parse 错误；缺体由后续 Sema 报告。 */
    @Test
    fun legalCommonStaticAbstractFunctionHasNoParserErrors() {
        // SDK 1.1.3 的完整编译结果：20261113-cjmp-extend-static-abstract-authority/cjc-results.json。
        val text = """
            package cjmp_p

            public common abstract class AuthorityCommon {
                public common init()
                public static abstract func absFn(): Unit
            }
        """.trimIndent()
        assertBoth(text, emptyList(), listOf(CjmpParsingContext.Compilation(true, false, true)))
    }

    /** SDK 1.1.3 的局部无体函数停在 Parse，不把它包装成 Sema body 期望或借外层 abstract 豁免。 */
    @Test
    fun localFunctionWithoutBodyReportsNativeParseMissingBody() {
        val text = """
            abstract class LocalOwner {
                public func host(): Unit {
                    func local(): Unit
                }
            }
        """.trimIndent()
        assertBoth(text, listOf(
            diagnostic(text, 3, 27, 27, "body of function 'local' is missing")
                .copy(rangeKind = ParsingDiagnostic.RangeKind.POINT),
        ))
    }

    /** 两入口分别与官方完整期望比较；没有编译角色时普通 parser 规则仍然生效。 */
    private fun assertBoth(
        text: String,
        expected: List<ParsingDiagnostic>,
        contexts: List<CjmpParsingContext> = listOf(
            CjmpParsingContext.SyntaxOnly,
            CjmpParsingContext.Compilation(false, false, true),
        ),
    ) {
        for (context in contexts) {
            val file = createPsiFile("staticModifierDiagnostics", text) as CjFile
            file.cjmpParsingContext = context
            val psiDiagnostics = file.collectParsingDiagnosticPublication().diagnostics
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
            builder.setDebugMode(true)
            val lightDiagnostics = parseLightWithPsiInput(file, builder).diagnostics
            val sourceContext = checkNotNull(publishedParserSeed(file).sourceContext)
            val sortedExpected = sorted(expected)
            val sortedPsi = sorted(psiDiagnostics)
            val sortedLight = sorted(lightDiagnostics)
            assertLegacyParsingDiagnostics(sortedExpected, sortedPsi, sourceContext, "PSI: $context\n$text")
            assertLegacyParsingDiagnostics(sortedExpected, sortedLight, sourceContext, "LightTree: $context\n$text")
            assertPublishedParsingDiagnosticParity(sortedPsi, sortedLight, sourceContext, text)
        }
    }

    /** 官方 parse_conflict_modifier 当前 token 主锚、先前 static 和声明关键字两项 related hints。 */
    private fun conflict(text: String, line: Int, modifier: String, funcColumn: Int): ParsingDiagnostic = diagnostic(
        text, line, 19, 19 + modifier.length,
        "'$modifier' and 'static' modifiers conflict on function declaration",
        listOf(
            hint(text, line, 12, 18, "because it is conflicted with this"),
            hint(text, line, funcColumn, funcColumn + 4, "on function declaration"),
        ),
    )

    /** 官方主范围包含 static 前缀到返回类型末尾，排除行尾空白。 */
    private fun explicitlyAbstract(text: String, line: Int): ParsingDiagnostic = diagnostic(
        text, line, 5, 39,
        "only common/specific or Native FFI mirror abstract classes can have explicitly abstract function",
    )

    /** 官方 illegal-modifier 另锚 abstract；on 提示使用声明起点，in 提示使用外层 class token。 */
    private fun illegalAbstract(text: String, line: Int, classLine: Int): ParsingDiagnostic = diagnostic(
        text, line, 12, 20, "unexpected modifier 'abstract' on function declaration in class body",
        listOf(
            hint(text, line, 5, 9, "on function declaration"),
            hint(text, classLine, 10, 15, "in class"),
        ),
    )

    /** 将权威 JSON 的 ASCII 行列换算为夹具偏移，不使用被测 parser 的定位算法。 */
    private fun offset(text: String, line: Int, column: Int): Int {
        val lines = text.lines()
        require(line in 1..lines.size && column in 1..lines[line - 1].length + 1)
        return lines.take(line - 1).sumOf { it.length + 1 } + column - 1
    }

    private fun diagnostic(
        text: String, line: Int, begin: Int, end: Int, message: String,
        hints: List<ParsingDiagnosticHint> = emptyList(),
    ): ParsingDiagnostic = ParsingDiagnostic(offset(text, line, begin), offset(text, line, end), message, hints = hints)

    private fun hint(text: String, line: Int, begin: Int, end: Int, message: String): ParsingDiagnosticHint =
        ParsingDiagnosticHint(offset(text, line, begin), offset(text, line, end), message)

    private fun sorted(diagnostics: List<ParsingDiagnostic>): List<ParsingDiagnostic> =
        diagnostics.sortedWith(compareBy({ it.startOffset }, { it.endOffset }, { it.message }))
}
