package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol

/**
 * class-like 错误类型。
 *
 * 表示在源码中写出的某个 class-like 类型(`Foo.Bar<T>`)未能成功解析,但前端仍记录了
 * 可用的部分信息,以便 IDE 给出更精准的诊断与补全候选。
 *
 * 与一般 [CaErrorType] 的差异在于:
 * - 通过 [qualifiers] 暴露逐段限定信息,其中部分段可能已成功解析为 [CaResolvedClassTypeQualifier];
 * - 通过 [candidateSymbols] 暴露在错误发生前考虑过的候选类型符号(例如重载/同名歧义/参数个数不匹配)。
 *
 * 对齐 Kotlin Analysis API 的 `KaClassErrorType`。
 */
abstract class CaClassErrorType : CaErrorType {
    /**
     * 源码中的限定段序列。
     *
     * 每段为 [CaClassTypeQualifier],可能是 [CaResolvedClassTypeQualifier] 或 [CaUnresolvedClassTypeQualifier],
     * 取决于错误发生在哪个段上。
     */
    abstract val qualifiers: List<CaClassTypeQualifier>

    /**
     * 在解析失败前曾被纳入考虑的候选 class-like 符号集合。
     *
     * 用于诊断/补全:即使最终未选中任何候选,这些符号也可以帮助渲染 “did you mean...” 之类的提示。
     */
    abstract val candidateSymbols: Collection<CaClassLikeSymbol>

    /**
     * 创建可恢复该 class-like 错误类型的类型指针。
     */
    abstract override fun createPointer(): CaTypePointer<CaClassErrorType>
}
