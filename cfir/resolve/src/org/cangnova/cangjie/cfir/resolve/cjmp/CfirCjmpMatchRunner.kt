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

package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CjmpMismatchKind
import org.cangnova.cangjie.cfir.session.cjmpHasCommonDefault

/**
 * CJMP 配对执行核心（前端 CJMP_MATCHING 阶段与 low-level 懒解析共用）。
 *
 * 只负责"对某个声明求配对并写存储"，不含遍历/门禁/锁语义。流程对位 cjc 1.1.3：
 * - nominal：`MergeCommonIntoSpecific`——种类相同才合并（绑定），合并成功后其 specific 成员才参与配对；
 *   合并失败（种类不同）时 specific 成员一律无配对；
 * - 非 nominal：`MatchSpecificDeclWithCommonDecls`——逐候选尝试，首个成功者绑定；参数级失败记录锚点；
 * - 绑定：`TrySetSpecificImpl`——common 已被绑定时为第二绑定（检查器报 `MULTIPLE_COMMON_IMPLEMENTATIONS`）；
 *   common 自带默认实现而 specific 成员为 abstract 时不绑定（`NeedToReportMissingBody`）。
 *
 * 写入方向恒为 specific 侧（C25 单侧存储）。
 */
object CfirCjmpMatchRunner {
    /**
     * 对单个声明执行配对并写存储。
     *
     * 只处理标记为 specific 的**顶层**声明；成员经外层 nominal 的 [matchMembers] 路径处理。
     */
    fun matchDeclaration(declaration: CfirDeclaration, session: CfirSession, storage: CfirCjmpMappingStorage) {
        val status = (declaration as? CfirMemberDeclaration)?.status ?: return
        if (!status.isSpecific) return
        if (declaration is CfirEnumConstructor) return
        if (declaration is CfirCallableDeclaration && declaration.symbol.callableId.classId != null) return

        val candidates = CfirCjmpResolver.findCommonCandidates(declaration, session)
        if (candidates.isEmpty()) {
            storage.recordUnmatched(declaration)
            if (declaration is CfirClassLikeDeclaration) markMembersUnmatched(declaration, storage)
            return
        }
        if (declaration is CfirPatternVariable) {
            matchPatternVariable(declaration, candidates, session, storage)
            return
        }
        val matchedCommon = matchAgainstCandidates(declaration, candidates, session, storage)
        if (declaration is CfirClassLikeDeclaration) {
            if (matchedCommon is CfirClassLikeDeclaration) {
                matchMembers(declaration, matchedCommon, session, storage)
                if (declaration is CfirEnum && matchedCommon is CfirEnum) {
                    matchEnumConstructors(declaration, matchedCommon, session, storage)
                }
            } else {
                markMembersUnmatched(declaration, storage)
            }
        }
        if (declaration is CfirExtend) {
            if (matchedCommon is CfirExtend) {
                matchMemberLists(declaration.declarations, matchedCommon.declarations, session, storage)
            } else {
                markMemberListUnmatched(declaration.declarations, storage)
            }
        }
    }

    /**
     * 已配对容器内的成员配对（官方合并后 `MatchSpecificDeclWithCommonDecls` 的成员面）。
     *
     * 只有标记 specific 的成员参与；enum 构造器随外层豁免（C18），static init 不参与（C31）。
     */
    fun matchMembers(
        specific: CfirClassLikeDeclaration,
        common: CfirClassLikeDeclaration,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        matchMemberLists(specific.declarations, common.declarations, session, storage)
    }

    /** 成员列表配对（nominal 成员与 extend 成员共用）。 */
    private fun matchMemberLists(
        specificDeclarations: List<CfirDeclaration>,
        commonDeclarations: List<CfirDeclaration>,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        val commonMembers = commonDeclarations
            .filterIsInstance<CfirCallableDeclaration>()
            .filter { it !is CfirEnumConstructor && it.status.isCommon }
        for (member in specificDeclarations) {
            if (member !is CfirCallableDeclaration || !member.status.isSpecific) continue
            if (member is CfirEnumConstructor) continue
            if (member is CfirConstructor && member.status.isStatic) continue

            val candidates = commonMembers.filter { candidate ->
                candidate.symbol.name == member.symbol.name &&
                        (candidate is CfirConstructor) == (member is CfirConstructor)
            }
            if (candidates.isEmpty()) {
                storage.recordUnmatched(member)
                continue
            }
            matchAgainstCandidates(member, candidates, session, storage)
        }
    }

