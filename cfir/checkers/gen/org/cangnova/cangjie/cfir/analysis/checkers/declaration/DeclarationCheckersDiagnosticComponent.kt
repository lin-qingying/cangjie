

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkersComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.utils.exceptions.rethrowExceptionWithDetails
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

@OptIn(CheckersComponentInternal::class)
class DeclarationCheckersDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
    private val checkers: DeclarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: CfirSession, reporter: PendingDiagnosticReporter) : this(
        session,
        reporter,
        session.checkersComponent.declarationCheckers
    )

    override fun visitElement(element: CfirElement, data: CheckerContext) {
        if (element is CfirDeclaration) {
            error("${element::class.simpleName} should call parent checkers inside ${this::class.simpleName}")
        }
    }

    override fun visitDeclaration(declaration: CfirDeclaration, data: CheckerContext) {
        checkers.allBasicDeclarationCheckers.check(declaration, data)
    }

    override fun visitEnumConstructor(enumConstructor: CfirEnumConstructor, data: CheckerContext) {
        checkers.allEnumConstructorCheckers.check(enumConstructor, data)
    }

    override fun visitNamedFunction(namedFunction: CfirNamedFunction, data: CheckerContext) {
        checkers.allSimpleFunctionCheckers.check(namedFunction, data)
    }

    override fun visitProperty(property: CfirProperty, data: CheckerContext) {
        checkers.allPropertyCheckers.check(property, data)
    }

    override fun visitAnonymousFunction(anonymousFunction: CfirAnonymousFunction, data: CheckerContext) {
        checkers.allAnonymousFunctionCheckers.check(anonymousFunction, data)
    }

    override fun visitConstructor(constructor: CfirConstructor, data: CheckerContext) {
        checkers.allConstructorCheckers.check(constructor, data)
    }

    override fun visitFile(file: CfirFile, data: CheckerContext) {
        checkers.allFileCheckers.check(file, data)
    }

    override fun visitTypeParameter(typeParameter: CfirTypeParameter, data: CheckerContext) {
        checkers.allTypeParameterCheckers.check(typeParameter, data)
    }

    override fun visitExtend(extend: CfirExtend, data: CheckerContext) {
        checkers.allExtendCheckers.check(extend, data)
    }

    override fun visitMainFunction(mainFunction: CfirMainFunction, data: CheckerContext) {
        checkers.allMainFunctionCheckers.check(mainFunction, data)
    }

    override fun visitPatternVariable(patternVariable: CfirPatternVariable, data: CheckerContext) {
        checkers.allPatternVariableCheckers.check(patternVariable, data)
    }

    override fun visitFieldVariable(fieldVariable: CfirFieldVariable, data: CheckerContext) {
        checkers.allFieldVariableCheckers.check(fieldVariable, data)
    }

    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: CheckerContext) {
        checkers.allTypeAliasCheckers.check(typeAlias, data)
    }

    override fun visitValueParameter(valueParameter: CfirValueParameter, data: CheckerContext) {
        checkers.allValueParameterCheckers.check(valueParameter, data)
    }

    override fun visitInvalidDeclaration(invalidDeclaration: CfirInvalidDeclaration, data: CheckerContext) {
        checkers.allInvalidDeclarationCheckers.check(invalidDeclaration, data)
    }

    override fun visitPatternBindingVariable(patternBindingVariable: CfirPatternBindingVariable, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(patternBindingVariable, data)
    }

    override fun visitMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: CheckerContext) {
        checkers.allFunctionCheckers.check(macroDeclaration, data)
    }

    override fun visitFinalizer(finalizer: CfirFinalizer, data: CheckerContext) {
        checkers.allFunctionCheckers.check(finalizer, data)
    }

    override fun visitClass(klass: CfirClass, data: CheckerContext) {
        checkers.allClassLikeCheckers.check(klass, data)
    }

    override fun visitInterface(`interface`: CfirInterface, data: CheckerContext) {
        checkers.allClassLikeCheckers.check(`interface`, data)
    }

    override fun visitStruct(struct: CfirStruct, data: CheckerContext) {
        checkers.allClassLikeCheckers.check(struct, data)
    }

    override fun visitEnum(enum: CfirEnum, data: CheckerContext) {
        checkers.allClassLikeCheckers.check(enum, data)
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
                if (shouldIjPlatformExceptionBeRethrown(e)) throw e
                rethrowExceptionWithDetails("Exception in declaration checkers", e) {
                    withCfirEntry("element", element)
                    context.containingFilePath?.let { withEntry("file", it) }
                }
            }
        }
    }
}
