package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey

private object InReferenceCycleKey : CfirDeclarationDataKey()

private var CfirClassLikeDeclaration.inReferenceCycleAttribute: Boolean? by
    CfirDeclarationDataRegistry.data(InReferenceCycleKey)

/**
 * 官方 IN_REFERENCE_CYCLE 的真实循环成员状态。
 *
 * SUPER_TYPES 完成时发布；诊断可以由环外 extend 承载，该承载节点不因此成为循环成员。
 * 普通继承查询排除循环目标的成员，当前声明自己的成员和构造器依赖仍使用各自的投影。
 */
var CfirClassLikeDeclaration.isInReferenceCycle: Boolean
    get() = inReferenceCycleAttribute == true
    set(value) {
        inReferenceCycleAttribute = value
    }
