package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.name.FqName

/**
 * 仓颉的 `internal` 不是 Kotlin 式的 module-internal，
 * 而是“声明所在包 + 该包的所有子包”可见。
 *
 * 这里抽成共享 helper，是为了让 resolve、checker、type-accessibility
 * 都复用同一条语义规则，避免不同层各自复制一套判断后再次漂移。
 */
fun canAccessPackageInternalDeclaration(
    useSitePackage: FqName,
    declarationPackage: FqName,
): Boolean {
    return useSitePackage == declarationPackage || useSitePackage.isSubpackageOf(declarationPackage)
}

/**
 * 仓颉 `protected` 的包级可见性对应官方 `Modules::PackageRelation != NONE`。
 *
 * 同包、子包、父包以及首段 module 相同的包可见；root 包与非 root 包没有
 * 共同 module，不能互相访问 protected 声明。
 */
fun canAccessPackageProtectedDeclaration(
    useSitePackage: FqName,
    declarationPackage: FqName,
): Boolean {
    if (useSitePackage == declarationPackage) return true
    if (useSitePackage.isRoot || declarationPackage.isRoot) return false
    return useSitePackage.pathSegments().firstOrNull() == declarationPackage.pathSegments().firstOrNull()
}

/**
 * 判断当前包是否是目标包本身，或者位于其子包树中。
 */
fun FqName.isSubpackageOf(parent: FqName): Boolean {
    if (this == parent) return true
    if (parent.isRoot) return true

    val parentSegments = parent.pathSegments()
    val currentSegments = pathSegments()
    if (currentSegments.size < parentSegments.size) return false

    return currentSegments.take(parentSegments.size) == parentSegments
}
