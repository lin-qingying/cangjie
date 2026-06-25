/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping

/** 带解析阶段信息与 session 一致性检查的 CFIR phase transformer 基类。 */
abstract class CfirAbstractPhaseTransformer<D>(
    /** transformer 默认执行的解析阶段。 */
    val baseTransformerPhase: CfirResolvePhase,
) : CfirDefaultTransformer<D>(), SessionHolder {
    /** 当前 transformer 绑定的 CFIR session。 */
    abstract override val session: CfirSession

    init {
        require(baseTransformerPhase != CfirResolvePhase.RAW_CFIR) {
            "Raw CFIR building should not be done in phase transformer"
        }
    }

    /** 当前 transformer 实际推进的 resolve phase。 */
    open val transformerPhase: CfirResolvePhase
        get() = baseTransformerPhase

    /** 转换文件前检查 session 一致性，并把异常包装成文件分析异常。 */
    override fun transformFile(file: CfirFile, data: D): CfirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            super.transformFile(file, data)
        }
    }


    /** 校验待转换文件与 transformer session 属于同一 module data。 */
    protected fun checkSessionConsistency(file: CfirFile) {
        require(session.moduleData == file.moduleData) {
            "File ${file.name} and transformer ${this::class} have inconsistent sessions"
        }
    }
}
