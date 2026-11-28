package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * v1.1.3 ParseForeignDecls 克隆集合前缀；CheckDeclModifiers 把返回属性交给真实声明。
 * 非法目标或冲突不会撤销已返回属性；文件角色拒绝则返回空集。
 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpForeignInheritedRoleRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiCommonCollectionClassAttributes() = checkNominal(lightTree = false, common = true)
    fun testLightTreeCommonCollectionClassAttributes() = checkNominal(lightTree = true, common = true)
    fun testPsiSpecificCollectionClassAttributes() = checkNominal(lightTree = false, common = false)
    fun testLightTreeSpecificCollectionClassAttributes() = checkNominal(lightTree = true, common = false)

    private fun checkNominal(lightTree: Boolean, common: Boolean) {
        val role = if (common) "common" else "specific"
        val source = "$role foreign { class C { $role func member(): Unit {} } }\nclass After {}"
        val file = build(source, lightTree, common)
        val classes = file.declarations.filterIsInstance<CfirClass>().associateBy { it.name.asString() }
        assertEquals(setOf("C", "After"), classes.keys)
        assertAttributes(classes.getValue("C").status, common, !common, foreign = true)
        assertAttributes(classes.getValue("After").status, common = false, specific = false, foreign = false)
        val member = classes.getValue("C").declarations.filterIsInstance<CfirNamedFunction>().single()
        assertAttributes(member.status, common, !common, foreign = false)
    }

    fun testPsiIllegalForeignStructKeepsReturnedAttributes() = checkStruct(lightTree = false)
    fun testLightTreeIllegalForeignStructKeepsReturnedAttributes() = checkStruct(lightTree = true)

    private fun checkStruct(lightTree: Boolean) {
        val file = build("common foreign { struct S { common init() {} } }", lightTree, common = true)
        assertAttributes(file.declarations.filterIsInstance<CfirStruct>().single().status,
            common = true, specific = false, foreign = true)
    }

    fun testPsiConflictingFunctionKeepsReturnedAttributes() = checkFunction(lightTree = false)
    fun testLightTreeConflictingFunctionKeepsReturnedAttributes() = checkFunction(lightTree = true)

    private fun checkFunction(lightTree: Boolean) {
        val file = build("common foreign { func nativeFun(): Unit }", lightTree, common = true)
        assertAttributes(file.declarations.filterIsInstance<CfirNamedFunction>().single().status,
            common = true, specific = false, foreign = true)
    }

    fun testPsiCollectionVariableUsesItsOwnReturnedAttributes() = checkVariable(lightTree = false)
    fun testLightTreeCollectionVariableUsesItsOwnReturnedAttributes() = checkVariable(lightTree = true)

    private fun checkVariable(lightTree: Boolean) {
        val file = build("common foreign { var nativeValue: Int64 }", lightTree, common = true)
        assertAttributes(file.declarations.filterIsInstance<CfirVariable>().single().status,
            common = true, specific = false, foreign = true)
    }

    fun testPsiRejectedCollectionRoleClearsAllCheckedAttributes() = checkRejected(lightTree = false)
    fun testLightTreeRejectedCollectionRoleClearsAllCheckedAttributes() = checkRejected(lightTree = true)

    private fun checkRejected(lightTree: Boolean) {
        val file = build("specific foreign { public unsafe func wrong(): Unit }", lightTree, common = true)
        val status = file.declarations.filterIsInstance<CfirNamedFunction>().single().status
        assertAttributes(status, common = false, specific = false, foreign = false)
        assertEquals(false, status.isUnsafe)
        assertEquals(false, status.isVisibilityExplicit)
    }

    private fun assertAttributes(status: CfirDeclarationStatus, common: Boolean, specific: Boolean, foreign: Boolean) {
        assertEquals(listOf(common, specific, foreign), listOf(status.isCommon, status.isSpecific, status.isForeign))
    }

    private fun build(source: String, lightTree: Boolean, common: Boolean): CfirFile {
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        val cjmp = CfirCjmpSettingsComponent(explicitMode = if (common) CfirCjmpMode.COMMON else CfirCjmpMode.SPECIFIC)
        session.register(CfirCjmpSettingsComponent::class, cjmp)
        val parsingContext = cjmp.toParsingContext(settings)
        return if (lightTree) {
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
            val sourceFile = CjInMemoryTextSourceFile("foreignInherited.cj", null, source)
            val parsed = CangJieLightParser.parseBuffered(builder, cjmpContext = parsingContext, sourceFile = sourceFile)
            LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
                .buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping()).cfirFile
        } else {
            val file = createCjFile("foreignInherited", source)
            file.cjmpParsingContext = parsingContext
            PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL, CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(file).cfirFile
        }
    }
}
