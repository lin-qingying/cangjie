package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirImportBindingResolver
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 仓颉允许 `import pkg.sub` 后用导入短名或别名作为包限定符。
 *
 * Kotlin FIR 不允许直接导入包；这里沿用 FIR 的 import binding / scope 分层，
 * 但把仓颉官方 `PackageDecl` lookup 语义映射为 package member scope。
 */
internal data class CfirImportedPackageQualifier(
    val name: Name,
    val packageFqNames: List<FqName>,
) {
    val isAmbiguous: Boolean get() = packageFqNames.size > 1
    val packageFqName: FqName? get() = packageFqNames.singleOrNull()
}

internal fun CfirFile.resolveImportedPackageQualifier(
    name: Name,
    session: CfirSession,
): CfirImportedPackageQualifier? {
    val store = session.importBindingStoreOrNull
    val bindings = store?.getBindings(this)?.imports ?: run {
        val resolvedImports = imports.map { CfirImportBindingResolver(session).resolveImportBinding(it) }
        store?.record(this, resolvedImports)
        resolvedImports
    }
    val packageFqNames = bindings
        .asSequence()
        .filter { binding -> binding.effectiveName == name && !binding.importDirective.isAllUnder }
        .flatMap { binding ->
            binding.targets.asSequence().mapNotNull { target ->
                (target as? CfirResolvedImportTarget.Package)?.fqName
            }
        }
        .distinct()
        .toList()
    if (packageFqNames.isEmpty()) return null
    return CfirImportedPackageQualifier(name, packageFqNames)
}

internal fun CfirExpression.importedPackageQualifierNameOrNull(): Name? =
    ((this as? CfirResolvable)?.calleeReference as? CfirNamedReference)?.name

internal fun CfirExpression.importedPackageQualifierOrNull(
    file: CfirFile,
    session: CfirSession,
): CfirImportedPackageQualifier? {
    val name = importedPackageQualifierNameOrNull() ?: return null
    return file.resolveImportedPackageQualifier(name, session)
}

internal fun CfirExpression.importedPackageQualifierScopeOrNull(
    file: CfirFile,
    session: CfirSession,
): CfirPackageMemberScope? {
    val packageFqName = importedPackageQualifierOrNull(file, session)?.packageFqName ?: return null
    return CfirPackageMemberScope(packageFqName, session)
}
