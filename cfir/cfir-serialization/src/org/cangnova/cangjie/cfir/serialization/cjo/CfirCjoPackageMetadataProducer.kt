@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo


import PackageFormat.AnnoKind
import PackageFormat.DeclKind
import PackageFormat.SemaTyInfo
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirConstantValueExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisInputPreparation
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypeUse
import org.cangnova.cangjie.config.CangJieCompilerVersion
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportSourceElement
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypeElement
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportTypePayload
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportNominalTypeKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantGraph
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantNode
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantIdentity
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoConstValueReader
import org.cangnova.cangjie.metadata.model.Attribute as CfirAttribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.text
import java.util.IdentityHashMap
import java.nio.file.Path
import java.math.BigInteger

/**
 * Live CFIR-to-CJO metadata producer.
 *
 * This is the serialization owner for source CFIR.  It first freezes one
 * declaration index and then uses that index for every type, parameter,
 * property and annotation reference.  Unsupported CFIR shapes fail at the
 * producer boundary instead of silently emitting a partial CJO.
 */
object CfirCjoPackageMetadataProducer {
    fun produce(
        files: Collection<CfirFile>,
        familySnapshot: CfirCjmpFamilySnapshot? = null,
        loadedPackages: Collection<CjoLoadedPackage> = emptyList(),
        exportOptions: CjoExportOptions = CjoExportOptions.forSourceFiles(files, familySnapshot),
        canonicalSources: Collection<CfirSourceGraph> = emptyList(),
    ): CjoPackageMetadata {
        require(files.isNotEmpty()) { "Cannot serialize an empty CFIR module" }
        val prepared = Preparation(files, familySnapshot, loadedPackages, exportOptions, canonicalSources).prepare()
        return prepared.publicationInputs.withReadOnlyInput {
            prepared.analysis.withReadOnlyInput {
                prepared.publicationInputs.validateUnchanged()
                Context(prepared).produce().also { prepared.publicationInputs.validateUnchanged() }
            }
        }
    }

    /** 语义和格式来源全部完成之后，才创建拥有输出 d/e/t/v 池的 Context。 */
    private class PreparedInputs(
        val files: List<CfirFile>,
        val family: CfirCjmpFamilySnapshot?,
        val options: CjoExportOptions,
        val roots: List<CfirDeclaration>,
        val builtins: List<CfirBuiltInDeclaration>,
        val sourceFiles: Map<CfirBasedSymbol<*>, CfirFile>,
        val analysis: CfirDeclarationExportAnalysis,
        val sources: CjoFrozenSourceInputs,
        val plan: CjoSourceExportPlan,
        val sourceFormats: CjoSourceFormatInputs,
        val initializationInputs: CjoInitializationDependencyInputs,
        val publicationInputs: CjoSourcePublicationInputs,
        val attributeInputs: CjoDeclarationAttributeInputs,
        val dependencyInputs: CjoDeclarationDependencyInputs,
    )

    private class Preparation(
        private val files: Collection<CfirFile>,
        private val familySnapshot: CfirCjmpFamilySnapshot?,
        private val loadedPackages: Collection<CjoLoadedPackage>,
        private val exportOptions: CjoExportOptions,
        private val canonicalSources: Collection<CfirSourceGraph>,
    ) {
        private data class SourceDeclarationIdentity(
            val declarationType: String,
            val sourcePath: Path,
            val startOffset: Int,
            val endOffset: Int,
        )

        private data class SessionDeclarationIdentity(val symbol: CfirBasedSymbol<*>)

        private data class ExportNodeIdentity(
            val declaration: Any,
            val owner: ExportNodeIdentity?,
        )

        private val sourceFilesBySymbol = IdentityHashMap<CfirBasedSymbol<*>, CfirFile>().apply {
            fun visit(declaration: CfirDeclaration, file: CfirFile) {
                putIfAbsent(declaration.symbol, file)
                physicalDeclarationChildren(declaration).forEach { visit(it, file) }
                if (declaration is CfirValueParameter) declaration.ownedDefaultParameterHelper
                    ?.takeIf { it.origin.fromSource }?.let { visit(it, file) }
            }
            files.forEach { file -> file.declarations.forEach { visit(it, file) } }
        }
        private val familyExportMembersByOwner = HashMap<ExportNodeIdentity, List<CfirDeclaration>>()
        private val familyOutputOwners = IdentityHashMap<CfirDeclaration, CfirDeclaration>()
        private val packageSnapshots = CjoExportSourceSnapshots.uniqueLoadedPackages(loadedPackages)
            .associateBy { it.sourcePath.toAbsolutePath().normalize() }.toMutableMap()
        private val lexicalOwners = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()

        fun prepare(): PreparedInputs {
            val roots = familySnapshot?.let(::collectFamilyExportGraph)
                ?: files.flatMap { file -> file.declarations.flatMap(::packageDeclarationProjection) }
            val commonRoots = familySnapshot?.let { family ->
                family.familyCommonDeclarations().filter { family.physicalOwnerFor(it) == null }
            }.orEmpty()
            val builtins = prepareStdCoreBuiltins()
            val completeRoots = (files.flatMap { it.declarations.flatMap(::packageDeclarationProjection) } +
                commonRoots + roots + builtins).distinctBy { it.symbol }
            val definitionInputs = ArrayList<CfirDeclaration>()
            val visited = java.util.Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>())
            fun definitions(declaration: CfirDeclaration, owner: CfirDeclaration?) {
                if (!visited.add(declaration)) return
                lexicalOwners[declaration] = owner
                definitionInputs += declaration
                when (declaration) {
                    is CfirClassLikeDeclaration, is CfirExtend -> exportMemberInput(declaration).forEach { definitions(it, declaration) }
                    is CfirProperty -> listOfNotNull(declaration.getter, declaration.setter).forEach { definitions(it, owner) }
                    else -> Unit
                }
            }
            completeRoots.forEach { definitions(it, null) }
            // 定义自身的 header/annotation 及 borrowed 依赖先完成，才能获取其 SOURCE 保存产品。
            val work = CfirExportAnalysisInputPreparation.Work(files, completeRoots, ::exportMemberInput)
            work.complete(canonicalSources)
            val includeContent = exportOptions.exportContent && !exportOptions.isMacroPackage
            CjoExportSourceSnapshots.sourceExportSnapshots(definitionInputs, includeContent)
            while (true) {
                val before = CjoExportSourceSnapshots.completedInputs(definitionInputs, includeContent, canonicalSources, work.declarations)
                val savedTypes = savedTypes(before.snapshots)
                val revision = work.dependencyRevision
                work.complete(before.graphs, savedTypes,
                    definitionInputs + sourceFilesBySymbol.keys.map { it.cfir } +
                        before.snapshots.flatMap { it.declarations.values } + before.graphs.flatMap { it.declarationOwners.keys } +
                        familySnapshot?.familyCommonDeclarations().orEmpty())
                val after = CjoExportSourceSnapshots.completedInputs(definitionInputs, includeContent, canonicalSources, work.declarations)
                if (revision != work.dependencyRevision || !before.sameInputs(after)) continue
                // 空图 owner 与全部 borrowed header owner 同样受护；此处仍未交付 Result。
                val prepared = after.withReadOnlyInput {
                    work.withReadOnlyInput freeze@{
                        val final = CjoExportSourceSnapshots.completedInputs(definitionInputs, includeContent, canonicalSources, work.declarations)
                        if (!after.sameInputs(final)) return@freeze null
                        final.validateUnchanged()
                        val initializationInputs = CjoInitializationDependencyInputs.capture(work.declarations)
                        check(initializationInputs.declarations.all { target -> work.declarations.any { it === target } }) {
                            "Final initialization dependencies must belong to the completed work set"
                        }
                        val preparedInput = work.freeze()
                        val analysis = CfirExportAnalysis.analyze(files, completeRoots, ::exportMemberInput, exportOptions.analysisSettings,
                            familySnapshot?.let { family -> family.familyCommonDeclarations().filter { family.specificBindingsFor(it).isNotEmpty() }.toSet() }.orEmpty(),
                            canonicalSources = final.graphs, additionalTypeInputs = savedTypes,
                            additionalDeclarationInputs = initializationInputs.declarations, preparedInput = preparedInput)
                        final.validateUnchanged()
                        finishPreparation(roots, builtins, analysis, initializationInputs, final)
                    }
                }
                if (prepared != null) return prepared
            }
        }

        private fun savedTypes(snapshots: List<CjoSourceExportSnapshot>): Map<org.cangnova.cangjie.cfir.session.CfirSession, List<ConeCangJieType>> {
            val saved = IdentityHashMap<org.cangnova.cangjie.cfir.session.CfirSession, List<ConeCangJieType>>()
            snapshots.forEach { snapshot ->
                val storageTypes = snapshot.constants.values.filterIsInstance<CjoSavedConstantStorage.Composite>().flatMap { layout -> layout.fields.map { it.type } } +
                    snapshot.constantRoots.values.map { it.type }
                val seenTypes = java.util.Collections.newSetFromMap(IdentityHashMap<ConeCangJieType, Boolean>())
                saved[snapshot.domain] = (saved[snapshot.domain].orEmpty() + snapshot.types.values + storageTypes).filter(seenTypes::add)
            }
            return java.util.Collections.unmodifiableMap(saved)
        }

