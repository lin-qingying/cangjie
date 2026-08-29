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

package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupDisposition
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.lookupOriginForAccessibility
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol

/**
 * 在非 Tower 的构造候选收集入口（委托构造 super(...) / this(...)、
 * 分类器实例化 ClassName(args)）内，在 CandidateFactory.createCandidate 之前，
 * 完成与 TowerLevelHandler 可见性分派等价的语言级可见性预判断。
 *
 * 行为与 TowerLevelHandler.consumeCallableCandidate (L197-221) 一致：
 *   - NOT_DISCOVERABLE   → 静默丢弃，调用方 continue（无 return 值）
 *   - EXCLUDE_CALLABLE   → 必须排除出 overload 集合，调用方 continue（无 return 值）
 *   - Accessible / REPORT_ACCESS_ERROR → 返回 accessibility 结果本身；调用方继续
 *         createCandidate，并把返回值填入 Candidate.discoveryAccessibilityResult，
 *         后续 VisibilityUtils L50 会短路复用，不再重新调用统一 checker；
 *         CfirCheckVisibility 守夜人断言不会再遇到 EXCLUDE/NOT。
 *
 * 由于 #1 / #2 两条非 tower 入口使用本地 constructorCandidates 列表 +
 * reduceCollectedCandidates 归约（不会重新引入已跳过的符号），对 EXCLUDE/NOT 直接
 * continue 即达成与 tower consumeLookupOutcome(Excluded(...)) 相同的最终效果（excluded
 * 符号不参与候选选择），无需额外写入 collector 的 excluded 截止面。
 */
internal fun prefilterConstructorVisibilityBeforeCreateCandidate(
    session: CfirSession,
    callInfo: CallInfo,
    constructorSymbol: CfirConstructorSymbol,
    originScope: CfirScope?,
    provenance: CfirCallableLookupProvenance = CfirCallableLookupProvenance.None,
): CfirAccessibilityResult? {
    val accessContext = CfirAccessContext(
        useSiteFile = callInfo.containingFile,
        containingDeclarations = callInfo.containingDeclarations,
        // 构造器调用始终是显式无 dispatch receiver 的分类器/委托调用；
        // 语义对齐 Kotlin VisibilityUtils.isVisible L74-83 对 constructor 的
        // dispatchReceiver = null 强制规则。
        receiverType = null,
        qualifierSymbol = null,
        lookupOrigin = originScope?.lookupOriginForAccessibility() ?: CfirLookupOrigin.MEMBER,
        kind = CfirAccessKind.CALLABLE,
    )
    val accessibility = session.accessibilityChecker.checkCallable(
        symbol = constructorSymbol,
        context = accessContext,
        provenance = provenance,
    )
    val disposition = (accessibility as? CfirAccessibilityResult.Inaccessible)?.disposition
    return when (disposition) {
        // 对齐 Tower: NOT_DISCOVERABLE 静默丢弃，也不写 collector。
        CfirLookupDisposition.NOT_DISCOVERABLE -> null
        // 对齐 Tower: EXCLUDE_CALLABLE 从候选集中排除；本地列表中跳过该符号即等价。
        CfirLookupDisposition.EXCLUDE_CALLABLE -> null
        // Accessible (accessibility == CfirAccessibilityResult.Accessible → disposition == null)
        // 或 REPORT_ACCESS_ERROR: 继续建 Candidate，并携带可见性结果供后续阶段复用。
        else -> accessibility
    }
}
