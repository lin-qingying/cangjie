

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.patterns.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirCatchPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement

class CfirCatchPatternImpl @CfirImplementationDetail constructor(
    override var source: CjSourceElement?,
    override var semanticTypeRef: CfirTypeRef?,
    override val constantValues: MutableList<CfirConstantValue>,
    override var contextExpression: CfirExpression?,
    override var nestedPattern: CfirPattern,
    override val typeRefs: MutableList<CfirTypeRef>,
) : CfirCatchPattern() {

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        semanticTypeRef?.accept(visitor, data)
        nestedPattern.accept(visitor, data)
        typeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirCatchPatternImpl {
        transformSemanticTypeRef(transformer, data)
        transformNestedPattern(transformer, data)
        transformTypeRefs(transformer, data)
        return this
    }

    override fun <D> transformSemanticTypeRef(transformer: CfirTransformer<D>, data: D): CfirCatchPatternImpl {
        semanticTypeRef = semanticTypeRef?.transform(transformer, data)
        return this
    }

    override fun <D> transformNestedPattern(transformer: CfirTransformer<D>, data: D): CfirCatchPatternImpl {
        nestedPattern = nestedPattern.transform(transformer, data)
        return this
    }

    override fun <D> transformTypeRefs(transformer: CfirTransformer<D>, data: D): CfirCatchPatternImpl {
        typeRefs.transformInplace(transformer, data)
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

    override fun replaceNestedPattern(newNestedPattern: CfirPattern) {
        nestedPattern = newNestedPattern
    }

    override fun replaceTypeRefs(newTypeRefs: List<CfirTypeRef>) {
        if (typeRefs === newTypeRefs) return
        typeRefs.clear()
        typeRefs.addAll(newTypeRefs)
    }
}
