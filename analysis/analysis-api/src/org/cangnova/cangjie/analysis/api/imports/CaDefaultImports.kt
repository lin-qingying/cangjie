package org.cangnova.cangjie.analysis.api.imports

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.name.FqName

/**
 * 当前分析上下文的默认导入集合。
 *
 * 默认导入属于 use-site session 语义的一部分，而不是某个单独文件私有的解析细节。
 * 因此 Analysis API 需要提供稳定的公开视图，供 resolver、补全、渲染和工具层共享。
 */
interface CaDefaultImports : CaLifetimeOwner {
    /**
     * 常规优先级的默认导入。
     *
     * 该集合已经包含语言级和平台级的常规默认导入。
     */
    val regularImports: List<ImportPath>

    /**
     * 低优先级默认导入。
     */
    val lowPriorityImports: List<ImportPath>

    /**
     * 被显式排除的默认导入目标。
     */
    val excludedImports: List<FqName>

    /**
     * 当前分析上下文最终生效的默认导入集合。
     */
    val allImports: List<ImportPath>
        get() = regularImports + lowPriorityImports
}
