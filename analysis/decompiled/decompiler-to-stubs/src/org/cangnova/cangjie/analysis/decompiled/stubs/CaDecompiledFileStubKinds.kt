package org.cangnova.cangjie.analysis.decompiled.stubs

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl

/**
 * decompiled file stub kind 推导器。
 *
 * `.cjo` 仍然以 package 为二进制存储单位，但包头里的 `allFiles`
 * 说明“这个 package 是否由多个源文件共同提供顶层 callable”是可恢复的事实。
 *
 * 因此 file kind 的判定规则必须统一收口在这里：
 * - 无顶层 callable: `File`
 * - 单文件顶层 callable: `Facade`
 * - 多文件顶层 callable: `MultifileClass`
 */
internal object CaDecompiledFileStubKinds {
    fun inferKind(
        packageFqName: FqName,
        sourceFiles: List<String>,
        hasTopLevelCallables: Boolean,
    ): CangJieFileStubKind {
        if (!hasTopLevelCallables) {
            return CangJieFileStubKindImpl.File(packageFqName)
        }

        val facadePartNames = buildFacadePartSimpleNames(sourceFiles)
        return if (facadePartNames.size > 1) {
            CangJieFileStubKindImpl.MultifileClass(
                packageFqName = packageFqName,
                facadeFqName = packageFqName,
                facadePartSimpleNames = facadePartNames,
            )
        } else {
            CangJieFileStubKindImpl.Facade(
                packageFqName = packageFqName,
                facadeFqName = packageFqName,
            )
        }
    }

    /**
     * 为 multifile facade 生成稳定且唯一的 part simple names。
     *
     * 不能简单对去掉扩展名后的文件名做去重，否则不同目录下的同名文件
     * 会被压成同一个 part，进而把真实的 multifile facade 误判成 simple facade。
     */
    fun buildFacadePartSimpleNames(filePaths: List<String>): List<String> {
        val seenBaseNames = linkedMapOf<String, Int>()
        return filePaths
            .filter(String::isNotBlank)
            .distinct()
            .map { filePath ->
                val baseName = extractFacadePartSimpleName(filePath)
                val occurrence = (seenBaseNames[baseName] ?: 0) + 1
                seenBaseNames[baseName] = occurrence
                if (occurrence == 1) baseName else "${baseName}_$occurrence"
            }
    }

    private fun extractFacadePartSimpleName(filePath: String): String {
        val rawSimpleName = filePath
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBeforeLast('.')
            .ifBlank { filePath }
        return rawSimpleName.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_').ifBlank { "FacadePart" }
    }
}
