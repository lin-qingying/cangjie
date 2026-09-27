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

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpParameterMismatch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * first-fit 候选尝试期间已直接触发诊断的结论。
 *
 * 后续候选可以成功建立映射，但官方匹配器不会撤销先前候选已经报告的参数或实现体诊断。
 */
public sealed interface CfirCjmpCandidateDiagnostic {
    /** common 有默认实现而当前 specific 没有实现体。 */
    public data class MissingBody(val commonCandidate: CfirDeclaration?) : CfirCjmpCandidateDiagnostic

    /** 命名参数或默认值同时出现在两侧。 */
    public data class ParameterMismatch(val mismatch: CjmpParameterMismatch) : CfirCjmpCandidateDiagnostic
}

/**
 * CJMP 配对结果存储（specific session 组件，单侧写）。
 *
 * 架构约束（D1/C25）：
 * - 只写在 **specific 侧**：映射键/值均为 specific 模块内的声明与依赖侧 common 声明**符号引用**，
 *   绝不改写 common 侧声明（跨 session 写入违反风险 8）。
 * - 每个 session 持有独立实例（LL 不同声明可并行配对），由 session factory 注册；不能使用共享默认单例。
 * - 存储随 resolve 轮次重置（[clear]），避免增量/重复 resolve 残留。
 *
 * 消费方：
 * - 匹配阶段（`CfirCjmpMatcherTransformer`）写入配对/失配/第二绑定；
 * - 检查器（Phase 3 `CfirCommonSpecificChecker` 改造）读取配对结果，不再 symbolProvider 现查；
 * - 调用解析/类型精化（Phase 2.5）经 [commonFor] 做候选过滤与重解析。
 */
open class CfirCjmpMappingStorage : CfirSessionComponent {
    /** specific 声明 → 已匹配的 common 声明。 */
    private val commonForSpecific = ConcurrentHashMap<CfirDeclaration, CfirDeclaration>()

    /** specific extend → 其它合并进同一 specific extend 的 common extend 声明。 */
    private val additionalCommonExtendsForSpecific = ConcurrentHashMap<CfirExtend, CopyOnWriteArrayList<CfirExtend>>()

    /** common 声明 → 绑定到它的 specific 声明列表（用于 MULTIPLE_COMMON_IMPLEMENTATIONS 判据）。 */
    private val specificBindingsForCommon = ConcurrentHashMap<CfirDeclaration, CopyOnWriteArrayList<CfirDeclaration>>()

    /** specific 声明 → 从 common 外层类型参数到 specific 外层/自身类型参数的映射。 */
    private val typeParameterMappings = ConcurrentHashMap<CfirDeclaration, Map<CfirTypeParameter, CfirTypeParameter>>()

    /** specific 声明 → 失配原因列表（匹配阶段的结构性失配，供 NOT_MATCHED 分类）。 */
    private val mismatchKinds = ConcurrentHashMap<CfirDeclaration, CopyOnWriteArrayList<CjmpMismatchKind>>()

    /** specific 声明 → 与各结构失配对应的 common 候选，供 checker 消费原始匹配结论。 */
    private val mismatchCandidates = ConcurrentHashMap<CfirDeclaration, CopyOnWriteArrayList<CfirDeclaration>>()

    /** 无任何候选的 specific 声明。 */
    private val unmatched = ConcurrentHashMap.newKeySet<CfirDeclaration>()

    /** specific 声明 → 参数级失败（按候选顺序去重）。 */
    private val parameterMismatches = ConcurrentHashMap<CfirDeclaration, CopyOnWriteArrayList<CjmpParameterMismatch>>()

    /** specific 声明 → first-fit 期间已触发且不得被后续绑定撤销的候选诊断。 */
    private val candidateDiagnostics = ConcurrentHashMap<CfirDeclaration, CopyOnWriteArrayList<CfirCjmpCandidateDiagnostic>>()

    /** 通过 MergeCJMPExtensions 键判定的重复 specific extend 声明。 */
    private val duplicateSpecificExtends = ConcurrentHashMap.newKeySet<CfirDeclaration>()

    /** 记录一个 specific extend 在当前 CJMP 键下不是首个声明。 */
    open fun recordDuplicateSpecificExtend(specific: CfirDeclaration) {
        duplicateSpecificExtends += specific
    }

