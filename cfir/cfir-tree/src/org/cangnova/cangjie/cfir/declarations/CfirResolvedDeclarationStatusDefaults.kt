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
        isCommon = false
        isSpecific = false
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

    return buildResolvedDeclarationStatus {
        source = this@resolvedForStatuslessDeclaration.source
        visibility = this@resolvedForStatuslessDeclaration.visibility
        isVisibilityExplicit = this@resolvedForStatuslessDeclaration.isVisibilityExplicit
        isModalityExplicit = this@resolvedForStatuslessDeclaration.isModalityExplicit
        isAbstractExplicit = this@resolvedForStatuslessDeclaration.isAbstractExplicit
        isOverride = this@resolvedForStatuslessDeclaration.isOverride
        isOperator = this@resolvedForStatuslessDeclaration.isOperator
        isStatic = this@resolvedForStatuslessDeclaration.isStatic
        isConst = this@resolvedForStatuslessDeclaration.isConst
        isMut = this@resolvedForStatuslessDeclaration.isMut
        isUnsafe = this@resolvedForStatuslessDeclaration.isUnsafe
        isForeign = this@resolvedForStatuslessDeclaration.isForeign
        isCommon = this@resolvedForStatuslessDeclaration.isCommon
        isSpecific = this@resolvedForStatuslessDeclaration.isSpecific
        isRedef = this@resolvedForStatuslessDeclaration.isRedef
        isDefault = this@resolvedForStatuslessDeclaration.isDefault
        isAbstract = this@resolvedForStatuslessDeclaration.isAbstract
        isOpen = this@resolvedForStatuslessDeclaration.isOpen
        isSealed = this@resolvedForStatuslessDeclaration.isSealed
        modality = this@resolvedForStatuslessDeclaration.modality ?: Modality.FINAL
    }
}
