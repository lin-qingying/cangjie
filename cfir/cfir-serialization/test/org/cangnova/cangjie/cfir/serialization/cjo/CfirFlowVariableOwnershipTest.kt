package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.deserialization.SingleModuleDataProvider
import org.cangnova.cangjie.cfir.entrypoint.configuration.createForCfirFrontend
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.resolve.dfa.*
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import kotlin.test.*

/**
 * 固定 Kotlin 3c7a5041 的 Flow/LogicSystem 变量所有权合同。
 * 输入有实际绑定声明、解析类型和 receiver；只观测公开查询、赋值、合流及 SOURCE 访问。
 * 这些是 DFA primitive 行为测试，不证明 native For/replay 的 G3 readiness。
 */
class CfirFlowVariableOwnershipTest : SessionHolder {
    private val libraryModule = CfirBinaryDependenciesModuleData(Name.special("<flow-ownership-dependencies>"))
    private val module = CfirSourceModuleData(
        name = Name.identifier("flow-ownership"), dependencies = listOf(libraryModule), refinementDependencies = emptyList(),
        targetPlatform = CangJiePlatforms.defaultCangJiePlatform, platform = CfirPlatform.DEFAULT,
    )
    private val intType = ConePrimitiveType(PrimitiveTypeKind.INT64)
    private val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
    private val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
    private val ownerId = ClassId(FqName("flowownership"), Name.identifier("Owner"))
    private val ownerType = ConeClassLikeType(ConeClassLikeLookupTagImpl(ownerId))
    override val session: CfirSession = createSession()
    private val logic = object : LogicSystem(session.typeContext) {}

    @Test
    fun branchRememberDoesNotPolluteBaseOrPeerAndJoinKeepsCanonicalInstances() {
        val shared = binding("shared")
        val leftOnly = binding("leftOnly")
        val rightOnly = binding("rightOnly")
        val commonNew = binding("commonNew")
        val root = MutableFlow()
        val sharedVariable = root.rememberReal(access(shared))
        val base = root.freeze()
        val left = base.fork()
        val right = base.fork()
        val leftVariable = left.rememberReal(access(leftOnly))
        val rightVariable = right.rememberReal(access(rightOnly))
        val leftCommon = left.rememberReal(access(commonNew))
        val rightCommon = right.rememberReal(access(commonNew))
        assertEquals(leftCommon, rightCommon)
        assertNotSame(leftCommon, rightCommon)
        assertNull(base.getVariableIfKnown(access(leftOnly)))
        assertNull(base.getVariableIfKnown(access(rightOnly)))
        assertNull(base.getVariableIfKnown(access(commonNew)))
        assertNull(right.getVariableIfKnown(access(leftOnly)))
        assertNull(left.getVariableIfKnown(access(rightOnly)))
        assertSame(leftVariable, left.rememberReal(access(leftOnly)))
        assertSame(rightVariable, right.getVariableIfKnown(access(rightOnly)))
        // 非登记查询产生原型，不能让随后 getVariableIfKnown 成功。
        assertIs<RealVariable>(base.getVariable(access(leftOnly)))
        assertNull(base.getVariableIfKnown(access(leftOnly)))
        val inputs = listOf(left.freeze(), right.freeze())
        val joined = logic.joinFlow(inputs, inputs, union = false, valueOwner = shared)
        assertSame(sharedVariable, joined.getVariableIfKnown(access(shared)))
        assertSame(leftVariable, joined.getVariableIfKnown(access(leftOnly)))
        assertSame(rightVariable, joined.getVariableIfKnown(access(rightOnly)))
        // 固定 Kotlin mergeVariables 按输入次序合并规范表，后一条流提供同键实例。
        assertSame(rightCommon, joined.getVariableIfKnown(access(commonNew)))
        assertSame(rightCommon, joined.rememberReal(access(commonNew)))
        assertNull(base.getVariableIfKnown(access(commonNew)))
    }

