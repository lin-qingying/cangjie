package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 单条宏定义索引项。
 *
 * 一个 [MacroDefinitionEntry] 描述 "在哪个包下有一个叫这个名字的宏"。
 * `source` 标识来源类别，决定该 entry 是否能作为合法的宏调用查找目标
 * （baseline 第 4 节："合法宏调用目标来自 imported macro package / macro artifact / builtins"）。
 */
data class MacroDefinitionEntry(
    /** 宏所在包的完整路径；builtin 没有归属包时为 [FqName.ROOT]。 */
    val packageFqName: FqName,
    /** 宏名（与 `@<name>` / `@<name>()` 调用形式一致）。 */
    val name: Name,
    /**
     * 宏真实定义所在包。
     *
     * 对 `public import` / alias 重导出的宏，lookup 仍然使用 [packageFqName] + [name]，
     * 但 executor 必须按真实宏定义包去构造 wrapper 方法名和 packageName。
     */
    val executablePackageFqName: FqName = packageFqName,
    /**
     * 宏真实定义名。
     *
     * 对 alias 重导出，源码可见名与真实定义名可能不同；`MacroCallInfo.idName`
     * 保持源码可见名，而 `methodName` 必须使用这里的真实定义名。
     */
    val executableName: Name = name,
    /** Entry 来源类别。 */
    val source: Source,
    /**
     * 关联的源码 [CfirMacroDeclaration]；仅 [Source.SOURCE_PACKAGE] 时非空。
     *
     * baseline 强约束：source `CfirMacroDeclaration` 只进入 [MacroSymbolIndex]，
     * **不**进入 source final provider —— 因此该字段只在构造期使用。
     */
    val declaration: CfirMacroDeclaration? = null,
    /** 当 entry 来自动态 macro 库时，库文件路径；其他情况为 null。 */
    val libPath: String? = null,
    /** 该 entry 对应的 executor ABI 版本（baseline 第 11 节 cache key 之一）。 */
    val executorAbi: String? = null,
    /** 该 entry 所属宏 artifact 的稳定签名；由 resolver/orchestration 提供或由路径内容 hash 合成。 */
    val artifactSignature: String? = null,
    /** `.cjo` 内容 hash；宏定义元数据变化必须触发 cache 失效。 */
    val cjoHash: String? = null,
    /** 动态库内容 hash；运行产物变化必须触发 cache 失效。 */
    val dynamicLibHash: String? = null,
    /** 依赖产物内容 hash 聚合；宏依赖变化必须触发 cache 失效。 */
    val dependenciesArtifactHash: String? = null,
    /** 产生该 entry 的 artifact resolver 算法版本。 */
    val resolverAlgorithmVersion: Int? = null,
    /** 若该宏支持 `@!` 强制形式则为 true（baseline 第 8 节 / 第 12 节 Batch 5）。 */
    val supportsForcedKind: Boolean = false,
    /** 若该宏支持 `plain-attr overload`（无括号 attr 形式），则为 true。 */
    val supportsPlainAttrOverload: Boolean = false,
) {
    enum class Source {
        /** 由本次 build 中的源包内 `macro func ...` / `macro Name` 提供。不能作为合法宏调用目标。 */
        SOURCE_PACKAGE,

        /** 通过 `.cjo` 反序列化得到的库宏。 */
        LIBRARY,

        /** 共享库 / `std.core` 等基础库提供的宏。 */
        SHARED_BUILTIN,

        /** 与本次 build 关联的独立 macro artifact（动态库 / 包二进制）。 */
        MACRO_ARTIFACT,

        /** evaluator 内建宏：`sourcePackage` / `sourceFile` / `sourceLine` 等。 */
        BUILTIN_MACRO,
    }

    val fqName: FqName get() = if (packageFqName.isRoot) FqName.topLevel(name) else packageFqName.child(name)
    val executableFqName: FqName
        get() = if (executablePackageFqName.isRoot) {
            FqName.topLevel(executableName)
        } else {
            executablePackageFqName.child(executableName)
        }
}

/**
 * Macro 构造期符号索引（baseline 第 4 节）。
 *
 * 用于解决"宏解析前 source provider 必须为空、但解析仍需查到宏定义"的矛盾：
 * - 源包内的 macro 声明会被收集到本索引（[Source.SOURCE_PACKAGE]），用于
 *   "同包 macro def + call" 诊断，但**不**作为合法 lookup 目标；
 * - 通过 import 引入的 library / artifact / shared / builtin 宏，作为合法 lookup 目标。
 *
 * 该索引完全离线（不读取 [org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl]），
 * 因此满足 baseline 硬性边界 #2（construction 前 source provider 必为空）。
 */
