@file:OptIn(ExperimentalUnsignedTypes::class, org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.BlockInfo
import PackageFormat.CallKind
import PackageFormat.DeclKind
import PackageFormat.ExprInfo
import PackageFormat.ExprKind
import PackageFormat.FuncInfo
import PackageFormat.LambdaInfo
import PackageFormat.LitConstKind
import PackageFormat.OperatorKind
import PackageFormat.Package
import PackageFormat.PatternKind
import PackageFormat.TypeKind
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.defaultParameterHelperInfo
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.CfirImportedSourceGraphProvider
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionIdentity
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload
import org.cangnova.cangjie.cfir.source.CfirSourceReferenceTarget
import org.cangnova.cangjie.cfir.source.CfirSourceFunctionBody
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourcePatternKind
import org.cangnova.cangjie.cfir.source.CfirSourceSlot
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 统一 owned plan/F registry 的公开消费合同，只经 Writer → Manager → Library provider 请求产品。
 *
 * native 固定 14276740ffec1262b81b6b5236b1d840af270b37：ExprLoader.cpp:348–380、488–509，
 * ASTLoader.cpp:763–834、888–898，ReferenceLoader.cpp:586–650；Kotlin 固定
 * 3c7a5041f238102285764d370665f56f587f9dc4：IrDeclarationDeserializer.kt:540–575 的实际函数 scope/binder。
 * 依据 docs/cjmp-source-helper-20261117/s5-astloader-source-native-authority-20261119.md 和
 * source-decoder-gate-current-20261119.md 的 R1/R3/R4/R7；本轮补充依据为
 * build/cjmp-owned-closure-20261120/native-owned-closure-authority.md 和 kotlin-framework-map.md。
 *
 * 修改前失败机制：独立 initializer 未进入根闭包；全表 FuncDecl 预扫将 local 造为全局；
 * CURRENT borrow 按 requester 认领参数；变量 context 被误作 Return 的 F；map/Jump reserve
 * 未拥有的 e；helper/参数/F 的关系未完整发布。测试中的 schema 合同另行标注，不冒充源码正例。
 */
class CjoSourceOwnedClosureTest {
    /** 已保存 initializer 的 schema-loader 合同，不承诺普通 global let 的 native SOURCE 导出策略。 */
    @Test
    fun independentGlobalAndMemberLambdaInitializersBelongToThePackageClosure(@TempDir root: Path) {
        for (headerFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(headerFirst.toString()), initializerMetadata())
            val entry = fixture.function("entry")
            val before = if (headerFirst) fixture.source.sourceHeaderOf(entry) else null
            val globalBefore = if (headerFirst) assertIs<CfirVariable>(fixture.callable("global")) else null
            val holderBefore = if (headerFirst) fixture.struct("Holder") else null
            val earlyGraph = assertNotNull(fixture.source.packageSourceGraphOf(entry))
            val earlyRecords = earlyGraph.expressions.toMap()
            val earlyInitializers = earlyGraph.initializers.toMap()
            val earlyBodies = earlyGraph.functionBodies.toMap()
            val global = assertIs<CfirVariable>(fixture.callable("global"))
            val holder = fixture.struct("Holder")
            val member = holder.declarations.filterIsInstance<CfirVariable>().single()
            // 原 header 的普通 getter 被动读取；通过正式 provider 显式完成原 initializer。
            val loadedGlobalInitializer = assertNotNull(fixture.source.sourceInitializerOf(global))
            val loadedMemberInitializer = assertNotNull(fixture.source.sourceInitializerOf(member))
            assertSame(loadedGlobalInitializer, global.initializer)
            assertSame(loadedMemberInitializer, member.initializer)
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(entry))
            earlyRecords.forEach { (identity, record) ->
                assertSame(record, earlyGraph.expression(identity))
                assertSame(record, graph.expression(identity))
            }
            earlyInitializers.forEach { (owner, identity) -> assertSame(identity, graph.initializers.getValue(owner)) }
            earlyBodies.forEach { (owner, identity) -> assertSame(identity, graph.functionBodies.getValue(owner)) }
            if (globalBefore != null) assertSame(globalBefore, global)
            if (holderBefore != null) assertSame(holderBefore, holder)
            if (before != null) assertEquals(before, graph.functionHeaders.getValue(entry))
            assertEquals(setOf(global, member), graph.initializers.keys)
            val globalIdentity = graph.initializers.getValue(global)
            val memberIdentity = graph.initializers.getValue(member)
            assertNotSame(globalIdentity, memberIdentity)
            assertSame(global, graph.expression(globalIdentity).context)
            assertSame(member, graph.expression(memberIdentity).context)
            val globalInitializer = assertIs<CfirAnonymousFunctionExpression>(global.initializer)
            val memberInitializer = assertIs<CfirAnonymousFunctionExpression>(member.initializer)
            val first = assertLambda(graph, globalIdentity, "globalArg", expectedOrdinaryInitializer = globalInitializer)
            val second = assertLambda(graph, memberIdentity, "memberArg", expectedOrdinaryInitializer = memberInitializer)
            assertNotSame(first.owner, second.owner)
            assertNotSame(first.owner.symbol, second.owner.symbol)
            assertNotSame(first.body, second.body)
            val constructor = holder.declarations.filterIsInstance<CfirConstructor>().single()
            assertEquals(setOf(entry, first.owner, second.owner, constructor), graph.functionBodies.keys)
            assertSame(graph, fixture.source.packageSourceGraphOf(fixture.function("entry")))
            assertSame(first.owner, assertIs<CfirSourceExpressionPayload.Lambda>(graph.expression(globalIdentity).payload).function.owner)
            assertSame(second.owner, assertIs<CfirSourceExpressionPayload.Lambda>(graph.expression(memberIdentity).payload).function.owner)
            val firstBody = assertNotNull(assertIs<CfirAnonymousFunction>(first.owner).body)
            val secondBody = assertNotNull(assertIs<CfirAnonymousFunction>(second.owner).body)
            assertNotSame(firstBody, secondBody)
            repeat(3) {
                assertSame(global, fixture.callable("global"))
                assertSame(holder, fixture.struct("Holder"))
                assertSame(member, holder.declarations.filterIsInstance<CfirVariable>().single())
                assertSame(globalInitializer, global.initializer)
                assertSame(memberInitializer, member.initializer)
                assertSame(graph, fixture.source.packageSourceGraphOf(fixture.function("entry")))
                assertSame(first, assertLambda(graph, globalIdentity, "globalArg", expectedOrdinaryInitializer = globalInitializer))
                assertSame(second, assertLambda(graph, memberIdentity, "memberArg", expectedOrdinaryInitializer = memberInitializer))
                assertSame(firstBody, assertIs<CfirAnonymousFunction>(first.owner).body)
                assertSame(secondBody, assertIs<CfirAnonymousFunction>(second.owner).body)
                earlyRecords.forEach { (identity, record) ->
                    assertSame(record, earlyGraph.expression(identity))
                    assertSame(record, graph.expression(identity))
                }
            }
        }
    }

    /** 两个 same<T>(x:T):T 来自不同 Block.d；CallableId 的 local 角色不代替 symbol 身份。 */
    @Test
    fun sameNamedGenericLocalFunctionsKeepSeparateCanonicalHeadersAndReturns(@TempDir root: Path) {
        for (selected in listOf("left", "right")) {
            val fixture = fixture(root.resolve(selected), localFunctionMetadata())
            val selectedFunction = fixture.function(selected)
            val selectedHeader = assertNotNull(fixture.source.sourceHeaderOf(selectedFunction))
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(selectedFunction))
            val locals = listOf("left", "right").map { name ->
                val outer = fixture.function(name)
                val slots = block(graph, graph.functionBodies.getValue(outer)).statements
                val local = assertIs<CfirNamedFunction>(assertIs<CfirSourceSlot.Declaration>(slots[0]).declaration)
                assertEquals("same", local.name.asString())
                assertTrue(local.isLocal)
                assertTrue(local.symbol.callableId.isLocal)
                assertNull(local.symbol.callableId.classId)
                val typeParameter = assertIs<CfirTypeParameter>(local.typeParameters.single())
                val parameter = local.valueParameters.single()
                assertSame(local.symbol, typeParameter.containingDeclarationSymbol)
                assertSame(local.symbol, parameter.containingDeclarationSymbol)
                assertSame(typeParameter.symbol, assertIs<ConeTypeParameterType>(local.returnTypeRef.coneType).lookupTag.typeParameterSymbol)
                assertSame(typeParameter.symbol, assertIs<ConeTypeParameterType>(parameter.returnTypeRef.coneType).lookupTag.typeParameterSymbol)
                val reference = assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(expression(slots[1])).payload)
                assertSame(local, assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration)
                assertSame(local.symbol, assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration.symbol)
                assertEquals(listOf(ConePrimitiveType.INT64), reference.instantiatedTypes)
                val returned = returned(graph, graph.functionBodies.getValue(local))
                assertSame(local, returned.function)
                assertSame(parameter, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(returned.value).payload).target).declaration)
                val header = graph.functionHeaders.getValue(local)
                assertSame(parameter, header.parameterLists.single().single())
                assertEquals(header, fixture.source.sourceHeaderOf(local))
                assertSame(graph, fixture.source.packageSourceGraphOf(local))
                assertNull(local.body)
                local
            }
            assertNotSame(locals[0], locals[1])
            assertNotSame(locals[0].symbol, locals[1].symbol)
            assertNotSame(locals[0].typeParameters.single().symbol, locals[1].typeParameters.single().symbol)
            assertNotSame(locals[0].valueParameters.single().symbol, locals[1].valueParameters.single().symbol)
            assertEquals(selectedHeader, fixture.source.sourceHeaderOf(selectedFunction))
            assertTrue(fixture.provider.getTopLevelCallableSymbols(FqName(fixture.packageName), Name.identifier("same")).isEmpty())
        }
    }

    /** let value = if (...) { return 7 } else { 9 }；context 是变量，Return 仍属于外层 F。 */
    @Test
    fun returnInsideAnIfInitializerKeepsItsFunctionDomainAndVariableContext(@TempDir root: Path) {
        val fixture = fixture(root, conditionalInitializerMetadata())
        val outer = fixture.function("entry")
        val graph = assertNotNull(fixture.source.sourceGraphOf(outer))
        val slots = block(graph, graph.functionBodies.getValue(outer)).statements
        val variable = assertIs<CfirVariable>(assertIs<CfirSourceSlot.Declaration>(slots[0]).declaration)
        assertTrue(variable.isLocal)
        val identity = graph.initializers.getValue(variable)
        val conditional = assertIs<CfirSourceExpressionPayload.Conditional>(graph.expression(identity).payload)
        val returnIdentity = expression(block(graph, conditional.thenBranch).statements.single())
        val record = graph.expression(returnIdentity)
        val returned = assertIs<CfirSourceExpressionPayload.Return>(record.payload)
        assertSame(variable, graph.expression(identity).context)
        assertSame(variable, graph.expression(conditional.condition).context)
        assertSame(variable, record.context)
        assertSame(variable, graph.expression(returned.value).context)
        assertSame(outer, returned.function)
        assertEquals(ConePrimitiveType.NOTHING, record.type)
        assertEquals(ConePrimitiveType.INT64, graph.expression(returned.value).type)
        val elseIdentity = expression(block(graph, assertNotNull(conditional.elseBranch)).statements.single())
        assertEquals("9", assertIs<CfirSourceExpressionPayload.Literal>(graph.expression(elseIdentity).payload).text)
        val finalReturn = assertIs<CfirSourceExpressionPayload.Return>(graph.expression(expression(slots[1])).payload)
        assertSame(outer, finalReturn.function)
        assertSame(variable, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(finalReturn.value).payload).target).declaration)
        assertSame(graph, fixture.source.sourceGraphOf(outer))
        assertNull(outer.body)
    }

    /**
     * 正式 schema-loader 的 CURRENT FullId 合同：probe 借用另 F/local/lambda 的参数，
     * 不据引用者创造 owner。跨源码作用域的 probe 不是语言正例；loader 不重做 Sema 可见性检查。
     * 每个 target 的合法拥有根和带 Return 的 body 均保存在同一包，用两个查询入口验证时序独立。
     */
    @Test
    fun lateCurrentReferencesCannotClaimAnotherFunctionOrLambdaParameter(@TempDir root: Path) {
        for (selected in listOf("probe", "second")) {
            val fixture = fixture(root.resolve(selected), currentBorrowMetadata())
            val probe = fixture.function("probe")
            val second = fixture.function("second")
            val before = assertNotNull(fixture.source.sourceHeaderOf(probe))
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(fixture.function(selected)))
            val secondSlots = block(graph, graph.functionBodies.getValue(second)).statements
            val local = assertIs<CfirNamedFunction>(assertIs<CfirSourceSlot.Declaration>(secondSlots[0]).declaration)
            val variable = assertIs<CfirVariable>(assertIs<CfirSourceSlot.Declaration>(secondSlots[1]).declaration)
            val lambda = assertLambda(graph, graph.initializers.getValue(variable), "lambdaArg")
            val owners = listOf<CfirFunction>(second, local, assertIs<CfirAnonymousFunction>(lambda.owner))
            val parameters = owners.map { it.valueParameters.single() }
            val probeSlots = block(graph, graph.functionBodies.getValue(probe)).statements
            repeat(3) {
                owners.forEachIndexed { index, owner ->
                    val referenceIdentity = expression(probeSlots[index])
                    val reference = assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(referenceIdentity).payload)
                    assertSame(parameters[index], assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration)
                    assertSame(parameters[index].symbol, assertIs<CfirSourceReferenceTarget.Declaration>(reference.target).declaration.symbol)
                    assertSame(owner.symbol, parameters[index].containingDeclarationSymbol)
                    assertNotSame(probe.symbol, parameters[index].containingDeclarationSymbol)
                    assertSame(parameters[index], graph.functionHeaders.getValue(owner).parameterLists.single().single())
                    assertSame(owner, returned(graph, graph.functionBodies.getValue(owner)).function)
                    assertSame(graph.expression(referenceIdentity), graph.expression(referenceIdentity))
                }
                assertSame(graph, fixture.source.packageSourceGraphOf(fixture.function("probe")))
                assertSame(graph, fixture.source.packageSourceGraphOf(fixture.function("second")))
                assertEquals(before, fixture.source.sourceHeaderOf(probe))
            }
            assertNotSame(parameters[0].symbol, parameters[1].symbol)
            assertNotSame(parameters[1].symbol, parameters[2].symbol)
            assertTrue(local.isLocal)
        }
    }

    /** 原始 wire 先核验成功；只损坏生成 accessor 的 map 槽，e 存在但没有合法 owned root。 */
    @Test
    fun mapAndJumpCannotMaterializeAnUnownedExpressionAndFailureKeepsHeaders(@TempDir root: Path) {
        for (jump in listOf(false, true)) {
            val input = unownedBorrowMetadata(jump)
            val fixture = fixture(root.resolve(jump.toString()), input.metadata, input.corruption)
            val entry = fixture.function("entry")
            val before = assertNotNull(fixture.source.sourceHeaderOf(entry))
            assertTrue(before.bodyAvailable)
            // 正常声明和真实签名已完成；失败只针对 SOURCE 的未拥有 e 借用。
            repeat(2) {
                assertFailsWith<IllegalStateException> { fixture.source.sourceGraphOf(entry) }
                assertSame(entry, fixture.function("entry"))
                assertEquals(before, fixture.source.sourceHeaderOf(entry))
                assertNull(entry.body)
                assertWireStructure(input.metadata, fixture.originalWire)
            }
            assertEquals(input.corruption.target, assertNotNull(fixture.wire.allExprs(input.corruption.expression.toInt() - 1)).mapExpr)
            assertEquals(input.corruption.original, assertNotNull(fixture.originalWire.allExprs(input.corruption.expression.toInt() - 1)).mapExpr)
        }
    }

    /** 权威 §6.C：named api 的 default helper 返回捕获其 clone seed 的 lambda；lambda 没有 defaults。 */
    @Test
    fun namedDefaultHelperReturningLambdaKeepsThreeFunctionAndParameterIdentities(@TempDir root: Path) {
        val fixture = fixture(root, helperReturningLambdaMetadata())
        val api = fixture.function("api")
        val before = assertNotNull(fixture.source.sourceHeaderOf(api))
        val graph = assertNotNull(fixture.source.packageSourceGraphOf(api))
        val header = graph.functionHeaders.getValue(api)
        assertEquals(before, header)
        assertNull(header.desugaredParameterLists.single()[0])
        val helper = assertNotNull(header.desugaredParameterLists.single()[1])
        val defaultMetadata = assertNotNull(api.valueParameters[1].defaultValueMetadata)
        assertTrue(defaultMetadata.hasInitial)
        assertSame(helper, defaultMetadata.function)
        val helperInfo = assertNotNull(helper.defaultParameterHelperInfo)
        assertSame(api, helperInfo.owner)
        assertEquals(1, helperInfo.parameterIndex)
        val originalSeed = api.valueParameters[0]
        val clonedSeed = helper.valueParameters.single()
        assertEquals(originalSeed.name, clonedSeed.name)
        assertNotSame(originalSeed, clonedSeed)
        assertNotSame(originalSeed.symbol, clonedSeed.symbol)
        assertSame(api.symbol, originalSeed.containingDeclarationSymbol)
        assertSame(helper.symbol, clonedSeed.containingDeclarationSymbol)
        assertSame(clonedSeed, graph.functionHeaders.getValue(helper).parameterLists.single().single())
        assertFalse(graph.defaults.containsKey(api.valueParameters[1]), "捕获 seed 的非 const assignment 没有保存 defaultVal")
        val helperReturn = returned(graph, graph.functionBodies.getValue(helper))
        assertSame(helper, helperReturn.function)
        val lambdaIdentity = helperReturn.value
        val lambda = assertIs<CfirSourceExpressionPayload.Lambda>(graph.expression(lambdaIdentity).payload).function
        assertIs<CfirAnonymousFunction>(lambda.owner)
        assertNotSame(helper, lambda.owner)
        assertNotSame(api, lambda.owner)
        assertSame(lambda.body, graph.functionBodies.getValue(lambda.owner))
        val lambdaHeader = assertNotNull(lambda.importedHeader)
        assertEquals(lambdaHeader, graph.functionHeaders.getValue(lambda.owner))
        assertNull(lambdaHeader.returnType, "FL.retType=0，语义返回类型另从 lambda.type 读取")
        assertEquals(ConePrimitiveType.INT64, assertIs<org.cangnova.cangjie.cfir.source.CfirSourceCallableSignature.Resolved>(lambda.signature).returnType)
        val parameter = lambda.parameters.single()
        assertEquals("x", parameter.name.asString())
        assertSame(parameter, assertIs<CfirAnonymousFunction>(lambda.owner).valueParameters.single())
        assertSame(parameter, lambdaHeader.parameterLists.single().single())
        assertSame(lambda.owner.symbol, parameter.containingDeclarationSymbol)
        val parameterMetadata = assertNotNull(parameter.defaultValueMetadata)
        assertFalse(parameterMetadata.hasInitial)
        assertNull(parameterMetadata.function)
        assertNull(lambdaHeader.desugaredParameterLists.single().single())
        val lambdaReturn = returned(graph, assertNotNull(lambda.body))
        assertSame(lambda.owner, lambdaReturn.function)
        val sum = assertIs<CfirSourceExpressionPayload.Binary>(graph.expression(lambdaReturn.value).payload)
        assertSame(clonedSeed, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(sum.left).payload).target).declaration)
        assertSame(parameter, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(sum.right).payload).target).declaration)
        assertSame(api, returned(graph, graph.functionBodies.getValue(api)).function)
        assertEquals(setOf(api, helper, lambda.owner), graph.functionBodies.keys)
        assertEquals(graph.functionHeaders.getValue(helper), fixture.source.sourceHeaderOf(helper))
        assertSame(graph, fixture.source.packageSourceGraphOf(helper))
        assertSame(graph, fixture.source.packageSourceGraphOf(api))
        assertSame(lambda.owner, assertIs<CfirSourceExpressionPayload.Lambda>(graph.expression(lambdaIdentity).payload).function.owner)
        assertNull(api.body)
        assertNull(helper.body)
        assertNull(assertIs<CfirAnonymousFunction>(lambda.owner).body)
    }

    /** 合法语言形态：lambda 内声明 local(first: Int64, second!: Int64 = 7)，defaults 不放在 lambda 参数上。 */
    @Test
    fun localFunctionDefaultsInsideALambdaKeepTheirOwnHelperAndParameterDomain(@TempDir root: Path) {
        val fixture = fixture(root, localDefaultHelperMetadata())
        val entry = fixture.function("entry")
        val graph = assertNotNull(fixture.source.packageSourceGraphOf(entry))
        val variable = assertIs<CfirVariable>(assertIs<CfirSourceSlot.Declaration>(block(graph, graph.functionBodies.getValue(entry)).statements[0]).declaration)
        val lambda = assertIs<CfirSourceExpressionPayload.Lambda>(graph.expression(graph.initializers.getValue(variable)).payload).function
        assertTrue(lambda.parameters.isEmpty())
        val slots = block(graph, assertNotNull(lambda.body)).statements
        val local = assertIs<CfirNamedFunction>(assertIs<CfirSourceSlot.Declaration>(slots[0]).declaration)
        assertTrue(local.isLocal)
        assertTrue(local.symbol.callableId.isLocal)
        assertDefaultHelper(graph, local)
        assertSame(local, returned(graph, graph.functionBodies.getValue(local)).function)
        assertNotSame(lambda.owner, returned(graph, graph.functionBodies.getValue(local)).function)
        assertSame(lambda.owner, assertIs<CfirSourceExpressionPayload.Return>(graph.expression(expression(slots[1])).payload).function)
        assertEquals(graph.functionHeaders.getValue(local), fixture.source.sourceHeaderOf(local))
        assertSame(graph, fixture.source.packageSourceGraphOf(local))
    }

    /** PropInfo 的 FuncDecl 要成为 accessor；VarPattern 的 d 要成为共享 binding，不能普通函数/变量重造。 */
    @Test
    fun propertyAccessorsAndPatternBindingsShareTheirRealCanonicalSymbols(@TempDir root: Path) {
        for (headerFirst in listOf(false, true)) {
            val fixture = fixture(root.resolve(headerFirst.toString()), propertyPatternMetadata())
            val holderBefore = if (headerFirst) fixture.struct("Holder") else null
            val propertyBefore = holderBefore?.declarations?.filterIsInstance<CfirProperty>()?.single()
            val getterHeader = propertyBefore?.getter?.let { fixture.source.sourceHeaderOf(it) }
            val entry = fixture.function("entry")
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(entry))
            val holder = fixture.struct("Holder")
            val property = holder.declarations.filterIsInstance<CfirProperty>().single()
            if (holderBefore != null) assertSame(holderBefore, holder)
            if (propertyBefore != null) assertSame(propertyBefore, property)
            val getter = assertNotNull(property.getter)
            val setter = assertNotNull(property.setter)
            assertSame(property.symbol, getter.propertySymbol)
            assertSame(property.symbol, setter.propertySymbol)
            assertTrue(getter.isGetter)
            assertFalse(setter.isGetter)
            assertNotSame(getter.symbol, setter.symbol)
            assertSame(setter.symbol, setter.valueParameters.single().containingDeclarationSymbol)
            for (accessor in listOf(getter, setter)) {
                assertSame(accessor, accessor.symbol.cfir)
                assertSame(accessor, returned(graph, graph.functionBodies.getValue(accessor)).function)
                assertEquals(graph.functionHeaders.getValue(accessor), fixture.source.sourceHeaderOf(accessor))
                assertSame(graph, fixture.source.packageSourceGraphOf(accessor))
                assertNull(accessor.body)
            }
            if (getterHeader != null) assertEquals(getterHeader, graph.functionHeaders.getValue(getter))
            val slots = block(graph, graph.functionBodies.getValue(entry)).statements
            val variable = assertIs<CfirPatternVariable>(assertIs<CfirSourceSlot.Declaration>(slots[0]).declaration)
            val normalBinding = assertNotNull(assertIs<CfirBindingPattern>(variable.pattern).bindingVariable)
            val sourcePattern = graph.declarationPatterns.getValue(variable)
            assertEquals(CfirSourcePatternKind.VARIABLE, sourcePattern.kind)
            assertEquals(listOf(ConePrimitiveType.INT64), sourcePattern.types)
            val sourceBinding = assertIs<CfirPatternBindingVariable>(assertIs<CfirSourceSlot.Declaration>(sourcePattern.slots.single()).declaration)
            val target = assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(returned(graph, graph.functionBodies.getValue(entry)).value).payload).target).declaration
            assertSame(normalBinding, sourceBinding)
            assertSame(normalBinding.symbol, sourceBinding.symbol)
            assertSame(sourceBinding, target)
            assertSame(sourceBinding.symbol, target.symbol)
            assertSame(sourceBinding, sourceBinding.symbol.cfir)
            assertTrue(sourceBinding.isLocal)
            assertTrue(sourceBinding.symbol.callableId.isLocal)
            assertSame(variable, graph.expression(graph.initializers.getValue(variable)).context)
            assertEquals("7", assertIs<CfirSourceExpressionPayload.Literal>(graph.expression(graph.initializers.getValue(variable)).payload).text)
            assertSame(graph, fixture.source.packageSourceGraphOf(entry))
            assertSame(sourcePattern, graph.declarationPatterns.getValue(variable))
        }
    }

    /** R1：两个不同 inline F 的正文都未保存，空/非空参数组及实际身份仍必须完整。 */
    @Test
    fun bodylessAnonymousFunctionsKeepSeparateHeadersAndOwningContexts(@TempDir root: Path) {
        val pool = Pool()
        val emptyType = pool.functionType(emptyList(), INT)
        val parameterType = pool.functionType(listOf(INT), INT)
        val first = pool.lambda(emptyType, body(0u, 0u))
        val second = pool.lambda(parameterType, body(0u, 0u, listOf(4u)))
        val entryBody = pool.block(NOTHING, listOf(2u, 3u, pool.returned(pool.integer("1"))), listOf(false, false, true))
        val fixture = fixture(root, pool.metadata("ownedbodyless", listOf(
            function("entry", emptyType, body(entryBody, INT), topLevel = true),
            variable("first", emptyType, first), variable("second", parameterType, second), parameter("argument", INT),
        )))
        val entry = fixture.function("entry")
        val before = assertNotNull(fixture.source.sourceHeaderOf(entry))
        val graph = assertNotNull(fixture.source.sourceGraphOf(entry))
        val variables = block(graph, graph.functionBodies.getValue(entry)).statements.take(2).map {
            assertIs<CfirVariable>(assertIs<CfirSourceSlot.Declaration>(it).declaration)
        }
        val lambdas = variables.map { variable ->
            assertIs<CfirSourceExpressionPayload.Lambda>(graph.expression(graph.initializers.getValue(variable)).payload).function
        }
        assertNotSame(lambdas[0].owner, lambdas[1].owner)
        assertNotSame(lambdas[0].owner.symbol, lambdas[1].owner.symbol)
        assertTrue(lambdas[0].parameters.isEmpty())
        assertEquals("argument", lambdas[1].parameters.single().name.asString())
        for (lambda in lambdas) {
            assertNull(lambda.body)
            assertNull(assertIs<CfirAnonymousFunction>(lambda.owner).body)
            val header = assertNotNull(lambda.importedHeader)
            assertFalse(header.bodyAvailable)
            assertNull(header.returnType)
            header.parameterLists.flatten().forEach { assertSame(lambda.owner.symbol, it.containingDeclarationSymbol) }
            assertEquals(header, graph.functionHeaders.getValue(lambda.owner))
            assertFalse(graph.functionBodies.containsKey(lambda.owner))
            assertSame(entry, graph.functionContexts.getValue(lambda.owner).outerFunction)
            assertSame(entry, graph.functionContexts.getValue(lambda.owner).declarationContext)
        }
        assertEquals(setOf(entry), graph.functionBodies.keys)
        assertEquals(setOf(entry, lambdas[0].owner, lambdas[1].owner), graph.functionHeaders.keys)
        assertEquals(before, fixture.source.sourceHeaderOf(entry))
        assertSame(graph, fixture.source.sourceGraphOf(entry))
    }

    /** R2：走生产 FullId callback 的首次 local/TP/helper/匿名参数请求，随后 SOURCE 不能更换身份。 */
    @Test
    fun declarationFirstQueriesUseTheSameLocalAndAnonymousFunctionRegistry(@TempDir root: Path) {
        for (firstIndex in listOf(2, 3)) {
            val fixture = fixture(root.resolve("local-$firstIndex"), localFunctionMetadata())
            // 显式发现原 left F 的 owning 下行，只发布 registry；首次局部声明仍由下面的 FullId 查询绑定。
            assertTrue(fixture.source.discoverSourceFunctionHeadersOf(fixture.function("left")))
            val first = fixture.declaration(firstIndex)
            val local = assertIs<CfirNamedFunction>(fixture.declaration(2))
            val parameter = assertIs<CfirTypeParameter>(fixture.declaration(3))
            assertTrue(local.isLocal && local.symbol.callableId.isLocal)
            assertSame(local.symbol, parameter.containingDeclarationSymbol)
            assertSame(if (firstIndex == 2) local else parameter, first)
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(fixture.function("left")))
            val fromBody = assertIs<CfirNamedFunction>(assertIs<CfirSourceSlot.Declaration>(
                block(graph, graph.functionBodies.getValue(fixture.function("left"))).statements[0]).declaration)
            assertSame(local, fromBody)
            assertSame(parameter, fromBody.typeParameters.single())
            assertSame(first, fixture.declaration(firstIndex))
            assertSame(graph, fixture.source.packageSourceGraphOf(local))
        }
        run {
            val fixture = fixture(root.resolve("helper-first"), localDefaultHelperMetadata())
            // 原 entry F 下行发现 lambda 内的 local、参数及其真实 desugar helper。
            assertTrue(fixture.source.discoverSourceFunctionHeadersOf(fixture.function("entry")))
            val helper = assertIs<CfirNamedFunction>(fixture.declaration(4))
            val local = assertIs<CfirNamedFunction>(fixture.declaration(5))
            assertSame(local, assertNotNull(helper.defaultParameterHelperInfo).owner)
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(fixture.function("entry")))
            assertDefaultHelper(graph, local)
            assertSame(helper, graph.functionHeaders.getValue(local).desugaredParameterLists.single()[1])
            assertSame(helper, fixture.declaration(4))
        }
        run {
            val fixture = fixture(root.resolve("anonymous-parameter-first"), currentBorrowMetadata())
            // 匿名参数的拥有 F 来自 second 的真实 initializer，不能由 probe 的 CURRENT 引用认领。
            assertTrue(fixture.source.discoverSourceFunctionHeadersOf(fixture.function("second")))
            val parameter = fixture.declaration(6)
            val graph = assertNotNull(fixture.source.packageSourceGraphOf(fixture.function("probe")))
            val second = fixture.function("second")
            val variable = assertIs<CfirVariable>(assertIs<CfirSourceSlot.Declaration>(
                block(graph, graph.functionBodies.getValue(second)).statements[1]).declaration)
            val lambda = assertLambda(graph, graph.initializers.getValue(variable), "lambdaArg")
            assertSame(parameter, lambda.parameters.single())
            assertSame(lambda.owner.symbol, lambda.parameters.single().containingDeclarationSymbol)
            assertSame(parameter, fixture.declaration(6))
        }
    }

    /** R2-H：表内无 owning header 的 CURRENT target 不阻断 header 查询，选中 SOURCE 正文才拒绝。 */
    @Test
    fun owningHeaderQueriesDoNotReadAnUnownedBodyReferenceTarget(@TempDir root: Path) {
        for (firstIndex in listOf(3, 6)) {
            val input = currentBorrowMetadata()
            // 必须破坏 probe 真正选中的正文槽，不能拿 local 正文的首个 RefExpr 代替。
            val probeBody = assertIs<CjoFunctionInfo>(input.declarations.first().info).body.body
            val probeBlock = input.expressions[probeBody.toInt() - 1]
            assertEquals(ExprKind.Block, probeBlock.kind)
            val broken = probeBlock.operands[1].toInt() - 1
            val expressions = input.expressions.toMutableList()
            assertEquals(ExprKind.RefExpr, expressions[broken].kind)
            val reference = assertNotNull(expressions[broken].reference)
            assertEquals("localArg", reference.reference)
            val originalTarget = assertNotNull(reference.target)
            assertEquals(PackageIndex.CURRENT.value, originalTarget.pkgId)
            assertEquals(5u, originalTarget.index)
            // d8 在原表中真实存在，但不加入任何 F.paramLists、Block 声明槽或 package root。
            val unownedIndex = (input.declarations.size + 1).toUInt()
            assertTrue(unownedIndex != 0u)
            expressions[broken] = expressions[broken].copy(
                reference = reference.copy(target = originalTarget.copy(index = unownedIndex)),
            )
            val fixture = fixture(root.resolve(firstIndex.toString()), input.copy(
                declarations = input.declarations + parameter("unownedArg", INT),
                expressions = expressions,
            ))
            fun assertRawTarget() {
                val expression = assertNotNull(fixture.wire.allExprs(broken))
                assertEquals(ExprKind.RefExpr, expression.kind)
                assertEquals(INT, expression.type)
                val saved = expression.info(PackageFormat.ReferenceInfo()) as PackageFormat.ReferenceInfo
                assertEquals("localArg", saved.reference)
                val target = assertNotNull(saved.target)
                assertEquals(PackageIndex.CURRENT.value, target.pkgId)
                assertEquals(unownedIndex, target.index)
                assertTrue(target.index <= fixture.wire.allDeclsLength.toUInt())
                val declaration = assertNotNull(fixture.wire.allDecls(target.index.toInt() - 1))
                assertEquals(DeclKind.FuncParam, declaration.kind)
                assertEquals("unownedArg", declaration.identifier)
                assertFalse(declaration.isTopLevel)
                assertEquals(INT, declaration.type)
            }
            assertRawTarget()
            val probe = fixture.function("probe")
            val second = fixture.function("second")
            // 仅登记真实 F 的 owning 下行；冷 local/匿名参数查询前不绑定借用目标或物化正文。
            assertTrue(fixture.source.discoverSourceFunctionHeadersOf(probe))
            assertTrue(fixture.source.discoverSourceFunctionHeadersOf(second))
            val before = fixture.declaration(firstIndex)
            val local = assertIs<CfirNamedFunction>(fixture.declaration(3))
            assertTrue(local.isLocal && local.symbol.callableId.isLocal)
            val header = assertNotNull(fixture.source.sourceHeaderOf(local))
            assertTrue(header.bodyAvailable)
            val localParameter = header.parameterLists.single().single()
            assertEquals("localArg", localParameter.name.asString())
            assertSame(local.symbol, localParameter.containingDeclarationSymbol)
            val anonymousParameter = assertIs<CfirValueParameter>(fixture.declaration(6))
            assertEquals("lambdaArg", anonymousParameter.name.asString())
            val anonymousOwner = assertIs<CfirAnonymousFunction>(
                assertNotNull(anonymousParameter.containingDeclarationSymbol).cfir,
            )
            val anonymousHeader = assertNotNull(fixture.source.sourceHeaderOf(anonymousOwner))
            assertTrue(anonymousHeader.bodyAvailable)
            assertSame(anonymousParameter, anonymousHeader.parameterLists.single().single())
            assertSame(anonymousOwner.symbol, anonymousParameter.containingDeclarationSymbol)
            assertNull(probe.body)
            assertNull(second.body)
            assertNull(local.body)
            assertNull(anonymousOwner.body)
            repeat(2) {
                val failure = assertFailsWith<IllegalStateException> { fixture.source.sourceGraphOf(probe) }
                assertEquals(
                    "SOURCE target d $unownedIndex requires its actual registered owning header",
                    failure.message,
                )
                assertSame(before, fixture.declaration(firstIndex))
                assertSame(local, fixture.declaration(3))
                assertEquals(header, fixture.source.sourceHeaderOf(local))
                assertSame(localParameter, assertNotNull(fixture.source.sourceHeaderOf(local)).parameterLists.single().single())
                assertSame(anonymousParameter, fixture.declaration(6))
                assertSame(anonymousOwner.symbol, anonymousParameter.containingDeclarationSymbol)
                assertEquals(anonymousHeader, fixture.source.sourceHeaderOf(anonymousOwner))
                assertSame(probe, fixture.function("probe"))
                assertSame(second, fixture.function("second"))
                assertNull(probe.body)
                assertNull(second.body)
                assertNull(local.body)
                assertNull(anonymousOwner.body)
                assertRawTarget()
            }
            assertRawTarget()
        }
    }

    /** R3：损坏单一 type 槽指向没有合法拥有 root 的 TP，后置失败不能发布孤儿 Fn/TP。 */
    @Test
    fun sourceTypeReferencesCannotPublishAnOrphanGenericOwner(@TempDir root: Path) {
        val pool = Pool()
        val entryType = pool.functionType(emptyList(), INT)
        val orphanType = pool.type(CjoTypeMetadata(TypeKind.Generic, semanticInfo = CjoGenericTypeInfoMetadata(3u)))
        val orphanSignature = pool.functionType(listOf(orphanType), orphanType)
        val literal = pool.integer("1")
        val entryBody = pool.returnBlock(literal)
        val orphanBody = pool.returnBlock(pool.reference("x", 4u, orphanType))
        val input = pool.metadata("ownedorphantype", listOf(
            function("entry", entryType, body(entryBody, INT), topLevel = true),
            function("orphan", orphanSignature, body(orphanBody, orphanType, listOf(4u)), generic = CjoGenericMetadata(listOf(3u))),
            CjoPackageDeclaration("T", DeclKind.GenericParamDecl, isTopLevel = false, type = orphanType), parameter("x", orphanType),
        ))
        val fixture = fixture(root, input, typeCorruption = TypeCorruption(literal, orphanType))
        val entry = fixture.function("entry")
        val before = assertNotNull(fixture.source.sourceHeaderOf(entry))
        repeat(2) {
            assertFailsWith<IllegalStateException> { fixture.source.sourceGraphOf(entry) }
            assertSame(entry, fixture.function("entry"))
            assertEquals(before, fixture.source.sourceHeaderOf(entry))
            assertFailsWith<IllegalStateException> { fixture.declaration(1) }
            assertFailsWith<IllegalStateException> { fixture.declaration(2) }
        }
        assertEquals(INT, assertNotNull(fixture.originalWire.allExprs(literal.toInt() - 1)).type)
        assertEquals(orphanType, assertNotNull(fixture.wire.allExprs(literal.toInt() - 1)).type)
    }

    private fun assertDefaultHelper(graph: CfirSourceGraph, owner: CfirFunction) {
        val header = graph.functionHeaders.getValue(owner)
        assertEquals(listOf("first", "second"), header.parameterLists.single().map { it.name.asString() })
        assertEquals(2, header.desugaredParameterLists.single().size)
        assertNull(header.desugaredParameterLists.single()[0])
        val helper = assertNotNull(header.desugaredParameterLists.single()[1])
        owner.valueParameters.forEachIndexed { index, parameter ->
            assertSame(parameter, header.parameterLists.single()[index])
            assertSame(owner.symbol, parameter.containingDeclarationSymbol)
        }
        val firstMetadata = assertNotNull(owner.valueParameters[0].defaultValueMetadata)
        assertFalse(firstMetadata.hasInitial)
        assertNull(firstMetadata.function)
        val second = owner.valueParameters[1]
        val metadata = assertNotNull(second.defaultValueMetadata)
        assertTrue(metadata.hasInitial)
        assertSame(helper, metadata.function)
        val helperInfo = assertNotNull(helper.defaultParameterHelperInfo)
        assertSame(owner, helperInfo.owner)
        assertEquals(1, helperInfo.parameterIndex)
        assertSame(helper, helper.symbol.cfir)
        assertEquals(ConePrimitiveType.INT64, helper.returnTypeRef.coneType)
        val clonedFirst = helper.valueParameters.single()
        assertNotSame(owner.valueParameters[0], clonedFirst)
        assertNotSame(owner.valueParameters[0].symbol, clonedFirst.symbol)
        assertEquals(owner.valueParameters[0].name, clonedFirst.name)
        assertSame(helper.symbol, clonedFirst.containingDeclarationSymbol)
        assertSame(clonedFirst, graph.functionHeaders.getValue(helper).parameterLists.single().single())
        assertTrue(graph.functionHeaders.getValue(helper).bodyAvailable)
        val originalDefault = graph.defaults.getValue(second)
        assertSame(second, graph.expression(originalDefault).context)
        val helperReturn = returned(graph, graph.functionBodies.getValue(helper))
        assertSame(helper, helperReturn.function)
        assertNotSame(originalDefault, helperReturn.value)
        assertEquals("7", assertIs<CfirSourceExpressionPayload.Literal>(graph.expression(originalDefault).payload).text)
        assertEquals("7", assertIs<CfirSourceExpressionPayload.Literal>(graph.expression(helperReturn.value).payload).text)
        assertNull(helper.body)
        assertNull(owner.body)
    }

    private fun assertLambda(
        graph: CfirSourceGraph,
        identity: CfirSourceExpressionIdentity,
        parameterName: String,
        expectedOrdinaryInitializer: CfirAnonymousFunctionExpression? = null,
    ): CfirSourceFunctionBody {
        val lambda = assertIs<CfirSourceExpressionPayload.Lambda>(graph.expression(identity).payload).function
        assertIs<CfirAnonymousFunction>(lambda.owner)
        assertTrue(lambda.owner.isLocal)
        assertSame(lambda.owner, lambda.owner.symbol.cfir)
        assertSame(lambda.body, graph.functionBodies.getValue(lambda.owner))
        assertSame(lambda.owner, graph.expression(assertNotNull(lambda.body)).context)
        val header = assertNotNull(lambda.importedHeader)
        assertEquals(header, graph.functionHeaders.getValue(lambda.owner))
        assertTrue(header.bodyAvailable)
        val parameter = lambda.parameters.single()
        assertEquals(parameterName, parameter.name.asString())
        assertSame(parameter, assertIs<CfirAnonymousFunction>(lambda.owner).valueParameters.single())
        assertSame(parameter, header.parameterLists.single().single())
        assertSame(lambda.owner.symbol, parameter.containingDeclarationSymbol)
        val returned = returned(graph, assertNotNull(lambda.body))
        assertSame(lambda.owner, returned.function)
        assertSame(parameter, assertIs<CfirSourceReferenceTarget.Declaration>(assertIs<CfirSourceExpressionPayload.Reference>(graph.expression(returned.value).payload).target).declaration)
        assertSame(lambda.owner, graph.expression(returned.value).context)
        if (expectedOrdinaryInitializer == null) {
            assertNull(assertIs<CfirAnonymousFunction>(lambda.owner).body)
        } else {
            // owned initializer 的真实普通正文、Return/参数绑定与原 canonical e 共用身份。
            val owner = assertIs<CfirAnonymousFunction>(lambda.owner)
            assertSame(owner, expectedOrdinaryInitializer.anonymousFunction)
            assertSame(identity, graph.expressionOf(expectedOrdinaryInitializer))
            val ordinaryBody = assertNotNull(owner.body)
            assertSame(lambda.body, graph.expressionOf(ordinaryBody))
            val returnIdentity = expression(block(graph, assertNotNull(lambda.body)).statements.single())
            val ordinaryReturn = assertIs<CfirReturnExpression>(ordinaryBody.statements.single())
            assertSame(returnIdentity, graph.expressionOf(ordinaryReturn))
            assertTrue(ordinaryReturn.target.isBound)
            assertSame(owner, ordinaryReturn.target.labeledElement)
            val ordinaryReference = assertIs<CfirNamedAccessExpression>(ordinaryReturn.result)
            assertSame(returned.value, graph.expressionOf(ordinaryReference))
            assertSame(parameter.symbol, assertIs<CfirResolvedNamedReference>(ordinaryReference.calleeReference).resolvedSymbol)
            assertSame(parameter, assertIs<CfirResolvedNamedReference>(ordinaryReference.calleeReference).resolvedSymbol.cfir)
            assertSame(graph.expression(returnIdentity).type, ordinaryReturn.coneTypeOrNull)
            assertSame(graph.expression(returned.value).type, ordinaryReference.coneTypeOrNull)
        }
        return lambda
    }

    private fun block(graph: CfirSourceGraph, identity: CfirSourceExpressionIdentity): CfirSourceExpressionPayload.Block =
        assertIs<CfirSourceExpressionPayload.Block>(graph.expression(identity).payload)

    private fun expression(slot: CfirSourceSlot): CfirSourceExpressionIdentity = assertIs<CfirSourceSlot.Expression>(slot).identity

    private fun returned(graph: CfirSourceGraph, body: CfirSourceExpressionIdentity): CfirSourceExpressionPayload.Return =
        assertIs<CfirSourceExpressionPayload.Return>(graph.expression(expression(block(graph, body).statements.last())).payload)

    /** d1 entry；d2 global；d3 Holder；d4 member；d5/d6 匿名 F 的各自参数。 */
    private fun initializerMetadata(): CjoPackageMetadata {
        val pool = Pool()
        val entryType = pool.functionType(emptyList(), INT)
        val lambdaType = pool.functionType(listOf(INT), INT)
        val holderType = pool.type(CjoTypeMetadata(TypeKind.Struct, semanticInfo = CjoCompositeTypeInfoMetadata(3u)))
        val globalBody = pool.returnBlock(pool.reference("globalArg", 5u, INT))
        val memberBody = pool.returnBlock(pool.reference("memberArg", 6u, INT))
        val globalLambda = pool.lambda(lambdaType, body(globalBody, 0u, listOf(5u)))
        val memberLambda = pool.lambda(lambdaType, body(memberBody, 0u, listOf(6u)))
        val entryBody = pool.returnBlock(pool.integer("42"))
        val constructorType = pool.functionType(emptyList(), UNIT)
        val constructorBody = pool.returnBlock(pool.unit())
        return pool.metadata("ownedinitializers", listOf(
            function("entry", entryType, body(entryBody, INT), topLevel = true),
            variable("global", lambdaType, globalLambda, topLevel = true),
            CjoPackageDeclaration("Holder", DeclKind.StructDecl, type = holderType, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL), info = CjoStructInfo(body = listOf(4u, 7u))),
            variable("member", lambdaType, memberLambda, attributes = flags(Attribute.PUBLIC)),
            parameter("globalArg", INT), parameter("memberArg", INT),
            function("init", constructorType, body(constructorBody, UNIT), attributes = flags(Attribute.PUBLIC, Attribute.CONSTRUCTOR)),
        ))
    }

    /** d3/d6 是两个 same；d4/d7 是各自 T，d5/d8 是各自 x；GenericTy 按真实 FullId 指回 TP。 */
    private fun localFunctionMetadata(): CjoPackageMetadata {
        val pool = Pool()
        val outerType = pool.functionType(emptyList(), INT)
        val firstT = pool.type(CjoTypeMetadata(TypeKind.Generic, semanticInfo = CjoGenericTypeInfoMetadata(4u)))
        val secondT = pool.type(CjoTypeMetadata(TypeKind.Generic, semanticInfo = CjoGenericTypeInfoMetadata(7u)))
        val firstType = pool.functionType(listOf(firstT), firstT)
        val secondType = pool.functionType(listOf(secondT), secondT)
        val appliedType = pool.functionType(listOf(INT), INT)
        val firstBody = pool.returnBlock(pool.reference("x", 5u, firstT))
        val secondBody = pool.returnBlock(pool.reference("x", 8u, secondT))
        val leftBody = pool.block(NOTHING, listOf(3u, pool.reference("same", 3u, appliedType, listOf(INT)), pool.returned(pool.integer("1"))), listOf(false, true, true))
        val rightBody = pool.block(NOTHING, listOf(6u, pool.reference("same", 6u, appliedType, listOf(INT)), pool.returned(pool.integer("2"))), listOf(false, true, true))
        return pool.metadata("ownedlocals", listOf(
            function("left", outerType, body(leftBody, INT), topLevel = true),
            function("right", outerType, body(rightBody, INT), topLevel = true),
            function("same", firstType, body(firstBody, firstT, listOf(5u)), generic = CjoGenericMetadata(listOf(4u))),
            CjoPackageDeclaration("T", DeclKind.GenericParamDecl, isTopLevel = false, type = firstT), parameter("x", firstT),
            function("same", secondType, body(secondBody, secondT, listOf(8u)), generic = CjoGenericMetadata(listOf(7u))),
            CjoPackageDeclaration("T", DeclKind.GenericParamDecl, isTopLevel = false, type = secondT), parameter("x", secondT),
        ))
    }

    private fun conditionalInitializerMetadata(): CjoPackageMetadata {
        val pool = Pool()
        val entryType = pool.functionType(emptyList(), INT)
        val thenBody = pool.returnBlock(pool.integer("7"))
        val elseBody = pool.block(INT, listOf(pool.integer("9")))
        val initializer = pool.expression(CjoExpressionMetadata(ExprKind.IfExpr, type = INT, operands = listOf(pool.boolean(true), thenBody, elseBody)))
        val entryBody = pool.block(NOTHING, listOf(2u, pool.returned(pool.reference("value", 2u, INT))), listOf(false, true))
        return pool.metadata("ownedconditional", listOf(
            function("entry", entryType, body(entryBody, INT), topLevel = true), variable("value", INT, initializer),
        ))
    }

    private fun currentBorrowMetadata(): CjoPackageMetadata {
        val pool = Pool()
        val entryType = pool.functionType(emptyList(), INT)
        val parameterType = pool.functionType(listOf(INT), INT)
        val localBody = pool.returnBlock(pool.reference("localArg", 5u, INT))
        val lambdaBody = pool.returnBlock(pool.reference("lambdaArg", 7u, INT))
        val initializer = pool.lambda(parameterType, body(lambdaBody, 0u, listOf(7u)))
        val secondBody = pool.block(NOTHING, listOf(4u, 6u, pool.returned(pool.reference("secondArg", 3u, INT))), listOf(false, false, true))
        val probeBody = pool.block(NOTHING, listOf(
            pool.reference("secondArg", 3u, INT), pool.reference("localArg", 5u, INT),
            pool.reference("lambdaArg", 7u, INT), pool.returned(pool.integer("42")),
        ))
        return pool.metadata("ownedcurrent", listOf(
            function("probe", entryType, body(probeBody, INT), topLevel = true),
            function("second", parameterType, body(secondBody, INT, listOf(3u)), topLevel = true),
            parameter("secondArg", INT), function("local", parameterType, body(localBody, INT, listOf(5u))),
            parameter("localArg", INT), variable("closure", parameterType, initializer), parameter("lambdaArg", INT),
        ))
    }

    private data class MapCorruption(val expression: UInt, val original: UInt, val target: UInt)
    private data class BorrowInput(val metadata: CjoPackageMetadata, val corruption: MapCorruption)

    private fun unownedBorrowMetadata(jump: Boolean): BorrowInput {
        if (!jump) return unownedMapMetadata()
        val pool = Pool()
        val entryType = pool.functionType(emptyList(), INT)
        val orphanBody = pool.block(UNIT, listOf(pool.unit()))
        val orphanLoop = pool.expression(CjoExpressionMetadata(ExprKind.WhileExpr, type = UNIT, operands = listOf(pool.boolean(false), orphanBody)))
        // 先保留 loop 的正式 e identity，Jump.mapExpr 指向其 enclosing owned loop。
        val loopIdentity = (pool.expressions.size + 1).toUInt()
        pool.expression(CjoExpressionMetadata(ExprKind.WhileExpr, type = UNIT))
        val jumpIdentity = pool.expression(CjoExpressionMetadata(ExprKind.JumpExpr, type = NOTHING, mapExpression = loopIdentity,
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Jump(isBreak = true)))
        val loopBody = pool.block(NOTHING, listOf(jumpIdentity))
        val condition = pool.boolean(false)
        pool.expressions[loopIdentity.toInt() - 1] = CjoExpressionMetadata(ExprKind.WhileExpr, type = UNIT,
            operands = listOf(condition, loopBody), mapExpression = if (jump) 0u else loopBody)
        val entryBody = pool.block(NOTHING, listOf(loopIdentity, pool.returned(pool.integer("42"))))
        val input = pool.metadata("ownedborrow${if (jump) "jump" else "map"}", listOf(
            function("entry", entryType, body(entryBody, INT), topLevel = true),
        ))
        return BorrowInput(input, MapCorruption(if (jump) jumpIdentity else loopIdentity, if (jump) loopIdentity else loopBody, orphanLoop))
    }

    /** 权威 §6.F 的 tuple comparison clone Call.map：两个 owned Call，第三个同型 Call 没有 owned root。 */
    private fun unownedMapMetadata(): BorrowInput {
        val pool = Pool()
        val tupleType = pool.type(CjoTypeMetadata(TypeKind.Tuple, listOf(INT, INT)))
        val functionType = pool.functionType(emptyList(), tupleType)
        fun call(): UInt {
            val base = pool.reference("pair", 2u, functionType)
            return pool.expression(CjoExpressionMetadata(ExprKind.CallExpr, type = tupleType, operands = listOf(base),
                call = CjoCallExpressionMetadata(false, CallKind.CallDeclaredFunction)))
        }
        val orphan = call()
        val first = call()
        val second = call()
        pool.expressions[first.toInt() - 1] = pool.expressions[first.toInt() - 1].copy(mapExpression = first)
        pool.expressions[second.toInt() - 1] = pool.expressions[second.toInt() - 1].copy(mapExpression = first)
        fun subscript(base: UInt, index: String): UInt = pool.expression(CjoExpressionMetadata(ExprKind.SubscriptExpr,
            type = INT, operands = listOf(base, pool.integer(index)), additionalInfo = CjoAdditionalExpressionInfoMetadata.Subscript(true)))
        val result = pool.expression(CjoExpressionMetadata(ExprKind.TupleLit, type = tupleType,
            operands = listOf(subscript(first, "0"), subscript(second, "1"))))
        val entryBody = pool.returnBlock(result)
        val pairBody = pool.returnBlock(pool.expression(CjoExpressionMetadata(ExprKind.TupleLit, type = tupleType,
            operands = listOf(pool.integer("1"), pool.integer("2")))))
        val input = pool.metadata("ownedborrowmap", listOf(
            function("entry", functionType, body(entryBody, tupleType), topLevel = true),
            function("pair", functionType, body(pairBody, tupleType), topLevel = true),
        ))
        return BorrowInput(input, MapCorruption(second, first, orphan))
    }

    /** §6.C 的完整 d/e/t：d4 helper 拥有 clone seed d5；lambda F 独立拥有 x d6。 */
    private fun helperReturningLambdaMetadata(): CjoPackageMetadata {
        val pool = Pool()
        val lambdaType = pool.functionType(listOf(INT), INT)
        val apiType = pool.functionType(listOf(INT, lambdaType), INT)
        val helperType = pool.functionType(listOf(INT), lambdaType)
        val apiBody = pool.returnBlock(pool.reference("seed", 2u, INT))
        val sum = pool.expression(CjoExpressionMetadata(ExprKind.BinaryExpr, type = INT,
            operands = listOf(pool.reference("seed", 5u, INT), pool.reference("x", 6u, INT)),
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Binary(OperatorKind.Add)))
        val lambdaBody = pool.returnBlock(sum)
        val lambda = pool.lambda(lambdaType, body(lambdaBody, 0u, listOf(6u)))
        val helperBody = pool.returnBlock(lambda)
        return pool.metadata("ownedhelperlambda", listOf(
            function("api", apiType, body(apiBody, INT, listOf(2u, 3u), listOf(0u, 4u)), topLevel = true),
            parameter("seed", INT),
            CjoPackageDeclaration("f", DeclKind.FuncParam, isTopLevel = false, type = lambdaType, attributes = flags(Attribute.HAS_INITIAL),
                info = CjoParameterInfo(isNamed = true, isMember = false, defaultValue = 0u)),
            function("f.1", helperType, body(helperBody, lambdaType, listOf(5u)), attributes = flags(Attribute.HAS_INITIAL, Attribute.IMPLICIT_ADD, Attribute.NO_REFLECT_INFO)),
            parameter("seed", INT).copy(attributes = flags(Attribute.COMPILER_ADD)), parameter("x", INT),
        ))
    }

    /** d3/d4 是 lambda 内 local 函数参数；d5 helper 拥有此前参数的新 clone d7。 */
    private fun localDefaultHelperMetadata(): CjoPackageMetadata {
        val pool = Pool()
        val noArgs = pool.functionType(emptyList(), INT)
        val twoArgs = pool.functionType(listOf(INT, INT), INT)
        val originalDefault = pool.integer("7")
        val helperType = pool.functionType(listOf(INT), INT)
        val helperBody = pool.returnBlock(pool.integer("7"))
        val functionBody = pool.returnBlock(pool.reference("second", 4u, INT))
        val withDefaults = body(functionBody, INT, listOf(3u, 4u), listOf(0u, 5u))
        val lambdaBody = body(pool.block(NOTHING, listOf(6u, pool.returned(pool.integer("1"))), listOf(false, true)), 0u)
        val lambdaType = noArgs
        val initializer = pool.lambda(lambdaType, lambdaBody)
        val entryBody = pool.block(NOTHING, listOf(2u, pool.returned(pool.integer("42"))), listOf(false, true))
        val declarations = mutableListOf(
            function("entry", noArgs, body(entryBody, INT), topLevel = true), variable("closure", lambdaType, initializer),
            parameter("first", INT), parameter("second", INT, originalDefault),
            function("second.1", helperType, body(helperBody, INT, listOf(7u)), attributes = flags(Attribute.HAS_INITIAL, Attribute.IMPLICIT_ADD, Attribute.NO_REFLECT_INFO)),
            function("local", twoArgs, withDefaults), parameter("first", INT).copy(attributes = flags(Attribute.COMPILER_ADD)),
        )
        return pool.metadata("owneddefaultslocal", declarations)
    }

    /** d2 nominal owns d3 property，d3 owns d4/d5 accessor；d7 pattern owns d8 binding。 */
    private fun propertyPatternMetadata(): CjoPackageMetadata {
        val pool = Pool()
        val noArgs = pool.functionType(emptyList(), INT)
        val setterType = pool.functionType(listOf(INT), UNIT)
        val holderType = pool.type(CjoTypeMetadata(TypeKind.Struct, semanticInfo = CjoCompositeTypeInfoMetadata(2u)))
        val getterBody = pool.returnBlock(pool.integer("3"))
        val setterBody = pool.returnBlock(pool.unit())
        val constructorType = pool.functionType(emptyList(), UNIT)
        val constructorBody = pool.returnBlock(pool.unit())
        val initializer = pool.integer("7")
        val entryBody = pool.block(NOTHING, listOf(7u, pool.returned(pool.reference("bound", 8u, INT))), listOf(false, true))
        return pool.metadata("ownedroles", listOf(
            function("entry", noArgs, body(entryBody, INT), topLevel = true),
            CjoPackageDeclaration("Holder", DeclKind.StructDecl, type = holderType, attributes = flags(Attribute.PUBLIC, Attribute.GLOBAL), info = CjoStructInfo(body = listOf(3u, 9u))),
            CjoPackageDeclaration("value", DeclKind.PropDecl, isTopLevel = false, type = INT, attributes = flags(Attribute.PUBLIC),
                info = CjoPropertyInfo(isMutable = true, getters = listOf(4u), setters = listOf(5u))),
            function("value", noArgs, body(getterBody, INT), attributes = flags(Attribute.PUBLIC)),
            function("value", setterType, body(setterBody, UNIT, listOf(6u)), attributes = flags(Attribute.PUBLIC)),
            parameter("newValue", INT),
            CjoPackageDeclaration("", DeclKind.VarWithPatternDecl, isTopLevel = false, type = INT,
                info = CjoVarWithPatternInfo(irrefutablePattern = CjoPatternMetadata(PatternKind.VarPattern, types = listOf(INT), exprs = listOf(8u)), initializer = initializer)),
            variable("bound", INT, 0u),
            function("init", constructorType, body(constructorBody, UNIT), attributes = flags(Attribute.PUBLIC, Attribute.CONSTRUCTOR)),
        ))
    }

    private fun function(
        name: String, type: UInt, body: CjoFunctionBodyInfo, topLevel: Boolean = false,
        generic: CjoGenericMetadata? = null, attributes: List<ULong> = emptyList(),
    ): CjoPackageDeclaration = CjoPackageDeclaration(name, isTopLevel = topLevel, type = type, generic = generic,
        attributes = if (topLevel) flags(Attribute.PUBLIC, Attribute.GLOBAL) else attributes, info = CjoFunctionInfo(body = body))

    private fun variable(
        name: String, type: UInt, initializer: UInt, topLevel: Boolean = false, attributes: List<ULong> = emptyList(),
    ): CjoPackageDeclaration = CjoPackageDeclaration(name, DeclKind.VarDecl, isTopLevel = topLevel, type = type,
        attributes = if (topLevel) flags(Attribute.PUBLIC, Attribute.GLOBAL) else attributes, info = CjoVariableInfo(initializer = initializer))

    private fun parameter(name: String, type: UInt, default: UInt = 0u): CjoPackageDeclaration =
        CjoPackageDeclaration(name, DeclKind.FuncParam, isTopLevel = false, type = type,
            attributes = if (default != 0u) flags(Attribute.HAS_INITIAL) else emptyList(),
            info = CjoParameterInfo(isNamed = default != 0u, isMember = false, defaultValue = default))

    private fun body(body: UInt, result: UInt, parameters: List<UInt> = emptyList(), helpers: List<UInt> = List(parameters.size) { 0u }): CjoFunctionBodyInfo =
        CjoFunctionBodyInfo(parameterLists = listOf(parameters), desugaredParameterLists = listOf(helpers), returnType = result, body = body, always = true)

    /** 这里只装配公开 Writer 的输入池，不提供解析器、SOURCE binder 或 internal decoder。 */
    private class Pool {
        val types = mutableListOf(CjoTypeMetadata(TypeKind.Int64), CjoTypeMetadata(TypeKind.Nothing), CjoTypeMetadata(TypeKind.Bool), CjoTypeMetadata(TypeKind.Unit))
        val expressions = mutableListOf<CjoExpressionMetadata>()

        fun type(value: CjoTypeMetadata): UInt { types += value; return types.size.toUInt() }
        fun expression(value: CjoExpressionMetadata): UInt { expressions += value; return expressions.size.toUInt() }
        fun functionType(parameters: List<UInt>, result: UInt): UInt =
            type(CjoTypeMetadata(TypeKind.Func, parameters, semanticInfo = CjoFunctionTypeInfoMetadata(result, false, false)))
        fun reference(name: String, declaration: UInt, type: UInt, instantiatedTypes: List<UInt> = emptyList()): UInt = expression(CjoExpressionMetadata(ExprKind.RefExpr, type = type,
            reference = CjoReferenceExpressionMetadata(name, CjoExpressionTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = declaration), instantiatedTypes)))
        fun integer(value: String): UInt = expression(CjoExpressionMetadata(ExprKind.LitConstExpr, type = INT, literal = CjoLiteralExpressionMetadata(value, LitConstKind.Integer)))
        fun boolean(value: Boolean): UInt = expression(CjoExpressionMetadata(ExprKind.LitConstExpr, type = BOOL, literal = CjoLiteralExpressionMetadata(value.toString(), LitConstKind.Bool)))
        fun unit(): UInt = expression(CjoExpressionMetadata(ExprKind.LitConstExpr, type = UNIT, literal = CjoLiteralExpressionMetadata("", LitConstKind.Unit)))
        fun returned(value: UInt): UInt = expression(CjoExpressionMetadata(ExprKind.ReturnExpr, type = NOTHING, operands = listOf(value)))
        fun block(type: UInt, statements: List<UInt>, roles: List<Boolean> = List(statements.size) { true }): UInt {
            require(roles.size == statements.size)
            return expression(CjoExpressionMetadata(ExprKind.Block, type = type, operands = statements,
                additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(roles + false)))
        }
        fun returnBlock(value: UInt): UInt = block(NOTHING, listOf(returned(value)))
        fun lambda(type: UInt, body: CjoFunctionBodyInfo): UInt = expression(CjoExpressionMetadata(ExprKind.LambdaExpr, type = type,
            additionalInfo = CjoAdditionalExpressionInfoMetadata.Lambda(body, supportMock = false)))
        fun metadata(name: String, declarations: List<CjoPackageDeclaration>): CjoPackageMetadata = CjoPackageMetadata(
            fullPackageName = name, moduleName = name, schemaProfile = CjoSchemaProfile.OFFICIAL_V1_1_3,
            cjoVersion = CjoFormatVersion(0u, 1u, 0u), allFiles = listOf("source-owned.cj"),
            fileImports = listOf(CjoPackageFileImports(emptyList())),
            declarations = declarations, types = types.toList(), expressions = expressions.toList(),
        )
    }

    private data class Fixture(val packageName: String, val provider: CfirDeserializedSymbolProvider, val wire: Package, val originalWire: Package, val sourcePath: Path) {
        val source: CfirImportedSourceGraphProvider get() = provider
        fun callable(name: String): CfirDeclaration = provider.getTopLevelCallableSymbols(FqName(packageName), Name.identifier(name)).single().cfir
        fun function(name: String): CfirNamedFunction = assertIs<CfirNamedFunction>(callable(name))
        fun struct(name: String): CfirStruct = assertIs<CfirStruct>(assertNotNull(provider.getClassLikeSymbolByClassId(ClassId.topLevel(FqName("$packageName.$name")))).cfir)
        fun declaration(index: Int): CfirDeclaration = assertNotNull(provider.deserializeCjoDeclaration(sourcePath, packageName, index))
    }

    private data class TypeCorruption(val expression: UInt, val type: UInt)

    /** 原样重用 CjoSourcePublicPayloadTest 的真实 Writer/Manager/Library 装配；负例仅改保存字节。 */
    private fun fixture(root: Path, input: CjoPackageMetadata, corruptedMap: MapCorruption? = null, typeCorruption: TypeCorruption? = null): Fixture {
        Files.createDirectories(root)
        val encoded = CjoPackageWriter.toByteArray(input)
        val original = Package.getRootAsPackage(ByteBuffer.wrap(encoded.copyOf()))
        assertWireStructure(input, original)
        if (corruptedMap != null) {
            val wire = Package.getRootAsPackage(ByteBuffer.wrap(encoded))
            val expression = assertNotNull(wire.allExprs(corruptedMap.expression.toInt() - 1))
            assertEquals(corruptedMap.original, expression.mapExpr)
            assertTrue(expression.mutateMapExpr(corruptedMap.target))
        }
        if (typeCorruption != null) {
            val wire = Package.getRootAsPackage(ByteBuffer.wrap(encoded))
            assertTrue(assertNotNull(wire.allExprs(typeCorruption.expression.toInt() - 1)).mutateType(typeCorruption.type))
        }
        Files.write(root.resolve(input.fullPackageName + ".cjo"), encoded)
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(root.toFile()), envProvider = { null }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val module = Module()
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.session.register(CfirSymbolProvider::class, provider)
        val loaded = assertNotNull(manager.loadPackage(input.fullPackageName))
        return Fixture(input.fullPackageName, provider, loaded, original, root.resolve(input.fullPackageName + ".cjo"))
    }

    /** 消费前核对 d/e/t、mixed Block 角色、inline F 和非零正文，损坏借用不靠缺类型制造失败。 */
    private fun assertWireStructure(input: CjoPackageMetadata, wire: Package) {
        assertEquals(input.declarations.size, wire.allDeclsLength)
        assertEquals(input.types.size, wire.allTypesLength)
        assertEquals(input.expressions.size, wire.allExprsLength)
        fun validType(reference: UInt) { assertTrue(reference != 0u && reference <= wire.allTypesLength.toUInt()) }
        fun validExpression(reference: UInt) { assertTrue(reference != 0u && reference <= wire.allExprsLength.toUInt()) }
        fun assertBody(body: PackageFormat.FuncBody, expected: CjoFunctionBodyInfo) {
            assertEquals(expected.body, body.body)
            if (expected.body != 0u) {
                validExpression(body.body)
                assertEquals(ExprKind.Block, assertNotNull(wire.allExprs(body.body.toInt() - 1)).kind)
            }
            if (expected.returnType != 0u) validType(body.retType)
            assertEquals(expected.returnType, body.retType)
            assertEquals(expected.parameterLists.size, body.paramListsLength)
            for (index in expected.parameterLists.indices) {
                val list = assertNotNull(body.paramLists(index))
                assertEquals(expected.parameterLists[index], (0 until list.paramsLength).map { list.params(it) })
                assertEquals(expected.desugaredParameterLists[index], (0 until list.desugarsLength).map { list.desugars(it) })
                (0 until list.paramsLength).forEach { assertEquals(DeclKind.FuncParam, assertNotNull(wire.allDecls(list.params(it).toInt() - 1)).kind) }
                (0 until list.desugarsLength).forEach { if (list.desugars(it) != 0u) assertEquals(DeclKind.FuncDecl, assertNotNull(wire.allDecls(list.desugars(it).toInt() - 1)).kind) }
            }
        }
        input.declarations.forEachIndexed { index, expected ->
            val declaration = assertNotNull(wire.allDecls(index))
            assertEquals(expected.kind, declaration.kind)
            assertEquals(expected.isTopLevel, declaration.isTopLevel)
            validType(declaration.type)
            if (expected.info is CjoFunctionInfo) assertBody(assertNotNull((declaration.info(FuncInfo()) as FuncInfo).funcBody), expected.info.body)
        }
        input.expressions.forEachIndexed { index, expected ->
            val expression = assertNotNull(wire.allExprs(index))
            assertEquals(expected.kind, expression.kind)
            validType(expression.type)
            assertEquals(expected.mapExpression, expression.mapExpr)
            assertEquals(expected.operands, (0 until expression.operandsLength).map { expression.operands(it) })
            when (expected.kind) {
                ExprKind.Block -> {
                    assertEquals(ExprInfo.BlockInfo, expression.infoType)
                    val block = expression.info(BlockInfo()) as BlockInfo
                    assertEquals(expected.operands.size + 1, block.isExprLength)
                    assertEquals(assertIs<CjoAdditionalExpressionInfoMetadata.Block>(expected.additionalInfo).expressions,
                        (0 until block.isExprLength).map { block.isExpr(it) })
                    assertFalse(block.isExpr(expected.operands.size))
                    expected.operands.forEachIndexed { operand, reference ->
                        if (block.isExpr(operand)) validExpression(reference)
                        else assertTrue(reference != 0u && reference <= wire.allDeclsLength.toUInt())
                    }
                }
                ExprKind.ReturnExpr -> { assertEquals(ExprInfo.NONE, expression.infoType); assertEquals(1, expression.operandsLength); validExpression(expression.operands(0)) }
                ExprKind.LitConstExpr -> assertEquals(ExprInfo.LitConstInfo, expression.infoType)
                ExprKind.RefExpr -> {
                    assertEquals(ExprInfo.ReferenceInfo, expression.infoType)
                    val reference = expression.info(PackageFormat.ReferenceInfo()) as PackageFormat.ReferenceInfo
                    val expectedTarget = expected.reference?.target
                    if (expectedTarget == null) assertNull(reference.target)
                    else {
                        val target = assertNotNull(reference.target)
                        assertEquals(PackageIndex.CURRENT.value, target.pkgId)
                        assertEquals(expectedTarget.index, target.index)
                    }
                }
                ExprKind.LambdaExpr -> {
                    assertEquals(ExprInfo.LambdaInfo, expression.infoType)
                    assertTrue(expected.operands.isEmpty())
                    assertBody(assertNotNull((expression.info(LambdaInfo()) as LambdaInfo).funcBody), assertIs<CjoAdditionalExpressionInfoMetadata.Lambda>(expected.additionalInfo).body)
                }
                ExprKind.IfExpr -> { assertEquals(ExprInfo.NONE, expression.infoType); assertEquals(3, expression.operandsLength); expected.operands.forEach(::validExpression) }
                ExprKind.WhileExpr -> { assertEquals(ExprInfo.NONE, expression.infoType); assertEquals(2, expression.operandsLength); expected.operands.forEach(::validExpression) }
                ExprKind.JumpExpr -> { assertEquals(ExprInfo.JumpInfo, expression.infoType); assertTrue(expected.operands.isEmpty()); validExpression(expression.mapExpr) }
            }
        }
    }

    private fun flags(vararg attributes: Attribute): List<ULong> =
        MutableList(attributes.maxOf { it.ordinal / 64 } + 1) { 0uL }.apply {
            attributes.forEach { this[it.ordinal / 64] = this[it.ordinal / 64] or (1uL shl (it.ordinal % 64)) }
        }

    private class Module : CfirModuleData() {
        override val name: Name = Name.identifier("cjo-source-owned-closure-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = false
        override val stableModuleName: String = "cjo-source-owned-closure-test"
        override val session: CfirSession = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }

    private companion object {
        const val INT: UInt = 1u
        const val NOTHING: UInt = 2u
        const val BOOL: UInt = 3u
        const val UNIT: UInt = 4u
    }
}
