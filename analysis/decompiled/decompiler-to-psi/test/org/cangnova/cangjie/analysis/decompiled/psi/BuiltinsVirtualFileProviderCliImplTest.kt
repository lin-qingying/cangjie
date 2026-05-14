package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

/**
 * 锁定 builtins virtual file provider 的 CLI/测试宿主行为：
 * 只要显式提供 `cangjie.stdlib.module`，就必须能枚举出真实 `.cjo` builtins 文件。
 */
class BuiltinsVirtualFileProviderCliImplTest {
    @Test
    fun discoverStdlibFilesFromSystemProperty() {
        withEnvironment {
            val stdlibRoot = locateStdlibFixtureRoot()
            val oldValue = System.getProperty("cangjie.stdlib.module")
            try {
                System.setProperty("cangjie.stdlib.module", stdlibRoot.toString())

                val provider = BuiltinsVirtualFileProviderCliImpl()
                val files = provider.getBuiltinVirtualFiles()

                assertTrue(files.isNotEmpty(), "builtins provider should expose at least one `.cjo` file")
                assertTrue(
                    files.any { file -> file.name.equals("std.core.cjo", ignoreCase = true) },
                    "builtins provider should expose `std.core.cjo`; actual=${files.map { it.name }.sorted()}",
                )
            } finally {
                if (oldValue == null) {
                    System.clearProperty("cangjie.stdlib.module")
                } else {
                    System.setProperty("cangjie.stdlib.module", oldValue)
                }
            }
        }
    }

    private fun locateStdlibFixtureRoot(): Path {
        val repoRoot = locateRepositoryRoot(Paths.get("").toAbsolutePath().normalize())
        val fixtureRoot = repoRoot
            .resolve("cfir")
            .resolve("cfir-serialization")
            .resolve("testResources")
            .resolve("cjo-sdk")
            .resolve("windows_x86_64_cjnative")

        require(fixtureRoot.resolve("std.cjo").isRegularFile()) {
            "Cannot locate stdlib fixture root under $fixtureRoot"
        }
        return fixtureRoot
    }

    private fun locateRepositoryRoot(start: Path): Path {
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    private fun withEnvironment(action: () -> Unit) {
        val disposable = Disposer.newDisposable("BuiltinsVirtualFileProviderCliImplTest")
        try {
            CangJieCoreEnvironment.createForTests(disposable)
            action()
        } finally {
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction {
                    Disposer.dispose(disposable)
                }
            } else {
                Disposer.dispose(disposable)
            }
        }
    }
}
