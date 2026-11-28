package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.symbols.constructType

import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind
import org.cangnova.cangjie.annotations.officialKind
import org.cangnova.cangjie.annotations.supportsBuiltinAnnotationKind
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.cjmpValueParameters
import org.cangnova.cangjie.cfir.declarations.hasInitialDefaultValue
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.cjmpAnnotationLiteralConstant
import org.cangnova.cangjie.cfir.expressions.annotationVersionSupport
import org.cangnova.cangjie.cfir.expressions.isSupportedBuiltinAnnotation
import org.cangnova.cangjie.cfir.expressions.platformAnnotationKind
import org.cangnova.cangjie.cfir.resolve.CfirGenericConstraintCompatibility
import org.cangnova.cangjie.cfir.resolve.CfirGenericConstraintParent
import org.cangnova.cangjie.cfir.resolve.CfirGenericConstraintBounds
import org.cangnova.cangjie.cfir.resolve.providers.getDeclarationMetadataOwner
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.ordinarySupertypeTypeOrNull
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.substitution.AbstractConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpMatchingMode
import org.cangnova.cangjie.cfir.session.getContainingCjmpDeclaration
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirSerializedInteropFacts
import org.cangnova.cangjie.cfir.declarations.serializedInteropFacts
import org.cangnova.cangjie.resolve.calls.mpp.AbstractCjmpChecker
import org.cangnova.cangjie.resolve.calls.mpp.CjmpAnnotationMatch
import org.cangnova.cangjie.resolve.calls.mpp.CjmpAnnotationAttribute
import org.cangnova.cangjie.resolve.calls.mpp.CjmpAnnotationMatchingContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpAnnotationMatchingPolicy
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCompatibilityIssue
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchingValueParameter
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCallableKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpClassLikeKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCompatibilityContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpDeclarationModifiers
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.name.Name

/** CFIR 注解的身份键；不同的注解身份来源保持相互区分。 */
sealed interface CfirCjmpAnnotationKey {
    data class BuiltIn(val kind: BuiltInAnnotationKind) : CfirCjmpAnnotationKey
    data class Platform(val kind: CangjiePlatformAnnotationKind) : CfirCjmpAnnotationKey
    data class Resolved(val sourceName: String) : CfirCjmpAnnotationKey
    data object Unknown : CfirCjmpAnnotationKey
}

private fun CfirCjmpAnnotationKey.isSpecialHandled(languageVersion: org.cangnova.cangjie.LanguageVersion): Boolean = when (this) {
    is CfirCjmpAnnotationKey.BuiltIn ->
        CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(kind, languageVersion)

    is CfirCjmpAnnotationKey.Platform ->
        CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(kind.officialKind, languageVersion)

    is CfirCjmpAnnotationKey.Resolved -> false

    CfirCjmpAnnotationKey.Unknown ->
        CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(
            CangjieAnnotationIdentity.Unknown,
            languageVersion,
        )
}

/** CFIR 适配层：把共享匹配与兼容性检查映射到声明站点、类型系统和注解身份。 */
class CfirCjmpMatchingContext(
    private val session: CfirSession,
    private val matchingMode: CfirCjmpMatchingMode = CfirCjmpMatchingMode.FINAL,
) : CjmpCompatibilityContext<
    CfirBasedSymbol<*>,
    CfirTypeParameterSymbol,
    ConeCangJieType,
