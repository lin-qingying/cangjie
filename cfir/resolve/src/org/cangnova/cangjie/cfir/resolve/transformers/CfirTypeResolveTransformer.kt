/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef

/**
 * TYPES 闃舵澶勭悊鍣ㄣ€? *
 * 璐熻矗灏嗗０鏄庡ご涓殑鎵€鏈夋樉寮忕被鍨嬪紩鐢ㄨВ鏋愪负 [CfirResolvedTypeRef]銆? * 涓嶅鐞嗗嚱鏁颁綋锛堢暀缁?BODY_RESOLVE锛夊拰闅愬紡绫诲瀷锛堢暀缁?IMPLICIT_TYPES锛夈€? *
 * 瀵归綈 K2: `FirTypeResolveProcessor`
 */
class CfirTypeResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(session, scopeSession, CfirResolvePhase.TYPES) {
    override val transformer = CfirTypeResolveTransformer(session)
}

/**
 * TYPES 闃舵杞崲鍣ㄣ€? *
 * 閬嶅巻 CFIR 鏍戯紝灏嗗０鏄庡ご涓殑鏄惧紡绫诲瀷寮曠敤锛坄CfirUserTypeRef`銆乣CfirFunctionTypeRef`銆? * `CfirTupleTypeRef`銆乣CfirVArrayTypeRef`锛夎В鏋愪负 `CfirResolvedTypeRef`銆? *
 * 鏍稿績鑱岃矗锛? * - 鍑芥暟杩斿洖绫诲瀷锛坄CfirFunction.returnTypeRef`锛? * - 灞炴€х被鍨嬶紙`CfirProperty.returnTypeRef`锛? * - 鍙橀噺绫诲瀷锛坄CfirVariable.returnTypeRef`锛? * - 鍊煎弬鏁扮被鍨嬶紙`CfirValueParameter.returnTypeRef`锛? * - 鏋勯€犲嚱鏁板弬鏁板拰杩斿洖绫诲瀷
 * - 绫诲瀷鍙傛暟杈圭晫锛坄CfirTypeParameter.bounds`锛? * - 绫诲瀷鍒悕灞曞紑绫诲瀷锛坄CfirTypeAlias.expandedTypeRef`锛? * - extend 鍧楃殑鎵╁睍绫诲瀷锛坄CfirExtend.extendedTypeRef`锛? *
 * 璁捐鍐崇瓥锛? * - 绫诲瀷鍙傛暟 scope 鐢?`Map<String, CfirTypeParameter>` 绠€鍖栵紙K2 鐢?PersistentList<FirScope>锛? * - `transformBlock` 鐩存帴杩斿洖锛孴YPES 闃舵鍙鐞嗗０鏄庡ご
 * - 涓夊眰濮旀墭锛氭湰绫伙紙鏍戦亶鍘?+ scope 绠＄悊锛夆啋 [CfirSpecificTypeResolverTransformer]锛堢被鍨嬪紩鐢ㄨВ鏋愬鎵橈級
 *   鈫?[CfirExplicitTypeRefResolver][org.cangnova.cangjie.cfir.resolve.CfirExplicitTypeRefResolver]锛堝叿浣撹В鏋愰€昏緫锛? *
 * 瀵归綈 K2: `FirTypeResolveTransformer`
 */
