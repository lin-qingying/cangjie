

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.declarations.CfirResolvedImport
import org.cangnova.cangjie.cfir.declarations.impl.CfirResolvedImportImpl
import org.cangnova.cangjie.name.FqName

@CfirBuilderDsl
class CfirResolvedImportBuilder {
    lateinit var delegate: CfirImport
    lateinit var packageFqName: FqName

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirResolvedImport {
        return CfirResolvedImportImpl(
            delegate,
            packageFqName,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedImport(init: CfirResolvedImportBuilder.() -> Unit): CfirResolvedImport {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirResolvedImportBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedImportCopy(original: CfirResolvedImport, init: CfirResolvedImportBuilder.() -> Unit): CfirResolvedImport {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirResolvedImportBuilder()
    copyBuilder.delegate = original.delegate
    copyBuilder.packageFqName = original.packageFqName
    return copyBuilder.apply(init).build()
}
