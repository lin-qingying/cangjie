

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkersComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

@OptIn(CheckersComponentInternal::class)
class TypeCheckersDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
    private val checkers: TypeCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: CfirSession, reporter: PendingDiagnosticReporter) : this(
        session,
        reporter,
        session.checkersComponent.typeCheckers
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

    override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: CheckerContext) {
        checkers.allResolvedTypeRefCheckers.check(errorTypeRef, data)
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
                if (shouldIjPlatformExceptionBeRethrown(e)) throw e
                rethrowExceptionWithDetails("Exception in type checkers", e) {
                    withCfirEntry("element", element)
                    context.containingFilePath?.let { withEntry("file", it) }
                }
            }
        }
    }
}
