package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.scopes.CfirClassScope
import org.cangnova.cangjie.cfir.scopes.CfirExtendScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope

/**
 * 由结构性 scope 暴露的查找来源。
 *
 * scope 只说明候选如何到达使用点，不在这里执行 private/internal/protected 判断。
 * 可见性结论始终由会话级 [CfirAccessibilityChecker] 基于完整使用点上下文给出。
 */
interface CfirLookupOriginScope {
    /** 当前 scope 产生候选时使用的结构性来源。 */
    val lookupOrigin: CfirLookupOrigin
}

/**
 * 把任意 scope 归一化为可访问性服务使用的结构性来源。
 *
 * import/package scope 显式携带来源；类型成员 scope 统一视为 member；局部、文件和
 * 类型参数 scope 属于词法查找。这里不根据声明种类或可见性猜测处置结果。
 */
fun CfirScope.lookupOriginForAccessibility(): CfirLookupOrigin = when (this) {
    is CfirLookupOriginScope -> lookupOrigin
    is CfirTypeScope,
    is CfirClassScope,
    is CfirExtendScope,
    -> CfirLookupOrigin.MEMBER

    else -> CfirLookupOrigin.LEXICAL
}
