package org.cangnova.cangjie.chir.core.serializer

import PackageFormatV113.CHIRExprKind
import PackageFormatV113.CHIRTypeKind
import PackageFormatV113.ClassDefKind
import PackageFormatV113.IntrinsicKind
import PackageFormatV113.OverflowStrategy
import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirNominalTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDefinitionKind
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirConstantExpression
import org.cangnova.cangjie.chir.core.expression.ChirDebugMetadata
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirIntrinsicKind
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirVirtualMethodContext
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryOperation
import org.cangnova.cangjie.chir.core.expression.ChirOverflowStrategy
import org.cangnova.cangjie.chir.core.expression.ChirOtherOperation
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryOperator
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryOperator
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageAccessLevel
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113IndexedTable
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113PackageAccessLevel
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113Phase
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticPackage
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticIdMap
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticTable
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticValue
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113TableDomain
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.type.ChirTypeRelations
import org.cangnova.cangjie.chir.core.type.ChirThisType
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.type.ChirEnumCaseType
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirConstantKind
import org.cangnova.cangjie.chir.core.value.ChirGlobalValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.chir.core.value.ChirParameterValue
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue

/**
 * 将官方 v1.1.3 RAW CHIR 中由 CJO/family 身份选中的函数，导入仓内 CHIR 包。
 *
 * 官方整个 semantic package 在此 API 被调用前已经完成版本化结构与引用域校验。此转换只发布
 * 当前仓内 CHIR 能精确表达的函数形态；遇到尚未映射的表或操作会拒绝整个包，不丢弃语义后继续。
 */
interface OfficialChirV113ChirPackageImporter {
    /**
     * 按官方 CHIR declaration identity 导入函数及包初始化入口。
     *
     * [functionIdentities] 必须由 common CJO 身份与 CFIR family matcher 提供，不得使用显示名推导。
     * [packageSemanticId] 由 specific CHIR package 提供，使两包可以在 linker 层保持相同 package identity。
     */
    fun importFunctions(
        semanticPackage: OfficialChirV113SemanticPackage,
        packageSemanticId: ChirSemanticId,
        functionIdentities: Set<ChirSemanticId>,
    ): ChirPackage

    /** 将 RAW 官方包及名义类型源身份索引导入；索引保留定义表的精确身份。 */
    fun importPackageWithIdentities(
        semanticPackage: OfficialChirV113SemanticPackage,
        packageSemanticId: ChirSemanticId,
    ): OfficialChirV113ImportedChirPackage

    /** 将 RAW 官方包中的全部函数、变量与类型定义导入一个经 CHIR validator 校验的完整包。 */
    fun importPackage(
        semanticPackage: OfficialChirV113SemanticPackage,
        packageSemanticId: ChirSemanticId,
    ): ChirPackage = importPackageWithIdentities(semanticPackage, packageSemanticId).chirPackage
}

/** 官方 nominal definition 的源码身份与声明种类。 */
data class OfficialChirV113NominalTypeIdentity(
    val typeDeclarationId: ChirTypeDeclarationId,
    val definitionKind: ChirTypeDefinitionKind,
) {
    init {
        require(definitionKind in setOf(ChirTypeDefinitionKind.CLASS, ChirTypeDefinitionKind.STRUCT, ChirTypeDefinitionKind.ENUM)) {
            "official nominal type identity requires a class, struct, or enum definition"
        }
    }
}

/** 官方 Func VALUE 的源码身份；CJO mangledBeforeSema 与 CHIR rawMangledName 共用此字段。 */
data class OfficialChirV113FunctionSourceIdentity(
    val rawMangledName: String,
) {
    init {
        require(rawMangledName.isNotBlank()) { "official function raw mangled name must not be blank" }
    }
}

/** 官方包导入结果及由同一 VALUE/DEFINITION 表记录建立的精确源码身份索引。 */
data class OfficialChirV113ImportedChirPackage(
    val chirPackage: ChirPackage,
    val nominalTypeIdsBySourceIdentity: Map<OfficialChirV113NominalTypeIdentity, ChirSemanticId>,
    /** 多值索引让 linker 对 RAW CHIR 中的重复源身份显式报错，而不任取一项。 */
    val functionIdsByRawMangledName: Map<OfficialChirV113FunctionSourceIdentity, List<ChirSemanticId>>,
)

/** v1.1.3 函数导入实现；所有局部 ID 保留官方 VALUE/EXPRESSION 域前缀。 */
class DefaultOfficialChirV113ChirPackageImporter : OfficialChirV113ChirPackageImporter {
    override fun importFunctions(
        semanticPackage: OfficialChirV113SemanticPackage,
        packageSemanticId: ChirSemanticId,
        functionIdentities: Set<ChirSemanticId>,
    ): ChirPackage = importPackageGraph(
        semanticPackage = semanticPackage,
        packageSemanticId = packageSemanticId,
        functionIdentities = functionIdentities,
        includeAllDeclarations = false,
    ).chirPackage

    override fun importPackageWithIdentities(
        semanticPackage: OfficialChirV113SemanticPackage,
        packageSemanticId: ChirSemanticId,
    ): OfficialChirV113ImportedChirPackage {
        val functionIdentities = semanticPackage.values.asSequence()
            .filter { it.table.schemaType == "Func" }
            .map { ChirSemanticId(functionIdentity(it)) }
            .toSet()
        return importPackageGraph(
            semanticPackage = semanticPackage,
            packageSemanticId = packageSemanticId,
            functionIdentities = functionIdentities,
            includeAllDeclarations = true,
        )
    }

