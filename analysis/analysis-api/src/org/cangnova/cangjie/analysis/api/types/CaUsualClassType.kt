package org.cangnova.cangjie.analysis.api.types

/**
 * 普通 class-like 类型。
 *
 * 表示一个成功解析且非函数类型的 class-like 应用,例如 `String`、`Array<Int64>`、`MyClass<T>` 等,
 * 涵盖 class / interface / struct / enum / type alias 应用等常见情形。
 *
 * 这是 [CaClassLikeType] 在 Analysis API 中最常见的具体子类:对应已能找到 [symbol] 的合法类型;
 * 错误形态请参见 [CaClassErrorType]。
 *
 * 对齐 Kotlin Analysis API 的 `KaUsualClassType`。
 */
abstract class CaUsualClassType : CaClassLikeType() {
    abstract override fun createPointer(): CaTypePointer<CaUsualClassType>
}
