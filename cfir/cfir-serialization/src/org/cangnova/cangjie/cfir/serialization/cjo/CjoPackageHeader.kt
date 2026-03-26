package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.Package
import org.cangnova.cangjie.name.Name

/**
 * 轻量级 .cjo 包头信息。
 *
 * 从 FlatBuffers Package 表中提取关键元数据，不加载声明体。
 * 用于快速判断包中是否存在某个顶级名称。
 */
class CjoPackageHeader(
    /** 完整包名，如 "std.core" */
    val fullPkgName: String,
    /** 模块名 */
    val moduleName: String,
    /** 导入的包列表 */
    val imports: List<String>,
    /** 包类型（Normal/Macro/Foreign/Mock） */
    val kind: UByte,
    /** 包访问级别 */
    val access: UByte,
    /** 顶级类/接口/结构体/枚举名称集合 */
    val topLevelClassNames: Set<Name>,
    /** 顶级函数/属性/变量名称集合 */
    val topLevelCallableNames: Set<Name>,
    /** exportId → allDecls 索引映射 */
    val exportIdToIndex: Map<String, Int>,
    /** 顶级声明的 identifier → allDecls 索引列表映射（同名可能有多个重载） */
    val topLevelNameToIndices: Map<String, List<Int>>,
    /** 顶级类/接口/结构体/枚举/类型别名 的 identifier → allDecls 索引列表映射 */
    val topLevelClassifierNameToIndices: Map<String, List<Int>>,
) {
    companion object {
        /** 类类型的 DeclKind 集合 */
        private val CLASSIFIER_KINDS = setOf(
            DeclKind.ClassDecl, DeclKind.InterfaceDecl,
            DeclKind.StructDecl, DeclKind.EnumDecl,
            DeclKind.TypeAliasDecl,
        )

        /** 可调用类型的 DeclKind 集合 */
        private val CALLABLE_KINDS = setOf(
            DeclKind.FuncDecl, DeclKind.PropDecl, DeclKind.VarDecl,
        )

        /**
         * 从 FlatBuffers Package 对象构建包头。
         */
        fun fromPackage(pkg: Package): CjoPackageHeader {
            val fullPkgName = pkg.fullPkgName ?: ""
            val moduleName = pkg.moduleName ?: ""
            val imports = (0 until pkg.importsLength).map { pkg.imports(it) ?: "" }
            val classNames = mutableSetOf<Name>()
            val callableNames = mutableSetOf<Name>()
            val exportIdMap = mutableMapOf<String, Int>()
            val nameToIndices = mutableMapOf<String, MutableList<Int>>()
            val classifierNameToIndices = mutableMapOf<String, MutableList<Int>>()

            for (i in 0 until pkg.allDeclsLength) {
                val decl = pkg.allDecls(i) ?: continue
                if (!decl.isTopLevel) continue

                val identifier = decl.identifier ?: continue
                val kind = decl.kind
                val name = Name.identifier(identifier)

                // 按声明类型分类
                if (kind in CLASSIFIER_KINDS) {
                    classNames.add(name)
                    classifierNameToIndices.getOrPut(identifier) { mutableListOf() }.add(i)
                } else if (kind in CALLABLE_KINDS) {
                    callableNames.add(name)
                }

                // 构建 exportId 映射
                val exportId = decl.exportId
                if (exportId != null) {
                    exportIdMap[exportId] = i
                }

                // 构建名称 → 索引映射
                nameToIndices.getOrPut(identifier) { mutableListOf() }.add(i)
            }

            return CjoPackageHeader(
                fullPkgName = fullPkgName,
                moduleName = moduleName,
                imports = imports,
                kind = pkg.kind,
                access = pkg.access,
                topLevelClassNames = classNames,
                topLevelCallableNames = callableNames,
                exportIdToIndex = exportIdMap,
                topLevelNameToIndices = nameToIndices,
                topLevelClassifierNameToIndices = classifierNameToIndices,
            )
        }
    }
}
