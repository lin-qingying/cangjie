package org.cangnova.cangjie.analysis.api.impl.base.import

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImport
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports

@CaImplementationDetail
class CaDefaultImportsImpl(
    override val defaultImports: List<CaDefaultImport>,
    override val excludedFromDefaultImports: List<ImportPath>
) : CaDefaultImports {
    override fun equals(other: Any?): Boolean {
        return this === other
                || other is CaDefaultImports
                && other.defaultImports == defaultImports
                && other.excludedFromDefaultImports == excludedFromDefaultImports
    }

    override fun hashCode(): Int {
        var result = defaultImports.hashCode()
        result = 31 * result + excludedFromDefaultImports.hashCode()
        return result
    }
}