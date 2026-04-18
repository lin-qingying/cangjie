/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.source.CjFakePsiSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.SuspiciousFakeSourceCheck

internal fun blockGuard(fir: CfirBlock): CfirBlock {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyBlock()
}

internal fun expressionGuard(fir: CfirExpression): CfirExpression {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyExpression {
        source = fir.source
    }
}

private fun isLazyStatement(fir: CfirStatement): Boolean {
    return fir is CfirLazyExpression || fir is CfirLazyBlock
}

private val SPECIAL_BODY_CALLABLE_SOURCE_KINDS = setOf(
    CjFakeSourceElementKind.DefaultAccessor,
    CjFakeSourceElementKind.ImplicitConstructor,
    CjFakeSourceElementKind.PropertyFromParameter,
    CjFakeSourceElementKind.DataClassGeneratedMembers,
    CjFakeSourceElementKind.EnumGeneratedDeclaration,
)

@OptIn(SuspiciousFakeSourceCheck::class)
internal fun isCallableWithSpecialBody(fir: CfirCallableDeclaration): Boolean {
    val source = fir.source as? CjFakePsiSourceElement ?: return false
    return source.kind in SPECIAL_BODY_CALLABLE_SOURCE_KINDS
}
