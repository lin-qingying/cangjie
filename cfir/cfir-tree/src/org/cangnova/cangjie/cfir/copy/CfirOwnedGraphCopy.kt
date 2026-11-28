package org.cangnova.cangjie.cfir.copy

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.EvaluatedValue
import org.cangnova.cangjie.cfir.declarations.utils.ImplicitDefaultUpperBoundKey
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirEmptyAnnotationArgumentMapping
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.impl.CfirNamedReferenceWithCandidateBaseImpl
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.name.Name

/** 类型系统显式提供 Cone 类型替换；Simple/VArray 槽要求替换结果仍属于对应类型。 */
fun interface CfirTypeRefRemapper {
    fun remapType(type: ConeCangJieType, context: CfirOwnedGraphCopyContext): ConeCangJieType
}

/** 扩展属性的所有权协议。两阶段分别声明拥有边和复制值，不能默认浅拷贝未知属性。 */
interface CfirOwnedAttributeRemapper {
    fun collectOwnedChildren(key: String, value: Any, visit: (CfirElement) -> Unit)
    fun copyAttribute(key: String, value: Any, context: CfirOwnedGraphCopyContext): Any
}

/** 一次复制域的节点、符号及控制目标映射；借用边只重定位，不扩大拥有域。 */
@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
class CfirOwnedGraphCopyContext internal constructor(
    private val typeRemapper: CfirTypeRefRemapper,
    private val attributeRemapper: CfirOwnedAttributeRemapper,
    symbolRebindings: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>>,
    targetRebindings: Map<CfirTarget<*>, CfirTarget<*>>,
    private val nameRebindings: Map<CfirElement, Name>,
) {
    private val owned = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    private val elements = IdentityHashMap<CfirElement, CfirElement>()
    private val symbols = IdentityHashMap<CfirBasedSymbol<*>, CfirBasedSymbol<*>>()
    private val targets = IdentityHashMap<CfirTarget<*>, CfirTarget<*>>()
    private val attributeCopies = IdentityHashMap<CfirDeclarationAttributes, CfirDeclarationAttributes>()
    private val pendingAttributes = mutableListOf<CfirDeclarationAttributes>()
    private val pendingTargets = mutableListOf<Pair<CfirTarget<*>, CfirTarget<*>>>()
    private val pendingReboundFunctions = mutableListOf<Triple<CfirFunctionTarget, CfirBasedSymbol<*>, CfirFunctionBodyOwner>>()
    private val allocating = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    private val visitor = CfirDeepCopyOwnedGraphVisitor(this)
    private val renamed = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    private val constants = CfirOwnedConstantGraphCopy(this)
    /** 每个真实 Package 数据源只捕获一次；收集、壳构造、回填及分析就绪判断读取同一代。 */
    private val contentSnapshots = IdentityHashMap<CfirContentSource.Published, CfirPublishedContentSnapshot>()

    private fun snapshot(source: CfirContentSource.Published): CfirPublishedContentSnapshot =
        contentSnapshots.getOrPut(source) { source.snapshot() }

    internal fun body(owner: CfirFunctionBodyOwner): CfirBlock? = when (val source = owner.contentSource) {
        CfirContentSource.Owned -> when (owner) {
            is CfirFunction -> owner.body
            is CfirEnumConstructor -> owner.body
            else -> error("A body owner must retain its actual function or enum-constructor kind")
        }
        is CfirContentSource.Published -> snapshot(source).body(owner)
    }

    internal fun initializer(owner: CfirVariable): CfirExpression? = when (val source = owner.contentSource) {
        CfirContentSource.Owned -> owner.initializer
        is CfirContentSource.Published -> snapshot(source).initializer(owner)
    }

    internal fun defaultValue(owner: CfirValueParameter): CfirExpression? = when (val source = owner.contentSource) {
        CfirContentSource.Owned -> owner.defaultValue
        is CfirContentSource.Published -> snapshot(source).defaultValue(owner)
    }

    internal fun annotations(owner: CfirDeclaration): List<CfirAnnotation> = when (val source = owner.contentSource) {
        CfirContentSource.Owned -> owner.annotations
        is CfirContentSource.Published -> snapshot(source).annotations(owner).values
    }

    init {
        symbolRebindings.forEach { (old, new) ->
            require(old !== new && old::class == new::class) { "Symbol rebinding must preserve its declaration kind" }
            symbols[old] = new
        }
        val targetDestinations = Collections.newSetFromMap(IdentityHashMap<CfirTarget<*>, Boolean>())
        targetRebindings.forEach { (old, new) ->
            require(old !== new && old::class == new::class) { "Target rebinding must preserve its control kind" }
            require(old.labelName == new.labelName && targetDestinations.add(new))
            require(new !in targetRebindings.keys) { "A target destination cannot reuse an input identity" }
            if (old is CfirFunctionTarget && new is CfirFunctionTarget) require(old.isLambda == new.isLambda)
            targets[old] = new
        }
    }

    internal fun copyGraphs(roots: List<CfirElement>): CfirOwnedGraphCopyResult {
        val collector = object : CfirVisitor<Unit, Unit>() {
            override fun visitElement(element: CfirElement, data: Unit) {
                check(element !is CfirLazyBlock && element !is CfirLazyExpression) {
                    "Owned copy requires materialized RAW or resolved nodes"
                }
                check(element !is CfirNamedReferenceWithCandidateBase || element is CfirNamedReferenceWithCandidateBaseImpl) {
                    "Candidate products must complete in resolve before owned graph copying"
                }
                if (element is CfirControlFlowGraphReference) return
                if (!owned.add(element)) return
                if (element is CfirDeclaration) {
                    val supplied = symbols[element.symbol]
                    if (supplied == null) symbols[element.symbol] = freshCfirOwnedSymbol(element.symbol)
                    else check(!supplied.isBound) { "An owned declaration requires an unbound fresh target symbol" }
                    collectAttributes(element.attributes) { it.accept(this, Unit) }
                }
                if (element is ResolvedImplicitTypeRef) element.typeRef.accept(this, Unit)
                if (element is CfirBuiltInDeclaration || element is CfirPrimitiveTypeDeclaration) {
                    val declaration = element as CfirClassLikeDeclaration
                    annotations(declaration).forEach { it.accept(this, Unit) }
                    declaration.typeParameters.forEach { it.accept(this, Unit) }
                    declaration.status.accept(this, Unit)
                    declaration.declarations.forEach { it.accept(this, Unit) }
                    declaration.superTypeRefs.forEach { it.accept(this, Unit) }
                } else visitor.collectChildren(element, this)
            }
        }
        roots.forEach { it.accept(collector, Unit) }
        check(owned.containsAll(nameRebindings.keys)) { "Name rebinding must refer to owned nodes" }
        val originalSymbols = Collections.newSetFromMap(IdentityHashMap<CfirBasedSymbol<*>, Boolean>())
        owned.filterIsInstance<CfirDeclaration>().forEach { originalSymbols.add(it.symbol) }
        val declaredDestinations = Collections.newSetFromMap(IdentityHashMap<CfirBasedSymbol<*>, Boolean>())
        originalSymbols.forEach { require(declaredDestinations.add(symbols.getValue(it))) {
            "Distinct owned declarations cannot share a fresh symbol"
        } }
        check(symbols.values.none { it in originalSymbols }) { "A rebinding destination cannot reuse an owned input symbol" }
        targets.forEach { (old, new) ->
            if (old.isBound && old.labeledElement in owned) {
                check(!new.isBound) { "An internal target requires a fresh unbound destination" }
                pendingTargets.add(old to new)
            } else {
                if (old is CfirFunctionTarget && new is CfirFunctionTarget && old.isBound && !new.isBound) {
                    val symbol = remapSymbol(old.labeledElement.symbol)
                    check(symbol in declaredDestinations) { "An unbound destination requires a declared owned function owner" }
                    pendingReboundFunctions.add(Triple(new, symbol, old.labeledElement))
                    return@forEach
                }
                check(old.isBound && new.isBound) { "Explicit external targets must declare a bound destination owner" }
                check(new.labeledElement !in owned && new.labeledElement !== old.labeledElement) {
                    "An external target destination cannot reuse an owned input or its old owner"
                }
                when {
                    old is CfirFunctionTarget && new is CfirFunctionTarget -> check(
                        new.labeledElement::class == old.labeledElement::class &&
                            new.labeledElement.symbol === remapSymbol(old.labeledElement.symbol),
                    ) { "External F target differs from its declared owner or symbol rebinding" }
                    old is CfirLoopTarget && new is CfirLoopTarget -> requireSameIterationKind(old.labeledElement, new.labeledElement)
                    else -> error("Target has no concrete owner rebinding contract")
                }
            }
        }
        // 第一遍的第二步分配节点壳，符号绑定仅指向新声明；壳从不向调用方发布。
        owned.toList().forEach { allocate(it) }
        // 第二遍回填拥有边和借用边，循环和前向引用均从同一 identity 表取值。
        owned.toList().forEach { it.accept(visitor, true) }
        // 手写语义节点与生成节点采用同一来源事实合同；借用 map 不扩大拥有域。
        owned.filterIsInstance<CfirExpression>().forEach { original ->
            val copied = copyOwned(original)
            copied.replaceMappedExpression(original.mappedExpression?.let { copyReferenceElement(it) })
            copied.replaceOverflowStrategy(original.overflowStrategy)
        }
        val result = roots.map { copyOwned(it) }
        pendingTargets.forEach { (old, new) -> bindTarget(old, new) }
        pendingReboundFunctions.forEach { (target, symbol, originalOwner) ->
            check(symbol.isBound) { "A rebound F requires its filled canonical declaration" }
            val declaration = symbol.cfir
            check(declaration is CfirFunctionBodyOwner) { "Rebound F symbol changed its owner domain" }
            check(declaration::class == originalOwner::class) { "Rebound F changed its concrete declaration kind" }
            target.bind(declaration)
            check(target.labeledElement.symbol === symbol)
        }
        pendingTargets.forEach { (old, new) ->
            check(new.isBound && new.labeledElement === copyOwned(old.labeledElement)) { "Internal control target escaped the copied domain" }
        }
        pendingAttributes.forEach { old ->
            val new = checkNotNull(attributeCopies[old])
            old.forEachOwnedCopyEntry { key, value ->
                // 初始化数据属于原 owner 的完成分析，不是语法拥有边；新图在正常
                // CFA 中重新计算，不能保留旧字段/构造器/CFG 的完成产品。
                if (key == CfirClassInitializationInfoKey::class.qualifiedName) return@forEachOwnedCopyEntry
                new.putOwnedCopyEntry(key, copyAttributeValue(key, value))
            }
        }
        check(elements.keys.containsAll(owned)) { "Owned copy left an unallocated node" }
        // 派生provider必须引用全部回填后的新注解，不能保留输入provider里的旧mutable annotation节点。
        owned.filterIsInstance<CfirDeclaration>().forEach { original ->
            val copiedDeclaration = copyOwned(original)
            val selectedSource = original.contentSource
            if (selectedSource is CfirContentSource.Published) {
                when (val selected = snapshot(selectedSource).annotations(original)) {
                    CfirPublishedAnnotations.Unselected -> {
                        copiedDeclaration.ownedGraphAnnotationsUnselected = true
                        copiedDeclaration.annotationInfo = null
                    }
                    is CfirPublishedAnnotations.Complete -> {
                        copiedDeclaration.annotationInfo = selected.annotationInfo?.frozenCopy()
                        copiedDeclaration.interopInfo = selected.interopInfo?.frozenCopy()
                    }
                }
            }
            when (val copied = copyOwned(original)) {
                is CfirCallableDeclaration -> copied.replaceDeprecationsProvider(if (copied.ownedGraphAnnotationsUnselected == true ||
                    selectedSource === CfirContentSource.Owned && original is CfirCallableDeclaration && original.deprecationsProvider === UnresolvedDeprecationProvider)
                    UnresolvedDeprecationProvider else buildDeprecationsProvider(copied.annotations))
                is CfirClassLikeDeclaration -> copied.replaceDeprecationsProvider(if (copied.ownedGraphAnnotationsUnselected == true ||
                    selectedSource === CfirContentSource.Owned && original is CfirClassLikeDeclaration && original.deprecationsProvider === UnresolvedDeprecationProvider)
                    UnresolvedDeprecationProvider else buildDeprecationsProvider(copied.annotations))
                else -> Unit
            }
        }
        owned.filterIsInstance<CfirFunctionBodyOwner>().forEach {
            check(copyOwned(it).contentSource === CfirContentSource.Owned) { "An owned F copy cannot retain its Library content source" }
        }
        owned.filterIsInstance<CfirVariable>().forEach {
            check(copyOwned(it).contentSource === CfirContentSource.Owned) { "An owned variable copy cannot retain its Library content source" }
        }
        check(renamed.containsAll(nameRebindings.keys)) { "Name rebinding has no concrete name slot" }
        owned.filterIsInstance<CfirDeclaration>().forEach { original ->
            val copied = copyOwned(original)
            check(copied.symbol === remapSymbol(original.symbol) && copied.symbol.isBound && copied.symbol.cfir === copied) {
                "Owned declaration and symbol binding diverged"
            }
        }
        owned.filterIsInstance<CfirControlFlowGraphOwner>().forEach { original ->
            check(copyOwned(original).controlFlowGraphReference == null) { "CFG is a separately rebuilt analysis product" }
        }
        val analysisOwners = owned.filterIsInstance<CfirControlFlowGraphOwner>().map { copyOwned(it) }
        val resolvedCandidates = owned.filterIsInstance<CfirControlFlowGraphOwner>().filter {
            it.controlFlowGraphReference != null || (it is CfirDeclaration && it.resolvePhase >= CfirResolvePhase.BODY_RESOLVE)
        }
        owned.filterIsInstance<CfirDeclaration>().forEach { original -> copyOwned(original).ownedGraphAnalysisReadiness = null }
        val resolvedAnalysisOwners = resolvedCandidates.filter { original ->
            original.controlFlowGraphReference != null || when (original) {
                is CfirFunctionBodyOwner -> body(original) != null
                is CfirValueParameter -> defaultValue(original) != null
                is CfirVariable -> initializer(original) != null
                else -> false
            }
        }.map { copyOwned(it) }
        resolvedCandidates.filter { copyOwned(it) !in resolvedAnalysisOwners }.filterIsInstance<CfirDeclaration>().forEach {
            copyOwned(it).ownedGraphAnalysisState = CfirOwnedGraphAnalysisState.NO_BODY_ANALYSIS
        }
        val resolvedDeclarations = resolvedAnalysisOwners.filterIsInstance<CfirDeclaration>() +
            owned.filterIsInstance<CfirCodeFragment>().filter { it.resolvePhase >= CfirResolvePhase.BODY_RESOLVE }.map { copyOwned(it) }
        resolvedDeclarations.forEach {
            it.ownedGraphAnalysisState = CfirOwnedGraphAnalysisState.PENDING
        }
        return CfirOwnedGraphCopyResult(result, this, analysisOwners, resolvedAnalysisOwners, resolvedDeclarations)
    }

    private fun collectAttributes(attributes: CfirDeclarationAttributes, visit: (CfirElement) -> Unit) {
        attributes.forEachOwnedCopyEntry { key, value ->
            when (key) {
                ParameterDefaultValueMetadataKey::class.qualifiedName -> (value as CfirParameterDefaultValueMetadata).function?.let(visit)
                EvaluatedValue::class.qualifiedName -> when (value) {
                    is CfirEvaluatorResult.Evaluated -> visit(value.result)
                    CfirEvaluatorResult.NotEvaluated -> Unit
                    else -> attributeRemapper.collectOwnedChildren(key, value, visit)
                }
                TypeConstraintDiagnosticDataKey::class.qualifiedName ->
                    (value as CfirTypeConstraintDiagnosticData).typeConstraints.forEach { it.boundTypeRefs.forEach(visit) }
                CorrespondingFieldKey::class.qualifiedName, CorrespondingValueParameterKey::class.qualifiedName,
                DefaultParameterHelperInfoKey::class.qualifiedName, FunctionBodyDiagnosticDataKey::class.qualifiedName,
                CfirInteropInfoKey::class.qualifiedName,
                CfirDeclarationAnnotationInfoKey::class.qualifiedName,
                CfirOwnedDeclarationAnnotationInfoKey::class.qualifiedName,
                CfirSerializedAnnotationHeaderRegisteredKey::class.qualifiedName,
                OwnedGraphAnnotationsUnselectedKey::class.qualifiedName,
                GenericNodePresentKey::class.qualifiedName, CjmpSourceNominalDefaultKey::class.qualifiedName,
                ImplicitDefaultUpperBoundKey::class.qualifiedName,
                OriginalDoNotExportKey::class.qualifiedName,
                MemberParameterKey::class.qualifiedName, CfirDeclarationExportFactsKey::class.qualifiedName,
                CfirClassInitializationInfoKey::class.qualifiedName,
                DefaultAssignmentSyntaxFactsKey::class.qualifiedName,
                OwnedGraphAnalysisStateKey::class.qualifiedName,
                OwnedGraphAnalysisReadinessKey::class.qualifiedName -> Unit
                else -> attributeRemapper.collectOwnedChildren(key, value, visit)
            }
        }
    }

    private fun copyAttributeValue(key: String, value: Any): Any = when (key) {
        ParameterDefaultValueMetadataKey::class.qualifiedName -> (value as CfirParameterDefaultValueMetadata).let {
            it.copy(function = it.function?.let { function -> copyOwned(function) })
        }
        EvaluatedValue::class.qualifiedName -> when (value) {
            is CfirEvaluatorResult.Evaluated -> CfirEvaluatorResult.Evaluated(copyOwned(value.result))
            CfirEvaluatorResult.NotEvaluated -> value
            else -> attributeRemapper.copyAttribute(key, value, this)
        }
        CorrespondingFieldKey::class.qualifiedName, CorrespondingValueParameterKey::class.qualifiedName -> remapDeclaration(value as CfirDeclaration)
        OwnedGraphAnalysisStateKey::class.qualifiedName -> value as CfirOwnedGraphAnalysisState
        OwnedGraphAnalysisReadinessKey::class.qualifiedName -> value as CfirOwnedGraphAnalysisReadiness
        DefaultParameterHelperInfoKey::class.qualifiedName -> (value as CfirDefaultParameterHelperInfo).let {
            it.copy(owner = remapElement(it.owner))
        }
        FunctionBodyDiagnosticDataKey::class.qualifiedName -> (value as CfirFunctionBodyDiagnosticData).let {
            it.copy(valueParameterLists = it.valueParameterLists.map { entry -> entry.copy() })
        }
        CfirInteropInfoKey::class.qualifiedName -> (value as CfirInteropInfo).frozenCopy()
        CfirDeclarationAnnotationInfoKey::class.qualifiedName,
        CfirOwnedDeclarationAnnotationInfoKey::class.qualifiedName -> (value as CfirDeclarationAnnotationInfo).frozenCopy()
        // 真实 null 没有 value 条目，但登记资格与未选择标记仍必须跨每次复制保留。
        CfirSerializedAnnotationHeaderRegisteredKey::class.qualifiedName,
        OwnedGraphAnnotationsUnselectedKey::class.qualifiedName -> value as Boolean
        TypeConstraintDiagnosticDataKey::class.qualifiedName -> (value as CfirTypeConstraintDiagnosticData).let {
            it.copy(typeConstraints = it.typeConstraints.map { entry ->
                entry.copy(boundTypeRefs = entry.boundTypeRefs.map { ref -> copyOwned(ref) })
            })
        }
        GenericNodePresentKey::class.qualifiedName, CjmpSourceNominalDefaultKey::class.qualifiedName,
        ImplicitDefaultUpperBoundKey::class.qualifiedName,
        OriginalDoNotExportKey::class.qualifiedName -> value as Boolean
        MemberParameterKey::class.qualifiedName -> value as Boolean
        CfirDeclarationExportFactsKey::class.qualifiedName -> (value as CfirDeclarationExportFacts).copy()
        DefaultAssignmentSyntaxFactsKey::class.qualifiedName -> (value as CfirDefaultAssignmentSyntaxFacts).copy()
        else -> attributeRemapper.copyAttribute(key, value, this)
    }

    internal fun copyAttributes(old: CfirDeclarationAttributes): CfirDeclarationAttributes =
        attributeCopies.getOrPut(old) {
            pendingAttributes.add(old)
            CfirDeclarationAttributes.emptyForOwnedCopy()
        }

    /** 已登记的域内符号必须用 fresh 身份；明确域外符号保持 identity。 */
    @Suppress("UNCHECKED_CAST")
    fun <S : CfirBasedSymbol<*>> remapSymbol(symbol: S): S =
        (symbols[symbol] ?: symbol) as S

    /** 声明引用不取得所有权，例如 resolved argument mapping 中的形参。 */
    @Suppress("UNCHECKED_CAST")
    fun <D : CfirDeclaration> remapDeclaration(declaration: D): D {
        if (owned.contains(declaration)) return copyOwned(declaration)
        if (!symbols.containsKey(declaration.symbol)) return declaration
        val copied = remapSymbol(declaration.symbol).cfir
        check(copied::class == declaration::class) { "Declaration rebinding changed the concrete node kind" }
        return copied as D
    }

    /** 扩展 metadata 的拥有边只能引用第一遍已登记的节点。 */
    @Suppress("UNCHECKED_CAST")
    fun <E : CfirElement> copyOwned(element: E): E {
        check(element in owned) { "Attribute ownership must be declared during collection" }
        return checkNotNull(elements[element]) { "Owned node shell was not allocated" } as E
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <E : CfirElement> allocate(element: E): E {
        elements[element]?.let { return it as E }
        check(element in owned)
        check(allocating.add(element)) { "Immutable wrapper allocation cycle" }
        val result = element.accept(visitor, false)
        check(result !== element || element === CfirEmptyArgumentList || element === CfirEmptyAnnotationArgumentMapping) {
            "Mutable owned node identity was shared"
        }
        check(result::class == element::class) { "Owned copy changed the concrete node kind" }
        elements[element] = result
        allocating.remove(element)
        return result as E
    }

    /** 非拥有节点回边（包括域外默认值）仅在本域内重定位。 */
    @Suppress("UNCHECKED_CAST")
    fun <E : CfirElement> remapElement(element: E): E =
        if (element in owned) copyOwned(element) else element

    internal fun type(type: ConeCangJieType): ConeCangJieType = typeRemapper.remapType(type, this)

    /** 名称替换按节点身份预先声明；immutable 名称在壳分配时写入，不破坏共享引用。 */
    internal fun name(element: CfirElement, original: Name): Name {
        val replacement = nameRebindings[element] ?: return original
        renamed.add(element)
        return replacement
    }

    internal fun forEachCopied(action: (CfirElement, CfirElement) -> Unit) {
        owned.forEach { action(it, copyOwned(it)) }
    }

    internal fun simpleType(type: ConeSimpleCangJieType): ConeSimpleCangJieType {
        val result = type(type)
        check(result is ConeSimpleCangJieType) { "Type remapper violated a Simple type slot" }
        return result
    }

    internal fun copyConstant(value: CfirConstantValue): CfirConstantValue = constants.copy(value)

    /** 借用回边只重绑本次已经认领的节点；域外节点保留身份，不由引用者认领。 */
    internal fun <E : CfirElement> copyReferenceElement(value: E): E =
        if (value in owned) copyOwned(value) else value

    @Suppress("UNCHECKED_CAST")
    internal fun <E : CfirTargetElement> target(old: CfirTarget<E>): CfirTarget<E> {
        targets[old]?.let { return it as CfirTarget<E> }
        if (old.isBound && old.labeledElement !in owned) {
            if (old is CfirFunctionTarget) check(remapSymbol(old.labeledElement.symbol) === old.labeledElement.symbol) {
                "A rebound external function owner requires an explicit Return target rebinding"
            }
            return old
        }
        val new: CfirTarget<*> = when (old) {
            is CfirFunctionTarget -> CfirFunctionTarget(old.labelName, old.isLambda)
            is CfirLoopTarget -> CfirLoopTarget(old.labelName)
            else -> error("Unknown CFIR target kind")
        }
        targets[old] = new
        if (old.isBound) pendingTargets.add(old to new)
        return new as CfirTarget<E>
    }

    private fun bindTarget(old: CfirTarget<*>, new: CfirTarget<*>) {
        when {
            old is CfirFunctionTarget && new is CfirFunctionTarget -> {
                check(old.isLambda == new.isLambda)
                val owner = copyOwned(old.labeledElement)
                check(owner::class == old.labeledElement::class) { "F copy changed its concrete declaration" }
                new.bind(owner)
            }
            old is CfirLoopTarget && new is CfirLoopTarget -> {
                val owner = copyOwned(old.labeledElement)
                requireSameIterationKind(old.labeledElement, owner)
                new.bind(owner)
            }
            else -> error("Target kind changed during owned copy")
        }
    }

    /** 真正的循环种类由原 concrete 和语义阶段保存，统一 target 不创建第二套 kind 协议。 */
    private fun requireSameIterationKind(original: CfirIterationOwner, destination: CfirIterationOwner) {
        check(original::class == destination::class) { "An iteration target must retain its actual owner kind" }
        when (original) {
            is CfirLoopExpression -> check((destination as CfirLoopExpression).isDoWhile == original.isDoWhile)
            is CfirForInExpression -> check((destination as CfirForInExpression).iterationKind == original.iterationKind)
            else -> error("Iteration owner has no completed copy contract")
        }
    }
}

/** 一组 roots 的同域复制结果；查询不触发新的复制或符号分配。 */
class CfirOwnedGraphCopyResult internal constructor(
    val roots: List<CfirElement>,
    private val context: CfirOwnedGraphCopyContext,
    /** RAW owner 由正常 BODY 流水线构建自己的 CFG，复制完成不等于分析完成。 */
    val analysisOwners: List<CfirControlFlowGraphOwner>,
    /** 保留了解析事实的 owner 必须由 resolve 完整重建分析产品，不能再次请求 BODY 冒充完成。 */
    val resolvedOwnersRequiringAnalysisRebuild: List<CfirControlFlowGraphOwner>,
    /** code fragment 的分析图由完成产品承载；它与 Kotlin 一样没有 CFG owner 字段。 */
    val resolvedDeclarationsRequiringAnalysisRebuild: List<CfirDeclaration>,
) {
    fun <E : CfirElement> getCopied(original: E): E = context.copyOwned(original)
    fun <S : CfirBasedSymbol<*>> getCopiedSymbol(original: S): S = context.remapSymbol(original)
    /** 外部 metadata owner 用此表登记派生槽；枚举不扩大复制域。 */
    fun forEachCopied(action: (CfirElement, CfirElement) -> Unit): Unit = context.forEachCopied(action)

    /** 已解析复制的正式消费者必须检查重建结果；此检查不推进 phase 或修改类型。 */
    fun requireAnalysisReady() {
        resolvedDeclarationsRequiringAnalysisRebuild.forEach {
            check(it.ownedGraphAnalysisState == CfirOwnedGraphAnalysisState.COMPLETE) {
                "Owned graph analysis was not rebuilt: ${it.symbol}"
            }
            it.requireOwnedGraphAnalysisReady()
        }
    }
}

/** S1 将 assignment、自有泛型/约束与参数类型作为真实 roots，共用一份复制域。 */
fun deepCopyOwnedGraphs(
    roots: List<CfirElement>,
    typeRemapper: CfirTypeRefRemapper,
    attributeRemapper: CfirOwnedAttributeRemapper,
    symbolRebindings: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>> = emptyMap(),
    targetRebindings: Map<CfirTarget<*>, CfirTarget<*>> = emptyMap(),
    nameRebindings: Map<CfirElement, Name> = emptyMap(),
): CfirOwnedGraphCopyResult = CfirOwnedGraphCopyContext(
    typeRemapper, attributeRemapper, symbolRebindings, targetRebindings, IdentityHashMap(nameRebindings),
).copyGraphs(roots.toList())

/** 单根入口调用 grouped，RAW 和已解析 CFIR 具有同一所有权合同。 */
fun CfirElement.deepCopyOwnedGraph(
    typeRemapper: CfirTypeRefRemapper,
    attributeRemapper: CfirOwnedAttributeRemapper,
    symbolRebindings: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>> = emptyMap(),
    targetRebindings: Map<CfirTarget<*>, CfirTarget<*>> = emptyMap(),
    nameRebindings: Map<CfirElement, Name> = emptyMap(),
): CfirElement = deepCopyOwnedGraphs(
    listOf(this), typeRemapper, attributeRemapper, symbolRebindings, targetRebindings, nameRebindings,
).getCopied(this)

/** 保留根静态类型；复制器逐节点保持实际 concrete kind。 */
inline fun <reified T : CfirElement> T.deepCopyOwnedGraphAs(
    typeRemapper: CfirTypeRefRemapper,
    attributeRemapper: CfirOwnedAttributeRemapper,
    symbolRebindings: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>> = emptyMap(),
    targetRebindings: Map<CfirTarget<*>, CfirTarget<*>> = emptyMap(),
    nameRebindings: Map<CfirElement, Name> = emptyMap(),
): T = deepCopyOwnedGraph(typeRemapper, attributeRemapper, symbolRebindings, targetRebindings, nameRebindings) as T