    private fun importPackageGraph(
        semanticPackage: OfficialChirV113SemanticPackage,
        packageSemanticId: ChirSemanticId,
        functionIdentities: Set<ChirSemanticId>,
        includeAllDeclarations: Boolean,
    ): OfficialChirV113ImportedChirPackage {
        if (semanticPackage.metadata.phase != OfficialChirV113Phase.RAW) {
            throw CjmpOfficialChirImportException(
                "Only official CHIR v1.1.3 RAW packages can be imported for CJMP",
            )
        }
        val packageName = semanticPackage.metadata.name?.takeIf(String::isNotBlank)
            ?: throw CjmpOfficialChirImportException("Official CHIR v1.1.3 package has no package name")
        val accessLevel = when (semanticPackage.metadata.accessLevel) {
            OfficialChirV113PackageAccessLevel.INTERNAL -> ChirPackageAccessLevel.INTERNAL
            OfficialChirV113PackageAccessLevel.PUBLIC -> ChirPackageAccessLevel.PUBLIC
            OfficialChirV113PackageAccessLevel.PROTECTED,
            OfficialChirV113PackageAccessLevel.INVALID,
            -> throw CjmpOfficialChirImportException(
                "Official CHIR package access level ${semanticPackage.metadata.accessLevel} has no repository CHIR equivalent",
            )
        }

        val valuesById = semanticPackage.values.associateBy(OfficialChirV113IndexedTable::id)
        val semanticIds = OfficialChirV113SemanticIdMap(packageSemanticId, semanticPackage)
        val functionsByIdentity = semanticPackage.values
            .asSequence()
            .filter { it.table.schemaType == "Func" }
            .map { indexed -> functionIdentity(indexed) to indexed }
            .groupBy({ it.first }, { it.second })

        val selectedFunctions = linkedMapOf<UInt, Pair<ChirSemanticId, OfficialChirV113IndexedTable>>()
        functionIdentities.forEach { identity ->
            val matches = functionsByIdentity[identity.value].orEmpty()
            if (matches.size != 1) {
                throw CjmpOfficialChirImportException(
                    "Official CHIR identity ${identity.value} resolves to ${matches.size} function definitions",
                )
            }
            val function = matches.single()
            selectedFunctions[function.id] = identity to function
        }

        val packageInitFunctionId = selectInitializer(
            semanticPackage.metadata.packageInitFunctionId,
            "package-init",
            valuesById,
            selectedFunctions,
        )
        val packageLiteralInitFunctionId = selectInitializer(
            semanticPackage.metadata.packageLiteralInitFunctionId,
            "package-literal-init",
            valuesById,
            selectedFunctions,
        )
        val expressionsById = semanticPackage.expressions.associateBy(OfficialChirV113IndexedTable::id)
        includeDirectFunctionDependencies(
            selectedFunctions = selectedFunctions,
            valuesById = valuesById,
            expressionsById = expressionsById,
        )
        val functionIdentitiesByOfficialId = selectedFunctions.mapValues { (_, selected) -> selected.first }
        val typesById = semanticPackage.types.associateBy(OfficialChirV113IndexedTable::id)
        val definitionsById = semanticPackage.definitions.associateBy(OfficialChirV113IndexedTable::id)
        val importedFunctions = linkedMapOf<ChirSemanticId, DefaultChirFunctionDeclaration>()

        val functionImportersById = selectedFunctions.mapValues { (_, selected) ->
            val (identity, function) = selected
            FunctionImporter(
                semanticIds = semanticIds,
                valuesById = valuesById,
                expressionsById = expressionsById,
                typesById = typesById,
                definitionsById = definitionsById,
                functionIdentitiesByOfficialId = functionIdentitiesByOfficialId,
                importedFunctions = importedFunctions,
                semanticId = identity,
                function = function,
            )
        }
        val functionsByOfficialId = functionImportersById.mapValues { (_, importer) -> importer.import() }
        val functions = functionsByOfficialId.values.toList()
        val packageDeclarations = if (includeAllDeclarations) {
            importPackageDeclarations(
                semanticPackage = semanticPackage,
                semanticIds = semanticIds,
                functionsByOfficialId = functionsByOfficialId,
                functionImporters = functionImportersById,
            )
        } else {
            PackageDeclarations()
        }
        val moduleFunctions = if (includeAllDeclarations) {
            functionsByOfficialId.filterKeys { officialFunctionId ->
                valuesById.getValue(officialFunctionId).table.optionalReference(
                    "declaredParent",
                    OfficialChirV113TableDomain.DEFINITION,
                )?.takeIf { it != 0u } == null
            }.values.toList()
        } else {
            functions
        }
        val module = if (moduleFunctions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ChirModule(
                    semanticId = ChirSemanticId("official:v113:module:${packageSemanticId.value}"),
                    name = "$packageName.common",
                    declarations = moduleFunctions,
                ),
            )
        }
        val result = ChirPackage(
            semanticId = packageSemanticId,
            name = packageName,
            modules = module,
            members = ChirPackageMembers(
                globalVariables = packageDeclarations.globalVariables,
                importedVariables = packageDeclarations.importedVariables,
                importedFunctions = packageDeclarations.importedFunctions + importedFunctions.values,
            ),
            typeDefinitions = packageDeclarations.typeDefinitions,
            importedTypeDefinitions = packageDeclarations.importedTypeDefinitions,
            packageInitFunctionId = packageInitFunctionId,
            packageLiteralInitFunctionId = packageLiteralInitFunctionId,
            accessLevel = accessLevel,
        )
        val report = validator.validatePackage(result)
        if (report.hasErrors) {
            throw CjmpOfficialChirImportException(
                "Official CHIR v1.1.3 package import produced invalid CHIR: ${ChirValidationReportFormatter.render(report)}",
            )
        }
        val nominalTypeIds = linkedMapOf<OfficialChirV113NominalTypeIdentity, ChirSemanticId>()
        (packageDeclarations.typeDefinitions + packageDeclarations.importedTypeDefinitions)
            .filterIsInstance<ChirNominalTypeDeclaration>()
            .forEach { declaration ->
                val sourceIdentity = OfficialChirV113NominalTypeIdentity(
                    typeDeclarationId = declaration.typeDeclarationId,
                    definitionKind = declaration.definitionKind,
                )
                val previous = nominalTypeIds.putIfAbsent(sourceIdentity, declaration.semanticId)
                if (previous != null) {
                    throw CjmpOfficialChirImportException(
                        "Official CHIR nominal source identity ${sourceIdentity.typeDeclarationId.qualifiedName} " +
                            "(${sourceIdentity.definitionKind}) identifies multiple definitions",
                    )
                }
            }
        val functionIdsByRawMangledName = semanticPackage.values
            .asSequence()
            .filter { it.table.schemaType in setOf("Func", "ImportedFunc") }
            .mapNotNull { indexed ->
                val rawMangledName = indexed.table.optionalScalarString("rawMangledName")
                    ?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                OfficialChirV113FunctionSourceIdentity(rawMangledName) to
                    ChirSemanticId(officialValueIdentity(indexed))
            }
            .groupBy({ it.first }, { it.second })
        return OfficialChirV113ImportedChirPackage(
            chirPackage = result,
            nominalTypeIdsBySourceIdentity = java.util.Collections.unmodifiableMap(nominalTypeIds),
            functionIdsByRawMangledName = java.util.Collections.unmodifiableMap(
                functionIdsByRawMangledName.mapValues { (_, identities) -> identities.toList() },
            ),
        )
    }

    private data class PackageDeclarations(
        val globalVariables: List<ChirVariableDeclaration> = emptyList(),
        val importedVariables: List<ChirVariableDeclaration> = emptyList(),
        val importedFunctions: List<DefaultChirFunctionDeclaration> = emptyList(),
        val typeDefinitions: List<ChirTypeDeclaration> = emptyList(),
        val importedTypeDefinitions: List<ChirTypeDeclaration> = emptyList(),
    )

    private fun importPackageDeclarations(
        semanticPackage: OfficialChirV113SemanticPackage,
        semanticIds: OfficialChirV113SemanticIdMap,
        functionsByOfficialId: Map<UInt, DefaultChirFunctionDeclaration>,
        functionImporters: Map<UInt, FunctionImporter>,
    ): PackageDeclarations {
        val defaultTypeImporter = functionImporters.values.firstOrNull()
            ?: throw CjmpOfficialChirImportException(
                "Cannot map declarations without a function-scoped official type resolver",
            )
        fun typeImporterFor(ownerDefinitionId: UInt?): FunctionImporter =
            ownerDefinitionId?.let { owner -> functionImporters.values.firstOrNull { it.ownerDefinitionId() == owner } }
                ?: defaultTypeImporter

        val globalVariablesById = linkedMapOf<UInt, DefaultChirVariableDeclaration>()
        val importedVariablesById = linkedMapOf<UInt, DefaultChirVariableDeclaration>()
        semanticPackage.values.forEach { indexed ->
            if (indexed.table.schemaType !in setOf("GlobalVar", "ImportedVar")) return@forEach
            val base = indexed.table.valueBase()
            val declaredParent = indexed.table.optionalReference(
                "declaredParent",
                OfficialChirV113TableDomain.DEFINITION,
            )?.takeIf { it != 0u }
            val type = typeImporterFor(declaredParent).resolvePackageType(
                base.requiredReference("type", OfficialChirV113TableDomain.TYPE),
            )
            val sourceName = indexed.table.requiredScalarString("srcCodeIdentifier")
            if (sourceName.isBlank()) {
                throw CjmpOfficialChirImportException("Official CHIR ${indexed.table.schemaType} VALUE#${indexed.id} has no source name")
            }
            val declaration = DefaultChirVariableDeclaration(
                semanticId = ChirSemanticId(officialValueIdentity(indexed)),
                name = sourceName,
                type = type,
                mutable = base.hasAttribute(10),
                attributes = base.toChirAttributes(),
            )
            val target = if (indexed.table.schemaType == "GlobalVar") globalVariablesById else importedVariablesById
            if (target.put(indexed.id, declaration) != null) {
                throw CjmpOfficialChirImportException("Official CHIR repeats ${indexed.table.schemaType} VALUE#${indexed.id}")
            }
        }

        val importedFunctionsById = linkedMapOf<UInt, DefaultChirFunctionDeclaration>()
        semanticPackage.values.filter { it.table.schemaType == "ImportedFunc" }.forEach { indexed ->
            val ownerDefinitionId = indexed.table.optionalReference(
                "declaredParent",
                OfficialChirV113TableDomain.DEFINITION,
            )?.takeIf { it != 0u }
            val resolver = typeImporterFor(ownerDefinitionId)
            val base = indexed.table.valueBase()
            val functionType = resolver.resolvePackageType(
                base.requiredReference("type", OfficialChirV113TableDomain.TYPE),
            ).typeOrNull() as? ChirFunctionType
                ?: throw CjmpOfficialChirImportException("ImportedFunc VALUE#${indexed.id} has a non-function type")
            val genericTypeParameters = indexed.table.requiredReferences(
                "genericTypeParams",
                OfficialChirV113TableDomain.TYPE,
            ).map(resolver::resolvePackageType)
            genericTypeParameters.forEachIndexed { index, type ->
                if (type.typeOrNull() !is ChirGenericType) {
                    throw CjmpOfficialChirImportException(
                        "ImportedFunc VALUE#${indexed.id} generic parameter #$index is not a generic type",
                    )
                }
            }
            val parameterInfo = indexed.table.sequence("paramInfo").mapIndexed { index, value ->
                (value as? OfficialChirV113SemanticValue.Table)?.value
                    ?: throw CjmpOfficialChirImportException("ImportedFunc VALUE#${indexed.id} paramInfo[$index] is not a table")
            }
            if (parameterInfo.isNotEmpty() && parameterInfo.size != functionType.parameterTypes.size) {
                throw CjmpOfficialChirImportException(
                    "ImportedFunc VALUE#${indexed.id} has ${parameterInfo.size} parameter records for " +
                        "${functionType.parameterTypes.size} signature parameters",
                )
            }
            val parameters = functionType.parameterTypes.mapIndexed { index, parameterType ->
                val info = parameterInfo.getOrNull(index)
                val resolvedParameterType = info?.optionalReference("paramType", OfficialChirV113TableDomain.TYPE)
                    ?.let(resolver::resolvePackageType) ?: parameterType
                if (!ChirTypeRelations.areEquivalent(resolvedParameterType, parameterType)) {
                    throw CjmpOfficialChirImportException(
                        "ImportedFunc VALUE#${indexed.id} parameter $index disagrees with its function signature",
                    )
                }
                DefaultChirVariableDeclaration(
                    semanticId = semanticIds.syntheticId(
                        OfficialChirV113TableDomain.VALUE,
                        "imported-func:${indexed.id}:parameter:$index",
                    ),
                    name = info?.requiredScalarString("paramName").orEmpty(),
                    type = resolvedParameterType,
                    mutable = false,
                    attributes = info?.toChirAttributes().orEmpty(),
                )
            }
            importedFunctionsById[indexed.id] = DefaultChirFunctionDeclaration(
                semanticId = ChirSemanticId(officialValueIdentity(indexed)),
                name = indexed.table.requiredScalarString("srcCodeIdentifier"),
                returnType = functionType.returnType,
                parameters = parameters,
                blocks = emptyList(),
                entryBlockId = null,
                attributes = base.toChirAttributes(),
                hasBody = false,
                isImported = true,
                genericTypeParameters = genericTypeParameters,
                isStatic = base.hasAttribute(OfficialChirV113Attribute.STATIC),
            )
        }

        val functionsByValueId = functionsByOfficialId + importedFunctionsById
        val variablesByValueId = globalVariablesById + importedVariablesById
        val typeDefinitions = mutableListOf<ChirTypeDeclaration>()
        val importedTypeDefinitions = mutableListOf<ChirTypeDeclaration>()
        semanticPackage.definitions.forEach { definition ->
            if (definition.table.schemaType !in setOf("ClassDef", "StructDef", "EnumDef", "ExtendDef")) return@forEach
            val customDefinition = definition.table.requiredTable("base")
            val resolver = typeImporterFor(definition.id)
            val formalTypeParameters = if (definition.table.schemaType == "ExtendDef") {
                definition.table.requiredReferences("genericParams", OfficialChirV113TableDomain.TYPE)
                    .map(resolver::resolvePackageType)
            } else {
                resolver.resolveFormalTypeParameters(definition.id, customDefinition)
            }
            val typeParameterNames = formalTypeParameters.mapIndexed { index, parameter ->
                val generic = parameter.typeOrNull() as? ChirGenericType
                    ?: throw CjmpOfficialChirImportException(
                        "Official DEFINITION#${definition.id} generic parameter $index is not a generic type",
                    )
                generic.identifier
            }
            val instanceFields = customDefinition.sequence("instanceMemberVars").mapIndexed { index, value ->
                val field = (value as? OfficialChirV113SemanticValue.Table)?.value
                    ?: throw CjmpOfficialChirImportException(
                        "Official DEFINITION#${definition.id} instanceMemberVars[$index] is not a MemberVarInfo",
                    )
                DefaultChirVariableDeclaration(
                    semanticId = semanticIds.syntheticId(
                        OfficialChirV113TableDomain.DEFINITION,
                        "member-var:${definition.id}:$index",
                    ),
                    name = field.requiredScalarString("name"),
                    type = resolver.resolvePackageType(field.requiredReference("type", OfficialChirV113TableDomain.TYPE)),
                    mutable = field.hasAttribute(10),
                    attributes = field.toChirAttributes(),
                )
            }
            val staticFields = customDefinition.requiredReferences(
                "staticMemberVars",
                OfficialChirV113TableDomain.VALUE,
            ).map { valueId ->
                variablesByValueId[valueId]
                    ?: throw CjmpOfficialChirImportException(
                        "Official DEFINITION#${definition.id} references missing static variable VALUE#$valueId",
                    )
            }
            val memberFunctions = customDefinition.requiredReferences(
                "methods",
                OfficialChirV113TableDomain.VALUE,
            ).map { valueId ->
                functionsByValueId[valueId]
                    ?: throw CjmpOfficialChirImportException(
                        "Official DEFINITION#${definition.id} references missing method VALUE#$valueId",
                    )
            }
            val memberDeclarations = instanceFields + staticFields + memberFunctions
            val name = customDefinition.requiredScalarString("srcCodeIdentifier")
            val attributes = customDefinition.toChirAttributes()
            val declaration: ChirTypeDeclaration = when (definition.table.schemaType) {
                "ClassDef" -> {
                    val superclassId = definition.table.optionalReference(
                        "superClass",
                        OfficialChirV113TableDomain.TYPE,
                    )?.takeIf { it != 0u }
                    val implementedTypes = customDefinition.requiredReferences(
                        "implementedInterfaces",
                        OfficialChirV113TableDomain.TYPE,
                    ).map(resolver::resolvePackageType)
                    DefaultChirClassDeclaration(
                        semanticId = semanticIds.idOf(OfficialChirV113TableDomain.DEFINITION, definition.id),
                        name = name,
                        typeDeclarationId = customTypeDeclarationId(customDefinition),
                        typeParameters = typeParameterNames,
                        superTypes = superclassId?.let { listOf(resolver.resolvePackageType(it)) }.orEmpty(),
                        implementedTypes = implementedTypes,
                        memberDeclarations = memberDeclarations,
                        attributes = attributes,
                        typeParameterTypes = formalTypeParameters,
                        isInterface = definition.table.requiredScalarInt("kind").toShort() == ClassDefKind.INTERFACE,
                    )
                }
                "StructDef" -> DefaultChirStructDeclaration(
                    semanticId = semanticIds.idOf(OfficialChirV113TableDomain.DEFINITION, definition.id),
                    name = name,
                    typeDeclarationId = customTypeDeclarationId(customDefinition),
                    typeParameters = typeParameterNames,
                    fieldDeclarations = instanceFields,
                    memberDeclarations = memberDeclarations,
                    attributes = attributes,
                    typeParameterTypes = formalTypeParameters,
                )
                "EnumDef" -> {
                    val caseTypes = resolver.resolveEnumCaseTypes(definition, formalTypeParameters)
                    DefaultChirEnumDeclaration(
                        semanticId = semanticIds.idOf(OfficialChirV113TableDomain.DEFINITION, definition.id),
                        name = name,
                        typeDeclarationId = customTypeDeclarationId(customDefinition),
                        typeParameters = typeParameterNames,
                        cases = caseTypes.map(ChirEnumCaseType::name),
                        memberDeclarations = memberDeclarations,
                        attributes = attributes,
                        typeParameterTypes = formalTypeParameters,
                        caseTypes = caseTypes,
                    )
                }
                "ExtendDef" -> {
                    val extendedType = definition.table.requiredReference("extendedType", OfficialChirV113TableDomain.TYPE)
                    val extendedTypes = customDefinition.requiredReferences(
                        "extends",
                        OfficialChirV113TableDomain.DEFINITION,
                    ).map { extendedDefinitionId ->
                        val extendedDefinition = semanticPackage.definitions.singleOrNull { it.id == extendedDefinitionId }
                            ?: throw CjmpOfficialChirImportException(
                                "Official DEFINITION#${definition.id} references missing extended DEFINITION#$extendedDefinitionId",
                            )
                        resolver.resolvePackageType(
                            extendedDefinition.table.requiredTable("base")
                                .requiredReference("type", OfficialChirV113TableDomain.TYPE),
                        )
                    }
                    DefaultChirExtendDeclaration(
                        semanticId = semanticIds.idOf(OfficialChirV113TableDomain.DEFINITION, definition.id),
                        name = name,
                        targetType = resolver.resolvePackageType(extendedType),
                        extendedTypes = extendedTypes,
                        typeParameters = typeParameterNames,
                        memberDeclarations = memberDeclarations,
                        attributes = attributes,
                        typeParameterTypes = formalTypeParameters,
                    )
                }
                else -> error("custom definition filter changed unexpectedly")
            }
            if (customDefinition.hasAttribute(16)) importedTypeDefinitions += declaration else typeDefinitions += declaration
        }

        val globalVariables = globalVariablesById.filterKeys { valueId ->
            semanticPackage.values.single { it.id == valueId }.table.optionalReference(
                "declaredParent",
                OfficialChirV113TableDomain.DEFINITION,
            )?.takeIf { it != 0u } == null
        }.values.toList()
        val importedVariables = importedVariablesById.filterKeys { valueId ->
            semanticPackage.values.single { it.id == valueId }.table.optionalReference(
                "declaredParent",
                OfficialChirV113TableDomain.DEFINITION,
            )?.takeIf { it != 0u } == null
        }.values.toList()
        val importedFunctions = importedFunctionsById.filterKeys { valueId ->
            semanticPackage.values.single { it.id == valueId }.table.optionalReference(
                "declaredParent",
                OfficialChirV113TableDomain.DEFINITION,
            )?.takeIf { it != 0u } == null
        }.values.toList()
        return PackageDeclarations(
            globalVariables = globalVariables,
            importedVariables = importedVariables,
            importedFunctions = importedFunctions,
            typeDefinitions = typeDefinitions,
            importedTypeDefinitions = importedTypeDefinitions,
        )
    }

    private fun customTypeDeclarationId(customDefinition: OfficialChirV113SemanticTable): ChirTypeDeclarationId {
        val packageName = customDefinition.requiredScalarString("packageName")
        val name = customDefinition.requiredScalarString("srcCodeIdentifier")
        if (packageName.isBlank() || name.isBlank()) {
            throw CjmpOfficialChirImportException("Official CHIR custom type definition has no qualified source identity")
        }
        return ChirTypeDeclarationId(packageName, name)
    }

    private fun selectInitializer(
        officialValueId: UInt,
        kind: String,
        valuesById: Map<UInt, OfficialChirV113IndexedTable>,
        selectedFunctions: MutableMap<UInt, Pair<ChirSemanticId, OfficialChirV113IndexedTable>>,
    ): ChirSemanticId? {
        if (officialValueId == 0u) return null
        val function = valuesById[officialValueId]
            ?.takeIf { it.table.schemaType == "Func" }
            ?: throw CjmpOfficialChirImportException(
                "Official CHIR $kind identity $officialValueId does not reference a function",
            )
        val identity = functionIdentity(function)
        selectedFunctions[officialValueId] = ChirSemanticId(identity) to function
        return ChirSemanticId(identity)
    }

    private fun functionIdentity(function: OfficialChirV113IndexedTable): String {
        val value = officialValueIdentity(function)
        if (value.isBlank()) {
            throw CjmpOfficialChirImportException(
                "Official CHIR function ${function.id} has no stable declaration identity",
            )
        }
        return value
    }

    private fun includeDirectFunctionDependencies(
        selectedFunctions: MutableMap<UInt, Pair<ChirSemanticId, OfficialChirV113IndexedTable>>,
        valuesById: Map<UInt, OfficialChirV113IndexedTable>,
        expressionsById: Map<UInt, OfficialChirV113IndexedTable>,
    ) {
        val pending = ArrayDeque(selectedFunctions.keys)
        val visited = mutableSetOf<UInt>()
        while (pending.isNotEmpty()) {
            val functionId = pending.removeFirst()
            if (!visited.add(functionId)) continue
            val function = valuesById[functionId]
                ?.takeIf { it.table.schemaType == "Func" }
                ?: throw CjmpOfficialChirImportException("function closure references missing Func VALUE#$functionId")
            val bodyId = function.table.requiredReference("body", OfficialChirV113TableDomain.VALUE)
            if (bodyId == 0u) {
                throw CjmpOfficialChirImportException(
                    "Func VALUE#$functionId has no body; declaration-only values must use ImportedFunc",
                )
            }
            val body = valuesById[bodyId]
                ?.takeIf { it.table.schemaType == "BlockGroup" }
                ?: throw CjmpOfficialChirImportException("Func VALUE#$functionId body is not a BlockGroup")
            body.table.requiredReferences("blocks", OfficialChirV113TableDomain.VALUE).forEach { blockId ->
                val block = valuesById[blockId]
                    ?.takeIf { it.table.schemaType == "Block" }
                    ?: throw CjmpOfficialChirImportException("Func VALUE#$functionId references non-block VALUE#$blockId")
                block.table.requiredReferences("exprs", OfficialChirV113TableDomain.EXPRESSION).forEach expressionLoop@{ expressionId ->
                    val expression = expressionsById[expressionId]
                        ?: throw CjmpOfficialChirImportException("Func VALUE#$functionId references missing EXPRESSION#$expressionId")
                    if (expression.table.schemaType != "Apply") return@expressionLoop
                    val operands = expression.expressionBase()
                        .requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
                    val calleeId = operands.firstOrNull() ?: throw CjmpOfficialChirImportException(
                        "APPLY EXPRESSION#$expressionId has no callee operand",
                    )
                    val callee = valuesById[calleeId]
                        ?: throw CjmpOfficialChirImportException("APPLY EXPRESSION#$expressionId references missing callee VALUE#$calleeId")
                    if (callee.table.schemaType != "Func") return@expressionLoop
                    val identity = ChirSemanticId(functionIdentity(callee))
                    val existing = selectedFunctions[calleeId]
                    if (existing != null && existing.first != identity) {
                        throw CjmpOfficialChirImportException(
                            "official Func VALUE#$calleeId resolves to conflicting CJMP identities " +
                                "${existing.first.value} and ${identity.value}",
                        )
                    }
                    if (existing == null) {
                        selectedFunctions[calleeId] = identity to callee
                        pending.addLast(calleeId)
                    }
                }
            }
        }
    }

    private class FunctionImporter(
        private val semanticIds: OfficialChirV113SemanticIdMap,
        private val valuesById: Map<UInt, OfficialChirV113IndexedTable>,
        private val expressionsById: Map<UInt, OfficialChirV113IndexedTable>,
        private val typesById: Map<UInt, OfficialChirV113IndexedTable>,
        private val definitionsById: Map<UInt, OfficialChirV113IndexedTable>,
        private val functionIdentitiesByOfficialId: Map<UInt, ChirSemanticId>,
        private val importedFunctions: MutableMap<ChirSemanticId, DefaultChirFunctionDeclaration>,
        private val semanticId: ChirSemanticId,
        private val function: OfficialChirV113IndexedTable,
    ) {
        private val baseValue = function.table.requiredTable("base")
        private val resolvedTypes = mutableMapOf<UInt, ChirTypeRef>()
        private val resolvingTypes = mutableSetOf<UInt>()
        private val parameterDeclarations: List<DefaultChirVariableDeclaration>
        private val genericTypeParameters: List<ChirTypeRef>
        private val functionType: ChirFunctionType
        private val returnSlotId: UInt

        init {
            genericTypeParameters = function.table.requiredReferences(
                "genericTypeParams",
                OfficialChirV113TableDomain.TYPE,
            ).map(::resolveType)
            genericTypeParameters.forEachIndexed { index, type ->
                if (type.typeOrNull() !is ChirGenericType) {
                    fail("function ${semanticId.value} generic parameter #$index is not a generic type")
                }
            }
            functionType = baseValue.requiredReference("type", OfficialChirV113TableDomain.TYPE)
                .let(::resolveType)
                .requireFunctionType("function ${semanticId.value}")
            val parameterIds = function.table.requiredReferences("params", OfficialChirV113TableDomain.VALUE)
            if (parameterIds.size != functionType.parameterTypes.size) {
                fail(
                    "function ${semanticId.value} has ${parameterIds.size} parameter values but its type has " +
                        "${functionType.parameterTypes.size} parameters",
                )
            }
            parameterDeclarations = parameterIds.mapIndexed { index, parameterId ->
                val parameter = valuesById[parameterId]?.takeIf { it.table.schemaType == "Parameter" }
                    ?: fail("function parameter VALUE#$parameterId is not an official Parameter")
                val value = parameter.table.requiredTable("base")
                val type = value.requiredReference("type", OfficialChirV113TableDomain.TYPE).let(::resolveType)
                if (type != functionType.parameterTypes[index]) {
                    fail("function parameter VALUE#$parameterId disagrees with the official function type")
                }
                DefaultChirVariableDeclaration(
                    semanticId = valueId(parameterId),
                    name = value.requiredScalarString("identifier"),
                    type = type,
                    mutable = value.hasAttribute(10),
                )
            }
            returnSlotId = function.table.optionalReference("retVal", OfficialChirV113TableDomain.VALUE) ?: 0u
        }

        fun import(): DefaultChirFunctionDeclaration {
            val bodyId = function.table.requiredReference("body", OfficialChirV113TableDomain.VALUE)
            if (bodyId == 0u) {
                fail("Func VALUE#${function.id} has no body; declaration-only values must use ImportedFunc")
            }
            val body = valuesById[bodyId]?.takeIf { it.table.schemaType == "BlockGroup" }
                ?: fail("function ${semanticId.value} body VALUE#$bodyId is not an official BlockGroup")
            val blockIds = body.table.requiredReferences("blocks", OfficialChirV113TableDomain.VALUE)
            if (blockIds.isEmpty()) fail("Func VALUE#${function.id} has an empty BlockGroup")
            val entryBlockId = body.table.requiredReference("entryBlock", OfficialChirV113TableDomain.VALUE)
            if (entryBlockId !in blockIds) {
                fail("function ${semanticId.value} body has no valid entry block")
            }
            val successorsByBlock = blockIds.associateWith(::blockSuccessors)
            val referencedBlocks = successorsByBlock.values.flatten().toSet()
            if (!blockIds.containsAll(referencedBlocks)) {
                fail("function ${semanticId.value} has a control-flow edge to a missing block")
            }
            val unterminatedBlocks = blockIds.filter { blockId ->
                val block = valuesById[blockId]
                    ?: fail("function ${semanticId.value} body references missing block VALUE#$blockId")
                block.table.requiredReferences("exprs", OfficialChirV113TableDomain.EXPRESSION)
                    .lastOrNull()
                    ?.let(expressionsById::get)
                    ?.table
                    ?.schemaType !in TERMINATOR_EXPRESSION_TABLES
            }.toSet()
            if (entryBlockId in unterminatedBlocks || referencedBlocks.any(unterminatedBlocks::contains)) {
                fail("function ${semanticId.value} contains a reachable block without a terminator")
            }
            val blocks = blockIds.map(::importBlock)
            return DefaultChirFunctionDeclaration(
                semanticId = semanticId,
                name = function.table.requiredScalarString("srcCodeIdentifier"),
                returnType = functionType.returnType,
                parameters = parameterDeclarations,
                blocks = blocks,
                entryBlockId = valueId(entryBlockId),
                attributes = baseValue.toChirAttributes(),
                genericTypeParameters = genericTypeParameters,
                isStatic = baseValue.hasAttribute(OfficialChirV113Attribute.STATIC),
            )
        }

        private fun importBlock(blockId: UInt): ChirBlock {
            val indexedBlock = valuesById[blockId]?.takeIf { it.table.schemaType == "Block" }
                ?: fail("function ${semanticId.value} references non-block VALUE#$blockId")
            val expressions = indexedBlock.table.requiredReferences("exprs", OfficialChirV113TableDomain.EXPRESSION)
            val coreExpressions = mutableListOf<org.cangnova.cangjie.chir.core.expression.ChirExpression>()
            var terminator: org.cangnova.cangjie.chir.core.controlflow.ChirTerminator? = null
            expressions.forEachIndexed { index, expressionId ->
                val expression = expressionsById[expressionId]
                    ?: fail("function ${semanticId.value} references missing EXPRESSION#$expressionId")
                val isLast = index == expressions.lastIndex
                when (expression.table.schemaType) {
                    "Debug" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importDebug(expression)
                    }
                    "UnaryExpression" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importUnary(expression)
                    }
                    "BinaryExpression" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importBinary(expression)
                    }
                    "Tuple" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importTuple(expression)
                    }
                    "Field" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importField(expression)
                    }
                    "FieldByName" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importFieldByName(expression)
                    }
                    "TypeCast" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importTypeCast(expression)
                    }
                    "Allocate" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importAllocation(expression)
                    }
                    "RawArrayAllocate" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importRawArrayAllocate(expression)
                    }
                    "RawArrayLiteralInit" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importRawArrayLiteralInit(expression)
                    }
                    "RawArrayInitByValue" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importRawArrayInitByValue(expression)
                    }
                    "Load" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importLoad(expression)
                    }
                    "Constant" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importConstant(expression)
                    }
                    "Apply" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importApply(expression)
                    }
                    "Invoke" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importInvoke(expression)
                    }
                    "Intrinsic" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importIntrinsic(expression)
                    }
                    "Store" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importStore(expression)
                    }
                    "StoreElementByName" -> {
                        if (terminator != null) fail("function ${semanticId.value} has an expression after its terminator")
                        coreExpressions += importStoreElementByName(expression)
                    }
                    "GoTo" -> {
                        requireLastTerminator(isLast, expressionId)
                        verifyExpressionKind(expression, CHIRExprKind.GOTO, expressionId)
                        val successors = expression.table.findNestedTable("Terminator")
                            ?.requiredReferences("successors", OfficialChirV113TableDomain.VALUE)
                            ?: fail("GOTO EXPRESSION#$expressionId has no Terminator base table")
                        if (successors.size != 1) fail("GOTO EXPRESSION#$expressionId must have one successor")
                        terminator = ChirBranchTerminator(expressionId(expressionId), valueId(successors.single()))
                    }
                    "Branch" -> {
                        requireLastTerminator(isLast, expressionId)
                        verifyExpressionKind(expression, CHIRExprKind.BRANCH, expressionId)
                        val base = expression.expressionBase()
                        val conditionId = base.requiredReferences("operands", OfficialChirV113TableDomain.VALUE).singleOrNull()
                            ?: fail("BRANCH EXPRESSION#$expressionId must have one condition operand")
                        val successors = expression.table.findNestedTable("Terminator")
                            ?.requiredReferences("successors", OfficialChirV113TableDomain.VALUE)
                            ?: fail("BRANCH EXPRESSION#$expressionId has no Terminator base table")
                        if (successors.size != 2) fail("BRANCH EXPRESSION#$expressionId must have two successors")
                        terminator = ChirConditionalBranchTerminator(
                            semanticId = expressionId(expressionId),
                            condition = resolveValue(conditionId),
                            trueTargetBlockId = valueId(successors[0]),
                            falseTargetBlockId = valueId(successors[1]),
                        )
                    }
                    "Exit" -> {
                        requireLastTerminator(isLast, expressionId)
                        verifyExpressionKind(expression, CHIRExprKind.EXIT, expressionId)
                        terminator = importReturn(expressionId, blockId, coreExpressions)
                    }
                    "RaiseException" -> {
                        requireLastTerminator(isLast, expressionId)
                        verifyExpressionKind(expression, CHIRExprKind.RAISE_EXCEPTION, expressionId)
                        val base = expression.expressionBase()
                        val exceptionId = base.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
                            .singleOrNull()
                            ?: fail("RAISE_EXCEPTION EXPRESSION#$expressionId must have one exception operand")
                        val successors = expression.table.findNestedTable("Terminator")
                            ?.requiredReferences("successors", OfficialChirV113TableDomain.VALUE)
                            ?: fail("RAISE_EXCEPTION EXPRESSION#$expressionId has no Terminator base table")
                        if (successors.size > 1) {
                            fail("RAISE_EXCEPTION EXPRESSION#$expressionId has more than one unwind successor")
                        }
                        terminator = ChirThrowTerminator(
                            semanticId = expressionId(expressionId),
                            exceptionValue = resolveValue(exceptionId),
                            unwindTargetBlockId = successors.singleOrNull()?.let(::valueId),
                        )
                    }
                    else -> fail(
                        "function ${semanticId.value} has no lowering for official CHIR " +
                            "${expression.table.schemaType} expression EXPRESSION#$expressionId " +
                            "has no repository CHIR lowering",
                    )
                }
            }
            return ChirBlock(
                semanticId = valueId(blockId),
                name = indexedBlock.table.requiredTable("base").requiredScalarString("identifier"),
                expressions = coreExpressions,
                terminator = terminator,
            )
        }

        private fun blockSuccessors(blockId: UInt): List<UInt> {
            val block = valuesById[blockId]
                ?: fail("function ${semanticId.value} body references missing block VALUE#$blockId")
            val expressionIds = block.table.requiredReferences("exprs", OfficialChirV113TableDomain.EXPRESSION)
            val terminatorId = expressionIds.lastOrNull() ?: return emptyList()
            val terminator = expressionsById[terminatorId]
                ?: fail("function ${semanticId.value} references missing EXPRESSION#$terminatorId")
            if (terminator.table.schemaType !in setOf("GoTo", "Branch", "RaiseException")) return emptyList()
            val successors = terminator.table.findNestedTable("Terminator")
                ?.requiredReferences("successors", OfficialChirV113TableDomain.VALUE)
                ?: fail("${terminator.table.schemaType} EXPRESSION#$terminatorId has no Terminator base table")
            val expectedCount = when (terminator.table.schemaType) {
                "GoTo" -> 1
                "Branch" -> 2
                else -> null
            }
            if ((expectedCount != null && successors.size != expectedCount) ||
                (terminator.table.schemaType == "RaiseException" && successors.size > 1)
            ) {
                fail("${terminator.table.schemaType} EXPRESSION#$terminatorId has ${successors.size} successors")
            }
            return successors
        }

        private fun importAllocation(
            indexedExpression: OfficialChirV113IndexedTable,
        ): ChirMemoryExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.ALLOCATE, expressionId)
            val expression = indexedExpression.expressionBase()
            if (expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE).isNotEmpty()) {
                fail("ALLOCATE EXPRESSION#$expressionId has unexpected operands")
            }
            val targetType = indexedExpression.table.requiredReference("targetType", OfficialChirV113TableDomain.TYPE)
                .let(::resolveType)
            val resultLocalId = expression.requiredReference("resultLocalVar", OfficialChirV113TableDomain.VALUE)
            val resultLocal = valuesById[resultLocalId]?.takeIf { it.table.schemaType == "LocalVar" }
                ?: fail("ALLOCATE EXPRESSION#$expressionId has no result LocalVar")
            val resultType = resultLocal.table.requiredTable("base")
                .requiredReference("type", OfficialChirV113TableDomain.TYPE)
                .let(::resolveType)
            val reference = resultType.typeOrNull() as? ChirRefType
                ?: fail("ALLOCATE EXPRESSION#$expressionId result is not a reference type")
            if (reference.referencedType != targetType) {
                fail("ALLOCATE EXPRESSION#$expressionId target type disagrees with its result reference")
            }
            val resultLocalValue = validateResultLocal(expression, expressionId, resultType)
            return ChirMemoryExpression(
                semanticId = expressionId(expressionId),
                operation = "alloca",
                address = ChirConstantValue(
                    semanticId = ChirSemanticId("${semanticId.value}:alloca-count:${expressionId}"),
                    type = ChirResolvedTypeRef(ChirPrimitiveType.INT32),
                    literal = "1",
                ),
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importRawArrayAllocate(
            indexedExpression: OfficialChirV113IndexedTable,
        ): ChirOtherExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.RAW_ARRAY_ALLOCATE, expressionId)
            val expression = indexedExpression.expressionBase()
            val sizeId = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE).singleOrNull()
                ?: fail("RAWARRAY_ALLOCATE EXPRESSION#$expressionId must have one size operand")
            val size = resolveValue(sizeId)
            val elementType = resolveType(
                indexedExpression.table.requiredReference("elementType", OfficialChirV113TableDomain.TYPE),
            )
            val resultLocalId = expression.requiredReference("resultLocalVar", OfficialChirV113TableDomain.VALUE)
            val resultLocal = valuesById[resultLocalId]?.takeIf { it.table.schemaType == "LocalVar" }
                ?: fail("RAWARRAY_ALLOCATE EXPRESSION#$expressionId has no result LocalVar")
            val resultLocalBase = resultLocal.table.requiredTable("base")
            val resultType = resolveType(resultLocalBase.requiredReference("type", OfficialChirV113TableDomain.TYPE))
            val allocatedType = when (val result = resultType.typeOrNull()) {
                is ChirRawArrayType -> result
                is ChirRefType -> result.referencedType.typeOrNull() as? ChirRawArrayType
                else -> null
            } ?: fail("RAWARRAY_ALLOCATE EXPRESSION#$expressionId result is not a raw array value")
            if (!ChirTypeRelations.areEquivalent(allocatedType.elementType, elementType)) {
                fail("RAWARRAY_ALLOCATE EXPRESSION#$expressionId element type disagrees with its result type")
            }
            val associatedExpressionId = resultLocalBase.optionalReference(
                "associatedExpr",
                OfficialChirV113TableDomain.EXPRESSION,
            )
            if (associatedExpressionId != null && associatedExpressionId != expressionId) {
                fail("RAWARRAY_ALLOCATE EXPRESSION#$expressionId conflicts with VALUE#$resultLocalId associated expression")
            }
            val declaredResultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            if (declaredResultTypeId != null && !ChirTypeRelations.areEquivalent(resolveType(declaredResultTypeId), resultType)) {
                fail("RAWARRAY_ALLOCATE EXPRESSION#$expressionId resultTy disagrees with its result LocalVar")
            }
            if (!size.type.isInt64RawArraySize()) {
                fail("RAWARRAY_ALLOCATE EXPRESSION#$expressionId size is not Int64")
            }
            val resultLocalValue = validateResultLocal(expression, expressionId, resultType)
            return ChirOtherExpression(
                semanticId = expressionId(expressionId),
                operation = ChirOtherOperation.RAW_ARRAY_ALLOCATE.canonicalName,
                operands = listOf(size),
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun ChirTypeRef.isInt64RawArraySize(): Boolean = typeOrNull() == ChirPrimitiveType.INT64

        private fun importRawArrayLiteralInit(
            indexedExpression: OfficialChirV113IndexedTable,
        ): ChirOtherExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.RAW_ARRAY_LITERAL_INIT, expressionId)
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            val rawArrayId = operands.firstOrNull()
                ?: fail("RAW_ARRAY_LITERAL_INIT EXPRESSION#$expressionId has no destination array")
            val rawArray = resolveValue(rawArrayId)
            val rawArrayType = when (val type = rawArray.type.typeOrNull()) {
                is ChirRawArrayType -> type
                is ChirRefType -> type.referencedType.typeOrNull() as? ChirRawArrayType
                else -> null
            } ?: fail("RAW_ARRAY_LITERAL_INIT EXPRESSION#$expressionId destination is not a raw array")
            val elements = operands.drop(1).map(::resolveValue)
            elements.forEachIndexed { index, element ->
                if (!ChirTypeRelations.areEquivalent(rawArrayType.elementType, element.type)) {
                    fail("RAW_ARRAY_LITERAL_INIT EXPRESSION#$expressionId element $index has an incompatible type")
                }
            }
            val resultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            val resultType = resultTypeId?.let(::resolveType)
            if (resultType != null && resultType.typeOrNull() !in setOf(ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID)) {
                fail("RAW_ARRAY_LITERAL_INIT EXPRESSION#$expressionId unexpectedly produces ${resultType.renderName}")
            }
            val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
            val resultLocalValue = validateResultLocal(expression, expressionId, unitType)
            return ChirOtherExpression(
                semanticId = expressionId(expressionId),
                operation = ChirOtherOperation.RAW_ARRAY_LITERAL_INIT.canonicalName,
                operands = listOf(rawArray) + elements,
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importRawArrayInitByValue(
            indexedExpression: OfficialChirV113IndexedTable,
        ): ChirOtherExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.RAW_ARRAY_INIT_BY_VALUE, expressionId)
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
                .map(::resolveValue)
            val rawArray = operands.firstOrNull()
                ?: fail("RAW_ARRAY_INIT_BY_VALUE EXPRESSION#$expressionId has no destination array")
            val rawArrayType = when (val type = rawArray.type.typeOrNull()) {
                is ChirRawArrayType -> type
                is ChirRefType -> type.referencedType.typeOrNull() as? ChirRawArrayType
                else -> null
            } ?: fail("RAW_ARRAY_INIT_BY_VALUE EXPRESSION#$expressionId destination is not a raw array")
            val size = operands.getOrNull(1)
                ?: fail("RAW_ARRAY_INIT_BY_VALUE EXPRESSION#$expressionId has no element count")
            val initialValue = operands.getOrNull(2)
                ?: fail("RAW_ARRAY_INIT_BY_VALUE EXPRESSION#$expressionId has no initialization value")
            if (operands.size != 3) {
                fail(
                    "RAW_ARRAY_INIT_BY_VALUE EXPRESSION#$expressionId must have a destination, element count, and value; " +
                        "found ${operands.size} operands with types ${operands.map { it.type.renderName }}",
                )
            }
            if (!size.type.isInt64RawArraySize()) {
                fail("RAW_ARRAY_INIT_BY_VALUE EXPRESSION#$expressionId element count is not Int64")
            }
            if (!ChirTypeRelations.isAssignable(initialValue.type, rawArrayType.elementType)) {
                fail("RAW_ARRAY_INIT_BY_VALUE EXPRESSION#$expressionId value type is incompatible with its element type")
            }
            val resultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            val resultType = resultTypeId?.let(::resolveType)
            if (resultType != null && resultType.typeOrNull() !in setOf(ChirPrimitiveType.UNIT, ChirPrimitiveType.VOID)) {
                fail("RAW_ARRAY_INIT_BY_VALUE EXPRESSION#$expressionId unexpectedly produces ${resultType.renderName}")
            }
            val resultLocalValue = validateResultLocal(expression, expressionId, ChirResolvedTypeRef(ChirPrimitiveType.UNIT))
            return ChirOtherExpression(
                semanticId = expressionId(expressionId),
                operation = ChirOtherOperation.RAW_ARRAY_INIT_BY_VALUE.canonicalName,
                operands = operands,
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importStore(indexedExpression: OfficialChirV113IndexedTable): ChirMemoryExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.STORE, expressionId)
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operands.size != 2) fail("STORE EXPRESSION#$expressionId must have value and destination operands")
            val storedValue = resolveValue(operands[0])
            val address = resolveValue(operands[1])
            val addressType = address.type.typeOrNull() as? ChirRefType
                ?: fail("STORE EXPRESSION#$expressionId destination is not a reference value")
            if (!ChirTypeRelations.isAssignable(storedValue.type, addressType.referencedType)) {
                fail(
                    "STORE EXPRESSION#$expressionId value type ${storedValue.type.renderName} differs from " +
                        "destination type ${addressType.referencedType.renderName}",
                )
            }
            val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
            validateResultLocal(expression, expressionId, unitType)
            val declaredResultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            if (declaredResultTypeId != null &&
                !ChirTypeRelations.areEquivalent(resolveType(declaredResultTypeId), unitType)
            ) {
                fail("STORE EXPRESSION#$expressionId resultTy is not Unit")
            }
            return ChirMemoryExpression(
                semanticId = expressionId(expressionId),
                operation = "store",
                address = address,
                value = storedValue,
            )
        }

        private fun importStoreElementByName(indexedExpression: OfficialChirV113IndexedTable): ChirMemoryExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.STORE_ELEMENT_BY_NAME, expressionId)
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operands.size != 2) {
                fail("STORE_ELEMENT_BY_NAME EXPRESSION#$expressionId must have value and location operands")
            }
            val storedValue = resolveValue(operands[0])
            val location = resolveValue(operands[1])
            if (location.type.typeOrNull() !is ChirRefType) {
                fail("STORE_ELEMENT_BY_NAME EXPRESSION#$expressionId location is not a reference value")
            }
            val memberNames = indexedExpression.table.requiredStrings("names")
            if (memberNames.isEmpty() || memberNames.any(String::isBlank)) {
                fail("STORE_ELEMENT_BY_NAME EXPRESSION#$expressionId must have a non-empty member path")
            }
            val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
            val resultLocalValue = validateResultLocal(expression, expressionId, unitType)
            val resultType = resultLocalValue.type
            if (resultType.typeOrNull() != ChirPrimitiveType.UNIT) {
                fail("STORE_ELEMENT_BY_NAME EXPRESSION#$expressionId result must be Unit")
            }
            val declaredResultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            if (declaredResultTypeId != null && !ChirTypeRelations.areEquivalent(resolveType(declaredResultTypeId), resultType)) {
                fail("STORE_ELEMENT_BY_NAME EXPRESSION#$expressionId resultTy disagrees with its result LocalVar")
            }
            return ChirMemoryExpression(
                semanticId = expressionId(expressionId),
                operation = ChirMemoryOperation.STORE_ELEMENT_BY_NAME.canonicalName,
                address = location,
                value = storedValue,
                resultType = resultType,
                memberNames = memberNames,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importLoad(indexedExpression: OfficialChirV113IndexedTable): ChirMemoryExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.LOAD, expressionId)
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operands.size != 1) fail("LOAD EXPRESSION#$expressionId must have one address operand")
            val address = resolveValue(operands.single())
            val addressType = address.type.typeOrNull() as? ChirRefType
                ?: fail("LOAD EXPRESSION#$expressionId operand is not a reference value")
            val resultLocalValue = readResultLocal(expression, expressionId)
            val resultType = resultLocalValue.type
            if (addressType.referencedType != resultType) {
                fail("LOAD EXPRESSION#$expressionId result type disagrees with its address")
            }
            val declaredResultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            if (declaredResultTypeId != null &&
                !ChirTypeRelations.areEquivalent(resolveType(declaredResultTypeId), resultType)
            ) {
                fail("LOAD EXPRESSION#$expressionId resultTy disagrees with its result LocalVar")
            }
            return ChirMemoryExpression(
                semanticId = expressionId(expressionId),
                operation = "load",
                address = address,
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importConstant(indexedExpression: OfficialChirV113IndexedTable): ChirConstantExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.CONSTANT, expressionId)
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operands.size != 1) fail("CONSTANT EXPRESSION#$expressionId must have one literal operand")
            val resultLocal = readResultLocal(expression, expressionId)
            val resultType = resultLocal.type
            val literal = resolveValue(operands.single()) as? ChirConstantValue
                ?: fail("CONSTANT EXPRESSION#$expressionId operand is not an importable literal")
            if (!ChirTypeRelations.isOfficialChirTypeCompatible(resultType, literal.type)) {
                fail("CONSTANT EXPRESSION#$expressionId result type differs from its literal")
            }
            val declaredResultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            if (declaredResultTypeId != null &&
                !ChirTypeRelations.areEquivalent(resolveType(declaredResultTypeId), resultType)
            ) {
                fail("CONSTANT EXPRESSION#$expressionId resultTy disagrees with its result LocalVar")
            }
            return ChirConstantExpression(
                semanticId = expressionId(expressionId),
                value = literal,
                resultType = resultType,
                resultLocalValue = resultLocal,
            )
        }

        private fun importDebug(indexedExpression: OfficialChirV113IndexedTable): org.cangnova.cangjie.chir.core.expression.ChirOtherExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.DEBUGEXPR, expressionId)
            val expression = indexedExpression.expressionBase()
            val operandIds = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operandIds.size != 1) fail("DEBUG EXPRESSION#$expressionId must reference one value")
            val resultType = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
                ?.let(::resolveType)
            val resultLocalId = expression.optionalReference("resultLocalVar", OfficialChirV113TableDomain.VALUE)
                ?.takeIf { it != 0u }
            val resultLocal = resultLocalId?.let { readResultLocal(expression, expressionId) }
            val accurateEnvironmentObjectType = indexedExpression.table
                .optionalReference("accurateEnvObjTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
                ?.let(::resolveType)
            val sourceCodeIdentifier = indexedExpression.table.requiredScalarString("srcCodeIdentifier")
            return org.cangnova.cangjie.chir.core.expression.ChirOtherExpression(
                semanticId = expressionId(expressionId),
                operation = sourceCodeIdentifier,
                operands = operandIds.map(::resolveValue),
                resultType = resultType,
                resultLocalValue = resultLocal,
                debugMetadata = ChirDebugMetadata(
                    sourceCodeIdentifier = sourceCodeIdentifier,
                    accurateEnvironmentObjectType = accurateEnvironmentObjectType,
                ),
            )
        }

        private fun importApply(indexedExpression: OfficialChirV113IndexedTable): ChirCallExpression =
            importCallExpression(
                indexedExpression = indexedExpression,
                expectedKind = CHIRExprKind.APPLY,
                isSuperCall = indexedExpression.table.requiredScalarBoolean("isSuperCall"),
                virtualMethodContext = null,
            )

        private fun importInvoke(indexedExpression: OfficialChirV113IndexedTable): ChirCallExpression {
            val expressionId = indexedExpression.id
            val methodContext = indexedExpression.table.requiredTable("virMethodCtx")
            val originalFunctionType = resolveType(
                methodContext.requiredReference("originalFuncType", OfficialChirV113TableDomain.TYPE),
            ).requireFunctionType("virtual method context of INVOKE EXPRESSION#$expressionId")
            val genericTypeParameters = methodContext.requiredReferences(
                "genericTypeParams",
                OfficialChirV113TableDomain.TYPE,
            ).mapIndexed { index, typeId ->
                val parameterType = resolveType(typeId)
                if (parameterType.typeOrNull() !is ChirGenericType) {
                    fail("INVOKE EXPRESSION#$expressionId virtual generic parameter #$index is not a generic type")
                }
                parameterType
            }
            val sourceCodeIdentifier = methodContext.requiredScalarString("srcCodeIdentifier")
            if (sourceCodeIdentifier.isBlank()) {
                fail("INVOKE EXPRESSION#$expressionId virtual method context has no source identifier")
            }
            return importCallExpression(
                indexedExpression = indexedExpression,
                expectedKind = CHIRExprKind.INVOKE,
                isSuperCall = false,
                virtualMethodContext = ChirVirtualMethodContext(
                    sourceCodeIdentifier = sourceCodeIdentifier,
                    originalFunctionType = originalFunctionType,
                    genericTypeParameters = genericTypeParameters,
                ),
            )
        }

        private fun importCallExpression(
            indexedExpression: OfficialChirV113IndexedTable,
            expectedKind: UByte,
            isSuperCall: Boolean,
            virtualMethodContext: ChirVirtualMethodContext?,
        ): ChirCallExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, expectedKind, expressionId)
            val instantiatedTypeArguments = indexedExpression.table
                .requiredReferences("instantiatedTypeArgs", OfficialChirV113TableDomain.TYPE)
                .map(::resolveType)
            val thisType = indexedExpression.table.optionalReference("thisType", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
                ?.let(::resolveType)

            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            val calleeValueId = operands.firstOrNull()
                ?: fail("APPLY EXPRESSION#$expressionId has no callee operand")
            val encodedCallTarget = resolveValue(calleeValueId)
            val functionType = virtualMethodContext?.originalFunctionType
                ?: encodedCallTarget.type.requireFunctionType(
                    "callee of ${indexedExpression.table.schemaType.uppercase()} EXPRESSION#$expressionId",
                )
            val dispatchReceiver = encodedCallTarget.takeIf { virtualMethodContext != null }
            val callee = if (virtualMethodContext == null) {
                encodedCallTarget
            } else {
                ChirImportedFunctionValue(
                    semanticId = ChirSemanticId(
                        "${semanticIds.idOf(OfficialChirV113TableDomain.EXPRESSION, expressionId).value}:virtual-target",
                    ),
                    type = ChirResolvedTypeRef(functionType),
                    name = virtualMethodContext.sourceCodeIdentifier,
                )
            }
            val arguments = operands.drop(1).map(::resolveValue)
            val argumentParameterTypes = if (virtualMethodContext == null) {
                buildList {
                    functionType.receiverType?.let(::add)
                    addAll(functionType.parameterTypes)
                }
            } else {
                functionType.parameterTypes.drop(1)
            }
            if (virtualMethodContext != null && functionType.parameterTypes.isEmpty()) {
                fail("INVOKE EXPRESSION#$expressionId original function type has no receiver parameter")
            }
            val fixedParameterCount = argumentParameterTypes.size
            if (arguments.size < fixedParameterCount || (!functionType.hasVarArg && arguments.size != fixedParameterCount)) {
                val arityDescription = if (functionType.hasVarArg) {
                    "at least $fixedParameterCount"
                } else {
                    fixedParameterCount.toString()
                }
                fail("APPLY EXPRESSION#$expressionId has ${arguments.size} arguments for a callee requiring $arityDescription")
            }

            val typeSubstitutions = linkedMapOf<String, ChirTypeRef>()
            if (virtualMethodContext == null) {
                bindExplicitFunctionTypeArguments(calleeValueId, instantiatedTypeArguments, typeSubstitutions, expressionId)
                bindOwnerTypeArguments(calleeValueId, thisType, typeSubstitutions, expressionId)
            } else {
                val receiverParameterType = functionType.parameterTypes.first()
                val receiver = checkNotNull(dispatchReceiver)
                if (!ChirTypeRelations.constrain(receiverParameterType, receiver.type, typeSubstitutions)) {
                    fail(
                        "INVOKE EXPRESSION#$expressionId receiver has type ${receiver.type.renderName}, " +
                            "but its original method expects ${receiverParameterType.renderName}",
                    )
                }
                if (virtualMethodContext.genericTypeParameters.size != instantiatedTypeArguments.size) {
                    fail(
                        "INVOKE EXPRESSION#$expressionId has ${instantiatedTypeArguments.size} instantiated type arguments " +
                            "for a virtual method declaring ${virtualMethodContext.genericTypeParameters.size} generic parameters",
                    )
                }
                virtualMethodContext.genericTypeParameters.zip(instantiatedTypeArguments).forEachIndexed { index, (parameter, argument) ->
                    val genericType = parameter.typeOrNull() as? ChirGenericType
                        ?: fail("INVOKE EXPRESSION#$expressionId generic parameter #$index is not a generic type")
                    if (!ChirTypeRelations.constrain(ChirResolvedTypeRef(genericType), argument, typeSubstitutions)) {
                        fail("INVOKE EXPRESSION#$expressionId generic argument #$index conflicts with another binding")
                    }
                }
            }
            arguments.take(fixedParameterCount).forEachIndexed { index, argument ->
                if (!ChirTypeRelations.constrain(argumentParameterTypes[index], argument.type, typeSubstitutions)) {
                    fail(
                        "${indexedExpression.table.schemaType.uppercase()} EXPRESSION#$expressionId argument $index has type ${argument.type.renderName}, " +
                            "but its callee expects ${argumentParameterTypes[index].renderName}",
                    )
                }
            }

            val declaredResultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            val declaredResultType = declaredResultTypeId?.let(::resolveType)
            val resultLocalValue = readResultLocal(expression, expressionId)
            val resultLocalType = resultLocalValue.type
            if (declaredResultType != null &&
                !ChirTypeRelations.areEquivalent(declaredResultType, resultLocalType)
            ) {
                fail("APPLY EXPRESSION#$expressionId resultTy disagrees with its result LocalVar")
            }
            val actualResultType = declaredResultType ?: resultLocalType
            if (actualResultType != null &&
                !ChirTypeRelations.constrain(functionType.returnType, actualResultType, typeSubstitutions)
            ) {
                fail("APPLY EXPRESSION#$expressionId result type disagrees with its callee return type")
            }
            val instantiatedFunctionType = ChirTypeRelations.substitute(
                ChirResolvedTypeRef(functionType),
                typeSubstitutions,
            ).typeOrNull() as? ChirFunctionType
                ?: fail("${indexedExpression.table.schemaType.uppercase()} EXPRESSION#$expressionId did not produce a function type after substitution")
            val instantiatedArgumentParameterTypes = if (virtualMethodContext == null) {
                buildList {
                    instantiatedFunctionType.receiverType?.let(::add)
                    addAll(instantiatedFunctionType.parameterTypes)
                }
            } else {
                instantiatedFunctionType.parameterTypes.drop(1)
            }
            arguments.take(fixedParameterCount).forEachIndexed { index, argument ->
                if (!ChirTypeRelations.areEquivalent(instantiatedArgumentParameterTypes[index], argument.type)) {
                    fail("${indexedExpression.table.schemaType.uppercase()} EXPRESSION#$expressionId argument $index disagrees with the instantiated callee parameter")
                }
            }
            if (actualResultType != null && !ChirTypeRelations.areEquivalent(instantiatedFunctionType.returnType, actualResultType)) {
                fail("${indexedExpression.table.schemaType.uppercase()} EXPRESSION#$expressionId result disagrees with the instantiated callee return type")
            }
            return ChirCallExpression(
                semanticId = expressionId(expressionId),
                callee = callee,
                arguments = arguments,
                resultType = actualResultType ?: instantiatedFunctionType.returnType,
                isSuperCall = isSuperCall,
                instantiatedTypeArguments = instantiatedTypeArguments,
                thisType = thisType,
                virtualMethodContext = virtualMethodContext,
                dispatchReceiver = dispatchReceiver,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun bindExplicitFunctionTypeArguments(
            calleeValueId: UInt,
            instantiatedTypeArguments: List<ChirTypeRef>,
            substitutions: MutableMap<String, ChirTypeRef>,
            expressionId: UInt,
        ) {
            if (instantiatedTypeArguments.isEmpty()) return
            val callee = valuesById[calleeValueId]
                ?: fail("APPLY EXPRESSION#$expressionId references missing callee VALUE#$calleeValueId")
            val genericParameterIds = when (callee.table.schemaType) {
                "Func", "ImportedFunc" -> callee.table.requiredReferences(
                    "genericTypeParams",
                    OfficialChirV113TableDomain.TYPE,
                )

                else -> fail(
                    "APPLY EXPRESSION#$expressionId has instantiated type arguments but its callee " +
                        "VALUE#$calleeValueId (${callee.table.schemaType}) has no generic declaration metadata",
                )
            }
            if (genericParameterIds.size != instantiatedTypeArguments.size) {
                fail(
                    "APPLY EXPRESSION#$expressionId has ${instantiatedTypeArguments.size} instantiated type arguments " +
                        "for a callee declaring ${genericParameterIds.size} generic parameters",
                )
            }
            genericParameterIds.zip(instantiatedTypeArguments).forEachIndexed { index, (parameterTypeId, argumentType) ->
                val genericType = resolveType(parameterTypeId).typeOrNull() as? ChirGenericType
                    ?: fail("APPLY EXPRESSION#$expressionId generic parameter #$index is not a generic type")
                if (!ChirTypeRelations.constrain(
                        ChirResolvedTypeRef(genericType),
                        argumentType,
                        substitutions,
                    )
                ) {
                    fail("APPLY EXPRESSION#$expressionId generic argument #$index conflicts with another binding")
                }
            }
        }

        private fun bindOwnerTypeArguments(
            calleeValueId: UInt,
            thisType: ChirTypeRef?,
            substitutions: MutableMap<String, ChirTypeRef>,
            expressionId: UInt,
        ) {
            if (thisType == null) return
            val callee = valuesById[calleeValueId]
                ?: fail("APPLY EXPRESSION#$expressionId references missing callee VALUE#$calleeValueId")
            val ownerDefinitionId = callee.table.optionalReference(
                "declaredParent",
                OfficialChirV113TableDomain.DEFINITION,
            )?.takeIf { it != 0u } ?: return
            val ownerDefinition = definitionsById[ownerDefinitionId]
                ?: fail("APPLY EXPRESSION#$expressionId references missing owner DEFINITION#$ownerDefinitionId")
            if (ownerDefinition.table.schemaType !in setOf("ClassDef", "EnumDef", "StructDef", "ExtendDef")) return
            val customDefinition = ownerDefinition.table.requiredTable("base")
            val ownerTypeId = customDefinition.optionalReference("type", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u } ?: return
            val ownerType = resolveType(ownerTypeId)
            val receiverType = (thisType.typeOrNull() as? ChirRefType)?.referencedType ?: thisType
            if (!ChirTypeRelations.constrain(ownerType, receiverType, substitutions)) {
                fail("APPLY EXPRESSION#$expressionId thisType disagrees with callee owner type ${ownerType.renderName}")
            }
        }

        private fun importUnary(indexedExpression: OfficialChirV113IndexedTable): ChirUnaryExpression {
            val expressionId = indexedExpression.id
            val expression = indexedExpression.expressionBase()
            val kind = expression.requiredScalarInt("kind").toUByte()
            val operandIds = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operandIds.size != 1) fail("unary EXPRESSION#$expressionId must have one operand")
            val operand = resolveValue(operandIds.single())
            val operandType = operand.type.typeOrNull()
            val resultTypeId = expression.requiredReference("resultTy", OfficialChirV113TableDomain.TYPE)
            val resultType = resolveType(resultTypeId)
            val operator = when (kind) {
                CHIRExprKind.NEG -> if (operandType in FLOAT_TYPES) ChirUnaryOperator.FLOAT_NEG else ChirUnaryOperator.INT_NEG
                CHIRExprKind.NOT -> if (operandType == ChirPrimitiveType.BOOL) ChirUnaryOperator.LOGICAL_NOT else ChirUnaryOperator.BIT_NOT
                CHIRExprKind.BITNOT -> ChirUnaryOperator.BIT_NOT
                else -> fail("official unary expression kind $kind at EXPRESSION#$expressionId has no repository CHIR mapping")
            }
            val overflowStrategy = importOverflowStrategy(
                indexedExpression.table.requiredScalarInt("overflowStrategy").toUByte(),
                "unary EXPRESSION#$expressionId",
            )
            val resultLocalValue = validateResultLocal(expression, expressionId, resultType)
            return ChirUnaryExpression(
                semanticId = expressionId(expressionId),
                operator = operator.canonicalName,
                operand = operand,
                resultType = resultType,
                resultLocalValue = resultLocalValue,
                overflowStrategy = overflowStrategy,
            )
        }

        private fun importBinary(indexedExpression: OfficialChirV113IndexedTable): ChirBinaryExpression {
            val expressionId = indexedExpression.id
            val expression = indexedExpression.expressionBase()
            val kind = expression.requiredScalarInt("kind").toUByte()
            val operandIds = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operandIds.size != 2) fail("binary EXPRESSION#$expressionId must have two operands")
            val left = resolveValue(operandIds[0])
            val right = resolveValue(operandIds[1])
            val operandType = left.type.typeOrNull()
            if (kind == CHIRExprKind.EXP) {
                val leftType = left.type.typeOrNull()
                val rightType = right.type.typeOrNull()
                val validOperands = when (leftType) {
                    ChirPrimitiveType.INT64 -> rightType == ChirPrimitiveType.UINT64
                    ChirPrimitiveType.FLOAT64 -> rightType == ChirPrimitiveType.INT64 || rightType == ChirPrimitiveType.FLOAT64
                    else -> false
                }
                if (!validOperands) {
                    fail(
                        "EXP EXPRESSION#$expressionId has unsupported operands " +
                            "${left.type.renderName} ** ${right.type.renderName}",
                    )
                }
            } else if (kind == CHIRExprKind.LSHIFT || kind == CHIRExprKind.RSHIFT) {
                val rightType = right.type.typeOrNull()
                if (operandType !in INTEGER_TYPES || rightType != ChirPrimitiveType.INT64) {
                    fail(
                        "shift EXPRESSION#$expressionId requires an integer left operand and an Int64 shift count; " +
                            "found ${left.type.renderName} and ${right.type.renderName}",
                    )
                }
            } else if (!ChirTypeRelations.areEquivalent(left.type, right.type)) {
                fail(
                    "binary kind $kind EXPRESSION#$expressionId operands VALUE#${operandIds[0]} (${left.type.renderName}) and " +
                        "VALUE#${operandIds[1]} (${right.type.renderName}) have different types",
                )
            }
            val resultTypeId = expression.requiredReference("resultTy", OfficialChirV113TableDomain.TYPE)
            val resultType = resolveType(resultTypeId)
            if (kind == CHIRExprKind.EXP && !ChirTypeRelations.areEquivalent(left.type, resultType)) {
                fail(
                    "EXP EXPRESSION#$expressionId result type ${resultType.renderName} does not match " +
                        "base type ${left.type.renderName}",
                )
            }
            val unsigned = operandType in UNSIGNED_INTEGER_TYPES
            val operator = when (kind) {
                CHIRExprKind.ADD -> ChirBinaryOperator.ADD
                CHIRExprKind.SUB -> ChirBinaryOperator.SUB
                CHIRExprKind.MUL -> ChirBinaryOperator.MUL
                CHIRExprKind.EXP -> ChirBinaryOperator.POWER
                CHIRExprKind.DIV -> if (unsigned) ChirBinaryOperator.UNSIGNED_DIV else ChirBinaryOperator.SIGNED_DIV
                CHIRExprKind.MOD -> if (unsigned) ChirBinaryOperator.UNSIGNED_REM else ChirBinaryOperator.SIGNED_REM
                CHIRExprKind.LSHIFT -> ChirBinaryOperator.SHIFT_LEFT
                CHIRExprKind.RSHIFT -> if (unsigned) ChirBinaryOperator.UNSIGNED_SHIFT_RIGHT else ChirBinaryOperator.SIGNED_SHIFT_RIGHT
                CHIRExprKind.BITAND,
                CHIRExprKind.AND,
                -> ChirBinaryOperator.BIT_AND
                CHIRExprKind.BITOR,
                CHIRExprKind.OR,
                -> ChirBinaryOperator.BIT_OR
                CHIRExprKind.BITXOR -> ChirBinaryOperator.BIT_XOR
                CHIRExprKind.LT -> when {
                    operandType in FLOAT_TYPES -> ChirBinaryOperator.FLOAT_LESS
                    unsigned -> ChirBinaryOperator.UNSIGNED_LESS
                    else -> ChirBinaryOperator.SIGNED_LESS
                }
                CHIRExprKind.LE -> when {
                    operandType in FLOAT_TYPES -> ChirBinaryOperator.FLOAT_LESS_OR_EQUAL
                    unsigned -> ChirBinaryOperator.UNSIGNED_LESS_OR_EQUAL
                    else -> ChirBinaryOperator.SIGNED_LESS_OR_EQUAL
                }
                CHIRExprKind.GT -> when {
                    operandType in FLOAT_TYPES -> ChirBinaryOperator.FLOAT_GREATER
                    unsigned -> ChirBinaryOperator.UNSIGNED_GREATER
                    else -> ChirBinaryOperator.SIGNED_GREATER
                }
                CHIRExprKind.GE -> when {
                    operandType in FLOAT_TYPES -> ChirBinaryOperator.FLOAT_GREATER_OR_EQUAL
                    unsigned -> ChirBinaryOperator.UNSIGNED_GREATER_OR_EQUAL
                    else -> ChirBinaryOperator.SIGNED_GREATER_OR_EQUAL
                }
                CHIRExprKind.EQUAL -> if (operandType in FLOAT_TYPES) ChirBinaryOperator.FLOAT_EQUAL else ChirBinaryOperator.EQUAL
                CHIRExprKind.NOTEQUAL -> if (operandType in FLOAT_TYPES) ChirBinaryOperator.FLOAT_NOT_EQUAL else ChirBinaryOperator.NOT_EQUAL
                else -> fail("official binary expression kind $kind at EXPRESSION#$expressionId has no repository CHIR mapping")
            }
            val overflowStrategy = importOverflowStrategy(
                indexedExpression.table.requiredScalarInt("overflowStrategy").toUByte(),
                "binary EXPRESSION#$expressionId",
            )
            val resultLocalValue = validateResultLocal(expression, expressionId, resultType)
            return ChirBinaryExpression(
                semanticId = expressionId(expressionId),
                operator = operator.canonicalName,
                left = left,
                right = right,
                resultType = resultType,
                resultLocalValue = resultLocalValue,
                overflowStrategy = overflowStrategy,
            )
        }

        private fun importOverflowStrategy(strategy: UByte, expressionLabel: String): ChirOverflowStrategy = when (strategy) {
            OverflowStrategy.NA -> ChirOverflowStrategy.NOT_APPLICABLE
            OverflowStrategy.CHECKED -> ChirOverflowStrategy.CHECKED
            OverflowStrategy.WRAPPING -> ChirOverflowStrategy.WRAPPING
            OverflowStrategy.THROWING -> ChirOverflowStrategy.THROWING
            OverflowStrategy.SATURATING -> ChirOverflowStrategy.SATURATING
            else -> fail("$expressionLabel has invalid overflow strategy $strategy")
        }

        private fun importTuple(indexedExpression: OfficialChirV113IndexedTable): ChirTupleExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.TUPLE, expressionId)
            val expression = indexedExpression.expressionBase()
            val elements = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
                .map(::resolveValue)
            val resultTypeId = expression.requiredReference("resultTy", OfficialChirV113TableDomain.TYPE)
            val resultType = resolveType(resultTypeId)
            val resultLocalValue = validateResultLocal(expression, expressionId, resultType)
            return ChirTupleExpression(
                semanticId = expressionId(expressionId),
                elements = elements,
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importField(indexedExpression: OfficialChirV113IndexedTable): ChirFieldExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.FIELD, expressionId)
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operands.size != 1) fail("FIELD EXPRESSION#$expressionId must have one base value")
            val path = indexedExpression.table.requiredUnsignedLongs("path")
            if (path.isEmpty()) fail("FIELD EXPRESSION#$expressionId must have a non-empty field path")
            val resultType = resolveType(expression.requiredReference("resultTy", OfficialChirV113TableDomain.TYPE))
            val resultLocalValue = validateResultLocal(expression, expressionId, resultType)
            return ChirFieldExpression(
                semanticId = expressionId(expressionId),
                base = resolveValue(operands.single()),
                path = path,
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importFieldByName(indexedExpression: OfficialChirV113IndexedTable): ChirFieldExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.FIELD_BY_NAME, expressionId)
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            if (operands.size != 1) fail("FIELD_BY_NAME EXPRESSION#$expressionId must have one base value")
            val memberNames = indexedExpression.table.requiredStrings("names")
            if (memberNames.isEmpty() || memberNames.any(String::isBlank)) {
                fail("FIELD_BY_NAME EXPRESSION#$expressionId must have a non-empty member path")
            }

            val baseValueId = operands.single()
            val base = resolveValue(baseValueId)
            val path = resolveFieldPathByName(baseValueId, memberNames, expressionId)
            val resultType = resolveType(expression.requiredReference("resultTy", OfficialChirV113TableDomain.TYPE))
            val projected = ChirTypeRelations.projectFieldType(base.type, path)
                ?: fail("FIELD_BY_NAME EXPRESSION#$expressionId has no aggregate projection for $memberNames")
            if (!ChirTypeRelations.areEquivalent(projected.resultType, resultType)) {
                fail(
                    "FIELD_BY_NAME EXPRESSION#$expressionId resolves to ${projected.resultType.renderName}, " +
                        "but its result type is ${resultType.renderName}",
                )
            }
            val resultLocalValue = validateResultLocal(expression, expressionId, resultType)
            return ChirFieldExpression(
                semanticId = expressionId(expressionId),
                base = base,
                path = path,
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importTypeCast(indexedExpression: OfficialChirV113IndexedTable): ChirOtherExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.TYPECAST, expressionId)
            val overflowStrategy = indexedExpression.table.requiredScalarInt("overflowStrategy").toUByte()
            val operation = when (overflowStrategy) {
                OverflowStrategy.NA -> ChirOtherOperation.CANGJIE_TYPE_CAST_NA
                OverflowStrategy.CHECKED -> ChirOtherOperation.CANGJIE_TYPE_CAST_CHECKED
                OverflowStrategy.WRAPPING -> ChirOtherOperation.CANGJIE_TYPE_CAST_WRAPPING
                OverflowStrategy.THROWING -> ChirOtherOperation.CANGJIE_TYPE_CAST_THROWING
                OverflowStrategy.SATURATING -> ChirOtherOperation.CANGJIE_TYPE_CAST_SATURATING
                else -> fail("TYPECAST EXPRESSION#$expressionId has invalid overflow strategy $overflowStrategy")
            }
            val expression = indexedExpression.expressionBase()
            val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
            val operand = operands.singleOrNull()
                ?: fail("TYPECAST EXPRESSION#$expressionId must have one source operand")
            val resultType = resolveType(
                expression.requiredReference("resultTy", OfficialChirV113TableDomain.TYPE),
            )
            val resultLocalValue = validateResultLocal(expression, expressionId, resultType)
            return ChirOtherExpression(
                semanticId = expressionId(expressionId),
                operation = operation.canonicalName,
                operands = listOf(resolveValue(operand)),
                resultType = resultType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun resolveFieldPathByName(
            baseValueId: UInt,
            memberNames: List<String>,
            expressionId: UInt,
        ): List<ULong> {
            var currentTypeId = valuesById[baseValueId]
                ?.table
                ?.valueBase()
                ?.requiredReference("type", OfficialChirV113TableDomain.TYPE)
                ?: fail("FIELD_BY_NAME EXPRESSION#$expressionId base VALUE#$baseValueId has no type")

            return memberNames.mapIndexed { segmentIndex, memberName ->
                val indexedType = typesById[currentTypeId]
                    ?: fail("FIELD_BY_NAME EXPRESSION#$expressionId references missing TYPE#$currentTypeId")
                val typeBase = indexedType.table.findNestedTable("Type")
                    ?: fail("FIELD_BY_NAME EXPRESSION#$expressionId TYPE#$currentTypeId has no Type table")
                val typeKind = typeBase.requiredScalarInt("kind").toUByte()
                if (typeKind != CHIRTypeKind.CLASS && typeKind != CHIRTypeKind.STRUCT) {
                    fail(
                        "FIELD_BY_NAME EXPRESSION#$expressionId path segment $segmentIndex '$memberName' " +
                            "cannot select a member from TYPE#$currentTypeId kind $typeKind",
                    )
                }
                val customType = indexedType.table.findNestedTable("CustomType")
                    ?: fail("FIELD_BY_NAME EXPRESSION#$expressionId TYPE#$currentTypeId has no CustomType table")
                val definitionId = customType.requiredReference(
                    "customTypeDef",
                    OfficialChirV113TableDomain.DEFINITION,
                )
                val definition = definitionsById[definitionId]
                    ?: fail("FIELD_BY_NAME EXPRESSION#$expressionId references missing DEFINITION#$definitionId")
                val customDefinition = definition.table.requiredTable("base")
                val memberVariables = customDefinition.sequence("instanceMemberVars")
                val matchingMembers = memberVariables.mapIndexedNotNull { index, memberValue ->
                    val member = (memberValue as? OfficialChirV113SemanticValue.Table)?.value
                        ?: fail(
                            "FIELD_BY_NAME EXPRESSION#$expressionId DEFINITION#$definitionId " +
                                "instanceMemberVars[$index] is not a MemberVarInfo",
                        )
                    if (member.requiredScalarString("name") == memberName) index to member else null
                }
                val (fieldIndex, member) = matchingMembers.singleOrNull()
                    ?: fail(
                        "FIELD_BY_NAME EXPRESSION#$expressionId resolves '$memberName' to " +
                            "${matchingMembers.size} fields in DEFINITION#$definitionId",
                    )
                currentTypeId = member.requiredReference("type", OfficialChirV113TableDomain.TYPE)
                fieldIndex.toULong()
            }
        }

        private fun readResultLocal(
            expression: OfficialChirV113SemanticTable,
            officialExpressionId: UInt,
        ): ChirLocalValue {
            val resultLocalId = expression.requiredReference("resultLocalVar", OfficialChirV113TableDomain.VALUE)
                .takeIf { it != 0u }
                ?: fail("EXPRESSION#$officialExpressionId has no result LocalVar")
            val resultLocal = valuesById[resultLocalId]?.takeIf { it.table.schemaType == "LocalVar" }
                ?: fail("EXPRESSION#$officialExpressionId has a non-LocalVar result")
            val valueBase = resultLocal.table.valueBase()
            val localType = valueBase
                .requiredReference("type", OfficialChirV113TableDomain.TYPE)
                .let(::resolveType)
            val associatedExpressionId = resultLocal.table.requiredReference(
                "associatedExpr",
                OfficialChirV113TableDomain.EXPRESSION,
            )
            if (associatedExpressionId != officialExpressionId) {
                fail("EXPRESSION#$officialExpressionId is not the associated expression of VALUE#$resultLocalId")
            }
            return ChirLocalValue(
                semanticId = valueId(resultLocalId),
                type = localType,
                name = valueBase.requiredScalarString("identifier"),
                associatedExpressionId = expressionId(officialExpressionId),
                isReturnValue = resultLocal.table.requiredScalarBoolean("isRetVal"),
            )
        }

        private fun validateResultLocal(
            expression: OfficialChirV113SemanticTable,
            officialExpressionId: UInt,
            resultType: ChirTypeRef,
        ): ChirLocalValue {
            val resultLocal = readResultLocal(expression, officialExpressionId)
            if (!ChirTypeRelations.areEquivalent(resultLocal.type, resultType)) {
                fail("EXPRESSION#$officialExpressionId result LocalVar disagrees with resultTy")
            }
            return resultLocal
        }

        private fun importIntrinsic(indexedExpression: OfficialChirV113IndexedTable): ChirExpression {
            val expressionId = indexedExpression.id
            verifyExpressionKind(indexedExpression, CHIRExprKind.INTRINSIC, expressionId)
            val intrinsicKind = indexedExpression.table.requiredScalarInt("intrinsicKind").toUShort()
            if (intrinsicKind != IntrinsicKind.PREINITIALIZE) {
                if (intrinsicKind > IntrinsicKind.EXCLUSIVE_SCOPE) {
                    fail("unknown official intrinsic kind $intrinsicKind at EXPRESSION#$expressionId")
                }
                val chirIntrinsicKind = ChirIntrinsicKind(intrinsicKind)
                val expression = indexedExpression.expressionBase()
                val operands = expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE)
                    .map(::resolveValue)
                val typeArguments = indexedExpression.table.requiredReferences(
                    "instantiatedTypeArgs",
                    OfficialChirV113TableDomain.TYPE,
                ).map(::resolveType)
                val declaredResultType = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                    ?.takeIf { it != 0u }
                    ?.let(::resolveType)
                val resultLocalId = expression.optionalReference("resultLocalVar", OfficialChirV113TableDomain.VALUE)
                    ?.takeIf { it != 0u }
                val resultLocalValue = resultLocalId?.let { readResultLocal(expression, expressionId) }
                if (declaredResultType != null && resultLocalValue != null &&
                    !ChirTypeRelations.areEquivalent(declaredResultType, resultLocalValue.type)
                ) {
                    fail("INTRINSIC EXPRESSION#$expressionId resultTy disagrees with its result LocalVar")
                }
                return ChirOtherExpression(
                    semanticId = expressionId(expressionId),
                    operation = chirIntrinsicKind.canonicalName,
                    operands = operands,
                    resultType = declaredResultType ?: resultLocalValue?.type,
                    resultLocalValue = resultLocalValue,
                    intrinsicKind = chirIntrinsicKind,
                    intrinsicTypeArguments = typeArguments,
                )
            }
            if (indexedExpression.table.requiredReferences("instantiatedTypeArgs", OfficialChirV113TableDomain.TYPE).isNotEmpty()) {
                fail("PREINITIALIZE EXPRESSION#$expressionId has instantiated type arguments")
            }

            val expression = indexedExpression.expressionBase()
            if (expression.requiredReferences("operands", OfficialChirV113TableDomain.VALUE).isNotEmpty()) {
                fail("PREINITIALIZE EXPRESSION#$expressionId has unexpected operands")
            }
            val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
            val declaredResultTypeId = expression.optionalReference("resultTy", OfficialChirV113TableDomain.TYPE)
                ?.takeIf { it != 0u }
            if (declaredResultTypeId != null &&
                !ChirTypeRelations.areEquivalent(resolveType(declaredResultTypeId), unitType)
            ) {
                fail("PREINITIALIZE EXPRESSION#$expressionId resultTy is not Unit")
            }
            val resultLocalValue = validateResultLocal(expression, expressionId, unitType)

            val runtimeFunctionId = ChirSemanticId("runtime:cj:preinitialize-package")
            val pointerType = ChirResolvedTypeRef(ChirCPointerType(ChirResolvedTypeRef(ChirPrimitiveType.INT8)))
            val runtimeFunctionType = ChirFunctionType(
                parameterTypes = listOf(pointerType),
                returnType = unitType,
            )
            val runtimeFunctionName = "CJ_MRT_PreInitializePackage"
            importedFunctions.getOrPut(runtimeFunctionId) {
                DefaultChirFunctionDeclaration(
                    semanticId = runtimeFunctionId,
                    name = runtimeFunctionName,
                    returnType = unitType,
                    parameters = listOf(
                        DefaultChirVariableDeclaration(
                            semanticId = ChirSemanticId("${runtimeFunctionId.value}:context"),
                            name = "context",
                            type = pointerType,
                            mutable = false,
                        ),
                    ),
                    blocks = emptyList(),
                    entryBlockId = null,
                    hasBody = false,
                    isImported = true,
                )
            }
            val callee = ChirImportedFunctionValue(
                semanticId = runtimeFunctionId,
                type = ChirResolvedTypeRef(runtimeFunctionType),
                name = runtimeFunctionName,
            )
            return ChirCallExpression(
                semanticId = expressionId(expressionId),
                callee = callee,
                arguments = listOf(
                    ChirConstantValue(
                        semanticId = ChirSemanticId("${expressionId(expressionId).value}:context"),
                        type = pointerType,
                        literal = "null",
                    ),
                ),
                resultType = unitType,
                resultLocalValue = resultLocalValue,
            )
        }

        private fun importReturn(
            expressionId: UInt,
            blockId: UInt,
            expressions: MutableList<org.cangnova.cangjie.chir.core.expression.ChirExpression>,
        ): ChirReturnTerminator {
            val base = expressionsById[expressionId]?.expressionBase()
                ?: fail("EXIT EXPRESSION#$expressionId has no expression base")
            if (base.requiredReferences("operands", OfficialChirV113TableDomain.VALUE).isNotEmpty()) {
                fail("EXIT EXPRESSION#$expressionId has unexpected operands")
            }
            if (returnSlotId == 0u) {
                if (functionType.returnType.typeOrNull() != ChirPrimitiveType.UNIT) {
                    fail("non-Unit function ${semanticId.value} has no official return value slot")
                }
                return ChirReturnTerminator(expressionId(expressionId))
            }
            val returnSlot = resolveValue(returnSlotId)
            return when (val slotType = returnSlot.type.typeOrNull()) {
                is ChirRefType -> {
                    if (slotType.referencedType != functionType.returnType) {
                        fail("function ${semanticId.value} return slot has an incompatible referenced type")
                    }
                    val loadId = ChirSemanticId("${semanticId.value}:return-load:${blockId}")
                    val loadResult = ChirLocalValue(
                        semanticId = semanticIds.syntheticId(
                            OfficialChirV113TableDomain.VALUE,
                            "return-load:" + blockId,
                        ),
                        type = functionType.returnType,
                        name = "%official_return_value",
                        associatedExpressionId = loadId,
                    )
                    expressions += ChirMemoryExpression(
                        semanticId = loadId,
                        operation = "load",
                        address = returnSlot,
                        resultType = functionType.returnType,
                        resultLocalValue = loadResult,
                    )
                    ChirReturnTerminator(
                        semanticId = expressionId(expressionId),
                        returnValue = loadResult,
                    )
                }
                functionType.returnType -> ChirReturnTerminator(
                    semanticId = expressionId(expressionId),
                    returnValue = returnSlot,
                )
                else -> fail("function ${semanticId.value} return slot has an incompatible type $slotType")
            }
        }

        private fun resolveValue(valueId: UInt): org.cangnova.cangjie.chir.core.value.ChirValue {
            val indexed = valuesById[valueId]
                ?: fail("function ${semanticId.value} references missing VALUE#$valueId")
            val value = indexed.table.valueBase()
            val type = value.requiredReference("type", OfficialChirV113TableDomain.TYPE).let(::resolveType)
            val name = value.requiredScalarString("identifier")
            return when (indexed.table.schemaType) {
                "Parameter" -> {
                    val owner = indexed.table.requiredReference("ownedFunc", OfficialChirV113TableDomain.VALUE)
                    if (owner != function.id) fail("PARAMETER VALUE#$valueId belongs to another function")
                    ChirParameterValue(valueId(valueId), type, name, semanticId)
                }
                "LocalVar" -> {
                    val associatedExpressionId = indexed.table.optionalReference(
                        "associatedExpr",
                        OfficialChirV113TableDomain.EXPRESSION,
                    )?.takeIf { it != 0u }
                    ChirLocalValue(
                        semanticId = valueId(valueId),
                        type = type,
                        name = name,
                        associatedExpressionId = associatedExpressionId?.let(::expressionId),
                        isReturnValue = indexed.table.requiredScalarBoolean("isRetVal"),
                    )
                }
                "GlobalVar" -> ChirGlobalValue(ChirSemanticId(officialValueIdentity(indexed)), type, name)
                "Func" -> ChirFunctionValue(
                    semanticId = functionIdentitiesByOfficialId[valueId]
                        ?: ChirSemanticId(officialValueIdentity(indexed)),
                    type = type,
                    name = indexed.table.requiredScalarString("srcCodeIdentifier"),
                )
                "ImportedFunc" -> ChirImportedFunctionValue(
                    semanticId = ChirSemanticId(officialValueIdentity(indexed)),
                    type = type,
                    name = indexed.table.requiredScalarString("srcCodeIdentifier"),
                )
                "IntLiteral" -> ChirConstantValue(
                    semanticId = valueId(valueId),
                    type = type,
                    literal = integerLiteral(indexed.table, type),
                )
                "StringLiteral" -> ChirConstantValue(
                    semanticId = valueId(valueId),
                    type = type,
                    literal = indexed.table.requiredScalarString("val"),
                )
                "BoolLiteral" -> {
                    val literal = indexed.table.requiredScalarBoolean("val")
                    ChirConstantValue(
                        semanticId = valueId(valueId),
                        type = type,
                        literal = if (literal) "1" else "0",
                    )
                }
                "FloatLiteral" -> ChirConstantValue(
                    semanticId = valueId(valueId),
                    type = type,
                    literal = indexed.table.requiredScalarDouble("val").toString(),
                )
                "RuneLiteral" -> ChirConstantValue(
                    semanticId = valueId(valueId),
                    type = type,
                    literal = indexed.table.requiredScalarUInt("val").toString(),
                )
                "UnitLiteral" -> ChirConstantValue(
                    semanticId = valueId(valueId),
                    type = type,
                    literal = "unit",
                )
                "NullLiteral" -> ChirConstantValue(
                    semanticId = valueId(valueId),
                    type = type,
                    literal = "null",
                    constantKind = ChirConstantKind.NULL,
                )
                else -> fail("Official CHIR ${indexed.table.schemaType} value VALUE#$valueId has no repository CHIR lowering")
            }
        }

        private fun integerLiteral(
            literal: OfficialChirV113SemanticTable,
            type: ChirTypeRef,
        ): String {
            val raw = literal.requiredScalarLong("val").toULong()
            return when ((type.typeOrNull() as? ChirPrimitiveType)) {
                ChirPrimitiveType.INT8 -> raw.toByte().toString()
                ChirPrimitiveType.INT16 -> raw.toShort().toString()
                ChirPrimitiveType.INT32 -> raw.toInt().toString()
                ChirPrimitiveType.INT64,
                ChirPrimitiveType.INT_NATIVE,
                -> raw.toLong().toString()
                ChirPrimitiveType.UINT8 -> (raw and UByte.MAX_VALUE.toULong()).toString()
                ChirPrimitiveType.UINT16 -> (raw and UShort.MAX_VALUE.toULong()).toString()
                ChirPrimitiveType.UINT32 -> (raw and UInt.MAX_VALUE.toULong()).toString()
                ChirPrimitiveType.UINT64,
                ChirPrimitiveType.UINT_NATIVE,
                -> raw.toString()
                else -> fail("Official CHIR integer literal has a non-integer type ${type.renderName}")
            }
        }

        private fun resolveType(typeId: UInt): ChirTypeRef {
            if (typeId == 0u) fail("function ${semanticId.value} references the invalid TYPE#0")
            resolvedTypes[typeId]?.let { return it }
            if (!resolvingTypes.add(typeId)) return recursiveTypeReference(typeId)
            try {
                val indexed = typesById[typeId] ?: fail("function ${semanticId.value} references missing TYPE#$typeId")
                val type = indexed.table.findNestedTable("Type")
                    ?: fail("official TYPE#$typeId has no CHIR Type base table")
                val kind = type.requiredScalarInt("kind").toUByte()
                val arguments = type.requiredReferences("argTys", OfficialChirV113TableDomain.TYPE)
                    .map(::resolveType)
                val resolved = when (kind) {
                    CHIRTypeKind.UNIT -> ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
                    CHIRTypeKind.BOOLEAN -> ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
                    CHIRTypeKind.INT8 -> ChirResolvedTypeRef(ChirPrimitiveType.INT8)
                    CHIRTypeKind.INT16 -> ChirResolvedTypeRef(ChirPrimitiveType.INT16)
                    CHIRTypeKind.INT32 -> ChirResolvedTypeRef(ChirPrimitiveType.INT32)
                    CHIRTypeKind.INT64 -> ChirResolvedTypeRef(ChirPrimitiveType.INT64)
                    CHIRTypeKind.INT_NATIVE -> ChirResolvedTypeRef(ChirPrimitiveType.INT_NATIVE)
                    CHIRTypeKind.UINT8 -> ChirResolvedTypeRef(ChirPrimitiveType.UINT8)
                    CHIRTypeKind.UINT16 -> ChirResolvedTypeRef(ChirPrimitiveType.UINT16)
                    CHIRTypeKind.UINT32 -> ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
                    CHIRTypeKind.UINT64 -> ChirResolvedTypeRef(ChirPrimitiveType.UINT64)
                    CHIRTypeKind.UINT_NATIVE -> ChirResolvedTypeRef(ChirPrimitiveType.UINT_NATIVE)
                    CHIRTypeKind.FLOAT16 -> ChirResolvedTypeRef(ChirPrimitiveType.FLOAT16)
                    CHIRTypeKind.FLOAT32 -> ChirResolvedTypeRef(ChirPrimitiveType.FLOAT32)
                    CHIRTypeKind.FLOAT64 -> ChirResolvedTypeRef(ChirPrimitiveType.FLOAT64)
                    CHIRTypeKind.RUNE -> ChirResolvedTypeRef(ChirPrimitiveType.RUNE)
                    CHIRTypeKind.NOTHING -> ChirResolvedTypeRef(ChirPrimitiveType.NOTHING)
                    CHIRTypeKind.VOID -> ChirResolvedTypeRef(ChirPrimitiveType.VOID)
                    CHIRTypeKind.TUPLE -> ChirResolvedTypeRef(ChirTupleType(arguments))
                    CHIRTypeKind.STRUCT -> ChirResolvedTypeRef(
                        ChirStructType(
                            declarationId = customTypeDeclarationId(indexed, typeId, "StructDef"),
                            fieldTypes = customTypeFieldTypes(indexed, typeId, arguments),
                            typeArguments = arguments,
                        ),
                    )
                    CHIRTypeKind.CLASS -> ChirResolvedTypeRef(
                        ChirClassType(
                            declarationId = customTypeDeclarationId(indexed, typeId, "ClassDef"),
                            fieldTypes = customTypeFieldTypes(indexed, typeId, arguments),
                            typeArguments = arguments,
                        ),
                    )
                    CHIRTypeKind.ENUM -> ChirResolvedTypeRef(
                        ChirEnumType(
                            declarationId = customTypeDeclarationId(indexed, typeId, "EnumDef"),
                            cases = enumCases(indexed, typeId, arguments),
                            typeArguments = arguments,
                            nonExhaustive = enumIsNonExhaustive(indexed, typeId),
                        ),
                    )
                    CHIRTypeKind.GENERIC -> {
                        val genericType = indexed.table.findNestedTable("GenericType")
                            ?: fail("official TYPE#$typeId has no GenericType metadata")
                        ChirResolvedTypeRef(
                            ChirGenericType(
                                identifier = genericType.requiredScalarString("srcCodeIdentifier"),
                                upperBounds = genericType.requiredReferences(
                                    "upperBounds",
                                    OfficialChirV113TableDomain.TYPE,
                                ).map(::resolveType),
                                semanticIdentity = genericType.requiredScalarString("identifier"),
                            ),
                        )
                    }
                    CHIRTypeKind.FUNC -> {
                        val funcType = indexed.table.findNestedTable("FuncType")
                            ?: fail("official TYPE#$typeId has no FuncType metadata")
                        val isCFunctionType = funcType.requiredScalarBoolean("isCFuncType")
                        val hasVarArg = funcType.requiredScalarBoolean("hasVarArg")
                        val returnType = arguments.lastOrNull()
                            ?: fail("official TYPE#$typeId function type has no return type")
                        ChirResolvedTypeRef(
                            ChirFunctionType(
                                parameterTypes = arguments.dropLast(1),
                                returnType = returnType,
                                isCFunctionType = isCFunctionType,
                                hasVarArg = hasVarArg,
                            ),
                        )
                    }
                    CHIRTypeKind.RAWARRAY -> {
                        val rawArrayType = indexed.table.findNestedTable("RawArrayType")
                            ?: fail("official TYPE#$typeId has no RawArrayType metadata")
                        val elementType = arguments.singleOrNull()
                            ?: fail("official TYPE#$typeId raw array must have one element type")
                        val dimensions = rawArrayType.requiredScalarUInt("dims")
                        if (dimensions == 0u) fail("official TYPE#$typeId raw array has zero dimensions")
                        ChirResolvedTypeRef(ChirRawArrayType(elementType, dimensions))
                    }
                    CHIRTypeKind.VARRAY -> {
                        val varrayType = indexed.table.findNestedTable("VArrayType")
                            ?: fail("official TYPE#$typeId has no VArrayType metadata")
                        val elementType = arguments.singleOrNull()
                            ?: fail("official TYPE#$typeId VArray must have one element type")
                        val size = varrayType.requiredScalarLong("size")
                        if (size < 0L) {
                            fail("official TYPE#$typeId VArray has unsupported size $size")
                        }
                        ChirResolvedTypeRef(ChirVArrayType(elementType, size))
                    }
                    CHIRTypeKind.C_POINTER -> ChirResolvedTypeRef(
                        ChirCPointerType(
                            arguments.singleOrNull()
                                ?: fail("official TYPE#$typeId C pointer must have one pointee type"),
                        ),
                    )
                    CHIRTypeKind.C_STRING -> ChirResolvedTypeRef(ChirCStringType)
                    CHIRTypeKind.REFTYPE -> {
                        val referredType = arguments.singleOrNull()
                            ?: fail("official TYPE#$typeId reference type must have one argument")
                        ChirResolvedTypeRef(ChirRefType(referredType, mutable = true))
                    }
                    CHIRTypeKind.BOXTYPE -> ChirResolvedTypeRef(
                        ChirBoxType(
                            arguments.singleOrNull()
                                ?: fail("official TYPE#$typeId box type must have one element type"),
                        ),
                    )
                    CHIRTypeKind.THIS -> {
                        if (arguments.isNotEmpty()) fail("official TYPE#$typeId this type cannot have arguments")
                        ChirResolvedTypeRef(ChirThisType(functionOwnerTypeName(typeId)))
                    }
                    else -> fail("Official CHIR type kind $kind at TYPE#$typeId has no repository CHIR mapping")
                }
                resolvedTypes[typeId] = resolved
                return resolved
            } finally {
                resolvingTypes.remove(typeId)
            }
        }

        private fun recursiveTypeReference(typeId: UInt): ChirTypeRef {
            val indexedType = typesById[typeId]
                ?: fail("function ${semanticId.value} references missing recursive TYPE#$typeId")
            val type = indexedType.table.findNestedTable("Type")
                ?: fail("recursive official TYPE#$typeId has no CHIR Type base table")
            val kind = type.requiredScalarInt("kind").toUByte()
            val arguments = type.requiredReferences("argTys", OfficialChirV113TableDomain.TYPE)
                .map(::resolveType)
            return when (kind) {
                CHIRTypeKind.STRUCT -> ChirResolvedTypeRef(
                    ChirStructType(
                        declarationId = customTypeDeclarationId(indexedType, typeId, "StructDef"),
                        typeArguments = arguments,
                    ),
                )
                CHIRTypeKind.CLASS -> ChirResolvedTypeRef(
                    ChirClassType(
                        declarationId = customTypeDeclarationId(indexedType, typeId, "ClassDef"),
                        typeArguments = arguments,
                    ),
                )
                CHIRTypeKind.ENUM -> ChirResolvedTypeRef(
                    ChirEnumType(
                        declarationId = customTypeDeclarationId(indexedType, typeId, "EnumDef"),
                        typeArguments = arguments,
                        nonExhaustive = enumIsNonExhaustive(indexedType, typeId),
                    ),
                )
                CHIRTypeKind.GENERIC -> {
                    val genericType = indexedType.table.findNestedTable("GenericType")
                        ?: fail("recursive official TYPE#$typeId has no GenericType metadata")
                    ChirResolvedTypeRef(
                        ChirGenericType(
                            identifier = genericType.requiredScalarString("srcCodeIdentifier"),
                            semanticIdentity = genericType.requiredScalarString("identifier"),
                        ),
                    )
                }
                CHIRTypeKind.FUNC -> {
                    val functionType = indexedType.table.findNestedTable("FuncType")
                        ?: fail("recursive official TYPE#$typeId has no FuncType metadata")
                    val returnType = arguments.lastOrNull()
                        ?: fail("recursive official TYPE#$typeId function type has no return type")
                    ChirResolvedTypeRef(
                        ChirFunctionType(
                            parameterTypes = arguments.dropLast(1),
                            returnType = returnType,
                            isCFunctionType = functionType.requiredScalarBoolean("isCFuncType"),
                            hasVarArg = functionType.requiredScalarBoolean("hasVarArg"),
                        ),
                    )
                }
                CHIRTypeKind.REFTYPE -> ChirResolvedTypeRef(
                    ChirRefType(
                        referencedType = arguments.singleOrNull()
                            ?: fail("recursive official TYPE#$typeId reference type must have one argument"),
                        mutable = true,
                    ),
                )
                CHIRTypeKind.BOXTYPE -> ChirResolvedTypeRef(
                    ChirBoxType(
                        arguments.singleOrNull()
                            ?: fail("recursive official TYPE#$typeId box type must have one argument"),
                    ),
                )
                else -> fail(
                    "recursive official TYPE#$typeId has unsupported kind $kind and cannot be represented",
                )
            }
        }

        private fun verifyExpressionKind(
            indexedExpression: OfficialChirV113IndexedTable,
            expected: UByte,
            expressionId: UInt,
        ) {
            val actual = indexedExpression.expressionBase().requiredScalarInt("kind").toUByte()
            if (actual != expected) fail("official ${indexedExpression.table.schemaType} EXPRESSION#$expressionId has kind $actual")
        }

        private fun customTypeDeclarationId(
            indexedType: OfficialChirV113IndexedTable,
            typeId: UInt,
            expectedDefinitionType: String,
        ): ChirTypeDeclarationId {
            val customType = indexedType.table.findNestedTable("CustomType")
                ?: fail("official TYPE#$typeId has no CustomType table")
            val definitionId = customType.requiredReference("customTypeDef", OfficialChirV113TableDomain.DEFINITION)
            val definition = definitionsById[definitionId]
                ?: fail("official TYPE#$typeId references missing DEFINITION#$definitionId")
            if (definition.table.schemaType != expectedDefinitionType) {
                fail(
                    "official TYPE#$typeId has kind ${indexedType.table.findNestedTable("Type")?.requiredScalarInt("kind")}, " +
                        "but DEFINITION#$definitionId is ${definition.table.schemaType}",
                )
            }
            val customDefinition = definition.table.requiredTable("base")
            val definitionIdInBase = customDefinition.requiredReference(
                "customTypeDefID",
                OfficialChirV113TableDomain.DEFINITION,
            )
            if (definitionIdInBase != definitionId) {
                fail("official TYPE#$typeId resolves to an inconsistent custom type definition")
            }
            val packageName = customDefinition.requiredScalarString("packageName")
            val sourceName = customDefinition.requiredScalarString("srcCodeIdentifier")
            if (packageName.isBlank() || sourceName.isBlank()) {
                fail("official TYPE#$typeId custom type has no qualified source identity")
            }
            return ChirTypeDeclarationId(packageName, sourceName)
        }

        private fun functionOwnerTypeName(typeId: UInt): String {
            val ownerDefinitionId = function.table.optionalReference(
                "declaredParent",
                OfficialChirV113TableDomain.DEFINITION,
            )?.takeIf { it != 0u }
                ?: fail("official TYPE#$typeId uses this outside a declared custom type")
            val ownerDefinition = definitionsById[ownerDefinitionId]
                ?: fail("function ${semanticId.value} references missing owner DEFINITION#$ownerDefinitionId")
            if (ownerDefinition.table.schemaType !in setOf("ClassDef", "EnumDef", "StructDef", "ExtendDef")) {
                fail(
                    "function ${semanticId.value} this owner DEFINITION#$ownerDefinitionId is " +
                        ownerDefinition.table.schemaType,
                )
            }
            val customDefinition = ownerDefinition.table.requiredTable("base")
            val packageName = customDefinition.requiredScalarString("packageName")
            val sourceName = customDefinition.requiredScalarString("srcCodeIdentifier")
            if (packageName.isBlank() || sourceName.isBlank()) {
                fail("function ${semanticId.value} this owner has no qualified source identity")
            }
            return "$packageName.$sourceName"
        }

        private fun customTypeFieldTypes(
            indexedType: OfficialChirV113IndexedTable,
            typeId: UInt,
            actualTypeArguments: List<ChirTypeRef>,
        ): List<ChirTypeRef> {
            val customType = indexedType.table.findNestedTable("CustomType")
                ?: fail("official TYPE#$typeId has no CustomType table")
            val definitionId = customType.requiredReference("customTypeDef", OfficialChirV113TableDomain.DEFINITION)
            val definition = definitionsById[definitionId]
                ?: fail("official TYPE#$typeId references missing DEFINITION#$definitionId")
            val customDefinition = definition.table.requiredTable("base")
            val memberTypes = customDefinition.sequence("instanceMemberVars").mapIndexed { index, memberValue ->
                val member = (memberValue as? OfficialChirV113SemanticValue.Table)?.value
                    ?: fail("official DEFINITION#$definitionId instanceMemberVars[$index] is not a MemberVarInfo")
                val fieldTypeId = member.requiredReference("type", OfficialChirV113TableDomain.TYPE)
                resolveType(fieldTypeId)
            }
            return ChirTypeRelations.instantiateMemberTypes(
                formalTypeParameters = customTypeFormalParameters(definitionId, customDefinition),
                actualTypeArguments = actualTypeArguments,
                memberTypes = memberTypes,
            )
        }

        private fun enumCases(
            indexedType: OfficialChirV113IndexedTable,
            typeId: UInt,
            actualTypeArguments: List<ChirTypeRef>,
        ): List<ChirEnumCaseType> {
            val customType = indexedType.table.findNestedTable("CustomType")
                ?: fail("official TYPE#$typeId has no CustomType table")
            val definitionId = customType.requiredReference("customTypeDef", OfficialChirV113TableDomain.DEFINITION)
            val definition = definitionsById[definitionId]
                ?: fail("official TYPE#$typeId references missing DEFINITION#$definitionId")
            val customDefinition = definition.table.requiredTable("base")
            val formalTypeParameters = customTypeFormalParameters(definitionId, customDefinition)
            val constructors = when (val value = definition.table.fields["ctors"]) {
                null, OfficialChirV113SemanticValue.NullValue -> emptyList()
                is OfficialChirV113SemanticValue.Sequence -> value.values
                else -> fail("official DEFINITION#$definitionId ctors is not a sequence")
            }
            return constructors.mapIndexed { index, constructorValue ->
                val constructor = (constructorValue as? OfficialChirV113SemanticValue.Table)?.value
                    ?: fail("official DEFINITION#$definitionId ctors[$index] is not an EnumCtorInfo")
                val functionTypeId = constructor.requiredReference("funcType", OfficialChirV113TableDomain.TYPE)
                val functionType = resolveType(functionTypeId).typeOrNull() as? ChirFunctionType
                    ?: fail("official DEFINITION#$definitionId ctors[$index] does not reference a function type")
                ChirEnumCaseType(
                    name = constructor.requiredScalarString("identifier"),
                    payloadTypes = ChirTypeRelations.instantiateMemberTypes(
                        formalTypeParameters = formalTypeParameters,
                        actualTypeArguments = actualTypeArguments,
                        memberTypes = functionType.parameterTypes,
                    ),
                )
            }
        }

        private fun enumIsNonExhaustive(indexedType: OfficialChirV113IndexedTable, typeId: UInt): Boolean {
            val customType = indexedType.table.findNestedTable("CustomType")
                ?: fail("official TYPE#$typeId has no CustomType table")
            val definitionId = customType.requiredReference("customTypeDef", OfficialChirV113TableDomain.DEFINITION)
            val definition = definitionsById[definitionId]
                ?: fail("official TYPE#$typeId references missing DEFINITION#$definitionId")
            if (definition.table.schemaType != "EnumDef") {
                fail("official TYPE#$typeId references ${definition.table.schemaType}, expected EnumDef")
            }
            return definition.table.requiredScalarBoolean("nonExhaustive")
        }

        private fun customTypeFormalParameters(
            definitionId: UInt,
            customDefinition: OfficialChirV113SemanticTable,
        ): List<ChirTypeRef> {
            val genericDefinitionId = customDefinition.optionalReference(
                "genericDecl",
                OfficialChirV113TableDomain.DEFINITION,
            )?.takeIf { it != 0u } ?: definitionId

            val genericDefinition = definitionsById[genericDefinitionId]
                ?: fail("official DEFINITION#$definitionId references missing declaration DEFINITION#$genericDefinitionId")
            val genericTypeId = genericDefinition.table.requiredTable("base")
                .requiredReference("type", OfficialChirV113TableDomain.TYPE)
            val genericType = typesById[genericTypeId]
                ?: fail("official generic DEFINITION#$genericDefinitionId references missing TYPE#$genericTypeId")
            val genericTypeBase = genericType.table.findNestedTable("Type")
                ?: fail("official generic TYPE#$genericTypeId has no CHIR Type base table")
            return genericTypeBase.requiredReferences("argTys", OfficialChirV113TableDomain.TYPE)
                .map(::resolveType)
        }

        fun resolvePackageType(typeId: UInt): ChirTypeRef = resolveType(typeId)

        fun resolveFormalTypeParameters(
            definitionId: UInt,
            customDefinition: OfficialChirV113SemanticTable,
        ): List<ChirTypeRef> = customTypeFormalParameters(definitionId, customDefinition)

        fun resolveEnumCaseTypes(
            definition: OfficialChirV113IndexedTable,
            formalTypeParameters: List<ChirTypeRef>,
        ): List<ChirEnumCaseType> = definition.table.sequence("ctors").mapIndexed { index, value ->
            val constructor = (value as? OfficialChirV113SemanticValue.Table)?.value
                ?: fail("official DEFINITION#${definition.id} ctors[$index] is not an EnumCtorInfo")
            val functionTypeId = constructor.requiredReference("funcType", OfficialChirV113TableDomain.TYPE)
            val functionType = resolveType(functionTypeId).typeOrNull() as? ChirFunctionType
                ?: fail("official DEFINITION#${definition.id} ctors[$index] does not reference a function type")
            ChirEnumCaseType(
                name = constructor.requiredScalarString("identifier"),
                payloadTypes = ChirTypeRelations.instantiateMemberTypes(
                    formalTypeParameters = formalTypeParameters,
                    actualTypeArguments = formalTypeParameters,
                    memberTypes = functionType.parameterTypes,
                ),
            )
        }

        fun ownerDefinitionId(): UInt? = function.table.optionalReference(
            "declaredParent",
            OfficialChirV113TableDomain.DEFINITION,
        )?.takeIf { it != 0u }

        private fun requireLastTerminator(isLast: Boolean, expressionId: UInt) {
            if (!isLast) fail("terminator EXPRESSION#$expressionId is not the last block expression")
        }

        private fun fail(message: String): Nothing = throw CjmpOfficialChirImportException(message)

        private fun ChirTypeRef.typeOrNull() = (this as? ChirResolvedTypeRef)?.type

        private fun ChirTypeRef.requireFunctionType(context: String): ChirFunctionType =
            typeOrNull() as? ChirFunctionType ?: fail("$context does not have an official function type")

        private fun valueId(officialId: UInt) =
            semanticIds.idOf(OfficialChirV113TableDomain.VALUE, officialId)

        private fun expressionId(officialId: UInt) =
            semanticIds.idOf(OfficialChirV113TableDomain.EXPRESSION, officialId)
    }

    private companion object {
        val validator = DefaultChirValidator()
        val TERMINATOR_EXPRESSION_TABLES = setOf("GoTo", "Branch", "Exit", "RaiseException")
        val FLOAT_TYPES = setOf(ChirPrimitiveType.FLOAT16, ChirPrimitiveType.FLOAT32, ChirPrimitiveType.FLOAT64)
        val UNSIGNED_INTEGER_TYPES = setOf(
            ChirPrimitiveType.UINT8,
            ChirPrimitiveType.UINT16,
            ChirPrimitiveType.UINT32,
            ChirPrimitiveType.UINT64,
            ChirPrimitiveType.UINT_NATIVE,
        )
        val INTEGER_TYPES = UNSIGNED_INTEGER_TYPES + setOf(
            ChirPrimitiveType.INT8,
            ChirPrimitiveType.INT16,
            ChirPrimitiveType.INT32,
            ChirPrimitiveType.INT64,
            ChirPrimitiveType.INT_NATIVE,
        )
    }
}

