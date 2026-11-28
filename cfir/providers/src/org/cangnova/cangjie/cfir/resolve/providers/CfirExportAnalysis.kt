package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.cjmpDefaultParameterState
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourcePattern
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Visibilities
import java.util.Collections
import java.util.IdentityHashMap

/** 包级语义输入；导出文件的 I/E/common 模式不属于声明的语言级导出性。 */
data class CfirExportAnalysisSettings(
    val noSubPackages: Boolean = false,
    val exportForTest: Boolean = false,
    val canInline: Boolean = true,
    val compileCjd: Boolean = false,
) : CfirSessionComponent

val CfirSession.exportAnalysisSettings: CfirExportAnalysisSettings by CfirSession.sessionComponentAccessor()

/** ParamInfo.defaultVal 的三个独立原生输入，均来自原 assignment 或参数自己的属性。 */
data class CfirDefaultAssignmentExportFacts(
    val assignmentIsConst: Boolean,
    val assignmentIsLiteral: Boolean,
    val parameterIsToolAdded: Boolean,
)

/**
 * 原生 AnalyzeFunctionLinkage 的不可变结果，供诊断与元数据选择共同读取。
 *
 * 这是仓颉专有语义桥：Kotlin FIR 的 status/attributes 仍分别拥有原始事实，metadata
 * serializer 只消费分析结果。结果绑定本次完整 resolved 声明图，不使用跨代次的全局缓存。
 */
interface CfirDeclarationExportAnalysis {
    /** 已冻结输入的消费区；实际库内容拥有者禁止在其中启动任何材料化事务。 */
    fun <T> withReadOnlyInput(action: () -> T): T
    val sourceExportedDeclarations: Set<CfirDeclaration>
    /** 普通分析可没有 SOURCE 规范化产品；正式正文消费者必须请求已经准备的完整图。 */
    val hasCanonicalSource: Boolean
    fun canonicalSourceGraphs(): List<CfirSourceGraph>
    fun canonicalSourceGraphOf(declaration: CfirDeclaration): CfirSourceGraph
    fun hasCanonicalSourceOf(declaration: CfirDeclaration): Boolean
    /** 完成的语义依赖 inventory；它不是本次输出的定义/正文选择。 */
    fun capturedDeclarations(): List<CfirDeclaration>
    fun constantValueOf(variable: CfirVariable): CfirExportConstantGraph?
    fun constantValuesOf(pattern: CfirSourcePattern): List<CfirExportConstantGraph>
    fun linkageOf(declaration: CfirDeclaration): CfirDeclarationLinkage
    fun statusOf(declaration: CfirDeclaration): CfirExportDeclarationStatus
    fun containsDeclaration(declaration: CfirDeclaration): Boolean
    fun isDoNotExport(declaration: CfirDeclaration): Boolean
    fun parameterCountOf(declaration: CfirDeclaration): Int
    fun isExported(declaration: CfirDeclaration): Boolean
    fun ownerOf(declaration: CfirDeclaration): CfirDeclaration?
    /** 实际拥有 parent 与词法 owner 分域；helper 属于参数，accessor 属于 property。 */
    fun owningParentOf(declaration: CfirDeclaration): CfirDeclaration?
    /** 只读取初始化检查/库恢复已发布的冻结依赖；writer 不启动收集。 */
    fun initializationDependenciesOf(declaration: CfirDeclaration): org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies<CfirBasedSymbol<*>>
    /** CFIR 词法 owner 还包含 lambda carrier；原生 Decl.outerDecl 只指真实声明，局部变量没有该外层。 */
    fun nativeOuterOf(declaration: CfirDeclaration): CfirDeclaration?
    fun sourceFileOf(declaration: CfirDeclaration): CfirFile?
    fun sourceCoordinatesOf(file: CfirFile): CfirExportSourceCoordinates?
    fun isTopLevel(declaration: CfirDeclaration): Boolean
    fun canBeSourceExported(function: CfirFunction): Boolean
    /** writer 的词法 frozen 传播在前序原始事实之上计算，不回写语义声明。 */
    fun sourceFunctionBodyIsEligible(function: CfirFunction, writerFrozen: Boolean): Boolean
    fun isInGenericDeclaration(declaration: CfirDeclaration): Boolean
    fun isGenericInCommonSerialization(declaration: CfirDeclaration, serializingCommon: Boolean): Boolean
    fun shouldExportSource(variable: CfirVariable): Boolean
    fun precedingFactsOf(declaration: CfirDeclaration): CfirDeclarationExportFacts
    fun isInInstantiatedDeclaration(declaration: CfirDeclaration): Boolean
    /** 内容选择和写出读取同代节点，不能重走活动 ordinary 子树；根查询只接受 SOURCE 声明。 */
    fun sourceElement(element: CfirElement): CfirExportSourceElement
    /** 捕获的 SOURCE 文件归属与 CJO wire 来源共同决定查询域，不由 IMPORTED 位推断。 */
    fun isSourceDeclaration(declaration: CfirDeclaration): Boolean
    fun typeElement(type: ConeCangJieType, context: CfirDeclaration): CfirExportTypeElement
    /** 类型节点自己的捕获域与当前声明的 TP 投影域分别拥有，不能在 encoder 中换域重查。 */
    fun typeElement(type: ConeCangJieType, domain: CfirSession): CfirExportTypeElement
    fun typeUseGraph(): CfirExportTypeUseGraph
    fun walkSource(root: CfirElement, context: CfirDeclaration, action: (CfirExportSourceElement, CfirDeclaration) -> Unit)
    fun functionBodyOf(function: CfirFunction): CfirBlock?
    fun defaultValueOf(parameter: CfirValueParameter): CfirExpression?
    /** 参数的 HAS_INITIAL 在同代输入中冻结，writer 不再读取活动 effective default。 */
    fun hasInitialDefaultOf(parameter: CfirValueParameter): Boolean
    /** SOURCE 默认辅助声明是独立语义边，CJO desugar 继续由原 wire 入口拥有。 */
    fun defaultHelperOf(parameter: CfirValueParameter): CfirFunction?
    fun defaultAssignmentFactsOf(parameter: CfirValueParameter): CfirDefaultAssignmentExportFacts
    /** 写出前序 frozen／自身泛型事实；不回写声明，也不把词法父节点与 ownerFunc 混同。 */
    fun isFrozenOf(declaration: CfirDeclaration): Boolean
    fun hasOwnGenericNode(declaration: CfirDeclaration): Boolean
    /** 签名和容器选择读取捕获的声明边；不能在 FINAL 重新读取活动参数或成员列表。 */
    fun declarationTypeOf(declaration: CfirDeclaration): ConeCangJieType?
    /** TypeAliasDecl.self 与 AliasInfo.aliasedTy 独立，不能把 RHS 当作声明的类型身份。 */
    fun aliasUnderlyingTypeOf(declaration: CfirTypeAlias): ConeCangJieType
    fun valueParametersOf(declaration: CfirDeclaration): List<CfirValueParameter>
    fun typeParametersOf(declaration: CfirDeclaration): List<CfirTypeParameter>
    fun superTypesOf(declaration: CfirDeclaration): List<ConeCangJieType>
    fun explicitUpperBoundsOf(parameter: CfirTypeParameter): List<ConeCangJieType>
    fun accessorsOf(property: CfirProperty): List<CfirPropertyAccessor>
    fun memberDeclarationsOf(owner: CfirDeclaration): List<CfirDeclaration>
    /** 复杂模式保留语法壳与初值，真实 binding 的身份及次序独立冻结。 */
    fun patternOf(variable: CfirPatternVariable): CfirPattern
    fun patternBindingsOf(variable: CfirPatternVariable): List<CfirPatternBindingVariable>
    fun simplePatternOwnerOf(binding: CfirPatternBindingVariable): CfirPatternVariable?
    fun annotationSourceExpressionsOf(declaration: CfirDeclaration): List<CfirExpression>
    fun annotationsOf(declaration: CfirDeclaration): List<CfirAnnotation>
    fun initializerOf(variable: CfirVariable): CfirExpression?
    fun validateSourceUnchanged()
}

