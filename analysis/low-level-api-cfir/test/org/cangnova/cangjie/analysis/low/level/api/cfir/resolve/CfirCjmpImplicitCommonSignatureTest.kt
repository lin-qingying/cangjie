package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.TargetSourceTestSupport
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.prepareCjmpCommonCandidates
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.cjmpMatchFacts
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.CfirCjmpBatchFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.CjModifierKind
import org.cangnova.cangjie.parsing.CjScopeKind
import org.cangnova.cangjie.parsing.CjmpDeclarationRoleCheck
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.CjmpModifierDeclarationKind
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.parsing.cjmpDeclarationRoleCheckOrNull
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.parsing.parsedFileFacts
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.psiUtil.parsedDeclarationModifiersOrNull
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 直接从 raw 声明发起 CJMP 请求，验证 common 隐式签名的锁外准备和关闭门禁时的阶段推进。 */
class CfirCjmpImplicitCommonSignatureTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/cjmpImplicitCommonSignature",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
    override val additionalDirectives = super.additionalDirectives + CfirDiagnosticsDirectives

    @Test
    fun implicitCommonChain(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertImplicitCommonSignatures(mainFile, mainModule, testServices, parallel = false)
    }

    @Test
    fun parallelImplicitCommonChain(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertImplicitCommonSignatures(mainFile, mainModule, testServices, parallel = true)
    }

    @Test
    fun familyFirstImplicitCommonChain(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertSessionCompilationModes(testServices)
        val commonModule = testServices.cjTestModuleStructure.mainModules.single { it !== mainModule }
        val commonPsi = commonModule.cjFiles.single()
        val common = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "value" }
        val specificFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirNamedFunction>().single()

        assertTrue(specific.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertNull(specific.cjmpMatchFacts)
        val family = specific.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(specific)

        assertSame(common, family.commonFor(specific))
        assertEquals(ConePrimitiveType.INT64, common.returnTypeRef.coneTypeOrNull)
        assertTrue(common.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(specific.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertNull(specific.cjmpMatchFacts, "family preparation must not publish target-owned facts")
    }

    @Test
    fun concurrentFamilyAndTargetImplicitCommonChain(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertSessionCompilationModes(testServices)
        val commonModule = testServices.cjTestModuleStructure.mainModules.single { it !== mainModule }
        val commonPsi = commonModule.cjFiles.single()
        val common = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "value" }
        val specificFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val aggregate = specific.moduleData.session.cjmpFamilyAggregate
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val target = executor.submit {
                ready.countDown()
                check(start.await(30, TimeUnit.SECONDS))
                ApplicationManager.getApplication().runReadAction {
                    specific.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
                }
            }
            val family = executor.submit<CfirCjmpFamilySnapshot> {
                ready.countDown()
                check(start.await(30, TimeUnit.SECONDS))
                ApplicationManager.getApplication().runReadAction<CfirCjmpFamilySnapshot> {
                    aggregate.matchingGroupFor(specific)
                }
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS))
            start.countDown()
            target.get(60, TimeUnit.SECONDS)
            val snapshot = family.get(60, TimeUnit.SECONDS)
            assertSame(common, snapshot.commonFor(specific))
            assertEquals(ConePrimitiveType.INT64, common.returnTypeRef.coneTypeOrNull)
            assertTrue(common.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
            assertEquals(CfirResolvePhase.CJMP_MATCHING, specific.resolvePhase)
            assertNotNull(specific.cjmpMatchFacts)
        } finally {
            start.countDown()
            executor.shutdownNow()
            check(executor.awaitTermination(30, TimeUnit.SECONDS))
        }
    }

    @Test
    fun concurrentFirstGroupMissPreparesAndPublishesOneSnapshot(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        assertSessionCompilationModes(testServices)
        val commonModule = testServices.cjTestModuleStructure.mainModules.single { it !== mainModule }
        val commonPsi = commonModule.cjFiles.single()
        val common = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "value" }
        val specificFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val session = specific.moduleData.session
        val prepareCount = AtomicInteger()
        val prepareStarted = CountDownLatch(1)
        val releasePrepare = CountDownLatch(1)
        val ownerThread = AtomicReference<Thread>()
        val aggregate = CfirCjmpBatchFamilyAggregate(session) { declaration, ownerFacts, matchingMode ->
            prepareCount.incrementAndGet()
            ownerThread.set(Thread.currentThread())
            prepareStarted.countDown()
            check(releasePrepare.await(30, TimeUnit.SECONDS))
            declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
            val commonCandidates = prepareCjmpCommonCandidates(declaration, session, ownerFacts)
            CfirCjmpMatchRunner.prepareDeclaration(declaration, session, ownerFacts, commonCandidates, matchingMode)
        }

        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val threads = arrayOf(AtomicReference<Thread>(), AtomicReference<Thread>())
        val executor = Executors.newFixedThreadPool(2)
        try {
            val requests = threads.map { thread ->
                executor.submit<CfirCjmpFamilySnapshot> {
                    thread.set(Thread.currentThread())
                    ready.countDown()
                    check(start.await(30, TimeUnit.SECONDS))
                    ApplicationManager.getApplication().runReadAction<CfirCjmpFamilySnapshot> {
                        aggregate.matchingGroupFor(specific)
                    }
                }
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS))
            start.countDown()
            assertTrue(prepareStarted.await(30, TimeUnit.SECONDS))
            val waiterThread = threads.mapNotNull { it.get() }.single { it !== ownerThread.get() }
            awaitSingleFlightWait(waiterThread)
            assertEquals(1, prepareCount.get())
            assertFalse(requests.all { it.isDone })

            releasePrepare.countDown()
            val first = requests[0].get(60, TimeUnit.SECONDS)
            val second = requests[1].get(60, TimeUnit.SECONDS)
            assertSame(first, second)
            assertSame(common, first.commonFor(specific))
            assertEquals(1, prepareCount.get())
            assertSame(first, aggregate.matchingGroupFor(specific))
            assertEquals(1, prepareCount.get())
        } finally {
            releasePrepare.countDown()
            executor.shutdownNow()
            check(executor.awaitTermination(30, TimeUnit.SECONDS))
        }
    }

    @Test
    fun failedGroupPreparationIsEvictedAndRetriedFromTheSameInput(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        assertSessionCompilationModes(testServices)
        val commonModule = testServices.cjTestModuleStructure.mainModules.single { it !== mainModule }
        val commonPsi = commonModule.cjFiles.single()
        val common = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "value" }
        val specificFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val session = specific.moduleData.session
        val prepareCount = AtomicInteger()
        val failure = IllegalStateException("injected group preparation failure")
        val aggregate = CfirCjmpBatchFamilyAggregate(session) { declaration, ownerFacts, matchingMode ->
            val attempt = prepareCount.incrementAndGet()
            declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
            val commonCandidates = prepareCjmpCommonCandidates(declaration, session, ownerFacts)
            val facts = CfirCjmpMatchRunner.prepareDeclaration(declaration, session, ownerFacts, commonCandidates, matchingMode)
            if (attempt == 1) throw failure
            facts
        }

        val observed = assertThrows(IllegalStateException::class.java) {
            ApplicationManager.getApplication().runReadAction<CfirCjmpFamilySnapshot> {
                aggregate.matchingGroupFor(specific)
            }
        }
        assertSame(failure, observed)

        val retried = ApplicationManager.getApplication().runReadAction<CfirCjmpFamilySnapshot> {
            aggregate.matchingGroupFor(specific)
        }
        assertSame(common, retried.commonFor(specific))
        assertEquals(2, prepareCount.get())
        assertSame(retried, aggregate.matchingGroupFor(specific))
        assertEquals(2, prepareCount.get())
    }

    @Test
    fun modeDisabled(mainFile: CjFile, testServices: TestServices) = assertDisabledPhase(mainFile, testServices)

    @Test
    fun languageDisabled(mainFile: CjFile, testServices: TestServices) = assertDisabledPhase(mainFile, testServices)

    /**
     * 官方 v1.1.0 的 TOPLEVEL/FUNC 入口在 SPECIFIC 编译中拒绝 common，返回空属性并保留文件 SPECIFIC 角色。
     * 原始前缀与错误恢复声明继续保留，LL 匹配相位仍须发布 facts；该声明没有 CJMP 配对资格。
     * 证据：74452c8ee3387e4406f005a72ddd14926b52306f 的 ParseCJMPDecl.cpp、ParseModifiers.cpp、ParseDecl.cpp。
     */
    @Test
    fun commonRoleTakesPrecedence(mainFile: CjFile, testServices: TestServices) {
        assertSessionCompilationModes(testServices)
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val sources = TargetSourceTestSupport(file, mainFile)
        val originalFunction = mainFile.declarations.filterIsInstance<CjNamedFunction>().single()
        val targetFunction = sources.targetElement(originalFunction) as CjNamedFunction
        val targetFile = sources.targetFile
        sources.assertTargetElement(originalFunction, function.psi)
        assertEquals(mainFile.text, targetFile.text)
        // EOF trivia 可以由平台挂在原声明上、由完整目标 parser 挂在文件上；源码全文和真实文法词元均须保持。
        fun grammarTokens(declaration: CjNamedFunction) = com.intellij.psi.util.PsiTreeUtil.collectElements(declaration) {
            it.firstChild == null && it.textLength > 0 && it.node.elementType !in CjTokens.ALL_WHITESPACE_TOKENS && it.node.elementType !in CjTokens.COMMENTS
        }.map { it.node.elementType to it.text }
        assertEquals(grammarTokens(originalFunction), grammarTokens(targetFunction))

        // 原始 PUBLIC/COMMON/SPECIFIC 词元属于源码前缀；有效属性只读取本代目标声明的 parser 返回值。
        val rawPrefix = listOf(
            CjTokens.PUBLIC_KEYWORD to "public",
            CjTokens.COMMON_KEYWORD to "common",
            CjTokens.SPECIFIC_KEYWORD to "specific",
        )
        for (declaration in listOf(originalFunction, targetFunction)) {
            val modifierList = requireNotNull(declaration.modifierList)
            val tokens = rawPrefix.map { (kind, _) -> requireNotNull(modifierList.getModifier(kind)) }
            assertEquals(rawPrefix.map { it.first }, tokens.map { it.node.elementType })
            assertEquals(rawPrefix.map { it.second }, tokens.map { it.text })
            assertTrue(tokens.zipWithNext().all { (previous, next) -> previous.textRange.endOffset < next.textRange.startOffset })
        }
        val keyword = requireNotNull(targetFunction.funKeyword)
        assertEquals(CjmpDeclarationRoleCheck.REJECTED, requireNotNull(keyword.node.cjmpDeclarationRoleCheckOrNull()))
        val modifiers = requireNotNull(targetFunction.parsedDeclarationModifiersOrNull())
        assertEquals(CjScopeKind.TOPLEVEL, modifiers.scope)
        assertEquals(CjmpModifierDeclarationKind.FUNCTION, modifiers.declarationKind)
        assertEquals(emptySet<CjModifierKind>(), modifiers.attributes)
        val context = modifiers.parsingContext as CjmpParsingContext.Compilation
        assertTrue(context.enabled)
        assertFalse(context.compileCommon)
        assertTrue(context.compileSpecific)
        assertFalse(context.mixedCommonSpecificDeclarations)
        assertEquals(CjmpFileRole.SPECIFIC, requireNotNull(targetFile.parsedFileFacts).cjmpRole)

        assertTrue(file.moduleData.session.isCjmpSpecificCompilationEnabled())
        function.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertFalse(function.status.isCommon)
        assertFalse(function.status.isSpecific)
        assertFalse(function.status.isCommonWithDefault)
        assertEquals(Visibilities.Internal, function.status.visibility)
        assertFalse(function.status.isVisibilityExplicit)
        assertEquals(CfirResolvePhase.CJMP_MATCHING, function.resolvePhase)
        assertNotNull(function.cjmpMatchFacts)
        val facts = requireNotNull(function.cjmpMatchFacts)
        assertTrue(facts.plans.isEmpty())
        assertNull(facts.container)
        assertTrue(facts.commonContainers.isEmpty())
        val family = function.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(function)
        assertNull(family.commonFor(function))
        assertTrue(family.commonCounterpartsFor(function).isEmpty())
        assertTrue(family.specificBindingsFor(function).isEmpty())
        assertTrue(family.matching.acceptedBindings.isEmpty())

        // 官方 parse_common_in_non_common_file 映射到项目 parser 消息键；文件角色首报阻止后续修饰符冲突检查。
        val diagnostics = targetFile.collectParsingDiagnostics()
        assertEquals(1, diagnostics.size)
        val diagnostic = diagnostics.single()
        assertEquals(CangJieParsingBundle.message("parsing.error.cjmp.common.in.non.common.file"), diagnostic.message)
        assertEquals(ParsingDiagnostic.Origin.ORIGINAL_FILE, diagnostic.origin)
        assertEquals(ParsingDiagnostic.Category.PARSE, diagnostic.category)
        assertEquals(0, diagnostic.startOffset)
        assertEquals(0, diagnostic.endOffset)
        val mainRange = requireNotNull(diagnostic.mainRange)
        assertEquals(1 to 1, mainRange.begin)
        assertEquals(1 to 2, mainRange.end)
        assertTrue(diagnostic.hints.isEmpty())
        assertTrue(diagnostic.nativeHints.isEmpty())
    }

    /** 保留 raw 文件请求顺序，只检查模块工厂传入 session 的模式，不提前解析声明。 */
    private fun assertSessionCompilationModes(testServices: TestServices) {
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val file = module.cjFiles.first()
            val raw = file.getOrBuildCfirFile(file.getResolutionFacadeForTest())
            assertEquals(
                module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single(),
                raw.moduleData.session.cjmpSettings.mode,
                "${module.name}: session 必须消费模块配置的 CJMP 模式",
            )
        }
    }

    private fun assertImplicitCommonSignatures(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
        parallel: Boolean,
    ) {
        assertSessionCompilationModes(testServices)
        val commonModule = testServices.cjTestModuleStructure.mainModules.single { it !== mainModule }
        val commonPsi = commonModule.cjFiles.single()
        val commonFile = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
        val commonFunctions = commonFile.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name }
        val specificFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val specificFunctions = specificFile.declarations.filterIsInstance<CfirNamedFunction>()
        assertTrue(specificFile.moduleData.session.isCjmpSpecificCompilationEnabled())
        for (specific in specificFunctions) {
            assertTrue(commonFunctions.getValue(specific.name).returnTypeRef is CfirImplicitTypeRef)
            assertTrue(specific.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        }
        if (parallel) resolveTogether(specificFunctions) else specificFunctions.forEach {
            it.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        }
        for (specific in specificFunctions) {
            val common = commonFunctions.getValue(specific.name)
            val family = specific.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(specific)
            assertSame(common, family.commonFor(specific))
            assertEquals(ConePrimitiveType.INT64, common.returnTypeRef.coneTypeOrNull)
            assertTrue(common.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
            assertEquals(CfirResolvePhase.CJMP_MATCHING, specific.resolvePhase)
            assertTrue(specific.body is CfirLazyBlock, "matching must not resolve the specific body")
        }
    }

    private fun assertDisabledPhase(mainFile: CjFile, testServices: TestServices) {
        assertSessionCompilationModes(testServices)
        val file = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        assertFalse(file.moduleData.session.isCjmpSpecificCompilationEnabled())
        val declarations = file.declarations.filterIsInstance<CfirMemberDeclaration>().flatMap {
            if (it is CfirEnum) listOf(it) + it.declarations.filterIsInstance<CfirMemberDeclaration>() else listOf(it)
        }
        assertTrue(declarations.any { it is CfirNamedFunction })
        assertTrue(declarations.any { it is CfirEnum })
        for (declaration in declarations) {
            declaration.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            assertEquals(CfirResolvePhase.CJMP_MATCHING, declaration.resolvePhase)
            assertNull(declaration.cjmpMatchFacts)
        }
    }

    private fun resolveTogether(functions: List<CfirNamedFunction>) {
        val ready = CountDownLatch(functions.size)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(functions.size)
        try {
            val futures = functions.map { function ->
                executor.submit {
                    ready.countDown()
                    check(start.await(30, TimeUnit.SECONDS))
                    ApplicationManager.getApplication().runReadAction {
                        function.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
                    }
                }
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS))
            start.countDown()
            futures.forEach { it.get(60, TimeUnit.SECONDS) }
        } finally {
            start.countDown()
            executor.shutdownNow()
            check(executor.awaitTermination(30, TimeUnit.SECONDS))
        }
    }

    private fun awaitSingleFlightWait(thread: Thread) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (System.nanoTime() < deadline) {
            if (thread.stackTrace.any { frame ->
                    frame.className.contains("CfirCjmpSingleFlightCache") && frame.methodName == "await"
                }
            ) return
            Thread.yield()
        }
        error("Concurrent group lookup did not wait on the shared computation: ${thread.stackTrace.toList()}")
    }
}
