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

package org.cangnova.cangjie.parsing

/**
 * 成员缺体判定所需的**前端就地事实**。
 *
 * 不出现 `IElementType`、PSI/LightTree 节点、builder 或配置类型：parser、psi2cfir、
 * light-tree2cfir 各自把树形状提取成本类型，再交给 [CjMemberBodyPolicy] 做纯决策。
 * 这是 LightTree 路径可行（无 user-data 通道）的根本原因。
 *
 * @property scopeKind 宿主作用域；缺体是否合法**只**由它和下方因子决定。
 * @property hasBody 函数体块 / 属性体是否存在。
 * @property hasAccessors 属性是否存在 getter/setter（仅属性过程使用）。
 * @property hasReturnType 函数是否真的解析到返回类型（仅函数过程使用）。
 * @property isForeign 是否 `foreign`（官方 `isMember` 显式排除；属性过程不使用）。
 * @property isIntrinsic 是否带 `@Intrinsic`（官方是注解而非关键字修饰符）。
 * @property isDeclarationFile 是否 `.cj.d`（官方 `parseDeclFile`）。
 * @property isSignatureBroken 签名是否已经残缺到不应再叠报缺体：
 *   函数侧对位官方 `HAS_BROKEN`（缺参数列表）；属性侧近似官方"类型诊断已占用同一
 *   lookahead token"的去重效果（官方属性过程不读 `HAS_BROKEN`）。
 * @property cjmp 1.1.3 CJMP 例外事实；由 [CjMemberBodyPolicy.applyCjmpOverlay] 消费，主体过程不读取。
 */
data class CjMemberBodyFacts(
    val scopeKind: CjScopeKind,
    val hasBody: Boolean,
    val hasAccessors: Boolean = false,
    val hasReturnType: Boolean = false,
    val isForeign: Boolean = false,
    val isIntrinsic: Boolean = false,
    val isDeclarationFile: Boolean = false,
    val isSignatureBroken: Boolean = false,
    val cjmp: CjmpOverlayFacts? = null,
)

/**
 * 缺体判定的结果。
 *
 * @property markImplicitAbstract 是否应把声明视为隐式 `abstract`。
 * @property diagnostic parser 侧应上报的诊断；`null` 表示不报。
 */
data class CjMemberBodyDecision(
    val markImplicitAbstract: Boolean,
    val diagnostic: CjMemberBodyDiagnostic?,
)

/**
 * parser 缺体诊断种类。与官方 `DiagKindRefactor` 一一对应，**不是**消息 key。
 */
enum class CjMemberBodyDiagnostic {
    /** 官方 `DiagMissingBody("function", ...)` → `parse_missing_body`。 */
    MISSING_FUNCTION_BODY,

    /** 官方 `DiagMissingPropertyBody` → `parse_expected_character '{'` + note。 */
    MISSING_PROPERTY_BODY,

    /** 官方 `parse_abstract_func_must_have_return_type`。 */
    ABSTRACT_FUNCTION_MUST_HAVE_RETURN_TYPE,
}

/**
 * cjc 1.1.3 CJMP 隐式 abstract 例外事实（仅 `class` 体）。
 *
 * - `ParserImpl::CanBeAbstract`：class 体内 common 成员不隐式 abstract；
 * - `CheckClassLikeFuncBodyAbstractness` / `CheckClassLikePropAbstractness`：
 *   common/specific abstract class 中未显式写 `abstract` 的函数或属性不是抽象成员。
 *
 * 两个后置过程是官方 1.1.3 的函数 / 属性双份实现，规则一致；本 overlay 因此不按
 * 成员种类分支。它们只清除隐式 `ABSTRACT` 属性，不回退 parser 侧返回类型诊断。
 *
 * @property memberIsCommon 成员自身是否 `common`。
 * @property memberIsExplicitAbstract 成员自身是否显式 `abstract`。
 * @property enclosingClassIsAbstract 外层 class 是否 `abstract`。
 * @property enclosingClassIsCommonOrSpecific 外层 class 是否 `common` / `specific`。
 */
data class CjmpOverlayFacts(
    val memberIsCommon: Boolean,
    val memberIsExplicitAbstract: Boolean,
    val enclosingClassIsAbstract: Boolean,
    val enclosingClassIsCommonOrSpecific: Boolean,
)

/**
 * 成员缺体判定的一等模型：**一个规则载体、两个过程**。
 *
 * 官方是两段独立的解析过程，不是一个布尔量：
 * - 函数：`ParserImpl::CheckFuncBody`（`external/cangjie_compiler/src/Parse/ParseDecl.cpp:1533-1562`）
 * - 属性：`ParserImpl::ParsePropDecl`（`external/cangjie_compiler/src/Parse/Parser.cpp:340-394`）
 *
 * 两者在 `FOREIGN` / `INTRINSIC` / `UNKNOWN_SCOPE` / `parseDeclFile` / `HAS_BROKEN`
 * / 返回类型六个因子上**不对称**。本对象按官方拆分提供两个入口，二者永不互调、
 * 互不引用；把任一过程"统一"到另一个上都会制造回归。
 *
 * 纯函数：无状态、无配置、无版本分支。CJMP 例外放在 [applyCjmpOverlay]，
 * 使 1.0.x 路径上的主体保持零 CJMP 概念。
 *
 * 消费方：
 * - parser 诊断适配器（`CangJieParsing.reportMissingFunctionBody` /
 *   `CangJieParsing.reportMissingPropertyBody`）；
 * - `psi2cfir` / `light-tree2cfir` 的隐式 abstract 提取；
 * - `cfir:resolve` 的 STATUS 模态解析（以 raw `isAbstract` 为准，不再按 body 重推导）。
 */
