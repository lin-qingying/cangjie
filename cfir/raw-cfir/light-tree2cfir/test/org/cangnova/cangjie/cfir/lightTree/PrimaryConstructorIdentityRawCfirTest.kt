package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializePrimaryConstructorParameterFields
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals as equal
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 错名直接成员是错误声明；宏 input 的 primary 身份来自 parser，不能按名字或后代再次猜测。 */
@RunWith(JUnit3RunnerWithInners::class)
class PrimaryConstructorIdentityRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testSpecificClassAndStructDeferImplicitConstructorInBothRawBuilders() {
        for (lightTree in listOf(false, true)) {
            for (kind in listOf("class", "struct")) {
                val session = createTestSession().apply {
                    register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.SPECIFIC))
                }
                val specific = build("specific $kind Owner {}", lightTree, BodyBuildingMode.NORMAL, session)
                    .cfirFile.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
                assertTrue(
                    specific.declarations.filterIsInstance<CfirConstructor>().isEmpty(),
                    "a specific $kind must defer implicit construction until after CJMP member merging (lightTree=$lightTree)",
                )

                val ordinary = build("$kind Owner {}", lightTree, BodyBuildingMode.NORMAL, session)
                    .cfirFile.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
                val ordinaryConstructor = ordinary.declarations.filterIsInstance<CfirConstructor>().single()
                assertTrue(
                    ordinaryConstructor.source?.kind == CjFakeSourceElementKind.ImplicitConstructor,
                    "an ordinary $kind keeps its raw implicit constructor (lightTree=$lightTree)",
                )
            }
        }
    }

    fun testPsiNormalIdentity() = assertMatrix(false, BodyBuildingMode.NORMAL)
    fun testPsiLazyIdentity() = assertMatrix(false, BodyBuildingMode.LAZY_BODIES)
    fun testLightTreeNormalIdentity() = assertMatrix(true, BodyBuildingMode.NORMAL)
    fun testLightTreeLazyIdentity() = assertMatrix(true, BodyBuildingMode.LAZY_BODIES)

    private fun assertMatrix(lightTree: Boolean, mode: BodyBuildingMode) {
        for (kind in listOf("class", "struct")) {
            for (candidate in listOf("Owner", "OwneX", "Different")) {
                for (prefix in listOf("", "@M ", "@Outer @Inner ")) {
                    val member = prefix + candidate + "(public let stored: Int64) { let retained = 1 }"
                    val text = "$kind Owner {\n$member\nfunc afterMember(): Unit {}\n}\nfunc following(): Unit {}"
                    val raw = build(text, lightTree, mode)
                    val owner = assertIs<CfirClassLikeDeclaration>(raw.cfirFile.declarations.first())
                    equal("Owner", owner.name.asString(), text)
                    equal(listOf("afterMember"), owner.declarations.filterIsInstance<CfirNamedFunction>().map { it.name.asString() }, text)
                    equal(listOf("following"), raw.cfirFile.declarations.filterIsInstance<CfirNamedFunction>().map { it.name.asString() }, text)
                    val invalid = owner.declarations.filterIsInstance<CfirInvalidDeclaration>()
                    val constructors = owner.declarations.filterIsInstance<CfirConstructor>()
                    assertTrue(owner.declarations.filterIsInstance<CfirFieldVariable>().isEmpty(), "pre-macro raw 不提前物化字段：$text")
                    if (prefix.isEmpty()) materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
                    val fields = owner.declarations.filterIsInstance<CfirFieldVariable>()
                    val hasParsedPrimary = candidate == "Owner" || (prefix.isNotEmpty() && candidate == "OwneX")
                    if (hasParsedPrimary) {
                        assertTrue(invalid.isEmpty(), text)
                        val constructor = constructors.single()
                        assertTrue(constructor.isPrimary, text)
                        assertTrue(constructor.source?.kind != CjFakeSourceElementKind.ImplicitConstructor, text)
                        equal(listOf("stored"), constructor.valueParameters.map { it.name.asString() }, text)
                        assertSame(constructor.symbol, constructor.valueParameters.single().containingDeclarationSymbol, text)
                        if (mode == BodyBuildingMode.LAZY_BODIES) {
                            assertIs<CfirLazyBlock>(constructor.body, text)
                        } else {
                            assertTrue(assertNotNull(constructor.body, text) !is CfirLazyBlock, text)
                        }
                        if (prefix.isEmpty()) equal(listOf("stored"), fields.map { it.name.asString() }, text)
                        else {
                            assertTrue(fields.isEmpty(), "宏输入构造器不能提前生成宿主字段：$text")
                            assertTrue(raw.surfaces.any { it.replaceHandle.carrier === constructor }, text)
                        }
                    } else {
                        assertTrue(fields.isEmpty(), text)
                        assertTrue(constructors.all {
                            it.source?.kind == CjFakeSourceElementKind.ImplicitConstructor && it.valueParameters.isEmpty()
                        }, text)
                        if (prefix.isEmpty() && candidate == "OwneX") {
                            val declaration = invalid.single()
                            val source = kotlin.test.assertNotNull(declaration.source, text)
                            equal(member, text.substring(source.startOffset, source.endOffset), text)
                        }
                    }
                }
            }
        }
        for (kind in listOf("class", "struct")) {
            val text = "$kind Owner { func Owner(value: Int64) {}\nfunc afterMember(): Unit {} }\nfunc following(): Unit {}"
            val raw = build(text, lightTree, mode)
            val owner = assertIs<CfirClassLikeDeclaration>(raw.cfirFile.declarations.first())
            equal(listOf("Owner", "afterMember"), owner.declarations.filterIsInstance<CfirNamedFunction>().map { it.name.asString() }, text)
            assertTrue(owner.declarations.filterIsInstance<CfirConstructor>().all {
                it.source?.kind == CjFakeSourceElementKind.ImplicitConstructor && it.valueParameters.isEmpty()
            }, text)
            assertTrue(owner.declarations.filterIsInstance<CfirFieldVariable>().isEmpty(), text)
        }
        for (kind in listOf("class", "struct")) {
            val macro = "macro preserved(input: Tokens): Tokens { input }"
            val text = "$kind Owner {\nDifferent(value: Int64) {}\n$macro\nfunc afterMember(): Unit {}\n}\nfunc following(): Unit {}"
            val raw = build(text, lightTree, mode)
            val owner = assertIs<CfirClassLikeDeclaration>(raw.cfirFile.declarations.first())
            val declaration = owner.declarations.filterIsInstance<CfirMacroDeclaration>().single()
            equal("preserved", declaration.name.asString(), text)
            equal(listOf("input"), declaration.valueParameters.map { it.name.asString() }, text)
            val parameter = declaration.valueParameters.single()
            assertSame(declaration.symbol, parameter.containingDeclarationSymbol, text)
            for (typeRef in listOf(parameter.returnTypeRef, declaration.returnTypeRef)) {
                val userType = assertIs<CfirUserTypeRef>(typeRef, text)
                equal(listOf("Tokens"), userType.qualifier.map { it.name.asString() }, text)
                val typeSource = assertNotNull(typeRef.source, text)
                equal("Tokens", text.substring(typeSource.startOffset, typeSource.endOffset), text)
            }
            if (mode == BodyBuildingMode.LAZY_BODIES) {
                assertIs<CfirLazyBlock>(declaration.body, text)
            } else {
                val body = assertNotNull(declaration.body, text)
                val access = assertIs<CfirQualifiedAccessExpression>(body.statements.single(), text)
                equal("input", assertIs<CfirNamedReference>(access.calleeReference, text).name.asString(), text)
                val accessSource = assertNotNull(access.source, text)
                equal("input", text.substring(accessSource.startOffset, accessSource.endOffset), text)
            }
            val source = kotlin.test.assertNotNull(declaration.source, text)
            equal(macro, text.substring(source.startOffset, source.endOffset), text)
            equal(listOf("afterMember"), owner.declarations.filterIsInstance<CfirNamedFunction>().map { it.name.asString() }, text)
            equal(listOf("following"), raw.cfirFile.declarations.filterIsInstance<CfirNamedFunction>().map { it.name.asString() }, text)
            assertTrue(owner.declarations.filterIsInstance<CfirFieldVariable>().isEmpty(), text)
        }
    }

    private fun build(
        text: String,
        lightTree: Boolean,
        mode: BodyBuildingMode,
        session: CfirSession = createTestSession(),
    ): RawSnapshot {
        val context = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
        if (!lightTree) {
            val file = createCjFile("primaryIdentity", text)
            file.cjmpParsingContext = context
            val builder = PsiRawCfirBuilder(session, mode, CfirAnnotationMetadataBuilder())
            return RawSnapshot(builder.buildCfirFile(file), builder.annotationMetadata, builder.consumeCollectedMacroSurfaces())
        }
        val sourceFile = CjInMemoryTextSourceFile("primaryIdentity.cj", null, text)
        val settings = CjParserSettings(session.languageVersionSettings.languageVersion)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val parsed = CangJieLightParser.parseBuffered(
            builder, sourceKind = sourceFile.sourceKind, cjmpContext = context,
            sourceFile = sourceFile, settings = settings,
        )
        val raw = LightTree2Cfir(session, session.cangjieScopeProvider, mode, settings)
            .buildPreMacroCfirFile(parsed, sourceFile, text.toSourceLinesMapping())
        return RawSnapshot(raw.cfirFile, raw.annotationMetadata, raw.surfaces)
    }

    /** 私有 raw 检查只持有真实图、同次 metadata 和 surfaces，不冒充完成的 parser 产品。 */
    private data class RawSnapshot(
        val cfirFile: CfirFile,
        val annotationMetadata: CfirAnnotationMetadataBuilder,
        val surfaces: List<MacroSurface>,
    )
}
