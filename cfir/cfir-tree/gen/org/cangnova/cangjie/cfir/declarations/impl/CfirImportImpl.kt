

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

class CfirImportImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val importedFqName: FqName?,
    override val organizationName: Name?,
    override val isAllUnder: Boolean,
    override val aliasName: Name?,
    override val aliasSource: CjSourceElement?,
    override var condition: CfirExpression?,
) : CfirImport() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        condition?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirImportImpl {
        transformCondition(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirImportImpl {
        condition = condition?.transform(transformer, data)
        return this
    }

    override fun replaceCondition(newCondition: CfirExpression?) {
        condition = newCondition
    }
}
