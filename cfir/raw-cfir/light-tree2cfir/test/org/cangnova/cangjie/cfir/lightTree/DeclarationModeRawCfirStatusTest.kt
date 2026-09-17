package org.cangnova.cangjie.cfir.lightTree

import com.intellij.openapi.fileTypes.LanguageFileType
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
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
        val builder = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL)
        return builder.buildCfirFile(psiFile(text, fileName, kind))
    }

    private fun buildFromLightTree(text: String, fileName: String, kind: CjSourceKind): CfirFile {
        val session = createTestSession()
        val sourceFile = CjInMemoryTextSourceFile(fileName, null, text, explicitKind = kind)
        return LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildCfirFileWithSurfaces(text, sourceFile, text.toSourceLinesMapping())
            .first
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

        private val INTERFACE_BODYLESS_FUNC = """
            interface I {
                func f(): Int64
            }
        """.trimIndent()
    }
}
