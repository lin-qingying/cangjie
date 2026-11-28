package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.DeclKind
import PackageFormat.ExprKind
import PackageFormat.FuncInfo
import PackageFormat.Package
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.bindingOccurrences
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.source.CfirSourceGraphProducer
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.*
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.messages.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.realElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 真实 receiver 的 BODY→SOURCE→CJO→普通材料化回归；两 parser 共用同一语言合同。
 * 复用 compiler/frontend 的 executePhase/普通 CJO 写出及正式 Library decoder 装配。
 * 固定 K/N 与真实 cjc 证据见 receiverSourceLifecycle/FIXTURE.md，不手造初始化 Info。
 * 正例的零错误期待来自真实 CJ；hostSema 拒绝正例时应暴露失败，不能改期待或导出错误正文。
 */
@OptIn(CompilerConfiguration.Internals::class, CfirImplementationDetail::class)
class CfirReceiverSourceLifecycleTest {
    @TempDir lateinit var tempDir: Path

    @Test fun psiReceiverRead() = valid("read", false)
    @Test fun lightTreeReceiverRead() = valid("read", true)
    @Test fun psiReceiverCall() = valid("call", false)
    @Test fun lightTreeReceiverCall() = valid("call", true)
    @Test fun psiWriteRhsBeforeAddress() = valid("write_target", false)
    @Test fun lightTreeWriteRhsBeforeAddress() = valid("write_target", true)
    @Test fun psiCompoundAddressOnce() = valid("compound_once", false)
    @Test fun lightTreeCompoundAddressOnce() = valid("compound_once", true)
    @Test fun psiNestedOtherObjectIsolation() = valid("nested_other", false)
    @Test fun lightTreeNestedOtherObjectIsolation() = valid("nested_other", true)
    @Test fun psiFinalizerSuperField() = valid("finalizer_field", false)
    @Test fun lightTreeFinalizerSuperField() = valid("finalizer_field", true)
    @Test fun psiNestedFinalizerSuperField() = valid("finalizer_nested_field", false)
    @Test fun lightTreeNestedFinalizerSuperField() = valid("finalizer_nested_field", true)
    @Test fun psiNestedCurrentObjectIsForbidden() = invalid("nested_this_invalid", false)
    @Test fun lightTreeNestedCurrentObjectIsForbidden() = invalid("nested_this_invalid", true)
    @Test fun psiFinalizerFunctionIsForbidden() = invalid("finalizer_function_invalid", false)
    @Test fun lightTreeFinalizerFunctionIsForbidden() = invalid("finalizer_function_invalid", true)
    @Test fun psiFinalizerPropertyIsForbidden() = invalid("finalizer_property_invalid", false)
    @Test fun lightTreeFinalizerPropertyIsForbidden() = invalid("finalizer_property_invalid", true)
    @Test fun psiFinalizerBareThisIsForbidden() = invalid("finalizer_this_invalid", false)
    @Test fun lightTreeFinalizerBareThisIsForbidden() = invalid("finalizer_this_invalid", true)

    @Test fun psiInvalidConditionalWriteTarget() = invalid("write_rhs_first", false)
    @Test fun lightTreeInvalidConditionalWriteTarget() = invalid("write_rhs_first", true)
    @Test fun psiInvalidConditionalCompoundTarget() = invalid("compound", false)
    @Test fun lightTreeInvalidConditionalCompoundTarget() = invalid("compound", true)
    @Test fun psiRhsReadBeforeReceiverInitialization() = invalid("write_rhs_read_invalid", false)
    @Test fun lightTreeRhsReadBeforeReceiverInitialization() = invalid("write_rhs_read_invalid", true)

