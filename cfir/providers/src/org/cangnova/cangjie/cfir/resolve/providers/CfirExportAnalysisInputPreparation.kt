package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.CfirPublishedAnnotations
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.declarations.utils.explicitUpperBounds
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.expandedConeTypeWithEnsuredPhase
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.extendExportSurfaceService
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 解析/header owner 先完成导出实际读取的依赖闭包，冻结端只消费此产品。
 * 与 Kotlin 的 resolver/deserializer → serializer 分工一致；仓颉另有 SOURCE 图和
 * 输入不变协议，因此不能在记录声明之后才展开类型、完成注解或构建 override scope。
 */
object CfirExportAnalysisInputPreparation {
    /** 完备输入的只读产品；消费时核验身份与代次，不请求解析、解码或重新准备。 */
    class Result internal constructor(
        override val declarations: List<CfirDeclaration>,
        internal val interfaceImplementations: Set<CfirDeclaration>,
        private val declarationTypes: Map<CfirDeclaration, ConeCangJieType?>,
        private val types: Map<CfirSession, Map<ConeCangJieType, Type>>,
        private val signatures: Map<CfirDeclaration, CfirExportDeclarationTypeInput>,
        private val domain: Domain,
        private val owners: Map<CfirDeclaration, CfirDeclaration?>,
        private val declarationFiles: Map<CfirDeclaration, CfirFile?>,
        private val associations: Associations,
    ) : CfirPreparedExportDependencyInputs {
        override fun declarationTypeOf(declaration: CfirDeclaration): ConeCangJieType? = declarationType(declaration)
        override fun typeDependenciesOf(type: ConeCangJieType, domain: CfirSession): CfirPreparedExportTypeDependencies = withReadOnlyInput {
            type(type, domain).dependencyInputs(type, domain)
        }
        override fun owningParentOf(declaration: CfirDeclaration): CfirDeclaration? = withReadOnlyInput {
            associations.validate()
            signatures.getValue(declaration).validate(declaration)
            owner(declaration)
        }
        override fun sourceFileOf(declaration: CfirDeclaration): CfirFile? = withReadOnlyInput {
            associations.validate()
            signatures.getValue(declaration).validate(declaration)
            file(declaration)
        }
        internal fun declarationType(declaration: CfirDeclaration): ConeCangJieType? = withReadOnlyInput {
            signatures.getValue(declaration).validate(declaration)
            declarationTypes.getValue(declaration)
        }
        internal fun type(type: ConeCangJieType, session: CfirSession): Type = withReadOnlyInput {
            check(types.containsKey(session)) { "Export type has an unprepared session domain" }
            check(types.getValue(session).containsKey(type)) {
                "Export type dependency was not prepared: $type, preparedInOtherDomain=${types.any { (other, input) -> other !== session && input.containsKey(type) }}"
            }
            types.getValue(session).getValue(type).also { it.validate(type) }
        }
        internal fun owner(declaration: CfirDeclaration): CfirDeclaration? = owners.getValue(declaration)
        internal fun file(declaration: CfirDeclaration): CfirFile? = declarationFiles.getValue(declaration)
        internal fun forEachType(action: (ConeCangJieType, CfirSession) -> Unit) = withReadOnlyInput {
            types.forEach { (session, inputs) -> inputs.keys.forEach { action(it, session) } }
        }
        internal fun containsType(type: ConeCangJieType, session: CfirSession): Boolean = types[session]?.containsKey(type) == true
        internal fun <T> withReadOnlyInput(action: () -> T): T = domain.withReadOnlyInput(declarations, action)
        internal fun validateSignatures() = withReadOnlyInput {
            signatures.forEach { (declaration, input) -> input.validate(declaration) }
        }
        internal fun validateDomain(
            files: Collection<CfirFile>, roots: List<CfirDeclaration>, members: (CfirDeclaration) -> List<CfirDeclaration>,
            graphs: Collection<CfirSourceGraph>, additionalTypes: Map<CfirSession, List<ConeCangJieType>>,
            additionalDeclarations: Collection<CfirDeclaration>,
        ) = withReadOnlyInput {
            domain.validate(files, roots, members, graphs)
            additionalDeclarations.forEach { check(declarationTypes.containsKey(it)) { "Export declaration belongs to an unprepared input domain" } }
            additionalTypes.forEach { (session, input) -> input.forEach { type(it, session) } }
            validateSignatures()
        }
    }

