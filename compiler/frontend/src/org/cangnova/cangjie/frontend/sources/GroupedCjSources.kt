package org.cangnova.cangjie.frontend.sources

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.CjVirtualFileSourceFile
import org.cangnova.cangjie.frontend.environment.VfsBasedProjectEnvironment
import org.cangnova.cangjie.frontend.environment.allSourceFilesSequence
import org.cangnova.cangjie.frontend.environment.findFileByPath
import org.cangnova.cangjie.frontend.environment.getSourceRootsCheckingForDuplicates
import org.cangnova.cangjie.compiler.plugin.getCompilerExtensions
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.dontSortSourceFiles
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.extensions.CompilerConfigurationExtension
import org.cangnova.cangjie.extensions.PreprocessedFileCreator
import org.cangnova.cangjie.lang.CangJieFileType
import java.io.File
import java.util.Comparator
import java.util.TreeSet

/**
 * 按平台、common 和模块名分组后的仓颉源文件集合。
 */
data class GroupedCjSources(
    /**
     * 平台专属源文件。
     */
    val platformSources: Collection<CjSourceFile>,
    /**
     * common source root 中的源文件。
     */
    val commonSources: Collection<CjSourceFile>,
    /**
     * 按 HMPP 模块名分组的源文件。
     */
    val sourcesByModuleName: Map<String, Set<CjSourceFile>>,
) {
    /**
     * 当前分组结果是否没有任何平台或 common 源文件。
     */
    fun isEmpty(): Boolean = platformSources.isEmpty() && commonSources.isEmpty()
}

/**
 * 当前分组中的全部源文件。
 */
val GroupedCjSources.allFiles: List<CjSourceFile>
    get() = platformSources + commonSources

/**
 * 前端源文件收集的完整结果。
 */
data class CollectedCjSources(
    /**
     * 按来源分组后的源文件集合。
     */
    val groupedSources: GroupedCjSources,
    /**
     * 当前编译配置中的 classpath 根。
     */
    val classpathRoots: List<File>,
) {
    /**
     * 当前收集结果中的全部源文件。
     */
    val allSources: List<CjSourceFile>
        get() = groupedSources.allFiles
}

/**
 * 按源文件路径稳定排序的比较器。
 */
private val cjSourceFileComparator = Comparator<CjSourceFile> { first, second ->
    val firstPath = first.path ?: error("Expected a source file with a well-defined path")
    val secondPath = second.path ?: error("Expected a source file with a well-defined path")
    firstPath.compareTo(secondPath)
}

/**
 * 从编译配置和项目环境中收集仓颉源文件。
 */
fun collectCjSources(
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
): CollectedCjSources {
    fun createSet(): MutableSet<CjSourceFile> = if (compilerConfiguration.dontSortSourceFiles) {
        mutableSetOf()
    } else {
        TreeSet(cjSourceFileComparator)
    }

    val platformSources = createSet()
    val commonSources = createSet()
    val sourcesByModuleName = mutableMapOf<String, MutableSet<CjSourceFile>>()

    val virtualFileCreator = PreprocessedFileCreator(projectEnvironment.project)

    var pluginsConfigured = false
    fun ensurePluginsConfigured() {
        if (!pluginsConfigured) {
            for (extension in CompilerConfigurationExtension.getInstances(projectEnvironment.project)) {
                extension.updateFileRegistry()
            }
            pluginsConfigured = true
        }
    }

    fun findVirtualFile(file: File): VirtualFile? =
        projectEnvironment.knownFileSystems.findFileByPath(file.normalize().path, StandardFileSystems.FILE_PROTOCOL)

    getSourceRootsCheckingForDuplicates(compilerConfiguration)
        .allSourceFilesSequence(
            configuration = compilerConfiguration,
            findVirtualFile = ::findVirtualFile,
            filter = { virtualFile, isExplicit ->
                when (virtualFile.extension) {
                    "java" -> false
                    CangJieFileType.EXTENSION -> true
                    else -> {
                        if (virtualFile.isFile) {
                            ensurePluginsConfigured()
                            val isCangJie = virtualFile.fileType == CangJieFileType.INSTANCE
                            if (isExplicit && !isCangJie) {
                                compilerConfiguration.messageCollector.report(
                                    org.cangnova.cangjie.messages.CompilerMessageSeverity.ERROR,
                                    "Source entry is not a Cangjie file: ${virtualFile.path}",
                                )
                            }
                            isCangJie
                        } else {
                            false
                        }
                    }
                }
            },
            convertToSourceFiles = { virtualFile ->
                val sources = listOf<CjSourceFile>(CjVirtualFileSourceFile(virtualFileCreator.create(virtualFile)))
                if (virtualFile.extension == CangJieFileType.EXTENSION) {
                    sources
                } else {
                    applyCfirProcessSourcesExtension(
                        environment = projectEnvironment,
                        configuration = compilerConfiguration,
                        findVirtualFile = ::findVirtualFile,
                        sources = sources,
                    ) ?: sources
                }
            },
        )
        .forEach { fileInfo ->
            fileInfo.sourceFiles.forEach { file ->
                if (fileInfo.isCommon) commonSources.add(file) else platformSources.add(file)
                fileInfo.moduleName?.let { moduleName ->
                    sourcesByModuleName.getOrPut(moduleName) { mutableSetOf() }.add(file)
                }
            }
        }

    return CollectedCjSources(
        groupedSources = GroupedCjSources(platformSources, commonSources, sourcesByModuleName),
        classpathRoots = compilerConfiguration.classpathRoots.map { File(it.path) },
    )
}

/**
 * 执行 CFIR 源文件处理扩展并返回扩展后的源文件集合。
 */
private fun applyCfirProcessSourcesExtension(
    environment: VfsBasedProjectEnvironment,
    configuration: CompilerConfiguration,
    findVirtualFile: (File) -> VirtualFile?,
    sources: Iterable<CjSourceFile>,
): Iterable<CjSourceFile>? {
    val extensions = configuration
        .getCompilerExtensions(CollectAdditionalSourceFilesExtension)
        .filter { it.isApplicable(configuration) }

    return if (extensions.isEmpty()) {
        sources
    } else {
        extensions.fold(sources) { result, extension ->
            extension.collectSources(environment, configuration, findVirtualFile, result)
        }
    }
}
