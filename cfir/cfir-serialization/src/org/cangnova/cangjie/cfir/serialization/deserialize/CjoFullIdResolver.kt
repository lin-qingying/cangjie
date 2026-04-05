package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.ClassInfo
import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.EnumInfo
import PackageFormat.ExtendInfo
import PackageFormat.FullId
import PackageFormat.InterfaceInfo
import PackageFormat.Package
import PackageFormat.StructInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 官方 C++ 编译器 `ASTLoader::GetDeclFromIndex` 的 Kotlin 等价实现。
 *
 * 它负责把 `FullId` 还原成“声明目标”，而不是在类型反序列化阶段直接猜包名和声明名。
 */
internal class CjoFullIdResolver(
    private val context: CfirDeserializationContext,
) {
    private val currentPackageIndex: CjoPackageIndex = CjoPackageIndex(context.header, context.pkg)

    fun resolve(fullId: FullId): ResolvedFullId {
        return when (val reservedIndex = PackageIndex.fromValue(fullId.pkgId)) {
            PackageIndex.INVALID -> ResolvedFullId.Invalid(
                rawPkgId = fullId.pkgId,
                message = "FullId points to INVALID package index",
            )

            PackageIndex.CURRENT -> {
                val resolved = currentPackageIndex.resolveDeclByFormattedIndex(fullId.index)
                if (resolved == null) {
                    ResolvedFullId.Invalid(
                        rawPkgId = fullId.pkgId,
                        message = "Cannot resolve current-package decl index ${fullId.index}",
                    )
                } else {
                    ResolvedFullId.Declaration(
                        packageIndex = currentPackageIndex,
                        declaration = resolved,
                        source = ResolvedFullId.Declaration.Source.CURRENT_PACKAGE,
                    )
                }
            }

            PackageIndex.PACKAGE_REFERENCE -> {
                val packageName = fullId.decl?.takeIf { it.isNotBlank() }
                if (packageName == null) {
                    ResolvedFullId.Invalid(
                        rawPkgId = fullId.pkgId,
                        message = "Package reference FullId is missing package name",
                    )
                } else {
                    ResolvedFullId.PackageReference(FqName(packageName))
                }
            }

            null -> {
                if (!PackageIndex.isImportedPackage(fullId.pkgId)) {
                    return ResolvedFullId.Invalid(
                        rawPkgId = fullId.pkgId,
                        message = "Unsupported package index ${fullId.pkgId}",
                    )
                }

                val packageIndex = loadImportedPackageIndex(fullId.pkgId)
                    ?: return ResolvedFullId.Invalid(
                        rawPkgId = fullId.pkgId,
                        message = "Cannot load imported package at index ${fullId.pkgId}",
                    )

                val referenceKey = fullId.decl?.takeIf { it.isNotBlank() }
                    ?: return ResolvedFullId.Invalid(
                        rawPkgId = fullId.pkgId,
                        message = "Imported-package FullId is missing reference key",
                    )

                val resolved = packageIndex.resolveDeclByReferenceKey(referenceKey)
                    ?: return ResolvedFullId.Invalid(
                        rawPkgId = fullId.pkgId,
                        message = "Cannot resolve imported reference '$referenceKey' in ${packageIndex.packageFqName.asString()}",
                    )

                ResolvedFullId.Declaration(
                    packageIndex = packageIndex,
                    declaration = resolved,
                    source = ResolvedFullId.Declaration.Source.IMPORTED_PACKAGE,
                )
            }
        }
    }

    fun resolveClassId(fullId: FullId): ClassId? {
        val resolved = resolve(fullId) as? ResolvedFullId.Declaration ?: return null
        return resolved.packageIndex.resolveClassId(resolved.declaration.zeroBasedIndex)
    }

    fun resolveDeclarationName(fullId: FullId): Name? {
        val resolved = resolve(fullId) as? ResolvedFullId.Declaration ?: return null
        return resolved.declaration.name
    }

    fun describe(fullId: FullId): String {
        return when (val resolved = resolve(fullId)) {
            is ResolvedFullId.Invalid -> "pkgId=${resolved.rawPkgId} (${resolved.message})"
            is ResolvedFullId.PackageReference -> "package=${resolved.packageFqName.asString()}"
            is ResolvedFullId.Declaration ->
                "pkg=${resolved.packageIndex.packageFqName.asString()}, decl=${resolved.declaration.name.asString()}, index=${resolved.declaration.formattedIndex}"
        }
    }

    private fun loadImportedPackageIndex(importIndex: Int): CjoPackageIndex? {
        context.importedPackageIndices[importIndex]?.let { return it }

        val fullPackageName = context.header.imports.getOrNull(importIndex)?.takeIf { it.isNotBlank() } ?: return null
        val header = context.cjoManager.loadPackageHeader(fullPackageName) ?: return null
        val pkg = context.cjoManager.loadPackage(fullPackageName) ?: return null
        val created = CjoPackageIndex(header, pkg)
        context.importedPackageIndices.putIfAbsent(importIndex, created)
        return context.importedPackageIndices[importIndex] ?: created
    }
}

/**
 * 单个包的声明索引视图。
 *
 * 它把 FlatBuffers 中的“格式化索引”和“跨包引用键”统一映射为真实声明，
 * 同时构建父子声明关系以恢复嵌套 `ClassId`。
 */