    @Test
    fun receiverAssignmentInvalidatesRecursiveMembersOnlyInItsBranchAndAtJoin() {
        val child = property("child", ownerType)
        val value = property("value", ConeAnyType)
        owner(listOf(child, value))
        val receiver = binding("receiver", ownerType, mutable = true)
        val unrelatedReceiver = binding("unrelatedReceiver", ownerType)
        val condition = binding("condition", boolType)
        val root = MutableFlow()
        val receiverVariable = root.rememberReal(access(receiver))
        val member = root.rememberReal(access(child, access(receiver)))
        val nested = root.rememberReal(access(value, access(child, access(receiver))))
        val unrelated = root.rememberReal(access(value, access(unrelatedReceiver)))
        val predicate = root.rememberReal(access(condition))
        logic.addImplication(root, Implication(OperationStatement(predicate, Operation.EqTrue), fact(nested)))
        logic.addTypeStatement(root, fact(member, ownerType))
        logic.addTypeStatement(root, fact(nested))
        logic.addTypeStatement(root, fact(unrelated))
        val base = root.freeze()
        val left = base.fork()
        val right = base.fork()
        logic.recordNewAssignment(left, receiverVariable, 1)
        assertNull(left.getTypeStatement(member))
        assertNull(left.getTypeStatement(nested))
        assertNull(left.getImplications(predicate))
        assertTypes(left.getTypeStatement(unrelated), unrelated, intType)
        for (flow in listOf(base, right)) {
            assertTypes(flow.getTypeStatement(member), member, ownerType)
            assertTypes(flow.getTypeStatement(nested), nested, intType)
            assertEquals(1, checkNotNull(flow.getImplications(predicate)).size)
            assertTrue(logic.isSameValueIn(base, flow, receiverVariable))
        }
        assertFalse(logic.isSameValueIn(base, left, receiverVariable))
        val inputs = listOf(left.freeze(), right.freeze())
        val joined = logic.joinFlow(inputs, inputs, union = false, valueOwner = receiver)
        assertNull(joined.getTypeStatement(member))
        assertNull(joined.getTypeStatement(nested))
        assertTypes(joined.getTypeStatement(unrelated), unrelated, intType)
        assertFalse(logic.isSameValueIn(base, joined, receiverVariable))
    }

    @Test
    fun joinedBranchOnlyMembersRemainIndexedForReceiverInvalidation() {
        val value = property("value", ConeAnyType)
        owner(listOf(value))
        val receiver = binding("receiver", ownerType, mutable = true)
        val root = MutableFlow()
        val receiverVariable = root.rememberReal(access(receiver))
        val base = root.freeze()
        val left = base.fork()
        val right = base.fork()
        val member = left.rememberReal(access(value, access(receiver)))
        assertNull(base.getVariableIfKnown(access(value, access(receiver))))
        assertNull(right.getVariableIfKnown(access(value, access(receiver))))
        val inputs = listOf(left.freeze(), right.freeze())
        val joined = logic.joinFlow(inputs, inputs, union = false, valueOwner = receiver)
        assertSame(member, joined.getVariableIfKnown(access(value, access(receiver))))
        logic.addTypeStatement(joined, fact(member))
        logic.recordNewAssignment(joined, receiverVariable, 2)
        assertNull(joined.getTypeStatement(member))
        assertNull(base.getVariableIfKnown(access(value, access(receiver))))
    }

    @Test
    fun oneWayFactsAreTransitiveTowardsUnderlyingAndQueriesNeverWriteBack() {
        val root = MutableFlow()
        val underlying = root.rememberReal(access(binding("underlying")))
        val alias = root.rememberReal(access(binding("alias")))
        val leaf = root.rememberReal(access(binding("leaf")))
        logic.addOneWayAlias(root, alias, underlying)
        logic.addOneWayAlias(root, leaf, alias)
        logic.addTypeStatement(root, fact(underlying, ConeAnyType))
        logic.addTypeStatement(root, fact(leaf))
        repeat(3) {
            assertTypes(root.getTypeStatementWithOneWayData(underlying), underlying, ConeAnyType, intType)
            assertTypes(root.getTypeStatementWithOneWayData(alias), alias, intType)
            assertTypes(root.getTypeStatement(underlying), underlying, ConeAnyType)
            assertNull(root.getTypeStatement(alias))
        }
        assertTypes(root.getTypeStatementWithOneWayData(leaf), leaf, intType)
        assertSame(alias, root.unwrapVariable(alias))
        assertSame(underlying, root.unwrapVariable(underlying))
    }

    @Test
    fun underlyingFactsDoNotPropagateBackIntoOneWayAlias() {
        val flow = MutableFlow()
        val underlying = flow.rememberReal(access(binding("underlying")))
        val alias = flow.rememberReal(access(binding("alias")))
        logic.addOneWayAlias(flow, alias, underlying)
        logic.addTypeStatement(flow, fact(underlying))
        assertNull(flow.getTypeStatement(alias))
        assertNull(flow.getTypeStatementWithOneWayData(alias))
        assertTypes(flow.getTypeStatementWithOneWayData(underlying), underlying, intType)
    }

