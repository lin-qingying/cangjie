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

package org.cangnova.cangjie.psi.psiUtil

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.findTopmostParentInFile
import com.intellij.util.codeInsight.CommentUtilCore
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import java.util.*

// fun CjDeclaration.isExpectDeclaration(): Boolean =
//    when {
//        hasExpectModifier() -> true
//        this is CjParameter -> ownerFunction?.isExpectDeclaration() == true
//        else -> containingTypeStatement?.isExpectDeclaration() == true
//    }
/**
 * 提供 `effectiveDeclarations` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjTypeStatement.effectiveDeclarations(): List<CjDeclaration> {
    return when (this) {
        is CjStruct -> declarations + primaryConstructorParameters.filter { p -> p.hasLetOrVar() }

        is CjClass -> declarations + primaryConstructorParameters.filter { p -> p.hasLetOrVar() }
        else -> declarations
    }
}

/**
 * 提供 `getCallNameExpression` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjCallElement.getCallNameExpression(): CjSimpleNameExpression? {
    val calleeExpression = calleeExpression ?: return null

    return when (calleeExpression) {
        is CjSimpleNameExpression -> calleeExpression
        is CjConstructorCalleeExpression -> calleeExpression.constructorReferenceExpression
        else -> null
    }
}

/**
 * 提供 `getImportedSimpleNameByImportAlias` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun getImportedSimpleNameByImportAlias(file: CjFile, aliasName: String): String? {
    val importInfo = file.findImportByAlias(aliasName) ?: return null

    // 获取导入的引用表达式
    val reference = when (importInfo) {
        is CjImportItem -> importInfo.importedReference

        else -> null
    }

    var currentRef = reference
    while (currentRef is CjDotQualifiedExpression) {
        currentRef = currentRef.selectorExpression
    }
    if (currentRef is CjSimpleNameExpression) {
        return currentRef.referencedName
    }

    return null
}

/**
 * 提供 `lastBlockStatementOrThis` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjExpression.lastBlockStatementOrThis(): CjExpression =
    (this as? CjBlockExpression)?.statements?.lastOrNull() ?: this

/**
 * 提供 `findLabelAndCall` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjFunctionLiteral.findLabelAndCall(): Pair<Name?, CjCallExpression?> {
    val literalParent = (this.parent as CjLambdaExpression).parent

    fun CjValueArgument.callExpression(): CjCallExpression? {
        val parent = parent
        return (if (parent is CjValueArgumentList) parent else this).parent as? CjCallExpression
    }

    when (literalParent) {
//        is CjLabeledExpression -> {
//            val callExpression = (literalParent.parent as? CjValueArgument)?.callExpression()
//            return Pair(literalParent.getLabelNameAsName(), callExpression)
//        }

        is CjValueArgument -> {
            val callExpression = literalParent.callExpression()
            val label = (callExpression?.calleeExpression as? CjSimpleNameExpression)?.referencedNameAsName
            return Pair(label, callExpression)
        }

        else -> {
            return Pair(null, null)
        }
    }
}

// Annotations on labeled expression lies on it's base expression
/**
 * 提供 `getAnnotationEntries` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjExpression.getAnnotationEntries(): List<CjAnnotation> {
    return when (val parent = parent) {
//        is CjAnnotatedExpression -> parent.annotationEntries
//        is CjLabeledExpression -> parent.getAnnotationEntries()
        else -> emptyList()
    }
}

/**
 * 提供 `getOutermostParenthesizerOrThis` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjExpression.getOutermostParenthesizerOrThis(): CjExpression {
    return (parentsWithSelf.zip(parents)).firstOrNull {
        val (element, parent) = it
        when (parent) {
            is CjParenthesizedExpression -> false

            else -> true
        }
    }?.first as CjExpression? ?: this
}

/**
 * 提供 `isAbstract` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjTypeStatement.isAbstract(): Boolean =
    this is CjInterface || this is CjClass && hasModifier(CjTokens.ABSTRACT_KEYWORD)

/**
 * 提供 `isPropertyParameter` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjParameter.isPropertyParameter() = ownerFunction is CjPrimaryConstructor && hasLetOrVar()
/**
 * 提供 `isPackageDirectiveExpression` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjSimpleNameExpression.isPackageDirectiveExpression(): Boolean {
    val parent = parent
    return parent is CjPackageDirective || parent.parent is CjPackageDirective
}

/**
 * 提供 `isImportDirectiveExpression` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjSimpleNameExpression.isImportDirectiveExpression(): Boolean {
    val parent = parent
    return parent is CjImportItem ||
            parent!!.parent is CjImportItem
}

/**
 * 提供 `getQualifiedElementOrCallableRef` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjSimpleNameExpression.getQualifiedElementOrCallableRef(): CjElement {
    return getQualifiedElement()
}

/**
 * 提供 `getQualifiedExpressionForReceiver` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjExpression.getQualifiedExpressionForReceiver(): CjQualifiedExpression? {
    val parent = parent
    return if (parent is CjQualifiedExpression && parent.receiverExpression == this) parent else null
}

/**
 * 保存 `BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN` 的内部状态，供PSI 工具实现维护节点缓存或解析上下文。
 */
