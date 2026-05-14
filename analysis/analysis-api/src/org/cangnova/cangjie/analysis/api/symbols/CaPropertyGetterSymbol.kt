package org.cangnova.cangjie.analysis.api.symbols

/**
 * 属性 getter 符号。
 *
 * 表示属性的读路径，签名上无参数、返回属性类型。
 */
abstract class CaPropertyGetterSymbol : CaPropertyAccessorSymbol()
