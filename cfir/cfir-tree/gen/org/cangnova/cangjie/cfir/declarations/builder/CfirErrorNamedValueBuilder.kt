

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirErrorNamedValueImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.symbols.CfirErrorNamedValueSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirErrorNamedValueBuilder {
    var source: CjSourceElement? = null
    lateinit var moduleData: CfirModuleData
    lateinit var resolvePhase: CfirResolvePhase
    val annotations: MutableList<CfirAnnotation> = mutableListOf()
    lateinit var origin: CfirDeclarationOrigin
    lateinit var attributes: CfirDeclarationAttributes
    val typeParameters: MutableList<CfirTypeParameterRef> = mutableListOf()
    lateinit var status: CfirDeclarationStatus
    var dispatchReceiverType: ConeSimpleCangJieType? = null
    lateinit var diagnostic: ConeDiagnostic
    lateinit var name: Name
    lateinit var symbol: CfirErrorNamedValueSymbol

    fun build(): CfirErrorNamedValue {
        return CfirErrorNamedValueImpl(
            source,
            moduleData,
            resolvePhase,
            annotations.toMutableOrEmpty(),
            origin,
            attributes,
            typeParameters,
            status,
            dispatchReceiverType,
            diagnostic,
            name,
            symbol,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildErrorNamedValue(init: CfirErrorNamedValueBuilder.() -> Unit): CfirErrorNamedValue {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirErrorNamedValueBuilder().apply(init).build()
}
