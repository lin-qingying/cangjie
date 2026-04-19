/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

abstract class BeforeElementDiagnosticCollectionHandler: CfirSessionComponent {
    open fun beforeCollectingForElement(element: CfirElement) {}
    open fun beforeGoingNestedDeclaration(declaration: CfirDeclaration, context: CheckerContext) {}
}

val CfirSession.beforeElementDiagnosticCollectionHandler: BeforeElementDiagnosticCollectionHandler? by CfirSession.nullableSessionComponentAccessor()
