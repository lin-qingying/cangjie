package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.LanguageVersionSettingsImpl
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
import org.cangnova.cangjie.cfir.originalForSubstitutionOverrideAttr
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.dfa.*
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirFakeOverrideGenerator
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.test.testFramework.CjPlatformLiteFixture
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

/**
 * G3 first gate R1–R4 的公开工厂边界：真实声明、typed selector 与借用 reference 的有效身份。
 * 固定 K=3c7a5041、N=14276740、R=4243106b；这些 primitive 断言不证明 G3 replay readiness。
 */
class CfirDataFlowVariableFactoryBoundaryTest : SessionHolder {
    // 真实 SDK 的 CJD 经正规 IntelliJ application/PSI 服务解析，不关闭 sidecar 路径。
    private val platformFixture = object : CjPlatformLiteFixture() {
        fun start() = setUp()
        fun stop() = tearDown()
    }
    @BeforeEach fun setUpPlatform() = platformFixture.start()
    @AfterEach fun tearDownPlatform() = platformFixture.stop()

    private val libraryModule = CfirBinaryDependenciesModuleData(Name.special("<factory-boundary-dependencies>"))
    private val module = CfirSourceModuleData(
        name = Name.identifier("factory-boundary"), dependencies = listOf(libraryModule), refinementDependencies = emptyList(),
        targetPlatform = CangJiePlatforms.defaultCangJiePlatform, platform = CfirPlatform.DEFAULT,
    )
    private val intType = ConePrimitiveType(PrimitiveTypeKind.INT64)
    private val unitType = ConePrimitiveType(PrimitiveTypeKind.UNIT)
    // 保留标准 builder 的成员列表，使后建的真实字段挂入同一个已绑定 owner。
    private val ownerMembers = mutableMapOf<CfirClass, MutableList<CfirDeclaration>>()
    // 正规 application fixture 启动后才允许会话首次加载 SDK/CJD。
    override val session: CfirSession by lazy { createSession() }

    @Test
    fun classStructEnumAndInterfaceQualifiersRememberOneCanonicalVariableForTwoOccurrences() {
        val declarations = listOf(nominalClass("ClassOwner"), nominalStruct("StructOwner"), nominalEnum("EnumOwner"), nominalInterface("InterfaceOwner"))
        for (declaration in declarations) {
            val tag = ConeClassLikeLookupTagWithFixedSymbol(declaration.symbol.classId, declaration.symbol)
            val type = when (declaration) {
                is CfirClass -> ConeClassLikeType(tag)
                is CfirStruct -> ConeStructType(tag)
                is CfirEnum -> ConeEnumType(tag)
                is CfirInterface -> ConeClassLikeType(tag, isInterface = true)
                else -> error("The fixture contains only the four actual nominal declaration kinds")
            }
            val first = qualifier(declaration.symbol, type)
            val second = qualifier(declaration.symbol, type)
            assertNotSame(first, second)
            val flow = MutableFlow()
            assertSame(declaration.symbol, real(first).symbol)
            assertNull(flow.getVariableIfKnown(second))
            val canonical = assertIs<RealVariable>(flow.remember(first))
            assertSame(declaration.symbol, canonical.symbol)
            assertSame(declaration, canonical.symbol.cfir)
            assertSame(type, canonical.originalType)
            assertFalse(canonical.isImplicit)
            assertSame(canonical, flow.remember(second))
            for (stored in listOf<Flow>(flow, flow.freeze(), flow.freeze().fork())) {
                assertSame(canonical, stored.getVariable(first))
                assertSame(canonical, stored.getVariableIfKnown(second))
            }
        }
    }

