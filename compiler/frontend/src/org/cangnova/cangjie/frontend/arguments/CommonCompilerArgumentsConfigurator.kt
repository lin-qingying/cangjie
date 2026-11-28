package org.cangnova.cangjie.frontend.arguments

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.parsingDiagnosticSettings
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.ParsingDiagnosticSettings
import org.cangnova.cangjie.messages.WarningGroup

/**
 * 文本 CLI 与直接 frontend 调用共用的警告配置入口。原生 Driver 将 warning actions 稳定前移，
 * 因此调用者必须在路径处理和任何 raw parser 之前安装本次完整状态；跨参数顺序不能从数组推导。
 */
fun CompilerConfiguration.configureWarningOptions(arguments: CommonCompilerArguments): Boolean {
    val occurrences = arguments.explicitArgumentOccurrences.filter { (name, _) -> name == "warn-off" || name == "warn-on" }
    if (occurrences.filter { it.first == "warn-off" }.map { it.second } != arguments.warnOff.toList() ||
        occurrences.filter { it.first == "warn-on" }.map { it.second } != arguments.warnOn.toList()
    ) {
        messageCollector.report(CompilerMessageSeverity.ERROR, "Warning options require their complete original occurrence order.")
        return false
    }
    val suppressedGroups = mutableSetOf<WarningGroup>()
    for ((name, value) in occurrences) {
        if (value !in WarningGroup.commandLineValues) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Invalid value for argument '--$name': '$value'.")
            return false
        }
        val groups = if (value == "all") WarningGroup.entries else listOf(checkNotNull(WarningGroup.fromOptionName(value)))
        if (name == "warn-off") suppressedGroups.addAll(groups) else suppressedGroups.removeAll(groups.toSet())
    }
    parsingDiagnosticSettings = ParsingDiagnosticSettings(parsingDiagnosticSettings.disableWarnings, suppressedGroups)
    return true
}
