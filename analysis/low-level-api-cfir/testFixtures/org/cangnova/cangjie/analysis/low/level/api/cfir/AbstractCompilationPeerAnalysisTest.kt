package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.compile.CompilationPeerCollector
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 对齐 Kotlin `AbstractCompilationPeerAnalysisTest` 的 low-level 编译同伴测试。
 *
 * Kotlin FIR 会沿 inline peer 递归收集额外待编译文件；
 * 仓颉当前主干没有那套跨文件 inline backend 语义，因此这里只校验
 * `CompilationPeerCollector` 在真实仓颉语义下返回的源文件集合。
 */
abstract class AbstractCompilationPeerAnalysisTest : AbstractAnalysisApiBasedTest() {
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /**
     * 构建主文件 CFIR 并渲染编译同伴收集结果。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val cfirFile = mainFile.getOrBuildCfirFile(resolutionFacade)
        val compilationPeerData = CompilationPeerCollector.process(listOf(cfirFile))

        val filesToCompile = compilationPeerData.peers.values
            .flatten()
            .map { "File ${it.name}" }
            .sorted()

        val inlineClassesToCompile = compilationPeerData.inlinedClasses
            .map { klass ->
                "Class ${klass.name ?: "<anonymous[${klass.textRange}]>"}"
            }
            .sorted()

        val actualText = (filesToCompile + inlineClassesToCompile).joinToString(separator = "\n")
        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }
}
