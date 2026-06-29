/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi

/**
 * 提供 `CjVisitorUnit` visitor 适配器，将泛型 visitor 结果固定为 Unit。
 */
open class CjVisitorUnit : CjVisitor<Unit, Unit?>() {

    /**
     * 访问 `visitCjElement` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitCjElement(element: CjElement) {
        super.visitCjElement(element, Unit)
    }

    /**
     * 访问 `visitTupleType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTupleType(type: CjTupleType) {
        super.visitTupleType(type, Unit)
    }

    /**
     * 访问 `visitFunctionType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitFunctionType(type: CjFunctionType) {
        super.visitFunctionType(type, Unit)
    }

    /**
     * 将带数据参数的 `visitTupleType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTupleType(tupleType: CjTupleType, data: Unit?) {
        visitTupleType(tupleType)

    }

    /**
     * 将带数据参数的 `visitFunctionType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitFunctionType(type: CjFunctionType, data: Unit?): Unit {
        visitFunctionType(type)

    }

    /**
     * 将带数据参数的 `visitParenthesizedType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitParenthesizedType(parenthesizedType: CjParenthesizedType, data: Unit?): Unit {
        visitParenthesizedType(parenthesizedType)

    }

    /**
     * 访问 `visitParenthesizedType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitParenthesizedType(parenthesizedType: CjParenthesizedType) {
        super.visitParenthesizedType(parenthesizedType, Unit)
    }

    /**
     * 将带数据参数的 `visitOptionType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitOptionType(optionType: CjOptionType, data: Unit?) {
        visitOptionType(optionType)

    }

    /**
     * 访问 `visitOptionType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitOptionType(optionType: CjOptionType) {
        super.visitOptionType(optionType, Unit)
    }

    /**
     * 访问 `visitOptionalExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitOptionalExpression(expression: CjOptionalExpression) {
        super.visitOptionalExpression(expression, Unit)
    }

    /**
     * 访问 `visitOptionalChainExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitOptionalChainExpression(expression: CjOptionalChainExpression) {
        super.visitOptionalChainExpression(expression, Unit)
    }

    /**
     * 访问 `visitProperty` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitProperty(property: CjProperty) {
        super.visitProperty(property, Unit)
    }

    /**
     * 访问 `visitPropertyAccessor` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPropertyAccessor(accessor: CjPropertyAccessor) {
        super.visitPropertyAccessor(accessor, Unit)
    }

    /**
     * 访问 `visitPatternVariable` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPatternVariable(variable: CjPatternVariable) {
        super.visitPatternVariable(variable, Unit)
    }

    /**
     * 访问 `visitVariable` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitVariable(variable: CjVariable<*>) {
        super.visitVariable(variable, Unit)
    }

    /**
     * 访问 `visitFieldVariable` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitFieldVariable(field: CjFieldVariable) {
        super.visitFieldVariable(field, Unit)
    }

    /**
     * 访问 `visitTypeStatement` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTypeStatement(typeStatement: CjTypeStatement) {
        super.visitTypeStatement(typeStatement, Unit)
    }

    /**
     * 访问 `visitClass` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitClass(cclass: CjClass) {
        super.visitClass(cclass, Unit)
    }

    /**
     * 将带数据参数的 `visitExtend` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitExtend(extend: CjExtend, data: Unit?): Unit {
        visitExtend(extend)

    }

    /**
     * 访问 `visitExtend` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitExtend(extend: CjExtend) {
        super.visitExtend(extend, Unit)
    }

    /**
     * 将带数据参数的 `visitMainFunction` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitMainFunction(mainFunction: CjMainFunction, data: Unit?): Unit {
        visitMainFunction(mainFunction)

    }

    /**
     * 访问 `visitMainFunction` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitMainFunction(mainFunction: CjMainFunction) {
        super.visitMainFunction(mainFunction, Unit)
    }

    /**
     * 访问 `visitStruct` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitStruct(cstruct: CjStruct) {
        super.visitStruct(cstruct, Unit)
    }

    /**
     * 访问 `visitEnum` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitEnum(cenum: CjEnum) {
        super.visitEnum(cenum, Unit)
    }

    /**
     * 访问 `visitInterface` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitInterface(cinterface: CjInterface) {
        super.visitInterface(cinterface, Unit)
    }

    /**
     * 访问 `visitDeclaration` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitDeclaration(dcl: CjDeclaration) {
        super.visitDeclaration(dcl, Unit)
    }

    /**
     * 访问 `visitTypeAlias` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTypeAlias(typeAlias: CjTypeAlias) {
        super.visitTypeAlias(typeAlias, Unit)
    }

    /**
     * 访问 `visitFinalizer` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitFinalizer(constructor: CjFinalizer) {
        super.visitFinalizer(constructor, Unit)
    }

    /**
     * 访问 `visitSecondaryConstructor` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
        super.visitSecondaryConstructor(constructor, Unit)
    }

    /**
     * 访问 `visitPrimaryConstructor` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPrimaryConstructor(constructor: CjPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor, Unit)
    }

    /**
     * 访问 `visitMacroDeclaration` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitMacroDeclaration(function: CjMacroDeclaration) {
        super.visitMacroDeclaration(function, Unit)
    }

    /**
     * 访问 `visitNamedFunction` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitNamedFunction(function: CjNamedFunction) {
        super.visitNamedFunction(function, Unit)
    }

    /**
     * 访问 `visitCjFile` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitCjFile(file: CjFile) {
        super.visitCjFile(file, Unit)
    }

    /**
     * 访问 `visitImportAlias` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitImportAlias(importAlias: CjImportAlias) {
        super.visitImportAlias(importAlias, Unit)
    }

    /**
     * 访问 `visitImportDirective` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitImportDirective(importDirective: CjImportDirective) {
        super.visitImportDirective(importDirective, Unit)
    }


    /**
     * 访问 `visitImportList` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitImportList(importList: CjImportList) {
        super.visitImportList(importList, Unit)
    }

    /**
     * 访问 `visitClassBody` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitClassBody(classBody: CjAbstractClassBody) {
        super.visitClassBody(classBody, Unit)
    }

    /**
     * 访问 `visitModifierList` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitModifierList(list: CjModifierList) {
        super.visitModifierList(list, Unit)
    }

    /**
     * 访问 `visitConstructorCalleeExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitConstructorCalleeExpression(constructorCalleeExpression: CjConstructorCalleeExpression) {
        super.visitConstructorCalleeExpression(constructorCalleeExpression, Unit)
    }

    /**
     * 访问 `visitTypeParameterList` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTypeParameterList(list: CjTypeParameterList) {
        super.visitTypeParameterList(list, Unit)
    }

    /**
     * 访问 `visitTypeParameter` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTypeParameter(parameter: CjTypeParameter) {
        super.visitTypeParameter(parameter, Unit)
    }

    /**
     * 访问 `visitEnumConstructor` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitEnumConstructor(enumConstructor: CjEnumConstructor) {
        super.visitEnumConstructor(enumConstructor, Unit)
    }

    /**
     * 访问 `visitParameterList` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitParameterList(list: CjParameterList) {
        super.visitParameterList(list, Unit)
    }

    /**
     * 访问 `visitParameter` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitParameter(parameter: CjParameter) {
        super.visitParameter(parameter, Unit)
    }

    /**
     * 访问 `visitSuperTypeList` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitSuperTypeList(list: CjSuperTypeList) {
        super.visitSuperTypeList(list, Unit)
    }

    /**
     * 访问 `visitSuperTypeListEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry) {
        super.visitSuperTypeListEntry(specifier, Unit)
    }

    /**
     * 访问 `visitSuperTypeCallEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry) {
        super.visitSuperTypeCallEntry(call, Unit)
    }

    /**
     * 访问 `visitSuperTypeEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitSuperTypeEntry(specifier: CjSuperTypeEntry) {
        super.visitSuperTypeEntry(specifier, Unit)
    }


    /**
     * 访问 `visitTypeReference` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTypeReference(typeReference: CjTypeReference) {
        super.visitTypeReference(typeReference, Unit)
    }

    /**
     * 访问 `visitValueArgumentList` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitValueArgumentList(list: CjValueArgumentList) {
        super.visitValueArgumentList(list, Unit)
    }

    /**
     * 访问 `visitExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitExpression(expression: CjExpression) {
        super.visitExpression(expression, Unit)
    }

    /**
     * 访问 `visitLoopExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitLoopExpression(loopExpression: CjLoopExpression) {
        super.visitLoopExpression(loopExpression, Unit)
    }

    /**
     * 访问 `visitConstantExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitConstantExpression(expression: CjConstantExpression) {
        super.visitConstantExpression(expression, Unit)
    }

    /**
     * 访问 `visitSimpleNameExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
        super.visitSimpleNameExpression(expression, Unit)
    }

    /**
     * 访问 `visitReferenceExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitReferenceExpression(expression: CjReferenceExpression) {
        super.visitReferenceExpression(expression, Unit)
    }

    /**
     * 访问 `visitPrefixExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPrefixExpression(expression: CjPrefixExpression) {
        super.visitPrefixExpression(expression, Unit)
    }

    /**
     * 访问 `visitPostfixExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPostfixExpression(expression: CjPostfixExpression) {
        super.visitPostfixExpression(expression, Unit)
    }

    /**
     * 访问 `visitUnaryExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitUnaryExpression(expression: CjUnaryExpression) {
        super.visitUnaryExpression(expression, Unit)
    }

    /**
     * 访问 `visitBinaryExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitBinaryExpression(expression: CjBinaryExpression) {
        super.visitBinaryExpression(expression, Unit)
    }

    /**
     * 访问 `visitReturnExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitReturnExpression(expression: CjReturnExpression) {
        super.visitReturnExpression(expression, Unit)
    }

    /**
     * 访问 `visitExpressionWithLabel` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitExpressionWithLabel(expression: CjExpressionWithLabel) {
        super.visitExpressionWithLabel(expression, Unit)
    }

    /**
     * 访问 `visitThrowExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitThrowExpression(expression: CjThrowExpression) {
        super.visitThrowExpression(expression, Unit)
    }

    /**
     * 访问 `visitPerformExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPerformExpression(expression: CjPerformExpression) {
        super.visitPerformExpression(expression, Unit)
    }

    /**
     * 访问 `visitResumeExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitResumeExpression(expression: CjResumeExpression) {
        super.visitResumeExpression(expression, Unit)
    }

    /**
     * 访问 `visitBreakExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitBreakExpression(expression: CjBreakExpression) {
        super.visitBreakExpression(expression, Unit)
    }

    /**
     * 访问 `visitContinueExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitContinueExpression(expression: CjContinueExpression) {
        super.visitContinueExpression(expression, Unit)
    }

    /**
     * 访问 `visitIfExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitIfExpression(expression: CjIfExpression) {
        super.visitIfExpression(expression, Unit)
    }

    /**
     * 访问 `visitMatchExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitMatchExpression(expression: CjMatchExpression) {
        super.visitMatchExpression(expression, Unit)
    }

    /**
     * 访问 `visitCollectionLiteralExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitCollectionLiteralExpression(expression: CjCollectionLiteralExpression) {
        super.visitCollectionLiteralExpression(expression, Unit)
    }

    /**
     * 访问 `visitTryExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTryExpression(expression: CjTryExpression) {
        super.visitTryExpression(expression, Unit)
    }

    /**
     * 访问 `visitForExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitForExpression(expression: CjForExpression) {
        super.visitForExpression(expression, Unit)
    }

    /**
     * 访问 `visitWhileExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitWhileExpression(expression: CjWhileExpression) {
        super.visitWhileExpression(expression, Unit)
    }

    /**
     * 访问 `visitDoWhileExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitDoWhileExpression(expression: CjDoWhileExpression) {
        super.visitDoWhileExpression(expression, Unit)
    }

    /**
     * 访问 `visitCallExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitCallExpression(expression: CjCallExpression) {
        super.visitCallExpression(expression, Unit)
    }

    /**
     * 访问 `visitArrayAccessExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitArrayAccessExpression(expression: CjArrayAccessExpression) {
        super.visitArrayAccessExpression(expression, Unit)
    }

    /**
     * 访问 `visitQualifiedExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitQualifiedExpression(expression: CjQualifiedExpression) {
        super.visitQualifiedExpression(expression, Unit)
    }

    /**
     * 访问 `visitLambdaExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitLambdaExpression(lambdaExpression: CjLambdaExpression) {
        super.visitLambdaExpression(lambdaExpression, Unit)
    }

    /**
     * 将带数据参数的 `visitLambdaExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitLambdaExpression(expression: CjLambdaExpression, data: Unit?): Unit {
        visitLambdaExpression(expression)

    }

    /**
     * 访问 `visitDotQualifiedExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression, Unit)
    }

    /**
     * 访问 `visitBlockExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitBlockExpression(expression: CjBlockExpression) {
        super.visitBlockExpression(expression, Unit)
    }

    /**
     * 访问 `visitCatchSection` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitCatchSection(catchClause: CjCatchClause) {
        super.visitCatchSection(catchClause, Unit)
    }

    /**
     * 访问 `visitHandleClause` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitHandleClause(handleClause: CjHandleClause) {
        super.visitHandleClause(handleClause, Unit)
    }

    /**
     * 访问 `visitCommandTypePattern` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitCommandTypePattern(commandTypePattern: CjCommandTypePattern) {
        super.visitCommandTypePattern(commandTypePattern, Unit)
    }

    /**
     * 访问 `visitFinallySection` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitFinallySection(finallySection: CjFinallySection) {
        super.visitFinallySection(finallySection, Unit)
    }

    /**
     * 访问 `visitTypeArgumentList` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitTypeArgumentList(typeArgumentList: CjTypeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList, Unit)
    }

    /**
     * 访问 `visitThisExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitThisExpression(expression: CjThisExpression) {
        super.visitThisExpression(expression, Unit)
    }

    /**
     * 访问 `visitSuperExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitSuperExpression(expression: CjSuperExpression) {
        super.visitSuperExpression(expression, Unit)
    }

    /**
     * 访问 `visitParenthesizedExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitParenthesizedExpression(expression: CjParenthesizedExpression) {
        super.visitParenthesizedExpression(expression, Unit)
    }


    /**
     * 访问 `visitTypeConstraintList` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTypeConstraintList(list: CjTypeConstraintList) {
        super.visitTypeConstraintList(list, Unit)
    }

    /**
     * 访问 `visitTypeConstraint` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open   fun visitTypeConstraint(constraint: CjTypeConstraint) {
        super.visitTypeConstraint(constraint, Unit)
    }

    /**
     * 访问 `visitUserType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitUserType(type: CjUserType) {
        super.visitUserType(type, Unit)
    }

    /**
     * 访问 `visitVArrayType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitVArrayType(type: CjVArrayType) {
        super.visitVArrayType(type, Unit)
    }

    /**
     * 访问 `visitThisType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitThisType(type: CjThisType) {
        super.visitThisType(type, Unit)
    }

    /**
     * 访问 `visitBasicType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitBasicType(type: CjBasicType) {
        super.visitBasicType(type, Unit)
    }

    /**
     * 访问 `visitBinaryWithTypeRHSExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitBinaryWithTypeRHSExpression(expression: CjBinaryExpressionWithTypeRHS) {
        super.visitBinaryWithTypeRHSExpression(expression, Unit)
    }

    /**
     * 访问 `visitStringTemplateExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitStringTemplateExpression(expression: CjStringTemplateExpression) {
        super.visitStringTemplateExpression(expression, Unit)
    }

    /**
     * 访问 `visitNamedDeclaration` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitNamedDeclaration(declaration: CjNamedDeclaration) {
        super.visitNamedDeclaration(declaration, Unit)
    }

    /**
     * 访问 `visitTypeProjection` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitTypeProjection(typeProjection: CjTypeProjection) {
        super.visitTypeProjection(typeProjection, Unit)
    }

    /**
     * 访问 `visitWhenEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitWhenEntry(matchEntry: CjMatchEntry) {
        super.visitMatchEntry(matchEntry, Unit)
    }

    /**
     * 访问 `visitIsExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitIsExpression(expression: CjIsExpression) {
        super.visitIsExpression(expression, Unit)
    }

    /**
     * 访问 `visitStringTemplateEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitStringTemplateEntry(entry: CjStringTemplateEntry) {
        super.visitStringTemplateEntry(entry, Unit)
    }

    /**
     * 访问 `visitStringTemplateEntryWithExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitStringTemplateEntryWithExpression(entry: CjStringTemplateEntryWithExpression) {
        super.visitStringTemplateEntryWithExpression(entry, Unit)
    }

    /**
     * 访问 `visitBlockStringTemplateEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitBlockStringTemplateEntry(entry: CjBlockStringTemplateEntry) {
        super.visitBlockStringTemplateEntry(entry, Unit)
    }

    /**
     * 访问 `visitSimpleNameStringTemplateEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitSimpleNameStringTemplateEntry(entry: CjSimpleNameStringTemplateEntry) {
        super.visitSimpleNameStringTemplateEntry(entry, Unit)
    }

    /**
     * 访问 `visitLiteralStringTemplateEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitLiteralStringTemplateEntry(entry: CjLiteralStringTemplateEntry) {
        super.visitLiteralStringTemplateEntry(entry, Unit)
    }

    /**
     * 访问 `visitEscapeStringTemplateEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry) {
        super.visitEscapeStringTemplateEntry(entry, Unit)
    }

    /**
     * 访问 `visitPackageDirective` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPackageDirective(directive: CjPackageDirective) {
        super.visitPackageDirective(directive, Unit)
    }

    // hidden methods
    /**
     * 将带数据参数的 `visitCjElement` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitCjElement(element: CjElement, data: Unit?): Unit {
        visitCjElement(element)

    }

    /**
     * 将带数据参数的 `visitDeclaration` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitDeclaration(dcl: CjDeclaration, data: Unit?): Unit {
        visitDeclaration(dcl)

    }

    /**
     * 将带数据参数的 `visitProperty` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitProperty(property: CjProperty, data: Unit?): Unit {
        visitProperty(property)

    }

    /**
     * 将带数据参数的 `visitPropertyAccessor` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPropertyAccessor(accessor: CjPropertyAccessor, data: Unit?): Unit {
        visitPropertyAccessor(accessor)

    }

    /**
     * 将带数据参数的 `visitPatternVariable` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPatternVariable(variable: CjPatternVariable, data: Unit?) {
        visitPatternVariable(variable)
    }

    /**
     * 将带数据参数的 `visitFieldVariable` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitFieldVariable(field: CjFieldVariable, data: Unit?): Unit {
        visitFieldVariable(field)
    }

    /**
     * 将带数据参数的 `visitFinalizer` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitFinalizer(constructor: CjFinalizer, data: Unit?): Unit {
        visitFinalizer(constructor)

    }

    /**
     * 将带数据参数的 `visitSecondaryConstructor` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: Unit?): Unit {
        visitSecondaryConstructor(constructor)

    }

    /**
     * 将带数据参数的 `visitPrimaryConstructor` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: Unit?): Unit {
        visitPrimaryConstructor(constructor)

    }

    /**
     * 将带数据参数的 `visitMacroExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitMacroExpression(expression: CjMacroExpression, data: Unit?) {
        visitMacroExpression(expression)
    }

    /**
     * 访问 `visitMacroExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitMacroExpression(expression: CjMacroExpression) {
        super.visitMacroExpression(expression, Unit)
    }

    /**
     * 将带数据参数的 `visitMacroDeclaration` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitMacroDeclaration(macroDeclaration: CjMacroDeclaration, data: Unit?): Unit {
        visitMacroDeclaration(macroDeclaration)

    }

    /**
     * 将带数据参数的 `visitNamedFunction` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitNamedFunction(function: CjNamedFunction, data: Unit?): Unit {
        visitNamedFunction(function)

    }

    /**
     * 将带数据参数的 `visitTypeAlias` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeAlias(typeAlias: CjTypeAlias, data: Unit?): Unit {
        visitTypeAlias(typeAlias)

    }

    /**
     * 将带数据参数的 `visitCjFile` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitCjFile(file: CjFile, data: Unit?): Unit {
        visitCjFile(file)

    }


    /**
     * 将带数据参数的 `visitImportDirective` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitImportDirective(importDirective: CjImportDirective, data: Unit?): Unit {
        visitImportDirective(importDirective)

    }

    /**
     * 将带数据参数的 `visitImportList` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitImportList(importList: CjImportList, data: Unit?): Unit {
        visitImportList(importList)

    }

    /**
     * 将带数据参数的 `visitClassBody` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitClassBody(classBody: CjAbstractClassBody, data: Unit?): Unit {
        visitClassBody(classBody)

    }

    /**
     * 将带数据参数的 `visitModifierList` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitModifierList(list: CjModifierList, data: Unit?): Unit {
        visitModifierList(list)

    }

    /**
     * 将带数据参数的 `visitConstructorCalleeExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitConstructorCalleeExpression(
        constructorCalleeExpression: CjConstructorCalleeExpression,
        data: Unit?,
    ): Unit {
        visitConstructorCalleeExpression(constructorCalleeExpression)

    }

    /**
     * 将带数据参数的 `visitTypeParameterList` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeParameterList(list: CjTypeParameterList, data: Unit?): Unit {
        visitTypeParameterList(list)

    }

    /**
     * 将带数据参数的 `visitTypeParameter` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeParameter(parameter: CjTypeParameter, data: Unit?): Unit {
        visitTypeParameter(parameter)

    }

    /**
     * 将带数据参数的 `visitEnumConstructor` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitEnumConstructor(enumConstructor: CjEnumConstructor, data: Unit?): Unit {
        visitEnumConstructor(enumConstructor)

    }

    /**
     * 将带数据参数的 `visitParameterList` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitParameterList(parameterList: CjParameterList, data: Unit?): Unit {
        visitParameterList(parameterList)

    }

    /**
     * 将带数据参数的 `visitParameter` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitParameter(parameter: CjParameter, data: Unit?): Unit {
        visitParameter(parameter)

    }

    /**
     * 将带数据参数的 `visitSuperTypeList` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitSuperTypeList(list: CjSuperTypeList, data: Unit?): Unit {
        visitSuperTypeList(list)

    }

    /**
     * 将带数据参数的 `visitSuperTypeListEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry, data: Unit?): Unit {
        visitSuperTypeListEntry(specifier)

    }

    /**
     * 将带数据参数的 `visitSuperTypeCallEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry, data: Unit?): Unit {
        visitSuperTypeCallEntry(call)

    }

    /**
     * 将带数据参数的 `visitSuperTypeEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitSuperTypeEntry(specifier: CjSuperTypeEntry, data: Unit?): Unit {
        visitSuperTypeEntry(specifier)

    }

    /**
     * 将带数据参数的 `visitTypeReference` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeReference(typeReference: CjTypeReference, data: Unit?): Unit {
        visitTypeReference(typeReference)

    }

    /**
     * 将带数据参数的 `visitValueArgumentList` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitValueArgumentList(list: CjValueArgumentList, data: Unit?): Unit {
        visitValueArgumentList(list)

    }

    /**
     * 将带数据参数的 `visitExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitExpression(expression: CjExpression, data: Unit?): Unit {
        visitExpression(expression)

    }

    /**
     * 将带数据参数的 `visitLoopExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitLoopExpression(loopExpression: CjLoopExpression, data: Unit?): Unit {
        visitLoopExpression(loopExpression)

    }

    /**
     * 将带数据参数的 `visitConstantExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitConstantExpression(expression: CjConstantExpression, data: Unit?): Unit {
        visitConstantExpression(expression)

    }

    /**
     * 将带数据参数的 `visitSimpleNameExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitSimpleNameExpression(expression: CjSimpleNameExpression, data: Unit?): Unit {
        visitSimpleNameExpression(expression)

    }

    /**
     * 将带数据参数的 `visitReferenceExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitReferenceExpression(
        expression: CjReferenceExpression,
        data: Unit?,
    ): Unit {
        visitReferenceExpression(expression)

    }

    /**
     * 将带数据参数的 `visitPrefixExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPrefixExpression(expression: CjPrefixExpression, data: Unit?): Unit {
        visitPrefixExpression(expression)

    }

    /**
     * 将带数据参数的 `visitPostfixExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPostfixExpression(expression: CjPostfixExpression, data: Unit?): Unit {
        visitPostfixExpression(expression)

    }

    /**
     * 将带数据参数的 `visitUnaryExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitUnaryExpression(expression: CjUnaryExpression, data: Unit?): Unit {
        visitUnaryExpression(expression)

    }

    /**
     * 将带数据参数的 `visitBinaryExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitBinaryExpression(expression: CjBinaryExpression, data: Unit?): Unit {
        visitBinaryExpression(expression)

    }

    /**
     * 将带数据参数的 `visitReturnExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitReturnExpression(expression: CjReturnExpression, data: Unit?): Unit {
        visitReturnExpression(expression)

    }

    /**
     * 将带数据参数的 `visitExpressionWithLabel` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitExpressionWithLabel(expression: CjExpressionWithLabel, data: Unit?): Unit {
        visitExpressionWithLabel(expression)

    }

    /**
     * 将带数据参数的 `visitThrowExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitThrowExpression(expression: CjThrowExpression, data: Unit?): Unit {
        visitThrowExpression(expression)

    }

    /**
     * 将带数据参数的 `visitPerformExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPerformExpression(expression: CjPerformExpression, data: Unit?): Unit {
        visitPerformExpression(expression)

    }

    /**
     * 将带数据参数的 `visitResumeExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitResumeExpression(expression: CjResumeExpression, data: Unit?): Unit {
        visitResumeExpression(expression)

    }

    /**
     * 将带数据参数的 `visitBreakExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitBreakExpression(expression: CjBreakExpression, data: Unit?): Unit {
        visitBreakExpression(expression)

    }

    /**
     * 将带数据参数的 `visitContinueExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitContinueExpression(expression: CjContinueExpression, data: Unit?): Unit {
        visitContinueExpression(expression)

    }

    /**
     * 将带数据参数的 `visitIfExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitIfExpression(expression: CjIfExpression, data: Unit?): Unit {
        visitIfExpression(expression)

    }

    /**
     * 将带数据参数的 `visitMatchExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitMatchExpression(expression: CjMatchExpression, data: Unit?): Unit {
        visitMatchExpression(expression)

    }

    /**
     * 将带数据参数的 `visitCollectionLiteralExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitCollectionLiteralExpression(
        expression: CjCollectionLiteralExpression,
        data: Unit?,
    ): Unit {
        visitCollectionLiteralExpression(expression)

    }

    /**
     * 将带数据参数的 `visitTryExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTryExpression(expression: CjTryExpression, data: Unit?): Unit {
        visitTryExpression(expression)

    }

    /**
     * 将带数据参数的 `visitForExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitForExpression(expression: CjForExpression, data: Unit?): Unit {
        visitForExpression(expression)

    }

    /**
     * 将带数据参数的 `visitWhileExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitWhileExpression(expression: CjWhileExpression, data: Unit?): Unit {
        visitWhileExpression(expression)

    }

    /**
     * 将带数据参数的 `visitDoWhileExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitDoWhileExpression(expression: CjDoWhileExpression, data: Unit?): Unit {
        visitDoWhileExpression(expression)

    }

    /**
     * 将带数据参数的 `visitCallExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitCallExpression(expression: CjCallExpression, data: Unit?): Unit {
        visitCallExpression(expression)

    }

    /**
     * 将带数据参数的 `visitArrayAccessExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitArrayAccessExpression(
        expression: CjArrayAccessExpression,
        data: Unit?,
    ): Unit {
        visitArrayAccessExpression(expression)

    }

    /**
     * 将带数据参数的 `visitQualifiedExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitQualifiedExpression(expression: CjQualifiedExpression, data: Unit?): Unit {
        visitQualifiedExpression(expression)

    }

    /**
     * 将带数据参数的 `visitDotQualifiedExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression, data: Unit?): Unit {
        visitDotQualifiedExpression(expression)

    }

    /**
     * 将带数据参数的 `visitBlockExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitBlockExpression(expression: CjBlockExpression, data: Unit?): Unit {
        visitBlockExpression(expression)

    }

    /**
     * 将带数据参数的 `visitCatchSection` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitCatchSection(catchClause: CjCatchClause, data: Unit?): Unit {
        visitCatchSection(catchClause)

    }

    /**
     * 将带数据参数的 `visitHandleClause` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitHandleClause(handleClause: CjHandleClause, data: Unit?): Unit {
        visitHandleClause(handleClause)

    }

    /**
     * 将带数据参数的 `visitCommandTypePattern` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitCommandTypePattern(commandTypePattern: CjCommandTypePattern, data: Unit?): Unit {
        visitCommandTypePattern(commandTypePattern)

    }

    /**
     * 将带数据参数的 `visitFinallySection` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitFinallySection(finallySection: CjFinallySection, data: Unit?): Unit {
        visitFinallySection(finallySection)

    }

    /**
     * 将带数据参数的 `visitTypeArgumentList` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeArgumentList(
        typeArgumentList: CjTypeArgumentList,
        data: Unit?,
    ): Unit {
        visitTypeArgumentList(typeArgumentList)

    }

    /**
     * 将带数据参数的 `visitThisExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitThisExpression(expression: CjThisExpression, data: Unit?): Unit {
        visitThisExpression(expression)

    }

    /**
     * 将带数据参数的 `visitSuperExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitSuperExpression(expression: CjSuperExpression, data: Unit?): Unit {
        visitSuperExpression(expression)

    }

    /**
     * 将带数据参数的 `visitParenthesizedExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitParenthesizedExpression(
        expression: CjParenthesizedExpression,
        data: Unit?,
    ): Unit {
        visitParenthesizedExpression(expression)

    }


    /**
     * 将带数据参数的 `visitTypeConstraintList` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeConstraintList(list: CjTypeConstraintList, data: Unit?): Unit {
        visitTypeConstraintList(list)

    }

    /**
     * 将带数据参数的 `visitTypeConstraint` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeConstraint(constraint: CjTypeConstraint, data: Unit?): Unit {
        visitTypeConstraint(constraint)

    }

    /**
     * 将带数据参数的 `visitUserType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitUserType(type: CjUserType, data: Unit?): Unit {
        visitUserType(type)

    }

    /**
     * 将带数据参数的 `visitVArrayType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitVArrayType(optionType: CjVArrayType, data: Unit?): Unit {
        visitVArrayType(optionType)

    }

    /**
     * 将带数据参数的 `visitThisType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitThisType(thisType: CjThisType, data: Unit?): Unit {
        visitThisType(thisType)

    }

    /**
     * 将带数据参数的 `visitBasicType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitBasicType(basicType: CjBasicType, data: Unit?): Unit {
        visitBasicType(basicType)

    }

    /**
     * 将带数据参数的 `visitBinaryWithTypeRHSExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitBinaryWithTypeRHSExpression(
        expression: CjBinaryExpressionWithTypeRHS,
        data: Unit?,
    ): Unit {
        visitBinaryWithTypeRHSExpression(expression)

    }

    /**
     * 将带数据参数的 `visitStringTemplateExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitStringTemplateExpression(
        expression: CjStringTemplateExpression,
        data: Unit?,
    ): Unit {
        visitStringTemplateExpression(expression)

    }

    /**
     * 将带数据参数的 `visitNamedDeclaration` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitNamedDeclaration(declaration: CjNamedDeclaration, data: Unit?): Unit {
        visitNamedDeclaration(declaration)

    }

    /**
     * 将带数据参数的 `visitTypeProjection` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeProjection(typeProjection: CjTypeProjection, data: Unit?): Unit {
        visitTypeProjection(typeProjection)

    }

    /**
     * 访问 `visitMatchEntry` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    fun visitMatchEntry(matchEntry: CjMatchEntry) {
        super.visitMatchEntry(matchEntry, Unit)
    }

    /**
     * 将带数据参数的 `visitMatchEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitMatchEntry(matchEntry: CjMatchEntry, data: Unit?): Unit {
        visitMatchEntry(matchEntry)

    }

    /**
     * 将带数据参数的 `visitIsExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitIsExpression(expression: CjIsExpression, data: Unit?): Unit {
        visitIsExpression(expression)

    }

    /**
     * 将带数据参数的 `visitStringTemplateEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitStringTemplateEntry(entry: CjStringTemplateEntry, data: Unit?): Unit {
        visitStringTemplateEntry(entry)

    }

    /**
     * 将带数据参数的 `visitStringTemplateEntryWithExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitStringTemplateEntryWithExpression(
        entry: CjStringTemplateEntryWithExpression,
        data: Unit?,
    ): Unit {
        visitStringTemplateEntryWithExpression(entry)

    }

    /**
     * 将带数据参数的 `visitBlockStringTemplateEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitBlockStringTemplateEntry(
        entry: CjBlockStringTemplateEntry,
        data: Unit?,
    ): Unit {
        visitBlockStringTemplateEntry(entry)

    }

    /**
     * 将带数据参数的 `visitSimpleNameStringTemplateEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitSimpleNameStringTemplateEntry(
        entry: CjSimpleNameStringTemplateEntry,
        data: Unit?,
    ): Unit {
        visitSimpleNameStringTemplateEntry(entry)

    }

    /**
     * 将带数据参数的 `visitLiteralStringTemplateEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitLiteralStringTemplateEntry(
        entry: CjLiteralStringTemplateEntry,
        data: Unit?,
    ): Unit {
        visitLiteralStringTemplateEntry(entry)

    }

    /**
     * 将带数据参数的 `visitEscapeStringTemplateEntry` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitEscapeStringTemplateEntry(
        entry: CjEscapeStringTemplateEntry,
        data: Unit?,
    ): Unit {
        visitEscapeStringTemplateEntry(entry)

    }

    /**
     * 将带数据参数的 `visitPackageDirective` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPackageDirective(packageDirective: CjPackageDirective, data: Unit?): Unit {
        visitPackageDirective(packageDirective)

    }

    /**
     * 将带数据参数的 `visitTypeStatement` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitTypeStatement(typeStatement: CjTypeStatement, data: Unit?): Unit {
        visitTypeStatement(typeStatement)

    }

    /**
     * 将带数据参数的 `visitClass` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitClass(cclass: CjClass, data: Unit?): Unit {
        visitClass(cclass)

    }

    /**
     * 将带数据参数的 `visitStruct` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitStruct(cstruct: CjStruct, data: Unit?): Unit {
        visitStruct(cstruct)

    }

    /**
     * 将带数据参数的 `visitEnum` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitEnum(cenum: CjEnum, data: Unit?): Unit {
        visitEnum(cenum)

    }

    /**
     * 将带数据参数的 `visitInterface` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitInterface(cinterface: CjInterface, data: Unit?): Unit {
        visitInterface(cinterface)

    }

    /**
     * 将带数据参数的 `visitCasePattern` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitCasePattern(element: CjCasePatternElement, data: Unit?): Unit {
        visitCasePattern(element)

    }

    /**
     * 访问 `visitCasePattern` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitCasePattern(element: CjCasePatternElement) {
        super.visitCasePattern(element, Unit)
    }

    /**
     * 将带数据参数的 `visitPatternByBinding` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPatternByBinding(element: CjBindingPattern, data: Unit?): Unit {
        visitPatternByBinding(element)

    }

    /**
     * 访问 `visitPatternByBinding` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPatternByBinding(element: CjBindingPattern) {
        super.visitPatternByBinding(element, Unit)
    }

    /**
     * 将带数据参数的 `visitPatternByVarOrEnum` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPatternByVarOrEnum(element: CjVarOrEnumPattern, data: Unit?): Unit {
        visitPatternByVarOrEnum(element)

    }

    /**
     * 访问 `visitPatternByVarOrEnum` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPatternByVarOrEnum(element: CjVarOrEnumPattern) {
        super.visitPatternByVarOrEnum(element, Unit)
    }

    /**
     * 将带数据参数的 `visitPatternByConstant` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPatternByConstant(element: CjConstantPattern, data: Unit?): Unit {
        visitPatternByConstant(element)

    }

    /**
     * 访问 `visitPatternByConstant` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPatternByConstant(element: CjConstantPattern) {
        super.visitPatternByConstant(element, Unit)
    }

    /**
     * 将带数据参数的 `visitMatchConditionWithExpression` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitMatchConditionWithExpression(condition: CjMatchConditionWithExpression, data: Unit?): Unit {
        visitMatchConditionWithExpression(condition)

    }

    /**
     * 将带数据参数的 `visitPatternByEnum` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPatternByEnum(element: CjEnumPattern, data: Unit?): Unit {
        visitPatternByEnum(element)

    }

    /**
     * 访问 `visitMatchConditionWithExpression` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitMatchConditionWithExpression(element: CjMatchConditionWithExpression) {
        super.visitMatchConditionWithExpression(element, Unit)
    }

    /**
     * 访问 `visitPatternByEnum` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPatternByEnum(element: CjEnumPattern) {
        super.visitPatternByEnum(element, Unit)
    }

    /**
     * 将带数据参数的 `visitPatternByTuple` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPatternByTuple(element: CjTuplePattern, data: Unit?): Unit {
        visitPatternByTuple(element)

    }

    /**
     * 访问 `visitPatternByTuple` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPatternByTuple(element: CjTuplePattern) {
        super.visitPatternByTuple(element, Unit)
    }

    /**
     * 将带数据参数的 `visitPatternByWildcard` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPatternByWildcard(element: CjWildcardPattern, data: Unit?): Unit {
        visitPatternByWildcard(element)

    }

    /**
     * 将带数据参数的 `visitPatternByType` 访问回调转发为 Unit visitor 的无数据访问入口。
     */
    override fun visitPatternByType(element: CjTypePattern, data: Unit?): Unit {
        visitPatternByType(element)

    }

    /**
     * 访问 `visitPatternByType` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPatternByType(element: CjTypePattern) {
        super.visitPatternByType(element, Unit)
    }

    /**
     * 访问 `visitPatternByWildcard` 对应的 PSI 节点，作为 Unit visitor 的可覆写处理入口。
     */
    open fun visitPatternByWildcard(element: CjWildcardPattern) {
        super.visitPatternByWildcard(element, Unit)
    }
}
