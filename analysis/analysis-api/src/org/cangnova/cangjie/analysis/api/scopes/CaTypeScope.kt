package org.cangnova.cangjie.analysis.api.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.name.Name

/**
 * **具体类型**的成员作用域：其中的成员签名已按该类型的实际类型实参替换。
 *
 * 与 [CaScope] 的分工：`CaScope` 给出声明本身（未替换、保留原声明身份），
 * 本接口给出 use-site 视图——`Box<Int64>` 的 `value(x: T): T` 在这里是
 * `value(x: Int64): Int64`，而声明仍是 `Box.value` 同一个符号。
 * 需要「比较参数与返回类型」的消费方（补全签名渲染、适用性检查）必须走这里，
 * 只比较名字的消费方可以用 [declarationScope][org.cangnova.cangjie.analysis.api.components.CaTypeScopeProvider.declarationScope]。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeScope`（同样只暴露替换后的签名与 classifier，不暴露未替换的 `callables`）。
 *
 * 入口见 [org.cangnova.cangjie.analysis.api.components.CaTypeScopeProvider.typeScope]。
 */
@CaExperimentalApi
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaTypeScope : CaScopeLike {
    /**
     * 本作用域中签名匹配 [nameFilter] 的 callable 序列（类型参数已替换）。
     *
     * 实现需要先取名字全集再逐个处理；名字集合已知时应使用 [callableSignatures] 的集合重载。
     */
    fun callableSignatures(nameFilter: (Name) -> Boolean = { true }): Sequence<CaCallableSignature<*>>

    /**
     * 本作用域中签名匹配 [names] 的 callable 序列（类型参数已替换）。
     */
    fun callableSignatures(names: Collection<Name>): Sequence<CaCallableSignature<*>>

    /**
     * 本作用域中签名匹配 [names] 的 callable 序列（类型参数已替换）。
     */
    fun callableSignatures(vararg names: Name): Sequence<CaCallableSignature<*>> = withValidityAssertion {
        callableSignatures(names.toList())
    }

    /**
     * 本作用域中的 classifier 序列（嵌套类、内部类、嵌套 typealias 等）。
     */
    fun classifiers(nameFilter: (Name) -> Boolean = { true }): Sequence<CaClassifierSymbol>

    /**
     * 本作用域中名字匹配 [names] 的 classifier 序列。
     */
    fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol>

    /**
     * 本作用域中名字匹配 [names] 的 classifier 序列。
     */
    fun classifiers(vararg names: Name): Sequence<CaClassifierSymbol> = withValidityAssertion {
        classifiers(names.toList())
    }

    /**
     * 本作用域中的构造器序列。
     */
    val constructors: Sequence<CaConstructorSymbol>
}
