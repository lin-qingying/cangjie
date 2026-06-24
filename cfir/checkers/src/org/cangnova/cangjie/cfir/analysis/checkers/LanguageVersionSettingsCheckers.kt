package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.config.CfirLanguageVersionSettingsChecker

/** 语言版本设置 checker 分组基类，用于按配置注册可组合的全局 checker 集合。 */
abstract class LanguageVersionSettingsCheckers {
    /** 默认空分组，供没有语言设置 checker 的配置路径复用。 */
    companion object {
        /** 不包含任何语言版本设置 checker 的共享实例。 */
        val EMPTY: LanguageVersionSettingsCheckers = object : LanguageVersionSettingsCheckers() {}
    }

    /** 当前分组暴露的语言版本设置 checker 集合。 */
    open val languageVersionSettingsCheckers: Set<CfirLanguageVersionSettingsChecker> = emptySet()
}
