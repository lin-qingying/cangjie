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

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieBindingPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieConstantPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieEnumPatternStub
import org.cangnova.cangjie.psi.stubs.CangJieMatchConditionStub
import org.cangnova.cangjie.psi.stubs.CangJieTuplePatternStub
import org.cangnova.cangjie.psi.stubs.CangJieTypePatternStub
import org.cangnova.cangjie.psi.stubs.CangJieWildcardPatternStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 模式匹配 PSI 元素的基础接口
 */
interface CjCasePatternElement : CjElement, ValueArgument, CjExpression

/**
 * 具有名称的模式元素接口
 *
 * 用于标识可以创建变量绑定的模式元素，如 [CjBindingPattern] 和 [CjTypePattern]。
 * 这些模式元素会创建变量描述符，因此需要同时支持 PSI 元素操作和名称访问。
 */
interface CjNamedPattern : CjCasePatternElement, CjNamed

/**
 * 基于 Stub 的模式匹配基类（所有模式都支持 Stub）
 */
abstract class CjCasePattern<T : StubElement<*>> : CjElementImplStub<T>, CjCasePatternElement {

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitCasePattern(this, data)
    }

    /**
     * 实现 `getArgumentName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getArgumentName(): ValueArgumentName? = null
    /**
     * 实现 `isNamed` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isNamed(): Boolean = false
    /**
     * 实现 `asElement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun asElement(): CjElement = this
    /**
     * 实现 `getSpreadElement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getSpreadElement(): LeafPsiElement? = null
    /**
     * 实现 `isExternal` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isExternal(): Boolean = false
    /**
     * 实现 `getArgumentExpression` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getArgumentExpression(): CjExpression? = this
}

/**
 * match 表达式条件模式（支持 Stub）
 */
class CjMatchConditionWithExpression : CjCasePattern<CangJieMatchConditionStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieMatchConditionStub) : super(stub, CjStubElementTypes.MATCH_CONDITION)

    /**
     * 保存 `expression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    @get:IfNotParsed
    val expression
        get() = findChildByClass<CjExpression>(CjExpression::class.java)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitMatchConditionWithExpression(this, data)
    }
}

/**
 * 绑定模式 PSI 元素（支持 Stub）
 *
 * 表示变量绑定模式，如 `let a = 1` 中的 `a`
 * 绑定模式是模式匹配的一部分，不是独立的变量声明
 */
class CjBindingPattern : CjCasePattern<CangJieBindingPatternStub>, CjSimpleNameExpression, PsiNameIdentifierOwner, CjNamedPattern {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieBindingPatternStub) : super(stub, CjStubElementTypes.BINDING_PATTERN)

    /**
     * 暴露 `referencedName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedName: String
        get() = name ?: ""

    /**
     * 暴露 `referencedNameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameAsName: Name
        get() = Name.identifier(referencedName)

    /**
     * 保存 `isLocal`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isLocal: Boolean
        get() = !isTopLevel

    /**
     * 保存 `isTopLevel`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isTopLevel: Boolean
        get() = parent is CjFile

    /**
     * 暴露 `referencedNameElement`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameElement: PsiElement
        get() = expression ?: this

    /**
     * 暴露 `identifier`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val identifier: PsiElement?
        get() = findChildByType(CjTokens.IDENTIFIER)

    /**
     * 暴露 `referencedNameElementType`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val referencedNameElementType: IElementType
        get() = CjSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)

    /**
     * 保存 `expression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val expression: CjSimpleNameExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByBinding(this, data)
    }

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? {
        val stub = stub
        if (stub != null) {
            return stub.name
        }
        return expression?.referencedName
    }

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? {
        return expression?.identifier
    }

    /**
     * 保存 `nameAsSafeName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val nameAsSafeName: Name
        get() = Name.identifier(name ?: "")

    /**
     * 暴露 `nameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsName: Name?
        get() = nameAsSafeName

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setName(name: String): PsiElement = this

    /**
     * 获取所属的变量声明
     */
    private fun getParentVariable(): CjPatternVariable? {
        var parent = parent
        while (parent is CjCasePatternElement) {
            parent = parent.parent
        }
        return parent as? CjPatternVariable
    }

    /**
     * 保存 `variable`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val variable: CjPatternVariable?
        get() = getParentVariable()

    /**
     * 绑定模式只是模式变量声明的“名字视图”，真正的 use-scope 语义必须与变量声明一致。
     */
    override fun getUseScope(): SearchScope {
        return variable?.useScope ?: super.getUseScope()
    }
}

