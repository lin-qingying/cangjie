package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import java.util.Collections
import java.util.IdentityHashMap

/** 输出编号只归 Context；计划保存真实 identity、首次 stage 与原递归保存事件。 */
internal sealed interface CjoSourceSaveEvent {
    data class ReserveDeclaration(val declaration: CfirDeclaration) : CjoSourceSaveEvent
    data class SaveDeclaration(val declaration: CfirDeclaration) : CjoSourceSaveEvent
    data class ReserveExpression(val identity: CfirSourceExpressionIdentity) : CjoSourceSaveEvent
    data class SaveExpression(val identity: CfirSourceExpressionIdentity) : CjoSourceSaveEvent
    data class Type(val use: CfirExportTypeUse, val context: CfirDeclaration) : CjoSourceSaveEvent
    data class Constant(val graph: CfirExportConstantGraph, val context: CfirDeclaration) : CjoSourceSaveEvent
}

/**
 * 完成的本次输出选择。拥有 parent、词法/global、首次保存阶段和内容可用性各自保存；
 * PRE_CONTENT 的递归事件先于 FINAL，填充不得再发现声明或表达式。
 */
internal class CjoSourceExportPlan private constructor(
    val declarations: List<CfirDeclaration>,
    val events: List<CjoSourceSaveEvent>,
    val parents: Map<CfirDeclaration, CfirDeclaration?>,
    val topLevel: Map<CfirDeclaration, Boolean>,
    val global: Map<CfirDeclaration, Boolean>,
    val bodyMembers: Map<CfirDeclaration, List<CfirDeclaration>>,
    val canonicalDefinitions: Set<CfirDeclaration>,
    private val contents: Map<CfirDeclaration, CjoSourceContent?>,
    private val stages: Map<CfirDeclaration, CjoSourceSaveStage>,
    private val partialFunctions: Set<CfirFunction>,
) {
    fun sourceContent(declaration: CfirDeclaration): CjoSourceContent? {
        check(contents.containsKey(declaration)) { "Source content has no completed save decision: ${declaration.symbol}" }
        return contents[declaration]
    }
    fun sourceSaveStageOf(declaration: CfirDeclaration): CjoSourceSaveStage = stages.getValue(declaration)
    fun isPartialFunction(function: CfirFunction): Boolean = function in partialFunctions

    companion object {
        fun prepare(
            roots: List<CfirDeclaration>,
            builtins: List<CfirBuiltInDeclaration>,
            analysis: CfirDeclarationExportAnalysis,
            sources: CjoFrozenSourceInputs,
            options: CjoExportOptions,
            family: CfirCjmpFamilySnapshot?,
            files: List<CfirFile>,
            attributes: CjoDeclarationAttributeInputs,
            outputOwners: Map<CfirDeclaration, CfirDeclaration>,
        ): CjoSourceExportPlan {
            val dependencies = CjoDeclarationDependencyInputs(analysis, sources, family)
            val inventory = Builder(roots, builtins, analysis, sources, options, family, files, attributes, outputOwners, dependencies, null).build()
            val planned = Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>()).apply { addAll(inventory.declarations) }
            return Builder(roots, builtins, analysis, sources, options, family, files, attributes, outputOwners, dependencies, planned).build().also {
                check(it.declarations.toSet() == planned) { "Dependency PreSave changed the completed definition inventory" }
            }
        }
    }

    private class Builder(
        private val roots: List<CfirDeclaration>,
        private val builtins: List<CfirBuiltInDeclaration>,
        private val analysis: CfirDeclarationExportAnalysis,
        private val sources: CjoFrozenSourceInputs,
        private val options: CjoExportOptions,
        private val family: CfirCjmpFamilySnapshot?,
        private val files: List<CfirFile>,
        private val attributes: CjoDeclarationAttributeInputs,
        private val outputOwners: Map<CfirDeclaration, CfirDeclaration>,
        private val dependencies: CjoDeclarationDependencyInputs,
        private val plannedDefinitions: Set<CfirDeclaration>?,
    ) {
        private val selection = CjoDeclarationExportSelection.create(analysis, options,
            isSuppressed = { analysis.isDoNotExport(it) || family?.doNotExport?.contains(it.symbol) == true },
            isMatchedCommon = { analysis.statusOf(it).isCommon && family?.specificBindingsFor(it)?.isNotEmpty() == true })
        private val declarations = ArrayList<CfirDeclaration>()
        private val events = ArrayList<CjoSourceSaveEvent>()
        private val reservedDeclarations = identitySet<CfirDeclaration>()
        private val savedDeclarations = identitySet<CfirDeclaration>()
        private val activeDeclarations = identitySet<CfirDeclaration>()
        private val expressions = identitySet<CfirSourceExpressionIdentity>()
        private val activeExpressions = identitySet<CfirSourceExpressionIdentity>()
        private val parents = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        private val topLevel = IdentityHashMap<CfirDeclaration, Boolean>()
        private val global = IdentityHashMap<CfirDeclaration, Boolean>()
        private val members = IdentityHashMap<CfirDeclaration, List<CfirDeclaration>>()
        private val contents = IdentityHashMap<CfirDeclaration, CjoSourceContent?>()
        private val stages = IdentityHashMap<CfirDeclaration, CjoSourceSaveStage>()
        private val canonicalDefinitions = identitySet<CfirDeclaration>()
        private val partialFunctions = identitySet<CfirFunction>()
        private val seenTypes = IdentityHashMap<CfirDeclaration, IdentityHashMap<CfirSession, MutableSet<ConeCangJieType>>>()
        private val typeParameterMappings = IdentityHashMap<CfirDeclaration, Map<CfirTypeParameter, CfirTypeParameter>>()
        private val counterpartsByOrigin = buildMap<CjoSnapshotTableEntry, CfirDeclaration> {
            // 合并定义的 CURRENT 引用在预留 d 时就指到实际输出 counterpart。
            if (family != null) (roots + outputOwners.keys).forEach { specific ->
                family.commonCounterpartsFor(specific).forEach { common ->
                    sources.declarationOrigins[common]?.let { origin ->
                        check(put(origin, specific).let { it == null || it === specific }) {
                            "A common CJO declaration has two selected output counterparts"
                        }
                    }
                }
            }
        }
        private val expressionGraphs = IdentityHashMap<CfirSourceExpressionIdentity, CfirSourceGraph>().apply {
            analysis.canonicalSourceGraphs().forEach { graph -> graph.expressions.keys.forEach { identity ->
                check(put(identity, graph).let { it == null || it === graph })
            } }
        }
        private val borrowedExpressions = identitySet<CfirSourceExpressionIdentity>()
        private val constants = HashSet<CfirExportConstantIdentity>()

        fun build(): CjoSourceExportPlan {
            val pre = preContent()
            pre.first.forEach { saveDeclaration(it, CjoSourceSaveStage.PRE_CONTENT) }
            if (pre.first.isNotEmpty()) pre.second.forEach { saveExpression(it, CjoSourceSaveStage.PRE_CONTENT) }
            finalRoots().forEach { saveDeclaration(it, CjoSourceSaveStage.FINAL) }
            builtins.forEach { builtin ->
                check(declarations.filterIsInstance<CfirClassLikeDeclaration>().none { it.symbol.classId == builtin.kind.classId }) {
                    "An injected std.core builtin already has a source definition"
                }
                saveDeclaration(builtin, CjoSourceSaveStage.FINAL)
            }
            // CURRENT 的 type/reference 仅 PreSave 身份；它第一次真正保存时仍使用当时的 FINAL。
            var index = 0
            while (index < declarations.size) saveDeclaration(declarations[index++], CjoSourceSaveStage.FINAL)
            check(activeDeclarations.isEmpty() && activeExpressions.isEmpty())
            check(parents.values.all { it == null || it is CfirAnonymousFunction || it in reservedDeclarations }) {
                "A selected declaration has no saved owning parent"
            }
            check(borrowedExpressions.all { it in expressions }) { "A borrowed SOURCE expression has no selected owning root" }
            check(reservedDeclarations == savedDeclarations.filterNot { it is CfirAnonymousFunction }.toSet())
            return CjoSourceExportPlan(immutable(declarations), immutable(events), immutable(parents), immutable(topLevel),
                immutable(global), immutable(members), immutableSet(canonicalDefinitions), immutable(contents),
                immutable(stages), immutableSet(partialFunctions))
        }

        /** SOURCE 和原 CJO FILE 依赖均由完成输入调度；同文件源序和包域保留。 */
        private fun finalRoots(): List<CfirDeclaration> {
            val selected = roots.filter { selection.shouldExportRoot(it, it in searched) }
            val byFile = linkedMapOf<CjoDeclarationFile, MutableList<CfirDeclaration>>()
            selected.forEach { byFile.getOrPut(dependencies.fileOf(it)) { ArrayList() } += it }
            val visited = HashSet<CjoDeclarationFile>()
            val ordered = ArrayList<CfirDeclaration>()
            fun visit(file: CjoDeclarationFile) {
                if (!visited.add(file)) return
                val declarations = byFile[file].orEmpty()
                declarations.forEach { declaration ->
                    dependencies.directTargets(declaration).forEach { target ->
                        if (target is CjoFrozenDeclarationTarget.Current) {
                            val source = dependencies.fileOf(target.declaration)
                            if (byFile.containsKey(source)) visit(source)
                        }
                    }
                }
                ordered += declarations
            }
            selected.forEach { visit(dependencies.fileOf(it)) }
            return ordered
        }

        private val searched = identitySet<CfirDeclaration>()

        /** Native PreSaveFullExportDecls 的 FIFO 候选，只有实际保存调用才产生 d/e 事件。 */
        private fun preContent(): Pair<List<CfirDeclaration>, List<CfirSourceExpressionIdentity>> {
            if (!options.exportContent || options.isMacroPackage) return emptyList<CfirDeclaration>() to emptyList()
            val pending = ArrayDeque<CfirDeclaration>().apply { addAll(roots.filter { selection.shouldAlwaysExport(it) && !analysis.isDoNotExport(it) && family?.doNotExport?.contains(it.symbol) != true }) }
            val candidates = ArrayList<CfirDeclaration>()
            val annotations = ArrayList<CfirSourceExpressionIdentity>()
            fun queueReferences(owner: CfirDeclaration) {
                if (!analysis.hasCanonicalSourceOf(owner)) return
                val graph = analysis.canonicalSourceGraphOf(owner)
                val root = when (owner) {
                    is CfirFunction -> graph.functionBodies[owner]
                    is CfirVariable -> graph.initializers[owner]
                    else -> null
                } ?: return
                val seen = identitySet<CfirSourceExpressionIdentity>()
                fun visit(identity: CfirSourceExpressionIdentity) {
                    if (!seen.add(identity)) return
                    val payload = graph.expression(identity).payload
                    val reference = when (payload) {
                        is CfirSourceExpressionPayload.Reference -> payload
                        is CfirSourceExpressionPayload.Member -> payload.reference
                        else -> null
                    }
                    (reference?.takeIf { it.targetKind != CfirSourceReferenceKind.IMPORTED }?.target as? CfirSourceReferenceTarget.Declaration)?.declaration?.let { target ->
                        if (!analysis.precedingFactsOf(target).isImported && analysis.isTopLevel(target)) pending += target
                    }
                    payload.expressionChildren().forEach(::visit)
                }
                visit(root)
            }
            while (pending.isNotEmpty()) {
                val declaration = pending.removeFirst()
                if (!searched.add(declaration) || analysis.isDoNotExport(declaration) || family?.doNotExport?.contains(declaration.symbol) == true) continue
                when (declaration) {
                    is CfirFunction, is CfirEnumConstructor -> if (analysis.declarationTypeOf(declaration)?.containsErrorType() == false) {
                        val full = selection.isPreContentFunctionCandidate(declaration as CfirCallableDeclaration)
                        if (full) {
                            candidates += declaration
                            queueReferences(declaration)
                        }
                        analysis.valueParametersOf(declaration).forEach { parameter -> defaultHelper(parameter)?.let { helper ->
                            if (full || analysis.precedingFactsOf(helper).isInline) {
                                candidates += helper
                                queueReferences(helper)
                            }
                        } }
                    }
                    is CfirProperty -> pending.addAll(analysis.accessorsOf(declaration))
                    is CfirVariable -> if (analysis.shouldExportSource(declaration)) {
                        candidates += declaration
                        queueReferences(declaration)
                    }
                    else -> Unit
                }
                if ((declaration is CfirClassLikeDeclaration || declaration is CfirExtend) &&
                    analysis.linkageOf(declaration) != CfirDeclarationLinkage.INTERNAL) {
                    if (declaration is CfirClass) annotations(declaration)
                        .firstOrNull { it.kind == CfirAnnotationExportKind.ANNOTATION }
                        ?.arguments?.firstOrNull()?.let { annotations += it.expression }
                    analysis.memberDeclarationsOf(declaration).filter {
                        analysis.linkageOf(it) != CfirDeclarationLinkage.INTERNAL ||
                            analysis.isGenericInCommonSerialization(it, options.serializingCommon)
                    }.forEach(pending::addLast)
                }
            }
            return candidates to annotations
        }

        private fun canonical(declaration: CfirDeclaration): CfirDeclaration =
            if (declaration is CfirTypeParameter) declaration.symbol.cfir
            else if (declaration is CfirCallableDeclaration) declaration.unwrapSubstitutionOverrides() else declaration

        private fun projected(declaration: CfirDeclaration): CfirDeclaration {
            if (declaration !is CfirPatternVariable || !analysis.isSourceDeclaration(declaration)) return declaration
            val binding = analysis.patternBindingsOf(declaration).singleOrNull()
            return if (binding != null && analysis.simplePatternOwnerOf(binding) === declaration) binding else declaration
        }

        private fun parentOf(declaration: CfirDeclaration): CfirDeclaration? {
            outputOwners[declaration]?.let { return it }
            var parent = analysis.owningParentOf(declaration)
            val seen = identitySet<CfirDeclaration>()
            while (parent != null) {
                check(seen.add(parent)) { "A declaration has cyclic owning parents" }
                val owner = canonical(projected(parent))
                // 简单模式的语法容器折叠到自身 VarDecl，真正 parent 仍是该容器的拥有者。
                if (owner !== declaration) return owner
                parent = analysis.owningParentOf(parent)
            }
            return null
        }

        private fun reserveDeclaration(input: CfirDeclaration) {
            val declaration = canonical(projected(input))
            if (declaration is CfirAnonymousFunction) return
            val parent = parentOf(declaration)
            if (!reservedDeclarations.add(declaration)) {
                check(parents[declaration] === parent) { "One declaration has conflicting captured owning parents" }
                return
            }
            parents[declaration] = parent
            topLevel[declaration] = sources.formats[declaration]?.isTopLevel ?: analysis.isTopLevel(declaration)
            global[declaration] = analysis.isTopLevel(declaration)
            declarations += declaration
            events += CjoSourceSaveEvent.ReserveDeclaration(declaration)
        }

        private fun bodyMembers(owner: CfirDeclaration): List<CfirDeclaration> = members[owner] ?: immutable(
            analysis.memberDeclarationsOf(owner).filter { selection.shouldExportBodyMember(owner, it) }
                .filterNot { owner is CfirEnum && it is CfirConstructor && it.isPrimary }
        ).also { members[owner] = it }

        private fun saveDeclaration(input: CfirDeclaration, stage: CjoSourceSaveStage) {
            val declaration = canonical(projected(input))
            reserveDeclaration(declaration)
            if (!savedDeclarations.add(declaration)) return
            check(activeDeclarations.add(declaration))
            stages[declaration] = stage
            contents[declaration] = selection.selectSourceContent(declaration, stage)
            if (declaration is CfirFunction && attributes.attributesOf(declaration).partialFunction) partialFunctions += declaration
            if (declaration is CfirAnonymousFunction) {
                activeDeclarations.remove(declaration)
                return
            }
            val source = analysis.isSourceDeclaration(declaration)
            check(source || sources.formats.containsKey(declaration) || declaration is CfirBuiltInDeclaration ||
                declaration is CfirTypeParameter && parentOf(declaration) is CfirBuiltInDeclaration) {
                "A selected definition has no completed source/header product: ${declaration.symbol}"
            }
            if (!attributes.attributesOf(declaration).omitsType && (sources.formats[declaration]?.type != null || source ||
                declaration is CfirBuiltInDeclaration || declaration is CfirTypeParameter && parentOf(declaration) is CfirBuiltInDeclaration)) {
                analysis.typeUseGraph().declarationType(declaration)?.let { saveType(it, declaration) }
            }
            annotations(declaration).forEach { annotation ->
                annotation.arguments.forEach { saveExpression(it.expression, stage) }
                annotation.target?.let { reserveTarget(it, annotation.targetKind) }
            }
            if (options.serializingCommon && plannedDefinitions != null) {
                dependencies.plannedTargets(declaration, plannedDefinitions).forEach { target ->
                    if (target is CjoFrozenDeclarationTarget.Current) reserveDeclaration(target.declaration)
                }
            }
            when (declaration) {
                is CfirTypeAlias -> {
                    // 官方 SaveTypeAliasDecl 先保存别名右侧，再保存自有泛型。
                    saveType(analysis.typeUseGraph().aliasRightHandSide(declaration), declaration)
                    saveGeneric(declaration, stage)
                }
                is CfirClassLikeDeclaration, is CfirExtend -> {
                    analysis.typeUseGraph().superTypes(declaration).forEach { saveType(it, declaration) }
                    bodyMembers(declaration).forEach { saveDeclaration(it, stage) }
                    saveGeneric(declaration, stage)
                }
                is CfirEnumConstructor -> if (sources.formats[declaration]?.function != null ||
                    source && (analysis.precedingFactsOf(declaration).enumConstructorHasParameterList ||
                        analysis.valueParametersOf(declaration).isNotEmpty())) saveFunction(declaration, stage)
                is CfirFunction -> { saveFunction(declaration, stage); saveGeneric(declaration, stage) }
                is CfirProperty -> if (!options.analysisSettings.compileCjd) {
                    analysis.accessorsOf(declaration).forEach { saveDeclaration(it, stage) }
                }
                is CfirPatternVariable -> {
                    canonicalDefinitions += declaration
                    initializer(declaration)?.let { saveExpression(it, stage) }
                    savePattern(analysis.canonicalSourceGraphOf(declaration).declarationPatterns.getValue(declaration), declaration, stage)
                }
                is CfirValueParameter -> contents.getValue(declaration)?.root?.let { saveExpression(it, stage) }
                is CfirVariable -> {
                    initializer(declaration)?.let { saveExpression(it, stage) }
                    val saved = sources.formats[declaration]
                    val value = analysis.constantValueOf(declaration)
                    val hasValue = saved?.hasConstantValue ?: (options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA && value != null)
                    if (hasValue) {
                        check(options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA) {
                            "A saved VarInfo value requires explicit typed-schema re-export"
                        }
                        saveConstant(checkNotNull(value), declaration)
                    }
                }
                is CfirTypeParameter, is CfirBuiltInDeclaration -> Unit
                else -> error("Unsupported selected CJO declaration")
            }
            events += CjoSourceSaveEvent.SaveDeclaration(declaration)
            check(activeDeclarations.remove(declaration))
        }

        private fun saveGeneric(owner: CfirDeclaration, stage: CjoSourceSaveStage) {
            if (owner is CfirConstructor || owner is CfirEnumConstructor ||
                owner is CfirFunction && owner in partialFunctions) return
            analysis.typeParametersOf(owner).forEach { saveDeclaration(it, stage) }
        }

        private fun saveFunction(owner: CfirCallableDeclaration, stage: CjoSourceSaveStage) {
            if (owner is CfirFunction && owner in partialFunctions) return
            val header = if (analysis.hasCanonicalSourceOf(owner)) analysis.canonicalSourceGraphOf(owner).functionHeaders[owner]
                else sources.formats[owner]?.function?.header
            if (header == null) {
                check(analysis.isSourceDeclaration(owner)) { "A saved F has no completed header" }
                analysis.valueParametersOf(owner).forEach { parameter ->
                    saveDeclaration(parameter, stage)
                    defaultHelper(parameter)?.let { saveDeclaration(it, stage) }
                }
                when (owner) {
                    is CfirFunction -> analysis.typeUseGraph().functionReturn(owner)
                    is CfirEnumConstructor -> analysis.typeUseGraph().enumConstructorReturn(owner)
                    else -> error("A saved F requires its actual callable owner")
                }?.let { saveType(it, owner) }
            } else {
                header.parameterLists.forEachIndexed { ordinal, parameters ->
                    val helpers = header.desugaredParameterLists[ordinal]
                    parameters.forEachIndexed { index, parameter ->
                        saveDeclaration(parameter, stage)
                        if (helpers.isNotEmpty()) helpers[index]?.let { saveDeclaration(it, stage) }
                    }
                }
                header.returnType?.let { saveType(it, owner.moduleData.session, owner) }
            }
            contents.getValue(owner)?.root?.let { saveExpression(it, stage) }
        }

        private fun saveFunction(body: CfirSourceFunctionBody, stage: CjoSourceSaveStage) {
            saveDeclaration(body.owner, stage)
            val header = body.importedHeader
            if (header == null) body.parameters.forEach { parameter ->
                saveDeclaration(parameter, stage)
                defaultHelper(parameter)?.let { saveDeclaration(it, stage) }
            } else header.parameterLists.forEachIndexed { ordinal, parameters ->
                val helpers = header.desugaredParameterLists[ordinal]
                parameters.forEachIndexed { index, parameter ->
                    saveDeclaration(parameter, stage)
                    if (helpers.isNotEmpty()) helpers[index]?.let { saveDeclaration(it, stage) }
                }
            }
            if (header != null) header.returnType?.let { saveType(it, body.owner.moduleData.session, body.owner) }
            else analysis.typeUseGraph().functionReturn(body.owner as CfirFunction)?.let { saveType(it, body.owner) }
            body.body?.let { saveExpression(it, stage) }
        }

        private fun initializer(variable: CfirVariable): CfirSourceExpressionIdentity? =
            if (variable in canonicalDefinitions && analysis.hasCanonicalSourceOf(variable))
                analysis.canonicalSourceGraphOf(variable).initializers[variable]
            else contents.getValue(variable)?.root

        private fun metadataOwner(declaration: CfirDeclaration): CfirDeclaration =
            (declaration as? CfirPatternBindingVariable)?.let(analysis::simplePatternOwnerOf) ?: declaration

        private fun annotations(declaration: CfirDeclaration): List<CfirSourceAnnotation> {
            val owner = metadataOwner(declaration)
            if (analysis.hasCanonicalSourceOf(owner)) return analysis.canonicalSourceGraphOf(owner).annotations.getValue(owner)
            check(analysis.annotationsOf(owner).isEmpty()) { "Annotation content has no completed canonical product" }
            return emptyList()
        }

        private fun defaultHelper(parameter: CfirValueParameter): CfirNamedFunction? {
            if (analysis.isSourceDeclaration(parameter)) return analysis.defaultHelperOf(parameter) as? CfirNamedFunction
            val owner = parameter.containingDeclarationSymbol.cfir
            val header = if (analysis.hasCanonicalSourceOf(owner)) analysis.canonicalSourceGraphOf(owner).functionHeaders[owner]
                else sources.formats[owner]?.function?.header
            checkNotNull(header) { "A loaded parameter has no completed F header" }
            header.parameterLists.forEachIndexed { ordinal, parameters ->
                val index = parameters.indexOfFirst { it === parameter }
                if (index >= 0) return header.desugaredParameterLists[ordinal].takeIf { it.isNotEmpty() }?.get(index)
            }
            error("A loaded parameter does not belong to its captured F")
        }

        private fun saveExpression(identity: CfirSourceExpressionIdentity, stage: CjoSourceSaveStage) {
            if (!expressions.add(identity)) {
                check(identity !in activeExpressions) { "SOURCE has a cyclic owning expression edge" }
                return
            }
            check(activeExpressions.add(identity))
            val record = expressionGraphs.getValue(identity).expression(identity)
            events += CjoSourceSaveEvent.ReserveExpression(identity)
            saveType(record.type, record.context.moduleData.session, record.context)
            record.mapExpression?.let { borrowedExpressions += it }
            val payload = record.payload
            if (payload is CfirSourceExpressionPayload.Jump) borrowedExpressions += payload.loop
            payload.visitSavedChildren(
                expression = { saveExpression(it, stage) },
                declaration = { canonicalDefinitions += canonical(projected(it)); saveDeclaration(it, stage) },
                pattern = { savePattern(it, record.context, stage) },
                function = { saveFunction(it, stage) },
                reference = { saveReference(it, record.context) },
                target = ::reserveTarget,
                savedReference = { reference ->
                    if (options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA) {
                        val target = sources.savedReferences.getValue(reference).target
                        if (target is CjoFrozenDeclarationTarget.Current) reserveTarget(target.declaration, CfirSourceReferenceKind.CURRENT)
                    }
                },
            )
            events += CjoSourceSaveEvent.SaveExpression(identity)
            check(activeExpressions.remove(identity))
        }

        private fun savePattern(pattern: CfirSourcePattern, owner: CfirDeclaration, stage: CjoSourceSaveStage) {
            pattern.contextExpression?.let { borrowedExpressions += it }
            pattern.visitSavedChildren({ saveExpression(it, stage) }, {
                canonicalDefinitions += canonical(projected(it))
                saveDeclaration(it, stage)
            }, { savePattern(it, owner, stage) })
            pattern.types.forEach { saveType(it, owner.moduleData.session, owner) }
            pattern.typeReferences.forEach { saveType(analysis.typeUseGraph().reference(it), owner) }
            if (pattern.constantValues.isNotEmpty()) {
                check(options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA) {
                    "Fixed native SOURCE does not produce Pattern.values"
                }
                analysis.constantValuesOf(pattern).forEach { saveConstant(it, owner) }
            }
        }

        /** Reference 的 target/instTys/parentTy 严格跟随 receiver/args 已保存的事件。 */
        private fun saveReference(reference: CfirSourceExpressionPayload.Reference, owner: CfirDeclaration) {
            (reference.target as? CfirSourceReferenceTarget.Declaration)?.let { reserveTarget(it.declaration, reference.targetKind) }
            reference.instantiatedTypes.forEach { saveType(it, owner.moduleData.session, owner) }
            when (val parent = reference.matchedParentType) {
                org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Absent -> Unit
                org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Invalid ->
                    saveType(CfirExportTypeUse.Invalid, owner)
                is org.cangnova.cangjie.cfir.references.CfirReferenceParentType.Instantiated ->
                    saveType(parent.type, owner.moduleData.session, owner)
            }
        }

        private fun reserveTarget(target: CfirDeclaration, kind: CfirSourceReferenceKind = CfirSourceReferenceKind.SOURCE) {
            if (kind == CfirSourceReferenceKind.IMPORTED || target is CfirAnonymousFunction) return
            if (kind == CfirSourceReferenceKind.SOURCE && (analysis.precedingFactsOf(target).isImported ||
                analysis.isDoNotExport(target) || family?.doNotExport?.contains(target.symbol) == true)) return
            val canonical = canonical(projected(target))
            val selected = sources.declarationOrigins[canonical]?.let(counterpartsByOrigin::get) ?: canonical
            if (kind == CfirSourceReferenceKind.SOURCE &&
                !analysis.isSourceDeclaration(selected) && !sources.formats.containsKey(selected)
            ) return
            reserveDeclaration(selected)
        }

        private fun saveType(use: CfirExportTypeUse, owner: CfirDeclaration) {
            events += CjoSourceSaveEvent.Type(use, owner)
            fun input(type: CfirExportTypeElement) = reserveTypeDeclarations(type, owner)
            when (use) {
                is CfirExportTypeUse.Semantic -> input(use.type)
                is CfirExportTypeUse.Arguments -> { input(use.type); use.arguments.forEach { saveType(it, owner) } }
                is CfirExportTypeUse.Function -> {
                    input(use.type)
                    use.parameters.forEach { saveType(it, owner) }
                    saveType(use.returnType, owner)
                }
                CfirExportTypeUse.Invalid -> Unit
            }
        }

        private fun saveType(type: ConeCangJieType, domain: CfirSession, owner: CfirDeclaration) =
            saveType(CfirExportTypeUse.Semantic(analysis.typeElement(type, domain)), owner)

        private fun reserveTypeDeclarations(input: CfirExportTypeElement, owner: CfirDeclaration) {
            val seenForOwner = seenTypes.getOrPut(owner) { IdentityHashMap() }
            if (!seenForOwner.getOrPut(input.domain) { identitySet() }.add(input.identity)) return
            val parameters = typeParameterMappings.getOrPut(owner) {
                family?.ownerTypeParameterMappingForDescendant(
                    owner, generateSequence(analysis.ownerOf(owner)) { analysis.ownerOf(it) },
                ).orEmpty()
            }
            fun reserveTypeTarget(target: CfirDeclaration) {
                val parameter = target as? CfirTypeParameter
                val specific = parameter?.let(parameters::get)
                reserveTarget(specific ?: target)
            }
            when (val target = sources.typeTarget(input.identity, input.domain)) {
                is CjoFrozenDeclarationTarget.Current -> reserveTarget(target.declaration, CfirSourceReferenceKind.CURRENT)
                null -> listOfNotNull(input.declaration, input.abbreviatedDeclaration, input.expandedDeclaration).forEach(::reserveTypeTarget)
                else -> Unit
            }
            input.children.forEach { reserveTypeDeclarations(analysis.typeElement(it, input.domain), owner) }
        }

        private fun saveConstant(graph: CfirExportConstantGraph, owner: CfirDeclaration) {
            if (!constants.add(graph.root)) return
            graph.nodes.forEach { node ->
                check(sources.constants.containsKey(node.identity)) { "A selected typed value has no proven storage representation" }
                saveType(node.type, sources.constantDomain(node.identity), owner)
                val storage = sources.constants.getValue(node.identity)
                if (storage is CjoFrozenConstantStorage.Composite) storage.fields.forEach { field ->
                    // 值字段只是绑定/存储类型依赖，不能凭 v 中的字段 symbol 取得 d 拥有权。
                    saveType(field.type, storage.domain, owner)
                }
            }
            events += CjoSourceSaveEvent.Constant(graph, owner)
        }

        private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap())
        private fun <T> immutable(values: Collection<T>): List<T> = Collections.unmodifiableList(values.toList())
        private fun <K, V> immutable(values: IdentityHashMap<K, V>): Map<K, V> = Collections.unmodifiableMap(IdentityHashMap(values))
        private fun <T : Any> immutableSet(values: Set<T>): Set<T> =
            Collections.unmodifiableSet(identitySet<T>().apply { addAll(values) })
    }
}