private fun OfficialChirV113SemanticTable.valueBase(): OfficialChirV113SemanticTable {
    var value = this
    while (value.schemaType != "Value") {
        value = value.requiredTable("base")
    }
    return value
}

private fun officialValueIdentity(value: OfficialChirV113IndexedTable): String =
    value.table.valueBase().requiredScalarString("identifier")

private fun OfficialChirV113IndexedTable.expressionBase(): OfficialChirV113SemanticTable =
    table.findNestedTable("Expression")
        ?: throw CjmpOfficialChirImportException("Official CHIR ${table.schemaType} has no Expression base table")

private fun OfficialChirV113SemanticTable.findNestedTable(schemaType: String): OfficialChirV113SemanticTable? {
    if (this.schemaType == schemaType) return this
    val matches = buildList {
        fields.values.forEach { value ->
            when (value) {
                is OfficialChirV113SemanticValue.Table -> value.value.findNestedTable(schemaType)?.let(::add)
                is OfficialChirV113SemanticValue.Union -> value.value.findNestedTable(schemaType)?.let(::add)
                is OfficialChirV113SemanticValue.Sequence -> value.values.forEach { nested ->
                    when (nested) {
                        is OfficialChirV113SemanticValue.Table -> nested.value.findNestedTable(schemaType)?.let(::add)
                        is OfficialChirV113SemanticValue.Union -> nested.value.findNestedTable(schemaType)?.let(::add)
                        else -> Unit
                    }
                }
                is OfficialChirV113SemanticValue.Bytes,
                OfficialChirV113SemanticValue.NullValue,
                is OfficialChirV113SemanticValue.Reference,
                is OfficialChirV113SemanticValue.Scalar,
                -> Unit
            }
        }
    }
    return matches.singleOrNull()
}

