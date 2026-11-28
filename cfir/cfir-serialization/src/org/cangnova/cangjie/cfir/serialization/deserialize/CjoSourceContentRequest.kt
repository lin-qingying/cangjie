package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex
import java.util.Collections

/** 内容请求必须来自原保存槽；裸e下标没有声明/F拥有证明，不能作为正文授权。 */
internal sealed interface CjoSourceContentRoot {
    /** 纯metadata请求只闭合原声明/函数/参数身份，不能因此选择任何e槽。 */
    data class DeclarationHeader(val declaration: UInt) : CjoSourceContentRoot
    /** P header 借用已经完成的实际 P；不会选择其 initializer、F.body 或新的 P/e 内容。 */
    data class DeclarationPatternHeader(val declaration: UInt) : CjoSourceContentRoot
    /** 独立type-first入口也必须有同包的显式保存槽授权，不通过cache命中取得额外权限。 */
    data class TypeMetadata(val reference: UInt) : CjoSourceContentRoot
    /** 冷裸e不能恢复祖先；此入口只能接受此前拥有遍历登记的精确occurrence。 */
    data class SelectedExpression(val occurrence: CjoSourceExpressionContext) : CjoSourceContentRoot
    /** 注解内容按宿主显式选择，包含真实的零参数注解；不选择宿主F.body。 */
    data class DeclarationAnnotations(val declaration: UInt) : CjoSourceContentRoot
    data class FunctionBody(val function: CjoSourceFunctionIdentity) : CjoSourceContentRoot
    data class Initializer(val declaration: UInt) : CjoSourceContentRoot
    data class ParameterDefault(val parameter: UInt) : CjoSourceContentRoot
    data class AnnotationArgument(val declaration: UInt, val annotation: Int, val argument: Int) : CjoSourceContentRoot
}

/** 保存槽的来源随需求冻结；类型闭包不通过目标名称或消费者额外扫描恢复。 */
internal enum class CjoSourceBindingSlot {
    TYPE, TARGET, INIT_FUNCTION, INSTANTIATION, MATCHED_PARENT, TYPE_ARGUMENT, CLASSIFIER,
    UPPER_BOUND, FUNCTION_RETURN, ANNOTATION_TARGET, CONSTRAINT_SUBJECT, CONSTRAINT_UPPER, INHERITED_TYPE, ALIAS_RHS,
    GENERIC_DECLARATION, COMMON_DEPENDENCY, CONSTANT_TYPE, CONSTANT_FIELD_TYPE, SEMANTIC_PARAMETER_TYPE,
}

internal sealed interface CjoSourceBindingSite {
    data class Declaration(val reference: UInt, val slot: CjoSourceBindingSlot, val ordinal: Int = 0, val subOrdinal: Int = 0) : CjoSourceBindingSite
    data class Expression(val occurrence: CjoSourceExpressionContext, val slot: CjoSourceBindingSlot, val ordinal: Int = 0) : CjoSourceBindingSite
    data class Function(val identity: CjoSourceFunctionIdentity) : CjoSourceBindingSite
    data class Pattern(val position: Int, val ordinal: Int) : CjoSourceBindingSite
    data class Type(val reference: UInt, val slot: CjoSourceBindingSlot, val ordinal: Int = 0) : CjoSourceBindingSite
    data class Constant(val poolIndex: UInt, val slot: CjoSourceBindingSlot, val ordinal: Int = 0) : CjoSourceBindingSite
}

/** FullId仍由原包binder解析；expectedKind仅用于实际classifier/参数种类一致性校验。 */
internal data class CjoSourceDeclarationBinding(val site: CjoSourceBindingSite, val target: FullId, val expectedKind: UShort? = null)
internal data class CjoSourceTypeBinding(val site: CjoSourceBindingSite, val reference: UInt)

/** 名称仅定位已选classifier内的保存字段；外包由原provider完成并返回确切field symbol。 */
internal data class CjoSourceConstantFieldBinding(
    val poolIndex: UInt, val ordinal: Int, val classifier: FullId, val name: String, val type: UInt, val currentDeclaration: UInt?,
)

internal enum class CjoSourceExpressionBindingRole { MAP, REF_LOOP, SELECTOR }

/** targetContexts是原registry occurrence证明；借用目标仍须由binder提供已完成record，不能reserve为own。 */
internal class CjoSourceExpressionBinding(
    val role: CjoSourceExpressionBindingRole,
    val source: CjoSourceExpressionContext,
    val target: UInt,
    targetContexts: List<CjoSourceExpressionContext>,
    val owned: Boolean,
) {
    val targetContexts = Collections.unmodifiableList(targetContexts.toList())
}

/** 真实metadata祖先只授header/context身份，不授其body、default或annotation内容。 */
internal class CjoSourceBorrowedContexts(
    declarations: Map<UInt, CjoSourceOwnedDeclaration>,
    functions: Map<CjoSourceFunctionIdentity, CjoSourceOwnedFunction>,
    patterns: Map<Int, CjoSourceOwnedPattern>,
) {
    val declarations = Collections.unmodifiableMap(LinkedHashMap(declarations))
    val functions = Collections.unmodifiableMap(LinkedHashMap(functions))
    val patterns = Collections.unmodifiableMap(LinkedHashMap(patterns))
}

