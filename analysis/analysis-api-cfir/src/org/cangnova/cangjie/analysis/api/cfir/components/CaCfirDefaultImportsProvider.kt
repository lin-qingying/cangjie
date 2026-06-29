package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaDefaultImportsProvider
import org.cangnova.cangjie.analysis.api.impl.base.import.CaBaseDefaultImportsProvider
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.resolve.DefaultImportsProvider


/**
 * CFIR Analysis API 的默认导入 provider。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirDefaultImportsProvider : CaBaseDefaultImportsProvider() {
    /**
     * 返回编译器层统一使用的默认导入 provider。
     */
    override fun getCompilerDefaultImportsProvider(): DefaultImportsProvider = CommonDefaultImportsProvider
}


/**
 * 通用平台默认导入 provider。
 */
object CommonDefaultImportsProvider : DefaultImportsProvider() {
    /**
     * 仓颉通用 CFIR 后端暂不追加平台专属默认导入。
     */
    override val platformSpecificDefaultImports: List<ImportPath> = emptyList()
}
