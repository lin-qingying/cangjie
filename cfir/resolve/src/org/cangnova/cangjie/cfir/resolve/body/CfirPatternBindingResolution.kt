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
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name

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
        val symbol = bindingVariable.symbol as? CfirCallableSymbol<*> ?: continue
        context.storeVariable(bindingVariable.name, symbol)
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
    val enumType = expectedType as? ConeEnumType ?: return emptyList()
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
