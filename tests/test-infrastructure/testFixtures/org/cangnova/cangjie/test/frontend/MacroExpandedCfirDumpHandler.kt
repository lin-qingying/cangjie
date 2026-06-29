package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.renderer.CfirModifierRenderer
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
    /**
     * 保存 `directiveContainers`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(MacroConstructionDirectives)

    /**
     * 保存 `renderedFilesByModule`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val renderedFilesByModule = linkedMapOf<TestModule, List<RenderedMacroExpandedCfirFile>>()

    /**
     * 执行 `processModule` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `processAfterAllModules` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
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

/**
 * 表示 `RenderedMacroExpandedCfirFile`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
private data class RenderedMacroExpandedCfirFile(
    /**
     * 保存 `fileName`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val fileName: String,
    /**
     * 保存 `isMacroPackage`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val isMacroPackage: Boolean,
    /**
     * 保存 `rendered`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val rendered: String,
)

/**
 * 提供 `renderForMacroExpandedDump` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
private fun CfirFile.renderForMacroExpandedDump(): String {
    return CfirRenderer(
        modifierRenderer = object : CfirModifierRenderer() {
            override fun renderModifiers(constructor: CfirConstructor) {
                renderVisibility(constructor.status, constructor.source)
            }
        },
    ).renderElementAsString(this)
}

/**
 * 提供 `macroExpandedCfirSideFile` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
 */
private fun File.macroExpandedCfirSideFile(): File {
    return parentFile.resolve("$nameWithoutExtension.macro.cfir.txt")
}
