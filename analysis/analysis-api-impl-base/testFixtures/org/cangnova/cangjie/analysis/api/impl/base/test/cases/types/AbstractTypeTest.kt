package org.cangnova.cangjie.analysis.api.impl.base.test.cases.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * shared `types` 测试族的公共基类。
 *
 * 它统一观察 public `CaType` 的稳定属性：
 * - presentation
 * - 缩写形态 abbreviation
 * - fullyExpandedType
 * - classLikeSymbol
 * - qualified / short 两种公开渲染
 */
abstract class AbstractTypeTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val actual = analyzeForTest(mainFile) {
            val type = getType(useSiteSession, mainFile, mainModule, testServices)
            buildString {
                appendLine("presentation: ${type.presentation}")
                appendLine("qualified: ${normalizeTypeRendering(type.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES))}")
                appendLine("short: ${normalizeTypeRendering(type.render(CaTypeRendererForSource.WITH_SHORT_NAMES))}")
                appendLine(
                    "abbreviation: ${
                        type.abbreviation
                            ?.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)
                            ?.let(::normalizeTypeRendering)
                    }",
                )
                appendLine(
                    "fullyExpanded: ${
                        normalizeTypeRendering(
                            type.fullyExpandedType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES),
                        )
                    }",
                )
                appendLine("classLikeSymbol: ${type.classLikeSymbol?.classId?.asString()}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    protected abstract fun getType(
        analysisSession: CaSession,
        cjFile: CjFile,
        module: CjTestModule,
        testServices: TestServices,
    ): CaType
}
