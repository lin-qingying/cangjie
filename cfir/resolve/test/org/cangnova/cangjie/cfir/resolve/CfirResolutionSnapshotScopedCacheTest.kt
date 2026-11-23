@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationStatus as ComputationStatus
import org.cangnova.cangjie.cfir.resolve.body.ImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSessionSnapshot
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AST 与隐式声明变换缓存的配对生命周期验收。
 *
 * 仅通过正式 builder、compute/computeType、getStatus 和 loop 查询观察协议；
 * 不读取快照字段、私有缓存表或计算栈，也不模拟候选选择或语言类型推断。
 */
class CfirResolutionSnapshotScopedCacheTest {
    /** 同一父 Computing 栈内局部重算两次；范围外新完成的源码声明、类型记录和 loop 不得被回滚。 */
    @Test
    fun rollbackPreservesOuterComputationsAndPhases() {
        val fixture = Fixture()
        val owner = ImplicitBodyResolveComputationSession()
        val global = fixture.function("sourceGlobal", isLocal = false)
        val typeOnly = fixture.function("sourceTypeOnly", isLocal = false)
        val outsideFirst = fixture.function("outsideFirst", isLocal = false)
        val outsideSecond = fixture.function("outsideSecond", isLocal = false)
        val globalReference = buildResolvedNamedReference {
            name = global.name
            resolvedSymbol = global.symbol
        }
        val dependency = buildFunctionCall { calleeReference = globalReference }
        val literal = fixture.literal()
        val localBody = buildBlock { statements.addAll(listOf(dependency, literal)) }
        val local = fixture.function("relay", body = localBody)
        val lambdaBody = buildBlock { statements += local }
        val lambda = fixture.lambda(lambdaBody)
        val parent = fixture.parent(lambda)
        val originalLocalType = local.returnTypeRef
        var localComputations = 0

        owner.compute(parent.symbol) {
            assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
            val ast = CfirResolutionSnapshot.capture(lambda)
            val cache = requireNotNull(ast.captureImplicitBodyCache(owner))

            // 非局部跳转发生在快照之后，引用仍在快照 AST 中，但声明不在其子树中。
            val globalType = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
            owner.compute(global.symbol) { fixture.complete(global, globalType) }
            val globalStatus = assertComputed(owner, global, globalType)
            assertEquals(CfirDeclarationOrigin.Source, global.origin)
            assertFalse(global.isLocal)
            val typeOnlyRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
            owner.computeType(typeOnly.symbol) {
                typeOnly.replaceReturnTypeRef(typeOnlyRef)
                typeOnly.replaceResolvePhase(CfirResolvePhase.IMPLICIT_TYPES)
                typeOnlyRef
            }
            val typeOnlyStatus = assertComputedType(owner, typeOnly, typeOnlyRef)

            owner.compute(outsideFirst.symbol) {
                owner.compute(outsideSecond.symbol) {
                    owner.calculateAndStoreNonTrivialLoop(outsideFirst.symbol)
                    fixture.complete(outsideSecond)
                }
                fixture.complete(outsideFirst)
            }
            val outsideFirstStatus = owner.getStatus(outsideFirst.symbol)
            val outsideSecondStatus = owner.getStatus(outsideSecond.symbol)

            repeat(2) {
                assertSame(ComputationStatus.NotComputed, owner.getStatus(local.symbol))
                val trialType = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
                owner.compute(local.symbol) {
                    localComputations++
                    literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
                    fixture.complete(local, trialType)
                }
                assertComputed(owner, local, trialType)

                ast.restoreWithImplicitBodyCache(cache, owner)

                assertSame(lambdaBody, lambda.anonymousFunction.body)
                assertSame(local, lambdaBody.statements.single())
                assertSame(localBody, local.body)
                assertSame(literal, localBody.statements.last())
                assertSame(originalLocalType, local.returnTypeRef)
                assertEquals(CfirResolvePhase.RAW_CFIR, local.resolvePhase)
                assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
                assertSame(ComputationStatus.NotComputed, owner.getStatus(local.symbol))
                assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
                assertSame(globalStatus, owner.getStatus(global.symbol))
                assertSame(globalType, global.returnTypeRef)
                assertEquals(CfirResolvePhase.BODY_RESOLVE, global.resolvePhase)
                assertSame(typeOnlyStatus, owner.getStatus(typeOnly.symbol))
                assertSame(typeOnlyRef, typeOnly.returnTypeRef)
                assertEquals(CfirResolvePhase.IMPLICIT_TYPES, typeOnly.resolvePhase)
                assertSame(global.symbol, globalReference.resolvedSymbol)
                assertSame(outsideFirstStatus, owner.getStatus(outsideFirst.symbol))
                assertSame(outsideSecondStatus, owner.getStatus(outsideSecond.symbol))
                assertEquals(CfirResolvePhase.BODY_RESOLVE, outsideFirst.resolvePhase)
                assertEquals(CfirResolvePhase.BODY_RESOLVE, outsideSecond.resolvePhase)
                assertTrue(owner.belongToSomeNonTrivialLoop(outsideFirst.symbol))
                assertTrue(owner.belongToSomeNonTrivialLoop(outsideSecond.symbol))
            }
            assertEquals(2, localComputations)
            fixture.complete(parent)
        }
        assertTrue(owner.getStatus(parent.symbol) is ComputationStatus.Computed)
    }

