package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

sealed interface CfirResolvedImportTarget {
    data class Package(
        val fqName: FqName,
    ) : CfirResolvedImportTarget

    data class ClassLike(
        val classId: ClassId,
        val symbol: CfirClassLikeSymbol<*>,
    ) : CfirResolvedImportTarget

    data class Callable(
        val packageFqName: FqName,
        val name: Name,
        val symbols: List<CfirCallableSymbol<*>>,
    ) : CfirResolvedImportTarget
}

data class CfirResolvedImportBinding(
    val importDirective: CfirImport,
    val effectiveName: Name,
    val targets: List<CfirResolvedImportTarget>,
)

data class CfirFileImportBindings(
    val file: CfirFile,
    val imports: List<CfirResolvedImportBinding>,
)

class CfirImportBindingStore : CfirSessionComponent {
    private val bindingsByFile = mutableMapOf<CfirFile, CfirFileImportBindings>()

    fun record(file: CfirFile, imports: List<CfirResolvedImportBinding>) {
        bindingsByFile[file] = CfirFileImportBindings(file = file, imports = imports)
    }

    fun getBindings(file: CfirFile): CfirFileImportBindings? = bindingsByFile[file]
}
