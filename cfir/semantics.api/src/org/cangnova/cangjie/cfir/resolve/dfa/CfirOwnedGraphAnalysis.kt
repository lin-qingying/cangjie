package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.copy.CfirOwnedGraphCopyResult
import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirOwnedGraphAnalysisReadiness
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.session.CfirSession
import java.util.Collections

/**
 * 解析态复制的真实词法宿主。文件是已完成解析的本代产品；重建在其实际声明/参数/局部作用域
 * 位置应用 old→new 映射，不从 Return target、名称或源码位置猜测宿主。
 */
class CfirResolvedGraphOwnerContext(files: Collection<CfirFile>) {
    val files: List<CfirFile> = Collections.unmodifiableList(files.toList())
    val session: CfirSession

    init {
        require(this.files.isNotEmpty()) { "Resolved graph rebuilding requires its actual containing files" }
        session = this.files.first().moduleData.session
        require(this.files.all { it.moduleData.session === session }) { "Analysis owner context cannot cross sessions" }
    }
}

/** resolve 发布的完成能力；不能用另一棵同名图或任意非空 CFG 替代此产品。 */
interface CfirCompletedOwnedGraphAnalysis : CfirOwnedGraphAnalysisReadiness {
    val roots: List<CfirElement>
    val requiredOwners: List<CfirControlFlowGraphOwner>
    override fun requireReady(root: CfirElement, session: CfirSession)
    fun graphOf(root: CfirElement): CfirControlFlowGraphReference
}

/**
 * 新身份已经保留类型和 binding 后，由 resolve 完整驱动 DFA/CFG 事件。
 * RAW 图继续进入正常 TYPES/STATUS/BODY；本入口不进行符号解析、推断或 phase 回退。
 */
interface CfirOwnedGraphAnalysisRebuilder {
    fun rebuild(copy: CfirOwnedGraphCopyResult, ownerContext: CfirResolvedGraphOwnerContext): CfirCompletedOwnedGraphAnalysis
}
