package org.cangnova.cangjie.ide.completion.impl.shared.imports

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionImportTarget
import org.cangnova.cangjie.ide.completion.api.lookup.CaImportConflict
import org.cangnova.cangjie.ide.completion.api.lookup.CaImportConflictKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjImportItem

/**
 * 导入计划的写入结果。
 *
 * 报告而不是直接改文件：调用方需要区分「新增了 import」「已存在所以跳过」
 * 「与既有绑定冲突」三种不同结论，它们对应三种不同的 UI 行为。
 */
data class CaImportApplyReport(
    /** 实际写入的 import 文本，按写入顺序。 */
    val added: List<String> = emptyList(),
    /** 已存在因而跳过的 import 文本。 */
    val skippedAsAlreadyImported: List<String> = emptyList(),
    /** 检出的冲突。 */
    val conflicts: List<CaImportConflict> = emptyList(),
    /** 采用限定名策略的表达式文本，按候选去重键索引。 */
    val qualifiedExpressionText: Map<String, String> = emptyMap(),
) {
    /** 本次是否改动了文档。 */
    val documentChanged: Boolean
        get() = added.isNotEmpty()
}

/**
 * 导入计划的应用器。
 *
 * **必须在写命令内调用**：导入与候选文本是同一次撤销的一部分，
 * 分成两个命令会让用户按一次 Ctrl+Z 只撤掉一半。
 */
interface CaImportPlanApplier {
    /**
     * 应用导入计划。
     *
     * @param file 目标文件。
     * @param plans 候选去重键到导入计划的映射；无导入需求的候选不出现在其中。
     */
    fun apply(file: CjFile, plans: Map<String, CaCompletionImportPlan>): CaImportApplyReport
}

/**
 * 基于仓颉 PSI 的导入计划应用器。
 *
 * 三条不变量：
 * 1. **幂等**：同一 import 已存在时不重复写入，也不产生空行或重复项；
 * 2. **组织名与别名只出现在 import 里**：表达式侧使用 [CaCompletionImportTarget.expressionText]，
 *    绝不把 `org::` 前缀抄进表达式访问；
 * 3. **冲突不静默**：短名已被其它声明绑定时报告冲突，由调用方决定改用限定名还是隐藏候选。
 */
class CaPsiImportPlanApplier(
    private val project: Project,
) : CaImportPlanApplier {
    override fun apply(file: CjFile, plans: Map<String, CaCompletionImportPlan>): CaImportApplyReport {
        val added = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val conflicts = mutableListOf<CaImportConflict>()
        val qualified = mutableMapOf<String, String>()

        val existing = ExistingImports.of(file)

        for ((key, plan) in plans) {
            when (plan) {
                is CaCompletionImportPlan.NoImport -> Unit
                is CaCompletionImportPlan.QualifyThenShorten ->
                    qualified[key] = plan.target.expressionText
                is CaCompletionImportPlan.AddImport -> {
                    val path = plan.target.importPath
                    if (existing.covers(path)) {
                        // 已导入：目标本来就可达，改成短名引用即可，无需再写 import。
                        skipped += path.toString()
                        continue
                    }
                    when (val conflict = existing.conflictOf(path)) {
                        null -> {
                            insertImport(file, path)
                            existing.record(path)
                            added += path.toString()
                        }
                        // 短名已被别的声明占用：追加 import 会造成二义，
                        // 此时退回「写限定名 + 后续缩短」而不是硬插一行 import。
                        else -> {
                            conflicts += conflict
                            qualified[key] = plan.target.expressionText
                        }
                    }
                }
            }
        }
        return CaImportApplyReport(added, skipped, conflicts, qualified)
    }

    /**
     * 写入一条 import。
     *
     * 走**文档文本**而不是 PSI 子节点插入：`CjImportDirective` 是带 stub 的元素，
     * 对它调用 `addAfter` 会落到 stub 感知 PSI 生成的成员上，既不能保证节点被正确
     * reparse，也拿不到想要的插入位置。写入文本后统一 commit，让 parser 重建树。
     *
     * 插入点选在已有 import 之后：仓颉的 import 必须位于包声明之后、声明之前，
     * 追加到 import 区末尾是唯一不破坏现有分组顺序的位置。
     */
    private fun insertImport(file: CjFile, path: ImportPath) {
        val documentManager = PsiDocumentManager.getInstance(project)
        // 没有文档就写入不了：静默返回会让报告声称「已添加」而文档未变，
        // 把基础设施错误伪装成成功。这里如实抛出，由调用方保证传入编辑器中的文件。
        val document = documentManager.getDocument(file)
            ?: error("文件 ${file.name} 没有关联文档，无法写入 import")
        val anchor = insertionOffset(file, document)
        document.insertString(anchor, "\nimport $path")
        documentManager.commitDocument(document)
    }

    /**
     * 计算新 import 的插入偏移。
     *
     * 优先级：最后一条 import 之后 → 包声明之后 → 文件开头。
     * 找不到锚点时返回 0：宁可让 import 落在文件最前面（IDE 随后会重排），
     * 也不要因为算不出位置而静默丢弃这条 import。
     */
    private fun insertionOffset(file: CjFile, document: com.intellij.openapi.editor.Document): Int {
        val lastImport = file.importDirectives.lastOrNull()
        if (lastImport != null) return lineEndOffset(document, lastImport.textRange.endOffset)
        val packageDirective = file.packageDirective
        if (packageDirective != null) return lineEndOffset(document, packageDirective.textRange.endOffset)
        return 0
    }

    /**
     * 返回指定偏移之后第一个行终止换行符的位置（不含换行本身）。
     *
     * 插入点选在**行终止换行符之前**，配合写入文本自带的开头换行：
     * 新 import 与上一条 import 相邻成行、且不粘在上一行末尾；
     * 若取换行之后，空行会把新 import 推离 import 区、贴到后面的声明上。
     */
    private fun lineEndOffset(document: com.intellij.openapi.editor.Document, offset: Int): Int {
        val text = document.charsSequence
        var index = offset
        while (index < text.length && text[index] != NEW_LINE) index++
        return index
    }
}

