package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex
import java.util.Collections

/** 内联 F 的实际 table 地址；身份域是持有本计划的 loaded package，不是 body 的 e 下标。 */
internal data class CjoSourceFunctionIdentity(val tablePosition: Int)

/** header 结构登记不授予 e 内容能力；只有 CONTENT 计划可以进入 canonical decoder。 */
internal enum class CjoSourcePlanKind { HEADER_REGISTRY, CONTENT }

/** common依赖是调用方的实际加载职责，不由FROM_COMMON_PART来源位或正文缺失推断。 */
internal enum class CjoSourceHeaderBindingPolicy { STANDARD, COMMON_DEPENDENCIES }

/** 声明的类型/绑定上下文和表达式的拥有者分别保存；匿名函数没有伪造的 d 下标。 */
internal sealed interface CjoSourceContext {
    data object Package : CjoSourceContext
    data class Declaration(val reference: UInt) : CjoSourceContext
    data class Function(val identity: CjoSourceFunctionIdentity) : CjoSourceContext
}

/** 当前词法 F 与 Return 所在正文 F 分开；参数 default/annotation 自身不是 F.body。 */
internal data class CjoSourceScope(
    val declarationContext: CjoSourceContext,
    val expressionContext: CjoSourceContext,
    val function: CjoSourceFunctionIdentity?,
    val returnFunction: CjoSourceFunctionIdentity?,
    /** 所选 e 拥有的局部 d 必须保存其 initializer；具名 F.body 的开关独立。 */
    val includesInitializers: Boolean = true,
)

/** 内容选择位不参与身份比较；其余四项必须来自同一个实际metadata上下文。 */
internal fun CjoSourceScope.sameContextIdentity(other: CjoSourceScope): Boolean =
    declarationContext == other.declarationContext && expressionContext == other.expressionContext &&
        function == other.function && returnFunction == other.returnFunction

/** 真实拥有槽。重复的同 parent 槽保留源序，borrowed 引用从不进入此关系。 */
internal sealed interface CjoSourceOwningParent {
    data object Package : CjoSourceOwningParent
    data class Declaration(val reference: UInt) : CjoSourceOwningParent
    data class Expression(val reference: UInt) : CjoSourceOwningParent
    data class Function(val identity: CjoSourceFunctionIdentity) : CjoSourceOwningParent
    data class ParameterList(val function: CjoSourceFunctionIdentity, val ordinal: Int) : CjoSourceOwningParent
    data class Pattern(val tablePosition: Int) : CjoSourceOwningParent
    data class AnnotationArgument(val declaration: UInt, val annotation: Int, val argument: Int) : CjoSourceOwningParent
}

internal enum class CjoSourceDefinitionSlotKind {
    PACKAGE_ROOT, MEMBER, TYPE_PARAMETER, VALUE_PARAMETER, DEFAULT_HELPER, GETTER, SETTER,
    BLOCK_DECLARATION, PATTERN_BINDING, RESOURCE, FUTURE, DECLARATION_PATTERN, EXPRESSION_PATTERN, NESTED_PATTERN,
}
/** d/P定义槽只可幂等重访同一字段/ordinal；不同定义槽不能借查重吞掉原来源。 */
internal data class CjoSourceDefinitionSlot(val kind: CjoSourceDefinitionSlotKind, val ordinal: Int = 0, val field: Int = 0)

internal data class CjoSourceOwnedDeclaration(
    val reference: UInt,
    val wire: Decl,
    val parent: CjoSourceOwningParent,
    val scope: CjoSourceScope,
    val isPatternBinding: Boolean,
    val helperOwner: CjoSourceFunctionIdentity?,
    val accessorOwner: UInt?,
    val initializerSelected: Boolean,
    val slot: CjoSourceDefinitionSlot,
)

internal data class CjoSourceOwnedFunction(
    val identity: CjoSourceFunctionIdentity,
    val body: FuncBody,
    val declaration: UInt?,
    val lambda: UInt?,
    val lexicalScope: CjoSourceScope,
)

internal enum class CjoSourceExpressionSlotKind { FUNCTION_BODY, INITIALIZER, PARAMETER_DEFAULT, ANNOTATION_ARGUMENT, OPERAND, PATTERN_EXPRESSION }
/** record按e共享，occurrence按真实保存字段及ordinal保留；重复提交同槽才可幂等。 */
internal data class CjoSourceExpressionSlot(val kind: CjoSourceExpressionSlotKind, val ordinal: Int = 0)

internal data class CjoSourceOwnedExpression(
    val reference: UInt,
    val wire: Expr,
    val parent: CjoSourceOwningParent,
    val scope: CjoSourceScope,
    val slot: CjoSourceExpressionSlot,
)

/** 已由实际下行读取证明的e祖先信息；header registry保留它但不持有或读取Expr内容。 */
internal data class CjoSourceExpressionContext(
    val reference: UInt,
    val parent: CjoSourceOwningParent,
    val scope: CjoSourceScope,
    val slot: CjoSourceExpressionSlot,
    /** 实际下行时保存的标量证明；后续借用校验不能打开未选e的kind/type/selector。 */
    val kind: UShort,
    val type: UInt,
    val matchSelector: UInt?,
)

/** P的结构拥有关系与e/d/F使用同一索引；按需选择不重复实现wire字段遍历。 */
internal data class CjoSourceOwnedPattern(
    val position: Int,
    val wire: Pattern,
    val parent: CjoSourceOwningParent,
    val scope: CjoSourceScope,
    val slot: CjoSourceDefinitionSlot,
)

/**
 * ASTLoader 的拥有阶段计划：从实际 top-level roots 下行，完成 d/e/F/P 的拥有闭包。
 *
 * 固定 native 14276740 的 LoadPackageDecls/LoadBlock/LoadFuncBody/LoadAnnotationArgs 是
 * 槽位依据；Kotlin 3c7a504 的 declaration-parent 与 declare/reference binder 是职责依据。
 * 此阶段只读取保存的拥有字段，不解码类型、不创建 CFIR、不沿 map/FullId 认领目标。
 */
