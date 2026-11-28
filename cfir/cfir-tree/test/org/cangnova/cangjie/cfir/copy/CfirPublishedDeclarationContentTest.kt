@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.copy

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.common.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirValueParameterImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationArgumentMappingImpl
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.annotations.CangjieAnnotationTarget
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 验证 R26 的声明内容发布合同，不把内存图 fixture 当作 cjc 语言正例。
 * 沿用 CfirOwnedGraphCopyTest 的正式 builder、类型 remapper 和分组复制工具；
 * 固定 K 的 declaration parent/typed symbol 与 N 的参数、注解保存槽是身份依据。
 */
class CfirPublishedDeclarationContentTest {
    private val type = ConePrimitiveType(PrimitiveTypeKind.INT64)
    private val typeRemapper = CfirTypeRefRemapper { original, _ -> original }
    private val attributes = object : CfirOwnedAttributeRemapper {
        override fun collectOwnedChildren(key: String, value: Any, visit: (CfirElement) -> Unit) {
            error("Test has no extension attribute: $key")
        }

        override fun copyAttribute(key: String, value: Any, context: CfirOwnedGraphCopyContext): Any =
            error("Test has no extension attribute: $key")
    }
    private val module = CfirSourceModuleData(
        name = Name.identifier("published-declaration-content-test"),
        dependencies = emptyList(),
        refinementDependencies = emptyList(),
        targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
        platform = CfirPlatform.DEFAULT,
    )

    @Test
    fun gettersObserveSnapshotsPassivelyAndDoNotInferCompletionFromEmptyAnnotations() {
        val source = PublishedSource()
        val owner = function("owner", source)
        val parameter = parameter("parameter", owner.symbol, source)
        val pending = source.current
        assertEquals(0, source.snapshotReads, "Constructing a header must not request content")

        assertSame(CfirPublishedAnnotations.Unselected, pending.annotations(parameter))
        assertNull(parameter.defaultValue)
        assertTrue(parameter.annotations.isEmpty())
        assertTrue(owner.annotations.isEmpty())
        assertEquals(3, source.snapshotReads, "Each ordinary getter reads only its passive snapshot")
        assertEquals(0, pending.bodyReads)
        assertEquals(0, pending.initializerReads)

        val completeEmpty = CfirPublishedAnnotations.Complete(emptyList())
        val completed = CompletedSnapshot(annotationViews = mapOf(parameter to completeEmpty, owner to completeEmpty))
        source.current = completed
        assertNull(parameter.defaultValue)
        assertTrue(parameter.annotations.isEmpty())
        assertTrue(owner.annotations.isEmpty())
        assertEquals(6, source.snapshotReads)
        assertSame(completeEmpty, completed.annotations(parameter))
        assertNotSame(CfirPublishedAnnotations.Unselected, completed.annotations(parameter))
        assertSame(CfirPublishedAnnotations.Unselected, pending.annotations(parameter))
        assertEquals(0, completed.bodyReads)
        assertEquals(0, completed.initializerReads)
    }

    @Test
    fun completedAnnotationListCopiesInputAndRejectsIteratorAndSubListMutation() {
        val source = PublishedSource()
        val owner = function("owner", source)
        val parameter = parameter("parameter", owner.symbol, source)
        val value = literal("1")
        val annotation = annotation(owner.symbol, value)
        val input = mutableListOf<CfirAnnotation>(annotation, annotation)
        val completed = CfirPublishedAnnotations.Complete(input)
        source.current = CompletedSnapshot(annotationViews = mapOf(parameter to completed))
        input.clear()

        val observed = parameter.annotations
        assertEquals(2, observed.size)
        assertSame(annotation, observed[0])
        assertSame(annotation, observed[1])
        assertThrows(UnsupportedOperationException::class.java) {
            val iterator = observed.iterator() as MutableIterator<CfirAnnotation>
            iterator.next()
            iterator.remove()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (observed.subList(0, 1) as MutableList<CfirAnnotation>).clear()
        }
        assertEquals(listOf(annotation, annotation), observed)
        assertSame(completed, source.current.annotations(parameter))
        assertSame(value, annotation.argumentList.arguments.single())
    }

    @Test
    fun replacingCompletedGenerationRetainsOldDefaultAndAnnotationIdentities() {
        val source = PublishedSource()
        val owner = function("owner", source)
        val parameter = parameter("parameter", owner.symbol, source)
        val oldDefault = literal("1")
        val oldAnnotation = annotation(owner.symbol, oldDefault)
        val oldAnnotations = CfirPublishedAnnotations.Complete(listOf(oldAnnotation))
        val oldSnapshot = CompletedSnapshot(mapOf(parameter to oldDefault), mapOf(parameter to oldAnnotations))
        source.current = oldSnapshot
        val oldList = parameter.annotations
        val oldNode = parameter.defaultValue

        val newDefault = literal("2")
        val newAnnotation = annotation(owner.symbol, newDefault)
        val newAnnotations = CfirPublishedAnnotations.Complete(listOf(newAnnotation))
        source.current = CompletedSnapshot(mapOf(parameter to newDefault), mapOf(parameter to newAnnotations))

        assertSame(newDefault, parameter.defaultValue)
        assertSame(newAnnotation, parameter.annotations.single())
        assertSame(oldDefault, oldNode)
        assertSame(oldDefault, oldSnapshot.defaultValue(parameter))
        assertSame(oldAnnotations, oldSnapshot.annotations(parameter))
        assertSame(oldAnnotation, oldList.single())
        assertSame(oldDefault, oldList.single().let { (it as CfirAnnotationCall).argumentList.arguments.single() })
        assertEquals("1", oldDefault.value)
        assertEquals("2", newDefault.value)

        source.current = CompletedSnapshot(annotationViews = mapOf(parameter to CfirPublishedAnnotations.Complete(emptyList())))
        assertNull(parameter.defaultValue)
        assertTrue(parameter.annotations.isEmpty())
        assertSame(oldAnnotation, oldList.single())
        assertSame(oldDefault, oldSnapshot.defaultValue(parameter))
        assertSame(newDefault, newAnnotation.argumentList.arguments.single())
    }

