package org.cangnova.cangjie.cfir.lightTree

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.parsingFileFacts
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.ParsingFileFacts
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 声明文件（`.cj.d`）在 raw CFIR 层的属性位语义（4.5.2 护栏 1 / 护栏 2）。
 *
 * 验收（docs/cjd-declaration-file-support-v3.md P2）：
 *  - 护栏 1（R3）：`.cj.d` 的无体**函数**不得被推断为隐式 abstract；
 *    同一文本在 `.cj` 下**必须**被标记（双向验证）。
 *  - 护栏 1 反向（R4）：`.cj.d` 的无体**属性**仍然获得隐式 abstract ——
 *    官方"无 `{}` 即 abstract"没有 `parseDeclFile` 门禁。若实现者把护栏加错到属性分支，本用例失败。
 *  - 护栏 2（R15）：`.cj.d` 的 interface 无体函数获得 `DEFAULT`（官方 `SetDefaultFunc`
 *    中 DEFAULT 是 ABSTRACT 的补集，且 `SetDefaultFunc` 无 `parseDeclFile` 门禁）；
 *    `.cj` 下同一函数是 abstract 而非 default。
 *
 * 两条 CFIR 路径（PSI / LightTree）分别独立验证 —— 两个谓词是分开实现的。
 */
@RunWith(JUnit3RunnerWithInners::class)
class DeclarationModeRawCfirStatusTest : AbstractLightTree2CfirConverterTestCase() {
    /** parser 范围错误包装不能把真实 tuple/enum 根模式变成合成 wildcard。 */
    fun testPatternRootSurvivesParserRangeErrorInBothRawBuilders() {
        for (pattern in listOf("(a, b)", "Some(x)", "_")) {
            val text = "common let $pattern = value"
            val psiFile = PsiRawCfirBuilder(createTestSession(), annotationMetadata = CfirAnnotationMetadataBuilder()).buildCfirFile(createCjFile("patternError", text))
            val session = createTestSession()
            val lightFile = LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
                parseLightFile(text), CjInMemoryTextSourceFile("patternError.cj", null, text), text.toSourceLinesMapping(),
            ).cfirFile
            for (file in listOf(psiFile, lightFile)) {
                val root = (file.declarations.single() as org.cangnova.cangjie.cfir.declarations.CfirPatternVariable).pattern
                when (pattern) {
                    "(a, b)" -> kotlin.test.assertIs<org.cangnova.cangjie.cfir.patterns.CfirTuplePattern>(root)
                    "Some(x)" -> kotlin.test.assertIs<org.cangnova.cangjie.cfir.patterns.CfirEnumPattern>(root)
                    "_" -> kotlin.test.assertIs<org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern>(root)
                }
                kotlin.test.assertEquals(text.indexOf(pattern), root.source?.startOffset)
                kotlin.test.assertEquals(text.indexOf(pattern) + pattern.length, root.source?.endOffset)
            }
        }
    }

    /** 已解析文件入口完整发布真实角色；不能再为裸树填默认 NONE。 */
    fun testParsedLightFilePublishesActualSourceKindAndRole() {
        for (kind in listOf(CjSourceKind.SOURCE, CjSourceKind.DECLARATION)) {
            for ((modifier, role) in listOf("" to CjmpFileRole.NONE, "common " to CjmpFileRole.COMMON, "specific " to CjmpFileRole.SPECIFIC)) {
                val text = "${modifier}func generated(): Unit {}"
                val source = CjInMemoryTextSourceFile("physical.cj", null, text)
                val settings = LanguageVersionSettingsImpl.DEFAULT
                val session = createTestSession(settings)
                val mode = when (role) {
                    CjmpFileRole.COMMON -> CfirCjmpMode.COMMON
                    CjmpFileRole.SPECIFIC -> CfirCjmpMode.SPECIFIC
                    else -> CfirCjmpMode.NONE
                }
                val cjmp = CfirCjmpSettingsComponent(explicitMode = mode)
                session.register(CfirCjmpSettingsComponent::class, cjmp)
                val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
                val parsed = CangJieLightParser.parse(builder, sourceKind = kind, cjmpContext = cjmp.toParsingContext(settings))
                val file = LightTree2Cfir(session, session.cangjieScopeProvider)
                    .buildPreMacroCfirFile(parsed, source, text.toSourceLinesMapping()).cfirFile
                kotlin.test.assertEquals(ParsingFileFacts(kind, role), file.parsingFileFacts)
                kotlin.test.assertSame(parsed.fileFacts, file.parsingFileFacts)
            }
        }
    }

    /** 文法来自已完成 parser 的源种类，物理来源路径只负责定位，不覆盖语义来源。 */
    fun testParsedSourceKindControlsBodylessRawStatusIndependentlyOfPhysicalPath() {
        for (kind in listOf(CjSourceKind.SOURCE, CjSourceKind.DECLARATION)) {
            val parsed = parseLightFile(CLASS_BODYLESS_FUNC, kind)
            val physicalKind = if (kind == CjSourceKind.SOURCE) CjSourceKind.DECLARATION else CjSourceKind.SOURCE
            val source = CjInMemoryTextSourceFile("physical.cj", null, CLASS_BODYLESS_FUNC, explicitKind = physicalKind)
            val session = createTestSession()
            val file = LightTree2Cfir(session, session.cangjieScopeProvider)
                .buildPreMacroCfirFile(parsed, source, CLASS_BODYLESS_FUNC.toSourceLinesMapping()).cfirFile
            kotlin.test.assertEquals(kind, file.parsingFileFacts?.sourceKind)
            kotlin.test.assertEquals(kind == CjSourceKind.SOURCE, singleFunctionIn(singleClassLike(file)).status.isAbstract)
        }
    }


    // ==================== 护栏 1：无体函数不被标 abstract（R3） ====================

    fun testPsiDeclarationFileBodylessFunctionIsNotAbstract() {
        val file = buildFromPsi(CLASS_BODYLESS_FUNC, DECLARATION_FILE_NAME, CjSourceKind.DECLARATION)
        val status = singleFunctionIn(singleClassLike(file)).status
        assertFalse(status.isAbstract, ".cj.d 的无体类成员函数不得被推断为隐式 abstract")
    }

    fun testLightTreeDeclarationFileBodylessFunctionIsNotAbstract() {
        val file = buildFromLightTree(CLASS_BODYLESS_FUNC, DECLARATION_FILE_NAME, CjSourceKind.DECLARATION)
        val status = singleFunctionIn(singleClassLike(file)).status
        assertFalse(status.isAbstract, ".cj.d 的无体类成员函数不得被推断为隐式 abstract（LightTree 路径）")
    }

    fun testPsiSourceFileBodylessFunctionIsAbstract() {
        val file = buildFromPsi(CLASS_BODYLESS_FUNC, SOURCE_FILE_NAME, CjSourceKind.SOURCE)
        val status = singleFunctionIn(singleClassLike(file)).status
        assertTrue(status.isAbstract, ".cj 的无体类成员函数必须保持隐式 abstract（双向验证的另一半）")
    }

    fun testLightTreeSourceFileBodylessFunctionIsAbstract() {
        val file = buildFromLightTree(CLASS_BODYLESS_FUNC, SOURCE_FILE_NAME, CjSourceKind.SOURCE)
        val status = singleFunctionIn(singleClassLike(file)).status
        assertTrue(status.isAbstract, ".cj 的无体类成员函数必须保持隐式 abstract（LightTree 路径）")
    }

    // ==================== CJMP 抽象类成员的统一模态规则（E13） ====================

    fun testPsiCjmpAbstractClassOnlyKeepsExplicitAbstractMembersAbstract() {
        val owner = singleClassLike(buildFromPsi(CJMP_ABSTRACT_CLASS_MEMBERS, SOURCE_FILE_NAME, CjSourceKind.SOURCE))

        assertFalse(owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "implicitFunction" }.status.isAbstract)
        assertFalse(owner.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "implicitProperty" }.status.isAbstract)
        assertTrue(owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "explicitFunction" }.status.isAbstract)
        assertTrue(owner.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "explicitProperty" }.status.isAbstract)
    }

    fun testLightTreeCjmpAbstractClassOnlyKeepsExplicitAbstractMembersAbstract() {
        val session = createTestSession()
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        val source = CjInMemoryTextSourceFile(SOURCE_FILE_NAME, null, CJMP_ABSTRACT_CLASS_MEMBERS)
        val file = LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildPreMacroCfirFile(CJMP_ABSTRACT_CLASS_MEMBERS, source, CJMP_ABSTRACT_CLASS_MEMBERS.toSourceLinesMapping()).cfirFile
        val owner = singleClassLike(file)

        assertFalse(owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "implicitFunction" }.status.isAbstract)
        assertFalse(owner.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "implicitProperty" }.status.isAbstract)
        assertTrue(owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "explicitFunction" }.status.isAbstract)
        assertTrue(owner.declarations.filterIsInstance<CfirProperty>().single { it.name.asString() == "explicitProperty" }.status.isAbstract)
    }

    // ==================== 护栏 1 反向：无体属性仍标 abstract（R4） ====================

    fun testPsiDeclarationFileBodylessPropertyIsStillAbstract() {
        val file = buildFromPsi(CLASS_BODYLESS_PROP, DECLARATION_FILE_NAME, CjSourceKind.DECLARATION)
        val status = singlePropertyIn(singleClassLike(file)).status
        assertTrue(status.isAbstract, "护栏只作用于函数：.cj.d 的无体属性仍须获得隐式 abstract（R4）")
    }

    fun testLightTreeDeclarationFileBodylessPropertyIsStillAbstract() {
        val file = buildFromLightTree(CLASS_BODYLESS_PROP, DECLARATION_FILE_NAME, CjSourceKind.DECLARATION)
        val status = singlePropertyIn(singleClassLike(file)).status
        assertTrue(status.isAbstract, "护栏只作用于函数：.cj.d 的无体属性仍须获得隐式 abstract（R4，LightTree 路径）")
    }

    // ==================== 护栏 2：DEFAULT 与 ABSTRACT 同源（R15） ====================

    fun testPsiDeclarationFileInterfaceBodylessFunctionIsDefault() {
        val file = buildFromPsi(INTERFACE_BODYLESS_FUNC, DECLARATION_FILE_NAME, CjSourceKind.DECLARATION)
        val status = singleFunctionIn(singleClassLike(file)).status
        assertFalse(status.isAbstract)
        assertTrue(
            status.isDefault,
            "官方 SetDefaultFunc 无 parseDeclFile 门禁：.cj.d 的 interface 无体函数应获得 DEFAULT",
        )
    }

    fun testLightTreeDeclarationFileInterfaceBodylessFunctionIsDefault() {
        val file = buildFromLightTree(INTERFACE_BODYLESS_FUNC, DECLARATION_FILE_NAME, CjSourceKind.DECLARATION)
        val status = singleFunctionIn(singleClassLike(file)).status
        assertFalse(status.isAbstract)
        assertTrue(status.isDefault, "DEFAULT 与 ABSTRACT 同源（LightTree 路径）")
    }

    fun testPsiSourceFileInterfaceBodylessFunctionIsAbstractNotDefault() {
        val file = buildFromPsi(INTERFACE_BODYLESS_FUNC, SOURCE_FILE_NAME, CjSourceKind.SOURCE)
        val status = singleFunctionIn(singleClassLike(file)).status
        assertTrue(status.isAbstract, ".cj 的 interface 无体函数是纯虚（abstract），不是 default")
        assertFalse(status.isDefault)
    }

    fun testLightTreeSourceFileInterfaceBodylessFunctionIsAbstractNotDefault() {
        val file = buildFromLightTree(INTERFACE_BODYLESS_FUNC, SOURCE_FILE_NAME, CjSourceKind.SOURCE)
        val status = singleFunctionIn(singleClassLike(file)).status
        assertTrue(status.isAbstract)
        assertFalse(status.isDefault)
    }

    // ==================== 基础设施 ====================

    private fun buildFromPsi(text: String, fileName: String, kind: CjSourceKind): CfirFile {
        val session = createTestSession()
        val builder = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL, CfirAnnotationMetadataBuilder())
        return builder.buildCfirFile(psiFile(text, fileName, kind))
    }

    private fun buildFromLightTree(text: String, fileName: String, kind: CjSourceKind): CfirFile {
        val session = createTestSession()
        val sourceFile = CjInMemoryTextSourceFile(fileName, null, text, explicitKind = kind)
        return LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildPreMacroCfirFile(text, sourceFile, text.toSourceLinesMapping())
            .cfirFile
    }

    /** 以显式 FileType 创建 PSI 文件：fileType 是 CjFile.sourceKind 的唯一真源。 */
    private fun psiFile(text: String, fileName: String, kind: CjSourceKind): CjFile {
        val fileType: LanguageFileType = when (kind) {
            CjSourceKind.DECLARATION -> CangJieDeclarationFileType
            else -> CangJieFileType.INSTANCE
        }
        return psiFileFactory.createFileFromText(fileName, fileType, text) as CjFile
    }

    /** class 与 interface 在 CFIR 中是两个类型，公共父类 [CfirClassLikeDeclaration] 持有成员列表。 */
    private fun singleClassLike(file: CfirFile): CfirClassLikeDeclaration =
        file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()

    private fun singleFunctionIn(owner: CfirClassLikeDeclaration): CfirNamedFunction =
        owner.declarations.filterIsInstance<CfirNamedFunction>().single()

    private fun singlePropertyIn(owner: CfirClassLikeDeclaration): CfirProperty =
        owner.declarations.filterIsInstance<CfirProperty>().single()

    companion object {
        private const val DECLARATION_FILE_NAME = "decl.cj.d"
        private const val SOURCE_FILE_NAME = "decl.cj"

        private val CLASS_BODYLESS_FUNC = """
            class C {
                func f(): Int64
            }
        """.trimIndent()

        private val CLASS_BODYLESS_PROP = """
            class C {
                prop p: Int64
            }
        """.trimIndent()

        private val CJMP_ABSTRACT_CLASS_MEMBERS = """
            common abstract class C {
                func implicitFunction(): Int64
                prop implicitProperty: Int64
                abstract func explicitFunction(): Int64
                abstract prop explicitProperty: Int64
            }
        """.trimIndent()

        private val INTERFACE_BODYLESS_FUNC = """
            interface I {
                func f(): Int64
            }
        """.trimIndent()
    }
}
