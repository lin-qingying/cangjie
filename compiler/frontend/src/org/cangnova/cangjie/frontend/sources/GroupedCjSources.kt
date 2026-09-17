package org.cangnova.cangjie.frontend.sources

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjVirtualFileSourceFile
import org.cangnova.cangjie.frontend.environment.VfsBasedProjectEnvironment
import org.cangnova.cangjie.frontend.environment.allSourceFilesSequence
import org.cangnova.cangjie.frontend.environment.findFileByPath
import org.cangnova.cangjie.frontend.environment.getSourceRootsCheckingForDuplicates
import org.cangnova.cangjie.compiler.plugin.getCompilerExtensions
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.classpathRoots
import org.cangnova.cangjie.config.compileCjd
import org.cangnova.cangjie.config.dontSortSourceFiles
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.extensions.CompilerConfigurationExtension
import org.cangnova.cangjie.extensions.PreprocessedFileCreator
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.macro.file.CangJieMacroCallFileType
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
                when {
                    virtualFile.extension == "java" -> false

                    // 仓颉家族：三值穷尽，无 else —— 新增种类时编译器会强制在此表态（R1 互斥输入）。
                    // MACRO_CALL 显式拒绝：它是"能被识别的文件种类"，不是"能作为输入的源"——
                    // 这一条是声明过的规则，不能依赖"扩展名不匹配所以恰好落进 else"这种巧合。
                    virtualFile.isCangJieFamily() -> when (virtualFile.sourceKind()) {
                        CjSourceKind.SOURCE -> !compilerConfiguration.compileCjd
                        CjSourceKind.DECLARATION -> compilerConfiguration.compileCjd
                        CjSourceKind.MACRO_CALL -> false
                    }

                    // ↓ 既有 else 分支：逐字保留 —— ensurePluginsConfigured() 与显式 ERROR 上报
                    //   是有副作用的既有行为，不得简化。
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
                // 仓颉家族（.cj / .cj.d）直连；applyCfirProcessSourcesExtension 只服务
                // 真正的"非仓颉"插件扩展机制（如宏调用文件预处理）。
                // 注意方向：是"家族 ⇒ 直连"，而不是"非实现文件 ⇒ 走扩展"——后者对 .cj.d 是空操作。
                if (virtualFile.extension == CangJieFileType.EXTENSION || virtualFile.sourceKind().isDeclaration) {
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

/**
 * 判定虚拟文件的源种类。
 *
 * ⚠️ 不能用 [VirtualFile.getExtension]：它对 `a.cj.d` 返回 `"d"`、对 `a.cj.macrocall` 返回 `"macrocall"`，
 * 必须用完整文件名做字面后缀匹配（R2）。
 *
 * 先看 FileType（精确），再回退到文件名（覆盖 IDE 的 LightVirtualFile、测试夹具等未注册类型的场景）。
 * `filter` 与 `convertToSourceFiles` 共用这一份实现，不得抄成两份（3.1 原则 5：单一真源）。
 */
private fun VirtualFile.sourceKind(): CjSourceKind = when {
    fileType is CangJieDeclarationFileType -> CjSourceKind.DECLARATION
    fileType is CangJieMacroCallFileType -> CjSourceKind.MACRO_CALL
    fileType is CangJieFileType -> CjSourceKind.SOURCE
    else -> CjSourceKind.fromFileName(nameSequence.toString())
}

/**
 * 判断文件是否属于仓颉家族（`.cj` / `.cj.d` / `.cj.macrocall` 或对应 FileType）。
 *
 * `SOURCE` 的判定必须同时接受"扩展名命中"（普通源文件）与"FileType 命中"
 * （经 PreprocessedFileCreator 重命名/宏展开的产物），二者语义等价。
 */
private fun VirtualFile.isCangJieFamily(): Boolean = when (sourceKind()) {
    CjSourceKind.DECLARATION, CjSourceKind.MACRO_CALL -> true
    CjSourceKind.SOURCE -> extension == CangJieFileType.EXTENSION || fileType is CangJieFileType
}
