@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.buildThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolverImpl
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.resolve.expandedConeTypeWithEnsuredPhase
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendRuleQueryServiceImpl
import org.cangnova.cangjie.cfir.resolve.services.CfirTypeAwareSupertypeProviderImpl
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationInheritanceInput
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationInheritanceScope
import org.cangnova.cangjie.cfir.scopes.CfirFunctionInstantiationScope
import org.cangnova.cangjie.cfir.scopes.CfirInheritanceMemberKind
import org.cangnova.cangjie.cfir.scopes.CfirMemberScopeOwner
import org.cangnova.cangjie.cfir.scopes.CfirStoredFunctionMember
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpBatchFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.services.CfirExtendRuleQueryService
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 正式 scope provider 的模型输入回归；不执行诊断、cjc、CLI 或源码 parser 验收。
 *
 * 固定 K 3c7a5041f238102285764d370665f56f587f9dc4 只用于 scope/framework 对位。
 * 仓颉语义取固定 N 14276740ffec1262b81b6b5236b1d840af270b37：
 * StructInheritanceChecker.cpp 362–416、660–739、1307–1334，
 * MergeInheritedMemberHelper.cpp 139 起的逐成员更新，以及官方《直接扩展》《访问规则》《属性》。
 * 完全实例化 Box<Int64> 和 where U <: Mark 的 Box<U> 共用桶；Int64 不实现本例 Mark。
 * Fixture 只登记完整声明、Source 父图、文件与 extend 索引，所有匹配和归并均由生产服务执行。
 */
class CfirDeclarationInheritanceScopeTest {
    @Test
    fun invisiblePeerKeepsMappedParameterAndReturnWithoutCallingItsFunction() {
        val fixture = Fixture()
        val peer = fixture.genericPeer()
        val peerEcho = fixture.identityFunction("echo", peer.type, receiver = peer.type)
        val peerOnly = fixture.identityFunction("onlyPeer", peer.type, receiver = peer.type)
        (peer.owner.declarations as MutableList<CfirDeclaration>).addAll(listOf(peerEcho, peerOnly))
        val ownEcho = fixture.falseFunction("echo", fixture.intBox)
        val owner = fixture.extend(fixture.intBox, listOf(ownEcho))
        fixture.install(peer.owner, owner)
        val originals = listOf(peerEcho, peerOnly, ownEcho).map(::OriginalFunction)
        val storage = ScopeSession()

        val input = fixture.inheritance(owner, storage)
        assertSame(owner, input.owner)
        val step = input.steps.single()
        assertSame(ownEcho.symbol, step.member.declaration)
        assertEquals(ConePrimitiveType.BOOLEAN, step.member.returnType)
        assertTrue(step.visibleParents.isEmpty())
        val parent = step.invisibleParents.single()
        assertSame(peerEcho.symbol, parent.declaration)
        assertEquals(CfirInheritanceMemberKind.FUNCTION, parent.kind)
        assertSame(peer.owner, parent.physicalOwner)
        assertSame(peer.owner, parent.logicalOwner)
        assertSame(peer.owner, parent.sourceExtend)
        assertEquals(fixture.intBox, parent.structType)
        assertEquals(listOf(fixture.intBox), parent.parameterTypes)
        assertEquals(fixture.intBox, parent.returnType)
        assertEquals(listOf(Name.identifier("x")), parent.parameterNames)
        assertEquals(listOf(false), parent.namedParameters)
        assertFalse(parent.flags.isStatic)
        assertFalse(parent.flags.isAbstract)
        assertFalse(parent.flags.isInterfaceOwner)
        val stored = checkNotNull(parent.function)
        assertFalse(stored.hasOwnGeneric)
        assertEquals(fixture.intBox, stored.returnType)
        assertEquals(peer.type, peerEcho.returnTypeRef.coneTypeOrNull)
        assertEquals(peer.type, peerEcho.valueParameters.single().returnTypeRef.coneTypeOrNull)
        assertEquals(setOf(peerEcho.symbol, peerOnly.symbol), input.invisibleMembers.map { it.declaration }.toSet())
        assertEquals(listOf(ownEcho.symbol), input.visibleImplementations.map { it.declaration })
        assertTrue(input.interfaceRequirements.isEmpty())
        val peerInput = fixture.prepareHeaders(owner).peerOwners.getValue(owner to peer.owner)
        assertFalse(peerInput.visible)
        assertEquals(mapOf(peer.parameter.symbol.toLookupTag() to ConePrimitiveType.INT64), peerInput.substitution.replacements)
        val publishedScope = assertInstanceOf(CfirFunctionInstantiationScope::class.java, step.member.baseScope)
        assertEquals(listOf(ownEcho.symbol), publishedScope.publishedFunctionMemberTable(ownEcho.name).members.map { it.declaration })
        assertEquals(listOf(ownEcho.symbol), publishedScope.functionInstantiationTable(ownEcho.name).members.map { it.declaration })
        assertInvisibleFunctionNotPublished(input, peerOnly)
        fixture.assertOrdinaryFunctions(storage, listOf(ownEcho), listOf(peerEcho.name, peerOnly.name))
        originals.forEach(OriginalFunction::assertUnchanged)
    }

    @Test
    fun compatibleInvisiblePeerCannotImplementAnOwnInterfaceRequirement() {
        val fixture = Fixture()
        val peer = fixture.genericPeer()
        val peerEcho = fixture.identityFunction("echo", peer.type, receiver = peer.type)
        val peerOnly = fixture.identityFunction("onlyPeer", peer.type, receiver = peer.type)
        (peer.owner.declarations as MutableList<CfirDeclaration>).addAll(listOf(peerEcho, peerOnly))
        val requirementSymbol = CfirInterfaceSymbol(fixture.classId("Needs"))
        val requirement = fixture.abstractFunction("onlyPeer", fixture.intBox, requirementSymbol)
        val needs = fixture.interfaceDeclaration("Needs", requirementSymbol, members = listOf(requirement))
        val ownEcho = fixture.identityFunction("echo", fixture.intBox)
        val owner = fixture.extend(fixture.intBox, listOf(ownEcho), interfaces = listOf(fixture.type(needs)))
        fixture.install(peer.owner, owner)
        val originals = listOf(peerEcho, peerOnly, ownEcho).map(::OriginalFunction)
        val storage = ScopeSession()

        val input = fixture.inheritance(owner, storage)
        val step = input.steps.single()
        assertEquals(step.member.returnType, step.invisibleParents.single().returnType)
        assertEquals(step.member.parameterTypes, step.invisibleParents.single().parameterTypes)
        assertTrue(step.visibleParents.isEmpty())
        assertEquals(listOf(ownEcho.symbol), input.visibleImplementations.map { it.declaration })
        val required = input.interfaceRequirements.single()
        assertSame(requirement.symbol, required.declaration)
        assertEquals(peerOnly.name, required.name)
        assertEquals(listOf(fixture.intBox), required.parameterTypes)
        assertEquals(fixture.intBox, required.returnType)
        assertTrue(required.flags.isAbstract)
        assertTrue(required.flags.isInterfaceOwner)
        assertTrue(required.isInheritedInterface)
        assertSame(owner.superTypeRefs.single(), input.requirementSources.getValue(required))
        assertFalse(input.visibleImplementations.any { it.name == peerOnly.name })
        assertInvisibleFunctionNotPublished(input, peerOnly)
        // 普通 scope 可以看见自身接口的抽象 requirement；不能误把它算作 invisible 的实现。
        fixture.assertOrdinaryFunctions(storage, listOf(ownEcho, requirement), listOf(peerEcho.name, peerOnly.name))
        assertNull(requirement.body)
        originals.forEach(OriginalFunction::assertUnchanged)
    }