    /** lazy body 首次物化和再次物化产生的新符号都必须随旧 AST 恢复而撤销 Computed/ComputedType。 */
    @Test
    fun rollbackEvictsNewSymbolsFromMaterializedLazyBody() {
        val fixture = Fixture()
        val owner = ImplicitBodyResolveComputationSession()
        val lazyBody = buildLazyBlock()
        val lambda = fixture.lambda(lazyBody)
        val parent = fixture.parent(lambda)
        val discardedDeclarations = mutableListOf<CfirNamedFunction>()
        var computations = 0

        owner.compute(parent.symbol) {
            val ast = CfirResolutionSnapshot.capture(lambda)
            val cache = requireNotNull(ast.captureImplicitBodyCache(owner))

            repeat(2) {
                // 相同词法名字仅用于造声明；每次实际绑定的是新 CfirNamedFunctionSymbol。
                val materialized = fixture.function("relay")
                val materializedTypeOnly = fixture.function("typeOnly")
                discardedDeclarations.forEach {
                    assertNotSame(it.symbol, materialized.symbol)
                    assertNotSame(it.symbol, materializedTypeOnly.symbol)
                }
                val trialBody = buildBlock { statements.addAll(listOf(materialized, materializedTypeOnly)) }
                lambda.anonymousFunction.replaceBody(trialBody)
                val resolved = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
                owner.compute(materialized.symbol) {
                    computations++
                    fixture.complete(materialized, resolved)
                }
                owner.computeType(materializedTypeOnly.symbol) { resolved }
                assertComputed(owner, materialized, resolved)
                assertComputedType(owner, materializedTypeOnly, resolved)
                discardedDeclarations.addAll(listOf(materialized, materializedTypeOnly))

                ast.restoreWithImplicitBodyCache(cache, owner)

                assertSame(lazyBody, lambda.anonymousFunction.body)
                discardedDeclarations.forEach { assertSame(ComputationStatus.NotComputed, owner.getStatus(it.symbol)) }
                assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
            }
            assertEquals(2, computations)
            fixture.complete(parent)
        }
        assertTrue(owner.getStatus(parent.symbol) is ComputationStatus.Computed)
    }

