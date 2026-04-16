package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.cfir.types.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.substitution.CaSubstitutedSignature
import org.cangnova.cangjie.analysis.api.substitution.CaTypeSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeSubstitutor
import org.cangnova.cangjie.name.Name

/**
 * CFIR 类型替换协议。
 *
 * 该层把公开 Analysis API 的替换请求映射到 CFIR 现有的类型替换器，
 * 保证 public signature / type 的实例化仍然服从同一套编译器语义。
 */
internal class CaCfirTypeSubstitutorImpl(
    override val substitutions: Map<Name, CaType>,
    private val coneSubstitutor: ConeSubstitutor,
    override val token: CaLifetimeToken,
) : CaTypeSubstitutor {
    override fun substitute(type: CaType): CaType {
        val cfirType = type as? CaCfirType
            ?: error("仅支持对 CFIR Analysis API 类型执行替换：${type::class.simpleName}")
        return coneSubstitutor.substituteOrSelf(cfirType.coneType).asCaType(cfirType.analysisSession)
    }
}

internal class CaCfirSubstitutedSignatureImpl(
    override val original: CaSignature,
    override val substitutor: CaTypeSubstitutor,
    declarationName: Name?,
    typeParameters: List<Name>,
    valueParameters: List<CaCfirValueParameterSignatureImpl>,
    returnType: CaType?,
    annotations: List<CaAnnotation>,
    token: CaLifetimeToken,
) : CaCfirSignatureImpl(
    declarationName = declarationName,
    typeParameters = typeParameters,
    valueParameters = valueParameters,
    returnType = returnType,
    annotations = annotations,
    token = token,
), CaSubstitutedSignature

internal fun CaCfirSession.buildTypeSubstitutor(
    substitutions: Map<Name, CaType>,
): CaTypeSubstitutor {
    val normalizedSubstitutions = linkedMapOf<Name, CaType>()
    val coneSubstitutionsForSubstitutor = linkedMapOf<String, ConeCangJieType>()
    val coneSubstitutions = linkedMapOf<Name, ConeCangJieType>()
    substitutions.forEach { (name, type) ->
        check(name !in normalizedSubstitutions) {
            "类型替换表中出现重复类型参数名：`${name.asString()}`"
        }
        normalizedSubstitutions[name] = type
        val cfirType = type as? CaCfirType
            ?: error("仅支持使用 CFIR Analysis API 类型构建替换器：${type::class.simpleName}")
        coneSubstitutions[name] = cfirType.coneType
        coneSubstitutionsForSubstitutor[name.asString()] = cfirType.coneType
    }

    val cacheKey = CaCfirTypeSubstitutorCacheKey(coneSubstitutions.toList())
    return getOrCreateTypeSubstitutor(cacheKey) {
        CaCfirTypeSubstitutorImpl(
            substitutions = normalizedSubstitutions,
            coneSubstitutor = CfirTypeSubstitutorByMap(coneSubstitutionsForSubstitutor),
            token = token,
        )
    }
}

internal fun CaCfirSession.buildSignatureSubstitutor(
    signature: CaSignature,
    typeArguments: List<CaType>,
): CaTypeSubstitutor {
    check(signature.typeParameters.size == typeArguments.size) {
        "签名实例化的类型实参数量必须与类型参数数量严格一致：" +
            " expected=${signature.typeParameters.size}, actual=${typeArguments.size}"
    }
    return buildTypeSubstitutor(signature.typeParameters.zip(typeArguments).toMap(linkedMapOf()))
}

internal fun CaCfirSession.substituteSignature(
    signature: CaSignature,
    substitutor: CaTypeSubstitutor,
): CaSubstitutedSignature {
    val cfirSubstitutor = substitutor as? CaCfirTypeSubstitutorImpl
        ?: error("仅支持使用 CFIR 类型替换器实例化签名：${substitutor::class.simpleName}")
    val cacheKey = CaCfirSubstitutedSignatureCacheKey(signature, cfirSubstitutor.substitutions)
    return getOrCreateSubstitutedSignature(cacheKey) {
        CaCfirSubstitutedSignatureImpl(
            original = signature,
            substitutor = cfirSubstitutor,
            declarationName = signature.declarationName,
            typeParameters = signature.typeParameters,
            valueParameters = signature.valueParameters.map { parameter ->
                val substitutedType = parameter.type?.let(cfirSubstitutor::substitute)
                CaCfirValueParameterSignatureImpl(
                    name = parameter.name,
                    type = substitutedType,
                    annotations = parameter.annotations,
                    token = token,
                )
            },
            returnType = signature.returnType?.let(cfirSubstitutor::substitute),
            annotations = signature.annotations,
            token = token,
        )
    }
}

/**
 * 类型替换器缓存键。
 *
 * 同一组类型实参映射在同一 session 内必须复用同一个公开替换器实例。
 */
internal data class CaCfirTypeSubstitutorCacheKey(
    val substitutions: List<Pair<Name, ConeCangJieType>>,
)

/**
 * 已替换签名缓存键。
 *
 * 同一原始签名与同一组替换映射在同一 session 内必须落到同一公开签名快照。
 */
internal data class CaCfirSubstitutedSignatureCacheKey(
    val signature: CaSignature,
    val substitutions: Map<Name, CaType>,
)
