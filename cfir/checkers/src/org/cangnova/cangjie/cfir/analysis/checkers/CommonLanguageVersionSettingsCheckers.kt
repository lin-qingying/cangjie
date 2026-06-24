package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.config.CfirLanguageVersionSettingsChecker

/** CFIR 默认语言版本设置 checker 注册表。 */
object CommonLanguageVersionSettingsCheckers : LanguageVersionSettingsCheckers() {
    /** 当前默认配置下启用的语言版本设置 checker 集合。 */
    override val languageVersionSettingsCheckers: Set<CfirLanguageVersionSettingsChecker> = setOf(

    )

}
