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

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities

/**
 * 声明模态、可见性与成员覆盖关系的共享解析 owner，对位 Kotlin `FirStatusResolver`。
 *
 * 当前仓颉 tree 没有 Kotlin 的 `Unknown visibility/modality` raw 状态。raw builder 已按仓颉语法
 * 把普通声明缺省可见性物化为 `internal`、interface 成员物化为 `public`；STATUS 只能解析模态等
 * 尚未稳定的事实，不能把未显式写可见性的 override/redef 改写成父成员可见性。
 */
class CfirStatusResolver(
    /** 查询符号、scope 和可见性规则时使用的会话。 */
    private val session: CfirSession,
    /** 查询成员 scope 时复用的 scope 缓存会话。 */
    private val scopeSession: ScopeSession,
) {

    /** 对位 FirStatusResolver 的声明级分发；body resolver 与全局 STATUS 共用状态推导 owner。 */
    fun resolveStatus(
        declaration: CfirDeclaration,
        containingClass: CfirClassLikeDeclaration?,
        containingProperty: CfirProperty?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus = when (declaration) {
        is CfirProperty -> resolveStatus(declaration, containingClass, isLocal,
            getOverriddenProperties(declaration, containingClass).map { it.status as CfirResolvedDeclarationStatus })
        is CfirNamedFunction -> resolveStatus(declaration, containingClass, isLocal,
            getOverriddenFunctions(declaration, containingClass).map { it.status as CfirResolvedDeclarationStatus })
        is CfirPropertyAccessor -> resolveStatus(declaration, containingClass, containingProperty, isLocal)
        is CfirClass -> resolveStatus(declaration, containingClass, isLocal)
        is CfirInterface -> resolveStatus(declaration, containingClass, isLocal)
        is CfirStruct -> resolveStatus(declaration, containingClass, isLocal)
        is CfirEnum -> resolveStatus(declaration, containingClass, isLocal)
        is CfirTypeAlias -> resolveStatus(declaration, containingClass, isLocal)
        is CfirConstructor -> resolveStatus(declaration, containingClass, isLocal)
        is CfirEnumConstructor -> resolveStatus(declaration, containingClass, isLocal)
        is CfirFunction -> resolveStatus(declaration, containingClass, isLocal)
        is CfirVariable -> resolveStatus(declaration, containingClass, isLocal)
        is CfirExtend -> resolveStatus(declaration, containingClass, isLocal)
        else -> error("Unsupported declaration status owner: ${declaration::class}")
    }

    /** 查询属性在当前 class-like 容器中的直接覆盖属性。 */
    fun getOverriddenProperties(
        property: CfirProperty,
        containingClass: CfirClassLikeDeclaration?,
    ): List<CfirProperty> {
        val scope = containingClass?.unsubstitutedScope(
            useSiteSession = session,
            scopeSession = scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = null,
        ) ?: return emptyList()
        val result = linkedSetOf<CfirProperty>()
        scope.processDirectOverriddenPropertiesWithBaseScope(property.symbol) { overriddenSymbol, _ ->
            result += overriddenSymbol.cfir
            ProcessorAction.NEXT
        }
        return result.toList()
    }

    /** 查询具名函数在当前 class-like 容器中的直接覆盖函数。 */
    fun getOverriddenFunctions(
        function: CfirNamedFunction,
        containingClass: CfirClassLikeDeclaration?,
    ): List<CfirNamedFunction> {
        val scope = containingClass?.unsubstitutedScope(
            useSiteSession = session,
            scopeSession = scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = null,
        ) ?: return emptyList()
        val result = linkedSetOf<CfirNamedFunction>()
        scope.processDirectOverriddenFunctionsWithBaseScope(function.symbol) { overriddenSymbol, _ ->
            result += overriddenSymbol.cfir
            ProcessorAction.NEXT
        }
        return result.toList()
    }

    /** 计算 class 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirClass,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 interface 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirInterface,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 struct 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirStruct,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 enum 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirEnum,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 typealias 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirTypeAlias,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算非具名函数类声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirFunction,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算普通构造器声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirConstructor,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 enum constructor 声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirEnumConstructor,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 根据覆盖属性状态计算属性声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirProperty,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    /** 根据覆盖函数状态计算具名函数的 resolved status。 */
    fun resolveStatus(
        declaration: CfirNamedFunction,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    /** 根据所属属性和覆盖访问器状态计算属性访问器的 resolved status。 */
    fun resolveStatus(
        declaration: CfirPropertyAccessor,
        containingClass: CfirClassLikeDeclaration?,
        containingProperty: CfirProperty?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus> = emptyList(),
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = containingProperty,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    /** 计算变量类声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirVariable,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /** 计算 extend 容器声明的 resolved status。 */
    fun resolveStatus(
        declaration: CfirExtend,
        containingClass: CfirClassLikeDeclaration?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    /**
     * resolved status 的统一构造入口。
     *
     * 该函数保留 raw status 中的显式修饰符标志，并按声明种类、容器和覆盖状态补齐默认可见性与模态。
     */
    private fun resolveStatus(
        declaration: CfirDeclaration,
        status: CfirDeclarationStatus,
        containingClass: CfirClassLikeDeclaration?,
        containingProperty: CfirProperty?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus> = emptyList(),
    ): CfirResolvedDeclarationStatus {
        if (status is CfirResolvedDeclarationStatus) return status

        declaration.publishInteropInfo(session)

        if (declaration.origin == CfirDeclarationOrigin.Library) {
            return buildResolvedDeclarationStatus {
                source = status.source
                visibility = status.visibility
                visibilityExplicitness = status.visibilityExplicitness
                modalityExplicitness = status.modalityExplicitness
                bodyPresence = status.bodyPresence
                isDeclarationFile = status.isDeclarationFile
                isVisibilityExplicit = status.isVisibilityExplicit
                isModalityExplicit = status.isModalityExplicit
                isAbstractExplicit = status.isAbstractExplicit
                isOverride = status.isOverride
                isOperator = status.isOperator
                isStatic = status.isStatic
                isConst = status.isConst
                isMut = status.isMut
                isUnsafe = status.isUnsafe
                isForeign = status.isForeign
                isC = status.isC
                isCommon = status.isCommon
                isFromCommonPart = status.isFromCommonPart
                isSpecific = status.isSpecific
                isCommonWithDefault = status.isCommonWithDefault
                isRedef = status.isRedef
                isDefault = status.isDefault
                isAbstract = status.isAbstract
                isOpen = status.isOpen
                isSealed = status.isSealed
                modality = checkNotNull(status.modality) { "Library declaration must have an effective modality" }
            }
        }

        val visibility = if (status.isVisibilityExplicit) {
            status.visibility
        } else {
            resolveVisibility(declaration, status, containingProperty, overriddenStatuses, isLocal)
        }

        val effectiveFlags = effectiveStatusFlags(status, containingClass)

        val modality = if (effectiveFlags.isAbstract) {
            Modality.ABSTRACT
        } else if (effectiveFlags.modality != null) {
            effectiveFlags.modality
        } else {
            resolveModality(
                declaration,
                containingProperty,
                containingClass,
                effectiveFlags.isOverride,
            )
        }

        val resolvedIsAbstract = effectiveFlags.isAbstract ||
                declaration is CfirCallableDeclaration && modality == Modality.ABSTRACT

        return buildResolvedDeclarationStatus {
            source = status.source
            this.visibility = visibility
            visibilityExplicitness = status.visibilityExplicitness
            modalityExplicitness = effectiveFlags.modalityExplicitness
            bodyPresence = status.bodyPresence
            isDeclarationFile = status.isDeclarationFile
            isVisibilityExplicit = status.isVisibilityExplicit
            isModalityExplicit = effectiveFlags.modalityExplicitness == CfirModifierExplicitness.EXPLICIT
            isAbstractExplicit = status.isAbstractExplicit
            isOverride = effectiveFlags.isOverride
            isOperator = effectiveFlags.isOperator
            isStatic = effectiveFlags.isStatic
            isConst = status.isConst
            isMut = status.isMut
            isUnsafe = status.isUnsafe
            isForeign = status.isForeign
            isC = declaration.interopInfo?.isC == true
            isCommon = status.isCommon
            isFromCommonPart = status.isFromCommonPart
            isSpecific = status.isSpecific
            isCommonWithDefault = status.isCommonWithDefault
            isRedef = status.isRedef
            isDefault = status.isDefault
            isAbstract = resolvedIsAbstract
            isOpen = effectiveFlags.isOpen
            isSealed = status.isSealed
            this.modality = modality
        }
    }

    /**
     * 解析默认可见性。
     *
     * 局部声明固定为 local，访问器继承所属属性；其它声明保留 raw builder / deserializer
     * 已按仓颉语法写入的默认可见性。与 Kotlin 的 Unknown raw visibility 不同，仓颉 override/redef
     * 未写可见性时 raw status 已是 internal，不能继承父成员的 public/protected。
     */
    private fun resolveVisibility(
        declaration: CfirDeclaration,
        status: CfirDeclarationStatus,
        containingProperty: CfirProperty?,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
        isLocal: Boolean,
    ) = when {
        // 原生 MakeDefaultParamFunction 的 PRIVATE 是生成声明事实；局部名称作用域由 isLocal 表达。
        declaration.origin == CfirDeclarationOrigin.Synthetic.DefaultParameterHelper -> status.visibility
        isLocal -> Visibilities.Local
        declaration is CfirPropertyAccessor && containingProperty != null -> containingProperty.status.visibility
        else -> status.visibility
    }

    /**
     * 解析默认模态。
     *
     * class-like、顶层 callable、interface 成员和 override 成员各自遵循不同默认规则。
     */
    private fun resolveModality(
        declaration: CfirDeclaration,
        containingProperty: CfirProperty?,
        containingClass: CfirClassLikeDeclaration?,
        isOverride: Boolean,
    ): Modality {
        return when (declaration) {
            is CfirInterface -> Modality.ABSTRACT
            is CfirClass, is CfirStruct, is CfirEnum -> Modality.FINAL
            is CfirCallableDeclaration -> {
                val containingPropertyModality = containingProperty?.status?.modality
                when {
                    containingClass == null -> Modality.FINAL
                    declaration is CfirPropertyAccessor && containingPropertyModality != null -> containingPropertyModality
                    containingClass is CfirInterface -> {
                        when {
                            declaration.status.isStatic -> Modality.FINAL
                            declaration.status.visibility == Visibilities.Private -> Modality.FINAL
                            declaration.status.isAbstract -> Modality.ABSTRACT
                            else -> Modality.OPEN
                        }
                    }

                    isOverride && declaration is CfirFunction && declaration.status.bodyPresence == CfirBodyPresence.PRESENT ->
                        Modality.FINAL
                    isOverride && declaration.status.isDeclarationFile &&
                            declaration.status.bodyPresence == CfirBodyPresence.ABSENT -> Modality.FINAL
                    isOverride -> Modality.OPEN
                    else -> Modality.FINAL
                }
            }

            else -> Modality.FINAL
        }
    }

    /**
     * 将 raw declaration status 投影为后续语义阶段可消费的有效状态。
     *
     * Raw CFIR 忠实保留源码的全部 modifier，供 modifier checker 报告组合或目标错误；
     * STATUS phase 则移除不能共同形成成员语义的分派状态。继承与 open 消费 resolved status；
     * ABSTRACT 保持原生属性事实，由声明属性检查器判定缺体，不能作为组合冲突清除。
     */
    private fun effectiveStatusFlags(
        status: CfirDeclarationStatus,
        containingClass: CfirClassLikeDeclaration?,
    ): EffectiveStatusFlags {
        val incompatibleStaticOverride = status.isStatic && status.isOverride
        val incompatibleStaticOpen = status.isStatic && status.isOpen
        val incompatibleStaticOperator = status.isStatic && status.isOperator
        val incompatibleConstOpen = status.isConst && status.isOpen
        val incompatibleRedefOpen = status.isRedef && status.isOpen
        val effectiveStatic = status.isStatic &&
                !incompatibleStaticOverride &&
                !incompatibleStaticOpen &&
                !incompatibleStaticOperator
        val effectiveOverride = status.isOverride && !incompatibleStaticOverride
        // 原始 status 供冲突诊断使用；后续声明检查只能消费去除冲突项后的有效状态。
        val explicitOpenIsEffective = status.isOpen &&
                !incompatibleStaticOpen &&
                !incompatibleConstOpen &&
                !incompatibleRedefOpen
        // 官方 CheckInterfaceAttribute 为所有接口实例成员设置 OPEN，包括抽象成员。
        val effectiveOpen = explicitOpenIsEffective || containingClass is CfirInterface && !status.isStatic
        val effectiveOperator = status.isOperator && !incompatibleStaticOperator
        val effectiveAbstract = status.isAbstract
        // 显式性记录实际源码事实；非法 modifier 被移除语义后也不能改称源码省略了它。
        val effectiveModalityExplicitness = status.modalityExplicitness
        val effectiveModality = status.modality?.takeIf {
            effectiveModalityExplicitness == CfirModifierExplicitness.EXPLICIT &&
                    !(status.isOpen && !explicitOpenIsEffective || status.isAbstract && !effectiveAbstract)
        }

        return EffectiveStatusFlags(
            isStatic = effectiveStatic,
            isOverride = effectiveOverride,
            isOperator = effectiveOperator,
            isAbstract = effectiveAbstract,
            isOpen = effectiveOpen,
            modalityExplicitness = effectiveModalityExplicitness,
            modality = effectiveModality,
        )
    }

    /** STATUS phase 中 raw status 的有效成员修饰符视图。 */
    private data class EffectiveStatusFlags(
        val isStatic: Boolean,
        val isOverride: Boolean,
        val isOperator: Boolean,
        val isAbstract: Boolean,
        val isOpen: Boolean,
        val modalityExplicitness: CfirModifierExplicitness,
        val modality: Modality?,
    )
}