    /** 局部试跑引入父 loop 标记；撤销时只删除这一来源，独立 loop 及其父标记仍保留。 */
    @Test
    fun rollbackRevokesOnlyTrialLoopProvenance() {
        for (outsideLoopContainsParent in listOf(false, true)) {
            val fixture = Fixture()
            val owner = ImplicitBodyResolveComputationSession()
            val local = fixture.function("relay")
            val lambda = fixture.lambda(buildBlock { statements += local })
            val parent = fixture.parent(lambda)
            val outsideFirst = fixture.function("outsideFirst", isLocal = false)
            val outsideSecond = fixture.function("outsideSecond", isLocal = false)

            owner.compute(parent.symbol) {
                val ast = CfirResolutionSnapshot.capture(lambda)
                val cache = requireNotNull(ast.captureImplicitBodyCache(owner))
                owner.compute(outsideFirst.symbol) {
                    owner.compute(outsideSecond.symbol) {
                        owner.calculateAndStoreNonTrivialLoop(
                            if (outsideLoopContainsParent) parent.symbol else outsideFirst.symbol,
                        )
                        fixture.complete(outsideSecond)
                    }
                    fixture.complete(outsideFirst)
                }
                val firstStatus = owner.getStatus(outsideFirst.symbol)
                val secondStatus = owner.getStatus(outsideSecond.symbol)
                assertEquals(outsideLoopContainsParent, owner.belongToSomeNonTrivialLoop(parent.symbol))
                owner.compute(local.symbol) {
                    owner.calculateAndStoreNonTrivialLoop(parent.symbol)
                    fixture.complete(local)
                }
                assertTrue(owner.belongToSomeNonTrivialLoop(local.symbol))
                assertTrue(owner.belongToSomeNonTrivialLoop(parent.symbol))

                ast.restoreWithImplicitBodyCache(cache, owner)

                assertFalse(owner.belongToSomeNonTrivialLoop(local.symbol))
                assertEquals(outsideLoopContainsParent, owner.belongToSomeNonTrivialLoop(parent.symbol))
                assertTrue(owner.belongToSomeNonTrivialLoop(outsideFirst.symbol))
                assertTrue(owner.belongToSomeNonTrivialLoop(outsideSecond.symbol))
                assertSame(firstStatus, owner.getStatus(outsideFirst.symbol))
                assertSame(secondStatus, owner.getStatus(outsideSecond.symbol))
                assertSame(ComputationStatus.NotComputed, owner.getStatus(local.symbol))
                assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
                fixture.complete(parent)
            }
            assertTrue(owner.getStatus(parent.symbol) is ComputationStatus.Computed)
        }
    }

    /** 两个非空 owner 即使活动符号栈相同也不能交换快照；失败前后 AST 与两侧缓存都不变。 */
    @Test
    fun wrongOwnerIsRejectedBeforeAstMutation() {
        val fixture = Fixture()
        val owner = ImplicitBodyResolveComputationSession()
        val otherOwner = ImplicitBodyResolveComputationSession()
        val literal = fixture.literal()
        val local = fixture.function("relay", body = buildBlock { statements += literal })
        val lambda = fixture.lambda(buildBlock { statements += local })
        val parent = fixture.parent(lambda)
        val trialType = ConePrimitiveType.INT64.toCfirResolvedTypeRef()

        owner.compute(parent.symbol) {
            val ast = CfirResolutionSnapshot.capture(lambda)
            val cache = requireNotNull(ast.captureImplicitBodyCache(owner))
            owner.compute(local.symbol) {
                literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
                fixture.complete(local, trialType)
            }
            val ownerStatus = assertComputed(owner, local, trialType)

            otherOwner.compute(parent.symbol) {
                otherOwner.compute(local.symbol) { local }
                val otherStatus = assertComputed(otherOwner, local, trialType)
                val failure = assertThrows(IllegalStateException::class.java) {
                    ast.restoreWithImplicitBodyCache(cache, otherOwner)
                }
                assertEquals("Implicit body cache replay changed owner", failure.message)
                assertSame(trialType, local.returnTypeRef)
                assertEquals(CfirResolvePhase.BODY_RESOLVE, local.resolvePhase)
                assertEquals(ConePrimitiveType.INT64, literal.coneTypeOrNull)
                assertSame(ownerStatus, owner.getStatus(local.symbol))
                assertSame(otherStatus, otherOwner.getStatus(local.symbol))
                assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
                assertSame(ComputationStatus.Computing, otherOwner.getStatus(parent.symbol))
                parent
            }

            ast.restoreWithImplicitBodyCache(cache, owner)
            assertSame(ComputationStatus.NotComputed, owner.getStatus(local.symbol))
            assertEquals(CfirResolvePhase.RAW_CFIR, local.resolvePhase)
            assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
            assertTrue(otherOwner.getStatus(local.symbol) is ComputationStatus.Computed)
            fixture.complete(parent)
        }
        assertTrue(owner.getStatus(parent.symbol) is ComputationStatus.Computed)
        assertTrue(otherOwner.getStatus(parent.symbol) is ComputationStatus.Computed)
    }

