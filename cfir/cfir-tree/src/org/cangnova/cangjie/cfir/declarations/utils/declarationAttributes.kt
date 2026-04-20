package org.cangnova.cangjie.cfir.declarations.utils

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry

private object EvaluatedValue : CfirDeclarationDataKey()

var CfirVariable.evaluatedInitializer: CfirEvaluatorResult? by CfirDeclarationDataRegistry.data(EvaluatedValue)
