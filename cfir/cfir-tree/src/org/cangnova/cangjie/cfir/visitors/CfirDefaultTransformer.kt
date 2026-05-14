package org.cangnova.cangjie.cfir.visitors

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.CfirTargetElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.types.*

/**
 * 默认转换器，默认递归转换 children，并按实际继承关系逐层委托到父级 transform 方法。
 */
open class CfirDefaultTransformer<in D> : CfirTransformer<D>() {

    override fun <E : CfirElement> transformElement(element: E, data: D): E {
        element.transformChildren(this, data)
        return element
    }

    override fun transformElementWithResolveState(elementWithResolveState: CfirElementWithResolveState, data: D): CfirElementWithResolveState {
        return transformElement(elementWithResolveState, data)
    }

    override fun transformAnnotationContainer(annotationContainer: CfirAnnotationContainer, data: D): CfirAnnotationContainer {
        return transformElement(annotationContainer, data)
    }

    override fun transformControlFlowGraphOwner(controlFlowGraphOwner: CfirControlFlowGraphOwner, data: D): CfirControlFlowGraphOwner {
        return transformElement(controlFlowGraphOwner, data)
    }

    override fun transformPackageDirective(packageDirective: CfirPackageDirective, data: D): CfirPackageDirective {
        return transformElement(packageDirective, data)
    }

    override fun transformImport(import: CfirImport, data: D): CfirImport {
        return transformElement(import, data)
    }

    override fun transformResolvedImport(resolvedImport: CfirResolvedImport, data: D): CfirImport {
        return transformImport(resolvedImport, data)
    }

    override fun transformAnnotation(annotation: CfirAnnotation, data: D): CfirAnnotation {
        return transformElement(annotation, data)
    }

    override fun transformStatement(statement: CfirStatement, data: D): CfirStatement {
        return transformElement(statement, data)
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: D): CfirDeclaration {
        return transformElement(declaration, data)
    }

    override fun transformMemberDeclaration(memberDeclaration: CfirMemberDeclaration, data: D): CfirMemberDeclaration {
        return transformElement(memberDeclaration, data)
    }

    override fun transformCallableDeclaration(callableDeclaration: CfirCallableDeclaration, data: D): CfirCallableDeclaration {
        return transformElement(callableDeclaration, data)
    }

    override fun transformClassLikeDeclaration(classLikeDeclaration: CfirClassLikeDeclaration, data: D): CfirClassLikeDeclaration {
        return transformElement(classLikeDeclaration, data)
    }

    override fun transformFile(file: CfirFile, data: D): CfirFile {
        return transformDeclaration(file, data) as CfirFile
    }

    override fun transformCodeFragment(codeFragment: CfirCodeFragment, data: D): CfirCodeFragment {
        return transformDeclaration(codeFragment, data) as CfirCodeFragment
    }

    override fun transformClass(klass: CfirClass, data: D): CfirClass {
        return transformClassLikeDeclaration(klass, data) as CfirClass
    }

    override fun transformEnumConstructor(enumConstructor: CfirEnumConstructor, data: D): CfirEnumConstructor {
        return transformCallableDeclaration(enumConstructor, data) as CfirEnumConstructor
    }

