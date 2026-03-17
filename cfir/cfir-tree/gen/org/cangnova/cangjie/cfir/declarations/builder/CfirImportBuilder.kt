

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

@file:Suppress("DuplicatedCode", "unused")

package org.cangnova.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.builder.CfirBuilderDsl
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.declarations.impl.CfirImportImpl
import org.cangnova.cangjie.cfir.source.CjSourceElement
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirImportBuilder {
    var source: CjSourceElement? = null
    var importedFqName: FqName? = null
    var isAllUnder: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var aliasName: Name? = null
    var aliasSource: CjSourceElement? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirImport {
        return CfirImportImpl(
            source,
            importedFqName,
            isAllUnder,
            aliasName,
            aliasSource,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildImport(init: CfirImportBuilder.() -> Unit): CfirImport {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return CfirImportBuilder().apply(init).build()
}
