package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * class-like 类型公开模型,涵盖 class / interface / struct / enum / type alias 等所有以
 * [ClassId] 寻址的具名类型应用。
 *
 * - 既可能表示已完全解析的合法类型(子类 [CaUsualClassType]),
 *   也可能表示带错误的 class-like 类型(子类 [CaClassErrorType] 通过 [CaErrorType] 路径);
 * - 通过 [classId] 与 [symbol] 与 PSI/CFIR 内的 `ConeClassLikeType` 对齐,
 *   并以 [qualifiers] 还原源码中的多段限定形态(如 `Foo.Bar.Baz<T>`);
 * - sealed:子类型集合在 Analysis API 公开面上是封闭的,扩展须经版本演进。
 *
 * 对齐 Kotlin Analysis API 的 `KaClassType`。
 */
sealed class CaClassLikeType : CaType {
    /**
     * 类型对应类（含 type alias 与 enum 等)的全限定标识。
     *
     * 即使存在类型实参或多段限定,该 [ClassId] 仍指向最终的 class-like 声明。
     */
    abstract val classId: ClassId

    /**
     * 源码限定段序列,描述形如 `Outer.Inner.Leaf<T>` 的多段类型应用。
     *
     * 每个 [CaResolvedClassTypeQualifier] 对应一段限定及其在该段上的类型实参,
     * 便于 IDE 渲染、跳转到具体段的 symbol。
     */
    abstract val qualifiers: List<CaResolvedClassTypeQualifier>

    /**
     * 创建当前 class-like 类型的稳定跨 session 指针。
     */
    abstract override fun createPointer(): CaTypePointer<CaClassLikeType>

    /**
     * 实际传递给该 class-like 类型的类型实参列表,对应最末段的类型参数。
     *
     * 注意:这与 [symbol] 上声明的类型参数(type parameters)在概念上不同。
     */
    abstract val typeArguments: List<CaType>

    /**
     * 对应的 class-like 符号,可能为 `null` ——例如解析中途获取不到时(取决于具体子类语义)。
     *
     * 子类 [CaUsualClassType] 通常持有非空 symbol,[CaClassErrorType] 则放在 `candidateSymbols` 中。
     */
    abstract val symbol: CaClassLikeSymbol?
}
