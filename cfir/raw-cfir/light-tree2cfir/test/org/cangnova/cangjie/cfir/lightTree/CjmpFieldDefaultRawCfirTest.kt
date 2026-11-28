package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.declarations.collectAndPublishInitializationDependencies
import org.cangnova.cangjie.cfir.declarations.initializationDependencies
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertIs
import kotlin.test.assertSame

/** common 字段的默认实现事实来自声明头，惰性构建不能预先构建初始化表达式。 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpFieldDefaultRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiNormal() = checkFields(buildPsi(BodyBuildingMode.NORMAL), BodyBuildingMode.NORMAL)

    fun testPsiLazyBodies() = checkFields(buildPsi(BodyBuildingMode.LAZY_BODIES), BodyBuildingMode.LAZY_BODIES)

    fun testLightTreeNormal() = checkFields(
        buildCfirFileFromLightTree(SOURCE, session = commonSession(), bodyBuildingMode = BodyBuildingMode.NORMAL), BodyBuildingMode.NORMAL,
    )

    fun testLightTreeLazyBodies() = checkFields(
        buildCfirFileFromLightTree(SOURCE, session = commonSession(), bodyBuildingMode = BodyBuildingMode.LAZY_BODIES), BodyBuildingMode.LAZY_BODIES,
    )

    fun testLazyFieldDoesNotPublishDependenciesBeforeInitializerIsBuilt() {
        val lazyField = memberField(buildPsi(BodyBuildingMode.LAZY_BODIES), "memberDefault")
        val builtField = memberField(buildPsi(BodyBuildingMode.NORMAL), "memberDefault")
        assertIs<CfirLazyExpression>(lazyField.initializer)
        assertSame(CjmpInitializationDependencies.NotCollected, lazyField.collectAndPublishInitializationDependencies())
        assertSame(CjmpInitializationDependencies.NotCollected, lazyField.initializationDependencies)

        // 使用同一 PSI 源码正常构建的真实初值，模拟惰性初值被重建后的声明状态。
        lazyField.replaceInitializer(requireNotNull(builtField.initializer))
        val dependencies = assertIs<CjmpInitializationDependencies.Collected<*>>(
            lazyField.collectAndPublishInitializationDependencies(),
        )
        assertTrue(dependencies.declarations.isEmpty())
        assertSame(dependencies, lazyField.initializationDependencies)
    }

    private fun memberField(file: CfirFile, name: String): CfirFieldVariable =
        file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single().declarations
            .filterIsInstance<CfirFieldVariable>().single { it.name.asString() == name }

    private fun commonSession(): CfirSession = createTestSession().apply {
        register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
    }

    private fun buildPsi(mode: BodyBuildingMode): CfirFile {
        val session = commonSession()
        val file = createCjFile("cjmpFieldDefault", SOURCE)
        file.cjmpParsingContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
        return PsiRawCfirBuilder(session, mode, CfirAnnotationMetadataBuilder()).buildCfirFile(file)
    }

    private fun checkFields(file: CfirFile, mode: BodyBuildingMode) {
        val owner = file.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val fields = owner.declarations.filterIsInstance<CfirFieldVariable>()
            .associateBy { it.name.asString() }
        assertEquals(MEMBER_DEFAULTS.keys, fields.keys)

        for ((name, hasDefault) in MEMBER_DEFAULTS) {
            val field = fields.getValue(name)
            assertTrue(field.status.isCommon, name)
            assertEquals(hasDefault, field.cjmpHasCommonDefault(), name)
            checkInitializer(field.initializer, hasDefault, mode, name)
        }

        val patterns = file.declarations.filterIsInstance<CfirPatternVariable>()
        val simple = patterns.filterNot { it.pattern is CfirTuplePattern }
            .associateBy { it.pattern.bindingVariables().single().name.asString() }
        assertEquals(PACKAGE_DEFAULTS.keys, simple.keys)
        for ((name, hasDefault) in PACKAGE_DEFAULTS) {
            val variable = simple.getValue(name)
            assertTrue(variable.status.isCommon, name)
            assertEquals(hasDefault, variable.cjmpHasCommonDefault(), name)
            checkInitializer(variable.initializer, hasDefault, mode, name)
        }
        val tuple = patterns.single { it.pattern is CfirTuplePattern }
        assertIs<CfirTuplePattern>(tuple.pattern)
        assertEquals(setOf("tupleA", "tupleB"), tuple.pattern.bindingVariables().map { it.name.asString() }.toSet())
        assertEquals(false, tuple.cjmpHasCommonDefault(), "tuple pattern is not an ordinary variable declaration")
        checkInitializer(tuple.initializer, true, mode, "tuple")
    }

    private fun checkInitializer(initializer: CfirExpression?, hasInitializer: Boolean, mode: BodyBuildingMode, name: String) {
        when {
            !hasInitializer -> assertNull(initializer, name)
            mode == BodyBuildingMode.LAZY_BODIES -> assertIs<CfirLazyExpression>(initializer, name)
            else -> assertTrue(requireNotNull(initializer) !is CfirLazyExpression, name)
        }
    }

    companion object {
        private val PACKAGE_DEFAULTS = mapOf(
            "packageDefault" to true,
            "packageRequired" to false,
        )
        private val MEMBER_DEFAULTS = mapOf(
            "memberDefault" to true,
            "memberRequired" to false,
        )

        private val SOURCE = """
            package cjmp_field_defaults

            public common let packageDefault: Int64 = 1
            public common let packageRequired: Int64
            public common let (tupleA, tupleB) = (1, 2)

            public common class Owner {
                public common let memberDefault: Int64 = 2
                public common let memberRequired: Int64
            }
        """.trimIndent()
    }
}
