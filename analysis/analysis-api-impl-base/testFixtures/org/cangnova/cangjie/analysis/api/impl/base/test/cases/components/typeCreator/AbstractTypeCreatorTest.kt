package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeCreator

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiTypeCreatorTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiTypeTestSupport
import org.cangnova.cangjie.analysis.api.impl.base.test.containerClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedQualifiedTypeRender
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedShortTypeRender
import org.cangnova.cangjie.analysis.api.impl.base.test.secondTargetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetTypeParameterName
import org.cangnova.cangjie.analysis.api.impl.base.test.typeParameterOwnerClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.typeCreationKind
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * public type creator generated 测试。
 *
 * 这组测试固定覆盖仓颉公开 Analysis API 里真实存在的全部类型构造入口：
 * - class-like type
 * - generic class-like type
 * - tuple type
 * - intersection type
 * - union type
 * - function / c-function / closure function
 *
 * class-like 场景同时要求 `buildClassType(classId, ...)` 与
 * `buildClassType(symbol, ...)` 结果一致，避免两个公开入口语义漂移。
 */
abstract class AbstractTypeCreatorTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiTypeCreatorTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)

        analyzeForTest(mainFile) {
            val primaryClass = AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, directives.targetClassName)
            val secondaryClass = directives.secondTargetClassName?.let { name ->
                AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, name)
            }
            val containerClass = directives.containerClassName?.let { name ->
                AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, name)
            }
            val typeParameterOwnerClass = directives.typeParameterOwnerClassName?.let { name ->
                AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, name)
            }

            val createdType = AnalysisApiTypeTestSupport.buildType(
                kind = directives.typeCreationKind,
                primaryClass = primaryClass,
                secondaryClass = secondaryClass,
                containerClass = containerClass,
                typeParameterOwnerClass = typeParameterOwnerClass,
                targetTypeParameterName = directives.targetTypeParameterName,
            )

            assertEquals(
                directives.expectedQualifiedTypeRender,
                normalizeTypeRendering(createdType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES)),
                "qualified renderer 输出不符合预期。",
            )
            assertEquals(
                directives.expectedShortTypeRender,
                normalizeTypeRendering(createdType.render(CaTypeRendererForSource.WITH_SHORT_NAMES)),
                "short renderer 输出不符合预期。",
            )
        }
    }
}
