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
 */

package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.session.cjmpHasDefaultValue
import org.cangnova.cangjie.cfir.session.cjmpMappingStorage
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled

/**
 * 调用解析读取"形参是否可省"的 CJMP 入口（计划 D7 默认值读穿）。
 *
 * specific 形参自身没有默认值时，默认值在 common 对位声明上。实参映射阶段就可能需要这个答案，
 * 而该阶段可能位于 [org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.IMPLICIT_TYPES]
 * （省略返回类型的函数体推断）——lazy resolve 阶段契约只允许向**更低**阶段请求，
 * 因此不能把对位声明推进到 `CJMP_MATCHING` 来补配对。这里改为直接执行配对核心：
 * 配对只要求签名到达 `IMPLICIT_TYPES`（同阶段请求合法），写存储是幂等的，
 * 后续 CJMP_MATCHING 阶段复用同一结论。
 *
 * 成员的对位必须从**已配对**的 common 容器里查（官方 `MatchCJMPFunction` 的容器先行语义），
 * 因此父容器还没有配对结论时不在此时贸然配对：否则成员会被误记成 unmatched。
 * 这种情况仍按既有语义读不到默认值，由后续正常阶段补齐。
 */
internal fun CfirValueParameter.cjmpHasDefaultValueForCallResolution(): Boolean {
    ensureCjmpCounterpartEstablished()
    return cjmpHasDefaultValue()
}

/**
 * 在需要读穿默认值前，按需为所属 specific 函数建立 CJMP 对位（不推进 resolve phase）。
 */
private fun CfirValueParameter.ensureCjmpCounterpartEstablished() {
    if (defaultValue != null) return
    val function = containingDeclarationSymbol.cfir as? CfirFunction ?: return
    if (!function.status.isSpecific) return
    val session = function.moduleData.session
    if (!session.isCjmpSpecificCompilationEnabled()) return
    val storage = session.cjmpMappingStorage
    if (storage.hasResolutionResult(function)) return

    val container = (function as? CfirCallableDeclaration)?.getContainingCjmpDeclaration()
    if (container != null && !storage.hasResolutionResult(container)) return

    CfirCjmpMatchRunner.matchDeclaration(function, session, storage)
}