    /** 完成产品的消费域只按真实身份核对；相同包名或相同内容不能替代本次 files/roots/member 图。 */
    internal class Domain(
        files: Collection<CfirFile>, roots: List<CfirDeclaration>, members: (CfirDeclaration) -> List<CfirDeclaration>,
        graphs: Collection<CfirSourceGraph>, declarations: List<CfirDeclaration>,
    ) {
        private val files = files.toList()
        private val roots = roots.toList()
        private val graphs = graphs.distinct()
        fun <T> withReadOnlyInput(declarations: Collection<CfirDeclaration>, action: () -> T): T = readOnly(declarations, graphs, action)
        private val fileDeclarations = IdentityHashMap<CfirFile, List<CfirDeclaration>>().apply {
            files.forEach { put(it, it.declarations.toList()) }
        }
        private val memberInputs = IdentityHashMap<CfirDeclaration, List<CfirDeclaration>>().apply {
            declarations.filter { it is CfirClassLikeDeclaration || it is CfirExtend }.forEach {
                put(it, members(it).toList())
            }
        }
        fun validate(files: Collection<CfirFile>, roots: List<CfirDeclaration>, members: (CfirDeclaration) -> List<CfirDeclaration>, graphs: Collection<CfirSourceGraph>) {
            check(sameIdentities(this.files, files.toList()) && sameIdentities(this.roots, roots) &&
                sameIdentities(this.graphs, graphs.distinct())) { "Export preparation belongs to a different files/roots/SOURCE domain" }
            fileDeclarations.forEach { (file, input) ->
                check(sameIdentities(input, file.declarations)) { "Export file declarations changed after final preparation" }
            }
            memberInputs.forEach { (owner, input) ->
                check(sameIdentities(input, members(owner))) { "Export member input changed after final preparation: ${owner.symbol}" }
            }
        }
    }

    internal class Type(
        val children: List<ConeCangJieType>,
        val declaration: CfirDeclaration?,
        val abbreviatedType: ConeCangJieType?,
        val abbreviatedDeclaration: CfirDeclaration?,
        val expandedType: ConeCangJieType,
        val expandedDeclaration: CfirDeclaration?,
        val payload: CfirExportTypePayload,
        val expansionInputs: Map<CfirTypeAlias, CfirExportDeclarationTypeInput>,
    ) {
        fun dependencyInputs(type: ConeCangJieType, domain: CfirSession): CfirPreparedExportTypeDependencies =
            CfirPreparedExportTypeDependencies(type, domain, children, declaration, abbreviatedType,
                abbreviatedDeclaration, expandedType, expandedDeclaration)
        fun validate(type: ConeCangJieType) {
            check(sameIdentities(children, type.captureExportTypeDependencies()) && abbreviatedType === type.abbreviatedType &&
                payload.hasSameSourceFields(type.captureExportTypePayload(declaration)) &&
                expansionInputs.all { (alias, input) -> input.isCurrent(alias) }) {
                "Export type input changed after final preparation: $type"
            }
        }
    }

    /** 完成时的实际包含边，不以签名相等、文件名或 provider 再查询代替源图身份。 */
    internal class Associations(
        files: Collection<CfirFile>, declarations: List<CfirDeclaration>,
        elements: Map<CfirElement, Collection<CfirDeclaration>>,
        declarationFiles: Map<CfirDeclaration, CfirFile?>,
        private val members: (CfirDeclaration) -> List<CfirDeclaration>,
    ) {
        private class Edge(val element: CfirElement, val context: CfirDeclaration, val children: List<CfirElement>)
        private val edges = elements.flatMap { (element, contexts) -> contexts.map { context ->
            Edge(element, context, CfirExportAnalysisInputSnapshot.sourceChildren(element, context).toList())
        } }
        private val fileDeclarations = IdentityHashMap<CfirFile, List<CfirDeclaration>>().apply {
            (files + declarationFiles.values.filterNotNull()).forEach { put(it, it.declarations.toList()) }
        }
        private val memberInputs = IdentityHashMap<CfirDeclaration, List<CfirDeclaration>>().apply {
            declarations.filter { it is CfirClassLikeDeclaration || it is CfirExtend }.forEach { put(it, members(it).toList()) }
        }
        fun validate() {
            memberInputs.forEach { (owner, input) ->
                check(sameIdentities(input, members(owner))) { "Completed export member association changed: ${owner.symbol}" }
            }
            edges.forEach { edge ->
                check(sameIdentities(edge.children, CfirExportAnalysisInputSnapshot.sourceChildren(edge.element, edge.context))) {
                    "Completed export lexical association changed: ${edge.context.symbol}"
                }
            }
            fileDeclarations.forEach { (file, input) ->
                check(sameIdentities(input, file.declarations)) { "Completed export physical file association changed" }
            }
        }
    }

    fun prepare(
        files: Collection<CfirFile>,
        roots: List<CfirDeclaration>,
        members: (CfirDeclaration) -> List<CfirDeclaration>,
        canonicalSources: Collection<CfirSourceGraph> = emptyList(),
        additionalTypeInputs: Map<CfirSession, List<ConeCangJieType>> = emptyMap(),
        additionalDeclarationInputs: Collection<CfirDeclaration> = emptyList(),
    ): Result = Work(files, roots, members).apply {
        complete(canonicalSources, additionalTypeInputs, additionalDeclarationInputs)
    }.freeze()