    /**
     * 逐候选尝试配对（官方 `MatchSpecificDeclWithCommonDecls` 的候选循环），返回绑定到的 common 声明。
     */
    private fun matchAgainstCandidates(
        specific: CfirDeclaration,
        candidates: List<CfirDeclaration>,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ): CfirDeclaration? {
        for (candidate in candidates) {
            when (val result = CfirCjmpMatcher.match(specific, candidate, session)) {
                is CjmpMatchResult.Matched -> {
                    if (needToReportMissingBody(candidate, specific)) {
                        storage.recordMismatch(specific, CjmpMismatchKind.MISSING_BODY)
                        continue
                    }
                    // 第二绑定（common 已有 specific 实现）绑定失败，继续尝试其余候选（官方候选循环）
                    if (storage.bind(specific, candidate)) return candidate
                }

                is CjmpMatchResult.ParameterMismatched -> storage.recordParameterMismatch(specific, result.mismatch)
                is CjmpMatchResult.Mismatched -> storage.recordMismatch(specific, result.kind)
            }
        }
        return null
    }

    /**
     * 模式变量配对（官方 `MatchCJMPVar` / `TryMatchVarWithPatternWithVarDecls`）：
     * 逐绑定变量按名找 common 变量；单绑定（`let v: T = e`）时配对落在模式变量本身，
     * 元组模式要求每个绑定都配对成功，否则整个模式变量无配对。
     */
    private fun matchPatternVariable(
        declaration: CfirPatternVariable,
        candidates: List<CfirDeclaration>,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        val bindings = declaration.pattern.bindingVariables()
        val matched = bindings.map { binding ->
            val sameName = candidates.filter {
                (it as? CfirCallableDeclaration)?.symbol?.name == binding.name
            }
            sameName.firstOrNull { CfirCjmpMatcher.match(declaration, it, session) is CjmpMatchResult.Matched }
        }
        if (matched.isEmpty() || matched.any { it == null }) {
            storage.recordUnmatched(declaration)
            return
        }
        if (bindings.size == 1) {
            storage.bind(declaration, matched.single()!!)
        } else {
            // 元组模式：逐绑定记录配对（模式变量本身不绑定单个 common，检查器对其保持静默）
            bindings.zip(matched).forEach { (binding, common) -> storage.bind(binding, common!!) }
        }
    }

    /**
     * enum 构造器配对（官方 `MatchCJMPEnumConstructor`）：按名配对，带参构造器要求参数类型逐个相同；
     * common enum 非穷尽时 specific 多出的构造器合法（官方对 COMMON_NON_EXHAUSTIVE 外层静默返回）。
     */
    private fun matchEnumConstructors(
        specific: CfirEnum,
        common: CfirEnum,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        val commonConstructors = common.declarations.filterIsInstance<CfirEnumConstructor>()
        for (constructor in specific.declarations.filterIsInstance<CfirEnumConstructor>()) {
            val counterpart = commonConstructors.firstOrNull { candidate ->
                candidate.name == constructor.name &&
                        CfirCjmpMatcher.matchEnumConstructors(constructor, candidate)
            }
            when {
                counterpart != null -> storage.bind(constructor, counterpart)
                common.isNonExhaustive -> Unit
                else -> storage.recordUnmatched(constructor)
            }
        }
    }

    /** 未配对 nominal 的 specific 成员一律无配对（官方合并失败后成员找不到 common）。 */
    private fun markMembersUnmatched(specific: CfirClassLikeDeclaration, storage: CfirCjmpMappingStorage) {
        markMemberListUnmatched(specific.declarations, storage)
        if (specific is CfirEnum) {
            specific.declarations.filterIsInstance<CfirEnumConstructor>().forEach(storage::recordUnmatched)
        }
    }

    private fun markMemberListUnmatched(declarations: List<CfirDeclaration>, storage: CfirCjmpMappingStorage) {
        for (member in declarations) {
            if (member !is CfirCallableDeclaration || !member.status.isSpecific) continue
            if (member is CfirEnumConstructor) continue
            if (member is CfirConstructor && member.status.isStatic) continue
            storage.recordUnmatched(member)
        }
    }

    /**
     * 官方 `NeedToReportMissingBody`：common 成员自带默认实现（且非 abstract）而 specific 成员为 abstract。
     */
    private fun needToReportMissingBody(common: CfirDeclaration, specific: CfirDeclaration): Boolean {
        val commonStatus = (common as? CfirMemberDeclaration)?.status ?: return false
        val specificStatus = (specific as? CfirMemberDeclaration)?.status ?: return false
        if (common !is CfirCallableDeclaration || common.symbol.callableId.classId == null) return false
        return common.cjmpHasCommonDefault() && !commonStatus.isAbstract && specificStatus.isAbstract
    }

    /**
     * 声明是否可能拥有 common 对位（low-level 解析入口的过滤面，对位 Kotlin
     * `FirMemberDeclaration.canHaveExpectCounterPart`）。
     */
    fun canHaveCommonCounterpart(declaration: CfirDeclaration): Boolean = when (declaration) {
        is CfirClassLikeDeclaration -> true
        is CfirConstructor -> !declaration.status.isStatic
        is CfirCallableDeclaration -> true
        else -> false
    }
}
