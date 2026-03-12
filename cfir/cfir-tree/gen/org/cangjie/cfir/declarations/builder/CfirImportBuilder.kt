/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.cangjie.cfir.declarations.builder

import kotlin.contracts.*
import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.builder.CfirBuilderDsl
import org.cangjie.cfir.declarations.CfirImport
import org.cangjie.cfir.declarations.impl.CfirImportImpl
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

@CfirBuilderDsl
class CfirImportBuilder {
    lateinit var importedFqName: FqName
    var isAllUnder: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var aliasName: Name? = null

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirImport {
        return CfirImportImpl(
            importedFqName,
            isAllUnder,
            aliasName,
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

@OptIn(ExperimentalContracts::class)
inline fun buildImportCopy(original: CfirImport, init: CfirImportBuilder.() -> Unit): CfirImport {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = CfirImportBuilder()
    copyBuilder.importedFqName = original.importedFqName
    copyBuilder.isAllUnder = original.isAllUnder
    copyBuilder.aliasName = original.aliasName
    return copyBuilder.apply(init).build()
}