    @Test
    fun publishedParameterConstructorRejectsNonNullDefaultWithoutReadingSnapshot() {
        val source = PublishedSource()
        val owner = function("owner", source)
        assertThrows(IllegalArgumentException::class.java) {
            parameter("invalidDefault", owner.symbol, source, defaultValue = literal("1"))
        }
        assertEquals(0, source.snapshotReads)
    }

    @Test
    fun publishedDeclarationConstructorsRejectNonEmptyAnnotationsWithoutReadingSnapshot() {
        val source = PublishedSource()
        val owner = function("owner", source)
        val annotation = annotation(owner.symbol, literal("1"))
        assertThrows(IllegalArgumentException::class.java) {
            parameter("invalidAnnotations", owner.symbol, source, annotations = listOf(annotation))
        }
        assertThrows(IllegalArgumentException::class.java) {
            function("invalidFunctionAnnotations", source, annotations = listOf(annotation))
        }
        assertEquals(0, source.snapshotReads)
    }

    @Test
    fun publishedSettersRejectReplacementAndClearingWithoutChangingCompletedContent() {
        val source = PublishedSource()
        val owner = function("owner", source)
        val parameter = parameter("parameter", owner.symbol, source)
        val original = literal("1")
        val annotation = annotation(owner.symbol, original)
        val complete = CfirPublishedAnnotations.Complete(listOf(annotation))
        source.current = CompletedSnapshot(mapOf(parameter to original), mapOf(parameter to complete, owner to complete))
        val replacement = literal("2")
        val replacementAnnotation = annotation(owner.symbol, replacement)

        assertThrows(IllegalStateException::class.java) { parameter.replaceDefaultValue(replacement) }
        assertThrows(IllegalStateException::class.java) { parameter.replaceDefaultValue(null) }
        assertThrows(IllegalStateException::class.java) { parameter.replaceAnnotations(listOf(replacementAnnotation)) }
        assertThrows(IllegalStateException::class.java) { parameter.replaceAnnotations(emptyList()) }
        assertThrows(IllegalStateException::class.java) { owner.replaceAnnotations(listOf(replacementAnnotation)) }
        assertThrows(IllegalStateException::class.java) { owner.replaceAnnotations(emptyList()) }
        val implementation = parameter as CfirValueParameterImpl
        assertThrows(IllegalStateException::class.java) { implementation.defaultValue = replacement }
        assertThrows(IllegalStateException::class.java) { implementation.annotations = listOf(replacementAnnotation).toMutableOrEmpty() }
        assertEquals(0, source.snapshotReads, "Rejected setters must not read or publish a generation")
        assertSame(original, parameter.defaultValue)
        assertSame(annotation, parameter.annotations.single())
        assertSame(annotation, owner.annotations.single())
        assertSame(complete, source.current.annotations(parameter))
        assertSame(original, annotation.argumentList.arguments.single())
    }

    @Test
    fun ownedConstructorsAndSettersRetainOrdinaryMutableDeclarationContent() {
        val owner = function("ownedOwner")
        val first = literal("1")
        val firstAnnotation = annotation(owner.symbol, first)
        val parameter = parameter("parameter", owner.symbol, defaultValue = first, annotations = listOf(firstAnnotation))
        assertSame(CfirContentSource.Owned, parameter.contentSource)
        assertSame(first, parameter.defaultValue)
        assertSame(firstAnnotation, parameter.annotations.single())

        val second = literal("2")
        val secondAnnotation = annotation(owner.symbol, second)
        parameter.replaceDefaultValue(second)
        parameter.replaceAnnotations(listOf(secondAnnotation, secondAnnotation))
        owner.replaceAnnotations(listOf(secondAnnotation))
        assertSame(second, parameter.defaultValue)
        assertEquals(listOf(secondAnnotation, secondAnnotation), parameter.annotations)
        assertSame(secondAnnotation, owner.annotations.single())
        val implementation = parameter as CfirValueParameterImpl
        implementation.defaultValue = first
        implementation.annotations = listOf(firstAnnotation).toMutableOrEmpty()
        assertSame(first, parameter.defaultValue)
        assertSame(firstAnnotation, parameter.annotations.single())
        parameter.replaceDefaultValue(null)
        parameter.replaceAnnotations(emptyList())
        owner.replaceAnnotations(emptyList())
        assertNull(parameter.defaultValue)
        assertTrue(parameter.annotations.isEmpty())
        assertTrue(owner.annotations.isEmpty())
        assertSame(first, firstAnnotation.argumentList.arguments.single())
    }

    @Test
    fun groupedCopyCapturesOneGenerationForDefaultsAnnotationsAndInternalSymbols() {
        assertGroupedGenerationCopy(CfirResolvePhase.RAW_CFIR)
    }

    @Test
    fun resolvedGroupedCopyUsesCapturedGenerationForDefaultAnalysisReadiness() {
        assertGroupedGenerationCopy(CfirResolvePhase.BODY_RESOLVE)
    }

