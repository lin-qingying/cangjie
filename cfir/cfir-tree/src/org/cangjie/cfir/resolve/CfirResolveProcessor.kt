package org.cangjie.cfir.resolve

import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.session.CfirSession

/**
 * 解析处理器接口。
 *
 * 每个处理器负责将声明从一个阶段解析到下一个阶段。
 * 参考 K2 FirResolveProcessor。
 */
interface CfirResolveProcessor {

    val fromPhase: CfirResolvePhase
    val toPhase: CfirResolvePhase

    fun process(target: CfirDeclaration, session: CfirSession)
}
