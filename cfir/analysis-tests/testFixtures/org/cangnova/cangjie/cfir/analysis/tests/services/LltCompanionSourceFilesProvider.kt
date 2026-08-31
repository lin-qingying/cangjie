package org.cangnova.cangjie.cfir.analysis.tests.services

import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives.DEPENDENCE
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
     * 父级聚合文件扫描结果缓存。
     *
     * 全量 LLT 会对同一目录下的大量文件重复调用该 provider。父级目录中的聚合文件集合
     * 与当前测试文件无关，可以按目录缓存，避免每个测试都重新 canonicalize 和读取父级
     * `.cj` 文件。
     */
    private val ancestorAggregateFilesCache: MutableMap<Path, List<File>> = mutableMapOf()

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
        val dependenceCompanions = collectDependenceCompanionFiles(module, testModuleStructure)
        val explicitCompanions = if (containsDirective(globalDirectives, module, LLT_COMPANION_SOURCES)) {
            testModuleStructure.originalTestDataFiles.flatMap(::collectAllSiblingCjFiles)
        } else {
            emptyList()
        }

        val packageCompanions = testModuleStructure.originalTestDataFiles.flatMap(::collectPackageCompanionFiles)
        val multiFileCompanions = testModuleStructure.originalTestDataFiles.flatMap(::collectMultiFileDirectoryCompanions)
        val parentMultiFileCompanions = testModuleStructure.originalTestDataFiles.flatMap(::collectParentMultiFileCompanions)

        val dependencePaths = dependenceCompanions.mapTo(mutableSetOf()) { it.file.normalizedAbsolutePath() }
        val fileCompanions = dependenceCompanions.map { it.toTestFile() } +
            (explicitCompanions + packageCompanions + multiFileCompanions)
            .distinctBy { it.normalizedAbsolutePath() }
            .filter { it.normalizedAbsolutePath() !in dependencePaths }
            .map { it.toTestFile("lltCompanions") }
        val virtualCompanions = parentMultiFileCompanions
            .distinctBy { it.ownerFile.normalizedAbsolutePath() to it.relativePath }
            .map { it.toTestFile() }

        return fileCompanions + virtualCompanions
    }

    /**
     * 按当前模块的 [DEPENDENCE] 指令收集选择性 LLT 依赖源。
     *
     * 指令值必须是相对当前原始测试文件目录的 `.cj` 文件，并且解析后仍位于 LLT
     * 测试数据根目录内。所有结构错误都会立即失败，避免静默改变测试编译单元。
     */
    private fun collectDependenceCompanionFiles(
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<DependenceCompanionFile> {
        if (DEPENDENCE !in module.directives) return emptyList()

        val dependencies = module.directives[DEPENDENCE]
        require(dependencies.isNotEmpty()) {
            "DEPENDENCE must list at least one .cj dependency source for module ${module.name}"
        }
        val originalTestDataFile = testModuleStructure.originalTestDataFiles.singleOrNull()
            ?: error("DEPENDENCE requires exactly one original test data file")
        val originalPath = originalTestDataFile.normalizedAbsolutePath()
        val originalDirectory = originalTestDataFile.parentFile
            ?: error("Test data file has no parent directory: ${originalTestDataFile.path}")
        val lltRoot = originalTestDataFile.requireLltRoot()
        val seenPaths = mutableSetOf<Path>()
        val occupiedRelativePaths = module.files.mapTo(mutableSetOf()) { it.relativePath.replace('\\', '/') }
        val seenRelativePaths = mutableSetOf<String>()

        return dependencies.map { dependency ->
            val relativePath = runCatching { Path.of(dependency) }.getOrElse { cause ->
                throw IllegalArgumentException(
                    "Invalid DEPENDENCE path '$dependency' in ${originalTestDataFile.path}",
                    cause,
                )
            }
            require(!relativePath.isAbsolute) {
                "DEPENDENCE path must be relative: '$dependency' in ${originalTestDataFile.path}"
            }

            val resolvedPath = originalDirectory.toPath().resolve(relativePath).toAbsolutePath().normalize()
            require(resolvedPath.startsWith(lltRoot)) {
                "DEPENDENCE path escapes the LLT test data root: '$dependency' in ${originalTestDataFile.path}"
            }
            require(resolvedPath.toFile().exists()) {
                "DEPENDENCE source does not exist: '$dependency' in ${originalTestDataFile.path}"
            }
            require(resolvedPath.toFile().isFile) {
                "DEPENDENCE source is not a regular file: '$dependency' in ${originalTestDataFile.path}"
            }
            require(resolvedPath.toFile().extension == "cj") {
                "DEPENDENCE source must be a .cj file: '$dependency' in ${originalTestDataFile.path}"
            }
            require(resolvedPath != originalPath) {
                "DEPENDENCE source must not be the original test data file: '$dependency' in ${originalTestDataFile.path}"
            }
            require(seenPaths.add(resolvedPath)) {
                "Duplicate DEPENDENCE source '$dependency' in module ${module.name} of ${originalTestDataFile.path}"
            }

            val lltRelativePath = lltRoot.relativize(resolvedPath).toString().replace('\\', '/')
            val testRelativePath = "lltDependencies/$lltRelativePath"
            require(testRelativePath !in occupiedRelativePaths && seenRelativePaths.add(testRelativePath)) {
                "DEPENDENCE source '$dependency' produces duplicate test path '$testRelativePath' in ${originalTestDataFile.path}"
            }
            DependenceCompanionFile(
                file = resolvedPath.toFile(),
                relativePath = testRelativePath,
            )
        }
    }

    /**
     * 收集同目录下除当前测试数据外的所有 `.cj` sibling 文件。
     */
    private fun collectAllSiblingCjFiles(testDataFile: File): List<File> {
        val directory = testDataFile.parentFile ?: return emptyList()
        return directory.listFiles().orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension == "cj" }
            .filter { !it.isSameNormalizedFile(testDataFile) }
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
        val testDataText = testDataFile.readText()
        val packageName = PACKAGE_DIRECTIVE.find(testDataText)?.groupValues?.get(1)
        val referencedNames = IDENTIFIER.findAll(testDataText)
            .mapTo(mutableSetOf()) { it.value }
        // `// FILE:` 聚合源已经由虚拟 companion 逻辑拆分，不能再把同目录下的
        // 其他聚合测试作为物理源加入当前编译单元，否则会重复声明同名包成员。
        val samePackageFiles = packageName
            ?.takeIf { currentPackageName ->
                directory.name == currentPackageName.substringAfterLast('.') &&
                        !FILE_DIRECTIVE.containsMatchIn(testDataText)
            }
            ?.let { currentPackageName ->
                directory.listFiles().orEmpty()
                    .asSequence()
                    .filter { it.isFile && it.extension == "cj" }
                    .filter { !it.isSameNormalizedFile(testDataFile) }
                    .filter { file ->
                        val fileText = file.readText()
                        PACKAGE_DIRECTIVE.find(fileText)?.groupValues?.get(1) == currentPackageName &&
                                !FILE_DIRECTIVE.containsMatchIn(fileText) &&
                                TOP_LEVEL_DECLARATION.findAll(fileText).any { declaration ->
                                    declaration.groupValues[1] in referencedNames
                                }
                    }
                    .toList()
            }.orEmpty()
        val sameNamePackageFile = directory.resolve("${testDataFile.nameWithoutExtension}.pkg.cj")
        val packageFile = directory.resolve("pkg.cj")
        return (samePackageFiles + sameNamePackageFile + packageFile)
            .filter { it.isFile && !it.isSameNormalizedFile(testDataFile) }
            .distinctBy { it.normalizedAbsolutePath() }
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
            .groupBy({ it.first.normalizedAbsolutePath() }, { it })
        if (declaredByDirectory.isEmpty()) return emptyList()

        return declaredByDirectory.flatMap { (_, declaredEntries) ->
            val directory = declaredEntries.first().first
            val declaredNames = declaredEntries.mapTo(mutableSetOf()) { it.second }
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

        val directory = testDataFile.parentFile?.normalizedAbsolutePath() ?: return emptyList()
        val testDataPath = testDataFile.normalizedAbsolutePath()
        val aggregates = collectAncestorAggregateFiles(testDataFile)
        return aggregates.flatMap { aggregate ->
            val fragments = parseFileDirectiveFragments(aggregate)
            val declaresCurrentDirectory = fragments.any { fragment ->
                aggregate.parentFile.resolve(fragment.relativePath)
                    .parentFile
                    ?.normalizedAbsolutePath() == directory
            }
            if (!declaresCurrentDirectory) return@flatMap emptyList()

            fragments.filter { fragment ->
                aggregate.parentFile.resolve(fragment.relativePath).normalizedAbsolutePath() != testDataPath
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
        while (directory != null && directory.normalizedInvariantSeparatorsPath().contains(lltRootMarker)) {
            val directoryPath = directory.normalizedAbsolutePath()
            result += ancestorAggregateFilesCache.getOrPut(directoryPath) {
                directory.listFiles().orEmpty()
                    .asSequence()
                    .filter { it.isFile && it.extension == "cj" }
                    .filter { FILE_DIRECTIVE.containsMatchIn(it.readText()) }
                    .sortedBy { it.name }
                    .toList()
            }
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
     * 将选择性依赖源转换为保留 LLT 根相对目录的附加测试文件。
     */
    private fun DependenceCompanionFile.toTestFile(): TestFile {
        return TestFile(
            relativePath = relativePath,
            originalContent = file.useLines { it.joinToString("\n") },
            originalFile = file,
            startLineNumberInOriginalFile = 0,
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
     * 返回不触发文件系统 canonicalize 的规范化绝对路径。
     */
    private fun File.normalizedAbsolutePath(): Path = toPath().toAbsolutePath().normalize()

    /**
     * 返回当前测试数据所属的 LLT 测试数据根目录。
     */
    private fun File.requireLltRoot(): Path {
        var directory = parentFile
        while (directory != null) {
            val normalizedPath = directory.normalizedAbsolutePath()
            if (normalizedPath.endsWith(LLT_ROOT_SUFFIX)) return normalizedPath
            directory = directory.parentFile
        }
        error("Test data file is outside cfir/analysis-tests/testData/llt: $path")
    }

    /**
     * 使用规范化绝对路径判断两个测试数据文件是否相同。
     */
    private fun File.isSameNormalizedFile(other: File): Boolean =
        normalizedAbsolutePath() == other.normalizedAbsolutePath()

    /**
     * 返回使用 `/` 的规范化绝对路径字符串。
     */
    private fun File.normalizedInvariantSeparatorsPath(): String =
        normalizedAbsolutePath().toString().replace('\\', '/')

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

    /**
     * [DEPENDENCE] 选中的物理依赖源及其附加测试文件相对路径。
     */
    private data class DependenceCompanionFile(
        /** 依赖源物理文件。 */
        val file: File,
        /** 保留 LLT 根目录结构的附加测试文件路径。 */
        val relativePath: String,
    )

    companion object {
        /**
         * LLT 测试数据根目录的仓库相对路径。
         */
        private val LLT_ROOT_SUFFIX = Path.of("cfir", "analysis-tests", "testData", "llt")

        /**
         * 匹配官方多文件测试中的 `// FILE:` 指令。
         */
        private val FILE_DIRECTIVE = Regex("""(?m)^\s*//\s*FILE:\s*(.+)$""")

        /** 匹配源文件顶层 package 声明，用于同包源文件的编译单元归并。 */
        private val PACKAGE_DIRECTIVE = Regex("""(?m)^[ \t]*package[ \t]+([A-Za-z_][A-Za-z0-9_.]*)[ \t]*$""")

        /** 匹配同包源文件暴露的顶层声明名，用于构造直接依赖集合。 */
        private val TOP_LEVEL_DECLARATION = Regex(
            """(?m)^[ \t]*(?:(?:public|private|internal|protected|open|abstract|sealed|static|const|unsafe|mut|override|redef)[ \t]+)*(?:class|struct|interface|enum|func|let|var|const|typealias)[ \t]+([A-Za-z_][A-Za-z0-9_]*)"""
        )

        /** 匹配源文本中的标识符，供同包直接依赖筛选使用。 */
        private val IDENTIFIER = Regex("""[A-Za-z_][A-Za-z0-9_]*""")
    }
}