    @Test
    fun aliasChainExpandsToActualNominalAndMapsThatSymbolWithoutConflatingSameClassId() {
        val original = nominalClass("Aliased")
        val effective = nominalClass("Aliased")
        assertEquals(original.symbol.classId, effective.symbol.classId)
        assertNotSame(original.symbol, effective.symbol)
        val inner = alias("InnerAlias", nominalType(original.symbol))
        val outer = alias("OuterAlias", nominalType(inner.symbol))
        // 同 ClassId 的两个真实声明分别由正规阶段发布；RHS 原链不能被名称索引合并或抹平。
        val supertypeProvider = assertNotNull(session.directSupertypeProviderOrNull)
        val originalProduct = assertNotNull(supertypeProvider.getCompletedSupertypeProduct(original.symbol))
        val effectiveProduct = assertNotNull(supertypeProvider.getCompletedSupertypeProduct(effective.symbol))
        assertNotSame(originalProduct, effectiveProduct)
        assertSame(original.symbol, originalProduct.ownerSymbol)
        assertSame(effective.symbol, effectiveProduct.ownerSymbol)
        for ((declaration, target) in listOf(inner to original.symbol, outer to inner.symbol)) {
            val product = assertNotNull(supertypeProvider.getCompletedSupertypeProduct(declaration.symbol))
            assertSame(declaration.symbol, product.ownerSymbol)
            assertSame(declaration, product.ownerSymbol.cfir)
            assertTrue(declaration.origin.fromSource)
            assertSame(session, declaration.moduleData.session)
            assertFalse(product.isInReferenceCycle)
            assertSame(target, product.superTypeRefs.single().coneTypeOrNull?.toSymbol(session))
        }
        val aliasType = nominalType(outer.symbol)
        val first = qualifier(outer.symbol, aliasType)
        val second = qualifier(outer.symbol, aliasType)
        val reference = assertIs<CfirResolvedNamedReference>(first.calleeReference)
        val flow = MutableFlow()
        val originalVariable = assertIs<RealVariable>(flow.remember(first))
        assertSame(original.symbol, originalVariable.symbol)
        assertSame(originalVariable, flow.remember(second))
        assertSame(originalVariable, flow.remember(qualifier(original.symbol, nominalType(original.symbol))))
        val mapper = mapping(original.symbol to effective.symbol)
        assertSame(effective.symbol, real(first, mapper).symbol)
        val mapped = assertIs<RealVariable>(flow.remember(first, symbolMapper = mapper))
        assertSame(effective.symbol, mapped.symbol)
        assertSame(mapped, flow.getVariableIfKnown(second, symbolMapper = mapper))
        assertSame(mapped, flow.remember(qualifier(effective.symbol, nominalType(effective.symbol))))
        assertNotEquals(originalVariable, mapped)
        assertSame(aliasType, mapped.originalType)
        assertSame(reference, first.calleeReference)
        assertSame(outer.symbol, reference.resolvedSymbol)
        assertSame(original.symbol, inner.expandedTypeRef.coneTypeOrNull?.toSymbol(session))
        assertSame(inner.symbol, outer.expandedTypeRef.coneTypeOrNull?.toSymbol(session))
    }

    @Test
    fun primitiveAliasAndActualTypeParameterDoNotBecomeNominalStorage() {
        val primitiveAlias = alias("IntegerAlias", intType)
        lateinit var parameter: CfirTypeParameter
        val owner = nominalClass("GenericOwner") {
            val ownerSymbol = symbol
            parameter = buildTypeParameter {
                source = source("T"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
                name = Name.identifier("T"); symbol = CfirTypeParameterSymbol(); containingDeclarationSymbol = ownerSymbol
                bounds += typeRef(ConeAnyType)
            }
            typeParameters += parameter
        }
        assertSame(parameter, owner.typeParameters.single())
        val expressions = listOf(
            qualifier(primitiveAlias.symbol, intType),
            qualifier(parameter.symbol, ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())),
        )
        for (expression in expressions) {
            val flow = MutableFlow()
            assertIs<SyntheticVariable>(DataFlowVariable.of(expression, unwrapInReceivers = { it }))
            assertIs<SyntheticVariable>(flow.getVariable(expression))
            assertIs<SyntheticVariable>(flow.getVariableIfKnown(expression))
            assertIs<SyntheticVariable>(flow.remember(expression))
        }
    }

    @Test
    fun twoProducerSubstitutionWrappersOfOneOriginShareCanonicalVariable() {
        val owner = nominalClass("FakeOwner")
        val ownerType = nominalType(owner.symbol)
        val base = field("value", ConeAnyType, owner, mutable = false)
        val firstWrapper = substitution(base, owner, intType)
        val secondWrapper = substitution(base, owner, intType)
        val receiver = access(binding("receiver", ownerType))
        val first = access(firstWrapper, receiver)
        val second = access(secondWrapper, receiver)
        assertNotSame(firstWrapper.symbol, secondWrapper.symbol)
        assertSame(base, firstWrapper.originalForSubstitutionOverrideAttr)
        assertSame(base, secondWrapper.originalForSubstitutionOverrideAttr)
        val flow = MutableFlow()
        val canonical = assertIs<RealVariable>(flow.remember(first))
        assertSame(base.symbol, canonical.symbol)
        assertSame(intType, canonical.originalType)
        assertSame(canonical, flow.remember(second))
        assertSame(canonical, flow.remember(access(base, receiver, intType)))
        assertSame(canonical, flow.getVariableIfKnown(second))
        assertSame(firstWrapper.symbol, assertIs<CfirResolvedNamedReference>(first.calleeReference).resolvedSymbol)
        assertSame(base, firstWrapper.originalForSubstitutionOverrideAttr)
    }

