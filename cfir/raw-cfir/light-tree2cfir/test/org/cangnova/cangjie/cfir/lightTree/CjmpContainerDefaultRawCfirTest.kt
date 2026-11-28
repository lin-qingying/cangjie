package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializePrimaryConstructorParameterFields
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 容器默认实现来自最终原始成员，主构造成员参数字段物化不得改变既有事实。 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpContainerDefaultRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiNormal() = checkContainers(BodyBuildingMode.NORMAL, false)
    fun testPsiLazy() = checkContainers(BodyBuildingMode.LAZY_BODIES, false)
    fun testLightTreeNormal() = checkContainers(BodyBuildingMode.NORMAL, true)
    fun testLightTreeLazy() = checkContainers(BodyBuildingMode.LAZY_BODIES, true)

    fun testHistoricalPrimaryConstructorDefaults() {
        for (version in listOf(LanguageVersion.CANGJIE_1_1_0_ALPHA_65, LanguageVersion.CANGJIE_1_1_0_BETA_1)) {
            for (mode in listOf(BodyBuildingMode.NORMAL, BodyBuildingMode.LAZY_BODIES)) {
                for (lightTree in listOf(false, true)) {
                    val session = createTestSession(LanguageVersionSettingsImpl(version)).apply {
                        register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
                    }
                    val context = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
                    val raw = if (lightTree) {
                        buildLightRawSnapshot(HISTORICAL_SOURCE, session, mode, "historicalPrimary.cj")
                    } else {
                        val file = createCjFile("historicalPrimary", HISTORICAL_SOURCE)
                        file.cjmpParsingContext = context
                        val builder = PsiRawCfirBuilder(session, mode, CfirAnnotationMetadataBuilder())
                        RawSnapshot(builder.buildCfirFile(file), builder.annotationMetadata, builder.consumeCollectedMacroSurfaces())
                    }
                    val owners = raw.cfirFile.declarations.filterIsInstance<CfirClassLikeDeclaration>()
                        .associateBy { it.name.asString() }
                    assertEquals(setOf("Owner", "Record"), owners.keys)
                    for ((name, owner) in owners) {
                        assertTrue(owner.status.isCommon, "$version $mode $lightTree $name")
                        assertTrue(owner.cjmpHasCommonDefault(), "$version $mode $lightTree $name")
                    }
                }
            }
        }
    }

    private fun checkContainers(mode: BodyBuildingMode, lightTree: Boolean) {
        val session = createTestSession().apply {
            register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        }
        val raw = if (lightTree) {
            buildLightRawSnapshot(SOURCE, session, mode, "test.cj")
        } else {
            val file = createCjFile("cjmpContainerDefaults", SOURCE)
            file.cjmpParsingContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
            val builder = PsiRawCfirBuilder(session, mode, CfirAnnotationMetadataBuilder())
            RawSnapshot(builder.buildCfirFile(file), builder.annotationMetadata, builder.consumeCollectedMacroSurfaces())
        }
        val owners = raw.cfirFile.declarations.filterIsInstance<CfirClassLikeDeclaration>()
            .associateBy { it.name.asString() }
        assertEquals(EXPECTED.keys + setOf("PrimaryClass", "PrimaryStruct"), owners.keys)
        for ((name, expected) in EXPECTED) {
            assertTrue(owners.getValue(name).status.isCommon, name)
            assertEquals(expected, owners.getValue(name).cjmpHasCommonDefault(), name)
        }
        for (kind in listOf("Class", "Struct")) {
            val defaultOwner = owners.getValue("${kind}Default")
            val requiredOwner = owners.getValue("${kind}Required")
            val members = requiredOwner.declarations as MutableList<CfirDeclaration>
            val required = members.filterIsInstance<CfirFieldVariable>().single()
            assertTrue(required.status.isCommon)
            // 模拟宏替换删除原始成员：容器保留解析时的默认事实。
            assertTrue(members.remove(required))
            assertEquals(false, requiredOwner.cjmpHasCommonDefault(), kind)
            // 插入其他成员也不能改变原容器的解析时事实。
            val defaultMembers = defaultOwner.declarations as MutableList<CfirDeclaration>
            defaultMembers.add(required)
            assertTrue(defaultOwner.cjmpHasCommonDefault(), kind)
            assertTrue(defaultMembers.remove(required))
            assertTrue(defaultOwner.cjmpHasCommonDefault(), kind)
            members.add(required)
            assertEquals(false, requiredOwner.cjmpHasCommonDefault(), kind)
        }
        val before = owners.mapValues { it.value.cjmpHasCommonDefault() }
        for (name in listOf("PrimaryClass", "PrimaryStruct")) {
            assertTrue(owners.getValue(name).declarations.filterIsInstance<CfirFieldVariable>().isEmpty(), name)
        }
        materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        for (name in listOf("PrimaryClass", "PrimaryStruct")) {
            assertEquals(listOf("value"), owners.getValue(name).declarations.filterIsInstance<CfirFieldVariable>()
                .map { it.name.asString() }, name)
        }
        assertEquals(before, owners.mapValues { it.value.cjmpHasCommonDefault() })
        materializePrimaryConstructorParameterFields(raw.cfirFile, raw.annotationMetadata)
        assertEquals(before, owners.mapValues { it.value.cjmpHasCommonDefault() })
    }


    private fun buildLightRawSnapshot(text: String, session: CfirSession, mode: BodyBuildingMode, fileName: String): RawSnapshot {
        val sourceFile = CjInMemoryTextSourceFile(fileName, null, text)
        val settings = CjParserSettings(session.languageVersionSettings.languageVersion)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val parsed = CangJieLightParser.parseBuffered(
            builder, sourceKind = sourceFile.sourceKind,
            cjmpContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings),
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

    companion object {
        private val HISTORICAL_SOURCE = """
            common class Owner { common Owner() {} }
            common struct Record { common Record() {} }
        """.trimIndent()

        private val EXPECTED = mapOf(
            "ClassDefault" to true,
            "ClassRequired" to false,
            "StructDefault" to true,
            "StructRequired" to false,
        )

        private val SOURCE = """
            package cjmp_container_defaults

            public common class ClassDefault {
                public common let value: Int64 = 1
            }
            public common class ClassRequired {
                public common let value: Int64
            }
            public common struct StructDefault {
                public common let value: Int64 = 1
            }
            public common struct StructRequired {
                public common let value: Int64
            }
            public common class PrimaryClass {
                public PrimaryClass(public let value: Int64) {}
            }
            public common struct PrimaryStruct {
                public PrimaryStruct(public let value: Int64) {}
            }
        """.trimIndent()
    }
}
