package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvedQualifier
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.resolve.services.requireImportBindings
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 仓颉允许 `import pkg.sub` 后用导入短名或别名作为包限定符。
 *
 * Kotlin FIR 不允许直接导入包；这里沿用 FIR 的 import binding / scope 分层，
 * 但把仓颉官方 `PackageDecl` lookup 语义映射为 package member scope。
 */
internal sealed interface CfirImportedPackageQualifier {
    /** 源码中作为包限定符使用的短名或导入别名。 */
    val name: Name

    /** 成功解析出的包目标；只有 [UniquePackage] 可作为静态成员作用域。 */
    val packages: List<CfirResolvedImportTarget.Package>

    /** 唯一解析成功的包限定名；歧义或未解析时为空。 */
    val packageFqName: FqName?
        get() = (this as? UniquePackage)?.target?.fqName

    /** 唯一的导入包结果。 */
    data class UniquePackage(
        override val name: Name,
        val target: CfirResolvedImportTarget.Package,
    ) : CfirImportedPackageQualifier {
        override val packages: List<CfirResolvedImportTarget.Package> = listOf(target)
    }

    /** 同一短名或别名命中多个包的歧义结果。 */
    data class AmbiguousPackages(
        override val name: Name,
        override val packages: List<CfirResolvedImportTarget.Package>,
    ) : CfirImportedPackageQualifier

    /** 短名或别名只对应未解析导入的结果。 */
    data class UnresolvedImport(
        override val name: Name,
    ) : CfirImportedPackageQualifier {
        override val packages: List<CfirResolvedImportTarget.Package> = emptyList()
    }
}

/**
 * 在文件导入表中解析可作为包限定符使用的短名。
 */
internal fun CfirFile.resolveImportedPackageQualifier(
    name: Name,
    session: CfirSession,
): CfirImportedPackageQualifier? {
    val bindings = requireImportBindings().imports
    val matchingBindings = bindings
        .asSequence()
        .filter { binding -> binding.effectiveName == name && !binding.importDirective.isAllUnder }
        .toList()
    val packages = matchingBindings
        .asSequence()
        .flatMap { binding ->
            binding.targets.asSequence().mapNotNull { target ->
                target as? CfirResolvedImportTarget.Package
            }
        }
        .distinct()
        .toList()
    val hasUnresolvedImport = matchingBindings.any { binding -> binding.targets.isEmpty() }
    return when {
        packages.size > 1 -> CfirImportedPackageQualifier.AmbiguousPackages(name, packages)
        packages.singleOrNull() != null -> CfirImportedPackageQualifier.UniquePackage(name, packages.single())
        hasUnresolvedImport -> CfirImportedPackageQualifier.UnresolvedImport(name)
        else -> null
    }
}

/**
 * 判断源码首段是否已经被非 all-under 的显式导入绑定。
 *
 * 裸 FQN 解析只在没有任何此类绑定时启用；包、classifier、callable 和未解析 binding
 * 都会占用该首段，避免 FQN 回退越过现有导入语义。
 */
internal fun CfirFile.hasExplicitSimpleImportBinding(
    name: Name,
    session: CfirSession,
): Boolean = requireImportBindings().imports.any { binding ->
    binding.effectiveName == name && !binding.importDirective.isAllUnder
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
    if (this is CfirResolvedQualifier) return if (qualifierSymbol == null)
        CfirImportedPackageQualifier.UniquePackage(referenceName, CfirResolvedImportTarget.Package(packageFqName, organizationName)) else null
    val name = importedPackageQualifierNameOrNull() ?: return null
    return file.resolveImportedPackageQualifier(name, session)
}

/** 为导入包名或别名创建静态 package member scope。 */
internal fun CfirExpression.importedPackageQualifierScopeOrNull(
    file: CfirFile,
    session: CfirSession,
): CfirPackageMemberScope? {
    val target = (importedPackageQualifierOrNull(file, session) as? CfirImportedPackageQualifier.UniquePackage)
        ?.target ?: return null
    return CfirPackageMemberScope(target.fqName, session, organizationName = target.organizationName)
}
