/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

interface CfirSessionHolder {
    val session: CfirSession
}

interface CfirScopeSessionHolder {
    val scopeSession: CfirScopeSession
}

interface CfirSessionAndScopeSessionHolder : CfirSessionHolder, CfirScopeSessionHolder
