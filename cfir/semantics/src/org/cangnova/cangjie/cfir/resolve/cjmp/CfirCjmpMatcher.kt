package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.declarations.CfirErrorNamedValue
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCallableKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpClassLikeKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.cangnova.cangjie.cfir.session.cjmpOriginalExtendKey
import org.cangnova.cangjie.cfir.session.sameKeyAs

/** CFIR 层的 CJMP 特有键、类型适配与声明分类；兼容性算法位于 resolution.common。 */
object CfirCjmpMatcher {
    /** 同一原始 extend 键关系供声明准备与组键规范化共同使用。 */
    fun haveSameExtendKey(first: CfirExtend, second: CfirExtend): CjmpTypeCompatibility {
        if (first.typeParameters.size != second.typeParameters.size) return CjmpTypeCompatibility.INCOMPATIBLE
        val firstKey = first.cjmpOriginalExtendKey() ?: return CjmpTypeCompatibility.UNRESOLVED
        val secondKey = second.cjmpOriginalExtendKey() ?: return CjmpTypeCompatibility.UNRESOLVED
        return if (firstKey.sameKeyAs(secondKey, first.moduleData.session)) CjmpTypeCompatibility.COMPATIBLE
        else CjmpTypeCompatibility.INCOMPATIBLE
    }

    fun substituteCommonType(
        commonType: ConeCangJieType,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
        session: CfirSession,
    ): ConeCangJieType =
        substitutorByMap(
            typeParameterMapping.entries.associate { (common, specific) -> common.symbol to specific.symbol.constructType() },
            session,
        ).substituteOrSelf(commonType)

    /** 显式和函数体推断的返回类型均读取通用 CFIR resolved type view。 */
    fun typeOf(typeRef: CfirTypeRef?): ConeCangJieType? = typeRef?.coneTypeOrNull

    internal fun typesEquivalent(
        specificType: ConeCangJieType?,
        commonType: ConeCangJieType?,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
        session: CfirSession,
    ): CjmpTypeCompatibility {
        val typeContext = session.typeContext
        if (specificType == null || commonType == null) return CjmpTypeCompatibility.UNRESOLVED
        if (specificType.containsErrorType() || commonType.containsErrorType()) return CjmpTypeCompatibility.UNRESOLVED

        // 先按类型参数身份递归替换，再使用统一类型系统比较。不能只递归 class-like：
        // enum/struct/function/tuple 等同样可以包含外层参数，直接 == 会丢掉这些映射。
        val substitutedCommon = substituteCommonType(commonType, typeParameterMapping, session)
        return if (AbstractTypeChecker.equalTypes(typeContext, specificType, substitutedCommon)) {
            CjmpTypeCompatibility.COMPATIBLE
        } else {
            CjmpTypeCompatibility.INCOMPATIBLE
        }
    }

    internal fun nominalKind(declaration: CfirClassLikeDeclaration): CjmpClassLikeKind = when (declaration) {
        is CfirClass -> CjmpClassLikeKind.CLASS
        is CfirStruct -> CjmpClassLikeKind.STRUCT
        is CfirInterface -> CjmpClassLikeKind.INTERFACE
        is CfirEnum -> CjmpClassLikeKind.ENUM
        is CfirTypeAlias -> CjmpClassLikeKind.TYPE_ALIAS
        is CfirBuiltInDeclaration -> CjmpClassLikeKind.BUILT_IN
        is CfirPrimitiveTypeDeclaration -> CjmpClassLikeKind.PRIMITIVE
    }

    /** 按 CFIR 的密封声明层次分类；不支持配对的 callable 明确返回 null。 */
    fun callableKind(declaration: CfirCallableDeclaration): CjmpCallableKind? = when (declaration) {
        is CfirFunction -> when (declaration) {
            is CfirConstructor -> CjmpCallableKind.CONSTRUCTOR
            is CfirNamedFunction -> CjmpCallableKind.FUNCTION
            is CfirErrorFunction,
            is CfirAnonymousFunction,
            is CfirFinalizer,
            is CfirMacroDeclaration,
            is CfirMainFunction,
            is CfirPropertyAccessor -> null
        }
        is CfirEnumConstructor -> if (declaration.valueParameters.isEmpty()) {
            CjmpCallableKind.VARIABLE
        } else {
            CjmpCallableKind.FUNCTION
        }
        is CfirProperty -> CjmpCallableKind.PROPERTY
        is CfirVariable -> CjmpCallableKind.VARIABLE
        is CfirErrorNamedValue -> null
    }

    internal fun typeParameters(declaration: CfirDeclaration): List<CfirTypeParameter> = when (declaration) {
        is CfirClassLikeDeclaration -> declaration.typeParameters.map { it.symbol.cfir as CfirTypeParameter }
        is CfirExtend -> declaration.typeParameters
        // 构造器/enum 构造项的 constructed-class 引用属于名义 owner，匹配只比较自有 header。
        is CfirFunction -> declaration.typeParameters.filterIsInstance<CfirTypeParameter>()
        is CfirEnumConstructor -> declaration.typeParameters.filterIsInstance<CfirTypeParameter>()
        else -> emptyList()
    }

    fun isSpecific(declaration: CfirDeclaration): Boolean =
        (declaration as? CfirMemberDeclaration)?.status?.isSpecific == true
}
