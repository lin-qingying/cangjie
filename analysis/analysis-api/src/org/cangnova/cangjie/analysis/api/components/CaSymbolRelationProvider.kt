package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * 符号关系查询协议。
 *
 * 提供 containing declaration、override 链、子类关系、expect/actual 等"符号之间的语义关系",
 * 与 Kotlin Analysis API 的 `KaSymbolRelationProvider` 对齐。
 *
 * 设计要点:
 * - 把"关系计算"集中在 session component, 避免 symbol 叶子各自实现造成状态分裂;
 * - 只暴露语义事实, 不泄露 substitution override / fake override 等内部建模细节;
 * - 全部返回值均受 [CaLifetimeOwner] 约束, 不能逃逸出 analyze 块。
 */
interface CaSymbolRelationProvider : CaLifetimeOwner {
    /**
     * 语义上直接包含当前符号的声明符号。
     *
     * 对齐 Kotlin Analysis API 的 `KaSymbolRelationProvider.containingDeclaration`：
     * 该关系由 session component 统一计算，而不是由每个 symbol 叶子各自持有。
     */
    val CaSymbol.containingDeclaration: CaDeclarationSymbol?

    /**
     * 判断两个 symbol 在当前 session 下是否表示同一语义实体。
     *
     * 适用于跨 phase / 跨视图比较,实现细节可能涉及 mangled name、原始声明回溯等,
     * 但协议层只暴露布尔结果。
     */
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
     * 多个上层声明在当前 callable 处合并出的 intersection override 集合。
     */
    val CaCallableSymbol.intersectionOverriddenSymbols: List<CaCallableSymbol>

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

    /**
     * 返回 `actual` 声明对应的 `expect` 声明集合。
     *
     * 仓颉当前没有 expect/actual 语言语义，但框架接口与 Kotlin 对位保持一致。
     */
    fun CaDeclarationSymbol.getExpectsForActual(): List<CaDeclarationSymbol>
}

/**
 * 对齐 Kotlin Analysis API 的使用形态：
 * 在保留 session component 成员扩展的同时，补齐 `context(session)` 顶层 bridge，
 * 让调用方可以直接在 analyze 上下文中以自然语法访问符号关系能力。
 */
context(session: CaSession)
val CaSymbol.containingDeclaration: CaDeclarationSymbol?
    get() = with(session) { containingDeclaration }

/**
 * 顶层桥接:在 context [CaSession] 下判断两个 symbol 是否语义等价。
 */
context(session: CaSession)
fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean {
    return with(session) {
        isEquivalentTo(other)
    }
}

/**
 * 顶层桥接:获取当前 callable 直接覆盖到的声明序列。
 */
context(session: CaSession)
val CaCallableSymbol.directlyOverriddenSymbols: Sequence<CaCallableSymbol>
    get() = with(session) { directlyOverriddenSymbols }

/**
 * 顶层桥接:获取当前 callable 递归覆盖到的全部声明序列。
 */
context(session: CaSession)
val CaCallableSymbol.allOverriddenSymbols: Sequence<CaCallableSymbol>
    get() = with(session) { allOverriddenSymbols }

/**
 * 顶层桥接:判断 [superClass] 是否在当前类的继承链上。
 */
context(session: CaSession)
fun CaClassSymbol.isSubClassOf(superClass: CaClassSymbol): Boolean {
    return with(session) {
        isSubClassOf(superClass)
    }
}

/**
 * 顶层桥接:判断当前类是否直接继承自 [superClass]。
 */
context(session: CaSession)
fun CaClassSymbol.isDirectSubClassOf(superClass: CaClassSymbol): Boolean {
    return with(session) {
        isDirectSubClassOf(superClass)
    }
}

/**
 * 顶层桥接:返回当前 actual 声明对应的 expect 声明集合。
 */
context(session: CaSession)
fun CaDeclarationSymbol.getExpectsForActual(): List<CaDeclarationSymbol> {
    return with(session) {
        getExpectsForActual()
    }
}
