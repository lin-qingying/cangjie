/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
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
    val cfirFile: CfirFile? get() = designation.fileOrNull

    /**
     * The list of [CfirFile] and [CfirClassLikeDeclaration] which are
     * the required to go from file to target declarations in the top-down order.
     *
     * If resolve target is [CfirFile] or [CfirClassLikeDeclaration] itself, it's not included into the [path].
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

    /**
     * 从 designation 的路径起点开始递归进入目标声明上下文。
     */
    private fun goToTarget(visitor: LLCfirResolveTargetVisitor) {
        val pathIterator = path.iterator()
        goToTarget(pathIterator, visitor)
    }

    /**
     * 逐段进入 file、class-like 或 extend 上下文，直到最终 target 元素。
     */
    private fun goToTarget(
        pathIterator: Iterator<CfirDeclaration>,
        visitor: LLCfirResolveTargetVisitor,
    ) {
        if (pathIterator.hasNext()) {
            when (val declaration = pathIterator.next()) {
                is CfirClassLikeDeclaration -> visitor.withClassLike(declaration) { goToTarget(pathIterator, visitor) }
                is CfirExtend -> visitor.withExtend(declaration) { goToTarget(pathIterator, visitor) }
                is CfirFile -> visitor.withFile(declaration) { goToTarget(pathIterator, visitor) }
                else -> errorWithCfirSpecificEntries(
                    "Unexpected declaration in path: ${declaration::class.simpleName}",
                    cfir = declaration,
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

    /**
     * 输出 target 的路径和附加后缀，用于 lazy resolve 日志与断言信息。
     */
    override fun toString(): String = buildString {
        append(this@LLCfirResolveTarget::class.simpleName)
        append("(")
        buildList {
            path.mapTo(this) {
                when (it) {
                    is CfirFile -> it.name
                    is CfirClassLikeDeclaration -> it.name
                    is CfirExtend -> it.psi?.text ?: it::class.simpleName ?: "<extend>"
                    else -> errorWithCfirSpecificEntries("Unsupported path declaration: ${it::class.simpleName}", cfir = it)
                }
            }

            add(toStringForTarget())
            toStringAdditionalSuffix()?.let(this::add)
        }.joinTo(this, separator = " -> ")
        append(")")
    }

    /**
     * 子类可提供的字符串附加信息，例如全量解析标记或指定成员列表。
     */
    protected open fun toStringAdditionalSuffix(): String? = null

    /**
     * 将最终目标元素渲染为稳定的简短名称。
     */
    private fun toStringForTarget(): String = when (val cfir = target) {
        is CfirConstructor -> "constructor"
        is CfirClassLikeDeclaration -> cfir.symbol.name.asString()
        is CfirCallableDeclaration -> cfir.symbol.name.asString()
        is CfirFile -> cfir.name
        else -> "???"
    }
}
