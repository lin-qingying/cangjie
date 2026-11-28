package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/** 携带调用候选的命名引用。 */
open class CfirNamedReferenceWithCandidate(
    /** 引用源位置。 */
    override var source: CjSourceElement?,
    /** 引用名称。 */
    override val name: Name,
    /** 与该引用绑定的候选。 */
    val candidate: Candidate
) : CfirNamedReferenceWithCandidateBase() {
    /** 源迁移保留当前候选与命中符号。 */
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        source = newSource
    }
    /** 当前候选对应的符号。 */
    override val candidateSymbol: CfirBasedSymbol<*>
        get() = candidate.symbol

    /** 当前引用是否表示错误候选。 */
    open val isError: Boolean get() = false

    /** 候选引用没有子节点需要访问。 */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    /** 候选仍拥有解析时的调用输入；语法图改写后保留的输入节点也必须更新来源。 */
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        candidate.callInfo.callSite.accept(visitor, data)
        candidate.callInfo.explicitReceiver?.accept(visitor, data)
        candidate.callInfo.arguments.forEach { it.accept(visitor, data) }
        candidate.callInfo.typeArguments.forEach { it.accept(visitor, data) }
    }

    /** 候选引用没有子节点需要转换，直接返回自身。 */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement {
        return this
    }
}

/** 携带候选与诊断的错误命名引用。 */
class CfirErrorReferenceWithCandidate(
    source: CjSourceElement?,
    name: Name,
    candidate: Candidate,
    /** 该错误引用对应的诊断。 */
    override val diagnostic: ConeDiagnostic
) : CfirNamedReferenceWithCandidate(source, name, candidate), CfirDiagnosticHolder {
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        super<CfirNamedReferenceWithCandidate>.acceptSourceChildren(visitor, data)
    }
    /** 错误候选引用固定返回 true。 */
    override val isError: Boolean get() = true

    /** 访问错误候选引用节点。 */
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitNamedReferenceWithCandidateBase(this, data)

    @Suppress("UNCHECKED_CAST")
    /** 转换错误候选引用节点。 */
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformNamedReferenceWithCandidateBase(this, data) as E
}
