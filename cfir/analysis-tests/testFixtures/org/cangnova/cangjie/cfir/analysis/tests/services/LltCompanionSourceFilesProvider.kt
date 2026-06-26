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
    /**
     * 该 provider 使用的指令容器。
     */
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CfirDiagnosticsDirectives)

    /**
     * 为 LLT 测试生成附加源文件。
     *
     * 返回值包含显式 sibling companions、包 companion、目录型多文件 companion
     * 以及父级聚合文件中的虚拟 companion。
     */
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

    /**
     * 收集同目录下除当前测试数据外的所有 `.cj` sibling 文件。
     */
    private fun collectAllSiblingCjFiles(testDataFile: File): List<File> {
        val directory = testDataFile.parentFile ?: return emptyList()
        return directory.listFiles().orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension == "cj" }
            .filter { it.canonicalFile != testDataFile.canonicalFile }
            .sortedBy { it.name }
            .toList()
    }

    /**
     * 收集官方 LLT 约定的包 companion 文件。
     *
     * 包括 `<主文件名>.pkg.cj` 与 `pkg.cj`。
     */
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

    /**
     * 从当前聚合文件的 `// FILE:` 声明目录中收集未显式声明的物理 companion 文件。
     */
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

    /**
     * 从父级聚合文件中收集属于当前目录的虚拟 companion 源文件。
     */
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

    /**
     * 向上查找包含 `// FILE:` 声明的 LLT 聚合文件。
     */
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

    /**
     * 解析聚合文件中的 `// FILE:` 片段为虚拟 companion 文件。
     */
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

    /**
     * 将虚拟 companion 文件转换为测试框架的附加源文件。
     */
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

    /**
     * 判断当前文件是否为 LLT 包 companion 文件。
     */
    private fun File.isPackageCompanionFile(): Boolean {
        return name == "pkg.cj" || name.endsWith(".pkg.cj")
    }

    /**
     * 父级聚合文件中声明的虚拟 companion 源文件。
     *
     * @property relativePath 虚拟文件相对路径。
     * @property content 虚拟文件内容。
     * @property ownerFile 声明该虚拟文件的聚合文件。
     * @property startLineNumber 虚拟文件在聚合文件中的起始行号。
     */
    private data class VirtualCompanionFile(
        /** 虚拟文件相对路径。 */
        val relativePath: String,
        /** 虚拟文件内容。 */
        val content: String,
        /** 声明该虚拟文件的聚合文件。 */
        val ownerFile: File,
        /** 虚拟文件在聚合文件中的起始行号。 */
        val startLineNumber: Int,
    )

    companion object {
        /**
         * 匹配官方多文件测试中的 `// FILE:` 指令。
         */
        private val FILE_DIRECTIVE = Regex("""(?m)^\s*//\s*FILE:\s*(.+)$""")
    }
}