internal class CjoPackageIndex(
    private val header: CjoPackageHeader,
    private val pkg: Package,
) {
    val packageFqName: FqName = FqName(header.fullPkgName)

    private val parentDeclByIndex: Map<Int, Int> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildParentDeclIndex()
    }

    fun resolveDeclByFormattedIndex(formattedDeclIndex: UInt): IndexedDeclaration? {
        val zeroBasedIndex = decodeFormattedDeclIndex(formattedDeclIndex) ?: return null
        return resolveDeclByZeroBasedIndex(zeroBasedIndex)
    }

    fun resolveDeclByReferenceKey(referenceKey: String): IndexedDeclaration? {
        val zeroBasedIndex = header.fullIdReferenceKeyToIndex[referenceKey] ?: return null
        return resolveDeclByZeroBasedIndex(zeroBasedIndex)
    }

    fun resolveClassId(zeroBasedDeclIndex: Int): ClassId? {
        val relativeClassName = buildRelativeClassName(zeroBasedDeclIndex) ?: return null
        return ClassId(packageFqName, relativeClassName)
    }

    private fun resolveDeclByZeroBasedIndex(zeroBasedIndex: Int): IndexedDeclaration? {
        val decl = pkg.allDecls(zeroBasedIndex) ?: return null
        val identifier = decl.identifier?.takeIf { it.isNotBlank() } ?: return null
        return IndexedDeclaration(
            zeroBasedIndex = zeroBasedIndex,
            formattedIndex = (zeroBasedIndex + 1).toUInt(),
            decl = decl,
            name = Name.identifier(identifier),
        )
    }

    private fun buildRelativeClassName(zeroBasedDeclIndex: Int): FqName? {
        val segments = mutableListOf<Name>()
        var currentIndex: Int? = zeroBasedDeclIndex

        while (currentIndex != null) {
            val declaration = resolveDeclByZeroBasedIndex(currentIndex) ?: return null
            if (!declaration.decl.isClassLikeDeclaration()) return null

            segments += declaration.name
            currentIndex = parentDeclByIndex[currentIndex]
        }

        if (segments.isEmpty()) return null
        segments.reverse()

        var relativeClassName = FqName.topLevel(segments.first())
        for (segment in segments.drop(1)) {
            relativeClassName = relativeClassName.child(segment)
        }
        return relativeClassName
    }

    private fun buildParentDeclIndex(): Map<Int, Int> {
        val parents = mutableMapOf<Int, Int>()
        for (parentIndex in 0 until pkg.allDeclsLength) {
            val parentDecl = pkg.allDecls(parentIndex) ?: continue
            for (childIndex in parentDecl.childDeclIndices()) {
                parents.putIfAbsent(childIndex, parentIndex)
            }
        }
        return parents
    }

    private fun decodeFormattedDeclIndex(formattedDeclIndex: UInt): Int? {
        if (formattedDeclIndex == 0u || formattedDeclIndex == UInt.MAX_VALUE) return null
        val zeroBasedIndex = formattedDeclIndex.toInt() - 1
        return zeroBasedIndex.takeIf { it in 0 until pkg.allDeclsLength }
    }

    private fun Decl.childDeclIndices(): List<Int> {
        return when (kind) {
            DeclKind.ClassDecl -> (info(ClassInfo()) as? ClassInfo)?.bodyIndices().orEmpty()
            DeclKind.InterfaceDecl -> (info(InterfaceInfo()) as? InterfaceInfo)?.bodyIndices().orEmpty()
            DeclKind.StructDecl -> (info(StructInfo()) as? StructInfo)?.bodyIndices().orEmpty()
            DeclKind.EnumDecl -> (info(EnumInfo()) as? EnumInfo)?.bodyIndices().orEmpty()
            DeclKind.ExtendDecl -> (info(ExtendInfo()) as? ExtendInfo)?.bodyIndices().orEmpty()
            else -> emptyList()
        }
    }

    private fun Decl.isClassLikeDeclaration(): Boolean {
        return kind == DeclKind.ClassDecl ||
            kind == DeclKind.InterfaceDecl ||
            kind == DeclKind.StructDecl ||
            kind == DeclKind.EnumDecl ||
            kind == DeclKind.TypeAliasDecl
    }

    private fun ClassInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    private fun InterfaceInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    private fun StructInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    private fun EnumInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    private fun ExtendInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    private fun decodeBodyVector(length: Int, getter: (Int) -> UInt): List<Int> {
        if (length == 0) return emptyList()
        return buildList(length) {
            for (i in 0 until length) {
                val zeroBasedIndex = decodeFormattedDeclIndex(getter(i)) ?: continue
                add(zeroBasedIndex)
            }
        }
    }
}

internal sealed interface ResolvedFullId {
    data class Invalid(
        val rawPkgId: Int,
        val message: String,
    ) : ResolvedFullId

    data class PackageReference(
        val packageFqName: FqName,
    ) : ResolvedFullId

    data class Declaration(
        val packageIndex: CjoPackageIndex,
        val declaration: IndexedDeclaration,
        val source: Source,
    ) : ResolvedFullId {
        enum class Source {
            CURRENT_PACKAGE,
            IMPORTED_PACKAGE,
        }
    }
}

internal data class IndexedDeclaration(
    val zeroBasedIndex: Int,
    val formattedIndex: UInt,
    val decl: Decl,
    val name: Name,
)
