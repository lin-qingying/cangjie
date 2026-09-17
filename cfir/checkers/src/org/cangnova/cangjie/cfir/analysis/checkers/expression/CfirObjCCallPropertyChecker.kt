/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.CfirObjCTypeSemantics
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/**
 * Objective-C function/block `call` 属性的表达式级规则。
 *
 * 官方 Objective-C 互操作只允许 `value.call(args...)` 这一种直接调用形态。
 * `call` 的 getter 结果不能作为普通函数值继续流动，因此 `let f = value.call`、
 * `consume(value.call)` 和后续成员访问都必须在属性访问节点上报告错误。这个规则
 * 不能放在声明 checker：声明 checker 看不到属性访问的使用位置。
 *
 * 目标身份由接收者的已解析 ClassId 与已解析属性声明共同确定。不能按 `ObjCFunc`/
 * `ObjCBlock` 短名扫描，也不能把任意名为 `call` 的用户属性当成 Objective-C 属性。
 */
object CfirObjCCallPropertyChecker : CfirQualifiedAccessChecker() {
    private val callName = Name.identifier("call")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        val symbol = expression.resolvedCallableSymbolOrNull() ?: return
        val property = symbol.cfirPropertyOrNull() ?: return
        if (property.name != callName) return

        val receiverType = (expression.explicitReceiver ?: expression.dispatchReceiver)
            ?.coneTypeOrNull
            ?.fullyExpandedType(context.session)
        val receiverClassId = receiverType?.classIdOrPrimitiveClassId
        val receiverIsObjC = receiverClassId == CfirObjCTypeSemantics.objCFuncClassId ||
            receiverClassId == CfirObjCTypeSemantics.objCBlockClassId
        val ownerIsObjC = symbol.objCWrapperOwnerClassIdOrNull() in setOf(
            CfirObjCTypeSemantics.objCFuncClassId,
            CfirObjCTypeSemantics.objCBlockClassId,
        )
        if (!receiverIsObjC && !ownerIsObjC) return

        // `value.call(args...)` is the only permitted use. The same checker is
        // intentionally registered for qualified accesses and function calls;
        // a function call node is the direct-call form and is therefore valid.
        if (expression is CfirFunctionCall) return

        // The official walker diagnoses the complete member-access node, not
        // only the identifier `call`; keep the IDE range on the full relevant
        // expression while using the resolved property identity above.
        val accessSource = expression.source ?: expression.calleeReference.source ?: property.source ?: return
        val receiverSource = (expression.explicitReceiver ?: expression.dispatchReceiver)?.source
        val source = if (receiverSource != null && receiverSource.startOffset < accessSource.startOffset) {
            // Some raw-call/property paths retain only the selector source on
            // the qualified access. Reconstruct the complete member-access
            // range from already built source elements; no PSI is consulted.
            CjOffsetsOnlySourceElement(receiverSource.startOffset, accessSource.endOffset)
        } else {
            accessSource
        }
        reporter.reportOn(
            source = source,
            factory = CfirErrors.OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED,
            a = (receiverClassId ?: symbol.objCWrapperOwnerClassIdOrNull())
                ?.shortClassName
                ?.asString()
                ?: "ObjCFunc",
        )
    }

    /** 取得已经解析的属性声明；不从源码短名重新查找声明。 */
    private fun CfirBasedSymbol<*>.cfirPropertyOrNull(): CfirProperty? = when (this) {
        is org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol ->
            takeIf { isBound }?.cfir as? CfirProperty
        is org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol ->
            propertySymbol.takeIf { it.isBound }?.cfir as? CfirProperty
        else -> null
    }

    /** 读取属性符号的声明 owner，用于覆盖 receiver 类型已经被完成器替换的路径。 */
    private fun CfirBasedSymbol<*>.objCWrapperOwnerClassIdOrNull(): ClassId? = when (this) {
        is org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol -> callableId.classId
        is org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol -> propertySymbol.callableId.classId
        else -> null
    }

    /** 从 resolved/candidate reference 取得 resolver 已确认的属性符号。 */
    private fun CfirQualifiedAccessExpression.resolvedCallableSymbolOrNull(): CfirBasedSymbol<*>? = when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> null
    }
}
