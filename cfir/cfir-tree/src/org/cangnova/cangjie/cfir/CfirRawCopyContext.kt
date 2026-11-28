package org.cangnova.cangjie.cfir

import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.annotations.CangjieAnnotationTarget
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationFailure
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.CjSourceFile

/**
 * RAW 复制的类型化 metadata 分类；新增分类必须同时提供扫描和复制策略。
 *
 * 它不是节点白名单。节点覆盖由 CfirTree 的所有 concrete implementation 决定。
 */
enum class CfirRawCopyMetadataKind {
    SOURCE, MODULE_DATA, SOURCE_FILE, LINE_MAPPING, NAME, FQ_NAME, CLASS_ID,
    VISIBILITY, MODALITY, ORIGIN, STATUS, ATTRIBUTES, DEPRECATIONS, SCOPE_PROVIDER,
    CONE_TYPE, DIAGNOSTIC, TARGET, PATTERN_OWNER, ANNOTATION_MAPPING,
    ANNOTATION_VIEW, ANNOTATION_ARGUMENT_MAP, CONDITIONAL_COMPILATION_FAILURE,
    LITERAL_VALUE, MATCH_EXHAUSTIVENESS, PROPERTY_BODY_STATE, TYPE_PATTERN_KIND,
    ASSIGNMENT_MISMATCH, ANNOTATION_IDENTITY, CONTENT_SOURCE, PATTERN_CONSTANT,
}

/**
 * copier 与 metadata policy 共享的唯一复制事务。
 *
 * 元素 identity cache、symbol map、target map 和 deferred references 均由本对象管理。
 * context 的 metadata hooks 不得另建 copier 或另一套 source2clone。
 */
