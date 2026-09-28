package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility

/** CFIR 层的 CJMP 特有键、类型适配与声明分类；兼容性算法位于 resolution.common。 */
object CfirCjmpMatcher {
    /** 官方 MergeCJMPExtensions 键：目标类型、接口集合与按位置映射后的约束均须一致。 */
    fun haveSameExtendKey(first: CfirExtend, second: CfirExtend): CjmpTypeCompatibility {
        if (first.typeParameters.size != second.typeParameters.size) return CjmpTypeCompatibility.INCOMPATIBLE
        val typeParameterMapping = second.typeParameters.zip(first.typeParameters).toMap()
        val firstTarget = typeOf(first.extendedTypeRef) ?: return CjmpTypeCompatibility.UNRESOLVED
        val secondTarget = typeOf(second.extendedTypeRef) ?: return CjmpTypeCompatibility.UNRESOLVED
        val targetCompatibility = typesEquivalent(firstTarget, secondTarget, typeParameterMapping)
        if (targetCompatibility != CjmpTypeCompatibility.COMPATIBLE) {
            return targetCompatibility
        }

        val firstInterfaces = first.superTypeRefs.map { typeOf(it) ?: return CjmpTypeCompatibility.UNRESOLVED }
        val secondInterfaces = second.superTypeRefs.map { typeOf(it) ?: return CjmpTypeCompatibility.UNRESOLVED }
        val interfacesCompatibility = sameTypeSet(firstInterfaces, secondInterfaces, typeParameterMapping)
        if (interfacesCompatibility != CjmpTypeCompatibility.COMPATIBLE) return interfacesCompatibility

        for (index in first.typeParameters.indices) {
            val firstBounds = first.typeParameters[index].bounds.map { typeOf(it) ?: return CjmpTypeCompatibility.UNRESOLVED }
            val secondBounds = second.typeParameters[index].bounds.map { typeOf(it) ?: return CjmpTypeCompatibility.UNRESOLVED }
            val boundsCompatibility = sameTypeSet(firstBounds, secondBounds, typeParameterMapping)
            if (boundsCompatibility != CjmpTypeCompatibility.COMPATIBLE) return boundsCompatibility
        }
        return CjmpTypeCompatibility.COMPATIBLE
    }

    /** 带参 enum 构造器按位置比较参数类型，common 外层泛型先替换到 specific 空间。 */
    fun matchEnumConstructors(
        specific: CfirEnumConstructor,
        common: CfirEnumConstructor,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): CjmpTypeCompatibility {
        if (specific.valueParameters.size != common.valueParameters.size) return CjmpTypeCompatibility.INCOMPATIBLE
        for (index in specific.valueParameters.indices) {
            val compatibility = typesEquivalent(
                typeOf(specific.valueParameters[index].returnTypeRef),
                typeOf(common.valueParameters[index].returnTypeRef),
                typeParameterMapping,
            )
            if (compatibility != CjmpTypeCompatibility.COMPATIBLE) return compatibility
        }
        return CjmpTypeCompatibility.COMPATIBLE
    }

    fun substituteCommonType(
        commonType: ConeCangJieType,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): ConeCangJieType =
        CfirTypeSubstitutorByMap.fromTypeParameterMapping(typeParameterMapping).substituteOrSelf(commonType)

    /** 显式和函数体推断的返回类型均读取通用 CFIR resolved type view。 */
    fun typeOf(typeRef: CfirTypeRef?): ConeCangJieType? = typeRef?.coneTypeOrNull

