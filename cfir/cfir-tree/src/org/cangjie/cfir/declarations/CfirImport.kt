package org.cangjie.cfir.declarations

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 包声明。
 */
class CfirPackageDirective(
    val packageFqName: FqName,
    override val source: CfirSourceElement? = null,
) : CfirElement {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitPackageDirective(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirPackageDirective = this
}

/**
 * 导入声明。
 */
class CfirImport(
    /** 导入的全限定名 */
    val importedFqName: FqName,
    /** 是否为通配符导入（import a.b.*） */
    val isAllUnder: Boolean = false,
    /** 别名（import a.b.C as D 中的 D） */
    val aliasName: Name? = null,
    override val source: CfirSourceElement? = null,
) : CfirElement {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitImport(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirImport = this
}
