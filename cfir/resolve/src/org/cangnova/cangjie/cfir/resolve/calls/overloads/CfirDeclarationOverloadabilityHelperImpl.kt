package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.symbols.constructType

import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOverloadabilityHelper
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.utils.ownTypeParameters
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.areFunctionParameterTypesIdentical
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.typeContext

/**
 * 官方 PreCheckFuncRedefinitionWithSameSignature 的参数 IDENTICAL 判定。
 *
 * Kotlin 的接口/resolve 实现所有权保持一致；仓颉不用双向 specificity 或泛型上界约束。
 * 外层返回类型、receiver 与 where 不参与重载；参数内部的结构由公共类型比较器完整处理。
 */
class CfirDeclarationOverloadabilityHelperImpl(val session: CfirSession) : CfirDeclarationOverloadabilityHelper {
    override fun isConflicting(
        a: CfirCallableSymbol<*>,
        b: CfirCallableSymbol<*>,
        aOwnerSubstitutor: ConeSubstitutor,
        bOwnerSubstitutor: ConeSubstitutor,
    ): Boolean {
        val sigA = createSignature(a, aOwnerSubstitutor) ?: return false
        val sigB = createSignature(b, bOwnerSubstitutor) ?: return false
        if (sigA.isStatic != sigB.isStatic || sigA.isConstructor != sigB.isConstructor || sigA.isMain != sigB.isMain) return false
        if (sigA.parameterTypes.size != sigB.parameterTypes.size) return false

        // 泛型数量相等仅是 GenerateGenericMappingFromGeneric 的条件，不是冲突的前置门。
        val ownMapping = if (sigA.ownTypeParameters.size == sigB.ownTypeParameters.size) {
            sigA.ownTypeParameters.zip(sigB.ownTypeParameters).toMap()
        } else {
            emptyMap()
        }
        val ownSubstitutor = substitutorByMap((ownMapping).entries.associate { (origin, target) -> origin.symbol to target.symbol.constructType() }, session)
        return areFunctionParameterTypesIdentical(
            sigA.parameterTypes, sigB.parameterTypes, ownSubstitutor, ConeSubstitutor.Empty, session.typeContext,
        )
    }

    private fun createSignature(
        symbol: CfirCallableSymbol<*>,
        ownerSubstitutor: ConeSubstitutor,
    ): Signature? {
        val declaration = symbol.cfir
        val parameters: List<CfirValueParameter> = when (declaration) {
            is CfirFunction -> declaration.valueParameters
            is CfirEnumConstructor -> declaration.valueParameters
            else -> error("Declaration overloadability requires a function candidate")
        }
        val types = parameters.map { parameter ->
            // 未定型/错误参数已拥有原诊断，不能据此建立有效重载签名或比较展示文本。
            val type = parameter.returnTypeRef.coneTypeOrNull ?: return null
            val projected = ownerSubstitutor.substituteOrSelf(type)
            if (projected.containsErrorType()) return null
            projected
        }
        return Signature(
            types, declaration.typeParameters.ownTypeParameters,
            declaration.status.isStatic, declaration is CfirConstructor, declaration is CfirMainFunction,
        )
    }

    private data class Signature(
        val parameterTypes: List<ConeCangJieType>,
        val ownTypeParameters: List<CfirTypeParameter>,
        val isStatic: Boolean,
        val isConstructor: Boolean,
        val isMain: Boolean,
    )
}
