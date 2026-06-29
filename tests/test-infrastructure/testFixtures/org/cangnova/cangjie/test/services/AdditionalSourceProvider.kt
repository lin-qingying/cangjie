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

/**
 * 表示 `AdditionalSourceProvider`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
abstract class AdditionalSourceProvider(val testServices: TestServices) : ServicesAndDirectivesContainer {
    /**
     * 提供 `produceAdditionalFiles` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile>

    /**
     * 提供 `containsDirective` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    protected fun containsDirective(globalDirectives: RegisteredDirectives, module: TestModule, directive: SimpleDirective): Boolean {
        return globalDirectives.contains(directive) || module.directives.contains(directive)
    }

    /**
     * 提供 `toTestFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `toTestFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
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
