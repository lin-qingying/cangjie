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
        imports.firstOrNull { it.importDirective.importedFqName?.asString() == "untitled89.b.a11" }?.let { binding ->
            System.err.println(
                "DEBUG_A11 storeRecord file=${file.name} path=${file.sourceFile?.path} " +
                    "fileId=${System.identityHashCode(file)} importId=${System.identityHashCode(binding.importDirective)} " +
                    "targets=${binding.targets.joinToString { it::class.simpleName ?: it::class.java.name }}"
            )
        }
        bindingsByFile[file] = CfirFileImportBindings(file = file, imports = imports)
    }

    fun getBindings(file: CfirFile): CfirFileImportBindings? {
        val result = bindingsByFile[file]
        result?.imports
            ?.firstOrNull { it.importDirective.importedFqName?.asString() == "untitled89.b.a11" }
            ?.let { binding ->
                System.err.println(
                    "DEBUG_A11 storeGet file=${file.name} path=${file.sourceFile?.path} " +
                        "fileId=${System.identityHashCode(file)} importId=${System.identityHashCode(binding.importDirective)} " +
                        "targets=${binding.targets.joinToString { it::class.simpleName ?: it::class.java.name }}"
                )
            }
        return result
    }
}
