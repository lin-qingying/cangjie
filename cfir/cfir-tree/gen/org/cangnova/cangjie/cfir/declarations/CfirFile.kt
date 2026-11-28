

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFeaturesDirective
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationFailure
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping

/**
 * Generated from: [org.cangnova.cangjie.cfir.tree.generator.CfirTree.file]
 */
abstract class CfirFile : CfirDeclaration(), CfirControlFlowGraphOwner {
    abstract override val source: CjSourceElement?
    abstract override val moduleData: CfirModuleData
    abstract override val contentSource: CfirContentSource
    abstract override val annotations: List<CfirAnnotation>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val attributes: CfirDeclarationAttributes
    abstract override val controlFlowGraphReference: CfirControlFlowGraphReference?
    abstract override val symbol: CfirFileSymbol
    abstract val name: String
    abstract val sourceFile: CjSourceFile?
    abstract val featuresDirective: CfirFeaturesDirective?
    abstract val packageDirective: CfirPackageDirective
    abstract val imports: List<CfirImport>
    abstract val sourceFileLinesMapping: CjSourceFileLinesMapping?
    abstract val conditionalCompilationFailures: List<CfirConditionalCompilationFailure>
    abstract val declarations: List<CfirDeclaration>

    /** 访问本节点拥有的完整来源子图；独立于 ordinary visitor 的语法子树。 */
    @CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        controlFlowGraphReference?.accept(visitor, data)
        featuresDirective?.accept(visitor, data)
        packageDirective.accept(visitor, data)
        imports.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformFile(this, data) as E

    @CfirImplementationDetail
    abstract override fun replaceSource(newSource: CjSourceElement?)

    abstract override fun replaceAnnotations(newAnnotations: List<CfirAnnotation>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: CfirControlFlowGraphReference?)

    abstract fun replaceSourceFile(newSourceFile: CjSourceFile?)

    abstract fun replaceFeaturesDirective(newFeaturesDirective: CfirFeaturesDirective?)

    abstract fun replaceImports(newImports: List<CfirImport>)

    abstract fun replaceSourceFileLinesMapping(newSourceFileLinesMapping: CjSourceFileLinesMapping?)

    abstract fun replaceConditionalCompilationFailures(newConditionalCompilationFailures: List<CfirConditionalCompilationFailure>)

    abstract override fun <D> transformAnnotations(transformer: CfirTransformer<D>, data: D): CfirFile

    abstract fun <D> transformFeaturesDirective(transformer: CfirTransformer<D>, data: D): CfirFile

    abstract fun <D> transformPackageDirective(transformer: CfirTransformer<D>, data: D): CfirFile

    abstract fun <D> transformImports(transformer: CfirTransformer<D>, data: D): CfirFile

    abstract fun <D> transformDeclarations(transformer: CfirTransformer<D>, data: D): CfirFile
}
