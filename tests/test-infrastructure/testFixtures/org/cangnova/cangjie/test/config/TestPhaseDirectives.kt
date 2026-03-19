package org.cangnova.cangjie.test.config

import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer


enum class TestPhase {
    FRONTEND,
    BACKEND,
}
object TestPhaseDirectives : SimpleDirectivesContainer() {
    /**
     * See: [TestPhase] for the list of values.
     * If not set, the test will be generated into [FRONTEND][TestPhase.FRONTEND]
     * phases, but the infrastructure will still complain about the missing directive.
     */
    val RUN_PIPELINE_TILL by enumDirective<TestPhase>(
        description = "Verify that the test runs successfully until the specified phase (including). See `TestPhaseLabel` for the list of phases"
    )

    /**
     * [LATEST_PHASE_IN_PIPELINE] determines at which maximum phase the test pipeline theoretically could execute.
     * [RUN_PIPELINE_TILL] determines at which phase test might fail at leaf module, so it won't count as the test failure.
     *
     * Example:
     * // LATEST_PHASE_IN_PIPELINE: BACKEND
     * // RUN_PIPELINE_TILL: FIR2IR
     *
     * This combination means that the pipeline for all modules in the test will be executed until BACKEND phase, but
     * it's expected the leaf module will fail at FIR2IR stage.
     *
     * Important: [LATEST_PHASE_IN_PIPELINE] should be defined once per test runner and shouldn't be additionally set in any
     * particular test.
     */
    val LATEST_PHASE_IN_PIPELINE by enumDirective<TestPhase>(
        description = "The last phase of the pipeline in the test"
    )

    /**
     * Use this directive if the test includes multiple test runners failing at different phases.
     */
    val DISABLE_NEXT_PHASE_SUGGESTION by stringDirective(
        description = "Prevents the phased test runners from complaining that the current phase could be upgraded."
    )
}