    /** BODY 先完成且零错误，再要求正式 frontend 写出的真实非零 SOURCE；失败不继续导出。 */
    private fun valid(id: String, lightTree: Boolean) {
        // 原普通输入保留为 BODY 回归；只有独立实编译的 @Frozen SOURCE 输入申请正文写出。
        body(id, lightTree, null) { files, _ -> assertBodyReceiverIdentity(id, nodes(files)) }
        val sourcePath = ROOT.resolve(SOURCE_INPUTS.getValue(id))
        val output = Files.createDirectories(tempDir.resolve("$id-$lightTree-cjo"))
        body(id, lightTree, output, sourcePath) { files, session ->
            assertEquals(CfirSession.Kind.Source, session.kind)
            files.forEach { file ->
                assertSame(session, file.moduleData.session)
                assertEquals(Files.readString(sourcePath, UTF_8), checkNotNull(file.parsingSourceContext).snapshot.text)
            }
            val nodes = nodes(files)
            assertBodyReceiverIdentity(id, nodes)
            val canonical = CfirSourceGraphProducer.produce(files)
            assertCanonical(id, canonical)
            val cjo = output.resolve(CjoConstants.packageNameToPath("receiver_$id"))
            assertTrue(Files.isRegularFile(cjo))
            val wire = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
            assertTrue(wire.allExprsLength > 0, "必须是真实 SOURCE，不接受 header-only")
            val bodies = (0 until wire.allDeclsLength).mapNotNull(wire::allDecls)
                .filter { it.kind == DeclKind.FuncDecl }
                .mapNotNull { (it.info(FuncInfo()) as FuncInfo).funcBody }
                .filter { it.body != 0u }
            assertTrue(bodies.isNotEmpty())
            bodies.forEach { assertEquals(ExprKind.Block, checkNotNull(wire.allExprs(it.body.toInt() - 1)).kind) }
            val selectedNames = when (id) {
                "nested_other" -> setOf("init", "readOther")
                "finalizer_field" -> setOf("~init")
                "finalizer_nested_field" -> setOf("~init", "readParent")
                else -> setOf("probe")
            }
            val selected = (0 until wire.allDeclsLength).mapNotNull(wire::allDecls)
                .filter { it.kind == DeclKind.FuncDecl && it.identifier in selectedNames }
            assertEquals(selectedNames, selected.map { it.identifier }.toSet())
            if (id == "nested_other") assertEquals(2, selected.count { it.identifier == "init" })
            selected.forEach {
                val saved = checkNotNull((it.info(FuncInfo()) as FuncInfo).funcBody)
                assertTrue(saved.body != 0u, "被测 F 必须选中；其他 F 的非零正文不能代替 ${it.identifier}")
                assertEquals(ExprKind.Block, checkNotNull(wire.allExprs(saved.body.toInt() - 1)).kind)
            }
            // 分别以 entry/最后一个 F 为首个请求，验证普通解码不因查询顺序改变对象归属。
            val nativeBytes = java.util.Base64.getDecoder().decode(Files.readString(sourcePath.parent.resolve("official.cjo.base64"), UTF_8).trim())
            val expectedSha = Files.readString(sourcePath.parent.resolve("official.cjo.sha256"), UTF_8).trim()
            val actualSha = java.security.MessageDigest.getInstance("SHA-256").digest(nativeBytes).joinToString("") { "%02X".format(it) }
            assertEquals(expectedSha, actualSha, "必须消费封存的官方 SOURCE 原字节")
            val nativeCjo = tempDir.resolve("$id-$lightTree-official").resolve(CjoConstants.packageNameToPath("receiver_$id"))
            Files.createDirectories(nativeCjo.parent)
            Files.write(nativeCjo, nativeBytes)
            for (reverse in listOf(false, true)) {
                decode(id, cjo, reverse)
                decode(id, nativeCjo, reverse)
            }
        }
    }

    /** 非法语义样例只有 BODY 正式诊断，没有输出配置，也不调用 SOURCE producer/encoder。 */
    private fun invalid(id: String, lightTree: Boolean) = body(id, lightTree, null,
        if (id == "write_rhs_read_invalid") ROOT.resolve("negative-2/$id/source.cj") else ROOT.resolve("$id/source.cj")) { _, _ -> }

