

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

@OptIn(CfirImplementationDetail::class)
class CfirFileImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    override var annotations: List<CfirAnnotation>,
    override val symbol: CfirSymbol<*>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val name: String,
    override val sourceFile: CjSourceFile?,
    override var packageDirective: CfirPackageDirective,
    override var imports: List<CfirImport>,
    override var declarations: List<CfirDeclaration>,
) : CfirFile() {
    override var controlFlowGraphReference: CfirControlFlowGraphReference? = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        packageDirective.accept(visitor, data)
        imports.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)
     {
        this.annotations = newAnnotations
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?)
     {
        this.controlFlowGraphReference = newControlFlowGraphReference
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
        controlFlowGraphReference?.transform<org.cangnova.cangjie.cfir.CfirElement, D>(transformer, data)
        transformPackageDirective(transformer, data)
        transformImports(transformer, data)
        transformDeclarations(transformer, data)
        return this
    }
}
