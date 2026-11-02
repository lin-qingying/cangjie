package org.cangnova.cangjie.analysis.decompiled.psi.text

import PackageFormat.Package as CjoPackage
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.analysis.decompiler.stub.CjoDeclarationLoader
import org.cangnova.cangjie.analysis.decompiler.stub.CjoFileStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirExceptionHandler
import org.cangnova.cangjie.cfir.analysis.tests.AbstractCfirAnalysisResolveTest
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.serializedInteropFacts
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.serialization.cjo.CfirCjoPackageMetadataProducer
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/** 声明文件的有效状态经过 CJO、stub 和普通源码重解析后仍具有相同的成员语义。 */
class CjoDeclarationBodyProjectionTest : AbstractCfirAnalysisResolveTest() {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun declarationFileConcreteMembersAndInterfaceDefaultKeepBodies() {
        val source = """
            package sample.projection
            public class Plain {
                public func closed(): Unit
            }
            public open class OpenHost {
                public func ordinary(): Unit
                public open func expandable(): Unit
            }
            public interface Contract {
                func defaulted(): Unit
            }
        """.trimIndent()
        val file = psiFileFactory.createFileFromText("input.cj.d", CangJieDeclarationFileType, source) as CjFile
        val (cfirFile, loaded) = serializeAndLoad(file)
        val sourceMethods = cfirFile.functionsByName()
        assertEquals(Modality.FINAL, sourceMethods.getValue("closed").status.modality)
        assertEquals(Modality.FINAL, sourceMethods.getValue("ordinary").status.modality)
        assertEquals(Modality.OPEN, sourceMethods.getValue("expandable").status.modality)
        assertEquals(Modality.OPEN, sourceMethods.getValue("defaulted").status.modality)
        assertTrue(sourceMethods.getValue("defaulted").status.isDefault)

        val decoded = CjoDeclarationLoader.loadDeclarations(loaded, cfirFile.moduleData)
            .filterIsInstance<CfirClassLikeDeclaration>()
            .flatMap { it.declarations.filterIsInstance<CfirNamedFunction>() }
            .associateBy { it.name.asString() }
        assertEquals(Modality.OPEN, decoded.getValue("defaulted").status.modality)
        assertTrue(decoded.getValue("defaulted").status.isDefault)

