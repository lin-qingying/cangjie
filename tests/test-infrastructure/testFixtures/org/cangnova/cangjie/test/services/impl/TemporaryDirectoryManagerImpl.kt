package org.cangnova.cangjie.test.services.impl

import org.cangnova.cangjie.test.services.TemporaryDirectoryManager
import org.cangnova.cangjie.test.services.TestServices
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * 表示 `TemporaryDirectoryManagerImpl`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class TemporaryDirectoryManagerImpl(
    testServices: TestServices,
) : TemporaryDirectoryManager(testServices) {
    /**
     * 保存 `createdDirectories`，供测试服务在测试执行期间读取或传递。
     */
    private val createdDirectories = mutableMapOf<String, File>()

    /**
     * 保存 `rootDir`，供测试服务在测试执行期间读取或传递。
     */
    override val rootDir: File by lazy {
        createTempDirectory("cangjie-test-").toFile().also { it.deleteOnExit() }
    }

    /**
     * 执行 `getOrCreateTempDirectory` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun getOrCreateTempDirectory(name: String): File {
        return createdDirectories.getOrPut(name) {
            File(rootDir, name).also { it.mkdirs() }
        }
    }

    /**
     * 执行 `cleanupTemporaryDirectories` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun cleanupTemporaryDirectories() {
        createdDirectories.values.forEach { it.deleteRecursively() }
        createdDirectories.clear()
        rootDir.deleteRecursively()
    }
}