    @Test
    fun borrowedWrapperOriginMappingAndExplicitWrapperRebindingBothRemainEffective() {
        val owner = nominalClass("ReboundOwner")
        val base = field("value", ConeAnyType, owner, mutable = false)
        val effectiveBase = field("value", ConeAnyType, nominalClass("ReboundOwner"), mutable = false)
        val alternateBase = field("value", ConeAnyType, nominalClass("AlternateOwner"), mutable = false)
        val effectiveAlternateBase = field("value", ConeAnyType, nominalClass("AlternateOwner"), mutable = false)
        val borrowedWrapper = substitution(base, owner, intType)
        val effectiveWrapper = substitution(alternateBase, owner, intType)
        val originalReceiver = binding("receiver", nominalType(owner.symbol))
        val effectiveReceiver = binding("receiver", nominalType(owner.symbol))
        val borrowedReceiver = access(originalReceiver)
        val borrowed = access(borrowedWrapper, borrowedReceiver)
        val reference = assertIs<CfirResolvedNamedReference>(borrowed.calleeReference)
        val receiverReference = assertIs<CfirResolvedNamedReference>(borrowedReceiver.calleeReference)
        // F 仍借用旧 B，但显式 B→B' 必须在剥离之后生效。
        assertMappedBorrowed(borrowed, effectiveBase.symbol, effectiveReceiver.symbol, mapping(
            base.symbol to effectiveBase.symbol, originalReceiver.symbol to effectiveReceiver.symbol,
        ))
        // F→F' 必须先被采用；F' 的原链是另一个 B，再对它执行本次映射。
        assertMappedBorrowed(borrowed, effectiveAlternateBase.symbol, effectiveReceiver.symbol, mapping(
            borrowedWrapper.symbol to effectiveWrapper.symbol, base.symbol to effectiveBase.symbol,
            alternateBase.symbol to effectiveAlternateBase.symbol, originalReceiver.symbol to effectiveReceiver.symbol,
        ))
        assertSame(reference, borrowed.calleeReference)
        assertSame(borrowedWrapper.symbol, reference.resolvedSymbol)
        assertSame(borrowedReceiver, borrowed.dispatchReceiver)
        assertSame(borrowedReceiver, borrowed.explicitReceiver)
        assertSame(receiverReference, borrowedReceiver.calleeReference)
        assertSame(originalReceiver.symbol, receiverReference.resolvedSymbol)
        assertSame(base, borrowedWrapper.originalForSubstitutionOverrideAttr)
        assertSame(alternateBase, effectiveWrapper.originalForSubstitutionOverrideAttr)
        assertSame(intType, borrowed.coneTypeOrNull)
        assertSame(ConeAnyType, base.returnTypeRef.coneTypeOrNull)
    }

    @Test
    fun mappedOriginThatIsStillAProducerWrapperContinuesToFinalCanonicalTarget() {
        val owner = nominalClass("MappedOriginOwner")
        val base = field("value", ConeAnyType, owner, mutable = false)
        val nextBase = field("value", ConeAnyType, nominalClass("MappedOriginOwner"), mutable = false)
        val finalBase = field("value", ConeAnyType, nominalClass("MappedOriginOwner"), mutable = false)
        val borrowedWrapper = substitution(base, owner, intType)
        val mappedOriginWrapper = substitution(nextBase, owner, intType)
        val receiverBinding = binding("receiver", nominalType(owner.symbol))
        val borrowed = access(borrowedWrapper, access(receiverBinding))
        assertMappedBorrowed(borrowed, finalBase.symbol, receiverBinding.symbol, mapping(
            base.symbol to mappedOriginWrapper.symbol, nextBase.symbol to finalBase.symbol,
        ))
        assertSame(base, borrowedWrapper.originalForSubstitutionOverrideAttr)
        assertSame(nextBase, mappedOriginWrapper.originalForSubstitutionOverrideAttr)
        assertSame(borrowedWrapper.symbol, assertIs<CfirResolvedNamedReference>(borrowed.calleeReference).resolvedSymbol)
        assertSame(intType, borrowed.coneTypeOrNull)
    }

