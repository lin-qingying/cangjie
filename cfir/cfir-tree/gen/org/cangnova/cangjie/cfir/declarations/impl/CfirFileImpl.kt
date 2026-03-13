

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.declarations.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirFileImpl @CfirImplementationDetail constructor(
    override val symbol: CfirSymbol<*>,
    override val origin: CfirDeclarationOrigin,
    override val annotations: List<CfirAnnotation>,
    override val moduleData: CfirModuleData,
    override val resolvePhase: CfirResolvePhase,
    override val attributes: CfirDeclarationAttributes,
    override val name: String,
    override val packageDirective: CfirPackageDirective,
    override val imports: List<CfirImport>,
    override val declarations: List<CfirDeclaration>,
) : CfirFile() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        packageDirective.accept(visitor, data)
        imports.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFileImpl {
        annotations.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        packageDirective.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        imports.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        declarations.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        return this
    }
}
