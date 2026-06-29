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
package org.cangnova.cangjie.psi

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.parsing.CangJieExpressionParsing
import org.cangnova.cangjie.lexer.cdoc.psi.CDocElement
import org.cangnova.cangjie.psi.psiUtil.getQualifiedElement
import org.cangnova.cangjie.psi.psiUtil.getQualifiedElementSelector
import org.cangnova.cangjie.psi.psiUtil.getQualifiedExpressionForSelector
import org.cangnova.cangjie.resolve.StatementFilter
import org.cangnova.cangjie.resolve.getLastStatementInABlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.codeInsight.CommentUtilCore

/**
 * 提供 `CjPsiUtil` 单例，集中承载仓颉 PSI的共享状态、工厂或工具行为。
 */
object CjPsiUtil {
    /**
     * 提供 `isAbstract` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isAbstract(declaration: CjDeclarationWithBody): Boolean {
        return declaration.bodyExpression == null
    }

    /**
     * 提供 `isDeprecated` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isDeprecated(owner: CjModifierListOwner): Boolean {

            val annotationEntries = owner.annotationEntries
            for (annotation in annotationEntries) {
                val shortName = annotation.shortName
                if (StandardNames.FqNames.deprecated.shortName() == shortName) {
                    return true
                }
            }

        return false
    }

    /**
     * 提供 `isStatement` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isStatement(element: PsiElement): Boolean {
        return isStatementContainer(element.parent)
    }

    /**
     * 提供 `isBooleanConstant` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isBooleanConstant(condition: CjExpression?): Boolean {
        return condition != null && condition.node.elementType === CjNodeTypes.BOOLEAN_CONSTANT
    }

    /**
     * CommentUtilCore.isComment fails if element **inside** comment.
     *
     * Also, we can not add CDocTokens to COMMENTS TokenSet, because it is used in KotlinParserDefinition.getCommentTokens(),
     * and therefor all COMMENTS tokens will be ignored by PsiBuilder.
     *
     * @param element
     * @return
     */
    fun isInComment(element: PsiElement?): Boolean {
        return CommentUtilCore.isComment(element) || element is CDocElement
    }

