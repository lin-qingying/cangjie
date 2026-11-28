package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertTrue as assertValueTrue

/**
 * 真实声明 owner 的 v1.1.3 语义回归，全部经完整文件 parseBuffered 观察诊断和文件状态。
 * 官方行号来自 git show v1.1.3:src/Parse/<文件>，期望不从本仓库的检查器或角色转移实现生成。
 *
 * ParseDecl.cpp:1014–1025：secondary 预置 COMMON 不随 CheckDeclModifiers 返回空属性集撤销。
 * ParseDecl.cpp:1078–1092：primary 不预置 COMMON，缺体检查使用检查后的有效属性。
 * ParseModifiers.cpp:54–82：UNKNOWN_SCOPE 跳过文件检查，非空规则先检查文件角色再检查目标。
 * Parser.cpp:300、338–345、391–406：包头检查、foreign 前缀逐声明克隆、缺属性名提前返回。
 * ParseMacro.cpp:422 / ParserImpl.h:560：宏原子的声明输入保持显式 UNKNOWN_SCOPE。
 *
 * 实际 cjc 1.1.3 证据：.workbuddy/tmp/cjmp_probe113/review-20261106/local-role/
 * cjc-expanded-results-20261107.json 的 init-default-body / primary-default-body；
 * 前者同时报告文件混用和 constructor init 的外层 COMMON mismatch，后者没有该 mismatch。
 */
class CjmpDeclarationRoleOwnerSemanticsTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun rejectedSecondaryRetainsCommonAndReportsOuterMismatchAfterDefaultAndBodyLocals() {
        val source = constructorWithDefaultAndBodyLocals("class", "init")
        val parsed = parseBuffered(source)
        val mismatch = commonMismatch("parsing.scope.constructor", "init", "class")

