package org.cangnova.cangjie.analysis.api.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * 符号指针。
 *
 * 指针是跨 `analyze {}` 生命周期边界传递符号的唯一合法方式：
 * - 符号本身只在创建它的 [CaSession] 中有效；
 * - 当 Session 关闭后，符号引用即作废；
 * - 跨 Session 的传递与缓存必须先序列化为指针，下次在新 Session 上 [restoreSymbol] 还原。
 *
 * 对齐 Kotlin Analysis API 的 `KaSymbolPointer`。
 *
 * @param S 指针恢复后产出的具体符号类型（协变），便于在不同符号族上得到收窄的回链。
 */
interface CaSymbolPointer<out S : CaSymbol> {
    /**
     * 在指定 [session] 中恢复对应的符号；若符号已经不存在（声明被删除等）则返回 `null`。
     */
    fun restoreSymbol(session: CaSession): S?
}
