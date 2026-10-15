package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.ObsoleteTestInfrastructure
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith

/** 声明宏嵌套成员宏时，Raw CFIR 必须按 PSI 声明边界构建一次。 */
@RunWith(JUnit3RunnerWithInners::class)
@OptIn(ObsoleteTestInfrastructure::class)
@ObsoleteTestInfrastructure
class PsiRawCfirBuilderNestedMacroTest : AbstractRawCfirBuilderTestCase() {
    fun testNestedClassMemberMacrosBuildOneClassAndAllMacroSurfaces() {
        val file = createCjFile(
            "nestedClassMemberMacros",
            """
            package nestedmacros

            @ContextOuter
            class ContextMacroProbe {
                @ContextInner
                var firstCount: Int64 = 40

                @ContextInner
                var secondCount: Int64 = 2
            }
            """.trimIndent(),
        )
        val builder = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)

        val cfirFile = builder.buildCfirFile(file)
        val macroSurfaces = builder.consumeCollectedMacroSurfaces()

        assertEquals(1, cfirFile.declarations.size)
        assertEquals(3, macroSurfaces.size)
    }
}