/** 一个请求的三个身份域分别冻结；borrowedBindings是明确待绑定需求，绝非owned e。 */
internal class CjoSourceContentSelection(
    roots: List<CjoSourceContentRoot>,
    val ownedContent: CjoSourceOwnershipPlan,
    val borrowedContexts: CjoSourceBorrowedContexts,
    expressionBindings: List<CjoSourceExpressionBinding>,
    selectedBodies: Set<CjoSourceFunctionIdentity>,
    declarationBindings: List<CjoSourceDeclarationBinding>,
    typeBindings: List<CjoSourceTypeBinding>,
    types: Map<UInt, SemaTy>,
    constantValues: Map<UInt, CjoConstValueReader.Value.CompositeValue>,
    constantFields: List<CjoSourceConstantFieldBinding>,
    val registeredHeaders: CjoSourceOwnershipPlan,
) {
    val roots = Collections.unmodifiableList(roots.toList())
    val expressionBindings = Collections.unmodifiableList(expressionBindings.toList())
    /** 绑定计划已核验全部 occurrence；decoder 只消费每个 case 的唯一目标。 */
    val matchCaseSelectors: Map<UInt, UInt> = Collections.unmodifiableMap(LinkedHashMap<UInt, UInt>().apply {
        expressionBindings.filter { it.role == CjoSourceExpressionBindingRole.SELECTOR }.forEach { binding ->
            check(putIfAbsent(binding.source.reference, binding.target).let { it == null || it == binding.target })
        }
    })
    val borrowedBindings = Collections.unmodifiableList(expressionBindings.filter { !it.owned })
    val selectedBodies = Collections.unmodifiableSet(LinkedHashSet(selectedBodies))
    val selectedPatternHeaders: Set<UInt> = Collections.unmodifiableSet(LinkedHashSet(
        roots.filterIsInstance<CjoSourceContentRoot.DeclarationPatternHeader>().map { it.declaration }))
    val declarationBindings = Collections.unmodifiableList(declarationBindings.toList())
    val typeBindings = Collections.unmodifiableList(typeBindings.toList())
    val types = Collections.unmodifiableMap(LinkedHashMap(types))
    val constantValues = Collections.unmodifiableMap(LinkedHashMap(constantValues))
    val constantFields = Collections.unmodifiableList(constantFields.toList())
    val selectedInitializers: Set<UInt> = Collections.unmodifiableSet(LinkedHashSet(
        roots.filterIsInstance<CjoSourceContentRoot.Initializer>().map { it.declaration } +
            ownedContent.expressionOccurrences.filter { it.slot.kind == CjoSourceExpressionSlotKind.INITIALIZER }
                .map { (it.parent as CjoSourceOwningParent.Declaration).reference }))
    val selectedDefaults: Set<UInt> = Collections.unmodifiableSet(LinkedHashSet(
        roots.filterIsInstance<CjoSourceContentRoot.ParameterDefault>().map { it.parameter } +
            ownedContent.expressionOccurrences.filter { it.slot.kind == CjoSourceExpressionSlotKind.PARAMETER_DEFAULT }
                .map { (it.parent as CjoSourceOwningParent.Declaration).reference }))
    /** 只有完整Anno.args向量属于此次请求时才发布声明注解图；单arg请求保持其独立槽身份。 */
    val selectedAnnotations: Set<UInt> = Collections.unmodifiableSet(LinkedHashSet(
        roots.filterIsInstance<CjoSourceContentRoot.DeclarationAnnotations>().map { it.declaration } +
            ownedContent.declarations.keys.filter { reference ->
                val wire = registeredHeaders.declarations.getValue(reference).wire
                (0 until wire.annotationsLength).all { annotationIndex ->
                    val annotation = checkNotNull(wire.annotations(annotationIndex))
                    (0 until annotation.argsLength).all { argumentIndex ->
                        ownedContent.expressionOccurrences.any { occurrence -> occurrence.parent ==
                            CjoSourceOwningParent.AnnotationArgument(reference, annotationIndex, argumentIndex) }
                    }
                }
            }))

    init {
        check(ownedContent.kind == CjoSourcePlanKind.CONTENT)
        check(ownedContent.declarations.keys.all { it in borrowedContexts.declarations })
        check(ownedContent.functions.keys.all { it in borrowedContexts.functions })
        check(selectedBodies.all { it in ownedContent.functions })
        check(borrowedBindings.none { it.target in ownedContent.expressions })
        check(registeredHeaders.kind == CjoSourcePlanKind.HEADER_REGISTRY)
        check(typeBindings.all { it.reference == 0u || it.reference in this.types })
    }
}