    /**
     * 提供 `getExpressionOrLastStatementInBlock` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getExpressionOrLastStatementInBlock(expression: CjExpression?): CjExpression? {
        if (expression is CjBlockExpression) {
            return getLastStatementInABlock(expression)
        }
        return expression
    }

    /**
     * 提供 `isLHSOfDot` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isLHSOfDot(expression: CjExpression): Boolean {
        val parent = expression.parent
        if (parent !is CjQualifiedExpression) return false
        return parent.receiverExpression === expression || isLHSOfDot(parent)
    }

    /**
     * 提供 `findRootExpressions` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun findRootExpressions(unreachableElements: Collection<CjElement>): MutableSet<CjElement> {
        val rootElements: MutableSet<CjElement> = HashSet()
        val shadowedElements: MutableSet<CjElement> = HashSet<CjElement>()
        val shadowAllChildren: CjVisitorUnit = object : CjVisitorUnit() {
            override fun visitCjElement(element: CjElement) {
                if (shadowedElements.add(element)) {
                    element.acceptChildren(this)
                }
            }
        }

        for (element in unreachableElements) {
            if (shadowedElements.contains(element)) continue
            element.acceptChildren(shadowAllChildren)

            rootElements.removeAll(shadowedElements)
            rootElements.add(element)
        }
        return rootElements
    }

    /**
     * 提供 `isTrueConstant` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isTrueConstant(condition: CjExpression?): Boolean {
        return isBooleanConstant(condition) && condition!!.node.findChildByType(CjTokens.TRUE_KEYWORD) != null
    }

    /**
     * 提供 `isSelectorInQualified` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isSelectorInQualified(nameExpression: CjSimpleNameExpression): Boolean {
        val qualifiedElement = nameExpression.getQualifiedElement()
        return qualifiedElement is CjQualifiedExpression
                || ((qualifiedElement is CjUserType) && qualifiedElement.qualifier != null)
    }

    /**
     * 提供 `areParenthesesUseless` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @Suppress("unused") // used in intellij repo
    fun areParenthesesUseless(expression: CjParenthesizedExpression): Boolean {
        val innerExpression = expression.expression
        if (innerExpression == null) return true
        val parent = expression.getParent()
        if (parent !is CjElement) return true
        return !areParenthesesNecessary(innerExpression, expression, parent)
    }

    /**
     * 提供 `getLastStatementInABlock` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getLastStatementInABlock(blockExpression: CjBlockExpression?): CjExpression? {
        if (blockExpression == null) return null
        val statements = blockExpression.statements
        return if (statements.isEmpty()) null else statements[statements.size - 1]
    }

    /**
     * 提供 `getLastElementDeparenthesized` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getLastElementDeparenthesized(
        expression: CjExpression?,
        statementFilter: StatementFilter
    ): CjExpression? {
        val deparenthesizedExpression: CjExpression? = deparenthesize(expression)
        if (deparenthesizedExpression is CjBlockExpression) {
            // todo
            // This case is a temporary hack for 'if' branches.
            // The right way to implement this logic is to interpret 'if' branches as function literals with explicitly-typed signatures
            // (no arguments and no receiver) and therefore analyze them straight away (not in the 'complete' phase).
            val lastStatementInABlock = statementFilter.getLastStatementInABlock(deparenthesizedExpression)
            if (lastStatementInABlock != null) {
                return getLastElementDeparenthesized(lastStatementInABlock, statementFilter)
            }
        }
        return deparenthesizedExpression
    }

    /**
     * 提供 `deparenthesizeOnce` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @JvmOverloads
    fun deparenthesizeOnce(
        expression: CjExpression?, keepAnnotations: Boolean = false
    ): CjExpression? {
//        if (expression instanceof CjAnnotatedExpression && !keepAnnotations) {
//            return ((CjAnnotatedExpression) expression).getBaseExpression();
//        }
//        else if (expression instanceof CjLabeledExpression) {
//            return ((CjLabeledExpression) expression).getBaseExpression();
//        }
//        else
        if (expression is CjExpressionWrapper) {
            return (expression as CjExpressionWrapper).baseExpression
        } else if (expression is CjParenthesizedExpression) {
            return expression.expression
        }
        return expression
    }

    /**
     * 提供 `deparenthesize` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @JvmOverloads
    fun deparenthesize(expression: CjExpression?, keepAnnotations: Boolean = false): CjExpression? {
        var expression = expression
        while (true) {
            val baseExpression = deparenthesizeOnce(expression, keepAnnotations)

            if (baseExpression === expression) return baseExpression
            expression = baseExpression
        }
    }

    /**
     * 提供 `safeDeparenthesize` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    @JvmOverloads
    fun safeDeparenthesize(expression: CjExpression, keepAnnotations: Boolean = false): CjExpression {
        val deparenthesized = deparenthesize(expression, keepAnnotations)
        return deparenthesized ?: expression
    }

    /**
     * 提供 `isStatementContainer` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isStatementContainer(container: PsiElement?): Boolean {
        return container is CjBlockExpression ||
                container is CjContainerNodeForControlStructureBody
    }

    /**
     * 提供 `isAssignment` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isAssignment(element: PsiElement): Boolean {
        return element is CjBinaryExpression &&
                CjTokens.ALL_ASSIGNMENTS.contains(element.operationToken)
    }

    /**
     * 提供 `isLocal` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun isLocal(declaration: CjDeclaration): Boolean {
        return getEnclosingElementForLocalDeclaration(declaration) != null
    }

    /**
     * 提供 `getEnclosingElementForLocalDeclaration` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getEnclosingElementForLocalDeclaration(declaration: CjDeclaration): CjElement? {
        return getEnclosingElementForLocalDeclaration(declaration, true)
    }

    /**
     * 提供 `unquoteIdentifierOrFieldReference` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun unquoteIdentifierOrFieldReference(quoted: String): String {
        if (quoted.indexOf('`') < 0) {
            return quoted
        }

        return if (quoted.startsWith("$")) {
            "$" + unquoteIdentifier(quoted.substring(1))
        } else {
            unquoteIdentifier(quoted)
        }
    }

    /**
     * 提供 `getClassIfParameterIsProperty` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getClassIfParameterIsProperty(cjParameter: CjParameter): CjTypeStatement? {
        if (cjParameter.hasLetOrVar()) {
            var grandParent: PsiElement? = null
            if (cjParameter.getParent() != null) {
                grandParent = cjParameter.getParent()!!.parent
            }
            if (grandParent is CjPrimaryConstructor) {
                return grandParent.getContainingTypeStatement()
            }
        }

        return null
    }

    /**
     * 提供 `safeName` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun safeName(name: String?): Name {
        return name?.asOperatorName() ?: SpecialNames.NO_NAME_PROVIDED
    }

    /**
     * 提供 `getLastReference` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getLastReference(importedReference: CjExpression): CjSimpleNameExpression? {
        val selector = importedReference.getQualifiedElementSelector()
        return selector as? CjSimpleNameExpression
    }

    /**
     * 执行 `isNonLocalCallable` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun isNonLocalCallable(declaration: CjDeclaration?): Boolean {
        if (declaration is CjPatternVariable) {
            return !declaration.isLocal
        }

        return false
    }

    /**
     * 提供 `visitChildren` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun <D> visitChildren(element: CjElement, visitor: CjVisitor<Unit, D>, data: D) {
        var child = element.firstChild
        while (child != null) {
            if (child is CjElement) {
                child.accept(visitor, data)
            }
            child = child.nextSibling
        }
    }

    /**
     * 执行 `getOperation` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun getOperation(expression: CjExpression): IElementType? {
        if (expression is CjQualifiedExpression) {
            return expression.operationSign
        } else if (expression is CjOperationExpression) {
            return expression.operationReference.referencedNameElementType
        }
        return null
    }

    /**
     * 执行 `getPriority` 内部辅助逻辑，支撑仓颉 PSI节点的结构解析与访问。
     */
    private fun getPriority(expression: CjExpression): Int {
        val maxPriority = CangJieExpressionParsing.Precedence.entries.size + 1


        if (expression is CjQualifiedExpression ||
            expression is CjCallExpression
        ) {
            return maxPriority - 1
        }





        if (expression is CjDeclaration || expression is CjStatementExpression) {
            return 0
        }

        val operation = getOperation(expression)
        for (precedence in CangJieExpressionParsing.Precedence.entries) {
            if (precedence !== CangJieExpressionParsing.Precedence.PREFIX && precedence !== CangJieExpressionParsing.Precedence.POSTFIX &&
                precedence.getOperations().contains(operation)
            ) {
                return maxPriority - precedence.ordinal - 1
            }
        }

        return maxPriority
    }

