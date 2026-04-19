package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/**
 * 决定是否关闭“源码中默认星号导入跳过查询”的优化。
 *
 * 对位 Kotlin `FirLookupDefaultStarImportsInSourcesSettingHolder`。
 * 仓颉主干没有 `allow-kotlin-package` 等价语言开关，因此默认值固定为 `false`：
 * 只有 low-level 在特定分析场景下显式构造该组件时，才会关闭该优化。
 */
class CfirLookupDefaultStarImportsInSourcesSettingHolder(internal val value: Boolean) : CfirSessionComponent {
    companion object {
        fun defaultSetting(languageVersionSettings: LanguageVersionSettings): Boolean {
            @Suppress("UNUSED_PARAMETER")
            return false
        }

        fun createDefault(languageVersionSettings: LanguageVersionSettings): CfirLookupDefaultStarImportsInSourcesSettingHolder =
            CfirLookupDefaultStarImportsInSourcesSettingHolder(defaultSetting(languageVersionSettings))
    }
}

private val CfirSession.lookupDefaultStarImportsInSourcesSettingHolder: CfirLookupDefaultStarImportsInSourcesSettingHolder
    by CfirSession.sessionComponentAccessor()

internal val CfirSession.lookupDefaultStarImportsInSources: Boolean
    get() = lookupDefaultStarImportsInSourcesSettingHolder.value
