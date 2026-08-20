

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.impl.CfirResolvedDeclarationStatusImpl
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirResolvedDeclarationStatusBuilder {
    var source: CjSourceElement? = null
    lateinit var visibility: Visibility
    var isVisibilityExplicit: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isModalityExplicit: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isAbstractExplicit: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isOverride: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isOperator: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isStatic: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isConst: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isMut: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isUnsafe: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isForeign: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isCommon: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isSpecific: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isRedef: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isDefault: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isAbstract: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isOpen: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var isSealed: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var modality: Modality

    fun build(): CfirResolvedDeclarationStatus {
        return CfirResolvedDeclarationStatusImpl(
            source,
            visibility,
            isVisibilityExplicit,
            isModalityExplicit,
            isAbstractExplicit,
            isOverride,
            isOperator,
            isStatic,
            isConst,
            isMut,
            isUnsafe,
            isForeign,
            isCommon,
            isSpecific,
            isRedef,
            isDefault,
            isAbstract,
            isOpen,
            isSealed,
            modality,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedDeclarationStatus(init: CfirResolvedDeclarationStatusBuilder.() -> Unit): CfirResolvedDeclarationStatus {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedDeclarationStatusBuilder().apply(init).build()
}
