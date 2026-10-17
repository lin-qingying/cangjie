package org.cangnova.cangjie.psi

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertSame

/** 构造器和终结器沿 PSI 父链查找所属类型，兼容宏表达式插入的包装层。 */
class ConstructorContainingTypeTest : CjParsingTestCase(
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
    fun constructorsAndFinalizerFindTheirTypeAcrossMacroParent() {
        val file = createPsiFile(
            "constructorContainingType",
            """
            class MacroWrappedType {
                MacroWrappedType(primary: Int64) {}

                @ConstructorProbe
                init(secondary: Int64) {}

                ~init() {}
            }
            """.trimIndent(),
        ) as CjFile
        val type = PsiTreeUtil.findChildrenOfType(file, CjTypeStatement::class.java).single()
        val primaryConstructor = checkNotNull(type.primaryConstructor)
        val macro = PsiTreeUtil.findChildrenOfType(type, CjMacroExpression::class.java).single()
        val secondaryConstructor = checkNotNull(macro.unwrappedDeclaration) as CjSecondaryConstructor
        val finalizer = PsiTreeUtil.findChildrenOfType(type, CjFinalizer::class.java).single()

        assertSame(type, primaryConstructor.getContainingTypeStatement())
        assertSame(type, secondaryConstructor.getContainingTypeStatement())
        assertSame(type, finalizer.getContainingTypeStatement())
    }
}
