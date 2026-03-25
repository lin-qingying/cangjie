package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

/**
 * Mirrors Kotlin FIR's `FirThisOwnerSymbol`.
 */
sealed class CfirThisOwnerSymbol<out D : CfirDeclaration> : CfirSymbol<D>()