    private fun body(id: String, lightTree: Boolean, output: Path?, sourcePath: Path = ROOT.resolve("$id/source.cj"), inspect: (List<CfirFile>, CfirSession) -> Unit) {
        val disposable = Disposer.newDisposable("r23b-$id-$lightTree")
        try {
            val diagnostics = DiagnosticsCollectorImpl()
            val messages = RecordingMessageCollector()
            val configuration = CompilerConfiguration().apply {
                moduleName = "receiver_$id"
                messageCollector = messages
                diagnosticsCollector = diagnostics
                useLightTree = lightTree
                assertTrue(configureLanguageVersionSettings("1.1.3"))
                addCangJieSourceRoot(sourcePath.toString())
                if (output != null) cjoOutputDirectory = output.toString()
            }
            val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            val actual = diagnostics.diagnostics.filter { it.severity.isError }
                .map { it.factoryName to it.renderMessage().trimEnd('.') }.sortedBy { it.toString() }
            val expected = ERRORS[id].orEmpty().sortedBy { it.toString() }
            if (id in PARSER_ERRORS.keys) {
                assertTrue(actual.isEmpty(), "语法失败不得伪造 CFIR Sema 诊断")
                val reported = messages.errorRecords
                assertEquals(expected.map { it.second }, reported.map { it.first })
                val range = PARSER_ERRORS.getValue(id)
                assertEquals(listOf(range), reported.map {
                    val location = it.second as CompilerMessageLocationWithRange
                    assertEquals(sourcePath.toAbsolutePath().normalize(), Path.of(location.path).toAbsolutePath().normalize())
                    listOf(location.line, location.column, location.lineEnd, location.columnEnd)
                })
                assertNull(artifact)
                assertTrue(messages.hasErrors())
                assertNull(output)
                return
            }
            assertEquals(expected, actual, "真实 cjc 错误多重集，额外诊断不能过滤；messages=${messages.messages}")
            if (id == "write_rhs_read_invalid") {
                val text = Files.readString(sourcePath, UTF_8)
                val start = text.indexOf("= x\n") + 2
                assertTrue(start >= 2)
                val diagnostic = diagnostics.diagnostics.single { it.severity.isError } as CjDiagnosticWithSource
                assertEquals(TextRange(start, start + 1), diagnostic.firstRange, "真实 RHS x @6:72–73")
            }
            assertEquals(expected.isNotEmpty(), CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration))
            if (expected.isEmpty()) {
                assertFalse(messages.hasErrors(), messages.messages.toString())
                val result = checkNotNull(artifact) { messages.messages.toString() }.frontendOutput.outputs.single()
                val file = result.fir.single()
                assertEquals(sourcePath.toAbsolutePath().normalize(), Path.of(checkNotNull(file.sourceFile).path).toAbsolutePath().normalize())
                val parserContext = checkNotNull(file.parsingSourceContext)
                assertEquals(Files.readString(sourcePath, UTF_8), parserContext.snapshot.text)
                assertEquals(0, parserContext.inputStart)
                assertEquals(parserContext.snapshot.text.length, parserContext.inputEnd)
                assertSame(result.session, file.moduleData.session)
                inspect(result.fir, result.session)
            } else {
                assertNull(output)
            }
        } finally { Disposer.dispose(disposable) }
    }

    /** 只读 visitor 的 identity 集用于核对共享节点，不参与任何 Sema 或 flow 推进。 */
    private fun nodes(roots: Collection<CfirElement>): List<CfirElement> {
        val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
        val result = mutableListOf<CfirElement>()
        val visitor = object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (!seen.add(element)) return
                result += element
                element.acceptSourceChildren(this, null)
            }
        }
        roots.forEach { it.accept(visitor) }
        return result
    }

    private fun assertBodyReceiverIdentity(id: String, nodes: List<CfirElement>, fromBody: Boolean = true) {
        val accesses = nodes.filterIsInstance<CfirQualifiedAccessExpression>()
        if (id in setOf("read", "call", "write_target", "compound_once")) {
            val name = if (id == "call") "use" else "value"
            val accessesToMember = accesses.filter { (it.calleeReference as? CfirResolvedNamedReference)?.name?.asString() == name }
            assertTrue(accessesToMember.isNotEmpty())
            accessesToMember.forEach {
                assertSame(checkNotNull(it.explicitReceiver), it.dispatchReceiver, "已选对象只能由同一次求值发布")
                val fieldOwner = (it.calleeReference as CfirResolvedNamedReference).resolvedSymbol.cfir
                assertTrue(fieldOwner is CfirCallableDeclaration)
            }
            val x = nodes.filterIsInstance<CfirVariable>().single { it.symbol.name.asString() == "x" }
            assertFalse(x.isVar)
            if (fromBody) assertBindingSourceIdentity(nodes, x)
            else {
                assertEquals(CfirSession.Kind.Library, x.moduleData.session.kind)
                assertNotNull(x.cjoDeclarationOrigin, "native VarDecl 保留正式原CJO来源，不能伪装成PSI变量")
            }
            val writes = nodes.filterIsInstance<CfirAssignment>().filter {
                ((it.lValue as? CfirQualifiedAccessExpression)?.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol === x.symbol
            }
            assertEquals(2, writes.size, "then/else 对同一个不可变 local 各初始化一次")
        }
        if (id == "nested_other") {
            val box = nodes.filterIsInstance<CfirClass>().single { it.name.asString() == "Box" }
            val other = nodes.filterIsInstance<CfirValueParameter>().single { it.name.asString() == "other" }
            val field = box.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "value" }
            val access = accesses.single { (it.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol === field.symbol && it.explicitReceiver != null }
            assertSame(access.explicitReceiver, access.dispatchReceiver)
            val receiver = access.dispatchReceiver as CfirQualifiedAccessExpression
            assertSame(other.symbol, (receiver.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
            assertFalse(receiver is CfirSuperReceiverExpression)
            if (fromBody) {
                val info = checkNotNull(box.classInitializationInfo(box.moduleData.session))
                assertSame(box, info.owner)
                assertEquals(CfirClassInitializationOrigin.SOURCE, info.origin)
                assertTrue(info.isCurrent())
                assertTrue(info.fieldState(field.symbol).definitelyInitialized)
            }
        }
        if (id.startsWith("finalizer_")) {
            val child = nodes.filterIsInstance<CfirClass>().single { it.name.asString() == "Child" }
            val parent = nodes.filterIsInstance<CfirClass>().single { it.name.asString() == "Parent" }
            val field = parent.declarations.filterIsInstance<CfirFieldVariable>().single { it.name.asString() == "value" }
            val superNode = nodes.filterIsInstance<CfirSuperReceiverExpression>().single()
            val physical = superNode.dispatchReceiver as CfirInaccessibleReceiverExpression
            assertEquals(InaccessibleReceiverKind.FINALIZER, physical.kind)
            assertSame(child.symbol, physical.calleeReference.boundSymbol)
            assertEquals(parent.symbol.classId, (superNode.coneTypeOrNull as ConeClassLikeType).lookupTag.classId)
            assertTrue(accesses.any { (it.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol === field.symbol })
        }
    }

    /** 原声明容器是真实源；scope binding 是同一name锚点的派生视图，不是另一条源码声明。 */
    private fun assertBindingSourceIdentity(nodes: List<CfirElement>, variable: CfirVariable) {
        assertTrue(variable is CfirPatternBindingVariable)
        val probe = nodes.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "probe" }
        val container = checkNotNull(probe.body).statements.first() as CfirPatternVariable
        val occurrence = container.pattern.bindingOccurrences().single { it.variable === variable }
        val declarationSource = checkNotNull(container.source)
        val nameSource = checkNotNull(occurrence.source)
        val bindingSource = checkNotNull(variable.source)
        assertSame(CjRealSourceElementKind, declarationSource.kind)
        assertSame(CjRealSourceElementKind, nameSource.kind)
        assertSame(CjFakeSourceElementKind.PatternBindingVariable, bindingSource.kind)
        assertEquals(CfirDeclarationOrigin.Source, variable.origin)
        assertEquals("x", occurrence.name.asString())
        assertFalse(container.isVar)
        assertEquals("let x: Int64", sourceText(declarationSource))
        assertEquals("x", sourceText(nameSource))
        assertTrue(nameSource.startOffset >= declarationSource.startOffset && nameSource.endOffset <= declarationSource.endOffset)
        assertEquals(nameSource.startOffset, bindingSource.startOffset)
        assertEquals(nameSource.endOffset, bindingSource.endOffset)
        assertEquals("x", sourceText(bindingSource))
        assertSameSourceAnchor(nameSource, bindingSource)
        assertSameSourceAnchor(nameSource, bindingSource.realElement())
        assertSame(CjRealSourceElementKind, bindingSource.realElement().kind)
    }

    private fun sourceText(source: CjSourceElement): String = when (source) {
        is CjPsiSourceElement -> source.psi.text
        is CjLightSourceElement -> source.treeStructure.toString(source.lighterASTNode).toString()
        else -> error("Expected a real parser source anchor")
    }

    private fun assertSameSourceAnchor(expected: CjSourceElement, actual: CjSourceElement) {
        if (expected is CjPsiSourceElement && actual is CjPsiSourceElement) assertSame(expected.psi, actual.psi)
        else {
            assertTrue(expected is CjLightSourceElement && actual is CjLightSourceElement)
            assertSame(expected.lighterASTNode, actual.lighterASTNode)
            assertSame(expected.treeStructure, actual.treeStructure)
        }
    }

    /** 使用现有 CJO Library harness 的普通 provider；输入唯一来自上面的正式 SOURCE 写出。 */
    private fun decode(id: String, cjo: Path, reverse: Boolean) {
        val module = LibraryModule()
        val manager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(cjo.parent.toFile()), envProvider = {
            if (it == "CANGJIE_STDLIB_MODULE") checkNotNull(System.getenv(it)) else null
        }),
            CjoFormatVersionPolicy.Compilation(LanguageVersionSettingsImpl.DEFAULT))
        val provider = CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module)
        module.session.register(CfirSymbolProvider::class, provider)
        assertEquals(CfirSession.Kind.Library, module.session.kind)
        val wire = checkNotNull(manager.loadPackage("receiver_$id"))
        val packageName = FqName("receiver_$id")
        // 与正式消费端相同，通过公开 symbol 查询请求已加载声明，不调用跨模块内部反序列化入口。
        val declarations = (0 until wire.allDeclsLength).map { checkNotNull(wire.allDecls(it)) }
            .filter { it.isTopLevel }.map { declaration ->
                val name = Name.identifier(checkNotNull(declaration.identifier))
                when (declaration.kind) {
                    DeclKind.ClassDecl -> checkNotNull(provider.getClassLikeSymbolByClassId(ClassId(packageName, name))).cfir
                    DeclKind.FuncDecl -> provider.getTopLevelFunctionSymbols(packageName, name).single().cfir
                    else -> error("Unexpected fixture root kind: ${declaration.kind}")
                }
            }
        val first = declarations.filterIsInstance<CfirNamedFunction>().singleOrNull { it.name.asString() == "probe" }
            ?: declarations.filterIsInstance<CfirClass>().single { it.name.asString() == if (id == "nested_other") "Box" else "Child" }
        val entryFunctions = if (first is CfirFunction) listOf(first) else (first as CfirClass).declarations.filterIsInstance<CfirFunction>()
        val storedEntryFunctions = entryFunctions.filter { checkNotNull(provider.sourceHeaderOf(it)).bodyAvailable }
        assertTrue(storedEntryFunctions.isNotEmpty(), "首个 selected 请求必须来自实际保存的非零 BODY")
        val firstFunction = if (reverse) storedEntryFunctions.last() else storedEntryFunctions.first()
        // 首个 F 仍走正式 selected 请求；整包 oracle 必须显式申请 package 产品。
        val selected = checkNotNull(provider.sourceGraphOf(firstFunction))
        assertSame(selected, provider.sourceGraphOf(firstFunction))
        val selectedRoot = selected.functionBodies.getValue(firstFunction)
        val graph = checkNotNull(provider.packageSourceGraphOf(first))
        assertSame(graph, provider.packageSourceGraphOf(first))
        assertSame(selectedRoot, graph.functionBodies.getValue(firstFunction))
        assertSame(selected.expression(selectedRoot), graph.expression(selectedRoot))
        assertSame(selected.functionHeaders.getValue(firstFunction), graph.functionHeaders.getValue(firstFunction))
        assertCanonical(id, graph, CfirSourceReferenceKind.CURRENT)
        val functions = graph.functionBodies.keys.filterIsInstance<CfirFunction>().let { if (reverse) it.reversed() else it }
        assertTrue(functions.isNotEmpty())
        val unchangedCanonical = captureCanonicalView(graph)
        val completedBodies = IdentityHashMap<CfirFunction, CfirBlock>()
        val aliases = IdentityHashMap<CfirElement, CfirSourceExpressionIdentity>()
        val views = mutableListOf<Pair<CfirSourceGraph, Map<CfirElement, CfirSourceExpressionIdentity>>>()
        for (function in functions) {
            val selectedBefore = checkNotNull(provider.sourceGraphOf(function))
            val before = checkNotNull(provider.packageSourceGraphOf(first))
            unchangedCanonical(before)
            val root = graph.functionBodies.getValue(function)
            assertSame(root, selectedBefore.functionBodies.getValue(function))
            assertSame(graph.expression(root), selectedBefore.expression(root))
            val header = checkNotNull(provider.sourceHeaderOf(function))
            assertTrue(header.bodyAvailable)
            assertSame(graph.functionHeaders.getValue(function), header)
            assertSame(header, selectedBefore.functionHeaders.getValue(function))
            // 只读被动 body 快照：嵌套 F 可能已经在此前 owning 请求中完成。
            val bodiesBefore = functions.associateWith { it.body }
            functions.forEach { f ->
                bodiesBefore.getValue(f)?.let { body ->
                    assertTrue(before.containsOrigin(body))
                    val expected = graph.functionBodies.getValue(f)
                    assertSame(expected, before.expressionOf(body))
                    aliases[body] = expected
                }
            }
            views += before to IdentityHashMap(aliases)
            val selectedHadBody = bodiesBefore.getValue(function)?.let(selectedBefore::containsOrigin)
            val body = checkNotNull(provider.sourceBodyOf(function))
            val after = checkNotNull(provider.packageSourceGraphOf(first))
            val selectedAfter = checkNotNull(provider.sourceGraphOf(function))
            unchangedCanonical(before)
            unchangedCanonical(after)
            assertSame(root, selectedAfter.functionBodies.getValue(function))
            assertSame(graph.expression(root), selectedAfter.expression(root))
            assertSame(header, selectedAfter.functionHeaders.getValue(function))
            assertTrue(selectedAfter.containsOrigin(body))
            assertSame(root, selectedAfter.expressionOf(body))
            if (selectedHadBody != true) {
                assertNotSame(selectedBefore, selectedAfter)
                assertFalse(selectedBefore.containsOrigin(body))
            } else {
                assertTrue(selectedBefore.containsOrigin(body))
                assertSame(root, selectedBefore.expressionOf(body))
            }
            var newBodyAlias = false
            functions.forEach { f ->
                f.body?.let { current ->
                    val expected = graph.functionBodies.getValue(f)
                    val previous = bodiesBefore.getValue(f)
                    if (previous == null) {
                        newBodyAlias = true
                        assertFalse(before.containsOrigin(current), "旧 package view 不得增加新 body alias")
                    } else {
                        assertSame(previous, current)
                    }
                    assertTrue(after.containsOrigin(current))
                    assertSame(expected, after.expressionOf(current))
                    assertSame(graph.expression(expected).type, current.coneTypeOrNull)
                    aliases[current] = expected
                    completedBodies[f] = current
                }
            }
            if (newBodyAlias) assertNotSame(before, after)
            // 每代视图只认识当时已发布的 root aliases；新发布不得回写旧代。
            views.forEach { (view, known) ->
                unchangedCanonical(view)
                aliases.forEach { (origin, expected) ->
                    assertEquals(known.containsKey(origin), view.containsOrigin(origin))
                    if (known.containsKey(origin)) assertSame(expected, view.expressionOf(origin))
                }
            }
            assertSame(header, provider.sourceHeaderOf(function))
            assertSame(body, provider.sourceBodyOf(function))
            assertSame(module.session, function.moduleData.session)
            assertSame(body, function.body)
            assertSame(after, provider.packageSourceGraphOf(first))
            assertSame(selectedAfter, provider.sourceGraphOf(function))
        }
        val materialized = nodes(declarations)
        assertBodyReceiverIdentity(id, materialized, fromBody = false)
        val finalView = checkNotNull(provider.packageSourceGraphOf(first))
        unchangedCanonical(finalView)
        assertCanonical(id, finalView, CfirSourceReferenceKind.CURRENT)
        completedBodies.forEach { (function, body) ->
            assertSame(body, provider.sourceBodyOf(function))
            assertSame(body, function.body)
            assertSame(graph.functionHeaders.getValue(function), provider.sourceHeaderOf(function))
        }
        assertSame(finalView, provider.packageSourceGraphOf(first))
    }

    /** 纯 alias 换代共享全部 canonical 容器；快照独立保存集合内容以发现旧产品污染。 */
    private fun captureCanonicalView(graph: CfirSourceGraph): (CfirSourceGraph) -> Unit {
        fun maps(view: CfirSourceGraph): List<Map<*, *>> = listOf(
            view.expressions, view.borrowedExpressions, view.functionBodies, view.functionHeaders,
            view.initializers, view.defaults, view.declarationPatterns, view.generatedDeclarations,
            view.annotationPlans, view.inheritedTypeReferences, view.declarationOwners,
            view.functionContexts, view.annotations, view.constantValues,
        )
        val originalMaps = maps(graph)
        val entries = originalMaps.map { map -> map.entries.map { it.key to it.value } }
        val unloaded = graph.unloadedFunctionBodies.toList()
        val listChecks = mutableListOf<() -> Unit>()
        fun captureList(list: List<*>) {
            val saved = list.toList()
            listChecks += {
                assertEquals(saved.size, list.size)
                saved.indices.forEach { assertSame(saved[it], list[it]) }
            }
            list.filterIsInstance<List<*>>().forEach(::captureList)
        }
        fun capturePattern(pattern: CfirSourcePattern) {
            captureList(pattern.types)
            captureList(pattern.slots)
            captureList(pattern.patterns)
            captureList(pattern.typeReferences)
            captureList(pattern.constantValues)
            pattern.patterns.forEach(::capturePattern)
            listChecks += { assertSame(pattern, graph.pattern(pattern.identity)) }
        }
        entries.forEach { items -> items.forEach { (_, value) -> if (value is List<*>) captureList(value) } }
        graph.functionHeaders.values.forEach {
            captureList(it.parameterLists)
            captureList(it.desugaredParameterLists)
        }
        graph.annotations.values.flatten().forEach { captureList(it.arguments) }
        graph.declarationPatterns.values.forEach(::capturePattern)
        // 这里只捕获公开 canonical 集合，不遍历或改写普通 CFIR 子树。
        (graph.expressions.values + graph.borrowedExpressions.values).forEach { record ->
            when (val payload = record.payload) {
                is CfirSourceExpressionPayload.Reference -> captureList(payload.instantiatedTypes)
                is CfirSourceExpressionPayload.Member -> captureList(payload.reference.instantiatedTypes)
                is CfirSourceExpressionPayload.Call -> captureList(payload.arguments)
                is CfirSourceExpressionPayload.Subscript -> captureList(payload.indices)
                is CfirSourceExpressionPayload.Array -> captureList(payload.arguments)
                is CfirSourceExpressionPayload.ArrayLiteral -> captureList(payload.elements)
                is CfirSourceExpressionPayload.Tuple -> captureList(payload.elements)
                is CfirSourceExpressionPayload.Try -> {
                    captureList(payload.catches)
                    captureList(payload.patterns)
                    captureList(payload.resources)
                    payload.patterns.forEach(::capturePattern)
                }
                is CfirSourceExpressionPayload.Lambda -> captureList(payload.function.parameters)
                is CfirSourceExpressionPayload.Block -> captureList(payload.statements)
                is CfirSourceExpressionPayload.Match -> captureList(payload.cases)
                is CfirSourceExpressionPayload.LetPatternDestructor -> {
                    captureList(payload.patterns)
                    payload.patterns.forEach(::capturePattern)
                }
                is CfirSourceExpressionPayload.ForIn -> capturePattern(payload.pattern)
                is CfirSourceExpressionPayload.MatchCase -> {
                    captureList(payload.patterns)
                    payload.patterns.forEach(::capturePattern)
                }
                else -> Unit // 其它载荷没有集合字段；原 record/payload 身份仍逐项检查。
            }
        }
        return { view ->
            maps(view).forEachIndexed { index, map ->
                assertSame(originalMaps[index], map)
                assertEquals(entries[index].size, map.size)
                entries[index].forEach { (key, value) ->
                    assertTrue(map.containsKey(key))
                    assertSame(value, map[key])
                }
            }
            assertSame(graph.unloadedFunctionBodies, view.unloadedFunctionBodies)
            assertEquals(unloaded.toSet(), view.unloadedFunctionBodies)
            (graph.expressions + graph.borrowedExpressions).forEach { (identity, record) ->
                val current = view.expression(identity)
                assertSame(record, current)
                assertSame(record.type, current.type)
                assertSame(record.context, current.context)
                assertSame(record.payload, current.payload)
                assertSame(record.span, current.span)
                assertSame(record.importedPosition, current.importedPosition)
                assertSame(record.mapExpression, current.mapExpression)
            }
            graph.declarationPatterns.values.forEach { assertSame(it, view.pattern(it.identity)) }
            listChecks.forEach { it() }
        }
    }

    /** SOURCE 的 kind、同图 identity、local target 与 owning/lexical 均消费 decoder 正式产品。 */
    private fun assertCanonical(id: String, graph: CfirSourceGraph, targetKind: CfirSourceReferenceKind = CfirSourceReferenceKind.SOURCE) {
        assertTrue(graph.expressions.isNotEmpty())
        graph.expressions.forEach { (identity, record) -> assertSame(record, graph.expression(identity)) }
        val members = graph.expressions.values.mapNotNull { it.payload as? CfirSourceExpressionPayload.Member }
        members.forEach {
            val target = (it.reference.target as CfirSourceReferenceTarget.Declaration).declaration
            val local = graph.declarationOwners.containsKey(target)
            if (local) {
                assertEquals(targetKind, it.reference.targetKind)
                val owner = graph.declarationOwners.getValue(target) as CfirClass
                val expectedOwner = when (id) {
                    "nested_other" -> "Box"
                    "finalizer_field", "finalizer_nested_field" -> "Parent"
                    else -> "Cell"
                }
                assertEquals(expectedOwner, owner.name.asString(), "CURRENT Member 必须绑定实际同包 owner")
            } else {
                // SOURCE producer 保存绑定声明；普通 decoder 的 IMPORTED 身份来自真实 CJO provenance。
                assertNotNull(target.cjoDeclarationOrigin, "外部 Member 必须有正式库绑定来源")
                assertEquals(if (targetKind == CfirSourceReferenceKind.SOURCE) targetKind else CfirSourceReferenceKind.IMPORTED,
                    it.reference.targetKind)
            }
            assertNotNull(graph.expression(it.receiver))
        }
        if (id in setOf("read", "call", "write_target", "compound_once")) {
            val probe = graph.functionBodies.keys.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "probe" }
            val block = graph.expression(graph.functionBodies.getValue(probe)).payload as CfirSourceExpressionPayload.Block
            val x = (block.statements.first() as CfirSourceSlot.Declaration).declaration as CfirVariable
            assertEquals("x", x.symbol.name.asString())
            if (targetKind == CfirSourceReferenceKind.SOURCE) {
                // SOURCE 保留原 CFIR binding→容器→F；规范化的 Block D 槽不改变实际拥有边。
                val container = graph.declarationOwners.getValue(x) as CfirPatternVariable
                assertTrue(container.pattern.bindingOccurrences().any { it.variable === x })
                assertSame(probe, graph.declarationOwners.getValue(container))
                assertSame(checkNotNull(probe.body).statements.first(), container)
            } else {
                // plain CJO 的 D 是普通 VarDecl，没有本地额外 binding 容器。
                assertTrue(x is CfirFieldVariable)
                assertSame(probe, graph.declarationOwners.getValue(x))
            }
            val assignments = graph.expressions.values.mapNotNull { it.payload as? CfirSourceExpressionPayload.Assignment }
            val xWrites = assignments.filter {
                val ref = graph.expression(it.left).payload as? CfirSourceExpressionPayload.Reference
                (ref?.target as? CfirSourceReferenceTarget.Declaration)?.declaration === x
            }
            assertEquals(2, xWrites.size)
            assertTrue(graph.expressions.values.any { it.payload is CfirSourceExpressionPayload.Conditional })
            if (id == "compound_once") assertEquals(1, assignments.count { it.isCompound })
        }
        if (id == "nested_other") {
            val read = graph.functionBodies.keys.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "readOther" }
            val ctor = graph.declarationOwners.getValue(read) as CfirConstructor
            val other = ctor.valueParameters.single { it.name.asString() == "other" }
            assertSame(ctor, graph.functionContexts.getValue(read).outerFunction)
            val member = members.single { it.reference.name.asString() == "value" }
            val receiver = graph.expression(member.receiver).payload as CfirSourceExpressionPayload.Reference
            assertEquals(CfirSourceReferenceRole.ORDINARY, receiver.role)
            assertSame(other, (receiver.target as CfirSourceReferenceTarget.Declaration).declaration)
        }
        if (id.startsWith("finalizer_")) {
            val member = members.single { it.reference.name.asString() == "value" }
            val field = (member.reference.target as CfirSourceReferenceTarget.Declaration).declaration
            val parent = graph.declarationOwners.getValue(field) as CfirClass
            assertEquals("Parent", parent.name.asString())
            val receiver = graph.expression(member.receiver)
            val ref = receiver.payload as CfirSourceExpressionPayload.Reference
            assertEquals(CfirSourceReferenceRole.SUPER, ref.role)
            assertEquals(parent.symbol.classId, (receiver.type as ConeClassLikeType).lookupTag.classId)
            val finalizer = graph.functionBodies.keys.filterIsInstance<CfirFinalizer>().single()
            val child = graph.declarationOwners.getValue(finalizer) as CfirClass
            assertEquals("Child", child.name.asString())
            if (id == "finalizer_nested_field") {
                val nested = graph.functionBodies.keys.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "readParent" }
                assertSame(finalizer, graph.declarationOwners.getValue(nested))
                assertSame(finalizer, graph.functionContexts.getValue(nested).outerFunction)
            }
        }
    }

    private class RecordingMessageCollector : MessageCollector {
        val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        val errorRecords = mutableListOf<Pair<String, CompilerMessageSourceLocation?>>()
        override fun clear() { messages.clear(); errorRecords.clear() }
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            messages += severity to message
            if (severity.isError) errorRecords += message to location
        }
        override fun hasErrors(): Boolean = messages.any { it.first.isError }
    }

    private class LibraryModule : CfirModuleData() {
        override val name = Name.identifier("r23b-library")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform = CfirPlatform.DEFAULT
        override val isCommon = false
        override val stableModuleName = "r23b-library"
        override val session = object : CfirSession(Kind.Library) {}.also {
            it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
            it.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }
        init { bindSession(session) }
    }

    private companion object {
        val ROOT: Path = Path.of(checkNotNull(CfirReceiverSourceLifecycleTest::class.java.getResource("/receiverSourceLifecycle")).toURI())
        val SOURCE_INPUTS = mapOf(
            "read" to "source-selected/read/source.cj",
            "call" to "source-selected/call/source.cj",
            "write_target" to "source-selected/write_target/source.cj",
            "compound_once" to "source-selected/compound_once/source.cj",
            "nested_other" to "source-selected-2/nested_other/source.cj",
            "finalizer_field" to "source-selected-2/finalizer_field/source.cj",
            "finalizer_nested_field" to "source-selected-2/finalizer_nested_field/source.cj",
        )
        val PARSER_ERRORS = mapOf(
            "write_rhs_first" to listOf(5, 52, 5, 53),
            "compound" to listOf(5, 64, 5, 66),
        )
        val ERRORS = mapOf(
            "write_rhs_first" to listOf("parse_invalid_left_hand_expr" to "invalid left-hand expression of assignment '='"),
            "compound" to listOf("parse_invalid_left_hand_expr" to "invalid left-hand expression of assignment '+='"),
            "write_rhs_read_invalid" to listOf("CFIR_USED_BEFORE_INITIALIZATION" to "Variable 'x' is used before initialization"),
            "nested_this_invalid" to listOf("CFIR_ILLEGAL_USAGE_OF_MEMBER" to "'value' is not allowed to be accessed before all member variables are initialized"),
            "finalizer_this_invalid" to listOf("CFIR_THIS_AS_EXPRESSION_IN_FUNC" to "'this' cannot be used as an expression in the finalizer"),
            "finalizer_function_invalid" to listOf(
                "CFIR_INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER" to "instance function cannot be used in the finalizer",
                "CFIR_CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC" to "'use' cannot be captured in the finalizer",
            ),
            "finalizer_property_invalid" to listOf(
                "CFIR_INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER" to "instance property cannot be used in the finalizer",
                "CFIR_CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC" to "'value' cannot be captured in the finalizer",
            ),
        )
    }
}
