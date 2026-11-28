package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.providers.CfirEmptySymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendMemberPeerPreparationInput
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeInputStamp
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeSources
import org.cangnova.cangjie.cfir.resolve.providers.prepareExtendMemberTypeMapping
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.services.CfirExtendInheritedInterfaceSemantic
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerConflictChain
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerConflictRelation
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerConflictWitness
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerDecision
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerDisposition
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerFacts
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerRelation
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.AbbreviatedTypeAttribute
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.withAbbreviation
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证完成事实的纯裁决与共享 producer 边界，不用本地类型系统的结果倒推规则期望。
 *
 * 规则证据：build/cjmp-source-supertype-20261124/extend-peer-facts-kotlin-map.md，
 * 固定 N 14276740ffec1262b81b6b5236b1d840af270b37 的 StructInheritanceChecker.cpp
 * 573–628，以及官方《接口扩展》的父接口优先与检查顺序冲突规则。
 * 三项关系是独立事实；equal 为 true 时，两个 subtype 标志都必须跳过。
 */
class CfirExtendMemberPeerFactsTest {
    private val sessionAndModule = ExtendTestFixtures.newSessionAndModule("extend-peer-facts")
    private val session = sessionAndModule.first
    private val indexStore = CfirExtendIndexStore(session)
    private val query = CfirExtendRuleQueryServiceImpl(session, indexStore)
    private val owner = DeclarationIdentity("owner")
    private val peer = DeclarationIdentity("peer")

    @Test
    fun `equal interfaces skip both subtype flags and do not establish an order`() {
        val shared = semantic("Shared")
        val input = facts(listOf(shared), listOf(shared.copy()), listOf(listOf(EQUAL)))

        assertDecision(CfirExtendMemberPeerDisposition.INCLUDE, input)
    }

    @Test
    fun `equal interface does not hide a later strict child peer`() {
        val parent = semantic("Parent")
        val child = semantic("Child")
        val input = facts(
            listOf(parent), listOf(parent.copy(), child),
            listOf(listOf(EQUAL, PEER_SUBTYPE)),
        )

        assertDecision(CfirExtendMemberPeerDisposition.EXCLUDE, input)
    }

    @Test
    fun `strict child peer is excluded and strict parent peer is included`() {
        val parent = semantic("Parent")
        val child = semantic("Child")

        assertDecision(
            CfirExtendMemberPeerDisposition.EXCLUDE,
            facts(listOf(parent), listOf(child), listOf(listOf(PEER_SUBTYPE))),
        )
        assertDecision(
            CfirExtendMemberPeerDisposition.INCLUDE,
            facts(listOf(child), listOf(parent), listOf(listOf(OWNER_SUBTYPE))),
        )
    }

    @Test
    fun `unrelated interface instances with the same class id are included`() {
        val left = semantic("Generic", "Generic<Int64>")
        val right = semantic("Generic", "Generic<String>")
        assertEquals(left.classId, right.classId)

        assertDecision(
            CfirExtendMemberPeerDisposition.INCLUDE,
            facts(listOf(left), listOf(right), listOf(listOf(UNRELATED))),
        )
    }

    @Test
    fun `bidirectional subtype in one unequal pair produces one two-edge chain`() {
        val left = semantic("OwnerInterface")
        val right = semantic("PeerInterface")
        val input = facts(listOf(left), listOf(right), listOf(listOf(BIDIRECTIONAL)))

        assertConflict(input, listOf(listOf(right to left, left to right)))
    }

    @Test
    fun `same owner row retains the last witness for each subtype direction`() {
        val middle = semantic("Middle")
        val earlyChild = semantic("EarlyChild")
        val earlyParent = semantic("EarlyParent")
        val unrelated = semantic("Unrelated")
        val lastChild = semantic("LastChild")
        val lastParent = semantic("LastParent")
        val input = facts(
            listOf(middle),
            listOf(earlyChild, earlyParent, unrelated, lastChild, lastParent, middle.copy()),
            listOf(listOf(PEER_SUBTYPE, OWNER_SUBTYPE, UNRELATED, PEER_SUBTYPE, OWNER_SUBTYPE, EQUAL)),
        )

        // equal 在最后仍不得覆盖 witness；同一行的两个方向属于一条连续链。
        assertConflict(input, listOf(listOf(lastChild to middle, middle to lastParent)))
    }

