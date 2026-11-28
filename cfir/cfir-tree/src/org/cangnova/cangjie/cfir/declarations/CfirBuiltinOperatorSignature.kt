package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorSignature

/** primitive scope 提供的合成运算能力；普通用户函数没有这项绑定事实。 */
private object CfirBuiltinOperatorSignatureKey : CfirDeclarationDataKey()

/** 与 synthetic operator 声明一同复制，使 scope 候选和直接内建路径共享同一运算身份。 */
var CfirNamedFunction.builtinOperatorSignature: BuiltinPrimitiveOperatorSignature? by
    CfirDeclarationDataRegistry.data(CfirBuiltinOperatorSignatureKey)
