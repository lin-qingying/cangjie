

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.declarations.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

class CfirVariableImpl @CfirImplementationDetail constructor(
    override val symbol: CfirSymbol<*>,
    override val origin: CfirDeclarationOrigin,
    override val annotations: List<CfirAnnotation>,
    override val moduleData: CfirModuleData,
    override val resolvePhase: CfirResolvePhase,
    override val attributes: CfirDeclarationAttributes,
    override val status: CfirDeclarationStatus,
    override val typeParameters: List<CfirTypeParameter>,
    override val returnTypeRef: CfirTypeRef,
    override val name: Name,
    override val initializer: CfirExpression?,
    override val isVar: Boolean,
) : CfirVariable() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        initializer?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirVariableImpl {
        annotations.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        typeParameters.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        returnTypeRef.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        initializer?.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
