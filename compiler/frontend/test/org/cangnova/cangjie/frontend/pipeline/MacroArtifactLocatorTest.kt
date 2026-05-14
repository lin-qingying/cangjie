package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class MacroArtifactLocatorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun toCjoFileNameMatchesOfficialPackageNameRule() {
        assertEquals("macros.pkg", toCjoFileName(FqName("macros.pkg")))
        assertEquals("org@pkg", toCjoFileName(FqName("pkg::org")))
    }

    @Test
    fun locatesOrdinaryMacroArtifactsFromFirstSegmentDirectory() {
        val root = Files.createDirectories(tempDir.resolve("artifacts"))
        val packageDir = Files.createDirectories(root.resolve("macros"))
        val cjo = writeFile(packageDir.resolve("macros.pkg.cjo"))
        val dylib = writeFile(packageDir.resolve("lib-macro_macros.pkg.${dynamicLibraryExtension()}"))

        val artifact = MacroArtifactLocator(sdkHome = tempDir.resolve("sdk").toString())
            .locate(
                packageDemands = setOf(FqName("macros.pkg")),
                searchRoots = listOf(root.toString()),
            )
            .single()

        assertEquals(cjo.toString(), artifact.cjoPath)
        assertEquals(dylib.toString(), artifact.dynamicLibPath)
        assertEquals(MacroArtifactPackage.Origin.EXTERNAL_PATH, artifact.origin)
    }

    @Test
    fun locatesOrdinaryMacroArtifactsFromDirectFallbackPath() {
        val root = Files.createDirectories(tempDir.resolve("direct"))
        val cjo = writeFile(root.resolve("macros.pkg.cjo"))
        val dylib = writeFile(root.resolve("lib-macro_macros.pkg.${dynamicLibraryExtension()}"))

        val artifact = MacroArtifactLocator(sdkHome = tempDir.resolve("sdk").toString())
            .locate(
                packageDemands = setOf(FqName("macros.pkg")),
                searchRoots = listOf(root.toString()),
            )
            .single()

        assertEquals(cjo.toString(), artifact.cjoPath)
        assertEquals(dylib.toString(), artifact.dynamicLibPath)
        assertEquals(MacroArtifactPackage.Origin.EXTERNAL_PATH, artifact.origin)
    }

    @Test
    fun explicitArtifactsTakePriorityOverSearchRoots() {
        val root = Files.createDirectories(tempDir.resolve("search"))
        writeFile(root.resolve("macros.pkg.cjo"))
        writeFile(root.resolve("lib-macro_macros.pkg.${dynamicLibraryExtension()}"))
        val explicit = MacroArtifactPackage(
            packageFqName = FqName("macros.pkg"),
            kind = MacroArtifactPackage.Kind.MACRO,
            cjoPath = tempDir.resolve("explicit").resolve("macros.pkg.cjo").toString(),
            dynamicLibPath = tempDir.resolve("explicit").resolve("lib-macro_macros.pkg.${dynamicLibraryExtension()}").toString(),
            origin = MacroArtifactPackage.Origin.ORCHESTRATION,
        )

        val artifact = MacroArtifactLocator(sdkHome = tempDir.resolve("sdk").toString())
            .locate(
                packageDemands = setOf(FqName("macros.pkg")),
                searchRoots = listOf(root.toString()),
                explicitArtifacts = listOf(explicit),
            )
            .single()

        assertEquals(explicit, artifact)
        assertEquals(MacroArtifactPackage.Origin.ORCHESTRATION, artifact.origin)
    }

    @Test
    fun locatesStdMacroArtifactsFromSdkModulesAndRuntimeLibHost() {
        val sdkHome = Files.createDirectories(tempDir.resolve("sdk"))
        val modulesDir = Files.createDirectories(sdkHome.resolve("modules").resolve("windows_x86_64_cjnative").resolve("std"))
        val runtimeDir = Files.createDirectories(sdkHome.resolve("runtime").resolve("lib").resolve("windows_x86_64_cjnative"))
        val cjo = writeFile(modulesDir.resolve("std.core.cjo"))
        val dylib = writeFile(runtimeDir.resolve("libcangjie-std-core.${dynamicLibraryExtension()}"))

        val artifact = MacroArtifactLocator(
            sdkHome = sdkHome.toString(),
            host = "windows_x86_64_cjnative",
        ).locate(
            packageDemands = setOf(FqName("std.core")),
            searchRoots = emptyList(),
        ).single()

        assertEquals(cjo.toString(), artifact.cjoPath)
        assertEquals(dylib.toString(), artifact.dynamicLibPath)
        assertEquals(MacroArtifactPackage.Origin.SDK_STDLIB, artifact.origin)
    }

    @Test
    fun locatesAndResolvesFixedSdkStdMacroArtifact() {
        val artifact = MacroArtifactLocator(
            sdkHome = DEFAULT_MACRO_SDK_HOME,
            host = "windows_x86_64_cjnative",
        ).locate(
            packageDemands = setOf(FqName("std.unittest.mock.mockmacro")),
            searchRoots = emptyList(),
        ).single()

        assertEquals(MacroArtifactPackage.Origin.SDK_STDLIB, artifact.origin)
        assertTrue(artifact.cjoPath.endsWith("std.unittest.mock.mockmacro.cjo"))
        assertTrue(artifact.dynamicLibPath.endsWith("libcangjie-std-unittest.mock.mockmacro.${dynamicLibraryExtension()}"))

        val resolved = MacroArtifactResolver().resolve(listOf(artifact))
        assertTrue(resolved.diagnostics.isEmpty(), "Unexpected diagnostics: ${resolved.diagnostics}")
        assertTrue(resolved.definitions.isNotEmpty(), "Fixed SDK std macro artifact must export macro definitions.")
        assertTrue(resolved.definitions.all { it.libPath == artifact.dynamicLibPath })
    }

    @Test
    fun doesNotTreatUnknownStdPrefixPackageAsSdkStdlib() {
        val sdkHome = Files.createDirectories(tempDir.resolve("sdk"))
        val modulesDir = Files.createDirectories(sdkHome.resolve("modules").resolve("windows_x86_64_cjnative").resolve("std"))
        val runtimeDir = Files.createDirectories(sdkHome.resolve("runtime").resolve("lib").resolve("windows_x86_64_cjnative"))
        writeFile(modulesDir.resolve("std.unknown.cjo"))
        writeFile(runtimeDir.resolve("libcangjie-std-unknown.${dynamicLibraryExtension()}"))

        val artifacts = MacroArtifactLocator(
            sdkHome = sdkHome.toString(),
            host = "windows_x86_64_cjnative",
        ).locate(
            packageDemands = setOf(FqName("std.unknown")),
            searchRoots = emptyList(),
        )

        assertEquals(emptyList<MacroArtifactPackage>(), artifacts)
    }

    @Test
    fun doesNotReportArtifactWhenDynamicLibraryIsMissing() {
        val root = Files.createDirectories(tempDir.resolve("missing-lib"))
        writeFile(root.resolve("macros.pkg.cjo"))

        val artifacts = MacroArtifactLocator(sdkHome = tempDir.resolve("sdk").toString())
            .locate(
                packageDemands = setOf(FqName("macros.pkg")),
                searchRoots = listOf(root.toString()),
            )

        assertEquals(emptyList<MacroArtifactPackage>(), artifacts)
    }

    private fun writeFile(path: Path): Path {
        Files.createDirectories(path.parent)
        Files.write(path, byteArrayOf(1, 2, 3))
        assertNotNull(path)
        return path
    }
}
