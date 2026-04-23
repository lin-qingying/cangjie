package org.cangnova.cangjie.analysis.api.impl.base.import

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaIdeApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImport
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImportPriority
@OptIn(CaIdeApi::class)
@CaImplementationDetail
class CaDefaultImportImpl(
    override val importPath: ImportPath,
    override val priority: CaDefaultImportPriority,
) : CaDefaultImport {


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CaDefaultImport
                && other.importPath == importPath
                && other.priority == priority
    }

    override fun hashCode(): Int {
        var result = importPath.hashCode()
        result = 31 * result + priority.hashCode()
        return result
    }
}
