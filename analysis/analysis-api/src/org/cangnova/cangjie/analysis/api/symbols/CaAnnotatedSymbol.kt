package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer

/**
 * 可承载注解的公开符号能力接口。
 *
 * 同时实现 [CaSymbol] 与 [CaAnnotated]：表明该符号既是分析 API 的语义视图，
 * 又能通过 [CaAnnotated] 暴露其上的注解集合。
 *
 * [createPointer] 重写为返回收窄到 [CaAnnotatedSymbol] 的指针，
 * 便于上层在跨 Session 恢复后仍然保留注解能力。
 */
interface CaAnnotatedSymbol : CaSymbol, CaAnnotated {
    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol>
}