    /** 父栈增加一个 Computing 成员时先拒绝恢复；返回原栈后再恢复成功，栈由 compute 自己配对退出。 */
    @Test
    fun changedComputingStackIsRejectedBeforeAstMutation() {
        val fixture = Fixture()
        val owner = ImplicitBodyResolveComputationSession()
        val literal = fixture.literal()
        val local = fixture.function("relay", body = buildBlock { statements += literal })
        val lambda = fixture.lambda(buildBlock { statements += local })
        val parent = fixture.parent(lambda)
        val nestedParent = fixture.function("nestedParent", isLocal = false)
        val trialType = ConePrimitiveType.INT64.toCfirResolvedTypeRef()

        owner.compute(parent.symbol) {
            val ast = CfirResolutionSnapshot.capture(lambda)
            val cache = requireNotNull(ast.captureImplicitBodyCache(owner))
            owner.compute(local.symbol) {
                literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
                fixture.complete(local, trialType)
            }
            val localStatus = assertComputed(owner, local, trialType)

            owner.compute(nestedParent.symbol) {
                val failure = assertThrows(IllegalStateException::class.java) {
                    ast.restoreWithImplicitBodyCache(cache, owner)
                }
                assertEquals("Implicit body cache replay changed computing stack", failure.message)
                assertSame(trialType, local.returnTypeRef)
                assertEquals(CfirResolvePhase.BODY_RESOLVE, local.resolvePhase)
                assertEquals(ConePrimitiveType.INT64, literal.coneTypeOrNull)
                assertSame(localStatus, owner.getStatus(local.symbol))
                assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
                assertSame(ComputationStatus.Computing, owner.getStatus(nestedParent.symbol))
                fixture.complete(nestedParent)
            }
            val nestedStatus = owner.getStatus(nestedParent.symbol)

            ast.restoreWithImplicitBodyCache(cache, owner)

            assertSame(ComputationStatus.NotComputed, owner.getStatus(local.symbol))
            assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
            assertSame(nestedStatus, owner.getStatus(nestedParent.symbol))
            assertEquals(CfirResolvePhase.BODY_RESOLVE, nestedParent.resolvePhase)
            assertEquals(CfirResolvePhase.RAW_CFIR, local.resolvePhase)
            assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
            fixture.complete(parent)
        }
        assertTrue(owner.getStatus(parent.symbol) is ComputationStatus.Computed)
        assertTrue(owner.getStatus(nestedParent.symbol) is ComputationStatus.Computed)
    }