    @Test
    fun `cross owner row direction change keeps two chains and the latest prior witness`() {
        val firstParent = semantic("FirstParent")
        val latestParent = semantic("LatestParent")
        val neutral = semantic("Neutral")
        val otherChild = semantic("OtherChild")
        val firstChild = semantic("FirstChild")
        val latestChild = semantic("LatestChild")
        val otherParent = semantic("OtherParent")
        val input = facts(
            listOf(firstParent, latestParent, neutral, otherChild),
            listOf(firstChild, latestChild, otherParent),
            listOf(
                listOf(PEER_SUBTYPE, UNRELATED, UNRELATED),
                listOf(UNRELATED, PEER_SUBTYPE, UNRELATED),
                listOf(UNRELATED, UNRELATED, UNRELATED),
                listOf(UNRELATED, UNRELATED, OWNER_SUBTYPE),
            ),
        )

        // 无关系行不清除已确定的方向；跨行冲突的两条链不能拼成一条。
        assertConflict(input, listOf(listOf(latestChild to latestParent), listOf(otherChild to otherParent)))
    }

    @Test
    fun `cross owner row direction change is undecidable in the reverse order too`() {
        val child = semantic("Child")
        val parent = semantic("Parent")
        val peerParent = semantic("PeerParent")
        val peerChild = semantic("PeerChild")
        val input = facts(
            listOf(child, parent), listOf(peerParent, peerChild),
            listOf(listOf(OWNER_SUBTYPE, UNRELATED), listOf(UNRELATED, PEER_SUBTYPE)),
        )

        assertConflict(input, listOf(listOf(child to peerParent), listOf(peerChild to parent)))
    }

    @Test
    fun `consistent owner row directions survive intervening and trailing unrelated rows`() {
        val interfaces = listOf(semantic("First"), semantic("Neutral"), semantic("Second"), semantic("Trailing"))
        val peerInterfaces = listOf(semantic("PeerFirst"), semantic("PeerSecond"))
        for ((relation, expected) in listOf(
            PEER_SUBTYPE to CfirExtendMemberPeerDisposition.EXCLUDE,
            OWNER_SUBTYPE to CfirExtendMemberPeerDisposition.INCLUDE,
        )) {
            val input = facts(
                interfaces, peerInterfaces,
                listOf(
                    listOf(relation, UNRELATED), listOf(UNRELATED, UNRELATED),
                    listOf(UNRELATED, relation), listOf(UNRELATED, UNRELATED),
                ),
            )
            assertDecision(expected, input)
        }
    }

    @Test
    fun `completed initial disposition takes precedence over conflicting interface relations`() {
        val left = semantic("Left")
        val right = semantic("Right")
        for (initial in listOf(CfirExtendMemberPeerDisposition.INCLUDE, CfirExtendMemberPeerDisposition.EXCLUDE)) {
            val input = facts(listOf(left), listOf(right), listOf(listOf(BIDIRECTIONAL)), initial)

            assertDecision(initial, input)
        }
    }

    @Test
    fun `empty interface dimensions are valid and establish no exclusion`() {
        val left = semantic("Left")
        val right = semantic("Right")

        assertDecision(CfirExtendMemberPeerDisposition.INCLUDE, facts(emptyList(), emptyList(), emptyList()))
        assertDecision(CfirExtendMemberPeerDisposition.INCLUDE, facts(emptyList(), listOf(right), emptyList()))
        assertDecision(CfirExtendMemberPeerDisposition.INCLUDE, facts(listOf(left), emptyList(), listOf(emptyList())))
    }

