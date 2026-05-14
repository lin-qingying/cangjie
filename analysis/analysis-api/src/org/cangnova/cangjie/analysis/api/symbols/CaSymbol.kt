package org.cangnova.cangjie.analysis.api.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer

/**
 * Analysis API 对外暴露的公开符号根接口。
 *
 * 符号是仓颉语义模型的稳定承载体：
 * 1. 用统一方式表达“声明是什么”。
 * 2. 为类型、作用域、引用解析、渲染、指针恢复提供共同语义坐标。
 * 3. 隔离底层 CFIR/PSI 的具体实现细节，避免上层直接依赖后端对象。
 */
interface CaSymbol : CaLifetimeOwner {
    /**
     * 当前符号所属的 use-site 模块。
     */
    val containingModule: CaModule

    /**
     * 与当前符号对应的 PSI 元素。
     *
     * - 对源码符号必然非空；
     * - 对库符号、合成符号等其他 origin，可能为 `null`，由实现方按约定决定是否提供回链。
     */
    val psi: PsiElement?

    /**
     * 当前符号的来源（源码 / 库 / 合成 / 扩展等）。
     *
     * 与 [location] 正交：[origin] 关心"从哪里来"，[location] 关心"写在哪"。
     */
    val origin: CaSymbolOrigin

    /**
     * 创建当前符号的指针。
     *
     * 指针是跨 `analyze {}` 生命周期边界安全持有符号的唯一方式：
     * 跨 Session 时必须先序列化为指针、在新 Session 中再恢复。
     */
    fun createPointer(): CaSymbolPointer<CaSymbol>

    /**
     * 当前符号在源码结构中的声明位置（顶层 / class 体内 / extend 体内 / property 内 / 局部）。
     */
    val location: CaSymbolLocation
}

/**
 * 统一读取符号名称的公开入口。
 *
 * 只有实现了 [CaNamedSymbol] 的符号才具备稳定名称；其余符号返回 `null`。
 */
val CaSymbol.name: Name?
    get() = (this as? CaNamedSymbol)?.name

/**
 * 以指定 PSI 类型读取当前符号对应的 PSI；类型不匹配时抛出 [ClassCastException]。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbol.psi()`。
 */
inline fun <reified PSI : PsiElement> CaSymbol.psi(): PSI =
    psi as PSI

/**
 * 以指定 PSI 类型读取当前符号对应的 PSI；类型不匹配时返回 `null`。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbol.psiSafe()`。
 */
inline fun <reified PSI : PsiElement> CaSymbol.psiSafe(): PSI? =
    psi as? PSI

/**
 * 当前符号来源为源码时读取 PSI；非源码符号返回 `null`。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbol.sourcePsi()`。
 */
inline fun <reified PSI : PsiElement> CaSymbol.sourcePsi(): PSI? {
    if (origin != CaSymbolOrigin.SOURCE) return null

    return psi as PSI
}

/**
 * 当前符号来源为源码且 PSI 类型匹配时读取 PSI；否则返回 `null`。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbol.sourcePsiSafe()`。
 */
inline fun <reified PSI : PsiElement> CaSymbol.sourcePsiSafe(): PSI? {
    if (origin != CaSymbolOrigin.SOURCE) return null

    return psi as? PSI
}
