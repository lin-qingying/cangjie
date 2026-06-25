package org.cangnova.cangjie.cfir.resolve.providers

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement

/**
 * source import 的重导出视图。
 *
 * 这里只表达“本包可见名 -> 被导入包真实名”的映射，
 * 后续 provider 再决定去 source 还是 delegated provider 取 symbol。
 *
 * @property importedPackageFqName 被导入声明真实所在包。
 * @property importedName 被导入声明短名；星号导入时为 `null`。
 * @property exportedName 当前包对外暴露的短名；星号导入时为 `null`。
 * @property isAllUnder 是否为星号导入。
 */
internal data class CfirReexportImportInfo(
    /**
     * 被重导出声明真实所在的包名。
     */
    val importedPackageFqName: FqName,
    /**
     * 被重导出的具体短名；星号导入会保持为 `null`，由目标包名称集合决定。
     */
    val importedName: Name?,
    /**
     * 当前包向外暴露的短名；别名导入时等于 alias，星号导入时为 `null`。
     */
    val exportedName: Name?,
    /**
     * 是否为 all-under import，决定 provider 是否合并目标包的完整顶层名称集合。
     */
    val isAllUnder: Boolean,
)

/**
 * 仓颉 `public/protected/internal import` 会把导入成员重导出；
 * 默认 `import` 等价于 `private import`，不参与重导出。
 */
fun CfirImport.isReexportingSourceImport(): Boolean =
    source.importVisibilityKeyword() in REEXPORTING_IMPORT_VISIBILITIES

/**
 * 将当前 import 转换为 source provider 可使用的 reexport 信息。
 */
internal fun CfirImport.reexportInfoOrNull(): CfirReexportImportInfo? {
    if (!isReexportingSourceImport()) return null

    val importedFqName = importedFqName?.takeUnless { it.isRoot } ?: return null
    val importedPackageFqName = if (isAllUnder) importedFqName else importedFqName.parent()
    val importedName = importedFqName.shortName().takeUnless { isAllUnder }
    val exportedName = importedName?.let { aliasName ?: it }
    return CfirReexportImportInfo(
        importedPackageFqName = importedPackageFqName,
        importedName = importedName,
        exportedName = exportedName,
        isAllUnder = isAllUnder,
    )
}

/**
 * 从 PSI 或 light tree source 中解析 import 可见性关键字。
 */
private fun CjSourceElement?.importVisibilityKeyword(): String? {
    val directiveText = when (this) {
        is CjPsiSourceElement -> psi.findImportDirectiveText()
        is CjLightSourceElement -> lighterASTNode.findImportDirectiveText(this)
        else -> null
    } ?: return null

    val normalized = directiveText.trimStart()
    val match = IMPORT_VISIBILITY_PATTERN.find(normalized) ?: return null
    return match.groupValues[1]
}

/**
 * 沿 PSI 父链查找 import directive 源文本。
 */
private fun PsiElement.findImportDirectiveText(): String? {
    var current: PsiElement? = this
    while (current != null) {
        if (current.node?.elementType.toString() == IMPORT_DIRECTIVE_DEBUG_NAME) {
            return current.text
        }
        current = current.parent
    }
    return null
}

/**
 * 沿 light tree 父链查找 import directive 源文本。
 */
private fun LighterASTNode.findImportDirectiveText(source: CjLightSourceElement): String? {
    var current: LighterASTNode? = this
    while (current != null) {
        if (current.tokenType.toString() == IMPORT_DIRECTIVE_DEBUG_NAME) {
            return source.treeStructure.toString(current).toString()
        }
        current = source.treeStructure.getParent(current)
    }
    return null
}

/**
 * 匹配显式 import 可见性关键字的正则。
 */
private val IMPORT_VISIBILITY_PATTERN = Regex("^(public|protected|internal|private)\\s+import\\b")

/**
 * 会产生 reexport 的 import 可见性集合。
 */
private val REEXPORTING_IMPORT_VISIBILITIES = setOf(
    "public",
    "protected",
    "internal",
)

/**
 * PSI/light tree 中 import directive 节点的调试名。
 */
private const val IMPORT_DIRECTIVE_DEBUG_NAME = "IMPORT_DIRECTIVE"
