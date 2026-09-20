package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey

/**
 * Official CJO `FuncParamList.desugars` relation.
 *
 * The AST writer stores one desugared declaration reference parallel to each
 * parameter reference.  It is declaration metadata, not a second parameter
 * list, so it belongs on the canonical value-parameter node.
 */
public var CfirValueParameter.desugaredParameter: CfirValueParameter? by
    CfirDeclarationDataRegistry.data(DesugaredParameterKey)

private object DesugaredParameterKey : CfirDeclarationDataKey()
