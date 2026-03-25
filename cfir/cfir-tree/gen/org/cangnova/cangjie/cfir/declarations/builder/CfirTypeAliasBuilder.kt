

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeAliasImpl
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirTypeAliasBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    val declarations: MutableList<CfirDeclaration> = mutableListOf()
    val superTypeRefs: MutableList<CfirTypeRef> = mutableListOf()
    lateinit var symbol: CfirTypeAliasSymbol
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var name: Name
    lateinit var expandedTypeRef: CfirTypeRef

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirTypeAlias {
        return CfirTypeAliasImpl(
            source,
            moduleData,
            annotations,
            origin,
            attributes,
            declarations,
            superTypeRefs,
            symbol,
            status,
            typeParameters,
            name,
            expandedTypeRef,
        ).also {
            it.initDefaultResolveState()
        }
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAlias(init: CfirTypeAliasBuilder.() -> Unit): CfirTypeAlias {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirTypeAliasBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypeAliasCopy(original: CfirTypeAlias, init: CfirTypeAliasBuilder.() -> Unit): CfirTypeAlias {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirTypeAliasBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes
    copyBuilder.declarations.addAll(original.declarations)
    copyBuilder.superTypeRefs.addAll(original.superTypeRefs)
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.name = original.name
    copyBuilder.expandedTypeRef = original.expandedTypeRef
    return copyBuilder.apply(init).build()
}
