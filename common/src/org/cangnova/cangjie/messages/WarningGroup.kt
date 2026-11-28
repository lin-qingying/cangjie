package org.cangnova.cangjie.messages

import java.util.Collections

/** 原生 WarningOptionMgr 的完整警告组身份；错误不使用警告组，UNGROUPED 只接受 all 操作。 */
enum class WarningGroup(val optionName: String?) {
    UNUSED("unused"),
    UNUSED_MAIN("unused-main"),
    DRIVER_ARG("driver-arg"),
    DEPRECATED("deprecated"),
    UNSUPPORT_COMPILE_SOURCE("unsupport-compile-source"),
    PACKAGE_IMPORT("package-import"),
    PARSER("parser"),
    SEMANTICS("semantics"),
    INTERPRETER("interpreter"),
    APILEVEL_CHECK("apilevel-check"),
    UNGROUPED(null);

    companion object {
        /** 公开参数的单值集合；保留原生大小写，不接受诊断定义名或逗号列表。 */
        val commandLineValues: Set<String> = Collections.unmodifiableSet(entries.mapNotNull { it.optionName }.toSet() + "all")

        fun fromOptionName(value: String): WarningGroup? = entries.singleOrNull { it.optionName == value }
    }
}
