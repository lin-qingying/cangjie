package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolvableSessionForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * STUBS 下 `Array.size` 的 extend 提供者必须唯一。
 *
 * 守卫“builtins roots 与项目库 roots 重叠时对同一批 `.cjo` 建两份 extend provider”的回归：
 * 同一目标上的同名成员出现两份就说明去重失效（`isLibraryCoveredByBuiltins` 未生效）。
 */
class ExtendProviderUniquenessTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun arraySizeExtendProviderUniqueness(mainFile: CjFile) {
        assertEquals(
            org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin.STUBS,
            org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings.getInstance(mainFile.project)
                .deserializedDeclarationsOrigin,
            "该守卫必须运行在 STUBS 配置下",
        )
        val session = mainFile.getResolvableSessionForTest()
        val arrayExtends = session.extendProviderOrNull
            ?.getExtendsForClass(StdlibClassIds.Array)
            .orEmpty()
        assertTrue(arrayExtends.isNotEmpty(), "Array 必须有 extend 声明（Collection/ToString 等）")

        val sizeProviders = arrayExtends
            .flatMap { extend -> extend.declarations }
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirProperty>()
            .filter { declaration -> declaration.name.asString() == "size" }
        assertEquals(
            1,
            sizeProviders.size,
            "Array 的 size 成员只能有一个提供者，实际：${sizeProviders.map { it.name?.asString() }}",
        )
    }
}
