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
 */

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirCTypeSemantics
import org.cangnova.cangjie.cfir.types.ConeTupleType

/**
 * 元组成员不能是 `@C` struct。
 *
 * 对齐官方 `TypeChecker::CheckTupleType`（TypeCheckType.cpp:299-310）：
 * tuple 字段类型满足 `Ty::IsCTypeConstraint`（= `@C` struct，官方另排除 String，
 * CFIR 中 String 非 `status.isC`，天然不触发）时报
 * `sema_invalid_tuple_field_ctype`。primitive、CPointer、CString、CFunc 等
 * 其它满足 CType 的类型不受限。
 */
object CfirTupleCFieldTypeChecker : CfirResolvedTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirResolvedTypeRef) {
        val tupleType = typeRef.coneType as? ConeTupleType ?: return
        if (tupleType.elementTypes.none { elementType ->
                CfirCTypeSemantics.isCStruct(context.session, elementType)
            }
        ) {
            return
        }

        val source = typeRef.source ?: typeRef.delegatedTypeRef?.source ?: return
        reporter.reportOn(
            source = source,
            factory = CfirErrors.INVALID_TUPLE_FIELD_CTYPE,
        )
    }
}
