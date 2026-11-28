package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.source.CjFakeSourceElementKind

/** 声明在官方 CollectCJMPDecls 中的角色；不同于模块的编译模式。 */
enum class CfirCjmpDeclarationRole {
    COMMON,
    SPECIFIC,
}

/**
 * CJMP 对应物的共享资格判据。
 *
 * CFIR 显式 primary constructor 已是可调用表示，对应官方反糖后的 FuncDecl，不能按
 * isPrimary 排除；raw 与 CJMP 合并后的自动构造器都不是可配对的源码声明。enum 构造项的
 * 角色由两套 raw builder 从 enum 传播，CJO 则按原始属性位恢复。
 */
fun CfirDeclaration.isCjmpMatchingCandidate(role: CfirCjmpDeclarationRole): Boolean {
    val member = this as? CfirMemberDeclaration ?: return false
    if (this is CfirConstructor &&
        (origin == CfirDeclarationOrigin.ImplicitDefault || source?.kind == CjFakeSourceElementKind.ImplicitConstructor)
    ) return false
    return when (role) {
        CfirCjmpDeclarationRole.COMMON -> member.status.isCommon
        // 官方 CollectDecl 先判断 COMMON，再判断 SPECIFIC。
        CfirCjmpDeclarationRole.SPECIFIC -> !member.status.isCommon && member.status.isSpecific
    }
}