    /** second gate R2：不能整链剥离后才 map 末端；成员及 receiver 的中间 origin 都要逐步投影。 */
    @Test
    fun intermediateOriginRebindingSelectsMappedChainForMemberAndReceiverWithoutChangingBorrowedChain() {
        val owner = nominalClass("IntermediateOwner")
        val replacementOwner = nominalClass("IntermediateOwner")
        val ownerType = nominalType(owner.symbol)
        val baseB = field("value", ConeAnyType, owner, mutable = false)
        val baseC = field("value", ConeAnyType, replacementOwner, mutable = false)
        val f0 = substitution(baseB, owner, intType)
        val f1 = substitution(f0, owner, intType)
        val mappedF0 = substitution(baseC, replacementOwner, intType)
        val receiverB = field("receiver", ownerType, owner, mutable = false)
        val receiverC = field("receiver", ownerType, replacementOwner, mutable = false)
        val receiverF0 = substitution(receiverB, owner, ownerType)
        val receiverF1 = substitution(receiverF0, owner, ownerType)
        val mappedReceiverF0 = substitution(receiverC, replacementOwner, ownerType)
        val root = binding("root", ownerType)
        val effectiveRoot = binding("root", ownerType)
        val rootAccess = access(root)
        val receiverAccess = access(receiverF1, rootAccess)
        val borrowed = access(f1, receiverAccess)
        val reference = assertIs<CfirResolvedNamedReference>(borrowed.calleeReference)
        val receiverReference = assertIs<CfirResolvedNamedReference>(receiverAccess.calleeReference)
        val rootReference = assertIs<CfirResolvedNamedReference>(rootAccess.calleeReference)
        val mapper = mapping(
            f0.symbol to mappedF0.symbol, receiverF0.symbol to mappedReceiverF0.symbol,
            root.symbol to effectiveRoot.symbol,
        )
        val flow = MutableFlow()
        val prototype = real(borrowed, mapper)
        assertSame(baseC.symbol, prototype.symbol)
        assertSame(receiverC.symbol, assertNotNull(prototype.dispatchReceiver).symbol)
        val canonical = assertIs<RealVariable>(flow.remember(borrowed, symbolMapper = mapper))
        val canonicalReceiver = assertNotNull(canonical.dispatchReceiver)
        assertSame(baseC.symbol, canonical.symbol)
        assertSame(receiverC.symbol, canonicalReceiver.symbol)
        assertSame(effectiveRoot.symbol, assertNotNull(canonicalReceiver.dispatchReceiver).symbol)
        assertSame(intType, canonical.originalType)
        assertSame(ownerType, canonicalReceiver.originalType)
        for (stored in listOf<Flow>(flow, flow.freeze(), flow.freeze().fork())) {
            assertSame(canonical, stored.getVariable(borrowed, symbolMapper = mapper))
            assertSame(canonical, stored.getVariableIfKnown(borrowed, symbolMapper = mapper))
            assertSame(canonicalReceiver, stored.getVariable(receiverAccess, symbolMapper = mapper))
            assertSame(canonicalReceiver, stored.getVariableIfKnown(receiverAccess, symbolMapper = mapper))
            assertNull(stored.getVariableIfKnown(borrowed))
        }
        assertSame(canonicalReceiver, flow.remember(receiverAccess, symbolMapper = mapper))
        assertSame(canonical, flow.remember(borrowed, symbolMapper = mapper))
        assertSame(reference, borrowed.calleeReference)
        assertSame(f1.symbol, reference.resolvedSymbol)
        assertSame(receiverAccess, borrowed.dispatchReceiver)
        assertSame(receiverAccess, borrowed.explicitReceiver)
        assertSame(receiverReference, receiverAccess.calleeReference)
        assertSame(receiverF1.symbol, receiverReference.resolvedSymbol)
        assertSame(rootReference, rootAccess.calleeReference)
        assertSame(root.symbol, rootReference.resolvedSymbol)
        assertSame(f0, f1.originalForSubstitutionOverrideAttr)
        assertSame(baseB, f0.originalForSubstitutionOverrideAttr)
        assertSame(baseC, mappedF0.originalForSubstitutionOverrideAttr)
        assertSame(receiverF0, receiverF1.originalForSubstitutionOverrideAttr)
        assertSame(receiverB, receiverF0.originalForSubstitutionOverrideAttr)
        assertSame(receiverC, mappedReceiverF0.originalForSubstitutionOverrideAttr)
        assertSame(intType, borrowed.coneTypeOrNull)
        assertSame(ownerType, receiverAccess.coneTypeOrNull)
    }

    @Test
    fun optionalActualAssignmentCompoundAssignmentAndIncrementHaveNoFactoryOrFlowValue() {
        val owner = nominalClass("WriteOwner")
        val member = field("value", intType, owner, mutable = true)
        fun lValue(): CfirNamedAccessExpression = access(member, checkedSubject(binding("optionalOwner", option(nominalType(owner.symbol)))))
        val assignment = buildAssignment { this.lValue = lValue(); rValue = integer(1); coneTypeOrNull = unitType }
        val compound = buildAssignment {
            this.lValue = lValue(); rValue = integer(2); coneTypeOrNull = unitType
            isCompound = true; augmentedOperation = OperatorNameConventions.PLUS; operation = CfirSourceOperator.ADD_ASSIGN
        }
        val augmented = buildAugmentedAssignment {
            leftArgument = lValue(); rightArgument = integer(3); operation = OperatorNameConventions.PLUS; coneTypeOrNull = unitType
        }
        val increment = buildIncrementDecrementExpression {
            expression = lValue(); operationName = OperatorNameConventions.INC; isPrefix = false; coneTypeOrNull = unitType
        }
        assertTrue(compound.isCompound)
        for (selector in listOf(assignment, compound, augmented, increment)) {
            val writeChain = chain(selector, unitType)
            for (expression in listOf<CfirExpression>(writeChain, smartcast(writeChain), chain(smartcast(selector), unitType), smartcast(smartcast(writeChain)))) {
                val flow = MutableFlow()
                assertNull(DataFlowVariable.of(expression, unwrapInReceivers = { it }))
                assertNull(flow.getVariable(expression))
                assertNull(flow.getVariableIfKnown(expression))
                assertNull(flow.remember(expression))
                for (stored in listOf<Flow>(flow.freeze(), flow.freeze().fork())) {
                    assertNull(stored.getVariable(expression))
                    assertNull(stored.getVariableIfKnown(expression))
                }
                assertTrue(flow.knownVariables.isEmpty())
            }
        }
    }

