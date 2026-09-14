package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.expressionInfoProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.findUsageSimpleName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetNameText
import org.cangnova.cangjie.analysis.api.resolution.CaVariableAccessCall
import org.cangnova.cangjie.analysis.api.resolution.successfulVariableAccessCall
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 变量访问类型（读 / 写）公开 API 的抽象测试。
 *
 * 对齐 Kotlin `AbstractVariableAccessKindTest`：按 `TARGET_NAME` 定位变量访问引用，
 * 渲染 `CaVariableAccessCall.kind`。写访问额外输出赋值右侧表达式文本，
 * 保证不完整赋值（缺少右值）在 golden 中可见。
 */
abstract class AbstractVariableAccessKindTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行变量访问类型测试。
     *
     * 方法按 `TARGET_NAME` 定位变量访问引用使用点，渲染：
     * 1. 访问类别（Read / Write）；
     * 2. 写访问的右侧新值文本。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val accessReference = findUsageSimpleName(mainFile, directives.targetNameText)

        val actual = analyzeForTest(accessReference) {
            val callInfo = accessReference.resolveToCall()
            val accessCall = callInfo?.successfulVariableAccessCall()
            buildString {
                appendLine("callInfoClass: ${callInfo?.let { it::class.simpleName } ?: "null"}")
                when (val kind = accessCall?.kind) {
                    null -> appendLine("kind: NO_VARIABLE_ACCESS")
                    is CaVariableAccessCall.Kind.Read -> appendLine("kind: Read")
                    is CaVariableAccessCall.Kind.Write ->
                        appendLine("kind: Write(value: ${kind.value?.text ?: "null"})")
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = "kind.txt")
    }
}
