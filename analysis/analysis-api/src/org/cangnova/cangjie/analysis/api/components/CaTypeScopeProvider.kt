package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.scopes.CaTypeScope
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 具体类型的作用域查询协议。
 *
 * 与 [CaScopeProvider.scope] 的分工是**替换与否**：
 * - [CaScopeProvider.scope] 把类型还原成 class-like 声明后取未替换视图——成员签名里仍是声明自身的类型参数；
 * - [typeScope] 保留实际类型实参——`Box<Int64>` 的成员签名是 `Int64`，`Box<String>` 的是 `String`。
 *
 * 仓颉偏差（记录在案）：Kotlin 复用同一名字 `KaType.scope: KaTypeScope?`；
 * 仓颉的 `CaType.scope: CaScope?` 已有既有调用方，按「先增加新入口、旧入口保持不动」的策略
 * 新入口取名 [typeScope]，旧入口的迁移另案。
 *
 * 对齐 Kotlin Analysis API 的 `KaScopeProvider.scope` / `KaTypeScope.declarationScope`。
 */
@CaExperimentalApi
@OptIn(CaExperimentalApi::class)
interface CaTypeScopeProvider : CaLifetimeOwner {
    /**
     * 该类型作为**具体类型**时的成员作用域；错误类型或无法构造成员视图时返回 `null`。
     *
     * 进入本入口的类型必须携带实际类型实参（`Box<Int64>`）；类型变量按其上界展开，
     * 交叉类型合并各分支的成员，ideal primitive 类型按 primitive extend 规则处理。
     */
    val CaType.typeScope: CaTypeScope?

    /**
     * 该类型作用域对应的**声明**视图：不替换类型参数，保留原声明身份。
     *
     * 需要「这条替换后的成员是哪个声明」时走这里做对照；只关心签名时不要用它。
     */
    val CaTypeScope.declarationScope: CaScope
}

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaTypeScopeProvider` 的 context bridge。
 */
@OptIn(CaExperimentalApi::class)
context(session: CaSession)
val CaType.typeScope: CaTypeScope?
    get() = with(session) { typeScope }

/**
 * 自动生成的 context 桥接。请勿手工修改。
 *
 * 对齐 Kotlin Analysis API `KaTypeScopeProvider` 的 context bridge。
 */
@OptIn(CaExperimentalApi::class)
context(session: CaSession)
val CaTypeScope.declarationScope: CaScope
    get() = with(session) { declarationScope }