private fun OfficialChirV113SemanticTable.requiredTable(field: String): OfficialChirV113SemanticTable =
    (fields[field] as? OfficialChirV113SemanticValue.Table)?.value
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not a table")

private fun OfficialChirV113SemanticTable.requiredScalarString(field: String): String =
    (fields[field] as? OfficialChirV113SemanticValue.Scalar)?.value as? String
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not a string")

private fun OfficialChirV113SemanticTable.optionalScalarString(field: String): String? {
    val value = fields[field] ?: return null
    if (value == OfficialChirV113SemanticValue.NullValue) return null
    return (value as? OfficialChirV113SemanticValue.Scalar)?.value as? String
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not an optional string")
}

private fun OfficialChirV113SemanticTable.requiredScalarBoolean(field: String): Boolean =
    (fields[field] as? OfficialChirV113SemanticValue.Scalar)?.value as? Boolean
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not a boolean")

private fun OfficialChirV113SemanticTable.requiredScalarInt(field: String): Int =
    ((fields[field] as? OfficialChirV113SemanticValue.Scalar)?.value as? Number)?.toInt()
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not an integer")

private fun OfficialChirV113SemanticTable.requiredScalarUInt(field: String): UInt {
    val value = (fields[field] as? OfficialChirV113SemanticValue.Scalar)?.value
    return when (value) {
        is UInt -> value
        is Int -> value.toUInt()
        is ULong -> value.takeIf { it <= UInt.MAX_VALUE.toULong() }?.toUInt()
        is Long -> value.takeIf { it in 0..UInt.MAX_VALUE.toLong() }?.toUInt()
        is Number -> value.toLong().takeIf { it in 0..UInt.MAX_VALUE.toLong() }?.toUInt()
        else -> null
    } ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not a uint32")
}

