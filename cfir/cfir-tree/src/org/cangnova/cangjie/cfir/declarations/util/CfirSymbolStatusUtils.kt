package org.cangnova.cangjie.cfir.declarations.util

import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

inline val CfirCallableSymbol<*>.isOperator: Boolean get() = resolvedStatus.isOperator
inline val CfirCallableSymbol<*>.isMut: Boolean get() = resolvedStatus.isMut
