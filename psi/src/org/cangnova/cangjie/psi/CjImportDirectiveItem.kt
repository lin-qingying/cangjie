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
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.ImportPathPrefix
import org.cangnova.cangjie.resolveImportPath
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import org.cangnova.cangjie.psi.stubs.CangJieImportItemStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/** 导入语句容器；共享路径由分组拥有，语句只拥有修饰符和条件。 */
class CjImportDirective : CjDeclarationStub<CangJieImportDirectiveStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieImportDirectiveStub) : super(stub, CjStubElementTypes.IMPORT_DIRECTIVE)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitImportDirective(this, data)

    /** 仅展开导入结构，按源码顺序返回所有叶项；Stub 模式不访问 AST。 */
    val importItems: List<CjImportItem>
        get() {
            val elements = stub?.childrenStubs?.map { it.psi } ?: children.toList()
            return elements.flatMap {
                when (it) {
                    is CjImportItem -> listOf(it)
                    is CjImportGroup -> it.importItems
                    else -> emptyList()
                }
            }
        }

    val isValidSyntax: Boolean
        get() = stub?.isValidSyntax ?: !hasImportContainerErrors(this)

    fun getModifier(tokenType: CjKeywordToken): PsiElement? = findChildByType(tokenType)
    override fun hasModifier(modifier: CjKeywordToken): Boolean = getModifier(modifier) != null
}

/** 单个源码导入项；完整路径由局部路径和所属组组合，不构造虚拟表达式。 */
class CjImportItem : CjElementImplStub<CangJieImportItemStub>, CjImportInfo, CjImportPathOwner {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieImportItemStub) : super(stub, CjStubElementTypes.IMPORT_ITEM)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitImportItem(this, data)
    override val organizationReference: CjSimpleNameExpression?
        get() = importOrganizationReference(stub?.path)
    override val importedReference: CjExpression?
        get() = importLocalReference(stub?.path)
    val localFqName: FqName?
        get() { stub?.let { return it.path.localFqName }; return importFqName(importedReference) }
    val hasOrganizationQualifier: Boolean
        get() = stub?.path?.hasOrganizationQualifier ?: (node.findChildByType(CjTokens.DOUBLE_COLON) != null)
    val importGroup: CjImportGroup?
        get() = parent as? CjImportGroup
    val importDirective: CjImportDirective
        get() = requireNotNull(getStrictParentOfType<CjImportDirective>())

    override val organizationName: Name?
        get() {
            importGroup?.let { return it.organizationName }
            stub?.let { return it.path.organizationName }
            return organizationReference?.referencedNameAsName
        }

    private val resolvedPath: ImportPathPrefix?
        get() {
            if (!isValidImport) return null
            val local = localFqName ?: return null
            val group = importGroup
            val prefix = group?.let { ImportPathPrefix(it.organizationName, it.localFqName ?: FqName.ROOT) }
            return prefix.resolveImportPath(local, if (group == null) organizationName else null)
        }

    override val importedFqName: FqName?
        get() = resolvedPath?.fqName
    override val isAllUnder: Boolean
        get() = stub?.isAllUnder ?: (node.findChildByType(CjTokens.MUL) != null)
    val alias: CjImportAlias?
        get() = getStubOrPsiChild(CjStubElementTypes.IMPORT_ALIAS)
    override val aliasName: String?
        get() { stub?.let { return it.aliasName }; return alias?.name }
    override val importContent: CjImportInfo.ImportContent?
        get() = importedFqName?.let { CjImportInfo.ImportContent.FqNameBased(it) }
    override val importedName: Name?
        get() = if (isAllUnder) null else aliasName?.let(Name::identifier) ?: importedFqName?.shortName()

    /** 兄弟项错误不污染本项；组的列表/前缀错误和语句错误则使本项无效。 */
    val isValidImport: Boolean
        get() = (stub?.path?.isValidSyntax ?: !hasImportContainerErrors(this)) &&
            (importGroup?.isValidSyntax != false) && importDirective.isValidSyntax

    val importPath: ImportPath?
        get() = resolvedPath?.let { ImportPath(it.fqName, isAllUnder, aliasName?.let(Name::identifier), it.organizationName) }

    /** 删除叶项时同时维护列表分隔符，组内其余空白和注释保留原位。 */
    override fun delete() {
        val group = importGroup
        if (group == null || group.importItems.size == 1) {
            importDirective.delete()
            return
        }
        val followingComma = generateSequence(nextSibling) { it.nextSibling }
            .takeWhile { it !is CjImportItem && it.node.elementType != CjTokens.RBRACE }
            .firstOrNull { it.node.elementType == CjTokens.COMMA }
        val comma = followingComma ?: generateSequence(prevSibling) { it.prevSibling }
            .takeWhile { it !is CjImportItem && it.node.elementType != CjTokens.LBRACE }
            .firstOrNull { it.node.elementType == CjTokens.COMMA }
        comma?.delete()
        super.delete()
    }
}

/** 无 PSI 的单项导入信息，同样保留组织限定。 */
data class CangJieImportField(
    override val isAllUnder: Boolean,
    override val importContent: CjImportInfo.ImportContent?,
    override val importedFqName: FqName?,
    override val aliasName: String?,
    override val organizationName: Name? = null,
) : CjImportInfo