    /** 当前 specific extend 是否已被匹配阶段判定为同键重复声明。 */
    open fun isDuplicateSpecificExtend(specific: CfirDeclaration): Boolean = specific in duplicateSpecificExtends

    /** 记录参数级失败（官方在参数上报告诊断后匹配失败）。 */
    open fun recordParameterMismatch(specific: CfirDeclaration, mismatch: CjmpParameterMismatch) {
        recordParameterMismatch(specific, mismatch, candidate = null)
    }

    /** 记录参数级失败及产生该结果的 common 候选。 */
    open fun recordParameterMismatch(
        specific: CfirDeclaration,
        mismatch: CjmpParameterMismatch,
        candidate: CfirDeclaration?,
    ) {
        parameterMismatches.computeIfAbsent(specific) { CopyOnWriteArrayList() }.addIfAbsent(mismatch)
        candidateDiagnostics.computeIfAbsent(specific) { CopyOnWriteArrayList() }
            .addIfAbsent(CfirCjmpCandidateDiagnostic.ParameterMismatch(mismatch))
        recordMismatch(specific, mismatch.kind, candidate)
    }

    /** specific 声明的参数级失败列表。 */
    open fun parameterMismatchesFor(specific: CfirDeclaration): List<CjmpParameterMismatch> =
        parameterMismatches[specific]?.toList().orEmpty()

    /**
     * 绑定 specific → common。
     *
     * @return true 表示本次绑定成立；false 表示该 common 已被**另一个** specific 绑定
     *   （第二绑定事件，对位官方 `TrySetSpecificImpl`，检查器据此报 `MULTIPLE_COMMON_IMPLEMENTATIONS`）。
     *
     * 正反向索引及类型映射在一个同步事务内更新，避免并发 LL 查询观察到半完成绑定。
     */
    @Synchronized
    open fun bind(
        specific: CfirDeclaration,
        common: CfirDeclaration,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter> = emptyMap(),
    ): Boolean {
        val previousCommon = commonForSpecific[specific]
        if (previousCommon != null) {
            if (previousCommon === common) return true
            // 官方 `TrySetSpecificImpl`：common 已有 specific 实现时绑定失败——第二个 specific 无配对
            //（报 NOT_MATCHED）；same specific 重复求配对到另一 common 时也不得污染反向索引。
            recordMismatch(specific, CjmpMismatchKind.SECOND_BINDING, common)
            return false
        }

        val bindings = specificBindingsForCommon.compute(common) { _, existing ->
            existing ?: CopyOnWriteArrayList()
        }!!
        if (!bindings.addIfAbsent(specific)) {
            if (commonForSpecific[specific] === common) return true
            // 被拒绝的第二实现保留在反向列表中，供 common 方向报告 MULTIPLE_COMMON_IMPLEMENTATIONS；
            // 同一声明重入配对时仍应返回拒绝结果，而不能把这条记录误判为索引损坏。
            recordMismatch(specific, CjmpMismatchKind.SECOND_BINDING, common)
            return false
        }
        if (bindings.size > 1) {
            recordMismatch(specific, CjmpMismatchKind.SECOND_BINDING, common)
            return false
        }

        commonForSpecific[specific] = common
        if (typeParameterMapping.isEmpty()) {
            typeParameterMappings.remove(specific)
        } else {
        typeParameterMappings[specific] = typeParameterMapping.toMap()
        }
        unmatched -= specific
        mismatchKinds -= specific
        mismatchCandidates -= specific
        parameterMismatches -= specific
        return true
    }

    /** 返回 specific 声明已匹配的 common 声明。 */
    @Synchronized
    open fun commonFor(specific: CfirDeclaration): CfirDeclaration? = commonForSpecific[specific]

    /** 同 key 的多个 common extend 会共同组成一个 specific extend 的成员候选面。 */
    @Synchronized
    open fun commonCounterpartsFor(specific: CfirDeclaration): List<CfirDeclaration> {
        val primary = commonForSpecific[specific] ?: return emptyList()
        val additional = (specific as? CfirExtend)
            ?.let(additionalCommonExtendsForSpecific::get)
            .orEmpty()
        return (listOf(primary) + additional).distinct()
    }

