/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

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

    /**
     * 默认转换任意 CFIR 元素并递归转换其 children。
     */
    override fun <E : CfirElement> transformElement(element: E, data: D): E {
        element.transformChildren(this, data)
        return element
    }

    /**
     * 默认转换带 resolve state 的元素。
     */
    override fun transformElementWithResolveState(elementWithResolveState: CfirElementWithResolveState, data: D): CfirElementWithResolveState {
        return transformElement(elementWithResolveState, data)
    }

    /**
     * 默认转换注解容器。
     */
    override fun transformAnnotationContainer(annotationContainer: CfirAnnotationContainer, data: D): CfirAnnotationContainer {
        return transformElement(annotationContainer, data)
    }

    /**
     * 默认转换控制流图 owner。
     */
    override fun transformControlFlowGraphOwner(controlFlowGraphOwner: CfirControlFlowGraphOwner, data: D): CfirControlFlowGraphOwner {
        return transformElement(controlFlowGraphOwner, data)
    }

    /**
     * 默认转换包指令。
     */
    override fun transformPackageDirective(packageDirective: CfirPackageDirective, data: D): CfirPackageDirective {
        return transformElement(packageDirective, data)
    }

    /**
     * 默认转换 import。
     */
    override fun transformImport(import: CfirImport, data: D): CfirImport {
        return transformElement(import, data)
    }

    /**
     * 默认转换已解析 import。
     */
    override fun transformResolvedImport(resolvedImport: CfirResolvedImport, data: D): CfirImport {
        return transformImport(resolvedImport, data)
    }

    /**
     * 默认转换注解。
     */
    override fun transformAnnotation(annotation: CfirAnnotation, data: D): CfirAnnotation {
        return transformElement(annotation, data)
    }

    /**
     * 默认转换语句。
     */
    override fun transformStatement(statement: CfirStatement, data: D): CfirStatement {
        return transformElement(statement, data)
    }

    /**
     * 默认转换声明。
     */
    override fun transformDeclaration(declaration: CfirDeclaration, data: D): CfirDeclaration {
        return transformElement(declaration, data)
    }

    /**
     * 默认转换成员声明。
     */
    override fun transformMemberDeclaration(memberDeclaration: CfirMemberDeclaration, data: D): CfirMemberDeclaration {
        return transformElement(memberDeclaration, data)
    }

    /**
     * 默认转换 callable 声明。
     */
    override fun transformCallableDeclaration(callableDeclaration: CfirCallableDeclaration, data: D): CfirCallableDeclaration {
        return transformElement(callableDeclaration, data)
    }

    /**
     * 默认转换 class-like 声明。
     */
    override fun transformClassLikeDeclaration(classLikeDeclaration: CfirClassLikeDeclaration, data: D): CfirClassLikeDeclaration {
        return transformElement(classLikeDeclaration, data)
    }

    /**
     * 默认转换文件声明。
     */
    override fun transformFile(file: CfirFile, data: D): CfirFile {
        return transformDeclaration(file, data) as CfirFile
    }

    /**
     * 默认转换 code fragment。
     */
    override fun transformCodeFragment(codeFragment: CfirCodeFragment, data: D): CfirCodeFragment {
        return transformDeclaration(codeFragment, data) as CfirCodeFragment
    }

    /**
     * 默认转换 class 声明。
     */
    override fun transformClass(klass: CfirClass, data: D): CfirClass {
        return transformClassLikeDeclaration(klass, data) as CfirClass
    }

    /**
     * 默认转换 enum constructor。
     */
    override fun transformEnumConstructor(enumConstructor: CfirEnumConstructor, data: D): CfirEnumConstructor {
        return transformCallableDeclaration(enumConstructor, data) as CfirEnumConstructor
    }

    /**
     * 默认转换 extend 声明。
     */
    override fun transformExtend(extend: CfirExtend, data: D): CfirExtend {
        return transformMemberDeclaration(extend, data) as CfirExtend
    }

    /**
     * 默认转换 typealias 声明。
     */
    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: D): CfirTypeAlias {
        return transformClassLikeDeclaration(typeAlias, data) as CfirTypeAlias
    }

    /**
     * 默认转换函数声明。
     */
    override fun transformFunction(function: CfirFunction, data: D): CfirFunction {
        return transformCallableDeclaration(function, data) as CfirFunction
    }

    /**
     * 默认转换 main 函数。
     */
    override fun transformMainFunction(mainFunction: CfirMainFunction, data: D): CfirMainFunction {
        return transformCallableDeclaration(mainFunction, data) as CfirMainFunction
    }

    /**
     * 默认转换宏声明。
     */
    override fun transformMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: D): CfirMacroDeclaration {
        return transformCallableDeclaration(macroDeclaration, data) as CfirMacroDeclaration
    }

    /**
     * 默认转换 finalizer。
     */
    override fun transformFinalizer(finalizer: CfirFinalizer, data: D): CfirFinalizer {
        return transformCallableDeclaration(finalizer, data) as CfirFinalizer
    }

    /**
     * 默认转换构造器。
     */
    override fun transformConstructor(constructor: CfirConstructor, data: D): CfirConstructor {
        return transformCallableDeclaration(constructor, data) as CfirConstructor
    }

    /**
     * 默认转换无效声明。
     */
    override fun transformInvalidDeclaration(invalidDeclaration: CfirInvalidDeclaration, data: D): CfirInvalidDeclaration {
        return transformDeclaration(invalidDeclaration, data) as CfirInvalidDeclaration
    }

    /**
     * 默认转换属性声明。
     */
    override fun transformProperty(property: CfirProperty, data: D): CfirProperty {
        return transformCallableDeclaration(property, data) as CfirProperty
    }

    /**
     * 默认转换属性访问器。
     */
    override fun transformPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: D): CfirPropertyAccessor {
        return transformFunction(propertyAccessor, data) as CfirPropertyAccessor
    }

    /**
     * 默认转换字段变量。
     */
    override fun transformFieldVariable(fieldVariable: CfirFieldVariable, data: D): CfirFieldVariable {
        return transformCallableDeclaration(fieldVariable, data) as CfirFieldVariable
    }

    /**
     * 默认转换模式绑定变量。
     */
    override fun transformPatternBindingVariable(patternBindingVariable: CfirPatternBindingVariable, data: D): CfirPatternBindingVariable {
        return transformCallableDeclaration(patternBindingVariable, data) as CfirPatternBindingVariable
    }

    /**
     * 默认转换模式变量。
     */
    override fun transformPatternVariable(patternVariable: CfirPatternVariable, data: D): CfirPatternVariable {
        return transformCallableDeclaration(patternVariable, data) as CfirPatternVariable
    }

    /**
     * 默认转换值参数。
     */
    override fun transformValueParameter(valueParameter: CfirValueParameter, data: D): CfirValueParameter {
        return transformCallableDeclaration(valueParameter, data) as CfirValueParameter
    }

    /**
     * 默认转换类型参数。
     */
    override fun transformTypeParameter(typeParameter: CfirTypeParameter, data: D): CfirTypeParameter {
        return transformDeclaration(typeParameter, data) as CfirTypeParameter
    }

    /**
     * 默认转换声明状态。
     */
    override fun transformDeclarationStatus(declarationStatus: CfirDeclarationStatus, data: D): CfirDeclarationStatus {
        return transformElement(declarationStatus, data)
    }

    /**
     * 默认转换表达式。
     */
    override fun transformExpression(expression: CfirExpression, data: D): CfirExpression {
        return transformElement(expression, data)
    }

    /** 默认转换仅包装一个内部表达式的节点。 */
    override fun transformWrappedExpression(wrappedExpression: CfirWrappedExpression, data: D): CfirExpression {
        return transformExpression(wrappedExpression, data)
    }

    /** 默认转换 optional 后缀包装节点。 */
    override fun transformOptionalExpression(optionalExpression: CfirOptionalExpression, data: D): CfirExpression {
        return transformWrappedExpression(optionalExpression, data)
    }

    /** 默认转换 optional-chain 根包装节点。 */
    override fun transformOptionalChainExpression(optionalChainExpression: CfirOptionalChainExpression, data: D): CfirExpression {
        return transformWrappedExpression(optionalChainExpression, data)
    }

    /** 默认转换 inout 实参包装节点。 */
    override fun transformInoutArgumentExpression(inoutArgumentExpression: CfirInoutArgumentExpression, data: D): CfirExpression {
        return transformWrappedExpression(inoutArgumentExpression, data)
    }

    /** 默认转换命名实参包装节点。 */
    override fun transformNamedArgumentExpression(namedArgumentExpression: CfirNamedArgumentExpression, data: D): CfirExpression {
        return transformWrappedExpression(namedArgumentExpression, data)
    }

    /**
     * 默认转换代码块。
     */
    override fun transformBlock(block: CfirBlock, data: D): CfirExpression {
        return transformExpression(block, data)
    }

    /**
     * 默认转换 lazy block。
     */
    override fun transformLazyBlock(lazyBlock: CfirLazyBlock, data: D): CfirExpression {
        return transformBlock(lazyBlock, data)
    }

    /**
     * 默认转换 lazy expression。
     */
    override fun transformLazyExpression(lazyExpression: CfirLazyExpression, data: D): CfirExpression {
        return transformExpression(lazyExpression, data)
    }

    /**
     * 默认转换字面量表达式。
     */
    override fun transformLiteralExpression(literalExpression: CfirLiteralExpression, data: D): CfirExpression {
        return transformExpression(literalExpression, data)
    }

    /**
     * 默认转换字符串插值。
     */
    override fun transformStringInterpolation(stringInterpolation: CfirStringInterpolation, data: D): CfirExpression {
        return transformExpression(stringInterpolation, data)
    }

    /**
     * 默认转换函数调用。
     */
    override fun transformFunctionCall(functionCall: CfirFunctionCall, data: D): CfirExpression {
        return transformExpression(functionCall, data)
    }

    /**
     * 默认转换具名访问表达式。
     */
    override fun transformNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression, data: D): CfirExpression {
        return transformQualifiedAccessExpression(namedAccessExpression, data)
    }

    /**
     * 默认转换限定访问表达式。
     */
    override fun transformQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression, data: D): CfirExpression {
        return transformExpression(qualifiedAccessExpression, data)
    }

    /**
     * 默认转换赋值表达式。
     */
    override fun transformAssignment(assignment: CfirAssignment, data: D): CfirExpression {
        return transformExpression(assignment, data)
    }

    /**
     * 默认转换二元操作表达式。
     */
    override fun transformBinaryOp(binaryOp: CfirBinaryOp, data: D): CfirExpression {
        return transformExpression(binaryOp, data)
    }

    /**
     * 默认转换比较表达式。
     */
    override fun transformComparisonExpression(comparisonExpression: CfirComparisonExpression, data: D): CfirExpression {
        return transformExpression(comparisonExpression, data)
    }

    /**
     * 默认转换类型操作表达式。
     */
    override fun transformTypeOperator(typeOperator: CfirTypeOperator, data: D): CfirExpression {
        return transformExpression(typeOperator, data)
    }

    /**
     * 默认转换类型转换表达式。
     */
    override fun transformTypeConversion(typeConversion: CfirTypeConversion, data: D): CfirExpression {
        return transformExpression(typeConversion, data)
    }

    /**
     * 默认转换 if 表达式。
     */
    override fun transformIfExpression(ifExpression: CfirIfExpression, data: D): CfirExpression {
        return transformExpression(ifExpression, data)
    }

    /**
     * 默认转换 match 表达式。
     */
    override fun transformMatchExpression(matchExpression: CfirMatchExpression, data: D): CfirExpression {
        return transformExpression(matchExpression, data)
    }

    /**
     * 默认转换 match 分支。
     */
    override fun transformMatchBranch(matchBranch: CfirMatchBranch, data: D): CfirExpression {
        return transformExpression(matchBranch, data)
    }

    /**
     * 默认转换 catch 子句。
     */
    override fun transformCatch(catch: CfirCatch, data: D): CfirExpression {
        return transformExpression(catch, data)
    }

    /**
     * 默认转换循环表达式。
     */
    override fun transformLoopExpression(loopExpression: CfirLoopExpression, data: D): CfirExpression {
        return transformExpression(loopExpression, data)
    }

    /**
     * 默认转换 for-in 表达式。
     */
    override fun transformForInExpression(forInExpression: CfirForInExpression, data: D): CfirExpression {
        return transformLoopExpression(forInExpression, data)
    }

    /**
     * 默认转换 try 表达式。
     */
    override fun transformTryExpression(tryExpression: CfirTryExpression, data: D): CfirExpression {
        return transformExpression(tryExpression, data)
    }

    /**
     * 默认转换 throw 表达式。
     */
    override fun transformThrowExpression(throwExpression: CfirThrowExpression, data: D): CfirExpression {
        return transformExpression(throwExpression, data)
    }

    /**
     * 默认转换 return 表达式。
     */
    override fun transformReturnExpression(returnExpression: CfirReturnExpression, data: D): CfirExpression {
        return transformExpression(returnExpression, data)
    }

    /**
     * 默认转换 jump 表达式。
     */
    override fun <E : CfirTargetElement> transformJump(jump: CfirJump<E>, data: D): CfirExpression {
        return transformExpression(jump, data)
    }

    /**
     * 默认转换循环跳转表达式。
     */
    override fun transformLoopJump(loopJump: CfirLoopJump, data: D): CfirExpression {
        return transformJump(loopJump, data)
    }

    /**
     * 默认转换 break 表达式。
     */
    override fun transformBreakExpression(breakExpression: CfirBreakExpression, data: D): CfirExpression {
        return transformLoopJump(breakExpression, data)
    }

    /**
     * 默认转换 continue 表达式。
     */
    override fun transformContinueExpression(continueExpression: CfirContinueExpression, data: D): CfirExpression {
        return transformLoopJump(continueExpression, data)
    }

    /**
     * 默认转换匿名函数表达式。
     */
    override fun transformAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression, data: D): CfirExpression {
        return transformExpression(anonymousFunctionExpression, data)
    }



    /**
     * 默认转换区间表达式。
     */
    override fun transformRangeExpression(rangeExpression: CfirRangeExpression, data: D): CfirExpression {
        return transformExpression(rangeExpression, data)
    }

    /**
     * 默认转换数组字面量。
     */
    override fun transformArrayLiteral(arrayLiteral: CfirArrayLiteral, data: D): CfirExpression {
        return transformExpression(arrayLiteral, data)
    }

    /**
     * 默认转换元组字面量。
     */
    override fun transformTupleLiteral(tupleLiteral: CfirTupleLiteral, data: D): CfirExpression {
        return transformExpression(tupleLiteral, data)
    }

    /**
     * 默认转换 spawn 表达式。
     */
    override fun transformSpawnExpression(spawnExpression: CfirSpawnExpression, data: D): CfirExpression {
        return transformExpression(spawnExpression, data)
    }

    /**
     * 默认转换 synchronized 表达式。
     */
    override fun transformSynchronizedExpression(synchronizedExpression: CfirSynchronizedExpression, data: D): CfirExpression {
        return transformExpression(synchronizedExpression, data)
    }

    /**
     * 默认转换 unsafe 表达式。
     */
    override fun transformUnsafeExpression(unsafeExpression: CfirUnsafeExpression, data: D): CfirExpression {
        return transformExpression(unsafeExpression, data)
    }

    /**
     * 默认转换 quote 表达式。
     */
    override fun transformQuoteExpression(quoteExpression: CfirQuoteExpression, data: D): CfirExpression {
        return transformExpression(quoteExpression, data)
    }

    /**
     * 默认转换 subscript 表达式。
     */
    override fun transformSubscriptExpression(subscriptExpression: CfirSubscriptExpression, data: D): CfirExpression {
        return transformExpression(subscriptExpression, data)
    }

    /**
     * 默认转换错误表达式。
     */
    override fun transformErrorExpression(errorExpression: CfirErrorExpression, data: D): CfirExpression {
        return transformExpression(errorExpression, data)
    }

    /**
     * 默认转换 pattern。
     */
    override fun transformPattern(pattern: CfirPattern, data: D): CfirPattern {
        return transformElement(pattern, data)
    }

    /**
     * 默认转换 const pattern。
     */
    override fun transformConstPattern(constPattern: CfirConstPattern, data: D): CfirPattern {
        return transformPattern(constPattern, data)
    }

    /**
     * 默认转换 wildcard pattern。
     */
    override fun transformWildcardPattern(wildcardPattern: CfirWildcardPattern, data: D): CfirPattern {
        return transformPattern(wildcardPattern, data)
    }

    /**
     * 默认转换 binding pattern。
     */
    override fun transformBindingPattern(bindingPattern: CfirBindingPattern, data: D): CfirPattern {
        return transformPattern(bindingPattern, data)
    }

    /**
     * 默认转换 tuple pattern。
     */
    override fun transformTuplePattern(tuplePattern: CfirTuplePattern, data: D): CfirPattern {
        return transformPattern(tuplePattern, data)
    }

    /**
     * 默认转换 enum pattern。
     */
    override fun transformEnumPattern(enumPattern: CfirEnumPattern, data: D): CfirPattern {
        return transformPattern(enumPattern, data)
    }

    /**
     * 默认转换 type pattern。
     */
    override fun transformTypePattern(typePattern: CfirTypePattern, data: D): CfirPattern {
        return transformPattern(typePattern, data)
    }

    /**
     * 默认转换类型引用。
     */
    override fun transformTypeRef(typeRef: CfirTypeRef, data: D): CfirTypeRef {
        return transformElement(typeRef, data)
    }

    /**
     * 默认转换已解析类型引用。
     */
    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(resolvedTypeRef, data)
    }

    /**
     * 默认转换未解析类型引用。
     */
    override fun transformUnresolvedTypeRef(unresolvedTypeRef: CfirUnresolvedTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(unresolvedTypeRef, data)
    }

    /**
     * 默认转换用户类型引用。
     */
    override fun transformUserTypeRef(userTypeRef: CfirUserTypeRef, data: D): CfirTypeRef {
        return transformUnresolvedTypeRef(userTypeRef, data)
    }

    /**
     * 默认转换基础类型引用。
     */
    override fun transformBasicTypeRef(basicTypeRef: CfirBasicTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(basicTypeRef, data)
    }

    /**
     * 默认转换隐式类型引用。
     */
    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(implicitTypeRef, data)
    }

    /**
     * 默认转换函数类型引用。
     */
    override fun transformFunctionTypeRef(functionTypeRef: CfirFunctionTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(functionTypeRef, data)
    }

    /**
     * 默认转换 Option 类型引用。
     */
    override fun transformOptionTypeRef(optionTypeRef: CfirOptionTypeRef, data: D): CfirTypeRef {
        return transformUnresolvedTypeRef(optionTypeRef, data)
    }

    /**
     * 默认转换元组类型引用。
     */
    override fun transformTupleTypeRef(tupleTypeRef: CfirTupleTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(tupleTypeRef, data)
    }

    /**
     * 默认转换 VArray 类型引用。
     */
    override fun transformVArrayTypeRef(vArrayTypeRef: CfirVArrayTypeRef, data: D): CfirTypeRef {
        return transformTypeRef(vArrayTypeRef, data)
    }

    /**
     * 默认转换错误类型引用。
     */
    override fun transformErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: D): CfirTypeRef {
        return transformResolvedTypeRef(errorTypeRef, data)
    }

    /**
     * 默认转换引用。
     */
    override fun transformReference(reference: CfirReference, data: D): CfirReference {
        return transformElement(reference, data)
    }

    /**
     * 默认转换控制流图引用。
     */
    override fun transformControlFlowGraphReference(controlFlowGraphReference: CfirControlFlowGraphReference, data: D): CfirReference {
        return transformReference(controlFlowGraphReference, data)
    }

    /**
     * 默认转换具名引用。
     */
    override fun transformNamedReference(namedReference: CfirNamedReference, data: D): CfirReference {
        return transformReference(namedReference, data)
    }

    /**
     * 默认转换已解析具名引用。
     */
    override fun transformResolvedNamedReference(resolvedNamedReference: CfirResolvedNamedReference, data: D): CfirReference {
        return transformReference(resolvedNamedReference, data)
    }

    /**
     * 默认转换错误引用。
     */
    override fun transformErrorReference(errorReference: CfirErrorReference, data: D): CfirReference {
        return transformReference(errorReference, data)
    }

}