class CfirRawCopyState(
    borrowedElements: Collection<CfirElement> = emptyList(),
    borrowedSymbols: Collection<CfirBasedSymbol<*>> = emptyList(),
) {
    private enum class Phase { NEW, SCANNING, COPYING, FINISHED, FAILED }
    private var phase = Phase.NEW
    private val borrowedElements = identitySet<CfirElement>().apply { addAll(borrowedElements) }
    private val borrowedSymbols = identitySet<CfirBasedSymbol<*>>().apply { addAll(borrowedSymbols) }
    private val ownedElements = identitySet<CfirElement>()
    private val source2clone = IdentityHashMap<CfirElement, CfirElement>()
    private val symbols = IdentityHashMap<CfirBasedSymbol<*>, CfirBasedSymbol<*>>()
    private val targets = IdentityHashMap<CfirTarget<*>, CfirTarget<*>>()
    private val constructing = identitySet<CfirElement>()
    private val sharedImmutable = identitySet<CfirElement>()
    private val deferredReferences = ArrayDeque<() -> Unit>()
    private var copyOperation: ((CfirElement) -> CfirElement)? = null

    /** 每个 context 只允许安装一个 copier。 */
    fun attachCopier(copy: (CfirElement) -> CfirElement) {
        check(phase == Phase.NEW && copyOperation == null) { "RAW copy state already has a copier" }
        copyOperation = copy
    }

    fun beginScan() {
        check(phase == Phase.NEW) { "A RAW copy state is a single-root transaction" }
        phase = Phase.SCANNING
    }

    /** 返回 false 仅表示同一 owned 元素已扫描，不表示跳过未知节点。 */
    fun registerOwnedElement(source: CfirElement): Boolean {
        check(phase == Phase.SCANNING)
        check(source !in borrowedElements) { "Read-only scope owner was reached through an owning edge" }
        return ownedElements.add(source)
    }

    /** metadata scanner 中对非 owning declaration reference 使用此入口。 */
    fun isOwned(source: CfirElement): Boolean = source in ownedElements

    fun isBorrowedElement(source: CfirElement): Boolean = source in borrowedElements

    fun isBorrowedSymbol(source: CfirBasedSymbol<*>): Boolean = source in borrowedSymbols

    /** 由明确借用的 SDK 形参导出的 defaultValue 句柄；不遍历其外部声明图。 */
    fun registerBorrowedElementReference(source: CfirElement) {
        check(phase == Phase.SCANNING && source !in ownedElements)
        borrowedElements.add(source)
    }

    /** first pass 使用真实 concrete ctor 分配 symbol 后登记；不重新绑定 source symbol。 */
    fun registerDeclaredSymbol(
        owner: CfirDeclaration,
        source: CfirBasedSymbol<*>,
        target: CfirBasedSymbol<*>,
    ) {
        check(phase == Phase.SCANNING && owner in ownedElements)
        check(source !in borrowedSymbols) { "Borrowed symbol cannot be declared by a clone" }
        check(source.isBound && source.cfir === owner) { "Source symbol has a different declaration owner" }
        check(source !== target && source.javaClass == target.javaClass && !target.isBound) {
            "Declared symbols require fresh, unbound symbols of the same concrete class"
        }
        check(symbols.put(source, target) == null) { "Declared symbol was allocated twice" }
    }

    @Suppress("UNCHECKED_CAST")
    fun <S : CfirBasedSymbol<*>> declaredSymbol(source: S): S =
        (symbols[source] ?: error("Owned declared symbol has no first-pass allocation")) as S

    @Suppress("UNCHECKED_CAST")
    fun <S : CfirBasedSymbol<*>> referencedSymbol(source: S): S {
        symbols[source]?.let { return it as S }
        check(source in borrowedSymbols) {
            "A referenced owned/unclassified symbol has no allocation"
        }
        return source
    }

    fun beginCopy() {
        check(phase == Phase.SCANNING)
        phase = Phase.COPYING
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : CfirElement> lookup(source: E): E? = source2clone[source] as E?

    /** owning child cycle 必须拒绝；ancestor reference 应由 typed hook 延迟重定位。 */
    fun enterConstruction(source: CfirElement) {
        check(phase == Phase.COPYING && source in ownedElements) { "Element was not collected in first pass" }
        check(constructing.add(source)) { "Owning cycle encountered; a reference policy is missing" }
    }

    fun leaveConstruction(source: CfirElement) {
        check(constructing.remove(source))
    }

    /** 仅显式、已验证的无状态 singleton policy 可以登记共享。 */
    fun allowSharedImmutable(source: CfirElement) {
        check(phase == Phase.COPYING && source in ownedElements)
        sharedImmutable.add(source)
    }

    fun remember(source: CfirElement, target: CfirElement) {
        check(phase == Phase.COPYING && source in ownedElements)
        check(source.javaClass == target.javaClass) { "RAW copy changed the concrete implementation" }
        check(source !== target || source in sharedImmutable) { "A mutable RAW node was shallow-copied" }
        check(source2clone.put(source, target) == null) { "Two clones were created for one source element" }
    }

    /** metadata-owned 子树与 generated children 一律经过同一个 copier/cache。 */
    @Suppress("UNCHECKED_CAST")
    fun <E : CfirElement> copyOwned(source: E): E {
        check(phase == Phase.COPYING && source in ownedElements) { "Metadata omitted an owning edge during first pass" }
        return checkNotNull(copyOperation)(source) as E
    }

    @Suppress("UNCHECKED_CAST")
    fun <E : CfirElement> referencedElement(source: E): E {
        source2clone[source]?.let { return it as E }
        check(source !in ownedElements) { "Owned ancestor reference must be deferred until its clone is recorded" }
        check(source in borrowedElements || source is CfirDeclaration && source.symbol in borrowedSymbols) { "Unclassified external element reference" }
        return source
    }

    /** target hook 创建新的 wrapper，binding 在 finish 时指向 clone 或明确 borrowed owner。 */
    @Suppress("UNCHECKED_CAST")
    fun <E : CfirTargetElement> target(
        source: CfirTarget<E>,
        allocate: () -> CfirTarget<E>,
    ): CfirTarget<E> {
        check(phase == Phase.COPYING)
        targets[source]?.let { return it as CfirTarget<E> }
        val target = allocate()
        check(target !== source && target.javaClass == source.javaClass && !target.isBound)
        targets[source] = target
        if (source.isBound) {
            deferReference {
                val element = source.labeledElement
                target.bind(referencedElement(element))
            }
        }
        return target
    }

    fun deferReference(install: () -> Unit) {
        check(phase == Phase.COPYING)
        deferredReferences.addLast(install)
    }

    /** 全部节点记录后才安装 owner/target 等回边。 */
    fun finishReferences() {
        check(phase == Phase.COPYING && constructing.isEmpty())
        while (deferredReferences.isNotEmpty()) deferredReferences.removeFirst().invoke()
        check(source2clone.keys.containsAll(ownedElements)) { "An owned metadata subtree was never copied" }
        for ((source, target) in symbols) {
            val owner = source2clone[source.cfir]
            check(target.isBound && target.cfir === owner) { "Fresh symbol is not bound to its cloned declaration" }
        }
        phase = Phase.FINISHED
    }

    fun fail() { phase = Phase.FAILED }

    fun elementMapping(): Map<CfirElement, CfirElement> {
        check(phase == Phase.FINISHED) { "Only a committed RAW graph exposes its element mapping" }
        return Collections.unmodifiableMap(IdentityHashMap(source2clone))
    }

    fun symbolMapping(): Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>> {
        check(phase == Phase.FINISHED) { "Only a committed RAW graph exposes its symbol mapping" }
        return Collections.unmodifiableMap(IdentityHashMap(symbols))
    }

    private companion object {
        fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
    }
}

/**
 * 必须由 main 实现的 metadata 策略契约。
 *
 * collectOwnedMetadata 必须按 kind/value 的真实策略遍历 owned AST；不能反射猜字段，
 * 不能把 reference 当 child 扩大到外部声明。copy hooks 必须使用 state.copyOwned、
 * state.referencedSymbol 和 state.deferReference；不得创建另一套缓存。
 */
interface CfirRawCopyContext {
    val state: CfirRawCopyState
    fun validateRawNode(source: CfirElement)
    fun validateCopiedNode(source: CfirElement, target: CfirElement)
    fun registerDeclaredSymbol(owner: CfirDeclaration, symbol: CfirBasedSymbol<*>)
    fun resolvePhase(source: CfirElementWithResolveState): CfirResolvePhase
    fun <V : Any> collectOwnedMetadata(kind: CfirRawCopyMetadataKind, value: V, visit: (CfirElement) -> Unit)

    fun source(value: CjSourceElement): CjSourceElement
    fun moduleData(value: CfirModuleData): CfirModuleData
    fun sourceFile(value: CjSourceFile): CjSourceFile
    fun lineMapping(value: CjSourceFileLinesMapping): CjSourceFileLinesMapping
    fun name(value: Name): Name
    fun fqName(value: FqName): FqName
    fun classId(value: ClassId): ClassId
    fun lookupTag(value: ConeClassLikeLookupTag): ConeClassLikeLookupTag
    fun <V : Any> borrowedHandle(value: V): V
    fun visibility(value: Visibility): Visibility
    fun modality(value: Modality): Modality
    fun origin(value: CfirDeclarationOrigin): CfirDeclarationOrigin
    fun status(value: CfirDeclarationStatus): CfirDeclarationStatus
    fun attributes(value: CfirDeclarationAttributes): CfirDeclarationAttributes
    fun deprecationsProvider(value: DeprecationsProvider): DeprecationsProvider
    fun scopeProvider(value: CfirScopeProvider): CfirScopeProvider
    fun <T : ConeCangJieType> type(value: T): T
    fun diagnostic(value: ConeDiagnostic): ConeDiagnostic
    fun <E : CfirTargetElement> target(value: CfirTarget<E>): CfirTarget<E>
    fun patternOwner(value: CfirPatternTreeOwner): CfirPatternTreeOwner
    fun annotationMapping(value: CfirAnnotationArgumentMapping): CfirAnnotationArgumentMapping
    fun annotationView(value: CfirAnnotationArgumentView): CfirAnnotationArgumentView
    fun annotationArgumentMap(value: Map<Name, CfirExpression>): Map<Name, CfirExpression>
    fun conditionalCompilationFailure(value: CfirConditionalCompilationFailure): CfirConditionalCompilationFailure
    fun literalValue(value: Any): Any
    fun matchExhaustiveness(value: CfirMatchExhaustivenessStatus): CfirMatchExhaustivenessStatus
    fun propertyBodyState(value: CfirPropertyBodyResolveState): CfirPropertyBodyResolveState
    fun typePatternKind(value: CfirTypePatternMatchingKind): CfirTypePatternMatchingKind
    fun assignmentMismatch(value: CfirAssignmentTypeMismatchOutcome): CfirAssignmentTypeMismatchOutcome
    fun annotationIdentity(value: CangjieAnnotationIdentity): CangjieAnnotationIdentity
    fun contentSource(value: CfirContentSource): CfirContentSource.Owned
    fun patternConstant(value: CfirConstantValue): CfirConstantValue

    /** 外置 Element hook 只构造 target；copier 负责 remember，不能在 hook 中重复登记。 */
    fun resolvedImplicitTypeRef(value: org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef): org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
    fun emptyArgumentList(value: CfirEmptyArgumentList): CfirEmptyArgumentList
    fun resolvedArgumentList(value: CfirResolvedArgumentList): CfirResolvedArgumentList
}