/** 创建一次包图分析；逻辑成员输入可由已完成的 CJMP family 快照提供。 */
object CfirExportAnalysis {
    fun analyze(
        files: Collection<CfirFile>,
        roots: List<CfirDeclaration> = files.flatMap { file -> file.declarations.flatMap(::projectPattern) },
        members: (CfirDeclaration) -> List<CfirDeclaration> = ::physicalMembers,
        settings: CfirExportAnalysisSettings = CfirExportAnalysisSettings(),
        matchedCommon: Set<CfirDeclaration> = emptySet(),
        canonicalSources: Collection<CfirSourceGraph> = emptyList(),
        additionalTypeInputs: Map<CfirSession, List<ConeCangJieType>> = emptyMap(),
        additionalDeclarationInputs: Collection<CfirDeclaration> = emptyList(),
        preparedInput: CfirExportAnalysisInputPreparation.Result = CfirExportAnalysisInputPreparation.prepare(
            files, roots, members, canonicalSources, additionalTypeInputs, additionalDeclarationInputs),
    ): CfirDeclarationExportAnalysis = Analysis(files, roots, members, settings, matchedCommon, canonicalSources, additionalTypeInputs, additionalDeclarationInputs, preparedInput).run()

    private fun projectPattern(declaration: CfirDeclaration): List<CfirDeclaration> =
        if (declaration is CfirPatternVariable) declaration.pattern.bindingVariables() else listOf(declaration)

    private fun physicalMembers(owner: CfirDeclaration): List<CfirDeclaration> = when (owner) {
        is CfirEnum -> owner.declarations.filterNot { it is CfirConstructor && it.isPrimary }
        is CfirClassLikeDeclaration -> owner.declarations
        is CfirExtend -> owner.declarations
        else -> error("Only a class-like or extend declaration has a member body")
    }.flatMap(::projectPattern)

