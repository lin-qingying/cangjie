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
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.resolve.calls.mpp.AbstractCjmpMatcher
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchResult
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

/**
 * CJMP 配对执行核心（前端 CJMP_MATCHING 阶段与 low-level 懒解析共用）。
 *
 * 只负责"对某个声明求配对并写存储"，不含遍历/门禁/锁语义。流程对位 cjc 1.1.3：
 * - nominal：`MergeCommonIntoSpecific`——种类相同才合并（绑定），成员随后在自己的声明级入口查找对位；
 *   父容器未配对时，其 specific 成员不建立独立配对；
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
     * 每个 specific 声明独立进入 matcher。容器只建立自身配对与泛型映射，成员通过已配对父容器
     * 查找 common 候选；该调用形状同时服务 eager 树遍历和 LL 声明级懒解析。
     */
    fun matchDeclaration(
        declaration: CfirDeclaration,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
        containingContainer: CfirDeclaration? = null,
    ) {
        val member = declaration as? CfirMemberDeclaration ?: return
        val specificContainer = containingContainer ?:
                (declaration as? CfirCallableDeclaration)?.containingCjmpContainer(session)

        if (declaration is CfirEnumConstructor) {
            val specificEnum = specificContainer as? CfirEnum ?: return
            if (specificEnum.status.isSpecific) matchEnumConstructor(declaration, specificEnum, storage)
            return
        }

        if (!member.status.isSpecific) return
        if (declaration is CfirConstructor && declaration.status.isStatic) return

        if (declaration is CfirPatternVariable) {
            val commonCandidates = if (specificContainer != null) {
                val commonContainers = storage.commonCounterpartsFor(specificContainer)
                if (commonContainers.isEmpty()) {
                    storage.recordUnmatched(declaration)
                    return
                }
                CfirCjmpResolver.findCommonMemberCandidates(declaration, commonContainers)
            } else {
                CfirCjmpResolver.findCommonCandidates(declaration, session)
                    .filterIsInstance<CfirCallableDeclaration>()
            }
            matchPatternVariable(
                declaration,
                commonCandidates,
                session,
                storage,
                specificContainer?.let(storage::typeParameterMappingFor).orEmpty(),
            )
            return
        }

        if (declaration is CfirCallableDeclaration) {
            if (specificContainer != null) {
                val commonContainers = storage.commonCounterpartsFor(specificContainer)
                if (commonContainers.isEmpty()) {
                    storage.recordUnmatched(declaration)
                    return
                }
                val candidates = CfirCjmpResolver.findCommonMemberCandidates(declaration, commonContainers)
                if (candidates.isEmpty()) {
                    storage.recordUnmatched(declaration)
                    return
                }
                matchAgainstCandidates(
                    declaration,
                    candidates,
                    session,
                    storage,
                    storage.typeParameterMappingFor(specificContainer),
                )
                return
            }
        }

        if (declaration is CfirExtend && isDuplicateSpecificExtend(declaration, session)) {
            storage.recordDuplicateSpecificExtend(declaration)
            return
        }

        val candidates = CfirCjmpResolver.findCommonCandidates(declaration, session)
        if (candidates.isEmpty()) {
            storage.recordUnmatched(declaration)
            return
        }
        if (declaration is CfirExtend) {
            matchExtend(declaration, candidates, session, storage)
            return
        }
        matchAgainstCandidates(declaration, candidates, session, storage)
    }

    /** MergeCJMPExtensions 把同 key 的全部 common extend 合并到一个 specific extend。 */
    private fun matchExtend(
        specific: CfirExtend,
        candidates: List<CfirDeclaration>,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        val matchingContext = CfirCjmpMatchingContext(session, storage)
        val matchedCommonExtends = mutableListOf<CfirExtend>()
        val mismatchedCommonExtends = mutableListOf<Pair<CfirExtend, CjmpMismatchKind>>()

        for (candidate in candidates.filterIsInstance<CfirExtend>()) {
            candidate.resolveCjmpSignatureTypes()
            when (val result = AbstractCjmpMatcher.match(
                specific = specific.symbol,
                common = candidate.symbol,
                context = matchingContext,
            )) {
                CjmpMatchResult.Matched -> matchedCommonExtends += candidate
                // 官方在键分组前跳过类型无效的 extend，不能让未解析候选压制其它已匹配 owner。
                CjmpMatchResult.TypeNotResolved -> Unit

                is CjmpMatchResult.Mismatched -> mismatchedCommonExtends += candidate to result.kind
                is CjmpMatchResult.ParameterMismatched -> mismatchedCommonExtends += candidate to result.mismatch.kind
            }
        }

        if (matchedCommonExtends.isEmpty()) {
            if (mismatchedCommonExtends.isEmpty()) {
                storage.recordUnmatched(specific)
            } else {
                mismatchedCommonExtends.forEach { (common, kind) -> storage.recordMismatch(specific, kind, common) }
            }
            return
        }

        storage.bindCommonExtendCounterparts(
            specific,
            matchedCommonExtends.map { common -> common to common.typeParameters.zip(specific.typeParameters).toMap() },
        )
    }

    /**
     * 逐候选尝试配对（官方 `MatchSpecificDeclWithCommonDecls` 的候选循环），返回绑定到的 common 声明。
     */
    private fun matchAgainstCandidates(
        specific: CfirDeclaration,
        candidates: List<CfirDeclaration>,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
        parentTypeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter> = emptyMap(),
    ): CfirDeclaration? {
        specific.resolveCjmpSignatureTypes()
        candidates.forEach { it.resolveCjmpSignatureTypes() }
        val matchingContext = CfirCjmpMatchingContext(session, storage)
        return AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(
            specific = specific.symbol,
            commonCandidates = candidates.map { it.symbol },
            context = matchingContext,
            parentTypeParameterMapping = matchingContext.parentTypeParameterMapping(parentTypeParameterMapping),
        )?.cfir
    }

    /**
     * 模式变量配对（官方 `MatchCJMPVar` / `TryMatchVarWithPatternWithVarDecls`）：
     * 逐绑定变量按名找 common 变量；单绑定（`let v: T = e`）时配对落在模式变量本身，
     * 元组模式要求每个绑定都配对成功，否则整个模式变量无配对。
     */
    private fun matchPatternVariable(
        declaration: CfirPatternVariable,
        candidates: List<CfirCallableDeclaration>,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
        parentTypeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ) {
        declaration.resolveCjmpSignatureTypes()
        candidates.forEach { it.resolveCjmpSignatureTypes() }
        val matchingContext = CfirCjmpMatchingContext(session, storage)
        val mappedParentTypeParameters = matchingContext.parentTypeParameterMapping(parentTypeParameterMapping)
        val bindings = declaration.pattern.bindingVariables()
        val matched = bindings.map { binding ->
            val sameName = candidates.filter {
                it.symbol.name == binding.name
            }
            sameName.firstOrNull {
                AbstractCjmpMatcher.match(
                    specific = declaration.symbol,
                    common = it.symbol,
                    context = matchingContext,
                    parentTypeParameterMapping = mappedParentTypeParameters,
                ) == CjmpMatchResult.Matched
            }
        }
        if (matched.isEmpty() || matched.any { it == null }) {
            storage.recordUnmatched(declaration)
            return
        }
        if (bindings.size == 1) {
            storage.bind(declaration, matched.single()!!, parentTypeParameterMapping)
        } else {
            // 元组模式：逐绑定记录配对（模式变量本身不绑定单个 common，检查器对其保持静默）
            bindings.zip(matched).forEach { (binding, common) ->
                storage.bind(binding, common!!, parentTypeParameterMapping)
            }
        }
    }

    /**
     * enum 构造器声明级配对（官方 `MatchCJMPEnumConstructor`）：父 enum 已配对后按名比较，
     * 带参构造器的参数类型逐个相同；common enum 非穷尽时 specific 多出的构造器合法。
     */
    private fun matchEnumConstructor(
        specific: CfirEnumConstructor,
        specificEnum: CfirEnum,
        storage: CfirCjmpMappingStorage,
    ) {
        val commonEnum = storage.commonFor(specificEnum) as? CfirEnum
        if (commonEnum == null) {
            storage.recordUnmatched(specific)
            return
        }
        specific.resolveCjmpSignatureTypes()
        val typeParameterMapping = storage.typeParameterMappingFor(specificEnum)
        val sameName = commonEnum.declarations
            .filterIsInstance<CfirEnumConstructor>()
            .filter { it.name == specific.name }
            .onEach { it.resolveCjmpSignatureTypes() }
        val counterpart = sameName.firstOrNull { candidate ->
            CfirCjmpMatcher.matchEnumConstructors(specific, candidate, typeParameterMapping) ==
                    CjmpTypeCompatibility.COMPATIBLE
        }
        val unresolved = sameName.firstOrNull { candidate ->
            CfirCjmpMatcher.matchEnumConstructors(specific, candidate, typeParameterMapping) ==
                    CjmpTypeCompatibility.UNRESOLVED
        }
        when {
            counterpart != null -> storage.bind(specific, counterpart, typeParameterMapping)
            unresolved != null -> storage.recordMismatch(specific, CjmpMismatchKind.TYPE_NOT_RESOLVED, unresolved)
            commonEnum.isNonExhaustive -> Unit
            else -> storage.recordUnmatched(specific)
        }
    }

    /** 按官方 MergeCJMPExtensions 键排除同包、同模块中较早声明之后的 specific extend。 */
    private fun isDuplicateSpecificExtend(specific: CfirExtend, session: CfirSession): Boolean {
        val allExtends = session.extendProvider.getAllExtends()
        allExtends.forEach { it.resolveCjmpSignatureTypes() }
        val currentPackage = session.extendProvider.getPackageFqName(specific)
            ?: session.extendProvider.getContainingFile(specific)?.packageDirective?.packageFqName
            ?: return false
        val siblings = allExtends
            .filter { candidate ->
                candidate !== specific &&
                        candidate.status.isSpecific &&
                        candidate.moduleData.name == specific.moduleData.name &&
                        (session.extendProvider.getPackageFqName(candidate)
                            ?: session.extendProvider.getContainingFile(candidate)?.packageDirective?.packageFqName) == currentPackage &&
                        CfirCjmpMatcher.haveSameExtendKey(specific, candidate) == CjmpTypeCompatibility.COMPATIBLE
            }
            .sortedWith(
                compareBy<CfirExtend>(
                    { session.extendProvider.getContainingFile(it)?.sourceFile?.path.orEmpty() },
                    { it.source?.startOffset ?: Int.MAX_VALUE },
                ),
            )
        if (siblings.isEmpty()) return false

        val currentPath = session.extendProvider.getContainingFile(specific)?.sourceFile?.path.orEmpty()
        val currentOffset = specific.source?.startOffset ?: Int.MAX_VALUE
        return siblings.any { candidate ->
            val candidatePath = session.extendProvider.getContainingFile(candidate)?.sourceFile?.path.orEmpty()
            val candidateOffset = candidate.source?.startOffset ?: Int.MAX_VALUE
            candidatePath < currentPath || candidatePath == currentPath && candidateOffset < currentOffset
        }
    }

    private fun CfirDeclaration.resolveCjmpSignatureTypes() {
        if (this is CfirMemberDeclaration) lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES)
    }

    /** specific member 的父容器；common 候选只从该容器的已配对 common 声明中读取。 */
    private fun CfirCallableDeclaration.containingCjmpContainer(session: CfirSession): CfirDeclaration? {
        session.extendProvider.getContainingExtend(symbol)?.let { return it }
        val containingClassId = symbol.callableId.classId ?: return null
        return session.symbolProvider.getClassLikeSymbolByClassId(containingClassId)?.cfir as? CfirClassLikeDeclaration
    }

    /**
     * 声明是否可能拥有 common 对位（low-level 解析入口的过滤面，对位 Kotlin
     * `FirMemberDeclaration.canHaveExpectCounterPart`）。
     */
    fun canHaveCommonCounterpart(declaration: CfirDeclaration): Boolean = when (declaration) {
        is CfirClassLikeDeclaration -> true
        is CfirExtend -> true
        is CfirPatternVariable -> true
        is CfirConstructor -> !declaration.status.isStatic
        is CfirCallableDeclaration -> true
        else -> false
    }
}
