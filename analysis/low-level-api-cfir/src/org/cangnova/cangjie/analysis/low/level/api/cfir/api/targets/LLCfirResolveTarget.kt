/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * [target] element and optionally its subgraph to be lazily resolved by LL CFIR lazy resolver.
 *
 * Specifies the path to resolve targets and resolve targets themselves.
 * Those targets are going to be resolved by [LLCfirModuleLazyDeclarationResolver][org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirModuleLazyDeclarationResolver].
 *
 * @see CfirDesignation
 */
internal sealed class LLCfirResolveTarget(val designation: CfirDesignation) {
    /**
     * [CfirFile] where the targets are located.
     * Can be null if [target] does not belong to any file.
     * E.g., fake overrides.
     * @see org.cangnova.cangjie.cfir.scopes.impl.CfirFakeOverrideGenerator
     */
    val firFile: CfirFile? get() = designation.fileOrNull

    /**
     * The list of [CfirFile] and [CfirClass] which are
     * the required to go from file to target declarations in the top-down order.
     *
     * If resolve target is [CfirFile] or [CfirClass] itself, it's not included into the [path].
     */
    val path: List<CfirDeclaration> get() = designation.path

    /**
     * A dedicated main element.
     */
    val target: CfirElementWithResolveState get() = designation.target

    /**
     * Visit [path], [target] and optionally its subgraph.
     * Each nested declaration will be wrapped with corresponding [LLCfirResolveTargetVisitor.withFile]
     * and [LLCfirResolveTargetVisitor.withClass] recursively.
     */
    fun visit(visitor: LLCfirResolveTargetVisitor) {
        if (target is CfirFile) {
            visitor.performAction(target)
        }

        goToTarget(visitor)
    }

    private fun goToTarget(visitor: LLCfirResolveTargetVisitor) {
        val pathIterator = path.iterator()
        goToTarget(pathIterator, visitor)
    }

    private fun goToTarget(
        pathIterator: Iterator<CfirDeclaration>,
        visitor: LLCfirResolveTargetVisitor,
    ) {
        if (pathIterator.hasNext()) {
            when (val declaration = pathIterator.next()) {
                is CfirClass -> visitor.withClass(declaration) { goToTarget(pathIterator, visitor) }
                is CfirFile -> visitor.withFile(declaration) { goToTarget(pathIterator, visitor) }
                else -> errorWithCfirSpecificEntries(
                    "Unexpected declaration in path: ${declaration::class.simpleName}",
                    fir = declaration,
                )
            }
        } else {
            visitTargetElement(target, visitor)
        }
    }

    /**
     * [element] with [CfirFile] will be processed before.
     */
    protected abstract fun visitTargetElement(
        element: CfirElementWithResolveState,
        visitor: LLCfirResolveTargetVisitor,
    )

    /**
     * Executions the [action] for each target that this [LLCfirResolveTarget] represents.
     */
    fun forEachTarget(action: (CfirElementWithResolveState) -> Unit) {
        visit(object : LLCfirResolveTargetVisitor {
            override fun performAction(element: CfirElementWithResolveState) {
                action(element)
            }
        })
    }

    override fun toString(): String = buildString {
        append(this@LLCfirResolveTarget::class.simpleName)
        append("(")
        buildList {
            path.mapTo(this) {
                when (it) {
                    is CfirFile -> it.name
                    is CfirClass -> it.name
                    else -> errorWithCfirSpecificEntries("Unsupported path declaration: ${it::class.simpleName}", fir = it)
                }
            }

            add(toStringForTarget())
            toStringAdditionalSuffix()?.let(this::add)
        }.joinTo(this, separator = " -> ")
        append(")")
    }

    protected open fun toStringAdditionalSuffix(): String? = null

    private fun toStringForTarget(): String = when (val fir = target) {
        is CfirConstructor -> "constructor"
        is CfirClassLikeDeclaration -> fir.symbol.name.asString()
        is CfirCallableDeclaration -> fir.symbol.name.asString()
        is CfirFile -> fir.name
        else -> "???"
    }
}
