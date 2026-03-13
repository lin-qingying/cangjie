package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider

val CfirSession.symbolProvider: CfirSymbolProvider by CfirSession.sessionComponentAccessor()

val CfirSession.cfirProvider: CfirProvider by CfirSession.sessionComponentAccessor()

val CfirSession.extendProvider: CfirExtendProvider by CfirSession.sessionComponentAccessor()
