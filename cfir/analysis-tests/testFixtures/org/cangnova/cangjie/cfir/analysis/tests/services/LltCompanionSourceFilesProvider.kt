package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.LLT_COMPANION_SOURCES
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.AdditionalSourceProvider
import org.cangnova.cangjie.test.services.TestServices
import java.io.File
import java.nio.file.Path

/**
 * 为官方 LLT 目录型多文件用例补充同目录 companion 源文件。
 *
 * 官方 LLT 中存在 `1.cj` / `2.cj` 这类文件拆分：单独编译其中一个文件会报未解析，
 * 但同目录文件作为同一编译单元时才是该用例的真实语义。该 provider 只在测试数据显式
 * 标注 [LLT_COMPANION_SOURCES] 时生效，避免把普通同目录独立用例错误地合并。
 *
 * 官方 LLT 还使用 `xxx.pkg.cj` 或同目录 `pkg.cj` 表示被主文件导入的包源文件。
 * 这类包 companion 是文件命名约定，不要求官方测试数据额外写本项目指令。
 *
 * 另外，官方 LLT 允许一个聚合文件通过 `// FILE: dir/name.cj` 声明虚拟多文件源，
 * 同目录下的物理文件可能依赖这些虚拟源。此 provider 会从父级聚合文件补齐这些源。
 */
