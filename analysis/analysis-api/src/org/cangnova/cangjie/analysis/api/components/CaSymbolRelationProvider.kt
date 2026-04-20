package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

interface CaSymbolRelationProvider : CaLifetimeOwner {
    fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean

    /**
     * 当前 callable 直接覆盖到的显式声明集合。
     *
     * 这里对齐 Kotlin Analysis `directlyOverriddenSymbols` 的职责边界：
     * 1. 只暴露语义上真实存在的上层声明。
     * 2. 不把底层 substitution override / fake override 细节泄漏给调用方。
     */
    val CaCallableSymbol.directlyOverriddenSymbols: Sequence<CaCallableSymbol>

    /**
     * 当前 callable 递归覆盖到的全部显式声明集合。
     *
     * 该结果以 `directlyOverriddenSymbols` 为 spine 递归展开，
     * 用于覆盖关系、文档恢复、导航与后续 usages 语义统一。
     */
    val CaCallableSymbol.allOverriddenSymbols: Sequence<CaCallableSymbol>

    /**
     * 判断当前类是否在继承链上继承自 [superClass]。
     *
     * 这里不把类自身视为自己的子类，以保持与 Kotlin Analysis 一致的关系语义。
     */
    fun CaClassSymbol.isSubClassOf(superClass: CaClassSymbol): Boolean

    /**
     * 判断当前类是否把 [superClass] 作为直接父类。
     *
     * 与 [isSubClassOf] 相同，这里同样不把类自身视为直接子类。
     */
    fun CaClassSymbol.isDirectSubClassOf(superClass: CaClassSymbol): Boolean
}

/**
 * 对齐 Kotlin Analysis API 的使用形态：
 * 在保留 session component 成员扩展的同时，补齐 `context(session)` 顶层 bridge，
 * 让调用方可以直接在 analyze 上下文中以自然语法访问符号关系能力。
 */
context(session: CaSession)
fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean {
    return with(session) {
        isEquivalentTo(other)
    }
}

context(session: CaSession)
val CaCallableSymbol.directlyOverriddenSymbols: Sequence<CaCallableSymbol>
    get() = with(session) { directlyOverriddenSymbols }

context(session: CaSession)
val CaCallableSymbol.allOverriddenSymbols: Sequence<CaCallableSymbol>
    get() = with(session) { allOverriddenSymbols }

context(session: CaSession)
fun CaClassSymbol.isSubClassOf(superClass: CaClassSymbol): Boolean {
    return with(session) {
        isSubClassOf(superClass)
    }
}

context(session: CaSession)
fun CaClassSymbol.isDirectSubClassOf(superClass: CaClassSymbol): Boolean {
    return with(session) {
        isDirectSubClassOf(superClass)
    }
}