/**
 * 仓颉源码的语句终止符。
 *
 * 显式命名而不是内联换行字面量：插入点计算里它决定了「插在语句内还是语句间」，
 * 一旦被误改就会产出无法解析的文件。
 *
 * 放在文件顶层而不是类内：Kotlin 不允许普通类成员使用 `const val`。
 */
private const val NEW_LINE: Char = '\n'

/**
 * 文件当前的导入绑定。
 *
 * 判定「已导入」必须同时比对**完全限定名、组织名与别名**：
 * 只比短名会把 `a.Foo` 与 `b.Foo` 判成同一个，产生「以为导入了其实没导入」的静默错误。
 */
private class ExistingImports private constructor(
    private val paths: MutableList<ImportPath>,
    private val shortNameOwners: MutableMap<String, String>,
) {
    companion object {
        fun of(file: CjFile): ExistingImports {
            val paths = mutableListOf<ImportPath>()
            val owners = mutableMapOf<String, String>()
            for (directive in file.importDirectives) {
                for (item in directive.importItems) {
                    val path = item.toImportPath() ?: continue
                    paths += path
                    val importedName = path.importedName?.asString()
                    if (importedName != null) {
                        owners.putIfAbsent(importedName, path.pathStr)
                    }
                }
            }
            return ExistingImports(paths, owners)
        }
    }

    /**
     * 判断给定路径是否已被现有 import 覆盖。
     */
    fun covers(path: ImportPath): Boolean = paths.any { it == path }

    /**
     * 记录一条新写入的 import，使同一次调用中的后续计划能看到它。
     */
    fun record(path: ImportPath) {
        paths += path
        val importedName = path.importedName?.asString()
        if (importedName != null) shortNameOwners.putIfAbsent(importedName, path.pathStr)
    }

    /**
     * 检出短名冲突。
     *
     * 只在**同名不同路径**时判为冲突：同名同路径是幂等情形，已由 [covers] 处理。
     */
    fun conflictOf(path: ImportPath): CaImportConflict? {
        val importedName = path.importedName?.asString() ?: return null
        val owner = shortNameOwners[importedName] ?: return null
        if (owner == path.pathStr) return null
        return CaImportConflict(
            kind = CaImportConflictKind.SHORT_NAME_TAKEN_BY_OTHER,
            conflictingBinding = owner,
        )
    }
}

/**
 * 把仓颉 import 项还原为 [ImportPath]。
 *
 * 走 PSI 而非字符串反解：别名与组织限定在 PSI 上是结构化字段，
 * 用 `ImportPath.fromString` 重新解析会在带 `` ` `` 转义的标识符上出错。
 */
private fun CjImportItem.toImportPath(): ImportPath? {
    val fqName = importedFqName?.takeIf { !isAllUnder } ?: return null
    return ImportPath(fqName, isAllUnder, aliasName?.let { org.cangnova.cangjie.name.Name.identifier(it) }, organizationName)
}