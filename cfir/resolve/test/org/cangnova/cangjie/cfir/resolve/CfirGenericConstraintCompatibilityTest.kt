package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 与真实类型上下文配合，验证纯约束比较保留多父域规则和显式外层映射。 */
class CfirGenericConstraintCompatibilityTest {
    private val session = CallResolutionTestFixtures.newTestSession()
    private val boundId = ClassId.topLevel(FqName("test.Bound"))
    private val bound = ExtendTestFixtures.classTypeRef(boundId, isInterface = true)
    private val boundDeclaration = ExtendTestFixtures.newInterface(session.moduleData, "Bound", boundId)
    // fixture 的 symbol 默认生成 general tag；fixed 表示显式保留同一真实声明与接口种类。
    private val fixedBound = ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(boundId, boundDeclaration.symbol), isInterface = true)

    init {
        with(CallResolutionTestFixtures) {
            session.registerHierarchyProviders(listOf(boundDeclaration))
        }
    }

    @Test
    fun childMayLoosenButCannotNarrowParentDomain() {
        val unrestricted = parameter("T")
        val constrained = parameter("U", bound)
        assertEquals(listOf(0), incompatible(constrained, listOf(parent(unrestricted))))
        assertEquals(emptyList<Int>(), incompatible(unrestricted, listOf(parent(constrained))))
    }

    @Test
    fun matchingOneParentDoesNotPermitNarrowingAnotherParent() {
        val child = parameter("Child", bound)
        val sameDomain = parameter("Same", bound)
        val widerDomain = parameter("Wider")
        assertEquals(emptyList<Int>(), incompatible(child, listOf(parent(sameDomain))))
        assertEquals(listOf(0), incompatible(child, listOf(parent(sameDomain), parent(widerDomain))))
    }

    @Test
    fun parentDomainUsesTheExplicitOuterAndOwnSubstitution() {
        val commonOuter = parameter("CommonOuter", bound)
        val specificOuter = parameter("SpecificOuter", bound)
        val common = parameter("T", ExtendTestFixtures.typeParameterType(commonOuter).toCfirResolvedTypeRef(null))
        val specific = parameter("U", ExtendTestFixtures.typeParameterType(specificOuter).toCfirResolvedTypeRef(null))
        val onlyOwn = substitutorByMap((mapOf(common to specific)).entries.associate { (origin, target) -> origin.symbol to target.symbol.constructType() }, session)
        val complete = substitutorByMap((mapOf(commonOuter to specificOuter, common to specific)).entries.associate { (origin, target) -> origin.symbol to target.symbol.constructType() }, session)
        assertEquals(listOf(0), incompatible(specific, listOf(parent(common, onlyOwn))))
        assertEquals(emptyList<Int>(), incompatible(specific, listOf(parent(common, complete))))
    }

    @Test
    fun childProjectionMakesGeneralAndFixedBoundsOfTheSameDeclarationEqual() {
        val generalBound = bound.coneType
        // provider 解析和 fixed 头必须证明到同一声明，不能仅凭 ClassId 授权投影。
        assertSame(boundDeclaration.symbol, generalBound.toSymbol(session))
        assertSame(boundDeclaration.symbol, fixedBound.toSymbol(session))
        with(session.typeContext) {
            assertFalse(areEqualTypeConstructors(generalBound.typeConstructor(), fixedBound.typeConstructor()))
        }
        assertFalse(AbstractTypeChecker.equalTypes(session.typeContext, generalBound, fixedBound))

        val childSubstitutor = projection(generalBound to fixedBound)
        val projected = childSubstitutor.substituteOrSelf(generalBound)
        assertSame(boundDeclaration.symbol, projected.toSymbol(session))
        assertTrue(AbstractTypeChecker.equalTypes(session.typeContext, projected, fixedBound))

        val child = parameter("Child", bound)
        val common = parameter("Common", fixedBound.toCfirResolvedTypeRef(null))
        assertEquals(listOf(0), incompatible(child, listOf(parent(common))))
        assertEquals(emptyList<Int>(), incompatible(child, listOf(parent(common)), childSubstitutor))
    }

    @Test
    fun childProjectionDoesNotEquateDifferentBoundDeclarationsWithTheSameClassId() {
        val unrelatedDeclaration = ExtendTestFixtures.newInterface(session.moduleData, "Bound", boundId)
        val unrelatedBound = ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(boundId, unrelatedDeclaration.symbol), isInterface = true)
        assertEquals(boundDeclaration.symbol.classId, unrelatedDeclaration.symbol.classId)
        assertNotSame(boundDeclaration, unrelatedDeclaration)
        assertNotSame(boundDeclaration.symbol, unrelatedDeclaration.symbol)
        assertSame(unrelatedDeclaration.symbol, unrelatedBound.toSymbol(session))
        with(session.typeContext) {
            assertFalse(areEqualTypeConstructors(fixedBound.typeConstructor(), unrelatedBound.typeConstructor()))
        }
        assertFalse(AbstractTypeChecker.equalTypes(session.typeContext, fixedBound, unrelatedBound))

        val childSubstitutor = projection(bound.coneType to fixedBound)
        assertFalse(AbstractTypeChecker.equalTypes(session.typeContext, childSubstitutor.substituteOrSelf(bound.coneType), unrelatedBound))
        val child = parameter("Child", bound)
        val common = parameter("Common", unrelatedBound.toCfirResolvedTypeRef(null))
        assertEquals(listOf(0), incompatible(child, listOf(parent(common)), childSubstitutor))
    }

    @Test
    fun childProjectionUsesTheCompleteTransitiveOuterBounds() {
        val outer = parameter("SpecificOuter", bound)
        val outerType = outer.symbol.constructType()
        val middle = parameter("Middle", outerType.toCfirResolvedTypeRef(null))
        val middleType = middle.symbol.constructType()
        val child = parameter("Child", middleType.toCfirResolvedTypeRef(null))
        val common = parameter("Common", fixedBound.toCfirResolvedTypeRef(null))
        // 用真实 constructor 映射隔离收集顺序，不创建替身 TP，也不读取映射目标的 bounds。
        val childSubstitutor = projection(middleType to fixedBound, outerType to fixedBound, bound.coneType to fixedBound)
        assertEquals(listOf(0), incompatible(child, listOf(parent(common))))
        assertEquals(emptyList<Int>(), incompatible(child, listOf(parent(common)), childSubstitutor))
    }

    @Test
    fun childProjectionCannotDiscardASecondTransitiveOuterBound() {
        val otherId = ClassId.topLevel(FqName("test.OtherBound"))
        val otherDeclaration = ExtendTestFixtures.newInterface(session.moduleData, "OtherBound", otherId)
        with(CallResolutionTestFixtures) {
            session.registerHierarchyProviders(listOf(boundDeclaration, otherDeclaration))
        }
        val otherBound = ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(otherId, otherDeclaration.symbol), isInterface = true)
        val outer = parameter("SpecificOuter", bound, otherBound.toCfirResolvedTypeRef(null))
        val outerType = outer.symbol.constructType()
        val middle = parameter("Middle", outerType.toCfirResolvedTypeRef(null))
        val middleType = middle.symbol.constructType()
        val child = parameter("Child", middleType.toCfirResolvedTypeRef(null))
        val sameDomain = parameter("Same", fixedBound.toCfirResolvedTypeRef(null), otherBound.toCfirResolvedTypeRef(null))
        val widerDomain = parameter("Wider", fixedBound.toCfirResolvedTypeRef(null))
        // 若先把 Middle/Outer 换成 Bound 再遍历，OtherBound 会丢失，严格收窄将被误接受。
        val childSubstitutor = projection(middleType to fixedBound, outerType to fixedBound, bound.coneType to fixedBound)
        assertEquals(emptyList<Int>(), incompatible(child, listOf(parent(sameDomain)), childSubstitutor))
        assertEquals(listOf(0), incompatible(child, listOf(parent(widerDomain)), childSubstitutor))
        assertEquals(listOf(0), incompatible(child, listOf(parent(sameDomain), parent(widerDomain)), childSubstitutor))
    }

    @Test
    fun childProjectionStillRejectsNarrowingAnUnrestrictedParent() {
        val child = parameter("Child", bound)
        val sameDomain = parameter("Same", fixedBound.toCfirResolvedTypeRef(null))
        val widerDomain = parameter("Wider")
        val childSubstitutor = projection(bound.coneType to fixedBound)
        assertEquals(emptyList<Int>(), incompatible(child, listOf(parent(sameDomain)), childSubstitutor))
        assertEquals(listOf(0), incompatible(child, listOf(parent(sameDomain), parent(widerDomain)), childSubstitutor))
    }

    @Test
    fun transitiveBoundsKeepTheDomainsOfSameNamedOuterParametersInTwoSessions() {
        val otherSession = CallResolutionTestFixtures.newTestSession()
        val otherDeclaration = ExtendTestFixtures.newInterface(otherSession.moduleData, "Bound", boundId)
        with(CallResolutionTestFixtures) {
            otherSession.registerHierarchyProviders(listOf(otherDeclaration))
        }
        // 同一个 general Cone 在两个真实 provider 中绑定不同声明，来源必须随贡献者传递。
        val sharedType = bound.coneType
        val firstReference = sharedType.toCfirResolvedTypeRef(null)
        val secondReference = sharedType.toCfirResolvedTypeRef(null)
        val firstOuter = parameter("Outer", firstReference)
        val secondOuter = ExtendTestFixtures.newTypeParameter(otherSession.moduleData, "Outer", listOf(secondReference))
        val child = parameter("Child", firstOuter.symbol.constructType().toCfirResolvedTypeRef(null), secondOuter.symbol.constructType().toCfirResolvedTypeRef(null))
        val captured = CfirGenericConstraintBounds.capture(listOf(child))
        val records = captured.closedBounds(child.symbol)
        val first = records.single { it.owner === firstOuter.symbol }
        val second = records.single { it.owner === secondOuter.symbol }

        assertEquals(4, records.size)
        assertEquals(firstOuter.name, secondOuter.name)
        assertNotSame(firstOuter.symbol, secondOuter.symbol)
        assertSame(firstOuter, first.declaration)
        assertSame(secondOuter, second.declaration)
        assertSame(firstOuter.symbol, first.containingDeclarationSymbol)
        assertSame(secondOuter.symbol, second.containingDeclarationSymbol)
        assertSame(firstReference, first.reference)
        assertSame(secondReference, second.reference)
        assertSame(sharedType, first.originalRootType)
        assertSame(sharedType, second.originalRootType)
        assertSame(sharedType, first.originalType)
        assertSame(sharedType, second.originalType)
        assertSame(sharedType, first.type)
        assertSame(sharedType, second.type)
        assertSame(session, first.domain)
        assertSame(otherSession, second.domain)
        assertNotSame(first.domain, second.domain)
        assertSame(boundDeclaration.symbol, first.originalType.toSymbol(first.domain))
        assertSame(otherDeclaration.symbol, second.originalType.toSymbol(second.domain))
        assertNotSame(boundDeclaration.symbol, otherDeclaration.symbol)
    }

    @Test
    fun aSharedConeAndReferenceStillProduceOneRecordForEachContributingOwner() {
        val sharedReference = bound.coneType.toCfirResolvedTypeRef(null)
        val firstOuter = parameter("FirstOuter", sharedReference)
        val secondOuter = parameter("SecondOuter", sharedReference)
        val child = parameter("Child", firstOuter.symbol.constructType().toCfirResolvedTypeRef(null), secondOuter.symbol.constructType().toCfirResolvedTypeRef(null))
        val records = CfirGenericConstraintBounds.capture(listOf(child)).closedBounds(child.symbol)
        val contributions = records.filter { it.reference === sharedReference }

        assertEquals(4, records.size)
        assertEquals(2, contributions.size)
        assertNotSame(contributions[0], contributions[1])
        assertEquals(setOf(firstOuter.symbol, secondOuter.symbol), contributions.map { it.owner }.toSet())
        assertEquals(setOf(firstOuter, secondOuter), contributions.map { it.declaration }.toSet())
        contributions.forEach {
            assertSame(sharedReference, it.reference)
            assertSame(sharedReference.coneType, it.originalRootType)
            assertSame(sharedReference.coneType, it.originalType)
            assertSame(sharedReference.coneType, it.type)
            assertSame(session, it.domain)
        }
    }

    @Test
    fun originalTypeParameterCyclesHaveAFiniteClosureWithoutDroppingTheirEdges() {
        val first = parameter("First")
        val second = parameter("Second")
        val firstType = first.symbol.constructType()
        val secondType = second.symbol.constructType()
        val firstToSecond = secondType.toCfirResolvedTypeRef(null)
        val secondToFirst = firstType.toCfirResolvedTypeRef(null)
        first.replaceBounds(listOf(firstToSecond, bound))
        second.replaceBounds(listOf(secondToFirst, fixedBound.toCfirResolvedTypeRef(null)))
        // 内部真实 TP 图的环用于检验 visited 合同，不作为新增仓颉语法准入的证据。
        val captured = CfirGenericConstraintBounds.capture(listOf(first))
        val records = captured.closedBounds(first.symbol)
        assertEquals(4, records.size)
        assertEquals(2, records.count { it.owner === first.symbol })
        assertEquals(2, records.count { it.owner === second.symbol })
        assertSame(secondType, records.single { it.reference === firstToSecond }.originalType)
        assertSame(firstType, records.single { it.reference === secondToFirst }.originalType)
        assertEquals(records.toSet(), captured.closedBounds(second.symbol).toSet())

        val mapped = captured.mapTypes { fixedBound }
        val mappedRecords = mapped.closedBounds(first.symbol)
        assertEquals(4, mappedRecords.size)
        records.zip(mappedRecords).forEach { (original, derived) ->
            assertOriginalBoundRetained(original, derived)
            assertSame(fixedBound, derived.type)
        }
    }

    @Test
    fun intersectionLeavesKeepTheirOriginalRootAndImmutableContainers() {
        val generalType = bound.coneType
        val root = ConeIntersectionType(listOf(generalType, ConeIntersectionType(listOf(fixedBound))))
        val reference = root.toCfirResolvedTypeRef(null)
        val owner = parameter("Owner", reference)
        val parameters = mutableListOf(owner)
        val captured = CfirGenericConstraintBounds.capture(parameters)
        parameters.clear()
        captured.requireParameters(listOf(owner))
        val records = captured.closedBounds(owner.symbol)

        assertEquals(2, records.size)
        assertSame(generalType, records[0].originalType)
        assertSame(fixedBound, records[1].originalType)
        records.forEach {
            assertSame(owner.symbol, it.owner)
            assertSame(owner, it.declaration)
            assertSame(reference, it.reference)
            assertSame(root, it.originalRootType)
            assertSame(session, it.domain)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (records as MutableList<CfirGenericConstraintBound>).clear()
        }
        val mappedRecords = captured.mapTypes { fixedBound }.closedBounds(owner.symbol)
        assertEquals(2, mappedRecords.size)
        records.zip(mappedRecords).forEach { (original, derived) -> assertOriginalBoundRetained(original, derived) }
        assertSame(reference, owner.bounds.single())
        assertSame(root, reference.coneType)
    }

    @Test
    fun derivedViewsCollectAllOriginalTransitiveBoundsBeforeConversionAndKeepMultiParentRules() {
        val otherId = ClassId.topLevel(FqName("test.OtherBound"))
        val otherDeclaration = ExtendTestFixtures.newInterface(session.moduleData, "OtherBound", otherId)
        with(CallResolutionTestFixtures) {
            session.registerHierarchyProviders(listOf(boundDeclaration, otherDeclaration))
        }
        val otherType = ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(otherId, otherDeclaration.symbol), isInterface = true)
        val outer = parameter("Outer", bound, otherType.toCfirResolvedTypeRef(null))
        val outerType = outer.symbol.constructType()
        val middle = parameter("Middle", outerType.toCfirResolvedTypeRef(null))
        val middleType = middle.symbol.constructType()
        val child = parameter("Child", middleType.toCfirResolvedTypeRef(null))
        val captured = CfirGenericConstraintBounds.capture(listOf(child))
        val records = captured.closedBounds(child.symbol)
        val childSubstitutor = projection(middleType to fixedBound, outerType to fixedBound, bound.coneType to fixedBound)
        val visited = mutableListOf<CfirGenericConstraintBound>()
        val mapped = captured.mapTypes {
            visited += it
            childSubstitutor.substituteOrSelf(it.type)
        }
        val derivedRecords = mapped.closedBounds(child.symbol)

        assertEquals(4, records.size)
        assertEquals(4, visited.size)
        assertEquals(records.toSet(), visited.toSet())
        assertEquals(4, derivedRecords.size)
        records.zip(derivedRecords).forEach { (original, derived) -> assertOriginalBoundRetained(original, derived) }
        assertEquals(3, derivedRecords.count { it.type === fixedBound })
        assertSame(otherType, derivedRecords.single { it.originalType === otherType }.type)
        captured.closedBounds(child.symbol).forEach { assertSame(it.originalType, it.type) }
        assertSame(middleType, checkNotNull(child.bounds.single().coneTypeOrNull))
        assertSame(outerType, checkNotNull(middle.bounds.single().coneTypeOrNull))
        assertSame(bound, outer.bounds[0])
        val secondView = mapped.mapTypes { it.type }
        records.zip(secondView.closedBounds(child.symbol)).forEach { (original, derived) -> assertOriginalBoundRetained(original, derived) }

        val sameDomain = parameter("Same", fixedBound.toCfirResolvedTypeRef(null), otherType.toCfirResolvedTypeRef(null))
        val widerDomain = parameter("Wider", fixedBound.toCfirResolvedTypeRef(null))
        fun compare(parents: List<CfirGenericConstraintParent>): List<Int> =
            CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(child), parents, session.typeContext, ConeSubstitutor.Empty, mapped)
        assertEquals(emptyList<Int>(), compare(listOf(parent(sameDomain))))
        assertEquals(listOf(0), compare(listOf(parent(widerDomain))))
        assertEquals(listOf(0), compare(listOf(parent(sameDomain), parent(widerDomain))))
    }

    @Test
    fun replacingATransitiveBoundReferenceInvalidatesOriginalAndDerivedInputs() {
        val outer = parameter("Outer", bound)
        val child = parameter("Child", outer.symbol.constructType().toCfirResolvedTypeRef(null))
        val captured = CfirGenericConstraintBounds.capture(listOf(child))
        val mapped = captured.mapTypes { it.type }
        val recorded = captured.closedBounds(child.symbol).single { it.owner === outer.symbol }
        val common = parameter("Common", bound)
        val parents = listOf(parent(common))
        val capturedParent = parent(outer)
        val validChild = CfirGenericConstraintBounds.capture(listOf(common))
        // 即使 Cone 完全相同，新 TypeRef 也不是 capture 时的已完成产品。
        val replacement = bound.coneType.toCfirResolvedTypeRef(null)
        assertSame(recorded.originalRootType, replacement.coneType)
        assertNotSame(recorded.reference, replacement)
        outer.replaceBounds(listOf(replacement))

        assertSame(bound, recorded.reference)
        assertThrows(IllegalStateException::class.java) { captured.validateOriginalInput() }
        assertThrows(IllegalStateException::class.java) { captured.closedBounds(child.symbol) }
        assertThrows(IllegalStateException::class.java) { mapped.closedBounds(child.symbol) }
        var conversions = 0
        assertThrows(IllegalStateException::class.java) { captured.mapTypes { conversions++; it.type } }
        assertEquals(0, conversions)
        assertThrows(IllegalStateException::class.java) {
            CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(child), parents, session.typeContext, ConeSubstitutor.Empty, mapped)
        }
        assertThrows(IllegalStateException::class.java) {
            CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(common), listOf(capturedParent), session.typeContext, ConeSubstitutor.Empty, validChild)
        }
    }

    @Test
    @OptIn(CfirImplementationDetail::class)
    fun rebindingAnOriginalOwnerInvalidatesTheCapturedGraphInsteadOfReadingItsNewBounds() {
        val outer = parameter("Outer", bound)
        val child = parameter("Child", outer.symbol.constructType().toCfirResolvedTypeRef(null))
        val captured = CfirGenericConstraintBounds.capture(listOf(child))
        val mapped = captured.mapTypes { it.type }
        val recorded = captured.closedBounds(child.symbol).single { it.owner === outer.symbol }
        val common = parameter("Common", bound)
        val parents = listOf(parent(common))
        // 只变更已存在的真实符号绑定；没有造替代 TP 或用新声明的 bounds 证明类型等价。
        outer.symbol.bind(child)

        assertSame(child, outer.symbol.cfir)
        assertSame(outer, recorded.declaration)
        assertSame(bound, recorded.reference)
        assertThrows(IllegalStateException::class.java) { captured.validateOriginalInput() }
        assertThrows(IllegalStateException::class.java) { mapped.closedBounds(child.symbol) }
        assertThrows(IllegalStateException::class.java) { mapped.mapTypes { it.type } }
        assertThrows(IllegalStateException::class.java) {
            CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(child), parents, session.typeContext, ConeSubstitutor.Empty, mapped)
        }
    }

    @Test
    @OptIn(CfirImplementationDetail::class)
    fun rebindingAContainingDeclarationInvalidatesInputsWhileTypeParameterAndReferencesStayUnchanged() {
        val ownerId = ClassId.topLevel(FqName("test.GenericOwner"))
        val containingDeclaration = ExtendTestFixtures.newClass(session.moduleData, "GenericOwner", ownerId)
        // 使用实际生成 builder 与真实 class owner 构造原 TP，不引入声明替身或修改旧 fixture phase。
        val originalParameter = buildTypeParameter {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            containingDeclarationSymbol = containingDeclaration.symbol
            symbol = CfirTypeParameterSymbol()
            name = Name.identifier("Outer")
            bounds += bound
        }
        containingDeclaration.typeParameters += originalParameter
        val child = parameter("Child", originalParameter.symbol.constructType().toCfirResolvedTypeRef(null))
        val captured = CfirGenericConstraintBounds.capture(listOf(child))
        val records = captured.closedBounds(child.symbol)
        val recorded = records.single { it.owner === originalParameter.symbol }
        assertSame(originalParameter, originalParameter.symbol.cfir)
        assertSame(containingDeclaration, originalParameter.containingDeclarationSymbol.cfir)
        assertSame(containingDeclaration.symbol, recorded.containingDeclarationSymbol)
        assertSame(bound, recorded.reference)
        captured.validateOriginalInput()
        val mapped = captured.mapTypes { it.type }
        val mappedRecords = mapped.closedBounds(child.symbol)
        assertEquals(records.size, mappedRecords.size)
        records.zip(mappedRecords).forEach { (original, derived) -> assertOriginalBoundRetained(original, derived) }
        val mappedRecord = mappedRecords.single { it.owner === originalParameter.symbol }
        assertSame(containingDeclaration.symbol, mappedRecord.containingDeclarationSymbol)
        assertSame(containingDeclaration, mappedRecord.containingDeclarationSymbol.cfir)
        val common = parameter("Common", bound)
        val parents = listOf(parent(common))
        val capturedParent = parent(originalParameter)
        val validChild = CfirGenericConstraintBounds.capture(listOf(common))
        val replacement = ExtendTestFixtures.newClass(
            session.moduleData, "GenericOwner", ownerId, typeParameters = listOf(originalParameter),
        )
        assertNotSame(containingDeclaration, replacement)
        // 仅替换真实外层符号的绑定对象；TP 自身绑定、外层符号引用、原 refs/root 均未改变。
        containingDeclaration.symbol.bind(replacement)

        assertSame(originalParameter, originalParameter.symbol.cfir)
        assertSame(containingDeclaration.symbol, originalParameter.containingDeclarationSymbol)
        assertSame(bound, originalParameter.bounds.single())
        assertSame(recorded.originalRootType, bound.coneType)
        assertSame(session, originalParameter.moduleData.session)
        assertSame(containingDeclaration.symbol, mappedRecord.containingDeclarationSymbol)
        assertSame(replacement, mappedRecord.containingDeclarationSymbol.cfir)
        val failure = assertThrows(IllegalStateException::class.java) { captured.validateOriginalInput() }
        assertEquals("Original containing declaration changed after bound capture", failure.message)
        assertThrows(IllegalStateException::class.java) { captured.closedBounds(child.symbol) }
        assertThrows(IllegalStateException::class.java) { mapped.closedBounds(child.symbol) }
        assertThrows(IllegalStateException::class.java) { mapped.requireParameters(listOf(child)) }
        var conversions = 0
        assertThrows(IllegalStateException::class.java) { mapped.mapTypes { conversions++; it.type } }
        assertEquals(0, conversions)
        assertThrows(IllegalStateException::class.java) {
            CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(child), parents, session.typeContext, ConeSubstitutor.Empty, mapped)
        }
        assertThrows(IllegalStateException::class.java) {
            CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(common), listOf(capturedParent), session.typeContext, ConeSubstitutor.Empty, validChild)
        }
    }

    @Test
    fun capturedInputsRequireTheOriginalParametersInTheirOriginalPositions() {
        val first = parameter("T", bound)
        val second = parameter("T", bound)
        val captured = CfirGenericConstraintBounds.capture(listOf(first, second))
        captured.requireParameters(listOf(first, second))
        assertThrows(IllegalStateException::class.java) { captured.requireParameters(listOf(second, first)) }
        assertThrows(IllegalStateException::class.java) { captured.requireParameters(listOf(first)) }
        val firstOnly = CfirGenericConstraintBounds.capture(listOf(first))
        assertThrows(IllegalStateException::class.java) { firstOnly.closedBounds(second.symbol) }
        assertThrows(IllegalStateException::class.java) {
            CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(second), listOf(parent(first)), session.typeContext, ConeSubstitutor.Empty, firstOnly)
        }
    }

    @Test
    fun originalAnyAndErrorLeavesRemainRecordedButDoNotBecomeConstraintsAfterMapping() {
        val errorType = ConeErrorType(ConeSimpleDiagnostic("original erroneous bound"))
        val anyReference = ConeAnyType.toCfirResolvedTypeRef(null)
        val errorReference = errorType.toCfirResolvedTypeRef(null)
        val child = parameter("Child", anyReference, errorReference)
        val captured = CfirGenericConstraintBounds.capture(listOf(child))
        val records = captured.closedBounds(child.symbol)
        val mapped = captured.mapTypes { fixedBound }
        val derived = mapped.closedBounds(child.symbol)

        assertEquals(2, records.size)
        assertSame(ConeAnyType, records.single { it.reference === anyReference }.originalType)
        assertSame(errorType, records.single { it.reference === errorReference }.originalType)
        assertEquals(2, derived.size)
        records.zip(derived).forEach { (original, view) ->
            assertOriginalBoundRetained(original, view)
            assertSame(fixedBound, view.type)
        }
        // 显式转换仅隔离“按原叶过滤”的合同，不把错误类型宣称为已完成 classifier 绑定。
        val unrestricted = parameter("Unrestricted")
        assertEquals(emptyList<Int>(), CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(
            listOf(child), listOf(parent(unrestricted)), session.typeContext, ConeSubstitutor.Empty, mapped,
        ))
        val constrained = parameter("Constrained", fixedBound.toCfirResolvedTypeRef(null))
        val mappedParent = CfirGenericConstraintParent(listOf(child), ConeSubstitutor.Empty, mapped)
        assertEquals(listOf(0), CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(
            listOf(constrained), listOf(mappedParent), session.typeContext, ConeSubstitutor.Empty, CfirGenericConstraintBounds.capture(listOf(constrained)),
        ))
    }

    @Test
    fun replacingAnIntersectionComponentWithAnEqualNewObjectInvalidatesOriginalAndDerivedInputs() {
        for (nested in listOf(false, true)) {
            assertIntersectionMutationRejected(nested, "Original bound occurrence changed after input capture") { members ->
                val replacement = ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(boundId, boundDeclaration.symbol), isInterface = true)
                assertEquals(members[0], replacement)
                assertNotSame(members[0], replacement)
                members[0] = replacement
            }
        }
    }

    @Test
    fun reorderingIntersectionComponentsInvalidatesOriginalAndDerivedInputs() {
        for (nested in listOf(false, true)) {
            assertIntersectionMutationRejected(nested, "Original bound occurrence changed after input capture") { members ->
                val first = members[0]
                members[0] = members[1]
                members[1] = first
            }
        }
    }

    @Test
    fun deletingAnIntersectionComponentInvalidatesOriginalAndDerivedInputs() {
        for (nested in listOf(false, true)) {
            assertIntersectionMutationRejected(nested, "Original intersection shape changed after input capture") { members ->
                members.removeAt(0)
            }
        }
    }

    @Test
    fun wrappingTheSameLeafInANewIntersectionInvalidatesTheOriginalHierarchy() {
        for (nested in listOf(false, true)) {
            assertIntersectionMutationRejected(nested, "Original bound occurrence changed after input capture") { members ->
                // 展平后的原 leaf 对象完全相同，新增层级仍改变 capture 时的原槽结构。
                members[0] = ConeIntersectionType(listOf(members[0]))
            }
        }
    }

    @Test
    fun repeatedLeafOccurrencesKeepDistinctBoundOrdinalsAndComponentPathsInDerivedViews() {
        val sharedLeaf = fixedBound
        val root = ConeIntersectionType(listOf(sharedLeaf, ConeIntersectionType(listOf(sharedLeaf, sharedLeaf)), sharedLeaf))
        val rootReference = root.toCfirResolvedTypeRef(null)
        val leafReference = sharedLeaf.toCfirResolvedTypeRef(null)
        // 同一个原 TypeRef 重复占据两个 bound 槽，同一个 Cone 重复占据嵌套 child 槽。
        val owner = parameter("Owner", rootReference, rootReference, leafReference)
        val captured = CfirGenericConstraintBounds.capture(listOf(owner))
        val records = captured.closedBounds(owner.symbol)
        val rootPaths = listOf(listOf(0), listOf(1, 0), listOf(1, 1), listOf(2))
        val expectedOccurrences = (0..1).flatMap { ordinal -> rootPaths.map { ordinal to it } } + (2 to emptyList<Int>())
        assertEquals(9, records.size)
        assertEquals(expectedOccurrences, records.map { it.boundOrdinal to it.componentPath })
        assertEquals(9, records.map { it.boundOrdinal to it.componentPath }.toSet().size)
        records.forEach {
            assertSame(sharedLeaf, it.originalType)
            assertSame(owner.symbol, it.owner)
            assertSame(owner, it.declaration)
            if (it.boundOrdinal < 2) {
                assertSame(rootReference, it.reference)
                assertSame(root, it.originalRootType)
            } else {
                assertSame(leafReference, it.reference)
                assertSame(sharedLeaf, it.originalRootType)
            }
            assertThrows(UnsupportedOperationException::class.java) { (it.componentPath as MutableList<Int>).add(99) }
        }
        val mapped = captured.mapTypes { bound.coneType }
        val derived = mapped.closedBounds(owner.symbol)
        assertEquals(expectedOccurrences, derived.map { it.boundOrdinal to it.componentPath })
        records.zip(derived).forEach { (original, view) ->
            assertBoundOccurrenceRetained(original, view)
            assertSame(bound.coneType, view.type)
            assertThrows(UnsupportedOperationException::class.java) { (view.componentPath as MutableList<Int>).add(99) }
        }
        assertEquals(expectedOccurrences, captured.closedBounds(owner.symbol).map { it.boundOrdinal to it.componentPath })
    }

    @Test
    fun captureExplicitlyRejectsSelfAndMutualStructuralIntersectionCycles() {
        val selfMembers = mutableListOf<ConeCangJieType>(fixedBound)
        val selfRoot = ConeIntersectionType(selfMembers)
        selfMembers += selfRoot
        val firstMembers = mutableListOf<ConeCangJieType>(fixedBound)
        val firstRoot = ConeIntersectionType(firstMembers)
        val secondRoot = ConeIntersectionType(listOf(firstRoot))
        firstMembers += secondRoot
        // collection 构造后注入结构环；与 leaf lookupTag 指向真实 TP 的上界边不同。
        for (root in listOf(selfRoot, firstRoot)) {
            val reference = root.toCfirResolvedTypeRef(null)
            val owner = parameter("Owner", reference)
            assertSame(owner, owner.symbol.cfir)
            assertSame(reference, owner.bounds.single())
            assertSame(root, reference.coneType)
            val failure = assertThrows(IllegalStateException::class.java) { CfirGenericConstraintBounds.capture(listOf(owner)) }
            assertEquals("Original bounds contain a structural intersection cycle", failure.message)
        }
    }

    @Test
    fun directTypeParameterCyclesRemainFiniteWithEmptyComponentPathsAfterShapeCapture() {
        val first = parameter("First")
        val second = parameter("Second")
        val firstType = first.symbol.constructType()
        val secondType = second.symbol.constructType()
        first.replaceBounds(listOf(secondType.toCfirResolvedTypeRef(null), bound))
        second.replaceBounds(listOf(firstType.toCfirResolvedTypeRef(null), fixedBound.toCfirResolvedTypeRef(null)))
        val captured = CfirGenericConstraintBounds.capture(listOf(first))
        val records = captured.closedBounds(first.symbol)
        assertEquals(4, records.size)
        assertEquals(listOf(0, 1, 0, 1), records.map { it.boundOrdinal })
        assertTrue(records.all { it.componentPath.isEmpty() })
        assertSame(secondType, records.single { it.owner === first.symbol && it.boundOrdinal == 0 }.originalType)
        assertSame(firstType, records.single { it.owner === second.symbol && it.boundOrdinal == 0 }.originalType)
        assertEquals(records.toSet(), captured.closedBounds(second.symbol).toSet())
        val mapped = captured.mapTypes { fixedBound }
        val derived = mapped.closedBounds(first.symbol)
        assertEquals(4, derived.size)
        records.zip(derived).forEach { (original, view) ->
            assertBoundOccurrenceRetained(original, view)
            assertSame(fixedBound, view.type)
        }
        captured.validateOriginalInput()
        mapped.validateOriginalInput()
    }

    /** 保留 TP 绑定、原 TypeRef 和 root 对象，只变更 root 或嵌套 intersection 的 backing collection。 */
    private fun assertIntersectionMutationRejected(
        nested: Boolean,
        expectedMessage: String,
        mutate: (MutableList<ConeCangJieType>) -> Unit,
    ) {
        val members = mutableListOf<ConeCangJieType>(fixedBound, bound.coneType)
        val intersection = ConeIntersectionType(members)
        val root = if (nested) ConeIntersectionType(listOf(bound.coneType, intersection)) else intersection
        val reference = root.toCfirResolvedTypeRef(null)
        val owner = parameter("Owner", reference)
        val captured = CfirGenericConstraintBounds.capture(listOf(owner))
        val records = captured.closedBounds(owner.symbol)
        captured.validateOriginalInput()
        val mapped = captured.mapTypes { it.type }
        val derived = mapped.closedBounds(owner.symbol)
        assertEquals(records.size, derived.size)
        records.zip(derived).forEach { (original, view) -> assertBoundOccurrenceRetained(original, view) }
        val common = parameter("Common", bound)
        val parents = listOf(parent(common))
        val capturedParent = CfirGenericConstraintParent(listOf(owner), ConeSubstitutor.Empty, mapped)
        val validChild = CfirGenericConstraintBounds.capture(listOf(common))
        mutate(members)

        assertSame(owner, owner.symbol.cfir)
        assertSame(owner.symbol, owner.containingDeclarationSymbol)
        assertSame(reference, owner.bounds.single())
        assertSame(root, reference.coneType)
        assertSame(session, owner.moduleData.session)
        assertSame(members, intersection.intersectedTypes)
        records.forEach {
            assertSame(owner, it.declaration)
            assertSame(reference, it.reference)
            assertSame(root, it.originalRootType)
        }
        for (input in listOf(captured, mapped)) {
            val failure = assertThrows(IllegalStateException::class.java) { input.validateOriginalInput() }
            assertEquals(expectedMessage, failure.message)
            assertThrows(IllegalStateException::class.java) { input.closedBounds(owner.symbol) }
            assertThrows(IllegalStateException::class.java) { input.requireParameters(listOf(owner)) }
            var conversions = 0
            assertThrows(IllegalStateException::class.java) { input.mapTypes { conversions++; it.type } }
            assertEquals(0, conversions)
            assertThrows(IllegalStateException::class.java) {
                CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(owner), parents, session.typeContext, ConeSubstitutor.Empty, input)
            }
        }
        assertThrows(IllegalStateException::class.java) {
            CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(listOf(common), listOf(capturedParent), session.typeContext, ConeSubstitutor.Empty, validChild)
        }
    }

    /** occurrence 地址随视图保留，既不重编号原 bound 槽，也不重建原成分路径。 */
    private fun assertBoundOccurrenceRetained(original: CfirGenericConstraintBound, derived: CfirGenericConstraintBound) {
        assertOriginalBoundRetained(original, derived)
        assertEquals(original.boundOrdinal, derived.boundOrdinal)
        assertEquals(original.componentPath, derived.componentPath)
    }

    /** 派生记录仅更换比较 type，所有原来源字段必须保持引用身份。 */
    private fun assertOriginalBoundRetained(original: CfirGenericConstraintBound, derived: CfirGenericConstraintBound) {
        assertNotSame(original, derived)
        assertSame(original.owner, derived.owner)
        assertSame(original.declaration, derived.declaration)
        assertSame(original.declaration.containingDeclarationSymbol, original.containingDeclarationSymbol)
        assertSame(original.containingDeclarationSymbol, derived.containingDeclarationSymbol)
        assertSame(original.reference, derived.reference)
        assertSame(original.originalRootType, derived.originalRootType)
        assertSame(original.originalType, derived.originalType)
        assertSame(original.domain, derived.domain)
    }

    /** 测试映射以原类型构造器为键；目标均为真实 fixture 声明构造的类型。 */
    private fun projection(vararg mappings: Pair<ConeCangJieType, ConeCangJieType>): ConeSubstitutor =
        createTypeSubstitutorByTypeConstructor(
            with(session.typeContext) { mappings.associate { (origin, target) -> origin.typeConstructor() to target } },
            session.typeContext,
            approximateIntegerLiterals = false,
        )

    private fun parameter(name: String, vararg bounds: CfirTypeRef): CfirTypeParameter =
        ExtendTestFixtures.newTypeParameter(session.moduleData, name, bounds.toList())

    private fun parent(parameter: CfirTypeParameter, substitutor: ConeSubstitutor = ConeSubstitutor.Empty) =
        CfirGenericConstraintParent(listOf(parameter), substitutor, CfirGenericConstraintBounds.capture(listOf(parameter)))

    private fun incompatible(
        child: CfirTypeParameter,
        parents: List<CfirGenericConstraintParent>,
        childSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
    ): List<Int> =
        CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(
            listOf(child), parents, session.typeContext, childSubstitutor, CfirGenericConstraintBounds.capture(listOf(child)),
        )
}
