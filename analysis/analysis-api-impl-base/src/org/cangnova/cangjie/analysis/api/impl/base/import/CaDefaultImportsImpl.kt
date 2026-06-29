package org.cangnova.cangjie.analysis.api.impl.base.import

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImport
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports

/**
 * 默认导入集合的 Analysis API 值对象实现。
 */
@CaImplementationDetail
class CaDefaultImportsImpl(
    /**
     * 可见默认导入列表。
     */
    override val defaultImports: List<CaDefaultImport>,
    /**
     * 从默认导入中排除的路径列表。
     */
    override val excludedFromDefaultImports: List<ImportPath>
) : CaDefaultImports {
    /**
     * 按公开接口语义比较默认导入和排除列表。
     */
    override fun equals(other: Any?): Boolean {
        return this === other
                || other is CaDefaultImports
                && other.defaultImports == defaultImports
                && other.excludedFromDefaultImports == excludedFromDefaultImports
    }

    /**
     * 基于默认导入和排除列表计算 hash。
     */
    override fun hashCode(): Int {
        var result = defaultImports.hashCode()
        result = 31 * result + excludedFromDefaultImports.hashCode()
        return result
    }
}