    /** 空视图不是完成资格；已完成空注解和真实 Deprecated 都必须区别于 Unselected。 */
    @Test
    fun copiedUnselectedAnnotationsRemainUnresolvedAndRejectNormalCompletionAndAnalysis() {
        val source = PublishedSource()
        val owner = function("annotationStates")
        val pending = parameter("pending", owner.symbol, source)
        val empty = parameter("completeEmpty", owner.symbol, source)
        val nonempty = parameter("completeNonempty", owner.symbol, source)
        owner.replaceValueParameters(listOf(pending, empty, nonempty))
        val id = ClassId.topLevel(FqName("std.core.Deprecated"))
        val annotationType = ConeClassLikeType(id.toLookupTag())
        val message = buildLiteralExpression { kind = CfirLiteralKind.STRING; value = "retired" }
        val deprecated = buildAnnotationCall {
            annotationClassId = id
            coneTypeOrNull = annotationType
            typeRef = buildResolvedTypeRef { coneType = annotationType }
            argumentList = buildArgumentList { arguments += message }
            calleeReference = buildResolvedNamedReference { name = id.shortClassName; resolvedSymbol = CfirClassSymbol(id) }
            containingDeclarationSymbol = owner.symbol
        }
        val completeEmpty = CfirPublishedAnnotations.Complete(emptyList())
        val completeNonempty = CfirPublishedAnnotations.Complete(listOf(deprecated))
        source.current = CompletedSnapshot(annotationViews = mapOf(
            pending to CfirPublishedAnnotations.Unselected,
            empty to completeEmpty,
            nonempty to completeNonempty,
        ))

        val result = deepCopyOwnedGraphs(listOf(pending, empty, nonempty, owner), typeRemapper, attributes)
        val copiedPending = result.getCopied(pending)
        val copiedEmpty = result.getCopied(empty)
        val copiedNonempty = result.getCopied(nonempty)
        assertEquals(1, source.snapshotReads)
        assertTrue(copiedPending.annotations.isEmpty())
        assertTrue(copiedEmpty.annotations.isEmpty())
        assertEquals(true, copiedPending.ownedGraphAnnotationsUnselected)
        assertNotEquals(true, copiedEmpty.ownedGraphAnnotationsUnselected)
        assertNotEquals(true, copiedNonempty.ownedGraphAnnotationsUnselected)
        assertSame(UnresolvedDeprecationProvider, copiedPending.deprecationsProvider)
        assertSame(EmptyDeprecationsProvider, copiedEmpty.deprecationsProvider)
        assertTrue(copiedNonempty.deprecationsProvider is AnnotationDeprecationsProvider)
        assertNotSame(completeNonempty.deprecationsProvider, copiedNonempty.deprecationsProvider)
        assertSame(result.getCopied(deprecated), copiedNonempty.annotations.single())
        assertSame(result.getCopied(message), (copiedNonempty.annotations.single() as CfirAnnotationCall).argumentList.arguments.single())
        assertNotSame(deprecated, copiedNonempty.annotations.single())
        assertThrows(IllegalStateException::class.java) { copiedPending.resolveDeprecationsProviderIfNeeded() }
        // RAW 分组结果没有已解析重建队列；未选内容必须在实际声明的 readiness 门拒绝。
        assertThrows(IllegalStateException::class.java) { copiedPending.requireOwnedGraphAnalysisReady() }
        assertSame(UnresolvedDeprecationProvider, copiedPending.deprecationsProvider)
        assertEquals(true, copiedPending.ownedGraphAnnotationsUnselected)
        copiedEmpty.resolveDeprecationsProviderIfNeeded()
        copiedNonempty.resolveDeprecationsProviderIfNeeded()
        copiedEmpty.requireOwnedGraphAnalysisReady()
        copiedNonempty.requireOwnedGraphAnalysisReady()
        assertSame(EmptyDeprecationsProvider, copiedEmpty.deprecationsProvider)
        assertTrue(copiedNonempty.deprecationsProvider is AnnotationDeprecationsProvider)
        assertEquals("retired", message.value)
        assertEquals(1, source.snapshotReads, "Owned refresh/readiness must not request the old Library source")
    }

