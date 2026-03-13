

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.declarations.impl.CfirFileImpl
import org.cangjie.cfir.symbols.CfirSymbol

@CfirBuilderDsl
class CfirFileBuilder {
    lateinit var symbol: CfirSymbol<*>
    lateinit var origin: CfirDeclarationOrigin
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    lateinit var attributes: CfirDeclarationAttributes
    lateinit var name: String
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
    copyBuilder.packageDirective = original.packageDirective
    copyBuilder.imports.addAll(original.imports)
    copyBuilder.declarations.addAll(original.declarations)
    return copyBuilder.apply(init).build()
}
