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

import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.psiUtil.deleteSemicolon
import org.cangnova.cangjie.psi.psiUtil.parentSubstitute
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement



/**
 * 基础CangJie语言PSI元素接口
 *
 * 所有CangJie语言的PSI元素都应该实现这个接口，它提供了访问者模式支持
 * 和基本的导航功能。
 */
interface CjElement : NavigatablePsiElement, CjPureElement {

    /**
     * 让所有子元素接受访问者的访问
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的数据
     */
    fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D)

    /**
     * 接受访问者的访问
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的数据
     * @return 访问者返回的结果
     */
    fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R?

    /**
     * 获取引用
     *
     * 不建议在CjElement上直接使用getReference()，因为选择是不可预测的。
     * 应该使用getReferences()获取所有引用，或者使用更具体的方法。
     *
     * @return 此元素的引用，如果有多个引用则返回第一个，如果没有则返回null
     */
    @Deprecated("Don't use getReference() on CjElement for the choice is unpredictable")
    override fun getReference(): PsiReference?
}

/**
 * 把缓存里持有的 compiled PSI 恢复到当前 live PSI。
 *
 * `.cjo` 的 decompiled view provider / document 可能在 IDE 生命周期里被重建，
 * 旧的 compiled PSI 即使 `isValid == true`，其 text range 也可能已经对应不上当前文档。
 * 导航、引用解析、Analysis API 等任何再次把 compiled PSI 暴露回平台的入口，
 * 都必须先走一次 smart pointer 恢复，禁止继续直接返回旧元素。
 */
fun <E : CjElement> E.restoreCurrentCjElement(preferredProject: Project? = null): E? {
    val containingFile = runCatching { containingCjFile }.getOrNull() ?: return null
    if (!containingFile.isCompiled) return this

    val currentFile = containingFile.findCurrentCompiledFileForRestore(preferredProject) ?: return null

    /*
     * compiled PSI 的真实失效模式不是“元素自己 invalid”，而是旧元素仍然挂在旧的
     * decompiled view provider 上；这时只要把旧 PSI 再喂给 SmartPointerManager，
     * 平台会先做 provider 一致性校验并直接抛出 `different providers`。
     *
     * 因而 compiled 元素的恢复不能再以旧 PSI 本体为入口，而必须先拿到当前 live file，
     * 再按 stub 树中的结构身份把 live PSI 找回来。
     */
    if (containingFile.viewProvider === currentFile.viewProvider) {
        @Suppress("UNCHECKED_CAST")
        return this as E
    }

    if (this === containingFile) {
        @Suppress("UNCHECKED_CAST")
        return currentFile as E
    }

    val restoredByStubPath = restoreCurrentCompiledStubPsi(currentFile)
    if (restoredByStubPath != null) {
        @Suppress("UNCHECKED_CAST")
        return restoredByStubPath as E
    }

    return null
}

/**
 * compiled `.cjo`` 可能跨测试 / 跨项目被 session cache 暂时持有。
 *
 * 这时 `containingFile.project` 指向的往往是已经失效的旧 Project，
 * 继续用它去 `PsiManager.findFile()` 会直接把恢复链断掉，最终让 `symbol.psi` 退化成 `null`。
 *
 * 因此 live file 的查找必须先尝试当前元素自带 project，
 * 再退回平台级 `ProjectLocator`，按 virtual file 找到当前仍然活着的 Project。
 */
private fun CjFile.findCurrentCompiledFileForRestore(preferredProject: Project?): CjFile? {
    val virtualFile = virtualFile ?: return null
    val candidateProjects = LinkedHashSet<Project>()

    preferredProject
        ?.takeUnless(Project::isDisposed)
        ?.let(candidateProjects::add)
    project.takeUnless(Project::isDisposed)?.let(candidateProjects::add)

    val projectLocator = ProjectLocator.getInstance()
    projectLocator.guessProjectForFile(virtualFile)
        ?.takeUnless(Project::isDisposed)
        ?.let(candidateProjects::add)
    ProjectLocator.getPreferredProject(virtualFile)
        ?.takeUnless(Project::isDisposed)
        ?.let(candidateProjects::add)

    for (candidateProject in candidateProjects) {
        val liveFile = PsiManager.getInstance(candidateProject).findFile(virtualFile) as? CjFile
        if (liveFile != null) {
            return liveFile
        }
    }

    return null
}

/**
 * 导航链路进入平台前，compiled PSI 必须强制恢复成当前 live PSI。
 *
 * 这里不允许再回退到旧元素，否则平台会继续拿过期 text range 去构造 range marker，
 * 最终把 offset/document 不一致的问题延后到更深层再炸出来。
 */
fun <E : CjElement> E.requireCurrentCjElement(): E {
    val containingFile = runCatching { containingCjFile }.getOrNull()
    if (containingFile == null || !containingFile.isCompiled) return this

    return checkNotNull(restoreCurrentCjElement()) {
        "Failed to restore live compiled PSI for navigation: ${this::class.qualifiedName} in ${containingFile.virtualFile.path}"
    }
}

private data class CjCompiledStubPathSegment(
    val childIndex: Int,
    val stubType: Any?,
)

/**
 * 跨 provider 恢复 compiled PSI 时，只认 stub 结构身份。
 *
 * 同一个 `.cjo` 在 provider 重建前后，stub 树的父子结构与子节点顺序必须稳定；
 * 只要沿旧元素的 stub 路径回到当前 live file，就能拿到当前 provider 上的 PSI。
 */
