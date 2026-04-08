package org.cangnova.cangjie.frontend.environment

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.config.CangJieSourceRoot
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.cangjieSourceRoots
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.extensions.CompilerConfigurationExtension
import org.cangnova.cangjie.extensions.PreprocessedFileCreator
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import java.io.File

class SourceFileWithModule<T>(
    val sourceFiles: Iterable<T>,
    val isCommon: Boolean,
    val moduleName: String?,
)

fun List<CangJieSourceRoot>.forAllFiles(
    configuration: CompilerConfiguration,
    project: Project,
    body: (VirtualFile, Boolean, moduleName: String?) -> Unit,
) {
    if (isEmpty()) return

    val virtualFileCreator = PreprocessedFileCreator(project)

    var pluginsConfigured = false
    fun ensurePluginsConfigured() {
        if (!pluginsConfigured) {
            for (extension in CompilerConfigurationExtension.getInstances(project)) {
                extension.updateFileRegistry()
            }
            pluginsConfigured = true
        }
    }

    allSourceFilesSequence(
        configuration = configuration,
        findVirtualFile = { file ->
            listOf(StandardFileSystems.local()).findFileByPath(file.normalize().path, StandardFileSystems.FILE_PROTOCOL)
        },
        filter = { virtualFile, isExplicit ->
            if (virtualFile.extension != CangJieFileType.EXTENSION) {
                ensurePluginsConfigured()
            }
            val isCangJie = virtualFile.extension == CangJieFileType.EXTENSION || virtualFile.fileType == CangJieFileType.INSTANCE
            if (isExplicit && !isCangJie) {
                configuration.messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "Source entry is not a Cangjie file: ${virtualFile.path}",
                )
            }
            isCangJie
        },
        convertToSourceFiles = { listOf(virtualFileCreator.create(it)) },
    ).forEach { filesInfo ->
        filesInfo.sourceFiles.forEach { body(it, filesInfo.isCommon, filesInfo.moduleName) }
    }
}

fun interface ValidSourceFilesFilter<VirtualFileT> {
    operator fun invoke(virtualFile: VirtualFileT, isExplicit: Boolean): Boolean
}

fun <VirtualFileT, Source> List<CangJieSourceRoot>.allSourceFilesSequence(
    configuration: CompilerConfiguration,
    findVirtualFile: (File) -> VirtualFileT?,
    filter: ValidSourceFilesFilter<VirtualFileT>,
    convertToSourceFiles: (VirtualFileT) -> Iterable<Source>,
): Sequence<SourceFileWithModule<Source>> = sequence {
    val processedFiles = hashSetOf<VirtualFileT>()

    for ((sourceRootPath, isCommon, hmppModuleName) in this@allSourceFilesSequence) {
        val sourceRoot = File(sourceRootPath)
        val virtualRoot = findVirtualFile(sourceRoot)
        if (virtualRoot == null) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Source file or directory not found: $sourceRootPath",
            )
            continue
        }

        if (!sourceRoot.isDirectory && !filter(virtualRoot, true)) continue

        for (file in sourceRoot.walkTopDown()) {
            if (!file.isFile) continue

            val virtualFile = findVirtualFile(file.absoluteFile)
            if (virtualFile != null && processedFiles.add(virtualFile)) {
                if (filter(virtualFile, false)) {
                    yield(SourceFileWithModule(convertToSourceFiles(virtualFile), isCommon, hmppModuleName))
                }
            }
        }
    }
}

fun getSourceRootsCheckingForDuplicates(configuration: CompilerConfiguration): List<CangJieSourceRoot> {
    val uniqueSourceRoots = hashSetOf<String>()
    val result = mutableListOf<CangJieSourceRoot>()
    for (root in configuration.cangjieSourceRoots) {
        if (!uniqueSourceRoots.add(root.path)) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Duplicate source root: ${root.path}",
            )
        }
        result += root
    }
    return result
}

fun createSourceFilesFromSourceRoots(
    configuration: CompilerConfiguration,
    sourceRoots: List<CangJieSourceRoot>,
    findVirtualFile: (File) -> VirtualFile?,
    convertToSourceFiles: (VirtualFile) -> Iterable<CjSourceFile>,
): MutableList<CjSourceFile> {
    val result = mutableListOf<CjSourceFile>()
    sourceRoots.allSourceFilesSequence(
        configuration = configuration,
        findVirtualFile = findVirtualFile,
        filter = ValidSourceFilesFilter { _, _ -> true },
        convertToSourceFiles = convertToSourceFiles,
    ).forEach { filesInfo ->
        result += filesInfo.sourceFiles
    }
    return result
}
