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
    /** 当前包的反序列化上下文。 */
    private val context: CfirDeserializationContext,
) {
    /** 当前包声明索引视图，用于处理 `pkgId = CURRENT` 的 FullId。 */
    private val currentPackageIndex: CjoPackageIndex = CjoPackageIndex(context.header, context.pkg)

    /**
     * 将 FlatBuffers [FullId] 解析为包引用或具体声明引用。
     *
     * 该方法完整处理保留 `pkgId`、当前包索引和导入包引用键，失败时返回结构化 invalid 结果。
     */
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

    /** 将 [FullId] 解析为 class-like 声明的 [ClassId]；非 class-like 或非法引用返回 null。 */
    fun resolveClassId(fullId: FullId): ClassId? {
        val resolved = resolve(fullId) as? ResolvedFullId.Declaration ?: return null
        return resolved.packageIndex.resolveClassId(resolved.declaration.zeroBasedIndex)
    }

    /** 将 [FullId] 解析为声明名称；非法或非声明引用返回 null。 */
    fun resolveDeclarationName(fullId: FullId): Name? {
        val resolved = resolve(fullId) as? ResolvedFullId.Declaration ?: return null
        return resolved.declaration.name
    }

    /** 生成 [FullId] 解析结果的调试文本，用于错误类型与诊断原因。 */
    fun describe(fullId: FullId): String {
        return when (val resolved = resolve(fullId)) {
            is ResolvedFullId.Invalid -> "pkgId=${resolved.rawPkgId} (${resolved.message})"
            is ResolvedFullId.PackageReference -> "package=${resolved.packageFqName.asString()}"
            is ResolvedFullId.Declaration ->
                "pkg=${resolved.packageIndex.packageFqName.asString()}, decl=${resolved.declaration.name.asString()}, index=${resolved.declaration.formattedIndex}"
        }
    }

    /**
     * 按导入包数组下标加载并缓存 [CjoPackageIndex]。
     *
     * 下标来自 `FullId.pkgId >= 0`，包名由当前包头的 imports 表提供。
     */
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
    /** 当前包头的轻量索引信息。 */
    private val header: CjoPackageHeader,
    /** 当前包的 FlatBuffers Package。 */
    private val pkg: Package,
) {
    /** 当前包的完整包名。 */
    val packageFqName: FqName = FqName(header.fullPkgName)

    /** 子声明 `allDecls` 下标到父声明下标的映射，延迟构建以避免无 classId 查询时的开销。 */
    private val parentDeclByIndex: Map<Int, Int> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildParentDeclIndex()
    }

    /** 通过官方 1-based 格式化声明索引解析声明。 */
    fun resolveDeclByFormattedIndex(formattedDeclIndex: UInt): IndexedDeclaration? {
        val zeroBasedIndex = decodeFormattedDeclIndex(formattedDeclIndex) ?: return null
        return resolveDeclByZeroBasedIndex(zeroBasedIndex)
    }

    /** 通过跨包引用键解析声明。 */
    fun resolveDeclByReferenceKey(referenceKey: String): IndexedDeclaration? {
        val zeroBasedIndex = header.fullIdReferenceKeyToIndex[referenceKey] ?: return null
        return resolveDeclByZeroBasedIndex(zeroBasedIndex)
    }

    /** 通过 `allDecls` 下标解析公开 class-like 声明的 [ClassId]。 */
    fun resolveClassId(zeroBasedDeclIndex: Int): ClassId? {
        val relativeClassName = buildRelativeClassName(zeroBasedDeclIndex) ?: return null
        return ClassId(packageFqName, relativeClassName)
    }

    /** 通过 0-based `allDecls` 下标读取声明并包装为索引视图。 */
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

    /**
     * 构造 class-like 声明在包内的相对名称。
     *
     * 仓颉当前不把嵌套 class-like 暴露为公开 [ClassId]，存在父声明时直接返回 null。
     */
    private fun buildRelativeClassName(zeroBasedDeclIndex: Int): FqName? {
        /**
         * 仓颉当前不支持嵌套 class-like 声明进入公开 `ClassId` 体系。
         *
         * 因此这里必须显式拒绝存在 parent class-like 链的声明，
         * 不能像 Kotlin/JVM 元数据那样拼出 `Outer.Inner`。
         */
        if (parentDeclByIndex.containsKey(zeroBasedDeclIndex)) return null

        val declaration = resolveDeclByZeroBasedIndex(zeroBasedDeclIndex) ?: return null
        if (!declaration.decl.isClassLikeDeclaration()) return null
        return FqName.topLevel(declaration.name)
    }

    /** 遍历所有声明 body，建立子声明到父声明的反向索引。 */
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

    /** 将官方 1-based 格式化声明索引解码为 0-based `allDecls` 下标。 */
    private fun decodeFormattedDeclIndex(formattedDeclIndex: UInt): Int? {
        if (formattedDeclIndex == 0u || formattedDeclIndex == UInt.MAX_VALUE) return null
        val zeroBasedIndex = formattedDeclIndex.toInt() - 1
        return zeroBasedIndex.takeIf { it in 0 until pkg.allDeclsLength }
    }

    /** 提取声明 body 中记录的子声明下标列表。 */
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

    /** 判断声明种类是否属于可形成 [ClassId] 的 class-like 声明。 */
    private fun Decl.isClassLikeDeclaration(): Boolean {
        return kind == DeclKind.ClassDecl ||
            kind == DeclKind.InterfaceDecl ||
            kind == DeclKind.StructDecl ||
            kind == DeclKind.EnumDecl ||
            kind == DeclKind.TypeAliasDecl
    }

    /** 解码 class 声明 body 中的子声明索引。 */
    private fun ClassInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    /** 解码 interface 声明 body 中的子声明索引。 */
    private fun InterfaceInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    /** 解码 struct 声明 body 中的子声明索引。 */
    private fun StructInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    /** 解码 enum 声明 body 中的子声明索引。 */
    private fun EnumInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    /** 解码 extend 声明 body 中的子声明索引。 */
    private fun ExtendInfo.bodyIndices(): List<Int> = decodeBodyVector(bodyLength, ::body)

    /** 解码 FlatBuffers body vector 中的 1-based 声明索引列表。 */
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

