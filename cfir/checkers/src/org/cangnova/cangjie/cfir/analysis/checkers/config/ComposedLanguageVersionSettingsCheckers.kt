package org.cangnova.cangjie.cfir.analysis.checkers.config

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.LanguageVersionSettingsCheckers

class ComposedLanguageVersionSettingsCheckers : LanguageVersionSettingsCheckers() {

    override val languageVersionSettingsCheckers: Set<CfirLanguageVersionSettingsChecker>
        field = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: LanguageVersionSettingsCheckers) {
        languageVersionSettingsCheckers += checkers.languageVersionSettingsCheckers
    }

}
