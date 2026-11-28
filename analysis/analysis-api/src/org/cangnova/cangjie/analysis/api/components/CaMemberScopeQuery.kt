package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.scopes.CaScope

/**
 * 成员访问的使用点形态。
 *
 * 仓颉的成员访问存在两条语义不同的通道：**类型名限定**只能看到静态成员，
 * **接收者**访问看到实例成员（含继承链与适用 extend）。补全必须先判定使用点形态，
 * 再决定候选集合；把两条通道混为一个 `CaScope` 会给出「补出来必然报错」的候选。
 */
@CaExperimentalApi
enum class CaTypeAccessKind {
    /** `receiver.member`：实例访问。 */
    INSTANCE_THROUGH_RECEIVER,

    /** `Type.member`：类限定访问，只看静态成员。 */
    STATIC_THROUGH_TYPE_NAME,

    /** `this.member` / `super.member`：有显式接收者语义的实例访问。 */
    THIS_OR_SUPER,

    /** extend 声明体内的成员使用点。 */
    EXTEND_MEMBER,
}

/**
 * 单个成员作用域视图的查询结论。
 *
 * [scope] 为 `null` 表示该通道在此类型上不可用（例如没有静态成员），
 * 调用方据此跳过该通道而不是遍历一个空作用域。
 */
@CaExperimentalApi
interface CaMemberScopeQuery : CaLifetimeOwner {
    /** 该通道的成员作用域；不可用时为 `null`。 */
    val scope: CaScope?

    /** 该通道的结构性来源。 */
    val kind: CaScopeKind

    /**
     * 该通道的成员是否通过类型名限定访问。
     *
     * 为 `true` 时插入文本不得携带接收者表达式，也不得补出 `receiver.` 前缀。
     */
    val accessedThroughTypeName: Boolean

    /**
     * 该通道的成员是否必须配合接收者才能解析。
     *
     * 与 [accessedThroughTypeName] 互斥：静态通道为前者，实例通道为后者。
     */
    val requiresReceiver: Boolean
}

/**
 * 成员作用域查询结论的标准实现。
 */
@CaExperimentalApi
data class CaMemberScopeQueryData(
    /**
     * 生命周期令牌，取自构造查询的会话。
     *
     * 查询对象持有会话绑定的 [CaScope]，因此必须携带同一令牌：
     * 否则它会在会话结束后继续流通，把越界访问推迟到真正解引用作用域时才暴露。
     */
    override val token: CaLifetimeToken,
    override val scope: CaScope?,
    override val kind: CaScopeKind,
    override val accessedThroughTypeName: Boolean,
    override val requiresReceiver: Boolean,
) : CaMemberScopeQuery