    /** raw H 与完成代 M 使用不同 key；防御冻结不得靠改 raw 保存事实取得。 */
    @Test
    fun copiedRawHeaderAndDerivedMetadataRemainDistinctAndCompleteCollectionsAreFrozen() {
        val source = PublishedSource()
        val owner = function("headerOwner")
        val parameter = parameter("headerParameter", owner.symbol, source)
        val raw = CfirDeclarationAnnotationInfo(attributes = listOf("raw-H"), runtimeVisible = false)
        parameter.serializedAnnotationHeaderInfo = raw
        val targets = linkedSetOf(CangjieAnnotationTarget.entries.first())
        val names = mutableListOf("derived-M")
        val derived = CfirDeclarationAnnotationInfo(
            isAnnotation = true, annotationTargets = targets, attributes = names, runtimeVisible = true,
        )
        val node = annotation(owner.symbol, literal("1"))
        val annotationInput = mutableListOf<CfirAnnotation>(node)
        val complete = CfirPublishedAnnotations.Complete(annotationInput, annotationInfo = derived)
        val frozenDerived = checkNotNull(complete.annotationInfo)
        val expectedTargets = targets.toSet()
        targets.clear()
        names.clear()
        annotationInput.clear()
        assertEquals(expectedTargets, frozenDerived.annotationTargets)
        assertEquals(listOf("derived-M"), frozenDerived.attributes)
        assertSame(node, complete.values.single())
        assertThrows(UnsupportedOperationException::class.java) {
            (frozenDerived.annotationTargets as MutableSet<CangjieAnnotationTarget>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (frozenDerived.attributes as MutableList<String>).clear()
        }
        source.current = CompletedSnapshot(annotationViews = mapOf(parameter to complete))

        val result = deepCopyOwnedGraphs(listOf(parameter), typeRemapper, attributes)
        val copied = result.getCopied(parameter)
        assertEquals(1, source.snapshotReads)
        assertEquals(raw, copied.serializedAnnotationHeaderInfo)
        assertEquals(frozenDerived, copied.annotationInfo)
        assertNotEquals(copied.serializedAnnotationHeaderInfo, copied.annotationInfo)
        assertSame(raw, parameter.serializedAnnotationHeaderInfo)
        assertEquals(frozenDerived, parameter.annotationInfo)
        assertSame(result.getCopied(node), copied.annotations.single())

        // 等值重验合法，但保留第一次 raw 对象；不同事实和清空都拒绝。
        parameter.serializedAnnotationHeaderInfo = raw.copy()
        assertSame(raw, parameter.serializedAnnotationHeaderInfo)
        assertThrows(IllegalStateException::class.java) { parameter.serializedAnnotationHeaderInfo = frozenDerived }
        assertThrows(IllegalStateException::class.java) { parameter.serializedAnnotationHeaderInfo = null }
        assertThrows(IllegalStateException::class.java) { parameter.annotationInfo = raw }
        val newDerived = frozenDerived.copy(attributes = listOf("owned-M2"))
        copied.annotationInfo = newDerived
        assertEquals(newDerived, copied.annotationInfo)
        assertEquals(raw, copied.serializedAnnotationHeaderInfo)
        assertThrows(IllegalStateException::class.java) { copied.serializedAnnotationHeaderInfo = newDerived }
        assertThrows(IllegalStateException::class.java) { copied.serializedAnnotationHeaderInfo = null }
        assertSame(raw, parameter.serializedAnnotationHeaderInfo)
        assertEquals(frozenDerived, parameter.annotationInfo)
        assertEquals(listOf("derived-M"), checkNotNull(complete.annotationInfo).attributes)
        assertSame(node, complete.values.single())
    }

    /** 登记过 null 的 H 仍是保存事实；复制不能把其资格丢成“尚未登记”。 */
    @Test
    fun copiedNullRawHeaderRegistrationCannotBeReplacedByDerivedMetadata() {
        val source = PublishedSource()
        val owner = function("nullHeaderOwner")
        val parameter = parameter("nullHeaderParameter", owner.symbol, source)
        parameter.serializedAnnotationHeaderInfo = null
        parameter.serializedAnnotationHeaderInfo = null
        val derived = CfirDeclarationAnnotationInfo(attributes = listOf("completed-M"), isConstSafe = true)
        val complete = CfirPublishedAnnotations.Complete(emptyList(), annotationInfo = derived)
        source.current = CompletedSnapshot(annotationViews = mapOf(parameter to complete))

        val result = deepCopyOwnedGraphs(listOf(parameter), typeRemapper, attributes)
        val copied = result.getCopied(parameter)
        assertEquals(1, source.snapshotReads)
        assertNull(parameter.serializedAnnotationHeaderInfo)
        assertNull(copied.serializedAnnotationHeaderInfo)
        assertEquals(complete.annotationInfo, copied.annotationInfo)
        assertTrue(copied.annotations.isEmpty())
        copied.serializedAnnotationHeaderInfo = null
        assertThrows(IllegalStateException::class.java) { copied.serializedAnnotationHeaderInfo = derived }
        assertThrows(IllegalStateException::class.java) { parameter.serializedAnnotationHeaderInfo = derived }
        copied.annotationInfo = derived.copy(attributes = listOf("owned-M2"))
        assertEquals(listOf("owned-M2"), checkNotNull(copied.annotationInfo).attributes)
        assertNull(copied.serializedAnnotationHeaderInfo)
        assertNull(parameter.serializedAnnotationHeaderInfo)
        assertEquals(complete.annotationInfo, parameter.annotationInfo)
        assertEquals(listOf("completed-M"), checkNotNull(complete.annotationInfo).attributes)
    }

    /** C1 公共产品合同：Unselected 经两轮拥有复制仍缺内容，不能因空列表或旧源完成而获得资格。 */
    @Test
    fun twiceCopiedPublishedUnselectedRetainsMarkerAndUnresolvedDeprecations() {
        val source = PublishedSource()
        val owner = function("twiceUnselectedOwner")
        val pending = parameter("twiceUnselectedParameter", owner.symbol, source)
        owner.replaceValueParameters(listOf(pending))
        pending.serializedAnnotationHeaderInfo = CfirDeclarationAnnotationInfo(attributes = listOf("raw-H"))
        val firstResult = deepCopyOwnedGraphs(listOf(pending, owner), typeRemapper, attributes)
        val first = firstResult.getCopied(pending)
        val firstOwner = firstResult.getCopied(owner)
        assertEquals(1, source.snapshotReads)

        // 原 Library 的后来完成代不能补齐已有 Owned 副本，也不能被第二轮复制重新读取。
        source.current = CompletedSnapshot(annotationViews = mapOf(
            pending to CfirPublishedAnnotations.Complete(listOf(annotation(owner.symbol, literal("2")))),
        ))
        val secondResult = deepCopyOwnedGraphs(listOf(first, firstOwner), typeRemapper, attributes)
        val second = secondResult.getCopied(first)
        val secondOwner = secondResult.getCopied(firstOwner)
        assertNotSame(first, second)
        assertSame(secondOwner.symbol, second.containingDeclarationSymbol)
        assertSame(second, secondOwner.valueParameters.single())
        for (copy in listOf(first, second)) {
            assertSame(CfirContentSource.Owned, copy.contentSource)
            assertEquals(true, copy.ownedGraphAnnotationsUnselected)
            assertTrue(copy.annotations.isEmpty())
            assertNull(copy.annotationInfo)
            assertSame(UnresolvedDeprecationProvider, copy.deprecationsProvider)
            assertEquals(pending.serializedAnnotationHeaderInfo, copy.serializedAnnotationHeaderInfo)
            assertThrows(IllegalStateException::class.java) { copy.resolveDeprecationsProviderIfNeeded() }
            copy.ownedGraphAnalysisState = CfirOwnedGraphAnalysisState.NO_BODY_ANALYSIS
            assertThrows(IllegalStateException::class.java) { copy.requireOwnedGraphAnalysisReady() }
            assertEquals(true, copy.ownedGraphAnnotationsUnselected)
            assertSame(UnresolvedDeprecationProvider, copy.deprecationsProvider)
        }
        // 普通 Owned RAW 的 unresolved 生命周期不得被误标成 Library 未选内容。
        for (copy in listOf(firstOwner, secondOwner)) {
            assertNotEquals(true, copy.ownedGraphAnnotationsUnselected)
            assertSame(UnresolvedDeprecationProvider, copy.deprecationsProvider)
            copy.requireOwnedGraphAnalysisReady()
        }
        assertEquals(1, source.snapshotReads)
    }

    /** C1 公共产品合同：真实 null 登记必须跨 Published→Owned→Owned 保留，M 不得占用 H。 */
    @Test
    fun twiceCopiedNullRawHeaderRegistrationRejectsNonNullOverwrite() {
        val source = PublishedSource()
        val owner = function("twiceNullHeaderOwner")
        val parameter = parameter("twiceNullHeaderParameter", owner.symbol, source)
        parameter.serializedAnnotationHeaderInfo = null
        val derived = CfirDeclarationAnnotationInfo(attributes = listOf("completed-M"), isConstSafe = true)
        val complete = CfirPublishedAnnotations.Complete(emptyList(), annotationInfo = derived)
        source.current = CompletedSnapshot(annotationViews = mapOf(parameter to complete))
        val first = deepCopyOwnedGraphs(listOf(parameter), typeRemapper, attributes).getCopied(parameter)
        first.annotationInfo = derived.copy(attributes = listOf("owned-M2"))
        val second = deepCopyOwnedGraphs(listOf(first), typeRemapper, attributes).getCopied(first)
        assertNotSame(first, second)
        assertNotSame(first.attributes, second.attributes)
        assertEquals(listOf("owned-M2"), checkNotNull(second.annotationInfo).attributes)

        for (declaration in listOf(parameter, first, second)) {
            assertNull(declaration.serializedAnnotationHeaderInfo)
            declaration.serializedAnnotationHeaderInfo = null
            assertThrows(IllegalStateException::class.java) { declaration.serializedAnnotationHeaderInfo = derived }
            assertNull(declaration.serializedAnnotationHeaderInfo)
        }
        second.annotationInfo = derived.copy(attributes = listOf("owned-M3"))
        assertEquals(listOf("owned-M3"), checkNotNull(second.annotationInfo).attributes)
        assertEquals(listOf("owned-M2"), checkNotNull(first.annotationInfo).attributes)
        assertEquals(complete.annotationInfo, parameter.annotationInfo)
        assertNull(second.serializedAnnotationHeaderInfo)
        assertThrows(IllegalStateException::class.java) {
            second.serializedAnnotationHeaderInfo = checkNotNull(second.annotationInfo)
        }
        assertEquals(2, source.snapshotReads, "Only first copy and the explicit Published metadata getter read snapshots")
    }

    /** C1 公共产品合同：Owned 的 H/M 各自保存事实，两轮复制均拥有独立的实际集合容器。 */
    @Test
    fun twiceCopiedOwnedRawAndDerivedMetadataHaveIndependentContainersAndFacts() {
        val owner = function("ownedMetadataOwner")
        val parameter = parameter("ownedMetadataParameter", owner.symbol)
        val rawTargets = linkedSetOf(CangjieAnnotationTarget.entries.first())
        val rawNames = mutableListOf("raw-H")
        val derivedTargets = linkedSetOf(CangjieAnnotationTarget.entries.last())
        val derivedNames = mutableListOf("derived-M")
        val raw = CfirDeclarationAnnotationInfo(annotationTargets = rawTargets, attributes = rawNames, runtimeVisible = false)
        val derived = CfirDeclarationAnnotationInfo(
            isAnnotation = true, annotationTargets = derivedTargets, attributes = derivedNames, runtimeVisible = true,
        )
        val expectedRaw = raw.copy(annotationTargets = rawTargets.toSet(), attributes = rawNames.toList())
        val expectedDerived = derived.copy(annotationTargets = derivedTargets.toSet(), attributes = derivedNames.toList())
        parameter.serializedAnnotationHeaderInfo = raw
        parameter.annotationInfo = derived
        val first = deepCopyOwnedGraphs(listOf(parameter), typeRemapper, attributes).getCopied(parameter)
        val second = deepCopyOwnedGraphs(listOf(first), typeRemapper, attributes).getCopied(first)
        val declarations = listOf(parameter, first, second)
        for ((original, copy) in declarations.zipWithNext()) {
            assertNotSame(original.attributes, copy.attributes)
            val originalRaw = checkNotNull(original.serializedAnnotationHeaderInfo)
            val copiedRaw = checkNotNull(copy.serializedAnnotationHeaderInfo)
            val originalDerived = checkNotNull(original.annotationInfo)
            val copiedDerived = checkNotNull(copy.annotationInfo)
            assertNotSame(originalRaw.annotationTargets, copiedRaw.annotationTargets)
            assertNotSame(originalRaw.attributes, copiedRaw.attributes)
            assertNotSame(originalDerived.annotationTargets, copiedDerived.annotationTargets)
            assertNotSame(originalDerived.attributes, copiedDerived.attributes)
        }
        rawTargets.clear()
        rawNames.clear()
        derivedTargets.clear()
        derivedNames.clear()
        for (copy in listOf(first, second)) {
            val copiedRaw = checkNotNull(copy.serializedAnnotationHeaderInfo)
            val copiedDerived = checkNotNull(copy.annotationInfo)
            assertEquals(expectedRaw, copiedRaw)
            assertEquals(expectedDerived, copiedDerived)
            assertNotEquals(copiedRaw, copiedDerived)
            assertNotSame(copiedRaw.annotationTargets, copiedDerived.annotationTargets)
            assertNotSame(copiedRaw.attributes, copiedDerived.attributes)
            copy.serializedAnnotationHeaderInfo = expectedRaw.copy()
            assertSame(copiedRaw, copy.serializedAnnotationHeaderInfo)
            assertThrows(IllegalStateException::class.java) { copy.serializedAnnotationHeaderInfo = copiedDerived }
            assertThrows(IllegalStateException::class.java) { copy.serializedAnnotationHeaderInfo = null }
        }
        first.annotationInfo = expectedDerived.copy(attributes = listOf("owned-M2"))
        assertEquals(expectedRaw, first.serializedAnnotationHeaderInfo)
        assertEquals(expectedRaw, second.serializedAnnotationHeaderInfo)
        assertEquals(expectedDerived, second.annotationInfo)
        assertEquals(listOf("owned-M2"), checkNotNull(first.annotationInfo).attributes)
    }

    /** C2 公共产品合同：非空 Java/ObjC 生成 ID 和主层集合一起冻结，输入后续修改不得污染完成代。 */
    @Test
    fun completedInteropDefensivelyCopiesNonemptyJavaAndObjCGeneratedIds() {
        val javaIds = mutableListOf("java-A", "java-B")
        val objcIds = mutableListOf("objc-A", "objc-B")
        val ffiNames = mutableListOf("ffi-A")
        val platformNames = mutableListOf("platform-A")
        val ffiKinds = linkedSetOf(org.cangnova.cangjie.annotations.BuiltInAnnotationKind.entries.first())
        val platformKinds = linkedSetOf(org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind.entries.first())
        val input = CfirInteropInfo.NONE.copy(
            java = CfirJavaInteropInfo(isMirror = true, externalName = "java-name", generatedDeclarationIds = javaIds),
            objc = CfirObjCInteropInfo(isInit = true, externalName = "objc-name", generatedDeclarationIds = objcIds),
            ffiAnnotationNames = ffiNames, platformAnnotationNames = platformNames,
            ffiAnnotationKinds = ffiKinds, platformAnnotationKinds = platformKinds,
        )
        val expected = input.copy(
            java = checkNotNull(input.java).copy(generatedDeclarationIds = javaIds.toList()),
            objc = checkNotNull(input.objc).copy(generatedDeclarationIds = objcIds.toList()),
            ffiAnnotationNames = ffiNames.toList(), platformAnnotationNames = platformNames.toList(),
            ffiAnnotationKinds = ffiKinds.toSet(), platformAnnotationKinds = platformKinds.toSet(),
        )
        val complete = CfirPublishedAnnotations.Complete(emptyList(), interopInfo = input)
        val source = PublishedSource()
        val owner = function("frozenInteropOwner", source)
        source.current = CompletedSnapshot(annotationViews = mapOf(owner to complete))
        val observed = checkNotNull(owner.interopInfo)
        assertSame(complete.interopInfo, observed)
        assertNotSame(javaIds, checkNotNull(observed.java).generatedDeclarationIds)
        assertNotSame(objcIds, checkNotNull(observed.objc).generatedDeclarationIds)
        javaIds[0] = "changed-java"
        javaIds += "java-C"
        objcIds.clear()
        objcIds += "changed-objc"
        ffiNames.clear()
        platformNames.clear()
        ffiKinds.clear()
        platformKinds.clear()
        assertEquals(expected, observed)
        assertEquals(expected, owner.interopInfo)
        assertEquals(expected, complete.interopInfo)
    }

    /** C2 公共产品合同：返回的非空嵌套及主层列表拒绝直接、迭代器和子列表写入。 */
    @Test
    fun completedInteropReturnedListsRejectMutationIncludingGeneratedIds() {
        val input = CfirInteropInfo.NONE.copy(
            java = CfirJavaInteropInfo(generatedDeclarationIds = mutableListOf("java-A", "java-B")),
            objc = CfirObjCInteropInfo(generatedDeclarationIds = mutableListOf("objc-A", "objc-B")),
            ffiAnnotationNames = mutableListOf("ffi-A", "ffi-B"),
            platformAnnotationNames = mutableListOf("platform-A", "platform-B"),
        )
        val complete = CfirPublishedAnnotations.Complete(emptyList(), interopInfo = input)
        val observed = checkNotNull(complete.interopInfo)
        val lists = listOf(
            checkNotNull(observed.java).generatedDeclarationIds,
            checkNotNull(observed.objc).generatedDeclarationIds,
            observed.ffiAnnotationNames,
            observed.platformAnnotationNames,
        )
        for (list in lists) {
            val expected = list.toList()
            val mutable = list as MutableList<String>
            assertThrows(UnsupportedOperationException::class.java) { mutable.add("unexpected") }
            assertThrows(UnsupportedOperationException::class.java) { mutable[0] = "unexpected" }
            assertThrows(UnsupportedOperationException::class.java) { mutable.clear() }
            assertThrows(UnsupportedOperationException::class.java) {
                val iterator = mutable.iterator()
                iterator.next()
                iterator.remove()
            }
            assertThrows(UnsupportedOperationException::class.java) {
                val iterator = mutable.listIterator()
                iterator.next()
                iterator.set("unexpected")
            }
            assertThrows(UnsupportedOperationException::class.java) { mutable.subList(0, 1).clear() }
            assertEquals(expected, list)
        }
        assertEquals(input, observed)
        assertSame(observed, complete.interopInfo)
    }

    /** 拒绝必须先于任意子节点访问，避免最后 setter 抛错时默认值或注解映射已经被污染。 */
    @Test
    fun publishedChildTransformsRejectBeforeVisitingOrChangingDefaultsAnnotationsAndMapping() {
        val source = PublishedSource()
        val owner = function("transformOwner", source)
        val parameter = parameter("transformParameter", owner.symbol, source)
        val leaf = literal("1")
        val default = buildUnaryExpression { operand = leaf; operation = CfirSourceOperator.SUBTRACT; coneTypeOrNull = type }
        val node = annotation(owner.symbol, leaf)
        val argumentName = Name.identifier("value")
        val mapping = CfirAnnotationArgumentMappingImpl(null, mapOf(argumentName to leaf))
        node.replaceArgumentMapping(mapping)
        val originalTarget = node.annotationTarget
        val originalTypeRef = node.typeRef
        val originalArguments = node.argumentList
        val originalCallee = node.calleeReference
        val complete = CfirPublishedAnnotations.Complete(listOf(node))
        source.current = CompletedSnapshot(mapOf(parameter to default), mapOf(parameter to complete, owner to complete))
        var childVisits = 0
        val transformer = object : CfirTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E {
                childVisits++
                if (element is CfirAnnotationCall) {
                    element.replaceArgumentMapping(CfirAnnotationArgumentMappingImpl(null, emptyMap()))
                    element.replaceAnnotationTarget(CangjieAnnotationTarget.entries.first())
                }
                return element
            }
        }

        assertThrows(IllegalStateException::class.java) { parameter.transformChildren(transformer, Unit) }
        assertThrows(IllegalStateException::class.java) { parameter.transformAnnotations(transformer, Unit) }
        assertThrows(IllegalStateException::class.java) { parameter.transformDefaultValue(transformer, Unit) }
        assertThrows(IllegalStateException::class.java) { owner.transformChildren(transformer, Unit) }
        assertThrows(IllegalStateException::class.java) { owner.transformAnnotations(transformer, Unit) }
        assertEquals(0, childVisits)
        assertEquals(0, source.snapshotReads, "Rejected transforms must not read the Published children")
        assertSame(default, parameter.defaultValue)
        assertSame(leaf, default.operand)
        assertSame(type, default.coneTypeOrNull)
        assertEquals(CfirSourceOperator.SUBTRACT, default.operation)
        assertEquals("1", leaf.value)
        assertSame(node, parameter.annotations.single())
        assertSame(node, owner.annotations.single())
        assertSame(complete, source.current.annotations(parameter))
        assertSame(originalArguments, node.argumentList)
        assertSame(leaf, node.argumentList.arguments.single())
        assertSame(mapping, node.argumentMapping)
        assertEquals(setOf(argumentName), mapping.mapping.keys)
        assertSame(leaf, mapping.mapping.getValue(argumentName))
        assertSame(originalTypeRef, node.typeRef)
        assertSame(originalCallee, node.calleeReference)
        assertEquals(originalTarget, node.annotationTarget)
        assertSame(owner.symbol, node.containingDeclarationSymbol)
        assertEquals(0, childVisits)
    }

