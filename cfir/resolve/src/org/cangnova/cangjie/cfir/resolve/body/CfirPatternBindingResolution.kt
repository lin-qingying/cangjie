package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.payloadArity
import org.cangnova.cangjie.cfir.declarations.substitutedPayloadParameterTypes
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.Name

private val OPTION_SOME_CONSTRUCTOR_NAME = Name.identifier("Some")
private val OPTION_NONE_CONSTRUCTOR_NAME = Name.identifier("None")

/**
 * 统一的模式绑定解析支持。
 *
 * 这里负责两件事：
 * 1. 解析 pattern 自身携带的显式类型约束；
 * 2. 按 selector / initializer 的形状，把投影后的子类型写回每个 binding variable。
 *
 * 这样 `let/var pattern`、`match`、`for-in` 都复用同一套绑定模型。
 */
internal fun CfirPartialBodyResolveTransformer.resolvePatternBindingTypes(
    pattern: CfirPattern,
    expectedType: ConeCangJieType?,
    typeResolver: CfirSpecificTypeResolverTransformer,
) {
    when (pattern) {
        is CfirVarOrEnumPattern -> Unit
        is CfirBindingPattern -> {
            val resolvedTypeRef = pattern.typeRef?.let { resolvePatternTypeRefIfNeeded(it, typeResolver) }
            if (resolvedTypeRef != null && resolvedTypeRef !== pattern.typeRef) {
                pattern.transformTypeRef(typeResolver, patternTypeResolutionConfiguration())
            }

            val bindingType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
            pattern.bindingVariable?.replaceBindingType(bindingType)
            pattern.nestedPattern?.let { resolvePatternBindingTypes(it, bindingType ?: expectedType, typeResolver) }
        }

        is CfirTypePattern -> {
            val resolvedTypeRef = resolvePatternTypeRefIfNeeded(pattern.typeRef, typeResolver)
            if (resolvedTypeRef !== pattern.typeRef) {
                pattern.transformTypeRef(typeResolver, patternTypeResolutionConfiguration())
            }

            val bindingType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
            pattern.bindingVariable?.replaceBindingType(bindingType)
        }

        is CfirTuplePattern -> {
            val tupleType = expectedType as? ConeTupleType
            pattern.elements.forEachIndexed { index, element ->
                resolvePatternBindingTypes(
                    pattern = element,
                    expectedType = tupleType?.elementTypes?.getOrNull(index),
                    typeResolver = typeResolver,
                )
            }
        }

        is CfirEnumPattern -> {
            val argumentTypes = resolveEnumArgumentTypes(pattern, expectedType)
            pattern.arguments.forEachIndexed { index, argument ->
                resolvePatternBindingTypes(
                    pattern = argument,
                    expectedType = argumentTypes.getOrNull(index),
                    typeResolver = typeResolver,
                )
            }
        }

        is CfirOrPattern -> pattern.alternatives.forEach { alternative ->
            resolvePatternBindingTypes(alternative, expectedType, typeResolver)
        }

        else -> Unit
    }
}

internal fun CfirPartialBodyResolveTransformer.registerPatternBindings(pattern: CfirPattern) {
    if (pattern is CfirOrPattern) return
    for (bindingVariable in pattern.bindingVariables()) {
        context.storeVariable(bindingVariable, session)
    }
}

private fun CfirPartialBodyResolveTransformer.resolvePatternTypeRefIfNeeded(
    typeRef: CfirTypeRef,
    typeResolver: CfirSpecificTypeResolverTransformer,
): CfirTypeRef {
    if (typeRef is CfirResolvedTypeRef || typeRef is CfirImplicitTypeRef) return typeRef
    return typeResolver.transformTypeRef(typeRef, patternTypeResolutionConfiguration())
}

private fun CfirPartialBodyResolveTransformer.patternTypeResolutionConfiguration(): CfirTypeResolutionConfiguration {
    return CfirTypeResolutionConfiguration(
        useSiteFile = context.file,
        topContainer = context.containers.lastOrNull(),
    )
}

private fun CfirPartialBodyResolveTransformer.resolveEnumArgumentTypes(
    pattern: CfirEnumPattern,
    expectedType: ConeCangJieType?,
): List<ConeCangJieType> {
    val enumType = expectedType?.expandedPatternEnumType() ?: return emptyList()
    val optionArgumentTypes = resolveStdlibOptionArgumentTypes(pattern, enumType)
    if (optionArgumentTypes != null) return optionArgumentTypes

    if (enumType !is ConeEnumType) return emptyList()
    val enumDeclaration = (session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum)
        ?: return emptyList()

    val constructorName = extractEnumConstructorName(pattern) ?: return emptyList()
    val enumConstructor = enumDeclaration.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .firstOrNull { candidate ->
            candidate.name == constructorName && candidate.payloadArity() == pattern.arguments.size
        }
        ?: return emptyList()

    return enumConstructor.substitutedPayloadParameterTypes(enumDeclaration, enumType)
}

private fun ConeCangJieType.expandedPatternEnumType(): ConeCangJieType = when (this) {
    is ConeTypeAliasType -> expandedType?.expandedPatternEnumType() ?: this
    else -> this
}

/**
 * 标准库 `Option<T>` 在当前类型系统中以 class-like 类型承载，
 * 但官方语义仍是 `Some(T)` / `None` 的泛型 enum。
 */
private fun resolveStdlibOptionArgumentTypes(
    pattern: CfirEnumPattern,
    expectedType: ConeCangJieType,
): List<ConeCangJieType>? {
    val optionType = expectedType as? ConeClassLikeType ?: return null
    if (optionType.classId != StdlibClassIds.Option) return null

    val constructorName = extractEnumConstructorName(pattern)
    val optionArgumentType = optionType.typeArguments.singleOrNull()?.type
    return when {
        constructorName == OPTION_SOME_CONSTRUCTOR_NAME &&
                pattern.arguments.size == 1 &&
                optionArgumentType != null -> listOf(optionArgumentType)
        constructorName == OPTION_NONE_CONSTRUCTOR_NAME &&
                pattern.arguments.isEmpty() -> emptyList()
        constructorName == OPTION_SOME_CONSTRUCTOR_NAME ||
                constructorName == OPTION_NONE_CONSTRUCTOR_NAME -> emptyList()
        else -> null
    }
}

private fun extractEnumConstructorName(pattern: CfirEnumPattern): Name? {
    return when (val reference = pattern.constructorReference) {
        is CfirResolvedNamedReference -> reference.name
        is CfirNamedReference -> reference.name
        else -> null
    }
}

private fun CfirPatternBindingVariable.replaceBindingType(type: ConeCangJieType?) {
    if (type == null) return
    val currentTypeRef = returnTypeRef
    if (currentTypeRef is CfirResolvedTypeRef && currentTypeRef.coneType == type) return
    replaceReturnTypeRef(currentTypeRef.resolvedTypeFromPrototype(type, currentTypeRef.source))
}
