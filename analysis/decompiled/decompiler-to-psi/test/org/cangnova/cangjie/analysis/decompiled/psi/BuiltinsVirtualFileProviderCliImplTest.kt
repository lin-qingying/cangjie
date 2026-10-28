package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile

/**
 * 锁定 builtins virtual file provider 的 CLI/测试宿主行为：
 * 只要显式提供 `cangjie.stdlib.module`，就必须能枚举出真实 `.cjo` builtins 文件。
 */
class BuiltinsVirtualFileProviderCliImplTest {
    /**
     * 验证通过 `cangjie.stdlib.module` 系统属性提供的标准库根可以被枚举为 builtins `.cjo` 文件。
     */
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

    /**
     * 验证新复制到临时目录的标准库根也能通过刷新感知的 VFS 路径被发现。
     */
    @Test
    fun discoverStdlibFilesFromFreshTempDirectory() {
        withEnvironment {
            val tempStdlibRoot = Files.createTempDirectory("cangjie-builtins-provider-stdlib-")
            val oldValue = System.getProperty("cangjie.stdlib.module")
            try {
                copyStdlibFixtureRoot(tempStdlibRoot)
                System.setProperty("cangjie.stdlib.module", tempStdlibRoot.toString())

                val provider = BuiltinsVirtualFileProviderCliImpl()
                val files = provider.getBuiltinVirtualFiles()

                assertTrue(files.isNotEmpty(), "fresh temp stdlib root should also expose `.cjo` builtins files")
                assertTrue(
                    files.any { file -> file.name.equals("std.core.cjo", ignoreCase = true) },
                    "fresh temp stdlib root should expose `std.core.cjo`; actual=${files.map { it.name }.sorted()}",
                )
            } finally {
                if (oldValue == null) {
                    System.clearProperty("cangjie.stdlib.module")
                } else {
                    System.setProperty("cangjie.stdlib.module", oldValue)
                }
                tempStdlibRoot.toFile().deleteRecursively()
            }
        }
    }

    /**
     * 验证枚举缓存不随源码编辑失效，但可被显式失效。
     *
     * 这是 EDT 慢操作修复的核心不变量：旧实现把 `CaModificationTracker` / `PsiModificationTracker`
     * 的计数并进缓存键，于是**每次源码编辑**之后的第一次类查找都会重新递归遍历整棵 SDK 目录
     * （`VfsUtilCore.iterateChildrenRecursively` → `PersistentFSImpl.listAll` → `FSRecordsImpl.update`），
     * 平台在 `FSRecordsImpl.update` 入口断言“EDT 上禁止慢操作”。
     *
     * 这里用“同一 root 连续取两次必须是同一个集合实例”表达“第二次命中缓存”：
     * `computeIfAbsent` 命中时返回的正是上次存入的实例，重新枚举必然产生新的 `LinkedHashSet`。
     */
    @Test
    fun enumerationIsCacheHitForSameRoots() {
        withEnvironment {
            val stdlibRoot = locateStdlibFixtureRoot()
            val oldValue = System.getProperty("cangjie.stdlib.module")
            try {
                System.setProperty("cangjie.stdlib.module", stdlibRoot.toString())

                val provider = BuiltinsVirtualFileProviderCliImpl()
                val first = provider.getBuiltinVirtualFiles()
                assertTrue(first.isNotEmpty(), "builtins provider should expose `.cjo` files")

                val second = provider.getBuiltinVirtualFiles()
                assertSame(
                    first,
                    second,
                    "builtins enumeration must be cached for an unchanged root set; " +
                        "re-enumerating the SDK directory per lookup is the EDT slow-operation source",
                )

                // 显式失效后必须重新枚举（SDK 切换 / 标准库重装）。
                provider.invalidateBuiltinFileCache()
                val third = provider.getBuiltinVirtualFiles()
                assertNotSame(first, third, "invalidateBuiltinFileCache must drop the cached enumeration")
                assertEquals(
                    first.map { file -> file.path }.sorted(),
                    third.map { file -> file.path }.sorted(),
                    "re-enumerated builtins file set must be identical for the same root",
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

    /**
     * 定位当前仓库内用于 builtins provider 测试的标准库 `.cjo` fixture 根目录。
     */
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

    /**
     * 将最小标准库 fixture 复制到目标根目录，模拟运行期生成或复制出的 stdlib 目录。
     */
    private fun copyStdlibFixtureRoot(destinationRoot: Path) {
        val fixtureRoot = locateStdlibFixtureRoot()
        copyFile(fixtureRoot.resolve("std.cjo"), destinationRoot.resolve("std.cjo"))
        copyFile(
            fixtureRoot.resolve("std").resolve("std.core.cjo"),
            destinationRoot.resolve("std").resolve("std.core.cjo"),
        )
    }

    /**
     * 复制单个 fixture 文件并确保目标父目录已经存在。
     */
    private fun copyFile(source: Path, target: Path) {
        target.parent?.createDirectories()
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * 从起始目录向上查找包含 `settings.gradle.kts` 的仓库根目录。
     */
    private fun locateRepositoryRoot(start: Path): Path {
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    /**
     * 创建测试用仓颉核心环境并在执行完成后释放 IntelliJ disposable。
     */
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