    /** 先列参数再列函数，验证前向 owner 符号；共享默认节点只从内容槽发现，不另列根掩盖漏遍历。 */
    private fun assertGroupedGenerationCopy(phase: CfirResolvePhase) {
        val source = PublishedSource()
        val owner = function("groupOwner", source, phase = phase)
        val first = parameter("first", owner.symbol, source, phase = phase)
        val second = parameter("second", owner.symbol, source, phase = phase)
        owner.replaceValueParameters(listOf(first, second))
        val sharedDefault = literal("1")
        val sharedAnnotation = annotation(owner.symbol, sharedDefault)
        val firstView = CfirPublishedAnnotations.Complete(listOf(sharedAnnotation, sharedAnnotation))
        val singleView = CfirPublishedAnnotations.Complete(listOf(sharedAnnotation))
        val captured = CompletedSnapshot(
            mapOf(first to sharedDefault, second to sharedDefault),
            mapOf(first to firstView, second to singleView, owner to singleView),
        )
        val laterDefault = literal("9")
        val laterAnnotation = annotation(owner.symbol, laterDefault)
        val laterView = CfirPublishedAnnotations.Complete(listOf(laterAnnotation))
        val later = CompletedSnapshot(
            mapOf(first to laterDefault, second to laterDefault),
            mapOf(first to laterView, second to laterView, owner to laterView),
        )
        source.current = captured
        // 第一次读取后完成代立即变化；任何再次 snapshot 都会取得不同节点和列表。
        source.afterSnapshot = { source.current = later }

        val result = deepCopyOwnedGraphs(listOf(first, second, owner, first), typeRemapper, attributes)
        assertEquals(1, source.snapshotReads, "Collect, allocate, fill and readiness must share one source snapshot")
        assertSame(later, source.current)
        val copiedFirst = result.getCopied(first)
        val copiedSecond = result.getCopied(second)
        val copiedOwner = result.getCopied(owner)
        val copiedDefault = result.getCopied(sharedDefault)
        val copiedAnnotation = result.getCopied(sharedAnnotation)
        assertSame(result.roots[0], result.roots[3])
        assertSame(CfirContentSource.Owned, copiedFirst.contentSource)
        assertSame(CfirContentSource.Owned, copiedSecond.contentSource)
        assertSame(CfirContentSource.Owned, copiedOwner.contentSource)
        assertNotSame(sharedDefault, copiedDefault)
        assertNotSame(sharedAnnotation, copiedAnnotation)
        assertSame(sharedDefault.source, copiedDefault.source)
        assertSame(type, copiedDefault.coneTypeOrNull)
        assertEquals("1", copiedDefault.value)
        assertSame(copiedDefault, copiedFirst.defaultValue)
        assertSame(copiedDefault, copiedSecond.defaultValue)
        assertSame(copiedDefault, copiedAnnotation.argumentList.arguments.single())
        assertEquals(listOf(copiedAnnotation, copiedAnnotation), copiedFirst.annotations)
        assertSame(copiedAnnotation, copiedSecond.annotations.single())
        assertSame(copiedAnnotation, copiedOwner.annotations.single())
        assertEquals(listOf(copiedFirst, copiedSecond), copiedOwner.valueParameters)
        assertNotSame(first.symbol, copiedFirst.symbol)
        assertNotSame(owner.symbol, copiedOwner.symbol)
        assertSame(result.getCopiedSymbol(first.symbol), copiedFirst.symbol)
        assertSame(copiedFirst, copiedFirst.symbol.cfir)
        assertSame(result.getCopiedSymbol(owner.symbol), copiedOwner.symbol)
        assertSame(copiedOwner, copiedOwner.symbol.cfir)
        assertSame(copiedOwner.symbol, copiedFirst.containingDeclarationSymbol)
        assertSame(copiedOwner.symbol, copiedSecond.containingDeclarationSymbol)
        assertSame(copiedOwner.symbol, copiedAnnotation.containingDeclarationSymbol)
        // annotation class 是外部借用符号，不能因复制其注解而新建同名 classifier。
        assertSame(
            (sharedAnnotation.calleeReference as CfirResolvedNamedReference).resolvedSymbol,
            (copiedAnnotation.calleeReference as CfirResolvedNamedReference).resolvedSymbol,
        )
        if (phase == CfirResolvePhase.BODY_RESOLVE) {
            assertTrue(result.resolvedOwnersRequiringAnalysisRebuild.any { it === copiedFirst })
            assertTrue(result.resolvedOwnersRequiringAnalysisRebuild.any { it === copiedSecond })
        }

        copiedFirst.replaceDefaultValue(literal("3"))
        copiedFirst.replaceAnnotations(emptyList())
        assertSame(copiedDefault, copiedSecond.defaultValue)
        assertSame(copiedAnnotation, copiedOwner.annotations.single())
        assertSame(copiedDefault, copiedAnnotation.argumentList.arguments.single())
        assertSame(sharedDefault, captured.defaultValue(first))
        assertSame(sharedDefault, captured.defaultValue(second))
        assertSame(firstView, captured.annotations(first))
        assertEquals(listOf(sharedAnnotation, sharedAnnotation), firstView.values)
        assertSame(sharedDefault, sharedAnnotation.argumentList.arguments.single())
        assertSame(owner.symbol, first.containingDeclarationSymbol)
        assertSame(owner.symbol, sharedAnnotation.containingDeclarationSymbol)
        assertSame(laterDefault, later.defaultValue(first))
        assertSame(laterAnnotation, later.annotations(first).values.single())
        assertEquals(1, source.snapshotReads, "Mutating an Owned copy must not consult its former Published source")
    }

