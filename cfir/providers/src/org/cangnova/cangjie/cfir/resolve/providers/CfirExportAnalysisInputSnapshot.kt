package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.explicitUpperBounds
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.patterns.simpleVariableBindingOrNull
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.session.cjmpDefaultParameterState
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.source.CjSourceElement
import java.util.Collections
import java.util.IdentityHashMap

/** 导出查询使用的声明状态值；不向消费者交出活动的 declaration status。 */
data class CfirExportDeclarationStatus(
    val visibility: Visibility?,
    val isStatic: Boolean,
    val isCommon: Boolean,
    val isSpecific: Boolean,
    val isDefault: Boolean,
    val isConst: Boolean,
    val isForeign: Boolean,
    val isC: Boolean,
    val isOpen: Boolean,
    val isAbstract: Boolean,
) {
    companion object {
        internal fun capture(declaration: CfirDeclaration): CfirExportDeclarationStatus {
            val status = (declaration as? CfirMemberDeclaration)?.status
            return CfirExportDeclarationStatus(
                status?.visibility, status?.isStatic == true, status?.isCommon == true,
                status?.isSpecific == true, status?.isDefault == true, status?.isConst == true,
                status?.isForeign == true, status?.isC == true, status?.isOpen == true,
                status?.isAbstract == true,
            )
        }
    }
}

/**
 * AnalyzeFunctionLinkage 的完整输入图。
 *
 * 在 SOURCE 事实发布前收齐物理声明、辅助声明、局部定义、真实引用和类型依赖；
 * 分析阶段只读这里的状态值、类型绑定与子节点边，不能临时把新的声明补进活动图。
 * 快照保留原 CFIR 对象作为身份，所有语义输入单独保存，并在发布前核验代次没有变化。
 */
