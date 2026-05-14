package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol

/**
 * 已成功解析的 class-like 限定段。
 *
 * 在 [CaClassTypeQualifier] sealed 层级中,此分支额外承诺:
 * - 当前段的名字已在作用域中找到具体的 [symbol];
 * - IDE 可以以该 symbol 作为跳转/查阅文档/获取签名信息的可靠目标。
 *
 * 注意:即使该段已解析,所属类型整体仍可能存在错误(参见 [CaClassErrorType]),
 * 此时只是 “这一段有 symbol,但整体类型不一定合法”。
 *
 * 对齐 Kotlin Analysis API 的 `KaResolvedClassTypeQualifier`。
 */
interface CaResolvedClassTypeQualifier : CaClassTypeQualifier {
    /**
     * 该段对应的分类符号,可指向 class / interface / struct / enum / type alias 等 classifier。
     */
    val symbol: CaClassifierSymbol
}
