package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.renderer

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedRenderedCallableSymbol
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedRenderedClassSymbol
import org.cangnova.cangjie.analysis.api.impl.base.test.expectedRenderedType
import org.cangnova.cangjie.analysis.api.impl.base.test.targetCallText
import org.cangnova.cangjie.analysis.api.impl.base.test.targetClassName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetFunctionName
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * renderer 抽象测试。
 *
 * 这里锁定公开渲染协议的三个最小语义单元：
 * 1. class-like 符号渲染；
 * 2. callable 符号渲染；
 * 3. 公开类型渲染。
 */
abstract class AbstractRendererTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行 renderer 基础契约测试。
     *
     * 方法恢复 class-like symbol、callable symbol 和表达式类型，分别比较显式 source preset 与默认 render 入口。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val targetClass = PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java)
            .single { it.name == directives.targetClassName }
        val targetCall = PsiTreeUtil.findChildrenOfType(mainFile, CjCallExpression::class.java)
            .single { it.text == directives.targetCallText }

        analyzeForTest(targetCall) {
            val classSymbol = getClassLikeSymbol(targetClass.getClassId()!!)
            val callableSymbol = getTopLevelCallableSymbols(
                mainFile.packageFqName,
                Name.identifier(directives.targetFunctionName),
            ).singleOrNull()
            val expressionType = targetCall.expressionType

            assertNotNull(classSymbol, "renderer 测试需要可恢复的 class-like 符号。")
            assertNotNull(callableSymbol, "renderer 测试需要可恢复的 callable 符号。")
            assertNotNull(expressionType, "renderer 测试需要可恢复的表达式类型。")

            val resolvedClassSymbol = classSymbol!!
            val resolvedCallableSymbol = callableSymbol!!
            val resolvedExpressionType = expressionType!!

            val explicitClassRendering = resolvedClassSymbol.render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES)
            val explicitCallableRendering = resolvedCallableSymbol.render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES)
            val explicitTypeRendering = normalizeTypeRendering(
                resolvedExpressionType.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES),
            )

            assertEquals(
                directives.expectedRenderedClassSymbol,
                explicitClassRendering,
            )
            assertEquals(
                directives.expectedRenderedCallableSymbol,
                explicitCallableRendering,
            )
            assertEquals(directives.expectedRenderedType, explicitTypeRendering)

            /**
             * `render()` / `CaType.render()` 依然保留为默认入口，
             * 但它们的语义必须严格委托到公开 source preset，
             * 不能再偷偷维护另一套默认字符串格式。
             */
            assertEquals(explicitClassRendering, resolvedClassSymbol.render())
            assertEquals(explicitCallableRendering, resolvedCallableSymbol.render())
            assertEquals(explicitTypeRendering, normalizeTypeRendering(resolvedExpressionType.render()))
        }
    }
}
