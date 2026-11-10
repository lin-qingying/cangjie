package org.cangnova.cangjie.ide.completion.api.lookup

import org.cangnova.cangjie.ImportPath

/**
 * 候选的导入目标。
 *
 * 刻意把「import 里写什么」与「表达式里写什么」分开：
 * 仓颉的 import 携带组织名与别名，而表达式访问只能用短名或包限定名，
 * 两者混为一体就会出现「把 `org::` 前缀抄进表达式」这类非法插入。
 */
interface CompletionImportTarget {
    /** 需要追加或复用的 import 路径，含组织名与别名。 */
    val importPath: ImportPath

    /**
     * 在表达式位置应当写入的文本。
     *
     * - 已直接可达：短名；
     * - 已通过别名导入：别名；
     * - 需限定后再缩短：包限定名（如 `a.b.C`）。
     */
    val expressionText: String
}

/**
 * 导入目标的标准实现。
 */
data class CompletionImportTargetData(
    override val importPath: ImportPath,
    override val expressionText: String,
) : CompletionImportTarget

/**
 * 候选的导入策略。
 *
 * 策略由**分析期**给出，写入由界面期的写事务执行：
 * 两者分离才能保证「分析不碰文档」且「一次撤销覆盖插入与导入」。
 */
sealed interface CompletionImportPlan {
    /** 当前文件已可直接引用，无需任何导入改动。 */
    data object NoImport : CompletionImportPlan

    /** 追加一条 import 后使用短名（或已有别名）。 */
    data class AddImport(val target: CompletionImportTarget) : CompletionImportPlan

    /**
     * 先写入限定名，再交由引用缩短逻辑收回短名。
     *
     * 用于「追加 import 会与既有同名绑定冲突」或「该包已被整体导入但目标未导出」的场景。
     */
    data class QualifyThenShorten(val target: CompletionImportTarget) : CompletionImportPlan
}

/**
 * 导入冲突类型。
 *
 * 冲突不是错误，而是**必须选择策略**的信号：不同冲突的处理方式不同，
 * 因此必须能被界面期区分并分别处理，而不是笼统地「插入失败」。
 */
enum class ImportConflictKind {
    /** 短名已被同一声明绑定：目标已导入，只是走错了策略（幂等情形）。 */
    ALREADY_BOUND_SAME_DECLARATION,

    /** 短名已被**其它**声明绑定：追加 import 会造成二义。 */
    SHORT_NAME_TAKEN_BY_OTHER,

    /** 该包已整体导入，但目标未在包内导出。 */
    NOT_EXPORTED_BY_ALL_UNDER_PACKAGE,
}

/**
 * 导入冲突的诊断结论。
 *
 * 由分析期产出，界面期据此选择「放弃该候选」「改用限定名」或「提示冲突」。
 */
data class ImportConflict(
    val kind: ImportConflictKind,
    /** 与短名冲突的既有绑定描述（限定名或包路径），供提示文案与测试断言使用。 */
    val conflictingBinding: String,
)

/**
 * 导入计划与冲突结论的组合。
 *
 * 候选带 [plan] 不代表一定能直接写入：界面期在写事务内会用 [conflict] 复验一次，
 * 复验结果与分析期不一致时以复验为准（文档可能已被用户改动）。
 */
data class ImportResolution(
    val plan: CompletionImportPlan,
    val conflict: ImportConflict? = null,
) {
    companion object {
        /** 当前文件已可达的常见结论。 */
        val NoImport: ImportResolution = ImportResolution(CompletionImportPlan.NoImport)
    }

    /** 是否需要在写事务中改动 import 语句。 */
    val requiresImportWrite: Boolean
        get() = plan is CompletionImportPlan.AddImport
}