package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.typeInfoProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `typeInfoProvider.superTypes` 的抽象测试。
 *
 * 该测试通过 class symbol 暴露的 `superTypes` 验证公开继承类型列表。
 */
abstract class AbstractSuperTypesTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行 super types 快照测试。
     *
     * 方法定位目标 class，恢复 class symbol，并按 qualified renderer 输出所有直接超类型。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val classDeclaration = testServices.expressionMarkerProvider
            .getTopmostSelectedElementOfTypeByDirectiveOrNull(mainFile, mainModule, CjClass::class)
            as? CjClass
            ?: PsiTreeUtil.findChildrenOfType(mainFile, CjClass::class.java).single { it.name != "Base" }

        val actual = analyzeForTest(classDeclaration) {
            val symbol = getClassSymbol(classDeclaration.getClassId()!!) as CaClassSymbol
            buildString {
                appendLine("class: ${classDeclaration.name}")
                appendLine("superTypes:")
                symbol.superTypes.forEach { type ->
                    appendLine(type.render(CaTypeRendererForSource.WITH_QUALIFIED_NAMES).let(::normalizeTypeRendering))
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
