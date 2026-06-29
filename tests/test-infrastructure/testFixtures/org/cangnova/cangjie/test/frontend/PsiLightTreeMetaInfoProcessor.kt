package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.singleValue
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.utils.AbstractTwoAttributesMetaInfoProcessor

/**
 * 表示 `PsiLightTreeMetaInfoProcessor`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class PsiLightTreeMetaInfoProcessor(testServices: TestServices) : AbstractTwoAttributesMetaInfoProcessor(testServices) {
    companion object {
        const val PSI = "PSI"
        const val LT = "LT" // Light Tree
    }

    /**
     * 保存 `firstAttribute`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val firstAttribute: String get() = PSI
    /**
     * 保存 `secondAttribute`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val secondAttribute: String get() = LT

    /**
     * 执行 `processorEnabled` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun processorEnabled(module: TestModule): Boolean {
        return CfirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in module.directives
    }

    /**
     * 执行 `firstAttributeEnabled` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun firstAttributeEnabled(module: TestModule): Boolean {
        return module.directives.singleValue(CfirDiagnosticsDirectives.CFIR_PARSER) == CfirParser.Psi
    }
}