    private class Analysis(
        files: Collection<CfirFile>,
        rootInput: List<CfirDeclaration>,
        memberInput: (CfirDeclaration) -> List<CfirDeclaration>,
        private val settings: CfirExportAnalysisSettings,
        private val matchedCommon: Set<CfirDeclaration>,
        canonicalSources: Collection<CfirSourceGraph>,
        additionalTypeInputs: Map<CfirSession, List<ConeCangJieType>>,
        additionalDeclarationInputs: Collection<CfirDeclaration>,
        preparedInput: CfirExportAnalysisInputPreparation.Result,
    ) : CfirDeclarationExportAnalysis {
        private val input = CfirExportAnalysisInputSnapshot.capture(files, rootInput, memberInput, canonicalSources, additionalTypeInputs, additionalDeclarationInputs, preparedInput)
        override fun <T> withReadOnlyInput(action: () -> T): T = input.withReadOnlyInput(action)
        override val hasCanonicalSource: Boolean get() = input.canonicalSources.isNotEmpty()
        override fun canonicalSourceGraphs(): List<CfirSourceGraph> = input.canonicalSources
        override fun canonicalSourceGraphOf(declaration: CfirDeclaration): CfirSourceGraph = input.canonicalSourceGraphOf(declaration)
        override fun hasCanonicalSourceOf(declaration: CfirDeclaration): Boolean = input.hasCanonicalSourceOf(declaration)
        override fun capturedDeclarations(): List<CfirDeclaration> = input.declarations.keys.toList()
        override fun constantValueOf(variable: CfirVariable): CfirExportConstantGraph? = input.constantValueOf(variable)
        override fun constantValuesOf(pattern: CfirSourcePattern): List<CfirExportConstantGraph> = input.constantValuesOf(pattern)
        private val roots = input.roots
        private val sourceFiles = input.sourceFiles
        private val owners = input.owners
        private val propertyOwners = input.propertyOwners
        private val helperOwners = input.helperOwners
        private val memberBodies = input.memberBodies
        private val sourceFacts = IdentityHashMap<CfirDeclaration, CfirDeclarationExportFacts>().apply {
            input.declarations.forEach { (declaration, value) -> put(declaration, value.facts) }
        }
        // SOURCE 事实只写本次发布记录，不回写共享 declaration attributes。
        private var CfirDeclaration.analysisFacts: CfirDeclarationExportFacts
            get() = sourceFacts.getValue(this)
            set(value) { sourceFacts[this] = value }
        private fun dataOf(declaration: CfirDeclaration) = input.declaration(declaration)
        override fun sourceElement(element: CfirElement): CfirExportSourceElement = input.element(element)
        override fun isSourceDeclaration(declaration: CfirDeclaration): Boolean = input.isSourceDeclaration(declaration)
        override fun typeElement(type: ConeCangJieType, context: CfirDeclaration): CfirExportTypeElement = input.type(type, context.moduleData.session)
        override fun typeElement(type: ConeCangJieType, domain: CfirSession): CfirExportTypeElement = input.type(type, domain)
        private lateinit var selectedTypeUses: CfirExportTypeUseGraph
        override fun typeUseGraph(): CfirExportTypeUseGraph = selectedTypeUses
        override fun walkSource(root: CfirElement, context: CfirDeclaration, action: (CfirExportSourceElement, CfirDeclaration) -> Unit) =
            input.walk(root, context, action)
        override fun functionBodyOf(function: CfirFunction): CfirBlock? = input.sourceDeclaration(function).body
        override fun defaultValueOf(parameter: CfirValueParameter): CfirExpression? = input.sourceDeclaration(parameter).defaultValue
        override fun hasInitialDefaultOf(parameter: CfirValueParameter): Boolean = input.declaration(parameter).hasInitialDefault
        override fun defaultHelperOf(parameter: CfirValueParameter): CfirFunction? = input.sourceDeclaration(parameter).desugaredDefaultSource
        override fun defaultAssignmentFactsOf(parameter: CfirValueParameter): CfirDefaultAssignmentExportFacts = input.defaultAssignmentFactsOf(parameter)
        override fun isFrozenOf(declaration: CfirDeclaration): Boolean = dataOf(declaration).isFrozen
        override fun hasOwnGenericNode(declaration: CfirDeclaration): Boolean = dataOf(declaration).hasGenericNode
        override fun declarationTypeOf(declaration: CfirDeclaration): ConeCangJieType? = dataOf(declaration).type
        override fun aliasUnderlyingTypeOf(declaration: CfirTypeAlias): ConeCangJieType = checkNotNull(dataOf(declaration).aliasUnderlyingType)
        override fun valueParametersOf(declaration: CfirDeclaration): List<CfirValueParameter> = dataOf(declaration).valueParameters
        override fun typeParametersOf(declaration: CfirDeclaration): List<CfirTypeParameter> = dataOf(declaration).typeParameters
        override fun superTypesOf(declaration: CfirDeclaration): List<ConeCangJieType> = dataOf(declaration).superTypes
        override fun explicitUpperBoundsOf(parameter: CfirTypeParameter): List<ConeCangJieType> = dataOf(parameter).explicitUpperBounds
        override fun accessorsOf(property: CfirProperty): List<CfirPropertyAccessor> = dataOf(property).accessors
        override fun memberDeclarationsOf(owner: CfirDeclaration): List<CfirDeclaration> = memberBodies.getValue(owner)
        override fun patternOf(variable: CfirPatternVariable): CfirPattern = input.patternOf(variable)
        override fun patternBindingsOf(variable: CfirPatternVariable): List<CfirPatternBindingVariable> = input.patternBindingsOf(variable)
        override fun simplePatternOwnerOf(binding: CfirPatternBindingVariable): CfirPatternVariable? = input.simplePatternOwnerOf(binding)
        override fun annotationSourceExpressionsOf(declaration: CfirDeclaration): List<CfirExpression> = dataOf(declaration).annotationSourceExpressions
        override fun annotationsOf(declaration: CfirDeclaration): List<CfirAnnotation> = dataOf(declaration).annotations
        override fun initializerOf(variable: CfirVariable): CfirExpression? = input.initializerOf(variable)
        override fun validateSourceUnchanged() = input.validateUnchanged()
        private val linkages = IdentityHashMap<CfirDeclaration, CfirDeclarationLinkage>()
        private val exported = IdentityHashMap<CfirDeclaration, Boolean>()
        private val topLevel = input.topLevel
        private val sourceDeclarations = identitySet<CfirDeclaration>()
        private val visitedSourceDeclarations = identitySet<CfirDeclaration>()
        private val pendingSourceDeclarations = ArrayDeque<CfirDeclaration>()
        private val pendingTypes = ArrayDeque<Pair<ConeCangJieType, CfirDeclaration>>()
        // 原生 visitedExportedTys 的 key 是真实 Ty 节点；同 ClassId 的 private 声明不能归并。
        private val visitedTypes = identitySet<ConeCangJieType>()
        private val pendingExpressions = ArrayDeque<Pair<CfirExpression, CfirDeclaration>>()

        override val sourceExportedDeclarations: Set<CfirDeclaration>
            get() = Collections.unmodifiableSet(sourceDeclarations)

        init {
            input.declarations.forEach { (declaration, value) -> linkages[declaration] = value.facts.linkage }
        }

        fun run(): CfirDeclarationExportAnalysis {
            publishSourceFacts()
            roots.forEach(::analyzeModifier)
            roots.forEach(::performPublicType)
            while (pendingSourceDeclarations.isNotEmpty() || pendingTypes.isNotEmpty() || pendingExpressions.isNotEmpty()) {
                while (pendingSourceDeclarations.isNotEmpty()) {
                    val declaration = pendingSourceDeclarations.removeFirst()
                    if (!visitedSourceDeclarations.add(declaration)) continue
                    walkSource(declaration, declaration)
                    if (declaration is CfirPatternBindingVariable && dataOf(declaration).initializer == null) {
                        initializerOf(declaration)?.let { walkSource(it, declaration) }
                    }
                }
                while (pendingTypes.isNotEmpty()) {
                    val (type, context) = pendingTypes.removeFirst()
                    analyzeType(type, context)
                }
                while (pendingExpressions.isNotEmpty()) {
                    val (expression, declaration) = pendingExpressions.removeFirst()
                    walkSource(expression, declaration)
                }
            }
            // 最后的 owner 收敛没有 special 豁免；private owner 被真实引用提升时已不在此分支。
            roots.forEach { owner ->
                if (owner in memberBodies && linkageOf(owner) == CfirDeclarationLinkage.INTERNAL) {
                    memberBodies.getValue(owner).forEach { member ->
                        when (member) {
                            is CfirFunction -> markFunction(member, CfirDeclarationLinkage.INTERNAL)
                            is CfirProperty -> {
                                dataOf(member).accessors.forEach { markFunction(it, CfirDeclarationLinkage.INTERNAL) }
                                linkages[member] = CfirDeclarationLinkage.INTERNAL
                            }
                            is CfirVariable, is CfirEnumConstructor -> linkages[member] = CfirDeclarationLinkage.INTERNAL
                            else -> Unit
                        }
                    }
                }
            }
            input.validateUnchanged()
            selectedTypeUses = CfirExportTypeUseGraph.freeze(input, this)
            return FrozenAnalysis(this, linkages.keys.toList(), input, selectedTypeUses)
        }

        /**
         * 对位 PostTypeCheck / CheckInlineFunctions / interface implementation 的 SOURCE 发布点。
         * 每次完整图分析重新计算正文相关事实；初始 false 不被当成已完成的分析结果。
         */
        private fun publishSourceFacts() {
            val sourceNodes = owners.keys.filter(::isSourceDeclaration)
            owners.keys.forEach { it.analysisFacts = it.analysisFacts.copy(isCommonMatched = it in matchedCommon) }
            val implicitFunctions = mapOf(
                "std.core" to setOf("arrayInitByCollection", "arrayInitByFunction", "composition", "handleException",
                    "createOverflowExceptionMsg", "createArithmeticExceptionMsg", "getCommandLineArgs"),
                "std.ast" to setOf("MACRO_OBJECT", "refreshTokensPosition", "refreshPos", "unsafePointerCastFromUint8Array", "transformTokens"),
            )
            for (declaration in sourceNodes) {
                val old = declaration.analysisFacts
                val root = declaration in topLevel
                val name = when (declaration) {
                    is CfirCallableDeclaration -> declaration.symbol.callableId.callableName.asString()
                    is CfirClassLikeDeclaration -> declaration.name.asString()
                    else -> null
                }
                val file = sourceFiles[declaration]
                val markedImplicit = root && file != null && name in implicitFunctions[input.file(file).packageName].orEmpty()
                declaration.analysisFacts = old.copy(
                    isGlobal = root,
                    isImplicitlyUsed = markedImplicit || !old.sourceFactsPublished && old.isImplicitlyUsed,
                    isInline = false,
                    isInterfaceImplementation = !old.sourceFactsPublished && old.isInterfaceImplementation,
                    isCompilerAdded = old.isCompilerAdded || declaration.origin.generated,
                    isCommonMatched = declaration in matchedCommon,
                    sourceFactsPublished = false,
                )
            }
            for ((helper, parent) in helperOwners) {
                if (isSourceDeclaration(helper)) helper.analysisFacts = helper.analysisFacts.copy(
                    isGlobal = parent.analysisFacts.isGlobal,
                    isDefaultParameterHelper = true,
                )
            }
            // 完整输入已确定真实 interface 边；property 与全部 accessor 同时发布。
            input.interfaceImplementations.forEach { member ->
                if (member in sourceNodes) member.analysisFacts = member.analysisFacts.copy(isInterfaceImplementation = true)
            }
            exported.clear()
            if (settings.canInline) {
                val candidates = sourceNodes.filterIsInstance<CfirFunction>().filter {
                    ownerOf(it) !is CfirInterface && !dataOf(it).hasGenericNode && sourceFiles[it]?.let { file -> input.file(file).isMacroPackage } != true
                }
                candidates.forEach { function -> function.analysisFacts = function.analysisFacts.copy(isInline = calculateSourceInline(function)) }
                candidates.filter { canExportForInline(it) && !it.analysisFacts.isCompilerAdded }.forEach { function ->
                    helperOwners.filterValues { it === function }.keys.forEach { helper ->
                        helper.analysisFacts = helper.analysisFacts.copy(isInline = function.analysisFacts.isInline)
                    }
                }
            }
            sourceNodes.forEach { it.analysisFacts = it.analysisFacts.copy(sourceFactsPublished = true) }
        }

        private fun canExportForInline(function: CfirFunction): Boolean {
            val owner = ownerOf(function)
            val declaration = helperOwners[function] ?: function
            return (owner == null || isExported(owner)) && isExported(declaration)
        }

        /** CheckFuncInline 的32节点与引用/type排除规则；只消费当前完整解析图。 */
        private fun calculateSourceInline(function: CfirFunction): Boolean {
            if (!canExportForInline(function) || function is CfirConstructor || dataOf(function).body == null) return false
            val property = propertyOwners[function]
            val parent = helperOwners[function]
            val constOrFrozen = if (property != null) statusOf(property).isConst || dataOf(property).isFrozen
                else if (parent != null) statusOf(parent).isConst || dataOf(parent).isFrozen
                else statusOf(function).isConst || dataOf(function).isFrozen
            if (!constOrFrozen || parent is CfirConstructor || statusOf(function).isForeign || statusOf(function).isC ||
                dataOf(function).hasExplicitC || statusOf(function).isOpen || dataOf(function).isIntrinsic ||
                statusOf(function).isAbstract && function is CfirPropertyAccessor || function is CfirMainFunction ||
                function.symbol.name.asString() in setOf("\$mainInvoke", "\$test.entry") ||
                (ownerOf(function) as? CfirClassLikeDeclaration)?.name?.asString() == "CPointer") return false
            var count = 0
            var accepted = true
            fun containsInternalType(type: ConeCangJieType, seen: MutableSet<ConeCangJieType>): Boolean {
                if (!seen.add(type)) return false
                if (type.containsErrorType()) return true
                val binding = input.type(type, function.moduleData.session)
                val declaration = binding.declaration
                if (declaration != null && !isExported(declaration) && !dataOf(declaration).hasGenericNode) return true
                return binding.children.any { containsInternalType(it, seen) }
            }
            fun referenceAllowed(target: CfirDeclaration): Boolean = when (target) {
                is CfirVariable -> !(target.analysisFacts.isGlobal && !isExported(target)) &&
                    !(statusOf(target).isStatic && ownerOf(target) is CfirClassLikeDeclaration &&
                        (!isExported(checkNotNull(ownerOf(target))) || !isExported(target)))
                is CfirFunction -> dataOf(target).isIntrinsic ||
                    !statusOf(target).isForeign && !statusOf(target).isC && !dataOf(target).hasExplicitC && canExportForInline(target)
                else -> true
            }
            input.walk(checkNotNull(dataOf(function).body), function) { snapshot, _ ->
                val element = snapshot.identity
                if (accepted) {
                    if (element is CfirFunction && !element.analysisFacts.isDefaultParameterHelper) accepted = false
                    if (element is CfirExpression) {
                        if (count >= 32 || snapshot.type == null || containsInternalType(snapshot.type, identitySet())) accepted = false
                        if (element is org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression) accepted = false
                        if (element !is org.cangnova.cangjie.cfir.expressions.CfirBlock) count++
                        // CFIR 的 callee 是 reference；原生它是单独的 RefExpr。
                        if (element is org.cangnova.cangjie.cfir.expressions.CfirFunctionCall) count++
                    }
                    snapshot.target?.let { if (!referenceAllowed(it)) accepted = false }
                }
            }
            return accepted
        }

        override fun linkageOf(declaration: CfirDeclaration): CfirDeclarationLinkage =
            checkNotNull(linkages[declaration]) { "Declaration is outside the analyzed export graph" }

        override fun statusOf(declaration: CfirDeclaration): CfirExportDeclarationStatus = dataOf(declaration).status
        override fun containsDeclaration(declaration: CfirDeclaration): Boolean = owners.containsKey(declaration)
        override fun isDoNotExport(declaration: CfirDeclaration): Boolean = dataOf(declaration).doNotExport
        override fun parameterCountOf(declaration: CfirDeclaration): Int = dataOf(declaration).valueParameters.size
        override fun ownerOf(declaration: CfirDeclaration): CfirDeclaration? = owners.getValue(declaration)
        override fun nativeOuterOf(declaration: CfirDeclaration): CfirDeclaration? = input.nativeOuterOf(declaration)
        override fun sourceFileOf(declaration: CfirDeclaration): CfirFile? = sourceFiles[declaration]
        override fun owningParentOf(declaration: CfirDeclaration): CfirDeclaration? = input.owningParentOf(declaration)
        override fun initializationDependenciesOf(declaration: CfirDeclaration) = input.declaration(declaration).initializationDependencies
        override fun sourceCoordinatesOf(file: CfirFile): CfirExportSourceCoordinates? = input.file(file).coordinates
        override fun isTopLevel(declaration: CfirDeclaration): Boolean = declaration in topLevel
        override fun precedingFactsOf(declaration: CfirDeclaration): CfirDeclarationExportFacts = declaration.analysisFacts
        override fun isInInstantiatedDeclaration(declaration: CfirDeclaration): Boolean = isInInstantiatedDeclarationImpl(declaration)

        private fun baseIsExported(declaration: CfirDeclaration): Boolean {
            val visibility = statusOf(declaration).visibility ?: return true
            return when (visibility) {
                Visibilities.Public, Visibilities.Protected -> true
                Visibilities.Internal -> sourceFiles[declaration] == null || !settings.noSubPackages
                Visibilities.Private, Visibilities.PrivateToThis -> false
                else -> true
            }
        }

        override fun isExported(declaration: CfirDeclaration): Boolean = exported.getOrPut(declaration) {
            if (declaration is CfirExtend) return@getOrPut isExtendExported(declaration)
            val owner = ownerOf(declaration)
            if (owner is CfirExtend && (declaration is CfirFunction || declaration is CfirProperty)) {
                val target = dataOf(owner).type?.let { input.type(it, owner.moduleData.session).expandedDeclaration }
                if (target != null && declarationPackage(target) != declarationPackage(owner)) {
                    return@getOrPut dataOf(owner).superTypes.isNotEmpty() && baseIsExported(declaration) &&
                        declaration.analysisFacts.isInterfaceImplementation
                }
            }
            baseIsExported(declaration)
        }

        private fun isExtendExported(extend: CfirExtend): Boolean {
            val session = extend.moduleData.session
            val targetType = dataOf(extend).type ?: return false
            if (targetType.typeArguments.any { argument ->
                    val target = input.type(argument.type, session).declaration
                    target != null && !isExported(target)
                }) return false
            val target = input.type(targetType, session).expandedDeclaration
            val samePackage = target is CfirClassLikeDeclaration && declarationPackage(target) == declarationPackage(extend)
            fun upperBoundsExported(): Boolean = dataOf(extend).typeParameters.all { parameter ->
                dataOf(parameter).bounds.all { bound ->
                    val declaration = input.type(bound, session).declaration
                    declaration == null || isExported(declaration)
                }
            }
            return when {
                dataOf(extend).superTypes.isEmpty() && declarationPackage(extend) == "std.core" -> true
                dataOf(extend).superTypes.isEmpty() -> samePackage && isExported(checkNotNull(target)) &&
                    (!dataOf(extend).hasGenericNode || upperBoundsExported())
                samePackage -> isExported(checkNotNull(target))
                else -> dataOf(extend).superTypes.any { type ->
                    input.type(type, session).declaration?.let(::isExported) == true
                } && (!dataOf(extend).hasGenericNode || upperBoundsExported())
            }
        }

        private fun declarationPackage(declaration: CfirDeclaration): String = when (declaration) {
            is CfirClassLikeDeclaration -> declaration.symbol.classId.packageFqName.asString()
            is CfirCallableDeclaration -> declaration.symbol.callableId.packageName.asString()
            is CfirExtend -> if (sourceFiles.containsKey(declaration)) {
                input.file(sourceFiles.getValue(declaration)).packageName
            } else {
                checkNotNull(declaration.getDeclarationPackage()) { "A binary extend must retain its package identity" }.asString()
            }
            else -> checkNotNull(sourceFiles[declaration]) { "A source export declaration must retain its file" }
                .let { input.file(it).packageName }
        }

        private fun isSpecial(function: CfirFunction): Boolean {
            val facts = function.analysisFacts
            if (facts.isImported || statusOf(function).isForeign) return true
            val name = function.symbol.callableId.callableName.asString()
            val packageName = declarationPackage(function)
            return name in setOf("\$toAny", "\$mainInvoke") || packageName == "std.core" && name == "getCommandLineArgs" ||
                packageName in setOf("std.core", "std.ast") && facts.isImplicitlyUsed
        }

        private fun analyzeModifier(declaration: CfirDeclaration) {
            val file = sourceFiles[declaration]
            if (file != null && input.file(file).packageName == "std.core" &&
                input.file(file).name in setOf("future.cj", "runtime_call_throw_exception.cj")) return
            when (declaration) {
                is CfirExtend -> {
                    val ownerExported = settings.exportForTest || isExported(declaration)
                    linkages[declaration] = if (ownerExported) CfirDeclarationLinkage.EXTERNAL else CfirDeclarationLinkage.INTERNAL
                    functionsIn(declaration).forEach { function ->
                        val publicInstance = statusOf(function).visibility == Visibilities.Public && !statusOf(function).isStatic
                        if (!isSpecial(function) && (!isExported(function) || (!ownerExported && !publicInstance))) {
                            markFunction(function, CfirDeclarationLinkage.INTERNAL)
                        }
                    }
                }
                is CfirClassLikeDeclaration -> {
                    if (declaration is CfirTypeAlias) {
                        markVariableOrAlias(declaration)
                    } else {
                        if (!isExported(declaration) && !statusOf(declaration).isC && !dataOf(declaration).hasExplicitC)
                            linkages[declaration] = CfirDeclarationLinkage.INTERNAL
                        functionsIn(declaration).forEach { function ->
                            if (!isSpecial(function) && !isExported(function)) markFunction(function, CfirDeclarationLinkage.INTERNAL)
                        }
                        memberBodies.getValue(declaration).filter { it is CfirVariable || it is CfirProperty }
                            .forEach(::markVariableOrAlias)
                    }
                }
                is CfirFunction -> if (!isSpecial(declaration) && !isExported(declaration)) markFunction(declaration, CfirDeclarationLinkage.INTERNAL)
                is CfirProperty -> markVariableOrAlias(declaration)
                is CfirVariable -> markVariableOrAlias(declaration)
                else -> Unit
            }
        }

        private fun markVariableOrAlias(declaration: CfirDeclaration) {
            linkages[declaration] = if (isExported(declaration) || declaration.analysisFacts.isImplicitlyUsed)
                CfirDeclarationLinkage.EXTERNAL else CfirDeclarationLinkage.INTERNAL
        }

        private fun functionsIn(owner: CfirDeclaration): List<CfirFunction> = memberBodies.getValue(owner).flatMap { member ->
            when (member) {
                is CfirFunction -> listOf(member)
                is CfirProperty -> dataOf(member).accessors
                else -> emptyList()
            }
        }

        private fun markFunction(function: CfirFunction, linkage: CfirDeclarationLinkage) {
            linkages[function] = linkage
            helperOwners.filterValues { it === function }.keys.forEach { helper -> linkages[helper] = linkage }
            propertyOwners[function]?.let { property -> linkages[property] = linkage }
        }

        override fun canBeSourceExported(function: CfirFunction): Boolean = sourceFunctionBodyIsEligible(
            function, dataOf(function).isFrozen || propertyOwners[function]?.let { dataOf(it).isFrozen } == true,
        )

        override fun sourceFunctionBodyIsEligible(function: CfirFunction, writerFrozen: Boolean): Boolean {
            if (!isSourceDeclaration(function) && !hasCanonicalSourceOf(function)) return false
            val ownerFunction = helperOwners[function]
            if (function.analysisFacts.isInline) return true
            val property = propertyOwners[function]
            val constOrFrozen = statusOf(function).isConst || writerFrozen || property?.let { statusOf(it).isConst } == true
            if ((dataOf(function).hasGenericNode || isExported(function) || linkageOf(function) != CfirDeclarationLinkage.INTERNAL) && constOrFrozen) return true
            val declaration = ownerFunction ?: function
            return !isInInstantiatedDeclarationImpl(declaration) && statusOf(declaration).isDefault && nativeOuterOf(declaration) is CfirInterface
        }

        override fun isInGenericDeclaration(declaration: CfirDeclaration): Boolean = ancestry(declaration).any { dataOf(it).hasGenericNode }
        private fun isInInstantiatedDeclarationImpl(declaration: CfirDeclaration): Boolean =
            ancestry(declaration).any { it.analysisFacts.isGenericInstantiated }

        private fun ancestry(declaration: CfirDeclaration): Sequence<CfirDeclaration> = sequence {
            var current: CfirDeclaration? = declaration
            val visited = identitySet<CfirDeclaration>()
            while (current != null && visited.add(current)) {
                yield(current)
                current = if (current is CfirFunction && helperOwners.containsKey(current)) helperOwners.getValue(current) else ownerOf(current)
            }
        }

        override fun isGenericInCommonSerialization(declaration: CfirDeclaration, serializingCommon: Boolean): Boolean =
            serializingCommon && nativeOuterOf(declaration)?.let { dataOf(it).hasGenericNode } == true &&
                !(declaration is CfirConstructor && statusOf(declaration).isStatic && declaration.symbol.name.asString() == "static.init")

        private fun addSource(declaration: CfirDeclaration) {
            if (!isSourceDeclaration(declaration)) return
            sourceDeclarations += declaration
            if (declaration !in visitedSourceDeclarations) pendingSourceDeclarations += declaration
        }

        private fun addType(type: ConeCangJieType?, context: CfirDeclaration) {
            if (type != null && type !in visitedTypes) pendingTypes += type to context
        }

        private fun declarationType(declaration: CfirDeclaration): ConeCangJieType? = dataOf(declaration).type

        private fun addSuperTypes(owner: CfirDeclaration) {
            dataOf(owner).superTypes.forEach { addType(it, owner) }
        }

        private fun isLayoutMember(member: CfirDeclaration): Boolean {
            val owner = ownerOf(member)
            return member is CfirEnumConstructor || member is CfirVariable &&
                !statusOf(member).isStatic && (owner is CfirClass || owner is CfirStruct)
        }

        private fun performPublicType(declaration: CfirDeclaration) {
            if (linkageOf(declaration) == CfirDeclarationLinkage.INTERNAL ||
                declaration is CfirFunction && statusOf(declaration).isForeign) return
            if (declaration is CfirVariable && statusOf(declaration).isConst) {
                addSource(declaration)
                return
            }
            if (declaration is CfirFunction && canBeSourceExported(declaration)) {
                addSource(declaration)
                return
            }
            if (declaration !in memberBodies || declaration is CfirTypeAlias) return
            addSuperTypes(declaration)
            dataOf(declaration).annotationSourceExpressions.forEach { pendingExpressions += it to declaration }
            memberBodies.getValue(declaration).forEach(::handleTopLevelMember)
        }

        private fun handleTopLevelMember(member: CfirDeclaration) {
            when (member) {
                is CfirFinalizer -> markFunctionExternal(member)
                is CfirFunction -> if (isExported(member)) {
                    addType(declarationType(member), member)
                    if (canBeSourceExported(member)) addSource(member)
                }
                is CfirEnumConstructor -> addType(declarationType(member), member)
                is CfirProperty -> if (isExported(member)) {
                    addType(declarationType(member), member)
                    if (statusOf(member).isConst || dataOf(member).isFrozen)
                        dataOf(member).accessors.forEach(::addSource)
                }
                is CfirVariable -> {
                    if (isLayoutMember(member)) addType(declarationType(member), member)
                    if (!statusOf(member).isStatic && isInGenericDeclaration(member)) addSource(member)
                    if (!isExported(member)) return
                    addType(declarationType(member), member)
                }
                else -> Unit
            }
        }

        private fun walkSource(root: CfirElement, context: CfirDeclaration) {
            input.walk(root, context) { snapshot, current ->
                when (val element = snapshot.identity) {
                    is CfirFunction -> {
                        if (isExported(element) || !canBeSourceExported(element)) markFunctionExternal(element)
                        if (canBeSourceExported(element)) addSource(element)
                    }
                    is CfirResolvedNamedReference -> snapshot.target?.let(::setTargetLinkage)
                    is CfirResolvedTypeRef -> {
                        val binding = input.type(checkNotNull(snapshot.resolvedType), current.moduleData.session)
                        listOfNotNull(binding.abbreviatedDeclaration, binding.declaration, binding.expandedDeclaration)
                            .forEach(::setTargetLinkage)
                    }
                    else -> Unit
                }
            }
        }

        private fun ensureReferenced(declaration: CfirDeclaration) {
            check(input.declarations.containsKey(declaration)) {
                "Export target was not captured before SOURCE facts publication: " + declaration.symbol
            }
        }

        private fun setTargetLinkage(target: CfirDeclaration) {
            ensureReferenced(target)
            when (target) {
                is CfirFunction -> setFunctionTarget(target)
                is CfirProperty -> setPropertyTarget(target)
                is CfirVariable -> setVariableTarget(target)
                is CfirTypeAlias -> {
                    linkages[target] = CfirDeclarationLinkage.EXTERNAL
                    addType(dataOf(target).type, target)
                    addType(dataOf(target).aliasUnderlyingType, target)
                }
                else -> addType(declarationType(target), target)
            }
        }

        private fun isGlobalOrMember(declaration: CfirDeclaration): Boolean {
            if (declaration is CfirValueParameter || declaration is CfirTypeParameter) return false
            val owner = ownerOf(declaration)
            return declaration.analysisFacts.isGlobal || owner is CfirClassLikeDeclaration || owner is CfirExtend
        }

        private fun markFunctionExternal(function: CfirFunction) {
            val owner = ownerOf(function)
            if (owner in memberBodies) linkages[checkNotNull(owner)] = CfirDeclarationLinkage.EXTERNAL
            if (!isGlobalOrMember(function) || owner is CfirFunction) return
            markFunction(function, CfirDeclarationLinkage.EXTERNAL)
        }

        private fun setFunctionTarget(function: CfirFunction) {
            if (!isGlobalOrMember(function)) return
            markFunction(function, CfirDeclarationLinkage.EXTERNAL)
            if (canBeSourceExported(function)) addSource(function)
            addType(declarationType(function), function)
            ownerOf(function)?.let { owner -> linkages[owner] = CfirDeclarationLinkage.EXTERNAL; addSuperTypes(owner) }
            propertyOwners[function]?.let { setPropertyTarget(it) }
        }

        private fun setPropertyTarget(property: CfirProperty, byType: Boolean = false) {
            setVariableTarget(property, byType)
            val exportSource = statusOf(property).isConst || dataOf(property).isFrozen || isInGenericDeclaration(property)
            dataOf(property).accessors.forEach { accessor ->
                ensureReferenced(accessor)
                markFunction(accessor, CfirDeclarationLinkage.EXTERNAL)
                if (exportSource) addSource(accessor)
            }
        }

        private fun setVariableTarget(variable: CfirCallableDeclaration, byType: Boolean = false, isConstInCjmp: Boolean = false) {
            check(variable is CfirVariable || variable is CfirProperty) { "Native variable linkage requires a field or property" }
            if (!isGlobalOrMember(variable) || byType && variable is CfirVariable && statusOf(variable).isStatic &&
                statusOf(variable).visibility == Visibilities.Private && !isConstInCjmp) return
            linkages[variable] = CfirDeclarationLinkage.EXTERNAL
            if (statusOf(variable).isConst || dataOf(variable).isFrozen ||
                variable is CfirVariable && (!statusOf(variable).isStatic && isInGenericDeclaration(variable) || layoutInitializerIsSourceExported(variable)))
                addSource(variable)
            addType(declarationType(variable), variable)
            ownerOf(variable)?.let { owner -> linkages[owner] = CfirDeclarationLinkage.EXTERNAL; addSuperTypes(owner) }
        }

        private fun analyzeType(type: ConeCangJieType, context: CfirDeclaration) {
            if (!visitedTypes.add(type) || type.containsErrorType()) return
            val binding = input.type(type, context.moduleData.session)
            binding.children.forEach { addType(it, context) }
            binding.abbreviatedDeclaration?.let { alias -> linkages[alias] = CfirDeclarationLinkage.EXTERNAL }
            val declaration = binding.expandedDeclaration ?: return
            ensureReferenced(declaration)
            linkages[declaration] = CfirDeclarationLinkage.EXTERNAL
            val body = memberBodies[declaration] ?: return
            body.forEach { member ->
                val status = statusOf(member)
                val functionOrProperty = member is CfirFunction || member is CfirProperty
                val inVTable = functionOrProperty && (status?.visibility == Visibilities.Public || status?.visibility == Visibilities.Protected)
                val staticVTable = functionOrProperty && status?.isStatic == true && status.visibility != Visibilities.Private
                val ownerStatus = statusOf(declaration)
                val constInCjmp = (ownerStatus?.isCommon == true || ownerStatus?.isSpecific == true) && status?.isConst == true
                if (!inVTable && !staticVTable && !isLayoutMember(member) && !isExported(member) && !constInCjmp) return@forEach
                when (member) {
                    is CfirFinalizer -> markFunctionExternal(member)
                    is CfirFunction -> setFunctionTarget(member)
                    is CfirProperty -> setPropertyTarget(member, true)
                    is CfirVariable -> setVariableTarget(member, true, constInCjmp)
                    is CfirEnumConstructor -> {
                        linkages[member] = CfirDeclarationLinkage.EXTERNAL
                        addType(declarationType(member), member)
                    }
                    else -> Unit
                }
            }
        }

        private fun layoutInitializerIsSourceExported(variable: CfirVariable): Boolean {
            if (!isSourceDeclaration(variable) || initializerOf(variable) == null || statusOf(variable).isStatic) return false
            val owner = ownerOf(variable)
            if (owner !is CfirClass && owner !is CfirStruct) return false
            return memberBodies.getValue(owner).filterIsInstance<CfirConstructor>().any {
                statusOf(it).isConst || dataOf(it).isFrozen
            }
        }

        override fun shouldExportSource(variable: CfirVariable): Boolean {
            if (!isSourceDeclaration(variable)) {
                if (!hasCanonicalSourceOf(variable) || !input.canonicalSourceGraphOf(variable).initializers.containsKey(variable) ||
                    declarationTypeOf(variable)?.containsErrorType() != false || variable.analysisFacts.isCommonMatched) return false
                val owner = nativeOuterOf(variable)
                if (!isTopLevel(variable) && (owner == null || !statusOf(variable).isStatic && isInGenericDeclaration(variable))) return true
                if (owner?.analysisFacts?.isGenericInstantiated == true) return false
                return linkageOf(variable) != CfirDeclarationLinkage.INTERNAL || isExported(variable)
            }
            if (!isSourceDeclaration(variable) || variable.analysisFacts.isImported ||
                initializerOf(variable) == null || declarationType(variable)?.containsErrorType() != false) return false
            if (variable.analysisFacts.isCommonMatched) return false
            val owner = nativeOuterOf(variable)
            if (!isTopLevel(variable) && (owner == null || !statusOf(variable).isStatic && isInGenericDeclaration(variable))) return true
            if (owner?.analysisFacts?.isGenericInstantiated == true) return false
            val external = linkageOf(variable) != CfirDeclarationLinkage.INTERNAL || isExported(variable)
            val constGlobalOrMember = statusOf(variable).isConst && (isTopLevel(variable) || owner in memberBodies)
            if (!external || !constGlobalOrMember && !layoutInitializerIsSourceExported(variable)) return false
            val initializer = initializerOf(variable) as? org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
            val callee = initializer?.let(input::targetOf)
            return callee?.let { !isInInstantiatedDeclarationImpl(it) } != false
        }

        private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap())
    }

    /** 分析器只在创建阶段可变；发布后所有查询读取复制的结果，不再接触活动缓存。 */
    private class FrozenAnalysis(
        analysis: CfirDeclarationExportAnalysis,
        private val declarations: List<CfirDeclaration>,
        private val input: CfirExportAnalysisInputSnapshot,
        private val selectedTypeUses: CfirExportTypeUseGraph,
    ) : CfirDeclarationExportAnalysis {
        override fun <T> withReadOnlyInput(action: () -> T): T = input.withReadOnlyInput(action)
        private fun <V> capture(value: (CfirDeclaration) -> V): Map<CfirDeclaration, V> =
            Collections.unmodifiableMap(IdentityHashMap<CfirDeclaration, V>().apply { declarations.forEach { put(it, value(it)) } })

        private val linkages = capture(analysis::linkageOf)
        private val exported = capture(analysis::isExported)
        private val statuses = capture(analysis::statusOf)
        private val suppressed = capture(analysis::isDoNotExport)
        private val parameterCounts = capture(analysis::parameterCountOf)
        private val owners = capture(analysis::ownerOf)
        private val files = capture(analysis::sourceFileOf)
        private val topLevel = capture(analysis::isTopLevel)
        private val preceding = capture(analysis::precedingFactsOf)
        private val generic = capture(analysis::isInGenericDeclaration)
        private val instantiated = capture(analysis::isInInstantiatedDeclaration)
        private val commonGeneric = capture { analysis.isGenericInCommonSerialization(it, true) }
        private val sourceFunctions = Collections.unmodifiableMap(IdentityHashMap<CfirFunction, Boolean>().apply {
            declarations.filterIsInstance<CfirFunction>().forEach { put(it, analysis.canBeSourceExported(it)) }
        })
        private val sourceFunctionsWithoutFrozen = Collections.unmodifiableMap(IdentityHashMap<CfirFunction, Boolean>().apply {
            declarations.filterIsInstance<CfirFunction>().forEach { put(it, analysis.sourceFunctionBodyIsEligible(it, false)) }
        })
        private val sourceFunctionsWithFrozen = Collections.unmodifiableMap(IdentityHashMap<CfirFunction, Boolean>().apply {
            declarations.filterIsInstance<CfirFunction>().forEach { put(it, analysis.sourceFunctionBodyIsEligible(it, true)) }
        })
        private val sourceVariables = Collections.unmodifiableMap(IdentityHashMap<CfirVariable, Boolean>().apply {
            declarations.filterIsInstance<CfirVariable>().forEach { put(it, analysis.shouldExportSource(it)) }
        })
        override val sourceExportedDeclarations: Set<CfirDeclaration> = Collections.unmodifiableSet(
            Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>()).apply { addAll(analysis.sourceExportedDeclarations) },
        )
        override val hasCanonicalSource: Boolean get() = input.canonicalSources.isNotEmpty()
        override fun canonicalSourceGraphs(): List<CfirSourceGraph> = input.canonicalSources
        override fun canonicalSourceGraphOf(declaration: CfirDeclaration): CfirSourceGraph = input.canonicalSourceGraphOf(declaration)
        override fun hasCanonicalSourceOf(declaration: CfirDeclaration): Boolean = input.hasCanonicalSourceOf(declaration)
        override fun capturedDeclarations(): List<CfirDeclaration> = declarations
        override fun constantValueOf(variable: CfirVariable): CfirExportConstantGraph? = input.constantValueOf(variable)
        override fun constantValuesOf(pattern: CfirSourcePattern): List<CfirExportConstantGraph> = input.constantValuesOf(pattern)
        override fun linkageOf(declaration: CfirDeclaration): CfirDeclarationLinkage = linkages.getValue(declaration)
        override fun statusOf(declaration: CfirDeclaration): CfirExportDeclarationStatus = statuses.getValue(declaration)
        override fun containsDeclaration(declaration: CfirDeclaration): Boolean = linkages.containsKey(declaration)
        override fun isDoNotExport(declaration: CfirDeclaration): Boolean = suppressed.getValue(declaration)
        override fun parameterCountOf(declaration: CfirDeclaration): Int = parameterCounts.getValue(declaration)
        override fun isExported(declaration: CfirDeclaration): Boolean = exported.getValue(declaration)
        override fun ownerOf(declaration: CfirDeclaration): CfirDeclaration? = owners.getValue(declaration)
        override fun nativeOuterOf(declaration: CfirDeclaration): CfirDeclaration? = input.nativeOuterOf(declaration)
        override fun sourceFileOf(declaration: CfirDeclaration): CfirFile? = files.getValue(declaration)
        override fun owningParentOf(declaration: CfirDeclaration): CfirDeclaration? = input.owningParentOf(declaration)
        override fun initializationDependenciesOf(declaration: CfirDeclaration) = input.declaration(declaration).initializationDependencies
        override fun sourceCoordinatesOf(file: CfirFile): CfirExportSourceCoordinates? = input.file(file).coordinates
        override fun isTopLevel(declaration: CfirDeclaration): Boolean = topLevel.getValue(declaration)
        override fun precedingFactsOf(declaration: CfirDeclaration): CfirDeclarationExportFacts = preceding.getValue(declaration)
        override fun isInInstantiatedDeclaration(declaration: CfirDeclaration): Boolean = instantiated.getValue(declaration)
        override fun canBeSourceExported(function: CfirFunction): Boolean = sourceFunctions.getValue(function)
        override fun sourceFunctionBodyIsEligible(function: CfirFunction, writerFrozen: Boolean): Boolean =
            (if (writerFrozen) sourceFunctionsWithFrozen else sourceFunctionsWithoutFrozen).getValue(function)
        override fun isInGenericDeclaration(declaration: CfirDeclaration): Boolean = generic.getValue(declaration)
        override fun isGenericInCommonSerialization(declaration: CfirDeclaration, serializingCommon: Boolean): Boolean =
            serializingCommon && commonGeneric.getValue(declaration)
        override fun shouldExportSource(variable: CfirVariable): Boolean = sourceVariables.getValue(variable)
        override fun sourceElement(element: CfirElement): CfirExportSourceElement = input.element(element)
        override fun isSourceDeclaration(declaration: CfirDeclaration): Boolean = input.isSourceDeclaration(declaration)
        override fun typeElement(type: ConeCangJieType, context: CfirDeclaration): CfirExportTypeElement = input.type(type, context.moduleData.session)
        override fun typeElement(type: ConeCangJieType, domain: CfirSession): CfirExportTypeElement = input.type(type, domain)
        override fun typeUseGraph(): CfirExportTypeUseGraph = selectedTypeUses
        override fun walkSource(root: CfirElement, context: CfirDeclaration, action: (CfirExportSourceElement, CfirDeclaration) -> Unit) =
            input.walk(root, context, action)
        override fun functionBodyOf(function: CfirFunction): CfirBlock? = input.sourceDeclaration(function).body
        override fun defaultValueOf(parameter: CfirValueParameter): CfirExpression? = input.sourceDeclaration(parameter).defaultValue
        override fun hasInitialDefaultOf(parameter: CfirValueParameter): Boolean = input.declaration(parameter).hasInitialDefault
        override fun defaultHelperOf(parameter: CfirValueParameter): CfirFunction? = input.sourceDeclaration(parameter).desugaredDefaultSource
        override fun defaultAssignmentFactsOf(parameter: CfirValueParameter): CfirDefaultAssignmentExportFacts = input.defaultAssignmentFactsOf(parameter)
        override fun isFrozenOf(declaration: CfirDeclaration): Boolean = input.declaration(declaration).isFrozen
        override fun hasOwnGenericNode(declaration: CfirDeclaration): Boolean = input.declaration(declaration).hasGenericNode
        override fun declarationTypeOf(declaration: CfirDeclaration): ConeCangJieType? = input.declaration(declaration).type
        override fun aliasUnderlyingTypeOf(declaration: CfirTypeAlias): ConeCangJieType = checkNotNull(input.declaration(declaration).aliasUnderlyingType)
        override fun valueParametersOf(declaration: CfirDeclaration): List<CfirValueParameter> = input.declaration(declaration).valueParameters
        override fun typeParametersOf(declaration: CfirDeclaration): List<CfirTypeParameter> = input.declaration(declaration).typeParameters
        override fun superTypesOf(declaration: CfirDeclaration): List<ConeCangJieType> = input.declaration(declaration).superTypes
        override fun explicitUpperBoundsOf(parameter: CfirTypeParameter): List<ConeCangJieType> = input.declaration(parameter).explicitUpperBounds
        override fun accessorsOf(property: CfirProperty): List<CfirPropertyAccessor> = input.declaration(property).accessors
        override fun memberDeclarationsOf(owner: CfirDeclaration): List<CfirDeclaration> = input.memberBodies.getValue(owner)
        override fun patternOf(variable: CfirPatternVariable): CfirPattern = input.patternOf(variable)
        override fun patternBindingsOf(variable: CfirPatternVariable): List<CfirPatternBindingVariable> = input.patternBindingsOf(variable)
        override fun simplePatternOwnerOf(binding: CfirPatternBindingVariable): CfirPatternVariable? = input.simplePatternOwnerOf(binding)
        override fun annotationSourceExpressionsOf(declaration: CfirDeclaration): List<CfirExpression> = input.declaration(declaration).annotationSourceExpressions
        override fun annotationsOf(declaration: CfirDeclaration): List<CfirAnnotation> = input.declaration(declaration).annotations
        override fun initializerOf(variable: CfirVariable): CfirExpression? = input.initializerOf(variable)
        override fun validateSourceUnchanged() = input.validateUnchanged()
    }
}