        private fun finishPreparation(roots: List<CfirDeclaration>, builtins: List<CfirBuiltInDeclaration>,
            analysis: CfirDeclarationExportAnalysis, initializationInputs: CjoInitializationDependencyInputs,
            final: CjoSourcePublicationInputs): PreparedInputs = analysis.withReadOnlyInput {
                val sources = CjoFrozenSourceInputs.capture(analysis, final.snapshots, packageSnapshots.values, exportOptions, final.graphs)
                val sourceFormats = CjoSourceFormatInputs(files.toList(), analysis, exportOptions, builtins)
                val attributeInputs = CjoDeclarationAttributeInputs(analysis, sources, sourceFormats)
                val dependencyInputs = CjoDeclarationDependencyInputs(analysis, sources, familySnapshot)
                val plan = CjoSourceExportPlan.prepare(
                    roots, builtins, analysis, sources, exportOptions, familySnapshot, files.toList(), attributeInputs,
                    java.util.Collections.unmodifiableMap(IdentityHashMap(familyOutputOwners)),
                )
                analysis.validateSourceUnchanged()
                sourceFormats.validateUnchanged()
                initializationInputs.validateUnchanged()
                final.validateUnchanged()
                PreparedInputs(java.util.Collections.unmodifiableList(files.toList()), familySnapshot, exportOptions,
                    java.util.Collections.unmodifiableList(roots.toList()), java.util.Collections.unmodifiableList(builtins.toList()),
                    java.util.Collections.unmodifiableMap(IdentityHashMap(sourceFilesBySymbol)), analysis, sources, plan, sourceFormats, initializationInputs,
                    final, attributeInputs, dependencyInputs)
        }

        private fun cjoDeclarationOriginKey(declaration: CfirDeclaration): CjoSnapshotTableEntry? {
            val origin = declaration.cjoDeclarationOrigin ?: return null
            val path = Path.of(origin.packagePath).toAbsolutePath().normalize()
            val source = checkNotNull(CjoExportSourceSnapshots.loadedPackage(declaration.moduleData.session.symbolProvider, path))
            check(packageSnapshots.putIfAbsent(path, source).let { it == null || it === source })
            return CjoSnapshotTableEntry(source, CjoSnapshotTableDomain.DECLARATION, origin.declarationIndex)
        }

        private fun canonicalDeclaration(declaration: CfirDeclaration): CfirDeclaration {
            if (declaration is CfirTypeParameter) return declaration.symbol.cfir
            return if (declaration is CfirCallableDeclaration) declaration.unwrapSubstitutionOverrides() else declaration
        }

        private fun familyExportNodeIdentity(declaration: CfirDeclaration): ExportNodeIdentity =
            ExportNodeIdentity(exportDeclarationIdentity(declaration), lexicalOwners[declaration]?.let(::familyExportNodeIdentity))

        private fun exportMemberInput(owner: CfirDeclaration): List<CfirDeclaration> {
            val family = familySnapshot
            val isFamilyOwner = family?.hasMemberOwner(owner) == true
            val members = if (isFamilyOwner) checkNotNull(family).let {
                familyExportMembersByOwner[familyExportNodeIdentity(owner)] ?: family.membersFor(owner)
            } else when (owner) {
                is CfirClassLikeDeclaration -> owner.declarations
                is CfirExtend -> owner.declarations
                else -> error("Only class-like and extend declarations have member input")
            }
            // 外部类型仅消费自己的正规 header；它不属于本次 family 的合并/抑制产品。
            return if (isFamilyOwner) familyPackageDeclarations(checkNotNull(family), members)
                else members.flatMap(::packageDeclarationProjection)
        }

        private fun collectFamilyExportGraph(family: CfirCjmpFamilySnapshot): List<CfirDeclaration> =
            familyPackageDeclarations(family, family.packageDeclarationsForExport())
                .groupBy(::exportDeclarationIdentity)
                .values
                .map { candidates -> collectFamilyExportNode(candidates, owner = null, family) }

