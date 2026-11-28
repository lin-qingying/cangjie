package org.cangnova.cangjie.chir.cjmp

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirNominalTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirPropertyDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageMembers
import org.cangnova.cangjie.chir.core.model.allDeclarations
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeRelations
import org.cangnova.cangjie.chir.core.transform.ChirTypeSubstitutor
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue

/** CHIR 包 actualization 对 package 边界公开的契约。 */
interface CjmpChirPackageActualizer {
    /**
     * 保留既有调用契约，按共享的稳定身份 actualize 两侧 CHIR 包。
     */
    fun actualize(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        actualizedIdentities: Set<ChirSemanticId>,
    ): ChirPackage

    /** 按 CFIR matcher 冻结的声明和类型参数关系 actualize 两侧 CHIR 包。 */
    fun actualize(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        plan: CjmpChirActualizationPlan,
    ): ChirPackage = actualize(
        commonPackage,
        specificPackage,
        plan.asLegacyActualizedIdentities(),
    )
}

/**
 * 将两个仓内 CHIR 包 actualize 成单一不可变包。
 *
 * common 声明保持原顺序；matcher 确认的身份由 specific 声明替换，其余 specific-only 声明追加。
 * 不按名字推断身份，也不发布校验失败或部分合并的包。
 */
class DefaultCjmpChirPackageActualizer : CjmpChirPackageActualizer {
    override fun actualize(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        plan: CjmpChirActualizationPlan,
    ): ChirPackage {
        requireCompatiblePackages(commonPackage, specificPackage)
        validateInput("common", commonPackage)
        validateInput("specific", specificPackage)
        val context = createActualizationContext(commonPackage, specificPackage, plan)
        val crossContainerTypeActualizations = findCrossContainerTypeActualizations(
            commonPackage,
            specificPackage,
            context,
        )
        val movedSpecificTypeIds = crossContainerTypeActualizations.mapTo(hashSetOf()) { it.second.semanticId }
        val typeActualizationContext = context

        val importedFunctionActualizations = actualizeImportedFunctionsImplementedBySpecificModules(
            commonPackage = commonPackage,
            specificPackage = specificPackage,
            context = typeActualizationContext,
        )
        val modules = mergeModules(
            commonPackage.modules,
            specificPackage.modules,
            typeActualizationContext,
            movedSpecificTypeIds,
        )
            .map { module ->
                module.copy(
                    declarations = module.declarations.map { declaration ->
                        importedFunctionActualizations.replacements[declaration.semanticId] ?: declaration
                    },
                )
            }
        val members = mergeMembers(
            common = commonPackage.members,
            specific = specificPackage.members,
            context = typeActualizationContext,
            replacedImportedFunctionIds = importedFunctionActualizations.removedCommonIds,
        )
        val typeDefinitions = mergeTypedDeclarations(
            commonPackage.typeDefinitions,
            specificPackage.typeDefinitions + crossContainerTypeActualizations.map { it.second },
            typeActualizationContext,
            "type definitions",
        )
        val importedTypeDefinitions = mergeTypedDeclarations(
            commonPackage.importedTypeDefinitions,
            specificPackage.importedTypeDefinitions,
            typeActualizationContext,
            "imported type definitions",
        )

        val preInitializationPackage = commonPackage.copy(
            modules = modules,
            members = members,
            typeDefinitions = typeDefinitions,
            importedTypeDefinitions = importedTypeDefinitions,
            packageInitFunctionId = null,
            packageLiteralInitFunctionId = null,
        )
        val declarationsById = preInitializationPackage.allDeclarations.associateBy { it.semanticId }
        val packageInit = mergeInitializer(
            kind = "package-init",
            commonId = commonPackage.packageInitFunctionId,
            specificId = specificPackage.packageInitFunctionId,
            declarationsById = declarationsById,
        )
        val literalInit = mergeInitializer(
            kind = "package-literal-init",
            commonId = commonPackage.packageLiteralInitFunctionId,
            specificId = specificPackage.packageLiteralInitFunctionId,
            declarationsById = declarationsById,
        )
        val generatedInitializers = listOfNotNull(packageInit.generatedFunction, literalInit.generatedFunction)
        val modulesWithInitializers = appendGeneratedInitializers(preInitializationPackage.modules, generatedInitializers)

        val result = preInitializationPackage.copy(
            modules = modulesWithInitializers,
            packageInitFunctionId = packageInit.functionId,
            packageLiteralInitFunctionId = literalInit.functionId,
        )
        validateOutput(result)
        return result
    }

    /** 将旧 set 调用按包内实际声明解析为精确关系，避免把两侧身份集合当作配对授权。 */
    override fun actualize(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        actualizedIdentities: Set<ChirSemanticId>,
    ): ChirPackage = actualize(
        commonPackage,
        specificPackage,
        legacyActualizationPlan(commonPackage, specificPackage, actualizedIdentities),
    )

