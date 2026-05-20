package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure

/**
 * dangling file 场景下的文件诊断收集测试。
 *
 * 对齐 Kotlin `AbstractDanglingFileCollectDiagnosticsTest`：复制主文件为非物理 PSI，
 * 并通过 `originalFile` 让项目结构服务把它归入 dangling file module。
 */
abstract class AbstractDanglingFileCollectDiagnosticsTest : AbstractCollectDiagnosticsTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_DANGLING_FILES by stringDirective("Ignore dangling file diagnostic tests.")
        val IGNORE_SELF_MODE by stringDirective("Whether to use ${CaDanglingFileResolutionMode.IGNORE_SELF} mode.")
    }

    override fun doTest(testServices: TestServices) {
        try {
            super.doTest(testServices)
        } catch (error: AssertionError) {
            if (Directives.IGNORE_DANGLING_FILES !in testServices.moduleStructure.allDirectives) {
                throw error
            }
        }
    }

    override fun prepareCjFile(cjFile: CjFile, testServices: TestServices): PreparedFile {
        val psiFactory = CjPsiFactory.contextual(cjFile, markGenerated = true)
        val fakeFile = psiFactory.createFile("fake.cj", cjFile.text).apply {
            originalFile = cjFile
        }

        return PreparedFile(fakeFile, cjFile.name)
    }
}
