package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * 提供 `AdditionalFilesDirectives` 单例，集中承载测试指令的共享状态、常量或默认行为。
 */
object AdditionalFilesDirectives : SimpleDirectivesContainer() {
    /**
     * 保存 `CHECK_TYPE`，供测试指令在测试执行期间读取或传递。
     */
    val CHECK_TYPE by directive(
        description = """
            Adds utils functions for type checking
            See file ./compiler/testData/diagnostics/helpers/types/checkType.kt
        """.trimIndent()
    )

    /**
     * 保存 `CHECK_TYPE_WITH_EXACT`，供测试指令在测试执行期间读取或传递。
     */
    val CHECK_TYPE_WITH_EXACT by directive(
        description = """
            Adds utils functions for type checking that use @kotlin.internal.Exact annotation
            See file ./compiler/testData/diagnostics/helpers/types/checkTypeWithExact.kt
        """.trimIndent()
    )

    /**
     * 保存 `WITH_COROUTINES`，供测试指令在测试执行期间读取或传递。
     */
    val WITH_COROUTINES by directive(
        description = """
            Adds utils functions for checking coroutines
            See file ./compiler/testData/diagnostics/helpers/coroutines/CoroutineHelpers.kt
        """.trimIndent()
    )

    /**
     * 保存 `CHECK_STATE_MACHINE`，供测试指令在测试执行期间读取或传递。
     */
    val CHECK_STATE_MACHINE by directive(
        description = """
            Adds utils functions for checking state machines
            May be enabled only with $WITH_COROUTINES directive
            See file ./compiler/testData/diagnostics/helpers/coroutines/StateMachineChecker.kt
        """.trimIndent()
    )

    /**
     * 保存 `CHECK_TAIL_CALL_OPTIMIZATION`，供测试指令在测试执行期间读取或传递。
     */
    val CHECK_TAIL_CALL_OPTIMIZATION by directive(
        description = """
            Adds utils functions for checking tail call optimizations
            May be enabled only with $WITH_COROUTINES directive
            See file ./compiler/testData/diagnostics/helpers/coroutines/TailCallOptimizationChecker.kt
        """.trimIndent()
    )

    /**
     * 保存 `SPEC_HELPERS`，供测试指令在测试执行期间读取或传递。
     */
    val SPEC_HELPERS by directive(
        description = """
            Adds utils functions from `test-spec` modules
            See directory ./compiler/tests-spec/helpers/
        """.trimIndent()
    )

    /**
     * 保存 `INFERENCE_HELPERS`，供测试指令在测试执行期间读取或传递。
     */
    val INFERENCE_HELPERS by directive(
        description = """
            Adds utils functions for type checking
            See file ./compiler/testData/diagnostics/helpers/inference/inferenceUtils.kt
        """.trimIndent()
    )
}
