package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider

/**
 * CFIR 侧默认导入快照。
 *
 * Kotlin FIR 侧默认导入能力由独立的 provider / utility 承担，不与注解、签名混放。
 * 这里保持同样的职责边界：本文件只负责默认导入公开模型的构建与缓存接线。
 */
internal class CaCfirDefaultImportsImpl(
    override val regularImports: List<org.cangnova.cangjie.ImportPath>,
    override val lowPriorityImports: List<org.cangnova.cangjie.ImportPath>,
    override val excludedImports: List<org.cangnova.cangjie.name.FqName>,
    override val token: CaLifetimeToken,
) : CaDefaultImports

/**
 * 从当前 use-site session 构建默认导入公开视图。
 */
internal fun CaCfirSession.renderDefaultImports(): CaDefaultImports {
    return getOrCreateDefaultImports {
        val provider = cfirSession.defaultImportsProvider
        CaCfirDefaultImportsImpl(
            regularImports = provider.getDefaultImports(includeLowPriorityImports = false),
            lowPriorityImports = provider.defaultLowPriorityImports,
            excludedImports = provider.excludedImports,
            token = token,
        )
    }
}
