package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.CfirPublishedAnnotations
import org.cangnova.cangjie.cfir.CfirPublishedContentSnapshot
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.expressions.builder.buildReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.references.builder.buildThisReference
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirBodyPresence
import org.cangnova.cangjie.cfir.declarations.CfirModifierExplicitness
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirParameterDefaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.CfirInteropInfo
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildExtend
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunctionCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariableCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.exportFacts
import org.cangnova.cangjie.cfir.declarations.interopInfo
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirConstantValueExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePatternCopy
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.CfirImportedDeclarationHeaderSource
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionPayload
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionRecord
import org.cangnova.cangjie.cfir.source.CfirSourceFunctionContext
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceSlot
import org.cangnova.cangjie.cfir.source.CfirSourceStringKind
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/**
 * 导出输入冻结的是完整已解析图的一代身份和有序边。
 * 通过消费者的 walk/validateUnchanged 契约验证，不读取 children 存储或匹配内部错误文案。
 */
@OptIn(CfirImplementationDetail::class)
class CfirExportAnalysisInputSnapshotTest {
    /** 同名声明和同一个 Cone 不能合并两个 session；展开产品必须来自各自真实 classifier。 */
    @Test
    fun completedDependencyQueriesRetainRealAliasClassifierOwnerFileAndSessionIdentities() {
        val first = SourceFixture("completedDomains", withAlias = true)
        val second = SourceFixture("completedDomains", withAlias = true)
        val firstAlias = checkNotNull(first.alias)
        val secondAlias = checkNotNull(second.alias)
        val firstClassifier = checkNotNull(first.classifier)
        val secondClassifier = checkNotNull(second.classifier)
        val aliasType = ConeTypeAliasType(firstAlias.symbol.classId)
        val tuple = ConeTupleType(listOf(aliasType, aliasType))
        val domains = listOf(first.module.session, second.module.session)
        val graphs = listOf(completedUnitGraph(first), completedUnitGraph(second))
        val files = listOf(first.file, second.file)
        val roots = listOf<CfirDeclaration>(first.function, second.function)
        val work = CfirExportAnalysisInputPreparation.Work(files, roots) { emptyList() }
        work.complete(graphs, domains.associateWith { listOf(tuple) })
        val revision = work.dependencyRevision
        val expansions = mutableListOf<ConeCangJieType>()
        val declarationTypes = mutableListOf<ConeCangJieType>()
        work.withCompletedInputs { inputs ->
            assertThrows(UnsupportedOperationException::class.java) {
                (inputs.declarations as MutableList<CfirDeclaration>).clear()
            }
            for ((index, fixture) in listOf(first, second).withIndex()) {
                val domain = domains[index]
                val alias = checkNotNull(fixture.alias)
                val classifier = checkNotNull(fixture.classifier)
                assertTrue(inputs.declarations.any { it === alias })
                assertTrue(inputs.declarations.any { it === classifier })
                assertSame(alias, alias.symbol.cfir)
                assertSame(classifier, classifier.symbol.cfir)
                assertSame(fixture.file, inputs.sourceFileOf(fixture.function))
                assertSame(fixture.file, inputs.sourceFileOf(alias))
                assertSame(fixture.file, inputs.sourceFileOf(classifier))
                assertNull(inputs.owningParentOf(fixture.function))
                assertNull(inputs.owningParentOf(alias))
                val declared = checkNotNull(inputs.declarationTypeOf(fixture.function))
                declarationTypes += declared
                assertSame(declared, inputs.declarationTypeOf(fixture.function))
                assertSame(declared, inputs.typeDependenciesOf(declared, domain).type)
                val dependencies = inputs.typeDependenciesOf(tuple, domain)
                assertSame(tuple, dependencies.type)
                assertSame(domain, dependencies.domain)
                // children 是身份去重的依赖闭包；tuple 自身仍保存两个真实有序槽。
                assertSame(aliasType, dependencies.children.single())
                assertEquals(2, tuple.elementTypes.size)
                tuple.elementTypes.forEach { assertSame(aliasType, it) }
                assertThrows(UnsupportedOperationException::class.java) {
                    (dependencies.children as MutableList<ConeCangJieType>).clear()
                }
                assertSame(tuple, dependencies.expandedType)
                val original = inputs.typeDependenciesOf(aliasType, domain)
                assertSame(aliasType, original.type)
                assertSame(domain, original.domain)
                assertSame(alias, original.declaration)
                assertNull(original.abbreviatedType)
                assertNull(original.abbreviatedDeclaration)
                assertSame(classifier, original.expandedDeclaration)
                val expanded = original.expandedType
                expansions += expanded
                assertNotSame(aliasType, expanded)
                assertSame(aliasType, expanded.abbreviatedType)
                val expandedInputs = inputs.typeDependenciesOf(expanded, domain)
                assertSame(expanded, expandedInputs.type)
                assertSame(expanded, expandedInputs.expandedType)
                assertSame(classifier, expandedInputs.declaration)
                assertSame(classifier, expandedInputs.expandedDeclaration)
                assertSame(aliasType, expandedInputs.abbreviatedType)
                assertSame(alias, expandedInputs.abbreviatedDeclaration)
                assertSame(expanded, inputs.typeDependenciesOf(aliasType, domain).expandedType)
            }
        }
        assertNotSame(first.module.session, second.module.session)
        assertEquals(firstAlias.symbol.classId, secondAlias.symbol.classId)
        assertNotSame(firstAlias, secondAlias)
        assertNotSame(firstClassifier, secondClassifier)
        assertNotSame(expansions[0], expansions[1])
        assertEquals(revision, work.dependencyRevision)
        val prepared: CfirPreparedExportDependencyInputs = work.freeze()
        assertThrows(UnsupportedOperationException::class.java) {
            (prepared.declarations as MutableList<CfirDeclaration>).clear()
        }
        for ((index, fixture) in listOf(first, second).withIndex()) {
            val domain = domains[index]
            val original = prepared.typeDependenciesOf(aliasType, domain)
            assertSame(aliasType, original.type)
            assertSame(domain, original.domain)
            assertSame(fixture.alias, original.declaration)
            assertSame(fixture.classifier, original.expandedDeclaration)
            assertSame(expansions[index], original.expandedType)
            val expanded = prepared.typeDependenciesOf(expansions[index], domain)
            assertSame(expansions[index], expanded.type)
            assertSame(aliasType, expanded.abbreviatedType)
            assertSame(fixture.alias, expanded.abbreviatedDeclaration)
            assertSame(fixture.classifier, expanded.declaration)
            assertSame(declarationTypes[index], prepared.declarationTypeOf(fixture.function))
            assertSame(fixture.file, prepared.sourceFileOf(fixture.function))
            assertSame(fixture.file, prepared.sourceFileOf(checkNotNull(fixture.alias)))
            assertSame(fixture.file, prepared.sourceFileOf(checkNotNull(fixture.classifier)))
            assertNull(prepared.owningParentOf(fixture.function))
            assertThrows(IllegalStateException::class.java) {
                prepared.typeDependenciesOf(expansions[index], domains[1 - index])
            }
        }
        assertEquals(revision, work.dependencyRevision)
    }