class LltCompanionSourceFilesProvider(
    testServices: TestServices,
) : AdditionalSourceProvider(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives)

    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile> {
        val explicitCompanions = if (containsDirective(globalDirectives, module, LLT_COMPANION_SOURCES)) {
            testModuleStructure.originalTestDataFiles.flatMap(::collectAllSiblingCjFiles)
        } else {
            emptyList()
        }

        val packageCompanions = testModuleStructure.originalTestDataFiles.flatMap(::collectPackageCompanionFiles)
        val multiFileCompanions = testModuleStructure.originalTestDataFiles.flatMap(::collectMultiFileDirectoryCompanions)
        val parentMultiFileCompanions = testModuleStructure.originalTestDataFiles.flatMap(::collectParentMultiFileCompanions)

        val fileCompanions = (explicitCompanions + packageCompanions + multiFileCompanions)
            .distinctBy { it.canonicalFile }
            .map { it.toTestFile("lltCompanions") }
        val virtualCompanions = parentMultiFileCompanions
            .distinctBy { it.ownerFile.canonicalFile to it.relativePath }
            .map { it.toTestFile() }

        return fileCompanions + virtualCompanions
    }

    private fun collectAllSiblingCjFiles(testDataFile: File): List<File> {
        val directory = testDataFile.parentFile ?: return emptyList()
        return directory.listFiles().orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension == "cj" }
            .filter { it.canonicalFile != testDataFile.canonicalFile }
            .sortedBy { it.name }
            .toList()
    }

    private fun collectPackageCompanionFiles(testDataFile: File): List<File> {
        if (!testDataFile.invariantSeparatorsPath.contains("cfir/analysis-tests/testData/llt/")) return emptyList()
        if (testDataFile.isPackageCompanionFile()) return emptyList()

        val directory = testDataFile.parentFile ?: return emptyList()
        val sameNamePackageFile = directory.resolve("${testDataFile.nameWithoutExtension}.pkg.cj")
        val packageFile = directory.resolve("pkg.cj")
        return listOf(sameNamePackageFile, packageFile)
            .filter { it.isFile && it.canonicalFile != testDataFile.canonicalFile }
            .sortedBy { it.name }
    }

    private fun collectMultiFileDirectoryCompanions(testDataFile: File): List<File> {
        if (!testDataFile.invariantSeparatorsPath.contains("cfir/analysis-tests/testData/llt/")) return emptyList()

        val fileEntries = FILE_DIRECTIVE.findAll(testDataFile.readText())
            .map { it.groupValues[1].trim().replace('\\', '/') }
            .toList()
        if (fileEntries.isEmpty()) return emptyList()

        val declaredByDirectory = fileEntries
            .mapNotNull { entry ->
                val slashIndex = entry.lastIndexOf('/')
                if (slashIndex <= 0) return@mapNotNull null
                val directory = testDataFile.parentFile.resolve(entry.substring(0, slashIndex))
                directory to entry.substring(slashIndex + 1)
            }
            .groupBy({ it.first.canonicalFile }, { it.second })
        if (declaredByDirectory.isEmpty()) return emptyList()

        return declaredByDirectory.flatMap { (directory, declaredNames) ->
            directory.listFiles().orEmpty()
                .asSequence()
                .filter { it.isFile && it.extension == "cj" }
                .filter { it.name !in declaredNames }
                .sortedBy { it.name }
                .toList()
        }
    }

    private fun collectParentMultiFileCompanions(testDataFile: File): List<VirtualCompanionFile> {
        if (!testDataFile.invariantSeparatorsPath.contains("cfir/analysis-tests/testData/llt/")) return emptyList()

        val directory = testDataFile.parentFile?.canonicalFile ?: return emptyList()
        val aggregates = collectAncestorAggregateFiles(testDataFile)
        return aggregates.flatMap { aggregate ->
            val fragments = parseFileDirectiveFragments(aggregate)
            val declaresCurrentDirectory = fragments.any { fragment ->
                aggregate.parentFile.resolve(fragment.relativePath)
                    .parentFile
                    ?.canonicalFile == directory
            }
            if (!declaresCurrentDirectory) return@flatMap emptyList()

            fragments.filter { fragment ->
                aggregate.parentFile.resolve(fragment.relativePath).canonicalFile != testDataFile.canonicalFile
            }
        }
    }

    private fun collectAncestorAggregateFiles(testDataFile: File): List<File> {
        val lltRootMarker = Path.of("cfir", "analysis-tests", "testData", "llt").toString().replace('\\', '/')
        val result = mutableListOf<File>()
        var directory = testDataFile.parentFile?.parentFile
        while (directory != null && directory.invariantSeparatorsPath.contains(lltRootMarker)) {
            directory.listFiles().orEmpty()
                .asSequence()
                .filter { it.isFile && it.extension == "cj" }
                .filter { it.canonicalFile != testDataFile.canonicalFile }
                .filter { FILE_DIRECTIVE.containsMatchIn(it.readText()) }
                .sortedBy { it.name }
                .forEach(result::add)
            directory = directory.parentFile
        }
        return result
    }

    private fun parseFileDirectiveFragments(aggregateFile: File): List<VirtualCompanionFile> {
        val fragments = mutableListOf<VirtualCompanionFile>()
        var currentName: String? = null
        var currentStartLine = 0
        var currentLines = mutableListOf<String>()

        aggregateFile.readLines().forEachIndexed { index, line ->
            val fileMatch = FILE_DIRECTIVE.matchEntire(line)
            if (fileMatch != null) {
                currentName?.let { name ->
                    fragments += VirtualCompanionFile(
                        relativePath = name,
                        content = currentLines.joinToString("\n"),
                        ownerFile = aggregateFile,
                        startLineNumber = currentStartLine,
                    )
                }
                currentName = fileMatch.groupValues[1].trim().replace('\\', '/')
                currentStartLine = index
                currentLines = mutableListOf(line)
            } else if (currentName != null) {
                currentLines += line
            }
        }

        currentName?.let { name ->
            fragments += VirtualCompanionFile(
                relativePath = name,
                content = currentLines.joinToString("\n"),
                ownerFile = aggregateFile,
                startLineNumber = currentStartLine,
            )
        }

        return fragments
    }

    private fun VirtualCompanionFile.toTestFile(): TestFile {
        return TestFile(
            relativePath = relativePath,
            originalContent = content,
            originalFile = ownerFile,
            startLineNumberInOriginalFile = startLineNumber,
            isAdditional = true,
            directives = RegisteredDirectives.Empty,
        )
    }

    private fun File.isPackageCompanionFile(): Boolean {
        return name == "pkg.cj" || name.endsWith(".pkg.cj")
    }

    private data class VirtualCompanionFile(
        val relativePath: String,
        val content: String,
        val ownerFile: File,
        val startLineNumber: Int,
    )

    companion object {
        private val FILE_DIRECTIVE = Regex("""(?m)^\s*//\s*FILE:\s*(.+)$""")
    }
}