>, CjmpAnnotationMatchingContext<CfirBasedSymbol<*>, CfirCjmpAnnotationKey, CfirAnnotation> {
    private fun CfirBasedSymbol<*>.declaration(): CfirDeclaration = cfir

    /** 将配对存储中的声明类型参数映射转换为共享 matcher 使用的符号映射。 */
    fun parentTypeParameterMapping(
        mapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol> =
        mapping.mapKeys { it.key.symbol }
            .mapValues { it.value.symbol }

    private fun Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol>.toCfirTypeParameters():
        Map<CfirTypeParameter, CfirTypeParameter> =
        entries.associate { (common, specific) -> common.cfir to specific.cfir }

    /**
     * 使用配对事实携带的 common→specific 泛型映射执行共享后置兼容性检查。
     *
     * 该入口把 CFIR 类型参数映射转换封装在适配层内，使检查器只接收语义问题并负责诊断呈现。
     */
    fun checkCompatibility(
        specific: CfirBasedSymbol<*>,
        common: CfirBasedSymbol<*>,
        typeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): List<CjmpCompatibilityIssue> = AbstractCjmpChecker.checkMatched(
        specific,
        common,
        parentTypeParameterMapping(typeParameterMapping),
        this,
    )

    override fun classLikeKind(declaration: CfirBasedSymbol<*>): CjmpClassLikeKind? =
        (declaration.declaration() as? CfirClassLikeDeclaration)?.let(CfirCjmpMatcher::nominalKind)

    override fun isExtend(declaration: CfirBasedSymbol<*>): Boolean = declaration.declaration() is CfirExtend

    override fun haveSameExtendKey(
        first: CfirBasedSymbol<*>,
        second: CfirBasedSymbol<*>,
    ): CjmpTypeCompatibility =
        if (first.declaration() is CfirExtend && second.declaration() is CfirExtend) {
            CfirCjmpMatcher.haveSameExtendKey(first.declaration() as CfirExtend, second.declaration() as CfirExtend)
        } else {
            CjmpTypeCompatibility.INCOMPATIBLE
        }

    override fun callableKind(declaration: CfirBasedSymbol<*>): CjmpCallableKind? =
        (declaration.declaration() as? CfirCallableDeclaration)?.let(CfirCjmpMatcher::callableKind)

    override fun isFunction(declaration: CfirBasedSymbol<*>): Boolean = when (val node = declaration.declaration()) {
        is CfirFunction -> true
        is CfirEnumConstructor -> node.valueParameters.isNotEmpty()
        else -> false
    }

    override fun isConstructor(declaration: CfirBasedSymbol<*>): Boolean =
        declaration.declaration() is org.cangnova.cangjie.cfir.declarations.CfirConstructor

    override fun areEnumConstructors(specific: CfirBasedSymbol<*>, common: CfirBasedSymbol<*>): Boolean =
        specific.declaration() is CfirEnumConstructor && common.declaration() is CfirEnumConstructor

    override fun areEnumConstructorTypesEquivalent(
        specific: CfirBasedSymbol<*>,
        common: CfirBasedSymbol<*>,
        typeParameterMapping: Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol>,
    ): CjmpTypeCompatibility {
        val specificConstructor = specific.declaration() as CfirEnumConstructor
        val commonConstructor = common.declaration() as CfirEnumConstructor
        // nullary 对应真实 VarDecl，官方没有函数类型比较；native kind 已由资格阶段核实。
        if (specificConstructor.valueParameters.isEmpty()) return CjmpTypeCompatibility.COMPATIBLE
        val specificParameters = specificConstructor.valueParameters.map {
            CfirCjmpMatcher.typeOf(it.returnTypeRef) ?: return CjmpTypeCompatibility.UNRESOLVED
        }
        val commonParameters = commonConstructor.valueParameters.map {
            CfirCjmpMatcher.typeOf(it.returnTypeRef) ?: return CjmpTypeCompatibility.UNRESOLVED
        }
        val specificReturnType = CfirCjmpMatcher.typeOf(specificConstructor.returnTypeRef)
            ?: return CjmpTypeCompatibility.UNRESOLVED
        val commonReturnType = CfirCjmpMatcher.typeOf(commonConstructor.returnTypeRef)
            ?: return CjmpTypeCompatibility.UNRESOLVED
        val specificSignature = ConeFunctionType(specificParameters, specificReturnType)
        val commonSignature = ConeFunctionType(commonParameters, commonReturnType)
        val directComparison = areTypesEquivalent(specificSignature, commonSignature, typeParameterMapping)
        if (directComparison != CjmpTypeCompatibility.INCOMPATIBLE) return directComparison

        // CheckCJMP::MatchEnumFuncTypes 在配对的外层 enum 下比较完整函数类型。
        // CJO 的 FullId 仍指向真实 common 声明，只为这次比较投影实际配对的 owner。
        val specificOwner = specificConstructor.getContainingCjmpDeclaration() as? CfirEnum ?: return directComparison
        val commonOwner = commonConstructor.getContainingCjmpDeclaration() as? CfirEnum ?: return directComparison
        val specificOwnerTag = specificOwner.symbol.toLookupTag()
        val commonOwnerTag = commonOwner.symbol.toLookupTag()
        if (specificOwnerTag == commonOwnerTag || specificOwnerTag.classId != commonOwnerTag.classId) return directComparison
        if (specificReturnType !is ConeEnumType || specificReturnType.lookupTag != specificOwnerTag ||
            commonReturnType !is ConeEnumType || commonReturnType.lookupTag != commonOwnerTag ||
            specificReturnType.isRefEnum != commonReturnType.isRefEnum
        ) return directComparison

        val ownerProjection = object : AbstractConeSubstitutor(session.typeContext) {
            override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
                if (type !is ConeEnumType || type.lookupTag != commonOwnerTag) return null
                return ConeEnumType(
                    lookupTag = specificOwnerTag,
                    typeArguments = type.typeArguments.mapIndexed { index, argument ->
                        substituteArgument(argument, index) ?: argument
                    },
                    attributes = type.attributes,
                    isRefEnum = type.isRefEnum,
                )
            }
        }
        return areTypesEquivalent(specificSignature, ownerProjection.substituteOrSelf(commonSignature), typeParameterMapping)
    }

    override fun typeParameters(declaration: CfirBasedSymbol<*>): List<CfirTypeParameterSymbol> =
        CfirCjmpMatcher.typeParameters(declaration.declaration()).map { it.symbol }

    override fun valueParameters(
        declaration: CfirBasedSymbol<*>,
    ): List<CjmpMatchingValueParameter<ConeCangJieType>> =
        (declaration.declaration() as CfirCallableDeclaration).cjmpValueParameters().map { parameter ->
            CjmpMatchingValueParameter(
                type = CfirCjmpMatcher.typeOf(parameter.returnTypeRef),
                isNamed = parameter.isNamed,
                name = parameter.name.asString(),
                hasDefaultValue = parameter.hasInitialDefaultValue,
            )
        }

    override fun areTypesEquivalent(
        specific: ConeCangJieType?,
        common: ConeCangJieType?,
        typeParameterMapping: Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol>,
    ): CjmpTypeCompatibility =
        CfirCjmpMatcher.typesEquivalent(specific, common, typeParameterMapping.toCfirTypeParameters(), session)

    override fun areReturnTypesCompatible(
        specific: CfirBasedSymbol<*>,
        common: CfirBasedSymbol<*>,
        typeParameterMapping: Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol>,
    ): CjmpTypeCompatibility {
        if (matchingMode == CfirCjmpMatchingMode.DEFAULT_PARAMETER_BOOTSTRAP) {
            return CjmpTypeCompatibility.COMPATIBLE
        }
        val specificRef = (specific.declaration() as CfirCallableDeclaration).returnTypeRef
        val commonRef = (common.declaration() as CfirCallableDeclaration).returnTypeRef
        val specificType = CfirCjmpMatcher.typeOf(specificRef)
        val commonType = CfirCjmpMatcher.typeOf(commonRef)
        // 错误必须先于 Quest 判断，否则一侧的错误会被另一侧的待推断状态掩盖。
        if (specificType?.containsErrorType() == true || commonType?.containsErrorType() == true) {
            return CjmpTypeCompatibility.UNRESOLVED
        }
        if (specificType == null && specificRef !is CfirImplicitTypeRef ||
            commonType == null && commonRef !is CfirImplicitTypeRef
        ) return CjmpTypeCompatibility.UNRESOLVED
        if (specificType == null && specificRef is CfirImplicitTypeRef ||
            commonType == null && commonRef is CfirImplicitTypeRef
        ) {
            return CjmpTypeCompatibility.COMPATIBLE
        }
        if (specificType == null || commonType == null) return CjmpTypeCompatibility.UNRESOLVED

        val cfirTypeParameterMapping = typeParameterMapping.toCfirTypeParameters()
        val substitutedCommon = CfirCjmpMatcher.substituteCommonType(commonType, cfirTypeParameterMapping, session)
        when (CfirCjmpMatcher.typesEquivalent(specificType, substitutedCommon, emptyMap(), session)) {
            CjmpTypeCompatibility.COMPATIBLE -> return CjmpTypeCompatibility.COMPATIBLE
            CjmpTypeCompatibility.UNRESOLVED -> return CjmpTypeCompatibility.UNRESOLVED
            CjmpTypeCompatibility.INCOMPATIBLE -> Unit
        }
        return if (AbstractTypeChecker.isSubtypeOf(session.typeContext, specificType, substitutedCommon)) {
            CjmpTypeCompatibility.COMPATIBLE
        } else {
            CjmpTypeCompatibility.INCOMPATIBLE
        }
    }

    override fun needToReportMissingBody(common: CfirBasedSymbol<*>, specific: CfirBasedSymbol<*>): Boolean {
        val commonDeclaration = common.declaration() as? CfirMemberDeclaration ?: return false
        val specificDeclaration = specific.declaration() as? CfirMemberDeclaration ?: return false
        if (commonDeclaration !is CfirCallableDeclaration) return false
        if (commonDeclaration.getContainingCjmpDeclaration() == null) return false
        return commonDeclaration.cjmpHasCommonDefault() &&
                !commonDeclaration.status.isAbstract &&
                specificDeclaration.status.isAbstract
    }

    override fun isClassLike(declaration: CfirBasedSymbol<*>): Boolean =
        declaration.declaration() is CfirClassLikeDeclaration

    override fun isClass(declaration: CfirBasedSymbol<*>): Boolean = declaration.declaration() is CfirClass

    override fun isEnum(declaration: CfirBasedSymbol<*>): Boolean = declaration.declaration() is CfirEnum

    override fun isNamedFunction(declaration: CfirBasedSymbol<*>): Boolean =
        declaration.declaration() is CfirNamedFunction

    override fun isPayloadEnumConstructor(declaration: CfirBasedSymbol<*>): Boolean =
        (declaration.declaration() as? CfirEnumConstructor)?.valueParameters?.isNotEmpty() == true

    override fun isProperty(declaration: CfirBasedSymbol<*>): Boolean = declaration.declaration() is CfirProperty

    override fun isVariable(declaration: CfirBasedSymbol<*>): Boolean = declaration.declaration() is CfirVariable

    override fun isMutableProperty(declaration: CfirBasedSymbol<*>): Boolean =
        (declaration.declaration() as CfirProperty).status.isMut

    override fun modifiers(declaration: CfirBasedSymbol<*>): CjmpDeclarationModifiers =
        declaration.declaration().compatibilityStatus().asCjmpModifiers()

    // 官方 nominal 均比较接口集合，只有 class 另比较超类；非法继承目标不进入这个语义集合。
    override fun superTypes(declaration: CfirBasedSymbol<*>): List<ConeCangJieType> {
        val owner = declaration.declaration() as CfirClassLikeDeclaration
        return buildList {
            var hasSuperclass = false
            for (typeRef in owner.superTypeRefs) {
                val type = typeRef.classifyDeclaredSupertype(session).ordinarySupertypeTypeOrNull() ?: continue
                val target = type.toSymbol(session)?.cfir
                when {
                    target is CfirInterface -> Unit
                    owner is CfirClass && target is CfirClass -> {
                        // GetSuperClassDecl 只取首个 class；非法多继承由继承规则单独诊断。
                        if (hasSuperclass) continue
                        hasSuperclass = true
                    }
                    else -> continue
                }
                // 官方 GetSuperInterfaceTys 使用规范类型身份的 set；重复继承有自己的独立诊断。
                if (none { AbstractTypeChecker.equalTypes(session.typeContext, it, type) }) add(type)
            }
        }
    }

    override fun isNonExhaustiveEnum(declaration: CfirBasedSymbol<*>): Boolean =
        (declaration.declaration() as CfirEnum).isNonExhaustive

    override fun deprecatedAnnotation(declaration: CfirBasedSymbol<*>): CfirAnnotation? =
        declaration.getDeclarationMetadataOwner().annotations.firstOrNull {
            it.annotationKind == BuiltInAnnotationKind.DEPRECATED
        }

    override fun annotations(
        declaration: CfirBasedSymbol<*>,
    ): List<CjmpAnnotationMatch<CfirCjmpAnnotationKey, CfirAnnotation>> {
        val isCommonDeclaration = declaration.declaration().compatibilityStatus().isCommon
        return declaration.getDeclarationMetadataOwner().annotations.mapNotNull { annotation ->
            val key = annotation.cjmpMatchKey() ?: return@mapNotNull null
            // Official ASTWriter stores NUMERIC_OVERFLOW in Decl attributes, not in the common CJO
            // annotation vector. Project source-only common CFIR through that same serialization
            // boundary; a specific-side Overflow annotation remains eligible for mismatch checking.
            if (isCommonDeclaration && key == CfirCjmpAnnotationKey.BuiltIn(BuiltInAnnotationKind.NUMERIC_OVERFLOW)) {
                return@mapNotNull null
            }
            CjmpAnnotationMatch(key, annotation)
        }
    }

    override fun nonSerializedAnnotationAttributes(
        declaration: CfirBasedSymbol<*>,
    ): List<CjmpAnnotationAttribute<CfirAnnotation>> {
        val metadataOwner = declaration.getDeclarationMetadataOwner()
        val sourceAnnotationsByKind = metadataOwner.annotations.mapNotNull { annotation ->
            val kind = when (val key = annotation.cjmpRawKey()) {
                is CfirCjmpAnnotationKey.BuiltIn -> key.kind
                is CfirCjmpAnnotationKey.Platform -> key.kind.officialKind
                is CfirCjmpAnnotationKey.Resolved, CfirCjmpAnnotationKey.Unknown, null -> null
            } ?: return@mapNotNull null
            kind to annotation
        }.toMap()
        val serializedFacts = metadataOwner.serializedInteropFacts
        return CjmpAnnotationMatchingPolicy.nonSerializedAnnotationAttributeKinds(
            session.languageVersionSettings.languageVersion,
        ).filter { kind ->
            kind in sourceAnnotationsByKind || serializedFacts?.hasCjmpAnnotationAttribute(kind) == true
        }.map { kind ->
            CjmpAnnotationAttribute(kind, sourceAnnotationsByKind[kind])
        }
    }

    override fun areAnnotationArgumentsEqual(specific: CfirAnnotation, common: CfirAnnotation): Boolean {
        val specificCall = specific as? CfirAnnotationCall ?: return false
        val commonCall = common as? CfirAnnotationCall ?: return false
        val specificArguments = specificCall.explicitCjmpAnnotationArguments() ?: return false
        val commonArguments = commonCall.explicitCjmpAnnotationArguments() ?: return false
        if (specificArguments.size != commonArguments.size) return false

        return specificArguments.zip(commonArguments).all { (specificArgument, commonArgument) ->
            specificArgument.name == commonArgument.name &&
                specificArgument.literalKind == commonArgument.literalKind &&
                specificArgument.literalValue == commonArgument.literalValue
        }
    }

    override fun incompatibleGenericConstraintIndices(
        specific: CfirBasedSymbol<*>,
        common: CfirBasedSymbol<*>,
        typeParameterMapping: Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol>,
    ): List<Int> {
        val specificDeclaration = specific.declaration() as? CfirMemberDeclaration ?: return emptyList()
        val commonDeclaration = common.declaration() as? CfirMemberDeclaration ?: return emptyList()
        // 与匹配阶段共用同一参数投影：只比较声明自有的泛型约束。构造器借用的所属类型参数
        // 由名义类型自身的配对负责（cjc 只在类型头部报告约束收窄），不在构造器上重复比较。
        val specificParameters = CfirCjmpMatcher.typeParameters(specificDeclaration)
        val commonParameters = CfirCjmpMatcher.typeParameters(commonDeclaration)
        if (specificParameters.isEmpty() || specificParameters.size != commonParameters.size) return emptyList()

        val commonToSpecific = typeParameterMapping.toCfirTypeParameters()
        val parents = listOf(
            CfirGenericConstraintParent(
                commonParameters,
                substitutorByMap((commonToSpecific).entries.associate { (origin, target) -> origin.symbol to target.symbol.constructType() }, session),
                CfirGenericConstraintBounds.capture(commonParameters),
            ),
        )
        return CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(
            specificParameters,
            parents,
            session.typeContext,
            ConeSubstitutor.Empty,
            CfirGenericConstraintBounds.capture(specificParameters),
        )
    }

    override fun propertyOrVariableType(declaration: CfirBasedSymbol<*>): ConeCangJieType? {
        val typeRef = when (val node = declaration.declaration()) {
            is CfirProperty -> node.returnTypeRef
            is CfirVariable -> node.returnTypeRef
            else -> return null
        }
        return (typeRef as? CfirResolvedTypeRef)?.coneType?.takeUnless { it.containsErrorType() }
    }

    override fun isMutableVariable(declaration: CfirBasedSymbol<*>): Boolean =
        (declaration.declaration() as CfirVariable).isVar

    override fun checkParameterDefaultValueConflict(): Boolean =
        session.languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.ParameterDefaultValueRules)

    override fun postMatchReturnTypeCompatibility(
        specific: CfirBasedSymbol<*>,
        common: CfirBasedSymbol<*>,
        typeParameterMapping: Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol>,
    ): CjmpTypeCompatibility {
        if (!session.languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.PostMatchReturnTypeCheck)) {
            return CjmpTypeCompatibility.COMPATIBLE
        }
        val specificType = CfirCjmpMatcher.typeOf((specific.declaration() as CfirCallableDeclaration).returnTypeRef)
            ?: return CjmpTypeCompatibility.UNRESOLVED
        val commonType = CfirCjmpMatcher.typeOf((common.declaration() as CfirCallableDeclaration).returnTypeRef)
            ?: return CjmpTypeCompatibility.UNRESOLVED
        if (commonType.containsErrorType()) return CjmpTypeCompatibility.UNRESOLVED

        val mappedCommonType = CfirCjmpMatcher.substituteCommonType(
            commonType,
            typeParameterMapping.toCfirTypeParameters(),
            session,
        )
        return if (!specificType.containsErrorType() &&
            AbstractTypeChecker.isSubtypeOf(session.typeContext, specificType, mappedCommonType)
        ) {
            CjmpTypeCompatibility.COMPATIBLE
        } else {
            CjmpTypeCompatibility.INCOMPATIBLE
        }
    }

    private fun CfirDeclarationStatus.asCjmpModifiers(): CjmpDeclarationModifiers = CjmpDeclarationModifiers(
        visibility = visibility,
        isAbstract = isAbstract,
        isOpen = isOpen,
        isSealed = isSealed,
        isC = isC,
        isStatic = isStatic,
        isMut = isMut,
        isForeign = isForeign,
        isUnsafe = isUnsafe,
    )

    private fun CfirDeclaration.compatibilityStatus(): CfirDeclarationStatus = when (this) {
        is CfirClassLikeDeclaration -> status
        is CfirMemberDeclaration -> status
        else -> error("CJMP compatibility checks require a class-like or member declaration")
    }

    private fun CfirAnnotation.cjmpMatchKey(): CfirCjmpAnnotationKey? =
        cjmpRawKey()?.takeUnless { it.isSpecialHandled(session.languageVersionSettings.languageVersion) }

    /** 保留原始身份供 declaration attribute 比较；普通注解入口再应用特殊处理策略。 */
    private fun CfirAnnotation.cjmpRawKey(): CfirCjmpAnnotationKey? {
        val settings = session.languageVersionSettings
        val key = when {
            annotationKind != null &&
                ((this as? CfirAnnotationCall)?.isSupportedBuiltinAnnotation(annotationKind!!, settings)
                    ?: settings.supportsBuiltinAnnotationKind(annotationKind!!)) ->
                CfirCjmpAnnotationKey.BuiltIn(annotationKind!!)

            this is CfirAnnotationCall && platformAnnotationKind != null &&
                annotationVersionSupport(settings) == AnnotationVersionSupportStatus.SUPPORTED ->
                CfirCjmpAnnotationKey.Platform(platformAnnotationKind!!)

            this is CfirAnnotationCall && annotationIdentity == CangjieAnnotationIdentity.Unknown ->
                CfirCjmpAnnotationKey.Unknown

            this is CfirAnnotationCall && annotationSourceName != null ->
                CfirCjmpAnnotationKey.Resolved(annotationSourceName!!)
            else -> null
        }
        return key
    }
}