private val BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN = Regex("([a-zA-Z0-9_]|[^\\p{ASCII}]).*")

/**
 * Returns enclosing qualifying element for given [[CjSimpleNameExpression]]
 * ([[CjQualifiedExpression]] or [[CjUserType]] or original expression)
 */
fun CjSimpleNameExpression.getQualifiedElement(): CjElement {
    val baseExpression = (parent as? CjCallExpression) ?: this
    return when (val parent = baseExpression.parent) {
        is CjQualifiedExpression -> if (parent.selectorExpression == baseExpression) parent else baseExpression
        is CjUserType -> if (parent.referenceExpression == baseExpression) parent else baseExpression
        else -> baseExpression
    }
}

/**
 * 提供 `isPrivate` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjModifierListOwner.isPrivate(): Boolean = hasModifier(CjTokens.PRIVATE_KEYWORD)

/**
 * 提供 `canPlaceAfterSimpleNameEntry` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun canPlaceAfterSimpleNameEntry(element: PsiElement?): Boolean {
    val entryText = element?.text ?: return true
    return !BAD_NEIGHBOUR_FOR_SIMPLE_TEMPLATE_ENTRY_PATTERN.matches(entryText)
}

/**
 * 提供 `getReceiverExpression` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjSimpleNameExpression.getReceiverExpression(): CjExpression? {
    val parent = parent
    when {
        parent is CjQualifiedExpression -> {
            val receiverExpression = parent.receiverExpression
            // Name expression can't be receiver for itself
            if (receiverExpression != this) {
                return receiverExpression
            }
        }

        parent is CjCallExpression -> {
            // This is in case `a().b()`
            val grandParent = parent.parent
            if (grandParent is CjQualifiedExpression) {
                val parentsReceiver = grandParent.receiverExpression
                if (parentsReceiver != parent) {
                    return parentsReceiver
                }
            }
        }

        parent is CjUnaryExpression && parent.operationReference == this -> {
            return parent.baseExpression
        }

        parent is CjUserType -> {
            val qualifier = parent.qualifier
            if (qualifier != null) {
                return qualifier.referenceExpression!!
            }
        }
    }

    return null
}

/**
 * 提供 `isFunctionalExpression` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement.isFunctionalExpression(): Boolean = this is CjNamedFunction && nameIdentifier == null
/**
 * 提供 `containingClass` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjElement.containingClass(): CjClass? = getStrictParentOfType()

/**
 * 保存 `CjAbstractClassBody.containingClass`，供PSI 工具流程读取节点结构或语义信息。
 */
val CjAbstractClassBody.containingClass: CjTypeStatement?
    get() = when (val parent = parent) {
        is CjTypeStatement -> parent
        else -> null
    }

