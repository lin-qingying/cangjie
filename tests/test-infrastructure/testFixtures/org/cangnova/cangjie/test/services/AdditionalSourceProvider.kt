package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirective
import org.cangnova.cangjie.test.model.ServicesAndDirectivesContainer
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import java.io.File
import java.net.URL
import java.nio.file.Paths

abstract class AdditionalSourceProvider(val testServices: TestServices) : ServicesAndDirectivesContainer {
    abstract fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile>

    protected fun containsDirective(globalDirectives: RegisteredDirectives, module: TestModule, directive: SimpleDirective): Boolean {
        return globalDirectives.contains(directive) || module.directives.contains(directive)
    }

    protected fun URL.toTestFile(relativePath: String? = null): TestFile {
        val name = this.file.substringAfterLast("/")
        val dir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("filesFromResources")
        val originalContent = this.readText()
        val realFile = dir.resolve(name).also {
            it.writeText(originalContent)
        }
        return TestFile(
            relativePath = relativePath?.let(Paths::get)?.resolve(name)?.toString() ?: name,
            originalContent = originalContent,
            // TODO(KT-76305) add support for resources in jars
            originalFile = realFile,
            startLineNumberInOriginalFile = 0,
            isAdditional = true,
            directives = RegisteredDirectives.Empty
        )
    }

    protected fun File.toTestFile(relativePath: String? = null): TestFile {
        return TestFile(
            relativePath = relativePath?.let(Paths::get)?.resolve(name)?.toString() ?: name,
            originalContent = this.useLines { it.joinToString("\n") },
            originalFile = this,
            startLineNumberInOriginalFile = 0,
            isAdditional = true,
            directives = RegisteredDirectives.Empty
        )
    }
}

