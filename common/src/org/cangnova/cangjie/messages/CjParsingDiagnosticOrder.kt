package org.cangnova.cangjie.messages

import java.util.Collections
import java.util.UUID

/**
 * 实际 parser 根中的诊断调用区域。父 producer 在进入正文前分配子路径，
 * 完整解析和惰性展开使用相同路径，不以正文实际展开的时间决定首报顺序。
 */
class CjParsingDiagnosticScope(val rootIdentity: UUID, path: List<Long> = emptyList()) {
    val path: List<Long> = Collections.unmodifiableList(path.toList())

    init {
        require(this.path.all { it >= 0 })
    }

    fun child(sequence: Long): CjParsingDiagnosticScope {
        require(sequence >= 0)
        return CjParsingDiagnosticScope(rootIdentity, path + sequence)
    }

    override fun equals(other: Any?): Boolean = other is CjParsingDiagnosticScope &&
        rootIdentity == other.rootIdentity && path == other.path

    override fun hashCode(): Int = 31 * rootIdentity.hashCode() + path.hashCode()
}

/** 报告身份在 producer 入队时确定；位置排序和语法树父子关系均不参与此身份。 */
data class CjParsingDiagnosticOrder(val scope: CjParsingDiagnosticScope, val sequence: Long) {
    init {
        require(sequence >= 0)
    }

    val rootIdentity: UUID get() = scope.rootIdentity
    val path: List<Long> get() = scope.path + sequence

    fun compareWithinRoot(other: CjParsingDiagnosticOrder): Int {
        require(rootIdentity == other.rootIdentity) { "Independent diagnostic roots have no shared producer clock." }
        val left = path
        val right = other.path
        for (index in 0 until minOf(left.size, right.size)) {
            val comparison = left[index].compareTo(right[index])
            if (comparison != 0) return comparison
        }
        return left.size.compareTo(right.size)
    }
}

/** 诊断值与单次报告身份分离；发布 DTO 的来源、范围及相等性不随树遍历协议改变。 */
data class CjParsingDiagnosticReport(val diagnostic: ParsingDiagnostic, val order: CjParsingDiagnosticOrder)

/** 完成后的发布结果与按 producer 顺序保存的报告；后续源码映射必须保留后者的顺序。 */
class CjParsingDiagnosticPublication(
    diagnostics: List<ParsingDiagnostic>,
    reports: List<CjParsingDiagnosticReport>,
) {
    val diagnostics: List<ParsingDiagnostic> = Collections.unmodifiableList(diagnostics.toList())
    val reports: List<CjParsingDiagnosticReport> = Collections.unmodifiableList(reports.toList())

    init {
        require(this.diagnostics.all { it.mainRange != null } && this.reports.all { it.diagnostic.mainRange != null }) {
            "A diagnostic publication requires completed source ranges."
        }
    }

    companion object {
        /** 没有 source parser 的手工 raw 产品明确使用空报告，不制造 parser 根或 source 身份。 */
        val EMPTY: CjParsingDiagnosticPublication = CjParsingDiagnosticPublication(emptyList(), emptyList())
    }
}
