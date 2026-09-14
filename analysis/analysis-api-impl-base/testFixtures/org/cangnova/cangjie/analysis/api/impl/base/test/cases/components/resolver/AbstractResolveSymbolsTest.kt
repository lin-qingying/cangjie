package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.resolver

import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiReferenceTestUtils.findUsageSimpleName
import org.cangnova.cangjie.analysis.api.impl.base.test.targetNameText
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `resolver.resolveToSymbols` 的抽象测试。
 *
 * 与 `AbstractResolveSymbolTest` 只断言唯一目标的语义不同，本测试直接对齐 Kotlin
 * `AbstractResolveSymbolsTest`：渲染引用表达式解析出的完整候选集合，覆盖重载歧义、
 * 未解析等多目标/零目标场景。
 */
abstract class AbstractResolveSymbolsTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行引用候选集合解析测试。
     *
     * 方法按 `TARGET_NAME` 定位引用使用点，渲染 `resolveToSymbols()` 返回的符号集合；
     * 空集合显式输出 `NO_SYMBOLS`，保证 golden 中可见。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val nameReference = findUsageSimpleName(mainFile, directives.targetNameText)

        val actual = analyzeForTest(nameReference) {
            val symbols = nameReference.resolveToSymbols()
            buildString {
                appendLine("symbolsSize: ${symbols.size}")
                if (symbols.isEmpty()) {
                    append("NO_SYMBOLS")
                } else {
                    append(symbols.joinToString(separator = "\n\n") { symbol -> renderSymbolForResolveTest(symbol) })
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = "symbols.txt")
    }
}
