package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializePrimaryConstructorParameterFields
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.correspondingField
import org.cangnova.cangjie.cfir.correspondingValueParameter
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.memberParameterFieldInfo
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.parsing.CangJieParser
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 主构造 let/var 参数生成真实实例字段；普通参数、显式 prop 和 enum 载荷保持各自声明种类。 */
@RunWith(JUnit3RunnerWithInners::class)
class PrimaryConstructorMemberFieldRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiNormalMemberParameterFields() = assertPsi(BodyBuildingMode.NORMAL)
    fun testPsiLazyMemberParameterFields() = assertPsi(BodyBuildingMode.LAZY_BODIES)
    fun testLightTreeNormalMemberParameterFields() = assertLightTree(BodyBuildingMode.NORMAL)
    fun testLightTreeLazyMemberParameterFields() = assertLightTree(BodyBuildingMode.LAZY_BODIES)
    fun testPsiNormalWrappedPrimaryParameterFacts() = assertWrappedPrimaryFacts(BodyBuildingMode.NORMAL, false)
    fun testPsiLazyWrappedPrimaryParameterFacts() = assertWrappedPrimaryFacts(BodyBuildingMode.LAZY_BODIES, false)
    fun testLightTreeNormalWrappedPrimaryParameterFacts() = assertWrappedPrimaryFacts(BodyBuildingMode.NORMAL, true)
    fun testLightTreeLazyWrappedPrimaryParameterFacts() = assertWrappedPrimaryFacts(BodyBuildingMode.LAZY_BODIES, true)
    fun testPsiNormalMemberFactOwnershipAndRecovery() = assertMemberFactOwnershipAndRecovery(BodyBuildingMode.NORMAL, false)
    fun testPsiLazyMemberFactOwnershipAndRecovery() = assertMemberFactOwnershipAndRecovery(BodyBuildingMode.LAZY_BODIES, false)
    fun testLightTreeNormalMemberFactOwnershipAndRecovery() = assertMemberFactOwnershipAndRecovery(BodyBuildingMode.NORMAL, true)
    fun testLightTreeLazyMemberFactOwnershipAndRecovery() = assertMemberFactOwnershipAndRecovery(BodyBuildingMode.LAZY_BODIES, true)
    fun testPsiNormalSharedFieldCompletionBoundaries() = assertSharedFieldCompletionBoundaries(BodyBuildingMode.NORMAL, false)
    fun testPsiLazySharedFieldCompletionBoundaries() = assertSharedFieldCompletionBoundaries(BodyBuildingMode.LAZY_BODIES, false)
    fun testLightTreeNormalSharedFieldCompletionBoundaries() = assertSharedFieldCompletionBoundaries(BodyBuildingMode.NORMAL, true)
    fun testLightTreeLazySharedFieldCompletionBoundaries() = assertSharedFieldCompletionBoundaries(BodyBuildingMode.LAZY_BODIES, true)

    private fun assertPsi(mode: BodyBuildingMode) {
        val raw = buildRawFixture(SOURCE, mode, useLightTree = false)
        materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        assertFields(raw.cfirFile)
    }

    private fun assertLightTree(mode: BodyBuildingMode) {
        val raw = buildRawFixture(SOURCE, mode, useLightTree = true)
        materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        assertFields(raw.cfirFile)
    }

    /** 只读取 raw 头部、关联和参数标志，不访问 lazy body 的内容。 */
    private fun assertFields(file: CfirFile) {
        val classOwner = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "ClassOwner" }
        val structOwner = file.declarations.filterIsInstance<CfirStruct>().single()
        for (owner in listOf<CfirClassLikeDeclaration>(classOwner, structOwner)) {
            val primary = owner.declarations.filterIsInstance<CfirConstructor>().single { it.isPrimary }
            val parameters = primary.valueParameters.associateBy { it.name.asString() }
            assertEquals(setOf("immutable", "ordinary", "mutable"), parameters.keys)
            val fields = owner.declarations.filterIsInstance<CfirFieldVariable>().associateBy { it.name.asString() }
            assertEquals(setOf("immutable", "mutable", "declared"), fields.keys)
            for ((name, isMutable) in listOf("immutable" to false, "mutable" to true)) {
                val parameter = parameters.getValue(name)
                val field = fields.getValue(name)
                assertTrue(parameter.isMemberParameter, "$name parameter retains its independent member flag")
                assertTrue(field.isMemberParameter, "$name field retains its independent member flag")
                assertSame(field, parameter.correspondingField)
                assertSame(parameter, field.correspondingValueParameter)
                assertSame(primary.symbol, parameter.containingDeclarationSymbol)
                assertEquals(isMutable, field.isVar)
                assertEquals(Visibilities.Public, field.status.visibility)
                assertEquals(CjFakeSourceElementKind.FieldFromParameter, field.source?.kind)
                assertMemberParameterFacts(parameter, isMutable, Visibilities.Public)
            }
            assertTrue(parameters.getValue("mutable").isNamed)
            assertFalse(parameters.getValue("immutable").isNamed)
            assertOrdinaryParameter(parameters.getValue("ordinary"))
            val ordinaryField = fields.getValue("declared")
            assertFalse(ordinaryField.isMemberParameter)
            assertNull(ordinaryField.correspondingValueParameter)

            // 显式 prop 仍是真实属性，有自己的 getter；成员字段迁移不能删除这条合法语言能力。
            val property = owner.declarations.filterIsInstance<CfirProperty>().single()
            assertEquals("explicitProperty", property.name.asString())
            assertNotNull(property.getter)
            assertNull(property.setter)
        }

        val plainOwner = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "PlainOwner" }
        assertTrue(plainOwner.declarations.filterIsInstance<CfirFieldVariable>().isEmpty())
        plainOwner.declarations.filterIsInstance<CfirConstructor>().single { !it.isPrimary }
            .valueParameters.forEach(::assertOrdinaryParameter)
        file.declarations.filterIsInstance<CfirNamedFunction>().single().valueParameters.forEach(::assertOrdinaryParameter)

        val enumOwner = file.declarations.filterIsInstance<CfirEnum>().single()
        assertTrue(enumOwner.declarations.filterIsInstance<CfirFieldVariable>().isEmpty(), "enum payload does not generate instance fields")
        assertTrue(enumOwner.declarations.filterIsInstance<CfirProperty>().isEmpty(), "enum payload does not generate properties")
        val variants = enumOwner.declarations.filterIsInstance<CfirEnumConstructor>()
        assertEquals(setOf("Empty", "Value"), variants.map { it.name.asString() }.toSet())
        assertEquals(1, variants.single { it.name.asString() == "Value" }.valueParameters.size)
        variants.flatMap { it.valueParameters }.forEach(::assertOrdinaryParameter)
    }

    private fun assertOrdinaryParameter(parameter: CfirValueParameter) {
        assertFalse(parameter.isMemberParameter)
        assertNull(parameter.correspondingField)
        assertNull(parameter.memberParameterFieldInfo)
    }

    /** 显式调用共享字段算法验收已构建的 primary carrier，不把此 raw fixture 当作真实宏执行证明。 */
    private fun assertWrappedPrimaryFacts(mode: BodyBuildingMode, useLightTree: Boolean) {
        val raw = buildRawFixture(WRAPPED_SOURCE, mode, useLightTree)
        materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        val file = raw.cfirFile
        val owners = file.declarations.filterIsInstance<CfirClassLikeDeclaration>()
        assertEquals(setOf("WrappedClass", "WrappedStruct"), owners.map { it.name.asString() }.toSet())
        for (owner in owners) {
            val primary = owner.declarations.filterIsInstance<CfirConstructor>().single { it.isPrimary }
            val parameters = primary.valueParameters.associateBy { it.name.asString() }
            assertEquals(setOf("immutable", "ordinary", "mutable"), parameters.keys)
            assertMemberParameterFacts(parameters.getValue("immutable"), false, Visibilities.Public)
            assertMemberParameterFacts(parameters.getValue("mutable"), true, Visibilities.Private)
            assertOrdinaryParameter(parameters.getValue("ordinary"))
            parameters.values.forEach { assertSame(primary.symbol, it.containingDeclarationSymbol) }
            assertFalse(parameters.getValue("immutable").isNamed)
            assertTrue(parameters.getValue("mutable").isNamed)
            val fields = owner.declarations.filterIsInstance<CfirFieldVariable>().associateBy { it.name.asString() }
            assertEquals(setOf("immutable", "mutable"), fields.keys)
            for ((name, isVar) in listOf("immutable" to false, "mutable" to true)) {
                val parameter = parameters.getValue(name)
                val field = fields.getValue(name)
                assertSame(field, parameter.correspondingField)
                assertSame(parameter, field.correspondingValueParameter)
                assertEquals(isVar, field.isVar)
                assertEquals(if (isVar) Visibilities.Private else Visibilities.Public, field.status.visibility)
                assertEquals(owner.symbol.classId, field.symbol.callableId.classId)
                assertNull(field.initializer)
                val sourceAnnotation = parameter.annotations.single() as CfirAnnotationCall
                val derivedAnnotation = field.annotations.single() as CfirAnnotationCall
                assertNotSame(sourceAnnotation, derivedAnnotation)
                assertSame(field.symbol, derivedAnnotation.containingDeclarationSymbol)
                val sourceSlot = kotlin.test.assertNotNull(raw.annotationMetadata.snapshot(sourceAnnotation))
                val derivedSlot = kotlin.test.assertNotNull(raw.annotationMetadata.snapshot(derivedAnnotation))
                assertEquals(sourceSlot.copy(owner = field, originalAnnotation = derivedAnnotation), derivedSlot)
                assertTrue(raw.surfaces.none { it.replaceHandle.annotationCarrier?.owner === field })
            }
        }
    }

    /** 私有 raw 与字段完成显式分开；保留同次 parser/raw 构建实际使用的 metadata writer。 */
    private fun buildRawFixture(text: String, mode: BodyBuildingMode, useLightTree: Boolean): RawSnapshot =
        if (useLightTree) {
            val raw = buildLightConstructionFixture(text, mode)
            RawSnapshot(raw.cfirFile, raw.annotationMetadata, raw.surfaces)
        } else {
            val session = createTestSession()
            val file = createCjFile("memberFieldCompletion", text)
            file.cjmpParsingContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
            val builder = PsiRawCfirBuilder(session, mode, CfirAnnotationMetadataBuilder())
            RawSnapshot(builder.buildCfirFile(file), builder.annotationMetadata, builder.consumeCollectedMacroSurfaces())
        }

    /** registry seal 专测取得真实完整 parser 产品；普通 raw/字段检查不要求这份产品。 */
    private fun buildConstructionFixture(text: String, mode: BodyBuildingMode, useLightTree: Boolean): PreMacroCfirFile {
        if (useLightTree) return buildLightConstructionFixture(text, mode)
        val session = createTestSession()
        val origin = createCjFile("memberFieldCompletion", text)
        val settings = CjParserSettings(session.languageVersionSettings.languageVersion)
        val product = CangJieParser.parseCompilationFile(
            origin, session.cjmpSettings.toParsingContext(session.languageVersionSettings), settings,
        )
        val builder = PsiRawCfirBuilder(session, mode, CfirAnnotationMetadataBuilder())
        val file = product.withTargetFile { builder.buildCfirFile(it) }
        val publication = when (mode) {
            BodyBuildingMode.NORMAL -> checkNotNull(file.parsingDiagnosticPublication)
            BodyBuildingMode.LAZY_BODIES -> product.diagnosticPublication
        }
        product.requireConsistentInput()
        return PreMacroCfirFile(file, builder.consumeCollectedMacroSurfaces(), builder.annotationMetadata, publication, product.actualInputSeed.settings)
    }

    private fun buildLightConstructionFixture(text: String, mode: BodyBuildingMode): PreMacroCfirFile {
        val session = createTestSession()
        val sourceFile = CjInMemoryTextSourceFile("test.cj", null, text)
        val settings = CjParserSettings(session.languageVersionSettings.languageVersion)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val parsed = CangJieLightParser.parseBuffered(
            builder, sourceKind = sourceFile.sourceKind,
            cjmpContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings),
            sourceFile = sourceFile, settings = settings,
        )
        return LightTree2Cfir(session, session.cangjieScopeProvider, mode, settings)
            .buildPreMacroCfirFile(parsed, sourceFile, text.toSourceLinesMapping())
    }

    /** 与正式 Identity service 一样登记真实文件和 surfaces，不以未索引的空 registry 通过 seal。 */
    private fun registryFor(raw: PreMacroCfirFile): MacroExpansionRegistry =
        MacroExpansionRegistry().also { it.registerFileSurfaces(raw.cfirFile, raw.surfaces) }

    /** 私有 raw 检查只持有真实图、同次 metadata 和 surfaces，不冒充完成的 parser 产品。 */
    private data class RawSnapshot(
        val cfirFile: CfirFile,
        val annotationMetadata: CfirAnnotationMetadataBuilder,
        val surfaces: List<MacroSurface>,
    )


    /** 首个实际 primary、字段追加顺序和幂等都依赖最终声明身份，不能靠名字合并或读取 lazy body。 */
    private fun assertSharedFieldCompletionBoundaries(mode: BodyBuildingMode, useLightTree: Boolean) {
        val source = """
            package member_field_completion
            class Boundary {
                public Boundary(public let kept: Int64, static let excluded: Int64, ordinary: Int64, public var mutable!: Int64 = 3) {}
                public Boundary(public let duplicate: Int64) {}
                public let declared: Int64 = 1
            }
            class StaticConstructor {
                public static StaticConstructor(public let allowed: Int64) {}
            }
            class GenericOwner<T> {
                public GenericOwner(public let value: T) {}
            }
            class NamedFunctionOwner {
                @APILevel[1]
                func NamedFunctionOwner(value: Int64): Unit {}
            }
        """.trimIndent()
        val raw = buildConstructionFixture(source, mode, useLightTree)
        val pre = buildPreMacroRawFiles(raw.cfirFile.moduleData.session, listOf(raw))
        assertFailsWith<IllegalStateException> {
            MacroConstructionService.successOf(pre, listOf(raw.cfirFile), registryFor(raw))
        }
        val owners = raw.cfirFile.declarations.filterIsInstance<CfirClass>().associateBy { it.name.asString() }
        val boundary = owners.getValue("Boundary")
        val originalDeclarations = boundary.declarations.toList()
        val constructors = boundary.declarations.filterIsInstance<CfirConstructor>().filter { it.isPrimary }
        assertEquals(2, constructors.size)
        val parameters = constructors.first().valueParameters.associateBy { it.name.asString() }
        assertEquals(listOf("declared"), boundary.declarations.filterIsInstance<CfirFieldVariable>().map { it.name.asString() })
        val bodies = owners.values.flatMap { it.declarations.filterIsInstance<CfirConstructor>() }.associateWith { it.body }

        materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        val fields = boundary.declarations.filterIsInstance<CfirFieldVariable>().associateBy { it.name.asString() }
        assertEquals(setOf("declared", "kept", "mutable"), fields.keys)
        val generated = listOf(fields.getValue("kept"), fields.getValue("mutable"))
        assertEquals(originalDeclarations + generated, boundary.declarations)
        assertNull(parameters.getValue("excluded").correspondingField)
        assertNull(parameters.getValue("ordinary").correspondingField)
        assertNull(constructors.last().valueParameters.single().correspondingField)
        assertTrue(kotlin.test.assertNotNull(parameters.getValue("excluded").memberParameterFieldInfo).createFieldStatus().isStatic)
        assertNotNull(parameters.getValue("mutable").defaultValue)
        for (field in generated) {
            val parameter = parameters.getValue(field.name.asString())
            assertSame(field, parameter.correspondingField)
            assertSame(parameter, field.correspondingValueParameter)
            assertNull(field.initializer)
            assertNotSame(parameter.returnTypeRef, field.returnTypeRef)
        }

        // 官方排除参数的 STATIC 属性，不因为 primary 自身 STATIC 就丢弃其所有字段。
        val staticOwner = owners.getValue("StaticConstructor")
        val staticConstructors = staticOwner.declarations.filterIsInstance<CfirConstructor>()
        assertEquals(2, staticConstructors.size)
        val staticPrimary = staticConstructors.single { it.source?.kind != CjFakeSourceElementKind.ImplicitConstructor }
        val implicitDefault = staticConstructors.single { it.source?.kind == CjFakeSourceElementKind.ImplicitConstructor }
        assertTrue(implicitDefault.valueParameters.isEmpty())
        assertFalse(implicitDefault.status.isStatic)
        assertTrue(staticPrimary.status.isStatic)
        assertEquals(listOf("allowed"), staticOwner.declarations.filterIsInstance<CfirFieldVariable>().map { it.name.asString() })
        assertSame(staticPrimary.valueParameters.single(), staticOwner.declarations.filterIsInstance<CfirFieldVariable>().single().correspondingValueParameter)

        val generic = owners.getValue("GenericOwner")
        val genericParameter = generic.declarations.filterIsInstance<CfirConstructor>().single().valueParameters.single()
        val genericField = generic.declarations.filterIsInstance<CfirFieldVariable>().single()
        assertEquals(listOf("T"), assertIs<CfirUserTypeRef>(genericField.returnTypeRef).qualifier.map { it.name.asString() })
        assertNotSame(genericParameter.returnTypeRef, genericField.returnTypeRef)
        assertEquals(generic.symbol.classId, genericField.symbol.callableId.classId)
        val receiver = assertIs<ConeClassLikeType>(genericField.dispatchReceiverType)
        assertEquals(generic.symbol.classId, receiver.classId)
        val receiverArgument = assertIs<ConeTypeParameterType>(receiver.typeArguments.single().type)
        assertSame(generic.typeParameters.single().symbol, receiverArgument.lookupTag.typeParameterSymbol)
        assertSame(genericField, genericParameter.correspondingField)

        // 函数关键字保留真实声明种类；同名与 annotation wrapper 都不能把普通函数提升为 primary。
        val namedFunctionOwner = owners.getValue("NamedFunctionOwner")
        val ordinaryFunction = namedFunctionOwner.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals("NamedFunctionOwner", ordinaryFunction.name.asString())
        val ordinaryParameter = ordinaryFunction.valueParameters.single()
        assertEquals("value", ordinaryParameter.name.asString())
        assertOrdinaryParameter(ordinaryParameter)
        assertTrue(namedFunctionOwner.declarations.filterIsInstance<CfirFieldVariable>().isEmpty())
        for (constructor in namedFunctionOwner.declarations.filterIsInstance<CfirConstructor>()) {
            assertEquals(CjFakeSourceElementKind.ImplicitConstructor, constructor.source?.kind)
            assertTrue(constructor.valueParameters.isEmpty())
        }

        val completedDeclarations = owners.values.associateWith { it.declarations.toList() }
        val completedMetadata = raw.annotationMetadata.snapshots.toList()
        materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        owners.values.forEach { owner ->
            val previous = completedDeclarations.getValue(owner)
            assertEquals(previous.size, owner.declarations.size)
            previous.zip(owner.declarations).forEach { (before, after) -> assertSame(before, after) }
        }
        assertEquals(completedMetadata, raw.annotationMetadata.snapshots)
        bodies.forEach { (constructor, body) -> assertSame(body, constructor.body) }
        val firstPrimary = constructors.first()
        val originalParameters = firstPrimary.valueParameters.toList()
        firstPrimary.replaceValueParameters(originalParameters.reversed())
        assertFailsWith<IllegalStateException> {
            materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        }
        firstPrimary.replaceValueParameters(originalParameters)
        assertEquals(completedDeclarations.getValue(boundary), boundary.declarations)
        firstPrimary.replaceValueParameters(emptyList())
        assertFailsWith<IllegalStateException> { materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata) }
        assertFailsWith<IllegalStateException> { MacroConstructionService.successOf(pre, listOf(raw.cfirFile), registryFor(raw)) }
        firstPrimary.replaceValueParameters(originalParameters)
        val mutableDeclarations = boundary.declarations as MutableList<CfirDeclaration>
        mutableDeclarations.removeAll(constructors.toSet())
        assertFailsWith<IllegalStateException> { materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata) }
        assertFailsWith<IllegalStateException> { MacroConstructionService.successOf(pre, listOf(raw.cfirFile), registryFor(raw)) }
        mutableDeclarations.clear()
        mutableDeclarations.addAll(completedDeclarations.getValue(boundary))
        MacroConstructionService.successOf(pre, listOf(raw.cfirFile), registryFor(raw))
        assertFailsWith<IllegalStateException> {
            materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        }
    }

    /** 每次创建字段状态都是独立副本，修改返回值不改变冻结的参数事实。 */
    private fun assertMemberParameterFacts(
        parameter: CfirValueParameter,
        isVar: Boolean,
        visibility: Visibility,
        isConst: Boolean = false,
    ) {
        val info = kotlin.test.assertNotNull(parameter.memberParameterFieldInfo)
        assertTrue(parameter.isMemberParameter)
        assertEquals(isVar, info.isVar)
        val first = info.createFieldStatus()
        val second = info.createFieldStatus()
        assertNotSame(first, second)
        assertNotSame(parameter.status, first)
        assertEquals(visibility, first.visibility)
        assertEquals(visibility, second.visibility)
        assertTrue(first.isVisibilityExplicit)
        assertEquals(isConst, first.isConst)
        assertFalse(first.isStatic)
        first.isStatic = true
        first.isVisibilityExplicit = false
        assertFalse(second.isStatic)
        assertTrue(second.isVisibilityExplicit)
        val fresh = info.createFieldStatus()
        assertFalse(fresh.isStatic)
        assertTrue(fresh.isVisibilityExplicit)
        assertEquals(visibility, fresh.visibility)
        assertEquals(isConst, fresh.isConst)
    }

    /** 非主构造的非法参数拼写不能制造成员事实；const 修饰与 let/var 声明关键字分别记录。 */
    private fun assertMemberFactOwnershipAndRecovery(mode: BodyBuildingMode, useLightTree: Boolean) {
        fun build(source: String): CfirFile = if (useLightTree) {
            buildCfirFileFromLightTree(source, bodyBuildingMode = mode)
        } else {
            PsiRawCfirBuilder(createTestSession(), mode, CfirAnnotationMetadataBuilder()).buildCfirFile(createCjFile("memberFactRecovery", source))
        }

        for (keyword in listOf("let", "var", "const")) {
            val functionFile = build("func ordinary($keyword value: Int64) {}")
            val function = functionFile.declarations.filterIsInstance<CfirNamedFunction>().single()
            assertTrue(function.valueParameters.isNotEmpty(), "Malformed ordinary parameter must remain represented: $keyword")
            function.valueParameters.forEach { assertNull(it.memberParameterFieldInfo) }

            val constructorFile = build("class Recovery { init($keyword value: Int64) {} }")
            val owner = constructorFile.declarations.filterIsInstance<CfirClass>().single()
            val secondary = owner.declarations.filterIsInstance<CfirConstructor>().single { !it.isPrimary }
            assertTrue(secondary.valueParameters.isNotEmpty(), "Malformed secondary parameter must remain represented: $keyword")
            secondary.valueParameters.forEach { assertNull(it.memberParameterFieldInfo) }
        }

        for (annotation in listOf("", "@Carrier")) {
            val file = build("""
                class ConstParameterOwner {
                    $annotation
                    public ConstParameterOwner(const constant: Int64, public const let immutable: Int64, private const var mutable: Int64) {}
                }
            """.trimIndent())
            val owner = file.declarations.filterIsInstance<CfirClass>().single()
            val primary = owner.declarations.filterIsInstance<CfirConstructor>().single { it.isPrimary }
            val parameters = primary.valueParameters.associateBy { it.name.asString() }
            assertEquals(setOf("constant", "immutable", "mutable"), parameters.keys)
            // const-only 的旧字段生成行为不在本步改变；这里只检查它没有 let/var 来源事实。
            assertNull(parameters.getValue("constant").memberParameterFieldInfo)
            assertMemberParameterFacts(parameters.getValue("immutable"), false, Visibilities.Public, isConst = true)
            assertMemberParameterFacts(parameters.getValue("mutable"), true, Visibilities.Private, isConst = true)
        }
    }

    companion object {
        // 成员参数语法来自官方 LLT class/constructor/primaryConstructor/primaryConstructor1.cj
        // 与 record/primaryConstructor/primaryConstructor1.cj；enum 载荷是独立的构造项参数。
        private val SOURCE = """
            package member_parameter_fields

            class ClassOwner {
                public ClassOwner(public let immutable: Int64, ordinary: Bool, public var mutable!: Int64 = 1) {}
                public var declared: Int64 = 3
                public prop explicitProperty: Int64 { get() { 1 } }
            }

            struct StructOwner {
                public StructOwner(public let immutable: Int64, ordinary: Bool, public var mutable!: Int64 = 1) {}
                public var declared: Int64 = 3
                public prop explicitProperty: Int64 { get() { 1 } }
            }

            class PlainOwner {
                public init(value: Int64) {}
            }

            func ordinaryFunction(value: Int64): Int64 { value }

            enum Payload {
                | Empty | Value(Int64)
            }
        """.trimIndent()

        private val WRAPPED_SOURCE = """
            package member_parameter_facts

            class WrappedClass {
                @Carrier
                public WrappedClass(@Carrier public let immutable: Int64, ordinary: Bool, @Carrier private var mutable!: Int64 = 1) {}
            }

            struct WrappedStruct {
                @Carrier
                public WrappedStruct(@Carrier public let immutable: Int64, ordinary: Bool, @Carrier private var mutable!: Int64 = 1) {}
            }
        """.trimIndent()
    }
}