    @Test
    fun nextOwnOverloadSeesPreviousOwnAndLeavesEarlierStepFrozen() {
        val fixture = Fixture()
        val peer = fixture.genericPeer()
        val peerEcho = fixture.identityFunction("echo", peer.type, receiver = peer.type)
        (peer.owner.declarations as MutableList<CfirDeclaration>).add(peerEcho)
        val firstOwn = fixture.identityFunction("echo", fixture.intBox)
        val secondOwn = fixture.identityFunction("echo", ConePrimitiveType.BOOLEAN)
        val owner = fixture.extend(fixture.intBox, listOf(firstOwn, secondOwn))
        fixture.install(peer.owner, owner)
        val originals = listOf(peerEcho, firstOwn, secondOwn).map(::OriginalFunction)
        val storage = ScopeSession()

        val input = fixture.inheritance(owner, storage)
        assertEquals(listOf(firstOwn.symbol, secondOwn.symbol), input.steps.map { it.member.declaration })
        val firstStep = input.steps[0]
        val nextStep = input.steps[1]
        assertTrue(firstStep.visibleParents.isEmpty())
        assertEquals(listOf(peerEcho.symbol), firstStep.invisibleParents.map { it.declaration })
        assertEquals(listOf(firstOwn.symbol), nextStep.visibleParents.map { it.declaration })
        assertEquals(listOf(fixture.intBox), nextStep.visibleParents.single().parameterTypes)
        assertEquals(listOf(ConePrimitiveType.BOOLEAN), nextStep.member.parameterTypes)
        assertEquals(listOf(peerEcho.symbol), nextStep.invisibleParents.map { it.declaration })
        assertEquals(listOf(firstOwn.symbol, secondOwn.symbol), input.visibleImplementations.map { it.declaration })
        assertSame(input, fixture.inheritance(owner, storage))
        assertSame(firstStep, input.steps[0])
        assertTrue(firstStep.visibleParents.isEmpty(), "后一步 visible 更新不能反向修改前一步")
        assertThrows(UnsupportedOperationException::class.java) {
            (nextStep.visibleParents as MutableList<*>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (firstStep.invisibleParents as MutableList<*>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) { (input.steps as MutableList<*>).clear() }
        fixture.assertOrdinaryFunctions(storage, listOf(firstOwn, secondOwn), listOf(firstOwn.name))
        originals.forEach(OriginalFunction::assertUnchanged)
    }

    @Test
    fun invisiblePropertyKeepsMappedTypeAndMutabilityAsIndependentFacts() {
        // 四个真实声明组合：同类型/异类型 × mut/immut，不把 type 和 mut 压成同一个结果。
        for (sameType in listOf(true, false)) for (ownMut in listOf(true, false)) {
            val fixture = Fixture()
            val peer = fixture.genericPeer()
            val peerValue = fixture.property("value", peer.type, peer.owner.symbol, isMut = true)
            val peerOnly = fixture.property("onlyPeer", peer.type, peer.owner.symbol, isMut = false)
            (peer.owner.declarations as MutableList<CfirDeclaration>).addAll(listOf(peerValue, peerOnly))
            val owner = fixture.extend(fixture.intBox, emptyList())
            val ownType = if (sameType) fixture.intBox else ConePrimitiveType.BOOLEAN
            val ownValue = fixture.property("value", ownType, owner.symbol, ownMut)
            (owner.declarations as MutableList<CfirDeclaration>).add(ownValue)
            fixture.install(peer.owner, owner)
            val originals = listOf(peerValue, peerOnly, ownValue).map(::OriginalProperty)
            val storage = ScopeSession()

            val input = fixture.inheritance(owner, storage)
            val step = input.steps.single()
            val parent = step.invisibleParents.single()
            assertTrue(step.visibleParents.isEmpty())
            assertSame(peerValue.symbol, parent.declaration)
            assertEquals(CfirInheritanceMemberKind.PROPERTY, parent.kind)
            assertEquals(fixture.intBox, parent.structType)
            assertEquals(fixture.intBox, parent.returnType)
            assertEquals(ownType, step.member.returnType)
            assertEquals(sameType, parent.returnType == step.member.returnType)
            assertTrue(parent.flags.isMut)
            assertTrue(parent.flags.isVar)
            assertTrue(parent.flags.hasGetter)
            assertTrue(parent.flags.hasSetter)
            assertEquals(ownMut, step.member.flags.isMut)
            assertEquals(ownMut, step.member.flags.isVar)
            assertEquals(ownMut, step.member.flags.hasSetter)
            assertNull(parent.function, "属性不能被函数投影代替")
            assertTrue(parent.parameterTypes.isEmpty())
            assertEquals(peer.type, peerValue.returnTypeRef.coneTypeOrNull)
            assertEquals(listOf(ownValue.symbol), input.visibleImplementations.map { it.declaration })
            assertEquals(setOf(peerValue.symbol, peerOnly.symbol), input.invisibleMembers.map { it.declaration }.toSet())
            assertTrue(input.interfaceRequirements.isEmpty())
            val ordinary = fixture.ordinaryScope(storage)
            repeat(2) {
                val values = mutableListOf<CfirPropertySymbol>()
                ordinary.processPropertiesByName(ownValue.name, values::add)
                assertEquals(listOf(ownValue.symbol), values)
                val hidden = mutableListOf<CfirCallableSymbol<*>>()
                ordinary.processCallablesByName(peerOnly.name, hidden::add)
                assertTrue(hidden.isEmpty())
                assertEquals(setOf(ownValue.name), ordinary.getCallableNames())
            }
            assertSame(input, fixture.inheritance(owner, storage))
            originals.forEach(OriginalProperty::assertUnchanged)
        }
    }

    @Test
    fun invisibleInterfaceDefaultIsMappedWhileAbstractPeerMemberIsRemoved() {
        val fixture = Fixture()
        val interfaceSymbol = CfirInterfaceSymbol(fixture.classId("PeerDefault"))
        val parameter = fixture.parameter(interfaceSymbol, "R")
        val template = fixture.boxType(parameter.symbol.constructType())
        val interfaceReceiver = ConeClassLikeType(interfaceSymbol.toLookupTag(), listOf(parameter.symbol.constructType()), isInterface = true)
        val defaultEcho = fixture.identityFunction("echo", template, interfaceSymbol, isDefault = true, receiver = interfaceReceiver)
        val abstractOnly = fixture.abstractFunction("abstractOnly", template, interfaceSymbol, receiver = interfaceReceiver)
        val peerInterface = fixture.interfaceDeclaration("PeerDefault", interfaceSymbol,
            parameters = listOf(parameter), members = listOf(defaultEcho, abstractOnly))
        val peer = fixture.genericPeer()
        (peer.owner.superTypeRefs as MutableList<CfirTypeRef>).add(fixture.ref(fixture.type(peerInterface, peer.parameter.symbol.constructType())))
        val ownEcho = fixture.falseFunction("echo", fixture.intBox)
        val owner = fixture.extend(fixture.intBox, listOf(ownEcho))
        fixture.install(peer.owner, owner)
        val originals = listOf(defaultEcho, ownEcho).map(::OriginalFunction)
        val storage = ScopeSession()

        val input = fixture.inheritance(owner, storage)
        val parent = input.steps.single().invisibleParents.single()
        assertSame(defaultEcho.symbol, parent.declaration)
        assertEquals(listOf(fixture.intBox), parent.parameterTypes)
        assertEquals(fixture.intBox, parent.returnType)
        assertTrue(parent.flags.isDefault)
        assertTrue(parent.flags.isInterfaceOwner)
        assertSame(peer.owner, parent.sourceExtend)
        assertFalse(input.invisibleMembers.any { it.name == abstractOnly.name })
        assertEquals(listOf(ownEcho.symbol), input.visibleImplementations.map { it.declaration })
        assertTrue(input.interfaceRequirements.isEmpty())
        assertEquals(template, defaultEcho.returnTypeRef.coneTypeOrNull)
        assertNull(abstractOnly.body)
        fixture.assertOrdinaryFunctions(storage, listOf(ownEcho), listOf(defaultEcho.name, abstractOnly.name))
        assertSame(input, fixture.inheritance(owner, storage))
        originals.forEach(OriginalFunction::assertUnchanged)
    }

    @Test
    fun freshCapturedHeadersAreValueEqualAndReuseThePublicScopeProduct() {
        val fixture = Fixture()
        val peer = fixture.genericPeer()
        val peerEcho = fixture.identityFunction("echo", peer.type, receiver = peer.type)
        (peer.owner.declarations as MutableList<CfirDeclaration>).add(peerEcho)
        val ownEcho = fixture.identityFunction("echo", fixture.intBox)
        val owner = fixture.extend(fixture.intBox, listOf(ownEcho))
        fixture.install(peer.owner, owner)
        val originals = listOf(peerEcho, ownEcho).map(::OriginalFunction)
        val storage = ScopeSession()
        val first = fixture.inheritance(owner, storage)
        val ordinary = fixture.ordinaryScope(storage)
        // 直接调用现有正式 capture 入口观察 header；不在测试中重建 capture/mapping/merge。
        val captured = fixture.prepareHeaders(owner)
        val fresh = fixture.prepareHeaders(owner)
        val originalHeader = captured.owners.getValue(peer.owner).memberHeaders.getValue(peerEcho.symbol)
        val freshHeader = fresh.owners.getValue(peer.owner).memberHeaders.getValue(peerEcho.symbol)
        assertNotSame(originalHeader, freshHeader)
        assertEquals(originalHeader, freshHeader)
        assertEquals(originalHeader.hashCode(), freshHeader.hashCode())
        assertEquals(captured, fresh)
        assertEquals(captured.hashCode(), fresh.hashCode())
        repeat(3) {
            assertSame(first, fixture.inheritance(owner, storage))
            assertSame(ordinary, fixture.ordinaryScope(storage))
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (originalHeader.parameterTypes as MutableList<*>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (captured.owners.getValue(peer.owner).memberHeaders as MutableMap<*, *>).clear()
        }
        originals.forEach(OriginalFunction::assertUnchanged)

        // 同一声明身份发生合法 header 变化；旧完成输入不能重新读取活动 status。
        peerEcho.replaceStatus(fixture.status(visibility = Visibilities.Internal))
        val changed = fixture.prepareHeaders(owner).owners.getValue(peer.owner).memberHeaders.getValue(peerEcho.symbol)
        assertNotEquals(originalHeader, changed)
        assertSame(originalHeader.declaration, changed.declaration)
        assertEquals(Visibilities.Public, originalHeader.flags.visibility)
        assertEquals(Visibilities.Internal, changed.flags.visibility)
        assertEquals(Visibilities.Public, first.steps.single().invisibleParents.single().flags.visibility)
        val oldScope = assertInstanceOf(CfirDeclarationInheritanceScope::class.java, first.steps.single().member.baseScope)
        assertSame(first, oldScope.declarationInheritanceInput(owner))
        val next = fixture.inheritance(owner, storage)
        assertNotSame(first, next)
        assertEquals(Visibilities.Internal, next.steps.single().invisibleParents.single().flags.visibility)
        assertSame(next, fixture.inheritance(owner, storage))
        assertSame(originals.first().body, peerEcho.body)
    }

    @Test
    fun ownerReturnParameterBoundsStaySeparateFromFunctionOwnParameters() {
        val fixture = Fixture()
        val peer = fixture.genericPeer()
        // func echo(x: U): U { x }：U 属于 extend，而非函数自己的 Generic 节点。
        val peerEcho = fixture.identityFunction("echo", peer.parameter.symbol.constructType(), receiver = peer.type)
        (peer.owner.declarations as MutableList<CfirDeclaration>).add(peerEcho)
        val ownEcho = fixture.falseFunction("echo", ConePrimitiveType.INT64)
        val owner = fixture.extend(fixture.intBox, listOf(ownEcho))
        fixture.install(peer.owner, owner)
        val originals = listOf(peerEcho, ownEcho).map(::OriginalFunction)
        val storage = ScopeSession()

        val input = fixture.inheritance(owner, storage)
        val parent = input.steps.single().invisibleParents.single()
        assertEquals(listOf(ConePrimitiveType.INT64), parent.parameterTypes)
        assertEquals(ConePrimitiveType.INT64, parent.returnType)
        assertTrue(parent.typeParameters.isEmpty())
        assertTrue(parent.upperBounds.isEmpty())
        assertFalse(checkNotNull(parent.function).hasOwnGeneric)
        val headers = fixture.prepareHeaders(owner)
        val header = headers.owners.getValue(peer.owner).memberHeaders.getValue(peerEcho.symbol)
        assertTrue(header.typeParameters.isEmpty())
        assertTrue(header.upperBounds.isEmpty())
        assertFalse(header.hasOwnGeneric)
        assertEquals(peer.parameter.bounds.map { checkNotNull(it.coneTypeOrNull) }, header.returnUpperBounds)
        assertEquals(header.returnUpperBounds, parent.returnUpperBounds)
        assertThrows(UnsupportedOperationException::class.java) { (header.returnUpperBounds as MutableList<*>).clear() }
        assertThrows(UnsupportedOperationException::class.java) { (parent.returnUpperBounds as MutableList<*>).clear() }
        assertSame(input, fixture.inheritance(owner, storage))
        originals.forEach(OriginalFunction::assertUnchanged)
    }

    @Test
    fun returnParameterClosureMapsTransitiveOwnerBoundsWithoutAddingFunctionParameters() {
        val fixture = Fixture()
        val symbol = CfirExtendSymbol()
        // extend<U, V> Box<(U, V)> where U <: V, V <: Mark，两个 owner 参数均用于 target。
        val v = fixture.parameter(symbol, "V", listOf(fixture.markType))
        val u = fixture.parameter(symbol, "U", listOf(v.symbol.constructType()))
        val template = fixture.boxType(ConeTupleType(listOf(u.symbol.constructType(), v.symbol.constructType())))
        val peerEcho = fixture.identityFunction("echo", u.symbol.constructType(), receiver = template)
        val peer = fixture.extend(template, listOf(peerEcho), symbol, listOf(u, v))
        val concrete = fixture.boxType(ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN)))
        val ownEcho = fixture.falseFunction("echo", ConePrimitiveType.INT64, receiver = concrete)
        val owner = fixture.extend(concrete, listOf(ownEcho))
        fixture.install(peer, owner)
        val originals = listOf(peerEcho, ownEcho).map(::OriginalFunction)

        val input = fixture.inheritance(owner, ScopeSession())
        val parent = input.steps.single().invisibleParents.single()
        assertEquals(listOf(ConePrimitiveType.INT64), parent.parameterTypes)
        assertEquals(ConePrimitiveType.INT64, parent.returnType)
        assertTrue(parent.typeParameters.isEmpty())
        assertTrue(parent.upperBounds.isEmpty())
        assertTrue(checkNotNull(parent.function).upperBounds.isEmpty())
        val header = fixture.prepareHeaders(owner).owners.getValue(peer).memberHeaders.getValue(peerEcho.symbol)
        assertEquals(listOf(v.symbol.constructType(), fixture.markType), header.returnUpperBounds)
        assertEquals(listOf(ConePrimitiveType.BOOLEAN, fixture.markType), parent.returnUpperBounds)
        assertTrue(header.typeParameters.isEmpty())
        assertTrue(header.upperBounds.isEmpty())
        originals.forEach(OriginalFunction::assertUnchanged)
    }

    @Test
    fun nestedFunctionCollectionsRejectMutationAcrossBindCaptureAndPublishedTables() {
        val fixture = Fixture()
        val parentSymbol = CfirInterfaceSymbol(fixture.classId("FrozenParent"))
        val parentParameter = fixture.parameter(parentSymbol, "T")
        val parentReceiver = ConeClassLikeType(parentSymbol.toLookupTag(),
            listOf(parentParameter.symbol.constructType()), isInterface = true)
        val parentFunction = fixture.genericIdentityFunction("parent", parentReceiver, "R", parentSymbol)
        val parent = fixture.interfaceDeclaration("FrozenParent", parentSymbol,
            parameters = listOf(parentParameter), members = listOf(parentFunction))
        val peer = fixture.genericPeer()
        val peerFunction = fixture.genericIdentityFunction("boundPeer", peer.type, "P")
        (peer.owner.declarations as MutableList<CfirDeclaration>).add(peerFunction)
        (peer.owner.superTypeRefs as MutableList<CfirTypeRef>).add(
            fixture.ref(fixture.type(parent, peer.parameter.symbol.constructType())))
        val ownFunction = fixture.genericIdentityFunction("boundPeer", fixture.intBox, "O")
        val ownParent = fixture.genericIdentityFunction("parent", fixture.intBox, "S")
        val owner = fixture.extend(fixture.intBox, listOf(ownFunction, ownParent))
        fixture.install(peer.owner, owner)
        val originals = listOf(peerFunction, ownFunction, parentFunction, ownParent).map(::OriginalFunction)
        val storage = ScopeSession()
        val input = fixture.inheritance(owner, storage)
        val ordinary = fixture.ordinaryScope(storage)
        val prepared = fixture.prepareHeaders(owner)
        val preparedHash = prepared.hashCode()
        val cacheKeys = hashMapOf(prepared to "completed")
        val headers = prepared.owners.mapValues { (_, value) -> value.memberHeaders.toMap() }
        val headerHashes = headers.mapValues { (_, values) -> values.mapValues { (_, header) -> header.hashCode() } }

        assertEquals(listOf(ownFunction.symbol, ownParent.symbol), input.steps.map { it.member.declaration })
        val peerBound = input.steps[0].invisibleParents.single()
        val ownBound = input.steps[0].member
        val capturedParent = input.steps[1].invisibleParents.single()
        val ownParentBound = input.steps[1].member
        assertSame(peerFunction.symbol, peerBound.declaration)
        assertSame(parentFunction.symbol, capturedParent.declaration)
        assertFalse(peerBound.flags.isInterfaceOwner)
        assertTrue(capturedParent.flags.isInterfaceOwner, "父接口成员必须来自正式 prepare/capture 路径")
        assertSame(peer.owner, capturedParent.sourceExtend)
        val mappedMembers = listOf(peerBound, ownBound, capturedParent, ownParentBound)
        val mappedHashes = mappedMembers.map { it.hashCode() }
        val mappedKeys = mappedMembers.associateWith { it.declaration }.toMutableMap()

        for ((label, member) in listOf("bind peer" to peerBound, "bind own" to ownBound,
            "capture parent" to capturedParent, "bind own parent" to ownParentBound)) {
            assertEquals(listOf(fixture.markType), member.returnUpperBounds)
            assertEquals(listOf(fixture.markType), member.upperBounds.values.single())
            assertFunctionCollectionsFrozen(checkNotNull(member.function), label)
        }
        val tableScope = assertInstanceOf(CfirFunctionInstantiationScope::class.java, ownBound.baseScope)
        val published = tableScope.publishedFunctionMemberTable(ownFunction.name)
        val instantiated = tableScope.functionInstantiationTable(ownFunction.name)
        assertEquals(listOf(ownFunction.symbol), published.members.map { it.declaration })
        assertEquals(listOf(ownFunction.symbol), instantiated.members.map { it.declaration })
        assertFunctionCollectionsFrozen(published.members.single(), "published table")
        assertFunctionCollectionsFrozen(instantiated.members.single(), "instantiation copy")

        assertEquals(preparedHash, prepared.hashCode())
        assertEquals("completed", cacheKeys[prepared])
        val fresh = fixture.prepareHeaders(owner)
        assertEquals(prepared, fresh)
        assertEquals(preparedHash, fresh.hashCode())
        assertEquals("completed", cacheKeys[fresh])
        for ((declaration, expectedHeaders) in headers) {
            val actual = fresh.owners.getValue(declaration).memberHeaders
            assertEquals(expectedHeaders, actual)
            for ((symbol, header) in actual) {
                assertEquals(headerHashes.getValue(declaration).getValue(symbol), header.hashCode())
            }
        }
        mappedMembers.forEachIndexed { index, member ->
            assertEquals(mappedHashes[index], member.hashCode())
            assertSame(member.declaration, mappedKeys[member])
        }
        assertSame(input, fixture.inheritance(owner, storage))
        assertSame(ordinary, fixture.ordinaryScope(storage))
        assertSame(published, tableScope.publishedFunctionMemberTable(ownFunction.name))
        assertSame(instantiated, tableScope.functionInstantiationTable(ownFunction.name))
        fixture.assertOrdinaryFunctions(storage, listOf(ownFunction, ownParent), listOf(ownFunction.name, ownParent.name))
        originals.forEach(OriginalFunction::assertUnchanged)
    }

    /** R15：直接 mutation 生产函数载荷；非空上界必须同时冻结 Map 外层和每个值列表。 */
    private fun assertFunctionCollectionsFrozen(function: CfirStoredFunctionMember, label: String) {
        assertTrue(function.parameterTypes.isNotEmpty(), label)
        assertTrue(function.upperBounds.isNotEmpty(), label)
        assertTrue(function.returnUpperBounds.isNotEmpty(), label)
        val expected = function.copy(
            parameterTypes = function.parameterTypes.toList(),
            upperBounds = function.upperBounds.mapValues { (_, bounds) -> bounds.toList() },
            inconsistentReturnTypes = function.inconsistentReturnTypes.toList(),
            returnUpperBounds = function.returnUpperBounds.toList(),
        )
        val hash = function.hashCode()
        val keys = hashMapOf(function to label)
        val provenance = function.lookupProvenance
        assertThrows(UnsupportedOperationException::class.java, {
            (function.parameterTypes as MutableList<ConeCangJieType>).add(ConePrimitiveType.BOOLEAN)
        }, "$label parameterTypes")
        val parameter = function.upperBounds.keys.single()
        assertThrows(UnsupportedOperationException::class.java, {
            (function.upperBounds as MutableMap<CfirTypeParameterSymbol, List<ConeCangJieType>>)[parameter] =
                listOf(ConePrimitiveType.BOOLEAN)
        }, "$label upperBounds map")
        for (bounds in function.upperBounds.values) {
            assertTrue(bounds.isNotEmpty(), label)
            assertThrows(UnsupportedOperationException::class.java, {
                (bounds as MutableList<ConeCangJieType>).clear()
            }, "$label upperBounds nested list")
        }
        // 即使原列表为空，插入也必须拒绝，不能只验证无操作的 clear。
        assertThrows(UnsupportedOperationException::class.java, {
            (function.inconsistentReturnTypes as MutableList<ConeCangJieType>).add(ConePrimitiveType.BOOLEAN)
        }, "$label inconsistentReturnTypes")
        assertThrows(UnsupportedOperationException::class.java, {
            (function.returnUpperBounds as MutableList<ConeCangJieType>).clear()
        }, "$label returnUpperBounds")
        assertEquals(expected, function, label)
        assertEquals(hash, function.hashCode(), label)
        assertEquals(label, keys[function], label)
        assertSame(provenance, function.lookupProvenance, label)
    }

    @Test
    fun movedCommonNominalMemberKeepsBothOwnersThroughColdAndWarmParentProjection() = assertMovedParent(false)

    @Test
    fun movedCommonInterfaceMemberKeepsBothOwnersThroughColdAndWarmParentProjection() = assertMovedParent(true)

    /** 模型中的匹配和 moved 结果均由正式 aggregate/runner 生成，不手填 snapshot 或替换映射。 */
    private fun assertMovedParent(isInterface: Boolean) {
        val common = Fixture()
        val name = if (isInterface) "MovedInterface" else "MovedNominal"
        val physical = common.projectionOwner(name, "C", isInterface, commonRole = true)
        val c = physical.typeParameters.single().symbol.constructType()
        val moved = common.ownerIdentityFunction(physical, "choose", c, commonRole = true)
        (physical.declarations as MutableList<CfirDeclaration>).add(moved)
        common.install()
        val actual = Fixture(common)
        val logical = actual.projectionOwner(name, "S", isInterface, specificRole = true)
        val s = logical.typeParameters.single().symbol.constructType()
        val parentType = actual.type(logical, actual.boxParameterType())
        val childParents = actual.box.superTypeRefs as MutableList<CfirTypeRef>
        if (!isInterface) childParents.clear()
        childParents += actual.ref(parentType)
        val own = actual.falseFunction("choose", ConePrimitiveType.INT64)
        val owner = actual.extend(actual.intBox, listOf(own))
        actual.install(owner)
        val originals = listOf(moved, own).map(::OriginalFunction)
        val physicalMembers = physical.declarations.toList()
        val logicalMembers = logical.declarations.toList()
        val storage = ScopeSession()
        val family = actual.session.cjmpFamilyAggregate.matchingGroupFor(logical)
        assertSame(physical, family.physicalOwnerFor(moved))
        assertSame(logical, family.logicalOwnerFor(moved))
        assertEquals(mapOf(physical.typeParameters.single() to logical.typeParameters.single()), family.ownerTypeParameterMappingFor(moved))
        assertTrue(family.mergedDeclarationsFor(logical).any { it === moved })
        assertFalse(logical.declarations.any { it === moved }, "逻辑移入不能修改 specific 的原物理声明体")

        val direct = actual.instantiationScopes(logical, actual.type(logical, ConePrimitiveType.INT64), storage)
        val rawTable = direct.raw.publishedFunctionMemberTable(moved.name)
        val rawMember = rawTable.members.single()
        assertSame(moved.symbol, rawMember.declaration)
        assertEquals(listOf(s), rawMember.parameterTypes)
        assertEquals(s, rawMember.returnType)
        assertEquals(actual.type(logical, s), rawMember.structType)
        val projected = direct.substituted.publishedFunctionMemberTable(moved.name).members.single()
        assertSame(moved.symbol, projected.declaration)
        assertEquals(listOf(ConePrimitiveType.INT64), projected.parameterTypes)
        assertEquals(ConePrimitiveType.INT64, projected.returnType)
        assertEquals(actual.type(logical, ConePrimitiveType.INT64), projected.structType)
        assertSame(direct.substituted, projected.baseScope)
        assertEquals(isInterface, projected.isInterfaceOwner)
        assertFunctionOwnerProjection(direct.raw, moved.symbol, physical, logical)
        assertFunctionOwnerProjection(direct.substituted, moved.symbol, physical, logical)

        // 先冷名称查询，再查询同一替换 symbol 的 owner；热查询保持实例与完整签名。
        val symbols = mutableListOf<CfirNamedFunctionSymbol>()
        direct.substituted.processFunctionsByName(moved.name, symbols::add)
        val substituted = symbols.single()
        assertNotSame(moved.symbol, substituted)
        assertSame(moved.symbol, substituted.unwrapSubstitutionOverrides())
        assertEquals(ConePrimitiveType.INT64, substituted.cfir.returnTypeRef.coneTypeOrNull)
        assertEquals(listOf(ConePrimitiveType.INT64), substituted.cfir.valueParameters.map { it.returnTypeRef.coneTypeOrNull })
        assertFunctionOwnerProjection(direct.substituted, substituted, physical, logical)
        repeat(3) {
            val hot = mutableListOf<CfirNamedFunctionSymbol>()
            direct.substituted.processFunctionsByName(moved.name, hot::add)
            assertSame(substituted, hot.single())
            assertFunctionOwnerProjection(direct.substituted, hot.single(), physical, logical)
            val member = direct.substituted.publishedFunctionMemberTable(moved.name).members.single()
            assertEquals(projected, member)
            assertEquals(listOf(ConePrimitiveType.INT64), member.parameterTypes)
            assertEquals(ConePrimitiveType.INT64, member.returnType)
            assertSame(direct, actual.instantiationScopes(logical, actual.type(logical, ConePrimitiveType.INT64), storage))
        }
        val boolean = actual.instantiationScopes(logical, actual.type(logical, ConePrimitiveType.BOOLEAN), storage)
        assertNotSame(direct.substituted, boolean.substituted)
        assertSame(direct.raw.declarationMembers, boolean.raw.declarationMembers)
        val boolMember = boolean.substituted.publishedFunctionMemberTable(moved.name).members.single()
        assertSame(moved.symbol, boolMember.declaration)
        assertEquals(listOf(ConePrimitiveType.BOOLEAN), boolMember.parameterTypes)
        assertEquals(ConePrimitiveType.BOOLEAN, boolMember.returnType)
        assertEquals(actual.type(logical, ConePrimitiveType.BOOLEAN), boolMember.structType)
        assertFunctionOwnerProjection(boolean.substituted, moved.symbol, physical, logical)

        // 真正经 Box<T> 父 scope wrapper 捕获的继承输入，不把 logical owner 改写为 Box/extend。
        val input = actual.inheritance(owner, storage)
        val inherited = input.steps.single().visibleParents.single()
        assertSame(moved.symbol, inherited.declaration)
        assertSame(physical, inherited.physicalOwner)
        assertSame(logical, inherited.logicalOwner)
        assertEquals(listOf(ConePrimitiveType.INT64), inherited.parameterTypes)
        assertEquals(ConePrimitiveType.INT64, inherited.returnType)
        assertEquals(actual.type(logical, ConePrimitiveType.INT64), inherited.structType)
        assertEquals(isInterface, inherited.flags.isInterfaceOwner)
        assertEquals(isInterface, checkNotNull(inherited.function).isInterfaceOwner)
        assertNull(inherited.sourceExtend)
        assertInstanceOf(CfirClassSubstitutionScope::class.java, inherited.baseScope)
        assertFunctionOwnerProjection(inherited.baseScope, moved.symbol, physical, logical)
        assertTrue(input.steps.single().invisibleParents.isEmpty())
        repeat(3) {
            assertSame(input, actual.inheritance(owner, storage))
            assertSame(inherited, input.steps.single().visibleParents.single())
            assertEquals(ConePrimitiveType.INT64, inherited.returnType)
        }
        if (!isInterface) {
            val constructor = direct.substituted.constructorInstantiationTable().members.single()
            assertTrue(constructor.isConstructor)
            assertEquals(actual.type(logical, ConePrimitiveType.INT64), constructor.returnType)
            assertNull(constructor.sourceExtend)
            assertSame(org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance.None, constructor.lookupProvenance)
            assertSame(constructor.lookupProvenance, constructor.identity.lookupProvenance)
        }
        val raw = actual.scopes.getUseSiteMemberScope(logical.symbol, actual.session, storage,
            ownerType = actual.type(logical, ConePrimitiveType.INT64), scopeKind = CfirClassMemberScopeKind.USE_SITE,
            memberRequiredPhase = null, scopeOwner = CfirMemberScopeOwner.DeclarationOnlyInstantiation,
            memberScopeView = CfirCjmpMemberScopeView.RAW)
        val rawNames = mutableListOf<CfirNamedFunctionSymbol>()
        raw.processFunctionsByName(moved.name, rawNames::add)
        assertTrue(rawNames.isEmpty(), "RAW 不得取得 INST 的 moved header 产品")
        actual.assertOrdinaryFunctions(storage, listOf(own), listOf(own.name))
        assertEquals(physicalMembers, physical.declarations)
        assertEquals(logicalMembers, logical.declarations)
        assertEquals(c, moved.returnTypeRef.coneTypeOrNull)
        assertEquals(listOf(c), moved.valueParameters.map { it.returnTypeRef.coneTypeOrNull })
        originals.forEach(OriginalFunction::assertUnchanged)
    }

    private fun assertFunctionOwnerProjection(scope: CfirTypeScope, symbol: CfirCallableSymbol<*>,
        physical: CfirClassLikeDeclaration, logical: CfirClassLikeDeclaration) {
        val projection = checkNotNull((scope as CfirCjmpMemberSubstitutorProvider).cjmpMemberOwnerFor(symbol))
        assertSame(physical, projection.physicalOwner)
        assertSame(logical, projection.logicalOwner)
    }

    @Test
    fun sourceAliasClosureUsesPublishedRhsAndKeepsEqualHeadersUntilPublicationChanges() {
        val fixture = Fixture()
        val other = fixture.interfaceDeclaration("OtherBound", CfirInterfaceSymbol(fixture.classId("OtherBound")))
        val alias = fixture.sourceAlias("HeaderBound", fixture.markType)
        // abbreviation 故意不是权威 RHS；上界和返回 TP 闭包必须调用 Source alias 准备入口。
        val aliasType = ConeTypeAliasType(alias.symbol.classId, expandedType = ConePrimitiveType.BOOLEAN)
        val carrier = fixture.projectionOwner("AliasCarrier", null, false)
        val function = fixture.aliasBoundFunction(carrier, aliasType)
        (carrier.declarations as MutableList<CfirDeclaration>).add(function)
        fixture.install()
        val original = OriginalFunction(function)
        val storage = ScopeSession()
        val type = fixture.type(carrier)
        val first = fixture.instantiationScopes(carrier, type, storage)
        val member = first.substituted.publishedFunctionMemberTable(function.name).members.single()
        val parameter = function.typeParameters.single().symbol
        assertEquals(listOf(fixture.markType), member.upperBounds.getValue(parameter))
        assertEquals(listOf(fixture.markType), member.returnUpperBounds)
        val beforeHash = member.hashCode()
        repeat(3) {
            assertSame(first, fixture.instantiationScopes(carrier, type, storage))
            assertEquals(member, first.substituted.publishedFunctionMemberTable(function.name).members.single())
        }
        val nextRhs = fixture.ref(fixture.type(other))
        alias.replaceExpandedTypeRef(nextRhs)
        assertSame(first, fixture.instantiationScopes(carrier, type, storage), "活动 RHS 未发布不能改变完成 header")
        assertEquals(listOf(fixture.markType), member.upperBounds.getValue(parameter))
        fixture.publishAlias(alias)
        val next = fixture.instantiationScopes(carrier, type, storage)
        assertNotSame(first.raw, next.raw)
        assertNotSame(first.substituted, next.substituted)
        val changed = next.substituted.publishedFunctionMemberTable(function.name).members.single()
        assertEquals(listOf(fixture.type(other)), changed.upperBounds.getValue(parameter))
        assertEquals(listOf(fixture.type(other)), changed.returnUpperBounds)
        assertEquals(beforeHash, member.hashCode())
        assertEquals(listOf(fixture.markType), member.returnUpperBounds)
        assertSame(next, fixture.instantiationScopes(carrier, type, storage))
        val functions = mutableListOf<CfirNamedFunctionSymbol>()
        next.substituted.processFunctionsByName(function.name, functions::add)
        assertSame(function.symbol, functions.single().unwrapSubstitutionOverrides())
        original.assertUnchanged()
    }

    private fun assertInvisibleFunctionNotPublished(input: CfirDeclarationInheritanceInput, hidden: CfirNamedFunction) {
        val scope = assertInstanceOf(CfirFunctionInstantiationScope::class.java, input.steps.first().member.baseScope)
        for (table in listOf(scope.publishedFunctionMemberTable(hidden.name), scope.functionInstantiationTable(hidden.name))) {
            assertFalse(table.members.any { it.declaration === hidden.symbol })
            assertFalse(table.members.any { it.sourceExtend === hidden.symbol.getContainingExtend() })
        }
    }

    /** 保留参数引用、真实非空 body 及其每条语句身份；模型中没有成员调用表达式。 */
    private class OriginalFunction(private val function: CfirNamedFunction) {
        val body: CfirBlock = checkNotNull(function.body)
        private val statements = body.statements.toList()
        private val status = function.status
        private val attributes = function.attributes
        private val phase = function.resolvePhase
        private val returnRef = function.returnTypeRef
        private val receiver = function.dispatchReceiverType
        private val parameters = function.valueParameters.toList()
        private val parameterRefs = parameters.map { it.returnTypeRef }

        init {
            assertTrue(statements.isNotEmpty())
            assertEquals(returnRef.coneTypeOrNull, body.coneTypeOrNull)
        }

        fun assertUnchanged() {
            assertSame(body, function.body)
            assertSame(status, function.status)
            assertSame(attributes, function.attributes)
            assertEquals(phase, function.resolvePhase)
            assertSame(returnRef, function.returnTypeRef)
            assertSame(receiver, function.dispatchReceiverType)
            assertEquals(statements.size, body.statements.size)
            statements.forEachIndexed { index, statement -> assertSame(statement, body.statements[index]) }
            assertEquals(parameters.size, function.valueParameters.size)
            parameters.forEachIndexed { index, parameter ->
                assertSame(parameter, function.valueParameters[index])
                assertSame(parameterRefs[index], parameter.returnTypeRef)
                assertSame(function.symbol, parameter.containingDeclarationSymbol)
            }
        }
    }

    /** 属性保存完整 getter/setter；空 setter 是合法源码，getter 必须保留非空 body。 */
    private class OriginalProperty(private val property: CfirProperty) {
        private val returnRef = property.returnTypeRef
        private val status = property.status
        private val getter = checkNotNull(property.getter)
        private val getterBody = checkNotNull(getter.body)
        private val statements = getterBody.statements.toList()
        private val setter = property.setter
        private val setterBody = setter?.body
        private val setterParameter = setter?.valueParameters?.single()

        init { assertTrue(statements.isNotEmpty()) }

        fun assertUnchanged() {
            assertSame(returnRef, property.returnTypeRef)
            assertSame(status, property.status)
            assertSame(getter, property.getter)
            assertSame(getterBody, getter.body)
            assertEquals(statements.size, getterBody.statements.size)
            statements.forEachIndexed { index, statement -> assertSame(statement, getterBody.statements[index]) }
            assertSame(setter, property.setter)
            if (setter != null) {
                assertSame(setterBody, setter.body)
                assertSame(setterParameter, setter.valueParameters.single())
                assertEquals(returnRef.coneTypeOrNull, setterParameter?.returnTypeRef?.coneTypeOrNull)
            }
        }
    }

    /** 与既有 scope tests 一致：构造完成声明，注册真实组件，公开 provider API 生产结果。 */
    private class Fixture(common: Fixture? = null) {
        val session: CfirSession = object : CfirSession(Kind.Source) {}
        private val packageName = FqName("inheritance.inputs")
        private val module: CfirSourceModuleData = CfirSourceModuleData(Name.identifier("declaration-inheritance"), listOfNotNull(common?.module), listOfNotNull(common?.module),
            CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT).also {
            it.bindSession(session)
            session.register(CfirModuleData::class, it)
        }
        val scopes = CfirCangJieScopeProvider()
        private val classifiers = mutableListOf<CfirClassLikeDeclaration>()
        private val graph = CompletedGraph()
        private val index = CfirExtendIndexStore(session)
        private val symbols = object : CfirSymbolProvider(session) {
            override val symbolNamesProvider = CfirNullSymbolNamesProvider
            override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
                classifiers.singleOrNull { it.symbol.classId == classId }?.symbol
            override fun hasPackage(fqName: FqName): Boolean = classifiers.any { it.symbol.classId.packageFqName == fqName }
            override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {}
            override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {}
            override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {}
        }
        private val any = interfaceDeclaration("Any", CfirInterfaceSymbol(StdlibClassIds.Any), parents = emptyList(),
            declarationOrigin = CfirDeclarationOrigin.Library)
        private val objectDeclaration = classDeclaration("Object", CfirClassSymbol(StdlibClassIds.Object),
            parents = listOf(ref(type(any), CfirTypeRefOrigin.SEMANTIC)), declarationOrigin = CfirDeclarationOrigin.Library)
        private val boxSymbol = CfirClassSymbol(classId("Box"))
        private val boxParameter = parameter(boxSymbol, "T")
        val box = classDeclaration("Box", boxSymbol, parameters = listOf(boxParameter),
            parents = listOf(ref(type(objectDeclaration), CfirTypeRefOrigin.COMPILER_ADDED)))
        private val mark = interfaceDeclaration("Mark", CfirInterfaceSymbol(classId("Mark")))
        val markType: ConeClassLikeType get() = type(mark)
        val intBox: ConeClassLikeType = boxType(ConePrimitiveType.INT64)

        init {
            session.register(CfirCangJieScopeProvider::class, scopes)
            session.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            session.register(TypeComponents::class, TypeComponents(session))
            session.register(CfirSymbolProvider::class, symbols)
            session.register(CfirExtendProvider::class, CfirEmptyExtendProvider())
            session.register(CfirDirectSupertypeProvider::class, graph)
            session.register(CfirTypeResolver::class, CfirTypeResolverImpl(session))
            session.register(CfirTypeAwareSupertypeProvider::class, CfirTypeAwareSupertypeProviderImpl(session))
            session.register(CfirExtendIndexStore::class, index)
            session.register(CfirExtendRuleQueryService::class, CfirExtendRuleQueryServiceImpl(session, index))
            session.register(CfirAccessibilityChecker::class, CfirAccessibilityChecker(session))
            session.register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY,
                if (common == null) CfirEmptySymbolProvider(session) else common.symbols)
            session.register(CfirCjmpFamilyAggregate::class, CfirCjmpBatchFamilyAggregate(session) { declaration, ownerFacts, mode ->
                CfirCjmpMatchRunner.prepareDeclaration(declaration, session, ownerFacts, matchingMode = mode)
            })
            if (common != null) {
                common.session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
                session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.SPECIFIC))
            }
        }

        fun boxParameterType(): ConeCangJieType = boxParameter.symbol.constructType()

        /** 明确装配完整 generic owner 和显式 init；角色由输入给出，匹配交给生产 runner。 */
        fun projectionOwner(name: String, parameterName: String?, isInterface: Boolean,
            commonRole: Boolean = false, specificRole: Boolean = false): CfirClassLikeDeclaration {
            val symbol: CfirClassLikeSymbol<*> = if (isInterface) CfirInterfaceSymbol(classId(name)) else CfirClassSymbol(classId(name))
            val parameters = parameterName?.let { listOf(parameter(symbol, it)) }.orEmpty()
            val owner = if (isInterface) interfaceDeclaration(name, symbol as CfirInterfaceSymbol, parameters)
                else classDeclaration(name, symbol as CfirClassSymbol,
                    listOf(ref(type(objectDeclaration), CfirTypeRefOrigin.COMPILER_ADDED)), parameters)
            owner.replaceStatus(status(isOpen = true, commonRole = commonRole, specificRole = specificRole,
                commonDefault = commonRole))
            if (!isInterface) {
                val receiver = type(owner, *parameters.map { it.symbol.constructType() }.toTypedArray())
                val constructor = buildConstructor {
                    source = ModelSource("explicit-init:$name")
                    moduleData = module
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = CfirDeclarationOrigin.Source
                    attributes = CfirDeclarationAttributes()
                    deprecationsProvider = EmptyDeprecationsProvider
                    status = status(commonRole = commonRole, specificRole = specificRole, commonDefault = commonRole)
                    isLocal = false
                    this.symbol = CfirConstructorSymbol(CallableId(owner.symbol.classId, Name.special("<init>")))
                    dispatchReceiverType = receiver
                    returnTypeRef = ref(receiver)
                    body = block(ConePrimitiveType.UNIT)
                }
                (owner.declarations as MutableList<CfirDeclaration>).add(constructor)
            }
            return owner
        }

        fun ownerIdentityFunction(owner: CfirClassLikeDeclaration, name: String, result: ConeCangJieType,
            commonRole: Boolean = false): CfirNamedFunction = ownerFunction(owner, name, result, emptyList(), commonRole)

        fun aliasBoundFunction(owner: CfirClassLikeDeclaration, aliasType: ConeCangJieType): CfirNamedFunction {
            val symbol = CfirNamedFunctionSymbol(CallableId(owner.symbol.classId, Name.identifier("bounded")))
            val parameter = parameter(symbol, "V", listOf(aliasType))
            return ownerFunction(owner, "bounded", parameter.symbol.constructType(), listOf(parameter), false, symbol)
        }

        private fun ownerFunction(owner: CfirClassLikeDeclaration, name: String, result: ConeCangJieType,
            ownParameters: List<CfirTypeParameter>, commonRole: Boolean,
            symbol: CfirNamedFunctionSymbol = CfirNamedFunctionSymbol(CallableId(owner.symbol.classId, Name.identifier(name)))): CfirNamedFunction {
            val value = valueParameter("x", result, symbol)
            return buildNamedFunction {
                source = ModelSource("owner-function:$name")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                deprecationsProvider = EmptyDeprecationsProvider
                status = status(isDefault = owner is CfirInterface, isOpen = owner is CfirInterface,
                    commonRole = commonRole, commonDefault = commonRole)
                isLocal = false
                isMut = false
                this.symbol = symbol
                this.name = Name.identifier(name)
                dispatchReceiverType = type(owner, *owner.typeParameters.map { it.symbol.constructType() }.toTypedArray())
                returnTypeRef = ref(result)
                typeParameters += ownParameters
                valueParameters += value
                body = block(result, buildNamedAccessExpression {
                    source = ModelSource("owner-body:x")
                    coneTypeOrNull = result
                    calleeReference = buildResolvedNamedReference { this.name = value.name; resolvedSymbol = value.symbol }
                })
            }.also { it.hasGenericNode = ownParameters.isNotEmpty() }
        }

        fun sourceAlias(name: String, rhs: ConeCangJieType): CfirTypeAlias = buildTypeAlias {
            source = ModelSource("source-alias:$name")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            status = status()
            scopeProvider = scopes
            symbol = CfirTypeAliasSymbol(classId(name))
            this.name = Name.identifier(name)
            expandedTypeRef = ref(rhs)
        }.also { classifiers += it }

        fun publishAlias(alias: CfirTypeAlias) = graph.publishAlias(alias)

        fun instantiationScopes(owner: CfirClassLikeDeclaration, receiver: ConeCangJieType, storage: ScopeSession):
            CfirCangJieScopeProvider.GenericInstantiationMemberScopes = scopes.getGenericInstantiationMemberScopes(
                owner.symbol, session, storage, receiver,
                type(owner, *owner.typeParameters.map { it.symbol.constructType() }.toTypedArray()),
                CfirMemberScopeOwner.DeclarationOnlyInstantiation, CfirCjmpMemberScopeView.CJMP_INSTANTIATION)

        data class Peer(val owner: CfirExtend, val parameter: CfirTypeParameter, val type: ConeClassLikeType)

        fun genericPeer(): Peer {
            val symbol = CfirExtendSymbol()
            val parameter = parameter(symbol, "U", listOf(type(mark)))
            val type = boxType(parameter.symbol.constructType())
            return Peer(extend(type, emptyList(), symbol, listOf(parameter)), parameter, type)
        }

        fun classId(name: String): ClassId = ClassId(packageName, Name.identifier(name))
        fun boxType(argument: ConeCangJieType): ConeClassLikeType = ConeClassLikeType(boxSymbol.toLookupTag(), listOf(argument))
        fun type(declaration: CfirClassLikeDeclaration, vararg arguments: ConeCangJieType): ConeClassLikeType =
            ConeClassLikeType(declaration.symbol.toLookupTag(), arguments.toList(), isInterface = declaration is CfirInterface)

        fun ref(type: ConeCangJieType, origin: CfirTypeRefOrigin = CfirTypeRefOrigin.SOURCE): CfirResolvedTypeRef =
            buildResolvedTypeRef {
                source = ModelSource("type:$type")
                typeUseOrigin = origin
                coneType = type
            }

        fun parameter(owner: CfirBasedSymbol<*>, name: String, upperBounds: List<ConeCangJieType> = emptyList()): CfirTypeParameter =
            buildTypeParameter {
                source = ModelSource("parameter:$name")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                containingDeclarationSymbol = owner
                symbol = CfirTypeParameterSymbol()
                this.name = Name.identifier(name)
                bounds += upperBounds.map { ref(it) }
            }

        fun interfaceDeclaration(name: String, symbol: CfirInterfaceSymbol, parameters: List<CfirTypeParameter> = emptyList(),
            members: List<CfirDeclaration> = emptyList(), parents: List<ConeCangJieType> = listOf(type(any)),
            declarationOrigin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source): CfirInterface =
            buildInterface {
                source = if (declarationOrigin.fromSource) ModelSource("interface:$name") else null
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = declarationOrigin
                attributes = CfirDeclarationAttributes()
                deprecationsProvider = EmptyDeprecationsProvider
                scopeProvider = scopes
                status = status(isOpen = true)
                this.symbol = symbol
                this.name = Name.identifier(name)
                typeParameters += parameters
                declarations += members
                superTypeRefs += parents.map { ref(it) }
            }.also { it.hasGenericNode = parameters.isNotEmpty(); classifiers += it }

        private fun classDeclaration(name: String, symbol: CfirClassSymbol, parents: List<CfirTypeRef>,
            parameters: List<CfirTypeParameter> = emptyList(),
            declarationOrigin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source): CfirClass = buildClass {
            source = if (declarationOrigin.fromSource) ModelSource("class:$name") else null
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = declarationOrigin
            attributes = CfirDeclarationAttributes()
            deprecationsProvider = EmptyDeprecationsProvider
            scopeProvider = scopes
            status = status()
            this.symbol = symbol
            this.name = Name.identifier(name)
            typeParameters += parameters
            superTypeRefs += parents
        }.also { it.hasGenericNode = parameters.isNotEmpty(); classifiers += it }

        fun extend(receiver: ConeCangJieType, members: List<CfirDeclaration>, symbol: CfirExtendSymbol = CfirExtendSymbol(),
            parameters: List<CfirTypeParameter> = emptyList(), interfaces: List<ConeCangJieType> = emptyList()): CfirExtend =
            buildExtend {
                source = ModelSource("extend:$receiver")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                status = status()
                this.symbol = symbol
                extendedTypeRef = ref(receiver)
                typeParameters += parameters
                superTypeRefs += interfaces.map { ref(it) }
                declarations += members
            }.also { it.hasGenericNode = parameters.isNotEmpty() }

        fun identityFunction(name: String, parameterType: ConeCangJieType, interfaceOwner: CfirInterfaceSymbol? = null,
            isDefault: Boolean = false, receiver: ConeSimpleCangJieType? = null): CfirNamedFunction =
            function(name, parameterType, parameterType, interfaceOwner, isDefault, false, receiver)

        fun falseFunction(name: String, parameterType: ConeCangJieType, receiver: ConeSimpleCangJieType = intBox): CfirNamedFunction =
            function(name, parameterType, ConePrimitiveType.BOOLEAN, null, false, false, receiver)

        fun abstractFunction(name: String, parameterType: ConeCangJieType, owner: CfirInterfaceSymbol,
            receiver: ConeSimpleCangJieType? = null): CfirNamedFunction = function(name, parameterType, parameterType, owner, false, true, receiver)

        private fun function(name: String, parameterType: ConeCangJieType, result: ConeCangJieType,
            interfaceOwner: CfirInterfaceSymbol?, isDefault: Boolean, isAbstract: Boolean, receiver: ConeSimpleCangJieType?): CfirNamedFunction {
            val symbol = CfirNamedFunctionSymbol(if (interfaceOwner == null) CallableId(packageName, Name.identifier(name))
                else CallableId(interfaceOwner.classId, Name.identifier(name)))
            val parameter = valueParameter("x", parameterType, symbol)
            return buildNamedFunction {
                source = ModelSource("function:$name")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                deprecationsProvider = EmptyDeprecationsProvider
                status = status(isDefault = isDefault, isAbstract = isAbstract, isOpen = interfaceOwner != null)
                isLocal = false
                isMut = false
                this.symbol = symbol
                this.name = Name.identifier(name)
                dispatchReceiverType = receiver ?: if (interfaceOwner == null) intBox
                    else ConeClassLikeType(interfaceOwner.toLookupTag(), isInterface = true)
                returnTypeRef = ref(result)
                valueParameters += parameter
                if (!isAbstract) body = block(result, if (result == parameterType) buildNamedAccessExpression {
                    source = ModelSource("body:x")
                    coneTypeOrNull = result
                    calleeReference = buildResolvedNamedReference {
                        this.name = parameter.name
                        resolvedSymbol = parameter.symbol
                    }
                } else falseLiteral())
            }
        }

        /** 无调用的真实泛型函数：func f<R>(x: R): R where R <: Mark { x }。 */
        fun genericIdentityFunction(name: String, receiver: ConeSimpleCangJieType, parameterName: String,
            interfaceOwner: CfirInterfaceSymbol? = null): CfirNamedFunction {
            val symbol = CfirNamedFunctionSymbol(if (interfaceOwner == null) CallableId(packageName, Name.identifier(name))
                else CallableId(interfaceOwner.classId, Name.identifier(name)))
            val typeParameter = parameter(symbol, parameterName, listOf(markType))
            val result = typeParameter.symbol.constructType()
            val valueParameter = valueParameter("x", result, symbol)
            return buildNamedFunction {
                source = ModelSource("generic-function:$name")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                deprecationsProvider = EmptyDeprecationsProvider
                status = status(isDefault = interfaceOwner != null, isOpen = interfaceOwner != null)
                isLocal = false
                isMut = false
                this.symbol = symbol
                this.name = Name.identifier(name)
                dispatchReceiverType = receiver
                returnTypeRef = ref(result)
                typeParameters += typeParameter
                valueParameters += valueParameter
                body = block(result, buildNamedAccessExpression {
                    source = ModelSource("generic-body:x")
                    coneTypeOrNull = result
                    calleeReference = buildResolvedNamedReference {
                        this.name = valueParameter.name
                        resolvedSymbol = valueParameter.symbol
                    }
                })
            }.also { it.hasGenericNode = true }
        }

        /** 合法无调用属性：Box<U> getter 返回 this，Bool getter 返回 false；mut setter 可为空。 */
        fun property(name: String, result: ConeCangJieType, owner: CfirExtendSymbol, isMut: Boolean): CfirProperty {
            val propertySymbol = CfirPropertySymbol(CallableId(packageName, Name.identifier(name)))
            val receiver = assertInstanceOf(ConeSimpleCangJieType::class.java, owner.cfir.extendedTypeRef.coneTypeOrNull)
            fun accessor(isGetter: Boolean): CfirPropertyAccessor {
                val accessorSymbol = CfirPropertyAccessorSymbol()
                return buildPropertyAccessor {
                    source = ModelSource("${if (isGetter) "get" else "set"}:$name")
                    moduleData = module
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = CfirDeclarationOrigin.Source
                    attributes = CfirDeclarationAttributes()
                    status = status()
                    isLocal = false
                    dispatchReceiverType = receiver
                    symbol = accessorSymbol
                    this.propertySymbol = propertySymbol
                    this.isGetter = isGetter
                    returnTypeRef = ref(if (isGetter) result else ConePrimitiveType.UNIT)
                    if (isGetter) {
                        val expression = if (result == ConePrimitiveType.BOOLEAN) falseLiteral() else buildThisReceiverExpression {
                            source = ModelSource("body:this")
                            coneTypeOrNull = result
                            calleeReference = buildThisReference { boundSymbol = owner; isImplicit = false }
                        }
                        body = block(result, expression)
                    } else {
                        valueParameters += valueParameter("v", result, accessorSymbol)
                        body = block(ConePrimitiveType.UNIT)
                    }
                }
            }
            return buildProperty {
                source = ModelSource("property:$name")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                deprecationsProvider = EmptyDeprecationsProvider
                status = status(isMut = isMut)
                isLocal = false
                dispatchReceiverType = receiver
                symbol = propertySymbol
                this.name = Name.identifier(name)
                returnTypeRef = ref(result)
                getter = accessor(true)
                if (isMut) setter = accessor(false)
            }
        }

        private fun valueParameter(name: String, type: ConeCangJieType, owner: CfirBasedSymbol<*>): CfirValueParameter = buildValueParameter {
            source = ModelSource("value-parameter:$name")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            deprecationsProvider = EmptyDeprecationsProvider
            status = status()
            isLocal = false
            isNamed = false
            symbol = CfirValueParameterSymbol(CallableId(packageName, Name.identifier(name)))
            containingDeclarationSymbol = owner
            this.name = Name.identifier(name)
            returnTypeRef = ref(type)
        }

        private fun falseLiteral(): CfirExpression = buildLiteralExpression {
            source = ModelSource("literal:false")
            coneTypeOrNull = ConePrimitiveType.BOOLEAN
            kind = CfirLiteralKind.BOOLEAN
            value = false
        }

        private fun block(type: ConeCangJieType, vararg expressions: CfirExpression): CfirBlock = buildBlock {
            source = ModelSource("body")
            coneTypeOrNull = type
            statements += expressions
        }

        fun status(isMut: Boolean = false, isDefault: Boolean = false, isAbstract: Boolean = false,
            isOpen: Boolean = false, visibility: Visibility = Visibilities.Public, commonRole: Boolean = false,
            specificRole: Boolean = false, commonDefault: Boolean = false): CfirResolvedDeclarationStatus = buildResolvedDeclarationStatus {
            this.visibility = visibility
            modality = if (isAbstract) Modality.ABSTRACT else if (isOpen) Modality.OPEN else Modality.FINAL
            isVisibilityExplicit = true
            isModalityExplicit = false
            isAbstractExplicit = false
            isStatic = false
            isOverride = false
            isRedef = false
            isConst = false
            this.isMut = isMut
            this.isDefault = isDefault
            this.isAbstract = isAbstract
            this.isOpen = isOpen
            isSealed = false
            isOperator = false
            isUnsafe = false
            isForeign = false
            isC = false
            isCommon = commonRole
            isSpecific = specificRole
            isCommonWithDefault = commonDefault
        }

        fun install(vararg extends: CfirExtend) {
            val file = buildFile {
                source = ModelSource("file:inheritance.cj")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                symbol = CfirFileSymbol()
                name = "inheritance.cj"
                packageDirective = buildPackageDirective { packageFqName = packageName; isMacroPackage = false }
                declarations += classifiers.filter { it.origin.fromSource }
                declarations += extends
            }
            val callableFiles = buildMap<CfirCallableSymbol<*>, CfirFile> {
                for (owner in file.declarations) {
                    val members = when (owner) {
                        is CfirClassLikeDeclaration -> owner.declarations
                        is CfirExtend -> owner.declarations
                        else -> emptyList()
                    }
                    for (member in members.filterIsInstance<CfirCallableDeclaration>()) {
                        put(member.symbol, file)
                        if (member is CfirProperty) {
                            member.getter?.let { put(it.symbol, file) }
                            member.setter?.let { put(it.symbol, file) }
                        }
                    }
                }
            }
            session.register(CfirProvider::class, object : CfirProvider() {
                override val symbolProvider: CfirSymbolProvider = symbols
                override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? = symbols.getClassLikeSymbolByClassId(classId)?.cfir
                override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile = checkNotNull(getCfirClassifierContainerFileIfAny(fqName))
                override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? =
                    file.takeIf { classifiers.any { it.symbol.classId == fqName && it.origin.fromSource } }
                override fun getCfirClassifierContainerFileIfAny(symbol: CfirClassLikeSymbol<*>): CfirFile? =
                    file.takeIf { classifiers.any { it.symbol === symbol && it.origin.fromSource } }
                override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = callableFiles[symbol]
                override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = if (fqName == packageName) listOf(file) else emptyList()
                override fun getClassNamesInPackage(fqName: FqName): Set<Name> = classifiers.filter { it.symbol.classId.packageFqName == fqName }.map { it.name }.toSet()
            })
            session.register(CfirExtendProvider::class, FrozenExtends(file, extends.toList(), CfirExtendTargetKey.ClassLike(boxSymbol.classId)))
            graph.publish(classifiers)
            index.rebuild(listOf(file), CfirTypeResolverImpl(session))
        }

        fun inheritance(owner: CfirExtend, storage: ScopeSession): CfirDeclarationInheritanceInput =
            checkNotNull(scopes.getDeclarationInheritanceInput(owner, session, storage))

        fun ordinaryScope(storage: ScopeSession): CfirTypeScope = scopes.getUseSiteMemberScope(box.symbol, session, storage,
            ownerType = intBox, dispatchReceiverType = intBox, scopeKind = CfirClassMemberScopeKind.USE_SITE,
            memberRequiredPhase = null, memberScopeView = CfirCjmpMemberScopeView.RAW)

        fun assertOrdinaryFunctions(storage: ScopeSession, expected: List<CfirNamedFunction>, names: List<Name>) {
            val scope = ordinaryScope(storage)
            repeat(2) {
                for (name in names) {
                    val functions = mutableListOf<CfirNamedFunctionSymbol>()
                    scope.processFunctionsByName(name, functions::add)
                    assertEquals(expected.filter { it.name == name }.map { it.symbol }, functions)
                    val callables = mutableListOf<CfirCallableSymbol<*>>()
                    scope.processCallablesByName(name, callables::add)
                    assertEquals(expected.filter { it.name == name }.map { it.symbol }, callables)
                }
                assertEquals(expected.map { it.name }.toSet(), scope.getCallableNames())
            }
        }

        fun prepareHeaders(owner: CfirExtend): CfirExtendMemberScope.ScopeInput {
            val extends = session.extendProvider
            val structural = checkNotNull(session.typeAwareSupertypeProviderOrNull)
            val owners = extends.getAllExtends().toSet()
            return CfirExtendMemberScope.prepareScopeInput(CfirExtendTargetKey.ClassLike(boxSymbol.classId), extends, session,
                checkNotNull(owner.extendedTypeRef.coneTypeOrNull), CfirCjmpMemberScopeView.CJMP_INSTANTIATION, CfirResolvePhase.TYPES,
                CfirMemberScopeOwner.ExtendInstantiation(owner, owners), owner, structural.inputStamp.sources, structural,
                false, CfirTypeAlias::expandedConeTypeWithEnsuredPhase,
                registerParameterOwner = { parameter -> check(parameter.resolvedBounds.all { it.coneTypeOrNull != null }) })
        }
    }

    /** 显式给定桶、文件、member owner 的不可变索引；没有 applicability 或映射判断。 */
    private class FrozenExtends(private val file: CfirFile, private val owners: List<CfirExtend>, private val key: CfirExtendTargetKey) : CfirExtendProvider {
        private val memberOwners = owners.flatMap { owner -> owner.declarations.filterIsInstance<CfirCallableDeclaration>().map { it.symbol to owner } }.toMap()
        override val modificationCount: Long = 0L
        override fun getAllExtends(): List<CfirExtend> = owners.toList()
        override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> = if (targetKey == key) owners.toList() else emptyList()
        override fun getExtendsForClass(classId: ClassId): List<CfirExtend> = getExtendsForTarget(CfirExtendTargetKey.ClassLike(classId))
        override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> = if (packageFqName == file.packageDirective.packageFqName) owners.toList() else emptyList()
        override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> = emptyList()
        override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? = memberOwners[symbol]
        override fun getPackageFqName(extend: CfirExtend): FqName? = file.packageDirective.packageFqName.takeIf { extend in owners }
        override fun getContainingFile(extend: CfirExtend): CfirFile? = file.takeIf { extend in owners }
    }

    /** Source 图只发布给定的 resolved refs/edges；未知节点保持未发布。 */
    private class CompletedGraph : CfirDirectSupertypeProvider {
        private var products = emptyMap<CfirClassLikeSymbol<*>, CfirCompletedSupertypeProduct>()
        override var modificationCount: Long = 0L
            private set

        fun publish(declarations: List<CfirClassLikeDeclaration>) {
            products = declarations.associate { owner ->
                if (owner is CfirTypeAlias) return@associate owner.symbol to aliasProduct(owner)
                val edges = owner.superTypeRefs.map { sourceRef ->
                    val ref = assertInstanceOf(CfirResolvedTypeRef::class.java, sourceRef)
                    val type = assertInstanceOf(ConeClassLikeType::class.java, ref.coneType)
                    val target = declarations.single { it.symbol.classId == type.lookupTag.classId }.symbol
                    CfirSuperTypeGraphEdge(ref, ref.toString(), target)
                }
                owner.symbol to CfirCompletedSupertypeProduct(owner.symbol, owner.superTypeRefs, edges, emptyList(), false)
            }
            modificationCount++
        }

        private fun aliasProduct(alias: CfirTypeAlias): CfirCompletedSupertypeProduct = CfirCompletedSupertypeProduct(
            alias.symbol, listOf(alias.expandedTypeRef), emptyList(), emptyList(), false)

        fun publishAlias(alias: CfirTypeAlias) {
            check(alias.symbol in products)
            products = products + (alias.symbol to aliasProduct(alias))
            modificationCount++
        }

        override fun getCompletedSupertypeProduct(ownerSymbol: CfirClassLikeSymbol<*>): CfirCompletedSupertypeProduct? = products[ownerSymbol]
        override fun getPublishedReferenceCycleState(ownerSymbol: CfirClassLikeSymbol<*>): Boolean? = products[ownerSymbol]?.isInReferenceCycle
        override fun getDirectSuperTypeEdges(ownerClassId: ClassId): List<CfirSuperTypeGraphEdge> =
            products.entries.singleOrNull { it.key.classId == ownerClassId }?.value?.declaredSuperTypes.orEmpty()
        override fun getDirectSuperTypes(ownerClassId: ClassId): List<CfirResolvedTypeRef> = getDirectSuperTypeEdges(ownerClassId).map { it.typeRef }
    }

    /** 与既有 providers 模型测试相同的来源身份；不冒充 PSI/LightTree 解析结果。 */
    private class ModelSource(identity: String) : CjBinarySourceElement(identity, null, identity)
}
