package org.cangnova.cangjie.cfir.expressions.impl

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/** 对齐 Kotlin FirAnnotationArgumentMappingImpl；映射值不再作为语法子树重复遍历。 */
public class CfirAnnotationArgumentMappingImpl(
    override val source: CjSourceElement?,
    override val mapping: Map<Name, CfirExpression>,
) : CfirAnnotationArgumentMapping() {
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement = this
}

public object CfirEmptyAnnotationArgumentMapping : CfirAnnotationArgumentMapping() {
    override val source: CjSourceElement? get() = null
    override val mapping: Map<Name, CfirExpression> get() = emptyMap()
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement = this
}

/** 参数绑定只在 resolved argument list 中进行，此处仅按参数名投影。 */
public fun CfirResolvedArgumentList.toAnnotationArgumentMapping(): CfirAnnotationArgumentMapping =
    CfirAnnotationArgumentMappingImpl(source, mapping.entries.associate { (argument, parameter) ->
        parameter.name to ((argument as? CfirNamedArgumentExpression)?.expression ?: argument)
    })

/** SEMANTIC_RESOLVED 后，映射值为 annotation owner 保存的常量结果。 */
public val CfirAnnotation.resolvedArguments: Map<Name, CfirExpression>
    get() = argumentMapping.mapping