private fun OfficialChirV113SemanticTable.requiredScalarLong(field: String): Long {
    val value = (fields[field] as? OfficialChirV113SemanticValue.Scalar)?.value
    return when (value) {
        is Number -> value.toLong()
        is ULong -> value.toLong()
        else -> throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not a long integer")
    }
}

private fun OfficialChirV113SemanticTable.requiredScalarDouble(field: String): Double =
    ((fields[field] as? OfficialChirV113SemanticValue.Scalar)?.value as? Number)?.toDouble()
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not a double")

private fun OfficialChirV113SemanticTable.requiredReference(
    field: String,
    domain: OfficialChirV113TableDomain,
): UInt = optionalReference(field, domain)
    ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field has no $domain reference")

private fun OfficialChirV113SemanticTable.optionalReference(
    field: String,
    domain: OfficialChirV113TableDomain,
): UInt? {
    val value = fields[field] ?: return null
    if (value == OfficialChirV113SemanticValue.NullValue) return null
    val reference = value as? OfficialChirV113SemanticValue.Reference
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not a $domain reference")
    if (reference.domain != domain || reference.id < 0 || reference.id > UInt.MAX_VALUE.toLong()) {
        throw CjmpOfficialChirImportException(
            "Official CHIR $schemaType.$field has invalid ${reference.domain} reference ${reference.id}",
        )
    }
    return reference.id.toUInt()
}