class CfirTypeResolveTransformer(
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<Any?>(CfirResolvePhase.TYPES) {

    private val typeResolverTransformer = CfirSpecificTypeResolverTransformer(session)

    /**
     * 褰撳墠浣滅敤鍩熷唴鍙鐨勭被鍨嬪弬鏁帮紙鐢卞鍚戝唴绱Н锛夈€?     *
     * 閬嶅巻宓屽澹版槑鏃堕€氳繃 [withTypeParameters] 涓存椂鍔犲叆/绉婚櫎绫诲瀷鍙傛暟銆?     * Key 涓虹被鍨嬪弬鏁板悕绉帮紝Value 涓哄搴旂殑 [CfirTypeParameter] 澹版槑銆?     */
    private val typeParametersInScope = mutableMapOf<String, CfirTypeParameter>()

    // ---- 澹版槑閬嶅巻 ----

    override fun transformFile(file: CfirFile, data: Any?): CfirFile {
        checkSessionConsistency(file)
        file.transformDeclarations(this, data)
        return file
    }

    override fun transformClass(klass: CfirClass, data: Any?): CfirClass {
        return withTypeParameters(klass.typeParameters) {
            klass.transformTypeParameters(this, data)
            klass.transformSuperTypeRefs(this, data)
            klass.transformDeclarations(this, data)
            bumpPhase(klass)
            klass
        }
    }

    override fun transformExtend(extend: CfirExtend, data: Any?): CfirExtend {
        return withTypeParameters(extend.typeParameters) {
            extend.transformTypeParameters(this, data)
            extend.transformExtendedTypeRef(this, data)
            extend.transformSuperTypeRefs(this, data)
            extend.transformDeclarations(this, data)
            bumpPhase(extend)
            extend
        }
    }

    override fun transformFunction(function: CfirFunction, data: Any?): CfirFunction {
        return withTypeParameters(function.typeParameters) {
            function.transformTypeParameters(this, data)
            function.transformReturnTypeRef(this, data)
            function.transformValueParameters(this, data)
            // 涓嶉亶鍘?body 鈥?
            // TYPES 闃舵涓嶈В鏋愬嚱鏁颁綋
            bumpPhase(function)
            function
        }
    }

    override fun transformConstructor(constructor: CfirConstructor, data: Any?): CfirConstructor {
        return withTypeParameters(constructor.typeParameters) {
            constructor.transformTypeParameters(this, data)
            constructor.transformReturnTypeRef(this, data)
            constructor.transformValueParameters(this, data)
            bumpPhase(constructor)
            constructor
        }
    }

    override fun transformProperty(property: CfirProperty, data: Any?): CfirProperty {
        return withTypeParameters(property.typeParameters) {
            property.transformTypeParameters(this, data)
            property.transformReturnTypeRef(this, data)
            bumpPhase(property)
            property
        }
    }

    override fun transformVariable(variable: CfirVariable, data: Any?): CfirVariable {
        variable.transformReturnTypeRef(this, data)
        bumpPhase(variable)
        return variable
    }

    override fun transformPatternVariable(patternVariable: CfirPatternVariable, data: Any?): CfirPatternVariable {
        patternVariable.transformReturnTypeRef(this, data)
        bumpPhase(patternVariable)
        return patternVariable
    }

    override fun transformValueParameter(valueParameter: CfirValueParameter, data: Any?): CfirValueParameter {
        valueParameter.transformReturnTypeRef(this, data)
        return valueParameter
    }

    override fun transformTypeParameter(typeParameter: CfirTypeParameter, data: Any?): CfirTypeParameter {
        typeParameter.transformBounds(this, data)
        return typeParameter
    }

    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: Any?): CfirTypeAlias {
        return withTypeParameters(typeAlias.typeParameters) {
            typeAlias.transformTypeParameters(this, data)
            typeAlias.transformExpandedTypeRef(this, data)
            bumpPhase(typeAlias)
            typeAlias
        }
    }

    // ---- 绫诲瀷瑙ｆ瀽 ----

    override fun transformTypeRef(typeRef: CfirTypeRef, data: Any?): CfirTypeRef {
        // 濮旀墭鍒?CfirSpecificTypeResolverTransformer锛屼紶鍏ュ綋鍓?
        // scope 涓殑绫诲瀷鍙傛暟
        return typeResolverTransformer.transformTypeRef(typeRef, typeParametersInScope)
    }

    override fun transformUserTypeRef(userTypeRef: CfirUserTypeRef, data: Any?): CfirTypeRef {
        return transformTypeRef(userTypeRef, data)
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: Any?): CfirTypeRef {
        // 宸茶В鏋?鈫?鐩存帴杩斿洖
        return resolvedTypeRef
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: Any?): CfirTypeRef {
        // 闅愬紡绫诲瀷 鈫?鐣欑粰 IMPLICIT_TYPES 闃舵
        return implicitTypeRef
    }

    override fun transformBasicTypeRef(basicTypeRef: CfirBasicTypeRef, data: Any?): CfirTypeRef {
        // 鍩烘湰绫诲瀷锛圛nt64 绛夛級鈫?濮旀墭鍒?
        // transformTypeRef 缁熶竴瑙ｆ瀽
        return transformTypeRef(basicTypeRef, data)
    }

    // ---- 璺宠繃 ----

    override fun transformBlock(block: CfirBlock, data: Any?): CfirExpression {
        // TYPES 闃舵涓嶈В鏋愬嚱鏁颁綋
        return block
    }

    // ---- 杈呭姪 ----

    /**
     * 涓存椂灏?[params] 鍔犲叆绫诲瀷鍙傛暟 scope锛屾墽琛?[action]锛岀劧鍚庢仮澶嶃€?     *
     * 鏀寔宓屽璋冪敤锛氬灞傜被鍨嬪弬鏁板湪鍐呭眰浠嶇劧鍙锛?     * 浣嗗唴灞傚悓鍚嶇被鍨嬪弬鏁颁細閬斀澶栧眰銆?     */
    private inline fun <R> withTypeParameters(
        params: List<CfirTypeParameter>,
        action: () -> R,
    ): R {
        if (params.isEmpty()) return action()

        val savedEntries = mutableMapOf<String, CfirTypeParameter?>()
        for (param in params) {
            val name = param.name.asString()
            savedEntries[name] = typeParametersInScope.put(name, param)
        }
        return try {
            action()
        } finally {
            for ((name, previous) in savedEntries) {
                if (previous != null) {
                    typeParametersInScope[name] = previous
                } else {
                    typeParametersInScope.remove(name)
                }
            }
        }
    }

    /**
     * 鎺ㄨ繘澹版槑鐨?resolvePhase锛圫UPER_TYPES 鈫?TYPES锛夈€?     */
    private fun bumpPhase(declaration: CfirDeclaration) {
        declaration.replaceResolvePhase(CfirResolvePhase.TYPES)
    }
}


