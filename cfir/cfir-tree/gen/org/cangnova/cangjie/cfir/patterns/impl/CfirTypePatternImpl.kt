

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

class CfirTypePatternImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override var matchingKind: CfirTypePatternMatchingKind,
    override var typeRef: CfirTypeRef,
    override val bindingName: Name?,
    bindingVariable: CfirPatternBindingVariable?,
) : CfirTypePattern() {
    override var patternTreeOwner: CfirPatternTreeOwner? = null
        private set(value) {
            if (org.cangnova.cangjie.cfir.patterns.isPatternTreeRootRestoreInProgress()) {
                field = value
            } else {
                org.cangnova.cangjie.cfir.patterns.validatePatternTreeNodeOwnerChange(this, field, value)
                field = value
            }
        }
    override var bindingVariable: CfirPatternBindingVariable? = bindingVariable
        private set(value) {
            val owner = patternTreeOwner
            val previousTree = owner?.let { org.cangnova.cangjie.cfir.patterns.capturePatternTree(it.patternTreeRoot()) }
            val previousValue = field
            field = value
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
        typeRef.accept(visitor, data)
        bindingVariable?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypePatternImpl {
        transformTypeRef(transformer, data)
        transformBindingVariable(transformer, data)
        return this
    }

    override fun <D> transformTypeRef(transformer: CfirTransformer<D>, data: D): CfirTypePatternImpl {
        typeRef = typeRef.transform(transformer, data)
        return this
    }

    override fun <D> transformBindingVariable(transformer: CfirTransformer<D>, data: D): CfirTypePatternImpl {
        val owner = patternTreeOwner
        val previousTree = owner?.let { org.cangnova.cangjie.cfir.patterns.capturePatternTree(it.patternTreeRoot()) }
        try {
            bindingVariable = bindingVariable?.transform(transformer, data)
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

    override fun replaceMatchingKind(newMatchingKind: CfirTypePatternMatchingKind) {
        matchingKind = newMatchingKind
    }

    override fun replaceTypeRef(newTypeRef: CfirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceBindingVariable(newBindingVariable: CfirPatternBindingVariable?) {
        bindingVariable = newBindingVariable
    }
}
