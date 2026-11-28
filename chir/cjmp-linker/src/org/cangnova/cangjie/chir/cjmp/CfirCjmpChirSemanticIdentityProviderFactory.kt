package org.cangnova.cangjie.chir.cjmp

import org.cangnova.cangjie.chir.cfir2chir.Cfir2ChirConversionException
import org.cangnova.cangjie.chir.cfir2chir.Cfir2ChirSemanticIdentityProvider
import org.cangnova.cangjie.chir.cfir2chir.Cfir2ChirSemanticIdentityProvider.Companion.typeParameterIndex
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDefinitionKind
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113FunctionSourceIdentity
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113NominalTypeIdentity
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.util.classId
import org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackageSnapshotProvider
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.metadata.model.Attribute
import java.nio.file.Path

/**
 * 将 CJO callable 的官方 mangled name、nominal source identity 与 CFIR family matcher 关系连接为 CHIR 身份。
 */
object CfirCjmpChirSemanticIdentityProviderFactory {
    private const val OFFICIAL_CHIR_DECLARATION_PREFIX = "@"
    private const val CJO_NOMINAL_SOURCE_ID_PREFIX = "cfir:cjmp:cjo-nominal:"
    private const val CJO_RAW_MANGLED_FUNCTION_PREFIX = "cfir:cjmp:cjo-raw-function:"

    /**
     * 为一个编译图创建身份 provider；CJO 查询只读取当前反序列化 provider 已加载的精确来源快照。
     */
    fun create(
        families: Iterable<CfirCjmpFamilySnapshot>,
        loadedPackages: CjoLoadedPackageSnapshotProvider,
    ): Cfir2ChirSemanticIdentityProvider = Cfir2ChirSemanticIdentityProvider.fromCjmpFamilies(families) { symbol ->
        identityOf(symbol, loadedPackages)
    }

    /** 从生产依赖 provider 图解析 CJO 来源，保留当前 session 实际加载的来源代。 */
    fun create(
        families: Iterable<CfirCjmpFamilySnapshot>,
        dependencies: CfirSymbolProvider,
    ): Cfir2ChirSemanticIdentityProvider = Cfir2ChirSemanticIdentityProvider.fromCjmpFamilies(families) { symbol ->
        identityOf(symbol, dependencies)
    }