    override fun transformExtend(extend: CfirExtend, data: D): CfirExtend {
        return transformMemberDeclaration(extend, data) as CfirExtend
    }

    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: D): CfirTypeAlias {
        return transformClassLikeDeclaration(typeAlias, data) as CfirTypeAlias
    }

    override fun transformFunction(function: CfirFunction, data: D): CfirFunction {
        return transformCallableDeclaration(function, data) as CfirFunction
    }

    override fun transformMainFunction(mainFunction: CfirMainFunction, data: D): CfirMainFunction {
        return transformCallableDeclaration(mainFunction, data) as CfirMainFunction
    }

    override fun transformMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: D): CfirMacroDeclaration {
        return transformCallableDeclaration(macroDeclaration, data) as CfirMacroDeclaration
    }

    override fun transformFinalizer(finalizer: CfirFinalizer, data: D): CfirFinalizer {
        return transformCallableDeclaration(finalizer, data) as CfirFinalizer
    }

    override fun transformConstructor(constructor: CfirConstructor, data: D): CfirConstructor {
        return transformCallableDeclaration(constructor, data) as CfirConstructor
    }

    override fun transformInvalidDeclaration(invalidDeclaration: CfirInvalidDeclaration, data: D): CfirInvalidDeclaration {
        return transformDeclaration(invalidDeclaration, data) as CfirInvalidDeclaration
    }

    override fun transformProperty(property: CfirProperty, data: D): CfirProperty {
        return transformCallableDeclaration(property, data) as CfirProperty
    }

    override fun transformPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: D): CfirPropertyAccessor {
        return transformFunction(propertyAccessor, data) as CfirPropertyAccessor
    }

    override fun transformFieldVariable(variable: CfirFieldVariable, data: D): CfirFieldVariable {
        return transformCallableDeclaration(variable, data) as CfirFieldVariable
    }

    override fun transformPatternBindingVariable(patternBindingVariable: CfirPatternBindingVariable, data: D): CfirPatternBindingVariable {
        return transformCallableDeclaration(patternBindingVariable, data) as CfirPatternBindingVariable
    }

    override fun transformPatternVariable(patternVariable: CfirPatternVariable, data: D): CfirPatternVariable {
        return transformCallableDeclaration(patternVariable, data) as CfirPatternVariable
    }

    override fun transformValueParameter(valueParameter: CfirValueParameter, data: D): CfirValueParameter {
        return transformCallableDeclaration(valueParameter, data) as CfirValueParameter
    }

    override fun transformTypeParameter(typeParameter: CfirTypeParameter, data: D): CfirTypeParameter {
        return transformDeclaration(typeParameter, data) as CfirTypeParameter
    }

    override fun transformDeclarationStatus(declarationStatus: CfirDeclarationStatus, data: D): CfirDeclarationStatus {
        return transformElement(declarationStatus, data)
    }

    override fun transformExpression(expression: CfirExpression, data: D): CfirExpression {
        return transformElement(expression, data)
    }

    override fun transformBlock(block: CfirBlock, data: D): CfirExpression {
        return transformExpression(block, data)
    }

    override fun transformLazyBlock(lazyBlock: CfirLazyBlock, data: D): CfirExpression {
        return transformBlock(lazyBlock, data)
    }

    override fun transformLazyExpression(lazyExpression: CfirLazyExpression, data: D): CfirExpression {
        return transformExpression(lazyExpression, data)
    }

    override fun transformLiteralExpression(literalExpression: CfirLiteralExpression, data: D): CfirExpression {
        return transformExpression(literalExpression, data)
    }

    override fun transformStringInterpolation(stringInterpolation: CfirStringInterpolation, data: D): CfirExpression {
        return transformExpression(stringInterpolation, data)
    }

    override fun transformFunctionCall(functionCall: CfirFunctionCall, data: D): CfirExpression {
        return transformExpression(functionCall, data)
    }

    override fun transformNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression, data: D): CfirExpression {
        return transformQualifiedAccessExpression(namedAccessExpression, data)
    }

    override fun transformQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression, data: D): CfirExpression {
        return transformExpression(qualifiedAccessExpression, data)
    }

    override fun transformAssignment(assignment: CfirAssignment, data: D): CfirExpression {
        return transformExpression(assignment, data)
    }

    override fun transformBinaryOp(binaryOp: CfirBinaryOp, data: D): CfirExpression {
        return transformExpression(binaryOp, data)
    }

    override fun transformComparisonExpression(comparisonExpression: CfirComparisonExpression, data: D): CfirExpression {
        return transformExpression(comparisonExpression, data)
    }

    override fun transformTypeOperator(typeOperator: CfirTypeOperator, data: D): CfirExpression {
        return transformExpression(typeOperator, data)
    }

    override fun transformIfExpression(ifExpression: CfirIfExpression, data: D): CfirExpression {
        return transformExpression(ifExpression, data)
    }

    override fun transformMatchExpression(matchExpression: CfirMatchExpression, data: D): CfirExpression {
        return transformExpression(matchExpression, data)
    }

    override fun transformMatchBranch(matchBranch: CfirMatchBranch, data: D): CfirExpression {
        return transformExpression(matchBranch, data)
    }

    override fun transformCatch(catch: CfirCatch, data: D): CfirExpression {
        return transformExpression(catch, data)
    }

    override fun transformLoopExpression(loopExpression: CfirLoopExpression, data: D): CfirExpression {
        return transformExpression(loopExpression, data)
    }

    override fun transformForInExpression(forInExpression: CfirForInExpression, data: D): CfirExpression {
        return transformLoopExpression(forInExpression, data)
    }

    override fun transformTryExpression(tryExpression: CfirTryExpression, data: D): CfirExpression {
        return transformExpression(tryExpression, data)
    }

    override fun transformThrowExpression(throwExpression: CfirThrowExpression, data: D): CfirExpression {
        return transformExpression(throwExpression, data)
    }

    override fun transformReturnExpression(returnExpression: CfirReturnExpression, data: D): CfirExpression {
        return transformExpression(returnExpression, data)
    }

    override fun <E : CfirTargetElement> transformJump(jump: CfirJump<E>, data: D): CfirExpression {
        return transformExpression(jump, data)
    }

    override fun transformLoopJump(loopJump: CfirLoopJump, data: D): CfirExpression {
        return transformJump(loopJump, data)
    }

    override fun transformBreakExpression(breakExpression: CfirBreakExpression, data: D): CfirExpression {
        return transformLoopJump(breakExpression, data)
    }

    override fun transformContinueExpression(continueExpression: CfirContinueExpression, data: D): CfirExpression {
        return transformLoopJump(continueExpression, data)
    }

    override fun transformAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression, data: D): CfirExpression {
        return transformExpression(anonymousFunctionExpression, data)
    }



    override fun transformRangeExpression(rangeExpression: CfirRangeExpression, data: D): CfirExpression {
        return transformExpression(rangeExpression, data)
    }

    override fun transformArrayLiteral(arrayLiteral: CfirArrayLiteral, data: D): CfirExpression {
        return transformExpression(arrayLiteral, data)
    }

    override fun transformTupleLiteral(tupleLiteral: CfirTupleLiteral, data: D): CfirExpression {
        return transformExpression(tupleLiteral, data)
    }

    override fun transformSpawnExpression(spawnExpression: CfirSpawnExpression, data: D): CfirExpression {
        return transformExpression(spawnExpression, data)
    }

    override fun transformSynchronizedExpression(synchronizedExpression: CfirSynchronizedExpression, data: D): CfirExpression {
        return transformExpression(synchronizedExpression, data)
    }

    override fun transformUnsafeExpression(unsafeExpression: CfirUnsafeExpression, data: D): CfirExpression {
        return transformExpression(unsafeExpression, data)
    }

    override fun transformQuoteExpression(quoteExpression: CfirQuoteExpression, data: D): CfirExpression {
        return transformExpression(quoteExpression, data)
    }

    override fun transformSubscriptExpression(subscriptExpression: CfirSubscriptExpression, data: D): CfirExpression {
        return transformExpression(subscriptExpression, data)
    }

    override fun transformErrorExpression(errorExpression: CfirErrorExpression, data: D): CfirExpression {
        return transformExpression(errorExpression, data)
    }

    override fun transformPattern(pattern: CfirPattern, data: D): CfirPattern {
        return transformElement(pattern, data)
    }

    override fun transformConstPattern(constPattern: CfirConstPattern, data: D): CfirPattern {
        return transformPattern(constPattern, data)
    }

    override fun transformWildcardPattern(wildcardPattern: CfirWildcardPattern, data: D): CfirPattern {
        return transformPattern(wildcardPattern, data)
    }

    override fun transformBindingPattern(bindingPattern: CfirBindingPattern, data: D): CfirPattern {
        return transformPattern(bindingPattern, data)
    }

    override fun transformTuplePattern(tuplePattern: CfirTuplePattern, data: D): CfirPattern {
        return transformPattern(tuplePattern, data)
    }

    override fun transformEnumPattern(enumPattern: CfirEnumPattern, data: D): CfirPattern {
        return transformPattern(enumPattern, data)
    }

    override fun transformTypePattern(typePattern: CfirTypePattern, data: D): CfirPattern {
        return transformPattern(typePattern, data)
    }

    override fun transformTypeRef(typeRef: CfirTypeRef, data: D): CfirTypeRef {
        return transformElement(typeRef, data)
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(resolvedTypeRef, data)
    }

    override fun transformUserTypeRef(userTypeRef: CfirUserTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(userTypeRef, data)
    }

    override fun transformBasicTypeRef(basicTypeRef: CfirBasicTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(basicTypeRef, data)
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(implicitTypeRef, data)
    }

    override fun transformFunctionTypeRef(functionTypeRef: CfirFunctionTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(functionTypeRef, data)
    }

    override fun transformTupleTypeRef(tupleTypeRef: CfirTupleTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(tupleTypeRef, data)
    }

    override fun transformVArrayTypeRef(vArrayTypeRef: CfirVArrayTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(vArrayTypeRef, data)
    }

    override fun transformErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(errorTypeRef, data)
    }

    override fun transformReference(reference: CfirReference, data: D): CfirReference {
        return transformElement(reference, data)
    }

    override fun transformControlFlowGraphReference(controlFlowGraphReference: CfirControlFlowGraphReference, data: D): CfirReference {
        return transformReference(controlFlowGraphReference, data)
    }

    override fun transformNamedReference(namedReference: CfirNamedReference, data: D): CfirReference {
        return transformReference(namedReference, data)
    }

    override fun transformResolvedNamedReference(resolvedNamedReference: CfirResolvedNamedReference, data: D): CfirReference {
        return transformReference(resolvedNamedReference, data)
    }

    override fun transformErrorReference(errorReference: CfirErrorReference, data: D): CfirReference {
        return transformReference(errorReference, data)
    }

}
