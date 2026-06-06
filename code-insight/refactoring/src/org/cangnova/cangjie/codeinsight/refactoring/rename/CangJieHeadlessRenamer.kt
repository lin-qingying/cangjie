/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.codeinsight.refactoring.rename

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.RefactoringHelper
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringListenerManager
import com.intellij.refactoring.listeners.impl.RefactoringListenerManagerImpl
import com.intellij.refactoring.listeners.impl.RefactoringTransaction
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.refactoring.suggested.SuggestedRefactoringProvider
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.refactoring.util.RelatedUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewUtil
import com.intellij.util.containers.MultiMap

/**
 * 无 UI rename 执行器。
 *
 * 该类按 Kotlin LSP `Renamer` 的结构复用 IntelliJ refactoring 基础设施，但放在仓颉
 * `code-insight:refactoring` 中，作为 IDE 插件与 LSP 共同使用的语言重构核心。
 */
class CangJieHeadlessRenamer(
    private val project: Project,
    target: PsiElement,
    private val newName: String,
    private val searchInComments: Boolean,
    private val searchTextOccurrences: Boolean,
    private val refactoringScope: SearchScope = GlobalSearchScope.projectScope(project),
) {
    private val primaryElement: PsiElement =
        RenamePsiElementProcessor.forElement(target).substituteElementToRename(target, null) ?: target
    private val allRenames = linkedMapOf<PsiElement, String>()
    private val renamers = mutableListOf<AutomaticRenamer>()
    private val skippedUsages = mutableListOf<UnresolvableCollisionUsageInfo>()
    private var nonCodeUsages = emptyArray<NonCodeUsageInfo>()
    private val usages: Array<UsageInfo>

    val originals: Map<String, Pair<PsiFile, String>>
        get() = _originals
    private val _originals: MutableMap<String, Pair<PsiFile, String>> = linkedMapOf()

    init {
        RenameUtil.assertNonCompileElement(primaryElement)
        allRenames[primaryElement] = newName
        prepareRenaming(primaryElement, newName, allRenames)
        usages = initUsagesAndRenamers()
    }

    fun rename() {
        if (!primaryElement.isValid) return
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        var usagesIn = usages
        val conflicts = MultiMap<PsiElement?, String?>()
        RenameUtil.addConflictDescriptions(usagesIn, conflicts)
        RenamePsiElementProcessor.forElement(primaryElement)
            .findExistingNameConflicts(primaryElement, newName, conflicts, allRenames)
        check(conflicts.isEmpty) {
            conflicts.values().filterNotNull().sorted().joinToString(separator = "\n")
        }

        val variableUsages = mutableListOf<UsageInfo>()
        if (renamers.isNotEmpty()) {
            findRenamedVariables(variableUsages)
            val renames = linkedMapOf<PsiElement, String>()
            for (renamer in renamers) {
                for (variable in renamer.elements) {
                    val variableNewName = renamer.getNewName(variable) ?: continue
                    addElement(variable, variableNewName)
                    prepareRenaming(variable, variableNewName, renames)
                }
            }
            if (renames.isNotEmpty()) {
                for (element in renames.keys) {
                    RenameUtil.assertNonCompileElement(element)
                }
                allRenames.putAll(renames)
                for ((element, elementNewName) in renames) {
                    variableUsages += RenameUtil.findUsages(
                        element,
                        elementNewName,
                        refactoringScope,
                        searchInComments,
                        searchTextOccurrences,
                        allRenames,
                    )
                }
            }
        }

        for ((element, elementNewName) in allRenames) {
            RenameUtil.checkRename(element, elementNewName)
        }

        val usagesSet = linkedSetOf(*usagesIn)
        usagesSet.addAll(variableUsages)
        RenameUtil.removeConflictUsages(usagesSet)?.let(skippedUsages::addAll)
        usagesIn = usagesSet.toTypedArray()

        execute(usagesIn)
    }

    private fun initUsagesAndRenamers(): Array<UsageInfo> {
        val result = mutableListOf<UsageInfo>()
        val foundUsages = RenameUtil.findUsages(
            primaryElement,
            newName,
            refactoringScope,
            searchInComments,
            searchTextOccurrences,
            allRenames,
        )
        val usagesList = listOf(*foundUsages)
        result.addAll(usagesList)

        for (factory in AutomaticRenamerFactory.EP_NAME.extensionList) {
            if (factory.getOptionName() == null && factory.isApplicable(primaryElement)) {
                renamers += factory.createRenamer(primaryElement, newName, usagesList)
            }
        }

        return UsageViewUtil.removeDuplicatedUsages(result.toTypedArray())
    }

    private fun prepareRenaming(
        element: PsiElement,
        elementNewName: String,
        renames: MutableMap<PsiElement, String>,
    ) {
        for (processor in RenamePsiElementProcessor.allForElement(element)) {
            processor.prepareRenaming(element, elementNewName, renames)
        }
    }

    private fun findRenamedVariables(variableUsages: MutableList<UsageInfo>) {
        for (renamer in renamers) {
            for (element in renamer.elements) {
                renamer.setRename(element, renamer.getNewName(element))
            }
        }
        for (renamer in renamers) {
            renamer.findUsages(variableUsages, searchInComments, searchTextOccurrences, skippedUsages, allRenames)
        }
    }

    private fun addElement(element: PsiElement, elementNewName: String) {
        RenameUtil.assertNonCompileElement(element)
        allRenames[element] = elementNewName
    }

    private fun execute(usagesIn: Array<UsageInfo>) {
        saveOriginalFileTexts(usagesIn)
        doRefactoring(linkedSetOf(*usagesIn))
        SuggestedRefactoringProvider.getInstance(project).reset()
    }

    private fun doRefactoring(usageInfoSet: MutableCollection<UsageInfo>) {
        val writableUsageInfos = removeNonWritableUsages(usageInfoSet)
        val data = RefactoringEventData()
        data.addElement(primaryElement)
        data.addUsages(listOf(*writableUsageInfos))

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val listenerManager = RefactoringListenerManager.getInstance(project) as RefactoringListenerManagerImpl
        val transaction = listenerManager.startTransaction()
        val preparedData = linkedMapOf<RefactoringHelper<*>?, Any?>()
        val allElements = listOfNotNull(primaryElement)

        for (helper in RefactoringHelper.EP_NAME.extensionList) {
            preparedData[helper] = helper.prepareOperation(writableUsageInfos, allElements)
        }

        var commandFailure: Throwable? = null
        CommandProcessor.getInstance().executeCommand(project, {
            try {
                WriteAction.run<Throwable> {
                    performRefactoring(writableUsageInfos, transaction)
                    for ((helper, operation) in preparedData) {
                        @Suppress("UNCHECKED_CAST")
                        (helper as RefactoringHelper<Any>).performOperation(project, operation)
                    }
                    transaction.commit()
                    RenameUtil.renameNonCodeUsages(project, nonCodeUsages)
                }
            } catch (throwable: Throwable) {
                commandFailure = throwable
                throw throwable
            }
        }, null, null)
        commandFailure?.let { throw it }
    }

    private fun performRefactoring(usagesIn: Array<UsageInfo>, transaction: RefactoringTransaction) {
        val postRenameCallbacks = mutableListOf<Runnable>()
        val renameEvents = MultiMap.createLinked<RefactoringElementListener, SmartPsiElementPointer<PsiElement>>()
        val usagesList = listOf(*usagesIn)
        val classified = classifyUsages(allRenames.keys, usagesList)

        for ((element, elementNewName) in allRenames) {
            if (!element.isValid) continue

            val elementListener = transaction.getElementListener(element)
            val infos = classified[element]
            val processor = RenamePsiElementProcessor.forElement(element)
            processor.getPostRenameCallback(element, elementNewName, infos, allRenames, elementListener)
                ?.let(postRenameCallbacks::add)

            processor.renameElement(
                element,
                elementNewName,
                infos.toTypedArray(),
                object : RefactoringElementListener {
                    override fun elementMoved(newElement: PsiElement) = Unit

                    override fun elementRenamed(newElement: PsiElement) {
                        if (newElement.isValid) {
                            renameEvents.putValue(elementListener, SmartPointerManager.createPointer(newElement))
                        }
                    }
                },
            )
        }

        nonCodeUsages = usagesList.filterIsInstance<NonCodeUsageInfo>().toTypedArray()
        afterRename(postRenameCallbacks, renameEvents)
    }

    private fun afterRename(
        postRenameCallbacks: List<Runnable>,
        renameEvents: MultiMap<RefactoringElementListener, SmartPsiElementPointer<PsiElement>>,
    ) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        for ((listener, pointers) in renameEvents.entrySet()) {
            for (pointer in pointers) {
                pointer.element?.let(listener::elementRenamed)
            }
        }
        postRenameCallbacks.forEach(Runnable::run)
    }

    private fun removeNonWritableUsages(usageInfoSet: MutableCollection<UsageInfo>): Array<UsageInfo> {
        val iterator = usageInfoSet.iterator()
        while (iterator.hasNext()) {
            val usageInfo = iterator.next()
            if (usageInfo.element == null || !usageInfo.isWritable) {
                iterator.remove()
            }
        }
        return usageInfoSet.toTypedArray()
    }

    private fun saveOriginalFileTexts(infos: Array<UsageInfo>) {
        _originals.clear()
        addFiles(infos.mapNotNull(UsageInfo::getFile))
        addFiles(allRenames.keys.mapNotNull(PsiElement::getContainingFile))
    }

    private fun addFiles(files: List<PsiFile>) {
        files.mapNotNull { file ->
            val virtualFile = file.virtualFile ?: return@mapNotNull null
            file to virtualFile.url
        }.distinctBy { (_, url) -> url }
            .forEach { (file, url) -> _originals[url] = file to file.text }
    }

    companion object {
        /**
         * 判断目标 PSI 是否有已注册的 rename processor。
         *
         * LSP 只能依赖 code-insight 的共享重构入口，不直接触碰 IntelliJ rename EP 实现细节。
         */
        fun canRename(target: PsiElement): Boolean =
            RenamePsiElementProcessor.forElement(target).canProcessElement(target)

        fun classifyUsages(
            elements: MutableCollection<out PsiElement>,
            usages: Collection<UsageInfo>,
        ): MultiMap<PsiElement, UsageInfo> {
            val result = MultiMap<PsiElement, UsageInfo>()
            for (usage in usages) {
                if (usage !is MoveRenameUsageInfo) continue
                if (usage is RelatedUsageInfo && usage.relatedElement in elements) {
                    result.putValue(usage.relatedElement, usage)
                    continue
                }

                val referenced = usage.referencedElement
                when {
                    referenced in elements -> result.putValue(referenced, usage)
                    referenced?.navigationElement in elements -> referenced?.navigationElement?.let { navigationElement ->
                        result.putValue(navigationElement, usage)
                    }
                }
            }
            return result
        }
    }
}
