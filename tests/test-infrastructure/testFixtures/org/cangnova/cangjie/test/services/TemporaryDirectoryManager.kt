package org.cangnova.cangjie.test.services

import java.io.File

/**
 * 表示 `TemporaryDirectoryManager`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
abstract class TemporaryDirectoryManager(protected val testServices: TestServices) : TestService {
    /**
     * 提供 `getOrCreateTempDirectory` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun getOrCreateTempDirectory(name: String): File
    /**
     * 提供 `cleanupTemporaryDirectories` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun cleanupTemporaryDirectories()
    /**
     * 保存 `rootDir`，供测试服务在测试执行期间读取或传递。
     */
    abstract val rootDir: File
}

/**
 * 保存 `TestServices.temporaryDirectoryManager`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.temporaryDirectoryManager: TemporaryDirectoryManager by TestServices.testServiceAccessor()

/**
 * 执行 `getOrCreateTempDirectory` 对应的测试服务流程，维持测试框架的阶段契约。
 */
fun TestServices.getOrCreateTempDirectory(name: String): File {
    return temporaryDirectoryManager.getOrCreateTempDirectory(name)
}
