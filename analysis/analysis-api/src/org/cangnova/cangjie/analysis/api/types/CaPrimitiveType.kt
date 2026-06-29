package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind

/**
 * 仓颉基本类型(primitive type)公开模型。
 *
 * 表示语言内置的基础标量类型,例如各种 `Int*`、`UInt*`、`Float*`、`Bool`、`Rune`、`Unit`、`Nothing` 等。
 *
 * 设计要点:
 * - 基本类型不是普通的 class-like type,因此公开层不暴露 `classId` / `symbol` / `qualifiers`;
 * - 直接复用 CFIR 的强类型枚举 [PrimitiveTypeKind] 作为区分键,避免依赖弱语义字符串;
 * - 由 Analysis API 在 session 内构造,生命周期与其它 [CaType] 一致。
 *
 * 在 Kotlin Analysis API 中没有完全对应的类型——Kotlin 的基本类型被建模为普通 `KaUsualClassType`。
 * 仓颉这里把基本类型单独抽象出来更贴合仓颉编译器与 CFIR 的实际表示。
 */
abstract class CaPrimitiveType : CaType {
    /**
     * 该基本类型对应的种类标签,与 CFIR 的 [PrimitiveTypeKind] 对齐。
     */
    abstract val kind: PrimitiveTypeKind

    /**
     * 创建可恢复该基本类型的类型指针。
     */
    abstract override fun createPointer(): CaTypePointer<CaPrimitiveType>
}
