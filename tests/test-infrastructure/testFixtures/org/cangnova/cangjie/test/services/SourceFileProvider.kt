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

abstract class SourceFilePreprocessor(val testServices: TestServices) {
    abstract fun process(file: TestFile, content: String): String
}
class MetaInfosCleanupPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    override fun process(file: TestFile, content: String): String {
        return clearTextFromDiagnosticMarkup(content)
    }
}

interface ReversibleSourceFilePreprocessor {
    fun revert(file: TestFile, source: String): String
}

open class SourceFileProvider(
    val preprocessors: List<SourceFilePreprocessor> = emptyList(),
) : TestService {
    private val syntheticFiles = ConcurrentHashMap<String, File>()

    open fun getContentOfSourceFile(file: TestFile): String {
        return preprocessors.fold(file.originalContent) { source, preprocessor ->
            preprocessor.process(file, source)
        }
    }

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

val TestServices.sourceFileProvider: SourceFileProvider by TestServices.testServiceAccessor()


fun TestFile.toLightTreeShortName() = name.substringAfterLast('/').substringAfterLast('\\')
val TestFile.isCjFile: Boolean
    get() = name.endsWith(".cj")
fun SourceFileProvider.getCjFilesForSourceFiles(testFiles: Collection<TestFile>, project: Project, findViaVfs: Boolean = false): Map<TestFile, CjFile> {
    return testFiles.mapNotNull {
        if (!it.isCjFile) return@mapNotNull null
        it to getCjFileForSourceFile(it, project, findViaVfs)
    }.toMap()
}
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