private fun OfficialChirV113SemanticTable.requiredReferences(
    field: String,
    domain: OfficialChirV113TableDomain,
): List<UInt> = sequence(field).mapIndexed { index, value ->
    val reference = value as? OfficialChirV113SemanticValue.Reference
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field[$index] is not a reference")
    if (reference.domain != domain || reference.id < 0 || reference.id > UInt.MAX_VALUE.toLong()) {
        throw CjmpOfficialChirImportException(
            "Official CHIR $schemaType.$field[$index] has invalid ${reference.domain} reference ${reference.id}",
        )
    }
    reference.id.toUInt()
}

private fun OfficialChirV113SemanticTable.requiredUnsignedLongs(field: String): List<ULong> =
    sequence(field).mapIndexed { index, value ->
        val scalar = (value as? OfficialChirV113SemanticValue.Scalar)?.value
        when (scalar) {
            is ULong -> scalar
            is UInt -> scalar.toULong()
            is UShort -> scalar.toULong()
            is UByte -> scalar.toULong()
            is Long -> scalar.takeIf { it >= 0L }?.toULong()
            is Int -> scalar.takeIf { it >= 0 }?.toULong()
            is Short -> scalar.takeIf { it >= 0 }?.toULong()
            is Byte -> scalar.takeIf { it >= 0 }?.toULong()
            else -> null
        } ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field[$index] is not an unsigned integer")
    }