/**
 * 提供 `getTopmostParentQualifiedExpressionForSelector` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjSimpleNameExpression.getTopmostParentQualifiedExpressionForSelector(): CjQualifiedExpression? {
    return generateSequence<CjExpression>(this) {
        val parentQualified = it.parent as? CjQualifiedExpression
        if (parentQualified?.selectorExpression == it) parentQualified else null
    }.last() as? CjQualifiedExpression
}

/**
 * 提供 `visibilityModifier` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjModifierListOwner.visibilityModifier() = modifierList?.modifierFromTokenSet(CjTokens.VISIBILITY_MODIFIERS)
/**
 * 执行 `modifierFromTokenSet` 内部辅助逻辑，支撑PSI 工具节点的结构解析与访问。
 */
private fun CjModifierList.modifierFromTokenSet(set: TokenSet): PsiElement? {
    return set.types
        .asSequence()
        .map { getModifier(it as CjModifierKeywordToken) }
        .firstOrNull { it != null }
}

/**
 * 提供 `visibilityModifierType` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjModifierListOwner.visibilityModifierType(): CjModifierKeywordToken? =
    visibilityModifier()?.node?.elementType as CjModifierKeywordToken?

/**
 * 提供 `referenceExpression` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjExpression.referenceExpression(): CjReferenceExpression? =
    (if (this is CjCallExpression) calleeExpression else this) as? CjReferenceExpression

/**
 * 提供 `isContractPresentPsiCheck` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjNamedFunction.isContractPresentPsiCheck(isAllowedOnMembers: Boolean): Boolean {
    val contractAllowedHere =
        (isAllowedOnMembers || isTopLevel) &&
                hasBlockBody() &&
                !hasModifier(CjTokens.OPERATOR_KEYWORD)
    if (!contractAllowedHere) return false

    val firstExpression = (this as? CjFunction)?.bodyBlockExpression?.statements?.firstOrNull() ?: return false

    return firstExpression.isContractDescriptionCallPsiCheck()
}

/**
 * 提供 `isContractDescriptionCallPsiCheck` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjExpression.isContractDescriptionCallPsiCheck(): Boolean =
    (this is CjCallExpression && calleeExpression?.text == "contract") || (this is CjQualifiedExpression && isContractDescriptionCallPsiCheck())

/**
 * 提供 `visitChildren` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun <D> visitChildren(element: CjElement, visitor: CjVisitor<Void, D>, data: D) {
    var child = element.firstChild
    while (child != null) {
        if (child is CjElement) {
            child.accept(visitor, data)
        }
        child = child.nextSibling
    }
}

/**
 * 执行 `StubElement` 内部辅助逻辑，支撑PSI 工具节点的结构解析与访问。
 */
private fun StubElement<*>.collectAnnotationEntriesFromStubElement(): List<CjAnnotation> {
    return childrenStubs.flatMap { child ->
        when (child.stubType) {
            CjNodeTypes.ANNOTATION -> listOf(child.psi as CjAnnotation)

            else -> emptyList()
        }
    }
}

/**
 * 执行 `collectAnnotationEntriesFromPsi` 内部辅助逻辑，支撑PSI 工具节点的结构解析与访问。
 */
private fun CjAnnotationsContainer.collectAnnotationEntriesFromPsi(): List<CjAnnotation> {
    return children.flatMap { child ->
        when (child) {
            is CjAnnotation -> listOf(child)

            else -> emptyList()
        }
    }
}

