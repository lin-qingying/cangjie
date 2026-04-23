package org.cangnova.cangjie.analysis.api.imports

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaIdeApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.FqName

/**
 * 当前分析上下文的默认导入集合。
 *
 * 默认导入属于 use-site session 语义的一部分，而不是某个单独文件私有的解析细节。
 * 因此 Analysis API 需要提供稳定的公开视图，供 resolver、补全、渲染和工具层共享。
 */
interface CaDefaultImports  {
    /**
     * A list of [ImportPath] with [CaDefaultImportPriority] that represents a list of imports which are implicitly present
     * by default in every file.
     *
     * Some of these imports are star imports, and from them, we exclude some specific paths. This information is present in [excludedFromDefaultImports].
     */
    public val defaultImports: List<CaDefaultImport>

    /**
     * A list of non-star import paths that are excluded from some star default imports provided by [defaultImports].
     */
    public val excludedFromDefaultImports: List<ImportPath>
}
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaDefaultImport {
    /**
     * The path that is imported by default.
     *
     * It may be a star import if [ImportPath.isAllUnder] is `true`, or a non-star import if `false`.
     */
    public val importPath: ImportPath

    /**
     * Represents the priority of the current default import.
     *
     * @see [CaDefaultImportPriority]
     */
    @OptIn(CaIdeApi::class)
    public val priority: CaDefaultImportPriority
}
/**
 * Represents the priority of a default import.
 *
 * In the case of name conflicts, higher priority wins during resolution.
 */
@CaIdeApi
public enum class CaDefaultImportPriority {
    LOW,
    HIGH,
}