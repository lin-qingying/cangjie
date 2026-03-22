package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceLogger
import org.cangnova.cangjie.cfir.resolve.inference.inferenceLogger
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.DiagnosticsDirectives
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.moduleStructure
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.util.MultiModuleInfoDumper

class CfirInferenceLogsHandler(
    testServices: TestServices,
) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives, DiagnosticsDirectives)

    private val dumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")
    private val collectedLogsByModule = linkedMapOf<TestModule, String>()

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            if (CfirDiagnosticsDirectives.DUMP_INFERENCE_LOGS !in currentModule.directives) continue

            val logger = part.session.inferenceLogger
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
                        appendLine(renderItem(item))
                    }
                }
            }
        }
    }

    private fun renderOwner(owner: CfirInferenceLogger.BlockOwner): String = when (owner) {
        is CfirInferenceLogger.BlockOwner.CandidateOwner -> owner.candidate.toString()
        CfirInferenceLogger.BlockOwner.Unknown -> "Unknown"
    }

    private fun renderItem(item: CfirInferenceLogger.BlockItemElement): String = when (item) {
        is CfirInferenceLogger.NewVariableElement -> "NEW ${item.variable.lookupTag.name}"
        is CfirInferenceLogger.ConstraintElement -> "CONSTRAINT ${item.formatted}"
        is CfirInferenceLogger.ErrorElement -> "ERROR ${item.issue.message} @ ${item.issue.position}"
        is CfirInferenceLogger.FixVariableElement -> "FIX ${item.variable.lookupTag.name} -> ${item.resultType}"
    }
}
