package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.name.CallableId

/**
 * 可调用声明(函数、属性、构造器等)的 light declaration 视图。
 *
 * 与 [CaLightClassLikeDeclaration] 对应,本接口表达"可被调用的声明级别",
 * 用于符号 / 引用扫描、文档抽取、IDE 列表渲染等场景。
 *
 * 注意:本视图只暴露 ID 与签名等稳定信息,不直接持有 PSI;
 * 真实 PSI 需通过 [origin][CaLightDeclaration.origin] 取得。
 */
interface CaLightCallableDeclaration : CaLightDeclaration {
    /**
     * 可调用声明的 [CallableId]。
     *
     * 对于不可寻址(例如局部 lambda、合成产物)的声明可能为 `null`。
     */
    val callableId: CallableId?

    /**
     * 与该 light callable 对齐的签名视图。
     *
     * 当后端尚无法稳定提供签名时返回 `null`。
     */
    val signature: CaCallableSignature<*>?
}
