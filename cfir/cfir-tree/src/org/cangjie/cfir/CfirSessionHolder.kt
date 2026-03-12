/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangjie.cfir

import org.cangjie.cfir.scopes.CfirScopeSession
import org.cangjie.cfir.session.CfirSession

interface CfirSessionHolder {
    val session: CfirSession
}

interface CfirScopeSessionHolder {
    val scopeSession: CfirScopeSession
}

interface CfirSessionAndScopeSessionHolder : CfirSessionHolder, CfirScopeSessionHolder
