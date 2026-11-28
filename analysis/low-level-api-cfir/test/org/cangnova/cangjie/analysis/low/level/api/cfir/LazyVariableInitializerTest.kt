package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.CfirElementsRecorder
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.collectAndPublishInitializationDependencies
import org.cangnova.cangjie.cfir.declarations.initializationDependencies
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 从真实源码验证字段和顶层变量的惰性初值能够在声明解析时重建。 */
class LazyVariableInitializerTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/lazyVariableInitializer",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun explicitAndImplicitTypes(mainFile: CjFile) {
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val owner = file.declarations.filterIsInstance<CfirClass>().single()
        val fields = owner.declarations.filterIsInstance<CfirFieldVariable>().associateBy { it.name.asString() }
        val topLevel = file.declarations.filterIsInstance<CfirPatternVariable>()
            .associateBy { it.pattern.bindingVariables().single().name.asString() }
        assertEquals(setOf("explicitField", "implicitField"), fields.keys)
        assertEquals(setOf("explicitTop", "implicitTop"), topLevel.keys)

        val variables: List<CfirVariable> = fields.values.toList() + topLevel.values
        variables.forEach { assertTrue(it.initializer is CfirLazyExpression) }
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertTrue(function.body is CfirLazyBlock)
        assertTrue(CfirElementsRecorder.recordElementsFrom(file, CfirElementsRecorder()).isNotEmpty())
        function.symbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        assertNotNull(function.body)
        assertFalse(function.body is CfirLazyBlock)
        variables.forEach { assertTrue(it.initializer is CfirLazyExpression) }
        fields.values.forEach {
            assertSame(CjmpInitializationDependencies.NotCollected, it.collectAndPublishInitializationDependencies())
            assertSame(CjmpInitializationDependencies.NotCollected, it.initializationDependencies)
        }

        variables.forEach { variable ->
            variable.symbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            assertNotNull(variable.initializer)
            assertFalse(variable.initializer is CfirLazyExpression)
        }
        fields.values.forEach {
            assertTrue(it.collectAndPublishInitializationDependencies() is CjmpInitializationDependencies.Collected<*>)
        }
    }
}
