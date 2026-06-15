/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.ConeUnmatchedTypeArgumentsError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.scopes.CfirTypeParameterScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.*
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

private const val THIS_TYPE_NOT_ALLOWED_REASON = "This type is only allowed as an instance member function return type"

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
    private val cFuncName = Name.identifier("CFunc")
    private val cPointerName = StandardNames.CPOINTER
    private val cStringName = StandardNames.CSTRING
    private val thisTypeName = Name.identifier("This")
    private val aliasedTypeExpansionGloballyDisabled: Boolean =
        !session.languageVersionSettings.getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)

    override fun resolveType(
        typeRef: CfirTypeRef,
        configuration: TypeResolutionConfiguration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: SupertypeSupplier,
        expandTypeAliases: Boolean,
    ): CfirTypeResolutionResult {
        return when (typeRef) {
            is CfirResolvedTypeRef -> result(typeRef.coneType)
            is CfirImplicitTypeRef -> result(ConeErrorType(ConeSimpleDiagnostic("Implicit type reference is not resolvable at this stage")))
            is CfirBasicTypeRef -> result(resolveBasicType(typeRef, configuration))
            is CfirUserTypeRef -> resolveUserType(typeRef, configuration, expandTypeAliases)
            is CfirOptionTypeRef -> result(resolveOptionType(typeRef, configuration, expandTypeAliases))
            is CfirFunctionTypeRef -> {
                val parameterTypes = typeRef.parameterTypeRefs.map { resolveType(it, configuration, areBareTypesAllowed, isOperandOfIsOperator, resolveDeprecations, supertypeSupplier, expandTypeAliases).type }
                val returnType = resolveType(typeRef.returnTypeRef, configuration, areBareTypesAllowed, isOperandOfIsOperator, resolveDeprecations, supertypeSupplier, expandTypeAliases).type
                result(ConeFunctionType(parameterTypes = parameterTypes, returnType = returnType))
            }
            is CfirTupleTypeRef -> {
                val elementTypes = typeRef.elementTypeRefs.map { resolveType(it, configuration, areBareTypesAllowed, isOperandOfIsOperator, resolveDeprecations, supertypeSupplier, expandTypeAliases).type }
                result(ConeTupleType(elementTypes = elementTypes))
            }
            is CfirVArrayTypeRef -> {
                val elementType = resolveType(typeRef.elementTypeRef, configuration, areBareTypesAllowed, isOperandOfIsOperator, resolveDeprecations, supertypeSupplier, expandTypeAliases).type
                val size = CfirIntConstantEvalUtils.parseVArraySizeLiteral(typeRef.sizeLiteral)
                if (size != null) {
                    result(ConeVArrayType(elementType = elementType, size = size))
                } else {
                    result(ConeErrorType(ConeSimpleDiagnostic("Invalid VArray size: ${typeRef.sizeLiteral}")))
                }
            }
            is CfirErrorTypeRef -> result(ConeErrorType(typeRef.diagnostic))
            else -> result(ConeErrorType(ConeSimpleDiagnostic("Unsupported type reference: ${typeRef::class.simpleName}")))
        }
    }

    private fun result(type: ConeCangJieType): CfirTypeResolutionResult = CfirTypeResolutionResult(
        type = type,
        diagnostic = (type as? ConeErrorType)?.diagnostic,
    )

    override fun resolveClass(typeRef: CfirTypeRef): CfirClassLikeDeclaration? {
        val userTypeRef = typeRef as? CfirUserTypeRef ?: return null
        if (userTypeRef.qualifier.isEmpty()) return null

        val className = userTypeRef.qualifier.last().name
        val packageName = userTypeRef.qualifier.dropLast(1).joinToString(".") { it.name.asString() }
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
        configuration.typeParameterTypeOrNull(typeRef.name)?.let { return it }

        val primitiveType = session.builtinTypes.getPrimitiveTypeByName(typeRef.name.asString())
        return primitiveType ?: ConeErrorType(ConeSimpleDiagnostic("Unknown basic type: ${typeRef.name.asString()}"))
    }

    private fun resolveUserType(
        typeRef: CfirUserTypeRef,
        configuration: TypeResolutionConfiguration,
        expandTypeAliases: Boolean = true,
    ): CfirTypeResolutionResult {
        if (typeRef.qualifier.isEmpty()) {
            return result(ConeErrorType(ConeSimpleDiagnostic("Empty user type")))
        }

        if (typeRef.qualifier.size == 1) {
            val qualifierPart = typeRef.qualifier.single()
            if (qualifierPart.name == thisTypeName) {
                return result(resolveThisType(qualifierPart, configuration))
            }
            if (qualifierPart.name == cFuncName) {
                return result(resolveCFuncUserType(qualifierPart, configuration, expandTypeAliases))
            }
            resolveSpecialBuiltinUserType(qualifierPart, configuration, expandTypeAliases)?.let { return result(it) }
            if (qualifierPart.typeArguments.isEmpty()) {
                configuration.typeParameterTypeOrNull(qualifierPart.name)?.let { return result(it) }
            }
        }

        val resolvedQualifier = resolveQualifiedClassLike(typeRef, configuration)
        val resolvedClass = resolvedQualifier.declaration
            ?: return result(ConeErrorType(
                resolvedQualifier.diagnostic
                    ?: ConeUnresolvedTypeQualifierError(typeRef.qualifier)
            ))

        val classId = checkNotNull(resolvedQualifier.classId) {
            "Resolved class-like declaration `${resolvedClass.name}` is missing ClassId"
        }
        val finalQualifier = typeRef.qualifier.last()
        val resolvedArguments = finalQualifier.typeArguments.map { argument ->
            resolveType(
                argument,
                configuration,
                areBareTypesAllowed = false,
                isOperandOfIsOperator = false,
                resolveDeprecations = true,
                supertypeSupplier = SupertypeSupplier.Default,
                expandTypeAliases = expandTypeAliases,
            ).type
        }
        val expectedTypeArgumentsCount = resolvedClass.typeParameters.size
        if (expectedTypeArgumentsCount != resolvedArguments.size) {
            val resolvedRawType = createResolvedClassLikeType(
                resolvedClass = resolvedClass,
                classId = classId,
                resolvedArguments = emptyList(),
                expandTypeAliases = expandTypeAliases,
                configuration = configuration,
            )
            return result(
                ConeErrorType(
                    diagnostic = ConeUnmatchedTypeArgumentsError(
                        symbol = resolvedClass.symbol,
                        expectedCount = expectedTypeArgumentsCount,
                        actualCount = resolvedArguments.size,
                        providedTypeArguments = finalQualifier.typeArguments,
                    ),
                    delegatedType = resolvedRawType,
                    typeArguments = resolvedArguments,
                )
            )
        }

        val resolvedType = createResolvedClassLikeType(
            resolvedClass = resolvedClass,
            classId = classId,
            resolvedArguments = resolvedArguments,
            expandTypeAliases = expandTypeAliases,
            configuration = configuration,
        )
        resolvedArguments.firstOrNull { it is ConeErrorType }?.let { errorArgument ->
            return result(
                ConeErrorType(
                    diagnostic = ConeUnreportedDuplicateDiagnostic((errorArgument as ConeErrorType).diagnostic),
                    delegatedType = resolvedType,
                    typeArguments = resolvedArguments,
                )
            )
        }
        return result(resolvedType)
    }

    /**
     * 类型参数解析同时支持两类来源：
     * - declaration/type-resolve 阶段传入的 additional type parameters；
     * - body/tower 阶段安装到 scope 链上的 `CfirTypeParameterScope`。
     *
     * 这样显式类型、局部声明类型和函数体内类型实参都走同一个解析入口，
     * 避免函数或 extend 类型参数在 body resolve 中退化为普通 unresolved classifier。
     */
    private fun TypeResolutionConfiguration.typeParameterTypeOrNull(name: Name): ConeTypeParameterTypeImpl? {
        scopeTypeParameters[name.asString()]?.let { parameter ->
            return ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }

        for (scope in scopes) {
            val typeParameterScope = scope as? CfirTypeParameterScope ?: continue
            var result: ConeTypeParameterTypeImpl? = null
            typeParameterScope.processTypeParametersByName(name) { symbol ->
                if (result == null) {
                    result = ConeTypeParameterTypeImpl(symbol.toLookupTag())
                }
            }
            if (result != null) return result
        }

        return null
    }

    private fun resolveThisType(
        qualifierPart: CfirQualifierPart,
        configuration: TypeResolutionConfiguration,
    ): ConeCangJieType {
        val thisTypeContext = configuration.thisTypeContext
            ?: return thisTypeNotAllowedError()
        if (qualifierPart.typeArguments.isNotEmpty()) {
            return thisTypeNotAllowedError(
                reason = "This type does not accept type arguments",
                delegatedType = thisTypeContext.type,
            )
        }
        return if (thisTypeContext.isAllowed) {
            thisTypeContext.type
        } else {
            thisTypeNotAllowedError(
                delegatedType = thisTypeContext.type,
                kind = thisTypeContext.disallowedDiagnosticKind,
            )
        }
    }

    private fun thisTypeNotAllowedError(
        reason: String = THIS_TYPE_NOT_ALLOWED_REASON,
        delegatedType: ConeCangJieType? = null,
        kind: DiagnosticKind = DiagnosticKind.ThisTypeNotAllowed,
    ): ConeErrorType {
        return ConeErrorType(ConeSimpleDiagnostic(reason, kind), delegatedType = delegatedType)
    }

    private fun resolveCFuncUserType(
        qualifierPart: CfirQualifierPart,
        configuration: TypeResolutionConfiguration,
        expandTypeAliases: Boolean,
    ): ConeCangJieType {
        if (qualifierPart.typeArguments.size != 1) {
            return ConeErrorType(ConeSimpleDiagnostic("CFunc expects exactly one function type argument"))
        }

        val functionType = resolveType(
            qualifierPart.typeArguments.single(),
            configuration,
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
            expandTypeAliases = expandTypeAliases,
        ).type as? ConeFunctionType ?: return ConeErrorType(
            ConeSimpleDiagnostic("CFunc expects a function type argument")
        )

        return ConeFunctionType(
            parameterTypes = functionType.parameterTypes,
            returnType = functionType.returnType,
            isCFunc = true,
            isClosureType = functionType.isClosureType,
            hasVariableLenArg = functionType.hasVariableLenArg,
            attributes = functionType.attributes,
        )
    }

    /**
     * CPointer/CString 是官方前端的 non-primitive builtin types：
     * 只有名字和类型实参数量完全匹配时才进入内建类型路径，保持同名用户类型与错误实参数量的普通解析行为。
     */
    private fun resolveSpecialBuiltinUserType(
        qualifierPart: CfirQualifierPart,
        configuration: TypeResolutionConfiguration,
        expandTypeAliases: Boolean,
    ): ConeCangJieType? {
        return when {
            qualifierPart.name == cPointerName && qualifierPart.typeArguments.size == 1 -> {
                val pointeeType = resolveType(
                    qualifierPart.typeArguments.single(),
                    configuration,
                    areBareTypesAllowed = false,
                    isOperandOfIsOperator = false,
                    resolveDeprecations = true,
                    supertypeSupplier = SupertypeSupplier.Default,
                    expandTypeAliases = expandTypeAliases,
                ).type
                ConePointerType(pointeeType)
            }
            qualifierPart.name == cStringName && qualifierPart.typeArguments.isEmpty() -> ConeCStringType()
            else -> null
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
            typeArguments = listOf(componentType),
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

    private fun resolveQualifiedClassLike(
        typeRef: CfirUserTypeRef,
        configuration: TypeResolutionConfiguration,
    ): QualifiedClassLikeResolution {
        val qualifier = typeRef.qualifier
        val lastQualifier = qualifier.last()
        if (qualifier.size == 1) {
            val classId = resolveSimpleClassId(lastQualifier.name, configuration) ?: ClassId(FqName.ROOT, lastQualifier.name)
            val declaration = resolveClass(classId)
            return if (declaration != null) {
                QualifiedClassLikeResolution(classId, declaration, null)
            } else {
                QualifiedClassLikeResolution(classId, null, ConeUnresolvedTypeQualifierError(typeRef.qualifier))
            }
        }

        val qualifierNames = qualifier.map(CfirQualifierPart::name)
        val fullPackageFqName = qualifierNames.dropLast(1).toFqName()
        val fullClassId = ClassId(fullPackageFqName, lastQualifier.name)
        resolveClass(fullClassId)?.let { declaration ->
            return QualifiedClassLikeResolution(fullClassId, declaration, null)
        }
        if (packageExists(fullPackageFqName)) {
            return QualifiedClassLikeResolution(fullClassId, null, ConeUnresolvedTypeQualifierError(typeRef.qualifier))
        }

        return QualifiedClassLikeResolution(
            classId = null,
            declaration = null,
            diagnostic = ConeUnresolvedTypeQualifierError(typeRef.qualifier),
        )
    }

    private fun createResolvedClassLikeType(
        resolvedClass: CfirClassLikeDeclaration,
        classId: ClassId,
        resolvedArguments: List<ConeCangJieType>,
        expandTypeAliases: Boolean,
        configuration: TypeResolutionConfiguration,
    ): ConeCangJieType {
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
                if (expandTypeAliases && !aliasedTypeExpansionGloballyDisabled) {
                    ConeTypeAliasType(
                        classId = classId,
                        expandedType = expandedType,
                        typeArguments = resolvedArguments,
                    ).fullyExpandedType(session)
                } else {
                    ConeTypeAliasType(
                        classId = classId,
                        expandedType = expandedType,
                        typeArguments = resolvedArguments,
                    )
                }
            }
        }
    }

    private fun List<Name>.toFqName(): FqName =
        if (isEmpty()) FqName.ROOT else FqName(joinToString(".") { it.asString() })

    private fun packageExists(packageFqName: FqName): Boolean {
        if (packageFqName.isRoot) return true
        val packageNames = session.symbolProvider.symbolNamesProvider.getPackageNames() ?: return true
        return packageFqName.asString() in packageNames
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

    private data class QualifiedClassLikeResolution(
        val classId: ClassId?,
        val declaration: CfirClassLikeDeclaration?,
        val diagnostic: ConeDiagnostic?,
    )
}
