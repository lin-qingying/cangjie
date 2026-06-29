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
     * 当前会话所适用的默认 import 列表(每项携带优先级)。
     *
     * 其中部分可能是 star import,需结合 [excludedFromDefaultImports] 排除特定路径。
     */
    val defaultImports: List<CaDefaultImport>

    /**
     * 应从 [defaultImports] 中的 star import 内排除的具体路径(非 star import 路径)。
     */
    val excludedFromDefaultImports: List<ImportPath>
}
@SubclassOptInRequired(CaImplementationDetail::class)
/**
 * 单条默认 import 及其优先级。
 */
interface CaDefaultImport {
    /**
     * 该默认 import 实际指向的导入路径。
     *
     * 当 [ImportPath.isAllUnder] 为 `true` 时表示 star import,否则为单符号 import。
     */
    val importPath: ImportPath

    /**
     * 当前默认 import 的优先级。
     *
     * 命名冲突时,优先级高的胜出。
     *
     * @see [CaDefaultImportPriority]
     */
    @OptIn(CaIdeApi::class)
    val priority: CaDefaultImportPriority
}
/**
 * 默认 import 的优先级。
 *
 * 命名冲突时优先级高的胜出;主要用于区分 builtins 默认 import 与项目自定义默认 import。
 */
@CaIdeApi
enum class CaDefaultImportPriority {
    /** 低优先级,通常用于框架/扩展提供的默认 import。 */
    LOW,

    /** 高优先级,通常用于语言内置默认 import(builtins)。 */
    HIGH,
}
