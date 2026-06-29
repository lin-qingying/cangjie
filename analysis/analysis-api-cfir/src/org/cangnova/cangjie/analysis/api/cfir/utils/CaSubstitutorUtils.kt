package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor

/**
 * 对齐 Kotlin `typeUtils.kt` 中 `ConeSubstitutor -> KaSubstitutor` 的入口。
 */
internal fun ConeSubstitutor.toCaSubstitutor(analysisSession: CaCfirSession): CaSubstitutor =
    analysisSession.cfirSymbolBuilder.typeBuilder.buildSubstitutor(this)

/**
 * 在当前 CFIR Analysis API 会话上下文中将 Cone 替换器转换为公开替换器。
 */
context(analysisSession: CaCfirSession)
internal fun ConeSubstitutor.toCaSubstitutor(): CaSubstitutor = toCaSubstitutor(analysisSession)
