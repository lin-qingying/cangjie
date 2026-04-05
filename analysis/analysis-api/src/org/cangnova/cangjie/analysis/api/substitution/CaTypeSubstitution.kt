package org.cangnova.cangjie.analysis.api.substitution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

/**
 * Analysis API 对外暴露的类型替换器。
 *
 * 该模型表达的是“按类型参数名进行的语义替换快照”，而不是字符串层面的文本替换。
 * 它服务于签名实例化、调用结果呈现、文档渲染和后续平台工具统一消费。
 */
interface CaTypeSubstitutor : CaLifetimeOwner {
    /**
     * 当前替换器持有的类型参数替换表。
     *
     * key 使用稳定的类型参数名，value 使用当前 session 下的公开 [CaType]。
     */
    val substitutions: Map<Name, CaType>

    /**
     * 对公开类型执行语义替换。
     *
     * 如果类型中不包含任何可替换的类型参数，则应直接返回原语义类型。
     */
    fun substitute(type: CaType): CaType
}

/**
 * 已实例化签名的公开语义视图。
 *
 * 签名替换后既要保留原声明的结构信息，也要显式携带使用到的替换器，
 * 以便渲染器、LSP 和后续工具链观察“该签名是基于哪一组类型实参实例化得到的”。
 */
interface CaSubstitutedSignature : CaSignature {
    /**
     * 生成当前实例化签名所使用的类型替换器。
     */
    val substitutor: CaTypeSubstitutor

    /**
     * 未做替换前的原始声明签名。
     */
    val original: CaSignature
}
