package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.ImportSpec
import PackageFormat.Package
import org.cangnova.cangjie.name.Name

/**
 * 轻量级 `.cjo` 包头信息。
 *
 * 仅提取反序列化阶段所需的稳定索引信息，不在这里构建完整 AST。
 */
class CjoPackageHeader(
    /** 完整包名，例如 `std.core`。 */
    val fullPkgName: String,
    /** 模块名。 */
    val moduleName: String,
    /** 导入包列表，顺序与官方 C++ `importedFullPackageNames` 一致。 */
    val imports: List<String>,
    /** package 中所有源文件名称，用于 decompiled facade / multifile 推导。 */
    val allFiles: List<String>,
    /** 文件级 import 条目，保留 public re-export 与 alias 信息。 */
    val fileImportEntries: List<CjoImportEntry>,
    /** 包类型（Normal/Macro/Foreign/Mock）。 */
    val kind: UByte,
    /** 包访问级别。 */
    val access: UByte,
    /** 顶层类/接口/结构体/枚举/类型别名名称集合。 */
    val topLevelClassNames: Set<Name>,
    /** 顶层函数/属性/变量名称集合。 */
    val topLevelCallableNames: Set<Name>,
    /**
     * `FullId.decl` 跨包引用键到 `allDecls` 索引的映射。
     *
     * 对齐官方 C++ 读取逻辑：
     * - 优先使用 `exportId`
     * - `exportId` 为空时回落到 `identifier`
     * - 成员声明同样允许通过该表被跨包定位
     */
    val fullIdReferenceKeyToIndex: Map<String, Int>,
    /** 顶层声明名到 `allDecls` 索引列表的映射。 */
    val topLevelNameToIndices: Map<String, List<Int>>,
    /** 顶层类样式声明名到 `allDecls` 索引列表的映射。 */
    val topLevelClassifierNameToIndices: Map<String, List<Int>>,
    /**
     * 顶层 extend 在 `allDecls` 中的索引列表。
     *
     * extend 是可导出的顶层声明，但它不是通过“声明名”被外部引用的。
     * 因此二进制装载不能依赖 `topLevelNameToIndices` 这类按 identifier 建的索引，
     * 否则匿名或无稳定名称的 extend 会在反序列化入口之前被跳过。
     */
    val topLevelExtendIndices: List<Int>,
) {
    /** 面向 decompiled 文本展示的 import 片段列表，优先使用结构化 file import 信息。 */
    val decompiledImportTexts: List<String>
        get() = if (fileImportEntries.isNotEmpty()) {
            fileImportEntries.map(CjoImportEntry::renderForDecompiledText)
        } else {
            imports
        }

    companion object {
        /** 能作为顶层 classifier 名称导出的声明种类。 */
        private val CLASSIFIER_KINDS = setOf(
            DeclKind.ClassDecl,
            DeclKind.InterfaceDecl,
            DeclKind.StructDecl,
            DeclKind.EnumDecl,
            DeclKind.TypeAliasDecl,
        )

        /** 能作为顶层 callable 名称导出的声明种类。 */
        private val CALLABLE_KINDS = setOf(
            DeclKind.FuncDecl,
            DeclKind.PropDecl,
            DeclKind.VarDecl,
        )

        /**
         * 从 FlatBuffers [Package] 中提取轻量包头。
         *
         * 这里只建立包名、导入、顶层声明名称和跨包引用索引，不反序列化声明体。
         */
        fun fromPackage(pkg: Package): CjoPackageHeader {
            val fullPkgName = pkg.fullPkgName ?: ""
            val moduleName = pkg.moduleName ?: ""
            val imports = (0 until pkg.importsLength).map { pkg.imports(it) ?: "" }
            val allFiles = (0 until pkg.allFilesLength)
                .map { pkg.allFiles(it) ?: "" }
                .filter(String::isNotBlank)
            val fileImportEntries = linkedMapOf<String, CjoImportEntry>()
            for (fileIndex in 0 until pkg.allFileImportsLength) {
                val importsOfFile = pkg.allFileImports(fileIndex) ?: continue
                for (importIndex in 0 until importsOfFile.importSpecsLength) {
                    val importSpec = importsOfFile.importSpecs(importIndex) ?: continue
                    val entry = CjoImportEntry.fromImportSpec(importSpec)
                    fileImportEntries.putIfAbsent(entry.renderForDecompiledText(), entry)
                }
            }
            val classNames = mutableSetOf<Name>()
            val callableNames = mutableSetOf<Name>()
            val fullIdReferenceKeyToIndex = linkedMapOf<String, Int>()
            val nameToIndices = linkedMapOf<String, MutableList<Int>>()
            val classifierNameToIndices = linkedMapOf<String, MutableList<Int>>()
            val topLevelExtendIndices = mutableListOf<Int>()

            for (index in 0 until pkg.allDeclsLength) {
                val decl = pkg.allDecls(index) ?: continue
                val identifier = decl.identifier?.takeIf { it.isNotBlank() }
                val fullIdReferenceKey = decl.exportId?.takeIf { it.isNotBlank() } ?: identifier

                if (decl.kind != DeclKind.ExtendDecl && fullIdReferenceKey != null) {
                    fullIdReferenceKeyToIndex.putIfAbsent(fullIdReferenceKey, index)
                }

                if (!decl.isTopLevel) continue

                if (decl.kind == DeclKind.ExtendDecl) {
                    topLevelExtendIndices += index
                }

                if (identifier == null) continue

                val kind = decl.kind
                val name = Name.identifier(identifier)

                if (kind in CLASSIFIER_KINDS) {
                    classNames += name
                    classifierNameToIndices.getOrPut(identifier) { mutableListOf() }.add(index)
                } else if (kind in CALLABLE_KINDS) {
                    callableNames += name
                }

                nameToIndices.getOrPut(identifier) { mutableListOf() }.add(index)
            }

            return CjoPackageHeader(
                fullPkgName = fullPkgName,
                moduleName = moduleName,
                imports = imports,
                allFiles = allFiles,
                fileImportEntries = fileImportEntries.values.toList(),
                kind = pkg.kind,
                access = pkg.access,
                topLevelClassNames = classNames,
                topLevelCallableNames = callableNames,
                fullIdReferenceKeyToIndex = fullIdReferenceKeyToIndex,
                topLevelNameToIndices = nameToIndices,
                topLevelClassifierNameToIndices = classifierNameToIndices,
                topLevelExtendIndices = topLevelExtendIndices,
            )
        }
    }
}

