package org.cangnova.cangjie.analysis.api.symbols

/**
 * 类型分类器（classifier）符号根接口。
 *
 * "classifier" 这个概念覆盖所有能产生类型的实体：
 * - class-like 声明（[CaClassLikeSymbol] -> [CaClassSymbol] / [CaTypeAliasSymbol]）；
 * - 类型参数（[CaTypeParameterSymbol]）。
 *
 * 与 [CaCallableSymbol] 互为对称：前者描述"是一种类型"，后者描述"是一种可调用实体"。
 */
interface CaClassifierSymbol : CaDeclarationSymbol
