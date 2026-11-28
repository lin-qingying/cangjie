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
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.patterns.bindingVariables

/**
 * CJMP common 方向的只读事实查询，对应官方 `MatchCJMPDecls` 的 common 声明循环。
 *
 * 查询仅基于显式传入的声明和完整家族快照，不触发解析、不修改快照，也不构造或渲染诊断。
 */
interface CfirCjmpCommonSideFacts {
    /**
     * common 声明及其 common 成员（nominal 与 extend 成员；common enum 的全部构造器——官方构造器随外层携带
     * COMMON），附外层声明。
     */
    fun commonDeclarationsWithOuter(
        topLevel: Collection<CfirDeclaration>,
    ): List<Pair<CfirDeclaration, CfirDeclaration?>>

    /**
     * 该 common 声明是否因未被 specific 实现而须报 `NOT_MATCHED`（官方 `MustMatchWithSpecific` +
     * `specificImplementation` 判定；第二绑定由 [occupiedSpecificDeclarations] 单独报告）。
     *
     * 判定只读取**家族完备之后**的只读快照：官方在扫描 common 声明之前已经配对完整包的 specific
     * 声明（`CheckCJMP.cpp:1193-1215`），尚未被请求过的 specific 实现不能表现为"不存在"。
     */
    fun mustReportNotMatched(
        common: CfirDeclaration,
        outer: CfirDeclaration?,
        family: CfirCjmpFamilySnapshot,
    ): Boolean

    /** 从真实占用事件取得被拒绝的 specific 声明，不重新匹配声明。 */
    fun occupiedSpecificDeclarations(
        common: CfirDeclaration,
        family: CfirCjmpFamilySnapshot,
    ): List<CfirDeclaration>
}

/** 默认只读查询策略；跨模块消费者只依赖 [CfirCjmpCommonSideFacts] 抽象。 */
val defaultCfirCjmpCommonSideFacts: CfirCjmpCommonSideFacts = DefaultCfirCjmpCommonSideFacts

private object DefaultCfirCjmpCommonSideFacts : CfirCjmpCommonSideFacts {
    override fun commonDeclarationsWithOuter(
        topLevel: Collection<CfirDeclaration>,
    ): List<Pair<CfirDeclaration, CfirDeclaration?>> = buildList {
        for (common in topLevel) {
            if (!common.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON)) continue
            add(common to null)
            val members = when (common) {
                is CfirClassLikeDeclaration -> common.declarations
                is CfirExtend -> common.declarations
                else -> emptyList()
            }
            for (member in members) {
                if (!member.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON)) continue
                add(member to common)
            }
        }
    }

    override fun mustReportNotMatched(
        common: CfirDeclaration,
        outer: CfirDeclaration?,
        family: CfirCjmpFamilySnapshot,
    ): Boolean {
        val commonSpecificBindings = family.specificBindingsFor(common)
        if (commonSpecificBindings.isNotEmpty()) return false
        if (common is CfirEnumConstructor) {
            // 无 specific enum 实现时，官方对 COMMON_WITH_DEFAULT 外层 enum 豁免其构造器配对。
            return !(outer != null && outer.cjmpHasCommonDefault() &&
                    family.specificBindingsFor(outer).isEmpty())
        }
        if (common is CfirPatternVariable &&
            common.pattern.bindingVariables().any { family.specificBindingsFor(it).isNotEmpty() }
        ) return false
        if (common.cjmpHasCommonDefault()) return false
        if (outer is CfirInterface) return false
        return true
    }

    /** 从真实占用事件取得被拒绝的 specific 声明，不重新匹配声明或格式化诊断参数。 */
    override fun occupiedSpecificDeclarations(
        common: CfirDeclaration,
        family: CfirCjmpFamilySnapshot,
    ): List<CfirDeclaration> =
        family.specificDeclarations.flatMap { specific ->
            family.candidateDiagnosticsFor(specific)
                .filterIsInstance<CfirCjmpCandidateDiagnostic.Occupied>()
                .filter { it.commonCandidate === common }
                .map { specific }
        }.distinct()
}
