package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl

import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 鐎靛綊缍?Kotlin `FirTypeResolver` 閻ㄥ嫪绱扮拠婵堢矋娴犺埖濞婄挒鈽呮嫹? */
abstract class CfirTypeResolver : CfirSessionComponent {
    abstract fun resolveType(
        typeRef: CfirTypeRef,
        configuration: TypeResolutionConfiguration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: SupertypeSupplier,
        expandTypeAliases: Boolean = true,
    ): CfirTypeResolutionResult

    abstract fun resolveClass(typeRef: CfirTypeRef): CfirClass?

    abstract fun resolveClass(classId: ClassId): CfirClass?
}

data class CfirTypeResolutionResult(
    val type: ConeCangjieType,
    val diagnostic: ConeDiagnostic?,
)

/**
 * Supertype supplier hook used by the SUPER_TYPES phase.
 */
fun interface SupertypeSupplier {
    fun getSupertypes(classId: ClassId): List<ConeCangjieType>

    companion object {
        val Default: SupertypeSupplier = SupertypeSupplier { emptyList() }
    }
}

class CfirTypeResolverImpl(
    private val session: CfirSession,
) : CfirTypeResolver() {

    override fun resolveType(
        typeRef: CfirTypeRef,
        configuration: TypeResolutionConfiguration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: SupertypeSupplier,
        expandTypeAliases: Boolean,
    ): CfirTypeResolutionResult {
        val type = when (typeRef) {
            is CfirResolvedTypeRef -> typeRef.coneType
            is CfirImplicitTypeRef -> ConeErrorType("Implicit type reference is not resolvable at this stage")
            is CfirBasicTypeRef -> resolveBasicType(typeRef, configuration)
            is CfirUserTypeRef -> resolveUserType(typeRef, configuration)
            is CfirFunctionTypeRef -> {
                val parameterTypes = typeRef.parameterTypeRefs.map { resolveType(it, configuration, areBareTypesAllowed, isOperandOfIsOperator, resolveDeprecations, supertypeSupplier, expandTypeAliases).type }
                val returnType = resolveType(typeRef.returnTypeRef, configuration, areBareTypesAllowed, isOperandOfIsOperator, resolveDeprecations, supertypeSupplier, expandTypeAliases).type
                ConeFuncType(parameterTypes = parameterTypes, returnType = returnType)
            }
            is CfirTupleTypeRef -> {
                val elementTypes = typeRef.elementTypeRefs.map { resolveType(it, configuration, areBareTypesAllowed, isOperandOfIsOperator, resolveDeprecations, supertypeSupplier, expandTypeAliases).type }
                ConeTupleType(elementTypes = elementTypes)
            }
            is CfirVArrayTypeRef -> {
                val elementType = resolveType(typeRef.elementTypeRef, configuration, areBareTypesAllowed, isOperandOfIsOperator, resolveDeprecations, supertypeSupplier, expandTypeAliases).type
                val size = typeRef.sizeLiteral.toLongOrNull()
                if (size != null) {
                    ConeVArrayType(elementType = elementType, size = size)
                } else {
                    ConeErrorType("Invalid VArray size: ${typeRef.sizeLiteral}")
                }
            }
            is CfirErrorTypeRef -> ConeErrorType(typeRef.reason)
            else -> ConeErrorType("Unsupported type reference: ${typeRef::class.simpleName}")
        }

        return CfirTypeResolutionResult(
            type = type,
            diagnostic = (type as? ConeErrorType)?.diagnostic,
        )
    }

    override fun resolveClass(typeRef: CfirTypeRef): CfirClass? {
        val userTypeRef = typeRef as? CfirUserTypeRef ?: return null
        if (userTypeRef.qualifier.isEmpty()) return null

        val className = userTypeRef.qualifier.last()
        val packageName = userTypeRef.qualifier.dropLast(1).joinToString(".") { it.asString() }
        val packageFqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)
        return resolveClass(ClassId(packageFqName, className))
    }

    override fun resolveClass(classId: ClassId): CfirClass? {
        session.cfirProvider.getClassByClassId(classId)?.let { return it }
        return session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirClass
    }

    private fun resolveBasicType(
        typeRef: CfirBasicTypeRef,
        configuration: TypeResolutionConfiguration,
    ): ConeCangjieType {
        val typeName = typeRef.name.asString()
        if (configuration.scopeTypeParameters.containsKey(typeName)) {
            return ConeTypeParameterType(ConeTypeParameterLookupTag(typeName))
        }

        val primitiveType = session.builtinTypes.getPrimitiveTypeByName(typeRef.name.asString())
        return primitiveType ?: ConeErrorType("Unknown basic type: ${typeRef.name.asString()}")
    }

    private fun resolveUserType(
        typeRef: CfirUserTypeRef,
        configuration: TypeResolutionConfiguration,
    ): ConeCangjieType {
        if (typeRef.qualifier.isEmpty()) {
            return ConeErrorType("Empty user type")
        }

        if (typeRef.qualifier.size == 1) {
            val typeParameterName = typeRef.qualifier.single().asString()
            if (configuration.scopeTypeParameters.containsKey(typeParameterName)) {
                return ConeTypeParameterType(ConeTypeParameterLookupTag(typeParameterName))
            }
        }

        val className = typeRef.qualifier.last()
        val classId = if (typeRef.qualifier.size == 1) {
            resolveSimpleClassId(className, configuration) ?: ClassId(FqName.ROOT, className)
        } else {
            val packageName = typeRef.qualifier.dropLast(1).joinToString(".") { it.asString() }
            val packageFqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)
            ClassId(packageFqName, className)
        }

        val resolvedClass = resolveClass(classId) ?: return ConeErrorType(ConeUnresolvedSymbolError(classId))
        val resolvedArguments = typeRef.typeArguments.map { argument ->
            resolveType(
                argument,
                configuration,
                areBareTypesAllowed = false,
                isOperandOfIsOperator = false,
                resolveDeprecations = true,
                supertypeSupplier = SupertypeSupplier.Default,
                expandTypeAliases = true,
            ).type
        }

        val lookupTag = ConeClassLookupTagImpl(classId)
        return when (resolvedClass.classKind) {
            CfirClassKind.CLASS, CfirClassKind.INTERFACE -> ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = resolvedArguments,
                isInterface = resolvedClass.classKind == CfirClassKind.INTERFACE,
            )
            CfirClassKind.STRUCT -> ConeStructType(
                lookupTag = lookupTag,
                typeArguments = resolvedArguments,
            )
            CfirClassKind.ENUM -> ConeEnumType(
                lookupTag = lookupTag,
                typeArguments = resolvedArguments,
            )
        }
    }

    private fun resolveSimpleClassId(
        shortName: Name,
        configuration: TypeResolutionConfiguration,
    ): ClassId? {
        val candidates = LinkedHashSet<ClassId>()

        val file = configuration.useSiteFile
        if (file != null) {
            val simpleName = shortName.asString()
            for (importInfo in file.imports) {
                val importedFqName = importInfo.importedFqName ?: continue
                if (importInfo.isAllUnder) {
                    candidates += ClassId(importedFqName, shortName)
                    continue
                }
                val importedName = importInfo.aliasName?.asString() ?: importedFqName.shortName().asString()
                if (importedName == simpleName) {
                    candidates += ClassId.topLevel(importedFqName)
                }
            }
            candidates += ClassId(file.packageDirective.packageFqName, shortName)
        }

        val defaultImportsProvider = session.defaultImportsProvider
        val defaultImports = defaultImportsProvider.getDefaultImports(includeLowPriorityImports = true)
            .filter { it.fqName !in defaultImportsProvider.excludedImports }
        addDefaultImportCandidates(candidates, defaultImports, shortName)

        return candidates.firstOrNull { resolveClass(it) != null }
    }

    private fun addDefaultImportCandidates(
        candidates: MutableSet<ClassId>,
        imports: List<ImportPath>,
        shortName: Name,
    ) {
        val simpleName = shortName.asString()
        for (importPath in imports) {
            if (importPath.isAllUnder) {
                candidates += ClassId(importPath.fqName, shortName)
                continue
            }
            val importedName = importPath.alias?.asString() ?: importPath.fqName.shortName().asString()
            if (importedName == simpleName) {
                candidates += ClassId.topLevel(importPath.fqName)
            }
        }
    }
}

