package org.cangnova.cangjie.cfir.declarations.utils

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry

/**
 * 变量常量求值结果在声明属性表中的键。
 */
private object EvaluatedValue : CfirDeclarationDataKey()

/**
 * 变量初始化表达式的常量求值结果。
 */
var CfirVariable.evaluatedInitializer: CfirEvaluatorResult? by CfirDeclarationDataRegistry.data(EvaluatedValue)
