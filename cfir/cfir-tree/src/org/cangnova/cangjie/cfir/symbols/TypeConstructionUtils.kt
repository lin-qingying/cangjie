package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.CfirBuiltInTypeKind
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.ClassId


/**
 * 将 [ClassId] 转换为 class-like lookup tag。
 */
fun ClassId.toLookupTag(): ConeClassLikeLookupTagImpl {
    return ConeClassLikeLookupTagImpl(this)
}

/**
 * 基于 classifier lookup tag 构造类型。
 *
 * 类型参数 tag 构造 [ConeTypeParameterTypeImpl]，class-like tag 构造 class-like 类型。
 */
fun ConeClassifierLookupTag.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeLookupTagBasedType {
    return when (this) {
        is ConeTypeParameterLookupTag -> ConeTypeParameterTypeImpl(this, attributes)
        is ConeClassLikeLookupTag -> this.constructClassType(typeArguments, attributes)
        else -> error("! ${this::class}")
    }
}

/**
 * 基于类型参数符号构造其语义类型，并保留窄返回类型供类型参数专用调用链使用。
 */
fun CfirTypeParameterSymbol.constructType(
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeTypeParameterTypeImpl = ConeTypeParameterTypeImpl(toLookupTag(), attributes)

/**
 * 基于 classifier 符号构造对应类型。
 */
fun CfirClassifierSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeCangJieType {
    return when (this) {
        is CfirTypeParameterSymbol -> constructType(attributes)
        is CfirClassLikeSymbol<*> -> constructType(typeArguments, attributes)

    }
}

/**
 * 基于 class-like 符号构造对应的语义类型。
 *
 * Primitive 声明虽然以合成 class-like symbol 参与名称与成员查询，其语义类型仍必须是
 * [ConePrimitiveType]；不能泄漏为同 ClassId 的 [ConeClassLikeType]，否则 extend、父类型和
 * subtype 查询会进入错误的 class-like 索引路径。
 */
fun CfirClassLikeSymbol<*>.constructType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeCangJieType {

    return when (this) {
        is CfirPrimitiveTypeSymbol -> {
            require(typeArguments.isEmpty()) { "Primitive type $name cannot have type arguments" }
            ConePrimitiveType(kind, attributes)
        }
        is CfirBuiltInTypeSymbol -> constructBuiltInType(typeArguments, attributes)
        is CfirInterfaceSymbol -> ConeClassLikeType(this.toLookupTag(), typeArguments, attributes, isInterface = true)
        is CfirStructSymbol -> ConeStructType(this.toLookupTag(), typeArguments, attributes)
        is CfirEnumSymbol -> ConeEnumType(this.toLookupTag(), typeArguments, attributes, isRefEnum)
        else -> ConeClassLikeType(this.toLookupTag(), typeArguments, attributes)
    }
}

/**
 * 按官方 BuiltInDecl 的种类构造其语义类型。
 *
 * BuiltInDecl 在声明层是 class-like classifier，但官方类型系统并不把五种声明
 * 全部表示成普通 class type：CPointer、CString、VArray 和 CFunc 分别对应专用
 * Cone 类型，RawArray 当前则保留其 classifier identity。所有 classifier → Cone
 * 的转换在这里集中完成，避免类型解析器和调用解析器再次按名称分叉。
 */
private fun CfirBuiltInTypeSymbol.constructBuiltInType(
    typeArguments: List<ConeTypeProjection>,
    attributes: ConeAttributes,
): ConeCangJieType = when (kind) {
    CfirBuiltInTypeKind.ARRAY -> ConeClassLikeType(
        lookupTag = toLookupTag(),
        typeArguments = typeArguments,
        attributes = attributes,
    )

    CfirBuiltInTypeKind.VARRAY -> {
        val elementType = typeArguments.singleOrNull()?.type
        if (elementType == null) {
            ConeErrorType(
                ConeSimpleDiagnostic("VArray expects exactly one element type argument"),
                attributes = attributes,
            )
        } else {
            // VArray 的真实尺寸只能由 CfirVArrayTypeRef 提供；classifier 构造路径使用官方
            // GetBuiltInVArrayType 的未定尺寸 0，源码 VArray<T, $N> 不经过此分支。
            ConeVArrayType(elementType = elementType, size = 0, attributes = attributes)
        }
    }

    CfirBuiltInTypeKind.CPOINTER -> {
        val pointeeType = typeArguments.singleOrNull()?.type
        if (pointeeType == null) {
            ConeErrorType(
                ConeSimpleDiagnostic("CPointer expects exactly one pointee type argument"),
                attributes = attributes,
            )
        } else {
            ConePointerType(pointeeType = pointeeType, attributes = attributes)
        }
    }

    CfirBuiltInTypeKind.CSTRING -> {
        if (typeArguments.isEmpty()) {
            ConeCStringType(attributes)
        } else {
            ConeErrorType(
                ConeSimpleDiagnostic("CString does not accept type arguments"),
                attributes = attributes,
            )
        }
    }

    CfirBuiltInTypeKind.CFUNC -> {
        val argumentType = typeArguments.singleOrNull()?.type
        val functionType = argumentType as? ConeFunctionType
        if (functionType == null) {
            ConeErrorType(
                ConeSimpleDiagnostic("CFunc expects exactly one function type argument"),
                // 合成错误类型只在"实参存在、已解析、且不含错误类型"时才保留 CFunc 分类器身份：
                // 这类失败是"写了 CFunc<非函数类型>"，CFunc 检查器要靠身份把它与同名用户类型
                // 区分开并报 `CFUNC_TYPE`。
                // 实参本身是 error type（语法写坏 / 类型未解析）时不保留身份：那是上游已经报过
                // 的错误，错误类型不应冒充一个构造失败的 CFunc，否则任何按 `lookupTag.classId`
                // 识别 CFunc 的消费者都会对坏代码再报一条假阳性。与
                // `ConeTypeUtils.containsErrorType` 的"实参含错误类型必须阻断普通类型规则"一致。
                delegatedType = argumentType
                    ?.takeUnless { it.containsErrorType() }
                    ?.let {
                        ConeClassLikeType(
                            lookupTag = toLookupTag(),
                            typeArguments = typeArguments,
                        )
                    },
                attributes = attributes,
            )
        } else {
            ConeFunctionType(
                parameterTypes = functionType.parameterTypes,
                returnType = functionType.returnType,
                isCFunc = true,
                isClosureType = functionType.isClosureType,
                hasVariableLenArg = functionType.hasVariableLenArg,
                attributes = functionType.attributes.add(attributes),
            )
        }
    }
}

/**
 * 构造类内部 `This` 视图类型。
 *
 * `This` 只属于 class 实例成员返回类型语义；普通 class 类型仍由 [constructType] 构造。
 */
fun CfirClassSymbol.constructThisType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeClassLikeType {
    return ConeClassLikeType(this.toLookupTag(), typeArguments, attributes, isThisType = true)
}

/**
 * 基于 class-like lookup tag 构造普通 class-like 类型。
 */
fun ConeClassLikeLookupTag.constructClassType(
    typeArguments: List<ConeTypeProjection> = emptyList(),
    attributes: ConeAttributes = ConeAttributes.Empty,
): ConeClassifierType {
    return ConeClassLikeType(this, typeArguments, attributes)
}