    /**
     * 将已接受的 CFIR 声明和类型参数映射投影为 CHIR actualization 计划。
     *
     * 声明身份沿用转换器的稳定身份规则；common 泛型 owner 则必须能从当前 CJO provider 图解析。
     */
    fun createActualizationPlan(
        families: Iterable<CfirCjmpFamilySnapshot>,
        dependencies: CfirSymbolProvider,
        specificIdentityProvider: Cfir2ChirSemanticIdentityProvider,
    ): CjmpChirActualizationPlan {
        val acceptedRelations = buildList {
            families.forEach { family ->
                family.specificDeclarations.forEach { specific ->
                    family.commonCounterpartsFor(specific).forEach { common ->
                        add(Triple(family, specific, common))
                    }
                }
            }
        }
        val commonFunctionSourceIdentities = linkedMapOf<ChirSemanticId, OfficialChirV113FunctionSourceIdentity>()
        val declarationPairs = acceptedRelations.mapTo(linkedSetOf()) { (_, specific, common) ->
            val commonIdentity = declarationIdentity(common.symbol, dependencies)
                ?: throw Cfir2ChirConversionException(
                    "matched common declaration has no official CHIR identity",
                    common,
                )
            recordFunctionSourceIdentity(commonFunctionSourceIdentities, commonIdentity)
            CjmpChirDeclarationPair(
                commonIdentity = commonIdentity.semanticId,
                specificIdentity = specificIdentityProvider.semanticIdentityOf(specific.symbol),
            )
        }
        val commonNominalTypeIdentities = linkedMapOf<ChirSemanticId, OfficialChirV113NominalTypeIdentity>()
        acceptedRelations.forEach { (_, _, common) ->
            val declaration = common.symbol.cfir as? CfirClassLikeDeclaration ?: return@forEach
            val definitionKind = when (declaration) {
                is CfirClass, is CfirInterface -> ChirTypeDefinitionKind.CLASS
                is CfirStruct -> ChirTypeDefinitionKind.STRUCT
                is CfirEnum -> ChirTypeDefinitionKind.ENUM
                else -> return@forEach
            }
            val commonIdentity = declarationIdentity(common.symbol, dependencies)
                ?: throw Cfir2ChirConversionException(
                    "matched common nominal declaration has no official CHIR identity",
                    common,
                )
            recordFunctionSourceIdentity(commonFunctionSourceIdentities, commonIdentity)
            val classId = declaration.classId
            val sourceIdentity = OfficialChirV113NominalTypeIdentity(
                typeDeclarationId = ChirTypeDeclarationId(
                    packageName = classId.packageFqName.asString(),
                    declarationName = classId.relativeClassName.asString(),
                ),
                definitionKind = definitionKind,
            )
            val previous = commonNominalTypeIdentities.putIfAbsent(commonIdentity.semanticId, sourceIdentity)
            if (previous != null && previous != sourceIdentity) {
                throw Cfir2ChirConversionException(
                    "matched common nominal identity maps to conflicting RAW CHIR source declarations",
                    common,
                )
            }
        }
        val typeParameterPairs = acceptedRelations.flatMapTo(linkedSetOf()) { (family, specific, common) ->
            family.typeParameterMappingFor(specific, common).map { (commonParameter, specificParameter) ->
                val commonOwner = commonParameter.symbol.containingDeclarationSymbol
                val specificOwner = specificParameter.symbol.containingDeclarationSymbol
                val commonOwnerIdentity = declarationIdentity(commonOwner, dependencies)
                    ?: throw Cfir2ChirConversionException(
                        "matched common type parameter owner has no official CHIR identity",
                        commonParameter,
                    )
                recordFunctionSourceIdentity(commonFunctionSourceIdentities, commonOwnerIdentity)
                val specificOwnerIdentity = specificIdentityProvider.semanticIdentityOf(specificOwner)
                val ownerPair = CjmpChirDeclarationPair(commonOwnerIdentity.semanticId, specificOwnerIdentity)
                if (ownerPair !in declarationPairs) {
                    throw Cfir2ChirConversionException(
                        "accepted CJMP type parameter mapping has no accepted declaration owner pair",
                        specificParameter,
                    )
                }
                CjmpChirTypeParameterPair(
                    commonOwnerIdentity = commonOwnerIdentity.semanticId,
                    commonIndex = typeParameterIndex(commonParameter.symbol),
                    specificOwnerIdentity = specificOwnerIdentity,
                    specificIndex = typeParameterIndex(specificParameter.symbol),
                )
            }
        }
        return CjmpChirActualizationPlan(
            declarationPairs = declarationPairs,
            typeParameterPairs = typeParameterPairs,
            commonNominalTypeIdentities = commonNominalTypeIdentities,
            commonFunctionSourceIdentities = commonFunctionSourceIdentities,
        )
    }

    /** CJO 声明没有参与本轮 CFIR matcher 时也按其原始来源取得稳定 CHIR 身份。 */
    fun identityOf(
        symbol: CfirBasedSymbol<*>,
        loadedPackages: CjoLoadedPackageSnapshotProvider,
    ): ChirSemanticId? {
        return declarationIdentity(symbol, loadedPackages)?.semanticId
    }

    /** 解析组合依赖 provider 中的 CJO 声明身份；多代来源冲突会显式拒绝。 */
    fun identityOf(
        symbol: CfirBasedSymbol<*>,
        dependencies: CfirSymbolProvider,
    ): ChirSemanticId? {
        return declarationIdentity(symbol, dependencies)?.semanticId
    }

    /** 通过生产依赖 provider 图解析声明来源。 */
    fun identityOf(
        origin: CfirCjoDeclarationOrigin,
        dependencies: CfirSymbolProvider,
    ): ChirSemanticId = declarationIdentity(origin) { sourcePath -> loadedPackageSnapshots(dependencies, sourcePath) }.semanticId

    /** 从声明记录的精确 CJO 包路径和 allDecls 索引解析稳定 CHIR 身份。 */
    fun identityOf(
        origin: CfirCjoDeclarationOrigin,
        loadedPackages: CjoLoadedPackageSnapshotProvider,
    ): ChirSemanticId = declarationIdentity(origin) { sourcePath ->
        listOfNotNull(loadedPackages.loadedPackageSnapshot(sourcePath))
    }.semanticId

