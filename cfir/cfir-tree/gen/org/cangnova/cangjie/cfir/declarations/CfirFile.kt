

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.file]
 */
abstract class CfirFile : CfirDeclaration(), CfirControlFlowGraphOwner {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val annotations: List<CfirAnnotation>
    abstract override val symbol: CfirSymbol<*>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val controlFlowGraphReference: CfirControlFlowGraphReference?
    abstract val name: String
    abstract val sourceFile: CjSourceFile?
    abstract val packageDirective: CfirPackageDirective
    abstract val imports: List<CfirImport>
    abstract val declarations: List<CfirDeclaration>

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformFile(this, data) as E

    override abstract fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)


    override abstract fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?)


    override abstract fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirFile


    abstract fun <D> transformPackageDirective(transformer: CfirTransformer<D>, data: D): CfirFile


    abstract fun <D> transformImports(transformer: CfirTransformer<D>, data: D): CfirFile


    abstract fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirFile

}
