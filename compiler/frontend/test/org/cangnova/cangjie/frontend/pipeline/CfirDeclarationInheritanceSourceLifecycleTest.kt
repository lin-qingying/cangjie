package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.DeclKind
import PackageFormat.Package
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRelatedInformation
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters2
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters3
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.entrypoint.configuration.configureCjmpCommonPartInputs
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * 声明继承的真实 frontend/checker 回归，不在测试中构造 session、成员表或诊断。
 *
 * 四份源码原样取自 build/cjmp-invisible-peer-20261125/cjc 的最终非空无调用 fixture。
 * 语义权威为 fixed-n-authority.md：N=14276740ffec1262b81b6b5236b1d840af270b37。
 * 宿主 1.1.3 JSON 只为源码合法性、范围及 note 提供独立证据；其首例遗漏 shadow，
 * 本测试仍期待固定 N 的 return+shadow。property 的两个方向各三项，不过滤额外错误。
 *
 * SOURCE 范围使用原 JSON 与固定 N 的声明定位规则；框架范围/note 不一致也是回归失败，
 * 不能缩小期待、删除真实 body 或只保留某一条诊断。
 *
 * CJO 分割保持未标记的 public peer。固定 ParseCJMPDecl.cpp 的 CheckCJMPDecl 不会
 * 给普通声明补 COMMON；对位 CjmpTwoPhaseCompilationTest 的 ordinary common-part helper。
 * FROM_COMMON_PART 是来源，不是配对角色。给此泛型 peer 添 common 后再用完全实例化的
 * current owner 充当 counterpart 会改变原语义，因此这里不添 common/specific 修饰符。
 */
