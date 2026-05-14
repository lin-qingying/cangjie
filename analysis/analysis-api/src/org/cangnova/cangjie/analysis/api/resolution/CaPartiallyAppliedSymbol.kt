package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol


/**
 * 已经"部分应用"的可调用符号:绑定了 receiver 与类型实参,但还未携带完整的实参或访问模式。
 *
 * - 对函数,缺失的部分是值实参与上下文实参;
 * - 对属性 / 变量,缺失的部分是访问模式(读 / 写 / 复合访问)。
 *
 * 该接口是 [CaCallableMemberCall.partiallyAppliedSymbol] 的载体,
 * 也可单独使用,用于"得到候选符号但暂不真正发出调用"的场景。
 *
 * 对齐 Kotlin Analysis API 的 `KaPartiallyAppliedSymbol`。
 *
 * @param S 实际可调用符号类型(协变)。
 * @param C 与符号匹配的签名类型(协变)。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaPartiallyAppliedSymbol<out S : CaCallableSymbol, out C : CaCallableSignature<S>> : CaLifetimeOwner {
    /**
     * 函数或变量声明的 use-site 签名。
     */
    val signature: C

    /**
     * 该符号访问对应的 [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers)。
     *
     * 仅当目标声明位于某个类型内部时存在。
     */
    val dispatchReceiver: CaReceiverValue?




}

/**
 * 针对函数族的 [CaPartiallyAppliedSymbol] 别名。
 */
typealias CaPartiallyAppliedFunctionSymbol<S> = CaPartiallyAppliedSymbol<S, CaFunctionSignature<S>>

/**
 * 针对变量族的 [CaPartiallyAppliedSymbol] 别名。
 */
typealias CaPartiallyAppliedVariableSymbol<S> = CaPartiallyAppliedSymbol<S, CaVariableSignature<S>>
