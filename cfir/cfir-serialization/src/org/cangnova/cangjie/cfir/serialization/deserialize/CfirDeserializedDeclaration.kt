package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/** 同一个 CJO wire 条目的声明树与符号视图；全局 VarDecl 的 wire 符号属于真实绑定叶子。 */
data class CfirDeserializedDeclaration(
    val declarationRoot: CfirDeclaration,
    val wireSymbol: CfirBasedSymbol<*>,
)
