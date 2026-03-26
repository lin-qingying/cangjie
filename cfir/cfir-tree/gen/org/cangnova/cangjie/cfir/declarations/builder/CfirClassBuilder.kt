

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirClassBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    var isLocal: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var status: CfirDeclarationStatus
    val typeParameters: MutableList<CfirTypeParameter> = mutableListOf()
    lateinit var symbol: CfirClassSymbol
    val superTypeRefs: MutableList<CfirTypeRef> = mutableListOf()
    val declarations: MutableList<CfirDeclaration> = mutableListOf()
    lateinit var name: Name

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirClass {
        return CfirClassImpl(
            source,
            moduleData,
            annotations,
            origin,
            attributes,
            isLocal,
            status,
            typeParameters,
            symbol,
            superTypeRefs,
            declarations,
            name,
        ).also {
            it.initDefaultResolveState()
        }
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildClass(init: CfirClassBuilder.() -> Unit): CfirClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirClassBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildClassCopy(original: CfirClass, init: CfirClassBuilder.() -> Unit): CfirClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirClassBuilder()
    copyBuilder.source = original.source
    copyBuilder.moduleData = original.moduleData
    copyBuilder.annotations.addAll(original.annotations)
    copyBuilder.origin = original.origin
    copyBuilder.attributes = original.attributes
    copyBuilder.isLocal = original.isLocal
    copyBuilder.status = original.status
    copyBuilder.typeParameters.addAll(original.typeParameters)
    copyBuilder.superTypeRefs.addAll(original.superTypeRefs)
    copyBuilder.declarations.addAll(original.declarations)
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
