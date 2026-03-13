

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirFileImpl
import org.cangnova.cangjie.cfir.symbols.CfirSymbol

@CfirBuilderDsl
class CfirFileBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var name: String
    var sourceFile: CjSourceFile? = null
    lateinit var packageDirective: CfirPackageDirective
    val imports: MutableList<CfirImport> = mutableListOf()
    val declarations: MutableList<CfirDeclaration> = mutableListOf()

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirFile {
        return CfirFileImpl(
            symbol,
            origin,
            annotations,
            moduleData,
            resolvePhase,
            attributes,
            name,
            sourceFile,
            packageDirective,
            imports,
            declarations,
        )
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
    copyBuilder.symbol = original.symbol
    copyBuilder.origin = original.origin
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.moduleData = original.moduleData
    copyBuilder.resolvePhase = original.resolvePhase
    copyBuilder.attributes = original.attributes
    copyBuilder.name = original.name
    copyBuilder.sourceFile = original.sourceFile
    copyBuilder.packageDirective = original.packageDirective
    copyBuilder.imports.addAll(original.imports)
    copyBuilder.declarations.addAll(original.declarations)
    return copyBuilder.apply(init).build()
}
