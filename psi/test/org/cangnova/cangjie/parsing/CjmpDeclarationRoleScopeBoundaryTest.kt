package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertTrue as assertValueTrue

/**
 * 声明角色作用域边界的完整文件回归：事件、声明检查结果、文件角色和全部诊断主范围分别断言。
 * 期望来自官方源码及公共诊断资源，不调用生产角色 helper，也不模拟文件角色转移。
 *
 * v1.1.3 ParseMacro.cpp:422、597、617、621–624 / ParseDecl.cpp:1475–1480：
 * ENUM_CONSTRUCTOR 输入的普通函数不执行文件检查；整个非构造项宏 root 报 unknown enum constructor。
 * v1.1.3 Parser.cpp:139–161、300 / ParserDiag.cpp:625–629：包头角色检查不被非法前导注解取消。
 * v1.1.3 ParseModifiers.cpp:79–80 / ParseDecl.cpp:2104–2119：禁体规则依据有效 FOREIGN 属性。
 * v1.1.3 ParseDecl.cpp:1745–1750 / ParserDiag.cpp:692–702：非法 extend 前缀不赋角色，首个 public 报错。
 * v1.1.0-alpha.65 ParseDecl.cpp:1691–1694 / Parser.cpp:447–451 / ParseCJMPDecl.cpp:174–181：
 * single common extend 即使文件检查失败仍保留 COMMON；其泛型禁令不能按文件拒绝结果短路。
 */
class CjmpDeclarationRoleScopeBoundaryTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun enumConstructorMacroFunctionInputDoesNotCheckOrPolluteTheFollowingSpecificFunction() {
        val invocation = "@Wrap common func bad(): Unit {}"
        val source = "enum E { | $invocation }\nspecific func following(): Unit {}"
        for (context in listOf(BOTH_MODES, SPECIFIC_MODE)) {
            val parsed = parseBuffered(source, context)

            assertEvents(parsed, source.indexOf("func following") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS))
            assertChecks(parsed,
                source.indexOf("enum") to CjmpDeclarationRoleCheck.NOT_CHECKED,
                source.indexOf("func bad") to CjmpDeclarationRoleCheck.NOT_CHECKED,
                source.indexOf("func following") to CjmpDeclarationRoleCheck.ACCEPTED,
            )
            assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
            // DiagnosticParser.def:145 的主范围是整个 caseBody，即宏包装，而不是内部 func token。
            assertDiagnostics(parsed, range(source, invocation, UNKNOWN_ENUM_CONSTRUCTOR))
        }
    }

    @Test
    fun enumConstructorMacroConstructorInputKeepsItsOwnNameAnchorWithoutFileCheck() {
        val source = "enum E { | @Wrap A }\nspecific func following(): Unit {}"
        val parsed = parseBuffered(source, SPECIFIC_MODE)

        assertEvents(parsed, source.indexOf("func following") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS))
        assertChecks(parsed,
            source.indexOf("enum") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("@Wrap A") + "@Wrap ".length to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("func following") to CjmpDeclarationRoleCheck.ACCEPTED,
        )
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        assertDiagnostics(parsed)
    }

    /** 官方 ParseMacro.cpp:445–453、621–624：成功括号输入赋枚举构造项属性，内部 tokens 不解析为声明。 */
    @Test
    fun enumConstructorMacroParenthesizedInputDoesNotCheckOrPolluteTheFollowingSpecificFunction() {
        val source = "enum E { | @Wrap(A) }\nspecific func following(): Unit {}"
        val parsed = parseBuffered(source, SPECIFIC_MODE)

        assertEvents(parsed, source.indexOf("func following") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS))
        assertChecks(parsed,
            source.indexOf("enum") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("func following") to CjmpDeclarationRoleCheck.ACCEPTED,
        )
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        assertValueEquals(setOf(source.indexOf("@Wrap")), parsed.file.macroFacts.keys)
        assertValueEquals(
            MacroParsingScope.Declaration(CjScopeKind.ENUM_CONSTRUCTOR),
            parsed.file.macroFacts.getValue(source.indexOf("@Wrap")).scope,
        )
        assertDiagnostics(parsed)
    }

    /** 外层宏继承内层成功括号输入的枚举属性；整个调用链不追加 unknown enum constructor。 */
    @Test
    fun nestedEnumConstructorMacrosPropagateTheSuccessfulParenthesizedInputWithoutFileChecks() {
        val source = "enum E { | @Outer @Inner(A) }\nspecific func following(): Unit {}"
        val parsed = parseBuffered(source, SPECIFIC_MODE)

        assertEvents(parsed, source.indexOf("func following") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS))
        assertChecks(parsed,
            source.indexOf("enum") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("func following") to CjmpDeclarationRoleCheck.ACCEPTED,
        )
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        val occurrences = setOf(source.indexOf("@Outer"), source.indexOf("@Inner"))
        assertValueEquals(occurrences, parsed.file.macroFacts.keys)
        for (occurrence in occurrences) {
            assertValueEquals(
                MacroParsingScope.Declaration(CjScopeKind.ENUM_CONSTRUCTOR),
                parsed.file.macroFacts.getValue(occurrence).scope,
            )
        }
        assertDiagnostics(parsed)
    }

    @Test
    fun annotatedCommonPackageChecksCommonBeforeRejectingTheFollowingSpecificFunction() {
        val source = """
            @C
            common package scope_boundary
            specific func following(): Unit {}
        """.trimIndent()
        val parsed = parseBuffered(source, BOTH_MODES)

        assertEvents(parsed,
            source.indexOf("package") to CjmpRoleEventKind.Check(COMMON_MODIFIERS),
            source.indexOf("func following") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS),
        )
        assertChecks(parsed,
            source.indexOf("package") to CjmpDeclarationRoleCheck.ACCEPTED,
            source.indexOf("func following") to CjmpDeclarationRoleCheck.REJECTED,
        )
        assertValueEquals(CjmpFileRole.COMMON, parsed.file.fileFacts.cjmpRole)
        assertDiagnostics(parsed,
            fileError("parsing.error.cjmp.common.and.specific.in.same.file"),
            range(source, "common", message("parsing.error.cjmp.illegal.modifier", "common")),
            packageAnnotationError(source),
        )
        assertPackageAnnotationHint(parsed, source)
    }

    @Test
    fun annotatedPublicPackageStillChecksItsNonRoleModifierAndAllowsSpecificAfterIt() {
        val source = """
            @C
            public package scope_boundary
            specific func following(): Unit {}
        """.trimIndent()
        val parsed = parseBuffered(source, BOTH_MODES)

        assertEvents(parsed,
            source.indexOf("package") to CjmpRoleEventKind.Check(NO_ROLE_MODIFIERS),
            source.indexOf("func following") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS),
        )
        assertChecks(parsed,
            source.indexOf("package") to CjmpDeclarationRoleCheck.ACCEPTED,
            source.indexOf("func following") to CjmpDeclarationRoleCheck.ACCEPTED,
        )
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        assertDiagnostics(parsed, packageAnnotationError(source))
        assertPackageAnnotationHint(parsed, source)
    }

    @Test
    fun rejectedForeignRoleWithAFunctionBodyDoesNotApplyForeignBodyProhibition() {
        val source = "specific foreign { func f(): Unit {} }"
        val parsed = parseBuffered(source, COMMON_MODE)

        assertEvents(parsed, source.indexOf("func f") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS))
        assertChecks(parsed,
            source.indexOf("foreign") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("func f") to CjmpDeclarationRoleCheck.REJECTED,
        )
        assertValueEquals(CjmpFileRole.NONE, parsed.file.fileFacts.cjmpRole)
        // FOREIGN 和 SPECIFIC 都未进入有效属性；已有真实正文，因此也不报告公共缺体诊断。
        assertDiagnostics(parsed, fileError("parsing.error.cjmp.specific.in.non.specific.file"))
    }

    @Test
    fun acceptedForeignRoleWithAFunctionBodyReportsProhibitionOnTheActualBody() {
        val source = "specific foreign { func f(): Unit {} }"
        val parsed = parseBuffered(source, SPECIFIC_MODE)

        assertEvents(parsed, source.indexOf("func f") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS))
        assertChecks(parsed,
            source.indexOf("foreign") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("func f") to CjmpDeclarationRoleCheck.ACCEPTED,
        )
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        assertDiagnostics(parsed, range(source, "{}", message("parsing.error.foreign.function.no.body")))
    }

    @Test
    fun invalidPublicCommonExtendDoesNotAssignCommonAndReportsItsCommonMembersOuterMismatch() {
        val member = "common func member(): Unit {}"
        val source = "public common extend Int64 {\n    $member\n}"
        val parsed = parseBuffered(source, COMMON_MODE)

        // Extend 事件保留原始前缀候选及去重数量；两种 modifier 不执行文件 Check，也不赋外层角色。
        assertEvents(parsed,
            source.indexOf("extend") to CjmpRoleEventKind.Extend(COMMON_MODIFIERS, 2),
            source.indexOf("func member") to CjmpRoleEventKind.Check(COMMON_MODIFIERS),
        )
        assertChecks(parsed,
            source.indexOf("extend") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("func member") to CjmpDeclarationRoleCheck.ACCEPTED,
        )
        assertValueEquals(CjmpFileRole.COMMON, parsed.file.fileFacts.cjmpRole)
        // public 在官方 Modifier 集合中排在 common 前；非法前缀主范围不能错误锚定后一个 common。
        assertDiagnostics(parsed,
            range(source, "public", message("parsing.error.expected.no.modifier.before", "extend declaration", "public")),
            range(source, member, commonMemberMismatch()),
        )
    }

    @Test
    fun singleCommonExtendAssignsCommonAndAcceptsItsMatchingCommonMember() {
        val member = "common func member(): Unit {}"
        val source = "common extend Int64 {\n    $member\n}"
        val parsed = parseBuffered(source, COMMON_MODE)

        assertEvents(parsed,
            source.indexOf("extend") to CjmpRoleEventKind.Extend(COMMON_MODIFIERS, 1),
            source.indexOf("func member") to CjmpRoleEventKind.Check(COMMON_MODIFIERS),
        )
        assertChecks(parsed,
            source.indexOf("extend") to CjmpDeclarationRoleCheck.ACCEPTED,
            source.indexOf("func member") to CjmpDeclarationRoleCheck.ACCEPTED,
        )
        assertValueEquals(CjmpFileRole.COMMON, parsed.file.fileFacts.cjmpRole)
        assertDiagnostics(parsed)
    }

    @Test
    fun rejectedSingleCommonGenericExtendRetainsCommonForTheAlpha65GenericRule() {
        val source = "common extend<T> Int64 {}"
        val parsed = parseBuffered(source, SPECIFIC_MODE.copy(genericDeclarationsAvailable = false))

        assertEvents(parsed, source.indexOf("extend") to CjmpRoleEventKind.Extend(COMMON_MODIFIERS, 1))
        assertChecks(parsed, source.indexOf("extend") to CjmpDeclarationRoleCheck.REJECTED)
        assertValueEquals(CjmpFileRole.NONE, parsed.file.fileFacts.cjmpRole)
        // 文件角色仍为 NONE，extend 自身却保留 COMMON；历史 generic 诊断覆盖完整声明。
        assertDiagnostics(parsed,
            fileError("parsing.error.cjmp.common.in.non.common.file"),
            range(source, source, message("parsing.error.cjmp.generic.declaration.unsupported", "common")),
        )
    }

    @Test
    fun acceptedSingleCommonGenericExtendStillReportsTheAlpha65GenericRule() {
        val source = "common extend<T> Int64 {}"
        val parsed = parseBuffered(source, COMMON_MODE.copy(genericDeclarationsAvailable = false))

        assertEvents(parsed, source.indexOf("extend") to CjmpRoleEventKind.Extend(COMMON_MODIFIERS, 1))
        assertChecks(parsed, source.indexOf("extend") to CjmpDeclarationRoleCheck.ACCEPTED)
        assertValueEquals(CjmpFileRole.COMMON, parsed.file.fileFacts.cjmpRole)
        assertDiagnostics(parsed, range(source, source, message("parsing.error.cjmp.generic.declaration.unsupported", "common")))
    }

    @Test
    fun currentGenericRuleAllowsTheSameSingleCommonGenericExtend() {
        val source = "common extend<T> Int64 {}"
        val parsed = parseBuffered(source, COMMON_MODE)

        assertEvents(parsed, source.indexOf("extend") to CjmpRoleEventKind.Extend(COMMON_MODIFIERS, 1))
        assertChecks(parsed, source.indexOf("extend") to CjmpDeclarationRoleCheck.ACCEPTED)
        assertValueEquals(CjmpFileRole.COMMON, parsed.file.fileFacts.cjmpRole)
        assertDiagnostics(parsed)
    }

    /** 全部用例使用完整、非惰性的公开入口；错误输入仍必须覆盖文件末尾。 */
    private fun parseBuffered(source: String, context: CjmpParsingContext.Compilation): CangJieLightParseResult {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        builder.setDebugMode(true)
        return CangJieLightParser.parseBuffered(builder, sourceKind = CjSourceKind.SOURCE, cjmpContext = context).also {
            assertValueTrue(it.file.isCompleteFile)
            assertValueEquals(CangJieLightDiagnosticOwnership.BUFFERED, it.file.diagnosticOwnership)
            assertValueEquals(context, it.file.cjmpContext)
            assertValueEquals(0, it.file.tree.root.startOffset)
            assertValueEquals(source.length, it.file.tree.root.endOffset)
            assertValueTrue(it.diagnostics.all { diagnostic ->
                diagnostic.startOffset in 0..diagnostic.endOffset && diagnostic.endOffset <= source.length
            }, it.diagnostics.toString())
        }
    }

    /** 比较整个候选事件序列，重复 Check、错误的额外 Check 和未知区域均不允许混入。 */
    private fun assertEvents(parsed: CangJieLightParseResult, vararg expected: Pair<Int, CjmpRoleEventKind>) {
        assertValueEquals(expected.toList(), parsed.file.roleEvents.map { it.declarationOffset to it.kind })
    }

    private fun assertChecks(parsed: CangJieLightParseResult, vararg expected: Pair<Int, CjmpDeclarationRoleCheck>) {
        assertValueEquals(expected.toMap(), parsed.file.declarationRoleChecks)
    }

    /** 所有诊断都参与比较，逐一核对消息、主范围、归属和重复次数；不按消息筛掉其它错误。 */
    private fun assertDiagnostics(parsed: CangJieLightParseResult, vararg expected: ParsingDiagnostic) {
        val order = compareBy<ParsingDiagnostic>({ it.startOffset }, { it.endOffset }, { it.message }, { it.origin.ordinal })
        assertValueEquals(
            expected.sortedWith(order).map { Triple(it.startOffset to it.endOffset, it.message, it.origin) },
            parsed.diagnostics.sortedWith(order).map { Triple(it.startOffset to it.endOffset, it.message, it.origin) },
            parsed.diagnostics.toString(),
        )
    }

    private fun packageAnnotationError(source: String): ParsingDiagnostic =
        range(source, "@C", message("parsing.error.unexpected.annotation.on", "@C", "package"))

    /** ParserDiag.cpp:629 的辅助范围指向真实 package keyword，显示文本使用既有公共 hint 资源。 */
    private fun assertPackageAnnotationHint(parsed: CangJieLightParseResult, source: String) {
        val diagnostic = parsed.diagnostics.single { it.message == message("parsing.error.unexpected.annotation.on", "@C", "package") }
        val keyword = source.indexOf("package")
        assertValueEquals(
            listOf(ParsingDiagnosticHint(keyword, keyword + "package".length, message("parsing.error.hint.on", "package"))),
            diagnostic.hints,
        )
    }

    private fun commonMemberMismatch(): String = message(
        "parsing.error.cjmp.outdecl.mismatch",
        "${message("parsing.scope.func")} member", "common", message("parsing.scope.extend"), "common",
    )

    private fun range(source: String, syntax: String, diagnosticMessage: String): ParsingDiagnostic {
        val start = source.indexOf(syntax)
        require(start >= 0)
        return ParsingDiagnostic(start, start + syntax.length, diagnosticMessage)
    }

    private fun fileError(messageKey: String): ParsingDiagnostic =
        ParsingDiagnostic(0, 0, message(messageKey), ParsingDiagnostic.Origin.ORIGINAL_FILE)

    private fun message(key: String, vararg arguments: Any): String = CangJieParsingBundle.message(key, *arguments)

    private companion object {
        // 官方 v1.1.3 DiagnosticParser.def:145 的专用错误定义，不用宽泛的 scope 错误替代。
        const val UNKNOWN_ENUM_CONSTRUCTOR = "unknown enum constructor"
        val COMMON_MODIFIERS = CjmpRoleModifiers(true, false, false)
        val SPECIFIC_MODIFIERS = CjmpRoleModifiers(false, true, false)
        val NO_ROLE_MODIFIERS = CjmpRoleModifiers(false, false, false)
        val BOTH_MODES = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = true, enabled = true)
        val COMMON_MODE = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = false, enabled = true)
        val SPECIFIC_MODE = CjmpParsingContext.Compilation(compileCommon = false, compileSpecific = true, enabled = true)
    }
}
