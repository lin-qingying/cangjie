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

import com.intellij.psi.PsiElementVisitor

/**
 * 仓颉语言 PSI 树访问者基类
 *
 * 实现访问者模式，用于遍历和处理仓颉语言的程序结构接口（PSI）树。
 *
 * ## 类型参数
 * - `R`: 访问方法的返回值类型
 * - `D`: 访问过程中传递的数据类型
 *
 * ## 使用方式
 * 继承此类并重写需要处理的特定节点类型的 visit 方法。访问器遵循层次结构，
 * 子节点类型的默认实现会调用父节点类型的方法。
 *
 * ## 示例
 * ```kotlin
 * class MyVisitor : CjVisitor<Unit, MyData>() {
 *     override fun visitClass(klass: CjClass, data: MyData): Unit? {
 *         // 处理类声明
 *         return super.visitClass(klass, data)
 *     }
 * }
 * ```
 *
 * @see CjElement
 * @see PsiElementVisitor
 */
open class CjVisitor<R, D> : PsiElementVisitor() {

    /**
     * 访问仓颉语言的 PSI 元素
     *
     * 这是所有仓颉语言元素的基础访问方法，其他 visit 方法最终都会委托到这里。
     *
     * @param element 要访问的仓颉元素
     * @param data 传递给访问器的上下文数据
     * @return 访问结果，默认返回 null
     */
    open fun visitCjElement(element: CjElement, data: D): R? {
        visitElement(element)
        return null
    }

    /**
     * 提供 `visitQuoteInterpolate` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitQuoteInterpolate(expression: CjQuoteInterpolate, data: D): R? {
        return visitCjElement(expression, data)
    }


    /**
     * 访问通用仓颉文件
     *
     * 用于支持 [CjCommonFile] 的实现。通常不应直接使用此方法，
     * 因为 [CjCommonFile] 本身不应该被直接使用。
     *
     * @param file 仓颉通用文件
     * @return 访问结果，默认返回 null
     */
    @Suppress("DEPRECATION")
    fun visitCjCommonFile(file: CjCommonFile): R? {
        visitFile(file)
        return null
    }