    /** 同深度、同词法名字但不同父符号的 Computing 栈也必须拒绝，不能只核验栈长或名字。 */
    @Test
    fun differentComputingParentAtSameDepthIsRejectedBeforeAstMutation() {
        val fixture = Fixture()
        val owner = ImplicitBodyResolveComputationSession()
        val literal = fixture.literal()
        val local = fixture.function("relay", body = buildBlock { statements += literal })
        val lambda = fixture.lambda(buildBlock { statements += local })
        val capturedParent = fixture.parent(lambda)
        val currentParent = fixture.function("parent", isLocal = false, type = ConePrimitiveType.INT64.toCfirResolvedTypeRef())
        assertNotSame(capturedParent.symbol, currentParent.symbol)
        lateinit var ast: CfirResolutionSnapshot
        lateinit var cache: CfirImplicitBodyResolveComputationSessionSnapshot

        owner.compute(capturedParent.symbol) {
            ast = CfirResolutionSnapshot.capture(lambda)
            cache = requireNotNull(ast.captureImplicitBodyCache(owner))
            fixture.complete(capturedParent)
        }
        val capturedParentStatus = owner.getStatus(capturedParent.symbol)
        val type = ConePrimitiveType.INT64.toCfirResolvedTypeRef()

        owner.compute(currentParent.symbol) {
            owner.compute(local.symbol) {
                literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
                fixture.complete(local, type)
            }
            val localStatus = assertComputed(owner, local, type)
            val failure = assertThrows(IllegalStateException::class.java) {
                ast.restoreWithImplicitBodyCache(cache, owner)
            }
            assertEquals("Implicit body cache replay changed computing stack", failure.message)
            assertSame(type, local.returnTypeRef)
            assertEquals(CfirResolvePhase.BODY_RESOLVE, local.resolvePhase)
            assertEquals(ConePrimitiveType.INT64, literal.coneTypeOrNull)
            assertSame(localStatus, owner.getStatus(local.symbol))
            assertSame(capturedParentStatus, owner.getStatus(capturedParent.symbol))
            assertSame(ComputationStatus.Computing, owner.getStatus(currentParent.symbol))
            fixture.complete(currentParent)
        }
        assertSame(capturedParentStatus, owner.getStatus(capturedParent.symbol))
        assertTrue(owner.getStatus(currentParent.symbol) is ComputationStatus.Computed)
    }

    /** 同名但不同符号的两段 AST 不可配错缓存 scope，拒绝必须早于任一 AST 槽的写回。 */
    @Test
    fun mismatchedAstScopeIsRejectedBeforeAstMutation() {
        val fixture = Fixture()
        val owner = ImplicitBodyResolveComputationSession()
        val firstLiteral = fixture.literal()
        val secondLiteral = fixture.literal()
        val first = fixture.function("relay", body = buildBlock { statements += firstLiteral })
        val second = fixture.function("relay", body = buildBlock { statements += secondLiteral })
        assertNotSame(first.symbol, second.symbol)
        val firstLambda = fixture.lambda(buildBlock { statements += first })
        val secondLambda = fixture.lambda(buildBlock { statements += second })
        val parent = fixture.function(
            "parent",
            isLocal = false,
            body = buildBlock { statements.addAll(listOf(firstLambda, secondLambda)) },
            type = ConePrimitiveType.INT64.toCfirResolvedTypeRef(),
        )
        val type = ConePrimitiveType.INT64.toCfirResolvedTypeRef()

        owner.compute(parent.symbol) {
            val firstAst = CfirResolutionSnapshot.capture(firstLambda)
            val firstCache = requireNotNull(firstAst.captureImplicitBodyCache(owner))
            val secondAst = CfirResolutionSnapshot.capture(secondLambda)
            val secondCache = requireNotNull(secondAst.captureImplicitBodyCache(owner))
            owner.compute(first.symbol) {
                firstLiteral.replaceConeTypeOrNull(ConePrimitiveType.INT64)
                fixture.complete(first, type)
            }
            owner.compute(second.symbol) {
                secondLiteral.replaceConeTypeOrNull(ConePrimitiveType.INT64)
                fixture.complete(second, type)
            }
            val firstStatus = assertComputed(owner, first, type)
            val secondStatus = assertComputed(owner, second, type)

            val failure = assertThrows(IllegalStateException::class.java) {
                secondAst.restoreWithImplicitBodyCache(firstCache, owner)
            }
            assertEquals("Implicit body cache/AST scope mismatch", failure.message)
            assertSame(type, first.returnTypeRef)
            assertSame(type, second.returnTypeRef)
            assertEquals(CfirResolvePhase.BODY_RESOLVE, first.resolvePhase)
            assertEquals(CfirResolvePhase.BODY_RESOLVE, second.resolvePhase)
            assertEquals(ConePrimitiveType.INT64, firstLiteral.coneTypeOrNull)
            assertEquals(ConePrimitiveType.INT64, secondLiteral.coneTypeOrNull)
            assertSame(firstStatus, owner.getStatus(first.symbol))
            assertSame(secondStatus, owner.getStatus(second.symbol))

            secondAst.restoreWithImplicitBodyCache(secondCache, owner)
            assertSame(firstStatus, owner.getStatus(first.symbol))
            assertSame(ComputationStatus.NotComputed, owner.getStatus(second.symbol))
            firstAst.restoreWithImplicitBodyCache(firstCache, owner)
            assertSame(ComputationStatus.NotComputed, owner.getStatus(first.symbol))
            assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
            fixture.complete(parent)
        }
        assertTrue(owner.getStatus(parent.symbol) is ComputationStatus.Computed)
    }

