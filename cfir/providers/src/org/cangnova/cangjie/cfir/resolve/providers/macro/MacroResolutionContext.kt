package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 单个 macro-related import 的绑定结果。
 */
data class MacroImportBinding(
    /** import 源元素。 */
    val import: CfirImport,
    /** import 目标的完整 fqn（若 `isAllUnder`，则是包名）。 */
    val importedFqName: FqName,
    /** 是否为 wildcard import。 */
    val isAllUnder: Boolean,
    /** 显式 alias；无 alias 时为 `null`，wildcard import 也必为 `null`。 */
    val aliasName: Name?,
    /** 关联到的 [MacroDefinitionEntry]，由 alias / 包 lookup 共同决定，可能为空。 */
    val resolvedTargets: List<MacroDefinitionEntry>,
)

/**
 * Macro alias 冲突诊断（baseline 第 12 节 Batch 5）。
 *
 * 当同一 alias 短名被多个 import 用于不同 fqn 时，应当在 construction 期上报。
 */
data class MacroAliasConflict(
    val alias: Name,
    val targets: List<FqName>,
    val source: CjSourceElement?,
)

/**
 * Builtin macro / annotation / non-macro registry 集合（baseline 第 4 节）。
 *
 * 这些名称参与 [MacroResolutionContext.resolveMacroCall] 的优先级判定，
 * 默认通过 [bindMacroImports] 注入。
 */
data class MacroBuiltinRegistries(
    val macros: Set<Name>,
    val annotations: Set<Name>,
    val nonMacros: Set<Name>,
) {
    companion object {
        val DEFAULT: MacroBuiltinRegistries = MacroBuiltinRegistries(
            macros = BuiltinMacroRegistry.all.toSet(),
            // 暂未完整 lower 仓颉内建 annotation，先注册常用的几个
            annotations = setOf(
                Name.identifier("Java"),
                Name.identifier("C"),
                Name.identifier("Deprecated"),
                Name.identifier("Frozen"),
            ),
            // baseline 第 8 节："builtin non-macro surface"
            nonMacros = setOf(
                Name.identifier("IfAvailable"),
            ),
        )

        val EMPTY: MacroBuiltinRegistries = MacroBuiltinRegistries(emptySet(), emptySet(), emptySet())
    }
}

/**
 * Macro 构造期 import 解析上下文（baseline 第 4 节"bindMacroImports"产物）。
 *
 * 它是一个 mini import binding，**只**绑定宏相关信息：
 * - explicit import / wildcard import / package alias
 * - default macro imports
 * - builtin macro / annotation / non-macro registry
 * - alias 冲突诊断
 *
 * 这与 ordinary `IMPORTS` phase 是两套独立逻辑：
 * - ordinary `IMPORTS` 在 expanded files 已被 record 之后才运行，
 *   能够看到完整的 source provider 与 final CFIR；
 * - `MacroResolutionContext` 仅在 macro construction step 内使用，
 *   严禁读取 source provider。
 */