        assertRoleEvents(parsed,
            source.indexOf("func inDefault") to SPECIFIC_MODIFIERS,
            source.indexOf("func inBody") to SPECIFIC_MODIFIERS,
            source.indexOf("init(") to COMMON_MODIFIERS,
        )
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("func inDefault")])
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("func inBody")])
        assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.file.declarationRoleChecks[source.indexOf("init(")])
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        assertFirstFileError(parsed, "parsing.error.cjmp.common.and.specific.in.same.file")
        assertMessages(parsed, mixedFileMessage, mismatch, illegalModifier("specific"), illegalModifier("specific"))
        // cjc MainHint 为第 4 行第 5 列至第 6 行第 6 列，覆盖 common 前缀和完整构造器。
        assertRanges(parsed, mismatch, source.indexOf("common init") to source.lastIndexOf("\n}"))
        assertRanges(parsed, illegalModifier("specific"),
            modifierRange(source, "specific func inDefault", "specific"),
            modifierRange(source, "specific func inBody", "specific"),
        )
    }

    @Test
    fun rejectedPrimaryDoesNotRestoreCommonFromItsSourcePrefix() {
        val source = constructorWithDefaultAndBodyLocals("struct", "Owner")
        val parsed = parseBuffered(source)

        assertRoleEvents(parsed,
            source.indexOf("func inDefault") to SPECIFIC_MODIFIERS,
            source.indexOf("func inBody") to SPECIFIC_MODIFIERS,
            source.indexOf("Owner(") to COMMON_MODIFIERS,
        )
        assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.file.declarationRoleChecks[source.indexOf("Owner(")])
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        assertFirstFileError(parsed, "parsing.error.cjmp.common.and.specific.in.same.file")
        // 与实际 primary-default-body JSON 一致：只有文件混用及两个局部非法 specific，没有外层 mismatch。
        assertMessages(parsed, mixedFileMessage, illegalModifier("specific"), illegalModifier("specific"))
        assertRanges(parsed, illegalModifier("specific"),
            modifierRange(source, "specific func inDefault", "specific"),
            modifierRange(source, "specific func inBody", "specific"),
        )
    }

    @Test
    fun bodyLocalAloneRejectsTheOwnerWhileOnlySecondaryKeepsCommon() {
        for (nominal in listOf("class", "struct")) {
            for (constructor in listOf("init", "Owner")) {
                val source = """
                    $nominal Owner {
                        common $constructor() {
                            specific func local(): Unit {}
                        }
                    }
                """.trimIndent()
                val parsed = parseBuffered(source)

                assertRoleEvents(parsed,
                    source.indexOf("func local") to SPECIFIC_MODIFIERS,
                    source.indexOf("$constructor(") to COMMON_MODIFIERS,
                )
                assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("func local")])
                assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.file.declarationRoleChecks[source.indexOf("$constructor(")])
                assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
                assertFirstFileError(parsed, "parsing.error.cjmp.common.and.specific.in.same.file")
                if (constructor == "init") {
                    val mismatch = commonMismatch("parsing.scope.constructor", "init", nominal)
                    assertMessages(parsed, mixedFileMessage, illegalModifier("specific"), mismatch)
                    assertRanges(parsed, mismatch, source.indexOf("common init") to source.lastIndexOf("\n}"))
                } else {
                    assertMessages(parsed, mixedFileMessage, illegalModifier("specific"))
                }
                assertRanges(parsed, illegalModifier("specific"), modifierRange(source, "specific func local", "specific"))
            }
        }
    }

    @Test
    fun macroAtomInTopLevelInitializerKeepsUnknownScopeForItsDeclarationInput() {
        val source = """
            let value = @Wrap common func target(): Unit {}
            specific func following(): Unit {}
        """.trimIndent()
        val parsed = parseBuffered(source)

        assertRoleEvents(parsed, source.indexOf("func following") to SPECIFIC_MODIFIERS)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.file.declarationRoleChecks[source.indexOf("let value")])
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.file.declarationRoleChecks[source.indexOf("func target")])
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("func following")])
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        // UNKNOWN_SCOPE 不报告输入 common 的非法目标，也不能使 following 遇到文件混用。
        assertMessages(parsed)
    }

    @Test
    fun macroAtomInFunctionBodyInitializerKeepsUnknownScopeForItsDeclarationInput() {
        val source = """
            func outer(): Unit {
                let value = @Wrap common func target(): Unit {}
            }
            specific func following(): Unit {}
        """.trimIndent()
        val parsed = parseBuffered(source)

        assertRoleEvents(parsed, source.indexOf("func following") to SPECIFIC_MODIFIERS)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.file.declarationRoleChecks[source.indexOf("func outer")])
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.file.declarationRoleChecks[source.indexOf("let value")])
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.file.declarationRoleChecks[source.indexOf("func target")])
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("func following")])
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        assertMessages(parsed)
    }

    @Test
    fun rejectedForeignPrefixChecksEveryInnerFunctionAndKeepsTheFirstFileError() {
        val source = foreignSignatures()
        val parsed = parseBuffered(source, COMMON_MODE)

        assertRoleEvents(parsed,
            source.indexOf("func first") to SPECIFIC_MODIFIERS,
            source.indexOf("func second") to SPECIFIC_MODIFIERS,
        )
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.file.declarationRoleChecks[source.indexOf("foreign")])
        assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.file.declarationRoleChecks[source.indexOf("func first")])
        assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.file.declarationRoleChecks[source.indexOf("func second")])
        assertValueEquals(CjmpFileRole.NONE, parsed.file.fileFacts.cjmpRole)
        assertFirstFileError(parsed, "parsing.error.cjmp.specific.in.non.specific.file")
        // ParseModifiers.cpp:79–80 返回空属性；ParseDecl.cpp:2094–2100 随后对两个签名分别报告缺体。
        assertMessages(parsed, specificInNonSpecificMessage, missingBodyMessage, missingBodyMessage)
        assertValueEquals(2, parsed.diagnostics.count { it.origin == ParsingDiagnostic.Origin.INPUT && it.message == missingBodyMessage })
    }

    @Test
    fun acceptedForeignPrefixIsInheritedByEachSignatureAndSuppressesMissingBodies() {
        val source = foreignSignatures()
        val parsed = parseBuffered(source, SPECIFIC_MODE)

        assertRoleEvents(parsed,
            source.indexOf("func first") to SPECIFIC_MODIFIERS,
            source.indexOf("func second") to SPECIFIC_MODIFIERS,
        )
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("func first")])
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("func second")])
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
        assertMessages(parsed)
    }

    @Test
    fun publicPackageRunsAFileCheckWithAllRolePredicatesFalse() {
        val source = """
            public package owner_semantics
            func plain(): Unit {}
        """.trimIndent()
        val parsed = parseBuffered(source)

        assertRoleEvents(parsed, source.indexOf("package") to NO_ROLE_MODIFIERS)
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("package")])
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.file.declarationRoleChecks[source.indexOf("func plain")])
        assertValueEquals(CjmpFileRole.NONE, parsed.file.fileFacts.cjmpRole)
        assertMessages(parsed)
    }

    @Test
    fun illegalCommonPackageCommitsCommonBeforeTheFollowingSpecificFunctionIsRejected() {
        val source = """
            common package owner_semantics
            specific func following(): Unit {}
        """.trimIndent()
        val parsed = parseBuffered(source)

        assertRoleEvents(parsed,
            source.indexOf("package") to COMMON_MODIFIERS,
            source.indexOf("func following") to SPECIFIC_MODIFIERS,
        )
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("package")])
        assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.file.declarationRoleChecks[source.indexOf("func following")])
        assertValueEquals(CjmpFileRole.COMMON, parsed.file.fileFacts.cjmpRole)
        assertFirstFileError(parsed, "parsing.error.cjmp.common.and.specific.in.same.file")
        assertMessages(parsed, mixedFileMessage, illegalModifier("common"))
        assertRanges(parsed, illegalModifier("common"), 0 to "common".length)
    }

    @Test
    fun brokenCommonPropertyNeverChecksOrPollutesTheFollowingSpecificDeclaration() {
        val source = """
            class Owner {
                common prop : Int64
            }
            specific func following(): Unit {}
        """.trimIndent()
        for (context in listOf(BOTH_MODES, SPECIFIC_MODE)) {
            val parsed = parseBuffered(source, context)

            assertRoleEvents(parsed, source.indexOf("func following") to SPECIFIC_MODIFIERS)
            assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.file.declarationRoleChecks[source.indexOf("prop")])
            assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("func following")])
            assertValueEquals(CjmpFileRole.SPECIFIC, parsed.file.fileFacts.cjmpRole)
            assertValueEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics.filter { it.origin == ParsingDiagnostic.Origin.ORIGINAL_FILE })
            // 缺名字仍须报告真正的语法错误；ParsePropDecl 的提前返回只取消角色检查。
            assertValueTrue(parsed.diagnostics.isNotEmpty())
            assertValueTrue(parsed.diagnostics.all {
                it.origin == ParsingDiagnostic.Origin.INPUT && it.startOffset == source.indexOf(": Int64")
            }, parsed.diagnostics.toString())
        }
    }

    @Test
    fun rejectedCommonPrimaryWithoutBodyReportsMissingBodyUsingPostCheckAttributes() {
        for (nominal in listOf("class", "struct")) {
            val source = "$nominal Owner { common Owner() }"
            val parsed = parseBuffered(source, SPECIFIC_MODE)

            assertRoleEvents(parsed, source.indexOf("Owner(") to COMMON_MODIFIERS)
            assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.file.declarationRoleChecks[source.indexOf("Owner(")])
            assertValueEquals(CjmpFileRole.NONE, parsed.file.fileFacts.cjmpRole)
            assertFirstFileError(parsed, "parsing.error.cjmp.common.in.non.common.file")
            val primaryMissingBody = CangJieParsingBundle.message("parsing.error.body.missing", "main constructor", " 'Owner'")
            assertMessages(parsed, commonInNonCommonMessage, primaryMissingBody)
            val missing = parsed.diagnostics.single { it.origin == ParsingDiagnostic.Origin.INPUT && it.message == primaryMissingBody }
            val anchor = source.indexOf("Owner()") + "Owner()".length
            assertValueEquals(anchor to anchor, missing.startOffset to missing.endOffset)
            assertValueEquals(ParsingDiagnostic.Category.PARSE, missing.category)
            assertValueEquals(ParsingDiagnostic.RangeKind.POINT, missing.rangeKind)
            val native = requireNotNull(missing.mainRange)
            val begin = parsed.file.sourceContext.snapshot.nativePosition(anchor)
            assertValueTrue(native.source === parsed.file.sourceContext.snapshot)
            assertValueEquals(begin, native.begin)
            assertValueEquals(begin.first to begin.second + 1, native.end)
            val hint = missing.nativeHints.single()
            assertValueEquals(native, hint.range)
            assertValueEquals(CangJieParsingBundle.message("parsing.hint.body.missing", "main constructor"), hint.label)
        }
    }

    @Test
    fun acceptedCommonPrimaryWithoutBodyKeepsCommonDespiteItsIllegalModifierTarget() {
        for (nominal in listOf("class", "struct")) {
            val source = "common $nominal Owner { common Owner() }"
            val parsed = parseBuffered(source, COMMON_MODE)

            assertRoleEvents(parsed,
                source.indexOf(nominal) to COMMON_MODIFIERS,
                source.indexOf("Owner(") to COMMON_MODIFIERS,
            )
            assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("Owner(")])
            assertValueEquals(CjmpFileRole.COMMON, parsed.file.fileFacts.cjmpRole)
            // v1.1.3 的 primary 表不允许 common；GetModifierAttrs 对非法目标仍返回对应 COMMON 属性。
            assertMessages(parsed, illegalModifier("common"))
            val modifier = source.lastIndexOf("common")
            assertRanges(parsed, illegalModifier("common"), modifier to modifier + "common".length)
        }
    }

    @Test
    fun rejectedCommonSecondaryWithoutBodyKeepsItsPreassignedCommonAndOuterMismatch() {
        for (nominal in listOf("class", "struct")) {
            val source = "$nominal Owner { common init() }"
            val parsed = parseBuffered(source, SPECIFIC_MODE)
            val mismatch = commonMismatch("parsing.scope.constructor", "init", nominal)

            assertRoleEvents(parsed, source.indexOf("init(") to COMMON_MODIFIERS)
            assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, parsed.file.declarationRoleChecks[source.indexOf("init(")])
            assertValueEquals(CjmpFileRole.NONE, parsed.file.fileFacts.cjmpRole)
            assertFirstFileError(parsed, "parsing.error.cjmp.common.in.non.common.file")
            assertMessages(parsed, commonInNonCommonMessage, mismatch)
            assertRanges(parsed, mismatch, source.indexOf("common init") to source.indexOf(")") + 1)
        }
    }

    @Test
    fun acceptedCommonSecondaryWithoutBodyHasNeitherMissingBodyNorOuterMismatch() {
        for (nominal in listOf("class", "struct")) {
            val source = "common $nominal Owner { common init() }"
            val parsed = parseBuffered(source, COMMON_MODE)

            assertRoleEvents(parsed,
                source.indexOf(nominal) to COMMON_MODIFIERS,
                source.indexOf("init(") to COMMON_MODIFIERS,
            )
            assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.file.declarationRoleChecks[source.indexOf("init(")])
            assertValueEquals(CjmpFileRole.COMMON, parsed.file.fileFacts.cjmpRole)
            assertMessages(parsed)
        }
    }

    /** 与现有两个 cjc 探针保持相同源码和列位置，不以 parser 的角色事件生成诊断期望。 */
    private fun constructorWithDefaultAndBodyLocals(nominal: String, constructor: String): String = """
        package cjmp_local_role

        $nominal Owner {
            common $constructor(cb!: () -> Unit = { => specific func inDefault(): Unit {} }) {
                specific func inBody(): Unit {}
            }
        }
    """.trimIndent()

    private fun foreignSignatures(): String = """
        specific foreign {
            func first(): Unit
            func second(): Unit
        }
    """.trimIndent()

    /** 只调用完整文件公开入口，检查输入覆盖；不直接调用角色 helper 或模拟转移。 */
    private fun parseBuffered(
        source: String,
        context: CjmpParsingContext.Compilation = BOTH_MODES,
    ): CangJieLightParseResult {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        builder.setDebugMode(true)
        return CangJieLightParser.parseBuffered(builder, sourceKind = CjSourceKind.SOURCE, cjmpContext = context).also {
            assertValueTrue(it.file.isCompleteFile)
            assertValueEquals(CangJieLightDiagnosticOwnership.BUFFERED, it.file.diagnosticOwnership)
            assertValueEquals(0, it.file.tree.root.startOffset)
            assertValueEquals(source.length, it.file.tree.root.endOffset)
            assertValueTrue(it.diagnostics.all { diagnostic ->
                diagnostic.startOffset in 0..diagnostic.endOffset && diagnostic.endOffset <= source.length
            }, it.diagnostics.toString())
        }
    }

    /** 对整个事件序列断言，额外 Check、重复注册或未知惰性区都会失败。 */
    private fun assertRoleEvents(parsed: CangJieLightParseResult, vararg expected: Pair<Int, CjmpRoleModifiers>) {
        assertValueEquals(
            expected.map { (offset, modifiers) -> offset to CjmpRoleEventKind.Check(modifiers) },
            parsed.file.roleEvents.map { it.declarationOffset to it.kind },
        )
    }

    /** 比较完整诊断多重集，保留同消息的重复次数；首个文件错误单独按归属验证。 */
    private fun assertMessages(parsed: CangJieLightParseResult, vararg expected: String) {
        assertValueEquals(expected.sorted(), parsed.diagnostics.map { it.message }.sorted(), parsed.diagnostics.toString())
    }

    private fun assertFirstFileError(parsed: CangJieLightParseResult, messageKey: String) {
        val errors = parsed.diagnostics.filter { it.origin == ParsingDiagnostic.Origin.ORIGINAL_FILE }
        assertValueEquals(1, errors.size, parsed.diagnostics.toString())
        assertValueEquals(CangJieParsingBundle.message(messageKey), errors.single().message)
        assertValueEquals(0, errors.single().startOffset)
    }

    private fun assertRanges(parsed: CangJieLightParseResult, message: String, vararg expected: Pair<Int, Int>) {
        val diagnostics = parsed.diagnostics.filter { it.message == message }
        assertValueEquals(expected.sortedBy { it.first }, diagnostics.map { it.startOffset to it.endOffset }.sortedBy { it.first })
        assertValueTrue(diagnostics.all { it.origin == ParsingDiagnostic.Origin.INPUT })
    }

    private fun modifierRange(source: String, occurrence: String, spelling: String): Pair<Int, Int> {
        val start = source.indexOf(occurrence)
        require(start >= 0)
        return start to start + spelling.length
    }

    private fun illegalModifier(spelling: String): String =
        CangJieParsingBundle.message("parsing.error.cjmp.illegal.modifier", spelling)

    private fun commonMismatch(kindKey: String, name: String, nominal: String): String =
        CangJieParsingBundle.message(
            "parsing.error.cjmp.outdecl.mismatch",
            "${CangJieParsingBundle.message(kindKey)} $name",
            "common",
            CangJieParsingBundle.message("parsing.scope.$nominal"),
            "common",
        )

    private val mixedFileMessage: String
        get() = CangJieParsingBundle.message("parsing.error.cjmp.common.and.specific.in.same.file")

    private val commonInNonCommonMessage: String
        get() = CangJieParsingBundle.message("parsing.error.cjmp.common.in.non.common.file")

    private val specificInNonSpecificMessage: String
        get() = CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file")

    private val missingBodyMessage: String
        get() = CangJieParsingBundle.message("parsing.error.function.body.expected")

    private companion object {
        val COMMON_MODIFIERS = CjmpRoleModifiers(true, false, false)
        val SPECIFIC_MODIFIERS = CjmpRoleModifiers(false, true, false)
        val NO_ROLE_MODIFIERS = CjmpRoleModifiers(false, false, false)
        val BOTH_MODES = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = true, enabled = true)
        val COMMON_MODE = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = false, enabled = true)
        val SPECIFIC_MODE = CjmpParsingContext.Compilation(compileCommon = false, compileSpecific = true, enabled = true)
    }
}
