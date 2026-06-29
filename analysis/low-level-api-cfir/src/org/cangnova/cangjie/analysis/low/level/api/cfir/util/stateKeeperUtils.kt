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

/**
 * 在状态恢复时把非 lazy block 替换为新的 lazy block。
 */
internal fun blockGuard(fir: CfirBlock): CfirBlock {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyBlock()
}

/**
 * 在状态恢复时把非 lazy 表达式替换为新的 lazy 表达式。
 */
internal fun expressionGuard(fir: CfirExpression): CfirExpression {
    if (isLazyStatement(fir)) {
        return fir
    }

    return buildLazyExpression()
}

/**
 * 判断 [fir] 是否已经是 lazy 表达式或 lazy block。
 */
private fun isLazyStatement(fir: CfirStatement): Boolean {
    return fir is CfirLazyExpression || fir is CfirLazyBlock
}

/**
 * 拥有特殊 body 的 fake source kind 集合。
 */
private val SPECIAL_BODY_CALLABLE_SOURCE_KINDS = setOf(
    CjFakeSourceElementKind.DefaultAccessor,
    CjFakeSourceElementKind.ImplicitConstructor,
    CjFakeSourceElementKind.PropertyFromParameter,
    CjFakeSourceElementKind.DataClassGeneratedMembers,
    CjFakeSourceElementKind.EnumGeneratedDeclaration,
)

/**
 * 判断 callable 是否由特殊 fake source 生成，且其 body 不应被普通状态恢复逻辑替换。
 */
@OptIn(SuspiciousFakeSourceCheck::class)
internal fun isCallableWithSpecialBody(fir: CfirCallableDeclaration): Boolean {
    val source = fir.source as? CjFakePsiSourceElement ?: return false
    return source.kind in SPECIAL_BODY_CALLABLE_SOURCE_KINDS
}
