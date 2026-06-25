package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirImport
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
     * Lazy resolve 可能在 dangling/original CFIR 副本之间以不同顺序触发 IMPORTS。
     * 同一 session 内相同 import 指令的目标应保持一致，这里保存最完整的已知绑定，
     * 供同一语义 import 的其他 CFIR 文件副本读取。
     */
    private fun recordCanonicalBinding(binding: CfirResolvedImportBinding) {
        val signature = binding.importDirective.signature()
        val current = canonicalBindingsByImport[signature]
        if (current == null || binding.targets.isMoreCompleteThan(current.targets)) {
            canonicalBindingsByImport[signature] = binding
        }
    }

    /**
     * 用 canonical 目标补全当前绑定，避免同一 import 在不同 CFIR 副本中目标集合不一致。
     */
    private fun withCanonicalTargets(binding: CfirResolvedImportBinding): CfirResolvedImportBinding {
        val canonical = canonicalBindingsByImport[binding.importDirective.signature()] ?: return binding
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
    private fun CfirImport.signature(): ImportSignature = ImportSignature(
        importedFqName = importedFqName?.asString(),
        isAllUnder = isAllUnder,
        aliasName = aliasName?.asString(),
    )

    /**
     * import 指令的结构化签名。
     *
     * @property importedFqName 导入 FQN 的字符串表示。
     * @property isAllUnder 是否为 all-under import。
     * @property aliasName alias 短名的字符串表示。
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
    )
}
