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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieVariableStub
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 变量声明的抽象基类
 *
 * 所有变量类型的公共基类，包括：
 * - [CjPatternVariable]: 模式匹配变量（顶层或局部）
 * - [CjFieldVariable]: 类成员字段
 *
 * @see CjPatternVariable
 * @see CjFieldVariable
 */
abstract class CjVariable<S : StubElement<*>> : CjDeclarationStub<S>, CjVariableDeclaration {
    constructor(stub: S, type: IStubElementType<S, *>) : super(stub, type)
    constructor(node: ASTNode) : super(node)
    /**
     * 保存 `isLocal`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isLocal: Boolean
        get() = !isTopLevel && this !is CjFieldVariable
    /**
     * 保存 `isTopLevel`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    open val isTopLevel: Boolean get() = false
}

/**
 * 变量声明 PSI 元素
 *
 * 变量声明是模式匹配的声明方式，用于顶层变量和局部变量。
 * 变量本身没有名称，名称信息来自模式匹配中的绑定模式。
 * 一个变量声明可能包含多个绑定（如元组解构），因此不提供单一的 name 属性。
 *
 * 示例:
 * ```cangjie
 * let a = 1              // BINDING 模式 - 单个绑定
 * let (x, y) = tuple     // TUPLE 模式 - 多个绑定
 * let Some(v) = optional // ENUM 模式
 * let _ = ignored        // WILDCARD 模式 - 无绑定
 * ```
 *
 * 要获取绑定的变量名，请使用 [pattern] 属性遍历子模式。
 */
class CjPatternVariable : CjVariable<CangJieVariableStub> {
    constructor(stub: CangJieVariableStub) : super(stub, CjStubElementTypes.VARIABLE)
    constructor(node: ASTNode) : super(node)

    /**
     * 暴露 `valueParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameterList: CjParameterList?
        get() = null

    /**
     * 暴露 `valueParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val valueParameters: List<CjParameter>
        get() = emptyList()



    // Variables don't have type parameters
    /**
     * 暴露 `typeParameterList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameterList: CjTypeParameterList?
        get() = null

    /**
     * 暴露 `typeConstraintList`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeConstraintList: CjTypeConstraintList?
        get() = null

    /**
     * 暴露 `typeConstraints`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeConstraints: List<CjTypeConstraint>
        get() = emptyList()

    /**
     * 暴露 `typeParameters`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeParameters: List<CjTypeParameter>
        get() = emptyList()

    /**
     * 获取变量声明中的模式
     *
     * 模式可能是：
     * - [CjBindingPattern]: 简单绑定，如 `let a = 1`
     * - [CjTuplePattern]: 元组解构，如 `let (x, y) = tuple`
     * - [CjEnumPattern]: 枚举解构，如 `let Some(v) = optional`
     * - [CjWildcardPattern]: 通配符，如 `let _ = ignored`
     *
     * 使用 [getAllBindings] 扩展函数获取所有绑定的变量名。
     */
    val pattern: CjCasePatternElement?
        get() = getStubOrPsiChildByTypes(
            CjStubElementTypes.BINDING_PATTERN,
            CjStubElementTypes.TUPLE_PATTERN,
            CjStubElementTypes.ENUM_PATTERN,
            CjStubElementTypes.TYPE_PATTERN,
            CjStubElementTypes.WILDCARD_PATTERN
        )

    /**
     * 获取模式类型
     */
    val patternKind: PatternKind
        get() {
            val stub = stub
            if (stub != null) {
                return stub.getPatternKind()
            }
            return when (pattern) {
                null -> PatternKind.BINDING
                is CjBindingPattern -> PatternKind.BINDING
                is CjTuplePattern -> PatternKind.TUPLE
                is CjEnumPattern -> PatternKind.ENUM
                is CjWildcardPattern -> PatternKind.WILDCARD
                else -> PatternKind.BINDING
            }
        }

    /**
     * 保存 `equalsToken`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val equalsToken: PsiElement? get() = findChildByType(CjTokens.EQ)

    /**
     * 实现 `toString` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return "${super.toString()} [${patternKind}]"
    }

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternVariable(this, data)

    }

    /**
     * 暴露 `isTopLevel`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isTopLevel: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isTopLevel()
            }

            return parent is CjFile
        }

    /**
     * 暴露 `typeReference`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val typeReference: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasReturnTypeRef()) {
                    return null
                } else {
                    val typeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                    if (typeReferences.isEmpty()) {
                        LOG.error(
                            """
                                Invalid stub structure built for variable:
                                $text
                            """.trimIndent(),
                        )
                        return null
                    }
                    return typeReferences[0]
                }
            }
            return getTypeReference(this)
        }



    /**
     * 实现 `setTypeReference` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setTypeReference(typeRef: CjTypeReference?): CjTypeReference? {
        return setTypeReference(this, pattern, typeRef)
    }

    /**
     * 暴露 `colon`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val colon: PsiElement?
        get() = findChildByType(CjTokens.COLON)

    /**
     * 暴露 `isVar`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isVar: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isVar()
            }

            return node.findChildByType(CjTokens.VAR_KEYWORD) != null
        }

    /**
     * 保存 `isConst`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val isConst: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isConst()
            }

            return node.findChildByType(CjTokens.CONST_KEYWORD) != null
        }

    /**
     * 暴露 `isStatic`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val isStatic: Boolean
        get() = hasModifier(CjTokens.STATIC_KEYWORD)

    /**
     * 暴露 `letOrVarKeyword`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val letOrVarKeyword: PsiElement
        get() {
            val element =
                checkNotNull(findChildByType(LET_VAR_TOKEN_SET)) { "Let or var should always exist for variable: " + this.text }
            return element
        }

    /**
     * 暴露 `initializer`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val initializer: CjExpression?
        get() {
            val stub = stub
            if (stub != null) {
                if (!stub.hasInitializer()) {
                    return null
                }

                if (containingCjFile.isCompiled) {
                    return null
                }
            }

            return PsiTreeUtil.getNextSiblingOfType(
                findChildByType(CjTokens.EQ),
                CjExpression::class.java,
            )
        }

    /**
     * 实现 `hasInitializer` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun hasInitializer(): Boolean {
        val stub = stub
        if (stub != null) {
            return stub.hasInitializer()
        }

        return initializer != null
    }

    /**
     * 模式变量的 use-scope 由声明位置决定，而不是由具体名字节点决定。
     *
     * `find usages` 搜索 `CjBindingPattern` / `CjTypePattern` 时会委托到这里，
     * 因此必须与普通声明共享同一套作用域策略。
     */
    override fun getUseScope(): SearchScope {
        return computeCangJieDeclarationUseScope(
            declaration = this,
            defaultScope = super.getUseScope(),
        )
    }
// CjVariable 是模式匹配声明，可能包含多个绑定。请从 CjBindingPattern 获取 fqName。
    /**
     * 暴露 `nameAsSafeName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsSafeName: Name
        get() = throw UnsupportedOperationException(
            "CjVariable 是模式匹配声明，可能包含多个绑定。请使用 pattern.getAllBindings() 获取所有变量名。"
        )

    /**
     * 暴露 `fqName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val fqName: FqName?
        get() = null

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? {
      return  null
    }

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setName(name: @NlsSafe String): PsiElement? {
       return null
    }

    /**
     * 暴露 `nameAsName`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val nameAsName: Name?
        get() = null
    companion object {
        private val LOG = Logger.getInstance(
            CjVariable::class.java,
        )

        private val LET_VAR_TOKEN_SET =
            TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.CONST_KEYWORD, CjTokens.VAR_KEYWORD)
    }
}