    /** 记录已合并进 specific extend 的其它同 key common extend。 */
    @Synchronized
    open fun addCommonExtendCounterpart(
        specific: CfirExtend,
        common: CfirExtend,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ) {
        check(commonForSpecific[specific] is CfirExtend) {
            "A common extend counterpart can only be added after the specific extend is bound"
        }
        if (commonForSpecific[specific] === common) return

        val currentTypeParameterMapping = typeParameterMappings[specific].orEmpty()
        for ((commonTypeParameter, specificTypeParameter) in typeParameterMapping) {
            val existing = currentTypeParameterMapping[commonTypeParameter]
            check(existing == null || existing === specificTypeParameter) {
                "Equivalent common extends map one type parameter to different specific parameters"
            }
        }

        val additions = additionalCommonExtendsForSpecific.computeIfAbsent(specific) { CopyOnWriteArrayList() }
        if (!additions.addIfAbsent(common)) return

        specificBindingsForCommon.computeIfAbsent(common) { CopyOnWriteArrayList() }.addIfAbsent(specific)
        typeParameterMappings[specific] = currentTypeParameterMapping + typeParameterMapping
    }

    /** 原子写入一个 specific extend 与其全部 common extend owner 的关系。 */
    @Synchronized
    open fun bindCommonExtendCounterparts(
        specific: CfirExtend,
        commonCounterparts: List<Pair<CfirExtend, Map<CfirTypeParameter, CfirTypeParameter>>>,
    ): Boolean {
        require(commonCounterparts.isNotEmpty()) { "A specific extend must have at least one common counterpart" }
        val (primary, primaryTypeParameterMapping) = commonCounterparts.first()
        if (!bind(specific, primary, primaryTypeParameterMapping)) return false
        for ((common, typeParameterMapping) in commonCounterparts.drop(1)) {
            addCommonExtendCounterpart(specific, common, typeParameterMapping)
        }
        return true
    }

    /** 返回本次配对中 common 到 specific 的类型参数映射，供成员类型与约束检查复用。 */
    @Synchronized
    open fun typeParameterMappingFor(specific: CfirDeclaration): Map<CfirTypeParameter, CfirTypeParameter> =
        typeParameterMappings[specific].orEmpty()

    /** 返回绑定到 common 声明的全部 specific 声明。 */
    @Synchronized
    open fun specificBindingsFor(common: CfirDeclaration): List<CfirDeclaration> =
        specificBindingsForCommon[common]?.toList().orEmpty()

    /** 记录一条结构性失配。 */
    open fun recordMismatch(specific: CfirDeclaration, kind: CjmpMismatchKind) {
        recordMismatch(specific, kind, candidate = null)
    }

    /** 记录结构失配和产生该结果的 common 候选。 */
    open fun recordMismatch(specific: CfirDeclaration, kind: CjmpMismatchKind, candidate: CfirDeclaration?) {
        mismatchKinds.computeIfAbsent(specific) { CopyOnWriteArrayList() }.addIfAbsent(kind)
        if (kind == CjmpMismatchKind.MISSING_BODY) {
            candidateDiagnostics.computeIfAbsent(specific) { CopyOnWriteArrayList() }
                .addIfAbsent(CfirCjmpCandidateDiagnostic.MissingBody(candidate))
        }
        if (candidate != null) {
            mismatchCandidates.computeIfAbsent(specific) { CopyOnWriteArrayList() }.addIfAbsent(candidate)
        }
    }

    /** 返回 first-fit 期间已触发、且不能被后续候选绑定撤销的诊断事件。 */
    open fun candidateDiagnosticsFor(specific: CfirDeclaration): List<CfirCjmpCandidateDiagnostic> =
        candidateDiagnostics[specific]?.toList().orEmpty()

    /** 返回 specific 声明的结构性失配列表。 */
    open fun mismatchKindsFor(specific: CfirDeclaration): List<CjmpMismatchKind> =
        mismatchKinds[specific]?.toList().orEmpty()

    /** 返回与 specific 声明的失配结论一一关联的 common 候选。 */
    open fun mismatchCandidatesFor(specific: CfirDeclaration): List<CfirDeclaration> =
        mismatchCandidates[specific]?.toList().orEmpty()

    /** 记录"无候选"（依赖图中不存在同名 common 声明）。 */
    open fun recordUnmatched(specific: CfirDeclaration) {
        unmatched += specific
    }

    /** 返回 specific 声明是否无任何 common 候选。 */
    open fun isUnmatched(specific: CfirDeclaration): Boolean = specific in unmatched

