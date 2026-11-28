package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** 宏注解 fragment 的名称和每段源码范围必须来自 PSI 类型语法。 */
@RunWith(JUnit3RunnerWithInners::class)
class PsiAnnotationFragmentQualifiedNameRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testQualifiedUnicodeAnnotationKeepsSegmentSources() {
        // cjc 1.1.3 接受该名称语法，仅报告 macro package 未定义。
        val source = "package qualified_annotation\nclass Owner { @prefix./* gap */`cafe\u0301` func f(): Unit {} }"
        val session = createTestSession()
        val file = createCjFile("qualifiedAnnotation", source)
        val original = PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
            .buildPreMacroCfirFile(file)
        val surface = original.surfaces.single()
        assertEquals("prefix.café", surface.qualifiedName?.asString())
        assertTrue(surface.isQualifiedName)
        val snapshot = checkNotNull(original.annotationMetadata.snapshotForSurface(surface))
        val annotation = CjPsiFactory.contextual(file).createAnnotations(snapshot.rawSyntax).entries.single()
        val rebuilt = PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
            .buildAnnotationCallFragment(annotation, snapshot.owner.symbol, surface.replaceHandle.ownerSnapshot, snapshot.annotationSource)
        val qualifier = assertIs<CfirUserTypeRef>(rebuilt.typeRef).qualifier
        assertEquals(listOf("prefix", "café"), qualifier.map { it.name.asString() })
        assertEquals(listOf("prefix", "`cafe\u0301`"), qualifier.map {
            val range = checkNotNull(it.source)
            source.substring(range.startOffset, range.endOffset)
        })
        val callee = assertIs<CfirNamedReference>(rebuilt.calleeReference)
        val range = checkNotNull(callee.source)
        assertEquals("`cafe\u0301`", source.substring(range.startOffset, range.endOffset))
    }
}
