package org.cangnova.cangjie.analysis.api.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase

/**
 * 低层 CFIR facade 的补充工具。
 *
 * 这里保留与 phase/file 相关的共享工具入口，避免这些辅助逻辑继续散落在
 * `analysis-api-cfir` 或测试框架中。
 */
/**
 * 低层 CFIR facade 的补充工具。
 *
 * 这里保留与 phase/file 相关的共享工具入口，避免这些辅助逻辑继续散落在
 * `analysis-api-cfir` 或测试框架中。
 */
object CaCfirResolveFacade {
    fun areFilesResolvedTo(cfirFiles: List<CfirFile>, phase: CfirResolvePhase): Boolean {
        return cfirFiles.all { it.resolvePhase >= phase }
    }
}
