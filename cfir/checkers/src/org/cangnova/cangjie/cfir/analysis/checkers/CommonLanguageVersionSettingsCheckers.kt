package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.config.CfirLanguageVersionSettingsChecker

object CommonLanguageVersionSettingsCheckers : LanguageVersionSettingsCheckers() {
    override val languageVersionSettingsCheckers: Set<CfirLanguageVersionSettingsChecker> = setOf(

    )

}

