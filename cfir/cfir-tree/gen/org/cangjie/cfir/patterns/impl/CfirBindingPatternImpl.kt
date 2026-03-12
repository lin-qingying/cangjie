/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.patterns.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.patterns.CfirBindingPattern
import org.cangjie.cfir.patterns.CfirPattern
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

class CfirBindingPatternImpl @CfirImplementationDetail constructor(
    override val name: Name,
    override val typeRef: CfirTypeRef?,
    override val nestedPattern: CfirPattern?,
) : CfirBindingPattern() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        typeRef?.accept(visitor, data)
        nestedPattern?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBindingPatternImpl {
        typeRef?.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        nestedPattern?.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        return this
    }
}
