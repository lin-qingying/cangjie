package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

/**
 * 分析层读取 callable 声明状态时使用的统一入口。
 *
 * 局部声明（函数体里的 `func`/`prop`/`let`）在 BODY_RESOLVE 阶段才被创建，从未经过
 * STATUS phase，因此永远没有 [org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus]；
 * 对它们而言 raw status 就是真实状态（可见性只会是 local，modality 也是创建时既定的）。
 *
 * 其余声明（源码成员、反序列化的库声明）仍要求 STATUS 已发布：effective modality/visibility
 * 只能来自解析结果，用 raw status 会读到未收敛的默认值。
 */
internal val CfirCallableSymbol<*>.symbolDeclarationStatus: CfirDeclarationStatus
    get() = if (cfir.isLocal) rawStatus else resolvedStatus