/** 原registry与此次真正下行发现的header共同闭合context；此遍历不调用任一表达式reader。 */
/** 纯owning发现只合并已证明的metadata，不校验或绑定正文的borrow/type使用。 */
internal fun discoveredSourceHeaders(headers: CjoSourceOwnershipPlan, content: CjoSourceOwnershipPlan): CjoSourceOwnershipPlan {
    check(headers.ownerPackage === content.ownerPackage)
    check(headers.headerBindingPolicy == content.headerBindingPolicy)
    val declarations = LinkedHashMap(headers.declarations)
    content.declarations.forEach { (reference, entry) ->
        declarations[reference]?.let { old ->
            check(old.parent == entry.parent && old.slot == entry.slot && old.scope.sameContextIdentity(entry.scope) &&
                old.isPatternBinding == entry.isPatternBinding && old.helperOwner == entry.helperOwner && old.accessorOwner == entry.accessorOwner) {
                "A selected declaration cannot acquire a different header owner"
            }
        }
        declarations[reference] = entry
    }
    val functions = LinkedHashMap(headers.functions)
    content.functions.forEach { (identity, entry) ->
        functions[identity]?.let { old ->
            check(old.declaration == entry.declaration && old.lambda == entry.lambda &&
                old.lexicalScope.sameContextIdentity(entry.lexicalScope))
        }
        functions[identity] = entry
    }
    val functionByDeclaration = LinkedHashMap(headers.functionByDeclaration)
    content.functionByDeclaration.forEach { (reference, identity) ->
        check(functionByDeclaration.putIfAbsent(reference, identity).let { it == null || it == identity })
    }
    val patterns = LinkedHashMap(headers.patterns)
    content.patterns.forEach { (position, entry) ->
        patterns[position]?.let { check(it.parent == entry.parent && it.slot == entry.slot && it.scope.sameContextIdentity(entry.scope)) }
        patterns[position] = entry
    }
    return headers.discoveredHeaders(content, declarations, functions, patterns)
}

