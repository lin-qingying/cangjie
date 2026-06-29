package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl

/**
 * decompiled file stub kind 推导器。
 *
 * `.cjo` 以 package 为二进制存储单位，反编译 PSI 需要保留来源文件列表，
 * 这样低层 API 能按 Kotlin multifile facade 的同一条路径查找 part callable。
 *
 * 因此 file kind 的判定规则统一收口在这里：
 * - 无顶层 callable: `File`
 * - 单文件顶层 callable: `Facade`
 * - 多文件顶层 callable: `MultifileClass`
 */
internal object DecompiledFileStubKinds {
    /**
     * 根据 package、来源文件列表和顶层 callable 状态推导反编译文件 stub kind。
     *
     * 没有顶层 callable 的 package 作为普通 file stub；存在顶层 callable 时，
     * 再根据参与 facade 的来源文件数量选择 simple facade 或 multifile facade。
     */
    fun inferKind(
        packageFqName: FqName,
        sourceFiles: List<String>,
        hasTopLevelCallables: Boolean,
    ): CangJieFileStubKind {
        if (!hasTopLevelCallables) {
            return CangJieFileStubKindImpl.File(packageFqName)
        }

        val facadePartSimpleNames = buildFacadePartSimpleNames(sourceFiles)
        return if (facadePartSimpleNames.size <= 1) {
            CangJieFileStubKindImpl.Facade(
                packageFqName = packageFqName,
                facadeFqName = packageFqName,
            )
        } else {
            CangJieFileStubKindImpl.MultifileClass(
                packageFqName = packageFqName,
                facadeFqName = packageFqName,
                facadePartSimpleNames = facadePartSimpleNames,
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

    /**
     * 从来源文件路径中提取可作为 facade part 的稳定短名。
     */
    private fun extractFacadePartSimpleName(filePath: String): String {
        val rawSimpleName = filePath
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBeforeLast('.')
            .ifBlank { filePath }
        return rawSimpleName.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_').ifBlank { "FacadePart" }
    }
}
