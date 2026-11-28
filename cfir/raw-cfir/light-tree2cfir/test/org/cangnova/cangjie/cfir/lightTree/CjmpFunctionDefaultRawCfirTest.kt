package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 函数默认实现事实必须在两种原始构建方式及惰性函数体模式下保持一致。 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpFunctionDefaultRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiNormal() = checkRoleModes(BodyBuildingMode.NORMAL, false)

    fun testPsiLazyBodies() = checkRoleModes(BodyBuildingMode.LAZY_BODIES, false)

    fun testLightTreeNormal() = checkRoleModes(BodyBuildingMode.NORMAL, true)

    fun testLightTreeLazyBodies() = checkRoleModes(BodyBuildingMode.LAZY_BODIES, true)

    fun testPsiOtherLazyBodies() = checkOtherLazyBodies(false)

    fun testLightTreeOtherLazyBodies() = checkOtherLazyBodies(true)

    /** 缺失函数体的声明也覆盖解析恢复，验证不制造虚假的惰性体。 */
    private fun checkOtherLazyBodies(lightTree: Boolean) {
        for (kind in OtherKind.entries) {
            for (hasBody in listOf(false, true)) {
                val suffix = if (hasBody) " {}" else ""
                val text = when (kind) {
                    OtherKind.CONSTRUCTOR -> "class Owner { init()$suffix }"
                    OtherKind.FINALIZER -> "class Owner { ~init()$suffix }"
                    OtherKind.GETTER -> "class Owner { prop value: Int64 { get()$suffix } }"
                    OtherKind.SETTER -> "class Owner { prop value: Int64 { set(value)$suffix } }"
                    OtherKind.MACRO -> "macro example(input: Tokens): Tokens$suffix"
                    OtherKind.MAIN -> "main(): Int64$suffix"
                }
                val mode = BodyBuildingMode.LAZY_BODIES
                val file = if (lightTree) buildCfirFileFromLightTree(text, bodyBuildingMode = mode)
                else PsiRawCfirBuilder(createTestSession(), mode, CfirAnnotationMetadataBuilder())
                    .buildCfirFile(createCjFile("otherLazyBody", text))
                val function: CfirFunction = when (kind) {
                    OtherKind.MACRO -> file.declarations.filterIsInstance<CfirMacroDeclaration>().single()
                    OtherKind.MAIN -> file.declarations.filterIsInstance<CfirMainFunction>().single()
                    else -> {
                        val members = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single().declarations
                        when (kind) {
                            OtherKind.CONSTRUCTOR -> members.filterIsInstance<CfirConstructor>().single { !it.isPrimary }
                            OtherKind.FINALIZER -> members.filterIsInstance<CfirFinalizer>().single()
                            OtherKind.GETTER -> assertNotNull(members.filterIsInstance<CfirProperty>().single().getter, text)
                            OtherKind.SETTER -> assertNotNull(members.filterIsInstance<CfirProperty>().single().setter, text)
                            else -> error("Unexpected declaration kind: $kind")
                        }
                    }
                }
                if (hasBody) assertIs<CfirLazyBlock>(function.body, text) else assertNull(function.body, text)
            }
        }
    }

    /** 在各自的编译会话中分别验证 common 和 specific 默认实现事实。 */
    private fun checkRoleModes(mode: BodyBuildingMode, lightTree: Boolean) {
        for (specific in listOf(false, true)) {
            val session = createTestSession().apply {
                register(
                    CfirCjmpSettingsComponent::class,
                    CfirCjmpSettingsComponent(explicitMode = if (specific) CfirCjmpMode.SPECIFIC else CfirCjmpMode.COMMON),
                )
            }
            val source = if (specific) SPECIFIC_SOURCE else COMMON_SOURCE
            val file = if (lightTree) {
                buildCfirFileFromLightTree(source, session = session, bodyBuildingMode = mode)
            } else {
                val psi = createCjFile("cjmpFunctionDefault", source)
                psi.cjmpParsingContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
                PsiRawCfirBuilder(session, mode, CfirAnnotationMetadataBuilder()).buildCfirFile(psi)
            }
            checkFunctions(file, mode, specific)
        }
    }

    private fun checkFunctions(file: CfirFile, mode: BodyBuildingMode, specific: Boolean) {
        val functions = file.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name.asString() }
        val expected = CASES.filterValues { it.specific == specific }
        assertEquals(expected.keys, functions.keys)
        for ((name, case) in expected) {
            val function = functions.getValue(name)
            assertEquals(case.specific, function.status.isSpecific, name)
            assertEquals(!case.specific, function.status.isCommon, name)
            assertEquals(case.specific || case.hasBody, function.cjmpHasCommonDefault(), name)
            when {
                !case.hasBody -> assertNull(function.body, name)
                mode == BodyBuildingMode.LAZY_BODIES -> assertIs<CfirLazyBlock>(function.body, name)
                else -> assertTrue(assertNotNull(function.body, name) !is CfirLazyBlock, name)
            }
        }
    }

    private data class Case(val specific: Boolean, val hasBody: Boolean)

    private enum class OtherKind { CONSTRUCTOR, FINALIZER, GETTER, SETTER, MACRO, MAIN }

    companion object {
        private val CASES = mapOf(
            "commonBody" to Case(false, true),
            "commonEmpty" to Case(false, true),
            "commonRequired" to Case(false, false),
            "specificBody" to Case(true, true),
            "specificEmpty" to Case(true, true),
            "specificRequired" to Case(true, false),
        )

        private val COMMON_SOURCE = """
            package cjmp_function_defaults

            public common func commonBody(): Int64 { return 1 }
            public common func commonEmpty() {}
            public common func commonRequired(): Int64
        """.trimIndent()

        private val SPECIFIC_SOURCE = """
            package cjmp_function_defaults

            public specific func specificBody(): Int64 { return 2 }
            public specific func specificEmpty() {}
            public specific func specificRequired(): Int64
        """.trimIndent()
    }
}
