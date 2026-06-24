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
 *
 * @property alias 发生冲突的 alias 名称。
 * @property targets 该 alias 绑定到的不同目标 FQN 集合。
 * @property source alias 声明的源码位置，用于诊断定位。
 */
data class MacroAliasConflict(
    /** 发生冲突的 alias 名称。 */
    val alias: Name,
    /** 该 alias 绑定到的不同目标 FQN 集合。 */
    val targets: List<FqName>,
    /** alias 声明的源码位置，用于诊断定位。 */
    val source: CjSourceElement?,
)

/**
 * Builtin macro / annotation / non-macro registry 集合（baseline 第 4 节）。
 *
 * 这些名称参与 [MacroResolutionContext.resolveMacroCall] 的优先级判定，
 * 默认通过 [bindMacroImports] 注入。
 *
 * @property macros 内建 macro 名称集合。
 * @property annotations 内建普通 annotation 名称集合，不送 macro executor。
 * @property nonMacros 内建 non-macro surface 名称集合。
 */
data class MacroBuiltinRegistries(
    /** 内建 macro 名称集合。 */
    val macros: Set<Name>,
    /** 内建普通 annotation 名称集合，不送 macro executor。 */
    val annotations: Set<Name>,
    /** 内建 non-macro surface 名称集合。 */
    val nonMacros: Set<Name>,
) {
    /** 默认 builtin registry 与版本常量。 */
    companion object {
        /**
         * Builtin registry 版本（baseline §11 cache key 第 12/13 维之一）。
         *
         * 任何 [DEFAULT] 内 macros / annotations / nonMacros 名单变更都必须递增；
         * 上游 cache 据此整体失效。
         */
        const val VERSION: Int = 2

        /** 生产路径默认 builtin macro / annotation / non-macro 注册表。 */
        val DEFAULT: MacroBuiltinRegistries = MacroBuiltinRegistries(
            macros = BuiltinMacroRegistry.all.toSet(),
            // 仓颉内建/互操作 annotation 是普通 annotation site，不参与 macro executor 解析。
            annotations = setOf(
                Name.identifier("C"),
                Name.identifier("CallingConv"),
                Name.identifier("CJMapping"),
                Name.identifier("Deprecated"),
                Name.identifier("ForeignName"),
                Name.identifier("Frozen"),
                Name.identifier("Java"),
                Name.identifier("JavaImpl"),
                Name.identifier("JavaMirror"),
                Name.identifier("ObjCCJMapping"),
                Name.identifier("ObjCImpl"),
                Name.identifier("ObjCInit"),
                Name.identifier("ObjCMirror"),
            ),
            // baseline 第 8 节："builtin non-macro surface"
            nonMacros = setOf(
                Name.identifier("IfAvailable"),
            ),
        )

        /** 空 builtin registry，供测试或禁用 builtin 的 construction 路径使用。 */
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
 *
 * @property symbolIndex macro construction 期离线符号索引。
 * @property importBindings macro-related import 的绑定结果列表。
 * @property packageAliases alias 短名到真实包名的映射。
 * @property aliasConflicts 同名 alias 绑到多个目标时的冲突列表。
 * @property defaultMacroImports 默认隐式 macro import 包列表。
 * @property builtinRegistries builtin macro / annotation / non-macro 注册表。
 */
class MacroResolutionContext internal constructor(
    /** macro construction 期离线符号索引。 */
    val symbolIndex: MacroSymbolIndex,
    /** macro-related import 的绑定结果列表。 */
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
     * 1. 同包 def/call 检测 — 命中即诊断为非法，且覆盖 builtin 名称。
     * 2. builtin non-macro surface（如 `@IfAvailable`） — 不送 executor，
     *    必须在 stable splice 前 desugar。
     * 3. builtin macro（`sourcePackage` / `sourceFile` / `sourceLine`） — 内建 evaluator 入口。
     * 4. qualifier 形式（含包前缀） — 直接 fqn lookup。
     * 5. explicit import 命中（aliasName / shortName 匹配）。
     * 6. wildcard import 命中。
     * 7. default macro import 命中。
     * 8. custom annotation fallback —— builtin annotation 注册表或
     *    `MacroResolution.CustomAnnotation` 由上层 reclassify 处理。
     *
     * 当 [kind] 为 [MacroSurface.Kind.FORCED]（`@!`）时，
     * 仅返回支持强制形式的宏（[MacroDefinitionEntry.supportsForcedKind]）。
     * 当 [hasParenthesis] 为 false 时，表达式等普通调用仍要求目标显式支持
     * plain-attr overload；声明宏输入按仓颉语义允许省略小括号，由
     * [allowsDeclarationInputParenthesisOmission] 显式标记。
     */
    fun resolveMacroCall(
        callPackage: FqName,
        qualifier: FqName?,
        name: Name,
        kind: MacroSurface.Kind = MacroSurface.Kind.PLAIN,
        hasParenthesis: Boolean = true,
        allowsDeclarationInputParenthesisOmission: Boolean = false,
    ): MacroResolution {
        // 1. 同包 def/call 禁止。该规则覆盖 builtin 名称，防止同包宏定义劫持 annotation site。
        val samePackageDef = symbolIndex.samePackageMacroDef(callPackage, name)
        if (samePackageDef != null) {
            return MacroResolution.SamePackage(samePackageDef)
        }

        // 2. builtin annotation：annotation site 不能被同名外部 macro 定义劫持。
        if (name in builtinRegistries.annotations) {
            return MacroResolution.CustomAnnotation(name)
        }

        // 3. builtin non-macro
        if (name in builtinRegistries.nonMacros) {
            return MacroResolution.BuiltinNonMacro(name)
        }

        // 4. builtin macro
        if (name in builtinRegistries.macros) {
            val builtin = symbolIndex.lookupByFqName(FqName.topLevel(name))
            if (builtin != null && builtin.source == MacroDefinitionEntry.Source.BUILTIN_MACRO) {
                return verifyCallShapeOrMismatch(
                    builtin,
                    kind,
                    hasParenthesis,
                    allowsDeclarationInputParenthesisOmission,
                ) ?: MacroResolution.Builtin(builtin)
            }
        }

        // 5. qualifier 形式
        if (qualifier != null) {
            val resolved = symbolIndex.lookupByFqName(qualifier.child(name))
            if (resolved != null) {
                return verifyCallShapeOrMismatch(
                    resolved,
                    kind,
                    hasParenthesis,
                    allowsDeclarationInputParenthesisOmission,
                ) ?: MacroResolution.Resolved(resolved)
            }
        }

        // 6. explicit import 命中
        for (binding in importBindings) {
            if (binding.aliasName == name || (!binding.isAllUnder && binding.importedFqName.shortName() == name)) {
                val target = binding.resolvedTargets.firstOrNull() ?: continue
                return verifyCallShapeOrMismatch(
                    target,
                    kind,
                    hasParenthesis,
                    allowsDeclarationInputParenthesisOmission,
                ) ?: MacroResolution.Resolved(target)
            }
        }

        // 7. wildcard 命中
        for (binding in importBindings) {
            if (binding.isAllUnder) {
                val candidate = symbolIndex.lookupByFqName(binding.importedFqName.child(name))
                if (candidate != null) {
                    return verifyCallShapeOrMismatch(
                        candidate,
                        kind,
                        hasParenthesis,
                        allowsDeclarationInputParenthesisOmission,
                    ) ?: MacroResolution.Resolved(candidate)
                }
            }
        }

        // 8. default macro import 命中
        for (defaultPkg in defaultMacroImports) {
            val candidate = symbolIndex.lookupByFqName(defaultPkg.child(name))
            if (candidate != null) {
                return verifyCallShapeOrMismatch(
                    candidate,
                    kind,
                    hasParenthesis,
                    allowsDeclarationInputParenthesisOmission,
                ) ?: MacroResolution.Resolved(candidate)
            }
        }

        return MacroResolution.Unresolved(name)
    }

    /**
     * 若 [target] 支持调用形态，返回 null（继续返回 Resolved）；
     * 否则返回 [MacroResolution.KindMismatch]。
     */
    private fun verifyCallShapeOrMismatch(
        target: MacroDefinitionEntry,
        kind: MacroSurface.Kind,
        hasParenthesis: Boolean,
        allowsDeclarationInputParenthesisOmission: Boolean,
    ): MacroResolution? {
        if (kind == MacroSurface.Kind.FORCED && !target.supportsForcedKind) {
            return MacroResolution.KindMismatch(target, MacroResolution.KindMismatch.Reason.FORCED_KIND_NOT_SUPPORTED)
        }
        if (!hasParenthesis && !allowsDeclarationInputParenthesisOmission && !target.supportsPlainAttrOverload) {
            return MacroResolution.KindMismatch(target, MacroResolution.KindMismatch.Reason.PLAIN_ATTR_OVERLOAD_NOT_SUPPORTED)
        }
        return null
    }
}

/**
 * Macro 调用解析结果。
 *
 * 每个分支都只表达 construction routing 所需的信息，不直接触发 executor、
 * fragment parser 或 splice。
 */
sealed class MacroResolution {
    /**
     * 正常 import / qualifier 路径找到的 macro 定义。
     *
     * @property entry 被解析到的宏定义条目。
     */
    data class Resolved(val entry: MacroDefinitionEntry) : MacroResolution()

    /**
     * 内建 macro（不送 external executor）。
     *
     * @property entry 内建宏定义条目。
     */
    data class Builtin(val entry: MacroDefinitionEntry) : MacroResolution()

    /**
     * builtin non-macro surface（如 `@IfAvailable`），需在 splice 前 desugar。
     *
     * @property name builtin non-macro 名称。
     */
    data class BuiltinNonMacro(val name: Name) : MacroResolution()

    /**
     * 同包 macro def/call 非法形态。
     *
     * @property sourceEntry 同包中命中的源宏定义条目。
     */
    data class SamePackage(val sourceEntry: MacroDefinitionEntry) : MacroResolution()

    /**
     * Fallback 到 custom annotation reclassify 路径。
     *
     * @property name 被重新分类为普通 annotation 的名称。
     */
    data class CustomAnnotation(val name: Name) : MacroResolution()

    /**
     * target 不支持调用方使用的 [MacroSurface.Kind] 或 attr 形态。
     *
     * @property entry 形态不匹配的目标宏定义。
     * @property reason 不匹配原因。
     */
    data class KindMismatch(
        /** 形态不匹配的目标宏定义。 */
        val entry: MacroDefinitionEntry,
        /** 不匹配原因。 */
        val reason: Reason,
    ) : MacroResolution()
    {
        /** 宏调用形态不匹配原因。 */
        enum class Reason {
            /** 调用点使用 `@!`，但目标不支持 forced kind。 */
            FORCED_KIND_NOT_SUPPORTED,
            /** 调用点省略括号，但目标不支持 plain-attr overload。 */
            PLAIN_ATTR_OVERLOAD_NOT_SUPPORTED,
        }
    }

    /**
     * 找不到任何匹配。
     *
     * @property name 未解析到目标的 macro 名称。
     */
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

            // alias 仅在非 wildcard 且实际绑定到 macro definition 时属于 macro construction 语义。
            // 普通 import alias 冲突必须留给 ordinary imports checker 报 IMPORT_ALIAS_CONFLICT。
            if (!isAllUnder && alias != null && targets.isNotEmpty()) {
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