private data class CfirCjmpAnnotationArgument(
    val name: Name?,
    val literalKind: CfirLiteralKind,
    val literalValue: Any?,
)

/** 注解实参按官方 AST 中的原始参数列表比较，不依赖 CFIR 绑定状态。 */
private fun CfirAnnotationCall.explicitCjmpAnnotationArguments(): List<CfirCjmpAnnotationArgument>? =
    argumentList.arguments.map { argument ->
        val namedArgument = argument as? CfirNamedArgumentExpression
        val expression = namedArgument?.expression ?: argument
        val literal = expression.cjmpAnnotationLiteralConstant() ?: return null
        CfirCjmpAnnotationArgument(namedArgument?.argumentName, literal.kind, literal.value)
    }

private fun CfirSerializedInteropFacts.hasCjmpAnnotationAttribute(kind: BuiltInAnnotationKind): Boolean = when (kind) {
    BuiltInAnnotationKind.C -> hasCAnnotationAttribute
    BuiltInAnnotationKind.JAVA_MIRROR -> isJavaMirror
    BuiltInAnnotationKind.JAVA_HAS_DEFAULT -> hasJavaDefault
    BuiltInAnnotationKind.OBJ_C_MIRROR -> isObjCMirror
    BuiltInAnnotationKind.OBJ_C_INIT -> isObjCInit
    BuiltInAnnotationKind.OBJ_C_OPTIONAL -> isObjCOptional
    else -> false
}
