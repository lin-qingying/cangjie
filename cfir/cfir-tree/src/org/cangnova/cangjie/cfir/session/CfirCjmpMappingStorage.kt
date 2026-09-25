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
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/**
 * specific 声明与 candidate common 声明不能配对的原因分类。
 *
 * 对位 Kotlin `ExpectActualMatchingCompatibility.Mismatch`（结构匹配面的子集）：
 * 匹配阶段只覆盖"可被重载修复"的结构差异；返回类型/注解/修饰符等不在此列（D2：验证期职责）。
 */
enum class CjmpMismatchKind {
    /** 可调用者种类不同（函数 vs 属性）。 */
    CALLABLE_KIND,

    /** 参数形状不同（扩展接收者 vs 非扩展）。 */
    PARAMETER_SHAPE,

    /** 值参数个数不同。 */
    PARAMETER_COUNT,

    /** 函数类型参数个数不同。 */
    FUNCTION_TYPE_PARAMETER_COUNT,

    /** 参数类型不兼容。 */
    PARAMETER_TYPES,

    /** 参数命名性/参数名不同（D5）。 */
    PARAMETER_NAMES,

    /** 函数类型参数上界不兼容（D6：宽严比较在验证期细判，此处仅结构面，暂不激活）。 */
    FUNCTION_TYPE_PARAMETER_UPPER_BOUNDS,

    /** 函数类型不满足 specific ≤ common（官方 `IsFuncDeclSubType`：参数类型相等 + 返回类型协变）。 */
    FUNCTION_TYPE,

    /** 同一参数两侧都带默认值（官方 `sema_cjmp_parameter_default_value_both_sides`，匹配失败）。 */
    PARAMETER_DEFAULT_VALUE_BOTH_SIDES,

    /** class-like 种类不同（官方 `MatchNominativeDecl` 的 `specific_has_different_kind`，不绑定）。 */
    CLASS_KIND,

    /** common 带默认实现而 specific 成员为 abstract（官方 `NeedToReportMissingBody`，不绑定）。 */
    MISSING_BODY,

    /** common 已被另一 specific 绑定（官方 `TrySetSpecificImpl` 第二绑定，不绑定）。 */
    SECOND_BINDING,
}

/**
 * 参数级匹配失败（官方 `MatchCJMPFunction` 在参数上报告后返回 false）。
 *
 * @property kind [CjmpMismatchKind.PARAMETER_NAMES] 或 [CjmpMismatchKind.PARAMETER_DEFAULT_VALUE_BOTH_SIDES]。
 * @property parameterIndex 出错参数在 specific 值参数列表中的下标（诊断锚点）。
 */
data class CjmpParameterMismatch(val kind: CjmpMismatchKind, val parameterIndex: Int)

/**
 * CJMP 配对结果存储（specific session 组件，单侧写）。
 *
 * 架构约束（D1/C25）：
 * - 只写在 **specific 侧**：映射键/值均为 specific 模块内的声明与依赖侧 common 声明**符号引用**，
 *   绝不改写 common 侧声明（跨 session 写入违反风险 8）。
 * - 每个 session 持有独立实例（可变异步写入），由 session factory 注册；不能使用共享默认单例。
 * - 存储随 resolve 轮次重置（[clear]），避免增量/重复 resolve 残留。
 *
 * 消费方：
 * - 匹配阶段（`CfirCjmpMatcherTransformer`）写入配对/失配/第二绑定；
 * - 检查器（Phase 3 `CfirCommonSpecificChecker` 改造）读取配对结果，不再 symbolProvider 现查；
 * - 调用解析/类型精化（Phase 2.5）经 [commonFor] 做候选过滤与重解析。
 */
open class CfirCjmpMappingStorage : CfirSessionComponent {
    /** specific 声明 → 已匹配的 common 声明。 */
    private val commonForSpecific = HashMap<CfirDeclaration, CfirDeclaration>()

    /** common 声明 → 绑定到它的 specific 声明列表（用于 MULTIPLE_COMMON_IMPLEMENTATIONS 判据）。 */
    private val specificBindingsForCommon = HashMap<CfirDeclaration, MutableList<CfirDeclaration>>()

    /** specific 声明 → 失配原因列表（匹配阶段的结构性失配，供 NOT_MATCHED 分类）。 */
    private val mismatchKinds = HashMap<CfirDeclaration, MutableList<CjmpMismatchKind>>()

    /** 无任何候选的 specific 声明。 */
    private val unmatched = LinkedHashSet<CfirDeclaration>()

    /** specific 声明 → 参数级失败（按候选顺序去重）。 */
    private val parameterMismatches = HashMap<CfirDeclaration, MutableList<CjmpParameterMismatch>>()

    /** 记录参数级失败（官方在参数上报告诊断后匹配失败）。 */
    open fun recordParameterMismatch(specific: CfirDeclaration, mismatch: CjmpParameterMismatch) {
        val list = parameterMismatches.getOrPut(specific) { mutableListOf() }
        if (mismatch !in list) list += mismatch
        recordMismatch(specific, mismatch.kind)
    }

