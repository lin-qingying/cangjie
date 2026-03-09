package org.cangjie.cfir.visitors

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.expressions.*
import org.cangjie.cfir.patterns.*
import org.cangjie.cfir.references.*
import org.cangjie.cfir.types.*

/**
 * CFIR 访问者基类。
 *
 * 所有 visit 方法默认委托到 visitElement。
 * 子类可以覆盖感兴趣的方法来处理特定节点类型。
 */
abstract class CfirVisitor<out R, in D> {

    abstract fun visitElement(element: CfirElement, data: D): R

    // ---- 声明 ----

    open fun visitDeclaration(declaration: CfirDeclaration, data: D): R =
        visitElement(declaration, data)

    open fun visitFile(file: CfirFile, data: D): R =
        visitDeclaration(file, data)

    open fun visitPackageDirective(packageDirective: CfirPackageDirective, data: D): R =
        visitElement(packageDirective, data)

    open fun visitImport(import_: CfirImport, data: D): R =
        visitElement(import_, data)

    open fun visitClass(klass: CfirClass, data: D): R =
        visitDeclaration(klass, data)

    open fun visitEnumEntry(enumEntry: CfirEnumEntry, data: D): R =
        visitDeclaration(enumEntry, data)

    open fun visitExtend(extend: CfirExtend, data: D): R =
        visitDeclaration(extend, data)

    open fun visitTypeAlias(typeAlias: CfirTypeAlias, data: D): R =
        visitDeclaration(typeAlias, data)

    open fun visitFunction(function: CfirFunction, data: D): R =
        visitDeclaration(function, data)

    open fun visitConstructor(constructor: CfirConstructor, data: D): R =
        visitDeclaration(constructor, data)

    open fun visitProperty(property: CfirProperty, data: D): R =
        visitDeclaration(property, data)

    open fun visitVariable(variable: CfirVariable, data: D): R =
        visitDeclaration(variable, data)

    open fun visitValueParameter(valueParameter: CfirValueParameter, data: D): R =
        visitDeclaration(valueParameter, data)

    open fun visitTypeParameter(typeParameter: CfirTypeParameter, data: D): R =
        visitDeclaration(typeParameter, data)

    open fun visitAnonymousInitializer(anonymousInitializer: CfirAnonymousInitializer, data: D): R =
        visitDeclaration(anonymousInitializer, data)

    // ---- 表达式 ----

    open fun visitExpression(expression: CfirExpression, data: D): R =
        visitElement(expression, data)

    open fun visitBlock(block: CfirBlock, data: D): R =
        visitExpression(block, data)

    open fun visitLiteralExpression(literal: CfirLiteralExpression, data: D): R =
        visitExpression(literal, data)

    open fun visitStringInterpolation(interpolation: CfirStringInterpolation, data: D): R =
        visitExpression(interpolation, data)

    open fun visitFunctionCall(call: CfirFunctionCall, data: D): R =
        visitExpression(call, data)

    open fun visitPropertyAccess(access: CfirPropertyAccess, data: D): R =
        visitExpression(access, data)

    open fun visitQualifiedAccess(access: CfirQualifiedAccess, data: D): R =
        visitExpression(access, data)

    open fun visitAssignment(assignment: CfirAssignment, data: D): R =
        visitExpression(assignment, data)

    open fun visitBinaryOp(binaryOp: CfirBinaryOp, data: D): R =
        visitExpression(binaryOp, data)

    open fun visitComparisonExpression(comparison: CfirComparisonExpression, data: D): R =
        visitExpression(comparison, data)

    open fun visitTypeOperator(typeOperator: CfirTypeOperator, data: D): R =
        visitExpression(typeOperator, data)

    open fun visitIfExpression(ifExpression: CfirIfExpression, data: D): R =
        visitExpression(ifExpression, data)

    open fun visitMatchExpression(matchExpression: CfirMatchExpression, data: D): R =
        visitExpression(matchExpression, data)

    open fun visitMatchBranch(matchBranch: CfirMatchBranch, data: D): R =
        visitExpression(matchBranch, data)

    open fun visitLoopExpression(loop: CfirLoopExpression, data: D): R =
        visitExpression(loop, data)

