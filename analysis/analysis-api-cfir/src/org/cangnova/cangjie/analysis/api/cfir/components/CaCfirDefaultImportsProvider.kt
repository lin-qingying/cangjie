package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaDefaultImportsProvider
import org.cangnova.cangjie.analysis.api.impl.base.import.CaBaseDefaultImportsProvider
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.resolve.DefaultImportsProvider


@OptIn(CaImplementationDetail::class)
internal class CaCfirDefaultImportsProvider : CaBaseDefaultImportsProvider() {
    override fun getCompilerDefaultImportsProvider(): DefaultImportsProvider = CommonDefaultImportsProvider
}



object CommonDefaultImportsProvider : DefaultImportsProvider() {
    override val platformSpecificDefaultImports: List<ImportPath> = emptyList()
}