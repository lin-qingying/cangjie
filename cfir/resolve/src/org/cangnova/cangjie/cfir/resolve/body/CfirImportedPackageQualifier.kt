package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.importBindingStore
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 仓颉允许 `import pkg.sub` 后用导入短名或别名作为包限定符。
 *
 * Kotlin FIR 不允许直接导入包；这里沿用 FIR 的 import binding / scope 分层，
 * 但把仓颉官方 `PackageDecl` lookup 语义映射为 package member scope。
 */
internal data class CfirImportedPackageQualifier(
    /**
     * 源码中作为包限定符使用的短名或导入别名。
     */
    val name: Name,
    /**
     * 该短名可解析到的包限定名集合。
     */
    val packageFqNames: List<FqName>,
    /**
     * 是否存在同名但未成功解析目标的导入。
     */
    val hasUnresolvedImport: Boolean = false,
) {
    /**
     * 当前包限定符是否同时命中多个包。
     */
    val isAmbiguous: Boolean get() = packageFqNames.size > 1
    /**
     * 当前包限定符是否只来自未解析导入。
     */
    val isUnresolved: Boolean get() = packageFqNames.isEmpty() && hasUnresolvedImport
    /**
     * 唯一解析成功的包限定名；歧义或未解析时为空。
     */
    val packageFqName: FqName? get() = packageFqNames.singleOrNull()
}

/**
 * 在文件导入表中解析可作为包限定符使用的短名。
 */
internal fun CfirFile.resolveImportedPackageQualifier(
    name: Name,
    session: CfirSession,
): CfirImportedPackageQualifier? {
    val bindings = session.importBindingStore.requireBindings(this).imports
    val matchingBindings = bindings
        .asSequence()
        .filter { binding -> binding.effectiveName == name && !binding.importDirective.isAllUnder }
        .toList()
    val packageFqNames = matchingBindings
        .asSequence()
        .flatMap { binding ->
            binding.targets.asSequence().mapNotNull { target ->
                (target as? CfirResolvedImportTarget.Package)?.fqName
            }
        }
        .distinct()
        .toList()
    val hasUnresolvedImport = matchingBindings.any { binding -> binding.targets.isEmpty() }
    if (packageFqNames.isEmpty() && !hasUnresolvedImport) return null
    return CfirImportedPackageQualifier(
        name = name,
        packageFqNames = packageFqNames,
        hasUnresolvedImport = hasUnresolvedImport,
    )
}

/**
 * 从表达式的可解析引用中提取可能的导入包限定符短名。
 */
internal fun CfirExpression.importedPackageQualifierNameOrNull(): Name? =
    ((this as? CfirResolvable)?.calleeReference as? CfirNamedReference)?.name

/**
 * 将表达式解析为导入包限定符信息。
 */
internal fun CfirExpression.importedPackageQualifierOrNull(
    file: CfirFile,
    session: CfirSession,
): CfirImportedPackageQualifier? {
    val name = importedPackageQualifierNameOrNull() ?: return null
    return file.resolveImportedPackageQualifier(name, session)
}

/**
 * 为表达式命中的导入包限定符创建包成员作用域。
 */
internal fun CfirExpression.importedPackageQualifierScopeOrNull(
    file: CfirFile,
    session: CfirSession,
): CfirPackageMemberScope? {
    val packageFqName = importedPackageQualifierOrNull(file, session)?.packageFqName ?: return null
    return CfirPackageMemberScope(packageFqName, session)
}