    /** CJMP_MATCHING 阶段的存储态后置条件：每个可匹配 specific 声明都必须有一条处理结果。 */
    open fun hasResolutionResult(specific: CfirDeclaration): Boolean =
        commonForSpecific.containsKey(specific) || mismatchKinds.containsKey(specific) ||
                specific in unmatched || specific in duplicateSpecificExtends

    /** 已匹配的 specific 声明集合。 */
    open val matchedSpecificDeclarations: Set<CfirDeclaration> get() = commonForSpecific.keys.toSet()

    /** 存储是否为空（门禁早退判定用）。 */
    open val isEmpty: Boolean
        get() = commonForSpecific.isEmpty() && mismatchKinds.isEmpty() && unmatched.isEmpty() &&
                duplicateSpecificExtends.isEmpty() && candidateDiagnostics.isEmpty()

    /** 清空全部配对状态（resolve 轮次重置）。 */
    @Synchronized
    open fun clear() {
        commonForSpecific.clear()
        additionalCommonExtendsForSpecific.clear()
        specificBindingsForCommon.clear()
        typeParameterMappings.clear()
        mismatchKinds.clear()
        mismatchCandidates.clear()
        unmatched.clear()
        parameterMismatches.clear()
        candidateDiagnostics.clear()
        duplicateSpecificExtends.clear()
    }
}

/** 当前 session 的 CJMP 配对结果存储（session factory 逐 session 注册）。 */
val CfirSession.cjmpMappingStorage: CfirCjmpMappingStorage by
    CfirSession.sessionComponentAccessor()

/**
 * 当前 session 的 CJMP 配对结果存储（可空访问版本）。
 *
 * 供可能运行在未装配存储的轻量会话（如平台 common 会话、桩会话）内的消费点使用；
 * 缺失时按"无配对数据"处理，不抛出组件缺失错误。
 */
val CfirSession.cjmpMappingStorageOrNull: CfirCjmpMappingStorage? by
    CfirSession.nullableSessionComponentAccessor()

/**
 * 官方 `COMMON_WITH_DEFAULT`（cjc 1.1.3 `ParseCJMPDecl.cpp` `SetCJMPAttrs`/`HasDefault`）的 CFIR 判定：
 * - 函数/构造器有体、属性有访问器、变量有初始值；
 * - nominal（class/struct/interface/enum）：其全部 common 成员（enum 构造器除外）都有默认实现；
 * - 反序列化声明无体，读恢复位 [org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus.isCommonWithDefault]（D8）。
 */
fun org.cangnova.cangjie.cfir.declarations.CfirDeclaration.cjmpHasCommonDefault(): Boolean {
    val status = (this as? org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration)?.status ?: return false
    if (status.isSpecific) return true
    if (status.isCommonWithDefault) return true
    return when (this) {
        is org.cangnova.cangjie.cfir.declarations.CfirFunction -> body != null
        is org.cangnova.cangjie.cfir.declarations.CfirProperty -> getter != null || setter != null
        is org.cangnova.cangjie.cfir.declarations.CfirVariable -> initializer != null
        is org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration -> declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration>()
            .filter { it.status.isCommon && it !is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor }
            .all { it.cjmpHasCommonDefault() }

        is org.cangnova.cangjie.cfir.declarations.CfirExtend -> declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration>()
            .filter { it.status.isCommon }
            .all { it.cjmpHasCommonDefault() }

        else -> false
    }
}

/**
 * 形参在调用解析中是否视为"有默认值"（计划 D7 读穿：官方 `MatchCJMPFunction` 把 common 形参的默认值
 * 克隆到 specific 形参，本仓库不跨 session 克隆表达式，改为按配对结果读穿）。
 *
 * 自身有默认值，或所属函数是已配对的 specific 函数且 common 对应形参有默认值。
 */
fun org.cangnova.cangjie.cfir.declarations.CfirValueParameter.cjmpHasDefaultValue(): Boolean {
    if (defaultValue != null) return true
    val function = containingDeclarationSymbol.cfir as? org.cangnova.cangjie.cfir.declarations.CfirFunction ?: return false
    if (!function.status.isSpecific) return false
    val storage = runCatching { function.moduleData.session.cjmpMappingStorageOrNull }.getOrNull() ?: return false
    val common = storage.commonFor(function) as? org.cangnova.cangjie.cfir.declarations.CfirFunction ?: return false
    val index = function.valueParameters.indexOf(this)
    if (index < 0) return false
    return common.valueParameters.getOrNull(index)?.defaultValue != null
}
