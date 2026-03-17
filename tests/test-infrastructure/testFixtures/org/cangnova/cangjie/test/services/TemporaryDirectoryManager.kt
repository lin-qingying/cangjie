package org.cangnova.cangjie.test.services

import java.io.File

abstract class TemporaryDirectoryManager(protected val testServices: TestServices) : TestService {
    abstract fun getOrCreateTempDirectory(name: String): File
    abstract fun cleanupTemporaryDirectories()
    abstract val rootDir: File
}

val TestServices.temporaryDirectoryManager: TemporaryDirectoryManager by TestServices.testServiceAccessor()

fun TestServices.getOrCreateTempDirectory(name: String): File {
    return temporaryDirectoryManager.getOrCreateTempDirectory(name)
}