    /** 只有测试显式切换完成代；snapshot 本身不产生、补全或修改声明内容。 */
    private class PublishedSource : CfirContentSource.Published {
        var current = CompletedSnapshot()
        var snapshotReads = 0
            private set
        var afterSnapshot: (() -> Unit)? = null

        override fun snapshot(): CfirPublishedContentSnapshot {
            snapshotReads++
            val captured = current
            afterSnapshot?.invoke()
            return captured
        }
    }

    /** 身份键与完成视图均在构造时冻结；未知 owner 表示本代未选，不制造默认节点或注解。 */
    private class CompletedSnapshot(
        defaults: Map<CfirValueParameter, CfirExpression> = emptyMap(),
        annotationViews: Map<CfirDeclaration, CfirPublishedAnnotations> = emptyMap(),
    ) : CfirPublishedContentSnapshot {
        private val defaults = Collections.unmodifiableMap(IdentityHashMap(defaults))
        private val annotationViews = Collections.unmodifiableMap(IdentityHashMap(annotationViews))
        var bodyReads = 0
            private set
        var initializerReads = 0
            private set

        override fun body(owner: CfirFunctionBodyOwner): CfirBlock? {
            bodyReads++
            return null
        }

        override fun initializer(owner: CfirVariable): CfirExpression? {
            initializerReads++
            return null
        }

        override fun defaultValue(owner: CfirValueParameter): CfirExpression? = defaults[owner]

        override fun annotations(owner: CfirDeclaration): CfirPublishedAnnotations =
            annotationViews[owner] ?: CfirPublishedAnnotations.Unselected
    }

