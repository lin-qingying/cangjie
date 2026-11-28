package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 原始声明头根据真实直接空块确定 Unit，不依赖 CFIR body 的构建、展开或惰性解析。
 * 嵌套空块在语法上仍是一条直接语句；显式返回类型始终优先于空块事实。
 */
@RunWith(JUnit3RunnerWithInners::class)
class NamedFunctionRawReturnTypeTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiNormalBodiesKeepRawReturnTypeFacts() {
        assertReturnTypes(buildPsi(BodyBuildingMode.NORMAL), BodyBuildingMode.NORMAL)
    }

    fun testPsiLazyBodiesKeepRawReturnTypeFacts() {
        assertReturnTypes(buildPsi(BodyBuildingMode.LAZY_BODIES), BodyBuildingMode.LAZY_BODIES)
    }

    fun testLightTreeNormalBodiesKeepRawReturnTypeFacts() {
        assertReturnTypes(
            buildCfirFileFromLightTree(SOURCE, bodyBuildingMode = BodyBuildingMode.NORMAL),
            BodyBuildingMode.NORMAL,
        )
    }

    fun testLightTreeLazyBodiesKeepRawReturnTypeFacts() {
        assertReturnTypes(
            buildCfirFileFromLightTree(SOURCE, bodyBuildingMode = BodyBuildingMode.LAZY_BODIES),
            BodyBuildingMode.LAZY_BODIES,
        )
    }

    private fun buildPsi(mode: BodyBuildingMode): CfirFile =
        PsiRawCfirBuilder(createTestSession(), mode, CfirAnnotationMetadataBuilder()).buildCfirFile(createCjFile("rawReturnTypes", SOURCE))

    private fun assertReturnTypes(file: CfirFile, mode: BodyBuildingMode) {
        val functions = file.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name.asString() }
        val expectedNames = EMPTY_BLOCK_FUNCTIONS + IMPLICIT_FUNCTIONS + EXPLICIT_FUNCTIONS.keys
        assertEquals(expectedNames.toSet(), functions.keys)
        for ((name, function) in functions) {
            assertEquals(CfirResolvePhase.RAW_CFIR, function.resolvePhase, name)
            val typeRef = function.returnTypeRef
            when (name) {
                in EMPTY_BLOCK_FUNCTIONS -> {
                    assertEquals(ConePrimitiveType.UNIT, assertIs<CfirResolvedTypeRef>(typeRef, name).coneType)
                    assertNull(typeRef.source, "$name has no explicit return type annotation")
                }
                in IMPLICIT_FUNCTIONS -> assertIs<CfirImplicitTypeRef>(typeRef, name)
                else -> {
                    assertFalse(typeRef is CfirImplicitTypeRef, "$name keeps its explicit type")
                    val source = assertNotNull(typeRef.source, "$name keeps its type annotation source")
                    assertEquals(EXPLICIT_FUNCTIONS.getValue(name), SOURCE.substring(source.startOffset, source.endOffset))
                }
            }

            // 仅检查占位对象，禁止读取 CfirLazyBlock.statements/source/coneTypeOrNull。
            if (mode == BodyBuildingMode.LAZY_BODIES) {
                if (name == "bodyless" || name == "explicitBodyless") {
                    assertNull(function.body, "$name has no source body")
                } else {
                    assertIs<CfirLazyBlock>(function.body, "$name retains a lazy source body")
                }
            }
        }
    }

    companion object {
        private val EMPTY_BLOCK_FUNCTIONS = setOf("empty", "commentOnly")
        private val IMPLICIT_FUNCTIONS = setOf("nestedEmpty", "nestedComment", "nonEmpty", "bodyless")
        private val EXPLICIT_FUNCTIONS = mapOf(
            "explicitEmpty" to "Int64",
            "explicitNonEmpty" to "Int64",
            "explicitUnit" to "Unit",
            "explicitBodyless" to "Int64",
        )

        // 无体和显式 Int64 空体只检查 raw 头部事实，不把它们声明为语义检查通过的普通函数。
        private val SOURCE = """
            package raw_return_types

            func empty() {}
            func commentOnly() {
                /* comment with braces: { 1 } */
                // another comment
            }
            func nestedEmpty() { {} }
            func nestedComment() { { /* comment */ } }
            func nonEmpty() { 1 }
            func bodyless()
            func explicitEmpty(): Int64 {}
            func explicitNonEmpty(): Int64 { 1 }
            func explicitUnit(): Unit {}
            func explicitBodyless(): Int64
        """.trimIndent()
    }
}