    /** cache 与 owner 的缺失能力必须配对；不允许用 null owner 吞掉一个真实 cache 的恢复。 */
    @Test
    fun cacheAndOwnerPresenceMismatchIsRejectedBeforeAstMutation() {
        val fixture = Fixture()
        val owner = ImplicitBodyResolveComputationSession()
        val literal = fixture.literal()
        val local = fixture.function("relay", body = buildBlock { statements += literal })
        val lambda = fixture.lambda(buildBlock { statements += local })
        val ast = CfirResolutionSnapshot.capture(lambda)
        val cache = requireNotNull(ast.captureImplicitBodyCache(owner))
        val type = ConePrimitiveType.INT64.toCfirResolvedTypeRef()
        owner.compute(local.symbol) {
            literal.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            fixture.complete(local, type)
        }
        val status = assertComputed(owner, local, type)

        val missingOwner = assertThrows(IllegalStateException::class.java) {
            ast.restoreWithImplicitBodyCache(cache, null)
        }
        val missingCache = assertThrows(IllegalStateException::class.java) {
            ast.restoreWithImplicitBodyCache(null, owner)
        }
        assertEquals("Implicit body cache snapshot/owner mismatch", missingOwner.message)
        assertEquals("Implicit body cache snapshot/owner mismatch", missingCache.message)
        assertSame(type, local.returnTypeRef)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, local.resolvePhase)
        assertEquals(ConePrimitiveType.INT64, literal.coneTypeOrNull)
        assertSame(status, owner.getStatus(local.symbol))