    private fun parameter(
        name: String,
        owner: CfirBasedSymbol<*>,
        content: CfirContentSource = CfirContentSource.Owned,
        defaultValue: CfirExpression? = null,
        annotations: List<CfirAnnotation> = emptyList(),
        phase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR,
    ): CfirValueParameter = buildValueParameter {
        source = source(name)
        moduleData = module
        resolvePhase = phase
        origin = if (content === CfirContentSource.Owned) CfirDeclarationOrigin.Source else CfirDeclarationOrigin.Library
        this.attributes = CfirDeclarationAttributes.EMPTY
        contentSource = content
        isLocal = true
        status = CfirDeclarationStatusImpl()
        returnTypeRef = buildResolvedTypeRef { coneType = type }
        symbol = CfirValueParameterSymbol(CallableId(Name.identifier(name)))
        containingDeclarationSymbol = owner
        this.name = Name.identifier(name)
        isNamed = false
        isTypeImplicit = false
        this.defaultValue = defaultValue
        this.annotations.addAll(annotations)
    }

    private fun function(
        name: String,
        content: CfirContentSource = CfirContentSource.Owned,
        annotations: List<CfirAnnotation> = emptyList(),
        phase: CfirResolvePhase = CfirResolvePhase.RAW_CFIR,
    ): CfirNamedFunction = buildNamedFunction {
        source = source(name)
        moduleData = module
        resolvePhase = phase
        origin = if (content === CfirContentSource.Owned) CfirDeclarationOrigin.Source else CfirDeclarationOrigin.Library
        this.attributes = CfirDeclarationAttributes.EMPTY
        contentSource = content
        isLocal = true
        status = CfirDeclarationStatusImpl()
        returnTypeRef = buildResolvedTypeRef { coneType = type }
        symbol = CfirNamedFunctionSymbol(CallableId(Name.identifier(name)))
        this.name = Name.identifier(name)
        isMut = false
        this.annotations.addAll(annotations)
    }

    private fun literal(value: String): CfirLiteralExpression = buildLiteralExpression {
        source = source("literal-$value")
        kind = CfirLiteralKind.INT
        this.value = value
        coneTypeOrNull = type
    }

    private fun annotation(owner: CfirBasedSymbol<*>, argument: CfirExpression): CfirAnnotationCall {
        val id = ClassId.topLevel(FqName("CopyMarker"))
        val annotationType = ConeClassLikeType(id.toLookupTag())
        return buildAnnotationCall {
            source = source("copy-marker")
            coneTypeOrNull = annotationType
            annotationClassId = id
            typeRef = buildResolvedTypeRef { coneType = annotationType }
            argumentList = buildArgumentList { arguments += argument }
            calleeReference = buildResolvedNamedReference {
                name = Name.identifier("CopyMarker")
                resolvedSymbol = CfirClassSymbol(id)
            }
            containingDeclarationSymbol = owner
        }
    }

    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}
}
