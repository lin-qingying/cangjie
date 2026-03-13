package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

private val RULE_TYPES_ERROR_RECOVERY = CfirResolveRuleCatalog.TYPES_ERROR_RECOVERY

internal class CfirExplicitTypeRefResolver(
    private val session: CfirSession,
    private val diagnosticReporter: CfirDiagnosticReporter,
) {
    fun resolveExplicitTypeRef(
        typeRef: CfirTypeRef,
        scopeTypeParameters: Map<String, CfirTypeParameter>,
    ): CfirTypeRef = when (typeRef) {
        is CfirUserTypeRef -> resolveUserTypeRef(typeRef, scopeTypeParameters)
        is CfirFunctionTypeRef -> {
            val parameterTypes = typeRef.parameterTypeRefs.map { resolveConeType(it, scopeTypeParameters) }
            val returnType = resolveConeType(typeRef.returnTypeRef, scopeTypeParameters)
            buildResolvedTypeRef {
                coneType = ConeFuncType(parameterTypes = parameterTypes, returnType = returnType)
            }
        }
        is CfirTupleTypeRef -> {
            val elementTypes = typeRef.elementTypeRefs.map { resolveConeType(it, scopeTypeParameters) }
            buildResolvedTypeRef {
                coneType = ConeTupleType(elementTypes = elementTypes)
            }
        }
        is CfirVArrayTypeRef -> {
            val elementType = resolveConeType(typeRef.elementTypeRef, scopeTypeParameters)
            val size = typeRef.sizeLiteral.toLongOrNull()
            buildResolvedTypeRef {
                coneType = if (size != null) {
                    ConeVArrayType(elementType = elementType, size = size)
                } else {
                    ConeErrorType("Invalid VArray size: ${typeRef.sizeLiteral}")
                }
            }
        }
        else -> typeRef
    }

    private fun resolveUserTypeRef(
        userTypeRef: CfirUserTypeRef,
        scopeTypeParameters: Map<String, CfirTypeParameter>,
    ): CfirTypeRef {
        if (userTypeRef.qualifier.isEmpty()) return userTypeRef

        if (userTypeRef.qualifier.size == 1) {
            val typeParameterName = userTypeRef.qualifier.single().asString()
            if (scopeTypeParameters.containsKey(typeParameterName)) {
                return buildResolvedTypeRef {
                    coneType = ConeTypeParameterType(
                        lookupTag = ConeTypeParameterLookupTag(typeParameterName),
                    )
                }
            }
        }

        val className = userTypeRef.qualifier.last()
        val packageName = userTypeRef.qualifier.dropLast(1).joinToString(".") { it.asString() }
        val packageFqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)
        val classId = ClassId(packageFqName, className)
        val resolvedClass = session.cfirProvider.getClassByClassId(classId)

        return if (resolvedClass != null) {
            val resolvedArguments = userTypeRef.typeArguments.map { argument ->
                resolveConeType(argument, scopeTypeParameters)
            }
            buildResolvedTypeRef {
                coneType = ConeClassLikeType(
                    lookupTag = ConeClassLookupTagImpl(classId),
                    typeArguments = resolvedArguments,
                    isInterface = resolvedClass.classKind == CfirClassKind.INTERFACE,
                )
            }
        } else {
            val renderedType = userTypeRef.qualifier.joinToString(".") { it.asString() }
            val reason = "unresolved explicit type '$renderedType'"
            diagnosticReporter.reportOn(
                source = userTypeRef.source,
                factory = CfirErrors.TYPES_ERROR_RECOVERY,
                a = RULE_TYPES_ERROR_RECOVERY.id,
                b = "$reason (${RULE_TYPES_ERROR_RECOVERY.officialReference})",
                context = DiagnosticContext.Default,
            )
            buildErrorTypeRef { this.reason = reason }
        }
    }

    private fun resolveConeType(
        typeRef: CfirTypeRef,
        scopeTypeParameters: Map<String, CfirTypeParameter>,
    ): ConeCangjieType {
        val errorTypeRef = typeRef as? CfirErrorTypeRef
        if (errorTypeRef != null) return ConeErrorType(errorTypeRef.reason)
        val resolvedTypeRef = resolveExplicitTypeRef(typeRef, scopeTypeParameters) as? CfirResolvedTypeRef
        return resolvedTypeRef?.coneType ?: ConeErrorType("Unresolved type: $typeRef")
    }
}
