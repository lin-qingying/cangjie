package org.cangnova.cangjie.chir.cjmp

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.serializer.DefaultOfficialChirV113ChirPackageImporter
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113ChirPackageImporter
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113ImportedChirPackage
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticPackage

/** 将旧版 positional RAW CHIR 与已完成 CFIR matching 的 specific CHIR 连接到同一仓内包。 */
interface CjmpLegacyChirPackageLinker {
    /**
     * 保留既有调用契约，完整导入官方 common 包并按共享稳定身份 actualize specific 包。
     */
    fun link(
        commonSemanticPackage: OfficialChirV113SemanticPackage,
        specificPackage: ChirPackage,
        actualizedIdentities: Set<ChirSemanticId>,
    ): ChirPackage

    /** 按 CFIR matcher 冻结的声明及泛型关系链接官方 common 包与 specific 包。 */
    fun link(
        commonSemanticPackage: OfficialChirV113SemanticPackage,
        specificPackage: ChirPackage,
        plan: CjmpChirActualizationPlan,
    ): ChirPackage = link(commonSemanticPackage, specificPackage, plan.asLegacyLinkerIdentities())
}

/**
 * 旧版 CJMP 的 CHIR 包连接边界。
 *
 * 官方 positional CHIR 是 common 包唯一的生产来源；specific 包沿用它的仓内 package identity，
 * 并以官方 common 包的 access level 作为两侧共享的包级事实。
 */
class DefaultCjmpLegacyChirPackageLinker(
    private val importer: OfficialChirV113ChirPackageImporter = DefaultOfficialChirV113ChirPackageImporter(),
    private val actualizer: CjmpChirPackageActualizer = DefaultCjmpChirPackageActualizer(),
) : CjmpLegacyChirPackageLinker {
    override fun link(
        commonSemanticPackage: OfficialChirV113SemanticPackage,
        specificPackage: ChirPackage,
        plan: CjmpChirActualizationPlan,
    ): ChirPackage = linkActualizedPackage(commonSemanticPackage, specificPackage) { imported, specific ->
        actualizer.actualize(
            imported.chirPackage,
            specific,
            resolveCommonDeclarationIdentities(imported, plan),
        )
    }

    /** 旧 set 入口由 actualizer 根据实际包内名义类型构造精确配对。 */
    override fun link(
        commonSemanticPackage: OfficialChirV113SemanticPackage,
        specificPackage: ChirPackage,
        actualizedIdentities: Set<ChirSemanticId>,
    ): ChirPackage = linkActualizedPackage(commonSemanticPackage, specificPackage) { imported, specific ->
        actualizer.actualize(imported.chirPackage, specific, actualizedIdentities)
    }

    private fun linkActualizedPackage(
        commonSemanticPackage: OfficialChirV113SemanticPackage,
        specificPackage: ChirPackage,
        actualize: (OfficialChirV113ImportedChirPackage, ChirPackage) -> ChirPackage,
    ): ChirPackage {
        val importedCommonPackage = importer.importPackageWithIdentities(
            semanticPackage = commonSemanticPackage,
            packageSemanticId = specificPackage.semanticId,
        )
        val commonPackage = importedCommonPackage.chirPackage
        if (commonPackage.name != specificPackage.name) {
            throw CjmpChirPackageActualizationException(
                "Official common package ${commonPackage.name} does not match specific package ${specificPackage.name}",
            )
        }
        return actualize(importedCommonPackage, specificPackage.copy(accessLevel = commonPackage.accessLevel))
    }

    /** 在 importer/linker 边界将 matcher 源身份固化为已导入 DEFINITION 的精确仓内 ID。 */
    private fun resolveCommonDeclarationIdentities(
        imported: OfficialChirV113ImportedChirPackage,
        plan: CjmpChirActualizationPlan,
    ): CjmpChirActualizationPlan {
        val resolvedCommonIdentities = linkedMapOf<ChirSemanticId, ChirSemanticId>()
        plan.commonNominalTypeIdentities.forEach { (matcherIdentity, sourceIdentity) ->
            val importedIdentity = imported.nominalTypeIdsBySourceIdentity[sourceIdentity]
                ?: throw CjmpChirPackageActualizationException(
                    "Matcher-approved common nominal identity $matcherIdentity has no RAW CHIR definition " +
                        "for ${sourceIdentity.typeDeclarationId.qualifiedName} (${sourceIdentity.definitionKind})",
                )
            putResolvedIdentity(resolvedCommonIdentities, matcherIdentity, importedIdentity)
        }
        plan.commonFunctionSourceIdentities.forEach { (matcherIdentity, sourceIdentity) ->
            val matches = imported.functionIdsByRawMangledName[sourceIdentity].orEmpty()
            if (matches.size != 1) {
                throw CjmpChirPackageActualizationException(
                    "Matcher-approved common function identity $matcherIdentity resolves through RAW CHIR " +
                        "raw mangled name '${sourceIdentity.rawMangledName}' to ${matches.size} functions",
                )
            }
            putResolvedIdentity(resolvedCommonIdentities, matcherIdentity, matches.single())
        }
        val declarationPairs = plan.declarationPairs.mapTo(linkedSetOf()) { pair ->
            pair.copy(commonIdentity = resolvedCommonIdentities[pair.commonIdentity] ?: pair.commonIdentity)
        }
        val typeParameterPairs = plan.typeParameterPairs.mapTo(linkedSetOf()) { pair ->
            pair.copy(commonOwnerIdentity = resolvedCommonIdentities[pair.commonOwnerIdentity] ?: pair.commonOwnerIdentity)
        }
        return CjmpChirActualizationPlan(
            declarationPairs = declarationPairs,
            typeParameterPairs = typeParameterPairs,
        )
    }

    private fun putResolvedIdentity(
        identities: MutableMap<ChirSemanticId, ChirSemanticId>,
        matcherIdentity: ChirSemanticId,
        importedIdentity: ChirSemanticId,
    ) {
        val previous = identities.putIfAbsent(matcherIdentity, importedIdentity)
        if (previous != null && previous != importedIdentity) {
            throw CjmpChirPackageActualizationException(
                "Matcher identity $matcherIdentity resolves to conflicting RAW CHIR declarations",
            )
        }
    }
}