/**
 * 类型模式 PSI 元素（支持 Stub）
 *
 * 表示类型匹配模式，如 `case x: Int => ...` 中的 `x: Int`
 */
class CjTypePattern : CjCasePattern<CangJieTypePatternStub>, PsiNameIdentifierOwner, CjNamedPattern {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTypePatternStub) : super(stub, CjStubElementTypes.TYPE_PATTERN)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByType(this, data)
    }

    /**
     * 保存 `identifierElement`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val identifierElement: PsiElement?
        get() = findChildByType(CjTokens.IDENTIFIER)
    /**
     * 获取所属的变量声明
     */
    private fun getParentVariable(): CjPatternVariable? {
        var parent = parent
        while (parent is CjCasePatternElement) {
            parent = parent.parent
        }
        return parent as? CjPatternVariable
    }
    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? {
        val stub = stub
        if (stub != null) {
            return stub.name
        }
        return reference?.text
    }
    /**
     * 保存 `variable`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val variable: CjPatternVariable?
        get() = getParentVariable()
    /**
     * 保存 `reference`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val reference: CjSimpleNameExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)

    /**
     * 保存 `typeReference`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val typeReference: CjTypeReference?
        get() = findChildByType(CjNodeTypes.TYPE_REFERENCE)

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? = identifierElement

    /**
     * 保存 `nameAsSafeName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val nameAsSafeName: Name
        get() = Name.identifier(name ?: "")

    /**
     * 暴露 `nameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsName: Name?
        get() = nameAsSafeName

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setName(name: String): PsiElement = this

    /**
     * 类型模式中的名字同样属于模式变量声明的一部分，搜索边界委托给变量声明统一裁剪。
     */
    override fun getUseScope(): SearchScope {
        return variable?.useScope ?: super.getUseScope()
    }
}

/**
 * 元组模式 PSI 元素（支持 Stub）
 *
 * 表示元组解构模式，如 `let (a, b) = tuple` 中的 `(a, b)`
 */
class CjTuplePattern : CjCasePattern<CangJieTuplePatternStub>, CjEnumAndTuplePattern {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieTuplePatternStub) : super(stub, CjStubElementTypes.TUPLE_PATTERN)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByTuple(this, data)
    }

    /**
     * 暴露 `patterns`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val patterns: List<CjCasePatternElement>
        get() = findChildrenByClass(CjCasePatternElement::class.java).toList()
}

/**
 * 定义 `CjEnumAndTuplePattern` 接口，约束仓颉 PSI节点或服务需要暴露的结构能力。
 */
interface CjEnumAndTuplePattern {
    /**
     * 保存 `patterns`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val patterns: List<CjCasePatternElement>
}

/**
 * 枚举模式 PSI 元素（支持 Stub）
 *
 * 表示枚举解构模式，如 `let Some(x) = optional` 中的 `Some(x)`
 */
class CjEnumPattern : CjCasePattern<CangJieEnumPatternStub>, CjEnumAndTuplePattern {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieEnumPatternStub) : super(stub, CjStubElementTypes.ENUM_PATTERN)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByEnum(this, data)
    }

    /**
     * 保存 `type`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val type: CjTypeReference?
        get() = findChildByType(CjNodeTypes.TYPE_REFERENCE)

    /**
     * 保存 `expression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val expression: CjExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)
            ?: findChildByType(CjNodeTypes.DOT_QUALIFIED_EXPRESSION)

    /**
     * 暴露 `patterns`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val patterns: List<CjCasePatternElement>
        get() = findChildrenByClass(CjCasePatternElement::class.java).toList()
}

/**
 * 通配符模式 PSI 元素（支持 Stub）
 *
 * 表示通配符模式，如 `let _ = ignored` 中的 `_`
 */
class CjWildcardPattern : CjCasePattern<CangJieWildcardPatternStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieWildcardPatternStub) : super(stub, CjStubElementTypes.WILDCARD_PATTERN)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByWildcard(this, data)
    }
}

/**
 * 常量模式 PSI 元素（支持 Stub）
 *
 * 表示常量匹配模式，如 `case 1 => ...` 中的 `1`
 */
class CjConstantPattern : CjCasePattern<CangJieConstantPatternStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieConstantPatternStub) : super(stub, CjStubElementTypes.CONSTANT_PATTERN)

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByConstant(this, data)
    }

    /**
     * 保存 `expression`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val expression: CjExpression?
        get() = findChildByClass(CjExpression::class.java)
}