    private fun legacyActualizationPlan(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        actualizedIdentities: Set<ChirSemanticId>,
    ): CjmpChirActualizationPlan {
        val commonDeclarations = commonPackage.allDeclarations
        val specificDeclarations = specificPackage.allDeclarations
        val commonTypes = commonDeclarations.filterIsInstance<ChirNominalTypeDeclaration>()
        val specificTypes = specificDeclarations.filterIsInstance<ChirNominalTypeDeclaration>()
        val pairs = linkedSetOf<CjmpChirDeclarationPair>()

        fun declarationFor(
            declarations: List<ChirDeclaration>,
            identity: ChirSemanticId,
            role: String,
        ): ChirDeclaration? {
            val matches = declarations.filter { it.semanticId == identity }
            if (matches.size > 1) {
                throw CjmpChirPackageActualizationException(
                    "Legacy matcher identity $identity identifies multiple $role CHIR declarations",
                )
            }
            return matches.singleOrNull()
        }

        fun nominalCounterparts(
            declaration: ChirNominalTypeDeclaration,
            candidates: List<ChirNominalTypeDeclaration>,
        ): List<ChirNominalTypeDeclaration> = candidates.filter {
            it.typeDeclarationId == declaration.typeDeclarationId && it.definitionKind == declaration.definitionKind
        }

        actualizedIdentities.forEach { identity ->
            val common = declarationFor(commonDeclarations, identity, "common")
            val specific = declarationFor(specificDeclarations, identity, "specific")
            when {
                common != null && specific != null -> pairs += CjmpChirDeclarationPair(identity, identity)
                common is ChirNominalTypeDeclaration -> {
                    val counterparts = nominalCounterparts(common, specificTypes)
                    val counterpart = counterparts.singleOrNull()
                        ?: throw CjmpChirPackageActualizationException(
                            "Legacy matcher identity $identity has ${counterparts.size} specific nominal counterparts",
                        )
                    pairs += CjmpChirDeclarationPair(common.semanticId, counterpart.semanticId)
                }

                specific is ChirNominalTypeDeclaration -> {
                    val counterparts = nominalCounterparts(specific, commonTypes)
                    val counterpart = counterparts.singleOrNull()
                        ?: throw CjmpChirPackageActualizationException(
                            "Legacy matcher identity $identity has ${counterparts.size} common nominal counterparts",
                        )
                    pairs += CjmpChirDeclarationPair(counterpart.semanticId, specific.semanticId)
                }

                else -> throw CjmpChirPackageActualizationException(
                    "Legacy matcher identity $identity is absent from both packages or has no nominal counterpart",
                )
            }
        }
        return CjmpChirActualizationPlan(pairs)
    }

    private fun requireCompatiblePackages(common: ChirPackage, specific: ChirPackage) {
        if (common.semanticId != specific.semanticId || common.name != specific.name) {
            throw CjmpChirPackageActualizationException(
                "CJMP actualization requires the same package identity: common=${common.name}/${common.semanticId}, " +
                    "specific=${specific.name}/${specific.semanticId}",
            )
        }
        if (common.accessLevel != specific.accessLevel) {
            throw CjmpChirPackageActualizationException(
                "CJMP package access level differs for ${common.name}: ${common.accessLevel} vs ${specific.accessLevel}",
            )
        }
    }

    private fun validateInput(role: String, chirPackage: ChirPackage) {
        val report = validator.validatePackage(chirPackage)
        if (report.hasErrors) {
            throw CjmpChirPackageActualizationException(
                "Cannot actualize invalid $role CHIR package ${chirPackage.name}: " +
                    ChirValidationReportFormatter.render(report),
            )
        }
    }

    private fun validateOutput(chirPackage: ChirPackage) {
        val report = validator.validatePackage(chirPackage)
        if (report.hasErrors) {
            throw CjmpChirPackageActualizationException(
                "CJMP actualization produced an invalid CHIR package ${chirPackage.name}: " +
                    ChirValidationReportFormatter.render(report),
            )
        }
    }

    private fun createActualizationContext(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        plan: CjmpChirActualizationPlan,
    ): CjmpChirActualizationContext {
        val commonDeclarations = commonPackage.allDeclarations
        val specificDeclarations = specificPackage.allDeclarations
        plan.declarationPairs.forEach { pair ->
            val presentAsExactPair = pair.commonIdentity == pair.specificIdentity &&
                commonDeclarations.any { it.semanticId == pair.commonIdentity } &&
                specificDeclarations.any { it.semanticId == pair.specificIdentity }
            val presentAsNominalPair = hasNominalTypeCounterpart(
                commonPackage,
                specificPackage,
                pair.commonIdentity,
                pair.specificIdentity,
            )
            if (!presentAsExactPair && !presentAsNominalPair) {
                throw CjmpChirPackageActualizationException(
                    "Matcher-approved CHIR declaration pair ${pair.commonIdentity} -> ${pair.specificIdentity} " +
                        "must occur in common and specific packages",
                )
            }
        }

        val genericIdentityMappings = linkedMapOf<String, String>()
        val genericTypeSubstitutions = linkedMapOf<String, ChirTypeRef>()
        val reverseGenericIdentityMappings = linkedMapOf<String, String>()
        plan.typeParameterPairs.forEach { pair ->
            val ownerPair = CjmpChirDeclarationPair(pair.commonOwnerIdentity, pair.specificOwnerIdentity)
            if (ownerPair !in plan.declarationPairs) {
                throw CjmpChirPackageActualizationException(
                    "Matcher-approved CHIR type parameter mapping references an unapproved owner pair " +
                        "${pair.commonOwnerIdentity} -> ${pair.specificOwnerIdentity}",
                )
            }
            val commonOwner = declarationByIdentity("common", commonPackage, pair.commonOwnerIdentity)
            val specificOwner = declarationByIdentity("specific", specificPackage, pair.specificOwnerIdentity)
            val commonGeneric = genericTypeParameter(commonOwner, pair.commonIndex, "common")
            val specificGeneric = genericTypeParameter(specificOwner, pair.specificIndex, "specific")
            val specificGenericType = ChirResolvedTypeRef(specificGeneric)
            val previousSpecific = genericIdentityMappings.putIfAbsent(
                commonGeneric.semanticIdentity,
                specificGeneric.semanticIdentity,
            )
            if (previousSpecific != null && previousSpecific != specificGeneric.semanticIdentity) {
                throw CjmpChirPackageActualizationException(
                    "Matcher-approved common generic identity ${commonGeneric.semanticIdentity} maps to multiple " +
                        "specific generic identities",
                )
            }
            val previousTypeSubstitution = genericTypeSubstitutions.putIfAbsent(
                commonGeneric.semanticIdentity,
                specificGenericType,
            )
            if (previousTypeSubstitution != null && previousTypeSubstitution != specificGenericType) {
                throw CjmpChirPackageActualizationException(
                    "Matcher-approved common generic identity ${commonGeneric.semanticIdentity} maps to multiple " +
                        "specific generic types",
                )
            }
            val previousCommon = reverseGenericIdentityMappings.putIfAbsent(
                specificGeneric.semanticIdentity,
                commonGeneric.semanticIdentity,
            )
            if (previousCommon != null && previousCommon != commonGeneric.semanticIdentity) {
                throw CjmpChirPackageActualizationException(
                    "Matcher-approved specific generic identity ${specificGeneric.semanticIdentity} maps from " +
                        "multiple common generic identities",
                )
            }
        }
        return CjmpChirActualizationContext(
            declarationPairs = plan.declarationPairs,
            genericIdentityMappings = genericIdentityMappings,
            genericTypeSubstitutions = genericTypeSubstitutions,
        )
    }