    @Test
    fun ordinaryUnitCallInOptionalReadChainRetainsSyntheticOptionUnitValue() {
        val owner = nominalClass("CallOwner")
        val function = function("touch", unitType, owner)
        val subject = checkedSubject(binding("optionalOwner", option(nominalType(owner.symbol))))
        val call = buildFunctionCall {
            calleeReference = buildResolvedNamedReference { name = function.name; resolvedSymbol = function.symbol }
            explicitReceiver = subject; dispatchReceiver = subject; coneTypeOrNull = unitType
        }
        val resultType = option(unitType)
        val chain = chain(call, resultType)
        assertSame(unitType, call.coneTypeOrNull)
        assertSame(call, chain.expression)
        assertEquals(StdlibClassIds.Option, resultType.classId)
        assertSame(unitType, resultType.typeArguments.single())
        assertSyntheticAcrossEntrypoints(chain, chain, resultType)
        assertSyntheticAcrossEntrypoints(smartcast(chain), chain, resultType)
    }

    @Test
    fun optionValuedSelectorKeepsOuterOptionAndCheckedPayloadIsDistinctFromContainer() {
        val owner = nominalClass("ReadOwner")
        val ownerType = nominalType(owner.symbol)
        val innerType = option(intType)
        val member = field("nestedOption", innerType, owner, mutable = false)
        val container = binding("optionalOwner", option(ownerType))
        val containerAccess = access(container)
        val subject = buildOptionalExpression { expression = containerAccess; coneTypeOrNull = ownerType }
        val selector = access(member, subject)
        val outerType = option(innerType)
        val chain = chain(selector, outerType)
        assertSame(innerType, selector.coneTypeOrNull)
        assertSame(innerType, outerType.typeArguments.single())
        assertSame(intType, innerType.typeArguments.single())
        assertSyntheticAcrossEntrypoints(chain, chain, outerType)
        assertSyntheticAcrossEntrypoints(smartcast(chain), chain, outerType)
        val flow = MutableFlow()
        val original = assertIs<RealVariable>(flow.remember(containerAccess))
        val payload = assertIs<SyntheticVariable>(flow.remember(subject))
        assertSame(subject, payload.fir)
        assertSame(ownerType, payload.originalType)
        assertSame(container.returnTypeRef.coneTypeOrNull, original.originalType)
        assertNotEquals<DataFlowVariable>(original, payload)
        assertEquals(payload, flow.unwrapVariable(payload))
        assertSame(original, flow.getVariableIfKnown(access(container)))
        assertSame(containerAccess, subject.expression)
        assertSame(container.symbol, assertIs<CfirResolvedNamedReference>(containerAccess.calleeReference).resolvedSymbol)
        assertNotEquals(flow.remember(selector), flow.remember(chain))
    }

    @Test
    fun nonlocalMutableStaticAndFinalReceiverFieldsAreUnstableButImmutableControlsAreStable() {
        val owner = nominalClass("FinalFieldOwner")
        assertIs<CfirResolvedDeclarationStatus>(owner.status)
        assertFalse(owner.status.isOpen)
        val ownerType = nominalType(owner.symbol)
        assertSame(owner.symbol, ownerType.toSymbol(session))
        val receiverBinding = binding("stableReceiver", ownerType)
        val receiver = access(receiverBinding)
        val flow = MutableFlow()
        val receiverVariable = assertIs<RealVariable>(flow.remember(receiver))
        assertEquals(CfirSmartcastStability.STABLE_VALUE, receiverVariable.getStability(flow, session))
        for (isStatic in listOf(true, false)) {
            val mutable = field("mutable", intType, owner, mutable = true, isStatic = isStatic)
            val immutable = field("immutable", intType, owner, mutable = false, isStatic = isStatic)
            assertFalse(mutable.isLocal)
            assertTrue(mutable.isVar)
            assertEquals(isStatic, mutable.status.isStatic)
            val dispatch = receiver.takeUnless { isStatic }
            val mutableVariable = assertIs<RealVariable>(flow.remember(access(mutable, dispatch)))
            val immutableVariable = assertIs<RealVariable>(flow.remember(access(immutable, dispatch)))
            if (isStatic) assertNull(mutableVariable.dispatchReceiver) else assertSame(receiverVariable, mutableVariable.dispatchReceiver)
            assertEquals(CfirSmartcastStability.UNSTABLE_VALUE, mutableVariable.getStability(flow, session))
            assertEquals(CfirSmartcastStability.STABLE_VALUE, immutableVariable.getStability(flow, session))
            assertSame(mutable.symbol, mutableVariable.symbol)
            assertSame(immutable.symbol, immutableVariable.symbol)
        }
    }