/**
 * 提供 `collectAnnotationEntriesFromStubOrPsi` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjAnnotationsContainer.collectAnnotationEntriesFromStubOrPsi(): List<CjAnnotation> {
    return when (this) {
        is StubBasedPsiElementBase<*> -> stub?.collectAnnotationEntriesFromStubElement()
            ?: collectAnnotationEntriesFromPsi()

        else -> collectAnnotationEntriesFromPsi()
    }
}

/**
 * 提供 `getSuperNames` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun StubBasedPsiElementBase<out CangJieTypeStatementStub<out CjTypeStatement>>.getSuperNames(): List<String> {
    fun addSuperName(result: MutableList<String>, referencedName: String) {
        result.add(referencedName)

        val file = containingFile
        if (file is CjFile) {
            val directive = file.findImportByAlias(referencedName)
            if (directive != null) {
                val reference = when (directive) {
                    is CjImportItem -> directive.importedReference
                    else -> null
                }
                var currentRef = reference
                while (currentRef is CjDotQualifiedExpression) {
                    currentRef = currentRef.selectorExpression
                }
                if (currentRef is CjSimpleNameExpression) {
                    result.add(currentRef.referencedName)
                }
            }
        }
    }

    require(this is CjTypeStatement) { "it should be ${CjTypeStatement::class} but it is a ${this::class.java.name}" }

    val stub = stub
    if (stub != null) {
        return stub.getSuperNames()
    }

    val specifiers = this.superTypeListEntries
    if (specifiers.isEmpty()) return Collections.emptyList()

    val result = ArrayList<String>()
    for (specifier in specifiers) {
        val superType = specifier.typeAsUserType
        if (superType != null) {
            val referencedName = superType.referencedName
            if (referencedName != null) {
                addSuperName(result, referencedName)
            }
        }
    }

    return result
}

/**
 * 提供 `isComment` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun isComment(element: PsiElement): Boolean {
    return CommentUtilCore.isComment(element)
}

/**
 * 提供 `safeNameForLazyResolve` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjEnumConstructor.safeNameForLazyResolve(): Name {
    return name?.let { Name.identifier(it) }?.safeNameForLazyResolve() ?: SpecialNames.NO_NAME_PROVIDED
}

/**
 * 提供 `safeFqNameForLazyResolveByParent` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjEnumConstructor.safeFqNameForLazyResolveByParent(): FqName? {
    // 应该只为包级声明创建特殊名称，这样就可以安全地依赖于父级的真实fq名称
    val parentFqName = (this.parent?.parent as CjEnum).safeFqNameForLazyResolve()
    return parentFqName?.child(safeNameForLazyResolve())
}

/**
 * 提供 `safeFqNameForLazyResolve` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjEnumConstructor.safeFqNameForLazyResolve(): FqName? {
    // 应该只为包级声明创建特殊名称，这样就可以安全地依赖于父级的真实fq名称
    val parentFqName = CjNamedDeclarationUtil.getParentFqName(this.parent?.parent as CjEnum)
    return parentFqName?.child(safeNameForLazyResolve())
}

/**
 * 提供 `safeFqNameForLazyResolve` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjNamedDeclaration.safeFqNameForLazyResolve(name: Name): FqName? {
    // 应该只为包级声明创建特殊名称，这样就可以安全地依赖于父级的真实fq名称
    val parentFqName = CjNamedDeclarationUtil.getParentFqName(this)
    return parentFqName?.child(name)
}

/**
 * 提供 `safeFqNameForLazyResolve` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjNamedDeclaration.safeFqNameForLazyResolve(name: String?): FqName? {
    // 应该只为包级声明创建特殊名称，这样就可以安全地依赖于父级的真实fq名称
    val parentFqName = CjNamedDeclarationUtil.getParentFqName(this)
    return parentFqName?.child(Name.identifier(name ?: ""))
}

/**
 * 提供 `safeFqNameForLazyResolve` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjNamedDeclaration.safeFqNameForLazyResolve(): FqName? {
    // 应该只为包级声明创建特殊名称，这样就可以安全地依赖于父级的真实fq名称
    val parentFqName = CjNamedDeclarationUtil.getParentFqName(this)
    return parentFqName?.child(safeNameForLazyResolve())
}

/**
 * 提供 `safeNameForLazyResolve` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjNamedDeclaration.safeNameForLazyResolve(): Name {
    return nameAsName.safeNameForLazyResolve()
}
// fun CjFunctionImpl.isContractPresentPsiCheck(isAllowedOnMembers: Boolean): Boolean {
//    val contractAllowedHere =
//        (isAllowedOnMembers || isTopLevel) &&
//                hasBlockBody() &&
//                !hasModifier(CjTokens.OPERATOR_KEYWORD)
//    if (!contractAllowedHere) return false
//
//    val firstExpression = (this as? CjFunction)?.bodyBlockExpression?.statements?.firstOrNull() ?: return false
//
//    return firstExpression.isContractDescriptionCallPsiCheck()
// }
// fun CjExpression.isContractDescriptionCallPsiCheck(): Boolean =
//    (this is CjCallExpression && calleeExpression?.text == "contract") || (this is CjQualifiedExpression && isContractDescriptionCallPsiCheck())

/**
 * 提供 `safeNameForLazyResolve` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun Name?.safeNameForLazyResolve(): Name = this?.takeUnless(Name::isSpecial) ?: SpecialNames.NO_NAME_PROVIDED

/**
 * 提供 `getAssignmentByLHS` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjExpression.getAssignmentByLHS(): CjBinaryExpression? {
    val parent = parent as? CjBinaryExpression ?: return null
    return if (CjPsiUtil.isAssignment(parent) && parent.left == this) parent else null
}

/**
 * 提供 `getQualifiedExpressionForSelectorOrThis` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjExpression.getQualifiedExpressionForSelectorOrThis(): CjExpression {
    return getQualifiedExpressionForSelector() ?: this
}

/**
 * 提供 `copied` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
inline fun <reified T : PsiElement> T.copied(): T {
    return copy() as T
}

/**
 * 提供 `getTrailingCommaByClosingElement` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun getTrailingCommaByClosingElement(closingElement: PsiElement?): PsiElement? {
    val elementBeforeClosingElement =
        closingElement?.getPrevSiblingIgnoringWhitespaceAndComments() ?: return null

    return elementBeforeClosingElement.run { if (node.elementType == CjTokens.COMMA) this else null }
}

/**
 * 保存 `CjQualifiedExpression.callExpression`，供PSI 工具流程读取节点结构或语义信息。
 */