    private fun hasNominalTypeCounterpart(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        commonIdentity: ChirSemanticId,
        specificIdentity: ChirSemanticId,
    ): Boolean {
        val commonType = commonPackage.allDeclarations.filterIsInstance<ChirNominalTypeDeclaration>()
            .singleOrNull { it.semanticId == commonIdentity }
        val specificType = specificPackage.allDeclarations.filterIsInstance<ChirNominalTypeDeclaration>()
            .singleOrNull { it.semanticId == specificIdentity }
        return commonType != null && specificType != null &&
            commonType.typeDeclarationId == specificType.typeDeclarationId &&
            commonType.definitionKind == specificType.definitionKind
    }

    private fun declarationByIdentity(
        role: String,
        chirPackage: ChirPackage,
        identity: ChirSemanticId,
    ): ChirDeclaration {
        val declarations = chirPackage.allDeclarations.filter { it.semanticId == identity }
        return declarations.singleOrNull() ?: throw CjmpChirPackageActualizationException(
            "Matcher-approved $role generic owner $identity must identify exactly one CHIR declaration; " +
                "found ${declarations.size}",
        )
    }

    private fun genericTypeParameter(owner: ChirDeclaration, index: Int, role: String): ChirGenericType {
        val parameters = when (owner) {
            is ChirFunctionDeclaration -> owner.genericTypeParameters
            is ChirTypeDeclaration -> owner.typeParameterTypes
            else -> throw CjmpChirPackageActualizationException(
                "Matcher-approved $role generic owner ${owner.semanticId} has no type parameter list",
            )
        }
        val reference = parameters.getOrNull(index)
            ?: throw CjmpChirPackageActualizationException(
                "Matcher-approved $role generic owner ${owner.semanticId} has no type parameter at index $index",
            )
        val generic = (reference as? ChirResolvedTypeRef)?.type as? ChirGenericType
            ?: throw CjmpChirPackageActualizationException(
                "Matcher-approved $role type parameter ${owner.semanticId}#$index is not a resolved generic CHIR type",
            )
        if (generic.semanticIdentity.isBlank()) {
            throw CjmpChirPackageActualizationException(
                "Matcher-approved $role type parameter ${owner.semanticId}#$index has no semantic identity",
            )
        }
        return generic
    }

    /** 找出 official package typeDefinitions 与 specific module 根声明之间的 matcher 批准配对。 */
    private fun findCrossContainerTypeActualizations(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        context: CjmpChirActualizationContext,
    ): List<Pair<ChirNominalTypeDeclaration, ChirNominalTypeDeclaration>> {
        val commonTypesByTypeId = commonPackage.typeDefinitions
            .filterIsInstance<ChirNominalTypeDeclaration>()
            .groupBy(ChirNominalTypeDeclaration::typeDeclarationId)
        val specificModuleTypes = specificPackage.modules
            .flatMap(ChirModule::declarations)
            .filterIsInstance<ChirNominalTypeDeclaration>()
        return buildList {
            specificModuleTypes.forEach { specificType ->
                val commonTypes = commonTypesByTypeId[specificType.typeDeclarationId].orEmpty()
                if (commonTypes.isEmpty()) return@forEach
                if (commonTypes.size != 1) {
                    throw CjmpChirPackageActualizationException(
                        "Nominal type ${specificType.typeDeclarationId.qualifiedName} has multiple common package definitions",
                    )
                }
                val commonType = commonTypes.single()
                if (commonType.definitionKind != specificType.definitionKind) {
                    throw CjmpChirPackageActualizationException(
                        "Nominal type ${specificType.typeDeclarationId.qualifiedName} changes definition kind across packages",
                    )
                }
                if (!context.isApprovedPair(commonType.semanticId, specificType.semanticId)) {
                    throw CjmpChirPackageActualizationException(
                        "Nominal type ${specificType.typeDeclarationId.qualifiedName} has no CFIR matcher relation",
                    )
                }
                add(commonType to specificType)
            }
        }
    }