internal fun sourceContentSelection(
    headers: CjoSourceOwnershipPlan,
    content: CjoSourceOwnershipPlan,
    roots: List<CjoSourceContentRoot>,
): CjoSourceContentSelection {
    val registeredHeaders = discoveredSourceHeaders(headers, content)
    val declarations = registeredHeaders.declarations
    val functions = registeredHeaders.functions
    val functionByDeclaration = registeredHeaders.functionByDeclaration
    val patterns = registeredHeaders.patterns
    val expressionContexts = registeredHeaders.expressionContexts
    val neededDeclarations = LinkedHashMap<UInt, CjoSourceOwnedDeclaration>()
    val neededFunctions = LinkedHashMap<CjoSourceFunctionIdentity, CjoSourceOwnedFunction>()
    val neededPatterns = LinkedHashMap<Int, CjoSourceOwnedPattern>()
    val pendingContexts = ArrayDeque<CjoSourceContext>()
    val pendingPatterns = ArrayDeque<Int>()
    fun context(value: CjoSourceContext) { if (value != CjoSourceContext.Package) pendingContexts.addLast(value) }
    fun scope(value: CjoSourceScope) {
        context(value.declarationContext); context(value.expressionContext)
        value.function?.let { context(CjoSourceContext.Function(it)) }
        value.returnFunction?.let { context(CjoSourceContext.Function(it)) }
    }
    fun parent(value: CjoSourceOwningParent) {
        when (value) {
            CjoSourceOwningParent.Package -> Unit
            is CjoSourceOwningParent.Declaration -> context(CjoSourceContext.Declaration(value.reference))
            is CjoSourceOwningParent.Function -> context(CjoSourceContext.Function(value.identity))
            is CjoSourceOwningParent.ParameterList -> context(CjoSourceContext.Function(value.function))
            is CjoSourceOwningParent.AnnotationArgument -> context(CjoSourceContext.Declaration(value.declaration))
            is CjoSourceOwningParent.Expression -> expressionContexts.getValue(value.reference).forEach { scope(it.scope) }
            is CjoSourceOwningParent.Pattern -> pendingPatterns.addLast(value.tablePosition)
        }
    }
    roots.forEach { root ->
        when (root) {
            is CjoSourceContentRoot.DeclarationHeader -> context(CjoSourceContext.Declaration(root.declaration))
            is CjoSourceContentRoot.DeclarationPatternHeader -> context(CjoSourceContext.Declaration(root.declaration))
            is CjoSourceContentRoot.TypeMetadata -> Unit
            is CjoSourceContentRoot.SelectedExpression -> { scope(root.occurrence.scope); parent(root.occurrence.parent) }
            is CjoSourceContentRoot.DeclarationAnnotations -> context(CjoSourceContext.Declaration(root.declaration))
            is CjoSourceContentRoot.FunctionBody -> context(CjoSourceContext.Function(root.function))
            is CjoSourceContentRoot.Initializer -> context(CjoSourceContext.Declaration(root.declaration))
            is CjoSourceContentRoot.ParameterDefault -> context(CjoSourceContext.Declaration(root.parameter))
            is CjoSourceContentRoot.AnnotationArgument -> context(CjoSourceContext.Declaration(root.declaration))
        }
    }
    content.declarations.keys.forEach { context(CjoSourceContext.Declaration(it)) }
    content.functions.keys.forEach { context(CjoSourceContext.Function(it)) }
    content.expressionOccurrences.forEach { scope(it.scope) }
    content.patterns.keys.forEach(pendingPatterns::addLast)
    val declarationBindings = ArrayList<CjoSourceDeclarationBinding>()
    val typeBindings = ArrayList<CjoSourceTypeBinding>()
    val types = LinkedHashMap<UInt, SemaTy>()
    val pendingTypes = ArrayDeque<UInt>()
    val constantValues = LinkedHashMap<UInt, CjoConstValueReader.Value.CompositeValue>()
    val constantFields = ArrayList<CjoSourceConstantFieldBinding>()
    val pendingValues = ArrayDeque<UInt>()
    fun binding(site: CjoSourceBindingSite, reference: FullId?, expectedKind: UShort? = null) {
        val kind = classifySourceReferenceTarget(reference)
        if (expectedKind != null) check(kind == CjoSourceReferenceTargetKind.DECLARATION) { "A SOURCE classifier requires its actual declaration FullId" }
        if (kind != CjoSourceReferenceTargetKind.DECLARATION) return
        val target = checkNotNull(reference)
        declarationBindings += CjoSourceDeclarationBinding(site, target, expectedKind)
        if (target.pkgId == PackageIndex.CURRENT.value) {
            val declaration = checkNotNull(declarations[target.index]) { "SOURCE target d ${target.index} requires its actual registered owning header" }
            if (expectedKind != null) check(declaration.wire.kind == expectedKind) { "SOURCE classifier kind disagrees with its actual declaration" }
            context(CjoSourceContext.Declaration(target.index))
        }
    }
    fun type(site: CjoSourceBindingSite, reference: UInt) {
        // 零槽保留Invalid/optional/placeholder语义；它不是allTypes[-1]或Unit记录。
        typeBindings += CjoSourceTypeBinding(site, reference)
        if (reference == 0u) return
        require(reference <= content.ownerPackage.allTypesLength.toUInt()) { "Invalid SOURCE type reference $reference" }
        pendingTypes.addLast(reference)
    }
    roots.filterIsInstance<CjoSourceContentRoot.TypeMetadata>().forEach { root ->
        type(CjoSourceBindingSite.Type(root.reference, CjoSourceBindingSlot.TYPE), root.reference)
    }
    fun value(wire: CjoConstValueReader.Value) {
        when (wire) {
            is CjoConstValueReader.Value.Scalar, is CjoConstValueReader.Value.StringValue -> Unit
            is CjoConstValueReader.Value.ArrayValue -> wire.elements.forEach(::value)
            is CjoConstValueReader.Value.CompositeReference -> pendingValues.addLast(wire.index)
            is CjoConstValueReader.Value.CompositeValue -> error("A SOURCE constant demand requires its raw pool reference")
        }
    }
    fun frozenValue(wire: CjoConstValueReader.Value): CjoConstValueReader.Value = when (wire) {
        is CjoConstValueReader.Value.Scalar, is CjoConstValueReader.Value.StringValue,
        is CjoConstValueReader.Value.CompositeReference -> wire
        is CjoConstValueReader.Value.ArrayValue -> CjoConstValueReader.Value.ArrayValue(Collections.unmodifiableList(wire.elements.map(::frozenValue)))
        is CjoConstValueReader.Value.CompositeValue -> error("Raw constant fields cannot contain expanded pool objects")
    }
    val bindings = collectSourceExpressionBindings(content, expressionContexts, registeredHeaders.declarations, registeredHeaders.patterns)
    bindings.filter { !it.owned }.forEach { binding ->
        binding.targetContexts.forEach { occurrence ->
            scope(occurrence.scope); parent(occurrence.parent)
            type(CjoSourceBindingSite.Expression(occurrence, CjoSourceBindingSlot.TYPE), occurrence.type)
        }
    }
    content.expressionOccurrences.forEach { entry ->
        val occurrence = content.expressionContexts.getValue(entry.reference).single { it.parent == entry.parent && it.scope == entry.scope && it.slot == entry.slot }
        type(CjoSourceBindingSite.Expression(occurrence, CjoSourceBindingSlot.TYPE), entry.wire.type)
        when (entry.wire.kind) {
            ExprKind.RefExpr, ExprKind.MemberAccess -> {
                check(entry.wire.infoType == ExprInfo.ReferenceInfo)
                (entry.wire.info(ReferenceInfo()) as ReferenceInfo).let { info ->
                binding(CjoSourceBindingSite.Expression(occurrence, CjoSourceBindingSlot.TARGET), info.target)
                for (ordinal in 0 until info.instTysLength) type(CjoSourceBindingSite.Expression(occurrence, CjoSourceBindingSlot.INSTANTIATION, ordinal), info.instTys(ordinal))
                type(CjoSourceBindingSite.Expression(occurrence, CjoSourceBindingSlot.MATCHED_PARENT), info.matchedParentTy)
                }
            }
            ExprKind.ArrayExpr, ExprKind.ArrayLit -> {
                check(entry.wire.infoType == ExprInfo.ArrayInfo)
                val typeKind = if (entry.wire.type == 0u) TypeKind.Invalid else checkNotNull(content.ownerPackage.allTypes(entry.wire.type.toInt() - 1)).kind
                if (typeKind != TypeKind.VArray) {
                    val target = (entry.wire.info(ArrayInfo()) as ArrayInfo).initFunc
                    val expectedKind = if (entry.wire.kind == ExprKind.ArrayLit || classifySourceReferenceTarget(target) == CjoSourceReferenceTargetKind.DECLARATION) DeclKind.FuncDecl else null
                    binding(CjoSourceBindingSite.Expression(occurrence, CjoSourceBindingSlot.INIT_FUNCTION), target, expectedKind)
                }
            }
        }
    }
    while (pendingContexts.isNotEmpty() || pendingPatterns.isNotEmpty() || pendingTypes.isNotEmpty() || pendingValues.isNotEmpty()) {
        while (pendingValues.isNotEmpty()) {
            val index = pendingValues.removeFirst()
            if (index in constantValues) continue
            val wire = CjoConstValueReader.readCompositeRaw(index, content.ownerPackage)
            constantValues[index] = wire.copy(fields = Collections.unmodifiableList(wire.fields.map { it.copy(value = frozenValue(it.value)) }))
            type(CjoSourceBindingSite.Constant(index, CjoSourceBindingSlot.CONSTANT_TYPE), wire.type)
            check(wire.type != 0u)
            val typeWire = checkNotNull(content.ownerPackage.allTypes(wire.type.toInt() - 1))
            val classifier = if (typeWire.kind in listOf(TypeKind.Class, TypeKind.Struct, TypeKind.Enum))
                checkNotNull((typeWire.info(CompositeTyInfo()) as CompositeTyInfo).declPtr) else null
            wire.fields.forEachIndexed { ordinal, field ->
                type(CjoSourceBindingSite.Constant(index, CjoSourceBindingSlot.CONSTANT_FIELD_TYPE, ordinal), field.type)
                if (classifier != null) {
                    val declaration = if (classifier.pkgId == PackageIndex.CURRENT.value) declarations.values.single { entry ->
                        entry.parent == CjoSourceOwningParent.Declaration(classifier.index) && entry.wire.kind == DeclKind.VarDecl && entry.wire.identifier == field.name
                    }.reference else null
                    declaration?.let { context(CjoSourceContext.Declaration(it)) }
                    constantFields += CjoSourceConstantFieldBinding(index, ordinal, classifier, field.name, field.type, declaration)
                }
                value(field.value)
            }
        }
        while (pendingTypes.isNotEmpty()) {
            val reference = pendingTypes.removeFirst()
            if (reference in types) continue
            val wire = checkNotNull(content.ownerPackage.allTypes(reference.toInt() - 1))
            types[reference] = wire
            if (wire.kind in listOf(TypeKind.CPointer, TypeKind.Array, TypeKind.VArray, TypeKind.Tuple,
                TypeKind.Func, TypeKind.Class, TypeKind.Interface, TypeKind.Struct, TypeKind.Enum, TypeKind.Type))
                for (ordinal in 0 until wire.typeArgsLength) type(CjoSourceBindingSite.Type(reference, CjoSourceBindingSlot.TYPE_ARGUMENT, ordinal), wire.typeArgs(ordinal))
            when (wire.kind) {
                TypeKind.Class, TypeKind.Interface, TypeKind.Struct, TypeKind.Enum, TypeKind.Type -> {
                    check(wire.infoType == SemaTyInfo.CompositeTyInfo)
                    val info = checkNotNull(wire.info(CompositeTyInfo()) as? CompositeTyInfo)
                    val expectedKind = when (wire.kind) {
                        TypeKind.Class -> DeclKind.ClassDecl
                        TypeKind.Interface -> DeclKind.InterfaceDecl
                        TypeKind.Struct -> DeclKind.StructDecl
                        TypeKind.Enum -> DeclKind.EnumDecl
                        else -> DeclKind.TypeAliasDecl
                    }
                    binding(CjoSourceBindingSite.Type(reference, CjoSourceBindingSlot.CLASSIFIER), info.declPtr, expectedKind)
                }
                TypeKind.Generic -> {
                    check(wire.infoType == SemaTyInfo.GenericTyInfo)
                    val info = checkNotNull(wire.info(GenericTyInfo()) as? GenericTyInfo)
                    binding(CjoSourceBindingSite.Type(reference, CjoSourceBindingSlot.CLASSIFIER), info.declPtr, DeclKind.GenericParamDecl)
                    for (ordinal in 0 until info.upperBoundsLength) type(CjoSourceBindingSite.Type(reference, CjoSourceBindingSlot.UPPER_BOUND, ordinal), info.upperBounds(ordinal))
                }
                TypeKind.Func -> {
                    check(wire.infoType == SemaTyInfo.FuncTyInfo)
                    type(CjoSourceBindingSite.Type(reference, CjoSourceBindingSlot.FUNCTION_RETURN), checkNotNull(wire.info(FuncTyInfo()) as? FuncTyInfo).retType)
                }
                TypeKind.Array, TypeKind.VArray -> {
                    check(wire.infoType == SemaTyInfo.ArrayTyInfo && wire.typeArgsLength == 1) { "A saved array type requires its actual element and dimension/size info" }
                }
                TypeKind.CPointer -> check(wire.typeArgsLength == 1) { "A saved pointer type requires its actual pointee slot" }
            }
        }
        while (pendingPatterns.isNotEmpty()) {
            val position = pendingPatterns.removeFirst()
            if (position in neededPatterns) continue
            val entry = patterns.getValue(position)
            neededPatterns[position] = entry
            scope(entry.scope); parent(entry.parent)
            for (ordinal in 0 until entry.wire.typesLength) type(CjoSourceBindingSite.Pattern(position, ordinal), entry.wire.types(ordinal))
            // HEADER_REGISTRY的P只提供结构；仅真正选中的P.values具有常量内容能力。
            if (position in content.patterns) CjoConstValueReader.readRawRoots(entry.wire).forEach { value(it.value) }
        }
        if (pendingContexts.isEmpty()) continue
        when (val next = pendingContexts.removeFirst()) {
            CjoSourceContext.Package -> Unit
            is CjoSourceContext.Declaration -> {
                if (next.reference in neededDeclarations) continue
                val entry = declarations.getValue(next.reference)
                neededDeclarations[next.reference] = entry
                scope(entry.scope); parent(entry.parent)
                type(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.TYPE), entry.wire.type)
                binding(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.GENERIC_DECLARATION), entry.wire.genericDecl)
                if (headers.headerBindingPolicy == CjoSourceHeaderBindingPolicy.COMMON_DEPENDENCIES)
                    for (ordinal in 0 until entry.wire.dependenciesLength) binding(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.COMMON_DEPENDENCY, ordinal), entry.wire.dependencies(ordinal))
                if (entry.wire.kind == DeclKind.VarDecl) CjoConstValueReader.readRawRoot(entry.wire.info(VarInfo()) as VarInfo)?.let { value(it.value) }
                for (ordinal in 0 until entry.wire.annotationsLength) {
                    val annotation = checkNotNull(entry.wire.annotations(ordinal))
                    if (annotation.kind == AnnoKind.Custom) binding(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.ANNOTATION_TARGET, ordinal), annotation.target)
                }
                entry.helperOwner?.let { context(CjoSourceContext.Function(it)) }
                entry.accessorOwner?.let { context(CjoSourceContext.Declaration(it)) }
                if (entry.wire.kind == DeclKind.FuncDecl)
                    context(CjoSourceContext.Function(functionByDeclaration.getValue(next.reference)))
                entry.wire.generic?.let { generic ->
                    for (index in 0 until generic.typeParametersLength) context(CjoSourceContext.Declaration(generic.typeParameters(index)))
                    for (ordinal in 0 until generic.constraintsLength) {
                        val constraint = checkNotNull(generic.constraints(ordinal))
                        type(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.CONSTRAINT_SUBJECT, ordinal), constraint.type)
                        for (upper in 0 until constraint.uppersLength) type(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.CONSTRAINT_UPPER, ordinal, upper), constraint.uppers(upper))
                    }
                }
                fun inherited(length: Int, getter: (Int) -> UInt) {
                    for (ordinal in 0 until length) type(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.INHERITED_TYPE, ordinal), getter(ordinal))
                }
                fun members(length: Int, getter: (Int) -> UInt) {
                    // 原完整classifier header包含成员header；这些依赖仍没有body/default/annotation e权限。
                    for (ordinal in 0 until length) context(CjoSourceContext.Declaration(getter(ordinal)))
                }
                when (entry.wire.kind) {
                    DeclKind.ClassDecl -> (entry.wire.info(ClassInfo()) as ClassInfo).let { inherited(it.inheritedTypesLength, it::inheritedTypes); members(it.bodyLength, it::body) }
                    DeclKind.InterfaceDecl -> (entry.wire.info(InterfaceInfo()) as InterfaceInfo).let { inherited(it.inheritedTypesLength, it::inheritedTypes); members(it.bodyLength, it::body) }
                    DeclKind.StructDecl -> (entry.wire.info(StructInfo()) as StructInfo).let { inherited(it.inheritedTypesLength, it::inheritedTypes); members(it.bodyLength, it::body) }
                    DeclKind.EnumDecl -> (entry.wire.info(EnumInfo()) as EnumInfo).let { inherited(it.inheritedTypesLength, it::inheritedTypes); members(it.bodyLength, it::body) }
                    DeclKind.ExtendDecl -> (entry.wire.info(ExtendInfo()) as ExtendInfo).let { inherited(it.inheritedTypesLength, it::inheritedTypes); members(it.bodyLength, it::body) }
                    DeclKind.TypeAliasDecl -> type(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.ALIAS_RHS), (entry.wire.info(AliasInfo()) as AliasInfo).aliasedTy)
                    DeclKind.PropDecl -> (entry.wire.info(PropInfo()) as PropInfo).let { members(it.gettersLength, it::getters); members(it.settersLength, it::setters) }
                    DeclKind.GenericParamDecl -> headers.genericTypesByParameter[next.reference].orEmpty().forEachIndexed { ordinal, reference ->
                        type(CjoSourceBindingSite.Declaration(next.reference, CjoSourceBindingSlot.SEMANTIC_PARAMETER_TYPE, ordinal), reference)
                    }
                }
            }
            is CjoSourceContext.Function -> {
                if (next.identity in neededFunctions) continue
                val entry = functions.getValue(next.identity)
                neededFunctions[next.identity] = entry
                type(CjoSourceBindingSite.Function(next.identity), entry.body.retType)
                entry.declaration?.let { context(CjoSourceContext.Declaration(it)) }
                entry.lambda?.let { expressionContexts.getValue(it).forEach { occurrence ->
                    parent(occurrence.parent)
                    type(CjoSourceBindingSite.Expression(occurrence, CjoSourceBindingSlot.TYPE), occurrence.type)
                } }
                scope(entry.lexicalScope)
                for (ordinal in 0 until entry.body.paramListsLength) {
                    val parameters = checkNotNull(entry.body.paramLists(ordinal))
                    for (index in 0 until parameters.paramsLength) {
                        context(CjoSourceContext.Declaration(parameters.params(index)))
                        if (parameters.desugarsLength != 0 && parameters.desugars(index) != 0u)
                            context(CjoSourceContext.Declaration(parameters.desugars(index)))
                    }
                }
            }
        }
    }
    val bodies = content.functions.filter { (identity, entry) -> entry.body.body != 0u &&
        content.expressionOccurrences.any { it.reference == entry.body.body && it.parent == CjoSourceOwningParent.Function(identity) &&
            it.slot.kind == CjoSourceExpressionSlotKind.FUNCTION_BODY } }.keys
    validateSourceAncestry(neededDeclarations, neededFunctions, neededPatterns, expressionContexts,
        content.expressions.keys + bindings.filter { !it.owned }.map { it.target })
    return CjoSourceContentSelection(roots, content,
        CjoSourceBorrowedContexts(neededDeclarations, neededFunctions, neededPatterns), bindings, bodies, declarationBindings,
        typeBindings, types, constantValues, constantFields, registeredHeaders)
}

