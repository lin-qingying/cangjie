package org.cangnova.cangjie.test.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.TestMetadata
import org.cangnova.cangjie.utils.convertLineSeparators
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.regex.Pattern
import kotlin.io.path.createTempDirectory

object CjTestUtil {
    private const val PLEASE_REGENERATE_TESTS = "Please regenerate tests"
    private val homeDir: String = computeHomeDirectory()

    @JvmStatic
    fun tmpDirForTest(testClassName: String, testName: String): File {
        return normalizeFile(createTempDirectory("${testClassName}_${testName}_").toFile())
    }

    @JvmStatic
    fun tmpDir(name: String): File {
        return normalizeFile(createTempDirectory("${name}_").toFile())
    }

    @JvmStatic
    fun tmpDir(parentDir: File, name: String): File {
        mkdirs(parentDir)
        return normalizeFile(createTempDirectory(parentDir.toPath(), "${name}_").toFile())
    }

    @JvmStatic
    fun tmpDirForReusableFolder(name: String): File {
        val tmpRoot = File(System.getProperty("java.io.tmpdir"))
        mkdirs(tmpRoot)
        val dir = File(tmpRoot, name)
        mkdirs(dir)
        return normalizeFile(dir)
    }

    @JvmStatic
    fun createFile(name: String, text: String, project: Project): CjFile {
        val shortName = name.substringAfterLast('/').substringAfterLast('\\')
        val virtualFile = LightVirtualFile(shortName, CangJieLanguage, text.convertLineSeparators())
        virtualFile.charset = StandardCharsets.UTF_8

        val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
        return factory.trySetupPsiForFile(virtualFile, CangJieLanguage, true, false) as CjFile
    }

    @JvmStatic
    fun doLoadFile(basePath: String, name: String): String = doLoadFile(File(basePath, name))

    @JvmStatic
    fun doLoadFile(file: File): String {
        try {
            return file.readText(Charsets.UTF_8)
        } catch (e: FileNotFoundException) {
            val fullPath = file.absolutePath
            throw IOException(
                "Ensure working directory is project root.\n\t$fullPath (No such file or directory)",
                e,
            )
        } catch (e: IOException) {
            throw e
        }
    }

    @JvmStatic
    fun getFilePath(file: File): String = file.path.replace('\\', '/')

    @JvmStatic
    fun getTestDataPathBase(): String = "$homeDir/compiler/testData"

    @JvmStatic
    fun getHomeDirectory(): String = homeDir

    @JvmStatic
    fun mkdirs(file: File) {
        if (file.isDirectory) return
        if (!file.mkdirs()) {
            if (file.exists()) error("Failed to create $file: file exists and is not a directory")
            error("Failed to create $file")
        }
    }