    @Test
    fun `facts require one relation per ordered interface pair and reject undecidable initial state`() {
        val left = semantic("Left")
        val right = semantic("Right")

        assertThrows(IllegalArgumentException::class.java) { facts(listOf(left), listOf(right), emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { facts(listOf(left), listOf(right), listOf(emptyList())) }
        assertThrows(IllegalArgumentException::class.java) {
            facts(listOf(left), listOf(right), listOf(listOf(UNRELATED, UNRELATED)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            facts(listOf(left), listOf(right), listOf(listOf(UNRELATED), listOf(UNRELATED)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            facts(emptyList(), emptyList(), emptyList(), CfirExtendMemberPeerDisposition.UNDECIDABLE)
        }
    }

    @Test
    fun `fresh facts compare semantic contents while retaining original declaration identities`() {
        val left = semantic("Left")
        val right = semantic("Right")
        val input = facts(listOf(left), listOf(right), listOf(listOf(PEER_SUBTYPE)))
        val preparedAgain = facts(listOf(left.copy()), listOf(right.copy()), listOf(listOf(PEER_SUBTYPE.copy())))

        assertNotSame(input, preparedAgain)
        assertNotSame(input.ownerInterfaces.single(), preparedAgain.ownerInterfaces.single())
        assertNotSame(input.relations.single().single(), preparedAgain.relations.single().single())
        assertSame(owner, input.ownerDeclaration)
        assertSame(peer, input.peerDeclaration)
        assertEquals(input, preparedAgain)
        assertEquals(input.hashCode(), preparedAgain.hashCode())

        // 声明即使自身按值相等，也不能替换事实中的原 owner/peer 身份。
        val otherOwner = owner.copy()
        val otherPeer = peer.copy()
        assertEquals(owner, otherOwner)
        assertEquals(peer, otherPeer)
        assertNotSame(owner, otherOwner)
        assertNotSame(peer, otherPeer)
        assertNotEquals(input, facts(input.ownerInterfaces, input.peerInterfaces, input.relations, ownerDeclaration = otherOwner))
        assertNotEquals(input, facts(input.ownerInterfaces, input.peerInterfaces, input.relations, peerDeclaration = otherPeer))
    }

    @Test
    fun `interface occurrence order semantic payload initial state and all three flags belong to facts equality`() {
        val left = semantic("Left")
        val secondLeft = semantic("SecondLeft")
        val right = semantic("Right")
        val secondRight = semantic("SecondRight")
        val owners = listOf(left, secondLeft)
        val peers = listOf(right, secondRight)
        val matrix = listOf(listOf(PEER_SUBTYPE, UNRELATED), listOf(UNRELATED, OWNER_SUBTYPE))
        val input = facts(owners, peers, matrix)

        assertNotEquals(input, facts(owners.reversed(), peers, matrix))
        assertNotEquals(input, facts(owners, peers.reversed(), matrix))
        assertNotEquals(input, facts(listOf(left.copy(semanticKey = "Left<Int64>"), secondLeft), peers, matrix))
        assertNotEquals(input, facts(owners, listOf(right.copy(classId = null), secondRight), matrix))
        assertNotEquals(input, facts(owners, peers, matrix, CfirExtendMemberPeerDisposition.INCLUDE))
        for (changed in listOf(
            PEER_SUBTYPE.copy(equal = true),
            PEER_SUBTYPE.copy(peerSubtypeOfOwner = false),
            PEER_SUBTYPE.copy(ownerSubtypeOfPeer = true),
        )) {
            assertNotEquals(input, facts(owners, peers, listOf(listOf(changed, UNRELATED), matrix[1])))
        }
    }

    @Test
    fun `source list mutations cannot change frozen facts hash or a later decision`() {
        val left = semantic("Left")
        val right = semantic("Right")
        val owners = mutableListOf(left)
        val peers = mutableListOf(right)
        val row = mutableListOf(BIDIRECTIONAL)
        val matrix = mutableListOf<List<CfirExtendMemberPeerRelation>>(row)
        val input = facts(owners, peers, matrix)
        val sameValue = facts(listOf(left.copy()), listOf(right.copy()), listOf(listOf(BIDIRECTIONAL.copy())))
        val hashBefore = input.hashCode()
        val decisionBefore = query.extendMemberPeerDecision(input)

        owners[0] = semantic("ChangedOwner")
        peers.clear()
        row[0] = UNRELATED
        matrix[0] = emptyList()
        matrix.clear()

        assertEquals(listOf(left), input.ownerInterfaces)
        assertEquals(listOf(right), input.peerInterfaces)
        assertEquals(listOf(listOf(BIDIRECTIONAL)), input.relations)
        assertEquals(sameValue, input)
        assertEquals(hashBefore, input.hashCode())
        assertEquals(decisionBefore, query.extendMemberPeerDecision(input))
        assertConflict(input, listOf(listOf(right to left, left to right)))
    }

    @Test
    fun `published interface lists matrix and every matrix row reject mutation`() {
        val left = semantic("Left")
        val secondLeft = semantic("SecondLeft")
        val right = semantic("Right")
        val input = facts(
            listOf(left, secondLeft), listOf(right),
            listOf(listOf(PEER_SUBTYPE), listOf(UNRELATED)),
        )

        assertThrows(UnsupportedOperationException::class.java) {
            (input.ownerInterfaces as MutableList<CfirExtendInheritedInterfaceSemantic>)[0] = secondLeft
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (input.peerInterfaces as MutableList<CfirExtendInheritedInterfaceSemantic>).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (input.relations as MutableList<List<CfirExtendMemberPeerRelation>>)[0] = listOf(UNRELATED)
        }
        for (row in input.relations) {
            assertThrows(UnsupportedOperationException::class.java) {
                (row as MutableList<CfirExtendMemberPeerRelation>)[0] = BIDIRECTIONAL
            }
        }
        assertDecision(CfirExtendMemberPeerDisposition.EXCLUDE, input)
    }

    @Test
    fun `pure decision works in an unknown session with unavailable runtime providers`() {
        // fixture 提供真正绑定的 module 与 TypeComponents，但没有解析器、符号源或父图。
        assertSame(session, sessionAndModule.second.session)
        assertNull(session.symbolProviderForTest)
        assertNull(session.typeResolverForTest)
        assertNull(session.typeAwareSupertypeProviderOrNull)
        assertNull(session.directSupertypeProviderOrNull)
        assertNull(session.extendProviderOrNull)
        assertEquals(0L, indexStore.modificationCount)
        val left = semantic("Left")
        val right = semantic("Right")
        val input = facts(listOf(left), listOf(right), listOf(listOf(BIDIRECTIONAL)))

        assertConflict(input, listOf(listOf(right to left, left to right)))
        assertEquals(0L, indexStore.modificationCount)
    }

    @Test
    fun `pure decision neither invokes opaque callbacks nor reads a registered runtime provider`() {
        var callbackReads = 0
        val callback: () -> Nothing = {
            callbackReads++
            error("Pure peer decision must not execute a declaration callback")
        }
        val unavailable = UnavailableSupertypeProvider()
        session.register(CfirTypeAwareSupertypeProvider::class, unavailable)
        val left = semantic("Left")
        val right = semantic("Right")
        // Core 的 Any 声明槽只是身份；即使载荷可执行或来自另一 session，也不能解释它。
        val (unknownSession, _) = ExtendTestFixtures.newSessionAndModule("unknown-peer-owner")
        val input = facts(
            listOf(left), listOf(right), listOf(listOf(BIDIRECTIONAL)),
            ownerDeclaration = callback, peerDeclaration = unknownSession,
        )

        assertSame(callback, input.ownerDeclaration)
        assertSame(unknownSession, input.peerDeclaration)
        repeat(2) { assertConflict(input, listOf(listOf(right to left, left to right))) }
        assertEquals(0, callbackReads)
        assertEquals(0, unavailable.reads)
        assertEquals(0L, indexStore.modificationCount)
    }

    /** Source alias 的完成 RHS、节点展开缓存和声明旧 RHS 是三个不同的输入。 */
    @Test
    fun `producer source alias uses published RHS and reprepare retires the stale target key`() {
        val module = sessionAndModule.second
        val aliasId = ClassId(FqName("sample.peerfacts"), Name.identifier("TargetAlias"))
        val staleRhs = ConePrimitiveType.FLOAT64.toCfirResolvedTypeRef(null)
        val currentRhs = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
        val alias = buildTypeAlias {
            source = object : CjBinarySourceElement("TargetAlias", null, "facts:TargetAlias") {}
            moduleData = module
            resolvePhase = CfirResolvePhase.SUPER_TYPES
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            status = CfirDeclarationStatusImpl()
            scopeProvider = CfirCangJieScopeProvider()
            symbol = CfirTypeAliasSymbol(aliasId)
            name = aliasId.shortClassName
            expandedTypeRef = staleRhs
        }
        val graph = CfirSuperTypeGraphStore()
        graph.recordDeclared(alias, listOf(currentRhs), emptyList())
        graph.publishCompletedProducts()
        val selectedSymbols = AliasSymbolProvider(session, alias)
        val structural = UnavailableSupertypeProvider()
        val sharedId = ClassId(FqName("sample.peerfacts"), Name.identifier("Shared"))
        val shared = ExtendTestFixtures.newInterface(module, "Shared", sharedId)
        val sharedType = ConeClassLikeType(
            ConeClassLikeLookupTagWithFixedSymbol(shared.symbol.classId, shared.symbol), isInterface = true,
        )
        val cachedAlias = ConeTypeAliasType(aliasId, expandedType = ConePrimitiveType.FLOAT64)
        val cachedCarrier = ConePrimitiveType.FLOAT64.withAbbreviation(AbbreviatedTypeAttribute(cachedAlias))
        val ownerExtend = ExtendTestFixtures.newExtend(
            module, cachedCarrier.toCfirResolvedTypeRef(null), listOf(sharedType.toCfirResolvedTypeRef(null)),
        )
        val peerExtend = ExtendTestFixtures.newExtend(
            module, currentRhs, listOf(sharedType.toCfirResolvedTypeRef(null)),
        )
        var aliasReads = 0
        var allowAliasReads = true
        val input = CfirExtendMemberPeerPreparationInput(
            CfirTypeAwareSupertypeSources(session, selectedSymbols, null, graph), structural,
            cachedCarrier, ConePrimitiveType.INT64, listOf(sharedType), listOf(sharedType),
            emptyList(), emptyMap(),
        ) { selectedAlias ->
            check(allowAliasReads) { "Pure facts decision must not reread the Source alias product" }
            assertSame(alias, selectedAlias)
            aliasReads++
            val product = checkNotNull(graph.getCompletedSupertypeProduct(selectedAlias.symbol))
            (product.superTypeRefs.single() as CfirResolvedTypeRef).coneType
        }

        val before = query.prepareExtendMemberPeerFacts(ownerExtend, peerExtend, input)
        val preparedAgain = query.prepareExtendMemberPeerFacts(ownerExtend, peerExtend, input)
        assertNull(before.initialDisposition)
        assertEquals(listOf(shared.symbol.classId), before.ownerInterfaces.map { it.classId })
        assertEquals(before.ownerInterfaces, before.peerInterfaces)
        assertEquals(listOf(listOf(CfirExtendMemberPeerRelation(true, false, false))), before.relations)
        assertEquals(before, preparedAgain)
        assertEquals(before.hashCode(), preparedAgain.hashCode())
        val hashBefore = before.hashCode()
        assertTrue(aliasReads > 0)

        graph.recordDeclared(alias, listOf(staleRhs), emptyList())
        graph.publishCompletedProducts()
        val readsBeforeReprepare = aliasReads
        val after = query.prepareExtendMemberPeerFacts(ownerExtend, peerExtend, input)
        assertTrue(aliasReads > readsBeforeReprepare)
        assertEquals(CfirExtendMemberPeerDisposition.EXCLUDE, after.initialDisposition)
        assertEquals(emptyList<CfirExtendInheritedInterfaceSemantic>(), after.ownerInterfaces)
        assertEquals(emptyList<CfirExtendInheritedInterfaceSemantic>(), after.peerInterfaces)
        assertEquals(emptyList<List<CfirExtendMemberPeerRelation>>(), after.relations)
        assertNotEquals(before, after)
        assertSame(staleRhs, alias.expandedTypeRef)
        assertEquals(ConePrimitiveType.FLOAT64, cachedAlias.expandedType)

        allowAliasReads = false
        val readsAfterPreparation = aliasReads
        assertDecision(CfirExtendMemberPeerDisposition.INCLUDE, before)
        assertDecision(CfirExtendMemberPeerDisposition.EXCLUDE, after)
        assertEquals(hashBefore, before.hashCode())
        assertEquals(before, preparedAgain)
        assertEquals(readsAfterPreparation, aliasReads)
        assertEquals(0, structural.reads)
        assertEquals(0L, indexStore.modificationCount)
    }

    /** 固定 lookup tag 指向的实际声明不同，即使 nominal ClassId 相同也不能递归映射实参。 */
    @Test
    fun `member mapping distinguishes fixed declarations with the same class id`() {
        val module = sessionAndModule.second
        val id = ClassId(FqName("sample.peerfacts"), Name.identifier("Generic"))
        val parameter = ExtendTestFixtures.newTypeParameter(module, "T")
        val first = ExtendTestFixtures.newInterface(module, "Generic", id, typeParameters = listOf(parameter))
        val secondParameter = ExtendTestFixtures.newTypeParameter(module, "U")
        val second = ExtendTestFixtures.newInterface(module, "Generic", id, typeParameters = listOf(secondParameter))
        val source = ConeClassLikeType(
            ConeClassLikeLookupTagWithFixedSymbol(id, first.symbol),
            typeArguments = listOf(ExtendTestFixtures.typeParameterType(parameter)), isInterface = true,
        )
        fun destination(symbol: CfirClassLikeSymbol<*>) = ConeClassLikeType(
            ConeClassLikeLookupTagWithFixedSymbol(id, symbol),
            typeArguments = listOf(ConePrimitiveType.INT64), isInterface = true,
        )
        val symbols = CfirEmptySymbolProvider(session)
        val aliasExpansion: (CfirTypeAlias) -> ConeCangJieType? = { error("This input has no aliases") }

        assertEquals(first.symbol.classId, second.symbol.classId)
        assertNotSame(first.symbol, second.symbol)
        val matching = prepareExtendMemberTypeMapping(session, source, destination(first.symbol), symbols, aliasExpansion)
        assertEquals(mapOf(parameter.symbol.toLookupTag() to ConePrimitiveType.INT64), matching)
        val differentDeclaration = prepareExtendMemberTypeMapping(session, source, destination(second.symbol), symbols, aliasExpansion)
        assertEquals(emptyMap<TypeConstructorMarker, ConeCangJieType>(), differentDeclaration)
    }

    /** 重复 TP 属于同一次递归映射；第一项决定结果，不按后项覆盖，也不判为候选不适用。 */
    @Test
    fun `member mapping keeps the first destination of a repeated type parameter`() {
        val module = sessionAndModule.second
        val parameter = ExtendTestFixtures.newTypeParameter(module, "T")
        val type = ExtendTestFixtures.typeParameterType(parameter)
        val source = ConeTupleType(listOf(type, type))
        val symbols = CfirEmptySymbolProvider(session)
        val aliasExpansion: (CfirTypeAlias) -> ConeCangJieType? = { error("This input has no aliases") }
        val first = prepareExtendMemberTypeMapping(
            session, source, ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.FLOAT64)), symbols, aliasExpansion,
        )
        val reversed = prepareExtendMemberTypeMapping(
            session, source, ConeTupleType(listOf(ConePrimitiveType.FLOAT64, ConePrimitiveType.INT64)), symbols, aliasExpansion,
        )

        assertEquals(mapOf(parameter.symbol.toLookupTag() to ConePrimitiveType.INT64), first)
        assertEquals(mapOf(parameter.symbol.toLookupTag() to ConePrimitiveType.FLOAT64), reversed)
        assertThrows(UnsupportedOperationException::class.java) {
            (first as MutableMap<TypeConstructorMarker, ConeCangJieType>)[type.lookupTag] = ConePrimitiveType.FLOAT64
        }
        assertEquals(mapOf(parameter.symbol.toLookupTag() to ConePrimitiveType.INT64), first)
    }

    private fun facts(
        ownerInterfaces: List<CfirExtendInheritedInterfaceSemantic>,
        peerInterfaces: List<CfirExtendInheritedInterfaceSemantic>,
        relations: List<List<CfirExtendMemberPeerRelation>>,
        initialDisposition: CfirExtendMemberPeerDisposition? = null,
        ownerDeclaration: Any = owner,
        peerDeclaration: Any = peer,
    ): CfirExtendMemberPeerFacts = CfirExtendMemberPeerFacts(
        ownerDeclaration, peerDeclaration, initialDisposition, ownerInterfaces, peerInterfaces, relations,
    )

    private fun assertDecision(expected: CfirExtendMemberPeerDisposition, input: CfirExtendMemberPeerFacts) {
        val decision = query.extendMemberPeerDecision(input)
        assertEquals(expected, decision.disposition)
        assertNull(decision.conflictWitness)
    }

    /** 完整比较接口上下文、链数、链内方向与顺序，不只比较 UNDECIDABLE 枚举。 */
    private fun assertConflict(
        input: CfirExtendMemberPeerFacts,
        chains: List<List<Pair<CfirExtendInheritedInterfaceSemantic, CfirExtendInheritedInterfaceSemantic>>>,
    ) {
        val expected = CfirExtendMemberPeerConflictWitness(
            input.ownerInterfaces, input.peerInterfaces,
            chains.map { chain ->
                CfirExtendMemberPeerConflictChain(chain.map { (sub, superType) ->
                    CfirExtendMemberPeerConflictRelation(sub, superType)
                })
            },
        )
        assertEquals(
            CfirExtendMemberPeerDecision(CfirExtendMemberPeerDisposition.UNDECIDABLE, expected),
            query.extendMemberPeerDecision(input),
        )
    }

    private fun semantic(name: String, key: String = name): CfirExtendInheritedInterfaceSemantic =
        CfirExtendInheritedInterfaceSemantic(ClassId(FqName("sample.peerfacts"), Name.identifier(name)), key)

    /** 按值相等的测试身份，用来区分声明身份比较与普通 equals。 */
    private data class DeclarationIdentity(val name: String)

    /** 只注册本测试实际 Source alias；其它查询按正规空符号源处理。 */
    @OptIn(CfirSymbolProviderInternals::class)
    private class AliasSymbolProvider(session: CfirSession, private val alias: CfirTypeAlias) : CfirSymbolProvider(session) {
        private val empty = CfirEmptySymbolProvider(session)

        override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider() {
            override val hasSpecificClassifierPackageNamesComputation: Boolean = true
            override val hasSpecificCallablePackageNamesComputation: Boolean = true
            override fun getPackageNames(): Set<String> = setOf(alias.symbol.classId.packageFqName.asString())
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                if (packageFqName == alias.symbol.classId.packageFqName) setOf(alias.name) else emptySet()
            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
        }

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
            if (classId == alias.symbol.classId) alias.symbol else empty.getClassLikeSymbolByClassId(classId)

        override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) =
            empty.getTopLevelCallableSymbolsTo(destination, packageFqName, name)

        override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) =
            empty.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)

        override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) =
            empty.getTopLevelPropertySymbolsTo(destination, packageFqName, name)

        override fun hasPackage(fqName: FqName): Boolean = fqName == alias.symbol.classId.packageFqName
    }