    @Test
    fun oneWayRelationsAndFactsAreIsolatedAcrossForkAndFreeze() {
        val root = MutableFlow()
        val underlying = root.rememberReal(access(binding("underlying")))
        val alias = root.rememberReal(access(binding("alias", mutable = true)))
        logic.addOneWayAlias(root, alias, underlying)
        logic.addTypeStatement(root, fact(alias))
        val base = root.freeze()
        val left = base.fork()
        val right = base.fork()
        val frozenLeft = left.freeze()
        logic.recordNewAssignment(left, alias, 3)
        assertNull(left.getTypeStatementWithOneWayData(underlying))
        for (flow in listOf(base, right, frozenLeft)) {
            assertTypes(flow.getTypeStatementWithOneWayData(underlying), underlying, intType)
            assertNull(flow.getTypeStatement(underlying))
            assertTypes(flow.getTypeStatement(alias), alias, intType)
        }
        val branchOnly = left.rememberReal(access(binding("branchOnly")))
        logic.addOneWayAlias(left, branchOnly, underlying)
        logic.addTypeStatement(left, fact(branchOnly))
        val snapshot = left.freeze()
        logic.recordNewAssignment(left, underlying, 4)
        assertNull(left.getTypeStatementWithOneWayData(underlying))
        assertTypes(snapshot.getTypeStatementWithOneWayData(underlying), underlying, intType)
        assertNull(base.getVariableIfKnown(branchOnly))
        assertNull(right.getVariableIfKnown(branchOnly))
    }

    @Test
    fun reassigningOneWayAliasRemovesAllUsesWithoutConcurrentModification() {
        val flow = MutableFlow()
        val alias = flow.rememberReal(access(binding("alias", mutable = true)))
        val survivor = flow.rememberReal(access(binding("survivor")))
        val targets = (0 until 16).map { flow.rememberReal(access(binding("target$it"))) }
        targets.forEach { logic.addOneWayAlias(flow, alias, it) }
        logic.addOneWayAlias(flow, survivor, targets.first())
        logic.addTypeStatement(flow, fact(alias))
        logic.recordNewAssignment(flow, alias, 5)
        logic.addTypeStatement(flow, fact(alias))
        targets.forEach { assertNull(flow.getTypeStatementWithOneWayData(it)) }
        // 重新给旧 alias 加事实不能复活关系；同一底层变量的其它 alias 仍有效。
        logic.addTypeStatement(flow, fact(survivor))
        assertTypes(flow.getTypeStatementWithOneWayData(targets.first()), targets.first(), intType)
        targets.drop(1).forEach { assertNull(flow.getTypeStatementWithOneWayData(it)) }
    }

    @Test
    fun reassigningUnderlyingClearsItsOneWayRelationButPreservesAliasFacts() {
        val flow = MutableFlow()
        val underlying = flow.rememberReal(access(binding("underlying", mutable = true)))
        val alias = flow.rememberReal(access(binding("alias")))
        logic.addOneWayAlias(flow, alias, underlying)
        logic.addTypeStatement(flow, fact(alias))
        logic.recordNewAssignment(flow, underlying, 6)
        assertNull(flow.getTypeStatementWithOneWayData(underlying))
        assertTypes(flow.getTypeStatement(alias), alias, intType)
    }

    @Test
    fun reassigningDirectAliasKeepsOneWayRelationsOfTheUnchangedRepresentative() {
        val flow = MutableFlow()
        val underlying = flow.rememberReal(access(binding("underlying")))
        val directAlias = flow.rememberReal(access(binding("directAlias", mutable = true)))
        val incoming = flow.rememberReal(access(binding("incoming")))
        logic.addLocalVariableAlias(flow, directAlias, underlying)
        logic.addOneWayAlias(flow, incoming, underlying)
        logic.addTypeStatement(flow, fact(incoming))
        val before = flow.freeze()
        logic.recordNewAssignment(flow, directAlias, 14)
        assertSame(directAlias, flow.unwrapVariable(directAlias))
        assertNull(flow.getTypeStatementWithOneWayData(directAlias))
        assertTypes(flow.getTypeStatementWithOneWayData(underlying), underlying, intType)
        assertSame(underlying, before.unwrapVariable(directAlias))
        assertTypes(before.getTypeStatementWithOneWayData(directAlias), directAlias, intType)
        assertTrue(logic.isSameValueIn(before, flow, underlying))
        assertFalse(logic.isSameValueIn(before, flow, directAlias))
    }