/** 角色/occurrence先于typed固定点分类；仅有proof的borrow仍要求后续binder提供已完成record。 */
private fun collectSourceExpressionBindings(
    content: CjoSourceOwnershipPlan,
    expressionContexts: Map<UInt, List<CjoSourceExpressionContext>>,
    declarations: Map<UInt, CjoSourceOwnedDeclaration>,
    patterns: Map<Int, CjoSourceOwnedPattern>,
): List<CjoSourceExpressionBinding> {
    val bindings = ArrayList<CjoSourceExpressionBinding>()
    // e record 可由同一语境中的多个保存槽共享，但其 P.ctxExpr 必须只有一个真实 selector。
    val selectorsByCase = HashMap<UInt, UInt>()
    content.expressions.values.filter { it.wire.kind == ExprKind.MatchCase }.forEach { entry ->
        // registry 包含此前成功发现的上下文；只读标量证明，不读取未选 Match 的正文。
        expressionContexts.getValue(entry.reference).forEach { occurrence ->
            val match = occurrence.parent as? CjoSourceOwningParent.Expression
                ?: error("A SOURCE MatchCase requires its actual owning Match")
            expressionContexts.getValue(match.reference).forEach { owner ->
                check(owner.kind == ExprKind.MatchExpr && owner.matchSelector != null &&
                    owner.scope.sameContextIdentity(occurrence.scope)) { "SOURCE MatchCase has an invalid owning Match context" }
                val selector = checkNotNull(owner.matchSelector)
                val previous = selectorsByCase.putIfAbsent(entry.reference, selector)
                check(previous == null || previous == selector) {
                    "One SOURCE MatchCase cannot bind different selectors: case e ${entry.reference}, previous e $previous, new e $selector"
                }
            }
        }
    }
    fun encloses(parent: CjoSourceOwningParent, loop: UInt, visiting: MutableSet<SourceAncestryNode>): Boolean {
        if (parent is CjoSourceOwningParent.Expression && parent.reference == loop) return true
        val node = when (parent) {
            is CjoSourceOwningParent.Expression -> SourceAncestryNode.Expression(parent.reference)
            is CjoSourceOwningParent.Declaration -> SourceAncestryNode.Declaration(parent.reference)
            is CjoSourceOwningParent.Pattern -> SourceAncestryNode.Pattern(parent.tablePosition)
            else -> return false
        }
        check(visiting.add(node)) { "SOURCE loop binding has an owning cycle" }
        return try {
            when (parent) {
                is CjoSourceOwningParent.Expression -> expressionContexts.getValue(parent.reference).all { encloses(it.parent, loop, visiting) }
                is CjoSourceOwningParent.Declaration -> encloses(declarations.getValue(parent.reference).parent, loop, visiting)
                is CjoSourceOwningParent.Pattern -> encloses(patterns.getValue(parent.tablePosition).parent, loop, visiting)
                else -> error("SOURCE loop ancestry must use its actual owning node")
            }
        } finally { check(visiting.remove(node)) }
    }
    content.expressionOccurrences.forEach { entry ->
        val source = expressionContexts.getValue(entry.reference).single { it.parent == entry.parent && it.scope.sameContextIdentity(entry.scope) && it.slot == entry.slot }
        fun expressionBinding(role: CjoSourceExpressionBindingRole, target: UInt) {
            require(target != 0u && target <= content.ownerPackage.allExprsLength.toUInt()) { "Invalid SOURCE binding e $target" }
            val targets = checkNotNull(expressionContexts[target]) { "A borrowed SOURCE e requires its actual owning occurrence proof" }
            check(targets.isNotEmpty()) { "A borrowed SOURCE e requires its actual owning occurrence proof" }
            if (role == CjoSourceExpressionBindingRole.REF_LOOP) {
                check(targets.all { it.kind in listOf(ExprKind.WhileExpr, ExprKind.DoWhileExpr, ExprKind.ForInExpr) }) { "SOURCE jump must reference its actual loop" }
                check(targets.all { it.scope.returnFunction == source.scope.returnFunction }) { "SOURCE jump crosses a FuncBody boundary" }
                check(targets.all { it.scope.function == source.scope.function }) { "SOURCE jump crosses its actual lexical FuncBody" }
                check(encloses(source.parent, target, HashSet())) { "SOURCE jump must bind its actual enclosing loop, including header-content domains" }
            }
            if (role == CjoSourceExpressionBindingRole.SELECTOR) check(targets.all { it.scope.sameContextIdentity(source.scope) }) { "SOURCE selector crosses its actual Match context" }
            bindings += CjoSourceExpressionBinding(role, source, target, targets, target in content.expressions)
        }
        when (entry.wire.kind) {
            ExprKind.ReturnExpr -> check(entry.scope.returnFunction != null) { "Return must belong to an actual SOURCE FuncBody.body" }
            ExprKind.JumpExpr -> expressionBinding(CjoSourceExpressionBindingRole.REF_LOOP, entry.wire.mapExpr)
            else -> if (entry.wire.mapExpr != 0u) expressionBinding(CjoSourceExpressionBindingRole.MAP, entry.wire.mapExpr)
        }
        if (entry.wire.kind == ExprKind.MatchCase) {
            expressionBinding(CjoSourceExpressionBindingRole.SELECTOR, selectorsByCase.getValue(entry.reference))
        }
    }
    return bindings
}

