package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaDanglingFileModuleImpl
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.explicitModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjTypeCodeFragment
import org.cangnova.cangjie.test.services.TestServices

/**
 * code fragment 场景下的文件诊断收集测试。
 *
 * 对齐 Kotlin `AbstractCodeFragmentCollectDiagnosticsTest`：主文件提供上下文元素，
 * 同名 `.fragment.cj` 文件提供待分析片段文本。
 */
abstract class AbstractCodeFragmentCollectDiagnosticsTest : AbstractCollectDiagnosticsTest() {
    /**
     * 执行 code fragment 诊断收集测试。
     *
     * 方法从主文件定位上下文元素，读取 `.fragment.cj` 片段文本，构造对应 code fragment 后收集诊断。
     */
    override fun doTest(testServices: TestServices) {
        val (mainFile, mainModule) = findMainFileAndModule(testServices)
        if (mainFile == null) {
            error("`${AbstractCodeFragmentCollectDiagnosticsTest::class.simpleName}` does not support multiple use-site files.")
        }

        val contextElement = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjElement>(mainFile)
        val fragmentFile = mainModule.testModule.files.single { testFile ->
            testFile.name.endsWith(".fragment.cj")
        }
        val fragmentText = fragmentFile.originalContent
            .removePrefix("\n".repeat(fragmentFile.startLineNumberInOriginalFile))
            .removeFileDirectiveLine()
            .trimEnd('\r', '\n')

        val project = mainFile.project
        val factory = CjPsiFactory(project, markGenerated = false)

        val codeFragment = when {
            fragmentText.startsWith("// CODE_FRAGMENT_KIND: TYPE") ->
                CjTypeCodeFragment(project, "fragment.cj", fragmentText, contextElement)
            fragmentText.any { it == '\n' } -> factory.createBlockCodeFragment(fragmentText, contextElement)
            else -> factory.createExpressionCodeFragment(fragmentText, contextElement)
        }

        @OptIn(CaExperimentalApi::class)
        codeFragment.explicitModule = CaDanglingFileModuleImpl(
            files = listOf(codeFragment),
            contextModule = mainModule.caModule,
            resolutionMode = CaDanglingFileResolutionMode.PREFER_SELF,
        )

        val preparedFile = PreparedFile(codeFragment, mainFile.name)
        doTestByPreparedFiles(listOf(preparedFile), testServices)
    }

    /**
     * 移除 fragment 文件开头的 `// FILE:` 指令行。
     *
     * 片段 PSI 只需要真实代码文本，测试框架文件指令不应进入 fragment 内容。
     */
    private fun String.removeFileDirectiveLine(): String {
        if (!startsWith("// FILE:")) return this
        val lineEnd = indexOf('\n')
        return if (lineEnd == -1) "" else substring(lineEnd + 1)
    }
}