    @Test
    fun representativeReassignmentMigratesIncomingAndOutgoingOneWayRelationsToSibling() {
        val flow = MutableFlow()
        val representative = flow.rememberReal(access(binding("representative", mutable = true)))
        val sibling = flow.rememberReal(access(binding("sibling")))
        val incoming = flow.rememberReal(access(binding("incoming")))
        val outgoing = flow.rememberReal(access(binding("outgoing")))
        logic.addLocalVariableAlias(flow, sibling, representative)
        logic.addOneWayAlias(flow, incoming, representative)
        logic.addOneWayAlias(flow, representative, outgoing)
        logic.addTypeStatement(flow, fact(incoming))
        val before = flow.freeze()
        logic.recordNewAssignment(flow, representative, 7)
        assertSame(sibling, flow.unwrapVariable(sibling))
        assertNull(flow.getTypeStatementWithOneWayData(representative))
        assertTypes(flow.getTypeStatementWithOneWayData(sibling), sibling, intType)
        assertTypes(flow.getTypeStatementWithOneWayData(outgoing), outgoing, intType)
        assertNull(flow.getTypeStatement(sibling))
        assertNull(flow.getTypeStatement(outgoing))
        assertSame(representative, before.unwrapVariable(sibling))
        assertTypes(before.getTypeStatementWithOneWayData(representative), representative, intType)
        // outgoing 应已指向 sibling；改变旧代表的事实不会再向 outgoing 传播。
        logic.recordNewAssignment(flow, incoming, 8)
        logic.addTypeStatement(flow, fact(representative))
        assertNull(flow.getTypeStatementWithOneWayData(outgoing))
        logic.addTypeStatement(flow, fact(sibling))
        assertTypes(flow.getTypeStatementWithOneWayData(outgoing), outgoing, intType)
        logic.recordNewAssignment(flow, sibling, 9)
        assertNull(flow.getTypeStatementWithOneWayData(outgoing))
    }

