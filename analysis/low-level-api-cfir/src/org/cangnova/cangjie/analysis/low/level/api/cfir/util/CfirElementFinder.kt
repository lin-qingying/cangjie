/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.util.containers.ContainerUtil
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.patchDesignationPathIfNeeded
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

internal class CfirElementFinder : CfirSessionComponent {
    companion object {
        fun findClassifierWithClassId(
            cfirFile: CfirFile,
            classId: ClassId,
        ): CfirClassLikeDeclaration? = collectDesignationPath(
            cfirFile = cfirFile,
            containerClassId = null,
            targetDeclarationName = classId.shortClassName,
            expectedDeclarationAcceptor = { it is CfirClassLikeDeclaration },
        )?.target?.let { it as CfirClassLikeDeclaration }

        fun collectDesignationPath(
            cfirFile: CfirFile,
            declarationContainerClassId: ClassId?,
            targetMemberDeclaration: CfirDeclaration,
        ): CfirDesignation? = collectDesignationPath(
            cfirFile = cfirFile,
            containerClassId = declarationContainerClassId,
            targetDeclarationName = CfirFileStructureNode.mappingName(targetMemberDeclaration),
            expectedDeclarationAcceptor = { it == targetMemberDeclaration },
        )

        fun findDeclaration(cfirFile: CfirFile, nonLocalDeclaration: CjDeclaration): CfirDeclaration? = collectDesignationPath(
            cfirFile = cfirFile,
            nonLocalDeclaration = nonLocalDeclaration,
        )?.declarationTarget

        fun findPathToDeclarationWithTarget(
            cfirFile: CfirFile,
            nonLocalDeclaration: CjDeclaration,
        ): List<CfirDeclaration>? = collectDesignationPath(
            cfirFile = cfirFile,
            nonLocalDeclaration = nonLocalDeclaration,
        )?.let { it.path + it.declarationTarget }

        fun collectDesignationPath(
            cfirFile: CfirFile,
            nonLocalDeclaration: CjDeclaration,
        ): CfirDesignation? = collectDesignationPath(
            cfirFile = cfirFile,
            containerClassId = nonLocalDeclaration.containingTypeStatement?.getClassId(),
            targetDeclarationName = CfirFileStructureNode.mappingNameByPsi(nonLocalDeclaration),
            expectedDeclarationAcceptor = { it.psi == nonLocalDeclaration },
        )

        inline fun <reified E : CfirElement> findElementIn(
            container: CfirElement,
            crossinline canGoInside: (E) -> Boolean = { true },
            crossinline predicate: (E) -> Boolean,
        ): E? {
            var result: E? = null
            container.accept(object : CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    when {
                        result != null -> return
                        element !is E || element is CfirFile -> element.acceptChildren(this)
                        predicate(element) -> result = element
                        canGoInside(element) -> element.acceptChildren(this)
                    }
                }
            })

            return result
        }

        /**
         * @see collectDesignationPath
         */
        private val CfirDesignation.declarationTarget: CfirDeclaration get() = target as CfirDeclaration

        /**
         * @return [CfirDesignation] where [CfirDesignation.target] is [CfirDeclaration]
         *
         * @see declarationTarget
         */
        private fun collectDesignationPath(
            cfirFile: CfirFile,
            containerClassId: ClassId?,
            targetDeclarationName: Name?,
            expectedDeclarationAcceptor: (CfirDeclaration) -> Boolean,
        ): CfirDesignation? {
            if (containerClassId != null) {
                requireWithAttachment(
                    cfirFile.packageDirective.packageFqName == containerClassId.packageFqName,
                    { "ClassId package must match the file package" }
                ) {
                    withEntry("CfirFile.packageName", cfirFile.packageDirective.packageFqName) { it.asString() }
                    withEntry("ClassId.packageName", containerClassId.packageFqName) { it.asString() }
                }
            }

            val pathSegments = containerClassId?.relativeClassName?.pathSegments().orEmpty()
            val resultPath = ArrayList<CfirDeclaration>(pathSegments.size + 1)
            resultPath += cfirFile

            val structure = cfirFile.llCfirSession.cfirElementFinder.buildRootFileStructureNode(cfirFile)
            val result = structure.find(
                pathSegments = pathSegments,
                resultPath = resultPath,
                targetDeclarationName = targetDeclarationName,
                expectedDeclarationAcceptor = expectedDeclarationAcceptor,
            ) ?: return null

            return CfirDesignation(
                path = patchDesignationPathIfNeeded(result, resultPath).takeUnless(List<*>::isEmpty) ?: emptyList(),
                target = result,
            )
        }
    }

    private val cache = ContainerUtil.createConcurrentWeakKeySoftValueMap<CfirFile, CfirFileStructureNode>()

    private fun buildRootFileStructureNode(cfirFile: CfirFile): CfirFileStructureNode = cache.getOrPut(cfirFile) {
        CfirFileStructureNode.build(cfirFile)
    }
}

