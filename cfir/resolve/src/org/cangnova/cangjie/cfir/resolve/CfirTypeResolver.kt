package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
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
import org.cangnova.cangjie.cfir.types.CfirOptionTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
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

    abstract fun resolveClass(typeRef: CfirTypeRef): CfirClassLikeDeclaration?

    abstract fun resolveClass(classId: ClassId): CfirClassLikeDeclaration?
}

data class CfirTypeResolutionResult(
    val type: ConeCangJieType,
    val diagnostic: ConeDiagnostic?,
)

/**
 * Supertype supplier hook used by the SUPER_TYPES phase.
 */
fun interface SupertypeSupplier {
    fun getSupertypes(classId: ClassId): List<ConeCangJieType>

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
            is CfirImplicitTypeRef -> ConeErrorType(ConeSimpleDiagnostic("Implicit type reference is not resolvable at this stage"))
            is CfirBasicTypeRef -> resolveBasicType(typeRef, configuration)
            is CfirUserTypeRef -> resolveUserType(typeRef, configuration)
            is CfirOptionTypeRef -> resolveOptionType(typeRef, configuration, expandTypeAliases)
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
                    ConeErrorType(ConeSimpleDiagnostic("Invalid VArray size: ${typeRef.sizeLiteral}"))
                }
            }
            is CfirErrorTypeRef -> ConeErrorType(typeRef.diagnostic)
            else -> ConeErrorType(ConeSimpleDiagnostic("Unsupported type reference: ${typeRef::class.simpleName}"))
        }

        return CfirTypeResolutionResult(
            type = type,
            diagnostic = (type as? ConeErrorType)?.diagnostic,
        )
    }

    override fun resolveClass(typeRef: CfirTypeRef): CfirClassLikeDeclaration? {
        val userTypeRef = typeRef as? CfirUserTypeRef ?: return null
        if (userTypeRef.qualifier.isEmpty()) return null

        val className = userTypeRef.qualifier.last()
        val packageName = userTypeRef.qualifier.dropLast(1).joinToString(".") { it.asString() }
        val packageFqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)
        return resolveClass(ClassId(packageFqName, className))
    }

    override fun resolveClass(classId: ClassId): CfirClassLikeDeclaration? {
        session.cfirProvider.getCfirClassifierByFqName(classId)?.let { return it }
        return session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
    }

    private fun resolveBasicType(
        typeRef: CfirBasicTypeRef,
        configuration: TypeResolutionConfiguration,
    ): ConeCangJieType {
        val typeName = typeRef.name.asString()
        val typeParameter = configuration.scopeTypeParameters[typeName]
        if (typeParameter != null) {
            return ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }

        val primitiveType = session.builtinTypes.getPrimitiveTypeByName(typeRef.name.asString())
        return primitiveType ?: ConeErrorType(ConeSimpleDiagnostic("Unknown basic type: ${typeRef.name.asString()}"))
    }

    private fun resolveUserType(
        typeRef: CfirUserTypeRef,
        configuration: TypeResolutionConfiguration,
        expandTypeAliases: Boolean = true,
    ): ConeCangJieType {
        if (typeRef.qualifier.isEmpty()) {
            return ConeErrorType(ConeSimpleDiagnostic("Empty user type"))
        }

        if (typeRef.qualifier.size == 1) {
            val typeParameterName = typeRef.qualifier.single().asString()
            val typeParameter = configuration.scopeTypeParameters[typeParameterName]
            if (typeParameter != null) {
                return ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
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
        if (resolvedClass.typeParameters.isNotEmpty() && typeRef.typeArguments.isEmpty()) {
            return ConeErrorType(
                ConeSimpleDiagnostic(
                    "generic type '${resolvedClass.name.asString()}' should be used with type argument",
                    DiagnosticKind.GenericTypeWithoutTypeArgument,
                )
            )
        }
        val resolvedArguments = typeRef.typeArguments.map { argument ->
            ConeTypeProjection(
                resolveType(
                    argument,
                    configuration,
                    areBareTypesAllowed = false,
                    isOperandOfIsOperator = false,
                    resolveDeprecations = true,
                    supertypeSupplier = SupertypeSupplier.Default,
                    expandTypeAliases = expandTypeAliases,
                ).type
            )
        }

        return when (resolvedClass) {
            is CfirPrimitiveTypeDeclaration -> ConePrimitiveType(resolvedClass.kind)
            is CfirClass -> ConeClassLikeType(
                lookupTag = ConeClassLikeLookupTagImpl(classId),
                typeArguments = resolvedArguments,
            )
            is CfirInterface -> ConeClassLikeType(
                lookupTag = ConeClassLikeLookupTagImpl(classId),
                typeArguments = resolvedArguments,
                isInterface = true,
            )
            is CfirStruct -> ConeStructType(
                lookupTag = ConeClassLikeLookupTagImpl(classId),
                typeArguments = resolvedArguments,
            )
            is CfirEnum -> ConeEnumType(
                lookupTag = ConeClassLikeLookupTagImpl(classId),
                typeArguments = resolvedArguments,
                isRefEnum = resolvedClass.isRefEnum,
            )
            is CfirTypeAlias -> {
                val expandedType = resolvedClass.expandedTypeRef.coneTypeOrNull
                    ?: resolveType(
                        resolvedClass.expandedTypeRef,
                        configuration,
                        areBareTypesAllowed = false,
                        isOperandOfIsOperator = false,
                        resolveDeprecations = true,
                        supertypeSupplier = SupertypeSupplier.Default,
                        expandTypeAliases = true,
                    ).type
                if (expandTypeAliases) expandedType
                else ConeTypeAliasType(
                    classId = classId,
                    expandedType = expandedType,
                    typeArguments = resolvedArguments,
                )
            }
        }
    }

    /**
     * `?T` 在 resolve 阶段映射为 `Option<T>`。
     *
     * 这里保留 raw-cfir 中的 `CfirOptionTypeRef` 语法糖节点，
     * 仅在类型求解结果上构造标准库 `Option` 名义类型。
     */
    private fun resolveOptionType(
        typeRef: CfirOptionTypeRef,
        configuration: TypeResolutionConfiguration,
        expandTypeAliases: Boolean,
    ): ConeCangJieType {
        val componentType = resolveType(
            typeRef.componentTypeRef,
            configuration,
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
            expandTypeAliases = expandTypeAliases,
        ).type

        return ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(StdlibClassIds.Option),
            typeArguments = listOf(ConeTypeProjection(componentType)),
        )
    }

    private fun resolveSimpleClassId(
        shortName: Name,
        configuration: TypeResolutionConfiguration,
    ): ClassId? {
        for (scope in configuration.scopes) {
            var resolvedFromScope: ClassId? = null
            scope.processClassifiersByName(shortName) { classifier ->
                when (classifier) {
                    is org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*> ->
                        resolvedFromScope = resolvedFromScope ?: classifier.classId
                }
            }
            if (resolvedFromScope != null) {
                // scope 已经给出了真实符号，这里不能再退回 provider 重新按 ClassId 查询，
                // 否则当前文件尚未入索引时，同文件声明仍会被默认导入覆盖。
                return resolvedFromScope
            }
        }

        val file = configuration.useSiteFile
        val packageCandidates = LinkedHashSet<ClassId>()
        val explicitImportCandidates = LinkedHashSet<ClassId>()
        if (file != null) {
            findSameFileTopLevelClassifier(file, shortName)?.let { declaration ->
                return declaration.symbol.classId
            }

            // 与 file importing scopes 的顺序保持一致：
            // 当前文件顶层声明优先于同包其他声明；同包声明优先于显式导入，显式导入优先于默认导入。
            // 这样可以保证无包源码里的本地 `Box` / `Hashable` 不会被 `std.core.*` 中的同名声明抢先解析。
            packageCandidates += ClassId(file.packageDirective.packageFqName, shortName)

            val simpleName = shortName.asString()
            for (importInfo in file.imports) {
                val importedFqName = importInfo.importedFqName ?: continue
                if (importInfo.isAllUnder) {
                    explicitImportCandidates += ClassId(importedFqName, shortName)
                    continue
                }
                val importedName = importInfo.aliasName?.asString() ?: importedFqName.shortName().asString()
                if (importedName == simpleName) {
                    explicitImportCandidates += ClassId.topLevel(importedFqName)
                }
            }
        }

        val defaultImportCandidates = LinkedHashSet<ClassId>()
        val defaultImportsProvider = session.defaultImportsProvider
        val defaultImports = defaultImportsProvider.getDefaultImports(includeLowPriorityImports = true)
            .filter { it.fqName !in defaultImportsProvider.excludedImports }
        addDefaultImportCandidates(defaultImportCandidates, defaultImports, shortName)

        return sequenceOf(
            packageCandidates,
            explicitImportCandidates,
            defaultImportCandidates,
        ).flatMap { it.asSequence() }
            .firstOrNull { resolveClass(it) != null }
    }

    private fun findSameFileTopLevelClassifier(
        file: CfirFile,
        shortName: Name,
    ): CfirClassLikeDeclaration? {
        return file.declarations
            .asSequence()
            .filterIsInstance<CfirClassLikeDeclaration>()
            .filter { declaration -> declaration.name == shortName }
            .firstOrNull()
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
