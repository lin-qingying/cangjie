package org.cangnova.cangjie.cfir.references

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 对位 Kotlin FirStubReference：synthetic 表达式在进入真实候选 producer 前的无状态引用。
 * 不携带声明、候选、类型或源码完成能力；沿既有 Reference visitor/transformer 分派。
 */
object CfirStubReference : CfirReference {
    override val source: CjSourceElement? get() = null

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        require(newSource == null) { "A stub reference cannot hold a source." }
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirStubReference = this
}
