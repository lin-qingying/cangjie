package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializePrimaryConstructorParameterFields
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 非法嵌套类型保留完整错误声明源码，不制造ClassId；构造器和字段不得提升为外层声明。 */
@RunWith(JUnit3RunnerWithInners::class)
class NestedNominalRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiNormalRetainsActualConstructorOwners() = assertPsi(BodyBuildingMode.NORMAL)
    fun testPsiLazyRetainsActualConstructorOwners() = assertPsi(BodyBuildingMode.LAZY_BODIES)
    fun testLightTreeNormalRetainsActualConstructorOwners() = assertLightTree(BodyBuildingMode.NORMAL)
    fun testLightTreeLazyRetainsActualConstructorOwners() = assertLightTree(BodyBuildingMode.LAZY_BODIES)

    private fun assertPsi(mode: BodyBuildingMode) {
        val builder = PsiRawCfirBuilder(createTestSession(), mode, CfirAnnotationMetadataBuilder())
        val file = builder.buildCfirFile(createCjFile("nestedOwners", SOURCE))
        materializePrimaryConstructorParameterFields(file, builder.annotationMetadata)
        assertOwners(file)
    }

    private fun assertLightTree(mode: BodyBuildingMode) {
        assertOwners(buildCfirFileFromLightTree(SOURCE, bodyBuildingMode = mode))
    }

    private fun assertOwners(file: CfirFile) {
        assertEquals(listOf("Outer", "Following"), file.declarations.filterIsInstance<CfirClass>().map { it.name.asString() })
        val owners = file.declarations.filterIsInstance<CfirClass>().associateBy { it.name.asString() }
        assertEquals(setOf("Outer", "Following"), owners.keys)
        val outer = owners.getValue("Outer")
        assertTrue(outer.declarations.filterIsInstance<CfirFieldVariable>().isEmpty())
        val after = outer.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals("after", after.name.asString())
        val outerConstructor = outer.declarations.filterIsInstance<CfirConstructor>().single()
        assertTrue(outerConstructor.isPrimary)
        assertEquals(CjFakeSourceElementKind.ImplicitConstructor, outerConstructor.source?.kind)
        assertTrue(outerConstructor.valueParameters.isEmpty())

        assertTrue(outer.declarations.filterIsInstance<CfirClass>().isEmpty())
        val inner = outer.declarations.filterIsInstance<CfirInvalidDeclaration>().single()
        assertEquals(
            "Cangjie only supports top-level class declarations, but found illegal non-top-level declaration: Inner",
            inner.reason,
        )
        val innerSource = kotlin.test.assertNotNull(inner.source)
        val innerStart = SOURCE.indexOf("public class Inner")
        val innerEnd = SOURCE.indexOf("\n    func after")
        assertEquals(innerStart, innerSource.startOffset)
        assertEquals(innerEnd, innerSource.endOffset)
        assertEquals(
            "public class Inner {\n        public Inner(public let inner: Int64) {}\n    }",
            SOURCE.substring(innerSource.startOffset, innerSource.endOffset),
        )
        assertEquals(listOf(inner, after), outer.declarations.filterNot { it is CfirConstructor })

        val following = owners.getValue("Following")
        assertEquals(listOf("own"), following.declarations.filterIsInstance<CfirFieldVariable>().map { it.name.asString() })
        assertEquals(listOf("own"), following.declarations.filterIsInstance<CfirConstructor>().single().valueParameters.map { it.name.asString() })
    }

    companion object {
        // nested nominal 的 scope 错误由官方 v1.1.3 authority-* 探针确认；本测试验收错误源码的 raw 恢复。
        private val SOURCE = """
            package nested_nominal_owners

            class Outer {
                public class Inner {
                    public Inner(public let inner: Int64) {}
                }
                func after(): Unit {}
            }
            class Following {
                public Following(public let own: Int64) {}
            }
        """.trimIndent()
    }
}
