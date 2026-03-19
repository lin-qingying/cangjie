package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.LanguageVersionSettings

/**
 * 语言配置 session 组件。
 *
 * 承载编译器特性开关、目标平台配置、实验性功能控制等。
 * 对齐 K2 的 FirLanguageSettingsComponent，但移除了 isMetadataCompilation
 * （仓颉没有独立的 metadata 编译模式）。
 */
class CfirLanguageSettingsComponent(
    val languageVersionSettings: LanguageVersionSettings,
) : CfirSessionComponent

private val CfirSession.languageSettingsComponent: CfirLanguageSettingsComponent
    by CfirSession.sessionComponentAccessor()

val CfirSession.languageVersionSettings: LanguageVersionSettings
    get() = languageSettingsComponent.languageVersionSettings