internal class CjoSourceOwnershipPlan private constructor(
    internal val ownerPackage: PackageFormat.Package,
    val kind: CjoSourcePlanKind,
    val roots: List<UInt>,
    val declarations: Map<UInt, CjoSourceOwnedDeclaration>,
    val functions: Map<CjoSourceFunctionIdentity, CjoSourceOwnedFunction>,
    val expressions: Map<UInt, CjoSourceOwnedExpression>,
    /** wire 身份与拥有 occurrence 分开；每个真实父槽均保留，不能被 e registry 查重吞掉。 */
    val expressionOccurrences: List<CjoSourceOwnedExpression>,
    val functionByDeclaration: Map<UInt, CjoSourceFunctionIdentity>,
    val functionByLambda: Map<UInt, CjoSourceFunctionIdentity>,
    val patterns: Map<Int, CjoSourceOwnedPattern>,
    val expressionContexts: Map<UInt, List<CjoSourceExpressionContext>>,
    val headerBindingPolicy: CjoSourceHeaderBindingPolicy,
    /** TP的最终semantic bounds来源只在metadata发现阶段登记；binder不得自行扫描allTypes。 */
    val genericTypesByParameter: Map<UInt, List<UInt>>,
) {
    private sealed interface SelectedNode {
        data class Declaration(val reference: UInt) : SelectedNode
        data class Expression(val reference: UInt) : SelectedNode
        data class Function(val identity: CjoSourceFunctionIdentity) : SelectedNode
        data class Pattern(val position: Int) : SelectedNode
    }

    /** MatchCase 根 P 的 ctxExpr 来自原拥有 Match 的 selector 槽，与当前请求根无关。 */
    fun matchCaseSelector(reference: UInt): UInt {
        val entry = expressions.getValue(reference)
        check(entry.wire.kind == ExprKind.MatchCase)
        val parent = entry.parent as CjoSourceOwningParent.Expression
        val match = expressions.getValue(parent.reference).wire
        check(match.kind == ExprKind.MatchExpr && (match.info(MatchInfo()) as MatchInfo).matchMode)
        return match.operands(0).also { check(it != 0u) }
    }

    /** 绑定准备域来自真实 map/refLoop/selector 槽；这些根不加入请求的拥有选择。 */
    fun borrowedReferences(selected: CjoSourceOwnershipPlan): Set<UInt> = buildSet {
        selected.expressions.values.forEach { entry ->
            val map = entry.wire.mapExpr
            if (map != 0u && map !in selected.expressions) add(map)
            if (entry.wire.kind == ExprKind.MatchCase) {
                val selector = matchCaseSelector(entry.reference)
                if (selector !in selected.expressions) add(selector)
            }
        }
    }

    /** 定义位置与词法 owner 独立：包级 annotation 的 Block 也拥有真实局部声明。 */
    fun isLocalDeclaration(reference: UInt): Boolean {
        val entry = declarations.getValue(reference)
        return when (val parent = entry.parent) {
            CjoSourceOwningParent.Package -> false
            is CjoSourceOwningParent.Expression -> true
            is CjoSourceOwningParent.Pattern -> isLocalPattern(parent.tablePosition)
            else -> entry.scope.declarationContext is CjoSourceContext.Function
        }
    }

    /** VarWithPattern 的绑定继承容器定义位置；Match/For/Let/Try 的 P 在其拥有 e 内定义。 */
    private fun isLocalPattern(position: Int): Boolean = when (val parent = patterns.getValue(position).parent) {
        is CjoSourceOwningParent.Expression -> true
        is CjoSourceOwningParent.Declaration -> isLocalDeclaration(parent.reference)
        is CjoSourceOwningParent.Pattern -> isLocalPattern(parent.tablePosition)
        else -> error("SOURCE pattern requires its actual expression or declaration definition")
    }

    /** 只从真实e根下行，引用目标和祖先header不因此获得正文所有权。 */
    fun selectExpressions(references: Collection<UInt>): CjoSourceOwnershipPlan {
        check(kind == CjoSourcePlanKind.CONTENT) { "A header registry cannot select expression content" }
        fun parent(value: CjoSourceOwningParent): CjoSourceOwningParent = when (value) {
            is CjoSourceOwningParent.AnnotationArgument -> CjoSourceOwningParent.Declaration(value.declaration)
            is CjoSourceOwningParent.ParameterList -> CjoSourceOwningParent.Function(value.function)
            else -> value
        }
        val children = HashMap<CjoSourceOwningParent, MutableList<SelectedNode>>()
        fun add(owner: CjoSourceOwningParent, node: SelectedNode) {
            children.getOrPut(parent(owner)) { ArrayList() }.add(node)
        }
        declarations.values.forEach { add(it.parent, SelectedNode.Declaration(it.reference)) }
        expressionOccurrences.forEach { add(it.parent, SelectedNode.Expression(it.reference)) }
        patterns.values.forEach { add(it.parent, SelectedNode.Pattern(it.position)) }
        functions.values.forEach {
            val owner = if (it.declaration != null) CjoSourceOwningParent.Declaration(it.declaration)
                else CjoSourceOwningParent.Expression(checkNotNull(it.lambda))
            add(owner, SelectedNode.Function(it.identity))
        }
        val selected = LinkedHashSet<SelectedNode>()
        val work = ArrayDeque<SelectedNode>()
        references.forEach {
            check(expressions.containsKey(it)) { "Selected SOURCE e $it has no actual owning root" }
            work.addLast(SelectedNode.Expression(it))
        }
        while (work.isNotEmpty()) {
            val node = work.removeFirst()
            if (!selected.add(node)) continue
            val key = when (node) {
                is SelectedNode.Declaration -> CjoSourceOwningParent.Declaration(node.reference)
                is SelectedNode.Expression -> CjoSourceOwningParent.Expression(node.reference)
                is SelectedNode.Function -> CjoSourceOwningParent.Function(node.identity)
                is SelectedNode.Pattern -> CjoSourceOwningParent.Pattern(node.position)
            }
            children[key].orEmpty().forEach(work::addLast)
        }
        val ds = declarations.filterKeys { SelectedNode.Declaration(it) in selected }
        val es = expressions.filterKeys { SelectedNode.Expression(it) in selected }
        val fs = functions.filterKeys { SelectedNode.Function(it) in selected }
        val ps = patterns.filterKeys { SelectedNode.Pattern(it) in selected }
        es.values.forEach { entry ->
            val target = entry.wire.mapExpr
            if (target != 0u) check(expressions.containsKey(target)) {
                "Selected SOURCE borrowed e $target has no actual owning root"
            }
            if (entry.wire.kind == ExprKind.JumpExpr) {
                check(target != 0u) { "SOURCE jump must retain its actual loop reference" }
                val loop = expressions.getValue(target)
                check(loop.wire.kind in listOf(ExprKind.WhileExpr, ExprKind.DoWhileExpr, ExprKind.ForInExpr))
                check(entry.scope.returnFunction == loop.scope.returnFunction) { "SOURCE jump crosses its selected FuncBody" }
            }
        }
        return CjoSourceOwnershipPlan(ownerPackage, CjoSourcePlanKind.CONTENT, emptyList(), Collections.unmodifiableMap(ds), Collections.unmodifiableMap(fs),
            Collections.unmodifiableMap(es), Collections.unmodifiableList(expressionOccurrences.filter { it.reference in es }),
            Collections.unmodifiableMap(functionByDeclaration.filterValues(fs::containsKey)),
            Collections.unmodifiableMap(functionByLambda.filterValues(fs::containsKey)), Collections.unmodifiableMap(ps), expressionContexts, headerBindingPolicy, genericTypesByParameter)
    }

    /** 调用方在根事务成功后发布此新registry；旧registry永久保持原身份/边，不原位覆盖。 */
    internal fun discoveredHeaders(
        content: CjoSourceOwnershipPlan,
        declarations: Map<UInt, CjoSourceOwnedDeclaration>,
        functions: Map<CjoSourceFunctionIdentity, CjoSourceOwnedFunction>,
        patterns: Map<Int, CjoSourceOwnedPattern>,
    ): CjoSourceOwnershipPlan {
        check(kind == CjoSourcePlanKind.HEADER_REGISTRY && ownerPackage === content.ownerPackage)
        val contexts = LinkedHashMap(expressionContexts)
        content.expressionContexts.forEach { (reference, entries) ->
            val previous = contexts[reference].orEmpty()
            val combined = (previous + entries).map { it.copy(scope = it.scope.copy(includesInitializers = false)) }.distinct()
            check(combined.all { it.scope.sameContextIdentity(combined.first().scope) &&
                it.kind == combined.first().kind && it.type == combined.first().type && it.matchSelector == combined.first().matchSelector })
            contexts[reference] = Collections.unmodifiableList(combined.map {
                it.copy(scope = it.scope.copy(includesInitializers = false))
            })
        }
        val ds = declarations.mapValues { (_, entry) -> if (!entry.scope.includesInitializers && !entry.initializerSelected) entry
            else entry.copy(scope = entry.scope.copy(includesInitializers = false), initializerSelected = false) }
        val fs = functions.mapValues { (_, entry) -> if (!entry.lexicalScope.includesInitializers) entry
            else entry.copy(lexicalScope = entry.lexicalScope.copy(includesInitializers = false)) }
        val ps = patterns.mapValues { (_, entry) -> if (!entry.scope.includesInitializers) entry
            else entry.copy(scope = entry.scope.copy(includesInitializers = false)) }
        val byDeclaration = fs.values.filter { it.declaration != null }.associate { checkNotNull(it.declaration) to it.identity }
        val byLambda = fs.values.filter { it.lambda != null }.associate { checkNotNull(it.lambda) to it.identity }
        check(byDeclaration.size == fs.values.count { it.declaration != null } && byLambda.size == fs.values.count { it.lambda != null })
        return CjoSourceOwnershipPlan(ownerPackage, CjoSourcePlanKind.HEADER_REGISTRY, roots,
            Collections.unmodifiableMap(ds), Collections.unmodifiableMap(fs), emptyMap(), emptyList(),
            Collections.unmodifiableMap(byDeclaration), Collections.unmodifiableMap(byLambda), Collections.unmodifiableMap(ps),
            Collections.unmodifiableMap(contexts), headerBindingPolicy, genericTypesByParameter)
    }
    companion object {
        fun create(pkg: PackageFormat.Package, validateReferences: Boolean = true, includeFunctionBodies: Boolean = true): CjoSourceOwnershipPlan =
            Builder(pkg, validateReferences, includeFunctionBodies, includeHeaderContent = true,
                declarationContentRoots = null, headerSeed = null, requestedRoots = null).build()

        /** 只沿 top-level/member/TP/parameter/desugar/P-binding 槽登记 header，不读任何 e。 */
        fun headers(pkg: PackageFormat.Package, bindingPolicy: CjoSourceHeaderBindingPolicy = CjoSourceHeaderBindingPolicy.STANDARD): CjoSourceOwnershipPlan =
            Builder(pkg, validateReferences = false, includeFunctionBodies = false,
                includeHeaderContent = false, declarationContentRoots = null, headerSeed = null, requestedRoots = null, bindingPolicy = bindingPolicy).build()

        /** 从已登记header的真实保存槽开始，仅遍历本次请求下行；不先解析全包正文再过滤。 */
        fun discover(headers: CjoSourceOwnershipPlan, roots: List<CjoSourceContentRoot>): CjoSourceOwnershipPlan {
            check(headers.kind == CjoSourcePlanKind.HEADER_REGISTRY)
            val content = Builder(headers.ownerPackage, validateReferences = false, includeFunctionBodies = true,
                includeHeaderContent = true, declarationContentRoots = null, headerSeed = headers, requestedRoots = roots.toList()).build()
            return discoveredSourceHeaders(headers, content)
        }

        /** 内容请求在owning发现之后完成所有typed/binding资格；两阶段不可互相代替。 */
        fun request(headers: CjoSourceOwnershipPlan, roots: List<CjoSourceContentRoot>): CjoSourceContentSelection {
            check(headers.kind == CjoSourcePlanKind.HEADER_REGISTRY)
            val content = Builder(headers.ownerPackage, validateReferences = false, includeFunctionBodies = true,
                includeHeaderContent = true, declarationContentRoots = null,
                headerSeed = headers, requestedRoots = roots.toList()).build()
            return sourceContentSelection(headers, content, roots)
        }

        /** 声明 header 只选择其真实 default/annotation/initializer 根，未选择的 F.body 不参与读取。 */
        fun forDeclarationContent(pkg: PackageFormat.Package, reference: UInt): CjoSourceOwnershipPlan =
            Builder(pkg, validateReferences = false, includeFunctionBodies = false,
                includeHeaderContent = true, declarationContentRoots = setOf(reference),
                headerSeed = null, requestedRoots = null).build()
    }

    private class Builder(private val pkg: PackageFormat.Package, private val validateReferences: Boolean,
        private val includeFunctionBodies: Boolean, private val includeHeaderContent: Boolean,
        private val declarationContentRoots: Set<UInt>?, private val headerSeed: CjoSourceOwnershipPlan?,
        private val requestedRoots: List<CjoSourceContentRoot>?,
        private val bindingPolicy: CjoSourceHeaderBindingPolicy = CjoSourceHeaderBindingPolicy.STANDARD) {
        private val roots = ArrayList<UInt>()
        private val declarations = LinkedHashMap<UInt, CjoSourceOwnedDeclaration>()
        private val functions = LinkedHashMap<CjoSourceFunctionIdentity, CjoSourceOwnedFunction>()
        private val expressions = LinkedHashMap<UInt, CjoSourceOwnedExpression>()
        private val expressionOccurrences = LinkedHashMap<UInt, MutableList<CjoSourceOwnedExpression>>()
        private val functionByDeclaration = HashMap<UInt, CjoSourceFunctionIdentity>()
        private val functionByLambda = HashMap<UInt, CjoSourceFunctionIdentity>()
        private val patternParents = HashMap<Int, CjoSourceOwningParent>()
        private val patterns = LinkedHashMap<Int, CjoSourceOwnedPattern>()
        private val work = ArrayDeque<() -> Unit>()
        private val borrowedExpressions = ArrayList<Pair<UInt, UInt>>()
        private val borrowedDeclarations = ArrayList<FullId>()
        private val parameterOccurrences = HashMap<UInt, Pair<CjoSourceOwningParent.ParameterList, Int>>()
        private val typeParameterOccurrences = HashMap<UInt, Pair<UInt, Int>>()
        private val defaultHelperDeclarations = HashSet<UInt>()
        private val packageScope = CjoSourceScope(CjoSourceContext.Package, CjoSourceContext.Package, null, null,
            includeHeaderContent && includeFunctionBodies && declarationContentRoots == null)

        fun build(): CjoSourceOwnershipPlan {
            if (requestedRoots == null) {
                check(headerSeed == null)
                for (index in 0 until pkg.allDeclsLength) {
                    if (!checkNotNull(pkg.allDecls(index)).isTopLevel) continue
                    val reference = (index + 1).toUInt()
                    roots += reference
                    declaration(reference, CjoSourceOwningParent.Package, packageScope, CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.PACKAGE_ROOT, index))
                }
            } else {
                checkNotNull(headerSeed)
                requestedRoots.forEach(::requestRoot)
            }
            while (work.isNotEmpty()) work.removeFirst().invoke()
            if (validateReferences) borrowedExpressions.forEach { (source, target) ->
                check(expressions.containsKey(target)) {
                    "SOURCE borrowed e $target from e $source has no owning root"
                }
                if (expressions.getValue(source).wire.kind == ExprKind.JumpExpr) {
                    val loop = expressions.getValue(target)
                    check(loop.wire.kind == ExprKind.WhileExpr || loop.wire.kind == ExprKind.DoWhileExpr || loop.wire.kind == ExprKind.ForInExpr) {
                        "SOURCE jump must reference its actual owned loop"
                    }
                    check(expressions.getValue(source).scope.returnFunction == loop.scope.returnFunction) {
                        "SOURCE jump crosses a FuncBody boundary"
                    }
                }
            }
            if (validateReferences) borrowedDeclarations.forEach { target ->
                if (target.pkgId == PackageIndex.CURRENT.value) check(declarations.containsKey(target.index)) {
                    "SOURCE borrowed d ${target.index} has no owning root"
                }
            }
            val genericTypes = headerSeed?.genericTypesByParameter ?: if (!includeHeaderContent) {
                val discovered = LinkedHashMap<UInt, MutableList<UInt>>()
                for (index in 0 until pkg.allTypesLength) {
                    val wire = checkNotNull(pkg.allTypes(index))
                    if (wire.kind != TypeKind.Generic) continue
                    check(wire.infoType == SemaTyInfo.GenericTyInfo)
                    val target = checkNotNull((wire.info(GenericTyInfo()) as GenericTyInfo).declPtr)
                    if (target.pkgId == PackageIndex.CURRENT.value)
                        discovered.getOrPut(target.index) { ArrayList() }.add((index + 1).toUInt())
                }
                Collections.unmodifiableMap(discovered.mapValues { Collections.unmodifiableList(it.value.toList()) })
            } else emptyMap()
            return CjoSourceOwnershipPlan(
                pkg,
                if (includeHeaderContent) CjoSourcePlanKind.CONTENT else CjoSourcePlanKind.HEADER_REGISTRY,
                Collections.unmodifiableList(roots.toList()),
                Collections.unmodifiableMap(LinkedHashMap(declarations)),
                Collections.unmodifiableMap(LinkedHashMap(functions)),
                Collections.unmodifiableMap(LinkedHashMap(expressions)),
                Collections.unmodifiableList(expressionOccurrences.values.flatten()),
                Collections.unmodifiableMap(HashMap(functionByDeclaration)),
                Collections.unmodifiableMap(HashMap(functionByLambda)),
                Collections.unmodifiableMap(LinkedHashMap(patterns)),
                Collections.unmodifiableMap(expressionOccurrences.mapValues { (_, entries) ->
                    Collections.unmodifiableList(entries.map { entry ->
                        val wire = entry.wire
                        val selector = if (wire.kind == ExprKind.MatchExpr && (wire.info(MatchInfo()) as MatchInfo).matchMode)
                            wire.operands(0).also { check(it != 0u) } else null
                        CjoSourceExpressionContext(entry.reference, entry.parent, entry.scope, entry.slot, wire.kind, wire.type, selector)
                    })
                }),
                headerSeed?.headerBindingPolicy ?: bindingPolicy,
                genericTypes,
            )
        }

        /** 保存槽的零值是实际缺失；请求仍保留宿主context，不生成虚构e。 */
        private fun requestRoot(root: CjoSourceContentRoot) {
            val headers = checkNotNull(headerSeed)
            when (root) {
                is CjoSourceContentRoot.DeclarationHeader -> check(headers.declarations.containsKey(root.declaration)) { "Header request requires its actual declaration registry proof" }
                is CjoSourceContentRoot.DeclarationPatternHeader -> check(headers.declarations.getValue(root.declaration).wire.kind == DeclKind.VarWithPatternDecl) {
                    "Pattern header request requires its actual variable owning P slot"
                }
                is CjoSourceContentRoot.TypeMetadata -> require(root.reference <= pkg.allTypesLength.toUInt())
                is CjoSourceContentRoot.SelectedExpression -> {
                    val occurrence = root.occurrence
                    check(headers.expressionContexts.getValue(occurrence.reference).contains(occurrence)) {
                        "Selected e requires its actual registered owning occurrence"
                    }
                    expression(occurrence.reference, occurrence.parent, occurrence.scope.copy(includesInitializers = true), occurrence.slot)
                }
                is CjoSourceContentRoot.DeclarationAnnotations -> {
                    val declaration = headers.declarations.getValue(root.declaration)
                    for (annotation in 0 until declaration.wire.annotationsLength) {
                        val wire = checkNotNull(declaration.wire.annotations(annotation))
                        for (argument in 0 until wire.argsLength)
                            requestRoot(CjoSourceContentRoot.AnnotationArgument(root.declaration, annotation, argument))
                    }
                }
                is CjoSourceContentRoot.FunctionBody -> {
                    val function = headers.functions.getValue(root.function)
                    functions[root.function] = function
                    function.declaration?.let { functionByDeclaration[it] = root.function }
                    function.lambda?.let { functionByLambda[it] = root.function }
                    val context = CjoSourceContext.Function(root.function)
                    if (function.body.body != 0u) expression(function.body.body,
                        CjoSourceOwningParent.Function(root.function), CjoSourceScope(context, context, root.function, root.function),
                        CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.FUNCTION_BODY))
                }
                is CjoSourceContentRoot.Initializer -> {
                    val declaration = headers.declarations.getValue(root.declaration)
                    val reference = when (declaration.wire.kind) {
                        DeclKind.VarDecl -> (declaration.wire.info(VarInfo()) as VarInfo).initializer
                        DeclKind.VarWithPatternDecl -> (declaration.wire.info(VarWithPatternInfo()) as VarWithPatternInfo).initializer
                        else -> error("Initializer request requires its actual variable header")
                    }
                    if (reference != 0u) expression(reference, CjoSourceOwningParent.Declaration(root.declaration),
                        declaration.scope.copy(expressionContext = CjoSourceContext.Declaration(root.declaration),
                            includesInitializers = true), CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.INITIALIZER))
                }
                is CjoSourceContentRoot.ParameterDefault -> {
                    val parameter = headers.declarations.getValue(root.parameter)
                    check(parameter.wire.kind == DeclKind.FuncParam)
                    val reference = when (parameter.wire.infoType) {
                        DeclInfo.NONE -> 0u
                        DeclInfo.ParamInfo -> (parameter.wire.info(ParamInfo()) as ParamInfo).defaultVal
                        else -> error("Default request has invalid parameter info")
                    }
                    if (reference != 0u) expression(reference, CjoSourceOwningParent.Declaration(root.parameter),
                        parameter.scope.copy(expressionContext = CjoSourceContext.Declaration(root.parameter),
                            returnFunction = null, includesInitializers = true), CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.PARAMETER_DEFAULT))
                }
                is CjoSourceContentRoot.AnnotationArgument -> {
                    val declaration = headers.declarations.getValue(root.declaration)
                    require(root.annotation in 0 until declaration.wire.annotationsLength)
                    val annotation = checkNotNull(declaration.wire.annotations(root.annotation))
                    require(root.argument in 0 until annotation.argsLength)
                    val reference = checkNotNull(annotation.args(root.argument)).expr
                    expression(reference, CjoSourceOwningParent.AnnotationArgument(root.declaration, root.annotation, root.argument),
                        declaration.scope.copy(expressionContext = CjoSourceContext.Declaration(root.declaration),
                            returnFunction = null, includesInitializers = true), CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.ANNOTATION_ARGUMENT))
                    if (annotation.kind != AnnoKind.Annotation) check(expressions.getValue(reference).wire.kind == ExprKind.LitConstExpr) { "This annotation kind requires its saved literal argument" }
                }
            }
        }

        private fun declaration(
            reference: UInt,
            parent: CjoSourceOwningParent,
            scope: CjoSourceScope,
            slot: CjoSourceDefinitionSlot,
            isPatternBinding: Boolean = false,
            helperOwner: CjoSourceFunctionIdentity? = null,
            accessorOwner: UInt? = null,
        ) {
            require(reference != 0u && reference <= pkg.allDeclsLength.toUInt()) { "Invalid SOURCE d reference $reference" }
            declarations[reference]?.let { existing ->
                // Native GLOBAL helper 也是 package 查询根；desugars 才定义其真实 owning parent。
                if (existing.parent == CjoSourceOwningParent.Package && existing.wire.isTopLevel &&
                    parent is CjoSourceOwningParent.Declaration && helperOwner != null &&
                    existing.scope == scope && !existing.isPatternBinding && existing.accessorOwner == null) {
                    declarations[reference] = existing.copy(parent = parent, helperOwner = helperOwner, slot = slot)
                    return
                }
                check(existing.parent == parent && existing.scope == scope && existing.slot == slot && existing.isPatternBinding == isPatternBinding &&
                    existing.helperOwner == helperOwner && existing.accessorOwner == accessorOwner) {
                    "SOURCE d $reference has conflicting owning slots"
                }
                return
            }
            val wire = checkNotNull(pkg.allDecls(reference.toInt() - 1))
            val selectedInitializer = wire.kind == DeclKind.VarDecl && declarationContentRoots != null &&
                (wire.info(VarInfo()) as VarInfo).initializer in declarationContentRoots
            val entry = CjoSourceOwnedDeclaration(reference, wire,
                parent, scope, isPatternBinding, helperOwner, accessorOwner,
                includeHeaderContent && (scope.includesInitializers || wire.kind == DeclKind.VarWithPatternDecl || selectedInitializer), slot)
            declarations[reference] = entry
            work.addLast { visitDeclaration(entry) }
        }

        private fun visitDeclaration(entry: CjoSourceOwnedDeclaration) {
            val wire = entry.wire
            val parent = CjoSourceOwningParent.Declaration(entry.reference)
            val context = CjoSourceContext.Declaration(entry.reference)
            val scope = entry.scope.copy(expressionContext = context)
            wire.generic?.let { generic ->
                val ownScope = scope.copy(declarationContext = context)
                for (index in 0 until generic.typeParametersLength) {
                    val parameter = generic.typeParameters(index)
                    check(parameter != 0u && parameter <= pkg.allDeclsLength.toUInt() &&
                        checkNotNull(pkg.allDecls(parameter.toInt() - 1)).kind == DeclKind.GenericParamDecl) {
                        "Generic parameter metadata must point to its actual GenericParamDecl"
                    }
                    check(typeParameterOccurrences.putIfAbsent(parameter, entry.reference to index) == null) {
                        "A generic parameter cannot occupy two declaration slots"
                    }
                    declaration(parameter, parent, ownScope, CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.TYPE_PARAMETER, index))
                }
            }
            for (ordinal in 0 until wire.annotationsLength) {
                val annotation = checkNotNull(wire.annotations(ordinal))
                if (validateReferences && annotation.kind == AnnoKind.Custom && classifySourceReferenceTarget(annotation.target) == CjoSourceReferenceTargetKind.DECLARATION)
                    borrowedDeclarations += checkNotNull(annotation.target)
                if (includeHeaderContent && requestedRoots == null) for (index in 0 until annotation.argsLength) {
                    val reference = checkNotNull(annotation.args(index)).expr
                    val argumentScope = scope.copy(returnFunction = null)
                    if (!selectsExpression(reference, argumentScope)) continue
                    expression(reference,
                        CjoSourceOwningParent.AnnotationArgument(entry.reference, ordinal, index), argumentScope,
                        CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.ANNOTATION_ARGUMENT))
                    if (annotation.kind != AnnoKind.Annotation) check(expressions.getValue(reference).wire.kind == ExprKind.LitConstExpr) { "This annotation kind requires its saved literal argument" }
                }
            }
            fun members(length: Int, getter: (Int) -> UInt) {
                val memberScope = scope.copy(declarationContext = context, function = null, returnFunction = null)
                for (index in 0 until length) declaration(getter(index), parent, memberScope, CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.MEMBER, index))
            }
            when (wire.kind) {
                DeclKind.ClassDecl -> (wire.info(ClassInfo()) as ClassInfo).let { members(it.bodyLength, it::body) }
                DeclKind.InterfaceDecl -> (wire.info(InterfaceInfo()) as InterfaceInfo).let { members(it.bodyLength, it::body) }
                DeclKind.StructDecl -> (wire.info(StructInfo()) as StructInfo).let { members(it.bodyLength, it::body) }
                DeclKind.EnumDecl -> (wire.info(EnumInfo()) as EnumInfo).let { members(it.bodyLength, it::body) }
                DeclKind.ExtendDecl -> (wire.info(ExtendInfo()) as ExtendInfo).let { members(it.bodyLength, it::body) }
                DeclKind.FuncDecl -> {
                    val body = checkNotNull((wire.info(FuncInfo()) as FuncInfo).funcBody) { "A SOURCE FuncDecl must retain its actual FuncBody" }
                    function(checkNotNull(wire.info(CjoSourceTable())) as CjoSourceTable,
                        body, entry.reference, null, entry.scope)
                }
                DeclKind.VarDecl -> {
                    val initializer = (wire.info(VarInfo()) as VarInfo).initializer
                    // Native从实际F.body遍历Return；body内initializer继承该域，header/default/annotation中的initializer保留原null域。
                    if (entry.initializerSelected && initializer != 0u) expression(initializer, parent, scope,
                        CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.INITIALIZER))
                }
                DeclKind.VarWithPatternDecl -> {
                    val info = wire.info(VarWithPatternInfo()) as VarWithPatternInfo
                    if (entry.initializerSelected && info.initializer != 0u) expression(info.initializer, parent, scope,
                        CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.INITIALIZER))
                    pattern((checkNotNull(wire.info(CjoSourceTable())) as CjoSourceTable).tablePosition(8), parent, scope,
                        CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.DECLARATION_PATTERN, field = 8))
                }
                DeclKind.FuncParam -> {
                    val value = when (wire.infoType) {
                        DeclInfo.NONE -> 0u
                        DeclInfo.ParamInfo -> (wire.info(ParamInfo()) as ParamInfo).defaultVal
                        else -> error("A function parameter has an invalid additional-info kind")
                    }
                    if (includeHeaderContent && requestedRoots == null && value != 0u) expression(value, parent, scope.copy(returnFunction = null),
                        CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.PARAMETER_DEFAULT))
                }
                DeclKind.PropDecl -> {
                    val info = wire.info(PropInfo()) as PropInfo
                    val accessorScope = entry.scope.copy(declarationContext = context)
                    fun accessor(reference: UInt, kind: CjoSourceDefinitionSlotKind, ordinal: Int) {
                        check(reference != 0u && reference <= pkg.allDeclsLength.toUInt() && checkNotNull(pkg.allDecls(reference.toInt() - 1)).kind == DeclKind.FuncDecl) { "An accessor slot requires its actual FuncDecl" }
                        declaration(reference, parent, accessorScope, CjoSourceDefinitionSlot(kind, ordinal), accessorOwner = entry.reference)
                    }
                    for (index in 0 until info.gettersLength) accessor(info.getters(index), CjoSourceDefinitionSlotKind.GETTER, index)
                    for (index in 0 until info.settersLength) accessor(info.setters(index), CjoSourceDefinitionSlotKind.SETTER, index)
                }
                DeclKind.GenericParamDecl, DeclKind.TypeAliasDecl, DeclKind.BuiltInDecl -> Unit
                else -> error("Unsupported owned SOURCE declaration kind ${wire.kind}")
            }
        }

        private fun function(info: CjoSourceTable, body: FuncBody, declarationReference: UInt?, lambda: UInt?, lexicalScope: CjoSourceScope) {
            val identity = CjoSourceFunctionIdentity(info.tablePosition(4))
            functions[identity]?.let { previous ->
                check(previous.declaration == declarationReference && previous.lambda == lambda &&
                    previous.lexicalScope.sameContextIdentity(lexicalScope)) { "SOURCE FuncBody has conflicting owning definitions" }
                return
            }
            val entry = CjoSourceOwnedFunction(identity, body, declarationReference, lambda, lexicalScope)
            functions[identity] = entry
            declarationReference?.let { check(functionByDeclaration.put(it, identity) == null) }
            lambda?.let { check(functionByLambda.put(it, identity) == null) }
            work.addLast {
                val context = CjoSourceContext.Function(identity)
                val isBorrowedHelper = requestedRoots != null && declarationReference in defaultHelperDeclarations
                val selectedBody = !isBorrowedHelper && includeHeaderContent &&
                    (includeFunctionBodies || lambda != null || lexicalScope.includesInitializers)
                val headerScope = CjoSourceScope(context, context, identity, null, selectedBody)
                for (ordinal in 0 until body.paramListsLength) {
                    val list = checkNotNull(body.paramLists(ordinal))
                    check(list.desugarsLength == 0 || list.desugarsLength == list.paramsLength)
                    for (index in 0 until list.paramsLength) {
                        val parameter = list.params(index)
                        check(parameter != 0u && parameter <= pkg.allDeclsLength.toUInt() &&
                            checkNotNull(pkg.allDecls(parameter.toInt() - 1)).kind == DeclKind.FuncParam) {
                            "A parameter-list slot must reference its actual FuncParam"
                        }
                        val occurrence = CjoSourceOwningParent.ParameterList(identity, ordinal) to index
                        check(parameterOccurrences.putIfAbsent(parameter, occurrence) == null) {
                            "A parameter cannot occupy two signature slots"
                        }
                        declaration(parameter, CjoSourceOwningParent.ParameterList(identity, ordinal), headerScope,
                            CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.VALUE_PARAMETER, index))
                        if (list.desugarsLength != 0 && list.desugars(index) != 0u) {
                            val helper = list.desugars(index)
                            check(helper <= pkg.allDeclsLength.toUInt() && checkNotNull(pkg.allDecls(helper.toInt() - 1)).kind == DeclKind.FuncDecl) {
                                "A default-helper slot must reference its actual FuncDecl"
                            }
                            defaultHelperDeclarations += helper
                            // desugar 的 owning parent 是参数；其声明语境仍是原函数的词法外层。
                            declaration(list.desugars(index), CjoSourceOwningParent.Declaration(parameter), lexicalScope,
                                CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.DEFAULT_HELPER, index),
                                helperOwner = identity.takeIf { declarationReference != null && ordinal == 0 })
                        }
                    }
                }
                if (body.body != 0u && selectedBody) expression(body.body, CjoSourceOwningParent.Function(identity),
                    headerScope.copy(returnFunction = identity), CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.FUNCTION_BODY))
            }
        }

        private fun selectsExpression(reference: UInt, scope: CjoSourceScope): Boolean =
            declarationContentRoots == null || scope.includesInitializers || reference in declarationContentRoots

        private fun expression(reference: UInt, parent: CjoSourceOwningParent, inputScope: CjoSourceScope, slot: CjoSourceExpressionSlot) {
            check(includeHeaderContent) { "Header registration cannot read expression content" }
            if (!selectsExpression(reference, inputScope)) return
            val scope = inputScope.copy(includesInitializers = true)
            require(reference != 0u && reference <= pkg.allExprsLength.toUInt()) { "Invalid SOURCE e reference $reference" }
            val entry = CjoSourceOwnedExpression(reference, checkNotNull(pkg.allExprs(reference.toInt() - 1)), parent, scope, slot)
            if (parent is CjoSourceOwningParent.Function) check(entry.wire.kind == ExprKind.Block) { "A saved FuncBody.body must own its actual Block" }
            expressions[reference]?.let { old ->
                check(old.scope.sameContextIdentity(scope)) { "One SOURCE e cannot occupy conflicting owner/F contexts" }
            }
            val occurrences = expressionOccurrences.getOrPut(reference) { ArrayList() }
            if (occurrences.any { it.parent == parent && it.scope == scope && it.slot == slot }) return
            occurrences += entry
            expressions.putIfAbsent(reference, entry)
            work.addLast { visitExpression(entry) }
        }

        private fun visitExpression(entry: CjoSourceOwnedExpression) {
            val wire = entry.wire
            val parent = CjoSourceOwningParent.Expression(entry.reference)
            val scope = entry.scope
            // owning字段的union在使用前验证；canonical payload的晚期检查不能补回错误发现的子槽。
            val owningInfo = when (wire.kind) {
                ExprKind.Block -> ExprInfo.BlockInfo
                ExprKind.LambdaExpr -> ExprInfo.LambdaInfo
                ExprKind.SpawnExpr -> ExprInfo.SpawnInfo
                ExprKind.TryExpr -> ExprInfo.TryInfo
                ExprKind.LetPatternDestructor -> ExprInfo.LetPatternDestructorInfo
                ExprKind.ForInExpr -> ExprInfo.ForInInfo
                ExprKind.MatchCase -> ExprInfo.MatchCaseInfo
                ExprKind.MatchExpr -> ExprInfo.MatchInfo
                ExprKind.RefExpr, ExprKind.MemberAccess -> ExprInfo.ReferenceInfo
                ExprKind.ArrayExpr, ExprKind.ArrayLit -> ExprInfo.ArrayInfo
                else -> null
            }
            if (owningInfo != null) check(wire.infoType == owningInfo) { "SOURCE owning expression kind disagrees with its saved union" }
            if (validateReferences && wire.mapExpr != 0u) borrowedExpressions += entry.reference to wire.mapExpr
            when (wire.kind) {
                ExprKind.Block -> {
                    val info = wire.info(BlockInfo()) as BlockInfo
                    check(info.isExprLength == wire.operandsLength + 1)
                    for (index in 0 until wire.operandsLength) {
                        if (info.isExpr(index)) expression(wire.operands(index), parent, scope, CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.OPERAND, index))
                        else declaration(wire.operands(index), parent, scope, CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.BLOCK_DECLARATION, index))
                    }
                }
                ExprKind.LambdaExpr -> {
                    check(wire.operandsLength == 0)
                    val info = wire.info(LambdaInfo()) as LambdaInfo
                    function(checkNotNull(wire.info(CjoSourceTable())) as CjoSourceTable, info.funcBody, null, entry.reference, scope)
                }
                ExprKind.WildcardExpr, ExprKind.PrimitiveTypeExpr, ExprKind.ReturnExpr, ExprKind.JumpExpr,
                ExprKind.MemberAccess, ExprKind.RefExpr, ExprKind.CallExpr, ExprKind.UnaryExpr, ExprKind.IncOrDecExpr,
                ExprKind.LitConstExpr, ExprKind.BinaryExpr, ExprKind.SubscriptExpr, ExprKind.AssignExpr,
                ExprKind.ArrayExpr, ExprKind.PointerExpr, ExprKind.TypeConvExpr, ExprKind.ThrowExpr, ExprKind.PerformExpr,
                ExprKind.SpawnExpr, ExprKind.ArrayLit, ExprKind.TupleLit, ExprKind.IfExpr, ExprKind.TryExpr,
                ExprKind.WhileExpr, ExprKind.DoWhileExpr, ExprKind.MatchExpr, ExprKind.LetPatternDestructor,
                ExprKind.ForInExpr, ExprKind.MatchCase, ExprKind.MatchCaseOther, ExprKind.InvalidExpr -> {
                    for (index in 0 until wire.operandsLength) {
                        val child = wire.operands(index)
                        if (child != 0u) expression(child, parent, scope, CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.OPERAND, index))
                    }
                }
                else -> error("Unsupported owned SOURCE expression kind ${wire.kind}")
            }
            when (wire.kind) {
                ExprKind.ReturnExpr -> if (validateReferences) check(scope.returnFunction != null) { "Return must belong to an actual SOURCE FuncBody.body" }
                ExprKind.RefExpr, ExprKind.MemberAccess -> if (validateReferences) {
                    val target = (wire.info(ReferenceInfo()) as ReferenceInfo).target
                    if (classifySourceReferenceTarget(target) == CjoSourceReferenceTargetKind.DECLARATION)
                        borrowedDeclarations += checkNotNull(target)
                }
                ExprKind.ArrayExpr, ExprKind.ArrayLit -> if (validateReferences) {
                    val target = (wire.info(ArrayInfo()) as ArrayInfo).initFunc
                    if (classifySourceReferenceTarget(target) == CjoSourceReferenceTargetKind.DECLARATION)
                        borrowedDeclarations += checkNotNull(target)
                }
                ExprKind.SpawnExpr -> {
                    val target = checkNotNull((wire.info(SpawnInfo()) as SpawnInfo).future)
                    check(target.pkgId == PackageIndex.CURRENT.value)
                    declaration(target.index, parent, scope, CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.FUTURE))
                }
                ExprKind.TryExpr -> {
                    val info = wire.info(TryInfo()) as TryInfo
                    for (index in 0 until info.resourcesLength) {
                        val target = checkNotNull(info.resources(index))
                        check(target.pkgId == PackageIndex.CURRENT.value)
                        declaration(target.index, parent, scope, CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.RESOURCE, index))
                    }
                    patterns(checkNotNull(wire.info(CjoSourceTable())) as CjoSourceTable, 6, info.patternsLength, parent, scope)
                }
                ExprKind.LetPatternDestructor -> {
                    val info = wire.info(LetPatternDestructorInfo()) as LetPatternDestructorInfo
                    patterns(checkNotNull(wire.info(CjoSourceTable())) as CjoSourceTable, 4, info.patternsLength, parent, scope)
                }
                ExprKind.ForInExpr -> pattern((checkNotNull(wire.info(CjoSourceTable())) as CjoSourceTable).tablePosition(4), parent, scope,
                    CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.EXPRESSION_PATTERN, field = 4))
                ExprKind.MatchCase -> {
                    val info = wire.info(MatchCaseInfo()) as MatchCaseInfo
                    patterns(checkNotNull(wire.info(CjoSourceTable())) as CjoSourceTable, 4, info.patternsLength, parent, scope)
                }
            }
        }

        private fun patterns(info: CjoSourceTable, field: Int, count: Int, parent: CjoSourceOwningParent, scope: CjoSourceScope) {
            val kind = if (parent is CjoSourceOwningParent.Pattern) CjoSourceDefinitionSlotKind.NESTED_PATTERN else CjoSourceDefinitionSlotKind.EXPRESSION_PATTERN
            for (index in 0 until count) pattern(info.tablePosition(field, index), parent, scope, CjoSourceDefinitionSlot(kind, index, field))
        }

        private fun pattern(position: Int, parent: CjoSourceOwningParent, scope: CjoSourceScope, slot: CjoSourceDefinitionSlot) {
            val previous = patternParents.putIfAbsent(position, parent)
            check(previous == null || previous == parent) { "SOURCE pattern has multiple owning parents" }
            if (previous != null) {
                check(patterns.getValue(position).scope.sameContextIdentity(scope) && patterns.getValue(position).slot == slot) { "One SOURCE P cannot occupy conflicting definition slots/contexts" }
                return
            }
            patterns[position] = CjoSourceOwnedPattern(position, Pattern().__assign(position, pkg.getByteBuffer()), parent, scope, slot)
            work.addLast {
                val wire = Pattern().__assign(position, pkg.getByteBuffer())
                val patternParent = CjoSourceOwningParent.Pattern(position)
                if (wire.kind == PatternKind.VarPattern) {
                    check(wire.exprsLength == 1) { "A VarPattern requires exactly one saved binding declaration" }
                    val binding = wire.exprs(0)
                    check(binding != 0u && binding <= pkg.allDeclsLength.toUInt() && checkNotNull(pkg.allDecls(binding.toInt() - 1)).kind == DeclKind.VarDecl) { "A VarPattern binding must be its actual VarDecl" }
                }
                for (index in 0 until wire.exprsLength) {
                    val child = wire.exprs(index)
                    if (wire.kind == PatternKind.VarPattern) declaration(child, patternParent, scope,
                        CjoSourceDefinitionSlot(CjoSourceDefinitionSlotKind.PATTERN_BINDING, index), isPatternBinding = true)
                    else if (includeHeaderContent) expression(child, patternParent, scope, CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.PATTERN_EXPRESSION, index))
                }
                patterns(CjoSourceTable().at(position, pkg.getByteBuffer()), 10, wire.patternsLength, patternParent, scope)
            }
        }
    }

}