    /** 查询缺项不能启动新 header；已完成参数的 owner/default 和最终 Result 均保持原身份。 */
    @Test
    fun completedAndFrozenDependencyQueriesNeverCompleteAnUnpreparedPublishedHeader() {
        val borrowed = PublishedHeaderFixture("queryKnown")
        val unknown = PublishedHeaderFixture("queryUnknown")
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), listOf(borrowed.function)) { emptyList() }
        work.complete()
        val publication = borrowed.snapshot()
        val unknownPublication = unknown.snapshot()
        val completions = borrowed.completions.toList()
        val revision = work.dependencyRevision
        work.withCompletedInputs { inputs ->
            assertTrue(borrowed.isReading)
            assertSame(ConePrimitiveType.INT64, inputs.declarationTypeOf(borrowed.parameter))
            assertSame(borrowed.function, inputs.owningParentOf(borrowed.parameter))
            assertNull(inputs.sourceFileOf(borrowed.parameter))
            assertSame(ConePrimitiveType.INT64,
                inputs.typeDependenciesOf(ConePrimitiveType.INT64, borrowed.module.session).type)
            assertFalse(inputs.declarations.any { it === unknown.function })
            assertThrows(NoSuchElementException::class.java) { inputs.declarationTypeOf(unknown.function) }
            assertThrows(NoSuchElementException::class.java) { inputs.owningParentOf(unknown.parameter) }
            assertThrows(NoSuchElementException::class.java) { inputs.sourceFileOf(unknown.function) }
            assertThrows(NoSuchElementException::class.java) {
                inputs.typeDependenciesOf(ConePrimitiveType.INT64, unknown.module.session)
            }
        }
        val prepared: CfirPreparedExportDependencyInputs = work.freeze()
        repeat(2) {
            assertSame(ConePrimitiveType.INT64, prepared.declarationTypeOf(borrowed.parameter))
            assertSame(borrowed.function, prepared.owningParentOf(borrowed.parameter))
            assertNull(prepared.sourceFileOf(borrowed.parameter))
            assertSame(borrowed.module.session,
                prepared.typeDependenciesOf(ConePrimitiveType.INT64, borrowed.module.session).domain)
            assertThrows(NoSuchElementException::class.java) { prepared.declarationTypeOf(unknown.function) }
            assertThrows(NoSuchElementException::class.java) { prepared.owningParentOf(unknown.parameter) }
            assertThrows(NoSuchElementException::class.java) { prepared.sourceFileOf(unknown.function) }
            assertThrows(IllegalStateException::class.java) {
                prepared.typeDependenciesOf(ConePrimitiveType.INT64, unknown.module.session)
            }
        }
        assertEquals(revision, work.dependencyRevision)
        assertEquals(completions.size, borrowed.completions.size)
        completions.forEachIndexed { index, declaration -> assertSame(declaration, borrowed.completions[index]) }
        assertSame(publication, borrowed.snapshot())
        assertSame(borrowed.default, borrowed.parameter.defaultValue)
        assertNull(borrowed.function.body)
        assertTrue(unknown.completions.isEmpty())
        assertSame(unknownPublication, unknown.snapshot())
        assertSame(CfirPublishedAnnotations.Unselected, unknownPublication.annotations(unknown.function))
        assertSame(CfirPublishedAnnotations.Unselected, unknownPublication.annotations(unknown.parameter))
        assertNull(unknown.parameter.defaultValue)
        assertFalse(borrowed.isReading)
    }

    /** 没有外层测试 fence；观测入口自身必须在原 owner 中校验 Published 签名。 */
    @Test
    fun completedObservationRejectsOriginalOwnerCompletionFromItsPublishedSnapshotCallback() {
        val borrowed = PublishedHeaderFixture("observationFence")
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), listOf(borrowed.function)) { emptyList() }
        work.complete()
        val publication = borrowed.snapshot()
        val completions = borrowed.completions.size
        val revision = work.dependencyRevision
        var snapshots = 0
        var callbacks = 0
        var completionAttempts = 0
        var callbackActive = false
        borrowed.afterSnapshot = {
            snapshots++
            assertTrue(borrowed.isReading)
            if (callbackActive) {
                completionAttempts++
                borrowed.completeHeader(borrowed.function)
            }
        }
        assertThrows(IllegalStateException::class.java) {
            work.withCompletedInputs { inputs ->
                callbacks++
                assertSame(borrowed.function, inputs.owningParentOf(borrowed.parameter))
                // 真实 Published getter 进入原 afterSnapshot；只有 Work 自身持有 owner fence。
                callbackActive = true
                try {
                    borrowed.parameter.defaultValue
                } finally {
                    // 后验要读取真实关联边；攻击已退出，保持原完成输入可被完整核验。
                    callbackActive = false
                }
            }
        }
        assertTrue(snapshots >= 1)
        assertEquals(1, completionAttempts)
        assertEquals(1, callbacks)
        assertEquals(completions, borrowed.completions.size)
        assertEquals(revision, work.dependencyRevision)
        assertFalse(borrowed.isReading)
        borrowed.afterSnapshot = null
        assertSame(publication, borrowed.snapshot())
        work.withCompletedInputs { assertSame(borrowed.function, it.owningParentOf(borrowed.parameter)) }
        assertSame(borrowed.default, work.freeze().declarations.filterIsInstance<CfirValueParameter>().single().defaultValue)
        assertSame(publication, borrowed.snapshot())
        assertEquals(completions, borrowed.completions.size)
    }

    /** 返回值也不能携带可用视图；下一次 complete 和最终 freeze 都不能使旧能力复活。 */
    @Test
    fun escapedCompletedDependencyViewRejectsEveryQueryBeforeAndAfterFurtherCompletionAndFreeze() {
        val borrowed = PublishedHeaderFixture("escapedObservation")
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), listOf(borrowed.function)) { emptyList() }
        work.complete()
        val escaped = work.withCompletedInputs { inputs ->
            assertSame(borrowed.function, inputs.owningParentOf(borrowed.parameter))
            inputs
        }
        assertUnavailableDependencyView(escaped, borrowed)
        work.complete()
        assertUnavailableDependencyView(escaped, borrowed)
        work.withCompletedInputs { current ->
            assertNotSame(escaped, current)
            assertUnavailableDependencyView(escaped, borrowed)
            assertSame(ConePrimitiveType.INT64, current.declarationTypeOf(borrowed.parameter))
        }
        val prepared: CfirPreparedExportDependencyInputs = work.freeze()
        assertUnavailableDependencyView(escaped, borrowed)
        assertSame(borrowed.function, prepared.owningParentOf(borrowed.parameter))
        assertThrows(IllegalStateException::class.java) { work.withCompletedInputs { error("Frozen callback ran") } }
        assertEquals(2, borrowed.completions.size)
    }

    /** FutureTask 把工作线程上的全部断言传回测试线程；回调尚活跃也不能跨线程读。 */
    @Test
    fun completedDependencyViewRejectsEveryQueryOnAnotherThreadWhileItsCallbackIsActive() {
        val borrowed = PublishedHeaderFixture("observationThread")
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), listOf(borrowed.function)) { emptyList() }
        work.complete()
        val publication = borrowed.snapshot()
        work.withCompletedInputs { inputs ->
            val observer = Thread.currentThread()
            val queries = FutureTask<Unit> {
                assertNotSame(observer, Thread.currentThread())
                assertUnavailableDependencyView(inputs, borrowed)
            }
            val thread = Thread(queries, "completed-dependency-reader").apply { isDaemon = true }
            thread.start()
            queries.get(10, TimeUnit.SECONDS)
            assertTrue(borrowed.isReading)
            assertSame(borrowed.function, inputs.owningParentOf(borrowed.parameter))
            assertSame(ConePrimitiveType.INT64, inputs.declarationTypeOf(borrowed.parameter))
        }
        assertFalse(borrowed.isReading)
        assertSame(publication, borrowed.snapshot())
        assertEquals(2, borrowed.completions.size)
        assertDoesNotThrow { work.freeze() }
    }

    /** 未完成和已中止的 Work 在进入任何 owner fence/回调之前拒绝发现。 */
    @Test
    fun completedObservationRejectsUncompletedAndAbortedWorkWithoutReadingItsOwner() {
        val borrowed = PublishedHeaderFixture("observationUnavailable")
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), listOf(borrowed.function)) { emptyList() }
        val publication = borrowed.snapshot()
        var callbacks = 0
        var fenceEntries = 0
        borrowed.beforeReadOnly = { fenceEntries++ }
        assertThrows(IllegalStateException::class.java) { work.withCompletedInputs { callbacks++ } }
        assertEquals(0, callbacks)
        assertEquals(0, fenceEntries)
        assertTrue(borrowed.completions.isEmpty())
        val failure = IllegalStateException("Original header owner rejected completion")
        borrowed.afterSnapshot = { throw failure }
        assertSame(failure, assertThrows(IllegalStateException::class.java) { work.complete() })
        borrowed.afterSnapshot = null
        assertThrows(IllegalStateException::class.java) { work.withCompletedInputs { callbacks++ } }
        assertEquals(0, callbacks)
        assertEquals(0, fenceEntries)
        assertSame(publication, borrowed.snapshot())
        assertTrue(borrowed.completions.isEmpty())
        assertNull(borrowed.parameter.defaultValue)
    }

    /** 禁止在发现回调里重抓输入或提前交付；被拒操作不消耗回调后的正常增量准备资格。 */
    @Test
    fun completedObservationRejectsCompleteFreezeAndNestedObservationWithoutRecapturingInputs() {
        val borrowed = PublishedHeaderFixture("observationProduction")
        val additional = PublishedHeaderFixture("observationAdditional")
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), listOf(borrowed.function)) { emptyList() }
        work.complete()
        val revision = work.dependencyRevision
        val publication = borrowed.snapshot()
        val additionalPublication = additional.snapshot()
        var nestedCallbacks = 0
        work.withCompletedInputs { inputs ->
            assertTrue(borrowed.isReading)
            assertThrows(IllegalStateException::class.java) {
                work.complete(additionalDeclarationInputs = listOf(additional.function))
            }
            assertThrows(IllegalStateException::class.java) { work.freeze() }
            assertThrows(IllegalStateException::class.java) { work.withCompletedInputs { nestedCallbacks++ } }
            assertEquals(0, nestedCallbacks)
            assertEquals(revision, work.dependencyRevision)
            assertFalse(inputs.declarations.any { it === additional.function || it === additional.parameter })
            assertSame(borrowed.function, inputs.owningParentOf(borrowed.parameter))
            assertSame(ConePrimitiveType.INT64, inputs.declarationTypeOf(borrowed.parameter))
            assertTrue(additional.completions.isEmpty())
            assertSame(additionalPublication, additional.snapshot())
        }
        assertSame(publication, borrowed.snapshot())
        assertEquals(2, borrowed.completions.size)
        work.complete(additionalDeclarationInputs = listOf(additional.function))
        work.withCompletedInputs { assertSame(additional.function, it.owningParentOf(additional.parameter)) }
        val prepared: CfirPreparedExportDependencyInputs = work.freeze()
        assertSame(additional.function, prepared.owningParentOf(additional.parameter))
        assertSame(additional.default, additional.parameter.defaultValue)
        assertEquals(2, additional.completions.size)
        assertSame(publication, borrowed.snapshot())
        assertEquals(2, borrowed.completions.size)
    }

    /** 相同已解析 Cone 的合法新 ref 仍是另一代输入，进入回调前必须拒绝。 */
    @Test
    fun completedObservationRejectsAnEquivalentTypeReferenceReplacementBeforeItsCallback() {
        val source = SourceFixture("observationPrevalidation")
        val graph = completedUnitGraph(source)
        val work = CfirExportAnalysisInputPreparation.Work(listOf(source.file), listOf(source.function)) { emptyList() }
        work.complete(listOf(graph))
        val revision = work.dependencyRevision
        val original = source.function.returnTypeRef as CfirResolvedTypeRef
        val replacement = buildResolvedTypeRef {
            this.source = original.source
            coneType = original.coneType
        }
        source.function.replaceReturnTypeRef(replacement)
        assertNotSame(original, replacement)
        assertSame(original.coneType, replacement.coneType)
        var callbacks = 0
        assertThrows(IllegalStateException::class.java) { work.withCompletedInputs { callbacks++ } }
        assertEquals(0, callbacks)
        assertSame(replacement, source.function.returnTypeRef)
        assertRejectedObservationWork(work)
        assertEquals(0, callbacks)
        assertEquals(revision, work.dependencyRevision)
        assertSame(replacement, source.function.returnTypeRef)
    }

    /** 绕过 API 直接换正确 ref：回调末尾校验必须拒绝交付，不能现场刷新完成基准。 */
    @Test
    fun completedObservationRejectsAnEquivalentTypeReferenceReplacementAfterItsCallback() {
        val borrowed = PublishedHeaderFixture("observationPostvalidation")
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), listOf(borrowed.function)) { emptyList() }
        work.complete()
        val original = borrowed.parameter.returnTypeRef as CfirResolvedTypeRef
        val replacement = buildResolvedTypeRef {
            source = original.source
            coneType = original.coneType
        }
        val revision = work.dependencyRevision
        val publication = borrowed.snapshot()
        var escaped: CfirPreparedExportDependencyInputs? = null
        var delivered = false
        var callbacks = 0
        assertThrows(IllegalStateException::class.java) {
            work.withCompletedInputs { inputs ->
                callbacks++
                escaped = inputs
                assertTrue(borrowed.isReading)
                assertSame(original.coneType, inputs.declarationTypeOf(borrowed.parameter))
                borrowed.parameter.replaceReturnTypeRef(replacement)
                assertNotSame(original, replacement)
                assertSame(original.coneType, replacement.coneType)
                assertThrows(IllegalStateException::class.java) { work.complete() }
                assertThrows(IllegalStateException::class.java) { work.freeze() }
                // 不再调用查询；只由 withCompletedInputs 的后验证发现这次直接写入。
            }
            delivered = true
        }
        assertEquals(1, callbacks)
        assertFalse(delivered)
        assertUnavailableDependencyView(checkNotNull(escaped), borrowed)
        assertEquals(revision, work.dependencyRevision)
        assertSame(replacement, borrowed.parameter.returnTypeRef)
        assertSame(publication, borrowed.snapshot())
        assertSame(borrowed.default, borrowed.parameter.defaultValue)
        assertEquals(2, borrowed.completions.size)
        assertFalse(borrowed.isReading)
        assertThrows(IllegalStateException::class.java) { work.withCompletedInputs { callbacks++ } }
        assertEquals(1, callbacks)
        assertRejectedObservationWork(work)
        assertSame(replacement, borrowed.parameter.returnTypeRef)
        assertEquals(revision, work.dependencyRevision)
    }

    /** 业务异常仍必须经过后验；输入完整时可再次发现、增量完成并正常交付。 */
    @Test
    fun callbackBusinessFailureWithUnchangedInputsRetainsTheCompletedWorkAfterPostvalidation() {
        val fixture = Fixture()
        val failure = IllegalArgumentException("Definition discovery business failure")
        var callbackExited = false
        var postvalidations = 0
        val members: (CfirDeclaration) -> List<CfirDeclaration> = {
            if (callbackExited) postvalidations++
            (it as CfirClass).declarations
        }
        val work = CfirExportAnalysisInputPreparation.Work(listOf(fixture.file), listOf(fixture.owner), members)
        work.complete()
        val revision = work.dependencyRevision
        var escaped: CfirPreparedExportDependencyInputs? = null
        var originalType: ConeCangJieType? = null
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) {
            work.withCompletedInputs { inputs ->
                escaped = inputs
                originalType = inputs.declarationTypeOf(fixture.function)
                assertSame(fixture.owner, inputs.owningParentOf(fixture.function))
                callbackExited = true
                throw failure
            }
        })
        assertEquals(1, postvalidations, "业务抛错也必须执行原 member reader 的后验")
        assertTrue(failure.suppressed.isEmpty())
        assertUnavailableDependencyView(checkNotNull(escaped), fixture)
        assertEquals(revision, work.dependencyRevision)
        callbackExited = false
        work.withCompletedInputs { inputs ->
            assertSame(originalType, inputs.declarationTypeOf(fixture.function))
            assertSame(fixture.file, inputs.sourceFileOf(fixture.function))
        }
        work.complete()
        assertEquals(revision, work.dependencyRevision)
        val prepared: CfirPreparedExportDependencyInputs = work.freeze()
        assertSame(originalType, prepared.declarationTypeOf(fixture.function))
        assertSame(fixture.owner, prepared.owningParentOf(fixture.function))
        assertSame(fixture.file, prepared.sourceFileOf(fixture.function))
    }

    /** 签名漂移加业务抛错必须保留两份异常证据，且 complete 不能吸收新 ref 为基准。 */
    @Test
    fun callbackFailureWithTypeReferenceDriftPreservesSuppressedEvidenceAndAbortsTheWork() {
        val borrowed = PublishedHeaderFixture("callbackDriftFailure")
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), listOf(borrowed.function)) { emptyList() }
        work.complete()
        val original = borrowed.parameter.returnTypeRef as CfirResolvedTypeRef
        val replacement = buildResolvedTypeRef {
            source = original.source
            coneType = original.coneType
        }
        val callbackFailure = IllegalArgumentException("Discovery failed after replacing the actual parameter ref")
        val publication = borrowed.snapshot()
        val revision = work.dependencyRevision
        var escaped: CfirPreparedExportDependencyInputs? = null
        val validationFailure = assertThrows(IllegalStateException::class.java) {
            work.withCompletedInputs { inputs ->
                escaped = inputs
                assertTrue(borrowed.isReading)
                assertSame(original.coneType, inputs.declarationTypeOf(borrowed.parameter))
                borrowed.parameter.replaceReturnTypeRef(replacement)
                throw callbackFailure
            }
        }
        assertSame(callbackFailure, validationFailure.suppressed.single())
        assertUnavailableDependencyView(checkNotNull(escaped), borrowed)
        assertNotSame(original, replacement)
        assertSame(original.coneType, replacement.coneType)
        assertSame(replacement, borrowed.parameter.returnTypeRef)
        assertRejectedObservationWork(work)
        assertSame(replacement, borrowed.parameter.returnTypeRef)
        assertEquals(revision, work.dependencyRevision)
        assertSame(publication, borrowed.snapshot())
        assertSame(borrowed.default, borrowed.parameter.defaultValue)
        assertEquals(2, borrowed.completions.size)
        assertFalse(borrowed.isReading)
    }

    /** 原关联 reader 无法完成前验也须中止；不只处理中途修改字段的情况。 */
    @Test
    fun failedCompletedInputPrevalidationRejectsCompleteObserveAndFreezeWithoutRecapturing() {
        val fixture = Fixture()
        val failure = IllegalStateException("Original member reader failed before observation")
        var failValidation = false
        var readsAfterFailure = 0
        val members: (CfirDeclaration) -> List<CfirDeclaration> = {
            if (failValidation) {
                readsAfterFailure++
                throw failure
            }
            (it as CfirClass).declarations
        }
        val work = CfirExportAnalysisInputPreparation.Work(listOf(fixture.file), listOf(fixture.owner), members)
        work.complete()
        val revision = work.dependencyRevision
        val ref = fixture.function.returnTypeRef
        var callbacks = 0
        failValidation = true
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            work.withCompletedInputs { callbacks++ }
        })
        assertEquals(0, callbacks)
        assertEquals(1, readsAfterFailure)
        failValidation = false
        assertRejectedObservationWork(work)
        assertEquals(1, readsAfterFailure)
        assertSame(ref, fixture.function.returnTypeRef)
        assertEquals(revision, work.dependencyRevision)
    }

    /** 后验本身抛错，无论业务回调是否抛错都关闭 Work；恢复 reader 不能重抓关联证据。 */
    @Test
    fun failedCompletedInputPostvalidationAbortsNormalAndThrowingCallbacksAndPreservesBothFailures() {
        for (throwFromCallback in listOf(false, true)) {
            val fixture = Fixture()
            val validationFailure = IllegalStateException("Original member reader failed during postvalidation")
            val callbackFailure = IllegalArgumentException("Definition discovery failed")
            var callbackExited = false
            var postvalidations = 0
            val members: (CfirDeclaration) -> List<CfirDeclaration> = {
                if (callbackExited) {
                    postvalidations++
                    throw validationFailure
                }
                (it as CfirClass).declarations
            }
            val work = CfirExportAnalysisInputPreparation.Work(listOf(fixture.file), listOf(fixture.owner), members)
            work.complete()
            val revision = work.dependencyRevision
            val ref = fixture.function.returnTypeRef
            var escaped: CfirPreparedExportDependencyInputs? = null
            assertSame(validationFailure, assertThrows(IllegalStateException::class.java) {
                work.withCompletedInputs { inputs ->
                    escaped = inputs
                    assertSame(fixture.owner, inputs.owningParentOf(fixture.function))
                    callbackExited = true
                    if (throwFromCallback) throw callbackFailure
                }
            })
            assertEquals(1, postvalidations)
            if (throwFromCallback) assertSame(callbackFailure, validationFailure.suppressed.single())
            else assertTrue(validationFailure.suppressed.isEmpty())
            assertUnavailableDependencyView(checkNotNull(escaped), fixture)
            callbackExited = false
            assertRejectedObservationWork(work)
            assertEquals(revision, work.dependencyRevision)
            assertSame(ref, fixture.function.returnTypeRef)
        }
    }

    /** 后验调用真实 member reader 时 action 已退出；旧 view 的五个入口必须全部先拒绝重入。 */
    @Test
    fun completedDependencyViewIsRevokedBeforePostvalidationForNormalAndThrowingCallbacks() {
        for (throwFromCallback in listOf(false, true)) {
            val fixture = Fixture()
            val callbackFailure = IllegalArgumentException("Business failure with complete inputs")
            var callbackExited = false
            var postvalidations = 0
            var escaped: CfirPreparedExportDependencyInputs? = null
            val members: (CfirDeclaration) -> List<CfirDeclaration> = {
                if (callbackExited) {
                    postvalidations++
                    assertUnavailableDependencyView(checkNotNull(escaped), fixture)
                }
                (it as CfirClass).declarations
            }
            val work = CfirExportAnalysisInputPreparation.Work(listOf(fixture.file), listOf(fixture.owner), members)
            work.complete()
            val callback: (CfirPreparedExportDependencyInputs) -> Unit = { inputs ->
                escaped = inputs
                assertSame(fixture.file, inputs.sourceFileOf(fixture.function))
                callbackExited = true
                if (throwFromCallback) throw callbackFailure
            }
            if (throwFromCallback) {
                assertSame(callbackFailure, assertThrows(IllegalArgumentException::class.java) {
                    work.withCompletedInputs(callback)
                })
                assertTrue(callbackFailure.suppressed.isEmpty())
            } else work.withCompletedInputs(callback)
            assertEquals(1, postvalidations)
            assertUnavailableDependencyView(checkNotNull(escaped), fixture)
            callbackExited = false
            work.withCompletedInputs { assertSame(fixture.owner, it.owningParentOf(fixture.function)) }
            assertSame(fixture.file, work.freeze().sourceFileOf(fixture.function))
        }
    }

    /** 已完成关联在首次发现前变化，必须由前验拒绝，不能等到 freeze 后才发现。 */
    @Test
    fun completedObservationPrevalidationRejectsRemovedClassExtendFileReaderAndLexicalAssociations() {
        for (input in associationInputs()) {
            val work = input.work()
            work.complete(input.graphs)
            work.withCompletedInputs { input.assertOriginalAssociations(it) }
            val revision = work.dependencyRevision
            input.removeAssociation()
            input.assertHeaderIdentities()
            var callbacks = 0
            assertThrows(IllegalStateException::class.java, {
                work.withCompletedInputs { callbacks++ }
            }, input.label)
            assertEquals(0, callbacks, input.label)
            assertRejectedObservationWork(work)
            input.assertHeaderIdentities()
            assertEquals(revision, work.dependencyRevision, input.label)
        }
    }

    /** 回调内查询与后验都核原关联；吞掉 getter 的拒绝也不能使 Work 回到可完成状态。 */
    @Test
    fun completedObservationGettersAndPostvalidationRejectRemovedClassExtendFileReaderAndLexicalAssociations() {
        for (input in associationInputs()) {
            val work = input.work()
            work.complete(input.graphs)
            val revision = work.dependencyRevision
            var callbackFinished = false
            assertThrows(IllegalStateException::class.java, {
                work.withCompletedInputs { inputs ->
                    input.assertOriginalAssociations(inputs)
                    input.removeAssociation()
                    input.assertHeaderIdentities()
                    assertThrows(IllegalStateException::class.java) { inputs.owningParentOf(input.function) }
                    assertThrows(IllegalStateException::class.java) { inputs.sourceFileOf(input.function) }
                    callbackFinished = true
                }
            }, input.label)
            assertTrue(callbackFinished, input.label)
            assertRejectedObservationWork(work)
            input.assertHeaderIdentities()
            assertEquals(revision, work.dependencyRevision, input.label)
        }
    }

    /** 最终 Result 必须消费 complete 的原证明；owner/file getter 不能重查当前 provider 或列表。 */
    @Test
    fun frozenResultOwnerAndFileGettersRejectRemovedClassExtendFileReaderAndLexicalAssociations() {
        for (input in associationInputs()) {
            val work = input.work()
            work.complete(input.graphs)
            val prepared: CfirPreparedExportDependencyInputs = work.freeze()
            input.assertOriginalAssociations(prepared)
            val originalType = prepared.declarationTypeOf(input.function)
            input.removeAssociation()
            input.assertHeaderIdentities()
            assertSame(originalType, prepared.declarationTypeOf(input.function), input.label)
            assertThrows(IllegalStateException::class.java, { prepared.owningParentOf(input.function) }, input.label)
            assertThrows(IllegalStateException::class.java, { prepared.sourceFileOf(input.function) }, input.label)
            input.assertHeaderIdentities()
        }
    }

    /** freeze 前已移除的真实边不能在 freeze 中被重抓成另一份“完成”证明。 */
    @Test
    fun finalFreezeRejectsAssociationDriftSinceCompletionWithoutRecapturingCurrentLists() {
        for (input in associationInputs()) {
            val work = input.work()
            work.complete(input.graphs)
            val revision = work.dependencyRevision
            input.removeAssociation()
            input.assertHeaderIdentities()
            assertThrows(IllegalStateException::class.java, { work.freeze() }, input.label)
            assertRejectedObservationWork(work)
            assertEquals(revision, work.dependencyRevision, input.label)
            input.assertHeaderIdentities()
        }
    }

    /** 失败状态必须拒绝所有重新生产/观察/交付入口，且拒绝过程不增加库存或修订号。 */
    private fun assertRejectedObservationWork(work: CfirExportAnalysisInputPreparation.Work) {
        val declarations = work.declarations
        val revision = work.dependencyRevision
        var callbacks = 0
        assertThrows(IllegalStateException::class.java) { work.complete() }
        assertThrows(IllegalStateException::class.java) { work.withCompletedInputs { callbacks++ } }
        assertThrows(IllegalStateException::class.java) { work.freeze() }
        assertEquals(0, callbacks)
        assertEquals(revision, work.dependencyRevision)
        val remaining = work.declarations
        assertEquals(declarations.size, remaining.size)
        declarations.forEachIndexed { index, declaration -> assertSame(declaration, remaining[index]) }
    }

    private fun assertUnavailableDependencyView(inputs: CfirPreparedExportDependencyInputs, fixture: Fixture) {
        assertThrows(IllegalStateException::class.java) { inputs.declarations }
        assertThrows(IllegalStateException::class.java) { inputs.declarationTypeOf(fixture.function) }
        assertThrows(IllegalStateException::class.java) {
            inputs.typeDependenciesOf(ConePrimitiveType.INT64, fixture.function.moduleData.session)
        }
        assertThrows(IllegalStateException::class.java) { inputs.owningParentOf(fixture.function) }
        assertThrows(IllegalStateException::class.java) { inputs.sourceFileOf(fixture.function) }
    }

    /** 每份输入只删一类真实边；保留 bound symbol、原 ref/Cone、status、参数向量及正文根。 */
    private class AssociationInput(
        val label: String,
        val file: CfirFile,
        val root: CfirDeclaration,
        val function: CfirNamedFunction,
        private val owner: CfirDeclaration?,
        private val members: (CfirDeclaration) -> List<CfirDeclaration>,
        val removeAssociation: () -> Unit,
        val graphs: List<CfirSourceGraph> = emptyList(),
    ) {
        private val symbol = function.symbol
        private val ref = function.returnTypeRef as CfirResolvedTypeRef
        private val type = ref.coneType
        private val receiver = function.dispatchReceiverType
        private val status = function.status
        private val body = function.body
        private val parameters = function.valueParameters.toList()
        private val rootSymbol = root.symbol

        fun work(): CfirExportAnalysisInputPreparation.Work =
            CfirExportAnalysisInputPreparation.Work(listOf(file), listOf(root), members)

        fun assertOriginalAssociations(inputs: CfirPreparedExportDependencyInputs) {
            assertSame(owner, inputs.owningParentOf(function), label)
            assertSame(file, inputs.sourceFileOf(function), label)
        }

        fun assertHeaderIdentities() {
            assertSame(symbol, function.symbol, label)
            assertSame(function, symbol.cfir, label)
            assertSame(rootSymbol, root.symbol, label)
            assertSame(root, rootSymbol.cfir, label)
            assertSame(ref, function.returnTypeRef, label)
            assertSame(type, (function.returnTypeRef as CfirResolvedTypeRef).coneType, label)
            assertSame(receiver, function.dispatchReceiverType, label)
            assertSame(status, function.status, label)
            assertSame(body, function.body, label)
            assertEquals(parameters.size, function.valueParameters.size, label)
            parameters.forEachIndexed { index, parameter -> assertSame(parameter, function.valueParameters[index], label) }
        }
    }

    private fun associationInputs(): List<AssociationInput> {
        val classFixture = Fixture()
        val classInput = AssociationInput(
            "class member", classFixture.file, classFixture.owner, classFixture.function, classFixture.owner,
            { (it as CfirClass).declarations },
            { assertSame(classFixture.function, (classFixture.owner.declarations as MutableList<CfirDeclaration>).removeAt(0)) },
        )
        val fileFixture = SourceFixture("associationFile")
        val fileInput = AssociationInput(
            "file declaration", fileFixture.file, fileFixture.function, fileFixture.function, null, { emptyList() },
            { assertSame(fileFixture.function, (fileFixture.file.declarations as MutableList<CfirDeclaration>).removeAt(0)) },
        )
        val readerFixture = Fixture()
        val projection = readerFixture.owner.declarations.toMutableList()
        val readerInput = AssociationInput(
            "member reader", readerFixture.file, readerFixture.owner, readerFixture.function, readerFixture.owner,
            { projection }, { assertSame(readerFixture.function, projection.removeAt(0)) },
        )
        val lexicalFixture = SourceFixture("associationLexical")
        val literal = lexicalFixture.body.statements.single()
        val lexicalInput = AssociationInput(
            "lexical body child", lexicalFixture.file, lexicalFixture.function, lexicalFixture.function, null, { emptyList() },
            { assertSame(literal, lexicalFixture.body.mutableStatements().removeAt(0)) },
        )
        val extendFixture = SourceFixture("associationExtend", withAlias = true, withExtend = true)
        val extend = checkNotNull(extendFixture.extend)
        val extendFunction = extend.declarations.single() as CfirNamedFunction
        val builder = CfirSourceGraph.Builder()
        val literalIdentity = builder.reserveGenerated()
        val extendLiteral = extendFixture.body.statements.single() as CfirLiteralExpression
        builder.fill(literalIdentity, CfirSourceExpressionRecord(
            extendFunction, checkNotNull(extendLiteral.coneTypeOrNull), null,
            CfirSourceExpressionPayload.Literal(extendLiteral.kind, "()", CfirSourceStringKind.NORMAL),
        ))
        val bodyIdentity = builder.reserveGenerated()
        builder.fill(bodyIdentity, CfirSourceExpressionRecord(
            extendFunction, checkNotNull(extendFixture.body.coneTypeOrNull), null,
            CfirSourceExpressionPayload.Block(listOf(CfirSourceSlot.Expression(literalIdentity)), isUnsafe = false),
        ))
        builder.body(extendFunction, bodyIdentity)
        builder.declarationOwner(extendFunction, extend)
        builder.functionContext(extendFunction, CfirSourceFunctionContext(extend, null))
        val extendInput = AssociationInput(
            "extend member", extendFixture.file, extend, extendFunction, extend,
            {
                when (it) {
                    is CfirExtend -> it.declarations
                    is CfirClass -> it.declarations
                    is CfirTypeAlias -> it.declarations
                    else -> error("Expected a completed nominal or extend owner")
                }
            },
            { assertSame(extendFunction, (extend.declarations as MutableList<CfirDeclaration>).removeAt(0)) },
            listOf(builder.finish()),
        )
        return listOf(classInput, extendInput, fileInput, readerInput, lexicalInput)
    }

    /** 五个公开读取入口都必须先验证回调能力，不能因某个键已经完成而绕过生命周期。 */
    private fun assertUnavailableDependencyView(inputs: CfirPreparedExportDependencyInputs, fixture: PublishedHeaderFixture) {
        assertThrows(IllegalStateException::class.java) { inputs.declarations }
        assertThrows(IllegalStateException::class.java) { inputs.declarationTypeOf(fixture.parameter) }
        assertThrows(IllegalStateException::class.java) {
            inputs.typeDependenciesOf(ConePrimitiveType.INT64, fixture.module.session)
        }
        assertThrows(IllegalStateException::class.java) { inputs.owningParentOf(fixture.parameter) }
        assertThrows(IllegalStateException::class.java) { inputs.sourceFileOf(fixture.parameter) }
    }

    /** 增量 complete 排空真实 header/default/type 闭包，唯一交付域采用最后完成的 SOURCE 图身份。 */
    @Test
    fun workCompletesIncrementalHeaderAndTypeDependenciesBeforeFreezingTheFinalSourceDomain() {
        val source = SourceFixture("workFinal")
        val borrowed = PublishedHeaderFixture("workBorrowed")
        val files = listOf(source.file)
        val roots = listOf<CfirDeclaration>(source.function)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { emptyList() }
        val originalGraph = completedUnitGraph(source)
        val work = CfirExportAnalysisInputPreparation.Work(files, roots, members)
        work.complete(listOf(originalGraph))
        val initialDeclarations = work.declarations
        assertFalse(initialDeclarations.any { it === borrowed.function || it === borrowed.parameter })
        assertSame(CfirPublishedAnnotations.Unselected, borrowed.snapshot().annotations(borrowed.function))
        assertNull(borrowed.parameter.defaultValue)

        val finalGraph = originalGraph.withOriginAliases(mapOf(
            source.body to originalGraph.functionBodies.getValue(source.function),
            source.body.statements.single() to originalGraph.expressions.entries.single {
                it.value.payload is CfirSourceExpressionPayload.Literal
            }.key,
        ))
        val savedType = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64))
        val savedTypes = mapOf(borrowed.module.session to listOf(savedType))
        val additionalDeclarations = listOf<CfirDeclaration>(borrowed.function)
        work.complete(listOf(finalGraph), savedTypes, additionalDeclarations)
        assertNotSame(originalGraph, finalGraph)
        assertSame(originalGraph.expressions, finalGraph.expressions)
        assertSame(originalGraph.functionBodies, finalGraph.functionBodies)
        assertFalse(originalGraph.containsOrigin(source.body))
        assertTrue(finalGraph.containsOrigin(source.body))
        assertSame(finalGraph.functionBodies.getValue(source.function), finalGraph.expressionOf(source.body))
        val block = finalGraph.expression(finalGraph.expressionOf(source.body)).payload as CfirSourceExpressionPayload.Block
        val literalIdentity = (block.statements.single() as CfirSourceSlot.Expression).identity
        assertSame(literalIdentity, finalGraph.expressionOf(source.body.statements.single()))
        assertSame(originalGraph.expression(literalIdentity), finalGraph.expression(literalIdentity))
        assertFalse(initialDeclarations.any { it === borrowed.function })
        assertTrue(work.declarations.any { it === borrowed.function })
        assertTrue(work.declarations.any { it === borrowed.parameter })
        assertEquals(2, borrowed.completions.size)
        assertSame(borrowed.function, borrowed.completions[0])
        assertSame(borrowed.parameter, borrowed.completions[1])
        assertSame(borrowed.default, borrowed.parameter.defaultValue)
        assertSame(ConePrimitiveType.INT64, borrowed.default.coneTypeOrNull)
        assertEquals(41L, borrowed.default.value)

        val publication = borrowed.snapshot()
        val prepared = work.freeze()
        assertTrue(prepared.declarations.any { it === borrowed.parameter })
        assertSame(borrowed.function, borrowed.function.symbol.cfir)
        assertSame(borrowed.parameter, borrowed.parameter.symbol.cfir)
        assertSame(borrowed.function.symbol, borrowed.parameter.containingDeclarationSymbol)
        assertTrue(prepared.containsType(savedType, borrowed.module.session))
        assertTrue(prepared.containsType(ConePrimitiveType.INT64, borrowed.module.session))
        assertFalse(prepared.containsType(savedType, source.module.session))
        assertFalse(prepared.containsType(ConePrimitiveType.INT64, source.module.session))
        val input = CfirExportAnalysisInputSnapshot.capture(
            files, roots, members, listOf(finalGraph), savedTypes, additionalDeclarations, prepared,
        )
        assertSame(finalGraph, input.canonicalSources.single())
        assertSame(finalGraph, input.canonicalSourceGraphOf(source.function))
        assertSame(source.file, input.sourceFiles.getValue(source.function))
        assertSame(borrowed.function, input.owners.getValue(borrowed.parameter))
        assertSame(borrowed.default, input.declaration(borrowed.parameter).defaultValue)
        assertSame(savedType, input.type(savedType, borrowed.module.session).identity)
        assertSame(borrowed.module.session, input.type(savedType, borrowed.module.session).domain)
        val analysis = CfirExportAnalysis.analyze(
            files, roots, members, canonicalSources = listOf(finalGraph), additionalTypeInputs = savedTypes,
            additionalDeclarationInputs = additionalDeclarations, preparedInput = prepared,
        )
        assertSame(finalGraph, analysis.canonicalSourceGraphs().single())
        assertSame(borrowed.parameter, analysis.valueParametersOf(borrowed.function).single())
        assertFalse(analysis.hasCanonicalSourceOf(borrowed.function))
        assertNull(borrowed.function.body)
        // 相同 records/owner 不能冒充最终 graph 域，也不能借用另一个 session 的已知类型。
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysisInputSnapshot.capture(
                files, roots, members, listOf(originalGraph), savedTypes, additionalDeclarations, prepared,
            )
        }
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysis.analyze(
                files, roots, members, canonicalSources = listOf(finalGraph),
                additionalTypeInputs = mapOf(source.module.session to listOf(savedType)),
                additionalDeclarationInputs = additionalDeclarations, preparedInput = prepared,
            )
        }
        assertDoesNotThrow {
            input.validateUnchanged()
            analysis.validateSourceUnchanged()
        }
        assertSame(publication, borrowed.snapshot())
        assertEquals(2, borrowed.completions.size)
    }

    /** final Result 只能交付一次；拒绝后仍可只读消费原产品，不能通过新 complete 刷新基准。 */
    @Test
    fun frozenWorkRejectsFurtherCompletionAndASecondFreezeWithoutChangingItsDeliveredInput() {
        val borrowed = PublishedHeaderFixture("workOnce")
        val roots = listOf<CfirDeclaration>(borrowed.function)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { emptyList() }
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), roots, members)
        work.complete()
        val prepared = work.freeze()
        val publication = borrowed.snapshot()
        val completions = borrowed.completions.toList()
        val input = CfirExportAnalysisInputSnapshot.capture(emptyList(), roots, members, preparedInput = prepared)
        assertThrows(IllegalStateException::class.java) { work.complete() }
        assertThrows(IllegalStateException::class.java) { work.freeze() }
        assertSame(publication, borrowed.snapshot())
        assertEquals(completions.size, borrowed.completions.size)
        completions.forEachIndexed { index, declaration -> assertSame(declaration, borrowed.completions[index]) }
        assertSame(borrowed.default, input.declaration(borrowed.parameter).defaultValue)
        assertSame(borrowed.default, borrowed.parameter.defaultValue)
        assertDoesNotThrow { input.validateUnchanged() }
        CfirExportAnalysis.analyze(emptyList(), roots, members, preparedInput = prepared).validateSourceUnchanged()
        assertSame(publication, borrowed.snapshot())
    }

    /** final freeze 自己进入声明原 owner 的 fence；snapshot 回调没有任何外层 testguard。 */
    @Test
    fun finalWorkFreezeRejectsOriginalOwnerCompletionFromItsPublishedSourceCallback() {
        val borrowed = PublishedHeaderFixture("workFence")
        val roots = listOf<CfirDeclaration>(borrowed.function)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { emptyList() }
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), roots, members)
        work.complete()
        val publication = borrowed.snapshot()
        val completions = borrowed.completions.size
        var callbacks = 0
        borrowed.afterSnapshot = {
            callbacks++
            assertTrue(borrowed.isReading, "final freeze 必须先进入这个原 Published owner 的 fence")
            borrowed.completeHeader(borrowed.function)
        }
        assertThrows(IllegalStateException::class.java) { work.freeze() }
        assertEquals(1, callbacks)
        assertEquals(completions, borrowed.completions.size)
        assertFalse(borrowed.isReading)
        borrowed.afterSnapshot = null
        assertSame(publication, borrowed.snapshot())
        assertSame(borrowed.default, borrowed.parameter.defaultValue)
        // 即使移除攻击回调、原发布代完全未变，首次冻结失败也已关闭这个工作集。
        assertThrows(IllegalStateException::class.java) { work.complete() }
        assertThrows(IllegalStateException::class.java) { work.freeze() }
        assertSame(publication, borrowed.snapshot())
        assertEquals(completions, borrowed.completions.size)
        assertEquals(1, callbacks)
        assertFalse(borrowed.isReading)
    }

    /** owner fence 建立在 action 之前失败也耗尽首次冻结资格；换图/类型/声明不能重新准备。 */
    @Test
    fun failedWorkFenceEntryRejectsReplacementInputsAndASecondFreezeWithoutProducingNewContent() {
        val source = SourceFixture("workFenceEntry")
        val borrowed = PublishedHeaderFixture("workFenceEntryBorrowed")
        val files = listOf(source.file)
        val roots = listOf<CfirDeclaration>(source.function)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { emptyList() }
        val graph = completedUnitGraph(source)
        val savedType = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64))
        val work = CfirExportAnalysisInputPreparation.Work(files, roots, members)
        work.complete(listOf(graph), mapOf(borrowed.module.session to listOf(savedType)), listOf(borrowed.function))
        val declarations = work.declarations
        val revision = work.dependencyRevision
        val publication = borrowed.snapshot()
        val completions = borrowed.completions.toList()
        val returnRef = borrowed.function.returnTypeRef
        val parameterRef = borrowed.parameter.returnTypeRef
        val sourceReturnRef = source.function.returnTypeRef
        val sourceStatus = source.function.status
        val records = graph.expressions
        val bodies = graph.functionBodies
        val owners = graph.declarationOwners
        val typeElements = savedType.elementTypes
        val fenceFailure = IllegalStateException("Original Published owner failed before entering its read-only action")
        var fenceEntries = 0
        var snapshotCallbacks = 0
        borrowed.beforeReadOnly = {
            fenceEntries++
            assertFalse(borrowed.isReading)
            throw fenceFailure
        }
        borrowed.afterSnapshot = { snapshotCallbacks++ }
        var delivered: CfirExportAnalysisInputPreparation.Result? = null
        assertSame(fenceFailure, assertThrows(IllegalStateException::class.java) { delivered = work.freeze() })
        assertNull(delivered)
        assertEquals(1, fenceEntries)
        assertEquals(0, snapshotCallbacks, "fence 建立失败必须发生在任何 signature/type 冻结读取之前")
        assertFalse(borrowed.isReading)

        // 原 owner 恢复后可独立建立 fence；已失败 Work 仍必须在调用它之前拒绝再次交付。
        borrowed.beforeReadOnly = null
        assertDoesNotThrow { borrowed.withReadOnlyContent { assertTrue(borrowed.isReading) } }
        borrowed.beforeReadOnly = { fenceEntries++ }
        val replacementGraph = graph.withOriginAliases(mapOf(source.body to bodies.getValue(source.function)))
        val replacement = PublishedHeaderFixture("workFenceEntryReplacement")
        val replacementPublication = replacement.snapshot()
        val replacementType = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.UNIT))
        assertNotSame(graph, replacementGraph)
        assertNotSame(savedType, replacementType)
        assertThrows(IllegalStateException::class.java) {
            work.complete(
                listOf(replacementGraph), mapOf(replacement.module.session to listOf(replacementType)),
                listOf(replacement.function),
            )
        }
        assertThrows(IllegalStateException::class.java) { delivered = work.freeze() }
        assertNull(delivered)
        assertEquals(1, fenceEntries, "再次 freeze 必须在重入原 owner fence 之前拒绝")
        assertEquals(0, snapshotCallbacks)
        borrowed.beforeReadOnly = null
        borrowed.afterSnapshot = null

        assertEquals(revision, work.dependencyRevision)
        val remainingDeclarations = work.declarations
        assertEquals(declarations.size, remainingDeclarations.size)
        declarations.forEachIndexed { index, declaration ->
            assertSame(declaration, remainingDeclarations[index])
            assertSame(declaration, declaration.symbol.cfir)
        }
        assertFalse(remainingDeclarations.any { it === replacement.function || it === replacement.parameter })
        assertSame(publication, borrowed.snapshot())
        assertEquals(completions.size, borrowed.completions.size)
        completions.forEachIndexed { index, declaration -> assertSame(declaration, borrowed.completions[index]) }
        assertSame(borrowed, borrowed.function.contentSource)
        assertSame(borrowed, borrowed.parameter.contentSource)
        assertSame(returnRef, borrowed.function.returnTypeRef)
        assertSame(parameterRef, borrowed.parameter.returnTypeRef)
        assertSame(borrowed.default, borrowed.parameter.defaultValue)
        assertSame(sourceReturnRef, source.function.returnTypeRef)
        assertSame(sourceStatus, source.function.status)
        assertSame(source.body, source.function.body)
        assertSame(records, graph.expressions)
        assertSame(bodies, graph.functionBodies)
        assertSame(owners, graph.declarationOwners)
        assertSame(typeElements, savedType.elementTypes)
        assertFalse(graph.containsOrigin(source.body))
        assertTrue(replacement.completions.isEmpty())
        assertSame(replacementPublication, replacement.snapshot())
        assertSame(CfirPublishedAnnotations.Unselected, replacementPublication.annotations(replacement.function))
        assertSame(CfirPublishedAnnotations.Unselected, replacementPublication.annotations(replacement.parameter))
        assertNull(replacement.parameter.defaultValue)
        assertFalse(borrowed.isReading)
    }

    /** 原 source 的实际读取失败使 complete 中止；回调恢复不能重新生产或交付这个失败工作集。 */
    @Test
    fun failedWorkCompletionRejectsFurtherCompletionAndFreezeAfterItsSourceCallbackRecovers() {
        val borrowed = PublishedHeaderFixture("workAborted")
        val roots = listOf<CfirDeclaration>(borrowed.function)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { emptyList() }
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), roots, members)
        val publication = borrowed.snapshot()
        val sourceFailure = IllegalStateException("Published source callback failed during header preparation")
        var callbacks = 0
        borrowed.afterSnapshot = {
            callbacks++
            assertFalse(borrowed.isReading, "complete 是真实生产阶段，不能伪装为冻结消费")
            throw sourceFailure
        }
        assertSame(sourceFailure, assertThrows(IllegalStateException::class.java) { work.complete() })
        assertEquals(1, callbacks)
        assertTrue(borrowed.completions.isEmpty())
        borrowed.afterSnapshot = null
        assertSame(publication, borrowed.snapshot())
        assertSame(CfirPublishedAnnotations.Unselected, publication.annotations(borrowed.function))
        assertSame(CfirPublishedAnnotations.Unselected, publication.annotations(borrowed.parameter))
        assertNull(borrowed.parameter.defaultValue)
        assertThrows(IllegalStateException::class.java) { work.complete() }
        assertThrows(IllegalStateException::class.java) { work.freeze() }
        assertSame(publication, borrowed.snapshot())
        assertTrue(borrowed.completions.isEmpty())
        assertEquals(1, callbacks)
        assertFalse(borrowed.isReading)
    }

    /** 没有 complete 的 Work 不具备交付资格；freeze 不得顺便完成真实未选 header/default。 */
    @Test
    fun workRejectsFreezeBeforeAnyCompletionWithoutProducingHeaderContent() {
        val borrowed = PublishedHeaderFixture("workPending")
        val roots = listOf<CfirDeclaration>(borrowed.function)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { emptyList() }
        val work = CfirExportAnalysisInputPreparation.Work(emptyList(), roots, members)
        val publication = borrowed.snapshot()
        var callbacks = 0
        borrowed.afterSnapshot = { callbacks++ }
        assertThrows(IllegalStateException::class.java) { work.freeze() }
        assertTrue(work.declarations.isEmpty())
        assertTrue(borrowed.completions.isEmpty())
        assertEquals(0, callbacks)
        borrowed.afterSnapshot = null
        assertSame(publication, borrowed.snapshot())
        assertSame(CfirPublishedAnnotations.Unselected, publication.annotations(borrowed.function))
        assertSame(CfirPublishedAnnotations.Unselected, publication.annotations(borrowed.parameter))
        assertNull(borrowed.parameter.defaultValue)
        assertFalse(borrowed.isReading)
    }

    /**
     * 同一已求值语义叶可在多个读取语境复用；节点去重不能吞掉第二个真实 session 的类型域。
     * 两个正文、函数和文件各自拥有，共享的仅为无声明、无绑定的 Int64 常量产品。
     */
    @Test
    fun preparationCollectsSharedConstantExpressionInEverySourceSessionDomain() {
        val value = CfirConstantValue.Primitive(
            ConePrimitiveType.INT64, CfirLiteralKind.INT, java.math.BigInteger.valueOf(41L),
        )
        val shared = CfirConstantValueExpression(TestSource("computed:41"), value)
        val first = SourceFixture("first", shared)
        val second = SourceFixture("second", shared)
        val headerOnly = SourceFixture("headerOnly")
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { emptyList() }

        assertNotSame(first.module, second.module)
        assertNotSame(first.module.session, second.module.session)
        assertNotSame(first.file, second.file)
        assertNotSame(first.function, second.function)
        assertNotSame(first.body, second.body)
        assertSame(shared, first.body.statements.first())
        assertSame(shared, second.body.statements.first())
        // 正控有同样的无参 Unit header 和 Unit 正文尾值，不含 Int64 来源边。
        val control = CfirExportAnalysisInputPreparation.prepare(
            listOf(headerOnly.file), listOf(headerOnly.function), members,
        )
        assertFalse(control.containsType(value.type, headerOnly.module.session))

        listOf(listOf(first, second), listOf(second, first)).forEach { fixtures ->
            val files = fixtures.map { it.file }
            val roots = fixtures.map { it.function }
            val statuses = fixtures.map { it.function.status }
            val returnRefs = fixtures.map { it.function.returnTypeRef }
            val source = shared.source
            val prepared = CfirExportAnalysisInputPreparation.prepare(files, roots, members)
            fixtures.forEach { fixture ->
                assertTrue(
                    prepared.containsType(value.type, fixture.module.session),
                    fixture.file.name + " 的实际 session 域必须准备共享常量",
                )
            }
            val snapshot = CfirExportAnalysisInputSnapshot.capture(
                files, roots, members, preparedInput = prepared,
            )
            val sourceElement = snapshot.element(shared)
            assertSame(shared, sourceElement.identity)
            assertSame(value.type, sourceElement.type)
            val graph = (sourceElement.payload as CfirExportSourcePayload.Constant).value
            val constant = graph.nodes.single() as CfirExportConstantNode.Primitive
            assertSame(value.type, constant.type)
            assertEquals(CfirLiteralKind.INT, constant.kind)
            assertEquals(java.math.BigInteger.valueOf(41L), constant.value)
            fixtures.forEachIndexed { index, fixture ->
                val type = snapshot.type(value.type, fixture.module.session)
                assertSame(value.type, type.identity)
                assertSame(fixture.module.session, type.domain)
                assertSame(fixture.file, snapshot.sourceFiles.getValue(fixture.function))
                assertNull(snapshot.owners.getValue(fixture.function))
                assertSame(fixture.body, snapshot.declaration(fixture.function).body)
                var visits = 0
                snapshot.walk(fixture.body, fixture.function) { element, context ->
                    if (element.identity === shared) {
                        visits++
                        assertSame(fixture.function, context)
                        assertSame(value.type, element.type)
                    }
                }
                assertEquals(1, visits)
                assertSame(fixture.module, fixture.function.moduleData)
                assertSame(fixture.function, fixture.function.symbol.cfir)
                assertSame(fixture.function, fixture.file.declarations.single())
                assertSame(statuses[index], fixture.function.status)
                assertSame(returnRefs[index], fixture.function.returnTypeRef)
                assertSame(ConePrimitiveType.UNIT, (fixture.function.returnTypeRef as CfirResolvedTypeRef).coneType)
                assertTrue(fixture.function.valueParameters.isEmpty())
                assertTrue(fixture.function.typeParameters.isEmpty())
                assertNull(fixture.function.dispatchReceiverType)
                assertSame(ConePrimitiveType.UNIT, fixture.body.coneTypeOrNull)
                assertSame(ConePrimitiveType.UNIT, (fixture.body.statements.last() as CfirExpression).coneTypeOrNull)
                listOf(fixture.file, fixture.function).forEach { declaration ->
                    assertEquals(CfirResolvePhase.BODY_RESOLVE, declaration.resolvePhase)
                    assertSame(CfirDeclarationOrigin.Source, declaration.origin)
                    assertNull(declaration.cjoDeclarationOrigin)
                }
            }
            assertSame(value, shared.constantValue)
            assertSame(source, shared.source)
            assertDoesNotThrow { repeat(2) { snapshot.validateUnchanged() } }
        }
    }

    /** 原 Cone 未变，只换同一函数的 returnRef；旧 Result 不得把旧 type 与新 header 混为一代。 */
    @Test
    fun preparedResultRejectsReplacedReturnTypeReferenceBeforeFirstCapture() {
        val fixture = Fixture()
        val files = listOf(fixture.file)
        val roots = listOf<CfirDeclaration>(fixture.owner)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { (it as CfirClass).declarations }
        val prepared = CfirExportAnalysisInputPreparation.prepare(files, roots, members)
        val original = fixture.function.returnTypeRef as CfirResolvedTypeRef
        val replacement = buildResolvedTypeRef { coneType = original.coneType }
        assertNotSame(original, replacement)
        assertSame(original.coneType, replacement.coneType)
        fixture.function.replaceReturnTypeRef(replacement)
        // 首次 capture 就要拒绝；不能靠已有 snapshot 的 validate 或未知 Cone key 偶然失败。
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysisInputSnapshot.capture(files, roots, members, preparedInput = prepared)
        }
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysis.analyze(files, roots, members, preparedInput = prepared)
        }
        assertSame(replacement, fixture.function.returnTypeRef)
    }

    /** 同一参数/函数身份且有序参数向量未变，parameter typeRef 换代也必须使旧 Result 失效。 */
    @Test
    fun preparedResultRejectsReplacedParameterTypeReferenceBeforeFirstCapture() {
        val fixture = Fixture(withDefaultHelper = true)
        val files = listOf(fixture.file)
        val roots = listOf<CfirDeclaration>(fixture.owner)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { (it as CfirClass).declarations }
        val parameter = checkNotNull(fixture.defaults).parameter
        val prepared = CfirExportAnalysisInputPreparation.prepare(files, roots, members)
        val original = parameter.returnTypeRef as CfirResolvedTypeRef
        val replacement = buildResolvedTypeRef { coneType = original.coneType }
        parameter.replaceReturnTypeRef(replacement)
        assertSame(parameter, fixture.function.valueParameters.single())
        assertNotSame(original, replacement)
        assertSame(original.coneType, replacement.coneType)
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysisInputSnapshot.capture(files, roots, members, preparedInput = prepared)
        }
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysis.analyze(files, roots, members, preparedInput = prepared)
        }
        assertSame(replacement, parameter.returnTypeRef)
    }

    /** 不同 lambda 提供同一有序成员身份是合法同域；不同已知 roots 仍不能复用旧 Result。 */
    @Test
    fun preparedResultAcceptsEquivalentMemberProjectionAndRejectsDifferentRoots() {
        val fixture = Fixture()
        val files = listOf(fixture.file)
        val roots = listOf<CfirDeclaration>(fixture.owner)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { (it as CfirClass).declarations }
        val equivalent: (CfirDeclaration) -> List<CfirDeclaration> = { (it as CfirClass).declarations.toList() }
        assertNotSame(members, equivalent)
        val prepared = CfirExportAnalysisInputPreparation.prepare(files, roots, members)
        assertTrue(prepared.declarations.any { it === fixture.function })
        CfirExportAnalysisInputSnapshot.capture(files, roots, equivalent, preparedInput = prepared).validateUnchanged()
        CfirExportAnalysis.analyze(files, roots, equivalent, preparedInput = prepared).validateSourceUnchanged()
        val otherRoots = listOf<CfirDeclaration>(fixture.function)
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysisInputSnapshot.capture(files, otherRoots, equivalent, preparedInput = prepared)
        }
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysis.analyze(files, otherRoots, equivalent, preparedInput = prepared)
        }
    }

    /** 同一 members closure 返回新投影：物理成员及所有 Cone 仍已知，不能只用 key 存在性放行。 */
    @Test
    fun preparedResultRejectsChangedMemberProjectionOnTheSameCallback() {
        val fixture = Fixture()
        val files = listOf(fixture.file)
        val roots = listOf<CfirDeclaration>(fixture.owner)
        var projection = listOf<CfirDeclaration>(fixture.function)
        val members: (CfirDeclaration) -> List<CfirDeclaration> = { owner ->
            check(owner === fixture.owner)
            projection
        }
        val prepared = CfirExportAnalysisInputPreparation.prepare(files, roots, members)
        CfirExportAnalysisInputSnapshot.capture(files, roots, members, preparedInput = prepared).validateUnchanged()
        projection = emptyList()
        assertSame(fixture.function, fixture.owner.declarations.single())
        assertTrue(prepared.declarations.any { it === fixture.function })
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysisInputSnapshot.capture(files, roots, members, preparedInput = prepared)
        }
        assertThrows(IllegalStateException::class.java) {
            CfirExportAnalysis.analyze(files, roots, members, preparedInput = prepared)
        }
        assertTrue(projection.isEmpty())
    }

    /** 输入换代包含实际Type节点来源，不能因Cone和source身份未变而接受重分类。 */
    @Test
    fun changingTypeReferenceOriginInvalidatesTheCapturedInput() {
        val fixture = Fixture()
        val reference = fixture.function.returnTypeRef
        val input = fixture.capture()
        val original = input.element(reference).payload as CfirExportSourcePayload.ResolvedType
        assertEquals(org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC, original.origin)
        reference.replaceTypeUseOrigin(org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SOURCE)
        assertEquals(org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC, original.origin)
        assertThrows(IllegalStateException::class.java) { input.validateUnchanged() }
    }

    /** 相等primitive仍有不同输入身份；重复参数、ABI与closure/variadic分别保存。 */
    @Test
    fun frozenFunctionTypeRetainsOrderedRepeatedParametersAndRejectsEqualReplacement() {
        val fixture = Fixture()
        val first = ConePrimitiveType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64)
        val second = ConePrimitiveType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64)
        val parameters = mutableListOf<org.cangnova.cangjie.cfir.types.ConeCangJieType>(first, second, first)
        val type = org.cangnova.cangjie.cfir.types.ConeFunctionType(parameters, first,
            isCFunc = true, isClosureType = false, hasVariableLenArg = true)
        fixture.function.replaceReturnTypeRef(buildResolvedTypeRef { coneType = type })
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val captured = analysis.typeElement(type, fixture.function).payload as CfirExportTypePayload.Function
        assertEquals(3, captured.parameters.size)
        assertSame(first, captured.parameters[0])
        assertSame(second, captured.parameters[1])
        assertSame(first, captured.parameters[2])
        assertTrue(captured.isC)
        assertTrue(captured.hasVariableLenArg)
        assertFalse(captured.isClosure)
        parameters[0] = ConePrimitiveType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64)
        assertSame(first, captured.parameters[0])
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    @Test
    fun frozenTupleTypeRetainsRepeatedIdentitySlotsAndRejectsReordering() {
        val fixture = Fixture()
        val first = ConePrimitiveType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64)
        val second = ConePrimitiveType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64)
        val elements = mutableListOf<org.cangnova.cangjie.cfir.types.ConeCangJieType>(first, second, first)
        val type = ConeTupleType(elements)
        fixture.function.replaceReturnTypeRef(buildResolvedTypeRef { coneType = type })
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val captured = analysis.typeElement(type, fixture.function).payload as CfirExportTypePayload.Tuple
        assertSame(first, captured.elements[0])
        assertSame(second, captured.elements[1])
        assertSame(first, captured.elements[2])
        elements[0] = second
        elements[1] = first
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
        assertSame(first, captured.elements[0])
    }

    @Test
    fun frozenThisTypeRetainsClassifierBindingAndIndependentThisFlag() {
        val fixture = Fixture()
        val type = ConeClassLikeType(fixture.owner.symbol.toLookupTag(), isThisType = true)
        fixture.function.replaceReturnTypeRef(buildResolvedTypeRef { coneType = type })
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val captured = analysis.typeElement(type, fixture.function).payload as CfirExportTypePayload.Nominal
        assertEquals(CfirExportNominalTypeKind.CLASS, captured.kind)
        assertSame(fixture.owner, captured.declaration)
        assertTrue(captured.isThisType)
        assertTrue(captured.arguments.isEmpty())
        analysis.validateSourceUnchanged()
    }

    /** 手写 receiver 也遵循共同来源协议；同一 this reference 内的重绑必须进入快照校验。 */
    @Test
    fun thisReceiverReferenceIsCapturedAndSourceUnboundStateIsRejected() {
        val fixture = Fixture()
        val reference = buildThisReference {
            source = fixture.function.source
            boundSymbol = fixture.owner.symbol
            isImplicit = false
            diagnostic = null
        }
        val receiver = CfirThisReceiverExpression(
            source = reference.source, annotations = emptyList(), coneTypeOrNull = fixture.owner.symbol.constructType(),
            calleeReference = reference,
        )
        checkNotNull(fixture.function.body).replaceStatements(listOf(receiver, fixture.literal(41L)))
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val captured = analysis.sourceElement(reference).payload as CfirExportSourcePayload.ThisReference
        assertSame(fixture.owner, captured.declaration)
        val other = buildClass {
            moduleData = fixture.owner.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            scopeProvider = fixture.owner.scopeProvider
            status = fixture.owner.status
            symbol = CfirClassSymbol(ClassId(FqName("snapshot"), Name.identifier("Other")))
            name = symbol.classId.shortClassName
        }
        reference.replaceBoundSymbol(other.symbol)
        assertSame(fixture.owner, captured.declaration)
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
        reference.replaceBoundSymbol(CfirClassSymbol(ClassId(FqName("snapshot"), Name.identifier("Unbound"))))
        assertThrows(IllegalStateException::class.java) { CfirExportAnalysis.analyze(listOf(fixture.file)) }
    }

    /** 控制目标不是拥有子边；同一 target 对象重绑也必须使本代冻结产品失效。 */
    @Test
    fun frozenReturnBindingSurvivesTargetRebindingAndRejectsTheChangedGraph() {
        val fixture = Fixture(withDefaultHelper = true)
        val target = CfirFunctionTarget(isLambda = false).also { it.bind(fixture.function) }
        val result = fixture.literal(41L)
        val expression = buildReturnExpression {
            source = result.source
            coneTypeOrNull = ConePrimitiveType.NOTHING
            this.target = target
            this.result = result
        }
        val body = checkNotNull(fixture.function.body)
        body.replaceStatements(listOf(expression))
        body.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val payload = analysis.sourceElement(expression).payload as CfirExportSourcePayload.Return
        assertSame(fixture.function, payload.function)
        assertSame(result, payload.result)
        val statements = (analysis.sourceElement(body).payload as CfirExportSourcePayload.Block).statements
        assertEquals(listOf(CfirExportSourceStatement.Expression(expression)), statements)
        target.bind(checkNotNull(fixture.defaults).helper)
        assertSame(fixture.function, payload.function)
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** metadata 的 header 登记与 SOURCE 保存读取同一参数向量，活动签名换代不能覆盖已发布的列表。 */
    @Test
    fun publishedParameterVectorSurvivesLiveSignatureReplacement() {
        val fixture = Fixture(withDefaultHelper = true)
        val parameter = requireNotNull(fixture.defaults).parameter
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val parameters = analysis.valueParametersOf(fixture.function)
        assertSame(parameter, parameters.single())
        fixture.function.replaceValueParameters(emptyList())
        assertSame(parameters, analysis.valueParametersOf(fixture.function))
        assertSame(parameter, parameters.single())
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** const VarDecl 的初值只存在于简单模式容器时，资格与公开根查询仍读取同一冻结投影。 */
    @Test
    fun simpleConstBindingUsesCapturedContainerInitializer() {
        val fixture = Fixture(withSimpleBinding = true)
        val binding = requireNotNull(fixture.simpleBinding)
        assertNull(binding.variable.initializer)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val pattern = analysis.patternOf(binding.container)
        val bindings = analysis.patternBindingsOf(binding.container)
        assertSame(binding.variable, bindings.single())
        assertSame(binding.container, analysis.simplePatternOwnerOf(binding.variable))
        assertTrue(analysis.isSourceDeclaration(binding.variable))
        assertSame(binding.initializer, analysis.initializerOf(binding.variable))
        assertTrue(analysis.shouldExportSource(binding.variable))
        assertSame(binding.initializer, analysis.sourceElement(binding.initializer).identity)
        assertNull(analysis.ownerOf(binding.variable))

        binding.container.replaceInitializer(fixture.literal(48L))
        assertSame(pattern, analysis.patternOf(binding.container))
        assertSame(bindings, analysis.patternBindingsOf(binding.container))
        assertSame(binding.container, analysis.simplePatternOwnerOf(binding.variable))
        assertSame(binding.initializer, analysis.initializerOf(binding.variable))
        assertTrue(analysis.shouldExportSource(binding.variable))
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** 嵌套 tuple 的真实 binding 按模式顺序冻结，交换原节点不能改变消费者已持有的向量。 */
    @Test
    fun publishedComplexPatternBindingsSurviveNestedPatternReordering() {
        val fixture = Fixture(withComplexBinding = true)
        val complex = requireNotNull(fixture.complexBinding)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val bindings = analysis.patternBindingsOf(complex.container)
        assertEquals(listOf("zeta", "alpha", "middle"), bindings.map { it.name.asString() })
        complex.bindings.forEachIndexed { index, binding -> assertSame(binding, bindings[index]) }
        assertSame(complex.pattern, analysis.patternOf(complex.container))
        assertDoesNotThrow { analysis.validateSourceUnchanged() }

        val nested = complex.pattern.elements[1] as CfirTuplePattern
        val elements = nested.elements as MutableList<CfirPattern>
        val first = elements[0]
        elements[0] = elements[1]
        elements[1] = first

        assertSame(bindings, analysis.patternBindingsOf(complex.container))
        complex.bindings.forEachIndexed { index, binding -> assertSame(binding, bindings[index]) }
        val visited = mutableListOf<CfirPatternBindingVariable>()
        analysis.walkSource(complex.pattern, complex.container) { element, _ ->
            (element.identity as? CfirPatternBindingVariable)?.let(visited::add)
        }
        assertEquals(complex.bindings, visited)
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** 根的源码、元素身份和顺序全部相同，新 tuple 根仍属于另一代；查询及遍历保留旧根。 */
    @Test
    fun publishedPatternRootSurvivesEquivalentReplacement() {
        val fixture = Fixture(withComplexBinding = true)
        val complex = requireNotNull(fixture.complexBinding)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val bindings = analysis.patternBindingsOf(complex.container)
        val replacement = buildTuplePatternCopy(complex.pattern)
        assertNotSame(complex.pattern, replacement)
        assertSame(complex.pattern.source, replacement.source)
        assertEquals(complex.pattern.elements, replacement.elements)

        complex.container.transformPattern(object : CfirTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E = element
            override fun transformTuplePattern(tuplePattern: CfirTuplePattern, data: Unit): CfirPattern = replacement
        }, Unit)

        assertSame(replacement, complex.container.pattern)
        assertSame(complex.pattern, analysis.patternOf(complex.container))
        assertSame(bindings, analysis.patternBindingsOf(complex.container))
        val visited = mutableListOf<CfirElement>()
        analysis.walkSource(complex.container, fixture.function) { element, _ -> visited += element.identity }
        assertTrue(visited.any { it === complex.pattern })
        assertFalse(visited.any { it === replacement })
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** 官方 VarWithPattern 保留 initializer；VarPattern 的真实声明不复制或别名引用 shell 初值。 */
    @Test
    fun complexPatternBindingsDoNotAliasShellInitializer() {
        val fixture = Fixture(withComplexBinding = true, withSimpleBinding = true)
        val complex = requireNotNull(fixture.complexBinding)
        val simple = requireNotNull(fixture.simpleBinding)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        assertTrue(analysis.isSourceDeclaration(complex.container))
        assertSame(complex.initializer, complex.container.initializer)
        assertSame(complex.initializer, analysis.initializerOf(complex.container))
        assertSame(complex.initializer, analysis.sourceElement(complex.initializer).identity)
        complex.bindings.forEach { binding ->
            assertTrue(analysis.isSourceDeclaration(binding))
            assertNull(binding.initializer)
            assertNull(analysis.initializerOf(binding))
            assertNull(analysis.simplePatternOwnerOf(binding))
            assertFalse(analysis.shouldExportSource(binding))
        }
        assertSame(simple.container, analysis.simplePatternOwnerOf(simple.variable))
        assertSame(simple.initializer, analysis.initializerOf(simple.variable))
        assertTrue(analysis.shouldExportSource(simple.variable))
        assertDoesNotThrow { analysis.validateSourceUnchanged() }

        complex.container.replaceInitializer(
            fixture.tuple(fixture.literal(90L), fixture.tuple(fixture.literal(91L), fixture.literal(92L))),
        )

        assertSame(complex.initializer, analysis.initializerOf(complex.container))
        complex.bindings.forEach { binding -> assertNull(analysis.initializerOf(binding)) }
        assertSame(simple.initializer, analysis.initializerOf(simple.variable))
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** SOURCE 身份与内容根属于捕获代；活动声明新增 CJO 身份后，旧域查询仍可读取简单别名。 */
    @Test
    fun publishedSourceDomainSurvivesCjoOriginAttachment() {
        val fixture = Fixture(withSimpleBinding = true)
        val binding = requireNotNull(fixture.simpleBinding)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        assertTrue(analysis.isSourceDeclaration(binding.variable))
        assertNull(binding.variable.cjoDeclarationOrigin)
        assertDoesNotThrow { analysis.validateSourceUnchanged() }

        binding.variable.cjoDeclarationOrigin = CfirCjoDeclarationOrigin("replacement.cjo", 1, isTopLevel = false)

        assertTrue(analysis.isSourceDeclaration(binding.variable))
        assertTrue(analysis.shouldExportSource(binding.variable))
        assertSame(binding.container, analysis.simplePatternOwnerOf(binding.variable))
        assertSame(binding.initializer, analysis.initializerOf(binding.variable))
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** 声明 origin 为只读字段；用真实声明副本替换模式边，验证旧 SOURCE 域不会重新绑定库声明。 */
    @Test
    fun publishedSourceDomainSurvivesLibraryDeclarationReplacement() {
        val fixture = Fixture(withSimpleBinding = true)
        val binding = requireNotNull(fixture.simpleBinding)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val pattern = analysis.patternOf(binding.container)
        val bindings = analysis.patternBindingsOf(binding.container)
        val replacement = buildPatternBindingVariableCopy(binding.variable) {
            origin = CfirDeclarationOrigin.Library
            symbol = CfirPatternBindingSymbol(binding.variable.symbol.callableId)
        }
        replacement.cjoDeclarationOrigin = CfirCjoDeclarationOrigin("replacement.cjo", 1, isTopLevel = false)
        val replacementPattern = buildBindingPattern {
            source = binding.variable.source
            name = replacement.name
            bindingVariable = replacement
        }
        binding.container.transformPattern(object : CfirTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E = element
            override fun transformBindingPattern(bindingPattern: CfirBindingPattern, data: Unit): CfirPattern = replacementPattern
        }, Unit)

        assertSame(replacementPattern, binding.container.pattern)
        assertSame(CfirDeclarationOrigin.Library, replacement.origin)
        assertFalse(analysis.containsDeclaration(replacement))
        assertTrue(analysis.isSourceDeclaration(binding.variable))
        assertTrue(analysis.shouldExportSource(binding.variable))
        assertSame(pattern, analysis.patternOf(binding.container))
        assertSame(bindings, analysis.patternBindingsOf(binding.container))
        assertSame(binding.variable, bindings.single())
        assertSame(binding.container, analysis.simplePatternOwnerOf(binding.variable))
        assertSame(binding.initializer, analysis.initializerOf(binding.variable))
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** 官方 SDK detailMessage 的原始 CJO IMPORTED=false、initializer=0，不能据此进入 SOURCE 查询域。 */
    @Test
    fun cjoVariableWithOriginalImportedBitFalseRejectsSourceInitializer() {
        val fixture = Fixture(withCjoVariable = true)
        val variable = requireNotNull(fixture.cjoVariable)
        assertSame(CfirDeclarationOrigin.Library, variable.origin)
        assertEquals(2288, requireNotNull(variable.cjoDeclarationOrigin).declarationIndex)
        assertFalse(variable.exportFacts.isImported)
        assertNull(variable.initializer)

        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file), roots = listOf(fixture.owner, variable))

        assertTrue(analysis.containsDeclaration(variable))
        assertNull(analysis.sourceFileOf(variable))
        assertFalse(analysis.precedingFactsOf(variable).isImported)
        assertFalse(analysis.isSourceDeclaration(variable))
        assertFalse(analysis.shouldExportSource(variable))
        assertThrows(IllegalStateException::class.java) { analysis.initializerOf(variable) }
        assertDoesNotThrow { repeat(2) { analysis.validateSourceUnchanged() } }
    }

    /** 库来源的路径与索引换代，旧查询域仍严格拒绝 SOURCE 初值；校验必须识别 wire 身份变化。 */
    @Test
    fun publishedCjoDomainSurvivesWireOriginReplacement() {
        val fixture = Fixture(withCjoVariable = true)
        val variable = requireNotNull(fixture.cjoVariable)
        val original = requireNotNull(variable.cjoDeclarationOrigin)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file), roots = listOf(fixture.owner, variable))
        assertDoesNotThrow { analysis.validateSourceUnchanged() }

        variable.cjoDeclarationOrigin = original.copy(packagePath = "next-core.cjo", declarationIndex = 2289)

        assertFalse(analysis.isSourceDeclaration(variable))
        assertFalse(analysis.precedingFactsOf(variable).isImported)
        assertFalse(analysis.shouldExportSource(variable))
        assertThrows(IllegalStateException::class.java) { analysis.initializerOf(variable) }
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** 活动库声明移除 wire 来源也不能使已发布的非 SOURCE 域变为可查询空初值的 SOURCE 域。 */
    @Test
    fun publishedCjoDomainSurvivesWireOriginRemoval() {
        val fixture = Fixture(withCjoVariable = true)
        val variable = requireNotNull(fixture.cjoVariable)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file), roots = listOf(fixture.owner, variable))
        assertDoesNotThrow { analysis.validateSourceUnchanged() }

        variable.cjoDeclarationOrigin = null

        assertFalse(analysis.isSourceDeclaration(variable))
        assertFalse(analysis.shouldExportSource(variable))
        assertThrows(IllegalStateException::class.java) { analysis.initializerOf(variable) }
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** writer frozen 覆盖不能回写 raw frozen，发布后的资格查询也不能重新读取活动互操作记录。 */
    @Test
    fun writerFrozenOverridePreservesPublishedSemanticEligibility() {
        val fixture = Fixture()
        fixture.function.interopInfo = CfirInteropInfo.NONE.copy(isFrozen = true)
        val analysis = CfirExportAnalysis.analyze(
            listOf(fixture.file), settings = CfirExportAnalysisSettings(canInline = false),
        )
        assertFalse(analysis.precedingFactsOf(fixture.function).isInline)
        assertTrue(analysis.isFrozenOf(fixture.function))
        assertTrue(analysis.canBeSourceExported(fixture.function))
        assertFalse(analysis.sourceFunctionBodyIsEligible(fixture.function, false))
        assertTrue(analysis.sourceFunctionBodyIsEligible(fixture.function, true))

        fixture.function.interopInfo = CfirInteropInfo.NONE
        assertTrue(analysis.isFrozenOf(fixture.function))
        assertTrue(analysis.canBeSourceExported(fixture.function))
        assertFalse(analysis.sourceFunctionBodyIsEligible(fixture.function, false))
        assertTrue(analysis.sourceFunctionBodyIsEligible(fixture.function, true))
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** desugar 写出与查询共用冻结 helper 身份，活动参数换代不能重新绑定已选择的辅助声明。 */
    @Test
    fun publishedDefaultHelperIdentitySurvivesLiveMetadataReplacement() {
        val fixture = Fixture(withDefaultHelper = true)
        val defaults = requireNotNull(fixture.defaults)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val metadata = requireNotNull(defaults.parameter.defaultValueMetadata)
        val replacement = fixture.namedFunction("replacementDefault", fixture.block(fixture.literal(99L)))
        assertSame(defaults.helper, analysis.defaultHelperOf(defaults.parameter))
        defaults.parameter.defaultValueMetadata = metadata.copy(function = replacement)
        assertSame(defaults.helper, analysis.defaultHelperOf(defaults.parameter))
        val visited = mutableListOf<CfirElement>()
        analysis.walkSource(fixture.function, fixture.function) { element, _ -> visited += element.identity }
        assertTrue(visited.any { it === defaults.helper })
        assertFalse(visited.any { it === replacement })
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** metadata 消费者通过公开分析结果读取冻结正文，不依赖 internal Capture 的具体实现。 */
    @Test
    fun publishedAnalysisWalkRetainsRemovedBodyNodeAndRejectsChangedGeneration() {
        val fixture = Fixture()
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        val body = requireNotNull(analysis.functionBodyOf(fixture.function))
        val originalChildren = analysis.sourceElement(body).children
        assertSame(fixture.body, body)
        assertEquals(fixture.literals, originalChildren)
        body.mutableStatements().removeAt(0)
        val visited = mutableListOf<CfirLiteralExpression>()
        analysis.walkSource(body, fixture.function) { element, owner ->
            if (element.identity is CfirLiteralExpression) {
                assertSame(fixture.function, owner)
                visited += element.identity
            }
        }
        assertEquals(fixture.literals, visited)
        assertSame(originalChildren, analysis.sourceElement(body).children)
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    /** 默认值槽本身换成同类型表达式时，不能读取活动参数的新默认值覆盖已选择的根。 */
    @Test
    fun publishedDefaultValueRootRemainsFrozenAndReplacementIsRejected() {
        val fixture = Fixture(withDefaultHelper = true)
        val parameter = requireNotNull(fixture.defaults).parameter
        val original = fixture.literal(40L)
        parameter.replaceDefaultValue(original)
        val analysis = CfirExportAnalysis.analyze(listOf(fixture.file))
        assertSame(original, analysis.defaultValueOf(parameter))
        assertSame(original, analysis.sourceElement(original).identity)
        parameter.replaceDefaultValue(fixture.literal(44L))
        assertSame(original, analysis.defaultValueOf(parameter))
        assertThrows(IllegalStateException::class.java) { analysis.validateSourceUnchanged() }
    }

    @Test
    fun unchangedResolvedOwnerFunctionAndBodyValidateRepeatedly() {
        val fixture = Fixture()
        val snapshot = fixture.capture()

        assertSame(fixture.owner, snapshot.roots.single())
        assertSame(fixture.owner, snapshot.owners.getValue(fixture.function))
        assertSame(fixture.file, snapshot.sourceFiles.getValue(fixture.function))
        assertSame(fixture.body, snapshot.declaration(fixture.function).body)
        assertFrozenLiterals(snapshot, fixture.owner, fixture.function, fixture.literals)
        snapshot.walk(fixture.owner, fixture.owner) { element, context ->
            if (element.identity is CfirLiteralExpression) assertSame(fixture.function, context)
        }
        assertDoesNotThrow { repeat(2) { snapshot.validateUnchanged() } }
    }

    /** compiler dummy 不维护逐声明 phase；输入完整性取决于真实 status、header 和正文解析结果。 */
    @Test
    fun completeResolvedSourceInputAtStatusPhaseIsAcceptedWithoutPromotingDeclarations() {
        val fixture = Fixture(phase = CfirResolvePhase.STATUS)
        val declarations = listOf(fixture.file, fixture.owner, fixture.function)
        declarations.forEach { declaration ->
            assertEquals(CfirResolvePhase.STATUS, declaration.resolvePhase)
            assertSame(CfirDeclarationOrigin.Source, declaration.origin)
        }
        assertTrue(fixture.owner.status is CfirResolvedDeclarationStatus)
        assertTrue(fixture.function.status is CfirResolvedDeclarationStatus)
        assertTrue(fixture.function.returnTypeRef is CfirResolvedTypeRef)
        assertSame(ConePrimitiveType.INT64, fixture.body.coneTypeOrNull)
        fixture.literals.forEach { literal -> assertSame(ConePrimitiveType.INT64, literal.coneTypeOrNull) }

        val snapshot = fixture.capture()

        assertFrozenLiterals(snapshot, fixture.owner, fixture.function, fixture.literals)
        assertDoesNotThrow { repeat(2) { snapshot.validateUnchanged() } }
        declarations.forEach { declaration -> assertEquals(CfirResolvePhase.STATUS, declaration.resolvePhase) }
    }

    /** 相同 STATUS 下只清除正文中间语句的类型，不能因声明 status/header 完整而接受不完整输入。 */
    @Test
    fun statusPhaseSourceInputWithUntypedBodyLiteralIsRejectedDuringCapture() {
        val fixture = Fixture(phase = CfirResolvePhase.STATUS)
        val literal = fixture.literals[1]
        literal.replaceConeTypeOrNull(null)

        listOf(fixture.file, fixture.owner, fixture.function).forEach { declaration ->
            assertEquals(CfirResolvePhase.STATUS, declaration.resolvePhase)
        }
        assertTrue(fixture.owner.status is CfirResolvedDeclarationStatus)
        assertTrue(fixture.function.status is CfirResolvedDeclarationStatus)
        assertTrue(fixture.function.returnTypeRef is CfirResolvedTypeRef)
        assertSame(fixture.body, fixture.function.body)
        assertSame(ConePrimitiveType.INT64, fixture.body.coneTypeOrNull)
        assertSame(literal, fixture.body.statements[1])
        assertNull(literal.coneTypeOrNull)

        assertThrows(IllegalStateException::class.java) { fixture.capture() }
    }

    /** 正文和 resolved status 均完整，函数头仍有隐式类型引用时必须拒绝捕获。 */
    @Test
    fun statusPhaseSourceInputWithImplicitReturnTypeRefIsRejectedDuringCapture() {
        val fixture = Fixture(phase = CfirResolvePhase.STATUS)
        val implicitReturnType = buildImplicitTypeRef()
        fixture.function.replaceReturnTypeRef(implicitReturnType)

        listOf(fixture.file, fixture.owner, fixture.function).forEach { declaration ->
            assertEquals(CfirResolvePhase.STATUS, declaration.resolvePhase)
        }
        assertTrue(fixture.owner.status is CfirResolvedDeclarationStatus)
        assertTrue(fixture.function.status is CfirResolvedDeclarationStatus)
        assertSame(implicitReturnType, fixture.function.returnTypeRef)
        assertSame(fixture.body, fixture.function.body)
        assertSame(ConePrimitiveType.INT64, fixture.body.coneTypeOrNull)
        fixture.literals.forEach { literal -> assertSame(ConePrimitiveType.INT64, literal.coneTypeOrNull) }

        assertThrows(IllegalStateException::class.java) { fixture.capture() }
    }

    /** 合并输出的成员表不能改写原物理词法归属。 */
    @Test
    fun mergedMemberRetainsItsPhysicalLexicalOwner() {
        val fixture = Fixture()
        val outputOwner = buildClass {
            moduleData = fixture.owner.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            scopeProvider = fixture.owner.scopeProvider
            status = fixture.owner.status
            symbol = CfirClassSymbol(ClassId(FqName("snapshot"), Name.identifier("Output")))
            name = symbol.classId.shortClassName
        }
        (fixture.file.declarations as MutableList<CfirDeclaration>).add(outputOwner)
        val input = CfirExportAnalysisInputSnapshot.capture(
            files = listOf(fixture.file),
            roots = listOf(fixture.owner, outputOwner),
            members = { if (it === outputOwner) listOf(fixture.function) else emptyList() },
        )

        assertSame(fixture.function, input.memberBodies.getValue(outputOwner).single())
        assertTrue(input.memberBodies.getValue(fixture.owner).isEmpty())
        assertSame(fixture.owner, input.owners.getValue(fixture.function))
        assertSame(fixture.file, input.sourceFiles.getValue(fixture.function))
        input.validateUnchanged()
    }

    /** 成员数和所有原有节点状态不变，仅替换物理成员身份也必须使输入代次失效。 */
    @Test
    fun replacingMemberInPlaceWithEquivalentResolvedFunctionIsRejected() {
        val fixture = Fixture()
        val snapshot = fixture.capture()
        val original = fixture.function
        val replacement = buildNamedFunctionCopy(original) {
            symbol = CfirNamedFunctionSymbol(CallableId(fixture.owner.symbol.classId, original.name))
        }
        assertNotSame(original, replacement)
        assertSame(original.source, replacement.source)
        assertSame(original.status, replacement.status)
        assertSame(original.returnTypeRef, replacement.returnTypeRef)
        assertSame(original.body, replacement.body)
        // 真实 class builder 的 backing list 只在测试中用于模拟原地替换。
        val members = fixture.owner.declarations as MutableList<CfirDeclaration>

        members[0] = replacement

        assertSame(members, fixture.owner.declarations)
        assertSame(replacement, members.single())
        assertSame(original, snapshot.memberBodies.getValue(fixture.owner).single())
        var retainedOriginal = false
        var sawReplacement = false
        snapshot.walk(fixture.owner, fixture.owner) { element, _ ->
            if (element.identity === original) retainedOriginal = true
            if (element.identity === replacement) sawReplacement = true
        }
        assertTrue(retainedOriginal)
        assertFalse(sawReplacement)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    /** 不替换 body 或 statements 容器，新增同类型语句也必须使当前输入代次失效。 */
    @Test
    fun appendingStatementInPlaceIsRejectedAndFrozenWalkKeepsOriginalStatements() {
        val fixture = Fixture()
        val snapshot = fixture.capture()
        val statements = fixture.body.mutableStatements()

        statements.add(fixture.literal(4L))

        assertSame(statements, fixture.body.statements)
        assertSame(fixture.body, fixture.function.body)
        assertFrozenLiterals(snapshot, fixture.owner, fixture.function, fixture.literals)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    /** 同 source、同值、同 Cone 类型的新节点仍属于另一代，不能靠类型和值相等放过替换。 */
    @Test
    fun replacingStatementInPlaceWithEquivalentLiteralIsRejected() {
        val fixture = Fixture()
        val snapshot = fixture.capture()
        val original = fixture.literals.first()
        val replacement = buildLiteralExpression {
            source = original.source
            coneTypeOrNull = original.coneTypeOrNull
            kind = original.kind
            value = original.value
        }
        assertNotSame(original, replacement)
        assertSame(original.source, replacement.source)
        assertSame(original.coneTypeOrNull, replacement.coneTypeOrNull)
        val statements = fixture.body.mutableStatements()

        statements[0] = replacement

        assertSame(statements, fixture.body.statements)
        assertSame(fixture.body, fixture.function.body)
        assertFrozenLiterals(snapshot, fixture.owner, fixture.function, fixture.literals)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    /** 节点集合、数量和尾表达式保持不变，只交换前两条语句，验证边具有顺序语义。 */
    @Test
    fun reorderingExistingStatementsInPlaceIsRejected() {
        val fixture = Fixture()
        val snapshot = fixture.capture()
        val statements = fixture.body.mutableStatements()

        statements[0] = fixture.literals[1]
        statements[1] = fixture.literals[0]

        assertSame(statements, fixture.body.statements)
        assertSame(fixture.body, fixture.function.body)
        assertSame(fixture.literals.last(), statements.last())
        assertFrozenLiterals(snapshot, fixture.owner, fixture.function, fixture.literals)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    @Test
    fun removingStatementInPlaceIsRejectedAndFrozenWalkRetainsRemovedNode() {
        val fixture = Fixture()
        val snapshot = fixture.capture()
        val statements = fixture.body.mutableStatements()

        statements.removeAt(0)

        assertSame(statements, fixture.body.statements)
        assertSame(fixture.body, fixture.function.body)
        assertFrozenLiterals(snapshot, fixture.owner, fixture.function, fixture.literals)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    /** 非直接函数正文的嵌套 block 也属于完整来源图，不能只校验 function.body 的第一层。 */
    @Test
    fun appendingInsideNestedBlockIsRejected() {
        val fixture = Fixture()
        val nestedLiteral = fixture.literal(10L)
        val nested = fixture.block(nestedLiteral)
        fixture.body.mutableStatements().add(1, nested)
        val snapshot = fixture.capture()
        val expected = listOf(fixture.literals[0], nestedLiteral, fixture.literals[1], fixture.literals[2])

        nested.mutableStatements().add(fixture.literal(11L))

        assertSame(nested, fixture.body.statements[1])
        assertSame(fixture.body, fixture.function.body)
        assertFrozenLiterals(snapshot, fixture.owner, fixture.function, expected)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    /** 类型值不变时替换 type ref，旧节点自身没有变化，只有父声明的边能检出替换。 */
    @Test
    fun replacingReturnTypeRefWithSameResolvedTypeIsRejected() {
        val fixture = Fixture()
        val original = fixture.function.returnTypeRef as CfirResolvedTypeRef
        val snapshot = fixture.capture()
        val replacement = buildResolvedTypeRef {
            source = original.source
            coneType = original.coneType
        }
        assertNotSame(original, replacement)
        assertSame(original.coneType, replacement.coneType)

        fixture.function.replaceReturnTypeRef(replacement)

        var retainedOriginal = false
        var sawReplacement = false
        snapshot.walk(fixture.function, fixture.function) { element, _ ->
            if (element.identity === original) retainedOriginal = true
            if (element.identity === replacement) sawReplacement = true
        }
        assertTrue(retainedOriginal)
        assertFalse(sawReplacement)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    /**
     * helper 不在物理成员列表里，必须经参数默认值元数据边捕获，并保留真实词法 owner。
     * 此正向契约要求生产捕获路径修复首次访问 helper 时误将参数作为词法 owner 的问题；
     * 不通过提前登记 helper 或调整物理成员顺序绕开该问题。
     */
    @Test
    fun unchangedDefaultHelperIsCapturedAndValidatesRepeatedly() {
        val fixture = Fixture(withDefaultHelper = true)
        val defaults = requireNotNull(fixture.defaults)
        val snapshot = fixture.capture()

        assertEquals(listOf(fixture.function), fixture.owner.declarations)
        assertSame(fixture.function, snapshot.helperOwners.getValue(defaults.helper))
        assertSame(fixture.owner, snapshot.owners.getValue(defaults.helper))
        assertSame(fixture.function, snapshot.owners.getValue(defaults.parameter))
        assertSame(fixture.file, snapshot.sourceFiles.getValue(defaults.helper))
        assertFrozenLiterals(snapshot, fixture.function, defaults.helper, defaults.literals)
        assertDoesNotThrow { repeat(2) { snapshot.validateUnchanged() } }
    }

    @Test
    fun appendingInsideDefaultHelperBodyIsRejected() {
        val fixture = Fixture(withDefaultHelper = true)
        val defaults = requireNotNull(fixture.defaults)
        val snapshot = fixture.capture()
        val statements = defaults.body.mutableStatements()

        statements.add(fixture.literal(43L))

        assertSame(defaults.helper, requireNotNull(defaults.parameter.defaultValueMetadata).function)
        assertSame(defaults.body, defaults.helper.body)
        assertSame(statements, defaults.body.statements)
        assertFrozenLiterals(snapshot, fixture.function, defaults.helper, defaults.literals)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    @Test
    fun replacingDefaultHelperEdgeIsRejectedAndFrozenWalkKeepsOriginalHelper() {
        val fixture = Fixture(withDefaultHelper = true)
        val defaults = requireNotNull(fixture.defaults)
        val snapshot = fixture.capture()
        val replacement = fixture.namedFunction("replacementDefault", fixture.block(fixture.literal(99L)))
        val metadata = requireNotNull(defaults.parameter.defaultValueMetadata)

        defaults.parameter.defaultValueMetadata = metadata.copy(function = replacement)

        assertSame(defaults.parameter, fixture.function.valueParameters.single())
        assertSame(fixture.body, fixture.function.body)
        assertFrozenLiterals(snapshot, fixture.function, defaults.helper, defaults.literals)
        var sawReplacement = false
        snapshot.walk(fixture.function, fixture.function) { element, _ ->
            if (element.identity === replacement) sawReplacement = true
        }
        assertFalse(sawReplacement)
        assertThrows(IllegalStateException::class.java) { snapshot.validateUnchanged() }
    }

    /**
     * 公开 statements 为只读 List；真实 buildBlock 产物保留 builder 的 MutableList backing。
     * 仅在测试中显式取得该 backing，模拟原地输入变更，不改变生产 API 的只读契约。
     */
    private fun CfirBlock.mutableStatements(): MutableList<CfirStatement> = statements as MutableList<CfirStatement>

    /** 按词法 owner 观察语句顺序，不约束参数区与正文区之间的遍历顺序。 */
    private fun assertFrozenLiterals(
        snapshot: CfirExportAnalysisInputSnapshot,
        root: CfirDeclaration,
        literalOwner: CfirDeclaration,
        expected: List<CfirLiteralExpression>,
    ) {
        val actual = mutableListOf<CfirLiteralExpression>()
        snapshot.walk(root, root) { element, context ->
            if (context === literalOwner) (element.identity as? CfirLiteralExpression)?.let(actual::add)
        }
        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { index, literal -> assertSame(literal, actual[index], "Frozen literal at index $index") }
    }

    private data class DefaultInput(
        val parameter: CfirValueParameter,
        val helper: CfirNamedFunction,
        val body: CfirBlock,
        val literals: List<CfirLiteralExpression>,
    )

    private data class BindingInput(
        val container: CfirPatternVariable,
        val variable: CfirPatternBindingVariable,
        val initializer: CfirLiteralExpression,
    )

    private data class ComplexBindingInput(
        val container: CfirPatternVariable,
        val pattern: CfirTuplePattern,
        val bindings: List<CfirPatternBindingVariable>,
        val initializer: CfirTupleLiteral,
    )

    /** 用正式 Builder 完成 Unit Block 的全部 record/slot；原始图暂不登记 ordinary CFIR aliases。 */
    private fun completedUnitGraph(fixture: SourceFixture): CfirSourceGraph {
        val builder = CfirSourceGraph.Builder()
        val literal = fixture.body.statements.single() as CfirLiteralExpression
        val literalIdentity = builder.reserveGenerated()
        builder.fill(literalIdentity, CfirSourceExpressionRecord(
            fixture.function, checkNotNull(literal.coneTypeOrNull), null,
            CfirSourceExpressionPayload.Literal(literal.kind, "()", CfirSourceStringKind.NORMAL),
        ))
        val bodyIdentity = builder.reserveGenerated()
        builder.fill(bodyIdentity, CfirSourceExpressionRecord(
            fixture.function, checkNotNull(fixture.body.coneTypeOrNull), null,
            CfirSourceExpressionPayload.Block(listOf(CfirSourceSlot.Expression(literalIdentity)), isUnsafe = false),
        ))
        builder.body(fixture.function, bodyIdentity)
        builder.declarationOwner(fixture.function, null)
        builder.functionContext(fixture.function, CfirSourceFunctionContext(null, null))
        return builder.finish()
    }

    /**
     * Published header 合同 fixture：真实 canonical 函数/参数和 Int64 literal 由各自完成请求发布。
     * 不提供 resolver，不伪造 CFG/readiness/phase 升级，也不宣称运行了 serialization 的原 context。
     */
    private class PublishedHeaderFixture(suffix: String) : CfirImportedDeclarationHeaderSource {
        private val template = SourceFixture(suffix)
        val module: CfirModuleData = template.module
        private val callableId = CallableId(FqName("snapshot.borrowed.$suffix"), Name.identifier("borrowed"))
        private val functionSymbol = CfirNamedFunctionSymbol(callableId)
        val default: CfirLiteralExpression = buildLiteralExpression {
            source = TestSource("default:$suffix")
            coneTypeOrNull = ConePrimitiveType.INT64
            kind = CfirLiteralKind.INT
            value = 41L
        }
        val parameter: CfirValueParameter = buildValueParameter {
            source = TestSource("parameter:$suffix")
            moduleData = module
            resolvePhase = CfirResolvePhase.TYPES
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            contentSource = this@PublishedHeaderFixture
            isLocal = false
            isNamed = true
            name = Name.identifier("value")
            symbol = CfirValueParameterSymbol(CallableId(name))
            containingDeclarationSymbol = functionSymbol
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT64 }
        }.also {
            it.cjoDeclarationOrigin = CfirCjoDeclarationOrigin("$suffix.cjo", 1, isTopLevel = false)
            it.defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = true, function = null)
        }
        val function: CfirNamedFunction = buildNamedFunctionCopy(template.function) {
            symbol = functionSymbol
            name = callableId.callableName
            origin = CfirDeclarationOrigin.Library
            resolvePhase = CfirResolvePhase.TYPES
            contentSource = this@PublishedHeaderFixture
            body = null
            valueParameters += parameter
        }.also { it.cjoDeclarationOrigin = CfirCjoDeclarationOrigin("$suffix.cjo", 0, isTopLevel = true) }
        val completions = mutableListOf<CfirDeclaration>()
        private var readingDepth = 0
        val isReading: Boolean get() = readingDepth != 0
        var afterSnapshot: (() -> Unit)? = null
        /** 只在原 owner 建立 fence 的入口注入失败，位于增深和 action 调用之前。 */
        var beforeReadOnly: (() -> Unit)? = null
        private var views = java.util.IdentityHashMap<CfirDeclaration, CfirPublishedAnnotations>()
        private var defaults = java.util.IdentityHashMap<CfirValueParameter, CfirExpression>()
        private var current: CfirPublishedContentSnapshot = completedSnapshot()

        override fun snapshot(): CfirPublishedContentSnapshot {
            val captured = current
            afterSnapshot?.invoke()
            return captured
        }

        override fun completeHeader(declaration: CfirDeclaration) {
            check(declaration === function || declaration === parameter)
            check(declaration.symbol.isBound && declaration.symbol.cfir === declaration)
            check(declaration.contentSource === this)
            check(!isReading) { "Published header production is forbidden inside its original owner fence" }
            completions += declaration
            views = java.util.IdentityHashMap(views).apply {
                putIfAbsent(declaration, CfirPublishedAnnotations.Complete(emptyList()))
            }
            defaults = java.util.IdentityHashMap(defaults).apply {
                if (declaration === parameter) putIfAbsent(parameter, default)
            }
            current = completedSnapshot()
        }

        override fun <T> withReadOnlyContent(action: () -> T): T {
            beforeReadOnly?.invoke()
            val previous = readingDepth
            readingDepth++
            try {
                return action()
            } finally {
                readingDepth = previous
            }
        }

        private fun completedSnapshot(): CfirPublishedContentSnapshot {
            val annotations = java.util.Collections.unmodifiableMap(java.util.IdentityHashMap(views))
            val parameterDefaults = java.util.Collections.unmodifiableMap(java.util.IdentityHashMap(defaults))
            return object : CfirPublishedContentSnapshot {
                override fun body(owner: CfirFunctionBodyOwner): CfirBlock? = null
                override fun initializer(owner: CfirVariable): CfirExpression? = null
                override fun defaultValue(owner: CfirValueParameter): CfirExpression? = parameterDefaults[owner]
                override fun annotations(owner: CfirDeclaration): CfirPublishedAnnotations =
                    annotations[owner] ?: CfirPublishedAnnotations.Unselected
            }
        }
    }

    /**
     * 沿用 Fixture 的真实 Source module 绑定和宏后 provider 登记流程。
     * 每个实例单独建文件、Unit header/正文；可复用的输入仅为已经求值的语义叶。
     */
    private class SourceFixture(
        suffix: String,
        bodyValue: CfirConstantValueExpression? = null,
        withAlias: Boolean = false,
        withExtend: Boolean = false,
    ) {
        private val session = object : CfirSession(Kind.Source) {}
        val module: CfirModuleData = CfirSourceModuleData(
            Name.identifier("shared-context-$suffix"), emptyList(), emptyList(),
            CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also { it.bindSession(session) }
        private val packageName = FqName("snapshot.shared.$suffix")
        private val scopeProvider = CfirCangJieScopeProvider()
        val body: CfirBlock = buildBlock {
            source = TestSource("block:$suffix")
            coneTypeOrNull = ConePrimitiveType.UNIT
            bodyValue?.let { statements += it }
            statements += buildLiteralExpression {
                source = TestSource("unit:$suffix")
                coneTypeOrNull = ConePrimitiveType.UNIT
                kind = CfirLiteralKind.UNIT
                value = Unit
            }
        }
        val function: CfirNamedFunction = buildNamedFunction {
            source = TestSource("function:$suffix")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = buildResolvedDeclarationStatus {
                visibility = Visibilities.Public
                visibilityExplicitness = CfirModifierExplicitness.EXPLICIT
                modalityExplicitness = CfirModifierExplicitness.IMPLICIT
                bodyPresence = CfirBodyPresence.UNKNOWN
                isDeclarationFile = false
                isFromCommonPart = false
                isVisibilityExplicit = true
                isModalityExplicit = false
                isAbstractExplicit = false
                isOverride = false
                isOperator = false
                isStatic = false
                isConst = false
                isMut = false
                isUnsafe = false
                isForeign = false
                isC = false
                isCommon = false
                isSpecific = false
                isCommonWithDefault = false
                isRedef = false
                isDefault = false
                isAbstract = false
                isOpen = false
                isSealed = false
                modality = Modality.FINAL
            }
            returnTypeRef = buildResolvedTypeRef {
                source = TestSource("return-type:$suffix")
                coneType = ConePrimitiveType.UNIT
            }
            name = Name.identifier("work")
            symbol = CfirNamedFunctionSymbol(CallableId(packageName, name))
            isMut = false
            body = this@SourceFixture.body
        }
        /** 仅 alias API 用例启用；在真实宏后登记前构造完整声明，不修改既有 Unit header/正文。 */
        val classifier: CfirClass? = if (withAlias) buildClass {
            source = TestSource("classifier:$suffix")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            scopeProvider = this@SourceFixture.scopeProvider
            status = function.status
            name = Name.identifier("Target")
            symbol = CfirClassSymbol(ClassId(packageName, name))
        } else null
        val alias: CfirTypeAlias? = classifier?.let { target ->
            buildTypeAlias {
                source = TestSource("alias:$suffix")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                scopeProvider = this@SourceFixture.scopeProvider
                status = function.status
                name = Name.identifier("TargetAlias")
                symbol = CfirTypeAliasSymbol(ClassId(packageName, name))
                expandedTypeRef = buildResolvedTypeRef {
                    source = TestSource("alias-rhs:$suffix")
                    coneType = ConeClassLikeType(target.symbol.toLookupTag())
                }
            }
        }
        /** Extend 复用完整 Unit header/正文，显式绑定真实 receiver；词法 owner 由完整 SOURCE 图登记。 */
        val extend: CfirExtend? = if (withExtend) buildExtend {
            source = TestSource("extend:$suffix")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            symbol = CfirExtendSymbol()
            status = function.status
            extendedTypeRef = buildResolvedTypeRef {
                source = TestSource("extend-target:$suffix")
                coneType = ConeClassLikeType(checkNotNull(classifier).symbol.toLookupTag())
            }
            declarations += buildNamedFunctionCopy(function) {
                name = Name.identifier("extensionWork")
                symbol = CfirNamedFunctionSymbol(CallableId(packageName, name))
                dispatchReceiverType = ConeClassLikeType(checkNotNull(classifier).symbol.toLookupTag())
            }
        } else null
        val file: CfirFile = buildFile {
            source = TestSource("file:$suffix.cj")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            symbol = CfirFileSymbol()
            name = "$suffix.cj"
            packageDirective = buildPackageDirective {
                packageFqName = packageName
                isMacroPackage = false
            }
            if (extend == null) declarations += function else declarations += extend
            classifier?.let { declarations += it }
            alias?.let { declarations += it }
        }

        init {
            if (withAlias) {
                // 正式 compiler resolver 不推进阶段；本 fixture 已有完整 BODY_RESOLVE 声明和真实 RHS。
                check(checkNotNull(classifier).resolvePhase >= CfirResolvePhase.SUPER_TYPES)
                check(checkNotNull(alias).resolvePhase >= CfirResolvePhase.SUPER_TYPES)
                check(alias.expandedTypeRef is CfirResolvedTypeRef)
                session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
                session.register(
                    CfirLanguageSettingsComponent::class,
                    CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT),
                )
                session.register(TypeComponents::class, TypeComponents(session))
            }
            val provider = CfirProviderImpl(session, scopeProvider)
            session.register(CfirProvider::class, provider)
            session.register(CfirSymbolProvider::class, provider.symbolProvider)
            val pre = buildPreMacroRawFiles(
                session, listOf(PreMacroCfirFile(file, annotationMetadata = CfirAnnotationMetadataBuilder())),
            )
            val result = MacroConstructionService.successOf(pre, listOf(file), MacroExpansionRegistry.EMPTY)
            recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
            if (withAlias) {
                val target = checkNotNull(classifier)
                val completedAlias = checkNotNull(alias)
                check(target.superTypeRefs.isEmpty())
                // 复用正式 SUPER_TYPES owner 的记录/发布协议；alias RHS 不是普通继承边。
                // 只发布已有真实 ref，不重解析、替换类型或改写任何声明阶段。
                val graphStore = CfirSuperTypeGraphStore()
                session.register(CfirSuperTypeGraphStore::class, graphStore)
                session.register(CfirDirectSupertypeProvider::class, graphStore)
                graphStore.recordDeclared(target, target.superTypeRefs, emptyList())
                graphStore.recordDeclared(completedAlias, listOf(completedAlias.expandedTypeRef), emptyList())
                graphStore.publishCompletedProducts()
            }
        }
    }

    /** 完整解析结果与声明 phase 独立建模，直接建树到指定 phase，不倒退已有 resolve state。 */
    private class Fixture(
        withDefaultHelper: Boolean = false,
        withSimpleBinding: Boolean = false,
        withComplexBinding: Boolean = false,
        withCjoVariable: Boolean = false,
        private val phase: CfirResolvePhase = CfirResolvePhase.BODY_RESOLVE,
    ) {
        private val session: CfirSession = object : CfirSession(Kind.Source) {}
        private val moduleData: CfirModuleData = CfirSourceModuleData(
            Name.identifier("export-snapshot-test"), emptyList(), emptyList(),
            CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also { it.bindSession(session) }
        private val packageName = FqName("snapshot")
        private val ownerSymbol = CfirClassSymbol(ClassId(packageName, Name.identifier("Owner")))
        private val ownerType = ConeClassLikeType(ownerSymbol.toLookupTag())
        private val scopeProvider = CfirCangJieScopeProvider()
        val literals = listOf(literal(1L), literal(2L), literal(3L))
        val body: CfirBlock = block(*literals.toTypedArray())
        val function: CfirNamedFunction = namedFunction("work", body)
        val defaults: DefaultInput? = if (withDefaultHelper) {
            val helperLiterals = listOf(literal(41L), literal(42L))
            val helperBody = block(*helperLiterals.toTypedArray())
            val helper = namedFunction("defaultValue", helperBody)
            val parameter = buildValueParameter {
                source = TestSource("parameter:value")
                moduleData = this@Fixture.moduleData
                resolvePhase = this@Fixture.phase
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                isNamed = true
                name = Name.identifier("value")
                symbol = CfirValueParameterSymbol(CallableId(name))
                containingDeclarationSymbol = function.symbol
                returnTypeRef = resolvedInt64("parameter-type:value")
            }
            parameter.defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = true, function = helper)
            function.replaceValueParameters(listOf(parameter))
            DefaultInput(parameter, helper, helperBody, helperLiterals)
        } else null
        val owner: CfirClass = buildClass {
            source = TestSource("class:Owner")
            moduleData = this@Fixture.moduleData
            resolvePhase = this@Fixture.phase
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            scopeProvider = this@Fixture.scopeProvider
            status = resolvedStatus()
            symbol = ownerSymbol
            name = ownerSymbol.classId.shortClassName
            declarations += function
        }
        val simpleBinding: BindingInput? = if (withSimpleBinding) {
            val bindingName = Name.identifier("seed")
            val initializer = literal(47L)
            val variable = buildPatternBindingVariable {
                source = TestSource("binding:seed")
                moduleData = this@Fixture.moduleData
                resolvePhase = this@Fixture.phase
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = resolvedStatus(isConst = true)
                name = bindingName
                symbol = CfirPatternBindingSymbol(CallableId(packageName, bindingName))
                isVar = false
                returnTypeRef = resolvedInt64("binding-type:seed")
            }
            val container = buildPatternVariable {
                source = TestSource("variable:seed")
                moduleData = this@Fixture.moduleData
                resolvePhase = this@Fixture.phase
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = resolvedStatus(isConst = true)
                symbol = CfirPatternVariableSymbol(CallableId(packageName, Name.special("<pattern-variable>")))
                isVar = false
                returnTypeRef = resolvedInt64("variable-type:seed")
                pattern = buildBindingPattern {
                    source = variable.source
                    name = bindingName
                    bindingVariable = variable
                }
                this.initializer = initializer
            }
            BindingInput(container, variable, initializer)
        } else null
        /** 官方 14276740ffec1262b81b6b5236b1d840af270b37：局部 tuple 模式的初值只由 VarWithPattern 拥有。 */
        val complexBinding: ComplexBindingInput? = if (withComplexBinding) {
            val bindings = listOf("zeta", "alpha", "middle").map { bindingVariable(it, isLocal = true) }
            val patterns = bindings.map { variable ->
                buildBindingPattern {
                    source = variable.source
                    name = variable.name
                    bindingVariable = variable
                }
            }
            val pattern = buildTuplePattern {
                source = TestSource("tuple-pattern")
                elements += patterns[0]
                elements += buildTuplePattern {
                    source = TestSource("nested-tuple-pattern")
                    elements += patterns[1]
                    elements += patterns[2]
                }
            }
            val initializer = tuple(literal(51L), tuple(literal(52L), literal(53L)))
            val container = buildPatternVariable {
                source = TestSource("variable:tuple")
                moduleData = this@Fixture.moduleData
                resolvePhase = this@Fixture.phase
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = true
                status = resolvedStatus()
                symbol = CfirPatternVariableSymbol(CallableId(Name.special("<tuple-pattern>")))
                isVar = false
                returnTypeRef = buildResolvedTypeRef { coneType = requireNotNull(initializer.coneTypeOrNull) }
                this.pattern = pattern
                this.initializer = initializer
            }
            // 与语句换代用例一样，仅在 Fixture 中访问真实 builder 的 mutable backing。
            (body.statements as MutableList<CfirStatement>).add(0, container)
            ComplexBindingInput(container, pattern, bindings, initializer)
        } else null
        /** loader 的普通 VarDecl 是 FieldVariable；保留 wire index 2288 与原始 IMPORTED=false。 */
        val cjoVariable: CfirFieldVariable? = if (withCjoVariable) {
            buildFieldVariable {
                source = null
                moduleData = this@Fixture.moduleData
                resolvePhase = this@Fixture.phase
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = resolvedStatus()
                name = Name.identifier("detailMessage")
                symbol = CfirFieldVariableSymbol(CallableId(packageName, name))
                isVar = false
                returnTypeRef = resolvedInt64("library-variable-type:detailMessage")
            }.also {
                it.cjoDeclarationOrigin = CfirCjoDeclarationOrigin("core.cjo", 2288, isTopLevel = false)
            }
        } else null
        val file: CfirFile = buildFile {
            source = TestSource("file:snapshot.cj")
            moduleData = this@Fixture.moduleData
            resolvePhase = this@Fixture.phase
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            symbol = CfirFileSymbol()
            name = "snapshot.cj"
            packageDirective = buildPackageDirective {
                packageFqName = packageName
                isMacroPackage = false
            }
            declarations += owner
            simpleBinding?.let { declarations += it.container }
        }

        init {
            val provider = CfirProviderImpl(session, scopeProvider)
            session.register(CfirProvider::class, provider)
            session.register(CfirSymbolProvider::class, provider.symbolProvider)
            val pre = buildPreMacroRawFiles(
                session, listOf(PreMacroCfirFile(file, annotationMetadata = CfirAnnotationMetadataBuilder())),
            )
            val result = MacroConstructionService.successOf(pre, listOf(file), MacroExpansionRegistry.EMPTY)
            recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
        }

        fun capture(): CfirExportAnalysisInputSnapshot = CfirExportAnalysisInputSnapshot.capture(
            files = listOf(file), roots = listOf(owner),
            members = { declaration -> (declaration as CfirClass).declarations },
        )

        fun literal(value: Long): CfirLiteralExpression = buildLiteralExpression {
            source = TestSource("literal:$value")
            coneTypeOrNull = ConePrimitiveType.INT64
            kind = CfirLiteralKind.INT
            this.value = value
        }

        fun tuple(vararg expressions: CfirExpression): CfirTupleLiteral = buildTupleLiteral {
            source = TestSource("tuple-initializer")
            coneTypeOrNull = ConeTupleType(expressions.map { requireNotNull(it.coneTypeOrNull) })
            elements += expressions
        }

        private fun bindingVariable(
            name: String,
            isLocal: Boolean,
            origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
        ): CfirPatternBindingVariable = buildPatternBindingVariable {
            source = TestSource("binding:$name")
            moduleData = this@Fixture.moduleData
            resolvePhase = this@Fixture.phase
            this.origin = origin
            attributes = CfirDeclarationAttributes()
            this.isLocal = isLocal
            status = resolvedStatus()
            this.name = Name.identifier(name)
            symbol = CfirPatternBindingSymbol(if (isLocal) CallableId(this.name) else CallableId(packageName, this.name))
            isVar = false
            returnTypeRef = resolvedInt64("binding-type:$name")
        }

        fun block(vararg statements: CfirStatement): CfirBlock = buildBlock {
            source = TestSource("block")
            coneTypeOrNull = ConePrimitiveType.INT64
            this.statements += statements
        }

        fun namedFunction(name: String, body: CfirBlock): CfirNamedFunction = buildNamedFunction {
            source = TestSource("function:$name")
            moduleData = this@Fixture.moduleData
            resolvePhase = this@Fixture.phase
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = resolvedStatus()
            dispatchReceiverType = ownerType
            returnTypeRef = resolvedInt64("return-type:$name")
            symbol = CfirNamedFunctionSymbol(CallableId(ownerSymbol.classId, Name.identifier(name)))
            this.name = Name.identifier(name)
            isMut = false
            this.body = body
        }

        private fun resolvedInt64(identity: String): CfirResolvedTypeRef = buildResolvedTypeRef {
            source = TestSource(identity)
            coneType = ConePrimitiveType.INT64
        }

        private fun resolvedStatus(isConst: Boolean = false): CfirResolvedDeclarationStatus = buildResolvedDeclarationStatus {
            visibility = Visibilities.Public
            visibilityExplicitness = CfirModifierExplicitness.EXPLICIT
            modalityExplicitness = CfirModifierExplicitness.IMPLICIT
            bodyPresence = CfirBodyPresence.UNKNOWN
            isDeclarationFile = false
            isFromCommonPart = false
            isVisibilityExplicit = true
            isModalityExplicit = false
            isAbstractExplicit = false
            isOverride = false
            isOperator = false
            isStatic = false
            this.isConst = isConst
            isMut = false
            isUnsafe = false
            isForeign = false
            isC = false
            isCommon = false
            isSpecific = false
            isCommonWithDefault = false
            isRedef = false
            isDefault = false
            isAbstract = false
            isOpen = false
            isSealed = false
            modality = Modality.FINAL
        }
    }

    /** 非空来源满足 Source 声明建树不变量；替换用例显式复用来源以隔离节点身份变化。 */
    private class TestSource(identity: String) : CjBinarySourceElement(identity, null, identity)
}
