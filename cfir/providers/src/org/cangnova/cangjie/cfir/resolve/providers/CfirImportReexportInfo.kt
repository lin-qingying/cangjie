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
 */
internal data class CfirReexportImportInfo(
    val importedPackageFqName: FqName,
    val importedName: Name?,
    val exportedName: Name?,
    val isAllUnder: Boolean,
)

/**
 * 仓颉 `public/protected/internal import` 会把导入成员重导出；
 * 默认 `import` 等价于 `private import`，不参与重导出。
 */
fun CfirImport.isReexportingSourceImport(): Boolean =
    source.importVisibilityKeyword() in REEXPORTING_IMPORT_VISIBILITIES

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

private val IMPORT_VISIBILITY_PATTERN = Regex("^(public|protected|internal|private)\\s+import\\b")

private val REEXPORTING_IMPORT_VISIBILITIES = setOf(
    "public",
    "protected",
    "internal",
)

private const val IMPORT_DIRECTIVE_DEBUG_NAME = "IMPORT_DIRECTIVE"