@OptIn(CompilerConfiguration.Internals::class, CfirImplementationDetail::class)
class CfirDeclarationInheritanceSourceLifecycleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test fun psiSourceMappedReturnReportsReturnAndShadow() = assertSource(Fixture.RETURN, false)
    @Test fun lightTreeSourceMappedReturnReportsReturnAndShadow() = assertSource(Fixture.RETURN, true)
    @Test fun psiSourceCompatibleReturnStillReportsShadow() = assertSource(Fixture.CONTROL, false)
    @Test fun lightTreeSourceCompatibleReturnStillReportsShadow() = assertSource(Fixture.CONTROL, true)
    @Test fun psiSourceInvisiblePeerCannotImplementRequirement() = assertSource(Fixture.REQUIREMENT, false)
    @Test fun lightTreeSourceInvisiblePeerCannotImplementRequirement() = assertSource(Fixture.REQUIREMENT, true)
    @Test fun psiSourcePropertiesReportBothDirectionsAndAllSixErrors() = assertSource(Fixture.PROPERTY, false)
    @Test fun lightTreeSourcePropertiesReportBothDirectionsAndAllSixErrors() = assertSource(Fixture.PROPERTY, true)

    @Test fun psiCommonCjoMappedReturnReportsReturnAndShadow() = assertCommonCjo(Fixture.RETURN, false)
    @Test fun lightTreeCommonCjoMappedReturnReportsReturnAndShadow() = assertCommonCjo(Fixture.RETURN, true)
    @Test fun psiCommonCjoCompatibleReturnStillReportsShadow() = assertCommonCjo(Fixture.CONTROL, false)
    @Test fun lightTreeCommonCjoCompatibleReturnStillReportsShadow() = assertCommonCjo(Fixture.CONTROL, true)
    @Test fun psiCommonCjoInvisiblePeerCannotImplementRequirement() = assertCommonCjo(Fixture.REQUIREMENT, false)
    @Test fun lightTreeCommonCjoInvisiblePeerCannotImplementRequirement() = assertCommonCjo(Fixture.REQUIREMENT, true)
    @Test fun psiCommonCjoPropertiesKeepBinaryAndSourceErrorsSeparate() = assertCommonCjo(Fixture.PROPERTY, false)
    @Test fun lightTreeCommonCjoPropertiesKeepBinaryAndSourceErrorsSeparate() = assertCommonCjo(Fixture.PROPERTY, true)

    private fun assertSource(fixture: Fixture, lightTree: Boolean) = withEnvironment { disposable ->
        assertReturnControlChangesOnlyAnnotation()
        val path = writeSource(fixture, lightTree, "source.cj", fixture.source)
        val result = compile(path, lightTree, disposable)
        val artifact = checkNotNull(result.artifact) { "Source frontend stopped before its diagnostic product: " + result.describe() }
        assertFalse(result.completed, "negative fixture cannot be a successful compilation")
        assertTrue(result.messages.none { it.severity.isError }, result.describe())
        assertDiagnostics(fixture, result, path, path, commonCjo = false)
        val output = artifact.frontendOutput.outputs.single()
        val file = output.fir.single()
        assertSourceProduct(file, path, fixture.source, output.session)
        assertRealBodies(fixture, file, hasCurrent = true)
        assertMappedInput(fixture, artifact, imported = false, cjo = null)
    }

    /** common 阶段只编译完整前缀，specific 保留当前声明原行列及所有 body。 */
    private fun assertCommonCjo(fixture: Fixture, lightTree: Boolean) = withEnvironment { disposable ->
        val split = splitSource(fixture)
        val commonPath = writeSource(fixture, lightTree, "common.cj", split.common)
        val outputDirectory = Files.createDirectories(commonPath.parent.resolve("common-output"))
        val common = compile(commonPath, lightTree, disposable) {
            cjmpChirOutput = true
            cjoOutputDirectory = outputDirectory.toString()
        }
        assertTrue(common.completed, "the actual common Source prefix must compile and write CJO: " + common.describe())
        assertTrue(common.diagnostics.isEmpty(), common.describe())
        val commonArtifact = checkNotNull(common.artifact)
        val commonOutput = commonArtifact.frontendOutput.outputs.single()
        val commonFile = commonOutput.fir.single()
        assertSourceProduct(commonFile, commonPath, split.common, commonOutput.session)
        assertRealBodies(fixture, commonFile, hasCurrent = false)
        val cjo = outputDirectory.resolve(CjoConstants.packageNameToPath(fixture.packageName))
        assertTrue(Files.isRegularFile(cjo), "CJO must be written by the production frontend")
        assertTrue(Files.size(cjo) > 0)
        assertOrdinaryPeerCjo(cjo)

        val specificPath = writeSource(fixture, lightTree, "specific.cj", split.specific)
        val specific = compile(specificPath, lightTree, disposable) {
            // 与已有正式 two-phase helper 相同：这里只走 CFIR，未执行 CHIR 链接或 cjc。
            configureCjmpCommonPartInputs(listOf(cjo.toString()), emptyList())
        }
        val boundaryEvidence = if (fixture == Fixture.PROPERTY) {
            describeCjoPropertyBoundary(cjo, specific).also { System.out.println(it) }
        } else ""
        assertFalse(specific.completed, "the consuming negative fixture cannot compile successfully" +
            if (boundaryEvidence.isEmpty()) "" else "\nCJO_PROPERTY_BOUNDARY_JSON=" + boundaryEvidence)
        assertDiagnostics(fixture, specific, specificPath, commonPath, commonCjo = true, boundaryEvidence = boundaryEvidence)
        if (fixture == Fixture.PROPERTY) {
            // imported peer 的三项诊断经真实 binary-source 消息投影后中止该 frontend 阶段。
            assertNull(specific.artifact)
            val binary = specific.diagnostics.map { it.diagnostic }.filter {
                (it as CjDiagnosticWithSource).element is CjBinarySourceElement
            }
            assertEquals(3, binary.size)
            val messages = specific.messages.filter { it.severity.isError }
            assertEquals(3, messages.size, specific.describe())
            assertEquals(binary.map { it.renderMessage() }.sorted(), messages.map { it.message }.sorted())
            val expectedPoints = fixture.expected.filter { it.side == Side.PEER }
                .map { it.range.begin }.sortedWith(compareBy(Point::line, Point::column))
            assertEquals(expectedPoints, messages.map { message ->
                val location = checkNotNull(message.location)
                assertEquals(normalize(commonPath), normalize(Path.of(location.path)))
                Point(location.line, location.column)
            }.sortedWith(compareBy(Point::line, Point::column)))
        } else {
            assertTrue(specific.messages.none { it.severity.isError }, specific.describe())
            val artifact = checkNotNull(specific.artifact) { specific.describe() }
            val output = artifact.frontendOutput.outputs.single()
            assertSourceProduct(output.fir.single(), specificPath, split.specific, output.session)
            assertCurrentBody(fixture, output.fir.single())
            assertMappedInput(fixture, artifact, imported = true, cjo = cjo)
        }
    }

    /** 期待是完整多重集：不只检查 contains，也不把未知 checker 错误过滤掉。 */
    private fun assertDiagnostics(fixture: Fixture, result: CompileResult, sourcePath: Path, peerPath: Path, commonCjo: Boolean,
        boundaryEvidence: String = "") {
        val description = result.describe() + if (boundaryEvidence.isEmpty()) "" else "\nCJO_PROPERTY_BOUNDARY_JSON=" + boundaryEvidence
        val actual = result.diagnostics.map { record ->
            val diagnostic = record.diagnostic
            assertTrue(diagnostic.severity.isError, description)
            assertTrue(diagnostic.isValid, description)
            val source = assertInstanceOf(CjDiagnosticWithSource::class.java, diagnostic, description).element
            val binary = source as? CjBinarySourceElement
            val range = if (binary == null) {
                assertEquals(normalize(sourcePath), normalize(Path.of(checkNotNull(record.filePath))), description)
                Range(pointAt(fixture.source, diagnostic.firstRange.startOffset), pointAt(fixture.source, diagnostic.firstRange.endOffset))
            } else {
                assertTrue(commonCjo, description)
                val location = checkNotNull(binary.sourceLocation) { "imported diagnostic needs its real CJO source position\n" + description }
                assertEquals(normalize(peerPath), normalize(Path.of(location.filePath)), description)
                assertEquals(0, diagnostic.firstRange.startOffset, description)
                assertEquals(0, diagnostic.firstRange.endOffset, description)
                // Binary source 没有可绘制 end range，只验公开的原始行列，不生成假 offsets。
                Range(Point(location.line, location.column), null)
            }
            val side = if (binary != null || range.begin.line < fixture.currentLine) Side.PEER else Side.CURRENT
            Key(diagnostic.factoryName, side, range)
        }
        val expected = fixture.expected.map { expectation ->
            val range = if (commonCjo && expectation.side == Side.PEER) expectation.range.copy(end = null) else expectation.range
            Key("CFIR_" + expectation.factory, expectation.side, range)
        }
        assertEquals(expected.sortedBy(Key::sortKey), actual.sortedBy(Key::sortKey), description)
        for (expectation in fixture.expected) {
            val index = actual.indexOfFirst { it.factory == "CFIR_" + expectation.factory && it.side == expectation.side }
            val diagnostic = result.diagnostics[index].diagnostic
            assertDiagnosticParameters(fixture, expectation, diagnostic)
            assertNotes(expectation, diagnostic.relatedInformation, fixture.source, sourcePath, peerPath, commonCjo)
            if (commonCjo && expectation.side == Side.PEER && expectation.range.end!!.line != expectation.range.begin.line) {
                val element = assertInstanceOf(CfirCjoDeclarationSourceElement::class.java, (diagnostic as CjDiagnosticWithSource).element)
                val end = checkNotNull(element.declarationPosition?.endLocation)
                assertEquals(expectation.range.end, Point(end.line, end.column))
                assertEquals(normalize(peerPath), normalize(Path.of(end.filePath)))
            }
        }
    }

    private fun assertDiagnosticParameters(fixture: Fixture, expected: Expected, diagnostic: CjDiagnostic) {
        when (expected.factory) {
            "EXTEND_MEMBER_CANNOT_SHADOW" -> {
                val parameters = assertInstanceOf(CjDiagnosticWithParameters2::class.java, diagnostic)
                assertEquals(Name.identifier(if (fixture == Fixture.PROPERTY) "value" else "echo"), parameters.a)
            }
            "PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF" -> {
                val parameters = assertInstanceOf(CjDiagnosticWithParameters3::class.java, diagnostic)
                assertEquals(Name.identifier("value"), parameters.c)
                if (expected.side == Side.CURRENT) {
                    assertEquals(ConePrimitiveType.BOOLEAN, parameters.a)
                    assertBox(parameters.b as ConeCangJieType, fixture.packageName, generic = false)
                } else {
                    assertBox(parameters.a as ConeCangJieType, fixture.packageName, generic = true)
                    assertEquals(ConePrimitiveType.BOOLEAN, parameters.b)
                }
            }
            "RETURN_TYPE_INCOMPATIBLE" -> assertEquals(Name.identifier("echo"),
                assertInstanceOf(CjDiagnosticWithParameters1::class.java, diagnostic).a)
            "PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT", "PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT" ->
                assertEquals(Name.identifier("value"), assertInstanceOf(CjDiagnosticWithParameters1::class.java, diagnostic).a)
            "NEED_MEMBER_IMPLEMENTATION" -> assertTrue(
                assertInstanceOf(CjDiagnosticWithParameters1::class.java, diagnostic).a.toString().contains("Box"))
            else -> error("Unlisted fixture diagnostic: " + expected.factory)
        }
    }

    /** 原 JSON 的 note 消息及锚点独立验证；SOURCE 验范围，CJO 验公开的来源行列。 */
    private fun assertNotes(expected: Expected, notes: List<CjDiagnosticRelatedInformation>, source: String,
        sourcePath: Path, peerPath: Path, commonCjo: Boolean) {
        assertEquals(expected.notes.map { it.message }, notes.map { it.message })
        expected.notes.zip(notes).forEach { (expectation, note) ->
            val binary = note.element as? CjBinarySourceElement
            if (commonCjo && expectation.side == Side.PEER) {
                val location = note.sourceLocation
                if (location != null) {
                    assertEquals(normalize(peerPath), normalize(Path.of(location.filePath)))
                    assertEquals(expectation.range.begin, Point(location.line, location.column))
                } else {
                    val position = checkNotNull(checkNotNull(binary).sourceLocation)
                    assertEquals(normalize(peerPath), normalize(Path.of(position.filePath)))
                    assertEquals(expectation.range.begin, Point(position.line, position.column))
                }
            } else {
                assertNull(binary)
                val element = checkNotNull(note.element)
                assertEquals(expectation.range, Range(pointAt(source, element.startOffset), pointAt(source, element.endOffset)))
                note.sourceLocation?.let { assertEquals(normalize(sourcePath), normalize(Path.of(it.filePath))) }
            }
        }
    }

    private fun assertMappedInput(fixture: Fixture, artifact: DefaultCfirFrontendPipelineArtifact, imported: Boolean, cjo: Path?) {
        val output = artifact.frontendOutput.outputs.single()
        val file = output.fir.single()
        val owner = file.declarations.filterIsInstance<CfirExtend>().single {
            assertInstanceOf(ConeClassLikeType::class.java, it.extendedTypeRef.coneTypeOrNull).typeArguments.single() == ConePrimitiveType.INT64
        }
        val box = assertInstanceOf(ConeClassLikeType::class.java, owner.extendedTypeRef.coneTypeOrNull)
        val peer = output.session.extendProvider.getExtendsForClass(box.lookupTag.classId).single { it.typeParameters.isNotEmpty() }
        assertFalse(peer.status.isCommon)
        assertFalse(peer.status.isSpecific)
        val peerMember = peer.declarations.filterIsInstance<CfirCallableDeclaration>().single {
            it is CfirNamedFunction || it is CfirProperty
        }
        assertFalse(peerMember.status.isCommon)
        assertFalse(peerMember.status.isSpecific)
        if (imported) {
            val expectedCjo = normalize(checkNotNull(cjo))
            assertTrue(peer.isFromCommonPart)
            assertTrue(peerMember.isFromCommonPart)
            assertNotSame(output.session, peer.moduleData.session)
            assertEquals(expectedCjo, normalize(Path.of(checkNotNull(peer.cjoDeclarationOrigin).packagePath)))
            assertEquals(expectedCjo, normalize(Path.of(checkNotNull(peerMember.cjoDeclarationOrigin).packagePath)))
            assertNoCalls(checkNotNull((peerMember as CfirNamedFunction).body), requireReturn = true, requireThrow = false)
        } else {
            assertSame(output.session, peer.moduleData.session)
            assertSame(file, output.session.cfirProvider.getCfirCallableContainerFile(peerMember.symbol))
        }
        assertBox(box, fixture.packageName, generic = false)
        val symbol = checkNotNull(output.session.symbolProvider.getClassLikeSymbolByClassId(box.lookupTag.classId))
        val scopes = assertInstanceOf(CfirCangJieScopeProvider::class.java, symbol.cfir.scopeProvider)
        val input = checkNotNull(scopes.getDeclarationInheritanceInput(owner, output.session, output.scopeSession))
        if (fixture == Fixture.REQUIREMENT) {
            assertTrue(input.steps.isEmpty())
            assertTrue(input.visibleImplementations.none { it.name.asString() == "echo" })
            assertEquals(listOf(peerMember.symbol), input.invisibleMembers.map { it.declaration })
            val requirement = input.interfaceRequirements.single()
            assertTrue(requirement.flags.isAbstract)
            assertEquals("echo", requirement.name.asString())
        } else if (fixture != Fixture.PROPERTY) {
            val step = input.steps.single()
            val parent = step.invisibleParents.single()
            assertSame(peerMember.symbol, parent.declaration)
            assertSame(peer, parent.sourceExtend)
            assertBox(parent.returnType, fixture.packageName, generic = false)
            assertBox(parent.parameterTypes.single(), fixture.packageName, generic = false)
            assertBox(step.member.parameterTypes.single(), fixture.packageName, generic = false)
            if (fixture == Fixture.RETURN) assertEquals(ConePrimitiveType.BOOLEAN, step.member.returnType)
            else assertBox(step.member.returnType, fixture.packageName, generic = false)
            assertFalse(input.visibleImplementations.any { it.declaration === peerMember.symbol })
        } else {
            val step = input.steps.single()
            val parent = step.invisibleParents.single()
            assertSame(peerMember.symbol, parent.declaration)
            assertBox(parent.returnType, fixture.packageName, generic = false)
            assertTrue(parent.flags.isVar)
            assertEquals(ConePrimitiveType.BOOLEAN, step.member.returnType)
            assertFalse(step.member.flags.isVar)
            assertFalse(input.visibleImplementations.any { it.declaration === peerMember.symbol })
            val reverse = checkNotNull(scopes.getDeclarationInheritanceInput(peer, output.session, output.scopeSession)).steps.single()
            assertBox(reverse.member.returnType, fixture.packageName, generic = true)
            assertTrue(reverse.member.flags.isVar)
            assertEquals(ConePrimitiveType.BOOLEAN, reverse.invisibleParents.single().returnType)
            assertFalse(reverse.invisibleParents.single().flags.isVar)
        }
    }

    private fun assertBox(type: ConeCangJieType, packageName: String, generic: Boolean) {
        val box = assertInstanceOf(ConeClassLikeType::class.java, type)
        assertEquals(packageName + ".Box", box.lookupTag.classId.asSingleFqName().asString())
        val argument = box.typeArguments.single()
        if (generic) {
            val parameter = assertInstanceOf(ConeTypeParameterType::class.java, argument)
            assertEquals("U", parameter.lookupTag.typeParameterSymbol.cfir.name.asString())
        } else assertEquals(ConePrimitiveType.INT64, argument)
    }

    private fun assertSourceProduct(file: CfirFile, path: Path, source: String, session: CfirSession) {
        assertEquals(CfirSession.Kind.Source, session.kind)
        assertTrue(file.origin.fromSource)
        assertSame(session, file.moduleData.session)
        assertNotNull(file.source)
        assertEquals(normalize(path), normalize(Path.of(checkNotNull(checkNotNull(file.sourceFile).path))))
        assertNotNull(file.sourceFileLinesMapping)
        val context = checkNotNull(file.parsingSourceContext)
        assertEquals(source, context.snapshot.text)
        context.requireInput(source)
        assertTrue(checkNotNull(file.parsingDiagnosticPublication).diagnostics.none { it.isError })
        file.declarations.filterIsInstance<CfirExtend>().forEach { assertTrue(it.resolvePhase >= CfirResolvePhase.TYPES) }
    }

    private fun assertRealBodies(fixture: Fixture, file: CfirFile, hasCurrent: Boolean) {
        val peer = file.declarations.filterIsInstance<CfirExtend>().single { it.typeParameters.isNotEmpty() }
        if (fixture == Fixture.PROPERTY) {
            val property = peer.declarations.filterIsInstance<CfirProperty>().single()
            assertNoCalls(checkNotNull(checkNotNull(property.getter).body), false, false)
            assertNotNull(checkNotNull(property.setter).body)
        } else {
            val function = peer.declarations.filterIsInstance<CfirNamedFunction>().single()
            assertNoCalls(checkNotNull(function.body), requireReturn = true, requireThrow = false)
        }
        if (hasCurrent) assertCurrentBody(fixture, file)
    }

    private fun assertCurrentBody(fixture: Fixture, file: CfirFile) {
        val current = file.declarations.filterIsInstance<CfirExtend>().single { it.typeParameters.isEmpty() }
        when (fixture) {
            Fixture.RETURN, Fixture.CONTROL -> assertNoCalls(
                checkNotNull(current.declarations.filterIsInstance<CfirNamedFunction>().single().body), false, true)
            Fixture.PROPERTY -> {
                val property = current.declarations.filterIsInstance<CfirProperty>().single()
                assertNoCalls(checkNotNull(checkNotNull(property.getter).body), false, false)
                assertNull(property.setter)
            }
            Fixture.REQUIREMENT -> assertTrue(current.declarations.isEmpty())
        }
    }

    /** 遍历真实 body；只忽略编译器自行加入的 constructor，不把它冒充用户调用。 */
    private fun assertNoCalls(body: CfirBlock, requireReturn: Boolean, requireThrow: Boolean) {
        assertTrue(body.statements.isNotEmpty(), "original nonempty body must survive the real pipeline")
        var returns = 0
        var throws = 0
        body.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                assertFalse(element is CfirFunctionCall, "the fixture has no user function or constructor calls")
                if (element is CfirReturnExpression) returns++
                if (element is CfirThrowExpression) throws++
                element.acceptChildren(this)
            }
        })
        if (requireReturn) assertTrue(returns > 0)
        if (requireThrow) assertTrue(throws > 0)
    }

    private fun assertOrdinaryPeerCjo(path: Path) {
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(path)))
        val declarations = (0 until pkg.allDeclsLength).mapNotNull(pkg::allDecls)
        val peer = declarations.single { it.kind == DeclKind.ExtendDecl }
        assertNotNull(peer.generic, "the real CJO retains the generic peer header")
        val info = peer.info(PackageFormat.ExtendInfo()) as PackageFormat.ExtendInfo
        val member = (0 until info.bodyLength).map { index -> checkNotNull(pkg.allDecls(info.body(index).toInt() - 1)) }
            .single { it.identifier == "echo" || it.identifier == "value" }
        for (declaration in listOf(peer, member)) {
            assertFalse(hasCjoAttribute(declaration, Attribute.COMMON))
            assertFalse(hasCjoAttribute(declaration, Attribute.SPECIFIC))
            assertTrue(hasCjoAttribute(declaration, Attribute.FROM_COMMON_PART))
        }
    }


    /** 只读 CJO/既有绑定的边界证据；不根据 fixture 修饰符推断角色，也不请求 resolver/prepare。 */
    private fun describeCjoPropertyBoundary(cjo: Path, result: CompileResult): String {
        val nodes = mutableListOf<Map<String, Any?>>()
        val edges = mutableListOf<Map<String, Any?>>()
        val roots = mutableListOf<Map<String, Any?>>()
        val queue = mutableListOf<CfirClassLikeSymbol<*>>()
        val ids = java.util.IdentityHashMap<CfirClassLikeSymbol<*>, String>()
        fun register(symbol: CfirClassLikeSymbol<*>): String {
            val existing = ids[symbol]
            if (existing != null) return existing
            val id = "n" + ids.size
            ids[symbol] = id
            queue += symbol
            return id
        }
        fun rootType(label: String, type: ConeCangJieType?) {
            if (type !is ConeClassLikeType || type.lookupTag.classId.shortClassName.asString() != "Box") return
            val binding = existingBoundaryBinding(type)
            roots += linkedMapOf("via" to label, "type" to type.toString(), "binding" to binding.details,
                "node" to binding.symbol?.let(::register))
        }

        val outputs = result.artifact?.frontendOutput?.outputs
        val artifact = if (outputs == null) {
            mapOf("state" to "missing-specific-artifact", "box" to "missing-specific-artifact")
        } else {
            val outputRecords = outputs.mapIndexed { outputIndex, output ->
                val files = output.fir.map { file ->
                    file.declarations.filterIsInstance<CfirClassLikeDeclaration>().filter { it.name.asString() == "Box" }
                        .forEach { roots += mapOf("via" to "artifact[$outputIndex]/declaration", "node" to register(it.symbol)) }
                    val extends = file.declarations.filterIsInstance<CfirExtend>().map { extend ->
                        rootType("artifact[$outputIndex]/extendedTypeRef", extend.extendedTypeRef.coneTypeOrNull)
                        observeBoundary { declarationBoundary(extend) }
                    }
                    linkedMapOf("file" to file.sourceFile?.path, "extends" to extends)
                }
                linkedMapOf("session" to sessionBoundary(output.session), "files" to files)
            }
            mapOf("state" to "present", "outputs" to outputRecords,
                "boxRoots" to roots.toList(), "boxState" to
                    if (roots.any { it["node"] != null }) "existing-Box-symbol-observed" else "missing-existing-Box-symbol")
        }
        // property 的 binary 诊断可中止阶段；这些实际参数仍可保留 Box 的固定/弱绑定身份。
        result.diagnostics.forEachIndexed { diagnosticIndex, record ->
            val diagnostic = record.diagnostic
            val parameters: List<Any?> = when (diagnostic) {
                is CjDiagnosticWithParameters3<*, *, *> -> listOf(diagnostic.a, diagnostic.b, diagnostic.c)
                is CjDiagnosticWithParameters2<*, *> -> listOf(diagnostic.a, diagnostic.b)
                is CjDiagnosticWithParameters1<*> -> listOf(diagnostic.a)
                else -> emptyList()
            }
            parameters.forEachIndexed { parameterIndex, parameter ->
                rootType("diagnostic[$diagnosticIndex]/parameter[$parameterIndex]", parameter as? ConeCangJieType)
            }
        }
        var cursor = 0
        while (cursor < queue.size) {
            val symbol = queue[cursor++]
            val id = checkNotNull(ids[symbol])
            val declaration = if (symbol.isBound) symbol.cfir else null
            nodes += linkedMapOf("node" to id, "symbolClass" to symbol.javaClass.name,
                "classId" to symbol.classId.asSingleFqName().asString(),
                "symbolIdentity" to System.identityHashCode(symbol), "isBound" to symbol.isBound,
                "declaration" to if (declaration == null) mapOf("state" to "missing-unbound-declaration")
                    else observeBoundary { declarationBoundary(declaration) },
                "parentRefsState" to if (declaration == null) "missing-unbound-declaration" else "observed",
                "parentRefsCount" to declaration?.superTypeRefs?.size)
            declaration?.superTypeRefs?.forEachIndexed { parentIndex, ref ->
                val type = ref.coneTypeOrNull
                val binding = if (type is ConeClassLikeType) existingBoundaryBinding(type) else null
                edges += linkedMapOf("from" to id, "parentRefIndex" to parentIndex,
                    "refClass" to ref.javaClass.name, "refSource" to sourceBoundary(ref.source),
                    "type" to type?.toString(), "typeClass" to type?.javaClass?.name,
                    "state" to when {
                        type == null -> "missing-unresolved-parent-type"
                        binding == null -> "missing-class-like-parent-binding"
                        binding.symbol == null -> "missing-existing-parent-symbol"
                        else -> "existing-parent-symbol-binding"
                    },
                    "binding" to binding?.details, "to" to binding?.symbol?.let(::register))
            }
        }
        return boundaryJson(linkedMapOf(
            "kind" to "CJMP_CJO_PROPERTY_MAIN_BOUNDARY", "cjoPath" to normalize(cjo).toString(),
            "rawCjo" to observeBoundary { rawCjoBoundary(cjo) },
            "specificCompleted" to result.completed, "specificArtifact" to artifact,
            "diagnostics" to result.diagnostics.map { it.diagnostic.factoryName },
            "roots" to roots, "nodes" to nodes, "parentEdges" to edges,
            "traversal" to "existing fixed/weak bindings only; Object/Any edges retained when actually present; missing bindings explicit",
        ))
    }

    /** 原始 0-based allDecls 索引与 1-based wire body 引用同时保留；每个角色位来自原始 UInt64。 */
    private fun rawCjoBoundary(path: Path): Map<String, Any?> {
        val bytes = Files.readAllBytes(path)
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        fun entry(index: Int): Map<String, Any?> {
            val declaration = if (index in 0 until pkg.allDeclsLength) pkg.allDecls(index) else null
            if (declaration == null) return mapOf("state" to "missing-raw-declaration",
                "allDeclsIndex" to index, "wireIndex" to index + 1)
            return linkedMapOf("state" to "present", "path" to normalize(path).toString(),
                "allDeclsIndex" to index, "wireIndex" to index + 1,
                "identifier" to declaration.identifier, "fullPkgName" to declaration.fullPkgName,
                "kind" to declaration.kind.toInt(), "isTopLevel" to declaration.isTopLevel,
                "attributesLength" to declaration.attributesLength,
                "attributesWordsHex" to (0 until declaration.attributesLength).map { "0x" + declaration.attributes(it).toString(16) },
                "COMMON" to hasCjoAttribute(declaration, Attribute.COMMON),
                "SPECIFIC" to hasCjoAttribute(declaration, Attribute.SPECIFIC),
                "IMPORTED" to hasCjoAttribute(declaration, Attribute.IMPORTED),
                "FROM_COMMON_PART" to hasCjoAttribute(declaration, Attribute.FROM_COMMON_PART))
        }
        val box = mutableListOf<Map<String, Any?>>()
        val peers = mutableListOf<Map<String, Any?>>()
        for (index in 0 until pkg.allDeclsLength) {
            val declaration = pkg.allDecls(index) ?: continue
            if (declaration.kind == DeclKind.ClassDecl && declaration.identifier == "Box") box += entry(index)
            if (declaration.kind == DeclKind.ExtendDecl) {
                val info = declaration.info(PackageFormat.ExtendInfo()) as PackageFormat.ExtendInfo
                peers += linkedMapOf("declaration" to entry(index),
                    "body" to (0 until info.bodyLength).map { entry(info.body(it).toInt() - 1) })
            }
        }
        return linkedMapOf("path" to normalize(path).toString(), "bytes" to bytes.size,
            "fullPkgName" to pkg.fullPkgName, "allDeclsLength" to pkg.allDeclsLength,
            "attributeOrdinals" to listOf(Attribute.COMMON, Attribute.SPECIFIC, Attribute.IMPORTED, Attribute.FROM_COMMON_PART)
                .associate { it.name to it.ordinal },
            "Box" to mapOf("state" to if (box.isEmpty()) "missing-raw-Box" else "present", "entries" to box),
            "peers" to peers)
    }

    /** 读取已存在的绑定，弱引用已失效也显式标记；不调用 toSymbol 来创建新的缓存绑定。 */
    private fun existingBoundaryBinding(type: ConeClassLikeType): BoundaryBinding {
        val tag = type.lookupTag
        val binding = (tag as? ConeClassLikeLookupTagImpl)?.boundSymbol
        val session = binding?.first
        val symbol = when (tag) {
            is ConeClassLikeLookupTagWithFixedSymbol -> tag.symbol
            is ConeClassLikeLookupTagImpl -> binding?.second
            else -> null
        }
        return BoundaryBinding(symbol, linkedMapOf("tagClass" to tag.javaClass.name,
            "requestedClassId" to tag.classId.asSingleFqName().asString(),
            "state" to when {
                tag is ConeClassLikeLookupTagWithFixedSymbol -> "fixed-symbol"
                binding == null -> "missing-existing-binding"
                symbol == null -> "missing-weak-symbol"
                else -> "existing-weak-binding"
            }, "bindingSession" to session?.let(::sessionBoundary)))
    }

    private fun declarationBoundary(declaration: CfirMemberDeclaration): Map<String, Any?> {
        val status = declaration.status
        val cjo = declaration.cjoDeclarationOrigin
        return linkedMapOf("declarationClass" to declaration.javaClass.name,
            "declarationIdentity" to System.identityHashCode(declaration),
            "symbolClass" to declaration.symbol.javaClass.name,
            "symbolIdentity" to System.identityHashCode(declaration.symbol),
            "symbolDebugName" to declaration.symbol.debugName,
            "identifier" to (declaration as? CfirClassLikeDeclaration)?.name?.asString(),
            "resolvePhase" to declaration.resolvePhase.toString(),
            "moduleName" to declaration.moduleData.name.toString(),
            "moduleSession" to observeBoundary { sessionBoundary(declaration.moduleData.session) },
            "status" to linkedMapOf("class" to status.javaClass.name,
                "isCommon" to status.isCommon, "isSpecific" to status.isSpecific,
                "isCommonWithDefault" to status.isCommonWithDefault, "isDefault" to status.isDefault,
                "isOpen" to status.isOpen, "isAbstract" to status.isAbstract,
                "visibility" to status.visibility.toString(), "modality" to status.modality?.toString()),
            "exportFacts" to observeBoundary {
                val facts = declaration.exportFacts
                linkedMapOf("class" to facts.javaClass.name, "allFields" to facts.toString(),
                    "isImported" to facts.isImported, "sourceFactsPublished" to facts.sourceFactsPublished,
                    "isCompilerAdded" to facts.isCompilerAdded, "isImplicitlyAdded" to facts.isImplicitlyAdded)
            },
            "origin" to linkedMapOf("class" to declaration.origin.javaClass.name,
                "value" to declaration.origin.toString(), "fromSource" to declaration.origin.fromSource,
                "generated" to declaration.origin.generated, "fromSupertypes" to declaration.origin.fromSupertypes),
            "isFromCommonPart" to declaration.isFromCommonPart,
            "source" to sourceBoundary(declaration.source),
            "cjoOrigin" to if (cjo == null) mapOf("state" to "missing-cjo-origin") else
                linkedMapOf("state" to "present", "packagePath" to cjo.packagePath,
                    "declarationIndex" to cjo.declarationIndex, "isTopLevel" to cjo.isTopLevel))
    }

    private fun sessionBoundary(session: CfirSession): Map<String, Any?> =
        linkedMapOf("class" to session.javaClass.name, "identity" to System.identityHashCode(session), "kind" to session.kind.toString())

    private fun sourceBoundary(source: CjSourceElement?): Map<String, Any?> {
        if (source == null) return mapOf("state" to "missing-source")
        val location = (source as? CjBinarySourceElement)?.sourceLocation
        return linkedMapOf("state" to "present", "class" to source.javaClass.name,
            "kindClass" to source.kind.javaClass.name, "kind" to source.kind.toString(),
            "startOffset" to source.startOffset, "endOffset" to source.endOffset,
            "binaryLocation" to location?.let { mapOf("file" to it.filePath, "line" to it.line, "column" to it.column) })
    }

    /** 证据读取失败自身也是证据，不能用默认 false 替代，或遮蔽原有正确 assertion。 */
    private fun observeBoundary(read: () -> Any?): Any? = try {
        read()
    } catch (failure: Exception) {
        linkedMapOf("state" to "read-failed", "exception" to failure.javaClass.name, "message" to failure.message)
    }

    private data class BoundaryBinding(val symbol: CfirClassLikeSymbol<*>?, val details: Map<String, Any?>)

    /** 无额外 JSON 依赖，stdout 与 assertion message 使用同一份结构化只读快照。 */
    private fun boundaryJson(value: Any?): String = when (value) {
        null -> "null"
        is Boolean, is Number -> value.toString()
        is String -> buildString {
            append('"')
            for (character in value) when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u" + character.code.toString(16).padStart(4, '0')) else append(character)
            }
            append('"')
        }
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { boundaryJson(it.key as String) + ":" + boundaryJson(it.value) }
        is Iterable<*> -> value.joinToString(",", "[", "]") { boundaryJson(it) }
        else -> error("Unsupported boundary evidence value: " + value.javaClass.name)
    }

    private fun hasCjoAttribute(declaration: PackageFormat.Decl, attribute: Attribute): Boolean {
        val index = attribute.ordinal / 64
        if (index >= declaration.attributesLength) return false
        return declaration.attributes(index) and (1uL shl (attribute.ordinal % 64)) != 0uL
    }

    /** 保持 source 文件行列和 offsets，分割本身不插入角色修饰符或删除 body。 */
    private fun splitSource(fixture: Fixture): SourceSplit {
        val start = offsetAt(fixture.source, Point(fixture.currentLine, 1))
        val packageEnd = fixture.source.indexOf('\n') + 1
        val common = fixture.source.substring(0, start)
        val prefix = fixture.source.substring(packageEnd, start).map { if (it == '\n') '\n' else ' ' }.joinToString("")
        val specific = fixture.source.substring(0, packageEnd) + prefix + fixture.source.substring(start)
        assertEquals(fixture.source, common + specific.substring(start))
        assertEquals(fixture.source.length, specific.length)
        assertEquals("extend", specific.substring(start, start + 6))
        return SourceSplit(common, specific)
    }

    private fun assertReturnControlChangesOnlyAnnotation() {
        assertEquals(Fixture.CONTROL.source, Fixture.RETURN.source.replace(
            "echo(value: Box<Int64>): Bool", "echo(value: Box<Int64>): Box<Int64>"))
    }

    private fun compile(path: Path, lightTree: Boolean, disposable: Disposable,
        configure: CompilerConfiguration.() -> Unit = {}): CompileResult {
        val messages = RecordingMessageCollector()
        val diagnostics = DiagnosticsCollectorImpl()
        val configuration = CompilerConfiguration().apply {
            messageCollector = messages
            diagnosticsCollector = diagnostics
            moduleName = "declaration_inheritance_source"
            useLightTree = lightTree
            assertTrue(configureLanguageVersionSettings("1.1.3"))
            addCangJieSourceRoot(path.toString())
            configure()
        }
        val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
        // 对位 two-phase helper：executePhase 不运行 postActions，完成状态调用生产错误判据。
        val completed = artifact != null && !CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration)
        return CompileResult(artifact, diagnostics.diagnosticsByFilePath.flatMap { (file, values) ->
            values.map { DiagnosticRecord(file, it) }
        }, messages.messages.toList(), completed)
    }

    private fun writeSource(fixture: Fixture, lightTree: Boolean, name: String, source: String): Path {
        val directory = Files.createDirectories(tempDir.resolve(fixture.fileName + if (lightTree) "-light-tree" else "-psi"))
        return directory.resolve(name).also { Files.writeString(it, source, StandardCharsets.UTF_8) }
    }

    private fun withEnvironment(action: (Disposable) -> Unit) {
        val disposable = Disposer.newDisposable("declaration-inheritance-source-lifecycle")
        try { action(disposable) } finally { Disposer.dispose(disposable) }
    }

    private fun normalize(path: Path): Path = path.toAbsolutePath().normalize()

    private data class CompileResult(val artifact: DefaultCfirFrontendPipelineArtifact?, val diagnostics: List<DiagnosticRecord>,
        val messages: List<Message>, val completed: Boolean) {
        fun describe(): String = diagnostics.joinToString("\n") { it.diagnostic.factoryName + ": " + it.diagnostic.renderMessage() } +
            "\nmessages=" + messages
    }
    private data class DiagnosticRecord(val filePath: String?, val diagnostic: CjDiagnostic)
    private data class Message(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageSourceLocation?)
    private class RecordingMessageCollector : MessageCollector {
        val messages = mutableListOf<Message>()
        override fun clear() = messages.clear()
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            messages += Message(severity, message, location)
        }
        override fun hasErrors(): Boolean = messages.any { it.severity.isError }
    }
    private data class SourceSplit(val common: String, val specific: String)
    private enum class Side { PEER, CURRENT }
    private data class Point(val line: Int, val column: Int)
    private data class Range(val begin: Point, val end: Point?)
    private data class Key(val factory: String, val side: Side, val range: Range) {
        fun sortKey(): String = factory + ":" + side + ":" + range
    }
    private data class Note(val message: String, val side: Side, val range: Range)
    private data class Expected(val factory: String, val side: Side, val range: Range, val notes: List<Note> = emptyList())

    private enum class Fixture(val fileName: String, val packageName: String, val source: String, val currentLine: Int,
        val expected: List<Expected>) {
        RETURN("return-incompatible", "invisible_peer", RETURN_SOURCE, 13, listOf(
            Expected("RETURN_TYPE_INCOMPATIBLE", Side.CURRENT, range(14, 17, 14, 21), listOf(
                Note("'Bool' is not a subtype of 'Class-Box<Int64>'", Side.PEER, range(8, 17, 8, 21)))),
            Expected("EXTEND_MEMBER_CANNOT_SHADOW", Side.CURRENT, range(14, 17, 14, 21)),
        )),
        CONTROL("return-compatible-control", "invisible_peer", CONTROL_SOURCE, 13, listOf(
            Expected("EXTEND_MEMBER_CANNOT_SHADOW", Side.CURRENT, range(14, 17, 14, 21)),
        )),
        REQUIREMENT("peer-only-requirement", "invisible_peer_requirement", REQUIREMENT_SOURCE, 17, listOf(
            Expected("NEED_MEMBER_IMPLEMENTATION", Side.CURRENT, range(17, 1, 17, 2), listOf(
                Note("unimplemented interface function 'echo'", Side.PEER, range(8, 5, 8, 6)))),
        )),
        PROPERTY("property-type-var-shadow", "invisible_peer_property", PROPERTY_SOURCE, 16, listOf(
            Expected("EXTEND_MEMBER_CANNOT_SHADOW", Side.PEER, range(8, 21, 8, 26)),
            Expected("PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF", Side.PEER, range(8, 5, 8, 6)),
            Expected("PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT", Side.PEER, range(8, 5, 13, 6)),
            Expected("EXTEND_MEMBER_CANNOT_SHADOW", Side.CURRENT, range(17, 17, 17, 22)),
            Expected("PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF", Side.CURRENT, range(17, 5, 17, 6)),
            Expected("PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT", Side.CURRENT, range(17, 5, 21, 6)),
        )),
    }

    private companion object {
        fun range(line: Int, column: Int, endLine: Int, endColumn: Int): Range = Range(Point(line, column), Point(endLine, endColumn))
        fun offsetAt(source: String, point: Point): Int {
            val lines = source.split('\n')
            require(point.line in 1..lines.size)
            require(point.column in 1..lines[point.line - 1].length + 1)
            return lines.take(point.line - 1).sumOf { it.length + 1 } + point.column - 1
        }
        fun pointAt(source: String, offset: Int): Point {
            require(offset in 0..source.length)
            val before = source.substring(0, offset)
            return Point(before.count { it == '\n' } + 1, offset - before.lastIndexOf('\n'))
        }

        val RETURN_SOURCE = """
            package invisible_peer

            public interface Mark {}

            public class Box<T> <: Exception {}

            extend<U> Box<U> where U <: Mark {
                public func echo(value: Box<U>): Box<U> {
                    return value
                }
            }

            extend Box<Int64> {
                public func echo(value: Box<Int64>): Bool {
                    throw value
                }
            }
        """.trimIndent() + "\n"
        val CONTROL_SOURCE = RETURN_SOURCE.replace("echo(value: Box<Int64>): Bool", "echo(value: Box<Int64>): Box<Int64>")
        val REQUIREMENT_SOURCE = """
            package invisible_peer_requirement

            public interface Mark {}

            public class Box<T> {}

            public interface EchoRequirement {
                func echo(value: Box<Int64>): Box<Int64>
            }

            extend<U> Box<U> where U <: Mark {
                public func echo(value: Box<U>): Box<U> {
                    return value
                }
            }

            extend Box<Int64> <: EchoRequirement {}
        """.trimIndent() + "\n"
        val PROPERTY_SOURCE = """
            package invisible_peer_property

            public interface Mark {}

            public class Box<T> {}

            extend<U> Box<U> where U <: Mark {
                public mut prop value: Box<U> {
                    get() {
                        this
                    }
                    set(_) {}
                }
            }

            extend Box<Int64> {
                public prop value: Bool {
                    get() {
                        true
                    }
                }
            }
        """.trimIndent() + "\n"
    }
}
