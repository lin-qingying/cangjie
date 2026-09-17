package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallSite
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 主构造 @! 参数与对应属性共享源码 provenance，各自持有独立注解槽位。
 * 每条源码注解只创建一个 macro surface；派生属性不得重复参与宏构造。
 */
@RunWith(JUnit3RunnerWithInners::class)
class CompileTimeVisibleParameterRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiKeepsAnnotatedParameterPropertiesAndSlots() {
        val session = createTestSession()
        val builder = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL)
        val file = builder.buildCfirFile(createCjFile("compileTimeParameters", SOURCE))
        assertParameterContract(file, session.annotationMetadataRegistry, builder.consumeCollectedMacroSurfaces())
    }

    fun testLightTreeKeepsAnnotatedParameterPropertiesAndSlots() {
        val session = createTestSession()
        val sourceFile = CjInMemoryTextSourceFile("compileTimeParameters.cj", null, SOURCE)
        val (file, surfaces) = LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildCfirFileWithSurfaces(parseLightTree(SOURCE), sourceFile, SOURCE.toSourceLinesMapping())
        assertParameterContract(file, session.annotationMetadataRegistry, surfaces)
    }

    private fun assertParameterContract(
        file: CfirFile,
        metadata: CfirAnnotationMetadataRegistry,
        surfaces: List<MacroSurface>,
    ) {
        val holder = file.declarations.filterIsInstance<CfirClass>().single()
        assertEquals("Holder", holder.name.asString())
        val constructor = holder.declarations.filterIsInstance<CfirConstructor>().single()
        assertTrue(constructor.isPrimary)
        val parameter = constructor.valueParameters.single()
        assertEquals("v", parameter.name.asString())
        assertEquals("Int64", sourceText(parameter.returnTypeRef.source))
        assertTrue(parameter.isNamed)
        assertDefaultValue(parameter, "1")
        assertSame(constructor.symbol, parameter.containingDeclarationSymbol)

        val property = holder.declarations.filterIsInstance<CfirProperty>().single()
        assertEquals("v", property.name.asString())
        assertSame(property, parameter.correspondingProperty)
        assertEquals(Visibilities.Public, property.status.visibility)
        assertTrue(property.status.isMut)
        checkNotNull(property.getter)
        checkNotNull(property.setter)
        assertEquals(sourceText(parameter.source), sourceText(property.source))

        val sourceAnnotation = parameter.annotations.single() as CfirAnnotationCall
        val derivedAnnotation = property.annotations.single() as CfirAnnotationCall
        assertNotSame(sourceAnnotation, derivedAnnotation)
        assertSame(constructor.symbol, sourceAnnotation.containingDeclarationSymbol)
        assertSame(property.symbol, derivedAnnotation.containingDeclarationSymbol)
        assertAnnotation(sourceAnnotation, "1")
        assertAnnotation(derivedAnnotation, "1")
        val sourceSlot = checkNotNull(metadata.snapshot(sourceAnnotation))
        val derivedSlot = checkNotNull(metadata.snapshot(derivedAnnotation))
        assertSame(parameter, sourceSlot.owner)
        assertSame(property, derivedSlot.owner)
        assertSame(sourceAnnotation, sourceSlot.originalAnnotation)
        assertSame(derivedAnnotation, derivedSlot.originalAnnotation)
        assertEquals(0, sourceSlot.annotationIndex)
        assertEquals(0, derivedSlot.annotationIndex)
        assertCopiedProvenance(sourceSlot, derivedSlot)

        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals("ordinary", function.name.asString())
        val ordinaryParameters = function.valueParameters
        assertEquals(listOf("value", "other"), ordinaryParameters.map { it.name.asString() })
        assertEquals(listOf(false, true), ordinaryParameters.map { it.isNamed })
        assertNull(ordinaryParameters[0].defaultValue)
        assertDefaultValue(ordinaryParameters[1], "2")
        for ((index, ordinaryParameter) in ordinaryParameters.withIndex()) {
            assertEquals("Int64", sourceText(ordinaryParameter.returnTypeRef.source))
            assertNull(ordinaryParameter.correspondingProperty)
            assertSame(function.symbol, ordinaryParameter.containingDeclarationSymbol)
            val annotation = ordinaryParameter.annotations.single() as CfirAnnotationCall
            assertSame(function.symbol, annotation.containingDeclarationSymbol)
            assertAnnotation(annotation, (index + 2).toString())
            assertSame(ordinaryParameter, checkNotNull(metadata.snapshot(annotation)).owner)
        }

        assertTrue(holder.annotations.isEmpty())
        assertTrue(constructor.annotations.isEmpty())
        assertTrue(function.annotations.isEmpty())
        assertEquals(4, metadata.snapshots.size)
        assertEquals(3, surfaces.size)
        assertEquals(3, surfaces.map { checkNotNull(it.sourceRange).startOffset }.toSet().size)
        assertTrue(surfaces.all { it is MacroSurfaceParam && it.kind == MacroSurface.Kind.FORCED })
        for (sourceParameter in listOf(parameter) + ordinaryParameters) {
            val annotation = sourceParameter.annotations.single() as CfirAnnotationCall
            val surface = surfaces.single { it.replaceHandle.annotationCarrier?.originalAnnotation === annotation }
            assertSame(sourceParameter, surface.replaceHandle.annotationCarrier?.owner)
            val snapshot = checkNotNull(metadata.snapshotForSurface(surface))
            assertSame(sourceParameter, snapshot.owner)
            assertEquals(MacroCallSite.PARAMETER, snapshot.callSite)
            assertEquals(snapshot.annotationSource.startOffset, checkNotNull(surface.sourceRange).startOffset)
        }
        assertTrue(surfaces.none { it.replaceHandle.annotationCarrier?.owner === property })
    }

    private fun assertAnnotation(annotation: CfirAnnotationCall, argumentValue: String) {
        assertEquals("ParamTag", annotation.annotationSourceName)
        assertTrue(annotation.forcedCustom)
        assertEquals(true, annotation.isCompileTimeVisible)
        assertEquals("@!ParamTag[$argumentValue]", sourceText(annotation.source))
        val argument = annotation.arguments.single() as CfirLiteralExpression
        assertEquals(CfirLiteralKind.INT, argument.kind)
        assertEquals(argumentValue, argument.value)
        assertEquals(argumentValue, sourceText(argument.source))
        val annotationSource = checkNotNull(annotation.source)
        val argumentSource = checkNotNull(argument.source)
        assertTrue(argumentSource.startOffset >= annotationSource.startOffset)
        assertTrue(argumentSource.endOffset <= annotationSource.endOffset)
    }

    private fun assertDefaultValue(parameter: CfirValueParameter, text: String) {
        val value = checkNotNull(parameter.defaultValue) as CfirLiteralExpression
        assertEquals(CfirLiteralKind.INT, value.kind)
        assertEquals(text, value.value)
        assertEquals(text, sourceText(value.source))
    }

    private fun assertCopiedProvenance(source: CfirAnnotationSlotSnapshot, derived: CfirAnnotationSlotSnapshot) {
        assertEquals("@!ParamTag[1]", source.rawSyntax)
        assertEquals(source.rawSyntax, derived.rawSyntax)
        assertEquals(source.argumentText, derived.argumentText)
        assertEquals(source.qualifiedName, derived.qualifiedName)
        assertSame(source.annotationSource, derived.annotationSource)
        assertEquals(source.tokens, derived.tokens)
        assertEquals(MacroCallSite.PARAMETER, source.callSite)
        assertEquals(source.callSite, derived.callSite)
        assertTrue(source.forcedCustom && derived.forcedCustom)
        assertTrue(source.isCompileTimeVisible && derived.isCompileTimeVisible)
    }

    private fun sourceText(source: CjSourceElement?): String {
        val sourceElement = checkNotNull(source)
        return SOURCE.substring(sourceElement.startOffset, sourceElement.endOffset)
    }

    companion object {
        private val SOURCE = """
            class Holder {
                public Holder(@!ParamTag[1] public var v!: Int64 = 1) {}
            }

            func ordinary(@!ParamTag[2] value: Int64, @!ParamTag[3] other!: Int64 = 2): Int64 {
                value + other
            }
        """.trimIndent()
    }
}