val CjQualifiedExpression.callExpression: CjCallExpression?
    get() = selectorExpression as? CjCallExpression

/**
 * 保存 `CjElement.parentSubstitute`，供PSI 工具流程读取节点结构或语义信息。
 */
var CjElement.parentSubstitute: PsiElement? by UserDataProperty(Key.create<PsiElement>("PARENT_SUBSTITUTE"))
/**
 * 提供 `quoteIfNeeded` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun String.quoteIfNeeded(): String = if (this.isIdentifier()) this else "`$this`"
/**
 * 提供 `isIdentifier` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun String?.isIdentifier(): Boolean {
    if (this == null || isEmpty()) return false

    val lexer = CangJieLexer()
    lexer.start(this, 0, length)
    if (lexer.tokenType !== CjTokens.IDENTIFIER) return false
    lexer.advance()
    return lexer.tokenType == null
}

/**
 * 提供 `astReplace` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun PsiElement.astReplace(newElement: PsiElement) = parent.node.replaceChild(node, newElement.node)
/**
 * 提供 `getQualifiedElementSelector` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjElement.getQualifiedElementSelector(): CjElement? {
    return when (this) {
        is CjSimpleNameExpression -> this
        is CjCallExpression -> calleeExpression
        is CjQualifiedExpression -> {
            val selector = selectorExpression
            (selector as? CjCallExpression)?.calleeExpression ?: selector
        }

        is CjUserType -> referenceExpression
        else -> null
    }
}

/**
 * 保存 `CjTypeElement.qualifier`，供PSI 工具流程读取节点结构或语义信息。
 */
val CjTypeElement.qualifier: CjTypeElement?
    get() {
        return when (this) {
            is CjUserType -> this.qualifier

            else -> null
        }
    }

