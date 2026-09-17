/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirObjCTypeSemantics
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.type

/**
 * Objective-C wrapper type-argument checker。
 *
 * `ObjCPointer<T>`、`ObjCFunc<F>` 与 `ObjCBlock<F>` 的约束属于类型使用语义，不能只
 * 在 `@ObjCMirror`/`@ObjCImpl` 声明成员上检查。挂在 resolved type-ref checker 后，
 * 参数、返回值、字段、属性、局部值、typealias 展开和泛型实例化都会经过同一个 owner。
 */
object CfirObjCTypeArgumentChecker : CfirResolvedTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirResolvedTypeRef) {
        val type = typeRef.coneType.fullyExpandedType(context.session)
        val classId = type.classIdOrPrimitiveClassId ?: return
        // The generic wrapper declaration itself necessarily mentions its own
        // type parameter. That declaration is the library contract being
        // consumed, not a user type-argument use site.
        if (context.containingDeclarations.any {
                (it as? CfirClassLikeSymbol<*>)?.classId == classId
            }) return
        val source = typeRef.source ?: typeRef.delegatedTypeRef?.source ?: return
        when (classId) {
            CfirObjCTypeSemantics.objCPointerClassId -> {
                val pointee = type.typeArguments.singleOrNull()?.type ?: return
                if (pointee is ConeErrorType) return
                if (!CfirObjCTypeSemantics.isObjCPointerPointee(context.session, pointee)) {
                    reporter.reportOn(
                        source = wrapperArgumentSource(typeRef) ?: source,
                        factory = CfirErrors.OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE,
                    )
                }
            }

            CfirObjCTypeSemantics.objCFuncClassId,
            CfirObjCTypeSemantics.objCBlockClassId -> {
                val function = type.typeArguments.singleOrNull()?.type ?: return
                if (function is ConeErrorType) return
                if (!CfirObjCTypeSemantics.isObjCFunctionSignature(context.session, function)) {
                    reporter.reportOn(
                        source = wrapperArgumentSource(typeRef) ?: source,
                        factory = CfirErrors.OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE,
                        a = classId.shortClassName.asString(),
                    )
                }
            }
        }
    }

    /** 将错误范围收窄到 wrapper 的具体类型实参，保持官方 type-use 诊断落点。 */
    private fun wrapperArgumentSource(typeRef: CfirResolvedTypeRef) =
        typeRef.originalUserTypeRef()
            ?.qualifier
            ?.lastOrNull()
            ?.typeArguments
            ?.singleOrNull()
            ?.let(::firstInvalidFunctionTypeSource)
            ?: typeRef.delegatedTypeRef?.source

    private fun firstInvalidFunctionTypeSource(typeRef: CfirTypeRef) = when (typeRef) {
        is CfirFunctionTypeRef -> typeRef.parameterTypeRefs.firstOrNull()?.source
            ?: typeRef.returnTypeRef.source
            ?: typeRef.source
        else -> typeRef.source
    }

    private tailrec fun CfirTypeRef?.originalUserTypeRef(): CfirUserTypeRef? = when (this) {
        is CfirUserTypeRef -> this
        is CfirResolvedTypeRef -> delegatedTypeRef.originalUserTypeRef()
        else -> null
    }
}
