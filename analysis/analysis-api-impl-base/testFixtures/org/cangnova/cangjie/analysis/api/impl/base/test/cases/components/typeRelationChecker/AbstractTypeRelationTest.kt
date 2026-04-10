package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeRelationChecker

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiTypeRelationTestDirectives
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiTypeTestSupport
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedIsSubtype
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedSemanticallyEqual
import org.cangnova.cangjie.analysis.api.impl.base.test.leftContainerClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.leftSecondTargetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.leftTargetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.leftTypeKind
import org.cangnova.cangjie.analysis.api.impl.base.test.rightContainerClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.rightSecondTargetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.rightTargetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.rightTypeKind
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * public type relation generated 测试。
 *
 * 左右两侧类型都通过公开 `CaTypeCreator` 构造，再统一校验
 * `CaTypeRelationChecker` 的两个核心契约：
 * - `isSubTypeOf`
 * - `semanticallyEquals`
 */
abstract class AbstractTypeRelationTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiTypeRelationTestDirectives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)

        analyzeForTest(mainFile) {
            val leftPrimary = AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, directives.leftTargetClassName)
            val leftSecondary = directives.leftSecondTargetClassName?.let { name ->
                AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, name)
            }
            val leftContainer = directives.leftContainerClassName?.let { name ->
                AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, name)
            }
            val rightPrimary = AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, directives.rightTargetClassName)
            val rightSecondary = directives.rightSecondTargetClassName?.let { name ->
                AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, name)
            }
            val rightContainer = directives.rightContainerClassName?.let { name ->
                AnalysisApiTypeTestSupport.resolveClassSymbol(mainModule, name)
            }

            val leftType = AnalysisApiTypeTestSupport.buildType(
                kind = directives.leftTypeKind,
                primaryClass = leftPrimary,
                secondaryClass = leftSecondary,
                containerClass = leftContainer,
            )
            val rightType = AnalysisApiTypeTestSupport.buildType(
                kind = directives.rightTypeKind,
                primaryClass = rightPrimary,
                secondaryClass = rightSecondary,
                containerClass = rightContainer,
            )

            assertEquals(
                directives.expectedIsSubtype,
                leftType.isSubTypeOf(rightType),
                "left.isSubTypeOf(right) 结果不符合预期。",
            )
            assertEquals(
                directives.expectedSemanticallyEqual,
                leftType.semanticallyEquals(rightType),
                "left.semanticallyEquals(right) 结果不符合预期。",
            )
            assertEquals(
                directives.expectedSemanticallyEqual,
                rightType.semanticallyEquals(leftType),
                "语义等价关系应保持对称。",
            )
        }
    }
}
