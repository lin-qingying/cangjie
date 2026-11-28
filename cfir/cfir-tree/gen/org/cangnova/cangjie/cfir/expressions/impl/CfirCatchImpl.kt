

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirCatch
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirCatchImpl @CfirImplementationDetail constructor(
    override var source: CjSourceElement?,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override var coneTypeOrNull: ConeCangJieType?,
    override var mappedExpression: CfirExpression?,
    override var overflowStrategy: CangjieOverflowStrategy?,
    pattern: CfirPattern,
    override var body: CfirBlock,
) : CfirCatch() {
    override var pattern: CfirPattern = pattern
        private set(value) {
            if (org.cangnova.cangjie.cfir.patterns.isPatternTreeRootRestoreInProgress()) {
                field = value
            } else {
                val previousValue = field
                val previousTree = org.cangnova.cangjie.cfir.patterns.capturePatternTree(previousValue)
                org.cangnova.cangjie.cfir.patterns.validatePatternTreeReplacement(this, value)
                field = value
                try {
                    org.cangnova.cangjie.cfir.patterns.reconcilePatternTree(this, previousTree, patternTreeRoot())
                } catch (failure: Throwable) {
                    field = previousValue
                    throw failure
                }
            }
        }

    override fun patternTreeRoot(): org.cangnova.cangjie.cfir.CfirElement = pattern
    override fun validatePatternTreeRoot(newPatternTreeRoot: org.cangnova.cangjie.cfir.CfirElement) {
        require(newPatternTreeRoot is CfirPattern) { "Pattern tree root has an incompatible CFIR node type." }
    }
    override fun replacePatternTreeRoot(newPatternTreeRoot: org.cangnova.cangjie.cfir.CfirElement) = replacePattern(newPatternTreeRoot as CfirPattern)

    init {
        org.cangnova.cangjie.cfir.patterns.bindPatternTree(this, patternTreeRoot())
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        pattern.accept(visitor, data)
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirCatchImpl {
        transformAnnotations(transformer, data)
        transformPattern(transformer, data)
        transformBody(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirCatchImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformPattern(transformer: CfirTransformer<D>, data: D): CfirCatchImpl {
        pattern = pattern.transform(transformer, data)
        return this
    }

    override fun <D> transformBody(transformer: CfirTransformer<D>, data: D): CfirCatchImpl {
        body = body.transform(transformer, data)
        return this
    }

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        source = newSource
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeCangJieType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun replaceMappedExpression(newMappedExpression: CfirExpression?) {
        mappedExpression = newMappedExpression
    }

    override fun replaceOverflowStrategy(newOverflowStrategy: CangjieOverflowStrategy?) {
        overflowStrategy = newOverflowStrategy
    }

    override fun replacePattern(newPattern: CfirPattern) {
        pattern = newPattern
    }
}
