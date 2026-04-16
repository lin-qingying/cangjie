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
    public val psi: PsiElement?

    /**
     * 当前符号的来源。
     */
    val origin: CaSymbolOrigin
    public fun createPointer(): CaSymbolPointer<CaSymbol>

    /**
     * 当前符号在源码结构中的声明位置。
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