/**
 * `.cjo` 文件中的单条 import 规格。
 *
 * 该结构保留前缀路径、成员名、alias 与 re-export 标志，用于 decompiled 文本和导出名称解析。
 */
data class CjoImportEntry(
    /** import 的包路径或限定名前缀。 */
    val prefixPaths: List<String>,
    /** import 的成员名；all-under import 使用 `*`。 */
    val identifier: String,
    /** `as` 后的别名，没有 alias 时为 null。 */
    val aliasName: String?,
    /** 是否为 `.*` all-under import。 */
    val isAllUnder: Boolean,
    /** 是否使用 `::` 连接首段路径。 */
    val hasDoubleColon: Boolean,
    /** 是否为 declaration import；只有 declaration import 才能参与 public re-export。 */
    val isDecl: Boolean,
    /** 是否带隐式导出标记。 */
    val withImplicitExport: Boolean,
) {
    /**
     * 仅返回“可作为逻辑导入目标”的路径主体。
     *
     * 对 all-under import，这里故意不带 `.*`，便于上层投影到
     * `ImportItemInfo(importedFqName, isAllUnder, alias)` 这类结构化表示。
     */
    fun renderImportedPath(): String {
        return buildString {
            prefixPaths.forEachIndexed { index, path ->
                append(path)
                if (isAllUnder && index == prefixPaths.lastIndex) return@forEachIndexed
                if (index == 0 && hasDoubleColon) {
                    append("::")
                } else {
                    append(".")
                }
            }
            if (!isAllUnder) {
                append(identifier)
            }
        }
    }

    /**
     * 返回用于 decompiled 文本展示的完整 import 文本片段。
     */
    fun renderForDecompiledText(): String {
        return buildString {
            append(renderImportedPath())
            if (isAllUnder) {
                append(".*")
            }
            aliasName?.takeIf(String::isNotBlank)?.let { alias ->
                append(" as ")
                append(alias)
            }
        }
    }

    companion object {
        /** 从 FlatBuffers [ImportSpec] 转换为结构化 [CjoImportEntry]。 */
        fun fromImportSpec(importSpec: ImportSpec): CjoImportEntry {
            val prefixPaths = (0 until importSpec.prefixPathsLength)
                .map { prefixIndex -> importSpec.prefixPaths(prefixIndex) ?: "" }
                .filter(String::isNotBlank)
            val identifier = importSpec.identifier ?: ""
            val aliasName = importSpec.asIdentifier?.takeIf(String::isNotBlank)
            return CjoImportEntry(
                prefixPaths = prefixPaths,
                identifier = identifier,
                aliasName = aliasName,
                isAllUnder = identifier == "*",
                hasDoubleColon = importSpec.hasDoubleColon,
                isDecl = importSpec.isDecl,
                withImplicitExport = importSpec.withImplicitExport,
            )
        }
    }
}