class MacroResolutionContext internal constructor(
    val symbolIndex: MacroSymbolIndex,
    val importBindings: List<MacroImportBinding>,
    /** alias 短名 → 真实 fqn */
    val packageAliases: Map<Name, FqName>,
    /** 同名 alias 绑到多个 fqn 的冲突列表（baseline 第 5 节 Batch 5）。 */
    val aliasConflicts: List<MacroAliasConflict>,
    /** 默认隐式 macro import */
    val defaultMacroImports: List<FqName>,
    /** Builtin macro / annotation / non-macro 名称注册表 */
    val builtinRegistries: MacroBuiltinRegistries,
) {
    /** 便利访问：builtin macro 名集合。 */
    val builtinMacroRegistry: Set<Name> get() = builtinRegistries.macros

    /** 便利访问：builtin annotation 名集合。 */
    val builtinAnnotationRegistry: Set<Name> get() = builtinRegistries.annotations

    /** 便利访问：builtin non-macro 名集合。 */
    val builtinNonMacroRegistry: Set<Name> get() = builtinRegistries.nonMacros

    /**
     * 按 `@<name>` / `@<package>.<name>` 调用形式查找。
     *
     * 返回结果优先级（baseline 第 8 节 + 第 12 节 Batch 5）：
     *
     * 1. builtin non-macro surface（如 `@IfAvailable`） — 不送 executor，
     *    必须在 stable splice 前 desugar。
     * 2. builtin macro（`sourcePackage` / `sourceFile` / `sourceLine`） — 内建 evaluator 入口。
     * 3. 同包 def/call 检测 — 命中即诊断为非法。
     * 4. qualifier 形式（含包前缀） — 直接 fqn lookup。
     * 5. explicit import 命中（aliasName / shortName 匹配）。
     * 6. wildcard import 命中。
     * 7. default macro import 命中。
     * 8. custom annotation fallback —— builtin annotation 注册表或
     *    `MacroResolution.CustomAnnotation` 由上层 reclassify 处理。
     *
     * 当 [kind] 为 [MacroSurface.Kind.FORCED]（`@!`）时，
     * 仅返回支持强制形式的宏（[MacroDefinitionEntry.supportsForcedKind]）。
     */
    fun resolveMacroCall(
        callPackage: FqName,
        qualifier: FqName?,
        name: Name,
        kind: MacroSurface.Kind = MacroSurface.Kind.PLAIN,
    ): MacroResolution {
        // 1. builtin non-macro：先于一切 macro lookup
        if (name in builtinRegistries.nonMacros) {
            return MacroResolution.BuiltinNonMacro(name)
        }

        // 2. builtin macro
        if (name in builtinRegistries.macros) {
            val builtin = symbolIndex.lookupByFqName(FqName.topLevel(name))
            if (builtin != null && builtin.source == MacroDefinitionEntry.Source.BUILTIN_MACRO) {
                return verifyKindOrUnresolved(builtin, kind) ?: MacroResolution.Builtin(builtin)
            }
        }

        // 3. 同包 def/call 禁止
        val samePackageDef = symbolIndex.samePackageMacroDef(callPackage, name)
        if (samePackageDef != null) {
            return MacroResolution.SamePackage(samePackageDef)
        }

        // 4. qualifier 形式
        if (qualifier != null) {
            val resolved = symbolIndex.lookupByFqName(qualifier.child(name))
            if (resolved != null) {
                return verifyKindOrUnresolved(resolved, kind) ?: MacroResolution.Resolved(resolved)
            }
        }

        // 5. explicit import 命中
        for (binding in importBindings) {
            if (binding.aliasName == name || (!binding.isAllUnder && binding.importedFqName.shortName() == name)) {
                val target = binding.resolvedTargets.firstOrNull() ?: continue
                return verifyKindOrUnresolved(target, kind) ?: MacroResolution.Resolved(target)
            }
        }

        // 6. wildcard 命中
        for (binding in importBindings) {
            if (binding.isAllUnder) {
                val candidate = symbolIndex.lookupByFqName(binding.importedFqName.child(name))
                if (candidate != null) {
                    return verifyKindOrUnresolved(candidate, kind) ?: MacroResolution.Resolved(candidate)
                }
            }
        }

        // 7. default macro import 命中
        for (defaultPkg in defaultMacroImports) {
            val candidate = symbolIndex.lookupByFqName(defaultPkg.child(name))
            if (candidate != null) {
                return verifyKindOrUnresolved(candidate, kind) ?: MacroResolution.Resolved(candidate)
            }
        }

        // 8. custom annotation fallback：builtin annotation 名称走 annotation reclassify
        if (name in builtinRegistries.annotations) {
            return MacroResolution.CustomAnnotation(name)
        }

        return MacroResolution.Unresolved(name)
    }

    /**
     * 若 [target] 支持 [kind]，返回 null（继续返回 Resolved）；
     * 否则返回 [MacroResolution.KindMismatch]。
     */
    private fun verifyKindOrUnresolved(
        target: MacroDefinitionEntry,
        kind: MacroSurface.Kind,
    ): MacroResolution? {
        if (kind == MacroSurface.Kind.FORCED && !target.supportsForcedKind) {
            return MacroResolution.KindMismatch(target, kind)
        }
        return null
    }
}

