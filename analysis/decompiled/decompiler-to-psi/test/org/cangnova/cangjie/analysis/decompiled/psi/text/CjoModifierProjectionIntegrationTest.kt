@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.decompiled.psi.text

import PackageFormat.ClassInfo
import PackageFormat.Decl
import PackageFormat.DeclKind
import PackageFormat.Package as CjoPackage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.cases.session.builder.StandaloneBuilderPlatformTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneLibraryModule
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.CaStandaloneSourceModule
import org.cangnova.cangjie.analysis.api.standalone.session.CaStandaloneSessionBuilder
import org.cangnova.cangjie.analysis.api.symbols.CaModifierExplicitness
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.decompiler.stub.CjoDeclarationLoader
import org.cangnova.cangjie.analysis.decompiler.stub.CjoFileStubBuilder
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.services.environmentManager
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirModifierExplicitness
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.resolvedWithEffectiveModality
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.serializedInteropFacts
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpResolver
import org.cangnova.cangjie.cfir.resolve.transformers.CfirStatusResolver
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.stubs.impl.CangJieClassStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

/**
 * 验证真实 SDK CJO 的 visibility 经公开 Analysis API、二进制 stub 和反编译文本完整传播。
 *
 * 零位、多位及 CJMP 组合场景直接改写 canonical FlatBuffers fixture 的 attributes vector，
 * 再经 CjoManager 和正常 stub builder 加载；测试不会 mock renderer，也不复制 CJO fixture。
 */
class CjoModifierProjectionIntegrationTest : AbstractAnalysisApiBasedTest() {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> = listOf(
        CaAnalysisApiDecompiledTestServiceRegistrar,
        StandaloneBuilderPlatformTestServiceRegistrar,
    )

    @Test
    fun canonicalObjectPoolVisibilityFlowsFromCjoStatusToStubAndText() = withCoreEnvironment { environment ->
        withCanonicalCjo { libraryPath ->
            withLoadedObjectPool(libraryPath) { loadedPackage, module ->
                val declarations = CjoDeclarationLoader.loadDeclarations(loadedPackage, module)
                val objectPool = declarations.filterIsInstance<CfirClass>().single { it.name.asString() == OBJECT_POOL_NAME }
                assertTrue(objectPool.status is CfirResolvedDeclarationStatus)
                assertEquals(Visibilities.Public, objectPool.symbol.cfir.status.visibility)
                val methods = objectPoolMethods(declarations)
                assertTrue(methods.isNotEmpty(), "std.objectpool.ObjectPool must come from the canonical CJO fixture")
                assertTrue(methods.any { it.name.asString() == "get" })
                assertTrue(methods.any { it.name.asString() == "put" })
                methods.forEach { method ->
                    assertTrue(method.status is CfirResolvedDeclarationStatus, "CJO status must be published as resolved")
                    assertEquals(method.status.modality, method.symbol.resolvedStatus.modality)
                    assertEquals(Visibilities.Public, method.symbol.resolvedStatus.visibility)
                    assertEquals(Visibilities.Public, method.status.visibility, method.name.asString())
                    assertEquals(
                        CfirModifierExplicitness.UNKNOWN,
                        method.status.visibilityExplicitness,
                        "CJO visibility explicitness is unknown for ${method.name}",
                    )
                }
                val expectedCompiledBodies = methods.associate { method ->
                    method.name.asString() to (
                            method.status.modality in setOf(
                                org.cangnova.cangjie.descriptors.Modality.FINAL,
                                org.cangnova.cangjie.descriptors.Modality.OPEN,
                            ) &&
                                    !method.status.isAbstract &&
                                    !method.status.isForeign &&
                                    method.serializedInteropFacts?.isIntrinsic != true
                            )
                }
                assertTrue(expectedCompiledBodies.values.any { it }, "ObjectPool.get/put must include concrete CJO members")
                assertPublicApiVisibility(libraryPath, CaSymbolVisibility.PUBLIC, repositoryRoot().resolve(CANONICAL_FIXTURE))

                val binaryStub = CjoFileStubBuilder.buildFileStub(loadedPackage, module)
                assertTrue(modifierStub(objectPoolClassStub(binaryStub)).hasModifier(CjTokens.PUBLIC_KEYWORD))
                val methodStubs = objectPoolFunctionStubs(binaryStub)
                assertEquals(
                    methods.groupingBy { it.name.asString() }.eachCount(),
                    methodStubs.groupingBy { it.getName() }.eachCount(),
                )
                methodStubs.forEach { functionStub ->
                    assertTrue(
                        modifierStub(functionStub).hasModifier(CjTokens.PUBLIC_KEYWORD),
                        "known public visibility must be present in the CJO stub mask for ${functionStub.getName()}",
                    )
                    assertEquals(
                        expectedCompiledBodies.getValue(functionStub.getName()),
                        functionStub.hasBody(),
                        "compiled body placeholder must follow effective CJO modality for ${functionStub.getName()}",
                    )
                }

                val rendered = buildDecompiledText(binaryStub)
                assertTrue(objectPoolPsiClass(rendered, environment).hasModifier(CjTokens.PUBLIC_KEYWORD))
                val renderedMethods = objectPoolPsiMethods(rendered, environment)
                assertEquals(
                    methods.groupingBy { it.name.asString() }.eachCount(),
                    renderedMethods.groupingBy { it.name }.eachCount(),
                )
                renderedMethods.forEach { function ->
                    val functionName = requireNotNull(function.name)
                    assertTrue(function.hasModifier(CjTokens.PUBLIC_KEYWORD), "$functionName must render public")
                    assertEquals(
                        expectedCompiledBodies.getValue(functionName),
                        function.hasBody(),
                        "decompiled PSI must retain the compiled body placeholder for $functionName",
                    )
                }
            }
        }
    }

