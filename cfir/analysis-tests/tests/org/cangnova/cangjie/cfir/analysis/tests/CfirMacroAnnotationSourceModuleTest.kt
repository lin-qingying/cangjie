package org.cangnova.cangjie.cfir.analysis.tests

import PackageFormat.DeclKind
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.cfir.DependencyListForCliModule
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFileCopy
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.cfir.entrypoint.configuration.diagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.entrypoint.session.CfirDefaultSessionFactory
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationResolveState
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.lightTree.LightTreeRawCfirDeclarationBuilder
import org.cangnova.cangjie.cfir.serialization.cjo.CfirCjoPackageMetadataProducer
import org.cangnova.cangjie.cfir.pipeline.CfirSessionConstructionUtils
import org.cangnova.cangjie.cfir.pipeline.CfirSessionProducer
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.expandWithDefaultContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.resolve.transformers.CfirFileReplacingResolveProcessor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirGlobalResolveProcessor
import org.cangnova.cangjie.cfir.resolve.transformers.CfirTransformerBasedResolveProcessor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.session.phaseResolverRegistry
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 普通宏的新 token 使用空 parser 模块，符号仍属于宿主包。
 * Raw provenance 必须穿过片段收集与 TYPES，不能根据宿主 std 包重新授予 ConstSafe 内置身份。
 */
@OptIn(CompilerConfiguration.Internals::class)
class CfirMacroAnnotationSourceModuleTest : AbstractCfirAnalysisTestCase() {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun psiMacroTokensKeepTheirSourceModuleThroughTypes() {
        assertSourceModuleContract(useLightTree = false)
    }

    @Test
    fun lightTreeMacroTokensKeepTheirSourceModuleThroughTypes() {
        assertSourceModuleContract(useLightTree = true)
    }

