/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.LLCfirDiagnosticVisitor
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.getNonLocalContainingOrThisElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.isAutonomousElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.elementCanBeLazilyResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.*
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjVisitorUnit
import org.cangnova.cangjie.psi.psiUtil.isAncestorOf
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import java.util.concurrent.ConcurrentHashMap

/**
 * Aggregates [KT][CjElement] -> [CFIR][org.cangnova.cangjie.cfir.CfirElement] mappings and diagnostics for the associated [CjFile].
 *
 * For every [CjFile] we need a mapping for, we have a [FileStructure] which contains a tree-like structure of [FileStructureElement]s.
 *
 * When we want to get a `KT -> CFIR` mapping,
 * we [getOrPut][getStructureElementFor] a [FileStructureElement] for the closest non-local element (usually a declaration)
 * which contains the requested [CjElement].
 *
 * Some [FileStructureElement]s can be invalidated in case of an in-block PSI modification.
 * See [invalidateElement] and [LLCfirDeclarationModificationService] for details.
 *
 * The mapping is an optimization to avoid searching for the associated [CfirElement][org.cangnova.cangjie.cfir.CfirElement]
 * by a [CjElement], as it requires a deep traversal through the main element of [FileStructureElement].
 *
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.CfirElementBuilder
 * @see FileStructureElement
 * @see LLCfirDeclarationModificationService
 */
internal class FileStructure private constructor(
    private val cjFile: CjFile,
    private val cfirFile: CfirFile,
    private val moduleComponents: LLCfirModuleResolveComponents,
) {
    companion object {
        fun build(
            cjFile: CjFile,
            moduleComponents: LLCfirModuleResolveComponents,
        ): FileStructure {
            val cfirFile = moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(cjFile)
            return FileStructure(cjFile, cfirFile, moduleComponents)
        }

        /**
         * Returns [CjElement] which will be used inside [getStructureElementFor].
         * `null` means that [CjElement.containingCjFile] will be used instead.
         *
         * @see getNonLocalContainingOrThisElement
         */
        private fun findNonLocalContainer(element: CjElement): CjElement? {
            return element.getNonLocalContainingOrThisElement(predicate = CjElement::isAutonomousElement)
        }
    }

    private val cfirProvider = cfirFile.moduleData.session.cfirProvider

    private val structureElements = ConcurrentHashMap<CjElement, FileStructureElement>()

    /**
     * Must be called only under write-lock.
     *
     * This method is responsible for "invalidation" of re-analyzable declarations.
     *
     * @see LLCfirDeclarationModificationService
     * @see getNonLocalReanalyzableContainingDeclaration
     */
    fun invalidateElement(element: CjElement) {
        val container = getContainerCjElement(element, findNonLocalContainer(element))
        structureElements.remove(container)
    }

    /**
     * @return [FileStructureElement] for the closest non-local element which contains this [element].
     */
    fun getStructureElementFor(
        element: CjElement,
        nonLocalContainer: CjElement? = findNonLocalContainer(element),
    ): FileStructureElement {
        val container = getContainerCjElement(element, nonLocalContainer)
        return structureElements.getOrPut(container) { createStructureElement(container) }
    }

    private fun addStructureElementForTo(element: CjElement, result: MutableCollection<FileStructureElement>) {
        checkCanceled()
        LLCfirDiagnosticVisitor.suppressAndLogExceptions {
            result += getStructureElementFor(element)
        }
    }

    private fun getContainerCjElement(element: CjElement, nonLocalContainer: CjElement?): CjElement {
        return getStructureCjElement(element, nonLocalContainer) ?: element.containingCjFile
    }

    private fun getStructureCjElement(element: CjElement, nonLocalContainer: CjElement?): CjElement? {
        val container = if (nonLocalContainer?.isAutonomousElement == true)
            nonLocalContainer
        else {
            nonLocalContainer?.let(::findNonLocalContainer)
        }

        val resultedContainer = when {
            container is CjTypeStatement && container.isPartOfSuperClassCall(element) -> {
                container.primaryConstructor
            }
            else -> null
        }

        return resultedContainer ?: container
    }

    private fun CjTypeStatement.isPartOfSuperClassCall(element: CjElement): Boolean {
        for (entry in superTypeListEntries) {
            if (entry !is CjSuperTypeCallEntry) continue

            // the structure element for `CjTypeReference` inside the super class call is a class declaration and not a primary constructor
            val typeReference = entry.calleeExpression.typeReference
            val typeReferenceIsAncestor = typeReference == element || typeReference?.isAncestorOf(element) == true
            if (typeReferenceIsAncestor) return false

            // the structure element for `CjSuperTypeCallEntry` is a primary constructor
            if (entry == element || entry.isAncestorOf(element)) return true
        }

        return false
    }

    fun getAllDiagnosticsForFile(diagnosticCheckerFilter: DiagnosticCheckerFilter): List<CjPsiDiagnostic> {
        val structureElements = getAllStructureElements()
        return buildList {
            collectDiagnosticsFromStructureElements(structureElements, diagnosticCheckerFilter)
        }
    }

    private fun MutableCollection<CjPsiDiagnostic>.collectDiagnosticsFromStructureElements(
        structureElements: Collection<FileStructureElement>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter,
    ) {
        structureElements.forEach { structureElement ->
            ProgressManager.checkCanceled()

            structureElement.diagnostics.forEach(diagnosticCheckerFilter) { diagnostics ->
                addAll(diagnostics)
            }
        }
    }

    fun getAllStructureElements(): Collection<FileStructureElement> {
        val structureElements = mutableSetOf<FileStructureElement>()
        addStructureElementForTo(cjFile, structureElements)

        cjFile.accept(object : CjVisitorUnit() {
            override fun visitElement(element: PsiElement) {
                element.acceptChildren(this)
            }

            override fun visitDeclaration(dcl: CjDeclaration) {
                addStructureElementForTo(dcl, structureElements)

                // Go down only in the case of container declaration
                val canHaveInnerStructure = dcl is CjTypeStatement
                if (canHaveInnerStructure) {
                    dcl.acceptChildren(this)
                }
            }

        })

        return structureElements.toList().asReversed()
    }

    private fun createRootStructure(): RootStructureElement {
        val cfirFile = moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(cjFile)
        cfirFile.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE.previous)
        return RootStructureElement(cfirFile, moduleComponents)
    }

    private fun createCodeFragmentStructure(): DeclarationStructureElement {
        val cfirCodeFragment = cfirFile.codeFragment
        cfirCodeFragment.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        return DeclarationStructureElement(cfirFile, cfirCodeFragment, moduleComponents)
    }

    private fun createDeclarationStructure(declaration: CjDeclaration): FileStructureElement {
        val cfirDeclaration = declaration.findSourceNonLocalCfirDeclaration(cfirFile, cfirProvider)
        return FileElementFactory.createFileStructureElement(
            cfirDeclaration = cfirDeclaration,
            cfirFile = cfirFile,
            moduleComponents = moduleComponents
        )
    }

    private fun createStructureElement(container: CjElement): FileStructureElement = when (container) {
        is CjCodeFragment -> createCodeFragmentStructure()
        is CjFile -> createRootStructure()
        is CjDeclaration -> createDeclarationStructure(container)
        else -> errorWithAttachment("Invalid container ${container::class}") {
            withPsiEntry("container", container)
        }
    }
}
