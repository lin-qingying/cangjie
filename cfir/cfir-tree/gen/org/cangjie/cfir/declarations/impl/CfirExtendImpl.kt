/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See cfir/cfir-tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.cangjie.cfir.declarations.impl

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

class CfirExtendImpl @CfirImplementationDetail constructor(
    override val symbol: CfirSymbol<*>,
    override val origin: CfirDeclarationOrigin,
    override val annotations: List<CfirAnnotation>,
    override val moduleData: CfirModuleData,
    override val resolvePhase: CfirResolvePhase,
    override val attributes: CfirDeclarationAttributes,
    override val status: CfirDeclarationStatus,
    override val typeParameters: List<CfirTypeParameter>,
    override val extendedTypeRef: CfirTypeRef,
    override val superTypeRefs: List<CfirTypeRef>,
    override val declarations: List<CfirDeclaration>,
) : CfirExtend() {
    override val source: CfirSourceElement?
        get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        extendedTypeRef.accept(visitor, data)
        superTypeRefs.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirExtendImpl {
        annotations.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        typeParameters.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        extendedTypeRef.transform<org.cangjie.cfir.CfirElement, D>(transformer, data)
        superTypeRefs.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        declarations.forEach { it.transform<org.cangjie.cfir.CfirElement, D>(transformer, data) }
        return this
    }
}
