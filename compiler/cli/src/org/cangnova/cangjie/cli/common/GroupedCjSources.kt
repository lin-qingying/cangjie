package org.cangnova.cangjie.cli.common

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.CjVirtualFileSourceFile
import org.cangnova.cangjie.cli.compiler.VfsBasedProjectEnvironment
import org.cangnova.cangjie.cli.compiler.allSourceFilesSequence
import org.cangnova.cangjie.cli.compiler.findFileByPath
import org.cangnova.cangjie.cli.compiler.getSourceRootsCheckingForDuplicates
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

data class GroupedCjSources(
    val platformSources: Collection<CjSourceFile>,
    val commonSources: Collection<CjSourceFile>,
    val sourcesByModuleName: Map<String, Set<CjSourceFile>>,
) {
    fun isEmpty(): Boolean = platformSources.isEmpty() && commonSources.isEmpty()
}

val GroupedCjSources.allFiles: List<CjSourceFile>
    get() = platformSources + commonSources

data class CollectedCjSources(
    val groupedSources: GroupedCjSources,
    val classpathRoots: List<File>,
) {
    val allSources: List<CjSourceFile>
        get() = groupedSources.allFiles
}

private val cjSourceFileComparator = Comparator<CjSourceFile> { first, second ->
    val firstPath = first.path ?: error("Expected a source file with a well-defined path")
    val secondPath = second.path ?: error("Expected a source file with a well-defined path")
    firstPath.compareTo(secondPath)
}

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
