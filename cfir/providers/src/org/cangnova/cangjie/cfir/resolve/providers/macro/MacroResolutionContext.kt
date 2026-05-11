package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

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
 * Macro 构造期 import 解析上下文（baseline 第 4 节"bindMacroImports"产物）。
 *
 * 它是一个 mini import binding，**只**绑定宏相关信息：
 * - explicit import / wildcard import / package alias
 * - default macro imports
 * - builtin macro / annotation / non-macro registry
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
    /** 默认隐式 macro import */
    val defaultMacroImports: List<FqName>,
    /** 内建 macro 名集合 */
    val builtinMacroRegistry: Set<Name>,
    /** 内建 annotation 名集合（custom annotation 回退使用） */
    val builtinAnnotationRegistry: Set<Name>,
    /** 内建 non-macro surface 名集合（如 `IfAvailable`） */
    val builtinNonMacroRegistry: Set<Name>,
) {
    /**
     * 按 `@<name>` / `@<package>.<name>` 调用形式查找。
     *
     * 返回结果优先级：
     * 1. builtin macro
     * 2. explicit import 命中
     * 3. wildcard import 命中
     * 4. default macro import 命中
     *
     * 不会返回 [MacroDefinitionEntry.Source.SOURCE_PACKAGE]
     * （因 baseline 禁止同包 def/call）。
     */
    fun resolveMacroCall(callPackage: FqName, qualifier: FqName?, name: Name): MacroResolution {
        // builtin macro 优先（不受 import 影响）
        if (name in builtinMacroRegistry) {
            val builtin = symbolIndex.lookupByFqName(FqName.topLevel(name))
            if (builtin != null && builtin.source == MacroDefinitionEntry.Source.BUILTIN_MACRO) {
                return MacroResolution.Builtin(builtin)
            }
        }

        // 同包 def/call 禁止：先查 source index，命中即视为非法
        val samePackageDef = symbolIndex.samePackageMacroDef(callPackage, name)
        if (samePackageDef != null) {
            return MacroResolution.SamePackage(samePackageDef)
        }

        // qualifier 形式：直接走 fqn 查
        if (qualifier != null) {
            val resolved = symbolIndex.lookupByFqName(qualifier.child(name))
            if (resolved != null) return MacroResolution.Resolved(resolved)
        }

        // explicit import 命中（aliasName / importedName 对得上）
        for (binding in importBindings) {
            if (binding.aliasName == name || (!binding.isAllUnder && binding.importedFqName.shortName() == name)) {
                val target = binding.resolvedTargets.firstOrNull()
                if (target != null) return MacroResolution.Resolved(target)
            }
        }

        // wildcard 命中
        for (binding in importBindings) {
            if (binding.isAllUnder) {
                val candidate = symbolIndex.lookupByFqName(binding.importedFqName.child(name))
                if (candidate != null) return MacroResolution.Resolved(candidate)
            }
        }

        // default macro import 命中
        for (defaultPkg in defaultMacroImports) {
            val candidate = symbolIndex.lookupByFqName(defaultPkg.child(name))
            if (candidate != null) return MacroResolution.Resolved(candidate)
        }

        return MacroResolution.Unresolved(name)
    }
}

sealed class MacroResolution {
    data class Resolved(val entry: MacroDefinitionEntry) : MacroResolution()
    data class Builtin(val entry: MacroDefinitionEntry) : MacroResolution()
    data class SamePackage(val sourceEntry: MacroDefinitionEntry) : MacroResolution()
    data class Unresolved(val name: Name) : MacroResolution()
}

/**
 * 解析 [pre] 的 import 列表，产出 macro construction 期可用的 [MacroResolutionContext]。
 *
 * 该入口与 ordinary `IMPORTS` phase **独立**：
 * - 不读取 source provider；
 * - 不构造 ordinary `CfirImportBinding` / `CfirScope`；
 * - 仅基于 [pre] 的 import 列表与 [symbolIndex] 做最小 lookup。
 */
fun bindMacroImports(
    pre: PreMacroRawBuildResult,
    symbolIndex: MacroSymbolIndex,
    defaultMacroImports: List<FqName> = emptyList(),
    builtinMacroRegistry: Set<Name> = BuiltinMacroRegistry.all.toSet(),
    builtinAnnotationRegistry: Set<Name> = emptySet(),
    builtinNonMacroRegistry: Set<Name> = emptySet(),
): MacroResolutionContext {
    val bindings = mutableListOf<MacroImportBinding>()
    val packageAliases = mutableMapOf<Name, FqName>()

    for (preFile in pre.files) {
        val file = preFile.cfirFile
        for (import in file.imports) {
            val fqn = import.importedFqName ?: continue
            val isAllUnder = import.isAllUnder
            val alias = import.aliasName

            // wildcard import：注册 alias 暂不可用，候选目标来自 symbolIndex 的 package
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
            if (!isAllUnder && alias != null && !fqn.isRoot) {
                packageAliases[alias] = fqn.parent()
            }
        }
    }

    return MacroResolutionContext(
        symbolIndex = symbolIndex,
        importBindings = bindings,
        packageAliases = packageAliases,
        defaultMacroImports = defaultMacroImports,
        builtinMacroRegistry = builtinMacroRegistry,
        builtinAnnotationRegistry = builtinAnnotationRegistry,
        builtinNonMacroRegistry = builtinNonMacroRegistry,
    )
}
