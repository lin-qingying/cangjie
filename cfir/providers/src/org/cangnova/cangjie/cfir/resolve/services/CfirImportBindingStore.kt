package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 单条 import 指令解析出的目标分类。
 */
sealed interface CfirResolvedImportTarget {
    /**
     * import 指向包本身或 all-under 包空间。
     *
     * @property fqName 被 import 暴露的包名。
     */
    data class Package(
        /**
         * 被 import 暴露的包名。
         */
        val fqName: FqName,
    ) : CfirResolvedImportTarget

    /**
     * import 指向 class、interface、struct、enum 或 typealias 等 class-like 声明。
     *
     * @property classId 目标声明的稳定 ClassId。
     * @property symbol 已解析出的 class-like symbol。
     */
    data class ClassLike(
        /**
         * 目标声明的稳定 ClassId。
         */
        val classId: ClassId,
        /**
         * import 解析命中的 class-like symbol。
         */
        val symbol: CfirClassLikeSymbol<*>,
    ) : CfirResolvedImportTarget

    /**
     * import 指向一个或多个 callable 声明。
     *
     * @property packageFqName callable 真实所在包名。
     * @property name callable 的导入短名。
     * @property symbols 该短名下解析出的 callable overload 集合。
     */
    data class Callable(
        /**
         * callable 真实所在包名。
         */
        val packageFqName: FqName,
        /**
         * callable 的导入短名。
         */
        val name: Name,
        /**
         * 该短名下解析出的 callable overload 集合。
         */
        val symbols: List<CfirCallableSymbol<*>>,
    ) : CfirResolvedImportTarget
}

/**
 * 单条 import 指令解析完成后的可见绑定。
 *
 * @property importDirective 原始 CFIR import 节点。
 * @property effectiveName 当前文件中用于查找的有效短名。
 * @property targets 该短名解析出的全部目标。
 * @property lookupOrigin 该绑定来自源码显式 import 还是语言默认 import。
 */
data class CfirResolvedImportBinding(
    /**
     * 原始 CFIR import 节点。
     */
    val importDirective: CfirImport,
    /**
     * 当前文件中用于查找的有效短名，包含 alias 归一化结果。
     */
    val effectiveName: Name,
    /**
     * 该 import 短名解析出的全部目标。
     */
    val targets: List<CfirResolvedImportTarget>,
    /**
     * import binding 的结构性来源；不固化任何使用点可见性结论。
     */
    val lookupOrigin: CfirLookupOrigin,
)

/**
 * 一个文件全部 import 指令的解析绑定快照。
 *
 * @property file import 所属的 CFIR 文件。
 * @property imports 按文件 import 顺序记录的解析绑定列表。
 */
data class CfirFileImportBindings(
    /**
     * import 所属的 CFIR 文件。
     */
    val file: CfirFile,
    /**
     * 按文件 import 顺序记录的解析绑定列表。
     */
    val imports: List<CfirResolvedImportBinding>,
)

/**
 * 默认导入的结构优先级。
 *
 * 优先级属于文件 scope 布局，而不是声明可见性；因此它不进入 [CfirResolvedImportBinding]
 * 的 use-site 语义，只用于把高、低优先级默认导入缓存为互不混合的结构绑定集合。
 */
enum class CfirDefaultImportPriority {
    /** 普通默认导入。 */
    HIGH,

    /** 仅在更高优先级名称均未命中时参与查找的默认导入。 */
    LOW,
}

/**
 * session 级 import 绑定缓存。
 *
 * 它同时保存文件到 import 绑定的直接索引，以及相同 import 语义在 dangling/original
 * CFIR 副本之间共享的 canonical 目标，保证 lazy resolve 顺序不影响 import 解析结果。
 */
class CfirImportBindingStore : CfirSessionComponent {
    /**
     * 文件到该文件 import 绑定快照的索引。
     */
    private val bindingsByFile = mutableMapOf<CfirFile, CfirFileImportBindings>()
    /**
     * import 签名到最完整解析绑定的规范化索引。
     */
    private val canonicalBindingsByImport = mutableMapOf<ImportSignature, CfirResolvedImportBinding>()
    /** 当前 session 按结构优先级拆分的语言默认 import binding。 */
    private val defaultImportBindingsByPriority =
        mutableMapOf<CfirDefaultImportPriority, List<CfirResolvedImportBinding>>()

    /**
     * 记录 [file] 的 import 解析结果，并同步更新 canonical 绑定索引。
     */
    fun record(file: CfirFile, imports: List<CfirResolvedImportBinding>) {
        bindingsByFile[file] = CfirFileImportBindings(file = file, imports = imports)
        imports.forEach(::recordCanonicalBinding)
    }

    /**
     * 读取 [file] 的 import 绑定；若 canonical 索引有更完整目标，则返回合并后的视图。
     */
    fun getBindings(file: CfirFile): CfirFileImportBindings? {
        val bindings = bindingsByFile[file] ?: return null
        val normalizedImports = bindings.imports.map(::withCanonicalTargets)
        return if (normalizedImports === bindings.imports || normalizedImports == bindings.imports) {
            bindings
        } else {
            CfirFileImportBindings(file = bindings.file, imports = normalizedImports)
        }
    }

