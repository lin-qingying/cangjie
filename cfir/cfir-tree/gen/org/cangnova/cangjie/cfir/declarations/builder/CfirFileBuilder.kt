

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirFileImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirSymbol

@CfirBuilderDsl
class CfirFileBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var name: String
    var sourceFile: CjSourceFile? = null
    lateinit var packageDirective: CfirPackageDirective
    val imports: MutableList<CfirImport> = mutableListOf()
    val declarations: MutableList<CfirDeclaration> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirFile {
        return CfirFileImpl(
            source,
            moduleData,
            annotations,
            symbol,
            origin,
            attributes,
            name,
            sourceFile,
            packageDirective,
            imports,
            declarations,
        ).also {
            it.initDefaultResolveState()
        }
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFile(init: CfirFileBuilder.() -> Unit): CfirFile {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirFileBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFileCopy(original: CfirFile, init: CfirFileBuilder.() -> Unit): CfirFile {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirFileBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes
    copyBuilder.name = original.name
    copyBuilder.sourceFile = original.sourceFile
    copyBuilder.packageDirective = original.packageDirective
    copyBuilder.imports.addAll(original.imports)
    copyBuilder.declarations.addAll(original.declarations)
    return copyBuilder.apply(init).build()
}
