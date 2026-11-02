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

package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities

/**
 * 对齐 Kotlin FIR 的 statusless declaration 约定。
 *
 * `CfirValueParameter` 等声明虽然在当前树模型中仍携带 `status` 字段，
 * 但它们不参与常规的 status 推导，运行期必须直接落到稳定的 resolved status。
 */
val DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS: CfirResolvedDeclarationStatus =
    buildResolvedDeclarationStatus {
        visibility = Visibilities.Public
        visibilityExplicitness = CfirModifierExplicitness.UNKNOWN
        modalityExplicitness = CfirModifierExplicitness.UNKNOWN
        bodyPresence = CfirBodyPresence.UNKNOWN
        isDeclarationFile = false
        isVisibilityExplicit = false
        isModalityExplicit = false
        isAbstractExplicit = false
        isOverride = false
        isOperator = false
        isStatic = false
        isConst = false
        isMut = false
        isUnsafe = false
        isForeign = false
        isC = false
        isCommon = false
        isFromCommonPart = false
        isSpecific = false
        isCommonWithDefault = false
        isRedef = false
        isDefault = false
        isAbstract = false
        isOpen = false
        isSealed = false
        modality = Modality.FINAL
    }

/**
 * 将当前 status 收敛为 statusless declaration 可发布的 resolved 形态。
 *
 * 保留既有 flags/source/visibility，仅在历史建树路径未初始化 `modality`
 * 时回收为 Kotlin FIR 同款的 `FINAL` 约定。
 */
fun CfirDeclarationStatus.resolvedForStatuslessDeclaration(): CfirResolvedDeclarationStatus {
    if (this is CfirResolvedDeclarationStatus) return this

    return copyResolvedStatus(modality ?: Modality.FINAL)
}

/** 将已有有效模态的状态复制为 resolved 形态，不重新推导库声明的语义。 */
fun CfirDeclarationStatus.resolvedWithEffectiveModality(): CfirResolvedDeclarationStatus {
    if (this is CfirResolvedDeclarationStatus) return this

    return copyResolvedStatus(checkNotNull(modality) { "Effective modality is required for a resolved status" })
}

private fun CfirDeclarationStatus.copyResolvedStatus(effectiveModality: Modality): CfirResolvedDeclarationStatus {
    return buildResolvedDeclarationStatus {
        source = this@copyResolvedStatus.source
        visibility = this@copyResolvedStatus.visibility
        visibilityExplicitness = this@copyResolvedStatus.visibilityExplicitness
        modalityExplicitness = this@copyResolvedStatus.modalityExplicitness
        bodyPresence = this@copyResolvedStatus.bodyPresence
        isDeclarationFile = this@copyResolvedStatus.isDeclarationFile
        isVisibilityExplicit = this@copyResolvedStatus.isVisibilityExplicit
        isModalityExplicit = this@copyResolvedStatus.isModalityExplicit
        isAbstractExplicit = this@copyResolvedStatus.isAbstractExplicit
        isOverride = this@copyResolvedStatus.isOverride
        isOperator = this@copyResolvedStatus.isOperator
        isStatic = this@copyResolvedStatus.isStatic
        isConst = this@copyResolvedStatus.isConst
        isMut = this@copyResolvedStatus.isMut
        isUnsafe = this@copyResolvedStatus.isUnsafe
        isForeign = this@copyResolvedStatus.isForeign
        isC = this@copyResolvedStatus.isC
        isCommon = this@copyResolvedStatus.isCommon
        isFromCommonPart = this@copyResolvedStatus.isFromCommonPart
        isSpecific = this@copyResolvedStatus.isSpecific
        isCommonWithDefault = this@copyResolvedStatus.isCommonWithDefault
        isRedef = this@copyResolvedStatus.isRedef
        isDefault = this@copyResolvedStatus.isDefault
        isAbstract = this@copyResolvedStatus.isAbstract
        isOpen = this@copyResolvedStatus.isOpen
        isSealed = this@copyResolvedStatus.isSealed
        modality = effectiveModality
    }
}