    /**
     * 提供 `areParenthesesNecessary` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun areParenthesesNecessary(
        innerExpression: CjExpression,
        currentInner: CjExpression,
        parentElement: CjElement
    ): Boolean {
        if (parentElement is CjPackageDirective) return false






        if (parentElement is CjCallExpression && currentInner === parentElement.calleeExpression) {
            var targetInnerExpression: CjExpression? = innerExpression
            if (targetInnerExpression is CjDotQualifiedExpression) {
                val selector = targetInnerExpression.selectorExpression
                if (selector != null) {
                    targetInnerExpression = selector
                }
            }
            if (targetInnerExpression is CjSimpleNameExpression) return false
            if (parentElement.getQualifiedExpressionForSelector() != null) return true

            return targetInnerExpression !is CjCallExpression
        }

        if (parentElement is CjValueArgument) {
            // a(___, d > (e + f)) => a((b < c), d > (e + f)) to prevent parsing < c, d > as type argument list
            val nextArg = PsiTreeUtil.getNextSiblingOfType<CjValueArgument?>(parentElement, CjValueArgument::class.java)
            val nextExpression: PsiElement? = nextArg?.getArgumentExpression()
        }

        val innerOperation = getOperation(innerExpression)



        if (parentElement !is CjExpression) return false

        val parentOperation = getOperation(parentElement)


        val innerPriority = getPriority(innerExpression)
        val parentPriority = getPriority(parentElement)

        if (innerPriority == parentPriority) {
            return false
        }

        return innerPriority < parentPriority
    }

    /**
     * 提供 `getEnclosingElementForLocalDeclaration` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getEnclosingElementForLocalDeclaration(declaration: CjDeclaration, skipParameters: Boolean): CjElement? {
        var declaration = declaration
        if (declaration is CjTypeParameter && skipParameters) {
            declaration = PsiTreeUtil.getParentOfType<CjNamedDeclaration?>(
                declaration,
                CjNamedDeclaration::class.java
            )!!
        } else if (declaration is CjParameterBase) {
            val functionType = PsiTreeUtil.getParentOfType<CjFunctionType?>(declaration, CjFunctionType::class.java)
            if (functionType != null) {
                return functionType
            }

            val parent = declaration.parent

            // let/var parameter of primary constructor should be considered as local according to containing class
            if (declaration.hasLetOrVar() && parent != null && parent.parent is CjPrimaryConstructor) {
                return getEnclosingElementForLocalDeclaration(
                    (parent.parent as CjPrimaryConstructor).getContainingTypeStatement(),
                    skipParameters
                )
            } else if (skipParameters && parent != null && (parent !is CjForExpression) && (parent !is CjTryResource) &&
                parent.parent is CjNamedFunction
            ) {
                declaration = parent.parent as CjNamedFunction
            }
        }
        if (declaration is PsiFile) {
            return declaration
        }


        var current = PsiTreeUtil.getStubOrPsiParent(declaration)
        val isNonLocalCallable = isNonLocalCallable(declaration)
        while (current != null) {
            val parent = PsiTreeUtil.getStubOrPsiParent(current)


            if (current is CjParameter) {
                return current as CjElement
            }
            if (current is CjValueArgument) {
                if (!isNonLocalCallable) {
                    return current as CjElement
                }
            }

            if (current is CjBlockExpression) {
                // For members also not applicable if has function literal parent
                if (!isNonLocalCallable || current.getParent() !is CjFunctionLiteral) {
                    return current as CjElement
                }
            }
            if (current is CjSuperTypeCallEntry) {
                val grandParent = current.parent.parent
                if (grandParent is CjTypeStatement) {
                    return grandParent as CjElement
                }
            }

            current = parent
        }
        return null
    }

    /**
     * 提供 `unquoteIdentifier` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun unquoteIdentifier(quoted: String): String {
        if (quoted.indexOf('`') < 0) {
            return quoted
        }

        return if (quoted.startsWith("`") && quoted.endsWith("`") && quoted.length >= 2) {
            quoted.substring(1, quoted.length - 1)
        } else {
            quoted
        }
    }

    /**
     * 定义 `CjExpressionWrapper` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
     */
    interface CjExpressionWrapper {
        /**
         * 保存 `baseExpression`，供仓颉 PSI流程读取节点结构或语义信息。
         */
        val baseExpression: CjExpression?
    }
}

