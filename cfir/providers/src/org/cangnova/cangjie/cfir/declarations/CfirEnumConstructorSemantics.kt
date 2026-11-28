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

import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 根据目标类型为无参 enum constructor 计算其完整的 use-site 类型。
 *
 * 无参 enum case 在表达式位置可以省略 owner 的类型实参，但只有目标类型确实是
 * 同一个 enum（或标准库 Option）时才允许这样定型。该判断位于 providers 共享层，
 * 让 resolve 与 checker 使用同一条 owner-identification 规则，避免默认参数、返回值
 * 和普通调用分别实现一套容易漂移的匹配逻辑。
 */
fun CfirEnumConstructorSymbol.noArgEnumConstructorTargetType(
    expectedType: ConeCangJieType,
    session: CfirSession,
): ConeCangJieType? {
    val constructor = cfir
    if (constructor.valueParameters.isNotEmpty()) return null
    val ownerClassId = session.cfirProvider.getContainingClass(this)?.classId ?: return null
    val expandedExpectedType = expectedType.fullyExpandedType(session)
    if (expandedExpectedType.containsErrorType()) return null
    val expectedOwnerClassId = when (expandedExpectedType) {
        is ConeEnumType -> expandedExpectedType.classId
        is ConeClassLikeType -> expandedExpectedType.classId.takeIf { it == StdlibClassIds.Option }
        else -> null
    }
    return expandedExpectedType.takeIf { expectedOwnerClassId == ownerClassId }
}

/**
 * 返回无参 enum case 的限定符已经确定的 owner 类型。
 *
 * `Option<Int64>.None` 和具体枚举别名已经完成 owner 实例化，外层目标只能检查该类型，
 * 不能重新定型。裸泛型限定符仍引用其声明参数，才允许由上下文补全。
 */
fun CfirEnumConstructorSymbol.instantiatedQualifierOwnerType(
    qualifier: CfirExpression?,
    session: CfirSession,
): ConeCangJieType? {
    val qualifierType = qualifier?.instantiatedTypeQualifierTypeOrNull(session) ?: return null
    return noArgEnumConstructorTargetType(qualifierType, session)
}

/**
 * 类型限定符表达式已经完整实例化时返回它的展开类型，否则返回 null。
 *
 * `Slot<Int64>`、`type IntSlot = Slot<Int64>` 的 `IntSlot` 都已确定全部类型实参；裸泛型限定符
 * `Slot` / `SlotAlias` 仍引用其声明参数，实参由使用处的上下文决定，此时返回 null。比较参数
 * symbol 而非名称，以保留 `Option<T>.None` 中来自外层声明的真实 T。
 *
 * 表达式不是已解析的 class-like / typealias 限定符、显式类型实参个数与声明不符、或类型含错误时
 * 同样返回 null。
 */
fun CfirExpression.instantiatedTypeQualifierTypeOrNull(session: CfirSession): ConeCangJieType? {
    val access = this as? CfirQualifiedAccessExpression ?: return null
    val classifier = (access.calleeReference as? CfirResolvedNamedReference)
        ?.resolvedSymbol as? CfirClassLikeSymbol<*> ?: return null
    val parameters = classifier.cfir.typeParameters
    val explicitTypeArguments = access.typeArguments.filter { argument ->
        argument.source?.let { it.kind !is CjFakeSourceElementKind.ImplicitTypeArgument } == true
    }
    if (explicitTypeArguments.isNotEmpty() && explicitTypeArguments.size != parameters.size) return null
    val qualifierType = access.coneTypeOrNull?.fullyExpandedType(session) ?: return null
    if (qualifierType.containsErrorType()) return null
    if (explicitTypeArguments.isEmpty()) {
        val parameterSymbols = parameters.mapTo(hashSetOf()) { it.symbol }
        if (qualifierType.contains { type ->
                when (type) {
                    is ConeTypeParameterType -> type.lookupTag.typeParameterSymbol in parameterSymbols
                    is ConeTypeVariableType ->
                        (type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)
                            ?.typeParameterSymbol in parameterSymbols
                    else -> false
                }
            }
        ) return null
    }
    return qualifierType
}

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

/**
 * pattern 语境下的 enum subject 类型归一化。
 *
 * 官方语义中 `VarOrEnumPattern` 依赖当前目标类型和 enum 构造器表决定裸名是
 * 构造器模式还是变量绑定；本地 CFIR 类型系统中，部分 enum use-site 类型可能先
 * 以普通 class-like 形态流入 checker/resolve。因此所有 pattern 路径都应先通过
 * 统一的符号查询把实际指向 [CfirEnumSymbol] 的类型恢复为 [ConeEnumType]。
 */
fun ConeCangJieType.expandedPatternEnumType(session: CfirSession): ConeEnumType? = when (this) {
    is ConeEnumType -> this
    is ConeClassLikeType -> {
        val enumSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? CfirEnumSymbol
        if (enumSymbol == null) null else ConeEnumType(lookupTag, typeArguments, attributes, enumSymbol.isRefEnum)
    }

    is ConeErrorType -> delegatedType?.expandedPatternEnumType(session)
    is ConeTypeAliasType -> expandedType?.expandedPatternEnumType(session)
    else -> null
}

/**
 * enum pattern 的 constructor 短名。
 *
 * 限定名模式的 owner 由 [CfirEnumPattern.ownerQualifier] 单独承载，引用里只有 `.` 之后的名字；
 * 语法恢复产生的占位引用（special name）返回 null。
 */
fun CfirEnumPattern.constructorNameOrNull(): Name? =
    (constructorReference as? CfirNamedReference)?.name?.takeUnless { name -> name.isSpecial }

/**
 * 判断 enum pattern 的 owner 限定符是否指向 subject 类型。
 *
 * 裸名字模式没有限定符，owner 就是 subject，恒为 true。限定名模式（`E.A` / `Alias<Int64>.B(x)`）
 * 以 resolve 阶段解析出的限定符为准，对齐官方 `ChkEnumPattern` 对 constructor 类型与 selector
 * 类型的相等检查（`external/cangjie_compiler/src/Sema/TypeCheckPattern.cpp`）：
 * - 限定符（typealias 展开后）必须是 subject 的同一个类型声明；
 * - 限定符已完整实例化时（显式类型实参，或别名固定了实参），各类型实参必须与 subject 相等；
 *   裸泛型限定符 `Slot.Used(x)` 的实参由 subject 补全。
 *
 * 限定符未能解析为 class-like 类型时返回 false：它自身的错误由限定符表达式报告。
 */
fun CfirEnumPattern.ownerQualifierMatchesSubject(subjectType: ConeCangJieType, session: CfirSession): Boolean {
    val qualifier = ownerQualifier ?: return true
    val ownerClassifier = qualifier.resolvedQualifierClassifier(session) ?: return false
    val subject = subjectType.fullyExpandedType(session)
    if (ownerClassifier.classId != subject.classIdOrPrimitiveClassId) return false

    val qualifierType = qualifier.instantiatedTypeQualifierTypeOrNull(session) ?: return true
    return qualifierType.typeArguments.size == subject.typeArguments.size &&
        qualifierType.typeArguments.zip(subject.typeArguments).all { (qualifierArgument, subjectArgument) ->
            AbstractTypeChecker.equalTypes(session.typeContext, qualifierArgument.type, subjectArgument.type)
        }
}