        ast.restoreWithImplicitBodyCache(cache, owner)
        assertSame(ComputationStatus.NotComputed, owner.getStatus(local.symbol))
        assertEquals(CfirResolvePhase.RAW_CFIR, local.resolvePhase)
        assertEquals(ConePrimitiveType.INT32, literal.coneTypeOrNull)
    }

    /**
     * OBL 成功状态实际使用的 call-root 配对 seam：成功 AST/cache 捕获后回滚，
     * 再覆盖另一试跑的物化 body 时恢复原成功记录，撤销被丢弃的 symbols，保留外部 source-global。
     */
    @Test
    fun successfulCallSnapshotReplaysSelectedBodyUnderOriginalOwner() {
        val fixture = Fixture()
        val owner = ImplicitBodyResolveComputationSession()
        val otherOwner = ImplicitBodyResolveComputationSession()
        val target = fixture.function("choose", isLocal = false)
        val lazyBody = buildLazyBlock()
        val lambda = fixture.lambda(lazyBody)
        val originalReference = buildNamedReference { name = target.name }
        val call = buildFunctionCall {
            calleeReference = originalReference
            argumentList = buildArgumentList { arguments += lambda }
        }
        val parent = fixture.function(
            "parent",
            isLocal = false,
            body = buildBlock { statements += call },
            type = ConePrimitiveType.INT64.toCfirResolvedTypeRef(),
        )
        val type = ConePrimitiveType.INT64.toCfirResolvedTypeRef()

        owner.compute(parent.symbol) {
            val initialAst = CfirResolutionSnapshot.capture(call)
            val initialCache = requireNotNull(initialAst.captureImplicitBodyCache(owner))
            owner.compute(target.symbol) { fixture.complete(target, type) }
            val targetStatus = assertComputed(owner, target, type)

            val selected = fixture.function("relay")
            val selectedTypeOnly = fixture.function("typeOnly")
            val selectedBody = buildBlock { statements.addAll(listOf(selected, selectedTypeOnly)) }
            lambda.anonymousFunction.replaceBody(selectedBody)
            owner.compute(selected.symbol) { fixture.complete(selected, type) }
            owner.computeType(selectedTypeOnly.symbol) { type }
            val selectedStatus = assertComputed(owner, selected, type)
            val selectedTypeOnlyStatus = assertComputedType(owner, selectedTypeOnly, type)
            val selectedReference = buildResolvedNamedReference {
                name = target.name
                resolvedSymbol = target.symbol
            }
            call.replaceCalleeReference(selectedReference)
            call.replaceConeTypeOrNull(ConePrimitiveType.INT64)
            val successfulAst = CfirResolutionSnapshot.capture(call)
            val successfulCache = requireNotNull(successfulAst.captureImplicitBodyCache(owner))

            initialAst.restoreWithImplicitBodyCache(initialCache, owner)

            assertSame(lazyBody, lambda.anonymousFunction.body)
            assertSame(originalReference, call.calleeReference)
            assertSame(ComputationStatus.NotComputed, owner.getStatus(selected.symbol))
            assertSame(ComputationStatus.NotComputed, owner.getStatus(selectedTypeOnly.symbol))
            assertSame(targetStatus, owner.getStatus(target.symbol))

            val discarded = fixture.function("relay")
            val discardedTypeOnly = fixture.function("typeOnly")
            assertNotSame(selected.symbol, discarded.symbol)
            assertNotSame(selectedTypeOnly.symbol, discardedTypeOnly.symbol)
            val discardedBody = buildBlock { statements.addAll(listOf(discarded, discardedTypeOnly)) }
            lambda.anonymousFunction.replaceBody(discardedBody)
            val discardedType = ConePrimitiveType.INT32.toCfirResolvedTypeRef()
            owner.compute(discarded.symbol) { fixture.complete(discarded, discardedType) }
            owner.computeType(discardedTypeOnly.symbol) { discardedType }
            val discardedStatus = assertComputed(owner, discarded, discardedType)
            val discardedTypeOnlyStatus = assertComputedType(owner, discardedTypeOnly, discardedType)

            otherOwner.compute(parent.symbol) {
                val failure = assertThrows(IllegalStateException::class.java) {
                    successfulAst.restoreWithImplicitBodyCache(successfulCache, otherOwner)
                }
                assertEquals("Implicit body cache replay changed owner", failure.message)
                assertSame(discardedBody, lambda.anonymousFunction.body)
                assertSame(originalReference, call.calleeReference)
                assertSame(discardedStatus, owner.getStatus(discarded.symbol))
                assertSame(discardedTypeOnlyStatus, owner.getStatus(discardedTypeOnly.symbol))
                assertSame(ComputationStatus.NotComputed, owner.getStatus(selected.symbol))
                parent
            }

            successfulAst.restoreWithImplicitBodyCache(successfulCache, owner)

            assertSame(lambda, call.argumentList.arguments.single())
            assertSame(selectedBody, lambda.anonymousFunction.body)
            assertSame(selected, selectedBody.statements[0])
            assertSame(selectedTypeOnly, selectedBody.statements[1])
            assertSame(selectedReference, call.calleeReference)
            assertSame(target.symbol, selectedReference.resolvedSymbol)
            assertEquals(ConePrimitiveType.INT64, call.coneTypeOrNull)
            assertSame(selectedStatus, owner.getStatus(selected.symbol))
            assertSame(selectedTypeOnlyStatus, owner.getStatus(selectedTypeOnly.symbol))
            assertComputed(owner, selected, type)
            assertComputedType(owner, selectedTypeOnly, type)
            assertEquals(CfirResolvePhase.BODY_RESOLVE, selected.resolvePhase)
            assertSame(ComputationStatus.NotComputed, owner.getStatus(discarded.symbol))
            assertSame(ComputationStatus.NotComputed, owner.getStatus(discardedTypeOnly.symbol))
            assertSame(targetStatus, owner.getStatus(target.symbol))
            assertSame(type, target.returnTypeRef)
            assertEquals(CfirResolvePhase.BODY_RESOLVE, target.resolvePhase)
            assertSame(ComputationStatus.Computing, owner.getStatus(parent.symbol))
            fixture.complete(parent)
        }
        assertTrue(owner.getStatus(parent.symbol) is ComputationStatus.Computed)
    }

    private fun assertComputed(
        owner: ImplicitBodyResolveComputationSession,
        declaration: CfirNamedFunction,
        type: CfirResolvedTypeRef,
    ): ComputationStatus.Computed {
        val status = owner.getStatus(declaration.symbol)
        assertTrue(status is ComputationStatus.Computed)
        val computed = status as ComputationStatus.Computed
        assertSame(declaration, computed.transformedDeclaration)
        assertSame(type, computed.resolvedTypeRef)
        assertSame(declaration, declaration.symbol.cfir)
        return computed
    }

    private fun assertComputedType(
        owner: ImplicitBodyResolveComputationSession,
        declaration: CfirNamedFunction,
        type: CfirResolvedTypeRef,
    ): ComputationStatus.ComputedType {
        val status = owner.getStatus(declaration.symbol)
        assertTrue(status is ComputationStatus.ComputedType)
        val computed = status as ComputationStatus.ComputedType
        assertSame(type, computed.resolvedTypeRef)
        return computed
    }

    /** 使用既有 Source session 与正式节点 builder；source 占位由模块已有测试 support 提供。 */
    private class Fixture {
        private val session = CallResolutionTestFixtures.newTestSession()
        private val source = requireNotNull(CallResolutionTestFixtures.buildTypedExpression(ConePrimitiveType.INT64).source)

        fun literal(): CfirLiteralExpression = buildLiteralExpression {
            source = this@Fixture.source
            kind = CfirLiteralKind.INT
            value = 1
            coneTypeOrNull = ConePrimitiveType.INT32
        }

        fun function(
            name: String,
            isLocal: Boolean = true,
            body: CfirBlock = buildBlock(),
            type: CfirTypeRef = buildImplicitTypeRef(),
        ): CfirNamedFunction = buildNamedFunction {
            source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            this.isLocal = isLocal
            status = CfirDeclarationStatusImpl()
            returnTypeRef = type
            this.body = body
            symbol = CfirNamedFunctionSymbol(CallableId(FqName.ROOT, Name.identifier(name)))
            this.name = Name.identifier(name)
            isMut = false
        }

        fun lambda(body: CfirBlock): CfirAnonymousFunctionExpression {
            val function = buildAnonymousFunction {
                source = this@Fixture.source
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.RAW_CFIR
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl()
                returnTypeRef = buildImplicitTypeRef()
                typeRef = buildImplicitTypeRef()
                this.body = body
                symbol = CfirAnonymousFunctionSymbol()
                hasExplicitParameterList = true
                isLambda = true
            }
            return buildAnonymousFunctionExpression {
                source = this@Fixture.source
                anonymousFunction = function
                isTrailingLambda = false
            }
        }

        fun parent(lambda: CfirAnonymousFunctionExpression): CfirNamedFunction =
            function(
                "parent",
                isLocal = false,
                body = buildBlock { statements += lambda },
                type = ConePrimitiveType.INT64.toCfirResolvedTypeRef(),
            )

        /** 测试驱动明确发布已完成的类型和 phase，不替代正式 resolver 的语言推断规则。 */
        fun complete(
            function: CfirNamedFunction,
            type: CfirResolvedTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(),
        ): CfirNamedFunction {
            function.replaceReturnTypeRef(type)
            function.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
            return function
        }
    }
}
