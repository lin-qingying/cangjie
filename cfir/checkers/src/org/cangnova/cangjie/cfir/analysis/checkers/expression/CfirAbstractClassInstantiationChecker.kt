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

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol

/**
 * 抽象类实例化检查器。
 *
 * 对齐官方 `TypeCheckCall.cpp:2515-2535 CheckCallKind`：构造调用解析到
 * abstract class 的构造器时报 `sema_abstract_class_can_not_be_instantiated`；
 * `this(...)`/`super(...)` 委托调用豁免（CFIR 中它们是独立的
 * 委托调用 origin，不进入本 checker）。
 */
object CfirAbstractClassInstantiationChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        // this(...)/super(...) 委托调用豁免（官方 :2527-2531 的 isSuper/isThis 分支）。
        if (expression.origin == CfirFunctionCallOrigin.ConstructorDelegationThis ||
            expression.origin == CfirFunctionCallOrigin.ConstructorDelegationSuper
        ) {
            return
        }
        val reference = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val constructor = reference.resolvedSymbol as? CfirConstructorSymbol ?: return

        val classId = constructor.callableId.classId ?: return
        val owner = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirClass ?: return
        if (!owner.status.isAbstract) return

        reporter.reportOn(
            source = reference.source ?: expression.source,
            factory = CfirErrors.ABSTRACT_CLASS_CAN_NOT_BE_INSTANTIATED,
            a = owner.name,
        )
    }
}