    @Test
    fun receiverRepresentativeChangeMovesMemberFactsToSiblingReceiver() {
        val value = property("value", ConeAnyType)
        val owner = owner(listOf(value))
        val receiverType = ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(owner.symbol.classId, owner.symbol))
        val receiver = binding("receiver", receiverType, mutable = true)
        val siblingDeclaration = binding("sibling", receiverType)
        val flow = MutableFlow()
        val original = flow.rememberReal(access(receiver))
        val sibling = flow.rememberReal(access(siblingDeclaration))
        val initialized = logic.initializeEntry(flow, original, receiver, receiverType)
        logic.initializeCompletedValue(flow, sibling, initialized, siblingDeclaration)
        val member = flow.rememberReal(access(value, access(receiver)))
        logic.addTypeStatement(flow, fact(member))
        val before = flow.freeze()
        logic.recordNewAssignment(flow, original, 10)
        val moved = assertIs<RealVariable>(flow.getVariableIfKnown(access(value, access(siblingDeclaration))))
        assertSame(sibling, moved.dispatchReceiver)
        assertNull(flow.getTypeStatement(member))
        assertTypes(flow.getTypeStatement(moved), moved, intType)
        assertTypes(before.getTypeStatement(member), member, intType)
        logic.recordNewAssignment(flow, sibling, 11)
        assertNull(flow.getTypeStatement(moved))
    }

    @Test
    fun commonJoinKeepsOnlyOneWayRelationsSharedByBothBranchesWithoutMaterializingFacts() {
        val root = MutableFlow()
        val underlying = root.rememberReal(access(binding("underlying")))
        val alias = root.rememberReal(access(binding("alias", mutable = true)))
        logic.addTypeStatement(root, fact(alias))
        val base = root.freeze()
        val left = base.fork()
        val right = base.fork()
        logic.addOneWayAlias(left, alias, underlying)
        fun join(): MutableFlow {
            val inputs = listOf(left.freeze(), right.freeze())
            return logic.joinFlow(inputs, inputs, union = false, valueOwner = alias.symbol.cfir)
        }
        val unshared = join()
        assertNull(unshared.getTypeStatementWithOneWayData(underlying))
        logic.addOneWayAlias(right, alias, underlying)
        val shared = join()
        assertTypes(shared.getTypeStatementWithOneWayData(underlying), underlying, intType)
        assertNull(shared.getTypeStatement(underlying))
        logic.recordNewAssignment(left, alias, 12)
        val invalidated = join()
        assertNull(invalidated.getTypeStatementWithOneWayData(underlying))
        assertNull(invalidated.getTypeStatement(underlying))
        assertTypes(right.getTypeStatementWithOneWayData(underlying), underlying, intType)
    }

    @Test
    fun sourceVisitorCoversCanonicalAndMemberOnlyStorageWithActualDeclarationsAndTypes() {
        val value = property("value", ConeAnyType)
        val owner = owner(listOf(value))
        val receiver = binding("receiver", ownerType)
        val standalone = binding("standalone", intType)
        val flow = MutableFlow()
        flow.rememberReal(access(standalone))
        val member = flow.rememberReal(access(value, access(receiver)))
        val thisAccess = buildThisReceiverExpression {
            source = source("this")
            coneTypeOrNull = ownerType
            calleeReference = buildThisReference { boundSymbol = owner.symbol; isImplicit = false }
        }
        flow.rememberReal(thisAccess)
        assertTrue(flow.knownVariables.isEmpty())
        assertNull(flow.getTypeStatement(member))
        for (stored in listOf<Flow>(flow, flow.freeze(), flow.freeze().fork())) {
            assertSource(stored, listOf(standalone, value, receiver, owner), listOf(intType, ConeAnyType, ownerType))
            assertTrue(stored.knownVariables.isEmpty())
        }
    }

    @Test
    fun sourceVisitorCoversBothEndpointsAndReceiverOfUnrememberedOneWayOnlyVariables() {
        val value = property("value", ConeAnyType)
        owner(listOf(value))
        val receiver = binding("receiver", ownerType)
        val aliasDeclaration = binding("alias", intType)
        val alias = real(access(aliasDeclaration))
        val underlying = real(access(value, access(receiver)))
        val flow = MutableFlow()
        logic.addOneWayAlias(flow, alias, underlying)
        assertTrue(flow.knownVariables.isEmpty())
        assertNull(flow.getVariableIfKnown(alias))
        assertNull(flow.getVariableIfKnown(underlying))
        for (stored in listOf<Flow>(flow, flow.freeze(), flow.freeze().fork())) {
            assertSource(stored, listOf(aliasDeclaration, value, receiver), listOf(intType, ConeAnyType, ownerType))
            assertTrue(stored.knownVariables.isEmpty())
            assertNull(stored.getTypeStatementWithOneWayData(underlying))
        }
    }

    @Test
    fun typedParameterLocalContainerAndPatternBindingAccessesHaveRealDistinctSymbolIdentity() {
        val ownerSymbol = CfirNamedFunctionSymbol(CallableId(FqName("flowownership"), Name.identifier("execute")))
        val parameter = parameter("argument", intType, ownerSymbol)
        val localBinding = binding("local", intType)
        val local = local(localBinding, integer(1))
        val patternBinding = binding("element", intType)
        val pattern = pattern(patternBinding)
        val twin = binding("local", intType)
        val function = buildNamedFunction {
            source = source("execute")
            moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY; isLocal = false; status = CfirDeclarationStatusImpl()
            symbol = ownerSymbol; name = ownerSymbol.name; returnTypeRef = typeRef(unitType); isMut = false
            valueParameters += parameter
            body = buildBlock { coneTypeOrNull = unitType; statements += local; statements += access(parameter) }
        }
        assertSame(function.symbol, parameter.containingDeclarationSymbol)
        assertSame(localBinding, assertIs<CfirBindingPattern>(local.pattern).bindingVariable)
        assertSame(patternBinding, pattern.bindingVariable)
        val flow = MutableFlow()
        for (declaration in listOf<CfirVariable>(parameter, local, localBinding, patternBinding, twin)) {
            val variable = flow.rememberReal(access(declaration))
            assertSame(declaration.symbol, variable.symbol)
            assertSame(declaration, variable.symbol.cfir)
            assertSame(intType, variable.originalType)
            assertSame(variable, flow.getVariableIfKnown(access(declaration)))
        }
        val originalVariable = flow.rememberReal(access(localBinding))
        val twinVariable = flow.rememberReal(access(twin))
        assertEquals(localBinding.symbol.callableId, twin.symbol.callableId)
        assertNotSame(localBinding.symbol, twin.symbol)
        assertNotEquals(originalVariable, twinVariable)
        logic.addTypeStatement(flow, fact(originalVariable))
        assertNull(flow.getTypeStatement(twinVariable))
    }

    @Test
    fun sparseSymbolMapperKeepsBorrowedReferencesAndUsesEffectiveMemberAndReceiverTargets() {
        val originalProperty = property("value", ConeAnyType)
        val effectiveProperty = property("value", ConeAnyType)
        owner(listOf(originalProperty))
        owner(listOf(effectiveProperty))
        val originalReceiver = binding("receiver", ownerType, mutable = true)
        val effectiveReceiver = binding("receiver", ownerType, mutable = true)
        val borrowedReceiver = access(originalReceiver)
        val borrowedAccess = access(originalProperty, borrowedReceiver)
        val reference = assertIs<CfirResolvedNamedReference>(borrowedAccess.calleeReference)
        val receiverReference = assertIs<CfirResolvedNamedReference>(borrowedReceiver.calleeReference)
        val mappings = mapOf<CfirBasedSymbol<*>, CfirBasedSymbol<*>>(
            originalProperty.symbol to effectiveProperty.symbol,
            originalReceiver.symbol to effectiveReceiver.symbol,
        )
        val mapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*> = { mappings[it] ?: it }
        val flow = MutableFlow()
        val effective = assertIs<RealVariable>(flow.remember(borrowedAccess, symbolMapper = mapper))
        assertSame(effectiveProperty.symbol, effective.symbol)
        assertSame(effectiveProperty, effective.symbol.cfir)
        val receiver = checkNotNull(effective.dispatchReceiver)
        assertSame(effectiveReceiver.symbol, receiver.symbol)
        assertSame(effectiveReceiver, receiver.symbol.cfir)
        assertSame(effective, flow.getVariableIfKnown(borrowedAccess, symbolMapper = mapper))
        assertSame(receiver, flow.getVariableIfKnown(borrowedReceiver, symbolMapper = mapper))
        assertNull(flow.getVariableIfKnown(borrowedAccess))
        assertNull(flow.getVariableIfKnown(borrowedReceiver))
        val unrelated = binding("unmapped", intType)
        assertSame(unrelated.symbol, assertIs<RealVariable>(flow.remember(access(unrelated), symbolMapper = mapper)).symbol)
        assertSame(reference, borrowedAccess.calleeReference)
        assertSame(originalProperty.symbol, reference.resolvedSymbol)
        assertSame(borrowedReceiver, borrowedAccess.dispatchReceiver)
        assertSame(borrowedReceiver, borrowedAccess.explicitReceiver)
        assertSame(receiverReference, borrowedReceiver.calleeReference)
        assertSame(originalReceiver.symbol, receiverReference.resolvedSymbol)
        logic.addTypeStatement(flow, fact(effective))
        val before = flow.freeze()
        logic.recordNewAssignment(flow, receiver, 13)
        assertNull(flow.getTypeStatement(effective))
        assertTypes(before.getTypeStatement(effective), effective, intType)
        assertSame(originalProperty.symbol, reference.resolvedSymbol)
        assertSame(originalReceiver.symbol, receiverReference.resolvedSymbol)
    }

    /** 用真实 shared→Library→source 链创建类型系统，不能以缺依赖的裸 session 替代。 */
    private fun createSession(): CfirSession {
        val configuration = CompilerConfiguration.createForCfirFrontend()
        val factory = CfirDefaultSessionFactory()
        val shared = factory.createSharedLibrarySession(module.name, emptyList(), configuration.languageVersionSettings)
        factory.createLibrarySession(shared, SingleModuleDataProvider(libraryModule), emptyList(), configuration.languageVersionSettings)
        return factory.createSourceSession(module, emptyList(), configuration)
    }

    private fun MutableFlow.rememberReal(expression: CfirExpression): RealVariable = assertIs<RealVariable>(remember(expression))
    private fun real(expression: CfirExpression): RealVariable = assertIs<RealVariable>(DataFlowVariable.of(expression, unwrapInReceivers = { it }))
    private fun fact(variable: DataFlowVariable, type: ConeCangJieType = intType): MutableTypeStatement =
        MutableTypeStatement(variable, linkedSetOf(type))

    private fun assertTypes(statement: TypeStatement?, variable: DataFlowVariable, vararg types: ConeCangJieType) {
        val actual = assertNotNull(statement)
        assertSame(variable, actual.variable)
        assertEquals(types.toSet(), actual.upperTypes)
        assertTrue(actual.lowerTypes.isEmpty())
    }

    private fun assertSource(flow: Flow, expectedElements: List<CfirElement>, expectedTypes: List<ConeCangJieType>) {
        val elements = mutableListOf<CfirElement>()
        val types = mutableListOf<ConeCangJieType>()
        flow.acceptSourceChildren(createDfaSourceVisitor({ elements += it }, { types += it }, { true }))
        expectedElements.forEach { expected -> assertTrue(elements.any { it === expected }, "Missing actual declaration ${expected::class}") }
        expectedTypes.forEach { expected -> assertTrue(types.any { it === expected }, "Missing actual type $expected") }
        assertTrue(elements.all { element -> expectedElements.any { it === element } })
    }

    /** 普通局部与模式名称均有实际 binding symbol；初始化由外层模式声明承载。 */
    private fun binding(name: String, type: ConeCangJieType = ConeAnyType, mutable: Boolean = false): CfirPatternBindingVariable =
        buildPatternBindingVariable {
            source = source(name)
            moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY; isLocal = true; status = CfirDeclarationStatusImpl()
            this.name = Name.identifier(name); symbol = CfirPatternBindingSymbol(CallableId(this.name))
            returnTypeRef = typeRef(type); initializer = null; isVar = mutable
        }

    private fun pattern(binding: CfirPatternBindingVariable): CfirBindingPattern = buildBindingPattern {
        source = binding.source; name = binding.name; bindingVariable = binding
        semanticTypeRef = binding.returnTypeRef; typeRef = binding.returnTypeRef
    }

    private fun local(binding: CfirPatternBindingVariable, initializer: CfirExpression): CfirPatternVariable = buildPatternVariable {
        source = binding.source
        moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY; isLocal = true; status = CfirDeclarationStatusImpl()
        symbol = CfirPatternVariableSymbol(binding.symbol.callableId); returnTypeRef = binding.returnTypeRef
        pattern = pattern(binding); this.initializer = initializer; isVar = binding.isVar
    }

    private fun parameter(name: String, type: ConeCangJieType, owner: CfirBasedSymbol<*>): CfirValueParameter = buildValueParameter {
        source = source(name)
        moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY; isLocal = true; isNamed = false
        this.name = Name.identifier(name); symbol = CfirValueParameterSymbol(CallableId(this.name))
        containingDeclarationSymbol = owner; returnTypeRef = typeRef(type)
    }

    private fun property(name: String, type: ConeCangJieType): CfirProperty = buildProperty {
        source = source(name)
        moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY; isLocal = false; status = CfirDeclarationStatusImpl()
        this.name = Name.identifier(name); symbol = CfirPropertySymbol(CallableId(ownerId, this.name))
        dispatchReceiverType = ownerType; returnTypeRef = typeRef(type)
    }

    private fun owner(members: List<CfirProperty>): CfirClass = buildClass {
        source = source(ownerId.shortClassName.asString())
        moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE; origin = CfirDeclarationOrigin.Source
        attributes = CfirDeclarationAttributes.EMPTY; status = CfirDeclarationStatusImpl()
        scopeProvider = CfirCangJieScopeProvider(); symbol = CfirClassSymbol(ownerId); name = ownerId.shortClassName
        declarations += members
    }

    private fun access(declaration: CfirCallableDeclaration, receiver: CfirExpression? = null): CfirNamedAccessExpression =
        buildNamedAccessExpression {
            source = declaration.source
            coneTypeOrNull = assertIs<CfirResolvedTypeRef>(declaration.returnTypeRef).coneType
            calleeReference = buildResolvedNamedReference {
                source = declaration.source; name = declaration.symbol.name; resolvedSymbol = declaration.symbol
            }
            explicitReceiver = receiver; dispatchReceiver = receiver
        }

    private fun integer(value: Int): CfirLiteralExpression = buildLiteralExpression {
        kind = CfirLiteralKind.INT; this.value = value.toLong(); coneTypeOrNull = intType
    }
    private fun typeRef(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { coneType = type }
    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}
}