private sealed interface SourceAncestryNode {
    data class Declaration(val reference: UInt) : SourceAncestryNode
    data class Function(val identity: CjoSourceFunctionIdentity) : SourceAncestryNode
    data class Expression(val reference: UInt) : SourceAncestryNode
    data class Pattern(val position: Int) : SourceAncestryNode
}

/** owning与lexical分别验证；参数/函数签名的正常互相引用不是祖先边，map环也不加入owning。 */
private fun validateSourceAncestry(
    declarations: Map<UInt, CjoSourceOwnedDeclaration>,
    functions: Map<CjoSourceFunctionIdentity, CjoSourceOwnedFunction>,
    patterns: Map<Int, CjoSourceOwnedPattern>,
    expressionParents: Map<UInt, List<CjoSourceExpressionContext>>,
    ownedExpressions: Set<UInt>,
) {
    fun parent(value: CjoSourceOwningParent): SourceAncestryNode? = when (value) {
        CjoSourceOwningParent.Package -> null
        is CjoSourceOwningParent.Declaration -> SourceAncestryNode.Declaration(value.reference)
        is CjoSourceOwningParent.Function -> SourceAncestryNode.Function(value.identity)
        is CjoSourceOwningParent.ParameterList -> SourceAncestryNode.Function(value.function)
        is CjoSourceOwningParent.Expression -> SourceAncestryNode.Expression(value.reference)
        is CjoSourceOwningParent.Pattern -> SourceAncestryNode.Pattern(value.tablePosition)
        is CjoSourceOwningParent.AnnotationArgument -> SourceAncestryNode.Declaration(value.declaration)
    }
    val visiting = HashSet<SourceAncestryNode>()
    val finished = HashSet<SourceAncestryNode>()
    fun owning(node: SourceAncestryNode) {
        if (node in finished) return
        check(visiting.add(node)) { "SOURCE metadata/content has an owning cycle" }
        val parents = when (node) {
            is SourceAncestryNode.Declaration -> listOfNotNull(parent(declarations.getValue(node.reference).parent))
            is SourceAncestryNode.Pattern -> listOfNotNull(parent(patterns.getValue(node.position).parent))
            is SourceAncestryNode.Expression -> expressionParents.getValue(node.reference).mapNotNull { parent(it.parent) }
            is SourceAncestryNode.Function -> functions.getValue(node.identity).let { entry ->
                if (entry.declaration != null) listOf(SourceAncestryNode.Declaration(entry.declaration))
                else listOf(SourceAncestryNode.Expression(checkNotNull(entry.lambda)))
            }
        }
        parents.forEach(::owning)
        check(visiting.remove(node))
        finished += node
    }
    declarations.keys.forEach { owning(SourceAncestryNode.Declaration(it)) }
    functions.keys.forEach { owning(SourceAncestryNode.Function(it)) }
    patterns.keys.forEach { owning(SourceAncestryNode.Pattern(it)) }
    ownedExpressions.forEach { owning(SourceAncestryNode.Expression(it)) }
    val lexicalVisiting = HashSet<CjoSourceFunctionIdentity>()
    val lexicalDone = HashSet<CjoSourceFunctionIdentity>()
    fun lexical(identity: CjoSourceFunctionIdentity) {
        if (identity in lexicalDone) return
        check(lexicalVisiting.add(identity)) { "SOURCE FuncBody has a lexical/outer-function cycle" }
        val scope = functions.getValue(identity).lexicalScope
        scope.function?.let(::lexical)
        (scope.declarationContext as? CjoSourceContext.Function)?.identity?.let(::lexical)
        check(lexicalVisiting.remove(identity))
        lexicalDone += identity
    }
    functions.keys.forEach(::lexical)
}
