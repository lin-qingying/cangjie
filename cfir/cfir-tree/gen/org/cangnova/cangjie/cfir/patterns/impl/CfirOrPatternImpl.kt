

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

internal class CfirOrPatternImpl(
    override val source: CjSourceElement?,
    alternatives: List<CfirPattern>,
) : CfirOrPattern() {
    override var patternTreeOwner: CfirPatternTreeOwner? = null
        private set(value) {
            if (org.cangnova.cangjie.cfir.patterns.isPatternTreeRootRestoreInProgress()) {
                field = value
            } else {
                org.cangnova.cangjie.cfir.patterns.validatePatternTreeNodeOwnerChange(this, field, value)
                field = value
            }
        }
    override var alternatives: List<CfirPattern> = java.util.Collections.unmodifiableList(alternatives.toList())
        private set(value) {
            val owner = patternTreeOwner
            val previousTree = owner?.let { org.cangnova.cangjie.cfir.patterns.capturePatternTree(it.patternTreeRoot()) }
            val previousValue = field
            field = java.util.Collections.unmodifiableList(value.toList())
            try {
                if (owner != null) {
                    org.cangnova.cangjie.cfir.patterns.reconcilePatternTree(owner, previousTree!!, owner.patternTreeRoot())
                } else {
                    org.cangnova.cangjie.cfir.patterns.validateDetachedPatternTree(this)
                }
            } catch (failure: Throwable) {
                field = previousValue
                throw failure
            }
        }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        alternatives.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirOrPatternImpl {
        transformAlternatives(transformer, data)
        return this
    }

    override fun <D> transformAlternatives(transformer: CfirTransformer<D>, data: D): CfirOrPatternImpl {
        val owner = patternTreeOwner
        val previousTree = owner?.let { org.cangnova.cangjie.cfir.patterns.capturePatternTree(it.patternTreeRoot()) }
        try {
            alternatives = alternatives.map { it.transform(transformer, data) }
        } finally {
            if (owner != null) {
                org.cangnova.cangjie.cfir.patterns.reconcilePatternTree(owner, previousTree!!, owner.patternTreeRoot())
            } else {
                org.cangnova.cangjie.cfir.patterns.validateDetachedPatternTree(this)
            }
        }
        return this
    }

    override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?) {
        patternTreeOwner = newPatternTreeOwner
    }

    override fun replaceAlternatives(newAlternatives: List<CfirPattern>) {
        alternatives = newAlternatives
    }
}
