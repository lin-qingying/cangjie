package org.cangnova.cangjie.cfir.copy

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationArgumentMappingImpl
import org.cangnova.cangjie.cfir.expressions.impl.CfirEmptyAnnotationArgumentMapping
import org.cangnova.cangjie.cfir.expressions.impl.CfirArgumentListImpl
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRefCopy
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name

/** 手写非生成节点的复制；data=false 分配壳，data=true 回填边。 */
@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
internal abstract class CfirOwnedGraphCopyVisitor(protected val context: CfirOwnedGraphCopyContext) :
    CfirVisitor<CfirElement, Boolean>() {
    override fun visitElement(element: CfirElement, data: Boolean): CfirElement =
        if (element === CfirStubReference) element
        else error("CFIR node has no owned-copy contract: " + element::class)

    override fun visitDeclarationStatus(original: CfirDeclarationStatus, data: Boolean): CfirElement =
        if (data) context.copyOwned(original) else cloneDeclarationStatus(original)

    /** 手写classifier维持实际concrete kind与symbol；借用classifier不因此进入拥有闭包。 */
    override fun visitClassLikeDeclaration(original: CfirClassLikeDeclaration, data: Boolean): CfirElement {
        if (!data) return when (original) {
            is CfirBuiltInDeclaration -> CfirBuiltInDeclaration(
                moduleData = original.moduleData, symbol = context.remapSymbol(original.symbol),
                name = context.name(original, original.name), kind = original.kind, scopeProvider = original.scopeProvider,
                annotations = MutableOrEmptyList.empty(), origin = original.origin,
                attributes = context.copyAttributes(original.attributes), contentSource = CfirContentSource.Owned,
            )
            is CfirPrimitiveTypeDeclaration -> CfirPrimitiveTypeDeclaration(
                moduleData = original.moduleData, symbol = context.remapSymbol(original.symbol),
                name = context.name(original, original.name), kind = original.kind, scopeProvider = original.scopeProvider,
                annotations = MutableOrEmptyList.empty(), origin = original.origin, attributes = context.copyAttributes(original.attributes),
            )
            else -> error("Class-like declaration has no manual concrete owned-copy contract: " + original::class)
        }
        val copied = context.copyOwned(original)
        check(copied::class == original::class)
        copied.replaceAnnotations(context.annotations(original).map { context.copyOwned(it) })
        copied.replaceStatus(context.copyOwned(original.status))
        when (copied) {
            is CfirBuiltInDeclaration -> {
                copied.typeParameters = original.typeParameters.map { context.copyOwned(it) }.toMutableList()
                copied.declarations = original.declarations.map { context.copyOwned(it) }.toMutableList()
                copied.superTypeRefs = original.superTypeRefs.map { context.copyOwned(it) }.toMutableList()
            }
            is CfirPrimitiveTypeDeclaration -> {
                copied.typeParameters = original.typeParameters.map { context.copyOwned(it) }.toMutableList()
                copied.declarations = original.declarations.map { context.copyOwned(it) }.toMutableList()
                copied.superTypeRefs = original.superTypeRefs.map { context.copyOwned(it) }.toMutableList()
            }
            else -> error("Manual classifier changed its concrete owned-copy kind")
        }
        copied.replaceDeprecationsProvider(UnresolvedDeprecationProvider)
        copied.resolveState = original.resolvePhase.asResolveState()
        return copied
    }

    override fun visitArgumentList(original: CfirArgumentList, data: Boolean): CfirElement {
        if (original === CfirEmptyArgumentList) return original
        if (!data) return when (original) {
            is CfirResolvedArgumentListImpl -> {
                val mapping = linkedMapOf<CfirExpression, CfirValueParameter>()
                CfirResolvedArgumentListImpl(
                    original.originalArgumentList?.let { context.allocate(it) }, mapping,
                    if (original.mapping === original.mappingIncludingContextArguments) mapping else linkedMapOf(),
                )
            }
            is CfirResolvedArgumentListForErrorCall -> CfirResolvedArgumentListForErrorCall(
                original.originalArgumentList?.let { context.allocate(it) }, linkedMapOf<CfirExpression, CfirValueParameter?>(),
            )
            is CfirArgumentListImpl -> buildArgumentList { source = original.source }
            else -> error("Argument list has no concrete owned-copy contract: " + original::class)
        }
        val copied = context.copyOwned(original)
        when {
            original is CfirResolvedArgumentListImpl && copied is CfirResolvedArgumentListImpl -> {
                original.mapping.forEach { (argument, parameter) ->
                    copied.mapping[context.copyOwned(argument)] = context.remapDeclaration(parameter)
                }
                if (copied.mappingIncludingContextArguments !== copied.mapping) {
                    original.mappingIncludingContextArguments.forEach { (argument, parameter) ->
                        copied.mappingIncludingContextArguments[context.copyOwned(argument)] = context.remapDeclaration(parameter)
                    }
                }
                copied.replaceCompletedArgumentsForOwnedCopy(original.arguments.map { context.copyOwned(it) },
                    original.cVariadicArguments?.map { context.copyOwned(it) }, original.usesCangjieVariadicCall)
            }
            original is CfirResolvedArgumentListForErrorCall && copied is CfirResolvedArgumentListForErrorCall -> {
                val mapping = linkedMapOf<CfirExpression, CfirValueParameter?>()
                original.rawMappingForOwnedCopy.forEach { (argument, parameter) ->
                    mapping[context.copyOwned(argument)] = parameter?.let { context.remapDeclaration(it) }
                }
                copied.replaceMappingForOwnedCopy(mapping)
            }
            original is CfirArgumentListImpl && copied is CfirArgumentListImpl -> {
                val arguments = (copied as org.cangnova.cangjie.cfir.expressions.impl.CfirArgumentListImpl).arguments
                arguments.clear()
                arguments.addAll(original.arguments.map { context.copyOwned(it) })
            }
            else -> error("Argument list changed concrete kind during owned copy")
        }
        return copied
    }

    override fun visitAnnotationArgumentMapping(original: CfirAnnotationArgumentMapping, data: Boolean): CfirElement {
        if (original === CfirEmptyAnnotationArgumentMapping) return original
        check(original is CfirAnnotationArgumentMappingImpl) { "Annotation mapping has no concrete owned-copy contract" }
        if (!data) return CfirAnnotationArgumentMappingImpl(original.source, linkedMapOf())
        val copied = context.copyOwned(original)
        @Suppress("UNCHECKED_CAST")
        val mapping = copied.mapping as MutableMap<Name, CfirExpression>
        original.mapping.forEach { (name, expression) -> mapping[name] = context.copyOwned(expression) }
        return copied
    }

    /** 手写 expression 的 concrete kind 与所有字段沿同一复制域保存。 */
    protected fun copyHandWrittenExpression(original: CfirExpression, data: Boolean): CfirExpression {
        if (!data) return when (original) {
            is CfirInaccessibleReceiverExpression -> CfirInaccessibleReceiverExpression(
                original.source, emptyList(), original.coneTypeOrNull?.let { context.type(it) },
                original.calleeReference, original.kind,
            )
            is CfirThisReceiverExpression -> {
                check(original::class == CfirThisReceiverExpression::class)
                CfirThisReceiverExpression(original.source, emptyList(), original.coneTypeOrNull?.let { context.type(it) }, original.calleeReference)
            }
            is CfirSmartCastExpression -> CfirSmartCastExpression(
                original.source, emptyList(), original.coneTypeOrNull?.let { context.type(it) },
                original.originalExpression, original.smartcastType,
                original.upperTypesFromSmartCast.map { context.type(it) },
                original.lowerTypesFromSmartCast.map { context.type(it) }, original.smartcastStability,
            )
            is CfirConstantValueExpression -> CfirConstantValueExpression(original.source, context.copyConstant(original.constantValue))
            else -> error("Expression has no concrete owned-copy contract: " + original::class)
        }
        val copied = context.copyOwned(original)
        copied.replaceAnnotations(original.annotations.map { context.copyOwned(it) })
        when {
            original is CfirThisReceiverExpression && copied is CfirThisReceiverExpression ->
                copied.calleeReference = context.copyOwned(original.calleeReference)
            original is CfirSmartCastExpression && copied is CfirSmartCastExpression -> {
                copied.originalExpression = context.copyOwned(original.originalExpression)
                copied.smartcastType = context.copyOwned(original.smartcastType)
            }
            original is CfirConstantValueExpression && copied is CfirConstantValueExpression -> Unit
            else -> error("Expression changed concrete kind during owned copy")
        }
        return copied
    }

    override fun visitErrorTypeRef(original: CfirErrorTypeRef, data: Boolean): CfirElement {
        if (!data) return buildErrorTypeRefCopy(original) {
            coneType = context.type(original.coneType)
        }
        val copied = context.copyOwned(original) as org.cangnova.cangjie.cfir.types.impl.CfirErrorTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.delegatedTypeRef = original.delegatedTypeRef?.let { context.copyOwned(it) }
        copied.partiallyResolvedTypeRef = original.partiallyResolvedTypeRef?.let { context.copyOwned(it) }
        return copied
    }

    protected fun copyArgumentView(view: CfirAnnotationArgumentView): CfirAnnotationArgumentView =
        CfirAnnotationArgumentView(view.entries.map { entry ->
            entry.copy(
                argument = entry.argument?.let { context.copyOwned(it) },
                resolvedParameter = entry.resolvedParameter?.let { context.remapDeclaration(it) },
                constantExpression = entry.constantExpression?.let {
                    if (entry.isDefaultOrigin) context.remapElement(it) else context.copyOwned(it)
                },
            )
        })

    protected fun copyOperatorSignature(signature: BuiltinPrimitiveOperatorSignature): BuiltinPrimitiveOperatorSignature =
        signature.copy(parameterKinds = signature.parameterKinds.toList())

    protected fun copyAppliedReference(original: CfirResolvedAppliedCallableReference): CfirResolvedAppliedCallableReference =
        CfirResolvedAppliedCallableReference(
            original.source, context.name(original, original.name), context.remapSymbol(original.resolvedSymbol),
            original.substitutedReturnType?.let { context.type(it) },
            original.substitutedParameterTypes.map { context.type(it) }, original.isFunctionValue,
            when (val parent = original.matchedParentType) {
                org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Absent -> parent
                org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Invalid -> parent
                is org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Instantiated ->
                    org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Instantiated(context.type(parent.type))
            },
            original.callSiteType?.let { context.type(it) },
        )
}
