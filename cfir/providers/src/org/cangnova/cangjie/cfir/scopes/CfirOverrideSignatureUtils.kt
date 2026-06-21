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

package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 成员 override 签名的 providers 层表示。
 *
 * Kotlin FIR 通过 `FirOverrideChecker` 在 use-site scope 中判断声明与父成员的覆盖关系；
 * 当前 CFIR 尚未抽出完整 override checker，因此 providers / checkers 共用这个签名入口，
 * 避免不同阶段各自用不同规则判断“同一成员签名”。
 */
fun CfirCallableSymbol<*>.overrideSignatureKey(): String {
    if (!isBound) return callableIdAsString()

    return when (val declaration = cfir) {
        is CfirFunction -> {
            val typeParameterPart = "#tp${declaration.typeParameters.size}"
            val ownTypeParameterIndices = declaration.typeParameters
                .mapIndexed { index, typeParameter -> typeParameter.symbol.toLookupTag() as TypeConstructorMarker to index }
                .toMap()
            val parameterPart = declaration.valueParameters.joinToString(
                prefix = "(",
                postfix = ")",
                separator = ",",
            ) { parameter ->
                parameter.returnTypeRef.toOverrideSignatureComponent(ownTypeParameterIndices)
            }
            "fun:${name.asString()}$typeParameterPart$parameterPart"
        }

        is CfirProperty -> {
            // Kotlin FIR 的 property override 关系不把返回类型放进签名；
            // 类型差异由 override checker 单独报告 PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF。
            "prop:${name.asString()}"
        }
        else -> callableIdAsString()
    }
}

fun CfirCallableSymbol<*>.isStaticMemberForOverride(): Boolean =
    isBound && cfir.status.isStatic

private fun CfirTypeRef.toOverrideSignatureComponent(
    ownTypeParameterIndices: Map<TypeConstructorMarker, Int>,
): String = when (this) {
    is CfirResolvedTypeRef -> coneType.toOverrideSignatureComponent(ownTypeParameterIndices)
    else -> toString()
}

private fun ConeTypeProjection.toOverrideSignatureComponent(
    ownTypeParameterIndices: Map<TypeConstructorMarker, Int>,
): String = type.toOverrideSignatureComponent(ownTypeParameterIndices)

private fun ConeCangJieType.toOverrideSignatureComponent(
    ownTypeParameterIndices: Map<TypeConstructorMarker, Int>,
): String {
    if (this is ConeTypeParameterType) {
        val ownIndex = ownTypeParameterIndices[lookupTag as TypeConstructorMarker]
        if (ownIndex != null) return "tp#$ownIndex"
    }

    return when (this) {
        is ConeTypeParameterType -> "typeParameter:${lookupTag.name}"
        is ConePrimitiveType -> "primitive:${kind.name}"
        is ConeClassLikeType -> {
            val marker = when {
                isThisType -> ":this"
                isInterface -> ":interface"
                else -> ""
            }
            "class:${classId.asString()}$marker${typeArguments.toOverrideSignatureComponent(ownTypeParameterIndices)}"
        }

        is ConeStructType ->
            "struct:${classId.asString()}${typeArguments.toOverrideSignatureComponent(ownTypeParameterIndices)}"

        is ConeEnumType -> {
            val marker = if (isRefEnum) ":ref" else ""
            "enum:${classId.asString()}$marker${typeArguments.toOverrideSignatureComponent(ownTypeParameterIndices)}"
        }

        is ConeTypeAliasType -> {
            // Override 签名比较使用展开后的真实语义类型；typealias 不是独立的 override/overload 键。
            val expanded = expandedType
            if (expanded != null) {
                expanded.toOverrideSignatureComponent(ownTypeParameterIndices)
            } else {
                "alias:${classId.asString()}${typeArguments.toOverrideSignatureComponent(ownTypeParameterIndices)}"
            }
        }

        is ConeFunctionType -> {
            val parameters = parameterTypes.joinToString(
                prefix = "(",
                postfix = ")",
                separator = ",",
            ) { it.toOverrideSignatureComponent(ownTypeParameterIndices) }
            val flags = buildString {
                if (isCFunc) append(":c")
                if (isClosureType) append(":closure")
                if (hasVariableLenArg) append(":vararg")
            }
            "function$flags$parameters->${returnType.toOverrideSignatureComponent(ownTypeParameterIndices)}"
        }

        is ConeTupleType ->
            elementTypes.joinToString(prefix = "tuple(", postfix = ")", separator = ",") {
                it.toOverrideSignatureComponent(ownTypeParameterIndices)
            }

        is ConePointerType -> "pointer:${pointeeType.toOverrideSignatureComponent(ownTypeParameterIndices)}"
        is ConeCStringType -> "cstring"
        is ConeVArrayType -> "varray:${elementType.toOverrideSignatureComponent(ownTypeParameterIndices)}#$size"
        is ConeIntersectionType ->
            intersectedTypes.map { it.toOverrideSignatureComponent(ownTypeParameterIndices) }
                .sorted()
                .joinToString(prefix = "intersection(", postfix = ")", separator = "&")

        is ConeUnionType ->
            unionTypes.map { it.toOverrideSignatureComponent(ownTypeParameterIndices) }
                .sorted()
                .joinToString(prefix = "union(", postfix = ")", separator = "|")

        is ConeQuestType -> "quest"
        ConeAnyType -> "any"
        is ConeErrorType -> delegatedType
            ?.toOverrideSignatureComponent(ownTypeParameterIndices)
            ?: renderForDebugging()

        else -> renderForDebugging()
    }
}

private fun List<ConeTypeProjection>.toOverrideSignatureComponent(
    ownTypeParameterIndices: Map<TypeConstructorMarker, Int>,
): String {
    if (isEmpty()) return ""
    return joinToString(prefix = "<", postfix = ">", separator = ",") {
        it.toOverrideSignatureComponent(ownTypeParameterIndices)
    }
}
