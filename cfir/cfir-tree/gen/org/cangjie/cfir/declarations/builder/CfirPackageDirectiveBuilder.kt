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
import org.cangjie.cfir.declarations.CfirPackageDirective
import org.cangjie.cfir.declarations.impl.CfirPackageDirectiveImpl
import org.cangnova.cangjie.name.FqName

@CfirBuilderDsl
class CfirPackageDirectiveBuilder {
    lateinit var packageFqName: FqName

    @OptIn(CfirImplementationDetail::class)
    fun build(): CfirPackageDirective {
        return CfirPackageDirectiveImpl(
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
    copyBuilder.packageFqName = original.packageFqName
    return copyBuilder.apply(init).build()
}
