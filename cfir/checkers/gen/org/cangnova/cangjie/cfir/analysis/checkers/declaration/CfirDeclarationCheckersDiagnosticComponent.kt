

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkersComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.withFirEntry

/*
 * 鏈枃浠剁敱鐢熸垚鍣ㄨ嚜鍔ㄧ敓鎴? * 璇峰嬁鎵嬪姩淇敼
 */

@OptIn(CheckersComponentInternal::class)
class CfirDeclarationCheckersDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
    private val checkers: CfirDeclarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: CfirSession, reporter: PendingDiagnosticReporter, dispatchKind: CheckerDispatchKind) : this(
        session,
        reporter,
        when (dispatchKind) {
            CheckerDispatchKind.Common -> session.checkersComponent.commonCfirDeclarationCheckers
            CheckerDispatchKind.Platform -> session.checkersComponent.platformCfirDeclarationCheckers
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

    override fun visitEnumConstructor(enumConstructor: CfirEnumConstructor, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(enumConstructor, data)
    }

    override fun visitMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(macroDeclaration, data)
    }

    override fun visitFinalizer(finalizer: CfirFinalizer, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(finalizer, data)
    }

    override fun visitConstructor(constructor: CfirConstructor, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(constructor, data)
    }

    override fun visitPatternVariable(patternVariable: CfirPatternVariable, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(patternVariable, data)
    }

    override fun visitExtend(extend: CfirExtend, data: CheckerContext) {
        checkers.allClassLikeCheckers.check(extend, data)
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

