/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.isIntegerType
import org.cangnova.cangjie.cfir.types.isString

/**
 * 程序入口 `main` 签名检查器。
 *
 * 对齐官方 `TypeCheckerImpl::CheckEntryFunc`：源码级全局入口只能无参，
 * 或只带一个 `Array<String>` 参数；显式返回类型只能是整数类型或 `Unit`。
 */
object CfirMainFunctionSignatureChecker : CfirMainFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirMainFunction) {
        if (declaration.isLocal || declaration.symbol.callableId.classId != null) return

        val diagnosticSource = declaration.mainFunctionNameDiagnosticSource() ?: return
        checkExplicitReturnType(declaration, diagnosticSource)
        checkValueParameters(declaration, diagnosticSource)
    }

    /**
     * 官方只针对源码中存在的显式返回类型执行入口返回类型约束。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExplicitReturnType(
        declaration: CfirMainFunction,
        diagnosticSource: org.cangnova.cangjie.source.AbstractCjSourceElement,
    ) {
        if (declaration.returnTypeRef.source == null) return
        val returnType = declaration.returnTypeRef.coneTypeOrNull ?: return
        if (returnType is ConeErrorType) return
        val expandedReturnType = returnType.fullyExpandedType(context.session)
        if (expandedReturnType is ConeErrorType) return
        if (expandedReturnType.isIntegerType || expandedReturnType.isUnit) return

        reporter.reportOn(
            source = diagnosticSource,
            factory = CfirErrors.UNEXPECTED_RETURN_TYPE_FOR_ENTRY,
        )
    }

    /**
     * 入口参数只能为空，或唯一且类型为标准库 `Array<String>`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkValueParameters(
        declaration: CfirMainFunction,
        diagnosticSource: org.cangnova.cangjie.source.AbstractCjSourceElement,
    ) {
        val parameters = declaration.valueParameters
        if (parameters.isEmpty()) return
        if (parameters.size == 1) {
            val parameterType = parameters.single().returnTypeRef.coneTypeOrNull
            if (parameterType != null && parameterType !is ConeErrorType && parameterType.isEntryArrayString()) {
                return
            }
        }

        reporter.reportOn(
            source = diagnosticSource,
            factory = CfirErrors.UNEXPECTED_PARAM_FOR_ENTRY,
        )
    }

    /**
     * 判断类型是否为官方入口参数允许的 `Array<String>`。
     */
    context(context: CheckerContext)
    private fun ConeCangJieType.isEntryArrayString(): Boolean {
        val expandedType = fullyExpandedType(context.session)
        if (expandedType is ConeErrorType) return false
        val elementType = expandedType.arrayElementType ?: return false
        val expandedElementType = elementType.fullyExpandedType(context.session)
        return expandedElementType !is ConeErrorType && expandedElementType.isString
    }
}
