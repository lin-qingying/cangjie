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

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
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
 * 不能重新定型。裸泛型限定符仍引用其声明参数，才允许由上下文补全；比较参数 symbol
 * 而非名称，以保留 `Option<T>.None` 中来自外层声明的真实 T。
 */
fun CfirEnumConstructorSymbol.instantiatedQualifierOwnerType(
    qualifier: CfirExpression?,
    session: CfirSession,
): ConeCangJieType? {
    val access = qualifier as? CfirQualifiedAccessExpression ?: return null
    val classifier = (access.calleeReference as? CfirResolvedNamedReference)
        ?.resolvedSymbol as? CfirClassLikeSymbol<*> ?: return null
    val parameters = classifier.cfir.typeParameters
    if (access.typeArguments.isNotEmpty() && access.typeArguments.size != parameters.size) return null
    val qualifierType = access.coneTypeOrNull?.fullyExpandedType(session) ?: return null
    if (qualifierType.containsErrorType()) return null
    if (access.typeArguments.isEmpty()) {
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
    return noArgEnumConstructorTargetType(qualifierType, session)
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

    is ConeTypeAliasType -> expandedType?.expandedPatternEnumType(session)
    else -> null
}

/**
 * enum pattern 构造器访问的语义化名字。
 *
 * Raw CFIR 目前把 `E.A`、`MyList<Int64>.Cons` 这类 pattern 构造器保存在单个
 * [CfirNamedReference.name] 中；后续所有 pattern 路径必须通过这个对象统一拆出
 * 构造器短名和 owner 限定，避免合法性、绑定类型和可达性各自做字符串判断。
 */
data class CfirEnumPatternConstructorAccess(
    val constructorName: Name,
    private val ownerQualifier: String?,
    private val ownerTypeArguments: List<String>?,
) {
    /**
     * 判断显式 owner 限定是否指向当前 expected enum。
     *
     * 未写 owner 的裸构造器由 expected enum 类型决定；写了 owner 时，owner 名称和显式
     * 类型实参都必须与 expected enum use-site 类型一致。
     */
    fun matchesEnumOwner(enumDeclaration: CfirEnum, enumType: ConeEnumType): Boolean {
        val owner = ownerQualifier ?: return true
        if (!owner.matchesEnumOwnerName(enumDeclaration, enumType)) return false
        val explicitTypeArguments = ownerTypeArguments ?: return true
        if (explicitTypeArguments.size != enumType.typeArguments.size) return false
        return explicitTypeArguments.zip(enumType.typeArguments).all { (expectedText, actualProjection) ->
            val actualText = actualProjection.type.enumPatternTypeArgumentTextOrNull() ?: return false
            expectedText.matchesTypeArgumentText(actualText)
        }
    }

    /**
     * 判断显式 owner 限定是否指向标准库 `Option<T>`。
     */
    fun matchesStdlibOptionOwner(expectedType: ConeCangJieType): Boolean {
        val owner = ownerQualifier ?: return true
        if (owner.shortTopLevelName() != StdlibClassIds.Option.shortClassName.asString()) return false
        val explicitTypeArguments = ownerTypeArguments ?: return true
        val optionElementType = expectedType.optionElementType ?: return false
        val actualText = optionElementType.enumPatternTypeArgumentTextOrNull() ?: return false
        return explicitTypeArguments.size == 1 && explicitTypeArguments.single().matchesTypeArgumentText(actualText)
    }
}

/**
 * 从 pattern 构造器引用中提取构造器短名和可选 owner 限定。
 */
fun CfirReference.enumPatternConstructorAccessOrNull(): CfirEnumPatternConstructorAccess? {
    val rawName = (this as? CfirNamedReference)?.name ?: return null
    val rawText = rawName.identifierOrNullIfSpecial?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val dotIndex = rawText.lastTopLevelDotIndex()
    val ownerText = dotIndex.takeIf { it >= 0 }?.let { rawText.substring(0, it).trim() }
    val constructorText = rawText.substring(dotIndex + 1).stripTopLevelTypeArguments().trim()
    val constructorName = Name.identifierIfValid(constructorText) ?: return null
    val owner = ownerText?.parseEnumPatternOwnerQualifier()
    return CfirEnumPatternConstructorAccess(
        constructorName = constructorName,
        ownerQualifier = owner?.classifierText,
        ownerTypeArguments = owner?.typeArguments,
    )
}

/** 枚举模式 owner 限定名的解析结果：classifier 文本与可选的顶层类型实参文本列表。 */
private data class ParsedEnumPatternOwnerQualifier(
    /** 去除类型实参后的 classifier 文本（可能带包名限定）。 */
    val classifierText: String,
    /** 顶层类型实参文本列表；owner 未写类型实参时为 `null`。 */
    val typeArguments: List<String>?,
)

/**
 * 把 owner 限定文本解析为 classifier 与类型实参两部分。
 *
 * 文本先去除全部空白，再按顶层 `<` 定位类型实参起点；没有实参或
 * 不以 `>` 结尾时视为无实参的裸限定名。
 */
private fun String.parseEnumPatternOwnerQualifier(): ParsedEnumPatternOwnerQualifier {
    val normalized = filterNot(Char::isWhitespace)
    val typeArgumentStart = normalized.firstTopLevelTypeArgumentStart()
    if (typeArgumentStart < 0 || !normalized.endsWith(">")) {
        return ParsedEnumPatternOwnerQualifier(normalized, null)
    }

    val classifierText = normalized.substring(0, typeArgumentStart)
    val typeArgumentText = normalized.substring(typeArgumentStart + 1, normalized.length - 1)
    return ParsedEnumPatternOwnerQualifier(
        classifierText = classifierText,
        typeArguments = typeArgumentText.splitTopLevelTypeArguments(),
    )
}

/**
 * 判断 owner 文本是否匹配给定的枚举声明。
 *
 * 支持三种拼写：全限定名（`pkg.Name`）、ClassId 形式（`pkg.Name` 的 dot 分隔），
 * 以及省略包名的顶层短名；与 `enum Name<T>` 的模式书写习惯一一对应。
 */
private fun String.matchesEnumOwnerName(enumDeclaration: CfirEnum, enumType: ConeEnumType): Boolean {
    return this == enumType.classId.asFqNameString() ||
            this == enumType.classId.asString() ||
            shortTopLevelName() == enumDeclaration.name.asString()
}

/** 判断该类型实参文本与实际类型渲染文本是否等价（支持限定名与短名两种形式）。 */
private fun String.matchesTypeArgumentText(actualText: String): Boolean {
    return this == actualText || shortQualifiedTypeText() == actualText.shortQualifiedTypeText()
}

/**
 * 把当前类型渲染为枚举模式比较用的类型文本。
 *
 * 输出形如 `Name<Arg1,Arg2>` 的短名形式：有 ClassId 时取短类名，
 * typealias 取其 ClassId 短名；递归展开类型实参，任一分量不可渲染则整体失败。
 */
private fun ConeCangJieType.enumPatternTypeArgumentTextOrNull(): String? {
    val classId = classIdOrPrimitiveClassId
    val baseName = when {
        classId != null -> classId.shortClassName.asString()
        this is ConeTypeAliasType -> this.classId.shortClassName.asString()
        else -> return null
    }
    if (typeArguments.isEmpty()) return baseName
    val argumentTexts = typeArguments.map { projection ->
        projection.type.enumPatternTypeArgumentTextOrNull() ?: return null
    }
    return "$baseName<${argumentTexts.joinToString(",")}>"
}

/**
 * 把类型文本规约为"短名 + 递归规约的类型实参"形式。
 *
 * 用于两侧文本比较前统一拼写：限定名折叠为顶层短名，
 * 嵌套类型实参逐层递归规约，保证 `pkg.A<pkg.B>` 与 `A<B>` 等价。
 */
private fun String.shortQualifiedTypeText(): String {
    val argumentStart = firstTopLevelTypeArgumentStart()
    val classifier = if (argumentStart < 0) this else substring(0, argumentStart)
    val shortClassifier = classifier.shortTopLevelName()
    if (argumentStart < 0 || !endsWith(">")) return shortClassifier
    val arguments = substring(argumentStart + 1, length - 1)
        .splitTopLevelTypeArguments()
        .joinToString(",") { it.shortQualifiedTypeText() }
    return "$shortClassifier<$arguments>"
}

/** 取最后一个顶层 `.` 之后的短名；没有限定点时返回原文。 */
private fun String.shortTopLevelName(): String {
    val dotIndex = lastTopLevelDotIndex()
    return if (dotIndex < 0) this else substring(dotIndex + 1)
}

/** 去除顶层类型实参部分；没有顶层 `<...>` 时返回原文。 */
private fun String.stripTopLevelTypeArguments(): String {
    val start = firstTopLevelTypeArgumentStart()
    return if (start >= 0 && endsWith(">")) substring(0, start) else this
}

/** 返回第一个不在嵌套 `<...>` 内的 `<` 下标；不存在时返回 -1。 */
private fun String.firstTopLevelTypeArgumentStart(): Int {
    var depth = 0
    for (index in indices) {
        when (this[index]) {
            '<' -> {
                if (depth == 0) return index
                depth++
            }
            '>' -> if (depth > 0) depth--
        }
    }
    return -1
}

/** 返回最后一个不在嵌套 `<...>` 内的 `.` 下标；不存在时返回 -1。 */
private fun String.lastTopLevelDotIndex(): Int {
    var depth = 0
    for (index in indices.reversed()) {
        when (this[index]) {
            '>' -> depth++
            '<' -> if (depth > 0) depth--
            '.' -> if (depth == 0) return index
        }
    }
    return -1
}

/** 按顶层逗号拆分类型实参文本，逐段去除空白并丢弃空段。 */
private fun String.splitTopLevelTypeArguments(): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (index in indices) {
        when (this[index]) {
            '<' -> depth++
            '>' -> if (depth > 0) depth--
            ',' -> if (depth == 0) {
                result += substring(start, index).filterNot(Char::isWhitespace)
                start = index + 1
            }
        }
    }
    result += substring(start).filterNot(Char::isWhitespace)
    return result.filter { it.isNotEmpty() }
}
