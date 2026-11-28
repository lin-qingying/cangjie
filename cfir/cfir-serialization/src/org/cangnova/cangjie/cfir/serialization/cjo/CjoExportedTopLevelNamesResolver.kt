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
    private val contextProvider: org.cangnova.cangjie.cfir.serialization.provider.CjoPackageContextProvider? = null,
    private val mode: org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode = org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode.FULL,
) {
    /** 绑定查询按 canonical context 隔离；纯包头入口仍由本 resolver 的 manager 限定来源。 */
    private data class NamesKey(
        val packageName: FqName,
        val context: org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext?,
    )
    private val cache = ConcurrentHashMap<NamesKey, CjoExportedTopLevelNames>()

    /**
     * 解析指定包最终对外导出的顶层 callable 与 classifier 名称。
     *
     * 返回值同时包含可见名称集合与 visible-name 到真实声明目标的映射。
     */
    fun resolve(packageFqName: FqName): CjoExportedTopLevelNames {
        val packageName = packageFqName.asString()
        val ownContexts = (contextProvider as? org.cangnova.cangjie.cfir.serialization.provider.CjoDeserializationBinding)
            ?.getOwnPackageContexts(packageName, mode).orEmpty()
        if (ownContexts.isNotEmpty()) {
            val views = ownContexts.map { resolve(packageFqName, it) }
            if (views.size == 1) return views.single()
            val callableTargets = linkedMapOf<Name, CjoExportedTopLevelTarget>()
            val classifierTargets = linkedMapOf<Name, CjoExportedTopLevelTarget>()
            views.forEach {
                mergeExportTargets(callableTargets, it.callableTargets)
                mergeExportTargets(classifierTargets, it.classifierTargets)
            }
            return CjoExportedTopLevelNames(
                views.flatMapTo(linkedSetOf()) { it.callableNames },
                views.flatMapTo(linkedSetOf()) { it.classifierNames },
                callableTargets, classifierTargets,
            )
        }
        val context = contextProvider?.getPackageContext(packageName, mode)
        return resolve(packageFqName, context)
    }

    private fun resolve(
        packageFqName: FqName,
        context: org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext?,
    ): CjoExportedTopLevelNames {
        val checkedContext = context?.validatedTarget(context.binding, packageFqName.asString(), mode)
        val key = NamesKey(packageFqName, checkedContext)
        cache[key]?.let { return it }
        val resolved = resolveRecursively(packageFqName, linkedSetOf(), checkedContext)
        return cache.putIfAbsent(key, resolved) ?: resolved
    }

    /**
     * 递归解析包导出视图，并使用 [visiting] 截断循环 re-export。
     *
     * 遇到缺失包或循环边时返回空导出视图，保证单个坏依赖不会污染已解析缓存。
     */
    private fun resolveRecursively(
        packageFqName: FqName,
        visiting: LinkedHashSet<FqName>,
        context: org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext? = null,
    ): CjoExportedTopLevelNames {
        cache[NamesKey(packageFqName, context?.canonical())]?.let { return it }
        if (!visiting.add(packageFqName)) {
            return CjoExportedTopLevelNames(
                callableNames = emptySet(),
                classifierNames = emptySet(),
                callableTargets = emptyMap(),
                classifierTargets = emptyMap(),
            )
        }

        val headers = when {
            context != null -> listOf(context.header)
            contextProvider == null -> cjoManager.loadPackageHeaders(packageFqName.asString())
            else -> emptyList()
        }
        if (headers.isEmpty()) {
            visiting.remove(packageFqName)
            return CjoExportedTopLevelNames(
                callableNames = emptySet(),
                classifierNames = emptySet(),
                callableTargets = emptyMap(),
                classifierTargets = emptyMap(),
            )
        }

        val callableNames = linkedSetOf<Name>()
        val classifierNames = linkedSetOf<Name>()
        val callableTargets = linkedMapOf<Name, CjoExportedTopLevelTarget>()
        val classifierTargets = linkedMapOf<Name, CjoExportedTopLevelTarget>()
        for (header in headers) {
            callableNames += header.topLevelCallableNames
            classifierNames += header.topLevelClassNames
            for (name in header.topLevelCallableNames) {
                callableTargets.putIfAbsent(name, CjoExportedTopLevelTarget(packageFqName, name))
            }
            for (name in header.topLevelClassNames) {
                classifierTargets.putIfAbsent(name, CjoExportedTopLevelTarget(packageFqName, name))
            }
        }

        for (entry in headers.flatMap { it.fileImportEntries }) {
            if (!entry.isPublicExportImport()) continue
            val importedPackageFqName = entry.importedPackageFqName() ?: continue
            val importedContext = context?.let {
                checkNotNull(it.packageContextOrNull(importedPackageFqName.asString())) {
                    "Cannot load CJO public import target " + importedPackageFqName
                }
            }
            val importedNames = resolveRecursively(importedPackageFqName, visiting, importedContext)

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
