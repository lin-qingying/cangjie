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

package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.renderStableSemanticKey
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/** 为 extend 类型引用生成稳定语义 key 的归一化器。 */
internal class CfirExtendTypeSemanticNormalizer(
    /** 当前正在建立语义 key 的 extend 声明。 */
    extend: CfirExtend,
) {
    /**
     * extend 语义键不能只保留“第几个类型参数”，还要把约束信息编码进去。
     *
     * 否则 `extend<T where T <: A>` 与 `extend<T where T <: B>` 在接口形状相同的情况下
     * 会退化成同一个稳定键，导致复杂特化冲突被错误忽略。
     */
    private val substitutor = run {
        val baseSubstitutor = CfirTypeSubstitutorByMap(
            buildMap<TypeConstructorMarker, ConeCangJieType> {
                extend.typeParameters.forEachIndexed { index, typeParameter ->
                    put(typeParameter.symbol.toLookupTag(), ConePlaceholderType("__EXT_TP_$index"))
                }
            },
        )
        val boundFingerprints = buildMap<TypeConstructorMarker, ConeCangJieType> {
            extend.typeParameters.forEachIndexed { index, typeParameter ->
                val fingerprint = typeParameter.bounds
                    .mapNotNull { boundTypeRef ->
                        val boundType = (boundTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
                        baseSubstitutor.substituteOrSelf(boundType).renderStableSemanticKey()
                    }
                    .sorted()
                    .joinToString(separator = "&")
                val debugName = if (fingerprint.isEmpty()) {
                    "__EXT_TP_$index"
                } else {
                    "__EXT_TP_${index}_BOUNDS[$fingerprint]"
                }
                put(typeParameter.symbol.toLookupTag(), ConePlaceholderType(debugName))
            }
        }
        CfirTypeSubstitutorByMap(boundFingerprints)
    }

    /** 从已解析类型引用中生成语义 key；未解析类型返回 null。 */
    fun semanticKeyOrNull(typeRef: CfirTypeRef): String? {
        val coneType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return null
        return semanticKeyOrNull(coneType)
    }

    /** 从 Cone 类型生成语义 key。 */
    fun semanticKeyOrNull(type: ConeCangJieType): String =
        canonicalize(type).renderStableSemanticKey()

    /** 把 extend 类型参数替换为带 bounds fingerprint 的 placeholder 类型。 */
    private fun canonicalize(type: ConeCangJieType): ConeCangJieType {
        return substitutor.substituteOrSelf(type)
    }
}
