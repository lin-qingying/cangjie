package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.isInReferenceCycle
import org.cangnova.cangjie.cfir.builder.buildQualifierPart
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompletedSupertypeProduct
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 图的完成版本与错误替换合同，覆盖曾向成员查询暴露旧 extend 边的根因。 */
class CfirSuperTypeGraphStoreTest {
    @Test
    fun `same graph product does not advance revision but cycle membership does`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val target = ExtendTestFixtures.newInterface(module, "Target", classId("Target"))
        val ref = ExtendTestFixtures.classTypeRef(target.symbol.classId, isInterface = true)
        val owner = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"), listOf(ref))
        val edge = CfirSuperTypeGraphEdge(ref, "Target", target.symbol)
        val store = CfirSuperTypeGraphStore()

        store.recordDeclared(owner, listOf(ref), listOf(edge))
        store.publishCompletedProducts()
        val firstRevision = store.modificationCount
        assertEquals(false, store.getPublishedReferenceCycleState(owner.symbol))
        store.recordDeclared(owner, listOf(ref), listOf(edge))
        store.publishCompletedProducts()
        assertEquals(firstRevision, store.modificationCount)

        owner.isInReferenceCycle = true
        assertEquals(false, store.getPublishedReferenceCycleState(owner.symbol))
        store.recordDeclared(owner, listOf(ref), listOf(edge))
        assertEquals(false, store.getPublishedReferenceCycleState(owner.symbol))
        store.publishCompletedProducts()
        assertTrue(store.modificationCount > firstRevision)
        assertSame(ref, store.getDirectSuperTypes(owner.symbol.classId).single())
        val product = checkNotNull(store.getCompletedSupertypeProduct(owner.symbol))
        assertSame(ref, product.superTypeRefs.single())
        assertTrue(product.isInReferenceCycle)
        assertEquals(true, store.getPublishedReferenceCycleState(owner.symbol))
    }

    @Test
    fun `extend error replacement retires the original graph edge`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val ownerDeclaration = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"))
        val owner = ownerDeclaration.symbol.classId
        val target = ExtendTestFixtures.newInterface(module, "Target", classId("Target"))
        val ref = ExtendTestFixtures.classTypeRef(target.symbol.classId, isInterface = true)
        val extend = ExtendTestFixtures.newExtend(
            moduleData = module,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(owner),
            superTypeRefs = listOf(ref),
        )
        val store = CfirSuperTypeGraphStore()
        store.recordDeclared(ownerDeclaration, emptyList(), emptyList())
        store.recordDeclared(target, emptyList(), emptyList())
        store.recordExtended(ownerDeclaration.symbol, listOf(CfirSuperTypeGraphEdge(ref, "Target", target.symbol, extend)))
        store.publishCompletedProducts()
        val firstRevision = store.modificationCount
        val replacement = ConeErrorType(ConeSimpleDiagnostic("Loop", DiagnosticKind.LoopInSupertype))
            .toCfirResolvedTypeRef(ref.source, ref)

        store.replaceExtendedTypeRef(extend, ref, replacement)
        store.publishCompletedProducts()
        assertSame(replacement, store.getDirectSuperTypeEdges(owner).single().typeRef)
        val product = checkNotNull(store.getCompletedSupertypeProduct(ownerDeclaration.symbol))
        assertTrue(product.superTypeRefs.isEmpty())
        assertSame(replacement, product.extendedSuperTypes.single().typeRef)
        assertSame(extend, product.extendedSuperTypes.single().sourceExtend)
        assertTrue(store.modificationCount > firstRevision)
        val replacementRevision = store.modificationCount
        store.replaceExtendedTypeRef(extend, ref, replacement)
        store.publishCompletedProducts()
        assertEquals(replacementRevision, store.modificationCount)
    }

    /** 空根的完成状态属于该 symbol；其它节点的发布版本不能替代它。 */
    @Test
    fun `completed empty product is distinct from unpublished symbol`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val root = ExtendTestFixtures.newInterface(module, "Root", classId("Root"))
        val missing = ExtendTestFixtures.newInterface(module, "Missing", classId("Missing"))
        val store = CfirSuperTypeGraphStore()
        assertNull(store.getCompletedSupertypeProduct(root.symbol))
        assertNull(store.getPublishedReferenceCycleState(root.symbol))
        store.recordDeclared(root, emptyList(), emptyList())
        assertNull(store.getCompletedSupertypeProduct(root.symbol))
        assertEquals(0L, store.modificationCount)
        store.publishCompletedProducts()
        val product = checkNotNull(store.getCompletedSupertypeProduct(root.symbol))
        assertSame(root.symbol, product.ownerSymbol)
        assertTrue(product.superTypeRefs.isEmpty())
        assertTrue(product.declaredSuperTypes.isEmpty())
        assertTrue(product.extendedSuperTypes.isEmpty())
        assertFalse(product.isInReferenceCycle)
        assertEquals(false, store.getPublishedReferenceCycleState(root.symbol))
        assertNull(store.getCompletedSupertypeProduct(missing.symbol))
        assertNull(store.getPublishedReferenceCycleState(missing.symbol))
        val revision = store.modificationCount
        store.recordDeclared(root, emptyList(), emptyList())
        store.publishCompletedProducts()
        assertEquals(revision, store.modificationCount)
    }

    /** 未完成原始 refs 只能发布已证实的环标记，不能授权非循环 false。 */
    @Test
    fun `marker only raw references publish cycle true independently of complete product`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val rawRef = buildUserTypeRef {
            source = object : CjBinarySourceElement("Future", null, "graph:Future") {}
            qualifier += buildQualifierPart { name = Name.identifier("Future") }
        }
        val owner = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"), listOf(rawRef))
        val unknown = ExtendTestFixtures.newInterface(module, "Unknown", classId("Unknown"))
        val store = CfirSuperTypeGraphStore()
        assertNull(store.getPublishedReferenceCycleState(owner.symbol))
        assertNull(store.getPublishedReferenceCycleState(unknown.symbol))
        store.recordDeclared(owner, listOf(rawRef), emptyList())
        store.publishCompletedProducts()
        assertNull(store.getCompletedSupertypeProduct(owner.symbol))
        assertNull(store.getPublishedReferenceCycleState(owner.symbol))
        val nonCycleRevision = store.modificationCount
        owner.isInReferenceCycle = true
        store.recordDeclared(owner, listOf(rawRef), emptyList())
        assertNull(store.getPublishedReferenceCycleState(owner.symbol))
        store.publishCompletedProducts()
        assertNull(store.getCompletedSupertypeProduct(owner.symbol))
        assertEquals(true, store.getPublishedReferenceCycleState(owner.symbol))
        assertNull(store.getPublishedReferenceCycleState(unknown.symbol))
        assertSame(rawRef, owner.superTypeRefs.single())
        assertTrue(store.modificationCount > nonCycleRevision)
        val cycleRevision = store.modificationCount
        owner.isInReferenceCycle = false
        assertEquals(true, store.getPublishedReferenceCycleState(owner.symbol))
        assertEquals(cycleRevision, store.modificationCount)
        owner.isInReferenceCycle = true
        store.recordDeclared(owner, listOf(rawRef), emptyList())
        store.publishCompletedProducts()
        assertEquals(cycleRevision, store.modificationCount)
        assertNull(store.getCompletedSupertypeProduct(owner.symbol))
        assertEquals(true, store.getPublishedReferenceCycleState(owner.symbol))
    }

    /** 完成快照必须同时隔离工作 refs 和工作 edges。 */
    @Test
    fun `working replacement cannot change completed refs or edges`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val first = ExtendTestFixtures.newInterface(module, "First", classId("First"))
        val second = ExtendTestFixtures.newInterface(module, "Second", classId("Second"))
        val firstRef = ExtendTestFixtures.classTypeRef(first.symbol.classId, isInterface = true)
        val secondRef = ExtendTestFixtures.classTypeRef(second.symbol.classId, isInterface = true)
        val owner = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"), listOf(firstRef))
        val store = CfirSuperTypeGraphStore()
        store.recordDeclared(first, emptyList(), emptyList())
        store.recordDeclared(second, emptyList(), emptyList())
        store.recordDeclared(owner, listOf(firstRef), listOf(CfirSuperTypeGraphEdge(firstRef, "First", first.symbol)))
        store.publishCompletedProducts()
        val revision = store.modificationCount
        store.recordDeclared(owner, listOf(secondRef), listOf(CfirSuperTypeGraphEdge(secondRef, "Second", second.symbol)))
        val before = checkNotNull(store.getCompletedSupertypeProduct(owner.symbol))
        assertSame(firstRef, before.superTypeRefs.single())
        assertSame(firstRef, before.declaredSuperTypes.single().typeRef)
        assertSame(first.symbol, before.declaredSuperTypes.single().resolvedClassSymbol)
        assertEquals(revision, store.modificationCount)
        store.publishCompletedProducts()
        val after = checkNotNull(store.getCompletedSupertypeProduct(owner.symbol))
        assertSame(secondRef, after.superTypeRefs.single())
        assertSame(secondRef, after.declaredSuperTypes.single().typeRef)
        assertSame(second.symbol, after.declaredSuperTypes.single().resolvedClassSymbol)
        assertSame(firstRef, before.superTypeRefs.single())
        assertTrue(store.modificationCount > revision)
    }

    @Test
    fun `same class id does not publish a different declaration symbol`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val first = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"))
        val second = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"))
        val store = CfirSuperTypeGraphStore()
        store.recordDeclared(first, emptyList(), emptyList())
        store.publishCompletedProducts()
        assertSame(first.symbol, checkNotNull(store.getCompletedSupertypeProduct(first.symbol)).ownerSymbol)
        assertNull(store.getCompletedSupertypeProduct(second.symbol))
    }

    /** 同名声明的完整 refs、声明边和 extend 边必须按原 symbol 分别保存。 */
    @Test
    fun `same class id symbols publish independent products and legacy lookup rejects ambiguity`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val a = ExtendTestFixtures.newInterface(module, "A", classId("A"))
        val b = ExtendTestFixtures.newInterface(module, "B", classId("B"))
        val aRef = ExtendTestFixtures.classTypeRef(a.symbol.classId, isInterface = true)
        val bRef = ExtendTestFixtures.classTypeRef(b.symbol.classId, isInterface = true)
        val first = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"), listOf(aRef))
        val second = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"), listOf(bRef))
        val firstExtend = ExtendTestFixtures.newExtend(module, ExtendTestFixtures.classTypeRef(first.symbol.classId, isInterface = true), listOf(bRef))
        val secondExtend = ExtendTestFixtures.newExtend(module, ExtendTestFixtures.classTypeRef(second.symbol.classId, isInterface = true), listOf(aRef))
        val firstEdge = CfirSuperTypeGraphEdge(aRef, "A", a.symbol)
        val secondEdge = CfirSuperTypeGraphEdge(bRef, "B", b.symbol)
        val firstExtendedEdge = CfirSuperTypeGraphEdge(bRef, "B", b.symbol, firstExtend)
        val secondExtendedEdge = CfirSuperTypeGraphEdge(aRef, "A", a.symbol, secondExtend)
        val store = CfirSuperTypeGraphStore()
        store.recordDeclared(a, emptyList(), emptyList())
        store.recordDeclared(b, emptyList(), emptyList())
        store.recordDeclared(first, listOf(aRef), listOf(firstEdge))
        store.recordDeclared(second, listOf(bRef), listOf(secondEdge))
        store.recordExtended(first.symbol, listOf(firstExtendedEdge))
        store.recordExtended(second.symbol, listOf(secondExtendedEdge))
        assertSame(a.symbol, checkNotNull(store.getNode(first.symbol)).declaredSuperTypes.single().resolvedClassSymbol)
        assertSame(b.symbol, checkNotNull(store.getNode(second.symbol)).declaredSuperTypes.single().resolvedClassSymbol)
        store.publishCompletedProducts()
        val firstProduct = checkNotNull(store.getCompletedSupertypeProduct(first.symbol))
        val secondProduct = checkNotNull(store.getCompletedSupertypeProduct(second.symbol))
        assertSame(first.symbol, firstProduct.ownerSymbol)
        assertSame(second.symbol, secondProduct.ownerSymbol)
        assertSame(aRef, firstProduct.superTypeRefs.single())
        assertSame(bRef, secondProduct.superTypeRefs.single())
        assertSame(firstEdge, firstProduct.declaredSuperTypes.single())
        assertSame(secondEdge, secondProduct.declaredSuperTypes.single())
        assertSame(firstExtendedEdge, firstProduct.extendedSuperTypes.single())
        assertSame(secondExtendedEdge, secondProduct.extendedSuperTypes.single())
        assertSame(firstExtend, firstProduct.extendedSuperTypes.single().sourceExtend)
        assertSame(secondExtend, secondProduct.extendedSuperTypes.single().sourceExtend)
        val revision = store.modificationCount
        store.recordDeclared(first, listOf(aRef), listOf(firstEdge))
        store.recordDeclared(second, listOf(bRef), listOf(secondEdge))
        store.recordExtended(first.symbol, listOf(firstExtendedEdge))
        store.recordExtended(second.symbol, listOf(secondExtendedEdge))
        store.publishCompletedProducts()
        assertEquals(revision, store.modificationCount)
        assertSame(aRef, checkNotNull(store.getCompletedSupertypeProduct(first.symbol)).superTypeRefs.single())
        assertSame(bRef, checkNotNull(store.getCompletedSupertypeProduct(second.symbol)).superTypeRefs.single())
        assertThrows(IllegalStateException::class.java) { store.getDirectSuperTypes(first.symbol.classId) }
        assertThrows(IllegalStateException::class.java) { store.getDirectSuperTypeEdges(first.symbol.classId) }
        assertThrows(IllegalStateException::class.java) { store.getNode(first.symbol.classId) }
    }

    /** 相同空成员投影仍须区分完整错误引用的不同完成产品。 */
    @Test
    fun `error reference replacement advances revision with empty member projection`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val original = ConeErrorType(ConeSimpleDiagnostic("first error")).toCfirResolvedTypeRef(null)
        val replacement = ConeErrorType(ConeSimpleDiagnostic("second error")).toCfirResolvedTypeRef(null)
        val owner = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"), listOf(original))
        val store = CfirSuperTypeGraphStore()
        store.recordDeclared(owner, listOf(original), emptyList())
        store.publishCompletedProducts()
        val first = checkNotNull(store.getCompletedSupertypeProduct(owner.symbol))
        assertSame(original, first.superTypeRefs.single())
        assertTrue(first.declaredSuperTypes.isEmpty())
        val revision = store.modificationCount
        store.recordDeclared(owner, listOf(replacement), emptyList())
        assertSame(original, checkNotNull(store.getCompletedSupertypeProduct(owner.symbol)).superTypeRefs.single())
        store.publishCompletedProducts()
        val second = checkNotNull(store.getCompletedSupertypeProduct(owner.symbol))
        assertSame(replacement, second.superTypeRefs.single())
        assertTrue(second.declaredSuperTypes.isEmpty())
        assertSame(original, first.superTypeRefs.single())
        assertTrue(store.modificationCount > revision)
        val replacementRevision = store.modificationCount
        store.recordDeclared(owner, listOf(replacement), emptyList())
        store.publishCompletedProducts()
        assertEquals(replacementRevision, store.modificationCount)
    }

    /** 公开完成产品不能随构造输入或外部 list 修改而变化。 */
    @Test
    fun `completed product owns immutable reference and edge snapshots`() {
        val (_, module) = ExtendTestFixtures.newSessionAndModule()
        val target = ExtendTestFixtures.newInterface(module, "Target", classId("Target"))
        val ref = ExtendTestFixtures.classTypeRef(target.symbol.classId, isInterface = true)
        val owner = ExtendTestFixtures.newInterface(module, "Owner", classId("Owner"), listOf(ref))
        val refs = mutableListOf<CfirTypeRef>(ref)
        val edges = mutableListOf(CfirSuperTypeGraphEdge(ref, "Target", target.symbol))
        val product = CfirCompletedSupertypeProduct(owner.symbol, refs, edges, emptyList(), false)
        refs.clear()
        edges.clear()
        assertSame(ref, product.superTypeRefs.single())
        assertSame(ref, product.declaredSuperTypes.single().typeRef)
        assertSame(target.symbol, product.superTypes.single().resolvedClassSymbol)
        assertThrows(UnsupportedOperationException::class.java) {
            (product.superTypeRefs as MutableList<CfirTypeRef>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (product.declaredSuperTypes as MutableList<CfirSuperTypeGraphEdge>).clear()
        }
    }

    private fun classId(name: String): ClassId = ClassId(FqName("graph.contract"), Name.identifier(name))
}