private val CfirSession.cfirElementFinder: CfirElementFinder by CfirSession.sessionComponentAccessor()

/**
 * This class represents non-local declarations from a [CfirFile] in a tree-like structure.
 * Each [CfirFileStructureNode] is associated with a corresponding [Name] from [CfirFileStructureNode.element] by [mappingName].
 *
 * ```kotlin
 * class TopLevelClass {
 *     class NestedClass {
 *         fun method() {}
 *         val property: Int = 0
 *     }
 *
 *     fun value() {}
 *     val value: Int = 1
 * }
 *
 * fun topLevelFunction() {}
 * fun topLevelFunction(i: Int) {}
 *```
 * For this file the structure will look like:
 * ```mermaid
 * graph LR
 *     File([File]) --> 'TopLevelClass'
 *     File --> 'topLevelFunction'
 *     'TopLevelClass' --> TopLevelClass(["class TopLevelClass"])
 *     'topLevelFunction' --> topLevelFunction_0(["fun topLevelFunction()"])
 *     'topLevelFunction' --> topLevelFunction_1(["fun topLevelFunction(i: Int)"])
 *     TopLevelClass --> 'NestedClass'
 *     TopLevelClass --> 'value'
 *     TopLevelClass --> 'TopLevelClass_cons'("'#60;init#62;'")
 *     'NestedClass' --> NestedClass(["class NestedClass"])
 *     'TopLevelClass_cons' --> TopLevelClass_cons(["constructor()"])
 *     'value' --> value_fun(["fun value()"])
 *     'value' --> value_prop(["val value"])
 *     NestedClass --> 'NestedClass_cons'("'#60;init#62;'")
 *     NestedClass --> 'method'
 *     'NestedClass_cons' --> NestedClass_cons(["constructor()"])
 *     'method' --> method(["fun method()"])
 * ```
 *
 * @see build
 * @see mappingName
 * @see CfirElementFinder
 */
private sealed class CfirFileStructureNode(val element: CfirDeclaration) {
    /**
     * Represents a [CfirDeclaration] which can have non-local nested declarations.
     * Currently, it is [CfirFile] and [CfirClass].
     *
     * @param element a container declaration.
     * @param elements nested [CfirFileStructureNode] nodes based on the [element] directly nested declarations grouped by [mappingName].
     */
    private class Container(element: CfirDeclaration, val elements: Map<Name, List<CfirFileStructureNode>>) : CfirFileStructureNode(element)

    /**
     * Represents a [CfirDeclaration] which cannot have non-local nested declarations.
     */
    private class Leaf(element: CfirDeclaration) : CfirFileStructureNode(element)

    /**
     * ```kotlin
     * // FILE: main.kt
     * package pack
     *
     * class TopLevel {
     *   class Nested {
     *     fun method() {}
     *   }
     * }
     * ```
     *
     * [pathSegments] examples:
     * - `method`: `listOf(TopLevel, Nested)`
     * - `Nested`: `listOf(TopLevel)`
     *
     * @param pathSegments a path to a target declaration.
     *   It must contain only [CfirClass] classes.
     *   The target declaration and the [CfirFile] is not included.
     *
     * @param resultPath a list into which a path to a target declaration will be added.
     * @param targetDeclarationName the [mappingName] of a target declaration. It helps to perform the search more efficiently if present.
     * @param expectedDeclarationAcceptor a predicate that will be called on potential target declaration.
     *   It should return **true** for the expected target declaration.
     *
     * @return a target declaration if found
     */
    fun find(
        pathSegments: List<Name>,
        resultPath: MutableList<CfirDeclaration>,
        targetDeclarationName: Name?,
        expectedDeclarationAcceptor: (CfirDeclaration) -> Boolean,
    ): CfirDeclaration? = find(
        pathSegments = pathSegments,
        pathIndex = 0,
        resultPath = resultPath,
        targetDeclarationName = targetDeclarationName,
        expectedDeclarationAcceptor = expectedDeclarationAcceptor,
    )

