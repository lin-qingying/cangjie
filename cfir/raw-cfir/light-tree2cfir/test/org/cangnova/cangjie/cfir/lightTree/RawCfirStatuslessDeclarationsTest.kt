package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import kotlin.test.assertEquals as equal
import kotlin.test.assertNull as absent
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull as present
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * LightTree 使用与 PSI 相同的已有语义输入、状态与阶段合同，直接检查 pre-macro raw 输出。
 * 1.1.3 parser context 和 source session 沿用现有 CjmpVariadicRawCfirTest 的真实工厂路径；
 * 不借由 STATUS 或 export analysis 修补 raw 构造结果。
 */
@RunWith(JUnit3RunnerWithInners::class)
class RawCfirStatuslessDeclarationsTest : AbstractLightTree2CfirConverterTestCase() {
    fun testLocalNamedFunctionParametersAreResolvedStatuslessAtRawCreation() {
        for ((fixture, localName) in listOf(Fixture.DEFAULT_FACTS to "frozenNested", Fixture.DECLARATIONS_CALLS to "nested")) {
            val local = declarations(build(fixture)).filterIsInstance<CfirNamedFunction>().single { it.name.asString() == localName }
            assertTrue(local.isLocal, "$localName 必须由真实函数正文创建")
            equal(CfirResolvePhase.RAW_CFIR, local.resolvePhase)
            assertNullCfg(local)
            val parameter = local.valueParameters.single()
            equal("value", parameter.name.asString())
            assertParameter(local, parameter)
            equal("Int64", present(parameter.returnTypeRef.source).text.toString())
        }
    }

    fun testExplicitTypedLambdaAndParameterAreResolvedStatuslessAtRawCreation() {
        val lambda = declarations(build(Fixture.DECLARATIONS_CALLS)).filterIsInstance<CfirAnonymousFunction>().single()
        assertTrue(lambda.isLambda)
        assertTrue(lambda.hasExplicitParameterList)
        assertTrue(lambda.isLocal)
        assertStatusless(lambda)
        assertNullCfg(lambda)
        val parameter = lambda.valueParameters.single()
        equal("value", parameter.name.asString())
        assertParameter(lambda, parameter)
        assertFalse(parameter.returnTypeRef is CfirImplicitTypeRef, "原文 value: Int64 保留显式类型")
        equal("Int64", present(parameter.returnTypeRef.source).text.toString())
    }

    /**
     * 三种调用原样来自 v113 unittests/Frontend/TrailingClosure/src/lambda.cj。
     * i 的类型省略，但三个原 lambda 都有 => 和一个显式参数；这不是省略整个参数列表。
     * 当前原文件没有无 => 或 {=> ...} 的零参数案例，不据此宣称两者的创建分支已经覆盖。
     */
    fun testUntypedExplicitLambdaParametersAreResolvedStatuslessAtRawCreation() {
        val lambdas = declarations(build(Fixture.TRAILING_CLOSURE)).filterIsInstance<CfirAnonymousFunction>()
        equal(3, lambdas.size, "普通实参、带括号的尾随闭包和省略调用括号的尾随闭包")
        equal(3, lambdas.map { present(it.source).startOffset }.toSet().size, "三个真实 lambda 保留各自来源身份")
        for (lambda in lambdas) {
            equal("{i => i * i}", present(lambda.source).text.toString())
            assertTrue(lambda.isLambda)
            assertTrue(lambda.hasExplicitParameterList, "省略参数类型不能使有 => 的列表变成 implicit")
            assertStatusless(lambda)
            assertNullCfg(lambda)
            val parameter = lambda.valueParameters.single()
            equal("i", parameter.name.asString())
            equal("i", present(parameter.source).text.toString())
            assertParameter(lambda, parameter)
            assertFalse(parameter.isNamed)
            assertIs<CfirImplicitTypeRef>(parameter.returnTypeRef)
            absent(parameter.returnTypeRef.source, "省略类型时不制造类型源码节点")
            equal(true, parameter.isLambdaParameterTypeOmitted, "必须保留 raw 原类型省略 payload，不由当前类型反推")
            absent(parameter.defaultValue)
            absent(parameter.defaultAssignmentSyntaxFacts)
        }
    }

    fun testEnumPayloadParametersAreResolvedStatuslessAtRawCreation() {
        val file = build(Fixture.ENUM_DECLARATION)
        val owner = file.declarations.filterIsInstance<CfirEnum>().single { it.name.asString() == "Option" }
        // v113 ParseEnumConstructorWithArgs 生成 FuncDecl，ParseNoArgsEnumConstructor 生成 VarDecl。
        // CFIR 使用统一 CfirEnumConstructor 表示枚举项；只有 Some(T) 拥有 payload 参数。
        val entries = owner.declarations.filterIsInstance<CfirEnumConstructor>()
        equal(listOf("Some", "None"), entries.map { it.name.asString() })
        val payloadEntries = entries.filter { it.valueParameters.isNotEmpty() }
        equal(listOf("Some"), payloadEntries.map { it.name.asString() })
        val valueEntries = entries.filter { it.valueParameters.isEmpty() }
        equal(listOf("None"), valueEntries.map { it.name.asString() })
        val none = valueEntries.single()
        equal(CfirResolvePhase.RAW_CFIR, none.resolvePhase)
        equal(CfirDeclarationOrigin.Source, none.origin)
        assertSame(none, none.symbol.cfir)
        equal("None", present(none.source).text.toString().trim(), "零载荷源码项没有参数列表")
        assertTrue(none.valueParameters.isEmpty(), "None 是零载荷枚举值，不制造 payload 参数")
        val constructor = payloadEntries.single()
        equal(CfirResolvePhase.RAW_CFIR, constructor.resolvePhase)
        val parameter = constructor.valueParameters.single()
        equal(CfirResolvePhase.RAW_CFIR, parameter.resolvePhase)
        assertSame(constructor.symbol, parameter.containingDeclarationSymbol)
        assertSame(parameter, parameter.symbol.cfir)
        assertStatusless(parameter)
        assertFalse(parameter.isNamed)
        equal("T", present(parameter.returnTypeRef.source).text.toString())
        absent(parameter.defaultValue)
    }

