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
package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl
import org.cangnova.cangjie.psi.stubs.CangJieCompiledFileErrors
import org.cangnova.cangjie.parsing.parsedRoleSnapshot
import org.cangnova.cangjie.parsing.collectParsedRoleSnapshot
import org.cangnova.cangjie.parsing.cjmpDeclarationRoleEventsIn
import org.cangnova.cangjie.parsing.CjmpParsedRoleEventId
import org.cangnova.cangjie.parsing.CjParsedDeclarationModifiers
import org.cangnova.cangjie.parsing.declarationModifiersFromActualRegion
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.stubs.cjmpPathIn

/**
 * 表示 `CjFileStubBuilder`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CjFileStubBuilder : DefaultStubBuilder() {
    /** InvalidDecl 没有语义签名；恢复子树不能成为持久化参数或成员。 */
    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, child: ASTNode): Boolean =
        parent.elementType == CjStubElementTypes.INVALID_DECLARATION ||
            super.skipChildProcessingWhenBuildingStubs(parent, child)

    /**
     * 实现 `buildStubTree` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun buildStubTree(file: PsiFile): StubElement<*> {
        (file as? CjFile)?.customStubBuilder?.let {
            return it.buildStubTree(file)
        }

        if (file !is CjFile) return super.buildStubTree(file)
        val node = file.node
        val root = createStubForFile(file)
        val sourceRoot = root as? CangJieFileStubImpl ?: return buildStubTreeFor(node, root)
        if (sourceRoot.cjmpRoleSnapshot == null) return buildStubTreeFor(node, root)
        val declarationOwners = mutableListOf<Pair<StubElement<*>, ASTNode>>()
        val visitor = object : StubBuildingWalkingVisitor(node, root) {
            override fun createStub(parentStub: StubElement<*>, node: ASTNode): StubElement<*>? {
                val stub = super.createStub(parentStub, node) ?: return null
                if (node.psi is CjDeclaration) {
                    declarationOwners += stub to node
                }
                return stub
            }
        }
        visitor.buildStubTree()
        // 遍历可能展开新的 parser 根；只能在真实 stub 结构完成后绑定最终事件及返回属性。
        val snapshot = checkNotNull(file.collectParsedRoleSnapshot()) {
            "A source stub build must retain the same parsed source input."
        }
        val bindings = linkedMapOf<List<Int>, List<CjmpParsedRoleEventId>>()
        val declarationModifiers = linkedMapOf<List<Int>, CjParsedDeclarationModifiers>()
        for ((stub, owner) in declarationOwners) {
            val path = checkNotNull(stub.cjmpPathIn(root)) { "声明 stub 不属于当前构建的文件树。" }
            owner.declarationModifiersFromActualRegion(file)?.let { modifiers ->
                check(modifiers.parsingContext == snapshot.parsingContext)
                check(declarationModifiers.put(path, modifiers) == null)
            }
            val events = generateSequence(owner.firstChildNode) { it.treeNext }
                .mapNotNull { it.cjmpDeclarationRoleEventsIn(snapshot) }.flatten().toList()
            if (events.isNotEmpty()) {
                val ordered = events.sortedBy { checkNotNull(snapshot.eventOrder(it)) }
                check(bindings.put(path, ordered.map { it.id }) == null) { "声明 stub 路径重复。" }
            }
        }
        sourceRoot.publishCjmpStubBindings(snapshot, bindings, declarationModifiers)
        return root
    }

    /**
     * 实现 `createStubForFile` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun createStubForFile(file: PsiFile): StubElement<*> {
        if (file !is CjFile) {
            return super.createStubForFile(file)
        }

        val packageFqName = file.packageFqName
        val errorMessage = findErrorMessage(file)

        val kind = when {
            errorMessage != null -> CangJieFileStubKindImpl.Invalid(errorMessage)
            file.hasTopLevelCallables() -> {
                CangJieFileStubKindImpl.Facade(
                    packageFqName = packageFqName,
                    facadeFqName = packageFqName,
                )
            }

            else -> CangJieFileStubKindImpl.File(packageFqName = packageFqName)
        }

        // 文档可能先于 PSI/stub 提交；旧事务的事件不能归属于当前文本。
        val roleSnapshot = if (file.isCompiled) null else file.parsedRoleSnapshot?.takeIf {
            it.matchesSourceContent(file.viewProvider.contents) == true
        }
        return CangJieFileStubImpl(file, kind, roleSnapshot)
    }
}

/**
 * Searches for a special error message from [CangJieFileStubKindImpl.Invalid].
 *
 * For now this place is aligned only with the decompiler.
 */
private fun findErrorMessage(file: CjFile): String? {
    if (!file.isCompiled) return null
    val firstComment = file.importList?.nextSibling as? PsiComment ?: return null
    if (firstComment.textMatches(CangJieCompiledFileErrors.NEWER_VERSION_DECOMPILE_ERROR)) {
        return file.text
    }

    return null
}
