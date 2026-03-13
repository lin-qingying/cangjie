package org.cangnova.cangjie.test.services.impl

import org.cangnova.cangjie.test.directives.CangjieTestDirectives
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.DependencyDescription
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.model.TestModuleStructureImpl
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

interface TestModuleStructureExtractor {
    fun extract(testDataPath: Path): TestModuleStructure
}

class TestModuleStructureExtractorImpl(
    private val directivesContainer: DirectivesContainer = CangjieTestDirectives,
) : TestModuleStructureExtractor {
    override fun extract(testDataPath: Path): TestModuleStructure {
        val originalFiles = collectOriginalFiles(testDataPath)
        require(originalFiles.isNotEmpty()) { "在 $testDataPath 未找到任何测试数据文件" }

        val (modules, allDirectives) = when (originalFiles.size) {
            1 -> parseSingleFile(originalFiles.single())
            else -> parseDirectoryFiles(originalFiles)
        }

        return TestModuleStructureImpl(
            modules = modules,
            allDirectives = allDirectives,
            originalTestDataFiles = originalFiles,
        )
    }

    private fun collectOriginalFiles(testDataPath: Path): List<File> {
        val files = when {
            testDataPath.isDirectory() ->
                Files.walk(testDataPath).use { stream ->
                    stream
                        .filter { Files.isRegularFile(it) }
                        .filter { it.fileName.toString().endsWith(".cj") || it.fileName.toString().endsWith(".cjs") }
                        .map { it.toFile() }
                        .toList()
                }

            Files.isRegularFile(testDataPath) -> listOf(testDataPath.toFile())
            else -> emptyList()
        }
        return files.sortedBy { it.path.replace('\\', '/') }
    }

    private fun parseDirectoryFiles(files: List<File>): Pair<List<TestModule>, RegisteredDirectives> {
        val moduleName = "main"
        val testFiles = files.map { file ->
            TestFile(
                name = file.name,
                content = file.readText(Charsets.UTF_8),
                originalFile = file,
            )
        }
        val directives = RegisteredDirectives.Empty
        return listOf(TestModule(name = moduleName, files = testFiles, directives = directives)) to directives
    }

    private fun parseSingleFile(file: File): Pair<List<TestModule>, RegisteredDirectives> {
        val lines = file.readLines(Charsets.UTF_8)
        val parser = RegisteredDirectivesParser(directivesContainer)

        data class MutableModule(
            val name: String,
            val files: MutableList<TestFile>,
            val dependencies: MutableList<DependencyDescription>,
            val directives: MutableList<RegisteredDirectives>,
        )

        var currentModule = MutableModule(
            name = "main",
            files = mutableListOf(),
            dependencies = mutableListOf(),
            directives = mutableListOf(),
        )

        val modules = linkedMapOf<String, MutableModule>()
        modules[currentModule.name] = currentModule

        var currentFileName = file.name
        val currentFileBuffer = StringBuilder()

        fun flushFile() {
            if (currentFileBuffer.isEmpty()) return
            val content = currentFileBuffer.toString().trimEnd()
            currentModule.files += TestFile(currentFileName, content, originalFile = file)
            currentFileBuffer.clear()
        }

        fun switchModule(newModuleName: String) {
            flushFile()
            currentFileName = "${newModuleName}.cj"
            currentModule = modules.getOrPut(newModuleName) {
                MutableModule(
                    name = newModuleName,
                    files = mutableListOf(),
                    dependencies = mutableListOf(),
                    directives = mutableListOf(),
                )
            }
        }

        fun switchFile(newFileName: String) {
            flushFile()
            currentFileName = newFileName
        }

        for (line in lines) {
            val raw = RegisteredDirectivesParser.parseDirective(line)
            if (raw != null) {
                when (raw.name) {
                    CangjieTestDirectives.MODULE.name -> {
                        val value = raw.rawValue?.trim().orEmpty()
                        val (moduleName, deps) = parseModuleHeader(value.ifEmpty { "main" })
                        switchModule(moduleName)
                        deps.forEach { depName -> currentModule.dependencies += DependencyDescription(depName) }
                        continue
                    }

                    CangjieTestDirectives.FILE.name -> {
                        val value = raw.rawValue?.trim().orEmpty()
                        require(value.isNotBlank()) { "FILE 指令必须提供文件名" }
                        switchFile(value)
                        continue
                    }

                    CangjieTestDirectives.DEPENDS_ON.name -> {
                        val deps = raw.values.orEmpty().flatMap { it.split(',') }.map { it.trim() }.filter { it.isNotEmpty() }
                        deps.forEach { depName -> currentModule.dependencies += DependencyDescription(depName) }
                        continue
                    }
                }

                // 已知但非结构性指令：记录到 directives 中（且不写入源码文本）
                if (parser.parse(line)) continue
            }

            currentFileBuffer.appendLine(line)
        }
        flushFile()

        val registeredDirectives = parser.build()
        val testModules = modules.values.map { m ->
            TestModule(
                name = m.name,
                files = m.files.toList(),
                dependencies = m.dependencies.toList(),
                directives = RegisteredDirectives.Empty,
            )
        }

        return testModules to registeredDirectives
    }

    /** 解析 `name(dep1, dep2)` 得到模块名与依赖列表。 */
    private fun parseModuleHeader(value: String): Pair<String, List<String>> {
        val trimmed = value.trim()
        val open = trimmed.indexOf('(')
        if (open < 0) return trimmed to emptyList()
        val close = trimmed.lastIndexOf(')')
        if (close <= open) return trimmed.substring(0, open).trim() to emptyList()

        val name = trimmed.substring(0, open).trim()
        val depsPart = trimmed.substring(open + 1, close)
        val deps = depsPart.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return name to deps
    }
}

