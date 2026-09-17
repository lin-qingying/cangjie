package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallSite
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceContainerContext.OuterDeclarationKind
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 双路 Raw CFIR 的枚举项及注解槽位契约。
 *
 * 直接检查参数类型、source 和对象身份，避免 PSI 与 LightTree 同时丢失 payload 时
 * 仅靠两份相同的文本 dump 误判通过。语义依据官方 896235c9 的枚举声明与宏作用域解析。
 */
@RunWith(JUnit3RunnerWithInners::class)
class EnumConstructorAnnotationRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiKeepsEnumPayloadAndAnnotationOwners() {
        val session = createTestSession()
        val builder = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL)
        val file = builder.buildCfirFile(createCjFile("enumAnnotations", SOURCE))
        assertEnumAnnotationContract(file, session.annotationMetadataRegistry, builder.consumeCollectedMacroSurfaces())
    }

    fun testLightTreeKeepsEnumPayloadAndAnnotationOwners() {
        val session = createTestSession()
        val sourceFile = CjInMemoryTextSourceFile("enumAnnotations.cj", null, SOURCE)
        val (file, surfaces) = LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildCfirFileWithSurfaces(parseLightTree(SOURCE), sourceFile, SOURCE.toSourceLinesMapping())
        assertEnumAnnotationContract(file, session.annotationMetadataRegistry, surfaces)
    }

    /** 文件起点的短注解名不能因合成调用前缀过长而偏移其参数 source。 */
    fun testPsiKeepsAnnotationArgumentOffsetsAtFileStart() {
        val session = createTestSession()
        val builder = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL)
        val file = builder.buildCfirFile(createCjFile("fileStartAnnotation", FILE_START_SOURCE))
        assertFileStartAnnotationOffsets(file, session.annotationMetadataRegistry, builder.consumeCollectedMacroSurfaces())
    }

    /** LightTree 路径也必须使用原始方括号位置，不能依赖额外前缀的 padding。 */
    fun testLightTreeKeepsAnnotationArgumentOffsetsAtFileStart() {
        val session = createTestSession()
        val sourceFile = CjInMemoryTextSourceFile("fileStartAnnotation.cj", null, FILE_START_SOURCE)
        val (file, surfaces) = LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildCfirFileWithSurfaces(
                parseLightTree(FILE_START_SOURCE), sourceFile, FILE_START_SOURCE.toSourceLinesMapping(),
            )
        assertFileStartAnnotationOffsets(file, session.annotationMetadataRegistry, surfaces)
    }

    /** 非法注解表达式不能让 attr 中的声明逃逸到 PSI Raw CFIR。 */
    fun testPsiRecoversMalformedAnnotationAttributeWithoutLeakingDeclarations() {
        val session = createTestSession()
        val builder = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL)
        val file = builder.buildCfirFile(createCjFile("malformedEnumAnnotation", RECOVERY_SOURCE))
        assertMalformedAttributeRecovery(file, session.annotationMetadataRegistry, builder.consumeCollectedMacroSurfaces())
    }

    /** LightTree 使用同一原始 attr 边界，恢复时仍保留后续枚举 payload。 */
    fun testLightTreeRecoversMalformedAnnotationAttributeWithoutLeakingDeclarations() {
        val session = createTestSession()
        val sourceFile = CjInMemoryTextSourceFile("malformedEnumAnnotation.cj", null, RECOVERY_SOURCE)
        val (file, surfaces) = LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildCfirFileWithSurfaces(
                parseLightTree(RECOVERY_SOURCE), sourceFile, RECOVERY_SOURCE.toSourceLinesMapping(),
            )
        assertMalformedAttributeRecovery(file, session.annotationMetadataRegistry, surfaces)
    }

    /** 只验证语法恢复和来源；非法 attr 的参数无需成为有效语义表达式。 */
    private fun assertMalformedAttributeRecovery(
        file: CfirFile,
        metadata: CfirAnnotationMetadataRegistry,
        surfaces: List<MacroSurface>,
    ) {
        fun originalText(source: CjSourceElement?): String {
            val sourceElement = checkNotNull(source)
            return RECOVERY_SOURCE.substring(sourceElement.startOffset, sourceElement.endOffset)
        }

        val enum = file.declarations.single() as CfirEnum
        assertEquals("E", enum.name.asString())
        val item = enum.declarations.filterIsInstance<CfirEnumConstructor>().single()
        assertEquals("item", item.name.asString())
        // Raw builder 可以生成隐式初始化节点，attr 中的 leaked 不能成为任何额外成员。
        assertTrue(enum.declarations.all { it === item || it is CfirConstructor })
        val payload = item.valueParameters.single()
        assertEquals("Int64", originalText(payload.returnTypeRef.source))
        assertSame(item.symbol, payload.containingDeclarationSymbol)

        val annotation = item.annotations.single() as CfirAnnotationCall
        assertEquals("A", annotation.annotationSourceName)
        assertSame(item.symbol, annotation.containingDeclarationSymbol)
        assertEquals(RECOVERY_ANNOTATION, originalText(annotation.source))
        val snapshot = metadata.snapshots.single()
        assertSame(item, snapshot.owner)
        assertSame(annotation, snapshot.originalAnnotation)
        assertEquals(0, snapshot.annotationIndex)
        assertEquals(RECOVERY_ANNOTATION, snapshot.rawSyntax)
        assertEquals(RECOVERY_ANNOTATION.removePrefix("@A"), snapshot.argumentText)
        assertEquals(RECOVERY_ANNOTATION, originalText(snapshot.annotationSource))
        val annotationStart = RECOVERY_SOURCE.indexOf('@')
        assertEquals(annotationStart, snapshot.annotationSource.startOffset)
        assertEquals(annotationStart + RECOVERY_ANNOTATION.length, snapshot.annotationSource.endOffset)

        val surface = surfaces.single()
        assertSame(snapshot, metadata.snapshotForSurface(surface))
        assertSame(item, surface.replaceHandle.annotationCarrier?.owner)
        assertEquals(annotationStart, checkNotNull(surface.sourceRange).startOffset)
        assertTrue(checkNotNull(surface.capturedRawSyntax).startsWith(RECOVERY_ANNOTATION))
    }

    private fun assertFileStartAnnotationOffsets(
        file: CfirFile,
        metadata: CfirAnnotationMetadataRegistry,
        surfaces: List<MacroSurface>,
    ) {
        fun assertRange(source: CjSourceElement?, start: Int, end: Int, text: String) {
            val sourceElement = checkNotNull(source)
            assertEquals(start, sourceElement.startOffset)
            assertEquals(end, sourceElement.endOffset)
            assertEquals(text, FILE_START_SOURCE.substring(sourceElement.startOffset, sourceElement.endOffset))
        }

        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals("f", function.name.asString())
        val annotation = function.annotations.single() as CfirAnnotationCall
        assertEquals("A", annotation.annotationSourceName)
        assertRange(annotation.source, 0, 15, "@A[1, value: 2]")
        assertRange(annotation.calleeReference.source, 1, 2, "A")
        assertEquals(2, annotation.arguments.size)
        val first = annotation.arguments[0] as CfirLiteralExpression
        assertEquals(CfirLiteralKind.INT, first.kind)
        assertEquals("1", first.value)
        assertRange(first.source, 3, 4, "1")
        val named = annotation.arguments[1] as CfirNamedArgumentExpression
        assertEquals("value", named.argumentName.asString())
        assertRange(named.nameSource, 6, 11, "value")
        assertRange(named.source, 6, 14, "value: 2")
        val second = named.expression as CfirLiteralExpression
        assertEquals(CfirLiteralKind.INT, second.kind)
        assertEquals("2", second.value)
        assertRange(second.source, 13, 14, "2")

        val snapshot = metadata.snapshots.single()
        assertSame(function, snapshot.owner)
        assertSame(annotation, snapshot.originalAnnotation)
        assertRange(snapshot.annotationSource, 0, 15, "@A[1, value: 2]")
        val surface = surfaces.single()
        assertSame(snapshot, metadata.snapshotForSurface(surface))
        assertEquals(0, checkNotNull(surface.sourceRange).startOffset)
        assertEquals(FILE_START_SOURCE, surface.capturedRawSyntax)
    }

    private fun assertEnumAnnotationContract(
        file: CfirFile,
        metadata: CfirAnnotationMetadataRegistry,
        surfaces: List<MacroSurface>,
    ) {
        val enums = file.declarations.filterIsInstance<CfirEnum>()
        assertEquals(listOf("E", "WithLeadingBar", "MixedPrefixes"), enums.map { it.name.asString() })
        val first = enums[0]
        val second = enums[1]
        val mixed = enums[2]
        val firstCases = first.declarations.filterIsInstance<CfirEnumConstructor>()
        val secondCases = second.declarations.filterIsInstance<CfirEnumConstructor>()
        val mixedCases = mixed.declarations.filterIsInstance<CfirEnumConstructor>()
        assertConstructors(
            firstCases,
            listOf("caseA", "caseB", "caseC"),
            listOf(emptyList(), listOf("Int64"), listOf("Int64", "Bool")),
        )
        assertConstructors(secondCases, listOf("first", "empty"), listOf(listOf("Int64"), emptyList()))
        assertConstructors(
            mixedCases,
            listOf("builtinBeforeWrapper", "forcedBeforeWrapper", "forcedAfterWrapper", "builtinAfterWrapper"),
            List(4) { emptyList() },
        )

        val function = first.declarations.filterIsInstance<CfirNamedFunction>().single()
        val property = first.declarations.filterIsInstance<CfirProperty>().single()
        assertEquals("number", function.name.asString())
        assertEquals("value", property.name.asString())

        val tag1 = ExpectedAnnotation("CaseTag", "@CaseTag[1]", listOf(ExpectedArgument("1")))
        val tag2 = ExpectedAnnotation("CaseTag", "@CaseTag[2]", listOf(ExpectedArgument("2")))
        val forcedTag3 = ExpectedAnnotation("CaseTag", "@!CaseTag[3]", listOf(ExpectedArgument("3")), true)
        val version = ExpectedAnnotation("CaseVersion", "@CaseVersion[]")
        val forcedVersion = ExpectedAnnotation("CaseVersion", "@!CaseVersion[]", compileTimeVisible = true)
        val deprecated = ExpectedAnnotation(
            "Deprecated",
            "@Deprecated[message: \"old\"]",
            listOf(ExpectedArgument("\"old\"", "old", CfirLiteralKind.STRING, "message")),
        )
        val expectedSlots = listOf(
            Triple(first, firstCases[0], listOf(tag1, version)),
            Triple(first, firstCases[1], listOf(tag2, version)),
            Triple(first, firstCases[2], listOf(forcedTag3)),
            Triple(
                first, function,
                listOf(ExpectedAnnotation("MemberTag", "@MemberTag[4]", listOf(ExpectedArgument("4")))),
            ),
            Triple(
                first, property,
                listOf(ExpectedAnnotation("MemberTag", "@MemberTag[5]", listOf(ExpectedArgument("5")))),
            ),
            Triple(
                second, secondCases[0],
                listOf(ExpectedAnnotation("CaseTag", "@CaseTag[6]", listOf(ExpectedArgument("6"))), version),
            ),
            Triple(
                second, secondCases[1],
                listOf(ExpectedAnnotation("CaseTag", "@CaseTag[7]", listOf(ExpectedArgument("7")))),
            ),
            Triple(mixed, mixedCases[0], listOf(deprecated, tag1)),
            Triple(mixed, mixedCases[1], listOf(forcedVersion, tag2)),
            Triple(mixed, mixedCases[2], listOf(version, forcedTag3)),
            Triple(mixed, mixedCases[3], listOf(version, deprecated)),
        )
        for ((enclosingEnum, owner, annotations) in expectedSlots) {
            assertAnnotationSlots(enclosingEnum, owner, annotations, metadata, surfaces)
        }
        val slotCount = expectedSlots.sumOf { it.third.size }
        assertEquals(slotCount, metadata.snapshots.size)
        assertEquals(slotCount, surfaces.size)
        assertEquals(slotCount, surfaces.map { it.surfaceId }.toSet().size)
        assertEquals(slotCount, metadata.snapshots.map { it.annotationSource.startOffset }.toSet().size)

        // 枚举内部生成的初始化节点不能夺走枚举项的注解或 payload。
        for (enum in enums) {
            assertTrue(enum.annotations.isEmpty())
            for (initializer in enum.declarations.filterIsInstance<CfirConstructor>()) {
                assertTrue(initializer.annotations.isEmpty())
                assertTrue(initializer.valueParameters.isEmpty())
            }
        }
    }

    private fun assertConstructors(
        constructors: List<CfirEnumConstructor>,
        names: List<String>,
        parameterTypes: List<List<String>>,
    ) {
        assertEquals(names, constructors.map { it.name.asString() })
        assertEquals(constructors.size, constructors.map { it.symbol }.toSet().size)
        assertEquals(parameterTypes, constructors.map { constructor ->
            constructor.valueParameters.map { sourceText(it.returnTypeRef.source) }
        })
        for (constructor in constructors) {
            assertNotNull(constructor.source)
            constructor.valueParameters.forEach { parameter ->
                assertSame(constructor.symbol, parameter.containingDeclarationSymbol)
                assertEquals(sourceText(parameter.returnTypeRef.source), sourceText(parameter.source))
            }
        }
    }

    private fun assertAnnotationSlots(
        enclosingEnum: CfirEnum,
        owner: CfirDeclaration,
        expectedAnnotations: List<ExpectedAnnotation>,
        metadata: CfirAnnotationMetadataRegistry,
        surfaces: List<MacroSurface>,
    ) {
        assertEquals(expectedAnnotations.size, owner.annotations.size)
        val annotationSources = owner.annotations.map { checkNotNull(it.source) }
        val sourceOffsets = annotationSources.map { it.startOffset }
        assertEquals(sourceOffsets.sorted(), sourceOffsets, "annotation slots must follow source order")
        for ((previous, next) in annotationSources.zipWithNext()) {
            assertTrue(previous.endOffset <= next.startOffset, "adjacent annotation sources must not overlap")
        }
        for ((index, expected) in expectedAnnotations.withIndex()) {
            val annotation = owner.annotations[index] as CfirAnnotationCall
            val snapshot = checkNotNull(metadata.snapshot(annotation))
            val annotationSource = checkNotNull(annotation.source)
            assertSame(owner, snapshot.owner)
            assertSame(owner.symbol, annotation.containingDeclarationSymbol)
            assertSame(annotation, snapshot.originalAnnotation)
            assertEquals(index, snapshot.annotationIndex)
            assertEquals(MacroCallSite.DECLARATION, snapshot.callSite)
            assertEquals(expected.text, snapshot.rawSyntax)
            assertEquals(expected.text, sourceText(snapshot.annotationSource))
            assertEquals(expected.text, sourceText(annotationSource))
            assertEquals(expected.name, annotation.annotationSourceName)
            assertEquals(expected.name, snapshot.qualifiedName?.asString())
            assertEquals(expected.compileTimeVisible, snapshot.forcedCustom)
            assertEquals(expected.compileTimeVisible, snapshot.isCompileTimeVisible)
            assertEquals(expected.compileTimeVisible, annotation.forcedCustom)
            assertEquals(expected.compileTimeVisible, annotation.isCompileTimeVisible)

            val callee = annotation.calleeReference as CfirNamedReference
            val calleeSource = checkNotNull(callee.source)
            assertEquals(expected.name, callee.name.asString())
            assertEquals(expected.name, sourceText(calleeSource))
            assertEquals(expected.name, sourceText(annotation.typeRef.source))
            val annotationPrefixLength = if (expected.compileTimeVisible) 2 else 1
            assertEquals(annotationSource.startOffset + annotationPrefixLength, calleeSource.startOffset)
            assertEquals(calleeSource.startOffset + expected.name.length, calleeSource.endOffset)
            assertSourceWithin(annotationSource, annotation.typeRef.source)
            assertAnnotationArguments(annotation, expected.arguments)

            val surface = surfaces.single { it.replaceHandle.annotationCarrier?.originalAnnotation === annotation }
            assertSame(owner, surface.replaceHandle.annotationCarrier?.owner)
            assertSame(snapshot, metadata.snapshotForSurface(surface))
            assertEquals(expected.name, surface.qualifiedName?.asString())
            val surfaceRange = checkNotNull(surface.sourceRange)
            val surfaceSyntax = checkNotNull(surface.capturedRawSyntax)
            assertEquals(annotationSource.startOffset, surfaceRange.startOffset)
            assertTrue(surfaceRange.endOffset >= annotationSource.endOffset)
            assertTrue(surfaceSyntax.startsWith(expected.text), "surface syntax must start at its own annotation")
            assertEquals(SOURCE.substring(surfaceRange.startOffset, surfaceRange.endOffset), surfaceSyntax)
            assertEquals(
                if (expected.compileTimeVisible) MacroSurface.Kind.FORCED else MacroSurface.Kind.PLAIN,
                surface.kind,
            )
            assertEquals(OuterDeclarationKind.ENUM_BODY, surface.containerContext.outerDeclarationKind)
            assertTrue(surface.containerContext.isInsideEnumBody)
            assertEquals(enclosingEnum.symbol.classId.asSingleFqName(), surface.scopeContext.enclosingClassFqName)
        }
    }

    /** 参数名称、值和 source 均属于当前注解，不能借用相邻 wrapper 的 attr。 */
    private fun assertAnnotationArguments(annotation: CfirAnnotationCall, expectedArguments: List<ExpectedArgument>) {
        assertEquals(expectedArguments.size, annotation.arguments.size)
        val annotationSource = checkNotNull(annotation.source)
        for ((index, expected) in expectedArguments.withIndex()) {
            val argument = annotation.arguments[index]
            val expression = when (argument) {
                is CfirNamedArgumentExpression -> {
                    assertEquals(expected.name, argument.argumentName.asString())
                    assertEquals(expected.name, sourceText(argument.nameSource))
                    assertSourceWithin(annotationSource, argument.nameSource)
                    argument.expression
                }
                else -> {
                    assertNull(expected.name)
                    argument
                }
            }
            val literal = expression as CfirLiteralExpression
            assertEquals(expected.text, sourceText(literal.source))
            assertEquals(expected.kind, literal.kind)
            assertEquals(expected.value, literal.value)
            assertSourceWithin(annotationSource, literal.source)
        }
    }

    private fun assertSourceWithin(ownerSource: CjSourceElement, childSource: CjSourceElement?) {
        val child = checkNotNull(childSource)
        assertTrue(child.startOffset >= ownerSource.startOffset)
        assertTrue(child.endOffset <= ownerSource.endOffset)
        assertTrue(child.startOffset < child.endOffset)
    }

    private data class ExpectedAnnotation(
        val name: String,
        val text: String,
        val arguments: List<ExpectedArgument> = emptyList(),
        val compileTimeVisible: Boolean = false,
    )

    private data class ExpectedArgument(
        val text: String,
        val value: String = text,
        val kind: CfirLiteralKind = CfirLiteralKind.INT,
        val name: String? = null,
    )

    private fun sourceText(source: CjSourceElement?): String {
        val sourceElement = checkNotNull(source)
        return SOURCE.substring(sourceElement.startOffset, sourceElement.endOffset)
    }

    companion object {
        private const val FILE_START_SOURCE = "@A[1, value: 2] func f() {}"
        private const val RECOVERY_ANNOTATION = "@A[}\nfunc leaked() {}\n]"
        private const val RECOVERY_SOURCE = "enum E { $RECOVERY_ANNOTATION item(Int64) }"

        private val SOURCE = """
            enum E {
                @CaseTag[1]
                @CaseVersion[]
                caseA
                | @CaseTag[2]
                @CaseVersion[]
                caseB(Int64)
                | @!CaseTag[3]
                caseC(Int64, Bool)

                @MemberTag[4]
                public func number(): Int64 { 1 }

                @MemberTag[5]
                public prop value: Int64 { get() { 2 } }
            }

            enum WithLeadingBar {
                | @CaseTag[6]
                @CaseVersion[]
                first(Int64)
                | @CaseTag[7]
                empty
            }

            enum MixedPrefixes {
                @Deprecated[message: "old"]
                @CaseTag[1]
                builtinBeforeWrapper
                | @!CaseVersion[]
                @CaseTag[2]
                forcedBeforeWrapper
                | @CaseVersion[]
                @!CaseTag[3]
                forcedAfterWrapper
                | @CaseVersion[]
                @Deprecated[message: "old"]
                builtinAfterWrapper
            }
        """.trimIndent()
    }
}
