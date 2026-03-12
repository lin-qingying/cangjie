/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.types.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.CfirTupleTypeRef
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirTupleTypeRefImpl @CfirImplementationDetail constructor(
    override val elementTypeRefs: List<CfirTypeRef>,
) : CfirTupleTypeRef() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        elementTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTupleTypeRefImpl {
        elementTypeRefs.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        return this
    }
}
