/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping

/** 默认递归转换所有子节点的树级 phase transformer 基类。 */
abstract class CfirAbstractTreeTransformer<D>(phase: CfirResolvePhase) : CfirAbstractPhaseTransformer<D>(phase) {
    /** 默认实现为转换当前元素的所有子节点并返回同一元素。 */
    override fun <E : CfirElement> transformElement(element: E, data: D): E {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E)
    }

    /** 转换文件前执行 session 一致性检查和文件级异常包装。 */
    override fun transformFile(file: CfirFile, data: D): CfirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            super.transformFile(file, data)
        }
    }
}