    @JvmStatic
    fun assertAllTestsPresentByMetadataWithExcluded(
        testCaseClass: Class<*>,
        testDataDir: File,
        filenamePattern: Pattern,
        excludedPattern: Pattern?,
        recursive: Boolean,
        vararg excludeDirs: String,
    ) {
        val rootFile = File(getTestsRoot(testCaseClass))
        val filePaths = collectPathsMetadata(testCaseClass)
        val exclude = excludeDirs.toSet()

        val files = testDataDir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                if (recursive && containsTestData(file, filenamePattern, excludedPattern) && file.name !in exclude) {
                    assertTestClassPresentByMetadata(testCaseClass, file)
                }
            } else {
                val excluded = excludedPattern?.matcher(file.name)?.matches() == true
                if (!excluded && filenamePattern.matcher(file.name).matches()) {
                    assertFilePathPresent(file, rootFile, filePaths)
                }
            }
        }
    }

    @JvmStatic
    fun assertAllTestsPresentByMetadata(
        testCaseClass: Class<*>,
        testDataDir: File,
        filenamePattern: Pattern,
        recursive: Boolean,
        vararg excludeDirs: String,
    ) {
        assertAllTestsPresentByMetadataWithExcluded(
            testCaseClass = testCaseClass,
            testDataDir = testDataDir,
            filenamePattern = filenamePattern,
            excludedPattern = null,
            recursive = recursive,
            excludeDirs = excludeDirs,
        )
    }

    @JvmStatic
    fun assertAllTestsPresentInSingleGeneratedClass(
        testCaseClass: Class<*>,
        testDataDir: File,
        filenamePattern: Pattern,
    ) {
        assertAllTestsPresentInSingleGeneratedClass(
            testCaseClass = testCaseClass,
            testDataDir = testDataDir,
            filenamePattern = filenamePattern,
            excludePattern = null,
        )
    }

    @JvmStatic
    fun assertAllTestsPresentInSingleGeneratedClass(
        testCaseClass: Class<*>,
        testDataDir: File,
        filenamePattern: Pattern,
        excludePattern: Pattern?,
    ) {
        val rootFile = File(getTestsRoot(testCaseClass))
        val filePaths = collectPathsMetadata(testCaseClass)

        testDataDir.walkTopDown().forEach { file ->
            val excluded = excludePattern?.matcher(file.name)?.matches() == true
            if (file.isFile && !excluded && filenamePattern.matcher(file.name).matches()) {
                assertFilePathPresent(file, rootFile, filePaths)
            }
        }
    }

    @JvmStatic
    fun getTestsRoot(testCaseClass: Class<*>): String {
        val metadata = testCaseClass.getAnnotation(TestMetadata::class.java)
            ?: error("No metadata for class: $testCaseClass")
        return metadata.value
    }

    @JvmStatic
    fun nameToCompare(name: String): String {
        val normalized = name.replace('\\', '/')
        return if (isFileSystemCaseSensitive()) normalized else normalized.lowercase(Locale.ROOT)
    }

    @JvmStatic
    fun getMethodMetadata(method: java.lang.reflect.Method): String? {
        return method.getAnnotation(TestMetadata::class.java)?.value
    }

    private fun computeHomeDirectory(): String {
        val userDir = System.getProperty("user.dir") ?: "."
        return File(userDir).absoluteFile.normalize().path.replace('\\', '/')
    }

    private fun normalizeFile(file: File): File {
        return file.canonicalFile
    }

    private fun isFileSystemCaseSensitive(): Boolean {
        val os = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
        return !os.contains("windows")
    }

    private fun assertFilePathPresent(file: File, rootFile: File, filePaths: Set<String>) {
        val relative = relativePath(rootFile, file) ?: return
        val normalized = nameToCompare(relative)
        check(normalized in filePaths) {
            "Test data file missing from generated test class: $file\n$PLEASE_REGENERATE_TESTS"
        }
    }

    private fun collectPathsMetadata(testCaseClass: Class<*>): Set<String> {
        return collectMethodsMetadata(testCaseClass).mapTo(linkedSetOf(), ::nameToCompare)
    }

    private fun collectMethodsMetadata(testCaseClass: Class<*>): Set<String> {
        return testCaseClass.declaredMethods
            .mapNotNull(::getMethodMetadata)
            .toSet()
    }

    private fun containsTestData(dir: File, filenamePattern: Pattern, excludedPattern: Pattern?): Boolean {
        val files = dir.listFiles() ?: return false
        for (file in files) {
            if (file.isDirectory) {
                if (containsTestData(file, filenamePattern, excludedPattern)) return true
            } else {
                val excluded = excludedPattern?.matcher(file.name)?.matches() == true
                if (!excluded && filenamePattern.matcher(file.name).matches()) return true
            }
        }
        return false
    }

    private fun assertTestClassPresentByMetadata(outerClass: Class<*>, testDataDir: File) {
        for (nestedClass in outerClass.declaredClasses) {
            val metadata = nestedClass.getAnnotation(TestMetadata::class.java) ?: continue
            if (getFilePath(testDataDir).endsWith(metadata.value)) {
                return
            }
        }
        error("Test data directory missing from generated test class: $testDataDir\n$PLEASE_REGENERATE_TESTS")
    }

    private fun relativePath(root: File, file: File): String? {
        val rootPath = root.canonicalFile.toPath()
        val filePath = file.canonicalFile.toPath()
        if (!filePath.startsWith(rootPath)) return null
        return rootPath.relativize(filePath).toString().replace('\\', '/')
    }
}
