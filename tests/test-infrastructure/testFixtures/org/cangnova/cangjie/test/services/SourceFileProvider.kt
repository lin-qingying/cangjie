package org.cangnova.cangjie.test.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.util.CjTestUtil
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 表示 `SourceFilePreprocessor`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
abstract class SourceFilePreprocessor(val testServices: TestServices) {
    /**
     * 提供 `process` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun process(file: TestFile, content: String): String
}
/**
 * 表示 `MetaInfosCleanupPreprocessor`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class MetaInfosCleanupPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    /**
     * 执行 `process` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun process(file: TestFile, content: String): String {
        return clearTextFromDiagnosticMarkup(content)
    }
}

/**
 * 定义 `ReversibleSourceFilePreprocessor` 接口，约束测试服务参与者需要暴露的协作能力。
 */
interface ReversibleSourceFilePreprocessor {
    /**
     * 执行 `revert` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun revert(file: TestFile, source: String): String
}

/**
 * 表示 `SourceFileProvider`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
open class SourceFileProvider(
    /**
     * 保存 `preprocessors`，供测试服务在测试执行期间读取或传递。
     */
    val preprocessors: List<SourceFilePreprocessor> = emptyList(),
) : TestService {
    /**
     * 保存 `syntheticFiles`，供测试服务在测试执行期间读取或传递。
     */
    private val syntheticFiles = ConcurrentHashMap<String, File>()

    /**
     * 提供 `getContentOfSourceFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    open fun getContentOfSourceFile(file: TestFile): String {
        return preprocessors.fold(file.originalContent) { source, preprocessor ->
            preprocessor.process(file, source)
        }
    }

    /**
     * 提供 `getOrCreateRealFileForSourceFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    open fun getOrCreateRealFileForSourceFile(file: TestFile): File {
        if (preprocessors.isEmpty()) return file.originalFile
        val key = file.originalFile.canonicalPath
        return syntheticFiles.computeIfAbsent(key) {
            val originalExtension = file.originalFile.extension
            val suffix = if (originalExtension.isBlank()) ".cj" else ".${originalExtension.lowercase()}"
            kotlin.io.path.createTempFile("cangjie-test-src-", suffix).toFile().apply {
                writeText(getContentOfSourceFile(file))
                deleteOnExit()
            }
        }
    }
}

/**
 * 保存 `TestServices.sourceFileProvider`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.sourceFileProvider: SourceFileProvider by TestServices.testServiceAccessor()


/**
 * 执行 `toLightTreeShortName` 对应的测试服务流程，维持测试框架的阶段契约。
 */
fun TestFile.toLightTreeShortName() = name.substringAfterLast('/').substringAfterLast('\\')
/**
 * 保存 `TestFile.isCjFile`，供测试服务在测试执行期间读取或传递。
 */
val TestFile.isCjFile: Boolean
    get() = name.endsWith(".cj") || name.endsWith(".cjs")
/**
 * 执行 `getCjFilesForSourceFiles` 对应的测试服务流程，维持测试框架的阶段契约。
 */
fun SourceFileProvider.getCjFilesForSourceFiles(testFiles: Collection<TestFile>, project: Project, findViaVfs: Boolean = false): Map<TestFile, CjFile> {
    return testFiles.mapNotNull {
        if (!it.isCjFile) return@mapNotNull null
        it to getCjFileForSourceFile(it, project, findViaVfs)
    }.toMap()
}
/**
 * 执行 `declaration` 对应的测试服务流程，维持测试框架的阶段契约。
 */
fun  SourceFileProvider.getCjSourceFilesForSourceFiles(
    testFiles: Collection<TestFile>,
): Map<TestFile, CjSourceFile> {
    return testFiles.mapNotNull {
        if (!it.isCjFile) return@mapNotNull null
        val shortName = it.toLightTreeShortName()
        val cjSourceFile = CjInMemoryTextSourceFile(shortName, "/$shortName", getContentOfSourceFile(it))
        it to cjSourceFile
    }.toMap()
}

/**
 * 执行 `getCjFileForSourceFile` 对应的测试服务流程，维持测试框架的阶段契约。
 */
fun SourceFileProvider.getCjFileForSourceFile(testFile: TestFile, project: Project, findViaVfs: Boolean = false): CjFile {
    if (findViaVfs) {
        val realFile = getOrCreateRealFileForSourceFile(testFile)
        StandardFileSystems.local().findFileByPath(realFile.path)
            ?.let { PsiManager.getInstance(project).findFile(it) as? CjFile }
            ?.let { return it }
    }
    return CjTestUtil.createFile(
        testFile.name,
        getContentOfSourceFile(testFile),
        project
    )
}
