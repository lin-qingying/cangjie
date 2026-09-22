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

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.name.Name

/**
 * 泛型构造器检查器。
 *
 * 对齐官方 `TypeChecker::CheckConstructor`（TypeChecker.cpp:1381-1392，v1.0.0）：
 * class/struct 内的构造器不能声明自身类型参数（`init<T>`），泛型性只看构造器
 * 自身的类型参数，所在类的类型参数不参与；static 构造器与 Java interop 声明豁免。
 * 官方对主构造器（CheckPrimaryCtorForClassOrStruct, :1696-1698）执行同一规则，
 * 主构造器在 CFIR 中同样表示为 [CfirConstructor]，无需单独分支。
 */
object CfirGenericConstructorChecker : CfirConstructorChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirConstructor) {
        if (declaration.typeParameters.isEmpty()) return
        if (declaration.status.isStatic) return
        if (declaration.hasBuiltinAnnotation(BuiltInAnnotationKind.JAVA)) return

        val owner = context.findClosestDeclaration<org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration>()
        if (owner !is CfirClass && owner !is CfirStruct) return

        reporter.reportOn(
            source = declaration.constructorNameDiagnosticSource(includeConstKeyword = false) ?: declaration.source,
            factory = CfirErrors.FORBID_GENERIC_CONSTRUCTOR,
            a = Name.identifier("init"),
        )
    }
}
