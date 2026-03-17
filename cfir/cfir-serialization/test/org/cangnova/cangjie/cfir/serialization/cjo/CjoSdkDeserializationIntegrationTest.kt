package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CjoSdkDeserializationIntegrationTest {
    @Test
    fun `sdk cjo fixtures can be deserialized with expected version`() {
        val fixtures = listOf(
            "cjo-sdk/windows_x86_64_cjnative/std.cjo",
            "cjo-sdk/windows_x86_64_cjnative/std/std.core.cjo",
            "cjo-sdk/windows_x86_64_cjnative/std/std.objectpool.cjo",
        )

        for (fixture in fixtures) {
            val bytes = resourceBytes(fixture)
            val byteBuffer = ByteBuffer.wrap(bytes)
            assertTrue(Package.PackageBufferHasIdentifier(byteBuffer), "fixture should have CJOF identifier: $fixture")

            val pkg = Package.getRootAsPackage(byteBuffer)
            val fullPkgName = pkg.fullPkgName ?: fail("missing fullPkgName in fixture: $fixture")
            val cjoVersion = pkg.cjoVersion ?: fail("missing cjoVersion in fixture: $fixture")
            assertTrue(fullPkgName.isNotBlank(), "fullPkgName should not be blank: $fixture")
            val fixtureVersion = Version(
                major = cjoVersion.majorNum.toUInt().toInt(),
                minor = cjoVersion.minorNum.toUInt().toInt(),
                patch = cjoVersion.patchNum.toUInt().toInt(),
            )
            val supportedVersion = Version(
                major = CjoConstants.VERSION_MAJOR,
                minor = CjoConstants.VERSION_MINOR,
                patch = CjoConstants.VERSION_PATCH,
            )
            assertTrue(
                fixtureVersion <= supportedVersion,
                "fixture version $fixtureVersion should not be newer than supported $supportedVersion: $fixture",
            )
        }
    }

    @Test
    fun `cjo manager loads sdk package in canonical search path layout`() {
        val fixture = "cjo-sdk/windows_x86_64_cjnative/std/std.objectpool.cjo"
        val bytes = resourceBytes(fixture)
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val fullPkgName = pkg.fullPkgName ?: fail("missing fullPkgName in fixture: $fixture")

        val tempDir = Files.createTempDirectory("cjo-sdk-deserialize-")
        try {
            val target = tempDir.resolve(CjoConstants.packageNameToPath(fullPkgName))
            target.parent?.createDirectories()
            target.outputStream().use { it.write(bytes) }

            val manager = CjoManager(
                searchPath = CjoSearchPath(
                    envProvider = { key ->
                        when (key) {
                            "CANGJIE_STDLIB_MODULE" -> tempDir.toString()
                            else -> null
                        }
                    }
                )
            )

            assertTrue(manager.hasPackage(org.cangnova.cangjie.name.FqName(fullPkgName)))
            val header = assertNotNull(manager.loadPackageHeader(fullPkgName), "should load package header for $fullPkgName")
            assertEquals(fullPkgName, header.fullPkgName)
            assertTrue(
                header.topLevelClassNames.isNotEmpty() || header.topLevelCallableNames.isNotEmpty(),
                "expected at least one top-level declaration in $fullPkgName",
            )
            val loadedPackage = assertNotNull(manager.loadPackage(fullPkgName), "should load full package for $fullPkgName")
            assertEquals(fullPkgName, loadedPackage.fullPkgName)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    private fun resourceBytes(path: String): ByteArray {
        return javaClass.classLoader.getResourceAsStream(path)?.use { it.readBytes() }
            ?: fail("fixture not found in test resources: $path (classpath lookup failed)")
    }

    private data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
    ) : Comparable<Version> {
        override fun compareTo(other: Version): Int {
            return compareValuesBy(this, other, Version::major, Version::minor, Version::patch)
        }
    }
}