    fun testConstOwnerParametersAreStatuslessBeforeAnyStatusProcessor() {
        val owner = declarations(build(Fixture.DEFAULT_FACTS)).filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "constDefaults" }
        equal(CfirResolvePhase.RAW_CFIR, owner.resolvePhase)
        assertTrue(owner.status.isConst, "const 只属于原函数，不替代参数的 statusless 状态")
        assertNullCfg(owner)
        equal(
            listOf("literal", "parenthesized", "negativeLiteral", "unaryExpression", "compound", "compoundParen", "invocation", "constantReference"),
            owner.valueParameters.map { it.name.asString() },
        )
        owner.valueParameters.forEach { parameter ->
            assertParameter(owner, parameter)
            assertTrue(parameter.isNamed)
            present(parameter.defaultValue, "raw builder 保留原 default assignment")
            present(parameter.defaultAssignmentSyntaxFacts, "statusless 修复不得丢失原语法 payload")
        }
    }

    private fun assertParameter(owner: CfirFunction, parameter: CfirValueParameter) {
        assertSame(owner.symbol, parameter.containingDeclarationSymbol)
        assertSame(parameter, parameter.symbol.cfir)
        equal(CfirDeclarationOrigin.Source, parameter.origin)
        assertStatusless(parameter)
    }

    private fun assertStatusless(declaration: CfirMemberDeclaration) {
        equal(CfirResolvePhase.RAW_CFIR, declaration.resolvePhase, "${declaration.symbol}: 创建阶段不能提前推进")
        val status = assertIs<CfirResolvedDeclarationStatus>(declaration.status, "${declaration.symbol}: raw 必须创建 resolved statusless")
        equal(Visibilities.Public, status.visibility)
        equal(Modality.FINAL, status.modality)
        assertFalse(status.isVisibilityExplicit)
        assertFalse(status.isModalityExplicit)
        assertFalse(status.isConst)
        assertFalse(status.isAbstract)
        assertFalse(status.isOpen)
        assertFalse(status.isCommon)
        assertFalse(status.isSpecific)
    }

    private fun assertNullCfg(function: CfirFunction) {
        absent(function.controlFlowGraphReference, "测试必须停在真实 raw 创建，不能经 body/CFG resolve")
    }

    private fun build(fixture: Fixture): CfirFile {
        val path = Path.of(fixture.path)
        val bytes = Files.readAllBytes(path)
        equal(fixture.sha256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
        val text = bytes.toString(Charsets.UTF_8)
        val session = createTestSession(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3))
        val sourceFile = CjInMemoryTextSourceFile(path.fileName.toString(), null, text)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val context = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
        val parsed = CangJieLightParser.parseBuffered(builder, cjmpContext = context, sourceFile = sourceFile)
        return LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
            .buildPreMacroCfirFile(parsed, sourceFile, text.toSourceLinesMapping()).cfirFile
    }

    /** 只遍历真实 raw builder 输出；不会创建、替换声明或制造参数状态。 */
    private fun declarations(file: CfirFile): List<CfirMemberDeclaration> {
        val declarations = mutableListOf<CfirMemberDeclaration>()
        file.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirMemberDeclaration) declarations += element
                element.acceptChildren(this)
            }
        }, null)
        return declarations
    }

    private enum class Fixture(val path: String, val sha256: String) {
        DEFAULT_FACTS(
            "cfir/analysis-tests/testData/diagnostics2/common-specific/source-expression-export/defaultFacts.cj",
            "a9dfb1d5611ecc9da2ca302ccaa0361eff545090bbc53b22303e59d3eeaa2116",
        ),
        DECLARATIONS_CALLS(
            "cfir/analysis-tests/testData/diagnostics2/common-specific/source-expression-export/declarationsCalls.cj",
            "bf8f88e2398ba1f562f848ab0814f68cef90c7803b96c3b1bc6394d145ad9002",
        ),
        // 此既有合法输入使用 | 分隔 Some(T) 和 None；不改原缺少分隔符的 match fixture。
        ENUM_DECLARATION(
            "cfir/raw-cfir/psi2cfir/testData/rawBuilder/declarations/class-like/enumDeclaration.cj",
            "ac79461a9fcad38816efbd9327b182e22cf6ab6561bb0b5ef3ead9f2080e2d9e",
        ),
        TRAILING_CLOSURE(
            "cfir/raw-cfir/psi2cfir/testData/rawBuilder/expressions/basics/trailingClosureOfficial.cj",
            "747f19752c6c205e60b334d7ad25e277b1202403d9fe5831006f42271385a545",
        ),
    }
}
