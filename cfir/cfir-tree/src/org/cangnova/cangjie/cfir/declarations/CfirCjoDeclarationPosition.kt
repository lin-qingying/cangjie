package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey

/** 从 CJO 还原的声明源文件位置，行和列均为一基。 */
data class CfirCjoDeclarationPosition(
    /** CJO metadata 中的原始源码路径。 */
    val filePath: String,
    /** 原始源码行号。 */
    val line: Int,
    /** 原始源码列号。 */
    val column: Int,
)

private object CjoDeclarationPositionKey : CfirDeclarationDataKey()

/** CJO 携带的原始声明位置，供 common-part 报告和反序列化候选 note 共用。 */
var CfirDeclaration.cjoDeclarationPosition: CfirCjoDeclarationPosition?
    by CfirDeclarationDataRegistry.data(CjoDeclarationPositionKey)