    /** specific 声明的参数级失败列表。 */
    open fun parameterMismatchesFor(specific: CfirDeclaration): List<CjmpParameterMismatch> =
        parameterMismatches[specific].orEmpty()

    /**
     * 绑定 specific → common。
     *
     * @return true 表示本次绑定成立；false 表示该 common 已被**另一个** specific 绑定
     *   （第二绑定事件，对位官方 `TrySetSpecificImpl`，检查器据此报 `MULTIPLE_COMMON_IMPLEMENTATIONS`）。
     */
    open fun bind(specific: CfirDeclaration, common: CfirDeclaration): Boolean {
        val bindings = specificBindingsForCommon.getOrPut(common) { mutableListOf() }
        if (bindings.any { it === specific }) return true
        val isSecondBinding = bindings.isNotEmpty()
        bindings += specific
        if (isSecondBinding) {
            // 官方 `TrySetSpecificImpl`：common 已有 specific 实现时绑定失败——第二个 specific 无配对
            //（报 NOT_MATCHED），common 侧报 MULTIPLE_COMMON_IMPLEMENTATIONS
            recordMismatch(specific, CjmpMismatchKind.SECOND_BINDING)
            return false
        }
        commonForSpecific[specific] = common
        unmatched -= specific
        mismatchKinds -= specific
        parameterMismatches -= specific
        return true
    }

    /** 返回 specific 声明已匹配的 common 声明。 */
    open fun commonFor(specific: CfirDeclaration): CfirDeclaration? = commonForSpecific[specific]

    /** 返回绑定到 common 声明的全部 specific 声明。 */
    open fun specificBindingsFor(common: CfirDeclaration): List<CfirDeclaration> =
        specificBindingsForCommon[common].orEmpty()

    /** 记录一条结构性失配。 */
    open fun recordMismatch(specific: CfirDeclaration, kind: CjmpMismatchKind) {
        val kinds = mismatchKinds.getOrPut(specific) { mutableListOf() }
        if (kind !in kinds) kinds += kind
    }

    /** 返回 specific 声明的结构性失配列表。 */
    open fun mismatchKindsFor(specific: CfirDeclaration): List<CjmpMismatchKind> =
        mismatchKinds[specific].orEmpty()

    /** 记录"无候选"（依赖图中不存在同名 common 声明）。 */
    open fun recordUnmatched(specific: CfirDeclaration) {
        unmatched += specific
    }

    /** 返回 specific 声明是否无任何 common 候选。 */
    open fun isUnmatched(specific: CfirDeclaration): Boolean = specific in unmatched

    /** 已匹配的 specific 声明集合。 */
    open val matchedSpecificDeclarations: Set<CfirDeclaration> get() = commonForSpecific.keys

    /** 存储是否为空（门禁早退判定用）。 */
    open val isEmpty: Boolean
        get() = commonForSpecific.isEmpty() && mismatchKinds.isEmpty() && unmatched.isEmpty()

    /** 清空全部配对状态（resolve 轮次重置）。 */
    open fun clear() {
        commonForSpecific.clear()
        specificBindingsForCommon.clear()
        mismatchKinds.clear()
        unmatched.clear()
        parameterMismatches.clear()
    }
}

/** 当前 session 的 CJMP 配对结果存储（session factory 逐 session 注册）。 */
val CfirSession.cjmpMappingStorage: CfirCjmpMappingStorage by
    CfirSession.sessionComponentAccessor()

/**
 * 当前 session 的 CJMP 配对结果存储（可空访问版本）。
 *
 * 供可能运行在未装配存储的轻量会话（如平台 common 会话、桩会话）内的消费点使用：
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
    if (status.isCommonWithDefault) return true
    return when (this) {
        is org.cangnova.cangjie.cfir.declarations.CfirFunction -> body != null
        is org.cangnova.cangjie.cfir.declarations.CfirProperty -> getter != null || setter != null
        is org.cangnova.cangjie.cfir.declarations.CfirVariable -> initializer != null
        is org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration -> declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration>()
            .filter { it.status.isCommon && it !is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor }
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

/**
 * 反序列化声明在 cjo 中记录的源码位置（官方 `Decl.begin`：文件 + 行 + 列，均为 1 基）。
 *
 * 计划 G20：CJMP specific 编译中 common 方向诊断锚在 common 声明上；common 来自 cjo 时没有 PSI/LightTree
 * source，官方用 cjo 内嵌位置输出 `common.cj:行:列`。本仓库同样从 cjo 恢复位置，由装配层以编译消息外显。
 *
 * @property filePath cjo `allFiles` 中记录的源文件路径（official ASTWriter 在 common part 下写绝对路径）。
 */
data class CfirCjoDeclarationPosition(val filePath: String, val line: Int, val column: Int)

private object CjoDeclarationPositionKey : org.cangnova.cangjie.cfir.CfirDeclarationDataKey()

/** 反序列化声明的 cjo 源码位置；源码声明为 null。 */
var org.cangnova.cangjie.cfir.declarations.CfirDeclaration.cjoDeclarationPosition: CfirCjoDeclarationPosition?
    by org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry.data(CjoDeclarationPositionKey)
