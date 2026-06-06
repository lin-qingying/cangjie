/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.usageView.UsageInfo
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.psiUtil.isIdentifier
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded

/**
 * 仓颉 PSI rename processor 的公共基类。
 *
 * 对齐 Kotlin `RenameKotlinPsiProcessor` 的框架职责：统一命名合法化、引用搜索、
 * qualified name 更新和最终 `RenameUtil.doRenameGenericNamedElement` 调用。仓颉当前没有
 * Kotlin/JVM light element、expect/actual、Java accessor 等语义，因此这些 Kotlin-only
 * 分支不在本层发明本地替代。
 */
abstract class RenameCangJiePsiProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean = element is CjNamedDeclaration

    override fun getElementToSearchInStringsAndComments(element: PsiElement): PsiElement? {
        val declaration = element as? CjNamedDeclaration ?: return null
        return if (declaration.fqName == null) null else element
    }

    override fun getQualifiedNameAfterRename(element: PsiElement, newName: String, nonJava: Boolean): String? {
        if (!nonJava) return newName

        val qualifiedName = when (element) {
            is CjNamedDeclaration -> element.fqName?.asString() ?: element.name
            is PsiNamedElement -> element.name
            else -> return null
        } ?: return null

        val lastDot = qualifiedName.lastIndexOf('.')
        return if (lastDot >= 0) {
            qualifiedName.substring(0, lastDot + 1) + newName
        } else {
            newName
        }
    }

    override fun prepareRenaming(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<PsiElement, String>,
        scope: SearchScope,
    ) {
        if (!newName.isIdentifier()) {
            allRenames[element] = newName.quoteIfNeeded()
        }
    }

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean,
    ): Collection<PsiReference> = ReferencesSearch.search(element, searchScope).findAll()

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<UsageInfo>,
        listener: RefactoringElementListener?,
    ) {
        RenameUtil.doRenameGenericNamedElement(element, newName, usages, listener)
    }
}