@OptIn(CfirImplementationDetail::class)
internal class CfirExportAnalysisInputSnapshot private constructor(
    val roots: List<CfirDeclaration>,
    val declarations: Map<CfirDeclaration, Declaration>,
    val owners: Map<CfirDeclaration, CfirDeclaration?>,
    val sourceFiles: Map<CfirDeclaration, CfirFile>,
    val memberBodies: Map<CfirDeclaration, List<CfirDeclaration>>,
    val propertyOwners: Map<CfirFunction, CfirProperty>,
    val helperOwners: Map<CfirFunction, CfirFunction>,
    private val simplePatternOwners: Map<CfirPatternBindingVariable, CfirPatternVariable>,
    val topLevel: Set<CfirDeclaration>,
    val interfaceImplementations: Set<CfirDeclaration>,
    private val files: Map<CfirFile, File>,
    val elements: Map<CfirElement, CfirExportSourceElement>,
    private val types: Map<CfirSession, Map<ConeCangJieType, CfirExportTypeElement>>,
    private val memberInput: (CfirDeclaration) -> List<CfirDeclaration>,
    val canonicalSources: List<CfirSourceGraph>,
    private val canonicalOwners: Map<CfirDeclaration, CfirSourceGraph>,
    private val constantValues: Map<CfirVariable, CfirExportConstantGraph>,
    private val patternConstants: Map<CfirSourcePattern, List<CfirExportConstantGraph>>,
    private val preparedInput: CfirExportAnalysisInputPreparation.Result,
) {
    data class Declaration(
        val status: CfirExportDeclarationStatus,
        val facts: CfirDeclarationExportFacts,
        val origin: CfirDeclarationOrigin,
        val cjoOrigin: CfirCjoDeclarationOrigin?,
        val hasGenericNode: Boolean,
        val doNotExport: Boolean,
        val type: ConeCangJieType?,
        /** Alias 声明自身的 Type 与 RHS 类型引用是两个独立原生槽。 */
        val aliasUnderlyingType: ConeCangJieType?,
        val superTypes: List<ConeCangJieType>,
        val bounds: List<ConeCangJieType>,
        val explicitUpperBounds: List<ConeCangJieType>,
        val body: CfirBlock?,
        val defaultValue: CfirExpression?,
        val initializer: CfirExpression?,
        val isFrozen: Boolean,
        val hasExplicitC: Boolean,
        val isIntrinsic: Boolean,
        val annotations: List<CfirAnnotation>,
        val source: CjSourceElement?,
        val phase: CfirResolvePhase,
        val valueParameters: List<CfirValueParameter>,
        val typeParameters: List<CfirTypeParameter>,
        val annotationSourceExpressions: List<CfirExpression>,
        val hasInitialDefault: Boolean,
        val desugaredDefaultSource: CfirFunction?,
        val defaultAssignmentSyntax: CfirDefaultAssignmentSyntaxFacts?,
        val accessors: List<CfirPropertyAccessor>,
        val isLocalCallable: Boolean,
        val pattern: CfirPattern?,
        val patternBindings: List<CfirPatternBindingVariable>,
        /** 槽的实际 ref 与原始节点存在性由 typeUseOrigin 发布，不能通过 resolved/source 猜测。 */
        val headerTypeRef: CfirTypeRef?,
        val superTypeReferences: List<CfirTypeRef>,
        val evaluatedInitializer: org.cangnova.cangjie.cfir.CfirEvaluatorResult?,
        val initializationDependencies: org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies<CfirBasedSymbol<*>>,
        private val originalInitializationDependencies: org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies<CfirBasedSymbol<*>>,
        private val originalFacts: CfirDeclarationExportFacts,
        private val initializationOwner: CfirClassLikeDeclaration?,
        private val initializationInfo: CfirClassInitializationInfo?,
    ) {
        fun validate(declaration: CfirDeclaration) {
            var changedField = ""
            // 保留全部不变量及原有短路顺序；只为失败提供变化槽，不接受变化后的新基准。
            fun unchanged(field: String, matches: Boolean): Boolean {
                if (!matches) changedField = field
                return matches
            }
            check(unchanged("status", status == CfirExportDeclarationStatus.capture(declaration)) &&
                unchanged("originalFacts", originalFacts == declaration.exportFacts) &&
                unchanged("origin", origin == declaration.origin) &&
                unchanged("initializationOwner", initializationOwner?.classInitializationInfo(initializationOwner.moduleData.session) === initializationInfo) &&
                unchanged("initializationInfoCurrent", (initializationInfo == null || initializationInfo.isCurrent())) &&
                unchanged("cjoOrigin", cjoOrigin == declaration.cjoDeclarationOrigin) &&
                unchanged("source", source === declaration.source) &&
                unchanged("isLocalCallable", isLocalCallable == ((declaration as? CfirCallableDeclaration)?.isLocal == true)) &&
                unchanged("phase", phase == declaration.resolvePhase) &&
                unchanged("headerTypeRef", headerTypeRef === headerTypeRefOf(declaration)) &&
                unchanged("superTypeReferences", sameIdentities(superTypeReferences, superTypeRefs(declaration))) &&
                unchanged("body", body === (declaration as? CfirFunction)?.body) &&
                unchanged("defaultValue", defaultValue === (declaration as? CfirValueParameter)?.defaultValue) &&
                unchanged("initializer", initializer === (declaration as? CfirVariable)?.initializer) &&
                unchanged("evaluatedInitializer", evaluatedInitializer === (declaration as? CfirVariable)?.evaluatedInitializer) &&
                unchanged("initializationDependencies", declaration.initializationDependencies === originalInitializationDependencies) &&
                unchanged("annotations", annotations == declaration.annotations) &&
                unchanged("hasGenericNode", hasGenericNode == declaration.hasGenericNode) &&
                unchanged("doNotExport", doNotExport == declaration.originalDoNotExport) &&
                unchanged("aliasUnderlyingType", aliasUnderlyingType === (declaration as? CfirTypeAlias)?.expandedTypeRef?.coneTypeOrNull) &&
                unchanged("superTypes", sameIdentities(superTypes, superTypeRefs(declaration).mapNotNull { it.coneTypeOrNull })) &&
                unchanged("bounds", sameIdentities(bounds, (declaration as? CfirTypeParameter)?.bounds.orEmpty().mapNotNull { it.coneTypeOrNull })) &&
                unchanged("explicitUpperBounds", sameIdentities(explicitUpperBounds, (declaration as? CfirTypeParameter)?.explicitUpperBounds.orEmpty().mapNotNull { it.coneTypeOrNull })) &&
                unchanged("typeParameters", sameIdentities(typeParameters, (declaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty().filterIsInstance<CfirTypeParameter>())) &&
                unchanged("accessors", sameIdentities(accessors, accessorsOf(declaration))) &&
                unchanged("pattern", pattern === (declaration as? CfirPatternVariable)?.pattern) &&
                unchanged("patternBindings", sameIdentities(patternBindings, (declaration as? CfirPatternVariable)?.pattern?.bindingVariables().orEmpty())) &&
                unchanged("isFrozen", isFrozen == (declaration.interopInfo?.isFrozen == true)) &&
                unchanged("hasExplicitC", hasExplicitC == (declaration.interopInfo?.abiRequest?.hasExplicitC == true)) &&
                unchanged("isIntrinsic", isIntrinsic == (declaration.annotationInfo?.isIntrinsic == true)) &&
                unchanged("annotationSourceExpressions", sameIdentities(annotationSourceExpressions, annotationSourceExpressionsOf(declaration))) &&
                unchanged("hasInitialDefault", hasInitialDefault == ((declaration as? CfirValueParameter)?.cjmpDefaultParameterState()?.hasInitial == true)) &&
                unchanged("desugaredDefaultSource", desugaredDefaultSource === (declaration as? CfirValueParameter)
                    ?.takeIf { it.origin.fromSource }?.cjmpDefaultParameterState()?.desugaredDefaultSource?.cfir) &&
                unchanged("defaultAssignmentSyntax", defaultAssignmentSyntax == (declaration as? CfirValueParameter)?.defaultAssignmentSyntaxFacts) &&
                unchanged("valueParameters", valueParameters == when (declaration) {
                    is CfirFunction -> declaration.valueParameters
                    is CfirEnumConstructor -> declaration.valueParameters
                    else -> emptyList<CfirValueParameter>()
                })) {
                "Export declaration changed while its resolved input was being captured: ${declaration.symbol}, field=$changedField"
            }
        }
    }

    data class File(
        val sourceFile: CjSourceFile?,
        val name: String,
        val packageName: String,
        val isMacroPackage: Boolean,
        val declarations: List<CfirDeclaration>,
        val coordinates: CfirExportSourceCoordinates?,
    )

    fun declaration(declaration: CfirDeclaration): Declaration = declarations.getValue(declaration)
    internal fun <T> withReadOnlyInput(action: () -> T): T = preparedInput.withReadOnlyInput(action)
    /** 内容拥有者的图来源唯一；借用的外部 target 不能由此取得整个包的正文。 */
    fun canonicalSourceGraphOf(declaration: CfirDeclaration): CfirSourceGraph = canonicalOwners.getValue(declaration)
    fun hasCanonicalSourceOf(declaration: CfirDeclaration): Boolean = canonicalOwners.containsKey(declaration)
    fun constantValueOf(variable: CfirVariable): CfirExportConstantGraph? = constantValues[variable]
    fun constantValuesOf(pattern: CfirSourcePattern): List<CfirExportConstantGraph> = patternConstants.getValue(pattern)
    /** 原始 IMPORTED 位与 CJO loader 的来源身份是不同事实；SOURCE 准入只读本代的真实归属。 */
    fun isSourceDeclaration(declaration: CfirDeclaration): Boolean =
        declaration(declaration).let { it.origin.fromSource && it.cjoOrigin == null } && sourceFiles.containsKey(declaration)

    /** CJO 的正文／初值边由 wire loader 持有，不属于 SOURCE 图的查询域。 */
    fun sourceDeclaration(declaration: CfirDeclaration): Declaration {
        val input = declaration(declaration)
        check(isSourceDeclaration(declaration)) {
            "Source content requires a declaration owned by the captured source graph: " +
                "${declaration.symbol}, origin=${input.origin}, capturedSource=${sourceFiles.containsKey(declaration)}, " +
                "cjoOrigin=${input.cjoOrigin}"
        }
        return input
    }
    fun patternOf(variable: CfirPatternVariable): CfirPattern = checkNotNull(sourceDeclaration(variable).pattern)
    fun patternBindingsOf(variable: CfirPatternVariable): List<CfirPatternBindingVariable> = declaration(variable).patternBindings
    fun simplePatternOwnerOf(binding: CfirPatternBindingVariable): CfirPatternVariable? {
        declaration(binding)
        return simplePatternOwners[binding]
    }
    fun type(type: ConeCangJieType, session: CfirSession): CfirExportTypeElement {
        check(types[session]?.containsKey(type) == true) {
            "Export type is absent from the captured input: $type, prepared=${preparedInput.containsType(type, session)}"
        }
        return types.getValue(session).getValue(type)
    }
    fun file(file: CfirFile): File = files.getValue(file)
    /** 优先使用完成图的真实拥有槽；无 canonical 内容的物理 header 使用同代容器关系。 */
    fun owningParentOf(declaration: CfirDeclaration): CfirDeclaration? {
        canonicalOwners[declaration]?.declarationOwners?.let { parents ->
            if (parents.containsKey(declaration)) return parents[declaration]
        }
        (declaration as? CfirFunction)?.let { helper ->
            helperOwners[helper]?.let { owner ->
                return declaration(owner).valueParameters.single { declaration(it).desugaredDefaultSource === helper }
            }
        }
        return owners.getValue(declaration)
    }
    fun element(element: CfirElement): CfirExportSourceElement = elements.getValue(element)
    /** 原生 outerDecl 与 CFIR 词法归属分开：局部变量没有 outerDecl，lambda 不是 Decl。 */
    fun nativeOuterOf(declaration: CfirDeclaration): CfirDeclaration? {
        if (declaration is CfirVariable && declaration(declaration).isLocalCallable) return null
        var owner = owners.getValue(declaration)
        val visited = identitySet<CfirDeclaration>()
        while (owner != null) {
            check(visited.add(owner)) { "A source declaration has cyclic native ownership" }
            if (owner is CfirAnonymousFunction || owner is CfirVariable && declaration(owner).isLocalCallable) {
                owner = owners.getValue(owner)
                continue
            }
            if (owner is CfirPatternVariable) {
                return simplePatternOwners.entries.firstOrNull { it.value === owner }?.key ?: owner
            }
            return owner
        }
        return null
    }
    /** 简单模式只是语法容器；原生 VarDecl 初值在两条 raw builder 中可由容器持有。 */
    fun initializerOf(variable: CfirVariable): CfirExpression? {
        val ownInitializer = sourceDeclaration(variable).initializer
        if (ownInitializer != null || variable !is CfirPatternBindingVariable) return ownInitializer
        val patternOwner = simplePatternOwners[variable] ?: return null
        return sourceDeclaration(patternOwner).initializer
    }
    /** literal 的 const 由成功类型检查发布；const owner/helper 的合法性结果不是 assignment.isConst。 */
    fun defaultAssignmentFactsOf(parameter: CfirValueParameter): CfirDefaultAssignmentExportFacts {
        val declaration = sourceDeclaration(parameter)
        val assignment = checkNotNull(declaration.defaultValue) { "A default assignment fact requires its source expression" }
        val syntax = checkNotNull(declaration.defaultAssignmentSyntax) {
            "Source default assignment requires its original syntax facts"
        }
        return CfirDefaultAssignmentExportFacts(
            assignmentIsConst = syntax.literalConst && element(assignment).type?.containsErrorType() == false,
            assignmentIsLiteral = syntax.isLiteral,
            parameterIsToolAdded = declaration.facts.isToolAdded,
        )
    }
    fun targetOf(access: CfirQualifiedAccessExpression): CfirDeclaration? =
        elements.getValue(access).calleeReference?.let { elements.getValue(it).target }

    /** 遍历冻结的实际边，context 依词法声明进入／离开；不读取本代之后的 body 列表。 */
    fun walk(root: CfirElement, context: CfirDeclaration, action: (CfirExportSourceElement, CfirDeclaration) -> Unit) {
        val visited = identitySet<CfirElement>()
        fun visit(element: CfirElement, owner: CfirDeclaration) {
            if (!visited.add(element)) return
            val snapshot = elements.getValue(element)
            val current = element as? CfirDeclaration ?: owner
            action(snapshot, current)
            snapshot.children.forEach { visit(it, current) }
        }
        visit(root, context)
    }

    /** SOURCE 事实和分析结果只能在完整输入仍属于同一代时发布。 */
    fun validateUnchanged() = preparedInput.withReadOnlyInput {
        preparedInput.validateSignatures()
        files.forEach { (file, input) ->
            check(file.sourceFile === input.sourceFile && file.name == input.name && file.declarations == input.declarations &&
                file.packageDirective.packageFqName.asString() == input.packageName &&
                file.packageDirective.isMacroPackage == input.isMacroPackage) {
                "Export source file changed while its resolved input was being captured: ${input.name}"
            }
            input.coordinates?.validate(file.sourceFileLinesMapping)
        }
        declarations.forEach { (declaration, input) -> input.validate(declaration) }
        constantValues.values.forEach(CfirExportConstantGraph::validateUnchanged)
        patternConstants.values.flatten().forEach(CfirExportConstantGraph::validateUnchanged)
        types.forEach { (session, domain) ->
            domain.forEach { (type, input) ->
                check(input.domain === session) { "Export type was published in a different capture domain" }
                check(input.containsError == type.containsErrorType() &&
                    input.payload.hasSameSourceFields(type.captureExportTypePayload(input.declaration))) {
                    "Export type fields changed while their resolved input was being captured"
                }
                check(type.abbreviatedType === input.abbreviatedType) { "Export type abbreviation changed during capture" }
                check(sameIdentities(type.captureExportTypeDependencies(), input.children)) {
                    "Export type dependency identity changed during capture"
                }
                listOfNotNull(input.declaration, input.abbreviatedDeclaration, input.expandedDeclaration).forEach { target ->
                    check(target.symbol.isBound && target.symbol.cfir === target) { "Export type binding changed during capture" }
                }
            }
        }
        simplePatternOwners.forEach { (binding, owner) ->
            check(owner.pattern.simpleVariableBindingOrNull() === binding) {
                "Export initializer projection changed while its resolved input was being captured"
            }
        }
        memberBodies.forEach { (owner, body) ->
            check(sameIdentities(body, memberInput(owner).flatMap(::projectPattern))) {
                "Export member input changed while its resolved input was being captured: ${owner.symbol}"
            }
        }
        elements.forEach { (element, input) ->
            check(element.source === input.source && (element as? CfirExpression)?.coneTypeOrNull === input.type &&
                (element as? CfirResolvedTypeRef)?.coneType === input.resolvedType &&
                (element as? CfirResolvedNamedReference)?.resolvedSymbol?.takeIf { it.isBound }?.cfir === input.target &&
                (element as? CfirQualifiedAccessExpression)?.calleeReference === input.calleeReference &&
                sameIdentities(input.children, sourceChildren(element, input.context)) &&
                (element as? CfirResolvedAppliedCallableReference)?.substitutedReturnType === input.appliedReturnType &&
                (element as? CfirResolvedAppliedCallableReference)?.callSiteType === input.appliedCallSiteType &&
                sameIdentities(input.appliedParameterTypes,
                    (element as? CfirResolvedAppliedCallableReference)?.substitutedParameterTypes.orEmpty()) &&
                (element as? CfirResolvedBuiltinOperatorReference)?.signature == input.builtinSignature &&
                element.captureExportSourcePayload() == input.payload &&
                CfirExportSourceSpan.capture(element.source, element is CfirLiteralExpression) == input.span) {
                "Export expression changed while its resolved input was being captured"
            }
        }
    }

    companion object {
        fun capture(
            files: Collection<CfirFile>,
            roots: List<CfirDeclaration>,
            members: (CfirDeclaration) -> List<CfirDeclaration>,
            canonicalSources: Collection<CfirSourceGraph> = emptyList(),
            additionalTypeInputs: Map<CfirSession, List<ConeCangJieType>> = emptyMap(),
            additionalDeclarationInputs: Collection<CfirDeclaration> = emptyList(),
            preparedInput: CfirExportAnalysisInputPreparation.Result = CfirExportAnalysisInputPreparation.prepare(
                files, roots, members, canonicalSources, additionalTypeInputs, additionalDeclarationInputs),
        ): CfirExportAnalysisInputSnapshot {
            return preparedInput.withReadOnlyInput {
                preparedInput.validateDomain(files, roots, members, canonicalSources, additionalTypeInputs, additionalDeclarationInputs)
                Capture(files, roots, members, canonicalSources, additionalTypeInputs, additionalDeclarationInputs, preparedInput).run()
            }
        }

        private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap())
        private fun <K : Any, V> freeze(map: IdentityHashMap<K, V>): Map<K, V> =
            Collections.unmodifiableMap(IdentityHashMap(map))
        private fun <T> freeze(list: Collection<T>): List<T> = Collections.unmodifiableList(list.toList())

        private fun <T : Any> sameIdentities(first: List<T>, second: List<T>): Boolean =
            first.size == second.size && first.indices.all { first[it] === second[it] }

        private fun projectPattern(declaration: CfirDeclaration): List<CfirDeclaration> =
            if (declaration is CfirPatternVariable) declaration.pattern.bindingVariables() else listOf(declaration)

        private fun superTypeRefs(declaration: CfirDeclaration): List<CfirTypeRef> = when (declaration) {
            is CfirClassLikeDeclaration -> declaration.superTypeRefs
            is CfirExtend -> declaration.superTypeRefs
            else -> emptyList()
        }

        private fun headerTypeRefOf(declaration: CfirDeclaration): CfirTypeRef? = when (declaration) {
            is CfirCallableDeclaration -> declaration.returnTypeRef
            is CfirTypeAlias -> declaration.expandedTypeRef
            is CfirExtend -> declaration.extendedTypeRef
            else -> null
        }

        private fun accessorsOf(declaration: CfirDeclaration): List<CfirPropertyAccessor> =
            (declaration as? CfirProperty)?.let { listOfNotNull(it.getter, it.setter) }.orEmpty()

        private fun annotationSourceExpressionsOf(declaration: CfirDeclaration): List<CfirExpression> =
            if (declaration is CfirClass) declaration.annotations.filterIsInstance<CfirAnnotationCall>()
                .filter { it.annotationKind == BuiltInAnnotationKind.ANNOTATION }
                .flatMap { it.argumentList.arguments } else emptyList()

        /** 捕获和发布前核验使用同一条来源边协议，包括参数指向的非物理 helper 边。 */
        internal fun sourceChildren(element: CfirElement, context: CfirDeclaration): List<CfirElement> = buildList {
            element.acceptSourceChildren(object : CfirDefaultVisitorVoid() {
                /** AST 导出消费实际内容根；CFG 是分析关联，其完整来源边由生命周期 owner 访问。 */
                override fun visitControlFlowGraphReference(controlFlowGraphReference: CfirControlFlowGraphReference) {}

                override fun visitElement(element: CfirElement) {
                    if (context.cjoDeclarationOrigin != null &&
                        (context is CfirFunction && element === context.body ||
                            context is CfirVariable && element === context.initializer ||
                            context is CfirValueParameter && element === context.defaultValue)) return
                    add(element)
                }
            }, null)
            (element as? CfirValueParameter)?.takeIf { it.origin.fromSource }?.let { parameter ->
                parameter.defaultValueMetadata?.function?.takeIf { helper ->
                    helper.origin.fromSource && (helper.defaultParameterHelperInfo == null ||
                        helper.defaultParameterHelperInfo?.owner?.symbol === parameter.containingDeclarationSymbol)
                }?.let(::add)
            }
        }
        internal fun declarationType(declaration: CfirDeclaration): ConeCangJieType? {
            if (declaration.exportFacts.hasUnexportedSignature) return ConeInvalidType
            return when (declaration) {
            is CfirFunction -> {
                val parameters = declaration.valueParameters.map { it.returnTypeRef.coneTypeOrNull ?: return null }
                val returnType = declaration.returnTypeRef.coneTypeOrNull ?: return null
                ConeFunctionType(parameters, returnType, declaration.interopInfo?.resolvedAbi?.isCFunction == true,
                    hasVariableLenArg = declaration.hasVariableLenArg)
            }
            is CfirEnumConstructor -> if (declaration.valueParameters.isEmpty()) declaration.returnTypeRef.coneTypeOrNull else {
                val parameters = declaration.valueParameters.map { it.returnTypeRef.coneTypeOrNull ?: return null }
                val returnType = declaration.returnTypeRef.coneTypeOrNull ?: return null
                ConeFunctionType(parameters, returnType)
            }
            is CfirCallableDeclaration -> declaration.returnTypeRef.coneTypeOrNull
            is CfirTypeAlias -> ConeTypeAliasType(
                declaration.symbol.classId,
                typeArguments = declaration.typeParameters.map { it.symbol.constructType() },
            )
            // 声明self type携带自己的类型参数；builtin声明形状与普通应用CFunc<T>不同。
            is CfirBuiltInDeclaration -> {
                val arguments = declaration.typeParameters.map { it.symbol.constructType() }
                when (declaration.kind) {
                    CfirBuiltInTypeKind.ARRAY -> declaration.symbol.constructType(arguments)
                    CfirBuiltInTypeKind.VARRAY -> ConeVArrayType(arguments.single(), 0L)
                    CfirBuiltInTypeKind.CPOINTER -> ConePointerType(arguments.single())
                    CfirBuiltInTypeKind.CSTRING -> ConeCStringType()
                    CfirBuiltInTypeKind.CFUNC -> ConeFunctionType(arguments, arguments.single(), isCFunc = true)
                }
            }
            is CfirClassLikeDeclaration -> declaration.symbol.constructType(declaration.typeParameters.map { it.symbol.constructType() })
            is CfirTypeParameter -> declaration.symbol.constructType()
            is CfirExtend -> declaration.extendedTypeRef.coneTypeOrNull
            else -> null
            }
        }

    }

    private class Capture(
        private val files: Collection<CfirFile>,
        private val roots: List<CfirDeclaration>,
        private val members: (CfirDeclaration) -> List<CfirDeclaration>,
        canonicalSources: Collection<CfirSourceGraph>,
        private val additionalTypeInputs: Map<CfirSession, List<ConeCangJieType>>,
        private val additionalDeclarationInputs: Collection<CfirDeclaration>,
        private val preparedInput: CfirExportAnalysisInputPreparation.Result,
    ) {
        private val canonicalSources = freeze(canonicalSources.distinct())
        private val canonicalOwners = IdentityHashMap<CfirDeclaration, CfirSourceGraph>()
        private val canonicalLexicalOwners = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        private val constantValues = IdentityHashMap<CfirVariable, CfirExportConstantGraph>()
        private val patternConstants = IdentityHashMap<CfirSourcePattern, List<CfirExportConstantGraph>>()
        private val declarations = IdentityHashMap<CfirDeclaration, Declaration>()
        private val owners = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        private val sourceFiles = IdentityHashMap<CfirDeclaration, CfirFile>()
        private val memberBodies = IdentityHashMap<CfirDeclaration, List<CfirDeclaration>>()
        private val propertyOwners = IdentityHashMap<CfirFunction, CfirProperty>()
        private val helperOwners = IdentityHashMap<CfirFunction, CfirFunction>()
        private val simplePatternOwners = IdentityHashMap<CfirPatternBindingVariable, CfirPatternVariable>()
        private val topLevel = identitySet<CfirDeclaration>()
        private val fileInputs = IdentityHashMap<CfirFile, File>()
        private val elements = IdentityHashMap<CfirElement, CfirExportSourceElement>()
        private val types = IdentityHashMap<CfirSession, IdentityHashMap<ConeCangJieType, CfirExportTypeElement>>()
        private val pendingDeclarations = ArrayDeque<CfirDeclaration>()
        private val pendingTypes = ArrayDeque<Pair<ConeCangJieType, CfirSession>>()
        private val queuedTypes = IdentityHashMap<CfirSession, MutableSet<ConeCangJieType>>()

        fun run(): CfirExportAnalysisInputSnapshot {
            files.forEach { file ->
                captureFile(file)
                // 物理 SOURCE 域保留完整模式容器；语义 roots 单独投影，不能丢掉 wildcard initializer。
                file.declarations.forEach { captureDeclaration(it, null, file) }
            }
            val orderedRoots = roots.flatMap(::projectPattern).distinctBy { it.symbol }
            // 全图先登记实际 owner/F/header，之后才能读取任何 record.context 或借用目标。
            canonicalSources.forEach(::indexCanonicalSource)
            canonicalSources.forEach(::prepareCanonicalSource)
            orderedRoots.forEach { declaration ->
                topLevel += declaration
                if (!declarations.containsKey(declaration)) captureReferenced(declaration)
            }
            canonicalSources.forEach(::captureCanonicalSource)
            additionalTypeInputs.forEach { (domain, inputs) -> inputs.forEach { addType(it, domain) } }
            pendingDeclarations.addAll(additionalDeclarationInputs)
            // 完成 inventory 与冻结 inventory 同域；非输出的中间依赖和类型仍完整核验。
            pendingDeclarations.addAll(preparedInput.declarations)
            preparedInput.forEachType(::addType)
            while (pendingDeclarations.isNotEmpty() || pendingTypes.isNotEmpty()) {
                while (pendingDeclarations.isNotEmpty()) captureReferenced(pendingDeclarations.removeFirst())
                while (pendingTypes.isNotEmpty()) {
                    val (type, session) = pendingTypes.removeFirst()
                    captureType(type, session)
                }
            }
            val input = CfirExportAnalysisInputSnapshot(
                freeze(orderedRoots), freeze(declarations), freeze(owners), freeze(sourceFiles),
                freeze(memberBodies), freeze(propertyOwners), freeze(helperOwners), freeze(simplePatternOwners),
                Collections.unmodifiableSet(identitySet<CfirDeclaration>().apply { addAll(topLevel) }),
                preparedInput.interfaceImplementations,
                freeze(fileInputs), freeze(elements), freeze(IdentityHashMap<CfirSession, Map<ConeCangJieType, CfirExportTypeElement>>().apply {
                    types.forEach { (session, values) -> put(session, freeze(values)) }
                }),
                members,
                canonicalSources, freeze(canonicalOwners), freeze(constantValues), freeze(patternConstants), preparedInput,
            )
            input.validateUnchanged()
            return input
        }

        /**
         * SOURCE 生成声明先登记其真实 owner/file，再捕获所有类型和借用目标；不能让引用先到
         * 的 generated VarDecl 被 captureReferenced 误判为没有来源的普通 synthetic 声明。
         * 原 CFIR 语法和默认值快照保持原根，canonical 初始化与控制图由独立产品拥有。
         */
        /** 拥有 parent 与 CFIR 词法语境分开保存；全部图先完成这个登记遍。 */
        private fun indexCanonicalSource(graph: CfirSourceGraph) {
            graph.requireCanonicalOwningDeclarations()
            fun owns(declaration: CfirDeclaration) {
                val previous = canonicalOwners.put(declaration, graph)
                check(previous == null || previous === graph) { "A SOURCE owner belongs to multiple completed graphs" }
            }
            graph.forEachExportLexicalOwner { declaration, lexical -> canonicalLexicalOwners[declaration] = lexical }
            graph.forEachOwningDeclaration(::owns)
            graph.functionContexts.forEach { (function, context) ->
                (function as? CfirNamedFunction)?.defaultParameterHelperInfo?.owner?.let { owner ->
                    check(helperOwners.put(function, owner).let { it == null || it === owner })
                }
            }
        }

        private fun prepareCanonicalSource(graph: CfirSourceGraph) {
            val preparing = identitySet<CfirDeclaration>()
            fun prepare(declaration: CfirDeclaration) {
                if (declarations.containsKey(declaration)) return
                check(preparing.add(declaration)) { "A canonical declaration has cyclic lexical ownership" }
                val owner = canonicalLexicalOwners[declaration]
                if (owner != null) {
                    if (canonicalOwners[owner] === graph) prepare(owner) else captureReferenced(owner)
                }
                val file = graph.generatedDeclarations[declaration]?.file ?: owner?.let(sourceFiles::get)
                captureDeclaration(declaration, owner, file)
                preparing.remove(declaration)
            }
            graph.generatedDeclarations.forEach { (declaration, context) ->
                check(declaration.origin.fromSource && declaration.cjoDeclarationOrigin == null)
                check(declarations.containsKey(context.owner) && sourceFiles[context.owner] === context.file) {
                    "Canonical SOURCE declaration must belong to the captured source domain"
                }
                canonicalLexicalOwners[declaration] = context.owner
                prepare(declaration)
            }
            canonicalOwners.entries.filter { it.value === graph }.map { it.key }.forEach(::prepare)
            graph.expressions.values.forEach { record ->
                if (!declarations.containsKey(record.context)) {
                    if (canonicalOwners[record.context] === graph) prepare(record.context) else captureReferenced(record.context)
                }
            }
        }

        /** 值与字段的实际绑定/类型进入同一个依赖队列；此阶段不重新求值。 */
        private fun captureConstant(value: CfirConstantValue, context: CfirDeclaration): CfirExportConstantGraph =
            CfirExportConstantGraph.capture(value).also { graph ->
                graph.nodes.forEach { node ->
                    addType(node.type, context.moduleData.session)
                    when (node) {
                        is CfirExportConstantNode.Object -> node.fields.forEach { field ->
                            pendingDeclarations.addLast(field.symbol.cfir)
                            addType(field.type, context.moduleData.session)
                        }
                        is CfirExportConstantNode.Enum -> pendingDeclarations.addLast(node.constructor.cfir)
                        else -> Unit
                    }
                }
            }

        private fun captureCanonicalSource(graph: CfirSourceGraph) {
            CfirExportSourceDependencies(
                { declaration ->
                    check(declaration.symbol.isBound && declaration.symbol.cfir === declaration)
                    pendingDeclarations.addLast(declaration)
                },
                { type, context ->
                    check(declarations.containsKey(context)) { "Canonical SOURCE node has no captured declaration context" }
                    addType(type, context.moduleData.session)
                },
                { ref, context ->
                    check(declarations.containsKey(context)) { "SOURCE inherited types require their captured declaration owner" }
                    captureElement(ref, context, sourceFiles[context])
                },
                { value, context -> constantValues[context as CfirVariable] = captureConstant(value, context) },
                { pattern, context -> patternConstants[pattern] = freeze(pattern.constantValues.map { captureConstant(it, context) }) },
            ).visit(graph)
        }

        private fun projectPattern(declaration: CfirDeclaration): List<CfirDeclaration> =
            if (declaration is CfirPatternVariable) declaration.pattern.bindingVariables() else listOf(declaration)

        /** 词法定义遍历和 override 归属都基于真实声明体。 */
        private fun physicalMembers(owner: CfirDeclaration): List<CfirDeclaration> = when (owner) {
            is CfirClassLikeDeclaration -> owner.declarations
            is CfirExtend -> owner.declarations
            else -> error("Only nominal and extend declarations own member bodies")
        }

        private fun captureFile(file: CfirFile) {
            file.requireOwnedGraphAnalysisReady()
            if (!fileInputs.containsKey(file)) fileInputs[file] = File(
                file.sourceFile, file.name, file.packageDirective.packageFqName.asString(),
                file.packageDirective.isMacroPackage, freeze(file.declarations),
                CfirExportSourceCoordinates.capture(file.sourceFileLinesMapping),
            )
        }

        private fun captureDeclaration(declaration: CfirDeclaration, owner: CfirDeclaration?, file: CfirFile?) {
            declaration.requireOwnedGraphAnalysisReady()
            if (declarations.containsKey(declaration)) {
                check(owners[declaration] === owner) {
                    "An export declaration has multiple lexical owners: ${declaration.symbol}, " +
                        "existing=${owners[declaration]?.symbol}, requested=${owner?.symbol}"
                }
                return
            }
            owners[declaration] = owner
            if (file != null) {
                captureFile(file)
                sourceFiles[declaration] = file
            }
            if (owner == null && declaration !is CfirPatternVariable) topLevel += declaration
            if (file != null && declaration.cjoDeclarationOrigin == null) validateSourceDeclaration(declaration)
            val type = preparedInput.declarationType(declaration)
            val superTypes = when (declaration) {
                is CfirClassLikeDeclaration -> declaration.superTypeRefs
                is CfirExtend -> declaration.superTypeRefs
                else -> emptyList()
            }.mapNotNull { it.coneTypeOrNull }
            val bounds = (declaration as? CfirTypeParameter)?.bounds.orEmpty().mapNotNull { it.coneTypeOrNull }
            val initializationOwner = if (declaration is CfirFieldVariable && !declaration.status.isStatic) owner as? CfirClassLikeDeclaration else null
            val initializationInfo = initializationOwner?.classInitializationInfo(initializationOwner.moduleData.session)
            // Header 来源事实与当前 logical owner 的完成初始化结果分别冻结。
            // 已材料化 CJO 字段不能通过普通 setter 改写；specific 输出使用实际 owner 产品。
            val facts = if (initializationInfo?.origin == CfirClassInitializationOrigin.SOURCE && !initializationInfo.hasUnavailableConstructors) {
                check(initializationInfo.isCurrent())
                declaration.exportFacts.copy(isInitialized = initializationInfo.fieldState(declaration.symbol).definitelyInitialized)
            } else declaration.exportFacts
            declarations[declaration] = Declaration(
                CfirExportDeclarationStatus.capture(declaration), facts,
                declaration.origin, declaration.cjoDeclarationOrigin,
                declaration.hasGenericNode, declaration.originalDoNotExport, type,
                (declaration as? CfirTypeAlias)?.expandedTypeRef?.coneTypeOrNull, freeze(superTypes), freeze(bounds),
                freeze((declaration as? CfirTypeParameter)?.explicitUpperBounds.orEmpty().mapNotNull { it.coneTypeOrNull }),
                (declaration as? CfirFunction)?.body, (declaration as? CfirValueParameter)?.defaultValue,
                (declaration as? CfirVariable)?.initializer,
                declaration.interopInfo?.isFrozen == true,
                declaration.interopInfo?.abiRequest?.hasExplicitC == true,
                declaration.annotationInfo?.isIntrinsic == true, freeze(declaration.annotations),
                declaration.source, declaration.resolvePhase,
                freeze(when (declaration) {
                    is CfirFunction -> declaration.valueParameters
                    is CfirEnumConstructor -> declaration.valueParameters
                    else -> emptyList()
                }),
                freeze((declaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty().filterIsInstance<CfirTypeParameter>()),
                freeze(annotationSourceExpressionsOf(declaration)),
                (declaration as? CfirValueParameter)?.cjmpDefaultParameterState()?.hasInitial == true,
                (declaration as? CfirValueParameter)?.takeIf { it.origin.fromSource }
                    ?.cjmpDefaultParameterState()?.desugaredDefaultSource?.cfir as? CfirFunction,
                (declaration as? CfirValueParameter)?.defaultAssignmentSyntaxFacts,
                freeze(accessorsOf(declaration)),
                (declaration as? CfirCallableDeclaration)?.isLocal == true,
                (declaration as? CfirPatternVariable)?.pattern,
                freeze((declaration as? CfirPatternVariable)?.pattern?.bindingVariables().orEmpty()),
                headerTypeRefOf(declaration), freeze(superTypeRefs(declaration)),
                (declaration as? CfirVariable)?.evaluatedInitializer,
                when (val dependencies = declaration.initializationDependencies) {
                    org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies.NotCollected -> dependencies
                    is org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies.Collected ->
                        org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies.Collected(dependencies.declarations)
                },
                declaration.initializationDependencies,
                declaration.exportFacts, initializationOwner, initializationInfo,
            )
            addType(type, declaration.moduleData.session)
            addType(declarations.getValue(declaration).aliasUnderlyingType, declaration.moduleData.session)
            (declaration as? CfirVariable)?.let { variable ->
                val evaluated = variable.evaluatedInitializer as? org.cangnova.cangjie.cfir.CfirEvaluatorResult.Evaluated
                val value = (evaluated?.result as? CfirConstantValueExpression)?.constantValue
                if (value != null) constantValues[variable] = captureConstant(value, variable)
            }
            // effective default 可能从另一声明借用；通过真实引用来源捕获，不授予 consumer 拥有权。
            declarations.getValue(declaration).desugaredDefaultSource?.let { pendingDeclarations.add(it) }
            (superTypes + bounds).forEach { addType(it, declaration.moduleData.session) }
            when (declaration) {
                is CfirClassLikeDeclaration, is CfirExtend -> {
                    val body = freeze(members(declaration).flatMap(::projectPattern))
                    memberBodies[declaration] = body
                    (declaration as CfirTypeParameterRefsOwner).typeParameters.filterIsInstance<CfirTypeParameter>()
                        .forEach { captureDeclaration(it, declaration, file) }
                    // 合并表决定输出成员，真实定义仍由其物理声明或 canonical SOURCE 图登记。
                    physicalMembers(declaration).forEach { captureDeclaration(it, declaration, file) }
                    body.forEach(pendingDeclarations::addLast)
                }
                is CfirFunction -> {
                    declaration.typeParameters.filterIsInstance<CfirTypeParameter>().forEach { captureDeclaration(it, declaration, file) }
                    // helper 的词法 outer 与 ownerFunc 是两条边，必须先登记再递归参数来源图。
                    declaration.valueParameters.filter { it.origin.fromSource }.forEach { parameter ->
                        val helper = parameter.defaultValueMetadata?.function?.takeIf {
                            it.origin.fromSource && (it.defaultParameterHelperInfo == null ||
                                it.defaultParameterHelperInfo?.owner === declaration)
                        }
                        if (helper != null) {
                            check(helperOwners.put(helper, declaration).let { it == null || it === declaration }) {
                                "A default helper has multiple owner functions"
                            }
                            captureDeclaration(helper, owner, file)
                        }
                    }
                    declaration.valueParameters.forEach { captureDeclaration(it, declaration, file) }
                }
                is CfirEnumConstructor -> declaration.valueParameters.forEach { captureDeclaration(it, declaration, file) }
                // binding 的原生 outer 是语法容器的 outer，不是模式语法壳本身。
                is CfirPatternVariable -> {
                    declaration.pattern.simpleVariableBindingOrNull()?.let { binding ->
                        check(simplePatternOwners.put(binding, declaration).let { it == null || it === declaration }) {
                            "An export binding has multiple initializer containers"
                        }
                    }
                    declarations.getValue(declaration).patternBindings.forEach { captureDeclaration(it, owner, file) }
                }
                is CfirProperty -> listOfNotNull(declaration.getter, declaration.setter).forEach { accessor ->
                    propertyOwners[accessor] = declaration
                    captureDeclaration(accessor, owner, file)
                }
                else -> Unit
            }
            captureElement(declaration, declaration, file)
        }

        /**
         * FINAL 源码输入按实际解析结果验证。Kotlin 的普通 compiler dummy resolver 不逐声明
         * 发布 BODY_RESOLVE，声明 phase 只用于代次核验，不能替代 header／正文完成度证明。
         * 与 LL 的 resolved-input 判据一致，错误类型仍属于已解析诊断结果；不补类型或补解析。
         */
        private fun validateSourceDeclaration(declaration: CfirDeclaration) {
            // 模式声明是语法容器；原生 VarDecl 和名称解析对应的是 pattern 内的真实 binding。
            // 与 roots/memberBodies 的投影相同，状态检查落在 binding，容器的类型和 initializer 继续完整捕获。
            if (declaration is CfirPatternVariable) projectPattern(declaration).forEach(::validateSourceDeclaration)
            else if (declaration is CfirMemberDeclaration) check(declaration.status is CfirResolvedDeclarationStatus) {
                "Source export requires resolved declaration status: ${declaration.symbol}"
            }
            val refs = buildList {
                addAll(superTypeRefs(declaration))
                if (declaration is CfirCallableDeclaration) add(declaration.returnTypeRef)
                if (declaration is CfirTypeAlias) add(declaration.expandedTypeRef)
                if (declaration is CfirExtend) add(declaration.extendedTypeRef)
                if (declaration is CfirTypeParameter) addAll(declaration.bounds)
            }
            refs.forEach { ref -> check(ref is CfirResolvedTypeRef) {
                "Source export requires resolved header types: ${declaration.symbol}"
            } }
            declaration.annotations.filterIsInstance<CfirAnnotationCall>().forEach { annotation ->
                // 内建注解以解析后的 identity 表达语义，其 typeRef 不必变为 nominal 类型引用。
                check(annotation.annotationResolveState != CfirAnnotationResolveState.UNRESOLVED) {
                    "Source export requires resolved annotation identity: ${declaration.symbol}"
                }
            }
        }

        private fun captureElement(element: CfirElement, context: CfirDeclaration, file: CfirFile?, syntax: Boolean = false) {
            if (elements.containsKey(element)) return
            if (element is CfirDeclaration && !declarations.containsKey(element)) captureDeclaration(element, context, file)
            if (elements.containsKey(element)) return
            if (file != null && context.cjoDeclarationOrigin == null && !syntax) {
                check(element !is CfirLazyExpression && element !is CfirLazyBlock) {
                    "Source export requires materialized expression regions: ${context.symbol}"
                }
                if (element is CfirExpression && element !is CfirAnnotation) check(element.coneTypeOrNull != null) {
                    "Source export requires resolved expression types: ${context.symbol}, ${element::class.simpleName}, " +
                        "sourceOffsets=${element.source?.startOffset}..${element.source?.endOffset}"
                }
                if (element is CfirQualifiedAccessExpression) {
                    when (val reference = element.calleeReference) {
                        is CfirResolvedBuiltinOperatorReference -> {
                            check(element is CfirFunctionCall && reference.name == reference.signature.name) {
                                "A builtin binding must belong to its operator call: ${context.symbol}"
                            }
                            val signature = reference.signature
                            val receiver = element.explicitReceiver ?: element.dispatchReceiver
                            val actualKinds = listOf(receiver?.coneTypeOrNull) + element.argumentList.arguments.map { it.coneTypeOrNull }
                            val expectedKinds = listOf(signature.receiverKind) + signature.parameterKinds
                            check(actualKinds.map(BuiltinPrimitiveOperators::primitiveOperandKind) == expectedKinds &&
                                expectedKinds.none { it.isIdeal } && !signature.returnKind.isIdeal &&
                                BuiltinPrimitiveOperators.primitiveOperandKind(element.coneTypeOrNull) == signature.returnKind &&
                                signature.operation.parameterCount == element.argumentList.arguments.size) {
                                "Source export requires a completed builtin signature: ${context.symbol}, $signature"
                            }
                        }
                        is CfirResolvedNamedReference -> check(reference.resolvedSymbol.isBound) {
                            "Source export requires a bound reference target: ${context.symbol}"
                        }
                        is CfirErrorNamedReference -> Unit
                        is CfirThisReference -> check(reference.boundSymbol?.isBound == true || reference.diagnostic != null) {
                            "Source export requires a resolved this binding: ${context.symbol}"
                        }
                        is CfirSuperReference -> check(reference.superTypeRef is CfirResolvedTypeRef) {
                            "Source export requires a resolved super type: ${context.symbol}"
                        }
                        else -> error("Source export requires a resolved callee: ${context.symbol}, " +
                            "${element::class.simpleName}/${reference::class.simpleName}")
                    }
                }
                if (element is CfirThisReceiverExpression) check(
                    element.calleeReference.boundSymbol?.isBound == true || element.calleeReference.diagnostic != null,
                ) {
                    "Source this receiver requires a resolved binding: ${context.symbol}"
                }
                if (element is CfirAnnotationCall) check(element.containingDeclarationSymbol.isBound) {
                    "Source annotation requires a bound containing declaration: ${context.symbol}"
                }
            }
            val children = sourceChildren(element, context)
            val expressionType = (element as? CfirExpression)?.coneTypeOrNull
            val resolvedType = (element as? CfirResolvedTypeRef)?.coneType
            val target = (element as? CfirResolvedNamedReference)?.resolvedSymbol?.takeIf { it.isBound }?.cfir
            val applied = element as? CfirResolvedAppliedCallableReference
            elements[element] = CfirExportSourceElement(element, context, freeze(children), expressionType, resolvedType, target,
                (element as? CfirQualifiedAccessExpression)?.calleeReference, element.source,
                applied?.substitutedReturnType, applied?.callSiteType, freeze(applied?.substitutedParameterTypes.orEmpty()),
                (element as? CfirResolvedBuiltinOperatorReference)?.signature, element.captureExportSourcePayload(),
                CfirExportSourceSpan.capture(element.source, element is CfirLiteralExpression), syntax)
            (element as? CfirConstantValueExpression)?.let { captureConstant(it.constantValue, context) }
            addType(expressionType, context.moduleData.session)
            addType(resolvedType, context.moduleData.session)
            if (element is CfirResolvedAppliedCallableReference) {
                addType(element.substitutedReturnType, context.moduleData.session)
                addType(element.callSiteType, context.moduleData.session)
                element.substitutedParameterTypes.forEach { addType(it, context.moduleData.session) }
                (element.matchedParentType as? CfirReferenceParentType.Instantiated)?.let {
                    addType(it.type, context.moduleData.session)
                }
            }
            // 类型限定名与可调用引用均由 CfirResolvedNamedReference 保存目标声明。
            target?.let(pendingDeclarations::addLast)
            val childSyntax = syntax || (element is CfirAnnotationCall && element.hasParserOwnedExportArguments())
            children.forEach { child ->
                if (child is CfirDeclaration && declarations.containsKey(child)) {
                    captureElement(child, child, sourceFiles[child])
                } else captureElement(child, element as? CfirDeclaration ?: context, file, childSyntax)
            }
        }

        private fun captureReferenced(declaration: CfirDeclaration) {
            if (declarations.containsKey(declaration)) return
            (declaration as? CfirNamedFunction)?.defaultParameterHelperInfo?.owner?.let { actualOwner ->
                // Source helper 的 ownerFunc 与词法 outer/file 来自原 owner，不能借用当前 consumer。
                captureReferenced(actualOwner)
                if (declarations.containsKey(declaration)) return
            }
            (declaration as? CfirCallableDeclaration)?.unwrapSubstitutionOverrides()?.let { original ->
                if (original !== declaration) captureReferenced(original)
            }
            val owner = preparedInput.owner(declaration)
            if (owner != null) captureReferenced(owner)
            if (declarations.containsKey(declaration)) return
            val file = preparedInput.file(declaration) ?: owner?.let(sourceFiles::get)
            check(file != null || declaration.cjoDeclarationOrigin != null || declaration.exportFacts.isImported ||
                declaration.origin.generated || declaration is CfirTypeParameter) {
                "A resolved source export target has no declaration file: ${declaration.symbol}"
            }
            captureDeclaration(declaration, owner, file)
        }


        private fun addType(type: ConeCangJieType?, session: CfirSession) {
            if (type != null && queuedTypes.getOrPut(session) { identitySet() }.add(type)) pendingTypes += type to session
        }

        private fun captureType(type: ConeCangJieType, session: CfirSession) {
            val prepared = preparedInput.type(type, session)
            types.getOrPut(session) { IdentityHashMap() }[type] = CfirExportTypeElement(
                type, session, type.captureExportTypePayload(prepared.declaration), freeze(prepared.children),
                prepared.declaration, prepared.abbreviatedDeclaration, prepared.abbreviatedType,
                prepared.expandedType, prepared.expandedDeclaration, type.containsErrorType(),
            )
            prepared.children.forEach { addType(it, session) }
            prepared.abbreviatedType?.let { addType(it, session) }
            if (prepared.expandedType !== type) addType(prepared.expandedType, session)
            listOfNotNull(prepared.declaration, prepared.abbreviatedDeclaration, prepared.expandedDeclaration)
                .forEach(pendingDeclarations::addLast)
        }

    }
}
