/*
 * Copyright 2026 LinQingYing. and contributors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

/** 解析转换器只将类型引用、跳转和错误节点等指定类别委托到其语义父节点。 */
@RequiresOptIn("CfirAbstractResolveTransformer 只对指定的类型引用、跳转和错误节点执行父节点分派。")
annotation class CfirAbstractResolveTransformerSubclassing

/**
 * 解析阶段的转换器基础抽象，对齐 Kotlin FirAbstractResolveTransformer。
 *
 * 覆写 transformTypeRef 同时处理其具体形态；覆写 transformResolvedTypeRef 同时处理错误类型引用。
 * 仓颉的 Option、tuple 和 VArray 类型引用沿相同类型入口分派，不引入 Kotlin 专有类型节点。
 */
@SubclassOptInRequired(CfirAbstractResolveTransformerSubclassing::class)
abstract class CfirAbstractResolveTransformer<D> : CfirTransformer<D>() {
    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: D): CfirTypeRef =
        transformTypeRef(implicitTypeRef, data)

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: D): CfirTypeRef =
        transformTypeRef(resolvedTypeRef, data)

    override fun transformErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: D): CfirTypeRef =
        transformResolvedTypeRef(errorTypeRef, data)

    override fun transformBasicTypeRef(basicTypeRef: CfirBasicTypeRef, data: D): CfirTypeRef =
        transformTypeRef(basicTypeRef, data)

    override fun transformUnresolvedTypeRef(unresolvedTypeRef: CfirUnresolvedTypeRef, data: D): CfirTypeRef =
        transformTypeRef(unresolvedTypeRef, data)

    override fun transformUserTypeRef(userTypeRef: CfirUserTypeRef, data: D): CfirTypeRef =
        transformUnresolvedTypeRef(userTypeRef, data)

    override fun transformFunctionTypeRef(functionTypeRef: CfirFunctionTypeRef, data: D): CfirTypeRef =
        transformTypeRef(functionTypeRef, data)

    override fun transformOptionTypeRef(optionTypeRef: CfirOptionTypeRef, data: D): CfirTypeRef =
        transformUnresolvedTypeRef(optionTypeRef, data)

    override fun transformTupleTypeRef(tupleTypeRef: CfirTupleTypeRef, data: D): CfirTypeRef =
        transformTypeRef(tupleTypeRef, data)

    override fun transformVArrayTypeRef(vArrayTypeRef: CfirVArrayTypeRef, data: D): CfirTypeRef =
        transformTypeRef(vArrayTypeRef, data)

    override fun transformReturnExpression(returnExpression: CfirReturnExpression, data: D): CfirStatement =
        transformJump(returnExpression, data)

    override fun transformContinueExpression(continueExpression: CfirContinueExpression, data: D): CfirStatement =
        transformJump(continueExpression, data)

    override fun transformBreakExpression(breakExpression: CfirBreakExpression, data: D): CfirStatement =
        transformJump(breakExpression, data)

    override fun transformNamedArgumentExpression(namedArgumentExpression: CfirNamedArgumentExpression, data: D): CfirStatement =
        transformWrappedExpression(namedArgumentExpression, data)

    override fun transformErrorExpression(errorExpression: CfirErrorExpression, data: D): CfirStatement =
        transformExpression(errorExpression, data)

    override fun transformErrorNamedReference(errorNamedReference: CfirErrorNamedReference, data: D): CfirReference =
        transformNamedReference(errorNamedReference, data)

    override fun transformErrorFunction(errorFunction: CfirErrorFunction, data: D): CfirStatement =
        transformFunction(errorFunction, data)
}
