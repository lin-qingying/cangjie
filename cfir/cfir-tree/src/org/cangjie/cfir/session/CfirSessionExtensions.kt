package org.cangjie.cfir.session

import org.cangjie.cfir.providers.CfirExtendProvider
import org.cangjie.cfir.providers.CfirProvider
import org.cangjie.cfir.providers.CfirSymbolProvider

val CfirSession.symbolProvider: CfirSymbolProvider by CfirSession.sessionComponentAccessor()

val CfirSession.cfirProvider: CfirProvider by CfirSession.sessionComponentAccessor()

val CfirSession.extendProvider: CfirExtendProvider by CfirSession.sessionComponentAccessor()