    internal fun typesEquivalent(
        specificType: ConeCangJieType?,
        commonType: ConeCangJieType?,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): CjmpTypeCompatibility {
        if (specificType == null || commonType == null) return CjmpTypeCompatibility.UNRESOLVED
        if (specificType.containsErrorType() || commonType.containsErrorType()) return CjmpTypeCompatibility.UNRESOLVED

        if (specificType is ConeTypeParameterType || commonType is ConeTypeParameterType) {
            if (specificType !is ConeTypeParameterType || commonType !is ConeTypeParameterType) {
                return CjmpTypeCompatibility.INCOMPATIBLE
            }
            val specificTag = specificType.lookupTag as? ConeTypeParameterLookupTag
                ?: return CjmpTypeCompatibility.INCOMPATIBLE
            val commonTag = commonType.lookupTag as? ConeTypeParameterLookupTag
                ?: return CjmpTypeCompatibility.INCOMPATIBLE
            val mappedSpecific = typeParameterMapping.entries
                .firstOrNull { it.key.symbol == commonTag.typeParameterSymbol }
                ?.value?.symbol
            return if (mappedSpecific == specificTag.typeParameterSymbol ||
                specificTag.typeParameterSymbol == commonTag.typeParameterSymbol
            ) {
                CjmpTypeCompatibility.COMPATIBLE
            } else {
                CjmpTypeCompatibility.INCOMPATIBLE
            }
        }

        if (specificType is ConeClassLikeType || commonType is ConeClassLikeType) {
            if (specificType !is ConeClassLikeType || commonType !is ConeClassLikeType) {
                return CjmpTypeCompatibility.INCOMPATIBLE
            }
            if (specificType.classId != commonType.classId) return CjmpTypeCompatibility.INCOMPATIBLE
            val specificArguments = specificType.typeArguments
            val commonArguments = commonType.typeArguments
            if (specificArguments.size != commonArguments.size) return CjmpTypeCompatibility.INCOMPATIBLE
            var unresolved = false
            for (index in specificArguments.indices) {
                when (typesEquivalent(specificArguments[index].type, commonArguments[index].type, typeParameterMapping)) {
                    CjmpTypeCompatibility.COMPATIBLE -> Unit
                    CjmpTypeCompatibility.INCOMPATIBLE -> return CjmpTypeCompatibility.INCOMPATIBLE
                    CjmpTypeCompatibility.UNRESOLVED -> unresolved = true
                }
            }
            return if (unresolved) CjmpTypeCompatibility.UNRESOLVED else CjmpTypeCompatibility.COMPATIBLE
        }

        if (specificType is ConePrimitiveType || commonType is ConePrimitiveType) {
            return if (specificType is ConePrimitiveType && commonType is ConePrimitiveType &&
                specificType.kind == commonType.kind
            ) {
                CjmpTypeCompatibility.COMPATIBLE
            } else {
                CjmpTypeCompatibility.INCOMPATIBLE
            }
        }

        return if (specificType == commonType) CjmpTypeCompatibility.COMPATIBLE
        else CjmpTypeCompatibility.INCOMPATIBLE
    }

    internal fun nominalKind(declaration: CfirClassLikeDeclaration): String = when (declaration) {
        is CfirClass -> "class"
        is CfirStruct -> "struct"
        is CfirInterface -> "interface"
        is CfirEnum -> "enum"
        else -> "class"
    }

    internal fun callableKind(declaration: CfirDeclaration): String = when (declaration) {
        is CfirConstructor -> "constructor"
        is CfirNamedFunction -> "function"
        is CfirProperty -> "property"
        is CfirVariable -> "variable"
        is CfirCallableDeclaration -> declaration::class.simpleName.orEmpty()
        else -> "other"
    }

    internal fun typeParameters(declaration: CfirDeclaration): List<CfirTypeParameter> = when (declaration) {
        is CfirClassLikeDeclaration -> declaration.typeParameters.map { it.symbol.cfir as CfirTypeParameter }
        is CfirExtend -> declaration.typeParameters
        is CfirFunction -> declaration.typeParameters
        else -> emptyList()
    }

    private fun sameTypeSet(
        first: List<ConeCangJieType>,
        second: List<ConeCangJieType>,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): CjmpTypeCompatibility {
        if (first.size != second.size) return CjmpTypeCompatibility.INCOMPATIBLE
        val unmatched = second.toMutableList()
        for (type in first) {
            var unresolved = false
            val index = unmatched.indexOfFirst { candidate ->
                when (typesEquivalent(type, candidate, typeParameterMapping)) {
                    CjmpTypeCompatibility.COMPATIBLE -> true
                    CjmpTypeCompatibility.INCOMPATIBLE -> false
                    CjmpTypeCompatibility.UNRESOLVED -> {
                        unresolved = true
                        false
                    }
                }
            }
            if (index < 0) {
                return if (unresolved) CjmpTypeCompatibility.UNRESOLVED else CjmpTypeCompatibility.INCOMPATIBLE
            }
            unmatched.removeAt(index)
        }
        return CjmpTypeCompatibility.COMPATIBLE
    }

    fun isSpecific(declaration: CfirDeclaration): Boolean =
        (declaration as? CfirMemberDeclaration)?.status?.isSpecific == true
}
