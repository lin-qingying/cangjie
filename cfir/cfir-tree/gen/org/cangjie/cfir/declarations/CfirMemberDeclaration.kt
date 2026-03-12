/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.cangjie.cfir.declarations

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * Generated from: [org.cangjie.cfir.tree.generator.CfirTree.memberDeclaration]
 */
sealed class CfirMemberDeclaration : CfirDeclaration() {
    abstract override val source: CfirSourceElement?
    abstract override val symbol: CfirSymbol<*>
    abstract override val origin: CfirDeclarationOrigin
    abstract override val annotations: List<CfirAnnotation>
    abstract override val moduleData: CfirModuleData
    abstract override val resolvePhase: CfirResolvePhase
    abstract override val attributes: CfirDeclarationAttributes

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitMemberDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : CfirElement, D> transform(transformer: CfirTransformer<D>, data: D): E =
        transformer.transformMemberDeclaration(this, data) as E
}