    /**
     * 首次交付前的生产工作集。来源 owner 可以继续完成实际发现的内容和类型，
     * 但只有最终输入稳定后才能 freeze；已交付产品没有重新准备或刷新基准入口。
     */
    class Work(
        files: Collection<CfirFile>,
        roots: List<CfirDeclaration>,
        members: (CfirDeclaration) -> List<CfirDeclaration>,
    ) {
        private val preparation = Preparation(files, roots, members)
        private var frozen = false
        private var completed = false
        private var aborted = false
        private var observing = false

        val declarations: List<CfirDeclaration> get() = preparation.completedDeclarations()
        /** 实际依赖及类型产品的增量序号，供来源准备协调固定点；不是冻结代次。 */
        val dependencyRevision: Long get() = preparation.dependencyRevision

        fun complete(
            canonicalSources: Collection<CfirSourceGraph> = emptyList(),
            additionalTypeInputs: Map<CfirSession, List<ConeCangJieType>> = emptyMap(),
            additionalDeclarationInputs: Collection<CfirDeclaration> = emptyList(),
        ) {
            check(!frozen) { "Export preparation cannot produce after its final input was delivered" }
            check(!aborted) { "Export cannot reuse a failed preparation work set" }
            check(!observing) { "Export cannot produce inside completed dependency observation" }
            completed = false
            try {
                preparation.complete(canonicalSources, additionalTypeInputs, additionalDeclarationInputs)
                completed = true
            } catch (failure: Throwable) {
                aborted = true
                throw failure
            }
        }

        fun <T> withReadOnlyInput(action: () -> T): T = preparation.withReadOnlyInput(action)

        /**
         * 生产固定点之间的只读发现入口。视图不能逃逸到下一轮 complete 或编码端，
         * 回调中的每个查询仅使用此工作集已经完成并核验的事实。
         */
        fun <T> withCompletedInputs(action: (CfirPreparedExportDependencyInputs) -> T): T {
            check(completed && !aborted && !frozen && !observing) { "Completed dependency observation requires an open completed work set" }
            observing = true
            try {
                return withReadOnlyInput {
                    fun validateObservation() {
                        try {
                            preparation.validateCompletedInputs()
                        } catch (failure: Throwable) {
                            completed = false
                            aborted = true
                            throw failure
                        }
                    }
                    validateObservation()
                    val observerThread = Thread.currentThread()
                    var active = true
                    fun requireActive() {
                        check(active && Thread.currentThread() === observerThread && completed && !aborted && !frozen) {
                            "Completed dependency view is valid only inside its original observation callback"
                        }
                    }
                    val inputs = object : CfirPreparedExportDependencyInputs {
                        override val declarations: List<CfirDeclaration>
                            get() { requireActive(); return preparation.completedDeclarations() }
                        override fun declarationTypeOf(declaration: CfirDeclaration): ConeCangJieType? {
                            requireActive(); return preparation.completedDeclarationType(declaration)
                        }
                        override fun typeDependenciesOf(type: ConeCangJieType, domain: CfirSession): CfirPreparedExportTypeDependencies {
                            requireActive(); return preparation.completedTypeDependencies(type, domain)
                        }
                        override fun owningParentOf(declaration: CfirDeclaration): CfirDeclaration? {
                            requireActive(); return preparation.completedOwner(declaration)
                        }
                        override fun sourceFileOf(declaration: CfirDeclaration): CfirFile? {
                            requireActive(); return preparation.completedFile(declaration)
                        }
                    }
                    var callbackFailure: Throwable? = null
                    try {
                        action(inputs)
                    } catch (failure: Throwable) {
                        callbackFailure = failure
                        throw failure
                    } finally {
                        // 回调已退出即撤销视图；异常路径也必须核验，保留业务异常和漂移证据。
                        active = false
                        try {
                            validateObservation()
                        } catch (validationFailure: Throwable) {
                            callbackFailure?.takeIf { it !== validationFailure }?.let(validationFailure::addSuppressed)
                            throw validationFailure
                        }
                    }
                }
            } finally {
                observing = false
            }
        }

        fun freeze(): Result {
            check(!frozen) { "Export preparation can deliver its final input only once" }
            check(completed && !aborted) { "Export requires a completed preparation work set before freeze" }
            check(!observing) { "Export cannot freeze inside completed dependency observation" }
            // fence 建立本身也可能失败；在调用任何 owner 之前封闭本次冻结尝试。
            frozen = true
            return withReadOnlyInput { preparation.freeze() }
        }
    }

