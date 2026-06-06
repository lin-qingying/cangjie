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
    private val canonicalBindingsByImport = mutableMapOf<ImportSignature, CfirResolvedImportBinding>()

    fun record(file: CfirFile, imports: List<CfirResolvedImportBinding>) {
        bindingsByFile[file] = CfirFileImportBindings(file = file, imports = imports)
        imports.forEach(::recordCanonicalBinding)
    }

    fun getBindings(file: CfirFile): CfirFileImportBindings? {
        val bindings = bindingsByFile[file] ?: return null
        val normalizedImports = bindings.imports.map(::withCanonicalTargets)
        return if (normalizedImports === bindings.imports || normalizedImports == bindings.imports) {
            bindings
        } else {
            CfirFileImportBindings(file = bindings.file, imports = normalizedImports)
        }
    }

    /**
     * Lazy resolve 可能在 dangling/original CFIR 副本之间以不同顺序触发 IMPORTS。
     * 同一 session 内相同 import 指令的目标应保持一致，这里保存最完整的已知绑定，
     * 供同一语义 import 的其他 CFIR 文件副本读取。
     */
    private fun recordCanonicalBinding(binding: CfirResolvedImportBinding) {
        val signature = binding.importDirective.signature()
        val current = canonicalBindingsByImport[signature]
        if (current == null || binding.targets.isMoreCompleteThan(current.targets)) {
            canonicalBindingsByImport[signature] = binding
        }
    }

    private fun withCanonicalTargets(binding: CfirResolvedImportBinding): CfirResolvedImportBinding {
        val canonical = canonicalBindingsByImport[binding.importDirective.signature()] ?: return binding
        val targets = binding.targets.mergeWith(canonical.targets)
        return if (targets == binding.targets) binding else binding.copy(targets = targets)
    }

    private fun List<CfirResolvedImportTarget>.isMoreCompleteThan(other: List<CfirResolvedImportTarget>): Boolean {
        val nonPackageTargets = count { it !is CfirResolvedImportTarget.Package }
        val otherNonPackageTargets = other.count { it !is CfirResolvedImportTarget.Package }
        return nonPackageTargets > otherNonPackageTargets ||
            nonPackageTargets == otherNonPackageTargets && size > other.size
    }

    private fun List<CfirResolvedImportTarget>.mergeWith(other: List<CfirResolvedImportTarget>): List<CfirResolvedImportTarget> {
        if (other.isEmpty()) return this
        if (isEmpty()) return other

        val result = toMutableList()
        for (target in other) {
            if (target !in result) {
                result += target
            }
        }
        return result
    }

    private fun CfirImport.signature(): ImportSignature = ImportSignature(
        importedFqName = importedFqName?.asString(),
        isAllUnder = isAllUnder,
        aliasName = aliasName?.asString(),
    )

    private data class ImportSignature(
        val importedFqName: String?,
        val isAllUnder: Boolean,
        val aliasName: String?,
    )
}