    @Test
    fun legalMutableLocalParameterAndImmutablePatternBindingKeepTheirActualIdentities() {
        val function = function("execute", unitType)
        val parameter = buildValueParameter {
            source = source("argument"); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true; isNamed = false; name = Name.identifier("argument")
            symbol = CfirValueParameterSymbol(CallableId(name)); containingDeclarationSymbol = function.symbol; returnTypeRef = typeRef(intType)
        }
        function.replaceValueParameters(listOf(parameter))
        val mutableBinding = binding("local", intType, mutable = true)
        val immutableBinding = binding("fixed", intType)
        val local = buildPatternVariable {
            source = mutableBinding.source; moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            status = resolvedStatus(); isLocal = true; isVar = true
            symbol = CfirPatternVariableSymbol(mutableBinding.symbol.callableId); returnTypeRef = mutableBinding.returnTypeRef
            pattern = buildBindingPattern {
                name = mutableBinding.name; bindingVariable = mutableBinding
                typeRef = mutableBinding.returnTypeRef; semanticTypeRef = mutableBinding.returnTypeRef
            }
            initializer = integer(1)
        }
        function.replaceBody(buildBlock { coneTypeOrNull = unitType; statements += local; statements += access(parameter) })
        assertSame(function.symbol, parameter.containingDeclarationSymbol)
        val flow = MutableFlow()
        for (declaration in listOf<CfirVariable>(local, mutableBinding, parameter, immutableBinding)) {
            val variable = assertIs<RealVariable>(flow.remember(access(declaration)))
            assertSame(declaration.symbol, variable.symbol)
            assertSame(declaration, variable.symbol.cfir)
            assertSame(variable, flow.getVariableIfKnown(access(declaration)))
            assertEquals(CfirSmartcastStability.STABLE_VALUE, variable.getStability(flow, session))
        }
        assertNotEquals(flow.remember(access(local)), flow.remember(access(mutableBinding)))
        assertNotEquals(flow.remember(access(parameter)), flow.remember(access(immutableBinding)))
    }

    private fun assertMappedBorrowed(
        expression: CfirNamedAccessExpression,
        target: CfirBasedSymbol<*>,
        receiverTarget: CfirBasedSymbol<*>,
        mapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*>,
    ) {
        val flow = MutableFlow()
        val prototype = real(expression, mapper)
        assertSame(target, prototype.symbol)
        assertSame(receiverTarget, assertNotNull(prototype.dispatchReceiver).symbol)
        val canonical = assertIs<RealVariable>(flow.remember(expression, symbolMapper = mapper))
        assertSame(target, canonical.symbol)
        assertSame(target.cfir, canonical.symbol.cfir)
        assertSame(receiverTarget, assertNotNull(canonical.dispatchReceiver).symbol)
        assertSame(expression.coneTypeOrNull, canonical.originalType)
        val receiver = assertNotNull(expression.dispatchReceiver)
        for (stored in listOf<Flow>(flow, flow.freeze(), flow.freeze().fork())) {
            assertSame(canonical, stored.getVariable(expression, symbolMapper = mapper))
            assertSame(canonical, stored.getVariableIfKnown(expression, symbolMapper = mapper))
            assertSame(canonical.dispatchReceiver, stored.getVariable(receiver, symbolMapper = mapper))
            assertSame(canonical.dispatchReceiver, stored.getVariableIfKnown(receiver, symbolMapper = mapper))
        }
        assertSame(canonical.dispatchReceiver, flow.remember(receiver, symbolMapper = mapper))
    }

    private fun assertSyntheticAcrossEntrypoints(expression: CfirExpression, identity: CfirExpression, type: ConeCangJieType) {
        val flow = MutableFlow()
        val variables = listOf(
            DataFlowVariable.of(expression, unwrapInReceivers = { it }), flow.getVariable(expression),
            flow.getVariableIfKnown(expression), flow.remember(expression), flow.freeze().getVariable(expression),
        )
        for (variable in variables) {
            val actual = assertIs<SyntheticVariable>(variable)
            assertSame(identity, actual.fir)
            assertSame(type, actual.originalType)
        }
    }

    /** 属性原链只由真实签名 producer 设置，测试不直接写 originalForSubstitutionOverrideAttr。 */
    private fun substitution(base: CfirFieldVariable, owner: CfirClass, returnType: ConeCangJieType): CfirFieldVariable =
        CfirFakeOverrideGenerator.createCopyForCfirField(
            newSymbol = CfirFieldVariableSymbol(base.symbol.callableId), baseField = base,
            derivedClassLookupTag = nominalType(owner.symbol).lookupTag, session = session,
            origin = CfirDeclarationOrigin.SubstitutionOverride.CallSite,
            newDispatchReceiverType = nominalType(owner.symbol), newReturnType = returnType,
        )

    private fun field(name: String, type: ConeCangJieType, owner: CfirClass, mutable: Boolean, isStatic: Boolean = false): CfirFieldVariable =
        buildFieldVariable {
            source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false; isVar = mutable; status = resolvedStatus(isStatic)
            this.name = Name.identifier(name); symbol = CfirFieldVariableSymbol(CallableId(owner.symbol.classId, this.name))
            dispatchReceiverType = nominalType(owner.symbol).takeUnless { isStatic }; returnTypeRef = typeRef(type)
        }.also { checkNotNull(ownerMembers[owner]).add(it) }

