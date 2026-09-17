package org.cangnova.cangjie.cfir.resolve.transformers.plugin

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationArgumentMappingImpl
import org.cangnova.cangjie.cfir.expressions.impl.CfirEmptyAnnotationArgumentMapping
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirMapArguments
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.*

/** 官方内置注解没有用户类构造器，由 Registry 提供签名并沿共享参数映射发布结果。 */
internal fun resolveBuiltinAnnotationArguments(
    annotation: CfirAnnotationCall,
    descriptor: BuiltInAnnotationDescriptor,
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) {
    val schema = descriptor.argumentSchema
    val sourceArguments = annotation.argumentList.arguments.toList()

    /**
     * Attribute/overflow/when are parser-owned syntax, not calls to a
     * synthetic annotation constructor.  Their raw argument list is still
     * retained for rendering and declaration-owned consumers, while the
     * resolved annotation mapping remains empty because there is no ordinary
     * parameter binding for these forms.
     */
    if (descriptor.argumentSyntax in setOf(
            CangjieAnnotationArgumentSyntax.ATTRIBUTE_TOKENS,
            CangjieAnnotationArgumentSyntax.OVERFLOW_STRATEGY,
            CangjieAnnotationArgumentSyntax.WHEN_CONDITION,
        )
    ) {
        annotation.replaceArgumentMapping(CfirEmptyAnnotationArgumentMapping)
        annotation.replaceArgumentView(
            CfirAnnotationArgumentView(
                sourceArguments.mapIndexed { index, argument ->
                    CfirAnnotationArgumentViewEntry(
                        sourceOrder = index,
                        explicitName = (argument as? CfirNamedArgumentExpression)?.argumentName,
                        argument = argument,
                        resolvedParameter = null,
                        status = if (argument.coneTypeOrNull?.isError == true) {
                            CfirAnnotationArgumentStatus.ERROR
                        } else {
                            CfirAnnotationArgumentStatus.RESOLVED
                        },
                        isDefaultOrigin = false,
                        constantExpression = (argument as? CfirLiteralExpression),
                        source = argument.source,
                    )
                },
            ),
        )
        annotation.replaceAnnotationResolveState(CfirAnnotationResolveState.SEMANTIC_RESOLVED)
        annotation.replaceConeTypeOrNull(ConePrimitiveType.UNIT)
        return
    }
    val callableSymbol = CfirNamedFunctionSymbol(CallableId(FqName("cangjie.internal.annotations"), Name.identifier(descriptor.sourceName)))
    val parameters = schema.parameters.map { parameter ->
        buildValueParameter {
            moduleData = annotation.containingDeclarationSymbol.cfir.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            name = Name.identifier(parameter.name)
            symbol = CfirValueParameterSymbol(CallableId(name))
            containingDeclarationSymbol = callableSymbol
            isLocal = false
            isNamed = !parameter.acceptsPositional
            returnTypeRef = buildResolvedTypeRef {
                coneType = when (parameter.kind) {
                    AnnotationParameterKind.BOOLEAN -> ConePrimitiveType.BOOLEAN
                    AnnotationParameterKind.INTEGER -> ConePrimitiveType.INT64
                    AnnotationParameterKind.STRING, AnnotationParameterKind.REFERENCE -> ConeClassLikeType(StdlibClassIds.String.toLookupTag())
                    else -> ConeClassLikeType(StdlibClassIds.Any.toLookupTag())
                }
            }
            defaultValue = when (val value = parameter.defaultValue) {
                is AnnotationDefaultValue.BooleanValue -> buildLiteralExpression { kind = CfirLiteralKind.BOOLEAN; this.value = value.value; coneTypeOrNull = ConePrimitiveType.BOOLEAN }
                is AnnotationDefaultValue.StringValue -> buildLiteralExpression { kind = CfirLiteralKind.STRING; this.value = value.value; coneTypeOrNull = ConeClassLikeType(StdlibClassIds.String.toLookupTag()) }
                null -> null
            }
        }
    }
    buildNamedFunction {
        moduleData = annotation.containingDeclarationSymbol.cfir.moduleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        isMut = false
        status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
        name = callableSymbol.callableId.callableName
        symbol = callableSymbol
        returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.UNIT }
        valueParameters += parameters
    }

    val resolved = CfirMapArguments.mapAnnotationArguments(annotation, schema, parameters)
    val values = linkedMapOf<Name, CfirExpression>()
    val view = resolved.toAnnotationArgumentView(parameters).entries.toMutableList()
    var failed = false
    for ((index, entry) in view.withIndex()) {
        val argument = entry.argument
        val expression = (argument as? CfirNamedArgumentExpression)?.expression ?: argument
        val parameter = entry.resolvedParameter
        if (entry.status != CfirAnnotationArgumentStatus.RESOLVED) {
            if (!schema.variadic) failed = true
            continue
        }
        if (parameter == null) continue
        val parameterSchema = schema.parameters.single { it.name == parameter.name.asString() }
        val value = if (entry.isDefaultOrigin) parameter.defaultValue else when (parameterSchema.kind) {
            AnnotationParameterKind.REFERENCE -> {
                val reference = (expression as? CfirQualifiedAccessExpression)?.takeIf { it.explicitReceiver == null }?.calleeReference as? CfirNamedReference
                reference?.let { buildLiteralExpression {
                    source = expression?.source
                    kind = CfirLiteralKind.STRING
                    this.value = it.name.asString()
                    coneTypeOrNull = ConeClassLikeType(StdlibClassIds.String.toLookupTag())
                } }
            }
            else -> expression?.transform<CfirExpression, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
        }
        if (value != null) values[parameter.name] = value
        val isValidKind = when (parameterSchema.kind) {
            AnnotationParameterKind.STRING -> value is CfirLiteralExpression && value.value is String
            AnnotationParameterKind.BOOLEAN -> value is CfirLiteralExpression && value.value is Boolean
            AnnotationParameterKind.INTEGER -> value is CfirLiteralExpression && value.kind == CfirLiteralKind.INT
            AnnotationParameterKind.REFERENCE -> value != null
            AnnotationParameterKind.TARGET_ARRAY -> value is CfirArrayLiteral
            AnnotationParameterKind.EXPRESSION -> value != null
        }
        if (!isValidKind) failed = true
        view[index] = entry.copy(
            status = if (isValidKind) entry.status else CfirAnnotationArgumentStatus.ERROR,
            constantExpression = value,
        )
    }
    for (parameterSchema in schema.parameters.filter { it.required }) {
        if (view.none { it.resolvedParameter?.name?.asString() == parameterSchema.name }) {
            failed = true
            val parameter = parameters.single { it.name.asString() == parameterSchema.name }
            view += CfirAnnotationArgumentViewEntry(view.size, null, null, parameter, CfirAnnotationArgumentStatus.MISSING, false, null, annotation.source)
        }
    }
    annotation.replaceArgumentMapping(CfirAnnotationArgumentMappingImpl(annotation.source, values))
    annotation.replaceArgumentView(CfirAnnotationArgumentView(view))
    annotation.replaceCalleeReference(buildResolvedNamedReference {
        source = annotation.calleeReference.source
        name = callableSymbol.callableId.callableName
        resolvedSymbol = callableSymbol
    })
    annotation.replaceAnnotationResolveState(if (failed) CfirAnnotationResolveState.ERROR else CfirAnnotationResolveState.SEMANTIC_RESOLVED)
    annotation.replaceConeTypeOrNull(ConePrimitiveType.UNIT)
}
