package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.cfir.source.CfirSourceGraphPublicationOwner
import org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest
import org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult
import org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionContentSlot

import PackageFormat.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.source.CfirSourceImportedPosition
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceTarget
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue

/** 正式库 context 的 SOURCE 能力：同一 snapshot/header/type binder，完整图完成后才发布。 */
internal class CfirSourceGraphLoader(private val context: CfirDeserializationContext,
    private val declarations: CfirDeclDeserializer = CfirDeclDeserializer.forSourceGraph(context)) : CfirSourceHeaderDecodingContext {
    override val pkg: PackageFormat.Package get() = context.pkg
    private val types = context.createTypeDeserializer()

    /** 整包SOURCE是显式业务请求；header目录提供根槽，不先读正文建立逆向索引。 */
    fun load(): CfirDeserializedSourceGraph {
        context.materialize {
            if (context.sourceGraph == null) {
                val headers = context.sourceHeaderRegistry
                var roots = packageRoots(headers, includeBodies = true, includeInitializers = true)
                while (true) {
                    context.registerDiscoveredHeaders(CjoSourceOwnershipPlan.discover(context.sourceHeaderRegistry, roots))
                    val discovered = packageRoots(context.sourceHeaderRegistry, includeBodies = true, includeInitializers = true)
                    if (discovered == roots) { context.sourceGraph = content(roots); break }
                    roots = discovered
                }
            }
        }
        return checkNotNull(if (context.inMaterialization) context.sourceGraph else context.completedSourceGraph)
    }

    override fun header(function: CfirCallableDeclaration, body: FuncBody): org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader =
        context.materialize {
            context.sourceHeaders[function] ?: readSourceFunctionHeader(this, function, body).also {
                context.sourceHeaders[function] = it
            }
        }

    override fun type(reference: UInt): ConeCangJieType = types.deserializeTypeFromField(reference)
    override fun signature(function: CfirCallableDeclaration): org.cangnova.cangjie.cfir.source.CfirSourceCallableSignature =
        context.callableSignature(function)

    /** 一个保存槽请求只消费自身下行与已完成借用记录；缺产品不扩张拥有域。 */
    internal fun discoverFunctionHeaders(function: org.cangnova.cangjie.cfir.declarations.CfirFunction) = context.materialize {
        check(context.isPublishedDeclaration(function))
        val registry = CjoSourceOwnershipPlan.discover(context.sourceHeaderRegistry,
            listOf(CjoSourceContentRoot.FunctionBody(context.sourceFunctionIdentity(function))))
        // 这里只登记原owning槽证明，不绑定local symbols、不创建canonical/ordinary正文产品。
        context.registerDiscoveredHeaders(registry)
    }

    /** 已冻结的真实内容请求，完成产品才进入目录。 */
    internal fun content(roots: List<CjoSourceContentRoot>): CfirDeserializedSourceGraph {
        val key = roots.toList()
        context.materialize {
            if (context.bodyState.selectedGraphs.containsKey(key)) return@materialize
            val selection = CjoSourceOwnershipPlan.request(context.sourceHeaderRegistry, key)
            context.registerSourceSelection(selection)
            context.withSourceSelection(selection) {
                val owned = OwnedDeclarations(selection.registeredHeaders)
                val bound = owned.prepare(selection)
                selection.borrowedBindings.forEach { binding ->
                    check(context.bodyState.expressionSources.containsKey(binding.target)) {
                        "Borrowed e ${binding.target} requires its explicitly completed owning product"
                    }
                }
                CfirCanonicalSourceDecoder(owned).decode(selection, bound).also {
                    context.bodyState.selectedGraphs[key] = it
                }
            }
        }
        return checkNotNull(context.selectedSourceGraph(key))
    }

    /** 原已登记e入口；冷裸下标没有拥有证明，调用方必须先显式请求实际保存槽。 */
    internal fun expression(reference: UInt): CfirDeserializedSourceGraph {
        val occurrence = context.sourceHeaderRegistry.expressionContexts.getValue(reference).single()
        return content(listOf(CjoSourceContentRoot.SelectedExpression(occurrence)))
    }

    fun loadHeaders() {
        context.materialize {
            if (context.headerSourceGraph == null) {
                var roots = packageRoots(context.sourceHeaderRegistry, includeBodies = false, includeInitializers = false)
                while (true) {
                    context.registerDiscoveredHeaders(CjoSourceOwnershipPlan.discover(context.sourceHeaderRegistry, roots))
                    val discovered = packageRoots(context.sourceHeaderRegistry, includeBodies = false, includeInitializers = false)
                    if (discovered == roots) { context.headerSourceGraph = content(roots); break }
                    roots = discovered
                }
            }
        }
    }

    /** 实际定义种子只沿原 header owning 边闭合，正文仅来自各自显式保存槽。 */
    internal fun definitionSnapshot(request: CjoDefinitionSnapshotRequest): CjoDefinitionSnapshotResult {
        context.definitionRequestViolation(request)?.let { return CjoDefinitionSnapshotResult.Failure(it) }
        context.completedDefinitionSnapshot(request)?.let { return it }
        context.materialize {
            val definitions = linkedSetOf<UInt>()
            val functions = linkedSetOf<CjoSourceFunctionIdentity>()
            val patterns = linkedSetOf<Int>()
            request.seeds.forEach { definitions += (checkNotNull(it.cjoDeclarationOrigin).declarationIndex + 1).toUInt() }
            fun completeHeaders() {
                val headers = context.sourceHeaderRegistry
                while (true) {
                    val size = definitions.size + functions.size + patterns.size
                    definitions.toList().mapNotNull(headers.functionByDeclaration::get).forEach(functions::add)
                    fun owns(parent: CjoSourceOwningParent): Boolean = when (parent) {
                        is CjoSourceOwningParent.Declaration -> parent.reference in definitions
                        is CjoSourceOwningParent.Function -> parent.identity in functions
                        is CjoSourceOwningParent.ParameterList -> parent.function in functions
                        is CjoSourceOwningParent.Pattern -> parent.tablePosition in patterns
                        else -> false
                    }
                    headers.declarations.values.filter { owns(it.parent) }.forEach { definitions += it.reference }
                    headers.patterns.values.filter { owns(it.parent) }.forEach { patterns += it.position }
                    if (size == definitions.size + functions.size + patterns.size) break
                }
            }
            completeHeaders()
            val roots = buildList {
                definitions.forEach { reference ->
                    add(CjoSourceContentRoot.DeclarationHeader(reference))
                    add(CjoSourceContentRoot.DeclarationAnnotations(reference))
                    if (context.sourceHeaderRegistry.declarations.getValue(reference).wire.kind == DeclKind.VarWithPatternDecl)
                        add(CjoSourceContentRoot.DeclarationPatternHeader(reference))
                    if (context.sourceHeaderRegistry.declarations.getValue(reference).wire.kind == DeclKind.FuncParam)
                        add(CjoSourceContentRoot.ParameterDefault(reference))
                }
                request.contents.forEach { requirement ->
                    val reference = (checkNotNull(requirement.declaration.cjoDeclarationOrigin).declarationIndex + 1).toUInt()
                    check(reference in definitions) { "A definition content request must belong to its actual owning header closure" }
                    when (requirement.slot) {
                        CjoDefinitionContentSlot.DECLARATION_ANNOTATIONS -> add(CjoSourceContentRoot.DeclarationAnnotations(reference))
                        CjoDefinitionContentSlot.PARAMETER_DEFAULT -> add(CjoSourceContentRoot.ParameterDefault(reference))
                        CjoDefinitionContentSlot.VARIABLE_INITIALIZER -> add(CjoSourceContentRoot.Initializer(reference))
                        CjoDefinitionContentSlot.FUNCTION_BODY -> add(CjoSourceContentRoot.FunctionBody(
                            checkNotNull(context.sourceHeaderRegistry.functionByDeclaration[reference]) {
                                "A definition body request requires its actual saved function identity"
                            }))
                    }
                }
            }.distinct()
            context.registerDiscoveredHeaders(CjoSourceOwnershipPlan.discover(context.sourceHeaderRegistry, roots))
            val graph = content(roots)
            definitions.addAll(graph.selection.ownedContent.declarations.keys)
            functions.addAll(graph.selection.ownedContent.functions.keys)
            patterns.addAll(graph.selection.ownedContent.patterns.keys)
            context.registerDefinitionScope(request, roots, definitions, functions, patterns)
        }
        return checkNotNull(context.completedDefinitionSnapshot(request))
    }

    /** 原FullId的metadata入口只绑定本次冻结header域；普通default/annotation由独立内容请求完成。 */
    internal fun headerDeclaration(index: Int): CfirDeclaration {
        val reference = (index + 1).toUInt()
        val selection = context.currentSourceSelection ?: context.metadataDemand(CjoSourceContentRoot.DeclarationHeader(reference))
        check(reference in selection.borrowedContexts.declarations)
        if (!context.inMaterialization && selection.borrowedContexts.declarations.keys.all { it.toInt() - 1 in context.declCache } &&
            selection.types.keys.all { it.toInt() - 1 in context.typeCache }) return context.declCache.getValue(index)
        return context.materialize {
            context.registerSourceSelection(selection)
            context.withSourceSelection(selection) {
                val owned = OwnedDeclarations(selection.registeredHeaders)
                owned.prepare(selection).declarations.getValue(reference)
            }
        }
    }

    private fun packageRoots(headers: CjoSourceOwnershipPlan, includeBodies: Boolean, includeInitializers: Boolean): List<CjoSourceContentRoot> = buildList {
        headers.declarations.values.forEach { declaration ->
            add(CjoSourceContentRoot.DeclarationHeader(declaration.reference))
            add(CjoSourceContentRoot.DeclarationAnnotations(declaration.reference))
            when (declaration.wire.kind) {
                DeclKind.FuncParam -> add(CjoSourceContentRoot.ParameterDefault(declaration.reference))
                DeclKind.VarDecl, DeclKind.VarWithPatternDecl -> if (includeInitializers)
                    add(CjoSourceContentRoot.Initializer(declaration.reference))
            }
        }
        if (includeBodies) headers.functions.keys.forEach { add(CjoSourceContentRoot.FunctionBody(it)) }
    }

    override fun declaration(reference: UInt): CfirDeclaration {
        require(reference != 0u && reference <= pkg.allDeclsLength.toUInt())
        val index = reference.toInt() - 1
        return checkNotNull(declarations.deserializeCanonicalDeclaration(index))
    }

    /** 拥有阶段先完成所有真实 d/F header；引用阶段只查这些身份，不再创建局部目标。 */
    private inner class OwnedDeclarations(private val plan: CjoSourceOwnershipPlan) : CfirCanonicalSourceDecodingContext {
        override val publicationOwner: CfirSourceGraphPublicationOwner get() = context.sourceGraphPublicationOwner
        override val pkg: PackageFormat.Package get() = context.pkg
        private val functions get() = context.sourceFunctions
        private val declarationsInProgress = HashSet<UInt>()
        private val functionsInProgress = HashSet<CjoSourceFunctionIdentity>()
        /** 首次 draft 只属于本次 decoder；未完成 selector 的 P 不进入共享完成池。 */
        private val patternDrafts = HashMap<Int, org.cangnova.cangjie.cfir.source.CfirSourcePattern>()

        fun header(reference: UInt): CfirDeclaration = bindDeclaration(reference)

        private lateinit var selection: CjoSourceContentSelection

        fun prepare(selected: CjoSourceContentSelection): CfirBoundSourceContexts {
            selection = selected
            check(selected.registeredHeaders === plan)
            selected.borrowedContexts.declarations.keys.forEach(::bindDeclaration)
            selected.borrowedContexts.functions.keys.forEach(::bindFunction)
            selected.typeBindings.forEach { binding -> if (binding.reference != 0u) type(binding.reference) }
            selected.declarationBindings.forEach { binding -> bindTargetHeader(binding.target) }
            context.completeTypeHeaders()
            selected.borrowedContexts.declarations.values.forEach { entry ->
                val declaration = declaration(entry.reference)
                if (entry.isPatternBinding) check(declaration is CfirPatternBindingVariable)
                if (declaration is CfirValueParameter) {
                    val owner = entry.scope.declarationContext as CjoSourceContext.Function
                    check(declaration.containingDeclarationSymbol === function(owner.identity).symbol)
                }
                entry.helperOwner?.let { owner ->
                    check((declaration as CfirNamedFunction).defaultParameterHelperInfo?.owner === function(owner))
                }
                entry.accessorOwner?.let { owner ->
                    check((declaration as CfirPropertyAccessor).propertySymbol === declaration(owner).symbol)
                }
            }
            val boundDeclarations = selected.borrowedContexts.declarations.keys.associateWith(::declaration)
            val boundFunctions = selected.borrowedContexts.functions.keys.associateWith(::function)
            val owners = java.util.IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
            val contexts = java.util.IdentityHashMap<CfirCallableDeclaration, org.cangnova.cangjie.cfir.source.CfirSourceFunctionContext>()
            val headers = java.util.IdentityHashMap<CfirCallableDeclaration, org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader>()
            selected.borrowedContexts.declarations.values.forEach { entry ->
                owners[boundDeclarations.getValue(entry.reference)] = owningDeclaration(entry)
            }
            selected.borrowedContexts.functions.values.forEach { entry ->
                val function = boundFunctions.getValue(entry.identity)
                contexts[function] = org.cangnova.cangjie.cfir.source.CfirSourceFunctionContext(
                    declarationContext(entry.lexicalScope.declarationContext), entry.lexicalScope.function?.let(::function))
                headers[function] = header(function, entry.body)
            }
            return CfirBoundSourceContexts(selected, boundDeclarations, owners, boundFunctions, contexts, headers)
        }

        private fun bindOptionalDeclarationTarget(reference: FullId?) {
            when (context.fullIdResolver.resolveSourceReferenceTarget(reference)) {
                is ResolvedSourceReferenceTarget.Declaration -> bindTargetHeader(checkNotNull(reference))
                is ResolvedSourceReferenceTarget.Absent -> Unit
                is ResolvedSourceReferenceTarget.Package -> error("Declaration slot cannot bind a package target")
            }
        }

        private fun bindTargetHeader(reference: FullId) {
            val resolved = context.fullIdResolver.resolve(reference)
            val target = resolved as? ResolvedFullId.Declaration
                ?: error("SOURCE target must identify its actual declaration: pkg=${reference.pkgId}, index=${reference.index}, decl=${reference.decl}, resolved=$resolved")
            if (target.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) bindDeclaration(target.declaration.formattedIndex)
            else checkNotNull(context.materializeExternalDeclaration(target))
        }

        private fun bindDeclaration(reference: UInt): CfirDeclaration {
            context.requireSourceDeclaration(reference.toInt() - 1)
            val entry = plan.declarations.getValue(reference)
            val index = reference.toInt() - 1
            context.declarations[index]?.let { return it }
            check(declarationsInProgress.add(reference)) { "SOURCE declaration owning cycle" }
            try {
                entry.accessorOwner?.let { owner ->
                    bindDeclaration(owner)
                    return checkNotNull(context.declarations[index])
                }
                entry.helperOwner?.let { owner ->
                    bindFunction(owner)
                    return checkNotNull(context.declarations[index])
                }
                if (entry.wire.kind == DeclKind.FuncParam) {
                    val owner = entry.scope.declarationContext as CjoSourceContext.Function
                    bindFunction(owner.identity)
                    return checkNotNull(context.declarations[index])
                }
                val owner = bindContext(entry.scope.declarationContext)
                context.declarations[index]?.let { return it }
                return when {
                    entry.isPatternBinding -> declarations.deserializeSourcePatternBinding(index, owner, plan.isLocalDeclaration(reference))
                    else -> declarations.deserializeSourceDeclaration(index, owner, plan.isLocalDeclaration(reference))
                }
            } finally {
                declarationsInProgress.remove(reference)
            }
        }

        private fun bindFunction(identity: CjoSourceFunctionIdentity): CfirCallableDeclaration {
            functions[identity]?.let { return it }
            check(functionsInProgress.add(identity)) { "SOURCE FuncBody owning cycle" }
            try {
                val entry = plan.functions.getValue(identity)
                val function = if (entry.declaration != null) {
                    val declaration = bindDeclaration(entry.declaration)
                    check(declaration is CfirFunction || declaration is CfirEnumConstructor) {
                        "A saved FuncBody must retain its actual callable owner"
                    }
                    declaration as CfirCallableDeclaration
                } else {
                    val owner = bindContext(entry.lexicalScope.declarationContext)
                    val lambda = plan.expressionContexts.getValue(checkNotNull(entry.lambda)).single()
                    declarations.deserializeSourceLambda(identity, entry.body, type(lambda.type) as ConeFunctionType, owner)
                }
                check(functions.putIfAbsent(identity, function).let { it == null || it === function }) {
                    "A saved FuncBody must retain its first bound callable owner"
                }
                return function
            } finally {
                functionsInProgress.remove(identity)
            }
        }

        private fun bindContext(value: CjoSourceContext): CfirDeclaration? = when (value) {
            CjoSourceContext.Package -> null
            is CjoSourceContext.Declaration -> bindDeclaration(value.reference)
            is CjoSourceContext.Function -> bindFunction(value.identity)
        }

        override fun declaration(reference: UInt): CfirDeclaration {
            check(reference in selection.borrowedContexts.declarations) { "SOURCE declaration is outside its frozen header demand" }
            return checkNotNull(context.declarations[reference.toInt() - 1]) { "SOURCE declaration header was not reserved" }
        }

        override fun function(identity: CjoSourceFunctionIdentity): CfirCallableDeclaration {
            check(identity in selection.borrowedContexts.functions)
            return functions.getValue(identity)
        }

        override fun declarationContext(value: CjoSourceContext): CfirDeclaration? = when (value) {
            CjoSourceContext.Package -> null
            is CjoSourceContext.Declaration -> declaration(value.reference)
            is CjoSourceContext.Function -> function(value.identity)
        }

        override fun owningDeclaration(entry: CjoSourceOwnedDeclaration): CfirDeclaration? = when (val parent = entry.parent) {
            CjoSourceOwningParent.Package -> null
            is CjoSourceOwningParent.Declaration -> declaration(parent.reference)
            is CjoSourceOwningParent.ParameterList -> function(parent.function)
            is CjoSourceOwningParent.Function -> function(parent.identity)
            is CjoSourceOwningParent.Expression, is CjoSourceOwningParent.Pattern,
            is CjoSourceOwningParent.AnnotationArgument -> declarationContext(entry.scope.expressionContext)
        }

        override fun target(reference: FullId): CfirDeclaration {
            val target = context.fullIdResolver.resolve(reference) as? ResolvedFullId.Declaration
                ?: error("SOURCE target does not identify a declaration")
            return if (target.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) {
                declaration(target.declaration.formattedIndex)
            } else checkNotNull(context.materializeExternalDeclaration(target))
        }

        override fun referenceTarget(reference: FullId?): CfirSourceReferenceTarget = when (val target = context.fullIdResolver.resolveSourceReferenceTarget(reference)) {
            is ResolvedSourceReferenceTarget.Declaration -> CfirSourceReferenceTarget.Declaration(if (target.value.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE)
                declaration(target.value.declaration.formattedIndex) else checkNotNull(context.materializeExternalDeclaration(target.value)))
            is ResolvedSourceReferenceTarget.Package -> CfirSourceReferenceTarget.Package(target.name)
            is ResolvedSourceReferenceTarget.Absent -> CfirSourceReferenceTarget.Absent(target.reason)
        }

        override fun type(reference: UInt): ConeCangJieType = this@CfirSourceGraphLoader.type(reference)
        override fun signature(function: CfirCallableDeclaration): org.cangnova.cangjie.cfir.source.CfirSourceCallableSignature =
            context.callableSignature(function)

        override fun header(function: CfirCallableDeclaration, body: FuncBody): org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader =
            this@CfirSourceGraphLoader.header(function, body)

        override fun position(expression: Expr): CfirSourceImportedPosition =
            context.sourcePositions(expression.begin, expression.end)

        override fun position(pattern: Pattern): CfirSourceImportedPosition =
            context.sourcePositions(pattern.begin, pattern.end)

        override fun typeReference(reference: UInt): CfirTypeRef = buildResolvedTypeRef {
            coneType = type(reference)
            typeUseOrigin = CfirTypeRefOrigin.SEMANTIC
        }

        override fun constants(pattern: Pattern, hostPosition: Int): List<CfirConstantValue> = context.constDeserializer.read(pattern, hostPosition)

        override fun constant(info: VarInfo, type: UInt, hostPosition: Int): CfirConstantValue? = context.constDeserializer.read(info, type, hostPosition)

        override fun expressionIdentity(reference: UInt) = context.bodyState.identities.getOrPut(reference) {
            org.cangnova.cangjie.cfir.source.CfirSourceExpressionIdentity.create()
        }

        override fun expressionOrigin(identity: org.cangnova.cangjie.cfir.source.CfirSourceExpressionIdentity) =
            context.bodyState.expressions[identity]

        override fun expressionSource(reference: UInt) = context.bodyState.expressionSources[reference]

        override fun borrowedExpressionSource(reference: UInt) = checkNotNull(context.bodyState.expressionSources[reference]) {
            "SOURCE borrowed e $reference requires its actual completed binding preparation"
        }

        override fun matchCaseSelector(reference: UInt): UInt = selection.matchCaseSelectors.getValue(reference)

        override fun savedReference(reference: PackageFormat.FullId?) = context.savedSourceReference(reference)

        override fun patternIdentity(position: Int) = context.bodyState.patternIdentities.getOrPut(position) {
            org.cangnova.cangjie.cfir.source.CfirSourcePatternIdentity.create()
        }

        override fun patternRecord(position: Int, produce: () -> org.cangnova.cangjie.cfir.source.CfirSourcePattern) =
            context.bodyState.patternRecords[position] ?: patternDrafts[position] ?: run {
                check(context.bodyState.patternRecordsInProgress.add(position)) { "SOURCE pattern owning cycle" }
                try { produce().also { patternDrafts[position] = it } }
                finally { check(context.bodyState.patternRecordsInProgress.remove(position)) }
            }

        override fun patternSource(position: Int) = context.bodyState.patternSources[position]

        /** finish 已冻结并补齐 selector/owner；池登记完成产品，后续 reader 不读取原 draft。 */
        override fun registerCompletedRecords(graph: org.cangnova.cangjie.cfir.source.CfirSourceGraph,
            references: Map<UInt, org.cangnova.cangjie.cfir.source.CfirSourceExpressionIdentity>) {
            fun pattern(value: org.cangnova.cangjie.cfir.source.CfirSourcePattern) {
                val position = context.bodyState.patternIdentities.entries.single { it.value === value.identity }.key
                check(context.bodyState.patternRecords.putIfAbsent(position, value).let { it == null || it === value }) {
                    "A SOURCE P must retain its first completed pattern product"
                }
                val firstGraph = context.bodyState.patternSources.getOrPut(position) { graph }
                check(firstGraph.pattern(value.identity) === value)
                value.patterns.forEach(::pattern)
            }
            references.forEach { (reference, identity) ->
                val record = graph.expression(identity)
                check(context.bodyState.records.putIfAbsent(reference, record).let { it == null || it === record }) {
                    "A SOURCE e must retain its first completed expression product"
                }
                val firstGraph = context.bodyState.expressionSources.getOrPut(reference) { graph }
                check(firstGraph.expression(identity) === record)
                when (val value = record.payload) {
                    is org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload.Try -> value.patterns.forEach(::pattern)
                    is org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload.MatchCase -> value.patterns.forEach(::pattern)
                    is org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload.LetPatternDestructor -> value.patterns.forEach(::pattern)
                    is org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload.ForIn -> pattern(value.pattern)
                    else -> Unit
                }
            }
            graph.declarationPatterns.values.forEach(::pattern)
        }
    }
}