    /**
     * 返回 [file] 已由 IMPORTS 阶段记录的绑定。
     *
     * 后续 resolve 阶段禁止重新从 provider 回放 import；缺失绑定说明阶段契约被破坏，
     * 应立即暴露，而不是把空 import 或现场解析当作兼容路径。
     */
    fun requireBindings(file: CfirFile): CfirFileImportBindings = checkNotNull(getBindings(file)) {
        "File import bindings are missing after IMPORTS phase: ${file.sourceFile?.path ?: file.packageDirective.packageFqName}"
    }

    /**
     * 记录 IMPORTS 阶段解析出的语言默认 import binding。
     *
     * 默认 import 不属于某个源码文件，不能混入 [bindingsByFile]；它们仍与显式 import
     * 使用同一 binding 模型，区别仅由 [CfirResolvedImportBinding.lookupOrigin] 表达。
     * 后续阶段只允许读取这里的结果，禁止再次从 provider 回放默认 import。
     */
    fun recordDefaultImportBindings(
        priority: CfirDefaultImportPriority,
        bindings: List<CfirResolvedImportBinding>,
    ) {
        require(bindings.all { it.lookupOrigin == CfirLookupOrigin.DEFAULT_IMPORT }) {
            "Default import binding store received a non-default lookup origin"
        }
        val previous = defaultImportBindingsByPriority.putIfAbsent(priority, bindings)
        require(previous == null || previous == bindings) {
            "Default import bindings were recorded twice with different targets for $priority priority"
        }
        if (previous == null) bindings.forEach(::recordCanonicalBinding)
    }

    /** 返回已记录的默认 import binding；仅供 IMPORTS 阶段判断是否需要初始化。 */
    fun getDefaultImportBindings(priority: CfirDefaultImportPriority): List<CfirResolvedImportBinding>? =
        defaultImportBindingsByPriority[priority]?.map(::withCanonicalTargets)

    /**
     * 返回 IMPORTS 阶段已经建立的默认 import binding。
     *
     * 缺失结果表示阶段契约被破坏；调用方不能现场解析或退回直接读取
     * `DefaultImportsProvider`，否则不同解析入口会重新形成第二套导入语义。
     */
    fun requireDefaultImportBindings(priority: CfirDefaultImportPriority): List<CfirResolvedImportBinding> =
        checkNotNull(getDefaultImportBindings(priority)) {
            "Default import bindings are missing after IMPORTS phase for $priority priority"
        }

    /**
     * Lazy resolve 可能在 dangling/original CFIR 副本之间以不同顺序触发 IMPORTS。
     * 同一 session 内相同 import 指令的目标应保持一致，这里保存最完整的已知绑定，
     * 供同一语义 import 的其他 CFIR 文件副本读取。
     */
    private fun recordCanonicalBinding(binding: CfirResolvedImportBinding) {
        val signature = binding.signature()
        val current = canonicalBindingsByImport[signature]
        if (current == null || binding.targets.isMoreCompleteThan(current.targets)) {
            canonicalBindingsByImport[signature] = binding
        }
    }

    /**
     * 用 canonical 目标补全当前绑定，避免同一 import 在不同 CFIR 副本中目标集合不一致。
     */
    private fun withCanonicalTargets(binding: CfirResolvedImportBinding): CfirResolvedImportBinding {
        val canonical = canonicalBindingsByImport[binding.signature()] ?: return binding
        val targets = binding.targets.mergeWith(canonical.targets)
        return if (targets == binding.targets) binding else binding.copy(targets = targets)
    }

    /**
     * 判断当前目标集合是否比 [other] 更完整，非包目标数量优先，其次比较总目标数量。
     */
    private fun List<CfirResolvedImportTarget>.isMoreCompleteThan(other: List<CfirResolvedImportTarget>): Boolean {
        val nonPackageTargets = count { it !is CfirResolvedImportTarget.Package }
        val otherNonPackageTargets = other.count { it !is CfirResolvedImportTarget.Package }
        return nonPackageTargets > otherNonPackageTargets ||
            nonPackageTargets == otherNonPackageTargets && size > other.size
    }

    /**
     * 合并两个 import 目标集合，保持当前集合顺序并追加缺失的 canonical 目标。
     */
    private fun List<CfirResolvedImportTarget>.mergeWith(other: List<CfirResolvedImportTarget>): List<CfirResolvedImportTarget> {
        if (other.isEmpty()) return this
        if (isEmpty()) return other

        val result = toMutableList()
        for (target in other) {
            if (target !in result) {
                result += target
            }
        }
        return result
    }

    /**
     * 将 import 节点归一化为可跨 CFIR 副本比较的签名。
     */
    private fun CfirResolvedImportBinding.signature(): ImportSignature = ImportSignature(
        importedFqName = importDirective.importedFqName?.asString(),
        isAllUnder = importDirective.isAllUnder,
        aliasName = importDirective.aliasName?.asString(),
        lookupOrigin = lookupOrigin,
    )

    /**
     * import 指令的结构化签名。
     *
     * @property importedFqName 导入 FQN 的字符串表示。
     * @property isAllUnder 是否为 all-under import。
     * @property aliasName alias 短名的字符串表示。
     * @property lookupOrigin 显式和默认 import 使用独立 canonical 空间。
     */
    private data class ImportSignature(
        /**
         * 导入 FQN 的字符串表示。
         */
        val importedFqName: String?,
        /**
         * 是否为 all-under import。
         */
        val isAllUnder: Boolean,
        /**
         * alias 短名的字符串表示。
         */
        val aliasName: String?,
        /** import 的结构性来源。 */
        val lookupOrigin: CfirLookupOrigin,
    )
}
