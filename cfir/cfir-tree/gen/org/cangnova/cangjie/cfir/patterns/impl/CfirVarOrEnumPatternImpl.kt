

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirVarOrEnumPatternImpl(
    override val source: CjSourceElement?,
    override val name: Name,
    override var bindingVariable: CfirPatternBindingVariable?,
) : CfirVarOrEnumPattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        bindingVariable?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirVarOrEnumPatternImpl {
        transformBindingVariable(transformer, data)
        return this
    }

    override fun <D> transformBindingVariable(transformer: CfirTransformer<D>, data: D): CfirVarOrEnumPatternImpl {
        bindingVariable = bindingVariable?.transform(transformer, data)
        return this
    }
}
