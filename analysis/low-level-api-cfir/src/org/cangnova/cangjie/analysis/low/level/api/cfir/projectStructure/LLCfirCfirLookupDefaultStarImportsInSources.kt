/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirLibraryOrLibrarySourceResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.cfir.scopes.CfirLookupDefaultStarImportsInSourcesSettingHolder

/**
 * Disables default star import lookup optimization when analyzing Stdlib sources (or another project with enabled `-Xallow-kotlin-package`
 * flag), even if attached as a .jar dependency (i.e., not Stdlib project itself).
 * This is done for all library source analysis sessions because there's no reliable way to distinguish stdlib .jar from other libs.
 */
internal fun LLCfirSession.createLookupDefaultStarImportsInSourcesSettingHolder(
    languageVersionSettings: LanguageVersionSettings,
): CfirLookupDefaultStarImportsInSourcesSettingHolder {
    val value =
        CfirLookupDefaultStarImportsInSourcesSettingHolder.defaultSetting(languageVersionSettings) || isLibrarySourceAnalysisSession()
    return CfirLookupDefaultStarImportsInSourcesSettingHolder(value)
}

private fun LLCfirSession.isLibrarySourceAnalysisSession(): Boolean =
    this is LLCfirLibraryOrLibrarySourceResolvableModuleSession && ktModule is CaLibrarySourceModule