    private fun declarationIdentity(
        symbol: CfirBasedSymbol<*>,
        dependencies: CfirSymbolProvider,
    ): CfirCjmpDeclarationIdentity? {
        val origin = symbol.cfir.cjoDeclarationOrigin ?: return null
        return declarationIdentity(origin) { sourcePath -> loadedPackageSnapshots(dependencies, sourcePath) }
    }

    private fun declarationIdentity(
        symbol: CfirBasedSymbol<*>,
        loadedPackages: CjoLoadedPackageSnapshotProvider,
    ): CfirCjmpDeclarationIdentity? {
        val origin = symbol.cfir.cjoDeclarationOrigin ?: return null
        return declarationIdentity(origin) { sourcePath ->
            listOfNotNull(loadedPackages.loadedPackageSnapshot(sourcePath))
        }
    }

    private fun declarationIdentity(
        origin: CfirCjoDeclarationOrigin,
        loadedPackageSnapshots: (Path) -> List<CjoLoadedPackage>,
    ): CfirCjmpDeclarationIdentity {
        val packagePath = Path.of(origin.packagePath).toAbsolutePath().normalize()
        val snapshots = loadedPackageSnapshots(packagePath)
            .filter { it.sourcePath.toAbsolutePath().normalize() == packagePath }
        if (snapshots.isEmpty()) {
            throw Cfir2ChirConversionException(
                "CJO declaration origin is absent from the active loaded-package generation: $origin",
            )
        }
        val identities = snapshots.map { snapshot ->
            if (origin.declarationIndex !in 0 until snapshot.pkg.allDeclsLength) {
                throw Cfir2ChirConversionException(
                    "CJO declaration origin index is outside its loaded package: $origin",
                )
            }
            val declaration = snapshot.pkg.allDecls(origin.declarationIndex)
                ?: throw Cfir2ChirConversionException(
                    "CJO declaration origin index has no declaration table: $origin",
                )
            declaration.toCjmpDeclarationIdentity(snapshot.header.fullPkgName, origin)
        }.distinct()
        if (identities.size != 1) {
            throw Cfir2ChirConversionException(
                "CJO declaration origin resolves to conflicting official CHIR identities: $origin",
            )
        }
        return identities.single()
    }

    private fun PackageFormat.Decl.toCjmpDeclarationIdentity(
        packageName: String,
        origin: CfirCjoDeclarationOrigin,
    ): CfirCjmpDeclarationIdentity {
        nominalTypeIdentity(packageName)?.let { nominalIdentity ->
            return CfirCjmpDeclarationIdentity(nominalIdentity.toCjoNominalSemanticId())
        }
        val foreignFunction = kind == PackageFormat.DeclKind.FuncDecl && hasAttribute(Attribute.FOREIGN)
        if (foreignFunction) {
            val rawMangledName = mangledBeforeSema?.takeIf(String::isNotBlank)
                ?: throw Cfir2ChirConversionException("CJO FOREIGN function has no official raw mangled name: $origin")
            return CfirCjmpDeclarationIdentity(ChirSemanticId(rawMangledName))
        }
        mangledName?.takeIf(String::isNotBlank)?.let { officialMangledName ->
            return CfirCjmpDeclarationIdentity(ChirSemanticId(OFFICIAL_CHIR_DECLARATION_PREFIX + officialMangledName))
        }
        val rawMangledName = if (kind == PackageFormat.DeclKind.FuncDecl) {
            mangledBeforeSema?.takeIf(String::isNotBlank)
        } else {
            null
        }
        if (kind == PackageFormat.DeclKind.FuncDecl) {
            val sourceIdentity = rawMangledName?.let(::OfficialChirV113FunctionSourceIdentity)
            val exportIdentity = exportId?.takeIf(String::isNotBlank)
            val semanticIdentity = exportIdentity?.let { ChirSemanticId(OFFICIAL_CHIR_DECLARATION_PREFIX + it) }
                ?: sourceIdentity?.toCjoFunctionSemanticId()
                ?: throw Cfir2ChirConversionException(
                    "CJO function declaration ${identifier ?: "<unnamed>"} has no official export or raw mangled identity: $origin",
                )
            return CfirCjmpDeclarationIdentity(
                semanticId = semanticIdentity,
                functionSourceIdentity = sourceIdentity,
            )
        }
        throw Cfir2ChirConversionException(
            "CJO ${kind} declaration ${identifier ?: "<unnamed>"} has no supported official CHIR identity: $origin",
        )
    }