    private fun binding(name: String, type: ConeCangJieType, mutable: Boolean = false): CfirPatternBindingVariable = buildPatternBindingVariable {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true; isVar = mutable; status = resolvedStatus()
        this.name = Name.identifier(name); symbol = CfirPatternBindingSymbol(CallableId(this.name)); returnTypeRef = typeRef(type)
    }

    private fun function(name: String, type: ConeCangJieType, owner: CfirClass? = null): CfirNamedFunction = buildNamedFunction {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY; status = resolvedStatus()
        isLocal = false; isMut = false; this.name = Name.identifier(name)
        symbol = CfirNamedFunctionSymbol(if (owner == null) CallableId(FqName("factoryboundary"), this.name) else CallableId(owner.symbol.classId, this.name))
        dispatchReceiverType = owner?.let { nominalType(it.symbol) }; returnTypeRef = typeRef(type)
        body = buildBlock { coneTypeOrNull = type }
    }.also { if (owner != null) checkNotNull(ownerMembers[owner]).add(it) }

    private fun alias(name: String, expanded: ConeCangJieType): CfirTypeAlias = buildTypeAlias {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.IMPORTS
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY; status = resolvedStatus()
        scopeProvider = CfirCangJieScopeProvider(); this.name = Name.identifier(name)
        symbol = CfirTypeAliasSymbol(classId(name)); expandedTypeRef = typeRef(expanded)
    }.also { original ->
        val symbol = original.symbol
        val source = original.source
        // typed RHS 已绑定实际 symbol；非局部正规入口无需虚构文件或按名字替换 RHS 目标。
        completeCjoSourceFixtureClassLikeHeaders(session, listOf(original))
        val product = assertNotNull(assertNotNull(session.directSupertypeProviderOrNull).getCompletedSupertypeProduct(symbol))
        assertSame(symbol, product.ownerSymbol)
        assertSame(original, symbol.cfir)
        assertSame(source, original.source)
        assertFalse(product.isInReferenceCycle)
        assertSame(expanded, product.superTypeRefs.single().coneTypeOrNull)
        assertSame(expanded, original.expandedTypeRef.coneTypeOrNull)
        resolveCjoSourceFixtureStatuses(session, listOf(original))
    }

    private fun nominalClass(name: String, init: CfirClassBuilder.() -> Unit = {}): CfirClass {
        lateinit var members: MutableList<CfirDeclaration>
        return buildClass {
            source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.IMPORTS
            origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY; status = resolvedStatus()
            scopeProvider = CfirCangJieScopeProvider(); this.name = Name.identifier(name); symbol = CfirClassSymbol(classId(name))
            members = declarations
            init()
        }.also {
            ownerMembers[it] = members
            resolveCjoSourceFixtureStatuses(session, listOf(it))
        }
    }

    private fun nominalStruct(name: String): CfirStruct = buildStruct {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.IMPORTS
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY; status = resolvedStatus()
        scopeProvider = CfirCangJieScopeProvider(); this.name = Name.identifier(name); symbol = CfirStructSymbol(classId(name))
    }.also { resolveCjoSourceFixtureStatuses(session, listOf(it)) }

    private fun nominalEnum(name: String): CfirEnum = buildEnum {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.IMPORTS
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY; status = resolvedStatus()
        scopeProvider = CfirCangJieScopeProvider(); this.name = Name.identifier(name); symbol = CfirEnumSymbol(classId(name))
        isRefEnum = false; isNonExhaustive = false
    }.also { resolveCjoSourceFixtureStatuses(session, listOf(it)) }

    private fun nominalInterface(name: String): CfirInterface = buildInterface {
        source = source(name); moduleData = module; resolvePhase = CfirResolvePhase.IMPORTS
        origin = CfirDeclarationOrigin.Source; attributes = CfirDeclarationAttributes.EMPTY
        status = CfirDeclarationStatusImpl().apply { isAbstract = true }.resolved(Visibilities.Public, Modality.ABSTRACT)
        scopeProvider = CfirCangJieScopeProvider(); this.name = Name.identifier(name); symbol = CfirInterfaceSymbol(classId(name))
    }.also { resolveCjoSourceFixtureStatuses(session, listOf(it)) }