    /**
     * 提供 `visitSynchronizedExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSynchronizedExpression(expression: CjSynchronizedExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitPatternGuard` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternGuard(expression: CjPatternGuard, data: D): R? {
        return visitCjElement(expression, data)
    }

    /**
     * 提供 `visitSafeQualifiedExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSafeQualifiedExpression(expression: CjSafeQualifiedExpression, data: D): R? {
        return visitQualifiedExpression(expression, data)
    }

    /**
     * 提供 `visitOptionalExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitOptionalExpression(expression: CjOptionalExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitOptionalChainExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitOptionalChainExpression(expression: CjOptionalChainExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitMacroExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitMacroExpression(expression: CjMacroExpression, data: D): R? {
        return visitCjElement(expression, data)
    }

    /**
     * 提供 `visitQuoteExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitQuoteExpression(element: CjQuoteExpression, data: D): R? {
        return visitExpression(element, data)
    }

    /**
     * 提供 `visitCasePattern` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitCasePattern(element: CjCasePatternElement, data: D): R? {
        return visitCjElement(element, data)
    }

    /**
     * 提供 `visitPatternByConstant` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternByConstant(element: CjConstantPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    /**
     * 提供 `visitPatternByBinding` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternByBinding(element: CjBindingPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    /**
     * 提供 `visitPatternByVarOrEnum` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternByVarOrEnum(element: CjVarOrEnumPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    /**
     * 提供 `visitMatchConditionWithExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitMatchConditionWithExpression(condition: CjMatchConditionWithExpression, data: D): R? {
        return visitCasePattern(condition, data)
    }

    /**
     * 提供 `visitPatternByType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternByType(element: CjTypePattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    /**
     * 提供 `visitPatternByTuple` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternByTuple(element: CjTuplePattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    /**
     * 提供 `visitPatternByWildcard` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternByWildcard(element: CjWildcardPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    /**
     * 提供 `visitPatternByEnum` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternByEnum(element: CjEnumPattern, data: D): R? {
        return visitCasePattern(element, data)
    }

    /**
     * 提供 `visitOptionType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitOptionType(optionType: CjOptionType, data: D): R? {
        return visitTypeElement(optionType, data)
    }

    /**
     * 提供 `visitVArrayType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitVArrayType(optionType: CjVArrayType, data: D): R? {
        return visitTypeElement(optionType, data)
    }

    /**
     * 提供 `visitThisType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitThisType(thisType: CjThisType, data: D): R? {
        return visitTypeElement(thisType, data)
    }

    /**
     * 提供 `visitProperty` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitProperty(property: CjProperty, data: D): R? {
        return visitNamedDeclaration(property, data)
    }
    /**
     * 提供 `visitPatternVariable` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPatternVariable(variable: CjPatternVariable, data: D): R? {
        return visitVariable(variable, data)
    }
    /**
     * 提供 `visitFieldVariable` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitFieldVariable(field: CjFieldVariable, data: D): R? {
        return visitVariable(field, data)
    }
    /**
     * 提供 `visitVariable` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitVariable(variable: CjVariable<*>, data: D): R? {
        return visitNamedDeclaration(variable, data)
    }


    /**
     * 提供 `visitRangeExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitRangeExpression(expression: CjRangeExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitSpawnExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSpawnExpression(expression: CjSpawnExpression, data: D): R? {
        return visitCallExpression(expression, data)
    }

    /**
     * 提供 `visitLambdaExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitLambdaExpression(expression: CjLambdaExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitUnsafeExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitUnsafeExpression(expression: CjUnsafeExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitTupleExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTupleExpression(expression: CjTupleExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitTryResourceList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTryResourceList(resource: CjTryResourceList, data: D): R? {
        return visitCjElement(resource, data)
    }

    /**
     * 提供 `visitTryResource` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTryResource(resource: CjTryResource, data: D): R? {
        return visitCjElement(resource, data)
    }

    /**
     * 提供 `visitTryExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTryExpression(expression: CjTryExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitCatchSection` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitCatchSection(catchClause: CjCatchClause, data: D): R? {
        return visitCjElement(catchClause, data)
    }

    /**
     * 提供 `visitHandleClause` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitHandleClause(handleClause: CjHandleClause, data: D): R? {
        return visitCjElement(handleClause, data)
    }

    /**
     * 提供 `visitCommandTypePattern` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitCommandTypePattern(commandTypePattern: CjCommandTypePattern, data: D): R? {
        return visitCjElement(commandTypePattern, data)
    }

    /**
     * 提供 `visitFinallySection` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitFinallySection(finallySection: CjFinallySection, data: D): R? {
        return visitCjElement(finallySection, data)
    }

    /**
     * 提供 `visitMatchExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitMatchExpression(expression: CjMatchExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitLetExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitLetExpression(expression: CjLetExpression, data: D): R? {
        return visitCjElement(expression, data)
    }

    /**
     * 提供 `visitWhileExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitWhileExpression(expression: CjWhileExpression, data: D): R? {
        return visitLoopExpression(expression, data)
    }

    /**
     * 提供 `visitDoWhileExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitDoWhileExpression(expression: CjDoWhileExpression, data: D): R? {
        return visitLoopExpression(expression, data)
    }

    /**
     * 提供 `visitLoopExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitLoopExpression(loopExpression: CjLoopExpression, data: D): R? {
        return visitExpression(loopExpression, data)
    }

    /**
     * 提供 `visitForExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitForExpression(expression: CjForExpression, data: D): R? {
        return visitLoopExpression(expression, data)
    }

    /**
     * 提供 `visitIfExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitIfExpression(expression: CjIfExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitContinueExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitContinueExpression(expression: CjContinueExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    /**
     * 提供 `visitBreakExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitBreakExpression(expression: CjBreakExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    /**
     * 提供 `visitCollectionLiteralExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitCollectionLiteralExpression(expression: CjCollectionLiteralExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitThrowExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitThrowExpression(expression: CjThrowExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitPerformExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPerformExpression(expression: CjPerformExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitResumeExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitResumeExpression(expression: CjResumeExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitReturnExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitReturnExpression(expression: CjReturnExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    /**
     * 提供 `visitParenthesizedExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitParenthesizedExpression(expression: CjParenthesizedExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitStringTemplateExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitStringTemplateExpression(expression: CjStringTemplateExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitSuperExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSuperExpression(expression: CjSuperExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    /**
     * 提供 `visitThisExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitThisExpression(expression: CjThisExpression, data: D): R? {
        return visitExpressionWithLabel(expression, data)
    }

    /**
     * 提供 `visitExpressionWithLabel` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitExpressionWithLabel(expression: CjExpressionWithLabel, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitLiteralStringTemplateEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitLiteralStringTemplateEntry(entry: CjLiteralStringTemplateEntry, data: D): R? {
        return visitStringTemplateEntry(entry, data)
    }

    /**
     * 提供 `visitEscapeStringTemplateEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry, data: D): R? {
        return visitStringTemplateEntry(entry, data)
    }

    /**
     * 提供 `visitStringTemplateEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitStringTemplateEntry(entry: CjStringTemplateEntry, data: D): R? {
        return visitCjElement(entry, data)
    }

    /**
     * 提供 `visitStringTemplateEntryWithExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitStringTemplateEntryWithExpression(entry: CjStringTemplateEntryWithExpression, data: D): R? {
        return visitStringTemplateEntry(entry, data)
    }

    /**
     * 提供 `visitBlockStringTemplateEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitBlockStringTemplateEntry(entry: CjBlockStringTemplateEntry, data: D): R? {
        return visitStringTemplateEntryWithExpression(entry, data)
    }

    /**
     * 提供 `visitPostfixExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPostfixExpression(expression: CjPostfixExpression, data: D): R? {
        return visitUnaryExpression(expression, data)
    }

    /**
     * 提供 `visitConstantExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitConstantExpression(expression: CjConstantExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitValueArgumentList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitValueArgumentList(list: CjValueArgumentList, data: D): R? {
        return visitCjElement(list, data)
    }

    /**
     * 提供 `visitArrayAccessExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitArrayAccessExpression(expression: CjArrayAccessExpression, data: D): R? {
        return visitReferenceExpression(expression, data)
    }

    /**
     * 提供 `visitSuperTypeEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSuperTypeEntry(specifier: CjSuperTypeEntry, data: D): R? {
        return visitSuperTypeListEntry(specifier, data)
    }

    /**
     * 提供 `visitPrefixExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPrefixExpression(expression: CjPrefixExpression, data: D): R? {
        return visitUnaryExpression(expression, data)
    }

    /**
     * 提供 `visitUnaryExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitUnaryExpression(expression: CjUnaryExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitIsExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitIsExpression(expression: CjIsExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitBinaryExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitBinaryExpression(expression: CjBinaryExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitSuperTypeCallEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry, data: D): R? {
        return visitSuperTypeListEntry(call, data)
    }

    /**
     * 提供 `visitBinaryWithTypeRHSExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitBinaryWithTypeRHSExpression(expression: CjBinaryExpressionWithTypeRHS, data: D): R? {
        return visitExpression(expression, data)
    }




    /**
     * 提供 `visitSuperTypeListEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry, data: D): R? {
        return visitCjElement(specifier, data)
    }

    /**
     * 提供 `visitTypeProjection` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTypeProjection(typeProjection: CjTypeProjection, data: D): R? {
        return visitCjElement(typeProjection, data)
    }

    /**
     * 提供 `visitTypeParameterList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTypeParameterList(list: CjTypeParameterList, data: D): R? {
        return visitCjElement(list, data)
    }

    /**
     * 提供 `visitTypeParameter` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTypeParameter(parameter: CjTypeParameter, data: D): R? {
        return visitNamedDeclaration(parameter, data)
    }

    /**
     * 提供 `visitCjFile` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitCjFile(file: CjFile, data: D): R? {
        visitFile(file)
        return null
    }

    /**
     * 提供 `visitBlockExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitBlockExpression(expression: CjBlockExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 访问表达式节点
     *
     * 这是所有表达式节点的基础访问方法，包括字面量、运算表达式、
     * 函数调用、控制流表达式等。
     *
     * @param expression 要访问的表达式
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    open fun visitExpression(expression: CjExpression, data: D): R? {
        return visitCjElement(expression, data)
    }

    /**
     * 提供 `visitClassBody` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitClassBody(classBody: CjAbstractClassBody, data: D): R? {
        return visitCjElement(classBody, data)
    }

    /**
     * 提供 `visitEnumBody` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitEnumBody(enumBody: CjEnumBody, data: D): R? {
        return visitCjElement(enumBody, data)
    }

    /**
     * 提供 `visitModifierList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitModifierList(list: CjModifierList, data: D): R? {
        return visitCjElement(list, data)
    }



    /**
     * 提供 `visitTypeReference` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTypeReference(typeReference: CjTypeReference, data: D): R? {
        return visitCjElement(typeReference, data)
    }

    /**
     * 提供 `visitSimpleNameExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSimpleNameExpression(expression: CjSimpleNameExpression, data: D): R? {
        return visitReferenceExpression(expression, data)
    }

    /**
     * 提供 `visitReferenceExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitReferenceExpression(expression: CjReferenceExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitTypeConstraintList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTypeConstraintList(list: CjTypeConstraintList, data: D): R? {
        return visitCjElement(list, data)
    }

    /**
     * 提供 `visitTypeConstraint` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTypeConstraint(constraint: CjTypeConstraint, data: D): R? {
        return visitCjElement(constraint, data)
    }

    /**
     * 提供 `visitParameterList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitParameterList(parameterList: CjParameterList, data: D): R? {
        return visitCjElement(parameterList, data)
    }

    /**
     * 提供 `visitParameter` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitParameter(parameter: CjParameter, data: D): R? {
        return visitCjElement(parameter, data)
    }

    /**
     * 访问声明节点
     *
     * 这是所有声明节点的基础访问方法，包括类型声明、函数声明、
     * 变量声明等。
     *
     * @param dcl 要访问的声明
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    open fun visitDeclaration(dcl: CjDeclaration, data: D): R? {
        return visitExpression(dcl, data)
    }

    /**
     * 提供 `visitTypeAlias` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTypeAlias(typeAlias: CjTypeAlias, data: D): R? {
        return visitNamedDeclaration(typeAlias, data)
    }

    /**
     * 访问命名声明节点
     *
     * 命名声明是指所有具有名称标识符的声明，如类、函数、变量、
     * 类型参数等。
     *
     * @param declaration 要访问的命名声明
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    open fun visitNamedDeclaration(declaration: CjNamedDeclaration, data: D): R? {
        return visitDeclaration(declaration, data)
    }

    /**
     * 提供 `visitPrimaryConstructor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: D): R? {
        return visitNamedDeclaration(constructor, data)
    }

    /**
     * 提供 `visitMacroDeclaration` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitMacroDeclaration(macroDeclaration: CjMacroDeclaration, data: D): R? {
        return visitNamedDeclaration(macroDeclaration, data)
    }

    /**
     * 提供 `visitNamedFunction` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitNamedFunction(function: CjNamedFunction, data: D): R? {
        return visitNamedDeclaration(function, data)
    }

    /**
     * 提供 `visitCallExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitCallExpression(expression: CjCallExpression, data: D): R? {
        return visitReferenceExpression(expression, data)
    }

    /**
     * 提供 `visitFinalizer` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitFinalizer(constructor: CjFinalizer, data: D): R? {
        return visitNamedDeclaration(constructor, data)
    }

    /**
     * 提供 `visitSecondaryConstructor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: D): R? {
        return visitNamedDeclaration(constructor, data)
    }

    /**
     * 提供 `visitPackageDirective` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPackageDirective(packageDirective: CjPackageDirective, data: D): R? {
        return visitCjElement(packageDirective, data)
    }

    /**
     * 提供 `visitQualifiedExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitQualifiedExpression(expression: CjQualifiedExpression, data: D): R? {
        return visitExpression(expression, data)
    }

    /**
     * 提供 `visitDotQualifiedExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression, data: D): R? {
        return visitQualifiedExpression(expression, data)
    }



    /**
     * 提供 `visitImportItem` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitImportItem(importItem: CjImportItem, data: D): R? {
        return visitCjElement(importItem, data)
    }

    /**
     * 提供 `visitImportDirective` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitImportDirective(importDirective: CjImportDirective, data: D): R? {
        return visitCjElement(importDirective, data)
    }

    /**
     * 提供 `visitUserType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitUserType(type: CjUserType, data: D): R? {
        return visitCjElement(type, data)
    }

    /**
     * 提供 `visitTypeArgumentList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTypeArgumentList(typeArgumentList: CjTypeArgumentList, data: D): R? {
        return visitCjElement(typeArgumentList, data)
    }

    /**
     * 提供 `visitImportAlias` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitImportAlias(importAlias: CjImportAlias, data: D): R? {
        return visitCjElement(importAlias, data)
    }

    /**
     * 提供 `visitImportList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitImportList(importList: CjImportList, data: D): R? {
        return visitCjElement(importList, data)
    }

    /**
     * 提供 `visitSuperTypeList` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSuperTypeList(list: CjSuperTypeList, data: D): R? {
        return visitCjElement(list, data)
    }

    /**
     * 提供 `visitConstructorCalleeExpression` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitConstructorCalleeExpression(
        constructorCalleeExpression: CjConstructorCalleeExpression,
        data: D
    ): R? {
        return visitCjElement(constructorCalleeExpression, data)
    }

    /**
     * 提供 `visitBasicType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitBasicType(basicType: CjBasicType, data: D): R? {
        return visitCjElement(basicType, data)
    }

    /**
     * 提供 `visitKeyword` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitKeyword(keyword: CjKeyword, data: D): R? {
        return visitCjElement(keyword, data)
    }

    /**
     * 提供 `visitSimpleNameStringTemplateEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitSimpleNameStringTemplateEntry(entry: CjSimpleNameStringTemplateEntry, data: D): R? {
        return visitStringTemplateEntryWithExpression(entry, data)
    }

    /**
     * 提供 `visitEnumConstructor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitEnumConstructor(enumConstructor: CjEnumConstructor, data: D): R? {
        return visitCjElement(enumConstructor, data)
    }

    /**
     * 提供 `visitMatchEntry` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitMatchEntry(matchEntry: CjMatchEntry, data: D): R? {
        return visitCjElement(matchEntry, data)
    }

    /**
     * 提供 `visitTupleType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitTupleType(tupleType: CjTupleType, data: D): R? {
        return visitCjElement(tupleType, data)
    }

    /**
     * 提供 `visitParenthesizedType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitParenthesizedType(parenthesizedType: CjParenthesizedType, data: D): R? {
        return visitCjElement(parenthesizedType, data)
    }

    /**
     * 提供 `visitFunctionType` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitFunctionType(type: CjFunctionType, data: D): R? {
        return visitTypeElement(type, data)
    }

    /**
     * 访问类型元素节点
     *
     * 类型元素表示类型表达式，如函数类型、可选类型、可变数组类型等。
     *
     * @param type 要访问的类型元素
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    protected open fun visitTypeElement(type: CjTypeElement, data: D): R? {
        return visitCjElement(type, data)
    }

    /**
     * 提供 `visitPropertyAccessor` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitPropertyAccessor(accessor: CjPropertyAccessor, data: D): R? {
        return visitDeclaration(accessor, data)
    }

    /**
     * 提供 `visitExtend` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitExtend(extend: CjExtend, data: D): R? {
        return visitTypeStatement(extend, data)
    }

    /**
     * 提供 `visitClass` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitClass(cclass: CjClass, data: D): R? {
        return visitTypeStatement(cclass, data)
    }

    /**
     * 提供 `visitEnum` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitEnum(cenum: CjEnum, data: D): R? {
        return visitTypeStatement(cenum, data)
    }

    /**
     * 提供 `visitStruct` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitStruct(cstruct: CjStruct, data: D): R? {
        return visitTypeStatement(cstruct, data)
    }

    /**
     * 提供 `visitInterface` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitInterface(cinterface: CjInterface, data: D): R? {
        return visitTypeStatement(cinterface, data)
    }

    /**
     * 访问类型声明节点
     *
     * 类型声明包括类（class）、接口（interface）、结构体（struct）、
     * 枚举（enum）、扩展（extend）等顶层类型定义。
     *
     * @param typeStatement 要访问的类型声明
     * @param data 传递给访问器的上下文数据
     * @return 访问结果
     */
    open fun visitTypeStatement(typeStatement: CjTypeStatement, data: D): R? {
        return visitNamedDeclaration(typeStatement, data)
    }

    /**
     * 提供 `visitAnnotation` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitAnnotation(annotation: CjAnnotations, data: D): R? {
        return visitCjElement(annotation, data)
    }

    /**
     * 提供 `visitMainFunction` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    open fun visitMainFunction(mainFunction: CjMainFunction, data: D): R? {
        return visitDeclaration(mainFunction, data)
    }
} 
