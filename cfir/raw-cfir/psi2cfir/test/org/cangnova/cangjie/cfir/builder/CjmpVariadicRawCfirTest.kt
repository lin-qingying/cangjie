package org.cangnova.cangjie.cfir.builder

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals as equal
import kotlin.test.assertIs
import kotlin.test.assertNotNull as present
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/**
 * 直接调用 PSI／LightTree 的 pre-macro raw 入口，不经 fixture 的宏注册和字段物化。
 * v1.1.3 CheckVariableParams 只删除单列表 FOREIGN 的合法尾部，错误参数仍归真实 callable 所有。
 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpVariadicRawCfirTest : AbstractRawCfirBuilderTestCase() {
    fun testNonForeignFunctionsAndSecondaryConstructorsKeepEveryParameterOwner() {
        for (kind in listOf(Kind.FUNCTION, Kind.SECONDARY)) {
            for (owner in if (kind == Kind.SECONDARY) listOf("class", "struct") else listOf("class")) {
                checkBoth(Case(kind, listOf(listOf("value: Int64", "...", "...")), owner = owner,
                    expectedParameters = listOf("value: Int64", "...", "..."), hasVariableLenArg = true))
                checkBoth(Case(kind, listOf(listOf("...")), owner = owner,
                    expectedParameters = listOf("..."), hasVariableLenArg = true))
            }
        }
    }

    fun testOnlyTheLegalForeignTailIsRemovedFromFunctionsAndRealSecondaryConstructors() {
        for (kind in listOf(Kind.FUNCTION, Kind.SECONDARY)) {
            for (owner in if (kind == Kind.SECONDARY) listOf("class", "struct") else listOf("class")) {
                checkBoth(Case(kind, listOf(listOf("first: Int64", "second: Int64", "...")),
                    prefix = "foreign ", owner = owner, expectedParameters = listOf("first: Int64", "second: Int64"),
                    hasVariableLenArg = true, effectiveForeign = true))
                checkBoth(Case(kind, listOf(listOf("...")), prefix = "foreign ", owner = owner,
                    expectedParameters = listOf("..."), hasVariableLenArg = true, effectiveForeign = true))
                checkBoth(Case(kind, listOf(listOf("first: Int64", "...", "last: Int64")),
                    prefix = "foreign ", owner = owner, expectedParameters = listOf("first: Int64", "...", "last: Int64"),
                    hasVariableLenArg = true, effectiveForeign = true))
                checkBoth(Case(kind, listOf(listOf("...", "value: Int64", "...", "...")),
                    prefix = "foreign ", owner = owner, expectedParameters = listOf("...", "value: Int64", "..."),
                    hasVariableLenArg = true, effectiveForeign = true))
            }
        }
    }

    fun testPrimaryConstructorsKeepEllipsesEvenWithAForeignSourceModifier() {
        for (owner in listOf("class", "struct")) {
            for (prefix in listOf("", "foreign ")) {
                checkBoth(Case(Kind.PRIMARY, listOf(listOf("value: Int64", "...", "...")),
                    prefix = prefix, owner = owner, expectedParameters = listOf("value: Int64", "...", "..."),
                    hasVariableLenArg = true))
            }
        }
    }

    /** 每个列表都有独立恢复参数；flatten 后也不允许补做单列表的检查或删除。 */
    fun testMultipleListsFlattenAllParametersWithoutSettingTheVariadicFlag() {
        val lists = listOf(listOf("first: Int64", "..."), listOf("...", "second: Int64"), listOf("last: Int64", "..."))
        for (kind in listOf(Kind.FUNCTION, Kind.PRIMARY, Kind.SECONDARY)) {
            for (prefix in listOf("", "foreign ")) {
                for (count in listOf(2, 3)) {
                    val parameters = lists.take(count)
                    checkBoth(Case(kind, parameters, prefix = prefix, expectedParameters = parameters.flatten(),
                        hasVariableLenArg = false, effectiveForeign = kind != Kind.PRIMARY && prefix.isNotEmpty()))
                }
            }
        }
    }

    fun testMacrosKeepTheirInputWithoutBecomingVariadicForeignFunctions() {
        for (parameters in listOf(listOf("input: Tokens", "..."), listOf("..."))) {
            checkBoth(Case(Kind.MACRO, listOf(parameters), expectedParameters = parameters, hasVariableLenArg = false))
        }
    }

    fun testRejectedRolesKeepTheForeignTailAsARealErrorParameter() {
        for (kind in listOf(Kind.FUNCTION, Kind.SECONDARY)) {
            checkBoth(Case(kind, listOf(listOf("value: Int64", "...")), prefix = "specific foreign ",
                expectedParameters = listOf("value: Int64", "..."), hasVariableLenArg = true,
                effectiveForeign = false, commonCompilation = true))
        }
    }

    fun testOrdinarySignaturesDoNotAcquireVariadicFlags() {
        for (kind in listOf(Kind.FUNCTION, Kind.PRIMARY, Kind.SECONDARY)) {
            checkBoth(Case(kind, listOf(listOf("value: Int64")), expectedParameters = listOf("value: Int64"),
                hasVariableLenArg = false))
        }
    }

    /** 命名默认值属于参数槽位，PSI／LightTree 必须遵守相同的正文构建模式。 */
    fun testNamedDefaultsRespectTheBodyBuildingMode() {
        val parameter = "value!: Int64 = 1"
        for (kind in listOf(Kind.FUNCTION, Kind.PRIMARY, Kind.SECONDARY)) {
            for (owner in if (kind == Kind.FUNCTION) listOf("class") else listOf("class", "struct")) {
                checkBoth(Case(kind, listOf(listOf(parameter)), owner = owner,
                    expectedParameters = listOf(parameter), hasVariableLenArg = false))
            }
        }
    }

    /** 官方 Nothing 文档与 cjc 1.1.3 都禁止默认值返回到外层函数。 */
    fun testPsiDefaultReturnCannotBindTheOuterFunction() = checkDefaultReturnBoundary(lightTree = false)

    fun testLightTreeDefaultReturnCannotBindTheOuterFunction() = checkDefaultReturnBoundary(lightTree = true)

    private fun checkDefaultReturnBoundary(lightTree: Boolean) {
        val text = """
            func outer(): Int64 {
                func inner(value!: Int64 = return): Int64 { 0 }
                1
            }
        """.trimIndent()
        val case = Case(Kind.FUNCTION, emptyList(), emptyList(), hasVariableLenArg = false)
        val file = build(text, case, BodyBuildingMode.NORMAL, lightTree)
        val outer = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val inner = present(outer.body).statements.filterIsInstance<CfirNamedFunction>().single()
        val defaultReturn = assertIs<CfirReturnExpression>(inner.valueParameters.single().defaultValue)
        assertIs<CfirErrorFunction>(defaultReturn.target.labeledElement,
            "A named default is outside both the declared function body and the outer function body")
    }

    private fun checkBoth(case: Case) {
        val text = source(case)
        for (mode in listOf(BodyBuildingMode.NORMAL, BodyBuildingMode.LAZY_BODIES)) {
            val psi = assertSignature(build(text, case, mode, lightTree = false), text, case, mode)
            val light = assertSignature(build(text, case, mode, lightTree = true), text, case, mode)
            equal(psi, light, "$text\n$mode")
        }
    }

    private fun assertSignature(file: CfirFile, text: String, case: Case, mode: BodyBuildingMode): Signature {
        val function: CfirFunction = when (case.kind) {
            Kind.FUNCTION -> file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "target" }
            Kind.MACRO -> file.declarations.filterIsInstance<CfirMacroDeclaration>().single()
            Kind.PRIMARY, Kind.SECONDARY -> {
                val owner = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
                equal("Owner", owner.name.asString(), text)
                val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single {
                    it.isPrimary == (case.kind == Kind.PRIMARY) && it.source?.kind != CjFakeSourceElementKind.ImplicitConstructor
                }
                equal(case.kind == Kind.PRIMARY, constructor.isPrimary, text)
                val followingMember = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "afterMember" }
                truth(followingMember.valueParameters.isEmpty() && !followingMember.hasVariableLenArg, text)
                constructor
            }
        }
        identical(function, function.symbol.cfir, text)
        equal(case.hasVariableLenArg, function.hasVariableLenArg, text)
        if (case.kind == Kind.FUNCTION || case.kind == Kind.SECONDARY) {
            equal(case.effectiveForeign, function.status.isForeign, text)
        }
        equal(case.expectedParameters, function.valueParameters.map { parameter ->
            val source = present(parameter.source, text)
            text.substring(source.startOffset, source.endOffset)
        }, text)
        val parameters = function.valueParameters.map { parameter ->
            identical(function.symbol, parameter.containingDeclarationSymbol, text)
            identical(parameter, parameter.symbol.cfir, text)
            val source = present(parameter.source, text)
            val parameterText = text.substring(source.startOffset, source.endOffset)
            val typeSource = parameter.returnTypeRef.source
            if (parameterText.contains(':')) {
                val sourceName = parameterText.substringBefore(':').trim()
                equal(sourceName.removeSuffix("!"), parameter.name.asString(), text)
                equal(sourceName.endsWith('!'), parameter.isNamed, text)
                val type = present(typeSource, text)
                equal(parameterText.substringAfter(':').substringBefore('=').trim(),
                    text.substring(type.startOffset, type.endOffset), text)
            }
            if ('=' in parameterText) {
                val expectedDefaultText = parameterText.substringAfter('=').trim()
                val defaultValue = present(parameter.defaultValue, text)
                if (mode == BodyBuildingMode.LAZY_BODIES) {
                    assertIs<CfirLazyExpression>(defaultValue, "$text\n$mode")
                } else {
                    val literal = assertIs<CfirLiteralExpression>(defaultValue, "$text\n$mode")
                    equal(CfirLiteralKind.INT, literal.kind, text)
                    equal(expectedDefaultText, literal.value, text)
                    val defaultSource = present(defaultValue.source, text)
                    equal(expectedDefaultText, text.substring(defaultSource.startOffset, defaultSource.endOffset), text)
                }
            }
            ParameterSource(source.startOffset, source.endOffset, parameterText,
                typeSource?.let { text.substring(it.startOffset, it.endOffset) })
        }
        val following = file.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "following" }
        truth(following.valueParameters.isEmpty() && !following.hasVariableLenArg, text)
        if (case.kind != Kind.FUNCTION) {
            if (mode == BodyBuildingMode.LAZY_BODIES) assertIs<CfirLazyBlock>(function.body, text)
            else truth(present(function.body, text) !is CfirLazyBlock, text)
        }
        val source = present(function.source, text)
        return Signature(function.hasVariableLenArg, source.startOffset, source.endOffset, parameters)
    }

    /** 两种 body mode 共用真实 raw 图入口，参数合同不依赖 completed 宏发布产品。 */
    private fun build(text: String, case: Case, mode: BodyBuildingMode, lightTree: Boolean): CfirFile {
        val session = createTestSession(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3))
        if (case.commonCompilation) {
            session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        }
        val context = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
        if (!lightTree) {
            val file = createCjFile("variadicRaw", text)
            file.cjmpParsingContext = context
            return PsiRawCfirBuilder(session, mode, CfirAnnotationMetadataBuilder()).buildCfirFile(file)
        }
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val sourceFile = CjInMemoryTextSourceFile("variadicRaw.cj", null, text)
        val parsed = CangJieLightParser.parseBuffered(builder, cjmpContext = context, sourceFile = sourceFile)
        return LightTree2Cfir(session, session.cangjieScopeProvider, mode)
            .buildPreMacroCfirFile(parsed, sourceFile, text.toSourceLinesMapping()).cfirFile
    }

    private fun source(case: Case): String {
        val signature = case.lists.joinToString("") { it.joinToString(", ", "(", ")") }
        val prefix = case.prefix
        val declaration = when (case.kind) {
            Kind.FUNCTION -> prefix + "func target" + signature + ": Unit" + if (prefix.isEmpty()) " {}" else ""
            Kind.PRIMARY -> prefix + "Owner" + signature + " { let retained = 1 }"
            Kind.SECONDARY -> prefix + "init" + signature + " { let retained = 1 }"
            Kind.MACRO -> "public macro target" + signature + ": Tokens { input }"
        }
        val owner = case.owner
        return when (case.kind) {
            Kind.FUNCTION -> declaration + "\nfunc following(): Unit {}"
            Kind.MACRO -> "macro package variadic_tests\n" + declaration + "\nfunc following(): Unit {}"
            else -> "$owner Owner {\n$declaration\nfunc afterMember(): Unit {}\n}\nfunc following(): Unit {}"
        }
    }

    private enum class Kind { FUNCTION, PRIMARY, SECONDARY, MACRO }
    private data class Case(
        val kind: Kind,
        val lists: List<List<String>>,
        val expectedParameters: List<String>,
        val hasVariableLenArg: Boolean,
        val prefix: String = "",
        val owner: String = "class",
        val effectiveForeign: Boolean = false,
        val commonCompilation: Boolean = false,
    )
    private data class ParameterSource(val start: Int, val end: Int, val text: String, val typeText: String?)
    private data class Signature(val hasVariableLenArg: Boolean, val start: Int, val end: Int, val parameters: List<ParameterSource>)
}