    private fun recordFunctionSourceIdentity(
        identities: MutableMap<ChirSemanticId, OfficialChirV113FunctionSourceIdentity>,
        declarationIdentity: CfirCjmpDeclarationIdentity,
    ) {
        val sourceIdentity = declarationIdentity.functionSourceIdentity ?: return
        val previous = identities.putIfAbsent(declarationIdentity.semanticId, sourceIdentity)
        if (previous != null && previous != sourceIdentity) {
            throw Cfir2ChirConversionException(
                "one CJMP matcher identity maps to conflicting RAW CHIR function source identities",
            )
        }
    }

    private data class CfirCjmpDeclarationIdentity(
        val semanticId: ChirSemanticId,
        val functionSourceIdentity: OfficialChirV113FunctionSourceIdentity? = null,
    )

    /**
     * Official nominal CHIR declarations are indexed by package, source identifier and definition kind.
     * Their CJO records do not provide the callable mangled-name identity used by function references.
     */
    private fun PackageFormat.Decl.nominalTypeIdentity(
        packageName: String,
    ): OfficialChirV113NominalTypeIdentity? {
        val definitionKind = when (kind) {
            PackageFormat.DeclKind.ClassDecl,
            PackageFormat.DeclKind.InterfaceDecl,
            -> ChirTypeDefinitionKind.CLASS

            PackageFormat.DeclKind.StructDecl -> ChirTypeDefinitionKind.STRUCT
            PackageFormat.DeclKind.EnumDecl -> ChirTypeDefinitionKind.ENUM
            else -> return null
        }
        val sourceName = identifier?.takeIf(String::isNotBlank)
            ?: throw Cfir2ChirConversionException("CJO nominal declaration has no source identifier")
        return OfficialChirV113NominalTypeIdentity(
            typeDeclarationId = ChirTypeDeclarationId(packageName, sourceName),
            definitionKind = definitionKind,
        )
    }

    /**
     * Temporary matcher identity for a CJO nominal declaration.
     *
     * The linker consumes this source-shaped key and replaces it with the exact imported RAW CHIR definition ID.
     * Length-prefixed fields keep package and declaration names unambiguous.
     */
    private fun OfficialChirV113NominalTypeIdentity.toCjoNominalSemanticId(): ChirSemanticId {
        val typeId = typeDeclarationId
        return ChirSemanticId(
            buildString {
                append(CJO_NOMINAL_SOURCE_ID_PREFIX)
                append(typeId.packageName.length)
                append(':')
                append(typeId.packageName)
                append(':')
                append(typeId.declarationName.length)
                append(':')
                append(typeId.declarationName)
                append(':')
                append(definitionKind.name)
            },
        )
    }

    private fun OfficialChirV113FunctionSourceIdentity.toCjoFunctionSemanticId(): ChirSemanticId =
        ChirSemanticId(
            buildString {
                append(CJO_RAW_MANGLED_FUNCTION_PREFIX)
                append(rawMangledName.length)
                append(':')
                append(rawMangledName)
            },
        )

    private fun PackageFormat.Decl.hasAttribute(attribute: Attribute): Boolean {
        val bit = attribute.ordinal
        val wordIndex = bit / ULong.SIZE_BITS
        if (wordIndex >= attributesLength) return false
        return attributes(wordIndex).toLong() and (1L shl (bit % ULong.SIZE_BITS)) != 0L
    }

    private fun loadedPackageSnapshots(
        provider: CfirSymbolProvider,
        sourcePath: Path,
    ): List<CjoLoadedPackage> = when (provider) {
        is CfirCompositeSymbolProvider -> provider.providers.flatMap { loadedPackageSnapshots(it, sourcePath) }
        is CjoLoadedPackageSnapshotProvider -> listOfNotNull(provider.loadedPackageSnapshot(sourcePath))
        else -> emptyList()
    }

}
