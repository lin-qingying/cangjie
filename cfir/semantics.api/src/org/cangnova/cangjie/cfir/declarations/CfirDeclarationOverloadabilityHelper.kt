package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

/**
 * 声明参数签名的共享判定接口，对位 Kotlin FIR semantics.api 的同角色 helper。
 *
 * 两个候选来自同一 normalized 名字组，包含独立分类的 enum 构造项。各自 owner 映射
 * 由声明/继承输入提供，先投影到同一逻辑参数域；缺失投影不能以空映射替代。
 * 函数自有 header 按位置映射，构造器借用的名义参数不是自有 header。
 * general 非函数重定义、跨文件 private 豁免及候选删除都不属于签名判定。
 */
interface CfirDeclarationOverloadabilityHelper : CfirSessionComponent {
    fun isConflicting(
        a: CfirCallableSymbol<*>,
        b: CfirCallableSymbol<*>,
        aOwnerSubstitutor: ConeSubstitutor,
        bOwnerSubstitutor: ConeSubstitutor,
    ): Boolean
}

val CfirSession.declarationOverloadabilityHelper: CfirDeclarationOverloadabilityHelper
    by CfirSession.sessionComponentAccessor()
