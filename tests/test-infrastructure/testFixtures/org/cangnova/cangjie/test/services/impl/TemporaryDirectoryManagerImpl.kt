package org.cangnova.cangjie.test.services.impl

import org.cangnova.cangjie.test.services.TemporaryDirectoryManager
import org.cangnova.cangjie.test.services.TestServices
import java.io.File
import kotlin.io.path.createTempDirectory

class TemporaryDirectoryManagerImpl(
    testServices: TestServices,
) : TemporaryDirectoryManager(testServices) {
    private val createdDirectories = mutableMapOf<String, File>()

    override val rootDir: File by lazy {
        createTempDirectory("cangjie-test-").toFile().also { it.deleteOnExit() }
    }

    override fun getOrCreateTempDirectory(name: String): File {
        return createdDirectories.getOrPut(name) {
            File(rootDir, name).also { it.mkdirs() }
        }
    }

    override fun cleanupTemporaryDirectories() {
        createdDirectories.values.forEach { it.deleteRecursively() }
        createdDirectories.clear()
        rootDir.deleteRecursively()
    }
}
