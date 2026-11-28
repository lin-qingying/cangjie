package org.cangnova.cangjie.messages

import org.cangnova.cangjie.parsing.CjModifierKind
import java.util.Collections

/** 编译调用的警告策略；suppression 在共享根去重之前生效，Werror 不改变原始 severity。 */
class ParsingDiagnosticSettings(
    val disableWarnings: Boolean = false,
    suppressedGroups: Set<WarningGroup> = emptySet(),
) {
    val suppressedGroups: Set<WarningGroup> = Collections.unmodifiableSet(suppressedGroups.toSet())

    /** 同一次编译的 driver 与 parser 使用相同组状态，独立全局关闭不被 Won 覆盖。 */
    fun isSuppressed(group: WarningGroup): Boolean = disableWarnings || group in suppressedGroups

    fun isSuppressed(diagnostic: ParsingDiagnostic): Boolean = diagnostic.severity == ParsingDiagnostic.Severity.WARNING &&
        (disableWarnings || diagnostic.warning?.group in suppressedGroups)

    override fun equals(other: Any?): Boolean = other is ParsingDiagnosticSettings &&
        disableWarnings == other.disableWarnings && suppressedGroups == other.suppressedGroups

    override fun hashCode(): Int = 31 * disableWarnings.hashCode() + suppressedGroups.hashCode()
}

/** WARNING 的原生身份与格式参数；公共 parser 层不持有 CFIR 诊断工厂。 */
sealed interface ParsingWarning {
    val nativeKind: String
    val group: WarningGroup get() = WarningGroup.PARSER
    val suppressionNote: String
        get() = "this warning can be suppressed by setting the compiler option `-Woff ${checkNotNull(group.optionName)}`"

    sealed interface Redundancy {
        data class Explicit(val higherModifier: CjModifierKind) : Redundancy
        data object InterfaceMember : Redundancy
    }

    data class RedundantModifier(val modifier: CjModifierKind, val reason: Redundancy) : ParsingWarning {
        override val nativeKind: String get() = "parse_redundant_modifier"
    }

    data class UnsafeIgnored(val backend: String) : ParsingWarning {
        override val nativeKind: String get() = "parse_unsafe_will_be_ignored"
    }

    data class ConfusingLineTerminator(val previousTokenText: String, val nextTokenText: String) : ParsingWarning {
        override val nativeKind: String get() = "parse_nl_warning"
    }

    /** v1.1.3 定义存在但未发现原生报告点；保留表示，不发明 producer。 */
    data class EmptyAttribute(val annotationName: String) : ParsingWarning {
        override val nativeKind: String get() = "parse_empty_attribute"
    }

    data class DuplicatedAttributeValue(val value: String) : ParsingWarning {
        override val nativeKind: String get() = "parse_duplicated_attr_value"
    }

    data object JavaMirrorPropertyDeprecated : ParsingWarning {
        override val nativeKind: String get() = "parse_java_mirror_prop_is_deprecated"
    }

    data class MacroReport(val message: String, val hint: String) : ParsingWarning {
        override val nativeKind: String get() = "parse_diag_warning"
    }
}
