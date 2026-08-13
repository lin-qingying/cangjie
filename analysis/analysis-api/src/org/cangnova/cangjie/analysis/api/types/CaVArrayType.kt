package org.cangnova.cangjie.analysis.api.types

/**
 * 仓颉定长数组类型（VArray type）。
 *
 * 表示形如 `VArray<T, N>` 的语言内建定长数组，其中 `N` 是编译期常量长度。
 * VArray 与标准库 `Array<T>` 语义不同：
 * - `Array<T>` 是引用语义的名义类型，走 [CaUsualClassType]；
 * - `VArray<T, N>` 是值语义的编译器内建结构类型，公开层以独立 type 形式建模，
 *   与元组类似不沿用 class-like type 的限定/符号视角。
 *
 * 在 Kotlin Analysis API 中并无直接对应类型，这是 Cangjie 相对 Kotlin 的偏差点之一。
 */
interface CaVArrayType : CaType {
    /**
     * 数组元素类型。
     */
    val elementType: CaType

    /**
     * 数组编译期定长大小。
     */
    val size: Long
}
