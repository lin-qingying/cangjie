

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode")

package org.cangnova.cangjie.cfir.declarations.impl

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.transformInplace
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping

@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirFileImpl @CfirImplementationDetail constructor(
    override val source: CjSourceElement?,
    override val moduleData: CfirModuleData,
    resolvePhase: CfirResolvePhase,
    override var annotations: MutableOrEmptyList<CfirAnnotation>,
    override val origin: CfirDeclarationOrigin,
    override val attributes: CfirDeclarationAttributes,
    override val symbol: CfirFileSymbol,
    override val name: String,
    override val sourceFile: CjSourceFile?,
    override var packageDirective: CfirPackageDirective,
    override val imports: MutableList<CfirImport>,
    override val sourceFileLinesMapping: CjSourceFileLinesMapping?,
    override val declarations: MutableList<CfirDeclaration>,
) : CfirFile() {
    override var controlFlowGraphReference: CfirControlFlowGraphReference? = null

    init {
        symbol.bind(this)
        resolveState = resolvePhase.asResolveState()
        @Suppress("SENSELESS_COMPARISON")
        require(source != null || origin != CfirDeclarationOrigin.Source) { "${this::class.simpleName} with Source origin was instantiated without a source element." }
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        packageDirective.accept(visitor, data)
        imports.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFileImpl {
        transformAnnotations(transformer, data)
        controlFlowGraphReference = controlFlowGraphReference?.transform(transformer, data)
        transformPackageDirective(transformer, data)
        transformImports(transformer, data)
        transformDeclarations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirFileImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformPackageDirective(transformer: CfirTransformer<D>, data: D): CfirFileImpl {
        packageDirective = packageDirective.transform(transformer, data)
        return this
    }

    override fun <D> transformImports(transformer: CfirTransformer<D>, data: D): CfirFileImpl {
        imports.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirFileImpl {
        declarations.transformInplace(transformer, data)
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?) {
        controlFlowGraphReference = newControlFlowGraphReference
    }
}
