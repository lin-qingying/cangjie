package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceLogger
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.DiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.util.MultiModuleInfoDumper

/**
 * 表示 `CfirInferenceLogsHandler`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirInferenceLogsHandler(
    testServices: TestServices,
) : CfirAnalysisHandler(testServices) {
    /**
     * 保存 `directiveContainers`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives, DiagnosticsDirectives)

    /**
     * 保存 `dumper`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val dumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")
    /**
     * 保存 `collectedLogsByModule`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    private val collectedLogsByModule = linkedMapOf<TestModule, String>()

    /**
     * 执行 `processModule` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            if (CfirDiagnosticsDirectives.DUMP_INFERENCE_LOGS !in currentModule.directives) continue

            val logger = part.session.inferenceLoggerOrNull
            val renderedLogs = when {
                logger == null -> "<no inference logger>"
                logger.topLevelElements.isEmpty() -> "<no inference logs>"
                else -> renderLogger(logger).trimEnd()
            }

            if (logger != null) {
                collectedLogsByModule[currentModule] = renderedLogs
            }
        }
    }

    /**
     * 执行 `processAfterAllModules` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val directives = testServices.moduleStructure.allDirectives
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.cfirSideFile("inference.txt")

        if (CfirDiagnosticsDirectives.DUMP_INFERENCE_LOGS !in directives) {
            testServices.assertNoUnexpectedSideFile(expectedFile, CfirDiagnosticsDirectives.DUMP_INFERENCE_LOGS)
            return
        }

        collectedLogsByModule.forEach { (module, renderedLogs) ->
            dumper.builderForModule(module).append(renderedLogs)
        }

        if (collectedLogsByModule.isEmpty()) {
            testServices.assertions.fail {
                buildString {
                    append("DUMP_INFERENCE_LOGS is enabled, but no inference logs were collected")
                    if (!expectedFile.exists()) {
                        append(" for ${testDataFile.name}")
                    }
                    append('.')
                }
            }
        }

        testServices.assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
    }

    /**
     * 提供 `renderLogger` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun renderLogger(logger: CfirInferenceLogger): String {
        return buildString {
            logger.topLevelElements.forEachIndexed { index, block ->
                if (index > 0) appendLine().appendLine()
                appendLine("## ${block.name}")
                appendLine("owner: ${renderOwner(block.owner)}")
                if (block.items.isEmpty()) {
                    append("<empty>")
                } else {
                    block.items.forEach { item ->
                        append(renderItem(item)).appendLine()
                    }
                }
            }
        }
    }

    /**
     * 提供 `renderOwner` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun renderOwner(owner: CfirInferenceLogger.BlockOwner): String = when (owner) {
        is CfirInferenceLogger.BlockOwner.CandidateOwner -> owner.candidate.toString()
        CfirInferenceLogger.BlockOwner.Unknown -> "Unknown"
    }

    /**
     * 提供 `renderItem` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun renderItem(item: CfirInferenceLogger.BlockItemElement): String = buildString {
        when (item) {
            is CfirInferenceLogger.NewVariableElement -> append("- NEW ${item.variable.lookupTag.name}")
            is CfirInferenceLogger.ConstraintElement -> {
                append("- CONSTRAINT ${item.formatted}")
                if (item.origins.isNotEmpty()) {
                    appendLine()
                    append(renderOrigins(item.origins))
                }
            }
            is CfirInferenceLogger.ErrorElement -> append("- ERROR ${item.issue.message} @ ${item.issue.position}")
            is CfirInferenceLogger.FixVariableElement -> append("- FIX ${item.variable.lookupTag.name} -> ${item.resultType}")
        }
    }

    /**
     * 提供 `renderOrigins` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    private fun renderOrigins(origins: List<CfirInferenceLogger.ConstraintElement>): String = buildString {
        origins.forEachIndexed { index, origin ->
            if (index > 0) appendLine()
            append("  origins[").append(index).append("]: ").append(origin.formatted)
        }
    }
}

/**
 * 保存 `CfirSession.inferenceLoggerOrNull`，供CFIR 前端测试在测试执行期间读取或传递。
 */
private val CfirSession.inferenceLoggerOrNull: CfirInferenceLogger?
    by CfirSession.nullableSessionComponentAccessor()