    private fun assertSourceModuleContract(useLightTree: Boolean) {
        for (context in SOURCE_CONTEXTS) {
            val session = createResolveSession()
            val sourceText = "${context.packageHeader}\n@ConstSafe\nfunc direct(): Unit {}"
            val sourcePsi = createCjFile("annotationSource", sourceText)
            val hostFile: CfirFile
            val annotationMetadata: CfirAnnotationMetadataBuilder
            val sourceSurfaces: List<MacroSurface>
            val generated: CfirNamedFunction
            val generatedSurfaces: List<MacroSurface>
            val ownerSnapshot = MacroRawOwnerSnapshot(
                packageFqName = context.packageFqName,
                containerSymbols = emptyList(),
                dispatchReceiverType = null,
                inLocalContext = false,
                functionTarget = null,
                signature = null,
                nominalSyntax = null,
                capturedTypeParameters = emptyList(),
                constructedClasses = emptyList(),
            )
            if (useLightTree) {
                val built = LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
                    parseLightFile(sourceText),
                    CjInMemoryTextSourceFile("annotationSource.cj", null, sourceText),
                    sourceText.toSourceLinesMapping(),
                )
                hostFile = built.cfirFile
                sourceSurfaces = built.surfaces
                annotationMetadata = built.annotationMetadata
                val generatedFile = parseLightFile(GENERATED_TOKENS)
                val generatedTree = generatedFile.tree
                val builder = LightTreeRawCfirDeclarationBuilder(
                    session = session,
                    annotationMetadata = annotationMetadata,
                    baseScopeProvider = session.cangjieScopeProvider,
                    tree = generatedTree,
                    source = GENERATED_TOKENS,
                    macroFacts = generatedFile.macroFacts,
                    nominalFacts = generatedFile.cjmpNominalFacts,
                    declarationModifiers = generatedFile.declarationModifiers,
                )
                generated = checkNotNull(builder.buildDeclarationFragment(
                    generatedTree.root, ownerSnapshot,
                )) as CfirNamedFunction
                generatedSurfaces = builder.consumeCollectedMacroSurfaces()
            } else {
                val sourceBuilder = PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
                annotationMetadata = sourceBuilder.annotationMetadata
                hostFile = sourceBuilder.buildCfirFile(sourcePsi)
                sourceSurfaces = sourceBuilder.consumeCollectedMacroSurfaces()
                val generatedPsi = CjPsiFactory.forMacroExpansion(project, sourcePsi)
                    .createFile("generated.cj", GENERATED_TOKENS)
                val builder = PsiRawCfirBuilder(session, annotationMetadata = annotationMetadata)
                generated = checkNotNull(builder.buildDeclarationFragment(
                    generatedPsi, ownerSnapshot,
                )) as CfirNamedFunction
                generatedSurfaces = builder.consumeCollectedMacroSurfaces()
            }

            assertEquals(context.packageFqName, hostFile.packageDirective.packageFqName)
            val direct = hostFile.declarations.filterIsInstance<CfirNamedFunction>().single()
            assertEquals("direct", direct.name.asString())
            assertEquals("generated", generated.name.asString())
            assertEquals(context.packageFqName, direct.symbol.callableId.packageName)
            assertEquals(context.packageFqName, generated.symbol.callableId.packageName)
            assertEquals(context.sourceModuleName, annotation(direct).sourceModuleName)
            assertEquals("", annotation(generated).sourceModuleName)
            assertEquals(context.sourceModuleName, sourceSurfaces.single().scopeContext.sourceModuleName)
            assertEquals("", generatedSurfaces.single().scopeContext.sourceModuleName)

            // 先完成 construction，再登记同一个宿主文件，保持 provider 与 resolve 的正常边界。
            val combined = buildFileCopy(hostFile) {
                symbol = CfirFileSymbol()
                declarations.add(generated)
            }
            recordRawCfirFile(PreMacroCfirFile(combined, sourceSurfaces + generatedSurfaces, annotationMetadata), session)
            val resolved = resolveThroughTypes(combined, session)
            val functions = resolved.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name.asString() }
            val resolvedDirect = checkNotNull(functions["direct"])
            val resolvedGenerated = checkNotNull(functions["generated"])
            assertEquals(CfirResolvePhase.TYPES, resolvedDirect.resolvePhase)
            assertEquals(CfirResolvePhase.TYPES, resolvedGenerated.resolvePhase)
            assertEquals(context.sourceModuleName, annotation(resolvedDirect).sourceModuleName)
            if (context.sourceModuleName == "std") {
                assertEquals(BuiltInAnnotationKind.CONSTSAFE, annotation(resolvedDirect).annotationKind)
                assertTrue(annotation(resolvedDirect).annotationIdentity is CangjieAnnotationIdentity.LanguageBuiltIn)
            } else {
                assertNull(annotation(resolvedDirect).annotationKind)
                assertFalse(annotation(resolvedDirect).annotationIdentity is CangjieAnnotationIdentity.LanguageBuiltIn)
            }
            val generatedAnnotation = annotation(resolvedGenerated)
            assertEquals("", generatedAnnotation.sourceModuleName)
            assertNull(generatedAnnotation.annotationKind)
            assertFalse(generatedAnnotation.annotationIdentity is CangjieAnnotationIdentity.LanguageBuiltIn)
            assertTrue(generatedAnnotation.annotationResolveState != CfirAnnotationResolveState.UNRESOLVED)
        }
    }

    @Test
    fun lightTreeCommonClassConstructorKeepsCjoIdentifierPosition() {
        val session = createResolveSession()
        val sourceText = "package cjmp_p\npublic common class P(x: Int64) {}"
        val sourceFile = CjInMemoryTextSourceFile("common.cj", null, sourceText)
        val built = LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
            parseLightFile(sourceText),
            sourceFile,
            sourceText.toSourceLinesMapping(),
        )
        val rawFile = built.cfirFile
        recordRawCfirFile(built, session)

        val resolvedFile = resolveThroughTypes(rawFile, session)
        val primaryConstructor = resolvedFile.declarations.filterIsInstance<CfirClass>().single()
            .declarations.filterIsInstance<CfirConstructor>().single { it.isPrimary }
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(resolvedFile))
        val constructorMetadata = metadata.declarations.single {
            it.kind == DeclKind.FuncDecl && it.identifier == primaryConstructor.symbol.name.asString()
        }
        val identifierPosition = checkNotNull(constructorMetadata.identifierPosition)

        assertEquals(2, identifierPosition.line)
        assertEquals(sourceText.lineSequence().elementAt(1).indexOf("P") + 1, identifierPosition.column)
    }

    @Test
    fun lightTreeSecondaryConstructorKeepsCjoIdentifierPosition() {
        val session = createResolveSession()
        val sourceText = "package cjmp_p\npublic common class P {\n    public init(y: Int64) {}\n}"
        val sourceFile = CjInMemoryTextSourceFile("common.cj", null, sourceText)
        val built = LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
            parseLightFile(sourceText),
            sourceFile,
            sourceText.toSourceLinesMapping(),
        )
        val rawFile = built.cfirFile
        recordRawCfirFile(built, session)

        val resolvedFile = resolveThroughTypes(rawFile, session)
        val classDeclaration = resolvedFile.declarations.filterIsInstance<CfirClass>().single()
        val constructors = classDeclaration.declarations.filterIsInstance<CfirConstructor>()
        val secondaryConstructor = constructors.firstOrNull { !it.isPrimary }
            ?: error("LightTree failed to retain secondary constructor: ${constructors.map { it.isPrimary }}")
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(resolvedFile))
        val constructorMetadata = metadata.declarations.singleOrNull {
            it.kind == DeclKind.FuncDecl &&
                    it.identifier == secondaryConstructor.symbol.name.asString() &&
                    it.identifierPosition?.line == 3
        } ?: error("CJO metadata omitted LightTree secondary init position: ${metadata.declarations.map { Triple(it.identifier, it.kind, it.identifierPosition) }}")
        val identifierPosition = checkNotNull(constructorMetadata.identifierPosition)

        assertEquals(3, identifierPosition.line)
        assertEquals(sourceText.lineSequence().elementAt(2).indexOf("init") + 1, identifierPosition.column)
    }

    @Test
    fun psiOperatorFunctionKeepsCjoIdentifierPosition() {
        assertOperatorFunctionIdentifierPosition(useLightTree = false)
    }

    @Test
    fun lightTreeOperatorFunctionKeepsCjoIdentifierPosition() {
        assertOperatorFunctionIdentifierPosition(useLightTree = true)
    }

    @Test
    fun psiExtendKeepsCjoIdentifierPositionAtTargetType() {
        assertExtendIdentifierPosition(useLightTree = false)
    }

    @Test
    fun lightTreeExtendKeepsCjoIdentifierPositionAtTargetType() {
        assertExtendIdentifierPosition(useLightTree = true)
    }

    @Test
    fun psiGenericExtendKeepsCjoIdentifierPositionAtTypeParameterStart() {
        assertExtendIdentifierPosition(useLightTree = false, generic = true)
    }

    @Test
    fun lightTreeGenericExtendKeepsCjoIdentifierPositionAtTypeParameterStart() {
        assertExtendIdentifierPosition(useLightTree = true, generic = true)
    }

    @Test
    fun psiPropertyAccessorsKeepCjoIdentifierPosition() {
        assertPropertyAccessorIdentifierPositions(useLightTree = false)
    }

    @Test
    fun lightTreePropertyAccessorsKeepCjoIdentifierPosition() {
        assertPropertyAccessorIdentifierPositions(useLightTree = true)
    }

    @Test
    fun actualDeclarationNameLookupDoesNotInventAFileNameRange() {
        val sourceText = "package cjmp_no_declaration_name"
        val psiSource = createCjFile("noDeclaration.cj", sourceText).toCjPsiSourceElement()
        val lightFile = parseLightFile(sourceText)
        val lightSource = org.cangnova.cangjie.source.CjLightSourceElement(
            lightFile.tree.root,
            0,
            sourceText.length,
            lightFile.tree,
        )

        assertNull(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME.markOrNull(psiSource))
        assertNull(SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME.markOrNull(lightSource))
    }

    private fun assertOperatorFunctionIdentifierPosition(useLightTree: Boolean) {
        val session = createResolveSession()
        val sourceText = """
            package cjmp_operator_position
            public class Position {
                public operator func +(other: Position): Position {
                    return Position()
                }
            }
        """.trimIndent()
        val built = if (useLightTree) {
            LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
                parseLightFile(sourceText),
                CjInMemoryTextSourceFile("operator.cj", null, sourceText),
                sourceText.toSourceLinesMapping(),
            )
        } else {
            PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(createCjFile("operator", sourceText))
        }
        recordRawCfirFile(built, session)

        val resolvedFile = resolveThroughTypes(built.cfirFile, session)
        val metadata = CfirCjoPackageMetadataProducer.produce(listOf(resolvedFile))
        val operatorFunction = resolvedFile.declarations.filterIsInstance<CfirClass>().single()
            .declarations.filterIsInstance<CfirNamedFunction>().single()
        val operatorMetadata = metadata.declarations.singleOrNull {
            it.kind == DeclKind.FuncDecl && it.identifier == operatorFunction.name.asString()
        } ?: error("CJO omitted operator ${operatorFunction.name}: ${metadata.declarations.map { it.identifier to it.kind }}")
        val identifierPosition = checkNotNull(operatorMetadata.identifierPosition)
        val operatorLine = sourceText.lineSequence().withIndex().single { (_, line) ->
            "operator func +" in line
        }

        assertEquals(operatorLine.index + 1, identifierPosition.line)
        assertEquals(operatorLine.value.indexOf('+') + 1, identifierPosition.column)
    }

    private fun assertPropertyAccessorIdentifierPositions(useLightTree: Boolean) {
        val session = createResolveSession()
        val sourceText = """
            package cjmp_property_accessor_position
            public class Position {
                public init() {}
                public mut prop value: Int64 {
                    get() { 0 }
                    set(next) {}
                }
            }
        """.trimIndent()
        val built = if (useLightTree) {
            LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
                parseLightFile(sourceText),
                CjInMemoryTextSourceFile("property-accessor.cj", null, sourceText),
                sourceText.toSourceLinesMapping(),
            )
        } else {
            PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(createCjFile("property-accessor", sourceText))
        }
        recordRawCfirFile(built, session)

        val resolvedFile = resolveThroughTypes(built.cfirFile, session)
        val accessors = CfirCjoPackageMetadataProducer.produce(listOf(resolvedFile)).declarations
            .filter { it.kind == DeclKind.FuncDecl && it.identifier in setOf("get", "set") }
        assertEquals(setOf("get", "set"), accessors.map { it.identifier }.toSet())
        accessors.forEach { accessor ->
            val line = sourceText.lineSequence().withIndex().single { (_, text) ->
                text.trimStart().startsWith("${accessor.identifier}(")
            }
            val identifierPosition = checkNotNull(accessor.identifierPosition)
            assertEquals(line.index + 1, identifierPosition.line)
            assertEquals(line.value.indexOf(accessor.identifier) + 1, identifierPosition.column)
        }
    }

    private fun assertExtendIdentifierPosition(useLightTree: Boolean, generic: Boolean = false) {
        val session = createResolveSession()
        val sourceText = """
            package cjmp_extend_position
            public interface Marker {}
            ${if (generic) "public class Box<T> {}" else "public class Box {}"}
            ${if (generic) "extend<T> Box<T> <: Marker {}" else "extend Box <: Marker {}"}
        """.trimIndent()
        val built = if (useLightTree) {
            LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
                parseLightFile(sourceText),
                CjInMemoryTextSourceFile("extend.cj", null, sourceText),
                sourceText.toSourceLinesMapping(),
            )
        } else {
            PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(createCjFile("extend", sourceText))
        }
        recordRawCfirFile(built, session)

        val resolvedFile = resolveThroughTypes(built.cfirFile, session)
        val extendMetadata = CfirCjoPackageMetadataProducer.produce(listOf(resolvedFile)).declarations
            .single { it.kind == DeclKind.ExtendDecl }
        val identifierPosition = checkNotNull(extendMetadata.identifierPosition)
        val targetLine = sourceText.lineSequence().withIndex().single { (_, line) ->
            line.startsWith("extend")
        }

        assertEquals(targetLine.index + 1, identifierPosition.line)
        val identifierOffset = if (generic) targetLine.value.indexOf('<') else targetLine.value.indexOf("Box")
        assertEquals(identifierOffset + 1, identifierPosition.column)
    }

    @Test
    fun invalidExplicitReturnDoesNotPoisonAValidInferredBodyTail() {
        val session = createResolveSession()
        val sourceText = """
            package inferred_return_error
            public func inferred(flag: Bool) {
                if (flag) {
                    return missing
                }
                1
            }
        """.trimIndent()
        val builder = PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
        val built = builder.buildPreMacroCfirFile(createCjFile("inferredReturnError", sourceText))
        val rawFile = built.cfirFile
        recordRawCfirFile(built, session)

        val typedFile = resolveThroughTypes(rawFile, session)
        val resolvedFile = resolveThroughBody(typedFile, session)
        val function = resolvedFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val inferredReturnType = function.returnTypeRef.coneTypeOrNull as? ConePrimitiveType

        assertEquals(PrimitiveTypeKind.INT64, inferredReturnType?.kind)
    }

    @Test
    fun nestedInvalidExplicitReturnDoesNotPoisonAValidInferredBodyTail() {
        val session = createResolveSession()
        val sourceText = """
            package inferred_return_nested_error
            public func inferred(flag: Bool) {
                if (flag) {
                    return (missing, 0)
                }
                1
            }
        """.trimIndent()
        val builder = PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
        val built = builder.buildPreMacroCfirFile(createCjFile("inferredNestedReturnError", sourceText))
        val rawFile = built.cfirFile
        recordRawCfirFile(built, session)

        val typedFile = resolveThroughTypes(rawFile, session)
        val resolvedFile = resolveThroughBody(typedFile, session)
        val function = resolvedFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val ifExpression = function.body!!.statements.filterIsInstance<CfirIfExpression>().single()
        val returnExpression = ifExpression.thenBranch.statements.filterIsInstance<CfirReturnExpression>().single()
        val nestedReturnType = returnExpression.result.coneTypeOrNull as? ConeTupleType
        val inferredReturnType = function.returnTypeRef.coneTypeOrNull as? ConePrimitiveType

        assertTrue(nestedReturnType?.containsErrorType() == true)
        assertEquals(ConePrimitiveType.IDEAL_INT, nestedReturnType?.elementTypes?.getOrNull(1))
        assertEquals(PrimitiveTypeKind.INT64, inferredReturnType?.kind)
    }

    @Test
    fun nestedInvalidBodyTailPropagatesInsteadOfCreatingAJoinFailure() {
        val session = createResolveSession()
        val sourceText = """
            package inferred_return_nested_tail_error
            public func inferred(flag: Bool) {
                if (flag) {
                    return 1
                }
                (missing, 0)
            }
        """.trimIndent()
        val builder = PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
        val built = builder.buildPreMacroCfirFile(createCjFile("inferredNestedTailError", sourceText))
        val rawFile = built.cfirFile
        recordRawCfirFile(built, session)

        val typedFile = resolveThroughTypes(rawFile, session)
        val resolvedFile = resolveThroughBody(typedFile, session)
        val function = resolvedFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val inferredReturnType = function.returnTypeRef.coneTypeOrNull as? ConeTupleType

        assertTrue(inferredReturnType?.containsErrorType() == true)
        assertEquals(ConePrimitiveType.IDEAL_INT, inferredReturnType?.elementTypes?.getOrNull(1))
    }

    @Test
    fun inferredReturnTypeDefaultingUpdatesTheReturnExpressions() {
        val session = createResolveSession()
        val sourceText = """
            package inferred_return_ideal_types
            public func inferred(flag: Bool) {
                if (flag) {
                    return 1
                }
                2
            }
        """.trimIndent()
        val builder = PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
        val built = builder.buildPreMacroCfirFile(createCjFile("inferredReturnIdealTypes", sourceText))
        val rawFile = built.cfirFile
        recordRawCfirFile(built, session)

        val typedFile = resolveThroughTypes(rawFile, session)
        val resolvedFile = resolveThroughBody(typedFile, session)
        val function = resolvedFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val ifExpression = function.body!!.statements.filterIsInstance<CfirIfExpression>().single()
        val returnExpression = ifExpression.thenBranch.statements.filterIsInstance<CfirReturnExpression>().single()
        val explicitReturnType = returnExpression.result.coneTypeOrNull as? ConePrimitiveType
        val bodyTailType = (function.body!!.statements.last() as? CfirExpression)
            ?.coneTypeOrNull as? ConePrimitiveType
        val bodyType = function.body!!.coneTypeOrNull as? ConePrimitiveType

        assertEquals(PrimitiveTypeKind.INT64, explicitReturnType?.kind)
        assertEquals(PrimitiveTypeKind.INT64, bodyTailType?.kind)
        assertEquals(PrimitiveTypeKind.INT64, bodyType?.kind)
    }

    /** 与 frontend 使用同一工厂完整创建 shared/library/source 会话，不逐项拼装解析组件。 */
    private fun createResolveSession(): CfirSession {
        val moduleName = Name.special("<annotation-source-module-test>")
        val configuration = CompilerConfiguration().apply {
            diagnosticFactoriesStorage = CjRegisteredDiagnosticFactoriesStorage()
        }
        val dependencies = DependencyListForCliModule.build(moduleName)
        val factory = CfirDefaultSessionFactory()
        return CfirSessionConstructionUtils.prepareSessions(
            files = listOf("annotationSource.cj"),
            configuration = configuration,
            rootModuleName = moduleName,
            dependencyList = dependencies,
            createSharedLibrarySession = {
                factory.createSharedLibrarySession(
                    mainModuleName = moduleName,
                    extensionRegistrars = emptyList(),
                    languageVersionSettings = configuration.languageVersionSettings,
                )
            },
            createLibrarySession = { shared ->
                factory.createLibrarySession(
                    sharedLibrarySession = shared,
                    moduleDataProvider = dependencies.moduleDataProvider,
                    extensionRegistrars = emptyList(),
                    languageVersionSettings = configuration.languageVersionSettings,
                )
            },
            createSourceSession = CfirSessionProducer<String> { _, moduleData, _, configurator ->
                factory.createSourceSession(
                    moduleData = moduleData,
                    extensionRegistrars = emptyList(),
                    configuration = configuration,
                    init = configurator,
                )
            },
        ).single().session
    }

    /** 使用正式阶段处理器停在 TYPES，防止后续阶段掩盖类型身份解析结果。 */
    private fun resolveThroughTypes(file: CfirFile, session: CfirSession): CfirFile {
        var files = listOf(file)
        for (phase in listOf(CfirResolvePhase.IMPORTS, CfirResolvePhase.SUPER_TYPES, CfirResolvePhase.TYPES)) {
            val processor = checkNotNull(session.phaseResolverRegistry.getProcessor(phase))
            processor.beforePhase()
            try {
                when (processor) {
                    is CfirFileReplacingResolveProcessor -> files = processor.processAndReplace(files)
                    is CfirGlobalResolveProcessor -> processor.process(files)
                    is CfirTransformerBasedResolveProcessor -> files.forEach(processor::processFile)
                }
            } finally {
                processor.afterPhase()
            }
        }
        return files.single()
    }

    /** 在正式 TYPES 之后继续执行隐式签名与函数体解析，读取函数体返回类型推断结果。 */
    private fun resolveThroughBody(file: CfirFile, session: CfirSession): CfirFile {
        var files = listOf(file)
        for (phase in CfirResolvePhase.entries.filter { it > CfirResolvePhase.TYPES && it <= CfirResolvePhase.BODY_RESOLVE }) {
            val processor = checkNotNull(session.phaseResolverRegistry.getProcessor(phase))
            processor.beforePhase()
            try {
                when (processor) {
                    is CfirFileReplacingResolveProcessor -> files = processor.processAndReplace(files)
                    is CfirGlobalResolveProcessor -> processor.process(files)
                    is CfirTransformerBasedResolveProcessor -> files.forEach(processor::processFile)
                }
            } finally {
                processor.afterPhase()
            }
        }
        return files.single()
    }

    private fun recordRawCfirFile(file: PreMacroCfirFile, session: CfirSession) {
        val provider = session.cfirProvider as CfirProviderImpl
        val pre = buildPreMacroRawFiles(session, listOf(file))
        val result = MacroConstructionService.Identity.expandWithDefaultContext(
            pre = pre,
            mode = MacroConstructionService.Mode.STRICT,
        ) as MacroConstructionResult.Success
        recordExpandedRawFilesOnce(provider, result.recordableFiles, result.registry)
    }

    private fun annotation(function: CfirNamedFunction): CfirAnnotationCall =
        function.annotations.single() as CfirAnnotationCall

    private fun parseLightTree(source: String): FlyweightCapableTreeStructure<LighterASTNode> =
        parseLightFile(source).tree

    private fun parseLightFile(source: String): org.cangnova.cangjie.parsing.ParsedCangJieLightFile {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        return CangJieLightParser.parse(builder, languageModuleName = "")
    }

    private data class SourceContext(
        val packageHeader: String,
        val packageFqName: FqName,
        val sourceModuleName: String,
    )

    companion object {
        private const val GENERATED_TOKENS = "@ConstSafe\nfunc generated(): Unit {}"
        private val SOURCE_CONTEXTS = listOf(
            SourceContext("package std.foo", FqName("std.foo"), "std"),
            SourceContext("package std::foo", FqName("foo"), "std"),
            SourceContext("package org::std.foo", FqName("std.foo"), "org"),
            SourceContext("package std", FqName("std"), ""),
            SourceContext("", FqName.ROOT, ""),
        )
    }
}
