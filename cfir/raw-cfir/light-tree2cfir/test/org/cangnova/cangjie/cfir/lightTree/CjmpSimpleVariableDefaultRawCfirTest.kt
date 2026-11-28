package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.simpleVariableBindingOrNull
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.CfirCjmpPreparedGroup
import org.cangnova.cangjie.cfir.session.CfirCjmpClassifierDomain
import org.cangnova.cangjie.cfir.session.defaultCfirCjmpCommonSideFacts
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 普通包级变量与解构声明共享 CFIR 容器，但只有前者对应官方 VarDecl。 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpSimpleVariableDefaultRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsi() {
        val file = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL, CfirAnnotationMetadataBuilder())
            .buildCfirFile(createCjFile("cjmpSimpleDefault", SOURCE))
        checkDeclarations(file)
    }

    fun testLightTree() = checkDeclarations(buildCfirFileFromLightTree(SOURCE, bodyBuildingMode = BodyBuildingMode.NORMAL))

    private fun checkDeclarations(file: CfirFile) {
        val declarations = file.declarations.filterIsInstance<CfirPatternVariable>()
        val ordinary = declarations.mapNotNull { declaration ->
            declaration.pattern.simpleVariableBindingOrNull()?.name?.asString()?.let { it to declaration }
        }.toMap()
        assertEquals(setOf("withDefault", "required"), ordinary.keys)
        assertTrue(ordinary.getValue("withDefault").cjmpHasCommonDefault())
        assertFalse(ordinary.getValue("required").cjmpHasCommonDefault())
        val symbols = ordinary.values.map { it.symbol }
        val family = CfirCjmpPreparedGroup(
            packageFqName = FqName("cjmp_simple_defaults"),
            roots = emptyList(),
            topLevelOrder = symbols,
            originalChildren = emptyMap(),
            commonDeclarations = symbols,
            containers = emptyList(),
            extensions = emptyList(),
            commonExtendOrder = emptyList(),
            originalDoNotExport = emptySet(),
            classifierDomain = CfirCjmpClassifierDomain.capture(file.moduleData.session, ordinary.values, sourceFiles = listOf(file)),
        ).evaluate()
        assertFalse(defaultCfirCjmpCommonSideFacts.mustReportNotMatched(ordinary.getValue("withDefault"), null, family))
        assertTrue(defaultCfirCjmpCommonSideFacts.mustReportNotMatched(ordinary.getValue("required"), null, family))
        val tuple = declarations.single { it.pattern is CfirTuplePattern }
        assertFalse(tuple.cjmpHasCommonDefault())
    }

    companion object {
        private val SOURCE = """
            package cjmp_simple_defaults
            public common let withDefault: Int64 = 1
            public common let required: Int64
            public common let (a, b) = (1, 2)
        """.trimIndent()
    }
}
