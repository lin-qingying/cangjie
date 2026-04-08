package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType

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

    val replacements = enumDeclaration.typeParameters.mapIndexedNotNull { index, parameter ->
        enumType.typeArguments.getOrNull(index)?.type?.let { substituted ->
            parameter.name.asString() to substituted
        }
    }.toMap()
    if (replacements.isEmpty()) return payloadTypes

    val substitutor = CfirTypeSubstitutorByMap(replacements)
    return payloadTypes.map(substitutor::substituteOrSelf)
}