    /**
     * assigned index:           0              1                    [targetDeclarationName]/[expectedDeclarationAcceptor]
     * result path: [CfirFile] -> [CfirClass] -> [CfirClass] -> [CfirDeclaration]
     * path index:  0            1              2                    3
     */
    private fun find(
        pathSegments: List<Name>,
        pathIndex: Int,
        resultPath: MutableList<CfirDeclaration>,
        targetDeclarationName: Name?,
        expectedDeclarationAcceptor: (CfirDeclaration) -> Boolean,
    ): CfirDeclaration? {
        if (this !is Container) return null

        val nextSegmentName = pathSegments.getOrNull(pathIndex)
        if (nextSegmentName != null) {
            val structures = elements[nextSegmentName] ?: return null

            for (structure in structures) {
                resultPath += structure.element
                val result = structure.find(
                    pathSegments = pathSegments,
                    pathIndex = pathIndex + 1,
                    resultPath = resultPath,
                    targetDeclarationName = targetDeclarationName,
                    expectedDeclarationAcceptor = expectedDeclarationAcceptor,
                )

                if (result != null) {
                    return result
                }

                resultPath.removeLast()
            }
            return null
        }

        return if (targetDeclarationName != null) {
            val structures = elements[targetDeclarationName].orEmpty()
            structures.firstNotNullOfOrNull {
                it.element.takeIf(expectedDeclarationAcceptor)
            }
        } else {
            elements.values.firstNotNullOfOrNull { structures ->
                structures.firstNotNullOfOrNull {
                    it.element.takeIf(expectedDeclarationAcceptor)
                }
            }
        }
    }

    companion object {
        fun build(element: CfirDeclaration): CfirFileStructureNode = when (element) {
            is CfirFile -> Container(
                element = element,
                elements = convertDeclarations(element.declarations),
            )

            is CfirClassLikeDeclaration -> Container(
                element = element,
                elements = convertDeclarations(element.declarations),
            )

            is CfirExtend -> Container(
                element = element,
                elements = convertDeclarations(element.declarations),
            )

            else -> Leaf(element)
        }

        /**
         * [LinkedHashMap] is used to preserve the original declarations order.
         */
        private fun convertDeclarations(
            declarations: List<CfirDeclaration>,
            destination: LinkedHashMap<Name, MutableList<CfirFileStructureNode>> = linkedMapOf(),
        ): Map<Name, List<CfirFileStructureNode>> = declarations.groupByTo(
            destination,
            keySelector = ::mappingName,
            valueTransform = ::build,
        )

        /**
         * @see mappingNameByPsi
         */
        fun mappingName(declaration: CfirDeclaration): Name = when (declaration) {
            is CfirClassLikeDeclaration -> declaration.symbol.name
            is CfirExtend -> declaration.psi.let { psi ->
                val extendPsi = psi as? CjExtend
                requireWithAttachment(
                    extendPsi != null,
                    { "Source extend declaration should keep CjExtend PSI for LL designation mapping" },
                ) {
                    withEntry("declarationClass", declaration::class.simpleName ?: "<anonymous>")
                    withEntry("origin", declaration.origin.toString())
                }
                extendPsi.nameAsSafeName
            }
            is CfirNamedFunction -> declaration.name
            is CfirMainFunction -> declaration.symbol.name
            is CfirMacroDeclaration -> declaration.name
            is CfirFinalizer -> declaration.symbol.name
            is CfirProperty -> declaration.name
            is CfirFieldVariable -> declaration.symbol.name
            is CfirConstructor -> SpecialNames.INIT
            is CfirEnumConstructor -> declaration.name
            is CfirTypeAlias -> declaration.name

            is CfirFile,
            is CfirAnonymousFunction,
            is CfirErrorFunction,
            is CfirPropertyAccessor,
            is CfirTypeParameter,
            is CfirPatternVariable,
            is CfirPatternBindingVariable,
                -> errorWithCfirSpecificEntries("Unexpected declaration ${declaration::class.simpleName}", cfir = declaration)

            else -> errorWithCfirSpecificEntries("Unexpected declaration ${declaration::class.simpleName}", cfir = declaration)
        }

        /**
         * This implementation must be in sync with the [mappingName].
         *
         * It may return `null` if there is no fast way to get the correct name.
         *
         * It is based on [org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder], but [mappingName] may rewrite/simplify some rules.
         */
        fun mappingNameByPsi(declaration: CjDeclaration): Name? = when (declaration) {
            is CjConstructor<*> -> SpecialNames.INIT
            is CjCodeFragment -> SpecialNames.NO_NAME_PROVIDED
            is CjEnumConstructor -> declaration.name?.let(Name::identifier)
            is CjTypeStatement, is CjTypeAlias, is CjNamedFunction, is CjProperty -> declaration.nameAsSafeName
            else -> null
        }
    }
}