    private class Preparation(
        private val files: Collection<CfirFile>,
        private val roots: List<CfirDeclaration>,
        private val members: (CfirDeclaration) -> List<CfirDeclaration>,
    ) {
        private var canonicalSources: List<CfirSourceGraph> = emptyList()
        var dependencyRevision: Long = 0
            private set
        private val declarations = identitySet<CfirDeclaration>()
        private val orderedDeclarations = ArrayList<CfirDeclaration>()
        private val declarationTypes = IdentityHashMap<CfirDeclaration, ConeCangJieType?>()
        private val completedTypeInputs = IdentityHashMap<CfirDeclaration, CfirExportDeclarationTypeInput>()
        // 相同来源节点可以由多个声明语境借用；节点身份不意味着唯一的类型 session/内容边界。
        private val elements = IdentityHashMap<CfirElement, MutableSet<CfirDeclaration>>()
        private val constants = IdentityHashMap<CfirSession, IdentityHashMap<CfirConstantValue, CfirDeclaration>>()
        private val owners = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        private val declarationFiles = IdentityHashMap<CfirDeclaration, CfirFile?>()
        private val queuedTypes = IdentityHashMap<CfirSession, MutableSet<ConeCangJieType>>()
        private val types = IdentityHashMap<CfirSession, IdentityHashMap<ConeCangJieType, Type>>()
        private val pendingDeclarations = ArrayDeque<CfirDeclaration>()
        private val pendingTypes = ArrayDeque<Pair<ConeCangJieType, CfirSession>>()
        private val sourceDeclarations = identitySet<CfirDeclaration>()
        private val physicalSourceFiles = IdentityHashMap<CfirDeclaration, CfirFile>()
        private val canonicalLexicalOwners = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        private val implemented = identitySet<CfirDeclaration>()
        private val scopeSessions = IdentityHashMap<CfirSession, ScopeSession>()
        private var associations: Associations? = null

        fun completedDeclarations(): List<CfirDeclaration> = Collections.unmodifiableList(orderedDeclarations.toList())
        fun <T> withReadOnlyInput(action: () -> T): T = readOnly(orderedDeclarations, canonicalSources, action)
        fun validateCompletedInputs() {
            check(pendingDeclarations.isEmpty() && pendingTypes.isEmpty()) { "Dependency observation cannot consume running preparation" }
            completedTypeInputs.forEach { (declaration, input) -> input.validate(declaration) }
            types.forEach { (_, inputs) -> inputs.forEach { (type, input) -> input.validate(type) } }
            checkNotNull(associations) { "Completed dependency observation requires its association evidence" }.validate()
        }
        fun completedDeclarationType(declaration: CfirDeclaration): ConeCangJieType? {
            completedTypeInputs.getValue(declaration).validate(declaration)
            return declarationTypes.getValue(declaration)
        }
        fun completedTypeDependencies(type: ConeCangJieType, domain: CfirSession): CfirPreparedExportTypeDependencies {
            val input = types.getValue(domain).getValue(type)
            input.validate(type)
            return input.dependencyInputs(type, domain)
        }
        fun completedOwner(declaration: CfirDeclaration): CfirDeclaration? {
            checkNotNull(associations).validate()
            completedTypeInputs.getValue(declaration).validate(declaration)
            return owners.getValue(declaration)
        }
        fun completedFile(declaration: CfirDeclaration): CfirFile? {
            checkNotNull(associations).validate()
            completedTypeInputs.getValue(declaration).validate(declaration)
            return declarationFiles.getValue(declaration)
        }

        fun complete(
            canonicalSources: Collection<CfirSourceGraph>,
            additionalTypeInputs: Map<CfirSession, List<ConeCangJieType>>,
            additionalDeclarationInputs: Collection<CfirDeclaration>,
        ) {
            val graphs = canonicalSources.distinct()
            if (!sameIdentities(this.canonicalSources, graphs)) dependencyRevision++
            this.canonicalSources = graphs
            val sourceElements = identitySet<CfirElement>()
            fun source(element: CfirElement, context: CfirDeclaration, file: CfirFile) {
                if (!sourceElements.add(element)) return
                if (context.origin.fromSource && context.cjoDeclarationOrigin == null) check(element !is CfirLazyExpression && element !is CfirLazyBlock) {
                    "Source export requires materialized expression regions"
                }
                if (element is CfirDeclaration && element.origin.fromSource && element.cjoDeclarationOrigin == null) {
                    sourceDeclarations += element
                    check(physicalSourceFiles.put(element, file).let { it == null || it === file }) { "A source declaration belongs to multiple physical files" }
                }
                CfirExportAnalysisInputSnapshot.sourceChildren(element, context).forEach {
                    source(it, element as? CfirDeclaration ?: context, file)
                }
            }
            files.forEach { file ->
                file.requireOwnedGraphAnalysisReady()
                file.declarations.forEach { source(it, it, file); reference(it) }
            }
            roots.forEach(::reference)
            canonicalSources.forEach { graph ->
                graph.requireCanonicalOwningDeclarations()
                graph.forEachExportLexicalOwner { declaration, owner -> canonicalLexicalOwners[declaration] = owner }
                graph.generatedDeclarations.forEach { (declaration, context) ->
                    check(physicalSourceFiles.put(declaration, context.file).let { it == null || it === context.file })
                }
                sourceDeclarations.addAll(graph.declarationOwners.keys.filter { it.origin.fromSource && it.cjoDeclarationOrigin == null })
                sourceDeclarations.addAll(graph.generatedDeclarations.keys)
                CfirExportSourceDependencies(
                    ::reference,
                    { type, context -> addType(type, context.moduleData.session) },
                    { ref, context -> element(ref, context) },
                    ::constant,
                    { pattern, context -> pattern.constantValues.forEach { constant(it, context) } },
                ).visit(graph)
            }
            additionalTypeInputs.forEach { (session, input) -> input.forEach { addType(it, session) } }
            additionalDeclarationInputs.forEach(::reference)
            // header/type 生产可能揭示新边或完成已有节点字段；完整重检发生在首次冻结之前。
            // 这里只排空实际发现的输入，不刷新已经交付的 Result 或快照基准。
            while (true) {
                drain()
                orderedDeclarations.toList().forEach(::currentHeaderDependencies)
                elements.entries.toList().forEach { (element, contexts) -> contexts.toList().forEach { context -> elementFields(element, context) } }
                constants.values.toList().forEach { input -> input.entries.toList().forEach { (value, context) -> constantFields(value, context) } }
                types.entries.toList().forEach { (session, input) -> input.keys.toList().forEach {
                    type(it, session)
                } }
                if (pendingDeclarations.isNotEmpty() || pendingTypes.isNotEmpty()) continue
                implemented.clear()
                orderedDeclarations.toList().filter { it is CfirClassLikeDeclaration || it is CfirExtend }
                    .forEach(::interfaceImplementations)
                if (pendingDeclarations.isNotEmpty() || pendingTypes.isNotEmpty()) continue
                // 复合类型依赖也属于生产工作集；发现新 header 后继续排空，freeze 不展开类型。
                orderedDeclarations.forEach {
                    if (completedTypeInputs[it]?.isCurrent(it) != true) {
                        completedTypeInputs[it] = CfirExportDeclarationTypeInput.capture(it)
                        val type = CfirExportAnalysisInputSnapshot.declarationType(it)
                        declarationTypes[it] = type
                        addType(type, it.moduleData.session)
                        dependencyRevision++
                    }
                }
                while (pendingTypes.isNotEmpty()) {
                    val (type, session) = pendingTypes.removeFirst()
                    type(type, session)
                }
                if (pendingDeclarations.isEmpty()) break
            }
            associations = withReadOnlyInput {
                Associations(files, orderedDeclarations, elements, declarationFiles, members).also { it.validate() }
            }
        }

        fun freeze(): Result {
            check(pendingDeclarations.isEmpty() && pendingTypes.isEmpty()) { "Export cannot freeze a running dependency preparation" }
            val associations = checkNotNull(associations)
            associations.validate()
            completedTypeInputs.forEach { (declaration, input) -> input.validate(declaration) }
            val signatures = IdentityHashMap<CfirDeclaration, CfirExportDeclarationTypeInput>()
            orderedDeclarations.forEach { signatures[it] = CfirExportDeclarationTypeInput.capture(it) }
            signatures.forEach { (declaration, signature) -> signature.validate(declaration) }
            types.forEach { (_, inputs) -> inputs.forEach { (type, input) -> input.validate(type) } }
            val domain = Domain(files, roots, members, canonicalSources, orderedDeclarations)
            associations.validate()
            return Result(Collections.unmodifiableList(orderedDeclarations.toList()),
                Collections.unmodifiableSet(identitySet<CfirDeclaration>().apply { addAll(implemented) }),
                Collections.unmodifiableMap(IdentityHashMap(declarationTypes)),
                Collections.unmodifiableMap(IdentityHashMap<CfirSession, Map<ConeCangJieType, Type>>().apply {
                    types.forEach { (session, values) -> put(session, Collections.unmodifiableMap(IdentityHashMap(values))) }
                }), Collections.unmodifiableMap(IdentityHashMap(signatures)), domain,
                Collections.unmodifiableMap(IdentityHashMap(owners)), Collections.unmodifiableMap(IdentityHashMap(declarationFiles)), associations)
        }

        private fun drain() {
            while (pendingDeclarations.isNotEmpty() || pendingTypes.isNotEmpty()) {
                while (pendingDeclarations.isNotEmpty()) declaration(pendingDeclarations.removeFirst())
                while (pendingTypes.isNotEmpty()) {
                    val (type, session) = pendingTypes.removeFirst()
                    type(type, session)
                }
            }
        }

        private fun reference(declaration: CfirDeclaration) {
            check(declaration.symbol.isBound && declaration.symbol.cfir === declaration) { "Export preparation requires canonical declaration bindings" }
            if (declaration !in declarations) pendingDeclarations.addLast(declaration)
        }

        private fun declaration(declaration: CfirDeclaration) {
            if (!declarations.add(declaration)) return
            dependencyRevision++
            declaration.requireOwnedGraphAnalysisReady()
            orderedDeclarations += declaration
            // 必须显式完成原 context 的 normal header；空 Unselected 不能冒充 Complete(empty)。
            // 此能力只包含 annotation/default，不授予 imported initializer 或函数正文。
            val contentSource = declaration.contentSource
            if (contentSource is CfirContentSource.Published) {
                check(declaration.cjoDeclarationOrigin != null) { "Published export headers require their actual CJO origin" }
                if (contentSource.snapshot().annotations(declaration) !is CfirPublishedAnnotations.Complete) {
                    check(contentSource is CfirImportedDeclarationHeaderSource) {
                        "Unselected imported header requires its original content completion owner: ${declaration.symbol}"
                    }
                    contentSource.completeHeader(declaration)
                }
                check(contentSource.snapshot().annotations(declaration) is CfirPublishedAnnotations.Complete) {
                    "Export preparation requires the completed annotation product: ${declaration.symbol}"
                }
            }
            val owner = if (canonicalLexicalOwners.containsKey(declaration)) canonicalLexicalOwners[declaration] else when (declaration) {
                is CfirTypeParameter -> declaration.containingDeclarationSymbol.cfir
                is CfirValueParameter -> declaration.containingDeclarationSymbol.cfir
                is CfirCallableDeclaration -> declaration.symbol.getContainingExtend() ?: declaration.symbol.getContainingClass()?.cfir
                else -> null
            }
            owners[declaration] = owner
            declarationFiles[declaration] = if (physicalSourceFiles.containsKey(declaration)) physicalSourceFiles.getValue(declaration)
                else declaration.symbol.getContainingFile()
            owner?.let(::reference)
            if (declaration is CfirCallableDeclaration) {
                val original = declaration.unwrapSubstitutionOverrides()
                if (original !== declaration) reference(original)
            }
            if (declaration is CfirNamedFunction) declaration.defaultParameterHelperInfo?.owner?.let(::reference)
            (declaration as? CfirTypeAlias)?.expandedTypeRef?.coneTypeOrNull?.let { addType(it, declaration.moduleData.session) }
            if (declaration is CfirTypeParameter) {
                (declaration.bounds + declaration.explicitUpperBounds).forEach { element(it, declaration) }
            }
            when (declaration) {
                is CfirClassLikeDeclaration -> {
                    declaration.typeParameters.filterIsInstance<CfirTypeParameter>().forEach(::reference)
                    declaration.superTypeRefs.forEach { element(it, declaration) }
                    declaration.declarations.forEach(::reference)
                    members(declaration).forEach(::reference)
                }
                is CfirExtend -> {
                    declaration.typeParameters.forEach(::reference)
                    declaration.superTypeRefs.forEach { element(it, declaration) }
                    declaration.declarations.forEach(::reference)
                    members(declaration).forEach(::reference)
                }
                is CfirFunction -> {
                    declaration.typeParameters.filterIsInstance<CfirTypeParameter>().forEach(::reference)
                    declaration.valueParameters.forEach(::reference)
                }
                is CfirEnumConstructor -> declaration.valueParameters.forEach(::reference)
                is CfirProperty -> listOfNotNull(declaration.getter, declaration.setter).forEach(::reference)
                is CfirPatternVariable -> declaration.pattern.bindingVariables().forEach(::reference)
                else -> Unit
            }
            if (declaration is CfirVariable) {
                val value = ((declaration.evaluatedInitializer as? CfirEvaluatorResult.Evaluated)?.result as? CfirConstantValueExpression)?.constantValue
                value?.let { constant(it, declaration) }
            }
            // 初始化依赖的生产在首次 freeze 之前；不输出的中间 target 仍进入实际语义 inventory。
            if (declaration is CfirFieldVariable && declaration.cjoDeclarationOrigin == null &&
                declaration.symbol.callableId.classId != null && !declaration.status.isStatic && declaration.initializer != null) {
                check(declaration.initializer !is CfirLazyExpression) { "Source export requires materialized expression regions" }
                check(declaration.collectAndPublishInitializationDependencies() !is CjmpInitializationDependencies.NotCollected)
            }
            (declaration.initializationDependencies as? CjmpInitializationDependencies.Collected)?.declarations
                ?.forEach { reference(it.cfir) }
            elements.getOrPut(declaration) { identitySet() }.add(declaration)
            elementFields(declaration, declaration)
        }

        /** 使用完成后的当前 header 边排空新增参数/成员/ref；尚未发布任何冻结基准。 */
        private fun currentHeaderDependencies(declaration: CfirDeclaration) {
            when (declaration) {
                is CfirCallableDeclaration -> element(declaration.returnTypeRef, declaration)
                is CfirTypeAlias -> element(declaration.expandedTypeRef, declaration)
                is CfirExtend -> element(declaration.extendedTypeRef, declaration)
                else -> Unit
            }
            (declaration as? CfirTypeParameterRefsOwner)?.typeParameters?.filterIsInstance<CfirTypeParameter>()?.forEach(::reference)
            when (declaration) {
                is CfirFunction -> declaration.valueParameters.forEach(::reference)
                is CfirEnumConstructor -> declaration.valueParameters.forEach(::reference)
                is CfirClassLikeDeclaration -> {
                    declaration.superTypeRefs.forEach { element(it, declaration) }
                    declaration.declarations.forEach(::reference)
                    members(declaration).forEach(::reference)
                }
                is CfirExtend -> {
                    declaration.superTypeRefs.forEach { element(it, declaration) }
                    declaration.declarations.forEach(::reference)
                    members(declaration).forEach(::reference)
                }
                is CfirTypeParameter -> (declaration.bounds + declaration.explicitUpperBounds).forEach { element(it, declaration) }
                else -> Unit
            }
        }

        private fun element(element: CfirElement, context: CfirDeclaration) {
            // declaration 的首次读取必须晚于其 normal header 完成，不能在父节点遍历时读 Unselected。
            if (element is CfirDeclaration) {
                reference(element)
                return
            }
            if (!elements.getOrPut(element) { identitySet() }.add(context)) return
            dependencyRevision++
            elementFields(element, context)
        }

        private fun elementFields(element: CfirElement, context: CfirDeclaration) {
            addType((element as? CfirExpression)?.coneTypeOrNull, context.moduleData.session)
            addType((element as? CfirResolvedTypeRef)?.coneType, context.moduleData.session)
            (element as? CfirResolvedNamedReference)?.resolvedSymbol?.takeIf { it.isBound }?.cfir?.let(::reference)
            if (element is CfirResolvedAppliedCallableReference) {
                addType(element.substitutedReturnType, context.moduleData.session)
                addType(element.callSiteType, context.moduleData.session)
                element.substitutedParameterTypes.forEach { addType(it, context.moduleData.session) }
                (element.matchedParentType as? CfirReferenceParentType.Instantiated)?.let { addType(it.type, context.moduleData.session) }
            }
            (element as? CfirConstantValueExpression)?.let { constant(it.constantValue, context) }
            CfirExportAnalysisInputSnapshot.sourceChildren(element, context).forEach {
                element(it, element as? CfirDeclaration ?: context)
            }
        }

        private fun constant(value: CfirConstantValue, context: CfirDeclaration) {
            val input = constants.getOrPut(context.moduleData.session) { IdentityHashMap() }
            if (input.containsKey(value)) return
            input[value] = context
            dependencyRevision++
            constantFields(value, context)
        }

        private fun constantFields(value: CfirConstantValue, context: CfirDeclaration) {
            CfirExportConstantGraph.capture(value).nodes.forEach { node ->
                addType(node.type, context.moduleData.session)
                when (node) {
                    is CfirExportConstantNode.Object -> node.fields.forEach { field ->
                        reference(field.symbol.cfir)
                        addType(field.type, context.moduleData.session)
                    }
                    is CfirExportConstantNode.Enum -> reference(node.constructor.cfir)
                    else -> Unit
                }
            }
        }

        private fun addType(type: ConeCangJieType?, session: CfirSession) {
            if (type != null && queuedTypes.getOrPut(session) { identitySet() }.add(type)) pendingTypes += type to session
        }

        private fun type(type: ConeCangJieType, session: CfirSession) {
            val children = type.captureExportTypeDependencies()
            val previous = types[session]?.get(type)
            // 无缓存的 alias 展开每次可能创建新 Cone；只有真实读取输入变化才重建本准备域的产品。
            // 此复用发生在首次发布之前，不接受任何已发布 Result/快照的字段漂移。
            if (previous != null && sameIdentities(previous.children, children) &&
                previous.abbreviatedType === type.abbreviatedType &&
                previous.payload.hasSameSourceFields(type.captureExportTypePayload(previous.declaration)) &&
                previous.expansionInputs.all { (alias, input) -> input.isCurrent(alias) }) return
            fun target(type: ConeCangJieType?): CfirDeclaration? = when (type) {
                is ConeTypeAliasType -> session.symbolProvider.getClassLikeSymbolByClassId(type.classId)?.cfir
                null -> null
                else -> type.toSymbol(session)?.cfir
            }
            val declaration = target(type)
            val abbreviatedType = type.abbreviatedType
            val abbreviatedDeclaration = target(abbreviatedType)
            val expansionInputs = IdentityHashMap<CfirTypeAlias, CfirExportDeclarationTypeInput>()
            val expanded = type.fullyExpandedType(session, expandedConeType = { alias ->
                val rhs = alias.expandedConeTypeWithEnsuredPhase()
                expansionInputs[alias] = CfirExportDeclarationTypeInput.capture(alias)
                reference(alias)
                rhs
            })
            val expandedDeclaration = target(expanded)
            types.getOrPut(session) { IdentityHashMap() }[type] = Type(Collections.unmodifiableList(children.toList()),
                declaration, abbreviatedType, abbreviatedDeclaration, expanded, expandedDeclaration,
                type.captureExportTypePayload(declaration), Collections.unmodifiableMap(expansionInputs))
            dependencyRevision++
            children.forEach { addType(it, session) }
            addType(abbreviatedType, session)
            if (expanded !== type) addType(expanded, session)
            listOfNotNull(declaration, abbreviatedDeclaration, expandedDeclaration).forEach(::reference)
        }

        /** 真实 scope/extend 产品由其原 owner 完成；capture 不再发起解析请求。 */
        private fun interfaceImplementations(owner: CfirDeclaration) {
            if (!owner.origin.fromSource || owner.cjoDeclarationOrigin != null || owner !in sourceDeclarations) return
            fun add(member: CfirDeclaration) {
                implemented += member
                reference(member)
                if (member is CfirProperty) listOfNotNull(member.getter, member.setter).forEach {
                    implemented += it
                    reference(it)
                }
            }
            val body = when (owner) {
                is CfirClassLikeDeclaration -> owner.declarations
                is CfirExtend -> owner.declarations
                else -> error("Only nominal and extend owners have interface implementation products")
            }.flatMap { if (it is CfirPatternVariable) it.pattern.bindingVariables() else listOf(it) }
            when (owner) {
                is CfirInterface -> body.filterIsInstance<CfirFunction>().filter { it.status.isDefault }.forEach(::add)
                is CfirClassLikeDeclaration -> if (owner.superTypeRefs.isNotEmpty()) {
                    val session = owner.moduleData.session
                    val scope = owner.unsubstitutedScope(session, scopeSessions.getOrPut(session) { ScopeSession() }, false, CfirResolvePhase.BODY_RESOLVE)
                    body.forEach { member ->
                        when (member) {
                            is CfirNamedFunction -> scope.processOverriddenFunctions(member.symbol) { parent ->
                                reference(parent.cfir.unwrapSubstitutionOverrides())
                                if (parent.getContainingClass()?.cfir is CfirInterface) add(member)
                                ProcessorAction.NEXT
                            }
                            is CfirProperty -> scope.processOverriddenProperties(member.symbol) { parent ->
                                reference(parent.cfir.unwrapSubstitutionOverrides())
                                if (parent.getContainingClass()?.cfir is CfirInterface) add(member)
                                ProcessorAction.NEXT
                            }
                            else -> Unit
                        }
                    }
                }
                is CfirExtend -> if (owner.superTypeRefs.isNotEmpty()) {
                    body.filterIsInstance<CfirCallableDeclaration>().forEach { member ->
                        val surface = owner.moduleData.session.extendExportSurfaceService.classifyMember(
                            owner, member.symbol, CfirCallableLookupProvenance.None, owner.extendedTypeRef.coneTypeOrNull)
                        if (surface is CfirExtendMemberExportSurface.InterfaceRequirements) add(member)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun <T> readOnly(declarations: Collection<CfirDeclaration>, graphs: Collection<CfirSourceGraph>, action: () -> T): T {
        val sources = identitySet<CfirImportedDeclarationHeaderSource>()
        declarations.mapNotNull { it.contentSource as? CfirImportedDeclarationHeaderSource }.forEach(sources::add)
        graphs.forEach { graph ->
            val provenance = graph.provenance
            if (provenance is CfirSourceGraphProvenance.Published) {
                val source = provenance.owner as? CfirImportedDeclarationHeaderSource
                    ?: error("Published SOURCE requires its original read-only content capability")
                sources.add(source)
            }
        }
        val owners = sources.toList()
        fun read(index: Int): T = if (index == owners.size) action() else owners[index].withReadOnlyContent { read(index + 1) }
        return read(0)
    }

    private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap())
    private fun <T : Any> sameIdentities(first: List<T>, second: List<T>): Boolean =
        first.size == second.size && first.indices.all { first[it] === second[it] }
}
