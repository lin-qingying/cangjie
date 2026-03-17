package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.model.TestFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap

open class SourceFilePreprocessor {
    open fun process(file: TestFile, source: String): String = source
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
            kotlin.io.path.createTempFile("cangjie-test-src-", ".tmp").toFile().apply {
                writeText(getContentOfSourceFile(file))
                deleteOnExit()
            }
        }
    }
}

val TestServices.sourceFileProvider: SourceFileProvider by TestServices.testServiceAccessor()
