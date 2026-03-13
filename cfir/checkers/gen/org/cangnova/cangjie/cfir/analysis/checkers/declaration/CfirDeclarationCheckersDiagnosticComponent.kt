/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.analysis.checkers.declaration

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangjie.cfir.analysis.checkersComponent
import org.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.withFirEntry

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@OptIn(CheckersComponentInternal::class)
class CfirDeclarationCheckersDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
    private val checkers: CfirDeclarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: CfirSession, reporter: PendingDiagnosticReporter, mppKind: MppCheckerKind) : this(
        session,
        reporter,
        when (mppKind) {
            MppCheckerKind.Common -> session.checkersComponent.commonCfirDeclarationCheckers
            MppCheckerKind.Platform -> session.checkersComponent.platformCfirDeclarationCheckers
        }
    )

    override fun visitElement(element: CfirElement, data: CheckerContext) {
        if (element is CfirDeclaration) {
            error("${element::class.simpleName} should call parent checkers inside ${this::class.simpleName}")
        }
    }

    override fun visitDeclaration(declaration: CfirDeclaration, data: CheckerContext) {
        checkers.allBasicDeclarationCheckers.check(declaration, data)
    }

    override fun visitMemberDeclaration(memberDeclaration: CfirMemberDeclaration, data: CheckerContext) {
        checkers.allMemberDeclarationCheckers.check(memberDeclaration, data)
    }

    override fun visitCallableDeclaration(callableDeclaration: CfirCallableDeclaration, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(callableDeclaration, data)
    }

    override fun visitClassLikeDeclaration(classLikeDeclaration: CfirClassLikeDeclaration, data: CheckerContext) {
        checkers.allClassLikeCheckers.check(classLikeDeclaration, data)
    }

    override fun visitClass(klass: CfirClass, data: CheckerContext) {
        checkers.allClassCheckers.check(klass, data)
    }

    override fun visitFile(file: CfirFile, data: CheckerContext) {
        checkers.allFileCheckers.check(file, data)
    }

    override fun visitFunction(function: CfirFunction, data: CheckerContext) {
        checkers.allFunctionCheckers.check(function, data)
    }

    override fun visitMainFunction(mainFunction: CfirMainFunction, data: CheckerContext) {
        checkers.allMainFunctionCheckers.check(mainFunction, data)
    }

    override fun visitProperty(property: CfirProperty, data: CheckerContext) {
        checkers.allPropertyCheckers.check(property, data)
    }

    override fun visitVariable(variable: CfirVariable, data: CheckerContext) {
        checkers.allVariableCheckers.check(variable, data)
    }

    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: CheckerContext) {
        checkers.allTypeAliasCheckers.check(typeAlias, data)
    }

    override fun visitTypeParameter(typeParameter: CfirTypeParameter, data: CheckerContext) {
        checkers.allTypeParameterCheckers.check(typeParameter, data)
    }

    override fun visitValueParameter(valueParameter: CfirValueParameter, data: CheckerContext) {
        checkers.allValueParameterCheckers.check(valueParameter, data)
    }

    override fun visitInvalidDeclaration(invalidDeclaration: CfirInvalidDeclaration, data: CheckerContext) {
        checkers.allInvalidDeclarationCheckers.check(invalidDeclaration, data)
    }

    private inline fun <reified E : CfirDeclaration> Array<CfirDeclarationChecker<E>>.check(
        element: E,
        context: CheckerContext
    ) {
        for (checker in this) {
            try {
                context(context, reporter) {
                    checker.check(element)
                }
            } catch (e: Exception) {
                rethrowExceptionWithDetails("Exception in declaration checkers", e) {
                    withFirEntry("element", element)
                    context.containingFilePath?.let { withEntry("file", it) }
                }
            }
        }
    }
}
