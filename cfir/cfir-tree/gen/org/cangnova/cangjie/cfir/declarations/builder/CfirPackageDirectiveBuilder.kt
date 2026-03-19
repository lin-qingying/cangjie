

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirPackageDirective
import org.cangnova.cangjie.cfir.declarations.impl.CfirPackageDirectiveImpl
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.source.CjSourceElement

@CfirBuilderDsl
class CfirPackageDirectiveBuilder {
    var source: CjSourceElement? = null
    lateinit var packageFqName: FqName

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirPackageDirective {
        return CfirPackageDirectiveImpl(
            source,
            packageFqName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildPackageDirective(init: CfirPackageDirectiveBuilder.() -> Unit): CfirPackageDirective {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirPackageDirectiveBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildPackageDirectiveCopy(original: CfirPackageDirective, init: CfirPackageDirectiveBuilder.() -> Unit): CfirPackageDirective {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirPackageDirectiveBuilder()
    copyBuilder.source = original.source
    copyBuilder.packageFqName = original.packageFqName
    return copyBuilder.apply(init).build()
}