    /** 这是读取陷阱，不提供任何伪父图或生产根；每个入口一旦被读取就立即失败。 */
    private class UnavailableSupertypeProvider : CfirTypeAwareSupertypeProvider {
        var reads: Int = 0
            private set

        private fun unexpectedRead(): Nothing {
            reads++
            error("Pure peer decision must not read a runtime supertype provider")
        }

        override val inputStamp: CfirTypeAwareSupertypeInputStamp
            get() = unexpectedRead()

        override fun forSources(sources: CfirTypeAwareSupertypeSources): CfirTypeAwareSupertypeProvider = unexpectedRead()

        override fun getDirectSupertypeDescriptors(type: ConeCangJieType): List<CfirInstantiatedSupertypeDescriptor> =
            unexpectedRead()

        override fun getDirectSupertypes(type: ConeCangJieType): List<ConeCangJieType> = unexpectedRead()

        override fun getPredicateSupertypes(type: ConeCangJieType): List<ConeCangJieType> = unexpectedRead()
    }

    private companion object {
        val UNRELATED = CfirExtendMemberPeerRelation(equal = false, peerSubtypeOfOwner = false, ownerSubtypeOfPeer = false)
        val PEER_SUBTYPE = CfirExtendMemberPeerRelation(equal = false, peerSubtypeOfOwner = true, ownerSubtypeOfPeer = false)
        val OWNER_SUBTYPE = CfirExtendMemberPeerRelation(equal = false, peerSubtypeOfOwner = false, ownerSubtypeOfPeer = true)
        val BIDIRECTIONAL = CfirExtendMemberPeerRelation(equal = false, peerSubtypeOfOwner = true, ownerSubtypeOfPeer = true)
        val EQUAL = CfirExtendMemberPeerRelation(equal = true, peerSubtypeOfOwner = true, ownerSubtypeOfPeer = true)

        val CfirSession.symbolProviderForTest: CfirSymbolProvider? by CfirSession.nullableSessionComponentAccessor()
        val CfirSession.typeResolverForTest: CfirTypeResolver? by CfirSession.nullableSessionComponentAccessor()
    }
}