/**
 * 提供 `getQualifiedExpressionForSelector` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjElement.getQualifiedExpressionForSelector(): CjQualifiedExpression? {
    val parent = parent
    return if (parent is CjQualifiedExpression && parent.selectorExpression == this) parent else null
}

/**
 * 保存 `CjDeclaration.containingTypeStatement`，供PSI 工具流程读取节点结构或语义信息。
 */
val CjDeclaration.containingTypeStatement: CjTypeStatement?
    get() = parent.let {
        when (it) {
            is CjAbstractClassBody -> it.parent as? CjTypeStatement
            is CjTypeStatement -> it
            is CjParameterList -> (it.parent as? CjPrimaryConstructor)?.getContainingTypeStatement()
            else -> null
        }
    }

/**
 * 提供 `getTrailingCommaByElementsList` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun getTrailingCommaByElementsList(elementList: PsiElement?): PsiElement? {
    val lastChild =
        elementList?.lastChild?.let { if (it !is PsiComment) it else it.getPrevSiblingIgnoringWhitespaceAndComments() }
    return lastChild?.takeIf { it.node.elementType == CjTokens.COMMA }
}

/**
 * 提供 `getContentRange` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjStringTemplateExpression.getContentRange(): TextRange {
    val start = node.firstChildNode.textLength
    val lastChild = node.lastChildNode
    val length = textLength
    return TextRange(
        start,
        if (lastChild.elementType == CjTokens.CLOSING_QUOTE) length - lastChild.textLength else length,
    )
}

/**
 * 提供 `isSingleQuoted` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjStringTemplateExpression.isSingleQuoted(): Boolean = node.firstChildNode.textLength == 1
/**
 * 提供 `isPlain` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjStringTemplateExpression.isPlain() = entries.all { it is CjLiteralStringTemplateEntry }


/**
 * 提供 `modalityModifier` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjDeclaration.modalityModifier() = modifierFromTokenSet(CjTokens.MODALITY_MODIFIERS)
/**
 * 执行 `modifierFromTokenSet` 内部辅助逻辑，支撑PSI 工具节点的结构解析与访问。
 */
private fun CjModifierListOwner.modifierFromTokenSet(set: TokenSet) = modifierList?.modifierFromTokenSet(set)

/**
 * 提供 `findElementOfAdditionalResolve` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun CjElement.findElementOfAdditionalResolve(): CjElement? {
    val elementOfAdditionalResolve = findTopmostParentInFile {
        it is CjFunction ||

//                    it is CjPrimaryConstructor ||
//                    it is CjSecondaryConstructor ||
                it is CjProperty ||
                it is CjVariable<*> ||
                it is CjSuperTypeList ||

                it is CjImportList ||
                it is CjAnnotation ||
                it is CjTypeParameter ||
                it is CjTypeConstraint ||
                it is CjPackageDirective ||
                it is CjCodeFragment ||
                it is CjTypeAlias ||
                it is CjMacroExpression

    } as CjElement?

    when (elementOfAdditionalResolve) {
        null -> {
            if (this is CjAnnotation) {
                return this
            }
            if (this is CjMacroExpression) {
                return this
            }

            return null
        }

        is CjPackageDirective -> return this
        is CjMacroExpression -> return elementOfAdditionalResolve
        is CjDeclaration -> {
            if (this is CjParameterBase && !CjPsiUtil.isLocal(this)) {
                return null
            }
            return elementOfAdditionalResolve
        }

        else -> return elementOfAdditionalResolve
    }
}

/**
 * 提供 `unwrapOptional` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
tailrec fun CjTypeElement.unwrapOptional(): CjTypeElement? {
    return when (this) {
        is CjOptionType -> this.getInnerType()?.unwrapOptional()
        else -> this
    }
}

/**
 * 提供 `quoteIfNeeded` 操作，封装PSI 工具节点的访问、构造或判断逻辑。
 */
fun FqName.quoteIfNeeded(): FqName {
    return FqName(pathSegments().joinToString(".") { it.asString().quoteIfNeeded() })
}
