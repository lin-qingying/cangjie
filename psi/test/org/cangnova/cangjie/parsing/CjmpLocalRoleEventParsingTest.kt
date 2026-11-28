package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 官方 v1.1.3 的局部声明角色检查契约，期望不依赖当前 PSI 实现。
 * ParseModifiers.cpp:50–82 先检查 CJMP，再检查修饰符是否合法；局部函数规则表并非空表。
 * ParseDecl.cpp:283–284 先解析普通变量初始化式，再检查变量修饰符。
 * ParseDecl.cpp:167–181 先检查模式变量修饰符，再解析模式和初始化式。
 * 官方 cjc 1.1.3 探针及诊断记录位于 .workbuddy/tmp/cjmp_probe113/review-20261106/local-role。
 */
class CjmpLocalRoleEventParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun localUnsafeFunctionChecksFileRoleWithoutRoleModifiers() {
        val source = """
            func outer(): Unit {
                unsafe func local(): Unit {}
            }
        """.trimIndent()
        val outer = source.indexOf("func outer")
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // unsafe 是合法局部修饰符；非空修饰符集合仍调用 CheckCJMPModifiers。
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, false, false)),
        ))
        assertEquals(mapOf(
            outer to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.ACCEPTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.NONE, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun illegalLocalCommonModifierStillRecordsRoleCheck() {
        val source = """
            func outer(): Unit {
                common func local(): Unit {}
            }
        """.trimIndent()
        val outer = source.indexOf("func outer")
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // 官方报告 parse_illegal_modifier_in_scope，但在此之前已接受 common 角色检查。
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
        ))
        assertEquals(mapOf(
            outer to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.ACCEPTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.COMMON, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun illegalLocalSpecificModifierStillRecordsRoleCheck() {
        val source = """
            func outer(): Unit {
                specific func local(): Unit {}
            }
        """.trimIndent()
        val outer = source.indexOf("func outer")
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // FUNC_BODY_FUNCDECL_MODIFIERS 只允许 const/unsafe；非法 specific 仍先更新文件角色。
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            outer to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.ACCEPTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun simpleTupleVariableChecksAfterLocalSpecificFunctionInInitializer() {
        val source = "common let pair = ({ => specific func local(): Unit {} }, ())"
        val variable = source.indexOf("let pair")
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // 初始化值是元组，不会使单名声明变成模式变量；仍走 ParseVarDecl 的后置检查。
        assertTrue(variable < local)
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
            variable to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
        ))
        assertEquals(mapOf(
            local to CjmpDeclarationRoleCheck.ACCEPTED,
            variable to CjmpDeclarationRoleCheck.REJECTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun tuplePatternVariableChecksBeforeLocalSpecificFunctionInInitializer() {
        val source = "common let (cb, unit) = ({ => specific func local(): Unit {} }, ())"
        val variable = source.indexOf("let (")
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // 与单名变量使用相同初始化式；模式声明在 ParseVarWithPatternDecl:167 先检查。
        // common 元组模式是官方负例，后续 parse_cjmp_pattern_decl 不撤销已接受的角色检查。
        assertTrue(variable < local)
        assertRoleEvents(parsed, listOf(
            variable to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            variable to CjmpDeclarationRoleCheck.ACCEPTED,
            local to CjmpDeclarationRoleCheck.REJECTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.COMMON, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun ordinaryFunctionAndLambdaParametersDoNotProduceRoleEvents() {
        val source = """
            func outer(value: Int64): Unit {
                let cb = { argument: Int64 => unsafe func local(): Unit {} }
            }
        """.trimIndent()
        val outer = source.indexOf("func outer")
        val variable = source.indexOf("let cb")
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // 普通参数不走成员变量参数角色检查；lambda 的 ParseFuncParam 不检查声明修饰符。
        // outer 和 cb 的空修饰符集合只记录 NOT_CHECKED；实际事件仅来自局部 unsafe。
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, false, false)),
        ))
        assertEquals(mapOf(
            outer to CjmpDeclarationRoleCheck.NOT_CHECKED,
            variable to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.ACCEPTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.NONE, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun simpleCommonVariableChecksAfterLocalSpecificFunctionInLambdaInitializer() {
        val source = "common let cb = { => specific func local(): Unit {} }"
        val variable = source.indexOf("let cb")
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // 事件按检查执行顺序排列，不能按源码偏移排序；无参数 lambda 不产生参数事件。
        assertTrue(variable < local)
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
            variable to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
        ))
        assertEquals(mapOf(
            local to CjmpDeclarationRoleCheck.ACCEPTED,
            variable to CjmpDeclarationRoleCheck.REJECTED,
        ), parsed.declarationRoleChecks)
        // common 检查报同文件角色混用并返回 false，不清除先前局部函数写入的 SPECIFIC。
        assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun classAndStructFieldsCheckAfterTheirInitializerLocalFunction() {
        for (kind in listOf("class", "struct")) {
            val source = "$kind Owner { common let field = { => specific func local(): Unit {} } }"
            val field = source.indexOf("let field")
            val local = source.indexOf("func local")
            val parsed = parse(source)

            // 普通成员字段仍走 ParseVarDecl:283→284，容器的空修饰符不生成事件。
            assertRoleEvents(parsed, listOf(
                local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
                field to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
            ))
            assertEquals(mapOf(
                source.indexOf(kind) to CjmpDeclarationRoleCheck.NOT_CHECKED,
                local to CjmpDeclarationRoleCheck.ACCEPTED,
                field to CjmpDeclarationRoleCheck.REJECTED,
            ), parsed.declarationRoleChecks)
            assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
        }
    }

    @Test
    fun ordinaryAndPrimaryConstructorsCheckAfterDefaultsAndBodies() {
        for (kind in listOf("class", "struct")) {
            for (constructor in listOf("init", "Owner")) {
                val source = "$kind Owner { " + constructorWithLocals(constructor) + " }"
                val owner = source.indexOf("$constructor(")
                val inDefault = source.indexOf("func inDefault")
                val inBody = source.indexOf("func inBody")
                val parsed = parse(source)

                // ParseConstructor:1019→1021、ParsePrimaryConstructor:1068/1073→1078。
                assertRoleEvents(parsed, constructorEvents(owner, inDefault, inBody))
                assertEquals(mapOf(
                    source.indexOf(kind) to CjmpDeclarationRoleCheck.NOT_CHECKED,
                    inDefault to CjmpDeclarationRoleCheck.ACCEPTED,
                    inBody to CjmpDeclarationRoleCheck.ACCEPTED,
                    owner to CjmpDeclarationRoleCheck.REJECTED,
                ), parsed.declarationRoleChecks)
                assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
            }
        }
    }

    @Test
    fun ordinaryFunctionChecksItsOwnCommonBeforeParameterDefaultLocal() {
        val source = functionWithDefaultLocal()
        val owner = source.indexOf("func outer")
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // ParseFuncDecl:2023→2036，默认值在 ParseAssignInParam 中才调用 ParseExpr。
        assertRoleEvents(parsed, listOf(
            owner to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            owner to CjmpDeclarationRoleCheck.ACCEPTED,
            local to CjmpDeclarationRoleCheck.REJECTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.COMMON, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun extendVariableWithoutScopeRulesStillChecksItsInitializerLocal() {
        val source = "extend Int64 { common let field = { => specific func local(): Unit {} } }"
        val parsed = parse(source)
        val local = source.indexOf("func local")

        // VARIABLE/EXTEND_BODY 不在官方规则表中；非法变量声明仍解析初始化式。
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            source.indexOf("extend") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("let field") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.ACCEPTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun finalizerWithEmptyScopeRulesStillChecksItsBodyLocal() {
        val source = "class Owner { common ~init() { specific func local(): Unit {} } }"
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // FINALIZER/CLASS_BODY 是空表；common 非法，但不进入 CheckCJMPModifiers。
        // 官方 keywordPos 在跳过 ~ 后的 init token 上（ParseFinalizer:967–969）。
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            source.indexOf("class") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            source.indexOf("init()") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.ACCEPTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun quoteTemplatesDoNotParseDeclarationRoleChecks() {
        val source = "import std.ast.*\nfunc tokens(): Tokens { " +
            "quote(common func template(): Unit {} specific func other(): Unit {}) }"
        val parsed = parse(source)

        // ParseQuoteTokens 收集 TokenPart，不把模板中的声明交给 ParseDecl。
        assertRoleEvents(parsed, emptyList())
        assertEquals(mapOf(
            source.indexOf("func tokens") to CjmpDeclarationRoleCheck.NOT_CHECKED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.NONE, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun quoteInterpolationLambdaChecksItsRealSpecificLocalDeclaration() {
        val expression = quoteInterpolationExpression("specific")
        val source = "import std.ast.*\nfunc tokens(): Tokens { $expression }"
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // $() 调用 ParseExpr；lambda 立即调用并返回 Tokens，模板 common 不参与检查。
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            source.indexOf("func tokens") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.ACCEPTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun legalQuoteInterpolationUnsafeLocalStillChecksWithoutRoleInput() {
        val expression = quoteInterpolationExpression("unsafe")
        val source = "import std.ast.*\nfunc tokens(): Tokens { $expression }"
        val local = source.indexOf("func local")
        val parsed = parse(source)

        // 官方 cjc 编译成功的对照：插值中的非角色修饰符仍触发真实局部 Check。
        assertRoleEvents(parsed, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, false, false)),
        ))
        assertEquals(mapOf(
            source.indexOf("func tokens") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.ACCEPTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.NONE, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun scopedClassAndStructFieldInitializersPreservePostCheckOrder() {
        val source = "common let field = { => specific func local(): Unit {} }"
        val field = source.indexOf("let field")
        val local = source.indexOf("func local")
        for (scope in listOf(CjScopeKind.CLASS_BODY, CjScopeKind.STRUCT_BODY)) {
            val parsed = parseFragment(source, MacroParsingScope.Declaration(scope), outerIdent = "Owner")
            assertRoleEvents(parsed.roleEvents, listOf(
                local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
                field to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
            ))
            assertEquals(mapOf(
                local to CjmpDeclarationRoleCheck.ACCEPTED,
                field to CjmpDeclarationRoleCheck.REJECTED,
            ), parsed.declarationRoleChecks)
            assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
        }
    }

    @Test
    fun scopedOrdinaryAndPrimaryConstructorsPreservePostCheckOrder() {
        for (scope in listOf(CjScopeKind.CLASS_BODY, CjScopeKind.STRUCT_BODY)) {
            for (constructor in listOf("init", "Owner")) {
                val source = constructorWithLocals(constructor)
                val owner = source.indexOf("$constructor(")
                val inDefault = source.indexOf("func inDefault")
                val inBody = source.indexOf("func inBody")
                val parsed = parseFragment(source, MacroParsingScope.Declaration(scope), outerIdent = "Owner")
                assertRoleEvents(parsed.roleEvents, constructorEvents(owner, inDefault, inBody))
                assertEquals(mapOf(
                    inDefault to CjmpDeclarationRoleCheck.ACCEPTED,
                    inBody to CjmpDeclarationRoleCheck.ACCEPTED,
                    owner to CjmpDeclarationRoleCheck.REJECTED,
                ), parsed.declarationRoleChecks)
                assertEquals(CjmpFileRole.SPECIFIC, parsed.fileFacts.cjmpRole)
            }
        }
    }

    @Test
    fun scopedOrdinaryFunctionsPreserveOwnCheckBeforeDefaultLocal() {
        val source = functionWithDefaultLocal()
        val owner = source.indexOf("func outer")
        val local = source.indexOf("func local")
        for (scope in listOf(CjScopeKind.TOPLEVEL, CjScopeKind.CLASS_BODY, CjScopeKind.STRUCT_BODY, CjScopeKind.FUNC_BODY)) {
            val parsed = parseFragment(source, MacroParsingScope.Declaration(scope))
            assertRoleEvents(parsed.roleEvents, listOf(
                owner to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
                local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
            ))
            assertEquals(mapOf(
                owner to CjmpDeclarationRoleCheck.ACCEPTED,
                local to CjmpDeclarationRoleCheck.REJECTED,
            ), parsed.declarationRoleChecks)
            assertEquals(CjmpFileRole.COMMON, parsed.fileFacts.cjmpRole)
        }
    }

    @Test
    fun scopedMissingAndEmptyScopeRulesDoNotCreateOwnerChecks() {
        val fieldSource = "common let field = { => specific func local(): Unit {} }"
        val field = parseFragment(fieldSource, MacroParsingScope.Declaration(CjScopeKind.EXTEND_BODY))
        assertRoleEvents(field.roleEvents, listOf(
            fieldSource.indexOf("func local") to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            fieldSource.indexOf("let field") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            fieldSource.indexOf("func local") to CjmpDeclarationRoleCheck.ACCEPTED,
        ), field.declarationRoleChecks)
        assertEquals(CjmpFileRole.SPECIFIC, field.fileFacts.cjmpRole)

        val finalizerSource = "common ~init() { specific func local(): Unit {} }"
        val finalizer = parseFragment(finalizerSource, MacroParsingScope.Declaration(CjScopeKind.CLASS_BODY), outerIdent = "Owner")
        assertRoleEvents(finalizer.roleEvents, listOf(
            finalizerSource.indexOf("func local") to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            finalizerSource.indexOf("init()") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            finalizerSource.indexOf("func local") to CjmpDeclarationRoleCheck.ACCEPTED,
        ), finalizer.declarationRoleChecks)
        assertEquals(CjmpFileRole.SPECIFIC, finalizer.fileFacts.cjmpRole)
    }

    @Test
    fun scopedQuoteInterpolationChecksAgainstActualSeedRole() {
        val expression = quoteInterpolationExpression("specific")
        val source = "let q = $expression"
        val local = source.indexOf("func local")
        val parsed = parseFragment(source, MacroParsingScope.Declaration(CjScopeKind.FUNC_BODY), entryRole = CjmpFileRole.COMMON)

        // 原文件已为 COMMON：模板不修改角色，真实插值局部 specific 被拒绝。
        assertRoleEvents(parsed.roleEvents, listOf(
            local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        ))
        assertEquals(mapOf(
            source.indexOf("let q") to CjmpDeclarationRoleCheck.NOT_CHECKED,
            local to CjmpDeclarationRoleCheck.REJECTED,
        ), parsed.declarationRoleChecks)
        assertEquals(CjmpFileRole.COMMON, parsed.fileFacts.cjmpRole)
    }

    @Test
    fun macroExpressionFragmentUsesItsExplicitEntryRoleForLambdaLocal() {
        val source = "{ argument: Int64 => specific func local(): Unit {} }"
        val local = source.indexOf("func local")
        for (entryRole in listOf(CjmpFileRole.NONE, CjmpFileRole.COMMON)) {
            val parsed = parseFragment(source, MacroParsingScope.Expression(CjExprKind.VAR_INIT), entryRole = entryRole)
            assertRoleEvents(parsed.roleEvents, listOf(
                local to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
            ))
            val expectedCheck = if (entryRole == CjmpFileRole.NONE) CjmpDeclarationRoleCheck.ACCEPTED else CjmpDeclarationRoleCheck.REJECTED
            assertEquals(mapOf(local to expectedCheck), parsed.declarationRoleChecks)
            assertEquals(if (entryRole == CjmpFileRole.NONE) CjmpFileRole.SPECIFIC else CjmpFileRole.COMMON, parsed.fileFacts.cjmpRole)
        }
    }

    /** 普通/主构造器共享同一实际默认值及 body，只改变官方构造器入口拼写。 */
    private fun constructorWithLocals(name: String): String =
        "common $name(cb!: () -> Unit = { => specific func inDefault(): Unit {} }) { specific func inBody(): Unit {} }"

    private fun constructorEvents(owner: Int, inDefault: Int, inBody: Int): List<Pair<Int, CjmpRoleEventKind>> = listOf(
        inDefault to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        inBody to CjmpRoleEventKind.Check(CjmpRoleModifiers(false, true, false)),
        owner to CjmpRoleEventKind.Check(CjmpRoleModifiers(true, false, false)),
    )

    private fun functionWithDefaultLocal(): String =
        "common func outer(cb!: () -> Unit = { => specific func local(): Unit {} }): Unit {}"

    /** 立即调用的有参 lambda 返回 Tokens，避免把函数值误当作 quote 插值结果。 */
    private fun quoteInterpolationExpression(modifier: String): String =
        "quote(common func template(): Unit {} ${'$'}({ argument: Int64 => $modifier func local(): Unit {}; quote(()) }(1)))"

    /** 按官方 ParseNodes 的显式 scope、原文件角色及外层声明名创建真实 fresh parser seed。 */
    private fun parseFragment(
        source: String,
        scope: MacroParsingScope,
        entryRole: CjmpFileRole = CjmpFileRole.NONE,
        outerIdent: String? = null,
    ): ParsedCangJieMacroFragment {
        val entry = CjMacroFragmentContext.Nodes(scope, CjMacroFragmentParent.OTHER, outerIdent, CjMacroDeclarationPrefix.EMPTY)
        val context = CjmpParsingContext.Compilation(true, true, true, initialFileRole = entryRole)
        val seed = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, entryRole), context, entry)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        return CangJieLightParser.parseMacroCodeFragment(builder, seed).also {
            assertEquals(seed, it.inputSeed)
            assertEquals(entryRole, it.parsingInfo.entryFileRole)
            assertEquals(context, it.parsingInfo.parsingContext)
        }
    }

    /** 使用真实完整文件非惰性入口，使函数体与 lambda 体的事件属于同一次解析。 */
    private fun parse(source: String): ParsedCangJieLightFile {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        return CangJieLightParser.parse(builder, cjmpContext = CjmpParsingContext.Compilation(true, true, true))
    }

    /** 空修饰符不产生事件；完整列表中出现额外 Skipped、参数事件或未知惰性区域均应失败。 */
    private fun assertRoleEvents(parsed: ParsedCangJieLightFile, expected: List<Pair<Int, CjmpRoleEventKind>>) {
        assertTrue(parsed.isCompleteFile)
        assertRoleEvents(parsed.roleEvents, expected)
    }

    /** 宏片段不声称是完整文件；按本次 fresh parser 根的真实事件身份比较顺序。 */
    private fun assertRoleEvents(events: List<CjmpParsedRoleEvent>, expected: List<Pair<Int, CjmpRoleEventKind>>) {
        assertEquals(expected, events.map { it.declarationOffset to it.kind })
        assertTrue(events.zipWithNext().all { (first, second) -> first.id.sequence < second.id.sequence })
    }
}
