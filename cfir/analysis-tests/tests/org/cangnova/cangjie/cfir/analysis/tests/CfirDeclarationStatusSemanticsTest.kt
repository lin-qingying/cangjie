package org.cangnova.cangjie.cfir.analysis.tests

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.cfir.declarations.CfirBodyPresence
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.psi.CjFile

/** CFIR STATUS 以 raw body/source facts 为输入，统一产生 class 与 interface 的有效模态。 */
class CfirDeclarationStatusSemanticsTest : AbstractCfirAnalysisResolveTest() {

    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun sourceClassBodylessOverrideIsImplicitAbstractAndDeclarationClassOverrideIsFinal() {
        val sourceFile = buildResolvedFile(
            """
            public abstract class Parent {
                public func run(): Unit
                public func runOpen(): Unit
            }

            public abstract class Child <: Parent {
                public override func run(): Unit
                public open override func runOpen(): Unit
            }
            """.trimIndent(),
        )
        val sourceFunction = sourceFile.member("Child", "run")
        assertEquals(CfirBodyPresence.ABSENT, sourceFunction.status.bodyPresence)
        assertFalse(sourceFunction.status.isDeclarationFile)
        assertTrue(sourceFunction.status.isAbstract)
        assertEquals(Modality.ABSTRACT, sourceFunction.status.modality)
        val sourceOpenOverride = sourceFile.member("Child", "runOpen")
        assertTrue(sourceOpenOverride.status.isAbstract)
        assertEquals(Modality.ABSTRACT, sourceOpenOverride.status.modality)
        assertTrue(sourceOpenOverride.status.isModalityExplicit)

        val declarationFile = buildResolvedFile(
            """
            public open class Parent {
                public open func run(): Unit { }
                public open func runOpen(): Unit { }
            }

            public open class Child <: Parent {
                public override func run(): Unit
                public open override func runOpen(): Unit
                public func runPlain(): Unit
            }
            """.trimIndent(),
            isDeclarationFile = true,
        )
        val declarationFunction = declarationFile.member("Child", "run")
        assertEquals(CfirBodyPresence.ABSENT, declarationFunction.status.bodyPresence)
        assertTrue(declarationFunction.status.isDeclarationFile)
        assertFalse(declarationFunction.status.isAbstract)
        assertEquals(Modality.FINAL, declarationFunction.status.modality)
        assertEquals(Modality.OPEN, declarationFile.member("Child", "runOpen").status.modality)
        assertEquals(Modality.FINAL, declarationFile.member("Child", "runPlain").status.modality)

        val explicitlyOpenOverride = declarationFile.member("Child", "runOpen")
        assertEquals(Modality.OPEN, explicitlyOpenOverride.status.modality)
        assertTrue(explicitlyOpenOverride.status.isModalityExplicit)
    }

    @Test
    fun bodyfulClassOverridesResolveFinalUnlessOpenWasWritten() {
        val cfirFile = buildResolvedFile(
            """
            public open class Parent {
                public open func run(): Unit { }
                public open func runOpen(): Unit { }
            }

            public open class Child <: Parent {
                public override func run(): Unit { }
                public open override func runOpen(): Unit { }
            }
            """.trimIndent(),
        )

        assertEquals(CfirBodyPresence.PRESENT, cfirFile.member("Child", "run").status.bodyPresence)
        assertEquals(Modality.FINAL, cfirFile.member("Child", "run").status.modality)
        val openOverride = cfirFile.member("Child", "runOpen")
        assertEquals(CfirBodyPresence.PRESENT, openOverride.status.bodyPresence)
        assertEquals(Modality.OPEN, openOverride.status.modality)
        assertTrue(openOverride.status.isModalityExplicit)
    }

