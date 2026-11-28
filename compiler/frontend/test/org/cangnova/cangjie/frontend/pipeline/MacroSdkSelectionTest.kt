package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.config.CompilerConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/** 验证调用边界的 SDK 来源选择、组件按需验证及不可变选择快照。 */
@OptIn(CompilerConfiguration.Internals::class)
class MacroSdkSelectionTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun configuredHomeWinsOverEveryOtherValidSource() {
        val configured = sdkDirectory("configured")
        val environment = sdkDirectory("environment")
        val property = sdkDirectory("property")
        val pathSdk = sdkDirectory("path-sdk")
        val pathCompiler = writeCompiler(pathSdk.resolve("bin"))

        val selected = MacroSdkSelector.select(
            MacroSdkSelectionInputs(configured.toString(), environment.toString(), property.toString(), pathCompiler.parent.toString()),
        )

        assertEquals(MacroSdkSelection.Selected(configured, MacroSdkOrigin.COMPILER_CONFIGURATION), selected)
    }

    @Test
    fun environmentAndPropertyAreSelectedOnlyWhenHigherPriorityHomesAreUnspecified() {
        val environment = sdkDirectory("environment")
        val property = sdkDirectory("property")

        assertEquals(
            MacroSdkSelection.Selected(environment, MacroSdkOrigin.CANGJIE_HOME),
            MacroSdkSelector.select(MacroSdkSelectionInputs(null, environment.toString(), property.toString(), null)),
        )
        assertEquals(
            MacroSdkSelection.Selected(property, MacroSdkOrigin.CJC_HOME_PROPERTY),
            MacroSdkSelector.select(MacroSdkSelectionInputs(null, null, property.toString(), null)),
        )
    }

    @Test
    fun blankAndMalformedExplicitHomesAreInvalidInsteadOfUnspecified() {
        val environment = sdkDirectory("environment")
        for (home in listOf("", " ", "invalid\u0000sdk")) {
            val selected = MacroSdkSelector.select(MacroSdkSelectionInputs(home, environment.toString(), null, null))
            assertTrue(selected is MacroSdkSelection.Invalid)
            selected as MacroSdkSelection.Invalid
            assertEquals(home, selected.requestedHome)
            assertEquals(MacroSdkOrigin.COMPILER_CONFIGURATION, selected.origin)
            assertTrue(selected.reason.isNotBlank())
        }
    }

    @Test
    fun homeMustBeADirectoryButDoesNotRequireUnusedCompilerOrExecutorComponents() {
        val file = Files.write(tempDir.resolve("not-a-directory"), byteArrayOf(1))
        assertTrue(MacroSdkSelector.select(MacroSdkSelectionInputs(file.toString(), null, null, null)) is MacroSdkSelection.Invalid)

        val root = sdkDirectory("artifact-only")
        val selected = MacroSdkSelector.select(MacroSdkSelectionInputs(root.toString(), null, null, null)).requireSelected()
        assertEquals(root, selected.home)
        assertFalse(Files.exists(selected.compilerExecutable))
        assertFalse(Files.exists(selected.executorExecutable))
    }

    @Test
    fun pathChoosesTheFirstSdkBinAndDerivesTheSharedRoot() {
        val first = sdkDirectory("first")
        val second = sdkDirectory("second")
        val firstCompiler = writeCompiler(first.resolve("bin"))
        val secondCompiler = writeCompiler(second.resolve("bin"))
        val path = listOf(tempDir.resolve("missing-bin"), firstCompiler.parent, secondCompiler.parent)
            .joinToString(File.pathSeparator)

        val selected = MacroSdkSelector.select(MacroSdkSelectionInputs(null, null, null, path)).requireSelected()

        assertEquals(MacroSdkOrigin.PATH, selected.origin)
        assertEquals(first, selected.home)
        assertEquals(firstCompiler, selected.compilerExecutable)
        assertEquals(first.resolve("tools/bin").resolve(toolName("LSPMacroServer")), selected.executorExecutable)
    }

    @Test
    fun pathWrapperOutsideBinIsRejectedWithoutChoosingALaterSdk() {
        val wrapper = writeCompiler(sdkDirectory("wrappers"))
        val later = writeCompiler(sdkDirectory("later-sdk").resolve("bin"))
        val selected = MacroSdkSelector.select(
            MacroSdkSelectionInputs(null, null, null, listOf(wrapper.parent, later.parent).joinToString(File.pathSeparator)),
        )

        assertTrue(selected is MacroSdkSelection.Invalid)
        selected as MacroSdkSelection.Invalid
        assertEquals(MacroSdkOrigin.PATH, selected.origin)
        assertEquals(wrapper.toString(), selected.requestedHome)
    }

    @Test
    fun emptyInputSnapshotDoesNotDiscoverAmbientOrUserDirectoryInstallations() {
        assertSame(
            MacroSdkSelection.Absent,
            MacroSdkSelector.select(MacroSdkSelectionInputs(null, null, null, null, userHome = null)),
        )
        assertSame(
            MacroSdkSelection.Absent,
            MacroSdkSelector.select(MacroSdkSelectionInputs(
                null, null, null, tempDir.resolve("missing-bin").toString(), userHome = null,
            )),
        )
    }

    @Test
    fun processEnvironmentUsesSelectedRootAndPreservesParentSearchPath() {
        val root = sdkDirectory("selected")
        val selected = MacroSdkSelection.Selected(root, MacroSdkOrigin.COMPILER_CONFIGURATION)
        val pathKey = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "Path" else "PATH"
        val parent = mapOf("CANGJIE_HOME" to "obsolete-sdk", pathKey to "parent-bin", "KEEP" to "parent-value")

        val overrides = selected.processEnvironment(parent)
        val child = parent + overrides

        assertEquals(root.toString(), child["CANGJIE_HOME"])
        assertEquals("parent-value", child["KEEP"])
        assertEquals(
            listOf(root.resolve("bin"), root.resolve("tools/bin"), root.resolve("runtime/lib/${defaultCangjieLibHost()}"),
                root.resolve("third_party/llvm/bin")).map(Path::toString) + "parent-bin",
            child.getValue(pathKey).split(File.pathSeparatorChar),
        )
        assertEquals("obsolete-sdk", parent["CANGJIE_HOME"])
    }

    @Test
    fun configurationCanClearExplicitHomeAndEachPreparationCapturesItsOwnSelection() {
        val firstHome = sdkDirectory("first")
        val secondHome = sdkDirectory("second")
        val configuration = CompilerConfiguration()
        assertNull(configuration.macroSdkHome)
        configuration.macroSdkHome = firstHome.toString()
        val first = prepareMacroArtifactDefinitionsForExpansion(configuration, emptyList())
        configuration.macroSdkHome = secondHome.toString()
        val second = prepareMacroArtifactDefinitionsForExpansion(configuration, emptyList())

        assertEquals(firstHome, first.sdkSelection.requireSelected().home)
        assertEquals(secondHome, second.sdkSelection.requireSelected().home)
        assertNotEquals(first.sdkSelection.identity, second.sdkSelection.identity)
        configuration.macroSdkHome = null
        assertNull(configuration.macroSdkHome)
        assertEquals(firstHome, first.sdkSelection.requireSelected().home)
        assertEquals(secondHome, second.sdkSelection.requireSelected().home)
    }

    private fun sdkDirectory(name: String): Path = Files.createDirectories(tempDir.resolve(name)).toAbsolutePath().normalize()

    private fun writeCompiler(bin: Path): Path =
        Files.write(Files.createDirectories(bin).resolve(toolName("cjc")), byteArrayOf(1))

    private fun toolName(name: String): String =
        if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) "$name.exe" else name
}
