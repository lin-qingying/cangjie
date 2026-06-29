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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.psi.psiUtil.findDocComment
import java.util.concurrent.atomic.AtomicLong

/**
 * 支持 Stub 索引的声明 PSI 基类。
 *
 * 这里统一封装三类基础能力：
 * 1. 优先通过 Stub 恢复父子关系，避免无必要的整树 PSI 回退。
 * 2. 维护修改戳，供缓存失效与增量分析使用。
 * 3. 提供文档注释与导航策略的统一入口。
 *
 * 仓颉当前只允许顶层 class-like 声明进入公开类标识体系，
 * 因此这里不再为类型声明保留额外的层级父节点回退分支。
 */
abstract class CjDeclarationStub<T : StubElement<*>> : CjModifierListOwnerStub<T>, CjDeclaration {
    /**
     * 保存 `modificationStamp` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private val modificationStamp = AtomicLong()

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    /**
     * 暴露 `expression`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val expression: CjExpression?
        get() = PsiTreeUtil.getStubChildOfType(this, CjExpression::class.java)

    /**
     * 实现 `subtreeChanged` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun subtreeChanged() {
        super.subtreeChanged()
        modificationStamp.getAndIncrement()
    }

    /**
     * 提供 `getModificationStamp` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
     */
    fun getModificationStamp(): Long = modificationStamp.get()

    /**
     * 暴露 `docComment`，实现仓颉 PSI节点对上层接口的属性契约。
     */
    override val docComment: CDoc?
        get() = findDocComment(this)

    /**
     * 实现 `getParent` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getParent(): PsiElement? {
        val stub = stub
        if (stub != null) {
            return stub.parentStub.psi
        }
        return super.getParent()
    }

    /**
     * 实现 `getOriginalElement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getOriginalElement(): PsiElement {
        val currentDeclaration = requireCurrentCjElement() as CjDeclaration
        val navigationPolicy = ApplicationManager.getApplication().getService(
            CangJieDeclarationNavigationPolicy::class.java,
        )
        val navigationTarget = navigationPolicy?.getOriginalElement(currentDeclaration) ?: currentDeclaration
        return (navigationTarget as? CjElement)?.requireCurrentCjElement() ?: navigationTarget
    }

    /**
     * 实现 `getNavigationElement` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNavigationElement(): PsiElement {
        val currentDeclaration = requireCurrentCjElement() as CjDeclaration
        val navigationPolicy = ApplicationManager.getApplication().getService(
            CangJieDeclarationNavigationPolicy::class.java,
        )
        val navigationTarget = navigationPolicy?.getNavigationElement(currentDeclaration) ?: currentDeclaration
        return (navigationTarget as? CjElement)?.requireCurrentCjElement() ?: navigationTarget
    }
}
