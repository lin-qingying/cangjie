package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.test.directives.MacroConstructionDirectives
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.moduleStructure
import java.io.File

/**
 * 宏端到端测试专用 CFIR dump。
 *
 * 这里读取的是 CFIR frontend facade 完成 macro construction、artifact 准备、
 * executor 展开后的 [CfirOutputArtifact]，用于确认宏展开后的 ordinary resolve/check 输入。
 */
class MacroExpandedCfirDumpHandler(
    testServices: TestServices,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(MacroConstructionDirectives)

    private val renderedFilesByModule = linkedMapOf<TestModule, List<RenderedMacroExpandedCfirFile>>()

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        if (MacroConstructionDirectives.DUMP_MACRO_EXPANDED_CFIR !in module.directives) return

        val files = module.files.asSequence()
            .filterNot(TestFile::isAdditional)
            .mapNotNull { testFile ->
                val cfirFile = info.allFirFilesByTestFile[testFile] ?: return@mapNotNull null
                RenderedMacroExpandedCfirFile(
                    fileName = testFile.name,
                    isMacroPackage = cfirFile.packageDirective.isMacroPackage,
                    rendered = cfirFile.renderForMacroExpandedDump(),
                )
            }
            .toList()

        renderedFilesByModule[module] = files
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.macroExpandedCfirSideFile()

        if (MacroConstructionDirectives.DUMP_MACRO_EXPANDED_CFIR !in testServices.moduleStructure.allDirectives) {
            testServices.assertNoUnexpectedSideFile(
                expectedFile,
                MacroConstructionDirectives.DUMP_MACRO_EXPANDED_CFIR,
            )
            return
        }

        if (renderedFilesByModule.isEmpty()) {
            testServices.assertions.fail {
                "DUMP_MACRO_EXPANDED_CFIR is enabled, but no expanded CFIR files were collected for ${testDataFile.name}."
            }
        }

        val actual = buildString {
            renderedFilesByModule.values
                .flatten()
                .forEachIndexed { index, file ->
                    if (index > 0) appendLine().appendLine()
                    appendLine("// FILE: ${file.fileName}")
                    appendLine("// MACRO_PACKAGE: ${file.isMacroPackage}")
                    append(file.rendered.trimEnd())
                    appendLine()
                }
        }

        testServices.assertions.assertEqualsToFile(expectedFile, actual)
    }
}

private data class RenderedMacroExpandedCfirFile(
    val fileName: String,
    val isMacroPackage: Boolean,
    val rendered: String,
)

private fun CfirFile.renderForMacroExpandedDump(): String {
    return CfirRenderer.withGoldenCompat().renderElementAsString(this)
}

private fun File.macroExpandedCfirSideFile(): File {
    return parentFile.resolve("$nameWithoutExtension.macro.cfir.txt")
}
