/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.patterns.impl

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirWildcardPatternImpl : CfirWildcardPattern() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirWildcardPatternImpl {
        return this
    }
}
