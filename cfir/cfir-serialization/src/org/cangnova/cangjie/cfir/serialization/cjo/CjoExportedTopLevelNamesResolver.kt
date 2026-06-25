package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * 导出顶层成员最终落到的真实声明目标。
 *
 * 对 `public import` / alias 重导出，visible name 与真实声明可能不是同一个 fqName。
 */
data class CjoExportedTopLevelTarget(
    /** 真实声明所在包名。 */
    val packageFqName: FqName,
    /** 真实声明名称。 */
    val name: Name,
)

/**
 * `.cjo` 包导出视图。
 *
 * `public import` 在二进制里不会被折叠进 `topLevelNameToIndices`，因此：
 * 1. 包头原始顶层声明名只覆盖“物理声明”；
 * 2. 通过 `public import` 重新导出的 callable / classifier 需要在读取阶段补齐。
 */
data class CjoExportedTopLevelNames(
    /** 对外可见的顶层 callable 名称集合。 */
    val callableNames: Set<Name>,
    /** 对外可见的顶层 classifier 名称集合。 */
    val classifierNames: Set<Name>,
    /** callable 可见名称到真实声明目标的映射。 */
    val callableTargets: Map<Name, CjoExportedTopLevelTarget> = emptyMap(),
    /** classifier 可见名称到真实声明目标的映射。 */
    val classifierTargets: Map<Name, CjoExportedTopLevelTarget> = emptyMap(),
)

/**
 * 递归解析 `.cjo` 包的导出顶层名称。
 *
 * - 保留包内物理顶层声明；
 * - 递归跟随 `public import` / `public import *`；
 * - 对 alias 使用导出后的名字，对非 alias 保持原名字。
 */
class CjoExportedTopLevelNamesResolver(
    /** 负责按包名加载 `.cjo` 包头的管理器。 */
    private val cjoManager: CjoManager,
) {
    /** 以完整包名为 key 的导出名称解析缓存，避免 public import 图重复遍历。 */
    private val cache = ConcurrentHashMap<String, CjoExportedTopLevelNames>()

    /**
     * 解析指定包最终对外导出的顶层 callable 与 classifier 名称。
     *
     * 返回值同时包含可见名称集合与 visible-name 到真实声明目标的映射。
     */
    fun resolve(packageFqName: FqName): CjoExportedTopLevelNames {
        cache[packageFqName.asString()]?.let { return it }
        val resolved = resolveRecursively(packageFqName, linkedSetOf())
        cache.putIfAbsent(packageFqName.asString(), resolved)
        return cache[packageFqName.asString()] ?: resolved
    }

    /**
     * 递归解析包导出视图，并使用 [visiting] 截断循环 re-export。
     *
     * 遇到缺失包或循环边时返回空导出视图，保证单个坏依赖不会污染已解析缓存。
     */
    private fun resolveRecursively(
        packageFqName: FqName,
        visiting: LinkedHashSet<FqName>,
    ): CjoExportedTopLevelNames {
        cache[packageFqName.asString()]?.let { return it }
        if (!visiting.add(packageFqName)) {
            return CjoExportedTopLevelNames(
                callableNames = emptySet(),
                classifierNames = emptySet(),
                callableTargets = emptyMap(),
                classifierTargets = emptyMap(),
            )
        }

        val header = cjoManager.loadPackageHeader(packageFqName.asString())
        if (header == null) {
            visiting.remove(packageFqName)
            return CjoExportedTopLevelNames(
                callableNames = emptySet(),
                classifierNames = emptySet(),
                callableTargets = emptyMap(),
                classifierTargets = emptyMap(),
            )
        }

        val callableNames = linkedSetOf<Name>().apply { addAll(header.topLevelCallableNames) }
        val classifierNames = linkedSetOf<Name>().apply { addAll(header.topLevelClassNames) }
        val callableTargets = linkedMapOf<Name, CjoExportedTopLevelTarget>().apply {
            for (name in header.topLevelCallableNames) {
                put(name, CjoExportedTopLevelTarget(packageFqName, name))
            }
        }
        val classifierTargets = linkedMapOf<Name, CjoExportedTopLevelTarget>().apply {
            for (name in header.topLevelClassNames) {
                put(name, CjoExportedTopLevelTarget(packageFqName, name))
            }
        }

        for (entry in header.fileImportEntries) {
            if (!entry.isPublicExportImport()) continue
            val importedPackageFqName = entry.importedPackageFqName() ?: continue
            val importedNames = resolveRecursively(importedPackageFqName, visiting)

            if (entry.isAllUnder) {
                callableNames += importedNames.callableNames
                classifierNames += importedNames.classifierNames
                mergeExportTargets(callableTargets, importedNames.callableTargets)
                mergeExportTargets(classifierTargets, importedNames.classifierTargets)
                continue
            }

            val importedMemberName = entry.importedMemberName() ?: continue
            val exportedMemberName = entry.exportedMemberName() ?: continue

            if (importedMemberName in importedNames.callableNames) {
                callableNames += exportedMemberName
                importedNames.callableTargets[importedMemberName]?.let { target ->
                    callableTargets.putIfAbsent(exportedMemberName, target)
                }
            }
            if (importedMemberName in importedNames.classifierNames) {
                classifierNames += exportedMemberName
                importedNames.classifierTargets[importedMemberName]?.let { target ->
                    classifierTargets.putIfAbsent(exportedMemberName, target)
                }
            }
        }

        visiting.remove(packageFqName)
        return CjoExportedTopLevelNames(
            callableNames = callableNames,
            classifierNames = classifierNames,
            callableTargets = callableTargets,
            classifierTargets = classifierTargets,
        )
    }

    /**
     * 把源包的 visible-name 到真实目标映射合并进目标表。
     *
     * 已存在项保持优先，符合当前包本地声明和先出现 public import 的遮蔽规则。
     */
    private fun mergeExportTargets(
        destination: MutableMap<Name, CjoExportedTopLevelTarget>,
        source: Map<Name, CjoExportedTopLevelTarget>,
    ) {
        for ((visibleName, target) in source) {
            destination.putIfAbsent(visibleName, target)
        }
    }
}

/**
 * `public import` 在 `.cjo` 里落成 declaration import，不能把普通 import 也当成导出。
 *
 * 实测 `std.core.*` 这类隐式普通 import 会带 `withImplicitExport=true`，但 `isDecl=false`，
 * 因此必须同时检查两者。
 */
internal fun CjoImportEntry.isPublicExportImport(): Boolean = isDecl && withImplicitExport

/** 将 import 前缀路径解释为被导入包的 [FqName]。 */
internal fun CjoImportEntry.importedPackageFqName(): FqName? =
    prefixPaths.takeIf { it.isNotEmpty() }?.let(FqName::fromSegments)

/** 返回非 all-under import 明确导入的成员名称。 */
internal fun CjoImportEntry.importedMemberName(): Name? =
    identifier.takeIf { !isAllUnder && it.isNotBlank() }?.let(Name::identifier)

/** 返回 re-export 后对外可见的成员名，alias 优先于原始 identifier。 */
internal fun CjoImportEntry.exportedMemberName(): Name? = when {
    isAllUnder -> null
    !aliasName.isNullOrBlank() -> Name.identifier(aliasName)
    identifier.isBlank() -> null
    else -> Name.identifier(identifier)
}