private fun OfficialChirV113SemanticTable.requiredStrings(field: String): List<String> =
    sequence(field).mapIndexed { index, value ->
        (value as? OfficialChirV113SemanticValue.Scalar)?.value as? String
            ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field[$index] is not a string")
    }

private fun OfficialChirV113SemanticTable.sequence(field: String): List<OfficialChirV113SemanticValue> =
    (fields[field] as? OfficialChirV113SemanticValue.Sequence)?.values
        ?: throw CjmpOfficialChirImportException("Official CHIR $schemaType.$field is not a sequence")

private const val OFFICIAL_CHIR_ATTRIBUTE_BITS = "official.chir.attribute-bits"

/** 官方 v1.1.3 CHIR 属性位的语义定义。 */
enum class OfficialChirV113Attribute(val bitIndex: Int) {
    STATIC(0);

    val mask: ULong
        get() = 1uL shl bitIndex
}

private fun OfficialChirV113SemanticTable.attributeBits(): ULong {
    val scalar = (fields["attributes"] as? OfficialChirV113SemanticValue.Scalar)?.value ?: return 0uL
    return when (scalar) {
        is ULong -> scalar
        is UInt -> scalar.toULong()
        is UShort -> scalar.toULong()
        is UByte -> scalar.toULong()
        is Long -> scalar.toULong()
        is Int -> scalar.toUInt().toULong()
        is Short -> scalar.toUShort().toULong()
        is Byte -> scalar.toUByte().toULong()
        else -> throw CjmpOfficialChirImportException("Official CHIR $schemaType.attributes is not an unsigned bitset")
    }
}

private fun OfficialChirV113SemanticTable.hasAttribute(bitIndex: Int): Boolean =
    attributeBits() and (1uL shl bitIndex) != 0uL

private fun OfficialChirV113SemanticTable.hasAttribute(attribute: OfficialChirV113Attribute): Boolean =
    attributeBits() and attribute.mask != 0uL

private fun OfficialChirV113SemanticTable.toChirAttributes(): Set<ChirAttribute> {
    val bits = attributeBits()
    if (bits == 0uL) return emptySet()
    return setOf(ChirStringAttribute(OFFICIAL_CHIR_ATTRIBUTE_BITS, bits.toString()))
}

private fun ChirTypeRef.typeOrNull() = (this as? ChirResolvedTypeRef)?.type

/** 官方 CHIR 输入无法无损映射为仓内 CHIR 时抛出。 */
class OfficialChirV113ImportException(message: String) : IllegalArgumentException(message)

typealias CjmpOfficialChirImportException = OfficialChirV113ImportException