    private fun mergeModules(
        common: List<ChirModule>,
        specific: List<ChirModule>,
        context: CjmpChirActualizationContext,
        movedSpecificTypeIds: Set<ChirSemanticId>,
    ): List<ChirModule> {
        val commonById = uniqueModulesBySemanticId(common, "common modules")
        val specificById = uniqueModulesBySemanticId(specific, "specific modules")
        val commonDeclarationLocations = common.flatMap { module ->
            module.declarations.map { declaration -> declaration.semanticId to (module to declaration) }
        }.toMap()
        val specificDeclarationLocations = specific.flatMap { module ->
            module.declarations.map { declaration -> declaration.semanticId to (module to declaration) }
        }.toMap()
        val crossModuleActualizations = context.declarationPairs.mapNotNull { pair ->
            if (pair.commonIdentity != pair.specificIdentity) return@mapNotNull null
            val (commonModule, commonDeclaration) = commonDeclarationLocations[pair.commonIdentity]
                ?: return@mapNotNull null
            val (specificModule, specificDeclaration) = specificDeclarationLocations[pair.specificIdentity]
                ?: return@mapNotNull null
            if (commonModule.semanticId == specificModule.semanticId) return@mapNotNull null
            specificDeclaration.semanticId to mergeDeclaration(
                common = commonDeclaration,
                specific = specificDeclaration,
                context = context,
                location = "cross-module actualization",
            )
        }.toMap()
        return buildList {
            common.forEach { commonModule ->
                val specificModule = specificById[commonModule.semanticId]
                val declarations = if (specificModule == null) {
                    commonModule.declarations.map(context::substitute)
                } else {
                    if (commonModule.name != specificModule.name) {
                        throw CjmpChirPackageActualizationException(
                            "CHIR module identity ${commonModule.semanticId} has conflicting names " +
                                "${commonModule.name} and ${specificModule.name}",
                        )
                    }
                    mergeDeclarations(
                        commonModule.declarations,
                        specificModule.declarations.filterNot { it.semanticId in movedSpecificTypeIds }.map { declaration ->
                            crossModuleActualizations[declaration.semanticId] ?: declaration
                        },
                        context,
                        "module ${commonModule.name}",
                    )
                }
                val retainedDeclarations = declarations.filterNot { declaration ->
                    declaration.semanticId in crossModuleActualizations &&
                        specificDeclarationLocations[declaration.semanticId]?.first?.semanticId != commonModule.semanticId
                }
                add(commonModule.copy(declarations = retainedDeclarations))
            }
            specific.forEach { specificModule ->
                if (specificModule.semanticId !in commonById) {
                    add(
                        specificModule.copy(
                            declarations = specificModule.declarations.filterNot { it.semanticId in movedSpecificTypeIds }.map { declaration ->
                                crossModuleActualizations[declaration.semanticId] ?: declaration
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun mergeMembers(
        common: ChirPackageMembers,
        specific: ChirPackageMembers,
        context: CjmpChirActualizationContext,
        replacedImportedFunctionIds: Set<ChirSemanticId>,
    ): ChirPackageMembers {
        val variables = mergeVariableMembers(common, specific, context)
        return ChirPackageMembers(
            globalVariables = variables.globalVariables,
            globalFunctions = mergeTypedDeclarations(
                common.globalFunctions,
                specific.globalFunctions,
                context,
                "global functions",
            ),
            importedVariables = variables.importedVariables,
            importedFunctions = mergeTypedDeclarations(
                common.importedFunctions.filterNot { it.semanticId in replacedImportedFunctionIds },
                specific.importedFunctions,
                context,
                "imported functions",
            ),
        )
    }

    /**
     * Package globals and imported globals are two storage roles of one declaration family.
     * Actualization must therefore resolve identities across both lists before partitioning them
     * by the specific package's selected storage role.
     */
    private fun mergeVariableMembers(
        common: ChirPackageMembers,
        specific: ChirPackageMembers,
        context: CjmpChirActualizationContext,
    ): MergedVariableMembers {
        val commonEntries = common.globalVariables.map { VariableMember(it, imported = false) } +
            common.importedVariables.map { VariableMember(it, imported = true) }
        val specificEntries = specific.globalVariables.map { VariableMember(it, imported = false) } +
            specific.importedVariables.map { VariableMember(it, imported = true) }
        val commonById = uniqueBySemanticId(commonEntries.map { it.declaration }, "common package variables").keys
        val specificById = uniqueBySemanticId(specificEntries.map { it.declaration }, "specific package variables").keys
        val specificEntryById = specificEntries.associateBy { it.declaration.semanticId }

        val merged = buildList {
            commonEntries.forEach { commonEntry ->
                val specificEntry = specificEntryById[commonEntry.declaration.semanticId]
                if (specificEntry == null) {
                    add(
                        VariableMember(
                            declaration = context.substitute(commonEntry.declaration) as ChirVariableDeclaration,
                            imported = commonEntry.imported,
                        ),
                    )
                } else {
                    if (!context.isApprovedPair(
                            commonEntry.declaration.semanticId,
                            specificEntry.declaration.semanticId,
                        )
                    ) {
                        throw CjmpChirPackageActualizationException(
                            "CHIR variable identity collision across package storage roles without a CFIR matcher relation: " +
                                commonEntry.declaration.semanticId,
                        )
                    }
                    val declaration = mergeDeclaration(
                        commonEntry.declaration,
                        specificEntry.declaration,
                        context,
                        "package variables",
                    ) as? ChirVariableDeclaration ?: throw CjmpChirPackageActualizationException(
                        "Matched package variable ${commonEntry.declaration.semanticId} changed declaration kind",
                    )
                    add(VariableMember(declaration, imported = specificEntry.imported))
                }
            }
            specificEntries.forEach { specificEntry ->
                if (specificEntry.declaration.semanticId !in commonById) add(specificEntry)
            }
        }
        return MergedVariableMembers(
            globalVariables = merged.filterNot { it.imported }.map { it.declaration },
            importedVariables = merged.filter { it.imported }.map { it.declaration },
        )
    }

    private fun actualizeImportedFunctionsImplementedBySpecificModules(
        commonPackage: ChirPackage,
        specificPackage: ChirPackage,
        context: CjmpChirActualizationContext,
    ): ImportedFunctionActualizations {
        val specificModuleFunctions = specificPackage.modules
            .flatMap { module -> module.declarations }
            .filterIsInstance<ChirFunctionDeclaration>()
            .associateBy { it.semanticId }
        val replacements = linkedMapOf<ChirSemanticId, ChirDeclaration>()
        commonPackage.members.importedFunctions.forEach { commonFunction ->
            if (!context.isApprovedPair(commonFunction.semanticId, commonFunction.semanticId)) return@forEach
            val specificFunction = specificModuleFunctions[commonFunction.semanticId] ?: return@forEach
            if (!specificFunction.hasBody || specificFunction.isImported) {
                throw CjmpChirPackageActualizationException(
                    "Specific module function ${specificFunction.semanticId} cannot replace an imported common declaration without a body",
                )
            }
            replacements[commonFunction.semanticId] = actualizeFunction(
                common = commonFunction,
                specific = specificFunction,
                context = context,
                location = "common imported function to specific module",
            )
        }
        return ImportedFunctionActualizations(
            replacements = replacements,
            removedCommonIds = replacements.keys,
        )
    }

    private fun <T : ChirDeclaration> mergeTypedDeclarations(
        common: List<T>,
        specific: List<T>,
        context: CjmpChirActualizationContext,
        location: String,
    ): List<T> = mergeDeclarations(common, specific, context, location).map { declaration ->
        @Suppress("UNCHECKED_CAST")
        (declaration as? T) ?: throw CjmpChirPackageActualizationException(
            "CHIR declaration kind changed in $location for ${declaration.semanticId}",
        )
    }

    private fun mergeDeclarations(
        common: List<ChirDeclaration>,
        specific: List<ChirDeclaration>,
        context: CjmpChirActualizationContext,
        location: String,
    ): List<ChirDeclaration> {
        val commonById = uniqueBySemanticId(common, "common $location")
        val specificById = uniqueBySemanticId(specific, "specific $location")
        val specificTypesByTypeId = specific.filterIsInstance<ChirNominalTypeDeclaration>()
            .groupBy(ChirNominalTypeDeclaration::typeDeclarationId)
        val consumedSpecificIds = hashSetOf<ChirSemanticId>()
        return buildList {
            common.forEach { commonDeclaration ->
                val approvedNominalCounterparts = (commonDeclaration as? ChirNominalTypeDeclaration)
                    ?.let { commonType ->
                        specificTypesByTypeId[commonType.typeDeclarationId].orEmpty().filter { specificType ->
                            context.isApprovedPair(commonType.semanticId, specificType.semanticId)
                        }
                    }.orEmpty()
                if (specificById[commonDeclaration.semanticId] == null && approvedNominalCounterparts.size > 1) {
                    throw CjmpChirPackageActualizationException(
                        "Multiple matcher-approved nominal declarations correspond to ${commonDeclaration.semanticId} " +
                            "in $location",
                    )
                }
                val specificDeclaration = specificById[commonDeclaration.semanticId]
                    ?: approvedNominalCounterparts.singleOrNull()
                if (specificDeclaration == null) {
                    add(context.substitute(commonDeclaration))
                } else {
                    if (!context.isApprovedPair(commonDeclaration.semanticId, specificDeclaration.semanticId)) {
                        throw CjmpChirPackageActualizationException(
                            "CHIR declaration identity collision in $location without a CFIR matcher relation: " +
                                commonDeclaration.semanticId,
                        )
                    }
                    add(mergeDeclaration(commonDeclaration, specificDeclaration, context, location))
                    consumedSpecificIds += specificDeclaration.semanticId
                }
            }
            specific.forEach { declaration ->
                if (declaration.semanticId !in commonById && declaration.semanticId !in consumedSpecificIds) add(declaration)
            }
        }
    }

    private fun mergeDeclaration(
        common: ChirDeclaration,
        specific: ChirDeclaration,
        context: CjmpChirActualizationContext,
        location: String,
    ): ChirDeclaration {
        if (common.name != specific.name) {
            throw CjmpChirPackageActualizationException(
                "Matched CHIR identity ${common.semanticId} has different declaration names in $location: " +
                    "${common.name} and ${specific.name}",
            )
        }
        return when {
            common is ChirFunctionDeclaration && specific is ChirFunctionDeclaration ->
                actualizeFunction(common, specific, context, location)

            common is ChirStructDeclaration && specific is ChirStructDeclaration ->
                actualizeStruct(common, specific, context, location)

            common is ChirClassDeclaration && specific is ChirClassDeclaration ->
                actualizeClass(common, specific, context, location)

            common is ChirEnumDeclaration && specific is ChirEnumDeclaration ->
                actualizeEnum(common, specific, context, location)

            common is ChirExtendDeclaration && specific is ChirExtendDeclaration ->
                actualizeExtend(common, specific, context, location)

            common is ChirTypeDeclaration && specific is ChirTypeDeclaration ->
                actualizePlainType(common, specific, context, location)

            common is ChirVariableDeclaration && specific is ChirVariableDeclaration ->
                actualizeVariable(common, specific, context, location)

            else -> throw CjmpChirPackageActualizationException(
                "Matched CHIR identity ${common.semanticId} changes declaration kind in $location: " +
                    "${common::class.simpleName} vs ${specific::class.simpleName}",
            )
        }
    }

    private fun actualizeFunction(
        common: ChirFunctionDeclaration,
        specific: ChirFunctionDeclaration,
        context: CjmpChirActualizationContext,
        location: String,
    ): ChirFunctionDeclaration {
        val signaturesMatch = common::class == specific::class &&
            context.areEquivalent(common.returnType, specific.returnType) &&
            common.parameters.size == specific.parameters.size &&
            common.genericTypeParameters.size == specific.genericTypeParameters.size &&
            common.genericTypeParameters.zip(specific.genericTypeParameters).all { (commonParameter, specificParameter) ->
                context.areEquivalent(commonParameter, specificParameter)
            } &&
            common.parameters.zip(specific.parameters).all { (commonParameter, specificParameter) ->
                commonParameter::class == specificParameter::class &&
                    context.areEquivalent(commonParameter.type, specificParameter.type) &&
                    commonParameter.mutable == specificParameter.mutable
            }
        if (!signaturesMatch) {
            throw CjmpChirPackageActualizationException(
                "Matched CHIR function ${common.semanticId} has incompatible signatures in $location",
            )
        }
        val commonFunction = common as? DefaultChirFunctionDeclaration
            ?: throw CjmpChirPackageActualizationException(
                "Matched CHIR function ${common.semanticId} uses an unsupported declaration implementation",
            )
        val specificFunction = specific as? DefaultChirFunctionDeclaration
            ?: throw CjmpChirPackageActualizationException(
                "Matched CHIR function ${specific.semanticId} uses an unsupported declaration implementation",
            )
        val bodySource = when {
            specificFunction.hasBody -> specificFunction
            commonFunction.hasBody -> commonFunction
            else -> specificFunction
        }
        val usesCommonBody = !specificFunction.hasBody && commonFunction.hasBody
        return specificFunction.copy(
            parameters = commonFunction.parameters.zip(specificFunction.parameters).map { (commonParameter, specificParameter) ->
                actualizeVariable(commonParameter, specificParameter, context, "parameters of ${specific.name}")
            },
            blocks = if (usesCommonBody) context.substitute(bodySource.blocks) else bodySource.blocks,
            entryBlockId = bodySource.entryBlockId,
            hasBody = bodySource.hasBody,
            isImported = bodySource.isImported,
            genericTypeParameters = if (usesCommonBody) {
                bodySource.genericTypeParameters.map(context::substitute)
            } else {
                bodySource.genericTypeParameters
            },
            attributes = mergeAttributes(commonFunction.attributes, specificFunction.attributes),
        )
    }

    private fun actualizeVariable(
        common: ChirVariableDeclaration,
        specific: ChirVariableDeclaration,
        context: CjmpChirActualizationContext,
        location: String,
    ): ChirVariableDeclaration {
        if (common::class != specific::class ||
            !context.areEquivalent(common.type, specific.type) ||
            common.mutable != specific.mutable
        ) {
            throw CjmpChirPackageActualizationException(
                "Matched CHIR variable ${common.semanticId} has incompatible shape in $location",
            )
        }
        val mergedAttributes = mergeAttributes(common.attributes, specific.attributes)
        return when (specific) {
            is DefaultChirVariableDeclaration -> specific.copy(attributes = mergedAttributes)
            is DefaultChirPropertyDeclaration -> specific.copy(attributes = mergedAttributes)
            else -> throw CjmpChirPackageActualizationException(
                "Matched CHIR variable ${specific.semanticId} uses an unsupported declaration implementation",
            )
        }
    }

    private fun actualizeClass(
        common: ChirClassDeclaration,
        specific: ChirClassDeclaration,
        context: CjmpChirActualizationContext,
        location: String,
    ): ChirClassDeclaration {
        if (common::class != specific::class ||
            common.typeParameters.size != specific.typeParameters.size ||
            !context.sameTypeRefs(common.typeParameterTypes, specific.typeParameterTypes) ||
            common.typeDeclarationId != specific.typeDeclarationId ||
            common.isInterface != specific.isInterface ||
            !context.sameTypeRefs(common.superTypes, specific.superTypes)
        ) {
            throw CjmpChirPackageActualizationException(
                "Matched CHIR class ${common.semanticId} has incompatible inheritance or generic shape in $location",
            )
        }
        return DefaultChirClassDeclaration(
            semanticId = specific.semanticId,
            name = specific.name,
            typeDeclarationId = specific.typeDeclarationId,
            typeParameters = specific.typeParameters,
            typeParameterTypes = specific.typeParameterTypes,
            superTypes = specific.superTypes,
            implementedTypes = context.mergeTypeRefs(
                common.implementedTypes.map(context::substitute),
                specific.implementedTypes,
            ),
            memberDeclarations = mergeDeclarations(
                common.memberDeclarations,
                specific.memberDeclarations,
                context,
                "members of ${specific.name}",
            ),
            attributes = mergeAttributes(common.attributes, specific.attributes),
            isInterface = specific.isInterface,
        )
    }

    private fun actualizeStruct(
        common: ChirStructDeclaration,
        specific: ChirStructDeclaration,
        context: CjmpChirActualizationContext,
        location: String,
    ): ChirStructDeclaration {
        if (common::class != specific::class ||
            common.typeParameters.size != specific.typeParameters.size ||
            !context.sameTypeRefs(common.typeParameterTypes, specific.typeParameterTypes) ||
            common.typeDeclarationId != specific.typeDeclarationId
        ) {
            throw CjmpChirPackageActualizationException(
                "Matched CHIR struct ${common.semanticId} has incompatible generic shape in $location",
            )
        }
        return DefaultChirStructDeclaration(
            semanticId = specific.semanticId,
            name = specific.name,
            typeDeclarationId = specific.typeDeclarationId,
            typeParameters = specific.typeParameters,
            typeParameterTypes = specific.typeParameterTypes,
            fieldDeclarations = mergeTypedDeclarations(
                common.fieldDeclarations,
                specific.fieldDeclarations,
                context,
                "fields of ${specific.name}",
            ),
            memberDeclarations = mergeDeclarations(
                common.memberDeclarations,
                specific.memberDeclarations,
                context,
                "members of ${specific.name}",
            ),
            attributes = mergeAttributes(common.attributes, specific.attributes),
        )
    }

    private fun actualizeEnum(
        common: ChirEnumDeclaration,
        specific: ChirEnumDeclaration,
        context: CjmpChirActualizationContext,
        location: String,
    ): ChirEnumDeclaration {
        if (common::class != specific::class ||
            common.typeParameters.size != specific.typeParameters.size ||
            !context.sameTypeRefs(common.typeParameterTypes, specific.typeParameterTypes) ||
            common.typeDeclarationId != specific.typeDeclarationId
        ) {
            throw CjmpChirPackageActualizationException(
                "Matched CHIR enum ${common.semanticId} has incompatible generic shape in $location",
            )
        }
        return DefaultChirEnumDeclaration(
            semanticId = specific.semanticId,
            name = specific.name,
            typeDeclarationId = specific.typeDeclarationId,
            typeParameters = specific.typeParameters,
            typeParameterTypes = specific.typeParameterTypes,
            cases = (common.cases + specific.cases).distinct(),
            caseTypes = mergeEnumCaseTypes(common.caseTypes, specific.caseTypes, context, location),
            memberDeclarations = mergeDeclarations(
                common.memberDeclarations,
                specific.memberDeclarations,
                context,
                "members of ${specific.name}",
            ),
            attributes = mergeAttributes(common.attributes, specific.attributes),
        )
    }

    private fun actualizeExtend(
        common: ChirExtendDeclaration,
        specific: ChirExtendDeclaration,
        context: CjmpChirActualizationContext,
        location: String,
    ): ChirExtendDeclaration {
        if (common::class != specific::class ||
            common.typeParameters.size != specific.typeParameters.size ||
            !context.sameTypeRefs(common.typeParameterTypes, specific.typeParameterTypes) ||
            !context.areEquivalent(common.targetType, specific.targetType)
        ) {
            throw CjmpChirPackageActualizationException(
                "Matched CHIR extend ${common.semanticId} has incompatible target or generic shape in $location",
            )
        }
        return DefaultChirExtendDeclaration(
            semanticId = specific.semanticId,
            name = specific.name,
            targetType = specific.targetType,
            extendedTypes = context.mergeTypeRefs(
                common.extendedTypes.map(context::substitute),
                specific.extendedTypes,
            ),
            typeParameters = specific.typeParameters,
            typeParameterTypes = specific.typeParameterTypes,
            memberDeclarations = mergeDeclarations(
                common.memberDeclarations,
                specific.memberDeclarations,
                context,
                "members of ${specific.name}",
            ),
            attributes = mergeAttributes(common.attributes, specific.attributes),
        )
    }

    private fun actualizePlainType(
        common: ChirTypeDeclaration,
        specific: ChirTypeDeclaration,
        context: CjmpChirActualizationContext,
        location: String,
    ): ChirTypeDeclaration {
        if (common::class != specific::class ||
            common.typeParameters.size != specific.typeParameters.size ||
            !context.sameTypeRefs(common.typeParameterTypes, specific.typeParameterTypes)
        ) {
            throw CjmpChirPackageActualizationException(
                "Matched CHIR type ${common.semanticId} has incompatible generic shape in $location",
            )
        }
        if (common !is DefaultChirTypeDeclaration || specific !is DefaultChirTypeDeclaration) {
            throw CjmpChirPackageActualizationException(
                "Matched plain CHIR type ${common.semanticId} uses an unsupported declaration implementation",
            )
        }
        return specific.copy(attributes = mergeAttributes(common.attributes, specific.attributes))
    }

    private fun mergeEnumCaseTypes(
        common: List<org.cangnova.cangjie.chir.core.type.ChirEnumCaseType>,
        specific: List<org.cangnova.cangjie.chir.core.type.ChirEnumCaseType>,
        context: CjmpChirActualizationContext,
        location: String,
    ): List<org.cangnova.cangjie.chir.core.type.ChirEnumCaseType> {
        val cases = linkedMapOf<String, org.cangnova.cangjie.chir.core.type.ChirEnumCaseType>()
        val substitutedCommonCases = common.map { case ->
            case.copy(payloadTypes = case.payloadTypes.map(context::substitute))
        }
        (substitutedCommonCases + specific).forEach { case ->
            val previous = cases[case.name]
            if (previous != null && !context.sameTypeRefs(previous.payloadTypes, case.payloadTypes)) {
                throw CjmpChirPackageActualizationException(
                    "Matched CHIR enum case ${case.name} has incompatible payload types in $location",
                )
            }
            cases[case.name] = case
        }
        return cases.values.toList()
    }

    private fun mergeInitializer(
        kind: String,
        commonId: ChirSemanticId?,
        specificId: ChirSemanticId?,
        declarationsById: Map<ChirSemanticId, ChirDeclaration>,
    ): InitializerMerge = when {
        commonId == null -> InitializerMerge(specificId, null)
        specificId == null -> InitializerMerge(commonId, null)
        commonId == specificId -> InitializerMerge(specificId, null)
        else -> {
            val commonFunction = declarationsById[commonId] as? ChirFunctionDeclaration
                ?: throw CjmpChirPackageActualizationException(
                    "Common $kind target $commonId is not a function declaration",
                )
            val specificFunction = declarationsById[specificId] as? ChirFunctionDeclaration
                ?: throw CjmpChirPackageActualizationException(
                    "Specific $kind target $specificId is not a function declaration",
                )
            val combined = combineInitializers(kind, commonFunction, specificFunction)
            InitializerMerge(combined.semanticId, combined)
        }
    }

    private fun combineInitializers(
        kind: String,
        common: ChirFunctionDeclaration,
        specific: ChirFunctionDeclaration,
    ): DefaultChirFunctionDeclaration {
        val unitType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
        listOf("common" to common, "specific" to specific).forEach { (role, initializer) ->
            if (initializer.parameters.isNotEmpty() || initializer.returnType != unitType) {
                throw CjmpChirPackageActualizationException(
                    "$role $kind function ${initializer.semanticId} must be a parameterless Unit function",
                )
            }
        }

        val functionId = ChirSemanticId("${common.semanticId.value}:cjmp:$kind:combined")
        val blockId = ChirSemanticId("${functionId.value}:entry")
        val functionType = ChirResolvedTypeRef(
            ChirFunctionType(parameterTypes = emptyList(), returnType = unitType),
        )
        val expressions = listOf(common, specific).mapIndexed { index, function ->
            ChirCallExpression(
                semanticId = ChirSemanticId("${functionId.value}:call:$index"),
                callee = ChirFunctionValue(
                    semanticId = function.semanticId,
                    type = functionType,
                    name = function.name,
                ),
                arguments = emptyList(),
                resultType = unitType,
            )
        }
        return DefaultChirFunctionDeclaration(
            semanticId = functionId,
            name = "<${kind}>",
            returnType = unitType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = blockId,
                    name = "entry",
                    expressions = expressions,
                    terminator = ChirReturnTerminator(ChirSemanticId("${functionId.value}:return")),
                ),
            ),
            entryBlockId = blockId,
        )
    }

    private fun appendGeneratedInitializers(
        modules: List<ChirModule>,
        generatedInitializers: List<DefaultChirFunctionDeclaration>,
    ): List<ChirModule> {
        if (generatedInitializers.isEmpty()) return modules
        val moduleId = ChirSemanticId("${generatedInitializers.first().semanticId.value}:module")
        if (modules.any { it.semanticId == moduleId }) {
            throw CjmpChirPackageActualizationException(
                "Generated CJMP initializer module identity collides with an input module: $moduleId",
            )
        }
        return modules + ChirModule(
            semanticId = moduleId,
            name = "cjmp-initializers",
            declarations = generatedInitializers,
        )
    }

    private fun mergeAttributes(
        common: Set<ChirAttribute>,
        specific: Set<ChirAttribute>,
    ): Set<ChirAttribute> = buildMap {
        common.forEach { put(it.key, it) }
        specific.forEach { put(it.key, it) }
    }.values.toCollection(linkedSetOf())

    private fun uniqueBySemanticId(
        declarations: List<ChirDeclaration>,
        location: String,
    ): Map<ChirSemanticId, ChirDeclaration> {
        val result = linkedMapOf<ChirSemanticId, ChirDeclaration>()
        declarations.forEach { declaration ->
            if (result.putIfAbsent(declaration.semanticId, declaration) != null) {
                throw CjmpChirPackageActualizationException(
                    "Duplicate CHIR declaration identity ${declaration.semanticId} in $location",
                )
            }
        }
        return result
    }

    private fun uniqueModulesBySemanticId(
        modules: List<ChirModule>,
        location: String,
    ): Map<ChirSemanticId, ChirModule> {
        val result = linkedMapOf<ChirSemanticId, ChirModule>()
        modules.forEach { module ->
            if (result.putIfAbsent(module.semanticId, module) != null) {
                throw CjmpChirPackageActualizationException(
                    "Duplicate CHIR module identity ${module.semanticId} in $location",
                )
            }
        }
        return result
    }

    private data class InitializerMerge(
        val functionId: ChirSemanticId?,
        val generatedFunction: DefaultChirFunctionDeclaration?,
    )

    private data class ImportedFunctionActualizations(
        val replacements: Map<ChirSemanticId, ChirDeclaration>,
        val removedCommonIds: Set<ChirSemanticId>,
    )

    private data class VariableMember(
        val declaration: ChirVariableDeclaration,
        val imported: Boolean,
    )

    private data class MergedVariableMembers(
        val globalVariables: List<ChirVariableDeclaration>,
        val importedVariables: List<ChirVariableDeclaration>,
    )

    private companion object {
        val validator = DefaultChirValidator()
    }
}

/** CJMP 合并期间共享的 matcher 授权关系与局部泛型等价上下文。 */
private data class CjmpChirActualizationContext(
    val declarationPairs: Set<CjmpChirDeclarationPair>,
    val genericIdentityMappings: Map<String, String>,
    val genericTypeSubstitutions: Map<String, ChirTypeRef>,
) {
    fun isApprovedPair(commonIdentity: ChirSemanticId, specificIdentity: ChirSemanticId): Boolean =
        CjmpChirDeclarationPair(commonIdentity, specificIdentity) in declarationPairs

    fun areEquivalent(first: ChirTypeRef, second: ChirTypeRef): Boolean =
        ChirTypeRelations.areEquivalent(first, second, genericIdentityMappings)

    fun substitute(type: ChirTypeRef): ChirTypeRef = ChirTypeRelations.substitute(type, genericTypeSubstitutions)

    fun substitute(declaration: ChirDeclaration): ChirDeclaration =
        ChirTypeSubstitutor.substitute(declaration, genericTypeSubstitutions)

    fun substitute(blocks: List<ChirBlock>): List<ChirBlock> =
        ChirTypeSubstitutor.substitute(blocks, genericTypeSubstitutions)

    fun sameTypeRefs(first: List<ChirTypeRef>, second: List<ChirTypeRef>): Boolean =
        first.size == second.size && first.zip(second).all { (left, right) -> areEquivalent(left, right) }

    fun mergeTypeRefs(common: List<ChirTypeRef>, specific: List<ChirTypeRef>): List<ChirTypeRef> = buildList {
        (common + specific).forEach { candidate ->
            if (none { existing -> areEquivalent(existing, candidate) }) add(candidate)
        }
    }
}

/** CFIR matcher 关系与 CHIR 包身份不一致或产生无效结果时抛出。 */
class CjmpChirPackageActualizationException(message: String) : IllegalArgumentException(message)