sealed class MacroResolution {
    /** 正常 import / qualifier 路径找到的 macro 定义。 */
    data class Resolved(val entry: MacroDefinitionEntry) : MacroResolution()

    /** 内建 macro（不送 external executor）。 */
    data class Builtin(val entry: MacroDefinitionEntry) : MacroResolution()

    /** builtin non-macro surface（如 `@IfAvailable`），需在 splice 前 desugar。 */
    data class BuiltinNonMacro(val name: Name) : MacroResolution()

    /** 同包 macro def/call 非法形态。 */
    data class SamePackage(val sourceEntry: MacroDefinitionEntry) : MacroResolution()

    /** Fallback 到 custom annotation reclassify 路径。 */
    data class CustomAnnotation(val name: Name) : MacroResolution()

    /** target 不支持调用方使用的 [MacroSurface.Kind]（如 `@!` 形式）。 */
    data class KindMismatch(
        val entry: MacroDefinitionEntry,
        val requestedKind: MacroSurface.Kind,
    ) : MacroResolution()

    /** 找不到任何匹配。 */
    data class Unresolved(val name: Name) : MacroResolution()
}

/**
 * 解析 [pre] 的 import 列表，产出 macro construction 期可用的 [MacroResolutionContext]。
 *
 * 该入口与 ordinary `IMPORTS` phase **独立**：
 * - 不读取 source provider；
 * - 不构造 ordinary `CfirImportBinding` / `CfirScope`；
 * - 仅基于 [pre] 的 import 列表与 [symbolIndex] 做最小 lookup。
 *
 * 同 alias 多 fqn 冲突在 construction 期被检测并写入
 * [MacroResolutionContext.aliasConflicts]。
 */
fun bindMacroImports(
    pre: PreMacroRawBuildResult,
    symbolIndex: MacroSymbolIndex,
    defaultMacroImports: List<FqName> = emptyList(),
    builtinRegistries: MacroBuiltinRegistries = MacroBuiltinRegistries.DEFAULT,
): MacroResolutionContext {
    val bindings = mutableListOf<MacroImportBinding>()
    val packageAliases = mutableMapOf<Name, FqName>()
    val aliasTargets = mutableMapOf<Name, MutableSet<FqName>>()
    val aliasSources = mutableMapOf<Name, CjSourceElement?>()

    for (preFile in pre.files) {
        val file = preFile.cfirFile
        for (import in file.imports) {
            val fqn = import.importedFqName ?: continue
            val isAllUnder = import.isAllUnder
            val alias = import.aliasName

            val targets = if (isAllUnder) {
                symbolIndex.foreigns.filter { it.packageFqName == fqn }
            } else {
                listOfNotNull(symbolIndex.lookupByFqName(fqn))
            }

            bindings += MacroImportBinding(
                import = import,
                importedFqName = fqn,
                isAllUnder = isAllUnder,
                aliasName = alias,
                resolvedTargets = targets,
            )

            // alias 仅在非 wildcard 时有意义
            if (!isAllUnder && alias != null) {
                if (!fqn.isRoot) {
                    packageAliases[alias] = fqn.parent()
                }
                aliasTargets.getOrPut(alias) { mutableSetOf() } += fqn
                aliasSources.putIfAbsent(alias, import.aliasSource ?: import.source)
            }
        }
    }

    val conflicts = aliasTargets
        .filter { it.value.size > 1 }
        .map { (alias, targets) -> MacroAliasConflict(alias, targets.toList(), aliasSources[alias]) }

    return MacroResolutionContext(
        symbolIndex = symbolIndex,
        importBindings = bindings,
        packageAliases = packageAliases,
        aliasConflicts = conflicts,
        defaultMacroImports = defaultMacroImports,
        builtinRegistries = builtinRegistries,
    )
}