private fun CjElement.restoreCurrentCompiledStubPsi(currentFile: CjFile): CjElement? {
    val stubBasedElement = this as? StubBasedPsiElement<*> ?: return null
    val elementStub = (stubBasedElement as? StubBasedPsiElementBase<*>)?.greenStub ?: stubBasedElement.stub ?: return null
    val pathFromFile = elementStub.buildCompiledStubPathFromFile() ?: return null
    return currentFile.findCompiledPsiByStubPath(pathFromFile)
}

private fun StubElement<*>.buildCompiledStubPathFromFile(): List<CjCompiledStubPathSegment>? {
    val path = ArrayDeque<CjCompiledStubPathSegment>()
    var current: StubElement<*> = this

    while (true) {
        val parent = current.parentStub ?: return null
        val childIndex = parent.childrenStubs.indexOfFirst { child -> child === current }
        if (childIndex < 0) return null

        path.addFirst(
            CjCompiledStubPathSegment(
                childIndex = childIndex,
                stubType = current.stubType,
            ),
        )

        if (parent is PsiFileStub<*>) {
            return path.toList()
        }

        current = parent
    }
}

private fun CjFile.findCompiledPsiByStubPath(path: List<CjCompiledStubPathSegment>): CjElement? {
    var currentStub: StubElement<*> = stub ?: calcStubTree().root

    for (segment in path) {
        val nextStub = currentStub.childrenStubs.getOrNull(segment.childIndex) ?: return null
        if (nextStub.stubType != segment.stubType) return null
        currentStub = nextStub
    }

    return currentStub.psi as? CjElement
}

/**
 * CjElement接口的基本实现
 *
 * 这个类提供了CjElement接口的标准实现，包括访问者模式支持、
 * 引用处理和父元素解析等功能。大多数CangJie语言的具体PSI元素
 * 都应该继承这个类。
 *
 * @param node AST节点
 */
open class CjElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), CjElement {

    /**
     * 返回元素的字符串表示
     *
     * @return 元素类型的字符串表示
     */
    override fun toString(): String = node.elementType.toString()

    /**
     * 让所有子元素接受访问者的访问
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的数据
     */
    override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {
        CjPsiUtil.visitChildren(this, visitor, data)
    }

    /**
     * 接受PSI元素访问者的访问
     *
     * 如果访问者是CjVisitor，则调用专门的accept方法，
     * 否则调用通用的visitElement方法。
     *
     * @param visitor PSI元素访问者
     */
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            @Suppress("UNCHECKED_CAST")
            accept(visitor as CjVisitor<Any?, Any?>, null as Any?)
        } else {
            visitor.visitElement(this)
        }
    }

    /**
     * 接受CjVisitor的访问
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的数据
     * @return 访问者返回的结果
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitCjElement(this, data)

    /**
     * 获取PSI元素或其父元素
     *
     * @return 当前元素
     */
    override fun getPsiOrParent(): CjElement = this

    /**
     * 获取包含此元素的CjFile
     *
     * @return 包含此元素的CjFile
     * @throws IllegalStateException 如果元素不在CjFile内
     */
    override fun getContainingCjFile(): CjFile {
        val file = containingFile
        if (file !is CjFile) {
            val fileString = if (file != null && file.isValid) " " + file.text else ""
            throw IllegalStateException(
                "CjElement not inside CjFile: " + file + fileString +
                        " for element " + this + " of type " + this.javaClass + " node = " + node,
            )
        }
        return file
    }

    /**
     * 删除此元素
     *
     * 在删除元素之前，尝试删除与之关联的分号
     */
    override fun delete() {
        this.deleteSemicolon()
        super.delete()
    }

    /**
     * 获取此元素的引用
     *
     * 如果元素有多个引用，返回第一个；如果没有引用，返回null
     *
     * @return 此元素的引用
     */
    override fun getReference(): PsiReference? {
        val references = references
        return if (references.size == 1) references[0] else null
    }

    /**
     * 获取此元素的所有引用
     *
     * @return 此元素的所有引用数组
     */
    override fun getReferences(): Array<PsiReference> {
//        if(this is CjBasicType) return emptyArray()

        return CangJieReferenceProvidersService.getReferencesFromProviders(this)
    }

    override fun getOwnReferences(): Collection<PsiSymbolReference> {
        return cangJieOwnReferences()
    }

    override fun getOwnDeclarations(): Collection<PsiSymbolDeclaration> {
        return cangJieOwnDeclarations()
    }

    /**
     * 获取此元素的父元素
     *
     * 首先检查是否有父元素替代品，如果有则返回替代品，
     * 否则返回默认的父元素
     *
     * @return 父元素
     */
    override fun getParent(): PsiElement? {
        val substitute: PsiElement? = this.parentSubstitute
        return substitute ?: super.getParent()
    }

    /**
     * 获取此元素的语言
     *
     * @return CangJie语言实例
     */
    override fun getLanguage(): Language = CangJieLanguage
}

// fun CjElement.findInScope(name: String, ns: Set<Namespace>): PsiElement? {
//    return pickFirstResolveVariant(name) {
//        processNestedScopesUpwards(this, ns, it)
//    }
// }