    open fun visitForInExpression(forIn: CfirForInExpression, data: D): R =
        visitExpression(forIn, data)

    open fun visitTryExpression(tryExpression: CfirTryExpression, data: D): R =
        visitExpression(tryExpression, data)

    open fun visitThrowExpression(throwExpression: CfirThrowExpression, data: D): R =
        visitExpression(throwExpression, data)

    open fun visitReturnExpression(returnExpression: CfirReturnExpression, data: D): R =
        visitExpression(returnExpression, data)

    open fun visitJumpExpression(jump: CfirJumpExpression, data: D): R =
        visitExpression(jump, data)

    open fun visitLambdaExpression(lambda: CfirLambdaExpression, data: D): R =
        visitExpression(lambda, data)

    open fun visitRangeExpression(range: CfirRangeExpression, data: D): R =
        visitExpression(range, data)

    open fun visitArrayLiteral(array: CfirArrayLiteral, data: D): R =
        visitExpression(array, data)

    open fun visitTupleLiteral(tuple: CfirTupleLiteral, data: D): R =
        visitExpression(tuple, data)

    open fun visitSpawnExpression(spawn: CfirSpawnExpression, data: D): R =
        visitExpression(spawn, data)

    open fun visitSubscriptExpression(subscript: CfirSubscriptExpression, data: D): R =
        visitExpression(subscript, data)

    open fun visitErrorExpression(error: CfirErrorExpression, data: D): R =
        visitExpression(error, data)

    // ---- 模式 ----

    open fun visitPattern(pattern: CfirPattern, data: D): R =
        visitElement(pattern, data)

    open fun visitConstPattern(pattern: CfirConstPattern, data: D): R =
        visitPattern(pattern, data)

    open fun visitWildcardPattern(pattern: CfirWildcardPattern, data: D): R =
        visitPattern(pattern, data)

    open fun visitBindingPattern(pattern: CfirBindingPattern, data: D): R =
        visitPattern(pattern, data)

    open fun visitTuplePattern(pattern: CfirTuplePattern, data: D): R =
        visitPattern(pattern, data)

    open fun visitEnumPattern(pattern: CfirEnumPattern, data: D): R =
        visitPattern(pattern, data)

    open fun visitTypePattern(pattern: CfirTypePattern, data: D): R =
        visitPattern(pattern, data)

    // ---- 类型引用 ----

    open fun visitTypeRef(typeRef: CfirTypeRef, data: D): R =
        visitElement(typeRef, data)

    open fun visitResolvedTypeRef(typeRef: CfirResolvedTypeRef, data: D): R =
        visitTypeRef(typeRef, data)

    open fun visitUserTypeRef(typeRef: CfirUserTypeRef, data: D): R =
        visitTypeRef(typeRef, data)

    open fun visitBasicTypeRef(typeRef: CfirBasicTypeRef, data: D): R =
        visitTypeRef(typeRef, data)

    open fun visitImplicitTypeRef(typeRef: CfirImplicitTypeRef, data: D): R =
        visitTypeRef(typeRef, data)

    open fun visitFunctionTypeRef(typeRef: CfirFunctionTypeRef, data: D): R =
        visitTypeRef(typeRef, data)

    open fun visitTupleTypeRef(typeRef: CfirTupleTypeRef, data: D): R =
        visitTypeRef(typeRef, data)

    open fun visitVArrayTypeRef(typeRef: CfirVArrayTypeRef, data: D): R =
        visitTypeRef(typeRef, data)

    open fun visitErrorTypeRef(typeRef: CfirErrorTypeRef, data: D): R =
        visitTypeRef(typeRef, data)

    // ---- 引用 ----

    open fun visitReference(reference: CfirReference, data: D): R =
        visitElement(reference, data)

    open fun visitNamedReference(reference: CfirNamedReference, data: D): R =
        visitReference(reference, data)

    open fun visitResolvedNamedReference(reference: CfirResolvedNamedReference, data: D): R =
        visitReference(reference, data)

    open fun visitErrorReference(reference: CfirErrorReference, data: D): R =
        visitReference(reference, data)
}
