package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name

/**
 * 调用信息的跨模块抽象。
 *
 * 诊断层、候选层和 resolve 具体实现之间通过该对象传递调用语义，避免诊断映射反向依赖
 * resolve 模块中的具体 `CallInfo` 实现。
 */
abstract class AbstractCallInfo {
    /** 当前调用在 CFIR 树中的语法节点。 */
    abstract val callSite: CfirElement

    /** 当前调用解析使用的名称。 */
    abstract val name: Name

    /** 当前调用是否由隐式 `invoke` 形式产生。 */
    abstract val isImplicitInvoke: Boolean

    /**
     * 当前调用语法上是否携带显式类型实参。
     *
     * 诊断映射层只能依赖语义层抽象，不能反向读取 resolve 模块的具体
     * `CallInfo.typeArguments`。该标记用于区分显式泛型调用中的参数错误
     * 和真正的隐式泛型推断失败，避免把具体参数类型错误泛化为推断错误。
     */
    abstract val hasExplicitTypeArguments: Boolean

    /**
     * 当前调用语法上携带的显式类型实参。
     *
     * 对齐 Kotlin `CallInfo.typeArguments` 的调用信息职责；诊断层只依赖
     * `AbstractCallInfo`，因此显式类型实参必须跟随调用语义信息跨模块传递。
     */
    abstract val typeArguments: List<CfirTypeRef>

    /**
     * 当前调用的来源语义。
     *
     * operator、构造委托、intrinsic 等调用来源会影响诊断归类；诊断层只能依赖
     * 语义层抽象，因此调用来源必须随 `AbstractCallInfo` 跨模块传递。
     */
    abstract val origin: CfirFunctionCallOrigin

    /** 调用语法中的显式接收者表达式。 */
    abstract val explicitReceiver: CfirExpression?

    /** 调用语法中的值实参表达式列表。 */
    abstract val arguments: List<CfirExpression>
}
