

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.declarations.CfirResolvedImport
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

class CfirResolvedImportImpl @CfirImplementationDetail constructor(
    override var condition: CfirExpression?,
    override var delegate: CfirImport,
    override val packageFqName: FqName,
) : CfirResolvedImport() {
    override val source: CjSourceElement?
        get() = delegate.source
    override val importedFqName: FqName?
        get() = delegate.importedFqName
    override val organizationName: Name?
        get() = delegate.organizationName
    override val isAllUnder: Boolean
        get() = delegate.isAllUnder
    override val aliasName: Name?
        get() = delegate.aliasName
    override val aliasSource: CjSourceElement?
        get() = delegate.aliasSource
    override val importedName: Name?
        get() = importedFqName?.shortName()

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        condition?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirResolvedImportImpl {
        transformCondition(transformer, data)
        return this
    }

    override fun <D> transformCondition(transformer: CfirTransformer<D>, data: D): CfirResolvedImportImpl {
        condition = condition?.transform(transformer, data)
        return this
    }

    override fun replaceCondition(newCondition: CfirExpression?) {
        condition = newCondition
    }
}
