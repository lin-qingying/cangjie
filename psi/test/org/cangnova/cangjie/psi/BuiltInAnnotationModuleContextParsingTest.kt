package org.cangnova.cangjie.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CangJieParser
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * std 专属注解的语法身份必须穿过完整文件、工厂片段及惰性子树。
 * 官方 ParseAnnotations.cpp:153–157 以模块身份识别 ConstSafe，文件名不参与判断。
 */
class BuiltInAnnotationModuleContextParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun bareConstSafeUsesTheDeclaredModuleRatherThanTheFileName() {
        val rootFile = createFile("std_annotation_contract", VARIABLE)
        assertConstSafeForm(rootFile, isBuiltIn = false)
        val standardFile = createFile("ordinary", "package std.annotation_contract\n$VARIABLE")
        assertConstSafeForm(standardFile, isBuiltIn = true)
        val nonstandardFile = createFile("std", "package example.annotation_contract\n$VARIABLE")
        assertConstSafeForm(nonstandardFile, isBuiltIn = false)
        val similarPrefix = createFile("stdLike", "package stdish.annotation_contract\n$VARIABLE")
        assertConstSafeForm(similarPrefix, isBuiltIn = false)
    }

    /** 单段 package std 没有官方 parser 用于模块判定的 prefixPaths。 */
    @Test
    fun singleSegmentStdPackageDoesNotEnableStandardLibraryAnnotations() {
        val file = createFile("singleSegmentStd", "package std\n$VARIABLE")
        assertConstSafeForm(file, isBuiltIn = false)
        val host = createHost("package std")
        assertConstSafeForm(CjPsiFactory.contextual(host).createFile(VARIABLE), isBuiltIn = false)
        assertConstSafeForm(CjPsiFactory.forPackage(project, FqName("std")).createFile(VARIABLE), isBuiltIn = false)
    }

    /** 固定 revision 的 Parser.cpp:269–307 把组织前缀也保存在模块判定的首个 prefixPaths 中。 */
    @Test
    fun organizationPrefixIsPreservedForCompilerModuleContext() {
        val file = createFile("organizationQualified", "package org::std.annotation_contract\n$VARIABLE")
        assertEquals("org", file.packageDirective?.organizationName?.asString())
        assertEquals(FqName("std.annotation_contract"), file.packageFqName)
        assertConstSafeForm(file, isBuiltIn = false)
        val host = createHost("package org::std.annotation_contract")
        assertConstSafeForm(CjPsiFactory.contextual(host).createFile(VARIABLE), isBuiltIn = false)

        val standardOrganization = createFile("standardOrganization", "package std::annotation_contract\n$VARIABLE")
        assertEquals("std", standardOrganization.packageDirective?.organizationName?.asString())
        assertEquals(FqName("annotation_contract"), standardOrganization.packageFqName)
        assertConstSafeForm(standardOrganization, isBuiltIn = true)
        val standardHost = createHost("package std::annotation_contract")
        assertConstSafeForm(CjPsiFactory.contextual(standardHost).createFile(VARIABLE), isBuiltIn = true)
    }

    @Test
    fun explicitCurrentPackageQualificationKeepsConstSafeAsAMacro() {
        val file = createFile(
            "qualifiedConstSafe",
            "package std.annotation_contract\n@std.annotation_contract.ConstSafe\nconst value: Int64 = 1",
        )
        assertConstSafeForm(file, isBuiltIn = false, macroName = "std.annotation_contract.ConstSafe")
    }

    @Test
    fun forcedCustomConstSafeIsReservedOnlyInTheStandardLibrary() {
        for (header in listOf(
            "", "package std\n", "package example.annotation_contract\n", "package org::std.annotation_contract\n",
        )) {
            val file = createFile("forcedCustom", "${header}@!ConstSafe\nconst value: Int64 = 1")
            assertNoErrors(file)
            val variable = PsiTreeUtil.findChildrenOfType(file, CjPatternVariable::class.java).single()
            val annotation = variable.annotationEntries.single()
            assertEquals("ConstSafe", annotation.shortName?.asString())
            assertEquals("@!ConstSafe", annotation.text)
            assertTrue(annotation.isCompileTimeVisible)
            assertTrue(PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java).isEmpty())
        }
        val reserved = createFile(
            "reservedStandardLibraryName",
            "package std.annotation_contract\n@!ConstSafe\nconst value: Int64 = 1",
        )
        assertTrue(PsiTreeUtil.findChildrenOfType(reserved, PsiErrorElement::class.java).isNotEmpty())
    }

    @Test
    fun contextualFactoryPreservesHostModuleAndExplicitPackageOverridesIt() {
        val standardHost = createHost("package std.annotation_contract")
        val standardFactory = CjPsiFactory.contextual(standardHost)
        assertConstSafeForm(standardFactory.createFile(VARIABLE), isBuiltIn = true)
        assertConstSafeForm(
            standardFactory.createFile("package example.annotation_contract\n$VARIABLE"),
            isBuiltIn = false,
        )

        val ordinaryHost = createHost("package example.annotation_contract")
        assertConstSafeForm(CjPsiFactory.contextual(ordinaryHost).createFile(VARIABLE), isBuiltIn = false)
        assertConstSafeForm(
            CjPsiFactory.contextual(ordinaryHost).createFile("package std.annotation_contract\n$VARIABLE"),
            isBuiltIn = true,
        )
    }

    @Test
    fun packageFactoryUsesItsSemanticPackageForFragmentsWithoutAHeader() {
        val standard = CjPsiFactory.forPackage(project, FqName("std.annotation_contract"))
        val standardFile = standard.createFile(VARIABLE)
        assertEquals("std", standardFile.parserLanguageModuleName)
        assertEquals("std", CangJieParser.languageModuleNameForContext(standardFile))
        assertConstSafeForm(standardFile, isBuiltIn = true)
        val root = CjPsiFactory.forPackage(project, FqName.ROOT)
        assertConstSafeForm(root.createFile(VARIABLE), isBuiltIn = false)
        val ordinary = CjPsiFactory.forPackage(project, FqName("example.annotation_contract"))
        assertConstSafeForm(ordinary.createFile(VARIABLE), isBuiltIn = false)
    }

    /** 单段 package 不覆盖已有 seed；普通宏产生的新 tokens 则必须从空 seed 开始。 */
    @Test
    fun singleSegmentHeaderPreservesContextSeedButMacroExpansionStartsEmpty() {
        val host = createHost("package std.annotation_contract")
        val contextual = CjPsiFactory.contextual(host)
        val expansion = CjPsiFactory.forMacroExpansion(project, host)
        for (header in listOf("package std", "package example")) {
            assertConstSafeForm(contextual.createFile("$header\n$VARIABLE"), isBuiltIn = true)
            assertConstSafeForm(expansion.createFile("$header\n$VARIABLE"), isBuiltIn = false)
        }
        assertConstSafeForm(expansion.createFile(VARIABLE), isBuiltIn = false)
    }

    @Test
    fun lazyFunctionBodyPreservesTheModuleOfItsHostFile() {
        for ((header, isBuiltIn) in listOf(
            "package std.annotation_contract" to true,
            "package std::annotation_contract" to true,
            "package std" to false,
            "package example" to false,
            "package org::std.annotation_contract" to false,
        )) {
            val file = createFile("lazyBody", "$header\nfunc host(): Unit {\n$VARIABLE\n}")
            val function = file.declarations.filterIsInstance<CjNamedFunction>().single()
            val block = checkNotNull(function.bodyBlockExpression)
            // 通过真实惰性 block 入口展开子树，不能重新包装到没有宿主语境的文件。
            assertTrue(block.statements.isNotEmpty())
            assertConstSafeForm(block, isBuiltIn)
        }
    }

    @Test
    fun blockCodeFragmentPreservesTheModuleOfItsOriginalContext() {
        for ((header, isBuiltIn) in listOf(
            "package std.annotation_contract" to true,
            "package std::annotation_contract" to true,
            "package std" to false,
            "package example" to false,
            "package org::std.annotation_contract" to false,
        )) {
            val host = createHost(header)
            val fragment = CjPsiFactory(project).createBlockCodeFragment(VARIABLE, host)
            assertSame(host, fragment.getOriginalContext())
            assertConstSafeForm(fragment.getContentElement(), isBuiltIn)
        }
    }

    private fun assertConstSafeForm(root: PsiElement, isBuiltIn: Boolean, macroName: String = "ConstSafe") {
        assertNoErrors(root)
        val variable = PsiTreeUtil.findChildrenOfType(root, CjPatternVariable::class.java).single()
        assertEquals("value", variable.pattern?.text)
        val macros = PsiTreeUtil.findChildrenOfType(root, CjMacroExpression::class.java)
        if (isBuiltIn) {
            kotlin.test.assertEquals(
                "std", CangJieParser.languageModuleNameForContext(root), "builtin annotation source module",
            )
            val annotation = variable.annotationEntries.single()
            assertEquals("ConstSafe", annotation.shortName?.asString())
            assertEquals("@ConstSafe", annotation.text)
            assertFalse(annotation.isCompileTimeVisible)
            assertTrue(macros.isEmpty())
        } else {
            assertTrue(variable.annotationEntries.isEmpty())
            val macro = macros.single()
            assertEquals(macroName, macro.referenceExpression?.text)
            assertSame(variable, macro.unwrappedDeclaration)
        }
    }

    private fun createHost(packageHeader: String): CjNamedFunction {
        val file = createFile("host", "$packageHeader\nfunc host(): Unit {}")
        assertNoErrors(file)
        return file.declarations.filterIsInstance<CjNamedFunction>().single()
    }

    private fun createFile(name: String, source: String): CjFile = createPsiFile(name, source) as CjFile

    private fun assertNoErrors(root: PsiElement) {
        val errors = PsiTreeUtil.findChildrenOfType(root, PsiErrorElement::class.java)
        assertTrue(errors.isEmpty(), errors.joinToString { it.errorDescription })
    }

    companion object {
        private const val VARIABLE = "@ConstSafe\nconst value: Int64 = 1"
    }
}