/** [FullId] 解析后的结构化结果。 */
internal sealed interface ResolvedFullId {
    /** 无法解析的 FullId，携带原始 pkgId 与失败说明。 */
    data class Invalid(
        /** 原始 `FullId.pkgId`。 */
        val rawPkgId: Int,
        /** 解析失败原因。 */
        val message: String,
    ) : ResolvedFullId

    /** 仅指向包本身的 FullId 结果。 */
    data class PackageReference(
        /** 被引用包的完整包名。 */
        val packageFqName: FqName,
    ) : ResolvedFullId

    /** 指向具体声明的 FullId 结果。 */
    data class Declaration(
        /** 声明所在包的索引视图。 */
        val packageIndex: CjoPackageIndex,
        /** 被解析出的声明索引信息。 */
        val declaration: IndexedDeclaration,
        /** 声明来自当前包还是导入包。 */
        val source: Source,
    ) : ResolvedFullId {
        /** 声明引用来源。 */
        enum class Source {
            /** 当前正在反序列化的包。 */
            CURRENT_PACKAGE,
            /** 当前包导入列表中的其他包。 */
            IMPORTED_PACKAGE,
        }
    }
}

/** 单个 `allDecls` 项的索引视图。 */
internal data class IndexedDeclaration(
    /** 0-based `allDecls` 下标。 */
    val zeroBasedIndex: Int,
    /** 官方二进制中使用的 1-based 格式化索引。 */
    val formattedIndex: UInt,
    /** 原始 FlatBuffers 声明对象。 */
    val decl: Decl,
    /** 声明名。 */
    val name: Name,
)
