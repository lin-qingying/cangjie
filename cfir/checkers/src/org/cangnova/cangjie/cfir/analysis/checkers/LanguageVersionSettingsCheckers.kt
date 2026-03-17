package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.config.CfirLanguageVersionSettingsChecker

abstract class LanguageVersionSettingsCheckers {
    companion object {
        val EMPTY: LanguageVersionSettingsCheckers = object : LanguageVersionSettingsCheckers() {}
    }

    open val languageVersionSettingsCheckers: Set<CfirLanguageVersionSettingsChecker> = emptySet()
}