object CjMemberBodyPolicy {

    /**
     * 函数缺体过程，对位官方 `CheckFuncBody`。
     *
     * ```cpp
     * const bool isMember = fb && !fb->body && !FOREIGN && !INTRINSIC &&
     *     scopeKind != ScopeKind::UNKNOWN_SCOPE;
     * if (isMember && !parseDeclFile) {
     *     if (scopeKind == CLASS_BODY || scopeKind == INTERFACE_BODY) {
     *         decl.EnableAttr(ABSTRACT);
     *         if (!retType && !HAS_BROKEN) parse_abstract_func_must_have_return_type;
     *     } else {
     *         if (!HAS_BROKEN) DiagMissingBody("function", ...);
     *     }
     * }
     * ```
     */
    fun checkFunctionBody(f: CjMemberBodyFacts): CjMemberBodyDecision {
        val isMember = !f.hasBody &&
                !f.isForeign &&
                !f.isIntrinsic &&
                f.scopeKind != CjScopeKind.UNKNOWN_SCOPE
        // 官方 `isMember && !parseDeclFile` 罩住整个分支：声明文件连"抽象函数缺返回类型"也不报。
        if (!isMember || f.isDeclarationFile) return CjMemberBodyDecision(false, null)

        return when (f.scopeKind) {
            CjScopeKind.CLASS_BODY, CjScopeKind.INTERFACE_BODY -> CjMemberBodyDecision(
                markImplicitAbstract = true,
                diagnostic = if (!f.hasReturnType && !f.isSignatureBroken) {
                    CjMemberBodyDiagnostic.ABSTRACT_FUNCTION_MUST_HAVE_RETURN_TYPE
                } else {
                    null
                },
            )
            else -> CjMemberBodyDecision(
                markImplicitAbstract = false,
                diagnostic = if (!f.isSignatureBroken) {
                    CjMemberBodyDiagnostic.MISSING_FUNCTION_BODY
                } else {
                    null
                },
            )
        }
    }

    /**
     * 属性缺体过程，对位官方 `ParsePropDecl`。
     *
     * ```cpp
     * if (Skip(LCURL)) {
     *     ParsePropBody(...);
     * } else if (scopeKind == CLASS_BODY || scopeKind == INTERFACE_BODY ||
     *     scopeKind == UNKNOWN_SCOPE) {
     *     ret->EnableAttr(ABSTRACT);
     * } else {
     *     DiagMissingPropertyBody(*ret);
     * }
     * ```
     *
     * 与函数过程的三处不对称（都是规格本身）：
     * 1. **没有** `FOREIGN` / `INTRINSIC` 因子；
     * 2. `UNKNOWN_SCOPE` 是**标记**而不是沉默；
     * 3. `parseDeclFile` 只门诊断，不门隐式 abstract 标记。
     *
     * ⚠️ 版本注记：1.1.3 的 `CanBeAbstract` 对 `UNKNOWN_SCOPE` 返回 false；本仓保留
     * 1.0.x 的标记语义，因为 CFIR 路径总在 MACRO_EXPAND 后以已知作用域重建，parser
     * 侧该分支不产生诊断。
     */
    fun checkPropertyBody(f: CjMemberBodyFacts): CjMemberBodyDecision {
        if (f.hasBody || f.hasAccessors) return CjMemberBodyDecision(false, null)

        return when (f.scopeKind) {
            CjScopeKind.CLASS_BODY,
            CjScopeKind.INTERFACE_BODY,
            CjScopeKind.UNKNOWN_SCOPE,
                -> CjMemberBodyDecision(markImplicitAbstract = true, diagnostic = null)
            else -> CjMemberBodyDecision(
                markImplicitAbstract = false,
                diagnostic = if (!f.isDeclarationFile && !f.isSignatureBroken) {
                    CjMemberBodyDiagnostic.MISSING_PROPERTY_BODY
                } else {
                    null
                },
            )
        }
    }

    /**
     * CJMP 隐式 abstract 后置修正，对应官方 1.1.3 的旁路地位。
     *
     * 只在 [CjMemberBodyDecision.markImplicitAbstract] 为 true 时生效；facts 为 `null`
     * 表示不在 `class` 体（interface/struct/enum/extend/top-level），原样返回。
     *
     * 官方把同一例外写了函数 / 属性两份（`CheckClassLikeFuncBodyAbstractness` /
     * `CheckClassLikePropAbstractness`），规则一致，故本函数不接收成员种类参数。
     * `CanBeAbstract` 的 common 规则同样对函数与属性都生效。
     */
    fun applyCjmpOverlay(
        decision: CjMemberBodyDecision,
        facts: CjMemberBodyFacts,
    ): CjMemberBodyDecision {
        val cjmp = facts.cjmp ?: return decision
        if (!decision.markImplicitAbstract) return decision
        if (cjmp.memberIsCommon) return decision.copy(markImplicitAbstract = false)
        if (!cjmp.memberIsExplicitAbstract &&
            cjmp.enclosingClassIsAbstract &&
            cjmp.enclosingClassIsCommonOrSpecific
        ) {
            return decision.copy(markImplicitAbstract = false)
        }
        return decision
    }
}