    @Test
    fun compiledBodyProjectionUsesMemberStatusAndJavaMirrorHostContext() = withCoreEnvironment { environment ->
        BODY_PROJECTION_CASES.forEach { testCase ->
            var mutatedBytes = replaceObjectPoolMethodAttributes(
                originalBytes = canonicalFixtureBytes(),
                replacedAttributes = BODY_ATTRIBUTES,
                desiredAttributes = testCase.memberAttributes,
            )
            mutatedBytes = replaceObjectPoolClassAttributes(
                originalBytes = mutatedBytes,
                replacedAttributes = HOST_ATTRIBUTES,
                desiredAttributes = testCase.hostAttributes,
            )
            withTemporaryCjo(mutatedBytes) { libraryPath ->
                withLoadedObjectPool(libraryPath) { loadedPackage, module ->
                    val declarations = CjoDeclarationLoader.loadDeclarations(loadedPackage, module)
                    val objectPool = declarations.filterIsInstance<CfirClass>().single { it.name.asString() == OBJECT_POOL_NAME }
                    assertEquals(testCase.hostIsJavaMirror, objectPool.serializedInteropFacts?.isJavaMirror)
                    assertEquals(testCase.hostIsObjCMirror, objectPool.serializedInteropFacts?.isObjCMirror)
                    assertEquals(testCase.hostIsAbstract, objectPool.status.isAbstract)

                    val methods = objectPool.declarations.filterIsInstance<CfirNamedFunction>()
                        .filter { it.name.asString() in TARGET_METHODS }
                    assertEquals(TARGET_METHODS, methods.map { it.name.asString() }.toSet())
                    methods.forEach { method ->
                        assertEquals(testCase.modality, method.status.modality, "${testCase.description}: ${method.name} modality")
                        assertEquals(testCase.isForeign, method.status.isForeign, "${testCase.description}: ${method.name} foreign")
                        assertEquals(
                            testCase.isIntrinsic,
                            method.serializedInteropFacts?.isIntrinsic,
                            "${testCase.description}: ${method.name} intrinsic",
                        )
                    }

                    val expectedBodies = methods.associate { it.name.asString() to testCase.expectedBody }
                    val binaryStub = CjoFileStubBuilder.buildFileStub(loadedPackage, module)
                    val methodStubs = objectPoolFunctionStubs(binaryStub)
                    assertEquals(methods.size, methodStubs.size)
                    methodStubs.forEach { functionStub ->
                        assertEquals(
                            testCase.expectedBody,
                            functionStub.hasBody(),
                            "${testCase.description}: CJO function stub body for ${functionStub.getName()}",
                        )
                        assertEquals(
                            testCase.hostIsJavaMirror && testCase.modality == org.cangnova.cangjie.descriptors.Modality.ABSTRACT,
                            modifierStub(functionStub).hasModifier(CjTokens.ABSTRACT_KEYWORD),
                            "${testCase.description}: abstract modifier must follow the host grammar",
                        )
                    }

                    val rendered = buildDecompiledText(binaryStub)
                    if (testCase.hostIsJavaMirror) assertTrue(rendered.contains("@JavaMirror"), rendered)
                    if (testCase.hostIsObjCMirror) assertTrue(rendered.contains("@ObjCMirror"), rendered)
                    val renderedMethods = objectPoolPsiMethods(rendered, environment)
                    assertEquals(methods.size, renderedMethods.size)
                    renderedMethods.forEach { function ->
                        val functionName = requireNotNull(function.name)
                        assertEquals(
                            expectedBodies.getValue(functionName),
                            function.hasBody(),
                            "${testCase.description}: decompiled PSI body for $functionName",
                        )
                        assertEquals(
                            testCase.hostIsJavaMirror && testCase.modality == org.cangnova.cangjie.descriptors.Modality.ABSTRACT,
                            function.hasModifier(CjTokens.ABSTRACT_KEYWORD),
                            "${testCase.description}: decompiled abstract modifier for $functionName",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun abstractMemberInCommonAbstractHostKeepsRequiredModifier() = withCoreEnvironment { environment ->
        val members = replaceObjectPoolMethodAttributes(
            canonicalFixtureBytes(), BODY_ATTRIBUTES, setOf(Attribute.ABSTRACT),
        )
        val bytes = replaceObjectPoolClassAttributes(
            members, HOST_ATTRIBUTES + CJMP_ATTRIBUTES, setOf(Attribute.ABSTRACT, Attribute.COMMON),
        )
        withTemporaryCjo(bytes) { libraryPath ->
            withLoadedObjectPool(libraryPath) { loadedPackage, module ->
                val stub = CjoFileStubBuilder.buildFileStub(loadedPackage, module)
                val functions = objectPoolFunctionStubs(stub)
                assertTrue(functions.isNotEmpty())
                functions.forEach {
                    assertTrue(modifierStub(it).hasModifier(CjTokens.ABSTRACT_KEYWORD))
                    assertFalse(it.hasBody())
                }
                val rendered = objectPoolPsiMethods(buildDecompiledText(stub), environment)
                assertEquals(functions.size, rendered.size)
                rendered.forEach {
                    assertTrue(it.hasModifier(CjTokens.ABSTRACT_KEYWORD))
                    assertFalse(it.hasBody())
                }
            }
        }
    }

    @Test
    fun commonHostModifierDoesNotLeakToItsMembers() = withCoreEnvironment { environment ->
        val mutatedBytes = replaceObjectPoolClassAttributes(
            originalBytes = canonicalFixtureBytes(),
            replacedAttributes = CJMP_ATTRIBUTES,
            desiredAttributes = setOf(Attribute.COMMON),
        )
        withTemporaryCjo(mutatedBytes) { libraryPath ->
            withLoadedObjectPool(libraryPath) { loadedPackage, module ->
                val binaryStub = CjoFileStubBuilder.buildFileStub(loadedPackage, module)
                val objectPoolStub = objectPoolClassStub(binaryStub)
                assertTrue(modifierStub(objectPoolStub).hasModifier(CjTokens.COMMON_KEYWORD))

                val methodStubs = objectPoolFunctionStubs(binaryStub)
                assertTrue(methodStubs.isNotEmpty())
                methodStubs.forEach { methodStub ->
                    assertFalse(
                        modifierStub(methodStub).hasModifier(CjTokens.COMMON_KEYWORD),
                        "owner common status must not be inherited by ${methodStub.getName()}",
                    )
                }

                val rendered = buildDecompiledText(binaryStub)
                val objectPoolPsi = PsiFileFactory.getInstance(environment.project)
                    .createFileFromText("stdObjectPool.decompiled.cj", CangJieFileType.INSTANCE, rendered) as CjFile
                val objectPoolClass = PsiTreeUtil.findChildrenOfType(objectPoolPsi, CjClass::class.java)
                    .single { it.name == OBJECT_POOL_NAME }
                assertTrue(objectPoolClass.hasModifier(CjTokens.COMMON_KEYWORD))
                PsiTreeUtil.findChildrenOfType(objectPoolClass, CjNamedFunction::class.java)
                    .filter { it.name in TARGET_METHODS }
                    .forEach { function ->
                        assertFalse(
                            function.hasModifier(CjTokens.COMMON_KEYWORD),
                            "owner common status must not render on ${function.name}",
                        )
                    }
            }
        }
    }

    @Test
    fun javaAndObjCMirrorHostsKeepDistinctCjmpProjectionRules() = withCoreEnvironment { environment ->
        for ((hostAttribute, permitsModifier) in listOf(Attribute.JAVA_MIRROR to false, Attribute.OBJ_C_MIRROR to true)) {
            for ((memberAttribute, keyword) in listOf(
                Attribute.COMMON to CjTokens.COMMON_KEYWORD,
                Attribute.SPECIFIC to CjTokens.SPECIFIC_KEYWORD,
            )) {
                val members = replaceObjectPoolMethodAttributes(
                    canonicalFixtureBytes(), setOf(Attribute.COMMON, Attribute.SPECIFIC), setOf(memberAttribute),
                )
                val bytes = replaceObjectPoolClassAttributes(members, HOST_ATTRIBUTES, setOf(hostAttribute))
                withTemporaryCjo(bytes) { libraryPath ->
                    withLoadedObjectPool(libraryPath) { loadedPackage, module ->
                        val stub = CjoFileStubBuilder.buildFileStub(loadedPackage, module)
                        val methodStubs = objectPoolFunctionStubs(stub)
                        assertTrue(methodStubs.isNotEmpty())
                        methodStubs.forEach { method ->
                            assertEquals(permitsModifier, modifierStub(method).hasModifier(keyword), "$hostAttribute / $memberAttribute")
                        }
                        val methods = objectPoolPsiMethods(buildDecompiledText(stub), environment)
                        assertEquals(methodStubs.size, methods.size)
                        methods.forEach { method ->
                            assertEquals(permitsModifier, method.hasModifier(keyword), "$hostAttribute / $memberAttribute")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun cjmpAttributesRemainIndependentAndOnlyCommonIsProjected() = withCoreEnvironment { environment ->
        CJMP_ATTRIBUTE_CASES.forEach { testCase ->
            val mutatedBytes = replaceObjectPoolMethodAttributes(
                originalBytes = canonicalFixtureBytes(),
                replacedAttributes = CJMP_ATTRIBUTES,
                desiredAttributes = testCase.attributes,
            )
            withTemporaryCjo(mutatedBytes) { libraryPath ->
                withLoadedObjectPool(libraryPath) { loadedPackage, module ->
                    val declarations = CjoDeclarationLoader.loadDeclarations(loadedPackage, module)
                    val objectPool = declarations.filterIsInstance<CfirClass>().single { it.name.asString() == OBJECT_POOL_NAME }
                    val methods = objectPool.declarations.filterIsInstance<CfirNamedFunction>()
                        .filter { it.name.asString() in TARGET_METHODS }
                    val statusResolver = CfirStatusResolver(module.session, ScopeSession())
                    assertTrue(methods.isNotEmpty(), "${testCase.description}: ObjectPool members must deserialize")
                    assertTrue(methods.any { it.name.asString() == "get" })
                    assertTrue(methods.any { it.name.asString() == "put" })
                    methods.forEach { method ->
                        assertEquals(testCase.isCommon, method.status.isCommon, "${testCase.description}: ${method.name} common")
                        assertEquals(
                            testCase.isFromCommonPart,
                            method.status.isFromCommonPart,
                            "${testCase.description}: ${method.name} from-common-part",
                        )
                        val original = method.status
                        val unresolved = CfirDeclarationStatusImpl(original.visibility, original.modality).apply {
                            isCommon = original.isCommon
                            isFromCommonPart = original.isFromCommonPart
                            isSpecific = original.isSpecific
                            isAbstract = original.isAbstract
                            isOpen = original.isOpen
                            isForeign = original.isForeign
                            isOverride = original.isOverride
                            isDefault = original.isDefault
                            visibilityExplicitness = CfirModifierExplicitness.UNKNOWN
                            modalityExplicitness = CfirModifierExplicitness.UNKNOWN
                        }
                        val resolvedCopy = unresolved.resolvedWithEffectiveModality()
                        assertNotSame(unresolved, resolvedCopy)
                        assertEquals(testCase.isCommon, resolvedCopy.isCommon, "${testCase.description}: resolved common copy")
                        assertEquals(
                            testCase.isFromCommonPart,
                            resolvedCopy.isFromCommonPart,
                            "${testCase.description}: resolved from-common-part copy",
                        )
                        method.replaceStatus(unresolved)
                        val libraryResolved = try {
                            statusResolver.resolveStatus(
                                declaration = method,
                                containingClass = objectPool,
                                isLocal = false,
                                overriddenStatuses = emptyList(),
                            )
                        } finally {
                            method.replaceStatus(original)
                        }
                        assertEquals(original.modality, libraryResolved.modality)
                        assertEquals(original.visibility, libraryResolved.visibility)
                        assertEquals(testCase.isCommon, libraryResolved.isCommon, "${testCase.description}: Library STATUS common")
                        assertEquals(
                            testCase.isFromCommonPart,
                            libraryResolved.isFromCommonPart,
                            "${testCase.description}: Library STATUS from-common-part",
                        )
                        assertEquals(CfirModifierExplicitness.UNKNOWN, libraryResolved.visibilityExplicitness)
                        assertEquals(CfirModifierExplicitness.UNKNOWN, libraryResolved.modalityExplicitness)
                        val candidates = CfirCjmpResolver.findCommonMemberCandidates(method, objectPool)
                        assertEquals(
                            testCase.isCommon,
                            candidates.any { it.symbol == method.symbol },
                            "${testCase.description}: only true COMMON members may be pairing candidates",
                        )
                    }

                    val binaryStub = CjoFileStubBuilder.buildFileStub(loadedPackage, module)
                    val methodStubs = objectPoolFunctionStubs(binaryStub)
                    assertEquals(methods.groupingBy { it.name.asString() }.eachCount(), methodStubs.groupingBy { it.getName() }.eachCount())
                    methodStubs.forEach { functionStub ->
                        assertEquals(
                            testCase.isCommon,
                            modifierStub(functionStub).hasModifier(CjTokens.COMMON_KEYWORD),
                            "${testCase.description}: common modifier mask for ${functionStub.getName()}",
                        )
                    }

                    val renderedMethods = objectPoolPsiMethods(buildDecompiledText(binaryStub), environment)
                    assertEquals(methods.groupingBy { it.name.asString() }.eachCount(), renderedMethods.groupingBy { it.name }.eachCount())
                    renderedMethods.forEach { function ->
                        assertEquals(
                            testCase.isCommon,
                            function.hasModifier(CjTokens.COMMON_KEYWORD),
                            "${testCase.description}: rendered common modifier for ${function.name}",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun zeroVisibilityBitsDecodeAsUnknownAndAreOmittedEndToEnd() = withCoreEnvironment { environment ->
        assertMalformedVisibilityIsUnknownAndOmitted(
            environment = environment,
            visibilityAttributes = emptySet(),
            description = "zero visibility bits",
        )
    }

    @Test
    fun multipleVisibilityBitsDecodeAsUnknownAndAreOmittedEndToEnd() = withCoreEnvironment { environment ->
        assertMalformedVisibilityIsUnknownAndOmitted(
            environment = environment,
            visibilityAttributes = setOf(Attribute.PUBLIC, Attribute.PRIVATE),
            description = "multiple visibility bits",
        )
    }

    private fun assertMalformedVisibilityIsUnknownAndOmitted(
        environment: CangJieCoreEnvironment,
        visibilityAttributes: Set<Attribute>,
        description: String,
    ) {
        val mutatedBytes = replaceObjectPoolClassAttributes(
            replaceObjectPoolMethodVisibility(canonicalFixtureBytes(), visibilityAttributes),
            VISIBILITY_ATTRIBUTES,
            visibilityAttributes,
        )
        withTemporaryCjo(mutatedBytes) { libraryPath ->
            val manager = CjoManager(CjoSearchPath { key -> if (key == "CANGJIE_LIBRARY") libraryPath.toString() else null })
            val loadedPackage = loadObjectPoolPackage(manager)
            val module = TestModule()
            registerCjoSymbolProvider(module, manager)
            val declarations = CjoDeclarationLoader.loadDeclarations(loadedPackage, module)
            val objectPool = declarations.filterIsInstance<CfirClass>().single { it.name.asString() == OBJECT_POOL_NAME }
            assertTrue(objectPool.status is CfirResolvedDeclarationStatus)
            assertEquals(Visibilities.Unknown, objectPool.symbol.cfir.status.visibility, description)
            val methods = objectPoolMethods(declarations)
            assertTrue(methods.isNotEmpty(), "mutated CJO must still deserialize ObjectPool members")
            assertTrue(methods.any { it.name.asString() == "get" })
            assertTrue(methods.any { it.name.asString() == "put" })
            methods.forEach { method ->
                assertEquals(Visibilities.Unknown, method.status.visibility, "$description: ${method.name}")
                assertEquals(Visibilities.Unknown, method.symbol.resolvedStatus.visibility, "$description symbol: ${method.name}")
                assertEquals(
                    CfirModifierExplicitness.UNKNOWN,
                    method.status.visibilityExplicitness,
                    "$description explicitness: ${method.name}",
                )
            }
            assertPublicApiVisibility(libraryPath, CaSymbolVisibility.UNKNOWN)

            val binaryStub = CjoFileStubBuilder.buildFileStub(loadedPackage, module)
            VISIBILITY_MODIFIERS.forEach { modifier ->
                assertFalse(modifierStub(objectPoolClassStub(binaryStub)).hasModifier(modifier), description)
            }
            val methodStubs = objectPoolFunctionStubs(binaryStub)
            assertEquals(methods.groupingBy { it.name.asString() }.eachCount(), methodStubs.groupingBy { it.getName() }.eachCount())
            methodStubs.forEach { functionStub ->
                val modifierStub = modifierStub(functionStub)
                VISIBILITY_MODIFIERS.forEach { modifier ->
                    assertFalse(
                        modifierStub.hasModifier(modifier),
                        "$description must not project ${modifier.value} into the stub mask for ${functionStub.getName()}",
                    )
                }
            }

            val rendered = buildDecompiledText(binaryStub)
            VISIBILITY_MODIFIERS.forEach { modifier ->
                assertFalse(objectPoolPsiClass(rendered, environment).hasModifier(modifier), description)
            }
            val renderedMethods = objectPoolPsiMethods(rendered, environment)
            assertEquals(methods.groupingBy { it.name.asString() }.eachCount(), renderedMethods.groupingBy { it.name }.eachCount())
            renderedMethods.forEach { function ->
                VISIBILITY_MODIFIERS.forEach { modifier ->
                    assertFalse(
                        function.hasModifier(modifier),
                        "$description must omit ${modifier.value} in rendered ${function.name}",
                    )
                }
            }
        }
    }

    /** 以真实二进制依赖查询公开符号，并验证该查询实际命中了当前用例的 CJO 文件。 */
    @Suppress("DEPRECATION")
    private fun assertPublicApiVisibility(
        libraryPath: Path,
        expectedVisibility: CaSymbolVisibility,
        expectedCjoPath: Path = libraryPath.resolve(CjoConstants.packageNameToPath(OBJECT_POOL_PACKAGE)),
    ) {
        val environment = testServices.environmentManager.getCoreEnvironment()
        val binaryRoot = requireNotNull(StandardFileSystems.local().refreshAndFindFileByPath(libraryPath.toFile().invariantSeparatorsPath))
        val libraryDirectory = ApplicationManager.getApplication().runReadAction<com.intellij.psi.PsiDirectory> {
            requireNotNull(PsiManager.getInstance(environment.project).findDirectory(binaryRoot))
        }
        // 与 LLBinaryOriginLibrarySymbolProviderFactory 使用相同的根和搜索规则，排除环境标准库抢先命中。
        val snapshot = requireNotNull(
            CjoManager(CjoSearchPath(additionalLibrarySearchPaths = listOf(libraryPath.toFile())))
                .loadPackageSnapshot(OBJECT_POOL_PACKAGE),
        )
        assertEquals(
            expectedCjoPath.toRealPath(),
            snapshot.sourcePath.toRealPath(),
            "公开 Analysis API 必须读取当前用例的 canonical CJO",
        )
        val mainFile = requireNotNull(findMainFileAndModule(testServices).mainFile)
        val source = CaStandaloneSourceModule(
            name = "cjo-projection-use-site",
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            project = environment.project,
            psiRoots = listOf(mainFile),
        )
        val library = CaStandaloneLibraryModule(
            libraryName = "canonical-objectpool-${libraryPath.fileName}",
            project = environment.project,
            binaryRoots = listOf(libraryDirectory),
        )
        source.directRegularDependencies += library
        val context = CaStandaloneSessionBuilder(environment.project).build(source)
        ApplicationManager.getApplication().runReadAction {
            context.analyze(source) {
                val owner = requireNotNull(getClassSymbol(ClassId(FqName(OBJECT_POOL_PACKAGE), Name.identifier(OBJECT_POOL_NAME))))
                assertEquals(CaSymbolOrigin.LIBRARY, owner.origin)
                assertEquals(expectedVisibility, owner.visibility)
                assertEquals(CaModifierExplicitness.UNKNOWN, owner.visibilityExplicitness)
                assertFalse(owner.isVisibilityExplicit)
                val functions = owner.declaredMemberScope
                    .callables(Name.identifier("get"), Name.identifier("put"))
                    .filterIsInstance<CaNamedFunctionSymbol>()
                    .toList()
                assertEquals(TARGET_METHODS, functions.map { it.name.asString() }.toSet())
                functions.forEach { function ->
                    assertEquals(CaSymbolOrigin.LIBRARY, function.origin)
                    assertEquals(expectedVisibility, function.visibility, function.name.asString())
                    assertEquals(CaModifierExplicitness.UNKNOWN, function.visibilityExplicitness)
                    assertEquals(CaModifierExplicitness.UNKNOWN, function.modalityExplicitness)
                    assertFalse(function.isVisibilityExplicit)
                    assertFalse(function.isModalityExplicit)
                }
            }
        }
    }

    private fun objectPoolMethods(declarations: List<CfirDeclaration>): List<CfirNamedFunction> {
        val objectPool = declarations.filterIsInstance<CfirClass>().single { it.name.asString() == OBJECT_POOL_NAME }
        return objectPool.declarations.filterIsInstance<CfirNamedFunction>().filter { it.name.asString() in TARGET_METHODS }
    }

    private fun objectPoolFunctionStubs(fileStub: CangJieFileStubImpl): List<CangJieNamedFunctionStubImpl> {
        val objectPool = fileStub.descendants()
            .filterIsInstance<CangJieClassStubImpl>()
            .single { it.getName() == OBJECT_POOL_NAME }
        return objectPool.descendants()
            .filterIsInstance<CangJieNamedFunctionStubImpl>()
            .filter { it.getName() in TARGET_METHODS }
            .toList()
    }

    private fun objectPoolClassStub(fileStub: CangJieFileStubImpl): CangJieClassStubImpl = fileStub.descendants()
        .filterIsInstance<CangJieClassStubImpl>()
        .single { it.getName() == OBJECT_POOL_NAME }

    private fun objectPoolPsiMethods(text: String, environment: CangJieCoreEnvironment): List<CjNamedFunction> {
        return PsiTreeUtil.findChildrenOfType(objectPoolPsiClass(text, environment), CjNamedFunction::class.java)
            .filter { it.name in TARGET_METHODS }
    }

    private fun objectPoolPsiClass(text: String, environment: CangJieCoreEnvironment): CjClass {
        val file = PsiFileFactory.getInstance(environment.project)
            .createFileFromText("stdObjectPool.decompiled.cj", CangJieFileType.INSTANCE, text) as CjFile
        return PsiTreeUtil.findChildrenOfType(file, CjClass::class.java)
            .single { it.name == OBJECT_POOL_NAME }
    }

    private fun modifierStub(functionStub: CangJieNamedFunctionStubImpl): CangJieModifierListStubImpl =
        functionStub.childrenStubs.filterIsInstance<CangJieModifierListStubImpl>().single()

    private fun modifierStub(classStub: CangJieClassStubImpl): CangJieModifierListStubImpl =
        classStub.childrenStubs.filterIsInstance<CangJieModifierListStubImpl>().single()

    private fun StubElement<*>.descendants(): Sequence<StubElement<*>> = sequence {
        yield(this@descendants)
        childrenStubs.forEach { child -> yieldAll(child.descendants()) }
    }

    private fun canonicalFixtureBytes(): ByteArray {
        val fixture = repositoryRoot().resolve(CANONICAL_FIXTURE)
        assertTrue(Files.isRegularFile(fixture), "canonical CJO fixture missing: $fixture")
        return Files.readAllBytes(fixture)
    }

    private fun withCanonicalCjo(action: (Path) -> Unit) {
        val fixture = repositoryRoot().resolve(CANONICAL_FIXTURE)
        assertTrue(Files.isRegularFile(fixture), "canonical CJO fixture missing: $fixture")
        action(fixture.parent)
    }

    private fun replaceObjectPoolMethodVisibility(
        originalBytes: ByteArray,
        desiredVisibilityAttributes: Set<Attribute>,
    ): ByteArray = replaceObjectPoolMethodAttributes(
        originalBytes = originalBytes,
        replacedAttributes = VISIBILITY_ATTRIBUTES,
        desiredAttributes = desiredVisibilityAttributes,
    )

    private fun replaceObjectPoolMethodAttributes(
        originalBytes: ByteArray,
        replacedAttributes: Set<Attribute>,
        desiredAttributes: Set<Attribute>,
    ): ByteArray {
        require(desiredAttributes.all { it in replacedAttributes }) {
            "replacement attributes must be a subset of the attributes being replaced"
        }
        val bytes = originalBytes.copyOf()
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        val objectPoolIndex = (0 until pkg.allDeclsLength).firstOrNull { index ->
            val declaration = pkg.allDecls(index) ?: return@firstOrNull false
            declaration.kind == DeclKind.ClassDecl && declaration.identifier == OBJECT_POOL_NAME
        } ?: error("ObjectPool class was not found in the canonical CJO fixture")
        val objectPool = requireNotNull(pkg.allDecls(objectPoolIndex))
        val classInfo = requireNotNull(objectPool.info(ClassInfo()) as? ClassInfo)
        val memberDeclarations = (0 until classInfo.bodyLength).mapNotNull { position ->
            val encodedIndex = classInfo.body(position)
            if (encodedIndex == 0u || encodedIndex == UInt.MAX_VALUE) return@mapNotNull null
            val index = encodedIndex.toInt() - 1
            require(index in 0 until pkg.allDeclsLength) { "invalid ObjectPool member index $encodedIndex" }
            pkg.allDecls(index)
        }
        val targetDeclarations = memberDeclarations.filter { declaration ->
            declaration.kind == DeclKind.FuncDecl && declaration.identifier in TARGET_METHODS
        }
        assertTrue(
            targetDeclarations.mapNotNull { it.identifier }.toSet().containsAll(TARGET_METHODS),
            "canonical CJO fixture must include ObjectPool.get and ObjectPool.put",
        )

        targetDeclarations.forEach { declaration ->
            replaceAttributes(declaration, replacedAttributes, desiredAttributes)
        }
        return bytes
    }

    private fun replaceObjectPoolClassAttributes(
        originalBytes: ByteArray,
        replacedAttributes: Set<Attribute>,
        desiredAttributes: Set<Attribute>,
    ): ByteArray {
        require(desiredAttributes.all { it in replacedAttributes }) {
            "replacement attributes must be a subset of the attributes being replaced"
        }
        val bytes = originalBytes.copyOf()
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        val objectPoolIndex = (0 until pkg.allDeclsLength).firstOrNull { index ->
            val declaration = pkg.allDecls(index) ?: return@firstOrNull false
            declaration.kind == DeclKind.ClassDecl && declaration.identifier == OBJECT_POOL_NAME
        } ?: error("ObjectPool class was not found in the canonical CJO fixture")
        replaceAttributes(requireNotNull(pkg.allDecls(objectPoolIndex)), replacedAttributes, desiredAttributes)
        return bytes
    }

    private fun replaceAttributes(
        declaration: Decl,
        replacedAttributes: Set<Attribute>,
        desiredAttributes: Set<Attribute>,
    ) {
        val attributesByWord = replacedAttributes.groupBy { it.ordinal / Long.SIZE_BITS }
        val desiredByWord = desiredAttributes.groupBy { it.ordinal / Long.SIZE_BITS }
        (attributesByWord.keys + desiredByWord.keys).forEach { wordIndex ->
            val current = if (wordIndex < declaration.attributesLength) {
                declaration.attributes(wordIndex).toString().toULong()
            } else {
                0uL
            }
            val clearMask = attributesByWord[wordIndex].orEmpty().fold(0uL) { mask, attribute ->
                mask or (1uL shl (attribute.ordinal % Long.SIZE_BITS))
            }
            val desiredMask = desiredByWord[wordIndex].orEmpty().fold(0uL) { mask, attribute ->
                mask or (1uL shl (attribute.ordinal % Long.SIZE_BITS))
            }
            val updated = (current and clearMask.inv()) or desiredMask
            if (wordIndex < declaration.attributesLength) {
                assertTrue(declaration.mutateAttributes(wordIndex, updated), "CJO attributes must be mutable in-place")
            } else {
                assertEquals(0uL, desiredMask, "cannot add an attributes word to an existing FlatBuffers vector")
            }
        }
    }

    private fun withLoadedObjectPool(
        libraryPath: Path,
        action: (LoadedCjoPackage, TestModule) -> Unit,
    ) {
        val manager = CjoManager(CjoSearchPath { key -> if (key == "CANGJIE_LIBRARY") libraryPath.toString() else null })
        val module = TestModule()
        registerCjoSymbolProvider(module, manager)
        action(loadObjectPoolPackage(manager), module)
    }

    private fun registerCjoSymbolProvider(module: TestModule, manager: CjoManager) {
        module.session.register(
            CfirSymbolProvider::class,
            CfirDeserializedSymbolProvider(module.session, manager, CfirCangJieScopeProvider(), module),
        )
    }

    private fun loadObjectPoolPackage(manager: CjoManager): LoadedCjoPackage {
        val snapshot = requireNotNull(manager.loadPackageSnapshot(OBJECT_POOL_PACKAGE))
        return LoadedCjoPackage(
            binaryFile = LightVirtualFile("std.objectpool.cjo"),
            packageFqName = FqName(OBJECT_POOL_PACKAGE),
            pkg = snapshot.pkg,
            header = snapshot.header,
            cjoManager = manager,
            isVersionSupported = true,
            sourcePath = snapshot.sourcePath,
        )
    }

    private fun withTemporaryCjo(bytes: ByteArray, action: (Path) -> Unit) {
        val tempDirectory = Files.createTempDirectory("objectpool-visibility-cjo-")
        try {
            val target = tempDirectory.resolve(CjoConstants.packageNameToPath(OBJECT_POOL_PACKAGE))
            target.parent?.let { Files.createDirectories(it) }
            Files.write(target, bytes)
            action(tempDirectory)
        } finally {
            tempDirectory.toFile().deleteRecursively()
        }
    }

    private fun withCoreEnvironment(action: (CangJieCoreEnvironment) -> Unit) {
        runTest(repositoryRoot().resolve("analysis/decompiled/decompiler-to-psi/testData/cjoModifierProjection/anchor.cj").toString()) {
            action(it.environmentManager.getCoreEnvironment())
        }
    }

    private fun repositoryRoot(): Path = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
        .first { Files.isRegularFile(it.resolve("settings.gradle.kts")) }

    private class TestModule : CfirModuleData() {
        override val name: Name = Name.identifier("cjo-visibility-integration-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform: org.cangnova.cangjie.platform.TargetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = false
        override val stableModuleName: String = "cjo-visibility-integration-test"
        override val session: CfirSession = object : CfirSession(Kind.Library) {}.also { session ->
            session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            session.register(
                CfirLanguageSettingsComponent::class,
                CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT),
            )
            session.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        }

        init {
            bindSession(session)
        }
    }

    private companion object {
        const val OBJECT_POOL_PACKAGE = "std.objectpool"
        const val OBJECT_POOL_NAME = "ObjectPool"
        val TARGET_METHODS = setOf("get", "put")
        val VISIBILITY_ATTRIBUTES = setOf(Attribute.PUBLIC, Attribute.PRIVATE, Attribute.PROTECTED, Attribute.INTERNAL)
        val CJMP_ATTRIBUTES = setOf(Attribute.COMMON, Attribute.FROM_COMMON_PART)
        val BODY_ATTRIBUTES = setOf(Attribute.OPEN, Attribute.ABSTRACT, Attribute.FOREIGN, Attribute.INTRINSIC)
        val HOST_ATTRIBUTES = setOf(Attribute.OPEN, Attribute.ABSTRACT, Attribute.JAVA_MIRROR, Attribute.OBJ_C_MIRROR)
        val BODY_PROJECTION_CASES = listOf(
            BodyProjectionCase(
                description = "final member",
                memberAttributes = emptySet(),
                hostAttributes = emptySet(),
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL,
                isForeign = false,
                isIntrinsic = false,
                hostIsAbstract = false,
                hostIsJavaMirror = false,
                hostIsObjCMirror = false,
                expectedBody = true,
            ),
            BodyProjectionCase(
                description = "open member",
                memberAttributes = setOf(Attribute.OPEN),
                hostAttributes = emptySet(),
                modality = org.cangnova.cangjie.descriptors.Modality.OPEN,
                isForeign = false,
                isIntrinsic = false,
                hostIsAbstract = false,
                hostIsJavaMirror = false,
                hostIsObjCMirror = false,
                expectedBody = true,
            ),
            BodyProjectionCase(
                description = "abstract member",
                memberAttributes = setOf(Attribute.ABSTRACT),
                hostAttributes = setOf(Attribute.ABSTRACT),
                modality = org.cangnova.cangjie.descriptors.Modality.ABSTRACT,
                isForeign = false,
                isIntrinsic = false,
                hostIsAbstract = true,
                hostIsJavaMirror = false,
                hostIsObjCMirror = false,
                expectedBody = false,
            ),
            BodyProjectionCase(
                description = "foreign member",
                memberAttributes = setOf(Attribute.FOREIGN),
                hostAttributes = emptySet(),
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL,
                isForeign = true,
                isIntrinsic = false,
                hostIsAbstract = false,
                hostIsJavaMirror = false,
                hostIsObjCMirror = false,
                expectedBody = false,
            ),
            BodyProjectionCase(
                description = "intrinsic member",
                memberAttributes = setOf(Attribute.INTRINSIC),
                hostAttributes = emptySet(),
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL,
                isForeign = false,
                isIntrinsic = true,
                hostIsAbstract = false,
                hostIsJavaMirror = false,
                hostIsObjCMirror = false,
                expectedBody = false,
            ),
            BodyProjectionCase(
                description = "JavaMirror host",
                memberAttributes = emptySet(),
                hostAttributes = setOf(Attribute.JAVA_MIRROR),
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL,
                isForeign = false,
                isIntrinsic = false,
                hostIsAbstract = false,
                hostIsJavaMirror = true,
                hostIsObjCMirror = false,
                expectedBody = false,
            ),
            BodyProjectionCase(
                description = "abstract member in JavaMirror abstract host",
                memberAttributes = setOf(Attribute.ABSTRACT),
                hostAttributes = setOf(Attribute.JAVA_MIRROR, Attribute.ABSTRACT),
                modality = org.cangnova.cangjie.descriptors.Modality.ABSTRACT,
                isForeign = false,
                isIntrinsic = false,
                hostIsAbstract = true,
                hostIsJavaMirror = true,
                hostIsObjCMirror = false,
                expectedBody = false,
            ),
            BodyProjectionCase(
                description = "concrete member in JavaMirror abstract host",
                memberAttributes = emptySet(),
                hostAttributes = setOf(Attribute.JAVA_MIRROR, Attribute.ABSTRACT),
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL,
                isForeign = false,
                isIntrinsic = false,
                hostIsAbstract = true,
                hostIsJavaMirror = true,
                hostIsObjCMirror = false,
                expectedBody = false,
            ),
            BodyProjectionCase(
                description = "ObjCMirror host",
                memberAttributes = emptySet(),
                hostAttributes = setOf(Attribute.OBJ_C_MIRROR),
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL,
                isForeign = false,
                isIntrinsic = false,
                hostIsAbstract = false,
                hostIsJavaMirror = false,
                hostIsObjCMirror = true,
                expectedBody = true,
            ),
            BodyProjectionCase(
                description = "abstract host with concrete member",
                memberAttributes = emptySet(),
                hostAttributes = setOf(Attribute.ABSTRACT),
                modality = org.cangnova.cangjie.descriptors.Modality.FINAL,
                isForeign = false,
                isIntrinsic = false,
                hostIsAbstract = true,
                hostIsJavaMirror = false,
                hostIsObjCMirror = false,
                expectedBody = true,
            ),
        )
        val CJMP_ATTRIBUTE_CASES = listOf(
            CjmpAttributeCase("neither CJMP bit", emptySet(), isCommon = false, isFromCommonPart = false),
            CjmpAttributeCase("COMMON only", setOf(Attribute.COMMON), isCommon = true, isFromCommonPart = false),
            CjmpAttributeCase("FROM_COMMON_PART only", setOf(Attribute.FROM_COMMON_PART), isCommon = false, isFromCommonPart = true),
            CjmpAttributeCase(
                "COMMON and FROM_COMMON_PART",
                setOf(Attribute.COMMON, Attribute.FROM_COMMON_PART),
                isCommon = true,
                isFromCommonPart = true,
            ),
        )
        val VISIBILITY_MODIFIERS = listOf(
            CjTokens.PUBLIC_KEYWORD,
            CjTokens.PRIVATE_KEYWORD,
            CjTokens.PROTECTED_KEYWORD,
            CjTokens.INTERNAL_KEYWORD,
        )
        val CANONICAL_FIXTURE =
            "cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative/std/std.objectpool.cjo"
    }

    private data class CjmpAttributeCase(
        val description: String,
        val attributes: Set<Attribute>,
        val isCommon: Boolean,
        val isFromCommonPart: Boolean,
    )

    private data class BodyProjectionCase(
        val description: String,
        val memberAttributes: Set<Attribute>,
        val hostAttributes: Set<Attribute>,
        val modality: org.cangnova.cangjie.descriptors.Modality,
        val isForeign: Boolean,
        val isIntrinsic: Boolean,
        val hostIsAbstract: Boolean,
        val hostIsJavaMirror: Boolean,
        val hostIsObjCMirror: Boolean,
        val expectedBody: Boolean,
    )
}
