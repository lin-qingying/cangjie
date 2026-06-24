package org.cangnova.cangjie.cfir.analysis.checkers.config

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.LanguageVersionSettingsCheckers

/** 可累积注册多个语言版本设置 checker 分组的组合容器。 */
class ComposedLanguageVersionSettingsCheckers : LanguageVersionSettingsCheckers() {

    /** 已注册到当前组合容器中的语言版本设置 checker 集合。 */
    override val languageVersionSettingsCheckers: Set<CfirLanguageVersionSettingsChecker>
        field = mutableSetOf()

    /** 将另一个分组中的语言版本设置 checker 合并进当前组合容器。 */
    @CheckersComponentInternal
    fun register(checkers: LanguageVersionSettingsCheckers) {
        languageVersionSettingsCheckers += checkers.languageVersionSettingsCheckers
    }

}