        private fun collectFamilyExportNode(
            candidates: List<CfirDeclaration>,
            owner: ExportNodeIdentity?,
            family: CfirCjmpFamilySnapshot,
        ): CfirDeclaration {
            val declaration = canonicalDeclaration(candidates.first())
            val nodeIdentity = ExportNodeIdentity(exportDeclarationIdentity(declaration), owner)
            if (declaration is CfirClassLikeDeclaration || declaration is CfirExtend) {
                val addedSpecific = java.util.Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>())
                val orderedMembers = buildList {
                    candidates.flatMap(family::mergedDeclarationsFor).forEach { member ->
                        if ((member as? CfirMemberDeclaration)?.status?.isSpecific == true && member in addedSpecific) return@forEach
                        family.specificBindingsFor(member).forEach { specific ->
                            addedSpecific += specific
                            add(specific)
                        }
                        add(member)
                    }
                }
                val exportedMembers = familyPackageDeclarations(family, orderedMembers)
                    .groupBy(::exportDeclarationIdentity)
                    .values
                    .map { childCandidates -> collectFamilyExportNode(childCandidates, nodeIdentity, family) }
                // 被保留的 common 定义仍有原语义 owner，输出包含关系由实际合并成员表决定。
                exportedMembers.forEach { member ->
                    check(familyOutputOwners.putIfAbsent(member, declaration).let { it == null || it === declaration }) {
                        "A merged declaration belongs to two output containers: ${member.symbol}"
                    }
                }
                familyExportMembersByOwner[nodeIdentity] = exportedMembers
            }
            return declaration
        }

        private fun physicalDeclarationChildren(declaration: CfirDeclaration): List<CfirDeclaration> = buildList {
            when (declaration) {
                is CfirEnum -> {
                    addAll(declaration.typeParameters.filterIsInstance<CfirTypeParameter>())
                    addAll(declaration.declarations.filterNot { it is CfirConstructor && it.isPrimary })
                }
                is CfirClassLikeDeclaration -> {
                    addAll(declaration.typeParameters.filterIsInstance<CfirTypeParameter>())
                    addAll(declaration.declarations)
                }
                is CfirExtend -> {
                    addAll(declaration.typeParameters)
                    addAll(declaration.declarations)
                }
                is CfirFunction -> {
                    // 构造器的 constructed-class 引用不是物理声明，只导出自有类型参数。
                    addAll(declaration.typeParameters.filterIsInstance<CfirTypeParameter>())
                    addAll(declaration.valueParameters)
                }
                is CfirEnumConstructor -> addAll(declaration.valueParameters)
                is CfirPatternVariable -> addAll(declaration.pattern.bindingVariables())
                is CfirProperty -> {
                    declaration.getter?.let(::add)
                    declaration.setter?.let(::add)
                }
                else -> Unit
            }
        }

        private fun packageDeclarationProjection(declaration: CfirDeclaration): List<CfirDeclaration> =
            when (declaration) {
                is CfirPatternVariable -> declaration.pattern.bindingVariables()
                else -> listOf(declaration)
            }

        /** Family 的屏蔽关系位于 binding identity，必须在 pattern root 展开后应用。 */
        private fun familyPackageDeclarations(
            family: CfirCjmpFamilySnapshot,
            declarations: Iterable<CfirDeclaration>,
        ): List<CfirDeclaration> = declarations
            .flatMap(::packageDeclarationProjection)
            .filterNot { it.symbol in family.doNotExport }

        private fun exportDeclarationIdentity(declaration: CfirDeclaration): Any {
            cjoDeclarationOriginKey(declaration)?.let { origin ->
                return origin
            }
            val filePath = sourceFilesBySymbol[declaration.symbol]?.sourceFile?.path
            val source = declaration.source
            if (declaration.origin.fromSource && filePath != null && source != null) {
                return SourceDeclarationIdentity(
                    declarationType = declaration::class.qualifiedName ?: declaration::class.java.name,
                    sourcePath = Path.of(filePath).toAbsolutePath().normalize(),
                    startOffset = source.startOffset,
                    endOffset = source.endOffset,
                )
            }
            return SessionDeclarationIdentity(declaration.symbol)
        }

        private fun prepareStdCoreBuiltins(): List<CfirBuiltInDeclaration> {
            if (files.first().packageDirective.packageFqName != StandardNames.STD_CORE_PACKAGE_FQ_NAME) return emptyList()

            val declarationsByKind = LinkedHashMap<CfirBuiltInTypeKind, CfirBuiltInDeclaration>()
            files.forEach { file ->
                val provider = file.moduleData.session.dependenciesSymbolProvider
                CfirBuiltInTypeKind.entries.forEach kindLoop@{ kind ->
                    val declaration = provider.getClassLikeSymbolByClassId(kind.classId)?.cfir as? CfirBuiltInDeclaration
                        ?: return@kindLoop
                    if (declaration.cjoDeclarationOrigin != null) return@kindLoop

                    check(declaration.origin == CfirDeclarationOrigin.Synthetic.Default) {
                        "A source std.core BuiltInDecl must be supplied by the builtin symbol provider"
                    }
                    check(declaration.kind == kind && declaration.symbol.classId == kind.classId) {
                        "The builtin symbol provider returned a declaration under the wrong std.core identity"
                    }
                    check(declaration.typeParameters.size == kind.typeParameterCount) {
                        "${kind.classId.asFqNameString()} has an invalid builtin type-parameter shape"
                    }
                    check(declaration.declarations.isEmpty() && declaration.superTypeRefs.isEmpty()) {
                        "A BuiltInDecl root cannot carry ordinary members or supertype edges"
                    }
                    declarationsByKind.putIfAbsent(kind, declaration)
                }
            }

            val missingKinds = CfirBuiltInTypeKind.entries.filterNot(declarationsByKind::containsKey)
            check(missingKinds.isEmpty()) {
                "std.core CJO self-build is missing builtin declaration roots: " +
                    missingKinds.joinToString { it.classId.asFqNameString() }
            }

            return CfirBuiltInTypeKind.entries.map { kind ->
                val declaration = checkNotNull(declarationsByKind[kind])
                declaration
            }
        }

    }

    private class Context(private val prepared: PreparedInputs) : CjoCanonicalSourceEncodingContext {
        private val files = prepared.files
        private val familySnapshot = prepared.family
        private val exportOptions = prepared.options
        override val sourceEncodingContract: CjoSourceEncodingContract get() = exportOptions.sourceEncodingContract
        private val frozenSources = prepared.sources
        private val sourceFormats = prepared.sourceFormats

        private data class ExternalCjoDeclarationReference(
            val packageName: String,
            val declarationKey: String,
        )

        private data class SourceDeclarationIdentity(
            val declarationType: String,
            val sourcePath: Path,
            val startOffset: Int,
            val endOffset: Int,
        )

        private data class SessionDeclarationIdentity(val symbol: CfirBasedSymbol<*>)

        private data class ExportNodeIdentity(
            val declaration: Any,
            val owner: ExportNodeIdentity?,
        )

        private val declarations = ArrayList<CfirDeclaration>()
        /** SOURCE 与正式 loaded 声明共用同一 append-only d 池，只有实际声明身份可登记。 */
        private val declarationTable = CjoGraphTable<CjoPackageDeclaration>("declaration")
        private val declarationIndex = IdentityHashMap<CfirDeclaration, UInt>()
        private val topLevel = IdentityHashMap<CfirDeclaration, Boolean>()
        private val declarationFiles = IdentityHashMap<CfirDeclaration, CfirFile>()
        private val declarationOwners = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        private val syntheticStdCoreBuiltins = IdentityHashMap<CfirBuiltInDeclaration, Boolean>()
        private var stdCoreBuiltinRoots: List<CfirBuiltInDeclaration> = emptyList()
        private val patternBindingOwners = IdentityHashMap<CfirPatternBindingVariable, CfirPatternVariable>()
        private val sourceFilesBySymbol = prepared.sourceFiles
        private val familyExportMembersByOwner = HashMap<ExportNodeIdentity, List<CfirDeclaration>>()
        private val fileIndices = IdentityHashMap<CfirFile, UInt>()
        private val packageFiles = ArrayList<String>()
        private val cjoFileIndices = HashMap<CjoSnapshotTableEntry, UInt>()
        private val loadedPackagesByPath = frozenSources.packages
        private val classIndex = linkedMapOf<ClassId, UInt>()
        private val externalImports = linkedSetOf<String>()
        private val typeTable = CjoGraphTable<CjoTypeMetadata>("type")
        /** Cone 对象只作为本代来源身份；表驻留键读取完整冻结 shape，不使用 Cone.equals。 */
        private val typeIndex = HashMap<FrozenTypeShape, UInt>()
        private val typeShapes = IdentityHashMap<CfirExportTypeElement, IdentityHashMap<TypeParameterProjection, FrozenTypeShape>>()
        private val sourceTypeIndices = IdentityHashMap<CfirExportTypeElement, IdentityHashMap<TypeParameterProjection, UInt>>()
        private val typeProjections = IdentityHashMap<CfirDeclaration, TypeParameterProjection>()
        private val emptyTypeProjection = TypeParameterProjection(emptyMap())
        private val genericTypeInputs = HashMap<GenericTypeAnchor, List<FrozenTypeShape>>()
        private val pendingGenericTypeInputs = HashSet<GenericTypeAnchor>()
        private val pendingGenericTypeBounds = ArrayDeque<PendingGenericTypeBounds>()
        private val specificSelfTypes = HashMap<FrozenTypeShape, FrozenTypeShape>()
        private var invalidTypeIndex: UInt? = null
        private val expressionTable = CjoGraphTable<CjoExpressionMetadata>("expression")
        private val expressionIndex = IdentityHashMap<CfirSourceExpressionIdentity, UInt>()
        private val filledExpressions = java.util.Collections.newSetFromMap(IdentityHashMap<CfirSourceExpressionIdentity, Boolean>())
        private val fillingExpressions = java.util.Collections.newSetFromMap(IdentityHashMap<CfirSourceExpressionIdentity, Boolean>())
        private val importedDeclarationIndices = HashMap<CjoSnapshotTableEntry, UInt>()
        private val commonCounterpartDeclarationIndices = HashMap<CjoSnapshotTableEntry, UInt>()
        private val valueTable = CjoGraphTable<CjoCompositeValueMetadata>("value")
        private val valueIndices = HashMap<org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantIdentity, UInt>()
        private val valueNodes = HashMap<org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantIdentity, org.cangnova.cangjie.cfir.resolve.providers.CfirExportConstantNode>()
        private val languageVersion = sourceFormats.languageVersion
        private var activeDeclaration: CfirDeclaration? = null
        private val exportAnalysis = prepared.analysis
        private val exportPlan = prepared.plan
        private val canonicalDefinitions = exportPlan.canonicalDefinitions
        private var activeExpression: CfirSourceExpressionIdentity? = null
        private val canonicalEncoder by lazy { CjoCanonicalSourceEncoder(exportAnalysis.canonicalSourceGraphs(), this) }

        /** 固定家族投影只重定位绑定；不能制造替换 Cone 后重新查询 provider 或类型展开。 */
        private class TypeParameterProjection(val parameters: Map<CfirTypeParameter, CfirTypeParameter>) {
            override fun equals(other: Any?): Boolean = other is TypeParameterProjection && parameters.size == other.parameters.size &&
                parameters.all { (source, target) -> other.parameters[source] === target }
            override fun hashCode(): Int = parameters.entries.sumOf { (source, target) ->
                System.identityHashCode(source) xor System.identityHashCode(target)
            }
        }

        private class TypeDeclarationIdentity(val declaration: CfirDeclaration) {
            override fun equals(other: Any?): Boolean = other is TypeDeclarationIdentity && declaration === other.declaration
            override fun hashCode(): Int = System.identityHashCode(declaration)
        }

        /** Generic 身份锚点保留捕获域和固定投影；其上界回边单独完成，不能递归进入驻留键。 */
        private data class GenericTypeAnchor(
            val parameter: TypeDeclarationIdentity,
            val domain: org.cangnova.cangjie.cfir.session.CfirSession,
            val projection: TypeParameterProjection,
        )

        private data class PendingGenericTypeBounds(
            val anchor: GenericTypeAnchor,
            val payload: CfirExportTypePayload.Generic,
            val context: CfirDeclaration,
            val projection: TypeParameterProjection,
        )

        /** Generic 的声明绑定是回环锚点；有序子 shape 与全部语义标记共同决定其它类型身份。 */
        private data class FrozenTypeShape(
            val kind: UShort,
            val declaration: TypeDeclarationIdentity? = null,
            val arguments: List<FrozenTypeShape> = emptyList(),
            val returnType: FrozenTypeShape? = null,
            val isThisType: Boolean = false,
            val isRefEnum: Boolean = false,
            val isC: Boolean = false,
            val isClosure: Boolean = false,
            val hasVariableLenArg: Boolean = false,
            val dimsOrSize: Long? = null,
            val genericAnchor: GenericTypeAnchor? = null,
            val serializedTarget: CjoFrozenDeclarationTarget? = null,
        )

        /**
         * 本次写出的是否为 CJMP common part cjo（官方 common part 编译 = CHIR 输出模式，
         * `ASTWriter` 此时写 `Package.options` 与 `FileInfo.feature`）。
         */
        private val writesCjmpCommonPart = sourceFormats.writesCjmpCommonPart

        fun produce(): CjoPackageMetadata {
            val packageName = sourceFormats.packageName
            sourceFormats.files.forEach { input ->
                fileIndices[input.file] = input.index
                packageFiles += input.name
            }
            registerDeclarationIdentities()
            indexCommonCounterpartDeclarationIndices()
            consumeSavePlan()
            check(declarations.size == declarationTable.size)
            val declarationMetadata = declarationTable.finish()
            val types = typeTable.finish()
            val expressions = expressionTable.finish()
            val values = valueTable.finish()
            check(pendingGenericTypeInputs.isEmpty()) { "CJO types cannot publish unfinished Generic bound inputs" }
            exportAnalysis.validateSourceUnchanged()
            sourceFormats.validateUnchanged()
            prepared.initializationInputs.validateUnchanged()
            prepared.publicationInputs.validateUnchanged()
            val module = sourceFormats.moduleName
            val schemaProfile = when {
                writesCjmpCommonPart -> CjoSchemaProfile.OFFICIAL_CJMP
                languageVersion >= LanguageVersion.CANGJIE_1_1_0 -> CjoSchemaProfile.OFFICIAL_V1_1_3
                else -> CjoSchemaProfile.OFFICIAL_V1_0_0
            }
            return CjoPackageMetadata(
                fullPackageName = packageName,
                moduleName = module,
                // native AST2FB写CANGJIE_VERSION，与二进制CjoVersion及语言选项各自独立。
                version = CangJieCompilerVersion.VERSION,
                schemaProfile = schemaProfile,
                // 官方 `ASTWriter::SaveOptions`：common part cjo 内嵌 debug 与优化级别，供 specific
                // 编译的加载门比对（`ASTLoader::ValidateOptions`）
                options = sourceFormats.moduleOptions,
                declarations = CjoSourceDeclarationKeys(
                    packageName = packageName,
                    declarations = declarationMetadata,
                    types = types,
                    imports = externalImports.toList(),
                    sourceFiles = declarations.mapNotNull { declaration ->
                        sourceFormats.declarations[declaration]?.sourcePath?.let { path ->
                            (declarationIndex.getValue(declaration).toInt() - 1) to path
                        }
                    }.toMap(),
                    sourceOffsets = declarations.mapNotNull { declaration ->
                        sourceFormats.declarations[declaration]?.sourceOffset?.let { offset ->
                            (declarationIndex.getValue(declaration).toInt() - 1) to offset
                        }
                    }.toMap(),
                    sourceDeclarations = declarations.filter { sourceFormats.declarations.containsKey(it) }
                        .mapTo(mutableSetOf()) { declarationIndex.getValue(it).toInt() - 1 },
                    localDeclarations = declarations.filter { (sourceFormats.declarations[it]?.isLocal ?: frozenSources.formats[it]?.isLocal) == true }
                        .mapTo(mutableSetOf()) { declarationIndex.getValue(it).toInt() - 1 },
                    owners = declarations.map { declaration ->
                        declarationOwners[declaration]?.let { owner ->
                            if (owner is CfirAnonymousFunction) null
                            else checkNotNull(declarationIndex[canonicalDeclaration(owner)]).toInt() - 1
                        }
                    },
                ).publish(),
                imports = externalImports.toList(),
                // Match ASTWriter::SaveFileInfo when absolute paths are not
                // requested: package identity plus the source-owned basename.
                // Do not serialize an absolute path or derive a name from PSI
                // text; CfirFile.name is shared by PSI and LightTree lowering.
                // 官方 `SaveFileInfo`：common part（serializingCommon）写源文件完整路径，specific 编译据此定位
                // common 方向诊断（G20）；其余情形写 package/basename
                allFiles = packageFiles,
                fileInfo = if (languageVersion >= LanguageVersion.CANGJIE_1_1_0) {
                    sourceFormats.files.mapNotNull { it.info }
                } else {
                    emptyList()
                },
                types = types,
                expressions = expressions,
                values = values,
                sourceEncodingContract = exportOptions.sourceEncodingContract,
            )
        }

        /** 编号归输出 Context；定义身份和拥有 parent 已由 Preparation 的计划封闭。 */
        private fun registerDeclarationIdentities() {
            stdCoreBuiltinRoots = prepared.builtins
            stdCoreBuiltinRoots.forEach { syntheticStdCoreBuiltins[it] = true }
            exportPlan.declarations.forEachIndexed { ordinal, declaration ->
                val index = ordinal.toUInt() + 1u
                check(declarationIndex.put(declaration, index) == null)
                declarations += declaration
                topLevel[declaration] = exportPlan.topLevel.getValue(declaration)
                declarationOwners[declaration] = exportPlan.parents.getValue(declaration)
                if (exportAnalysis.isSourceDeclaration(declaration)) {
                    declarationFiles[declaration] = checkNotNull(exportAnalysis.sourceFileOf(declaration))
                }
                frozenSources.formats[declaration]?.origin?.let { origin ->
                    check(importedDeclarationIndices.put(origin, index) == null)
                }
            }
        }

        /** 一个事件流完成 reserve/type/子槽/fill；填充调用不能扩大 d/e 输入。 */
        private fun consumeSavePlan() {
            exportPlan.events.forEach { event ->
                when (event) {
                    is CjoSourceSaveEvent.ReserveDeclaration ->
                        check(declarationTable.reserve() == declarationIndex.getValue(event.declaration))
                    is CjoSourceSaveEvent.Type -> type(event.use, event.context)
                    is CjoSourceSaveEvent.ReserveExpression -> {
                        check(!expressionIndex.containsKey(event.identity))
                        reserveExpression(event.identity)
                    }
                    is CjoSourceSaveEvent.SaveExpression -> {
                        check(activeExpression == null)
                        activeExpression = event.identity
                        try { canonicalEncoder.expression(event.identity) } finally { activeExpression = null }
                    }
                    is CjoSourceSaveEvent.SaveDeclaration -> {
                        val index = declarationIndex.getValue(event.declaration)
                        declarationTable.fill(index, declarationMetadata(event.declaration, index.toInt(),
                            topLevel.getValue(event.declaration)))
                    }
                    is CjoSourceSaveEvent.Constant -> reserveConstant(event.graph, event.context)
                }
            }
        }

        /**
         * 把 common CJO 中被 specific 声明实现的 wire identity 指向 specific 输出项。
         * 函数体、类型图和初始化依赖都会引用原 common allDecls，不能因 common 声明不导出而悬空。
         */
        private fun indexCommonCounterpartDeclarationIndices() {
            val family = familySnapshot ?: return
            for (specific in declarations) {
                val specificIndex = declarationIndex[canonicalDeclaration(specific)] ?: continue
                for (common in family.commonCounterpartsFor(specific)) {
                    indexCommonCounterpartDeclaration(common, specificIndex)
                }
                typeProjectionFor(specific).parameters.forEach { (common, specificParameter) ->
                    val specificParameterIndex = declarationIndex[canonicalDeclaration(specificParameter)]
                        ?: error("Specific owner type parameter is absent from the exported declaration graph")
                    indexCommonCounterpartDeclaration(common, specificParameterIndex)
                }
            }
        }

        private fun indexCommonCounterpartDeclaration(common: CfirDeclaration, specificIndex: UInt) {
            val key = cjoDeclarationOriginKey(common) ?: return
            val previous = commonCounterpartDeclarationIndices.putIfAbsent(key, specificIndex)
            require(previous == null || previous == specificIndex) {
                "A common CJO declaration maps to multiple specific output declarations: $key"
            }
        }

        /** 完成输入已固定 canonical declaration 和实际 CJO 来源，编码期只有 lookup。 */
        private fun canonicalDeclaration(declaration: CfirDeclaration): CfirDeclaration =
            frozenSources.canonicalDeclarations.getValue(declaration)

        private fun cjoDeclarationOriginKey(declaration: CfirDeclaration): CjoSnapshotTableEntry? =
            frozenSources.declarationOrigins[declaration]

        private fun externalCjoDeclarationReference(declaration: CfirDeclaration): ExternalCjoDeclarationReference {
            val value = frozenSources.externalDeclarations.getValue(canonicalDeclaration(declaration))
            return ExternalCjoDeclarationReference(value.packageName, value.declarationKey)
        }

        private fun frozenTarget(target: CjoFrozenDeclarationTarget): CjoExpressionTargetMetadata = when (target) {
            CjoFrozenDeclarationTarget.Invalid -> CjoExpressionTargetMetadata(pkgId = PackageIndex.INVALID.value)
            is CjoFrozenDeclarationTarget.Package -> CjoExpressionTargetMetadata(pkgId = PackageIndex.PACKAGE_REFERENCE.value,
                decl = target.packageName, index = target.index)
            is CjoFrozenDeclarationTarget.External -> CjoExpressionTargetMetadata(pkgId = externalPackageIndex(target.packageName), decl = target.declarationKey)
            is CjoFrozenDeclarationTarget.Current -> currentTarget(target.declaration)
        }

        private fun currentTarget(declaration: CfirDeclaration): CjoExpressionTargetMetadata {
            val canonical = canonicalDeclaration(declaration)
            val index = declarationIndex[canonical] ?: cjoDeclarationOriginKey(canonical)?.let {
                commonCounterpartDeclarationIndices[it] ?: importedDeclarationIndices[it]
            } ?: error("A current reference has no selected canonical declaration")
            return CjoExpressionTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = index)
        }

        private fun CjoExpressionTargetMetadata.toCjoAnnotationTargetMetadata(): CjoAnnotationTargetMetadata =
            CjoAnnotationTargetMetadata(pkgId = pkgId, decl = decl, index = index)

        private fun externalPackageIndex(packageName: String): Int {
            externalImports += packageName
            return externalImports.indexOf(packageName)
        }

        private fun frozenPosition(position: CjoFrozenSourcePosition?): CjoPositionMetadata? = position?.let {
            val file = it.file?.let { identity -> cjoFileIndices[identity] ?: run {
                val index = packageFiles.size.toUInt() + 1u
                packageFiles += frozenSources.fileNames.getValue(identity)
                cjoFileIndices[identity] = index
                index
            } } ?: it.savedFile
            CjoPositionMetadata(file = file,
                pkgId = it.importedPackage?.let { packageName -> externalPackageIndex(packageName).toUInt() + 1u } ?: 0u,
                line = it.line, column = it.column, ignore = it.ignore)
        }
        private fun declarationPositions(declaration: CfirDeclaration): Pair<CjoPositionMetadata?, CjoPositionMetadata?> {
            frozenSources.formats[declaration]?.let { return frozenPosition(it.begin) to frozenPosition(it.end) }
            val input = sourceFormats.declarations.getValue(declaration)
            return input.begin to input.end
        }

        private fun simplePatternOwner(binding: CfirPatternBindingVariable): CfirPatternVariable? {
            return exportAnalysis.simplePatternOwnerOf(binding)
        }

        /** 原名称 token 的位置已在 Preparation 捕获，编码不能再读 PSI/LightTree。 */
        private fun declarationIdentifierPosition(declaration: CfirDeclaration): CjoPositionMetadata? =
            frozenSources.formats[declaration]?.let { frozenPosition(it.identifierPosition) }
                ?: sourceFormats.declarations[declaration]?.identifierPosition

        /** specific 导出使用 P3 最终成员图，确保未遮蔽的 common 成员进入容器 body。 */

        private fun declaredMembers(owner: CfirDeclaration): List<CfirDeclaration> = exportAnalysis.memberDeclarationsOf(owner)

        /** 最终 Class/Struct/Enum/Interface/Extend body，仅由原生模式与本次 linkage 结果选择。 */
        private fun bodyMembers(owner: CfirDeclaration): List<CfirDeclaration> =
            exportPlan.bodyMembers.getValue(owner)

        private fun genericMetadata(declaration: CfirDeclaration): CjoGenericMetadata? {
            if (frozenSources.formats.containsKey(declaration)) return frozenSources.formats.getValue(declaration).generic?.let { generic ->
                CjoGenericMetadata(generic.parameters.map(::ref), generic.constraints.map { constraint ->
                    CjoConstraintMetadata(type(constraint.type, declaration), constraint.upperBounds.map { type(it, declaration) },
                        constraint.isImplicitlyIntroduced, frozenPosition(constraint.begin), frozenPosition(constraint.end))
                })
            }
            val hasGenericMetadata = exportAnalysis.hasOwnGenericNode(declaration) ||
                (declaration is CfirBuiltInDeclaration && exportAnalysis.typeParametersOf(declaration).isNotEmpty())
            if (!hasGenericMetadata || declaration is CfirConstructor || declaration is CfirEnumConstructor) return null
            val parameters = when (declaration) {
                is CfirClassLikeDeclaration -> exportAnalysis.typeParametersOf(declaration)
                is CfirConstructor -> emptyList()
                is CfirFunction -> exportAnalysis.typeParametersOf(declaration)
                is CfirExtend -> exportAnalysis.typeParametersOf(declaration)
                else -> emptyList()
            }
            return CjoGenericMetadata(
                typeParameters = parameters.map(::ref),
                constraints = parameters.mapNotNull { parameter ->
                    val upperBounds = exportAnalysis.explicitUpperBoundsOf(parameter).map { type(it, declaration) }
                    if (upperBounds.isEmpty()) {
                        null
                    } else {
                        CjoConstraintMetadata(
                            type = capturedDeclarationTypeReference(parameter),
                            upperBounds = upperBounds,
                        )
                    }
                },
            )
        }

        private fun declarationMetadata(
            declaration: CfirDeclaration,
            index: Int,
            topLevel: Boolean,
        ): CjoPackageDeclaration {
            val previousDeclaration = activeDeclaration
            val rawDeclaration = frozenSources.formats[declaration]
            activeDeclaration = declaration
            return try {
                val name = declarationName(declaration)
                val kind = declarationKind(declaration)
                val info = declarationInfo(declaration)
                val (begin, end) = declarationPositions(declaration)
                CjoPackageDeclaration(
                    identifier = name,
                    kind = kind,
                    isTopLevel = topLevel,
                    fullPackageName = rawDeclaration?.fullPackageName,
                    exportId = rawDeclaration?.exportId,
                    mangledName = rawDeclaration?.mangledName,
                    mangledBeforeSema = rawDeclaration?.mangledBeforeSema,
                    declarationHash = rawDeclaration?.hash,
                    genericDecl = rawDeclaration?.genericDecl?.let { genericDecl ->
                        frozenTarget(genericDecl).toCjoAnnotationTargetMetadata()
                    },
                    type = declarationType(declaration),
                    begin = begin,
                    end = end,
                    identifierPosition = declarationIdentifierPosition(declaration),
                    generic = genericMetadata(declaration),
                    attributes = prepared.attributeInputs.attributesOf(declaration).words,
                    annotations = annotations(declaration),
                    info = info,
                    dependencies = initializationDependencyMetadata(declaration),
                )
            } finally {
                activeDeclaration = previousDeclaration
            }
        }

        /** 参数、accessor 与泛型子声明继承其物理容器成员的 common → specific owner 映射。 */
        private fun typeProjectionFor(declaration: CfirDeclaration): TypeParameterProjection = typeProjections.getOrPut(declaration) {
            val mapping = familySnapshot?.ownerTypeParameterMappingForDescendant(
                declaration,
                generateSequence(exportAnalysis.ownerOf(declaration)) { exportAnalysis.ownerOf(it) },
            ).orEmpty()
            if (mapping.isEmpty()) emptyTypeProjection else TypeParameterProjection(
                java.util.Collections.unmodifiableMap(IdentityHashMap<CfirTypeParameter, CfirTypeParameter>().apply { putAll(mapping) }),
            )
        }

        /** 与规划器使用同一冻结依赖和 planned 集合，填充期只投影已预留 FullId。 */
        private fun initializationDependencyMetadata(declaration: CfirDeclaration): List<CjoAnnotationTargetMetadata> {
            if (!writesCjmpCommonPart && !frozenSources.formats.containsKey(declaration)) return emptyList()
            return prepared.dependencyInputs.plannedTargets(declaration, declarationIndex.keys).map {
                frozenTarget(it).toCjoAnnotationTargetMetadata()
            }
        }

        private fun declarationName(declaration: CfirDeclaration): String =
            frozenSources.formats[declaration]?.identifier ?: sourceFormats.declarations.getValue(declaration).identifier

        private fun declarationKind(declaration: CfirDeclaration): UShort =
            frozenSources.formats[declaration]?.kind ?: sourceFormats.declarations.getValue(declaration).kind

        private fun declarationType(declaration: CfirDeclaration): UInt = if (prepared.attributeInputs.attributesOf(declaration).omitsType) 0u else if (frozenSources.formats.containsKey(declaration)) frozenSources.formats.getValue(declaration).type?.let { type(it, declaration) } ?: 0u else when (declaration) {
            // 声明self type由语义输入一次捕获；encoder不能另造丢失自身T实参的bare类型。
            is CfirBuiltInDeclaration, is CfirClass, is CfirInterface, is CfirStruct, is CfirEnum -> capturedDeclarationTypeReference(declaration)
            // TypeAliasDecl 的 self 是原生 Type；RHS 独立写入 AliasInfo.aliasedTy。
            is CfirTypeAlias -> capturedDeclarationTypeReference(declaration)
            is CfirExtend -> capturedDeclarationTypeReference(declaration)
            is CfirTypeParameter -> capturedDeclarationTypeReference(declaration)
            is CfirEnumConstructor -> capturedDeclarationTypeReference(declaration)
            is CfirFunction -> capturedDeclarationTypeReference(declaration)
            is CfirCallableDeclaration -> capturedDeclarationTypeReference(declaration)
            else -> unsupported("missing declaration type")
        }

        private fun capturedDeclarationTypeReference(declaration: CfirDeclaration): UInt = type(
            exportAnalysis.typeUseGraph().declarationType(declaration) ?: unsupported("A declaration has no captured completed type use"),
            declaration,
        )


        private fun typeOf(ref: CfirTypeRef): ConeCangJieType =
            exportAnalysis.sourceElement(ref).resolvedType ?: unsupported("unresolved captured CFIR type reference")

        /** F header 与本次 body 选择分开，SOURCE/loaded 都调用同一 canonical encoder。 */
        private fun sourceFunctionBodyInfo(function: CfirCallableDeclaration): CjoFunctionBodyInfo {
            if (function is CfirFunction && exportPlan.isPartialFunction(function)) return CjoFunctionBodyInfo()
            val header = frozenSources.formats[function]?.function?.header
            val selected = exportPlan.sourceContent(function)?.root
            val parameters = exportAnalysis.valueParametersOf(function)
            val signature = header?.signature ?: checkNotNull(exportAnalysis.declarationTypeOf(function)).let { type ->
                if (type is ConeFunctionType) CfirSourceCallableSignature.Resolved(type.returnType)
                else {
                    check(function is CfirEnumConstructor) { "A captured function signature must be FuncTy" }
                    CfirSourceCallableSignature.Resolved(type)
                }
            }
            return canonicalEncoder.functionBody(CfirSourceFunctionBody(function, parameters,
                signature,
                selected, header?.captureKind ?: 0, header?.always ?: false, header))
        }

        private fun declarationInitializer(variable: CfirVariable): UInt {
            val selected = exportPlan.sourceContent(variable)?.root
            val owning = if (variable in canonicalDefinitions && exportAnalysis.hasCanonicalSourceOf(variable))
                exportAnalysis.canonicalSourceGraphOf(variable).initializers[variable] else null
            return (owning ?: selected)?.let(canonicalEncoder::expression) ?: 0u
        }

        private fun declarationInfo(declaration: CfirDeclaration): CjoDeclarationInfo? {
            val format = frozenSources.formats[declaration]
            if (format?.infoType == PackageFormat.DeclInfo.NONE) return null
            fun inherited(): List<UInt> = exportAnalysis.typeUseGraph().superTypes(declaration).map { type(it, declaration) }
            fun functionInfo(function: CfirCallableDeclaration): CjoFunctionInfo {
                val info = format?.function
                return CjoFunctionInfo(
                    overflowStrategy = sourceFormats.declarations[declaration]?.overflow,
                    operation = info?.operation ?: 0u,
                    body = sourceFunctionBodyInfo(function),
                    hasFunctionBody = info?.hasFunctionBody ?: true,
                    autoDiff = info?.autoDiff,
                    isConst = info?.isConst ?: exportAnalysis.statusOf(declaration).isConst,
                    isInline = exportAnalysis.precedingFactsOf(declaration).isInline && !exportAnalysis.isInInstantiatedDeclaration(declaration),
                    isFastNative = info?.isFastNative ?: sourceFormats.declarations.getValue(declaration).fastNative,
                    encodedOverflowPolicy = info?.overflowPolicy,
                )
            }
            return when (declaration) {
                is CfirBuiltInDeclaration -> CjoBuiltInInfo(checkNotNull(format?.builtin ?: sourceFormats.declarations.getValue(declaration).builtin).toCjoBuiltInType())
                is CfirEnumConstructor -> if (declarationKind(declaration) == DeclKind.VarDecl)
                    CjoVariableInfo(isVar = false, isConst = false) else functionInfo(declaration)
                is CfirFunction -> functionInfo(declaration)
                is CfirTypeAlias -> CjoAliasInfo(type(exportAnalysis.typeUseGraph().aliasRightHandSide(declaration), declaration))
                is CfirClass -> CjoClassInfo(
                    inheritedTypes = inherited(), body = bodyMembers(declaration).map(::ref),
                    autoDiff = format?.nominal?.autoDiff,
                    isAnnotation = format?.nominal?.isAnnotation ?: sourceFormats.declarations.getValue(declaration).isAnnotation,
                    annotationTargets = format?.nominal?.annotationTargets ?: sourceFormats.declarations.getValue(declaration).annotationTargets,
                    annotationTargets2 = format?.nominal?.annotationTargets2 ?: sourceFormats.declarations.getValue(declaration).annotationTargets2,
                    runtimeVisible = format?.nominal?.runtimeVisible ?: sourceFormats.declarations.getValue(declaration).runtimeVisible,
                )
                is CfirInterface -> CjoInterfaceInfo(inherited(), bodyMembers(declaration).map(::ref))
                is CfirStruct -> CjoStructInfo(inherited(), bodyMembers(declaration).map(::ref), format?.nominal?.autoDiff)
                is CfirEnum -> CjoEnumInfo(
                    inheritedTypes = inherited(), body = bodyMembers(declaration).filterNot { it is CfirConstructor && it.isPrimary }.map(::ref),
                    autoDiff = format?.nominal?.autoDiff,
                    hasArguments = format?.nominal?.hasArguments ?: declaredMembers(declaration).filterIsInstance<CfirEnumConstructor>().any { exportAnalysis.parameterCountOf(it) != 0 },
                    nonExhaustive = format?.nominal?.nonExhaustive ?: sourceFormats.declarations.getValue(declaration).nonExhaustive,
                    ellipsisPosition = frozenPosition(format?.nominal?.ellipsis),
                )
                is CfirProperty -> CjoPropertyInfo(
                    isConst = format?.isConst ?: exportAnalysis.statusOf(declaration).isConst,
                    isMutable = format?.isMutable ?: sourceFormats.declarations.getValue(declaration).isMutable,
                    getters = if (exportOptions.analysisSettings.compileCjd) emptyList() else
                        (format?.getters ?: sourceFormats.declarations.getValue(declaration).getters).map(::ref),
                    setters = if (exportOptions.analysisSettings.compileCjd) emptyList() else
                        (format?.setters ?: sourceFormats.declarations.getValue(declaration).setters).map(::ref),
                )
                is CfirValueParameter -> CjoParameterInfo(
                    isNamed = format?.isNamedParameter ?: sourceFormats.declarations.getValue(declaration).isNamed,
                    isMember = format?.isMemberParameter ?: sourceFormats.declarations.getValue(declaration).isMember,
                    defaultValue = exportPlan.sourceContent(declaration)?.root?.let(canonicalEncoder::expression) ?: 0u,
                )
                is CfirTypeParameter -> null
                is CfirVariable -> if (declaration is CfirPatternVariable && format?.kind != DeclKind.VarDecl) {
                    check(exportAnalysis.hasCanonicalSourceOf(declaration) && declaration in canonicalDefinitions)
                    CjoVarWithPatternInfo(format?.isVar ?: sourceFormats.declarations.getValue(declaration).isVar, format?.isConst ?: exportAnalysis.statusOf(declaration).isConst,
                        canonicalEncoder.declarationPattern(declaration), declarationInitializer(declaration))
                } else CjoVariableInfo(
                    isVar = format?.isVar ?: sourceFormats.declarations.getValue(declaration).isVar,
                    isConst = format?.isConst ?: exportAnalysis.statusOf(declaration).isConst,
                    isMemberParam = format?.isMemberParameter ?: sourceFormats.declarations.getValue(declaration).isMember,
                    initializer = declarationInitializer(declaration),
                    constValue = if (hasValueSlot(declaration)) constantValue(
                        checkNotNull(exportAnalysis.constantValueOf(declaration)) { "A selected value slot has no completed typed constant" }, declaration) else null,
                )
                is CfirExtend -> CjoExtendInfo(inherited(), bodyMembers(declaration).map(::ref))
                else -> null
            }
        }

        private fun CfirBuiltInTypeKind.toCjoBuiltInType(): UByte = when (this) {
            CfirBuiltInTypeKind.ARRAY -> PackageFormat.BuiltInType.Array
            CfirBuiltInTypeKind.VARRAY -> PackageFormat.BuiltInType.VArray
            CfirBuiltInTypeKind.CPOINTER -> PackageFormat.BuiltInType.CPointer
            CfirBuiltInTypeKind.CSTRING -> PackageFormat.BuiltInType.CString
            CfirBuiltInTypeKind.CFUNC -> PackageFormat.BuiltInType.CFunc
        }


        private fun ref(declaration: CfirDeclaration): UInt {
            val canonicalDeclaration = canonicalDeclaration(declaration)
            return declarationIndex[canonicalDeclaration]
                ?: error("CJO declaration reference was not indexed: ${declarationName(canonicalDeclaration)}")
        }

        /** CJO desugar 引用只能指向当前 source package 收录的 helper。 */
        private fun defaultHelperOf(parameter: CfirValueParameter): CfirNamedFunction? {
            if (exportAnalysis.isSourceDeclaration(parameter)) return exportAnalysis.defaultHelperOf(parameter) as? CfirNamedFunction
            val owner = checkNotNull(exportAnalysis.ownerOf(parameter))
            val header = if (exportAnalysis.hasCanonicalSourceOf(owner))
                exportAnalysis.canonicalSourceGraphOf(owner).functionHeaders[owner] else frozenSources.formats[owner]?.function?.header
            if (header == null) return null
            header.parameterLists.zip(header.desugaredParameterLists).forEach { (parameters, helpers) ->
                val index = parameters.indexOfFirst { it === parameter }
                if (index >= 0) return if (helpers.isEmpty()) null else helpers[index]
            }
            error("A saved parameter has no actual FuncBody group")
        }

        private fun defaultHelperReference(parameter: CfirValueParameter): UInt = defaultHelperOf(parameter)?.let(::ref) ?: 0u

        private fun canonicalAnnotations(declaration: CfirDeclaration): List<CfirSourceAnnotation> {
            val owner = patternMetadataOwner(declaration)
            if (exportAnalysis.hasCanonicalSourceOf(owner)) return exportAnalysis.canonicalSourceGraphOf(owner).annotations.getValue(owner)
            check(exportAnalysis.annotationsOf(owner).isEmpty()) { "Annotation content requires its completed canonical graph" }
            return emptyList()
        }

        private fun annotations(declaration: CfirDeclaration): List<CjoAnnotationMetadata> =
            canonicalEncoder.annotations(patternMetadataOwner(declaration), canonicalAnnotations(declaration))

        private fun patternMetadataOwner(declaration: CfirDeclaration): CfirDeclaration =
            (declaration as? CfirPatternBindingVariable)?.let(::simplePatternOwner) ?: declaration

        override fun reserveExpression(identity: CfirSourceExpressionIdentity): UInt =
            expressionIndex[identity] ?: expressionTable.reserve().also { expressionIndex[identity] = it }

        override fun expressionIndex(identity: CfirSourceExpressionIdentity): UInt =
            expressionIndex.getValue(identity)

        override fun beginExpression(identity: CfirSourceExpressionIdentity): Boolean {
            if (identity in filledExpressions) return false
            check(activeExpression === identity) { "Expression fill attempted to discover a child outside the completed save plan" }
            check(fillingExpressions.add(identity)) { "Canonical SOURCE has a cyclic owning edge" }
            return true
        }

        override fun fillExpression(index: UInt, metadata: CjoExpressionMetadata) {
            expressionTable.fill(index, metadata)
            val identity = expressionIndex.entries.single { it.value == index }.key
            check(fillingExpressions.remove(identity) && filledExpressions.add(identity))
        }

        override fun declarationReference(declaration: CfirDeclaration): UInt = ref(declaration)
        override fun declarationTarget(declaration: CfirDeclaration, kind: CfirSourceReferenceKind): CjoExpressionTargetMetadata =
            when (kind) {
                CfirSourceReferenceKind.CURRENT -> currentTarget(declaration)
                CfirSourceReferenceKind.IMPORTED -> frozenTarget(frozenSources.externalDeclarations.getValue(canonicalDeclaration(declaration)))
                CfirSourceReferenceKind.SOURCE -> sourceExpressionTarget(declaration)
            }
        override fun ownedDeclarationTarget(declaration: CfirDeclaration): CjoExpressionTargetMetadata = currentTarget(declaration)
        override fun savedReferenceTarget(reference: CfirSourceSavedReference): CjoExpressionTargetMetadata? {
            val saved = frozenSources.savedReferences.getValue(reference)
            val target = saved.target ?: return null
            val encoded = frozenTarget(target)
            return if (target is CjoFrozenDeclarationTarget.Current) encoded.copy(decl = saved.declarationKey)
                else encoded.copy(decl = saved.declarationKey, index = saved.index)
        }
        override fun typeReference(type: ConeCangJieType, context: CfirDeclaration): UInt = type(type, context)
        override fun typeReference(ref: CfirTypeRef, context: CfirDeclaration): UInt = type(exportAnalysis.typeUseGraph().reference(ref), context)
        override fun functionReturnTypeReference(function: CfirCallableDeclaration): UInt = when (function) {
            is CfirFunction -> exportAnalysis.typeUseGraph().functionReturn(function)?.let { type(it, function) } ?: 0u
            is CfirEnumConstructor -> exportAnalysis.typeUseGraph().enumConstructorReturn(function)?.let { type(it, function) } ?: 0u
            else -> error("A saved F requires its actual callable owner")
        }
        override fun invalidTypeReference(): UInt = type(FrozenTypeShape(PackageFormat.TypeKind.Invalid))
        override fun parameterDesugar(parameter: CfirValueParameter): UInt = defaultHelperReference(parameter)
        override fun positions(context: CfirDeclaration, span: CfirSourceSpan?, imported: CfirSourceImportedPosition?): Pair<CjoPositionMetadata?, CjoPositionMetadata?> {
            if (imported != null) return imported.beginIdentity?.let { frozenPosition(frozenSources.positions.getValue(it)) } to
                imported.endIdentity?.let { frozenPosition(frozenSources.positions.getValue(it)) }
            return sourceFormats.positions(context, span)
        }
        override fun annotationArgumentName(name: org.cangnova.cangjie.name.Name?, owner: CfirDeclaration): String? =
            if (exportAnalysis.isSourceDeclaration(owner)) name?.asString() ?: "" else name?.asString()

        override fun constantValues(pattern: CfirSourcePattern, owner: CfirDeclaration): List<CjoConstValueInfo> {
            if (pattern.constantValues.isEmpty()) return emptyList()
            check(exportOptions.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA) {
                "Fixed native SOURCE does not produce Pattern.values"
            }
            return exportAnalysis.constantValuesOf(pattern).map { constantValue(it, owner) }
        }

        /** 保存槽是原输入/显式 schema 合同的事实；native SOURCE 不制造值池。 */
        private fun hasValueSlot(variable: CfirVariable): Boolean {
            val saved = frozenSources.formats[variable]
            if (saved != null) {
                if (saved.hasConstantValue) check(sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA) {
                    "A saved VarInfo value requires explicit typed-schema re-export"
                }
                return saved.hasConstantValue
            }
            return sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA && exportAnalysis.constantValueOf(variable) != null
        }

        private fun reserveConstant(graph: CfirExportConstantGraph, owner: CfirDeclaration) {
            graph.nodes.forEach { node ->
                val previous = valueNodes.putIfAbsent(node.identity, node)
                check(previous == null || previous == node) { "A frozen constant identity has multiple records" }
            }
            val visited = HashSet<CfirExportConstantIdentity>()
            fun visit(identity: CfirExportConstantIdentity) {
                if (!visited.add(identity)) return
                val node = valueNodes.getValue(identity)
                when (val storage = frozenSources.constants.getValue(identity)) {
                    is CjoFrozenConstantStorage.Scalar -> Unit
                    CjoFrozenConstantStorage.Array -> constantChildren(node).forEach(::visit)
                    is CjoFrozenConstantStorage.Composite -> {
                        if (!valueIndices.containsKey(identity)) valueIndices[identity] = valueTable.reserve() - 1u
                        storage.fields.forEach { visit(it.value) }
                    }
                }
            }
            check(exportAnalysis.containsDeclaration(owner))
            visit(graph.root)
        }

        private fun constantChildren(node: CfirExportConstantNode): List<CfirExportConstantIdentity> = when (node) {
            is CfirExportConstantNode.Tuple -> node.elements
            is CfirExportConstantNode.Array -> node.elements
            is CfirExportConstantNode.VArray -> node.elements
            else -> error("ArrayValue storage requires its actual ordered element product")
        }

        private val filledValues = HashSet<CfirExportConstantIdentity>()
        private val fillingValues = HashSet<CfirExportConstantIdentity>()

        private fun constantValue(graph: CfirExportConstantGraph, owner: CfirDeclaration): CjoConstValueInfo {
            reserveConstant(graph, owner)
            fun value(identity: CfirExportConstantIdentity): CjoConstValueInfo {
                val node = valueNodes.getValue(identity)
                return when (val storage = frozenSources.constants.getValue(identity)) {
                    is CjoFrozenConstantStorage.Scalar -> storage.value
                    CjoFrozenConstantStorage.Array -> CjoArrayConstValue(constantChildren(node).map(::value))
                    is CjoFrozenConstantStorage.Composite -> {
                        val index = valueIndices.getValue(identity)
                        if (identity !in filledValues && fillingValues.add(identity)) {
                            val fields = storage.fields.map { field ->
                                CjoCompositeValueFieldMetadata(field.name, type(CfirExportTypeUse.Semantic(exportAnalysis.typeElement(field.type, storage.domain)), owner), value(field.value))
                            }
                            valueTable.fill(index + 1u, CjoCompositeValueMetadata(type(CfirExportTypeUse.Semantic(exportAnalysis.typeElement(node.type, storage.domain)), owner), fields))
                            check(fillingValues.remove(identity) && filledValues.add(identity))
                        }
                        CjoCompositeConstValue(index)
                    }
                }
            }
            return value(graph.root)
        }

        /**
         * 与官方 `ASTWriter::GetFullDeclIndex` 一致，将已解析引用分类为当前包、导入包或 INVALID。
         * 已解析但无法落入这三类的声明必须使 CJO 写出失败，不能静默丢失符号身份。
         */
        private fun sourceExpressionTarget(declaration: CfirDeclaration?): CjoExpressionTargetMetadata {
            declaration ?: return CjoExpressionTargetMetadata(pkgId = PackageIndex.INVALID.value)
            val canonicalDeclaration = canonicalDeclaration(declaration)
            if (exportAnalysis.isDoNotExport(declaration) || familySnapshot?.doNotExport?.contains(declaration.symbol) == true) {
                return CjoExpressionTargetMetadata(pkgId = PackageIndex.INVALID.value)
            }

            // IMPORTED 是引用身份，不因重导出正文先登记了本地声明槽而改变；对位 GetFullDeclIndex。
            if (exportAnalysis.precedingFactsOf(canonicalDeclaration).isImported) {
                val externalReference = externalCjoDeclarationReference(canonicalDeclaration)
                return CjoExpressionTargetMetadata(
                    pkgId = externalPackageIndex(externalReference.packageName),
                    decl = externalReference.declarationKey,
                )
            }

            val index = declarationIndex[canonicalDeclaration]
                ?: cjoDeclarationOriginKey(canonicalDeclaration)?.let { origin ->
                    commonCounterpartDeclarationIndices[origin] ?: importedDeclarationIndices[origin]
                }
            if (index != null) {
                return CjoExpressionTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = index)
            }

            val origin = cjoDeclarationOriginKey(canonicalDeclaration)
            if (origin != null) {
                val externalReference = externalCjoDeclarationReference(canonicalDeclaration)
                return CjoExpressionTargetMetadata(
                    pkgId = externalPackageIndex(externalReference.packageName),
                    decl = externalReference.declarationKey,
                )
            }

            unsupported(
                "Resolved CJO expression target is outside the emitted declaration graph: " +
                    declarationName(canonicalDeclaration),
            )
        }

        private fun type(type: ConeCangJieType, context: CfirDeclaration): UInt {
            val input = exportAnalysis.typeElement(type, context)
            val projection = typeProjectionFor(context)
            val indices = sourceTypeIndices.getOrPut(input) { IdentityHashMap() }
            indices[projection]?.let { return it }
            val shape = frozenTypeShape(input, context, projection)
            prepareGenericTypeBounds()
            val index = type(shape)
            indices[projection] = index
            return index
        }

        /** 节点用途在 providers 已裁决；这里仅消费冻结图并应用当前 owner 的固定 TP 投影。 */
        private fun type(use: CfirExportTypeUse, context: CfirDeclaration): UInt {
            val shape = frozenTypeUseShape(use, context, typeProjectionFor(context))
            prepareGenericTypeBounds()
            return type(shape)
        }

        private fun frozenTypeUseShape(
            use: CfirExportTypeUse,
            context: CfirDeclaration,
            projection: TypeParameterProjection,
        ): FrozenTypeShape = when (use) {
            is CfirExportTypeUse.Semantic -> frozenTypeShape(use.type, context, projection)
            is CfirExportTypeUse.Arguments -> {
                val base = frozenTypeShape(use.type, context, projection)
                val arguments = java.util.Collections.unmodifiableList(use.arguments.map { frozenTypeUseShape(it, context, projection) })
                // Native SubstituteTypeArgs→GetArrayTy在选择后再次归一叶子与累计维度。
                if (base.kind == PackageFormat.TypeKind.Array && arguments.single().kind == PackageFormat.TypeKind.Array) {
                    val element = arguments.single()
                    base.copy(arguments = element.arguments, dimsOrSize = Math.addExact(checkNotNull(base.dimsOrSize), checkNotNull(element.dimsOrSize)))
                } else base.copy(arguments = arguments)
            }
            is CfirExportTypeUse.Function -> frozenTypeShape(use.type, context, projection).copy(
                arguments = java.util.Collections.unmodifiableList(use.parameters.map { frozenTypeUseShape(it, context, projection) }),
                returnType = frozenTypeUseShape(use.returnType, context, projection),
                isC = use.isC,
            )
            CfirExportTypeUse.Invalid -> FrozenTypeShape(PackageFormat.TypeKind.Invalid)
        }

        private fun frozenTypeShape(
            type: ConeCangJieType,
            context: CfirDeclaration,
            projection: TypeParameterProjection,
            visiting: MutableSet<CfirExportTypeElement> = java.util.Collections.newSetFromMap(IdentityHashMap()),
        ): FrozenTypeShape = frozenTypeShape(exportAnalysis.typeElement(type, context), context, projection, visiting)

        /** 只遍历冻结角色列表；Generic 以投影后的实际参数身份结束 shape 递归。 */
        private fun frozenTypeShape(
            input: CfirExportTypeElement,
            context: CfirDeclaration,
            projection: TypeParameterProjection,
            visiting: MutableSet<CfirExportTypeElement> = java.util.Collections.newSetFromMap(IdentityHashMap()),
        ): FrozenTypeShape {
            val shapes = typeShapes.getOrPut(input) { IdentityHashMap() }
            shapes[projection]?.let { return it }
            // self 关系只登记双身份；是否复用已有 specific 槽由 reserve/fill owner 按 native 顺序裁决。
            var specificSelf: CfirDeclaration? = null
            val nominal = input.payload as? CfirExportTypePayload.Nominal
            if (nominal != null && nominal.kind in setOf(CfirExportNominalTypeKind.CLASS,
                    CfirExportNominalTypeKind.STRUCT, CfirExportNominalTypeKind.ENUM) && !nominal.isThisType) {
                val declaration = nominal.declaration
                val ownParameters = declaration?.let(exportAnalysis::typeParametersOf).orEmpty()
                val declaredPayload = declaration?.let(exportAnalysis::declarationTypeOf)?.let {
                    exportAnalysis.typeElement(it, checkNotNull(declaration)).payload as? CfirExportTypePayload.Nominal
                }
                // native TypeManager 驻留使同 classifier、相同有序 own TP 的 normal 类型就是 decl.ty；
                // CFIR 的多个 Cone 对象以这些完整冻结绑定确认同一 self，绝不使用名字或 Cone.equals。
                val hasSelfBindings = declaredPayload != null && declaredPayload.kind == nominal.kind &&
                    declaredPayload.isRefEnum == nominal.isRefEnum && nominal.arguments.size == ownParameters.size &&
                    nominal.arguments.indices.all { index ->
                        (exportAnalysis.typeElement(nominal.arguments[index], input.domain).payload as? CfirExportTypePayload.Generic)
                            ?.parameter === ownParameters[index]
                    }
                if (declaration != null && (exportAnalysis.declarationTypeOf(declaration) === input.identity || hasSelfBindings)) {
                    val bindings = familySnapshot?.specificBindingsFor(declaration).orEmpty()
                    if (bindings.isNotEmpty()) {
                        check(bindings.size == 1)
                        specificSelf = bindings.single()
                    }
                }
            }
            val genericPayload = input.payload as? CfirExportTypePayload.Generic
            if (genericPayload != null) {
                val payload = genericPayload
                val anchor = GenericTypeAnchor(
                    TypeDeclarationIdentity(projection.parameters[payload.parameter] ?: payload.parameter), input.domain, projection,
                )
                val shape = FrozenTypeShape(PackageFormat.TypeKind.Generic, declaration = anchor.parameter, genericAnchor = anchor)
                // 结构 DFS 只登记锚点，上界在独立队列中准备，I<T> -> T -> I<T> 不成为结构环。
                shapes[projection] = shape
                if (anchor !in genericTypeInputs && pendingGenericTypeInputs.add(anchor)) {
                    pendingGenericTypeBounds.addLast(PendingGenericTypeBounds(anchor, payload, context, projection))
                }
                return shape
            }
            check(visiting.add(input)) { "A completed type has a structural cycle outside its Generic binding" }
            fun child(value: ConeCangJieType): FrozenTypeShape =
                frozenTypeShape(exportAnalysis.typeElement(value, input.domain), context, projection, visiting)
            fun arguments(values: List<ConeCangJieType>): List<FrozenTypeShape> =
                java.util.Collections.unmodifiableList(values.map(::child))
            val shape = when (val payload = input.payload) {
                CfirExportTypePayload.Invalid -> FrozenTypeShape(PackageFormat.TypeKind.Invalid)
                is CfirExportTypePayload.Primitive -> FrozenTypeShape(payload.kind.toCjoTypeKind())
                is CfirExportTypePayload.Nominal -> FrozenTypeShape(
                    kind = when (payload.kind) {
                        CfirExportNominalTypeKind.CLASS -> PackageFormat.TypeKind.Class
                        CfirExportNominalTypeKind.INTERFACE -> PackageFormat.TypeKind.Interface
                        CfirExportNominalTypeKind.STRUCT -> PackageFormat.TypeKind.Struct
                        CfirExportNominalTypeKind.ENUM -> PackageFormat.TypeKind.Enum
                    },
                    declaration = TypeDeclarationIdentity(checkNotNull(payload.declaration) {
                        "External CJO type has no class-like declaration: ${payload.classId.asFqNameString()}"
                    }),
                    arguments = arguments(payload.arguments),
                    isThisType = payload.isThisType,
                    isRefEnum = payload.isRefEnum,
                    serializedTarget = frozenSources.typeTarget(input.identity, input.domain),
                )
                is CfirExportTypePayload.Alias -> FrozenTypeShape(
                    kind = PackageFormat.TypeKind.Type,
                    declaration = TypeDeclarationIdentity(checkNotNull(payload.declaration) {
                        "An alias export type requires its captured declaration binding"
                    }),
                    arguments = arguments(payload.arguments),
                    serializedTarget = frozenSources.typeTarget(input.identity, input.domain),
                )
                is CfirExportTypePayload.Generic -> error("Generic input must be prepared before structural shape traversal")
                is CfirExportTypePayload.Function -> FrozenTypeShape(
                    kind = PackageFormat.TypeKind.Func,
                    arguments = arguments(payload.parameters),
                    returnType = child(payload.returnType),
                    isC = payload.isC,
                    isClosure = payload.isClosure,
                    hasVariableLenArg = payload.hasVariableLenArg,
                )
                is CfirExportTypePayload.Tuple -> FrozenTypeShape(PackageFormat.TypeKind.Tuple, arguments = arguments(payload.elements))
                is CfirExportTypePayload.RawArray -> FrozenTypeShape(
                    PackageFormat.TypeKind.Array, arguments = listOf(child(payload.layout.element)), dimsOrSize = payload.layout.dimensions.toLong(),
                )
                is CfirExportTypePayload.VArray -> FrozenTypeShape(
                    PackageFormat.TypeKind.VArray, arguments = listOf(child(payload.element)), dimsOrSize = payload.size,
                )
                is CfirExportTypePayload.Pointer -> FrozenTypeShape(PackageFormat.TypeKind.CPointer, arguments = listOf(child(payload.pointee)))
                CfirExportTypePayload.CString -> FrozenTypeShape(PackageFormat.TypeKind.CString)
                is CfirExportTypePayload.Unrepresentable -> if (frozenSources.isSavedInvalid(input.identity, input.domain))
                    FrozenTypeShape(PackageFormat.TypeKind.Invalid)
                else unsupported("A captured type has no SOURCE wire role")
            }
            visiting.remove(input)
            shapes[projection] = shape
            specificSelf?.let { specific ->
                specificSelfTypes[shape] = frozenTypeShape(checkNotNull(exportAnalysis.declarationTypeOf(specific)),
                    specific, typeProjectionFor(specific), visiting)
            }
            return shape
        }

        /** 当前结构 shape 完成后排空上界，保存引用和捕获域之后才允许编码 Generic 槽。 */
        private fun prepareGenericTypeBounds() {
            while (pendingGenericTypeBounds.isNotEmpty()) {
                val pending = pendingGenericTypeBounds.removeFirst()
                genericTypeInputs[pending.anchor] = java.util.Collections.unmodifiableList(pending.payload.explicitBounds.map {
                    frozenTypeShape(exportAnalysis.typeElement(it, pending.anchor.domain), pending.context, pending.projection)
                })
                check(pendingGenericTypeInputs.remove(pending.anchor))
            }
            check(pendingGenericTypeInputs.isEmpty())
        }

        /** 同一 t 池先 reserve 再递归 fill；声明/Generic 回边只返回已预留的真实槽。 */
        private fun type(shape: FrozenTypeShape): UInt {
            typeIndex[shape]?.let { return it }
            val specific = specificSelfTypes[shape]
            specific?.let { specificShape ->
                typeIndex[specificShape]?.let { index ->
                    typeIndex[shape] = index
                    return index
                }
            }
            val index = typeTable.reserve()
            typeIndex[shape] = index
            // 两身份均未保存时预登记双方，并继续编码输入 shape 的正文，不能改写为 specific 输入。
            if (specific != null) typeIndex[specific] = index
            val metadata = when (shape.kind) {
                PackageFormat.TypeKind.Generic -> {
                    val parameter = checkNotNull(shape.declaration).declaration as CfirTypeParameter
                    val target = sourceExpressionTarget(parameter)
                    CjoTypeMetadata(
                        kind = shape.kind,
                        semanticInfo = CjoGenericTypeInfoMetadata(
                            declarationIndex = target.index,
                            packageId = target.pkgId,
                            declarationKey = target.decl,
                            upperBounds = genericTypeInputs.getValue(checkNotNull(shape.genericAnchor)).map(::type),
                        ),
                    )
                }
                PackageFormat.TypeKind.Type, PackageFormat.TypeKind.Class, PackageFormat.TypeKind.Interface,
                PackageFormat.TypeKind.Struct, PackageFormat.TypeKind.Enum -> CjoTypeMetadata(
                    kind = shape.kind,
                    typeArguments = shape.arguments.map(::type),
                    semanticInfo = compositeInfo(checkNotNull(shape.declaration).declaration, shape.isThisType, shape.serializedTarget),
                )
                PackageFormat.TypeKind.Array, PackageFormat.TypeKind.VArray -> CjoTypeMetadata(
                    kind = shape.kind,
                    typeArguments = shape.arguments.map(::type),
                    semanticInfo = CjoArrayTypeInfoMetadata(checkNotNull(shape.dimsOrSize)),
                )
                PackageFormat.TypeKind.Func -> {
                    check(!shape.isClosure && (!shape.hasVariableLenArg || shape.isC)) {
                        "SOURCE function types require the completed native AST ABI"
                    }
                    CjoTypeMetadata(
                        kind = shape.kind,
                        typeArguments = shape.arguments.map(::type),
                        semanticInfo = CjoFunctionTypeInfoMetadata(type(checkNotNull(shape.returnType)), shape.isC, shape.hasVariableLenArg),
                    )
                }
                else -> CjoTypeMetadata(shape.kind, shape.arguments.map(::type))
            }
            typeTable.fill(index, metadata)
            return index
        }

        private fun compositeInfo(declaration: CfirDeclaration, isThisType: Boolean, saved: CjoFrozenDeclarationTarget?): CjoCompositeTypeInfoMetadata {
            val target = if (saved == null) sourceExpressionTarget(declaration) else frozenTarget(saved)
            return CjoCompositeTypeInfoMetadata(
                declarationIndex = target.index,
                packageId = target.pkgId,
                declarationKey = target.decl,
                isThisType = isThisType,
            )
        }

        private fun PrimitiveTypeKind.toCjoTypeKind(): UShort = when (this) {
            PrimitiveTypeKind.UNIT -> PackageFormat.TypeKind.Unit
            PrimitiveTypeKind.INT8 -> PackageFormat.TypeKind.Int8
            PrimitiveTypeKind.INT16 -> PackageFormat.TypeKind.Int16
            PrimitiveTypeKind.INT32 -> PackageFormat.TypeKind.Int32
            PrimitiveTypeKind.INT64 -> PackageFormat.TypeKind.Int64
            PrimitiveTypeKind.INT_NATIVE -> PackageFormat.TypeKind.IntNative
            PrimitiveTypeKind.UINT8 -> PackageFormat.TypeKind.UInt8
            PrimitiveTypeKind.UINT16 -> PackageFormat.TypeKind.UInt16
            PrimitiveTypeKind.UINT32 -> PackageFormat.TypeKind.UInt32
            PrimitiveTypeKind.UINT64 -> PackageFormat.TypeKind.UInt64
            PrimitiveTypeKind.UINT_NATIVE -> PackageFormat.TypeKind.UIntNative
            PrimitiveTypeKind.FLOAT16 -> PackageFormat.TypeKind.Float16
            PrimitiveTypeKind.FLOAT32 -> PackageFormat.TypeKind.Float32
            PrimitiveTypeKind.FLOAT64 -> PackageFormat.TypeKind.Float64
            PrimitiveTypeKind.RUNE -> PackageFormat.TypeKind.Rune
            PrimitiveTypeKind.NOTHING -> PackageFormat.TypeKind.Nothing
            PrimitiveTypeKind.BOOLEAN -> PackageFormat.TypeKind.Bool
            PrimitiveTypeKind.IDEAL_INT, PrimitiveTypeKind.IDEAL_FLOAT -> unsupported("ideal type in CJO declaration")
        }

        private fun unsupported(message: String): Nothing = error(message)


    }
}
