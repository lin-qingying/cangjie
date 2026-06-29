package org.cangnova.cangjie.analysis.api.impl.base.import

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.CaIdeApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImport
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImportPriority

/**
 * 单条默认导入的 Analysis API 值对象实现。
 */
@OptIn(CaIdeApi::class)
@CaImplementationDetail
class CaDefaultImportImpl(
    /**
     * 默认导入路径。
     */
    override val importPath: ImportPath,
    /**
     * 默认导入优先级。
     */
    override val priority: CaDefaultImportPriority,
) : CaDefaultImport {


    /**
     * 按公开接口语义比较默认导入路径和优先级。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CaDefaultImport
                && other.importPath == importPath
                && other.priority == priority
    }

    /**
     * 基于导入路径和优先级计算 hash。
     */
    override fun hashCode(): Int {
        var result = importPath.hashCode()
        result = 31 * result + priority.hashCode()
        return result
    }
}
