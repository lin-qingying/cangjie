package org.cangnova.cangjie.chir.core.builder

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * CHIR 构建阶段诊断基类。
 */
sealed interface ChirBuildError {
    /**
     * 重复符号诊断。
     */
    data class DuplicateSymbol(
        /**
         * 重复的符号名称。
         */
        val symbolName: String,

        /**
         * 重复符号的语义标识。
         */
        val symbolId: ChirSemanticId,

        /**
         * 诊断详情。
         */
        val detail: String,
    ) : ChirBuildError

    /**
     * 未解析引用诊断。
     */
    data class UnresolvedReference(
        /**
         * 引用语义标识。
         */
        val referenceId: ChirSemanticId,

        /**
         * 未解析的目标名称。
         */
        val targetName: String,
    ) : ChirBuildError

    /**
     * CHIR 图结构无效诊断。
     */
    data class InvalidGraph(
        /**
         * 图结构错误详情。
         */
        val detail: String,
    ) : ChirBuildError
}

/**
 * CHIR 构建诊断收集器。
 */
interface ChirDiagnosticCollector {
    /**
     * 报告一条构建诊断。
     */
    fun report(error: ChirBuildError)
}

/**
 * 忽略所有诊断的诊断收集器。
 */
object NoopChirDiagnosticCollector : ChirDiagnosticCollector {
    /**
     * 忽略传入诊断。
     */
    override fun report(error: ChirBuildError) = Unit
}

/**
 * 记录诊断列表的诊断收集器。
 */
class RecordingChirDiagnosticCollector : ChirDiagnosticCollector {
    /**
     * 可变诊断列表。
     */
    private val mutableErrors = mutableListOf<ChirBuildError>()

    /**
     * 当前已记录诊断的只读视图。
     */
    val errors: List<ChirBuildError>
        get() = mutableErrors

    /**
     * 记录一条构建诊断。
     */
    override fun report(error: ChirBuildError) {
        mutableErrors += error
    }
}
