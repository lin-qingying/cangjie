package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.AnalysisFlag
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.session.languageVersionSettings

/**
 * CFIR 中统一读取 session 语言设置的上下文扩展。
 *
 * Kotlin FIR 将这些查询放在 `fir/providers` 的
 * `LanguageVersionUtils.kt`，让 resolver、provider 和 checker 不直接拿
 * `LanguageVersionSettings` 重复实现门禁逻辑。CFIR 保持同一层级和调用
 * 方向：具体 feature 的启用状态仍只由 session settings 决定。
 */
context(c: SessionHolder)
fun LanguageFeature.isEnabled(): Boolean =
    c.session.languageVersionSettings.supportsFeature(this)

/** 返回当前 session 中一个布尔分析标志是否被置位。 */
context(c: SessionHolder)
fun AnalysisFlag<Boolean>.isSet(): Boolean =
    c.session.languageVersionSettings.getFlag(this)

/** 与 Kotlin FIR 同构的 feature 反向查询。 */
context(c: SessionHolder)
fun LanguageFeature.isDisabled(): Boolean = !isEnabled()