    @Test
    fun interfaceBodylessOverridesUseAbstractAndDeclarationDefaultPaths() {
        val sourceFunction = resolvedFunction(
            """
            interface Parent {
                func transform(value: Int32): Int32
            }

            interface Child <: Parent {
                override func transform(value: Int32): Int32
            }
            """.trimIndent(),
            ownerName = "Child",
            functionName = "transform",
        )
        assertTrue(sourceFunction.status.isAbstract)
        assertEquals(Modality.ABSTRACT, sourceFunction.status.modality)

        val declarationFunction = resolvedFunction(
            """
            interface Parent {
                func transform(value: Int32): Int32
            }

            interface Child <: Parent {
                override func transform(value: Int32): Int32
            }
            """.trimIndent(),
            isDeclarationFile = true,
            ownerName = "Child",
            functionName = "transform",
        )
        assertTrue(declarationFunction.status.isDeclarationFile)
        assertFalse(declarationFunction.status.isAbstract)
        assertTrue(declarationFunction.status.isDefault)
        assertEquals(Modality.OPEN, declarationFunction.status.modality)

        val defaultOverride = resolvedFunction(
            """
            interface Parent {
                func transform(value: Int32): Int32 { return value }
            }

            interface Child <: Parent {
                override func transform(value: Int32): Int32 { return value + 1 }
            }
            """.trimIndent(),
            ownerName = "Child",
            functionName = "transform",
        )
        assertEquals(CfirBodyPresence.PRESENT, defaultOverride.status.bodyPresence)
        assertEquals(Modality.OPEN, defaultOverride.status.modality)
    }

    @Test
    fun commonInterfaceBodylessFunctionRetainsParserAndPostParseAttributes() {
        val sourceFunction = resolvedFunction(
            """
            interface CommonContract {
                common func transform(value: Int32): Int32
            }
            """.trimIndent(),
            ownerName = "CommonContract",
            functionName = "transform",
        )
        assertTrue(sourceFunction.status.isCommon)
        assertTrue(sourceFunction.status.isAbstract)
        assertFalse(sourceFunction.status.isDefault)
        assertEquals(Modality.ABSTRACT, sourceFunction.status.modality)

        val declarationFunction = resolvedFunction(
            """
            interface CommonContract {
                common func transform(value: Int32): Int32
            }
            """.trimIndent(),
            isDeclarationFile = true,
            ownerName = "CommonContract",
            functionName = "transform",
        )
        assertTrue(declarationFunction.status.isCommon)
        assertTrue(declarationFunction.status.isAbstract)
        assertTrue(declarationFunction.status.isDefault)
        assertEquals(Modality.ABSTRACT, declarationFunction.status.modality)
    }

    @Test
    fun cjmpOverlayClearsOnlyImplicitAbstractAndKeepsCommonModifierDistinct() {
        val cfirFile = buildResolvedFile(
            """
            common abstract class CommonBase {
                common func commonMember(): Unit
                func inheritedMember(): Unit
                abstract func explicitMember(): Unit
            }
            """.trimIndent(),
            cjmpMode = CfirCjmpMode.COMMON,
        )

        val commonMember = cfirFile.member("CommonBase", "commonMember")
        val inheritedMember = cfirFile.member("CommonBase", "inheritedMember")
        val explicitMember = cfirFile.member("CommonBase", "explicitMember")

        assertTrue(commonMember.status.isCommon)
        assertFalse(commonMember.status.isAbstract)
        assertFalse(inheritedMember.status.isCommon)
        assertFalse(inheritedMember.status.isAbstract)
        assertTrue(explicitMember.status.isAbstract)
        assertTrue(explicitMember.status.isAbstractExplicit)
    }

    private fun resolvedFunction(
        text: String,
        isDeclarationFile: Boolean = false,
        ownerName: String,
        functionName: String,
    ): CfirFunction = buildResolvedFile(text, isDeclarationFile)
        .member(ownerName, functionName)

    private fun buildResolvedFile(
        text: String,
        isDeclarationFile: Boolean = false,
        cjmpMode: CfirCjmpMode? = null,
    ): CfirFile {
        val sourcePsiFile = if (isDeclarationFile) {
            psiFileFactory.createFileFromText("status.cj.d", CangJieDeclarationFileType, text) as CjFile
        } else {
            createCjFile("status", text)
        }
        val session = createTestSession()
        if (cjmpMode != null) {
            session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = cjmpMode))
        }
        val cfirFile = sourcePsiFile.toCfirFile(session)
        resolveToPhase(cfirFile, session, CfirResolvePhase.STATUS)
        return cfirFile
    }

    private fun CfirFile.member(ownerName: String, functionName: String): CfirFunction {
        val owner = declarations.filterIsInstance<CfirClassLikeDeclaration>()
            .single { it.name.asString() == ownerName }
        return owner.declarations.filterIsInstance<CfirFunction>()
            .single { it.symbol.name.asString() == functionName }
    }
}