class MacroSymbolIndex internal constructor(
    private val sourceEntries: List<MacroDefinitionEntry>,
    private val foreignEntriesByName: Map<Name, List<MacroDefinitionEntry>>,
    private val foreignEntriesByFqName: Map<FqName, MacroDefinitionEntry>,
) {
    /** 源包内宏定义条目（不能作为合法 lookup 目标）。 */
    val sources: List<MacroDefinitionEntry> get() = sourceEntries

    /** 所有可作为合法 lookup 目标的条目。 */
    val foreigns: List<MacroDefinitionEntry>
        get() = foreignEntriesByName.values.flatten()

    /**
     * 是否存在与 [name] 同名、且与 [callPackage] 同包的源宏定义？
     *
     * 这是 baseline 第 4 节"官方同包 macro def/call 禁止"的判定函数。
     */
    fun samePackageMacroDef(callPackage: FqName, name: Name): MacroDefinitionEntry? =
        sourceEntries.firstOrNull { it.packageFqName == callPackage && it.name == name }

    /** 查找全限定名匹配的合法宏定义。 */
    fun lookupByFqName(fqName: FqName): MacroDefinitionEntry? = foreignEntriesByFqName[fqName]

    /** 仅按短名查找；alias / wildcard import 由 [MacroResolutionContext] 解析后调用此入口。 */
    fun lookupByShortName(name: Name): List<MacroDefinitionEntry> =
        foreignEntriesByName[name].orEmpty()

    companion object {
        val EMPTY: MacroSymbolIndex = MacroSymbolIndex(emptyList(), emptyMap(), emptyMap())
    }
}

/**
 * Builtin macro registry —— 由 evaluator 内建生成的宏。
 *
 * 注意 baseline 第 8 节："builtin macro（sourcePackage / sourceFile / sourceLine）
 * 由 evaluator 内建生成 tokens，不走 dynamic lib / external executor"。
 */
object BuiltinMacroRegistry {
    val sourcePackage: Name = Name.identifier("sourcePackage")
    val sourceFile: Name = Name.identifier("sourceFile")
    val sourceLine: Name = Name.identifier("sourceLine")

    val all: List<Name> = listOf(sourcePackage, sourceFile, sourceLine)
}

/**
 * 构造 [MacroSymbolIndex]：
 *
 * - 从 [pre] 的所有 raw 文件中收集 source [CfirMacroDeclaration]；
 * - 合并 [libraryDefinitions] / [sharedBuiltinDefinitions] / [macroArtifactDefinitions]
 *   作为合法 lookup 目标；
 * - 注册 [builtinMacros] 作为内建 evaluator 入口。
 *
 * 该入口禁止读取 source final provider，函数内部也不会触碰
 * [org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl] 的任何状态。
 */
fun buildMacroSymbolIndex(
    pre: PreMacroRawBuildResult,
    libraryDefinitions: List<MacroDefinitionEntry> = emptyList(),
    sharedBuiltinDefinitions: List<MacroDefinitionEntry> = emptyList(),
    macroArtifactDefinitions: List<MacroDefinitionEntry> = emptyList(),
    builtinMacros: List<Name> = BuiltinMacroRegistry.all,
): MacroSymbolIndex {
    val sourceEntries = collectSourceMacroDefinitions(pre)
    val builtinEntries = builtinMacros.map { name ->
        MacroDefinitionEntry(
            packageFqName = FqName.ROOT,
            name = name,
            source = MacroDefinitionEntry.Source.BUILTIN_MACRO,
        )
    }

    val foreignEntries: List<MacroDefinitionEntry> = buildList {
        addAll(libraryDefinitions)
        addAll(sharedBuiltinDefinitions)
        addAll(macroArtifactDefinitions)
        addAll(builtinEntries)
    }

    val foreignByName = foreignEntries.groupBy(MacroDefinitionEntry::name)
    val foreignByFqName = foreignEntries.associateBy(MacroDefinitionEntry::fqName)

    return MacroSymbolIndex(sourceEntries, foreignByName, foreignByFqName)
}

/**
 * 从 [pre] 中提取所有顶层 / 嵌套的源 [CfirMacroDeclaration]。
 *
 * 注意：仓颉 macro 声明可以出现在类体内（macro member），所以这里会递归 class-like 成员。
 */
private fun collectSourceMacroDefinitions(pre: PreMacroRawBuildResult): List<MacroDefinitionEntry> {
    val result = mutableListOf<MacroDefinitionEntry>()
    for (preFile in pre.files) {
        if (!preFile.isMacroPackage) continue
        val file = preFile.cfirFile
        val packageFqName = file.packageDirective.packageFqName
        collectMacroDeclarationsInto(file.declarations, packageFqName, result)
    }
    return result
}

private fun collectMacroDeclarationsInto(
    declarations: List<CfirDeclaration>,
    packageFqName: FqName,
    out: MutableList<MacroDefinitionEntry>,
) {
    for (declaration in declarations) {
        when (declaration) {
            is CfirMacroDeclaration -> {
                if (declaration.status.visibility == Visibilities.Public) {
                    out += MacroDefinitionEntry(
                        packageFqName = packageFqName,
                        name = declaration.name,
                        source = MacroDefinitionEntry.Source.SOURCE_PACKAGE,
                        declaration = declaration,
                    )
                }
            }
            is CfirClassLikeDeclaration -> {
                val nested: List<CfirDeclaration> = when (declaration) {
                    is CfirClass -> declaration.declarations
                    is CfirInterface -> declaration.declarations
                    is CfirStruct -> declaration.declarations
                    is CfirEnum -> declaration.declarations
                    else -> emptyList()
                }
                if (nested.isNotEmpty()) collectMacroDeclarationsInto(nested, packageFqName, out)
            }
            is CfirFile -> collectMacroDeclarationsInto(declaration.declarations, packageFqName, out)
            else -> Unit
        }
    }
}
