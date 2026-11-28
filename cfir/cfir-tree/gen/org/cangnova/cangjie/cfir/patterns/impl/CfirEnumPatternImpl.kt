

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

class CfirEnumPatternImpl @CfirImplementationDetail constructor(
    override var source: CjSourceElement?,
    override var semanticTypeRef: CfirTypeRef?,
    override val constantValues: MutableList<CfirConstantValue>,
    override var contextExpression: CfirExpression?,
    override var constructor: CfirExpression,
    arguments: List<CfirPattern>,
) : CfirEnumPattern() {
    override var patternTreeOwner: CfirPatternTreeOwner? = null
        private set(value) {
            if (org.cangnova.cangjie.cfir.patterns.isPatternTreeRootRestoreInProgress()) {
                field = value
            } else {
                org.cangnova.cangjie.cfir.patterns.validatePatternTreeNodeOwnerChange(this, field, value)
                field = value
            }
        }
    override val constructorReference: CfirReference?
        get() = when (val expression = constructor) {
    is CfirResolvable -> expression.calleeReference
    is CfirCallExpression -> checkNotNull(expression.base as? CfirResolvable) {
        "Enum constructor call must retain its actual reference-bearing base"
    }.calleeReference
    is CfirErrorExpression -> null
    else -> error("Enum constructor must retain its actual reference-bearing expression")
}
    override var arguments: List<CfirPattern> = java.util.Collections.unmodifiableList(arguments.toList())
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
        semanticTypeRef?.accept(visitor, data)
        constructor.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        transformSemanticTypeRef(transformer, data)
        transformConstructor(transformer, data)
        transformArguments(transformer, data)
        return this
    }

    override fun <D> transformSemanticTypeRef(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        semanticTypeRef = semanticTypeRef?.transform(transformer, data)
        return this
    }

    override fun <D> transformConstructor(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        constructor = constructor.transform(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: CfirTransformer<D>, data: D): CfirEnumPatternImpl {
        val owner = patternTreeOwner
        val previousTree = owner?.let { org.cangnova.cangjie.cfir.patterns.capturePatternTree(it.patternTreeRoot()) }
        try {
            arguments = arguments.map { it.transform(transformer, data) }
        } finally {
            if (owner != null) {
                org.cangnova.cangjie.cfir.patterns.reconcilePatternTree(owner, previousTree!!, owner.patternTreeRoot())
            } else {
                org.cangnova.cangjie.cfir.patterns.validateDetachedPatternTree(this)
            }
        }
        return this
    }

    @CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        source = newSource
    }

    override fun replaceSemanticTypeRef(newSemanticTypeRef: CfirTypeRef?) {
        semanticTypeRef = newSemanticTypeRef
    }

    override fun replaceConstantValues(newConstantValues: List<CfirConstantValue>) {
        if (constantValues === newConstantValues) return
        constantValues.clear()
        constantValues.addAll(newConstantValues)
    }

    override fun replaceContextExpression(newContextExpression: CfirExpression?) {
        contextExpression = newContextExpression
    }

    override fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?) {
        patternTreeOwner = newPatternTreeOwner
    }

    override fun replaceConstructor(newConstructor: CfirExpression) {
        constructor = newConstructor
    }

    override fun replaceArguments(newArguments: List<CfirPattern>) {
        arguments = newArguments
    }
}