        val text = buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, cfirFile.moduleData))
        val parsed = createCjFile("decompiledConcrete", text)
        val functions = parsed.functionsByName()
        listOf("closed", "ordinary", "expandable", "defaulted").forEach { name ->
            assertTrue(functions.getValue(name).hasBody(), "$name 应有 compiled body：$text")
        }
        assertTrue(text.contains("/* compiled code */"), text)
        val reparsed = assertNoBodyOrAbstractDiagnostics(parsed).functionsByName()
        assertEquals(Modality.FINAL, reparsed.getValue("closed").status.modality)
        assertEquals(Modality.FINAL, reparsed.getValue("ordinary").status.modality)
        assertEquals(Modality.OPEN, reparsed.getValue("expandable").status.modality)
        assertEquals(Modality.OPEN, reparsed.getValue("defaulted").status.modality)
    }

    @Test
    fun abstractClassMemberRemainsBodylessWithoutInventedAbstractKeyword() {
        val source = """
            package sample.projection
            public abstract class Host {
                public func pending(): Unit
            }
        """.trimIndent()
        val (cfirFile, loaded) = serializeAndLoad(createCjFile("abstractHost", source))
        assertEquals(Modality.ABSTRACT, cfirFile.functionsByName().getValue("pending").status.modality)

        val text = buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, cfirFile.moduleData))
        val parsed = createCjFile("decompiledAbstract", text)
        val function = parsed.functionsByName().getValue("pending")
        assertFalse(function.hasBody(), text)
        assertFalse(function.hasModifier(CjTokens.ABSTRACT_KEYWORD), text)
        val reparsed = assertNoBodyOrAbstractDiagnostics(parsed).functionsByName()
        assertEquals(Modality.ABSTRACT, reparsed.getValue("pending").status.modality)
    }

    @Test
    fun foreignAndIntrinsicDeclarationsRemainBodylessAfterCjoRoundTrip() {
        val source = """
            package sample.projection
            @Intrinsic
            func getTypeForTypeParameter(): Unit
            foreign func directNative(): Unit
            foreign {
                func blockNative(): Unit
            }
        """.trimIndent()
        val (cfirFile, loaded) = serializeAndLoad(createCjFile("foreignAndIntrinsic", source))
        val decoded = CjoDeclarationLoader.loadDeclarations(loaded, cfirFile.moduleData)
            .filterIsInstance<CfirNamedFunction>()
            .associateBy { it.name.asString() }
        assertTrue(decoded.getValue("getTypeForTypeParameter").serializedInteropFacts?.isIntrinsic == true)
        assertTrue(decoded.getValue("directNative").status.isForeign)
        assertTrue(decoded.getValue("blockNative").status.isForeign)

        val text = buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, cfirFile.moduleData))
        val parsed = createCjFile("decompiledForeignAndIntrinsic", text)
        val functions = parsed.functionsByName()
        listOf("getTypeForTypeParameter", "directNative", "blockNative").forEach { name ->
            assertFalse(functions.getValue(name).hasBody(), "$name 不应有 compiled body：$text")
        }
        assertTrue(text.contains("@Intrinsic"), text)
        assertNoBodyOrAbstractDiagnostics(parsed)
    }

    @Test
    fun propertyProjectionKeepsConcreteAndAbstractAccessorsDistinct() {
        val source = """
            package sample.projection
            public open class ConcreteHost {
                public open prop value: Int32 {
                    get() { return 1 }
                }
            }
            public abstract class AbstractHost {
                public prop pending: Int32
            }
        """.trimIndent()
        val (cfirFile, loaded) = serializeAndLoad(createCjFile("properties", source))
        val decoded = CjoDeclarationLoader.loadDeclarations(loaded, cfirFile.moduleData)
            .filterIsInstance<CfirClassLikeDeclaration>()
            .flatMap { it.declarations.filterIsInstance<CfirProperty>() }
            .associateBy { it.name.asString() }
        assertEquals(Modality.OPEN, decoded.getValue("value").status.modality)
        assertEquals(Modality.ABSTRACT, decoded.getValue("pending").status.modality)

        val text = buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, cfirFile.moduleData))
        val parsed = createCjFile("decompiledProperties", text)
        assertFalse(text.contains("@Annotation"), "普通属性不应被还原为注解声明：$text")
        val properties = PsiTreeUtil.findChildrenOfType(parsed, CjProperty::class.java).associateBy { it.name }
        assertTrue(properties.getValue("value").getter?.hasBody() == true, text)
        assertFalse(properties.getValue("pending").hasBody(), text)
        assertFalse(properties.getValue("pending").hasModifier(CjTokens.ABSTRACT_KEYWORD), text)
        assertNoBodyOrAbstractDiagnostics(parsed)
    }

    @Test
    fun customAnnotationExportUsesVisibilityIndependentlyOfCjmpBits() {
        val source = """
            package sample.projection
            public func plainHidden(): Unit {}
            public func plainVisible(): Unit {}
            public func commonHidden(): Unit {}
            public func commonVisible(): Unit {}
            public func specificHidden(): Unit {}
            public func specificVisible(): Unit {}
        """.trimIndent()
        val expectedAnnotationCounts = mapOf(
            "plainHidden" to 0,
            "plainVisible" to 1,
            "commonHidden" to 1,
            "commonVisible" to 1,
            "specificHidden" to 1,
            "specificVisible" to 1,
        )
        for (fromCommonPart in listOf(false, true)) {
            val session = createStrictSession()
            if (fromCommonPart) {
                session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
            }
            val file = createCjFile("annotationExport", source).toCfirFile(session)
            resolveToPhase(file, session, targetPhase)
            val functions = file.declarations.filterIsInstance<CfirNamedFunction>()
            functions.forEach { function ->
                val name = function.name.asString()
                val original = function.status
                // 在合法源码的已解析状态副本上构造 metadata 矩阵，COMMON 与 SPECIFIC 不同时置位。
                function.replaceStatus(buildResolvedDeclarationStatus {
                    this.source = original.source
                    visibility = original.visibility
                    visibilityExplicitness = original.visibilityExplicitness
                    modalityExplicitness = original.modalityExplicitness
                    bodyPresence = original.bodyPresence
                    isDeclarationFile = original.isDeclarationFile
                    isVisibilityExplicit = original.isVisibilityExplicit
                    isModalityExplicit = original.isModalityExplicit
                    isAbstractExplicit = original.isAbstractExplicit
                    isOverride = original.isOverride
                    isOperator = original.isOperator
                    isStatic = original.isStatic
                    isConst = original.isConst
                    isMut = original.isMut
                    isUnsafe = original.isUnsafe
                    isForeign = original.isForeign
                    isC = original.isC
                    isCommon = name.startsWith("common")
                    isFromCommonPart = fromCommonPart
                    isSpecific = name.startsWith("specific")
                    isCommonWithDefault = original.isCommonWithDefault
                    isRedef = original.isRedef
                    isDefault = original.isDefault
                    isAbstract = original.isAbstract
                    isOpen = original.isOpen
                    isSealed = original.isSealed
                    modality = checkNotNull(original.modality)
                })
                val visible = name.endsWith("Visible")
                val annotation = buildAnnotationCall {
                    typeRef = buildImplicitTypeRef()
                    argumentList = buildArgumentList {}
                    calleeReference = buildNamedReference { this.name = Name.identifier("Marker") }
                    containingDeclarationSymbol = function.symbol
                    annotationIdentity = CangjieAnnotationIdentity.Custom(FqName("sample.projection.Marker"))
                    annotationSourceName = "Marker"
                    isCompileTimeVisible = visible
                }
                function.replaceAnnotations(listOf(annotation))
            }
            val metadata = CfirCjoPackageMetadataProducer.produce(listOf(file))
            functions.forEach { function ->
                val name = function.name.asString()
                val context = "$name, FROM_COMMON_PART=$fromCommonPart"
                val encoded = metadata.declarations.single { it.identifier == name }
                fun hasAttribute(attribute: Attribute): Boolean {
                    val word = encoded.attributes.getOrNull(attribute.ordinal / Long.SIZE_BITS) ?: 0uL
                    return word and (1uL shl (attribute.ordinal % Long.SIZE_BITS)) != 0uL
                }
                assertEquals(name.startsWith("common"), hasAttribute(Attribute.COMMON), context)
                assertEquals(name.startsWith("specific"), hasAttribute(Attribute.SPECIFIC), context)
                assertEquals(fromCommonPart, hasAttribute(Attribute.FROM_COMMON_PART), context)
                // 官方 v1.1.3 ASTWriter::SaveAnnotations：可见或 COMMON/SPECIFIC 即保留，来源位独立不触发例外。
                assertEquals(expectedAnnotationCounts.getValue(name), encoded.annotations.size, context)
            }
        }
    }

    @Test
    fun interfaceAbstractMembersKeepOpenAttributeAcrossCjoProjection() {
        val source = """
            package sample.projection
            public interface Contract {
                func required(): Unit
                prop pending: Int32
                static func helper(): Unit {}
            }
        """.trimIndent()
        val cfirFile = resolveSource(createCjFile("interfaceAttributes", source))
        val owner = cfirFile.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        val function = owner.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "required" }
        val property = owner.declarations.filterIsInstance<CfirProperty>().single()
        for (member in listOf(function, property)) {
            assertEquals(Modality.ABSTRACT, member.status.modality)
            assertTrue(member.status.isOpen, "接口抽象成员也必须具有有效 OPEN 事实")
        }
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(cfirFile))
        for (name in listOf("required", "pending")) {
            val encoded = metadata.declarations.single { it.identifier == name }
            assertTrue(encoded.attributes.hasAttribute(Attribute.OPEN), name)
            assertTrue(encoded.attributes.hasAttribute(Attribute.ABSTRACT), name)
        }
        val staticMethod = metadata.declarations.single { it.identifier == "helper" }
        assertTrue(staticMethod.attributes.hasAttribute(Attribute.STATIC))
        assertFalse(staticMethod.attributes.hasAttribute(Attribute.OPEN))
        val loaded = loadMetadata(metadata)
        val decoded = CjoDeclarationLoader.loadDeclarations(loaded, cfirFile.moduleData)
            .filterIsInstance<CfirClassLikeDeclaration>().single()
        val decodedFunction = decoded.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "required" }
        val decodedProperty = decoded.declarations.filterIsInstance<CfirProperty>().single()
        for (member in listOf(decodedFunction, decodedProperty)) {
            assertEquals(Modality.ABSTRACT, member.status.modality)
            assertTrue(member.status.isOpen)
        }
        val text = buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, cfirFile.moduleData))
        val parsed = createCjFile("decompiledInterfaceAttributes", text)
        assertFalse(parsed.functionsByName().getValue("required").hasBody(), text)
        assertFalse(PsiTreeUtil.findChildrenOfType(parsed, CjProperty::class.java).single().hasBody(), text)
        assertNoBodyOrAbstractDiagnostics(parsed)
    }

    @Test
    fun javaMirrorConcretePropertyKeepsSignatureWithoutSourceAccessors() {
        val source = """
            package sample.projection
            import interoplib.interop.*
            public class MirrorHost {
                public mut prop value: Int32 {
                    get() { return 1 }
                    set(value) {}
                }
            }
        """.trimIndent()
        val cfirFile = resolveSource(createCjFile("mirrorProperty", source))
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(cfirFile))
        val propertyMetadata = metadata.declarations.single { it.kind == PackageFormat.DeclKind.PropDecl && it.identifier == "value" }
        assertFalse(propertyMetadata.attributes.hasAttribute(Attribute.OPEN))
        assertFalse(propertyMetadata.attributes.hasAttribute(Attribute.ABSTRACT))
        // 使用真实写出 metadata 的副本模拟官方 JavaMirror CJO，不把带访问器的源码冒充 mirror 声明。
        val mirrored = metadata.copy(declarations = metadata.declarations.map { declaration ->
            if (declaration.identifier != "MirrorHost") return@map declaration
            val attributes = declaration.attributes.toMutableList()
            val word = Attribute.JAVA_MIRROR.ordinal / Long.SIZE_BITS
            while (attributes.size <= word) attributes += 0uL
            attributes[word] = attributes[word] or (1uL shl (Attribute.JAVA_MIRROR.ordinal % Long.SIZE_BITS))
            declaration.copy(attributes = attributes)
        })
        val loaded = loadMetadata(mirrored)
        val decoded = CjoDeclarationLoader.loadDeclarations(loaded, cfirFile.moduleData)
            .filterIsInstance<CfirClassLikeDeclaration>().single()
        assertTrue(decoded.serializedInteropFacts?.isJavaMirror == true)
        assertEquals(Modality.FINAL, decoded.declarations.filterIsInstance<CfirProperty>().single().status.modality)
        val text = buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, cfirFile.moduleData))
        val parsed = createCjFile("decompiledMirrorProperty", text)
        val property = PsiTreeUtil.findChildrenOfType(parsed, CjProperty::class.java).single()
        assertTrue(text.contains("@JavaMirror"), "宿主的 CJO JavaMirror 身份必须恢复为注解：$text")
        assertFalse(property.hasBody(), text)
        assertTrue(property.accessors.isEmpty(), "JavaMirror 属性只保留签名：$text")
        val reparsed = assertNoBodyOrAbstractDiagnostics(parsed)
        assertEquals(
            Modality.FINAL,
            reparsed.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
                .declarations.filterIsInstance<CfirProperty>().single().status.modality,
        )
    }

    @Test
    fun javaMirrorInterfacePropertyOmitsForbiddenOpenModifier() {
        val source = """
            package sample.projection
            import interoplib.interop.*
            public interface MirrorContract {
                func required(): Unit
                prop value: Int32
            }
        """.trimIndent()
        val cfirFile = resolveSource(createCjFile("mirrorInterface", source))
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(cfirFile))
        val propertyMetadata = metadata.declarations.single { it.kind == PackageFormat.DeclKind.PropDecl }
        assertTrue(propertyMetadata.attributes.hasAttribute(Attribute.OPEN))
        assertTrue(propertyMetadata.attributes.hasAttribute(Attribute.ABSTRACT))
        val mirrored = metadata.copy(declarations = metadata.declarations.map { declaration ->
            if (declaration.identifier != "MirrorContract" && declaration.kind != PackageFormat.DeclKind.PropDecl) {
                return@map declaration
            }
            val attributes = declaration.attributes.toMutableList()
            val word = Attribute.JAVA_MIRROR.ordinal / Long.SIZE_BITS
            while (attributes.size <= word) attributes += 0uL
            attributes[word] = attributes[word] or (1uL shl (Attribute.JAVA_MIRROR.ordinal % Long.SIZE_BITS))
            declaration.copy(attributes = attributes)
        })
        val loaded = loadMetadata(mirrored)
        val text = buildDecompiledText(CjoFileStubBuilder.buildFileStub(loaded, cfirFile.moduleData))
        val parsed = createCjFile("decompiledMirrorInterface", text)
        val property = PsiTreeUtil.findChildrenOfType(parsed, CjProperty::class.java).single()
        assertTrue(text.contains("@JavaMirror"), text)
        assertFalse(property.hasModifier(CjTokens.OPEN_KEYWORD), text)
        assertTrue(property.annotationEntries.isEmpty(), "成员继承的 JAVA_MIRROR 位不能生成非法目标注解：$text")
        assertFalse(property.hasBody(), text)
        val reparsed = assertNoBodyOrAbstractDiagnostics(parsed)
        val owner = reparsed.declarations.filterIsInstance<CfirClassLikeDeclaration>().single()
        assertEquals(Modality.ABSTRACT, owner.declarations.filterIsInstance<CfirProperty>().single().status.modality)
        assertEquals(Modality.ABSTRACT, owner.declarations.filterIsInstance<CfirNamedFunction>().single().status.modality)
    }

    private fun List<ULong>.hasAttribute(attribute: Attribute): Boolean {
        val word = getOrNull(attribute.ordinal / Long.SIZE_BITS) ?: 0uL
        return word and (1uL shl (attribute.ordinal % Long.SIZE_BITS)) != 0uL
    }

    private fun serializeAndLoad(file: CjFile): Pair<CfirFile, LoadedCjoPackage> {
        val cfirFile = resolveSource(file)
        return cfirFile to loadMetadata(CfirCjoPackageMetadataProducer.produce(listOf(cfirFile)))
    }

    private fun resolveSource(file: CjFile): CfirFile {
        val session = createStrictSession()
        val cfirFile = file.toCfirFile(session)
        resolveToPhase(cfirFile, session, targetPhase)
        return cfirFile
    }

    private fun loadMetadata(metadata: CjoPackageMetadata): LoadedCjoPackage {
        val bytes = CjoPackageWriter.toByteArray(metadata)
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        return LoadedCjoPackage(
            binaryFile = LightVirtualFile("sample.projection.cjo"),
            packageFqName = FqName("sample.projection"),
            pkg = pkg,
            header = CjoPackageHeader.fromPackage(pkg),
            cjoManager = CjoManager(CjoSearchPath { null }),
            isVersionSupported = true,
        )
    }

    private fun CfirFile.functionsByName(): Map<String, CfirNamedFunction> = declarations
        .filterIsInstance<CfirClassLikeDeclaration>()
        .flatMap { it.declarations.filterIsInstance<CfirNamedFunction>() }
        .associateBy { it.name.asString() }

    private fun CjFile.functionsByName(): Map<String, CjNamedFunction> =
        PsiTreeUtil.findChildrenOfType(this, CjNamedFunction::class.java).associateBy { requireNotNull(it.name) }

    private fun assertNoBodyOrAbstractDiagnostics(file: CjFile): CfirFile {
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        assertTrue(errors.isEmpty(), "反编译文本应可解析：$errors\n${file.text}")
        val session = createStrictSession()
        val cfirFile = file.toCfirFile(session)
        val resolveCollector = CfirDiagnosticCollector()
        resolveToPhase(cfirFile, session, targetPhase, resolveCollector)
        val checkerCollector = RecordingDiagnosticsCollector()
        session.runCheckers(ScopeSession(), listOf(cfirFile), checkerCollector)
        val diagnostics = (resolveCollector.rawDiagnostics + checkerCollector.diagnostics)
            .map { it.factoryName.removePrefix("CFIR_") }
        val forbidden = setOf(
            "MISSING_FUNC_BODY",
            "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
            "EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS",
            "PARSE_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS",
            "INTRINSIC_FUNCTION_CANNOT_HAVE_BODY",
        )
        assertTrue(diagnostics.none { it in forbidden }, "反编译文本出现错误的缺体或抽象诊断：$diagnostics\n${file.text}")
        return cfirFile
    }

    private fun createStrictSession(): CfirSession = createTestSession().also { session ->
        session.register(CfirExceptionHandler::class, object : CfirExceptionHandler() {
            override fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing = throw throwable
            override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing = throw throwable
        })
    }

    private class RecordingDiagnosticsCollector : BaseDiagnosticsCollector() {
        private val storage = mutableListOf<CjDiagnostic>()
        override val diagnostics: List<CjDiagnostic> get() = storage
        override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>> get() = storage.groupBy { null }
        override val hasErrors: Boolean get() = storage.any { it.severity.isError }
        override val hasWarningsForWError: Boolean get() = false
        override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
            if (diagnostic != null) storage += diagnostic
        }
    }
}
