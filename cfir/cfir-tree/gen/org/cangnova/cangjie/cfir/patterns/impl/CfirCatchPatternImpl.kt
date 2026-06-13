

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

class CfirCatchPatternImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val bindingName: Name?,
    override val isWildcard: Boolean,
    override val typeRefs: MutableList<CfirTypeRef>,
    override var bindingVariable: CfirPatternBindingVariable?,
) : CfirCatchPattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        typeRefs.forEach { it.accept(visitor, data) }
        bindingVariable?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirCatchPatternImpl {
        transformTypeRefs(transformer, data)
        transformBindingVariable(transformer, data)
        return this
    }

    override fun <D> transformTypeRefs(transformer: CfirTransformer<D>, data: D): CfirCatchPatternImpl {
        typeRefs.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformBindingVariable(transformer: CfirTransformer<D>, data: D): CfirCatchPatternImpl {
        bindingVariable = bindingVariable?.transform(transformer, data)
        return this
    }
}
