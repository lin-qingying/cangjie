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

import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * enum constructor payload 的 use-site 语义工具。
 *
 * 这里基于 `valueParameters` 进行 owner enum 类型实参替换，
 * 让调用检查与模式匹配共享同一套 payload 推导规则。
 */
fun CfirEnumConstructor.substitutedPayloadParameterTypes(
    enumDeclaration: CfirEnum,
    enumType: ConeEnumType,
): List<ConeCangJieType> {
    val payloadTypes = payloadParameterTypesOrEmpty()
    if (payloadTypes.isEmpty()) return payloadTypes
    if (enumDeclaration.typeParameters.isEmpty()) return payloadTypes

    val replacements: Map<TypeConstructorMarker, ConeCangJieType> =
        enumDeclaration.typeParameters.mapIndexedNotNull { index, parameter ->
        enumType.typeArguments.getOrNull(index)?.type?.let { substituted ->
            parameter.symbol.toLookupTag() to substituted
        }
    }.toMap()
    if (replacements.isEmpty()) return payloadTypes

    val substitutor = CfirTypeSubstitutorByMap(replacements)
    return payloadTypes.map(substitutor::substituteOrSelf)
}