    private fun nominalType(symbol: CfirClassLikeSymbol<*>): ConeCangJieType =
        when (symbol) {
            is CfirClassSymbol -> nominalType(symbol)
            is CfirStructSymbol -> ConeStructType(ConeClassLikeLookupTagWithFixedSymbol(symbol.classId, symbol))
            is CfirEnumSymbol -> ConeEnumType(ConeClassLikeLookupTagWithFixedSymbol(symbol.classId, symbol), isRefEnum = symbol.isRefEnum)
            is CfirInterfaceSymbol -> ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(symbol.classId, symbol), isInterface = true)
            is CfirTypeAliasSymbol -> ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(symbol.classId, symbol))
            else -> error("This fixture requires an actual nominal or alias declaration")
        }

    private fun nominalType(symbol: CfirClassSymbol): ConeClassLikeType =
        ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(symbol.classId, symbol))

    private fun qualifier(symbol: CfirClassifierSymbol<*>, type: ConeCangJieType): CfirNamedAccessExpression = buildNamedAccessExpression {
        source = symbol.cfir.source; coneTypeOrNull = type
        calleeReference = buildResolvedNamedReference {
            name = when (symbol) {
                is CfirClassLikeSymbol<*> -> symbol.name
                is CfirTypeParameterSymbol -> symbol.name
            }
            resolvedSymbol = symbol
        }
    }

    private fun access(declaration: CfirCallableDeclaration, receiver: CfirExpression? = null, type: ConeCangJieType = assertNotNull(declaration.returnTypeRef.coneTypeOrNull)): CfirNamedAccessExpression =
        buildNamedAccessExpression {
            source = declaration.source; coneTypeOrNull = type
            calleeReference = buildResolvedNamedReference { name = declaration.symbol.name; resolvedSymbol = declaration.symbol }
            dispatchReceiver = receiver; explicitReceiver = receiver
        }

    /** typed checked subject 是 Option 容器的 payload 视图，绝不伪造 Some/None 值或 payload symbol。 */
    private fun checkedSubject(container: CfirPatternBindingVariable): CfirOptionalExpression = buildOptionalExpression {
        expression = access(container); coneTypeOrNull = assertNotNull(container.returnTypeRef.coneTypeOrNull?.optionElementType)
    }

    private fun option(payload: ConeCangJieType): ConeEnumType = ConeEnumType(StdlibClassIds.Option.toLookupTag(), listOf(payload))
    private fun chain(selector: CfirExpression, type: ConeCangJieType): CfirOptionalChainExpression = buildOptionalChainExpression {
        expression = selector; coneTypeOrNull = type
    }
    private fun smartcast(expression: CfirExpression): CfirSmartCastExpression = buildSmartCastExpression {
        originalExpression = expression; coneTypeOrNull = expression.coneTypeOrNull
        smartcastType = typeRef(assertNotNull(expression.coneTypeOrNull)); upperTypesFromSmartCast = listOf(assertNotNull(expression.coneTypeOrNull))
    }
    private fun integer(value: Int): CfirLiteralExpression = buildLiteralExpression {
        kind = CfirLiteralKind.INT; this.value = value.toLong(); coneTypeOrNull = intType
    }
    private fun real(expression: CfirExpression, mapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*> = { it }): RealVariable =
        assertIs<RealVariable>(DataFlowVariable.of(expression, unwrapInReceivers = { it }, symbolMapper = mapper))
    private fun mapping(vararg pairs: Pair<CfirBasedSymbol<*>, CfirBasedSymbol<*>>): (CfirBasedSymbol<*>) -> CfirBasedSymbol<*> {
        val targets = pairs.toMap()
        return { targets[it] ?: it }
    }
    private fun resolvedStatus(isStatic: Boolean = false): CfirResolvedDeclarationStatus =
        CfirDeclarationStatusImpl().apply { this.isStatic = isStatic }.resolved(Visibilities.Public, Modality.FINAL)
    private fun typeRef(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef { coneType = type }
    private fun classId(name: String): ClassId = ClassId(FqName("factoryboundary"), Name.identifier(name))
    private fun source(name: String) = object : CjBinarySourceElement(name, null, name) {}

    /** 与 ownership 测试相同的真实 shared→Library→source session 工厂链。 */
    private fun createSession(): CfirSession {
        val configuration = CompilerConfiguration.createForCfirFrontend()
        // 本组显式验证原 Inner/Outer RHS 链；正规阶段保留 abbreviation，DFA 仍经完成产品展开。
        configuration.languageVersionSettings = LanguageVersionSettingsImpl(
            configuration.languageVersionSettings.languageVersion,
            analysisFlags = mapOf(AnalysisFlags.expandTypeAliasesInTypeResolution to false),
        )
        val factory = CfirDefaultSessionFactory()
        val sdkRoot = java.io.File(assertNotNull(javaClass.classLoader.getResource("cjo-sdk/windows_x86_64_cjnative")).toURI())
        check(sdkRoot.isDirectory) { "The real CJO SDK fixture must be available as a classpath directory" }
        // 普通库 provider 读取真实 std.core；primitive builtin 不能代替 Object 的已绑定声明。
        val context = CfirDefaultSessionFactory.Context(
            cjoManager = CjoManager(CjoSearchPath(envProvider = { name ->
                if (name == "CANGJIE_STDLIB_MODULE") sdkRoot.absolutePath else null
            }), CjoFormatVersionPolicy.Compilation(configuration.languageVersionSettings)),
        )
        val shared = factory.createSharedLibrarySession(module.name, emptyList(), configuration.languageVersionSettings, context)
        factory.createLibrarySession(shared, SingleModuleDataProvider(libraryModule), emptyList(), configuration.languageVersionSettings, context)
        return factory.createSourceSession(module, emptyList(), configuration, context)
    }
}
