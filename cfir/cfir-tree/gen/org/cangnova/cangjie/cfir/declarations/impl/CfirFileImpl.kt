

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

class CfirFileImpl @CfirImplementationDetail constructor(
    override val symbol: CfirSymbol<*>,
    override val origin: CfirDeclarationOrigin,
    override var annotations: List<CfirAnnotation>,
    override val moduleData: CfirModuleData,
    override var resolvePhase: CfirResolvePhase,
    override val attributes: CfirDeclarationAttributes,
    override val name: String,
    override var packageDirective: CfirPackageDirective,
    override var imports: List<CfirImport>,
    override var declarations: List<CfirDeclaration>,
) : CfirFile() {
    override val source: CjSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        packageDirective.accept(visitor, data)
        imports.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceResolvePhase(newResolvePhase: CfirResolvePhase)
     {
        this.resolvePhase = newResolvePhase
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirFile
     {
        this.annotations = annotations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirAnnotation }
        return this
    }

    override fun <D> transformPackageDirective(transformer: CfirTransformer<D>, data: D): CfirFile
     {
        this.packageDirective = packageDirective.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirPackageDirective
        return this
    }

    override fun <D> transformImports(transformer: CfirTransformer<D>, data: D): CfirFile
     {
        this.imports = imports.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirImport }
        return this
    }

    override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirFile
     {
        this.declarations = declarations.map { it.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data) as CfirDeclaration }
        return this
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFileImpl {
        transformAnnotations(transformer, data)
        transformPackageDirective(transformer, data)
        transformImports(transformer, data)
        transformDeclarations(transformer, data)
        return this
    }
}
