/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.analysis.checkers.type

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangjie.cfir.analysis.checkersComponent
import org.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.types.*
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.withFirEntry

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@OptIn(CheckersComponentInternal::class)
class CfirTypeCheckersDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
    private val checkers: CfirTypeCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: CfirSession, reporter: PendingDiagnosticReporter, mppKind: MppCheckerKind) : this(
        session,
        reporter,
        when (mppKind) {
            MppCheckerKind.Common -> session.checkersComponent.commonCfirTypeCheckers
            MppCheckerKind.Platform -> session.checkersComponent.platformCfirTypeCheckers
        }
    )

    override fun visitElement(element: CfirElement, data: CheckerContext) {
        if (element is CfirTypeRef) {
            error("${element::class.simpleName} should call parent checkers inside ${this::class.simpleName}")
        }
    }

    override fun visitTypeRef(typeRef: CfirTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(typeRef, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: CheckerContext) {
        checkers.allResolvedTypeRefCheckers.check(resolvedTypeRef, data)
    }

    override fun visitImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(implicitTypeRef, data)
    }

    override fun visitUserTypeRef(userTypeRef: CfirUserTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(userTypeRef, data)
    }

    override fun visitBasicTypeRef(basicTypeRef: CfirBasicTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(basicTypeRef, data)
    }

    override fun visitFunctionTypeRef(functionTypeRef: CfirFunctionTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(functionTypeRef, data)
    }

    override fun visitTupleTypeRef(tupleTypeRef: CfirTupleTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(tupleTypeRef, data)
    }

    override fun visitVArrayTypeRef(vArrayTypeRef: CfirVArrayTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(vArrayTypeRef, data)
    }

    private inline fun <reified E : CfirTypeRef> Array<CfirTypeChecker<E>>.check(
        element: E,
        context: CheckerContext
    ) {
        for (checker in this) {
            try {
                context(context, reporter) {
                    checker.check(element)
                }
            } catch (e: Exception) {
                rethrowExceptionWithDetails("Exception in type checkers", e) {
                    withFirEntry("element", element)
                    context.containingFilePath?.let { withEntry("file", it) }
                }
            }
        }
    }
}
