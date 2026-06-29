package org.cangnova.cangjie.analysis.api.impl.base.test.cases.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

/**
 * 通过 caret 所在 callable 声明返回类型观察 `CaType` 的抽象测试。
 *
 * 该测试覆盖源码声明到公开 return type 的最直接路径。
 */
abstract class AbstractTypeByDeclarationReturnTypeTest : AbstractTypeTest() {
    /**
     * 从 caret 所在 callable 声明读取公开返回类型。
     *
     * 若声明没有可暴露的返回类型，测试会直接失败以暴露 testData 或 API 行为问题。
     */
    override fun getType(
        analysisSession: CaSession,
        cjFile: CjFile,
        module: CjTestModule,
        testServices: TestServices,
    ) = with(analysisSession) {
        val declaration = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjCallableDeclaration>(cjFile)
        declaration.returnType ?: error("Callable `${declaration.text}` does not expose a return type.")
    }
}
